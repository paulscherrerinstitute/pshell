package ch.psi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.logging.Logger;


public class Align extends ObservableBase<Align.AlignListener>{
    private static final Logger _logger = Logger.getLogger(RedisStream.class.getName());
    
    public static interface AlignListener {
        void onMessage(Long id, Long timestamp, Object msg);
    }    
    
    final String[] channels;    
    final Boolean partial;
    final int buffer;
    Filter filter = null;
    final MaxLenHashMap.OrderedMap<Long, Map<String, Object>> data;
    final Boolean partialAfter;
    boolean firstComplete=false;
    volatile boolean added = false;
    
    long sent_id = -1;
    
    public Align(String[] channels, Boolean partial, int buffer){
        this.channels = channels;
        this.buffer = buffer;
        partialAfter =  (partial==null);
        this.partial = partialAfter || partial;
        this.data = new MaxLenHashMap.OrderedMap<>((int)(buffer*1.2));
        this.sent_id = -1;
    }
    
    public void setFilter(String filter){
        this.filter = (filter==null) ? null : new Filter(filter);
    }
    
    public String getFilter(){
        return (filter==null) ? null : filter.get();
    }
    
    public synchronized void add(Long id, Long timestamp, String channel, Object value){
        if (id==null){
            id = timestamp;
        }
        if (!data.containsKey(id)){
            Map entry = new HashMap<String, Object>();
            entry.put("timestamp", timestamp);
            data.put(id, entry);
        }
        data.get(id).put(channel, value);
        added = true;
    }

    public synchronized void reset(){
        data.clear();
        firstComplete = false;
        sent_id = -1;
    }

    public synchronized void process(){        
        if (added){
            added = false;
            NavigableSet<Long> keysInOrder =  data.navigableKeySet();
            long last_complete_id = -1;  
            for (long id : keysInOrder.reversed()){
                if (data.get(id).size() == channels.length+1){
                    last_complete_id = id;
                    break;
                }            
            }

            for (Long id : new ArrayList<>(keysInOrder)){
                boolean complete = data.get(id).size() == channels.length+1;
                boolean done = complete || (last_complete_id > id) || (data.size() > buffer);
                if (!done) {
                    break;
                }
                Map<String, Object> msg = data.remove(id);
                if (complete || partial){  
                    if (complete){
                        firstComplete = true;
                    }
                    if (!partialAfter || firstComplete){
                        if (sent_id >= id){
                             _logger.warning(String.format("Invalid ID %d - last sent ID %d", id, sent_id));
                        } else{
                            Long timestamp = (Long) msg.getOrDefault("timestamp", null);
                            try{
                                if ((filter==null) || (isValid(id, timestamp, msg))){
                                    for (AlignListener listener : getListeners()) {
                                        listener.onMessage(id, timestamp, msg);
                                    }
                                }
                            }
                            catch (Exception ex){
                                _logger.warning("Error receiving data: " + ex.getMessage());
                            }                    
                            sent_id = id;
                        }
                    }
                } else {
                    _logger.finest("Discarding partial message: " + id);
                }
            }
        }
    }

    public boolean isValid(Long id, Long timestamp, Map<String, Object> msg){
        try{            
            if (filter!=null){
                return filter.check(msg);
            }
            return true;
        } catch (Exception ex){
            _logger.warning("Error processing filter: " + ex.getMessage());
            return false;
        }
    }                
}
