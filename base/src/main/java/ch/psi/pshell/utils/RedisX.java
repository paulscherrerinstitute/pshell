package ch.psi.pshell.utils;

import ch.psi.pshell.utils.Align.AlignListener;
import ch.psi.pshell.utils.Threading.VisibleCompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.XReadParams;


public class RedisX implements AutoCloseable {
    public static String getDefaultAddress() {
        return System.getenv().getOrDefault("REDIS_DEFAULT_URL", "localhost");
    }    
    public static int DEFAULT_PORT = 6379;
    
    private static final Logger _logger = Logger.getLogger(RedisX.class.getName());
    private final String host;
    private final int port;
    private final Integer db;
    private volatile boolean aborted = false;    
    ObjectMapper mapper;
    Set<VisibleCompletableFuture> futures = new HashSet<>();
    
    int bufferSize = 1000;    
    int readCount = 5;    
    int readBlock = 10;    
    
    public RedisX() {
        this(getDefaultAddress());
    }
    
    public RedisX(String address) {
        if (address.contains(":")) {
            String[] tokens = address.split(":");
            this.host = tokens[0].trim();
            this.port = Integer.valueOf(tokens[1]);        
        } else {
            this.host = address;
            this.port = DEFAULT_PORT;                    
        }
        this.db = null;   
        mapper = new ObjectMapper(new CBORFactory());
    }
    
    public RedisX(String host, int port) {
        this(host, port, null);
    }
    
    public RedisX(String host, int port, Integer db) {
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
    
    public void abort(){
        this.aborted = true;
    }           
    
    
    public String getHost(){
        return host;
    }

    public int getPort(){
        return port;
    }

    public String getAddress(){
        return getHost() + ":" + getPort();
    }
    

    public void run(List<String> channels, AlignListener lisnener, Boolean incomplete, Boolean  onlyNew, Range timeRange, Range idRange, String filter) {                        
        _logger.log(Level.INFO, "Starting Redis streaming - channels: {0} - filter: {1}", new Object[]{Str.toString(channels), Str.toString(filter)});
        aborted = false;        
        Align align = new Align(channels.toArray(new String[0]), incomplete, getBufferSize());
        align.setFilter(filter);
        align.setTimeRange(timeRange);
        align.setIdRange(idRange);
        align.addListener(lisnener);      
        try(Jedis jedis = new Jedis(host, port) ){       
            String initialId = Boolean.TRUE.equals(onlyNew) ? "$" : "0";
            if (db!=null){
                jedis.select(db);
            }
            
            XReadParams params = XReadParams.xReadParams().count(readCount).block(readBlock);            
            Map<String, String> streamIds = new HashMap<>();  
            Map<String, Integer> channelIndexes = new HashMap<>(); 
            int index=0;
            for (String channel : channels) {
                streamIds.put(channel, initialId); 
                channelIndexes.put(channel, index);
                index++;
            }
            // Pre-build the streams list 
            List<AbstractMap.SimpleEntry<byte[], byte[]>> streams = new ArrayList<>();
            for (String channel : channels) {
                streams.add(new AbstractMap.SimpleEntry<>(channel.getBytes(), initialId.getBytes()));
            }            

            while (!aborted) {                       
                AbstractMap.SimpleEntry[] sts = streams.toArray(new AbstractMap.SimpleEntry[0]);                
                
                List result  = jedis.xread(params,sts);
                if (result != null) {
                    for (Object s : result){
                        if (s instanceof List stream_msg){
                            String channelName = new String((byte[])stream_msg.get(0));
                            stream_msg.set(0, channelName);
                            for (Object m : (List)stream_msg.get(1)){
                                if (m instanceof List message){
                                    try {
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
                                        align.add(id, timestamp, channel, value);                                        
                                    } catch (Exception ex){                                        
                                        _logger.log(Level.WARNING, "Deserialization error: {0}", ex.getMessage());
                                    }                                
                                }
                            }
                        }
                    }
                    align.process();
                }                
            }
        } catch (Align.RangeException ex){
            _logger.info("Stream range fisished");
        } catch (Exception ex){
            _logger.log(Level.WARNING, "redis streaming error: {0}", ex.getMessage());
            throw ex;
        } finally{
            align.removeListener(lisnener);
            _logger.log(Level.INFO, "Stopping Redis streaming: {0}", Str.toString(channels));
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
                    if ((dtype!=null) && (shape!=null) && (value instanceof byte[])){                                                
                        ByteOrder byteOrder =  ByteOrder.LITTLE_ENDIAN;
                        Object arr = BufferConverter.fromArray((byte[]) value, Type.fromKey(dtype), byteOrder);
                        int[] sh = (int[]) Convert.toPrimitiveArray(shape, int.class);
                        value = Convert.reshape(arr, sh);                        
                    }
                } catch (Exception ex){
                    _logger.log(Level.WARNING, "Deserialization error: {0}", ex.getMessage());
                }
            } else {
                value = new String((byte[])msg.get(i+1));
            }
            data.put(field, value);
        }
        return data;
    }
    
    public VisibleCompletableFuture start(List<String> channels, AlignListener lisnener, Boolean incomplete, Boolean  onlyNew, Range timeRange, Range idRange, String filter) {
        VisibleCompletableFuture future =  (VisibleCompletableFuture) Threading.getPrivateThreadFuture(() -> run(channels, lisnener, incomplete, onlyNew, timeRange, idRange, filter));
        futures.add(future);
        future.handle((res, ex)->{
            futures.remove(future);
            return res;
        });
        return future;
    }
    
    public void join(long millis) throws InterruptedException{
        for (VisibleCompletableFuture future: futures){
            Thread t = future.getRunningThread();
            if (t!=null){
                t.join(millis);
            }
        }
    }
    
    public boolean isRunning(){
        for (VisibleCompletableFuture future: futures){
            Thread t = future.getRunningThread();
            if (t!=null){
                if (t.isAlive()){
                    return true;
                }                
            }
        }
        return false;
    }             
    
    @Override
    public void close() throws InterruptedException {
        abort();
        Thread.sleep(0);
        for (VisibleCompletableFuture future: futures){
            future.getRunningThread().interrupt();
        }
    }    
    
    static int count = 0;
    public static void main(String[] args) throws InterruptedException {        
        try (RedisX stream = new RedisX("std-daq-build", 6379)){

            AlignListener listener =  ((AlignListener) (Long id, Long timestamp, Map<String, Object> msg) -> {            
                System.out.println(String.format("ID: %d, Timestamp: %s, Count: %d, Msg: %s", id, Time.timestampToStr(timestamp), count++, Str.toString(msg)));
                //System.out.println(String.format("ID: %d, Timestamp: %d,  Now: %d,  Count: %d, Msg: %s", id, timestamp, System.currentTimeMillis(), count++, Str.toString(msg)));
            });
            String filter = null;
            //filter = "channel1<0.3 AND channel2<0.1";      
            
            //Long now = System.currentTimeMillis() - 860075; //0ffset
            //VisibleCompletableFuture future = stream.start(Arrays.asList("channel1", "channel2", "channel3"), listener, true,  true, new Range(now, now+2000), null, filter);   
            //stream.join(0);
            //System.out.println(stream.isRunning());         
            
            //ong start = 22238570272L + 2000;
            //System.out.println(start);
            //future = stream.start(Arrays.asList("channel1", "channel2", "channel3"), listener, true,  true, null,  new Range(start, start+100), filter);   
            //stream.join(0);
            //System.out.println(stream.isRunning());                     

            VisibleCompletableFuture future = stream.start(Arrays.asList("channel1", "channel2", "channel3"), listener, false,  false, null, null, filter);   
            Thread.sleep(2000);
            stream.abort();
            stream.join(0);
            System.out.println(stream.isRunning());         
            
            stream.start(Arrays.asList("array1", "array2"), listener, false, false, null, null, filter);   
            Thread.sleep(2000);
            System.out.println(stream.isRunning());         
            stream.abort();
            stream.join(0);

            stream.start(Arrays.asList("channel1"), listener, false,  false, null, null, filter);   
            stream.start(Arrays.asList("channel2"), listener, false,  false, null, null, filter);   
            Thread.sleep(2000);
            System.out.println(stream.isRunning());         
        }        
    }
}
