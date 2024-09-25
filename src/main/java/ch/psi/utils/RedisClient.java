package ch.psi.utils;

import ch.psi.utils.Align.AlignListener;
import redis.clients.jedis.Jedis;
import java.util.*;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import redis.clients.jedis.params.XReadParams;


public class RedisClient {
    private static final Logger _logger = Logger.getLogger(RedisClient.class.getName());
    private final String host;
    private final int port;
    private final Integer db;
    private volatile boolean aborted = false;    
    private volatile Filter filter;
    ObjectMapper mapper;
    
    int bufferSize = 1000;    
    int readCount = 5;    
    int readBlock = 10;    
    
    public RedisClient(String host, int port) {
        this(host, port, null);
    }
    
    public RedisClient(String host, int port, Integer db) {
        this.host = host;
        this.port = port;
        this.db = db;
        mapper = new ObjectMapper(new CBORFactory());
    }    
    
    public int getBufferSize(){
        return bufferSize;
    }

    public void setBufferSize(int size){
        this.bufferSize = bufferSize;
    }   
    
    
    public int getReadBlock(){
        return readBlock;
    }

    public void setReadBlock(int size){
        this.readBlock = readBlock;
    }       
    
    public int getReadCount(){
        return readCount;
    }

    public void setReadCount(int size){
        this.readCount = readCount;
    }       
    
    public enum InitialMsg{
        all,
        now,
        newer        
    }
    

    public void run(List<String> channels, AlignListener lisnener, Boolean partial, InitialMsg initialMsg, String filter) {                        
        _logger.info("Starting Redis streaming - channels: " + Str.toString(channels) + " - filter: " + Str.toString(filter));
        
        Align align = new Align(channels.toArray(new String[0]), partial, getBufferSize());
        align.setFilter(filter);
        align.addListener(lisnener);       
        if (initialMsg==null){
            initialMsg = InitialMsg.all;
        }
        String initialId = initialMsg==InitialMsg.newer ? "$" : "0";
        
        try(Jedis jedis = new Jedis(host, port) ){       
            if (db!=null){
                jedis.select(db);
            }
            
            XReadParams params = XReadParams.xReadParams().count(readCount).block(readBlock);            
            Map<String, String> streamIds = new HashMap<>();  // Track last message ID for each channel
            Map<String, Integer> channelIndexes = new HashMap<>();  // Track last message ID for each channel
            int index=0;
            for (String channel : channels) {
                streamIds.put(channel, initialId);  // New messages
                channelIndexes.put(channel, index);
                index++;
            }
            // Pre-build the streams list 
            List<AbstractMap.SimpleEntry<byte[], byte[]>> streams = new ArrayList<>();
            for (String channel : channels) {
                streams.add(new AbstractMap.SimpleEntry<>(channel.getBytes(), initialId.getBytes()));
            }            
            //for (int i = 0; i < channels.size(); i++) {
            //    streams.get(i).setValue(streamIds.get(channels.get(i)).getBytes());  // Update the message ID for the channel
            //}                

            while (!aborted) {                       
                AbstractMap.SimpleEntry[] sts = streams.toArray(new AbstractMap.SimpleEntry[0]);                
                
                List result  = jedis.xread(params,sts);
                if (result != null) {
                    for (Object s : result){
                        if (s instanceof List){
                            List stream_msg = (List)s;
                            String channelName = new String((byte[])stream_msg.get(0));
                            stream_msg.set(0, channelName);
                            for (Object m : (List)stream_msg.get(1)){
                                if (m instanceof List){
                                    try {
                                        List message = (List)m;                                                                                
                                        String msgid = new String((byte[]) message.get(0));
                                        message.set(0, msgid);
                                        //Update message for the channel
                                        streams.get(channelIndexes.get(channelName)).setValue(msgid.getBytes());                                                                            
                                        List msg = (List)message.get(1);
                                        Map fields = deserialize(msg);
                                        String channel = (String)fields.get("channel"); 
                                        long timestamp = Long.parseLong((String)fields.get("timestamp"));
                                        long id = Long.parseLong((String)fields.get("id"));
                                        Object value = fields.get("value");
                                        
                                        if ((initialMsg != InitialMsg.now) || (System.currentTimeMillis()>=Time.timestampToMillis(timestamp))){                                        
                                            //System.out.println(String.format("ID: %d, Timestamp: %d, Channel: %s, Value: %s", id, timestamp, channel, Str.toString(value)));
                                            align.add(id, timestamp, channel, value);                                        
                                        }
                                    } catch (Exception ex){                                        
                                        _logger.warning("Deserialization error: " + ex.getMessage());
                                    }                                
                                }
                            }
                        }
                    }
                    align.process();
                }                
            }
        } catch (Exception ex){
            _logger.warning("redis streaming error: " + ex.getMessage());
            throw ex;
        } finally{
            align.removeListener(lisnener);
            _logger.info("Stopping Redis streaming: " + Str.toString(channels));
        }               
    }

           
    private Map deserialize(List msg) {        
        Map data =  new HashMap();
        for (int i=0; i<msg.size(); i+=2){
            Object value = null;
            String field = new String((byte[])msg.get(i));
            byte[] blob = (byte[])msg.get(i+1);
            if (field.equals("value")){
                try{
                    Map frame = mapper.readValue(blob, Map.class);
                    value = frame.get("data");
                    List shape = (List) frame.get("shape");
                    String dtype = (String) frame.get("dtype");                    
                } catch (Exception ex){
                    _logger.warning("Deserialization error: " + ex.getMessage());
                }
            } else {
                value = new String((byte[])msg.get(i+1));
            }
            data.put(field, value);
        }
        return data;
    }
    static int count = 0;
    public static void main(String[] args) {        
        RedisClient client = new RedisClient("std-daq-build", 6379);
        
        AlignListener listener =  ((AlignListener) (Long id, Long timestamp, Object msg) -> {            
            System.out.println(String.format("ID: %d, Timestamp: %s, Count: %d, Msg: %s", id, Time.timestampToStr(timestamp), count++, Str.toString(msg)));
            //System.out.println(String.format("ID: %d, Timestamp: %d,  Now: %d,  Count: %d, Msg: %s", id, timestamp, System.currentTimeMillis(), count++, Str.toString(msg)));
        });
        String filter = null;
        filter = "channel1<0.3 AND channel2<0.1";      
        client.run(Arrays.asList("channel1", "channel2", "channel3"), listener, false, InitialMsg.newer, filter);   
    }
}
