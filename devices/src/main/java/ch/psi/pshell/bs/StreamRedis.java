package ch.psi.pshell.bs;

import ch.psi.bsread.message.Type;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.DeviceBase.DeviceTimeoutException;
import ch.psi.pshell.device.DeviceConfig;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.ReadonlyAsyncRegisterBase;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterNumber;
import ch.psi.pshell.utils.Align;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.RedisX;
import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.TimestampedValue;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A device implementing a synchronous stream through Redis
 */
public class StreamRedis extends DeviceBase implements StreamDevice {
    
    public static class StreamRedisConfig extends DeviceConfig {
            public boolean incomplete;
            public boolean onlyNew;
            public String filter;

            public String channel01;
            public String channel02;
            public String channel03;
            public String channel04;
            public String channel05;
            public String channel06;
            public String channel07;
            public String channel08;
            public String channel09;
            public String channel10;

            ArrayList<String> getChannels() {
                ArrayList<String> ret = new ArrayList<>();
                try {
                    for (int i = 1; i <= 1000; i++) {
                        Field f = StreamConfig.class.getField("channel" + String.format("%02d", i));
                        String val = ((String) (f.get(this)));
                        if (val != null) {
                            ret.add(val);
                        }
                    }
                } catch (Exception ex) {
                }
                return ret;
            }
            
         
    }    
    
    public static class StreamRedisValue extends StreamValue {
        StreamRedisValue(long pulseId, long timestamp, List<String> identifiers, java.util.Map<String, Object> msg) {
            super(pulseId, timestamp, identifiers, identifiers.stream()
                                                            .filter(msg::containsKey)
                                                            .map(msg::get)
                                                            .collect(Collectors.toList()), null);
        }
        
        public int[]  getShape(String id) {
            return getShape(toItemIndex(id));
        }     
     
        public int[]  getShape(int index) {
            if ((index<0) || (index>=identifiers.size())){
                return null;
            }
            Object value = getValue(index);
            return Arr.getShape(value);
        }        

        public Type getType(String id) {
            return getType(toItemIndex(id));
        }     

        public Type getType(int index) {
            if ((index<0) || (index>=identifiers.size())){
                return null;
            }
            Object value = getValue(index);
            String type = ch.psi.pshell.utils.Type.fromClass(value.getClass()).toString();
            return Type.valueOf(type);
        }     
        
        
    }
            
    public class Channel extends ReadonlyAsyncRegisterBase{
        long pulseId;
        protected Channel(String name) {
            super(name);
        }        
        
        @Override
        public BeamSynchronousValue takeTimestamped() {
            synchronized (cacheUpdateLock) {
                TimestampedValue val = super.takeTimestamped();
                return (val == null) ? null : new BeamSynchronousValue(val.getValue(), val.getTimestamp(), val.getNanosOffset(), pulseId);
            }
        }

        protected void set(long pulseId, long timestamp, Object value) {            
            synchronized (cacheUpdateLock) {
                this.pulseId = pulseId;
                setCache(value, timestamp);
            }
        }    
    }
        
    public class ChannelNumber extends Channel implements ReadonlyRegisterNumber{                
        protected ChannelNumber(String name) {
            super(name);
        }        
    }
    
    public class ChannelArray extends Channel implements ReadonlyRegisterArray{        
        protected ChannelArray(String name) {
            super(name);
        }        
    }
    
    public class ChannelMatrix extends Channel implements ReadonlyRegisterMatrix{        
        protected ChannelMatrix(String name) {
            super(name);
        }        
    }    
           
    Map<String, Channel> channels;
    List<String> channelNames = new ArrayList<>();
    List<Readable> readables;
    volatile AtomicBoolean started = new AtomicBoolean(false);
    volatile boolean paused;
    Boolean fixedChildren;
    String filter = null;
    
    RedisX redis;
    Boolean incomplete;
    Boolean onlyNew;

    @Override
    public StreamRedisConfig getConfig() {
        return (StreamRedisConfig) super.getConfig();
    }

    public String getAddress() {
        try{
            return redis.getAddress();
        } catch (Exception ex){
            return null;
        }
    }

    public String getStreamSocket() {
        try{
            return getAddress();
        } catch (Exception ex){
            return null;
        }
    }

    public StreamRedis(String name, boolean persisted) {
        this(name, (RedisX) null, persisted);
    }

    @Override
    public Class _getElementType() {
        return Long.class;
    }

    /**
     * Direct URL
     */    

    public StreamRedis(String name, String address, boolean incomplete) {
        this(name, address, null, incomplete);
    }

    public StreamRedis(String name, String address, String filter, boolean incomplete) {
        this(name, (RedisX)null, address, false);
        setIncomplete(incomplete);
    }

    
    /**
     * If provider is null then uses default provider.
     */
    public StreamRedis(String name, RedisX provider, boolean persisted) {
        this(name, provider, null, persisted);
    }

    protected StreamRedis(String name, RedisX provider, String address, boolean persisted) {
        super(name, persisted ? new StreamConfig() : null);
        if (provider == null) {
            if (address == null) {
                provider = new RedisX();
            } else {
                provider = new RedisX(address);                
            }
        }

        this.redis = provider;
        channels = new HashMap<>();
        channelNames =  new ArrayList<>();
        readables = new ArrayList<>();
        setMonitored(true);
        if (persisted) {
            setFilter(getConfig().filter);
            ArrayList<String> channels = getConfig().getChannels();
            for (String channel : channels) {
                try {
                    addChannel(channel);
                } catch (Exception ex) {
                    Logger.getLogger(StreamRedis.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
    }

    public StreamRedis(String name) {
        this(name, (RedisX) null);
    }

    public StreamRedis(String name, RedisX provider) {
        this(name, provider, false);
    }

    public StreamRedis(String name, String filter) {
        this(name, null, filter);
    }

    public StreamRedis(String name, RedisX provider, String filter) {
        this(name, provider);
        setFilter(filter);
    }

    public StreamRedis(String name, RedisX provider, String filter, Boolean incomplete) {
        this(name, provider, filter);
        setIncomplete(incomplete);
    }

    public StreamRedis(String name, String... channels) {
        this(name, null, channels);
    }

    public StreamRedis(String name, RedisX provider, String... channels) {
        this(name, provider, false);
        addChannels(channels);
    }

    public StreamRedis(String name, RedisX provider, Boolean incomplete, String... channels) {
        this(name, provider, channels);
        setIncomplete(incomplete);
    }

    public StreamRedis(String name, RedisX provider, String filter, Boolean incomplete, String... channels) {
        this(name, provider, incomplete, channels);
        setFilter(filter);
    }
    
    //Constructor for merger classes
    public StreamRedis(String name, StreamRedis... components) {
        super(name);
        channels = new HashMap<>();
        channelNames = new  ArrayList<>();
        readables = new ArrayList<>();        
        setComponents(components);
    }    

    public void setFilter(String filter){
        this.filter = filter;
    }    
    
    public String getFilter(){
        if (filter!=null){
            return filter;
        }
        if (getConfig()!=null){
            return getConfig().filter;
        }
        return null;
        
    }
       
    
    public void setIncomplete(Boolean incomplete) {
        this.incomplete = incomplete;
    }

    public Boolean getIncomplete() {        
        if (incomplete!=null){
            return incomplete;
        }
        if (getConfig()!=null){
            return getConfig().incomplete;
        }
        return false;
    }
    
    public void setOnlyNew(Boolean onlyNew) {
        this.onlyNew = onlyNew;
    }

    public Boolean getOnlyNew() {        
        if (onlyNew!=onlyNew){
            return incomplete;
        }
        if (getConfig()!=null){
            return getConfig().onlyNew;
        }
        return false;
    }

    
    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        stop();        
        if((fixedChildren!=null) &&  (!fixedChildren)){
            setChildren(new Device[0]);
        }
        channels.clear();        
        readables.clear();
        readables.add(this);
        super.doInitialize();
        //fixed by first initialize
        if (fixedChildren == null) {
            fixedChildren = Arr.containsClass(getChildren(), StreamChannel.class);
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        if (getState() == State.Busy) {
            if (!isPaused()){
                read();
            }
        } else if (getState() == State.Ready) {
            start();
            try {
                read();
            } finally {
                stop();
            }
        }
        super.doUpdate();
    }

    boolean debug;

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean value) {
        debug = value;
    }

    final Object lock = new Object();

    
    public void addChannels(String[] channels) {
        for (String channel : channels) {
            try {
                addChannel(channel);
            } catch (Exception ex) {
                Logger.getLogger(StreamRedis.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    
    public void addChannel(String channel) throws IOException, InterruptedException {
        synchronized (lock) {
            assertStateNot(State.Busy);
            
            //TODO
            if (!channelNames.contains(channel)){
                channelNames.add(channel);
            }
            
            if (isInitialized()) {
                doInitialize();
            }   
        }
    }

    
    Align.AlignListener streamListener =  ((Align.AlignListener) (Long id, Long timestamp, Map<String, Object> msg) -> {            
        if (paused){
            return;
        }
        for (String channel : channelNames){
            Channel c = channels.get(channel);
            Object val = msg.get(channel);
            if (val==null){
                msg.put(channel, null);
            }
            if (c == null) {                    
                if (val!=null){
                    try {
                        int[] shape = Arr.getShape(val);                         
                        if ((shape==null) || (shape.length==0)){
                            if (val instanceof Number){
                                c = new ChannelNumber(channel);
                            } else {
                                c = new Channel(channel);
                            }
                        } else if (shape.length==1){
                            c = new ChannelArray(channel);
                        } else if (shape.length==2){
                            c = new ChannelMatrix(channel);
                        } else {
                            c = new Channel(channel);
                        }
                        channels.put(channel, c);
                        readables.add(c);
                        addChild(c);
                        c.initialize();
                        c.set(id, timestamp, val);
                    } catch (Exception ex) {
                        getLogger().log(Level.FINE, null, ex);                    
                    }
                }
            } else {                   
                c.set(id, timestamp, val);
            }
        }
        setCache(new StreamRedisValue(id, timestamp, channelNames, msg));        
    });
    
    @Override
    public void start() {        
        paused = false;
        if (started.compareAndSet(false, true)) {
            channels.clear();
            redis.start(new ArrayList<>(channelNames), streamListener, getIncomplete(), getOnlyNew(), null, null, getFilter());   
            channelPrefix = redis.getAddress();
            setState(State.Busy);
        }
    }

    @Override
    public void stop() throws InterruptedException {
        paused = false;
        setState(State.Ready);
        if (isStarted()){
            getLogger().fine("Stopping");            
            redis.abort();
            redis.join(0);
        }
        channelPrefix = null;
    }
    
    @Override
    public boolean isStarted() {
        return started.get();
    }
    
    public boolean isPaused(){
        return paused;
    }

    public void pause(){
        paused = true;     
    }

    public void resume(){
        paused = false;
    }

    @Override
    public StreamValue read() throws IOException, InterruptedException {
        assertState(State.Busy);
        boolean wasPaused = paused;
        paused= false;
        try{
            Object cache = take();
            while (true) {
                try {
                    waitValueNot(cache, 10);
                    break;
                } catch (TimeoutException | DeviceTimeoutException ex) {
                    assertState(State.Busy);
                }
            }
            return take();
        } finally{
            paused = wasPaused;
        }
    }

    @Override
    protected boolean hasChanged(Object value, Object former) {
        return value != former;
    }

    @Override
    public StreamValue take() {
        return (StreamValue) super.take();
    }

    @Override
    public StreamValue request() {
        return (StreamValue) super.request();
    }

    
    public List<Readable> getReadables() {
        return readables;
    }

    StreamValue getCurrentValue() {
        StreamValue cache = take();
        if (cache == null) {
            throw new RuntimeException("No stream data");
        }
        return cache;
    }

    public List<String> getIdentifiers() {
        return getCurrentValue().getIdentifiers();
    }

    public List getValues() {

        return getCurrentValue().getValues();
    }

    public Object getValue(String id) {
        return getCurrentValue().getValue(id);
    }

    public Object getValue(int index) {
        return getCurrentValue().getValue(index);
    }
        
    public int[]  getShape(String id) {
         return getCurrentValue().getShape(id);
    }     
    
    public int[]  getShape(int index) {
         return getCurrentValue().getShape(index);
    }        
    
    public Type getType(String id) {
        return getCurrentValue().getType(id);
    }     
     
    public Type getType(int index) {
        return getCurrentValue().getType(index);
    }        

    public static List readChannels(List<String> names, int modulo, int offset, int timeout) throws IOException, InterruptedException {
        StreamRedis stream = new StreamRedis(null);
        try {
            stream.addChannels(names.toArray(new String[0]));
            stream.initialize();
            stream.start();
            stream.waitValueNot(null, timeout);
            return stream.getValues();
        } finally {
            stream.close();
        }
    }

    public Device getChildFromChannelName(String channel) {    
        if (channel!=null){
            for (Device dev: getChildren()){
                if (dev instanceof StreamChannel streamChannel){
                    if (channel.equals(streamChannel.getChannelName())){
                        return dev;
                    }
                }
            }
            for (Device dev: getChildren()){
                if (dev instanceof StreamChannel streamChannel){
                    if (channel.equals(streamChannel.getId())){
                        return dev;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void doClose() throws IOException {        
        try {
            redis.close();
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        channels.clear();        
        super.doClose();
    }

    public class PidReader extends ReadonlyAsyncRegisterBase<Long> {

        PidReader() {
            super("PID");
            setParent(StreamRedis.this);
        }
    };

    PidReader pidReader;

    public PidReader getPidReader() {
        if (pidReader == null) {
            pidReader = new PidReader();
        }
        return pidReader;
    }

    public class TimestampReader extends ReadonlyAsyncRegisterBase<Long> {

        TimestampReader() {
            super("Timestamp");
            setParent(StreamRedis.this);
        }
    };

    TimestampReader timestampReader;

    public TimestampReader getTimestampReader() {
        if (timestampReader == null) {
            timestampReader = new TimestampReader();
        }
        return timestampReader;
    }

    String channelPrefix;

    @Hidden
    public String getChannelPrefix() {
        return channelPrefix;
    }

    @Hidden
    public void setChannelPrefix(String channelPrefix) {
        this.channelPrefix = channelPrefix;
    }    
}
