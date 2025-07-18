package ch.psi.pshell.bs;

import ch.psi.bsread.Receiver;
import ch.psi.bsread.ReceiverConfig;
import ch.psi.bsread.analyzer.MainHeaderAnalyzer;
import ch.psi.bsread.converter.MatlabByteConverter;
import ch.psi.bsread.message.ChannelConfig;
import ch.psi.bsread.message.Message;
import ch.psi.bsread.message.Type;
import ch.psi.bsread.message.ValueImpl;
import ch.psi.pshell.bs.ProviderConfig.SocketType;
import static ch.psi.pshell.bs.StreamChannel.DEFAULT_MODULO;
import static ch.psi.pshell.bs.StreamChannel.DEFAULT_OFFSET;
import ch.psi.pshell.bs.StreamConfig.Incomplete;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.ReadonlyAsyncRegisterBase;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Filter;
import ch.psi.pshell.utils.Filter.FilterCondition;
import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Threading;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.zeromq.ZMQ;

/**
 * A device implementing a beam synchronous string, having, for each identifier,
 * a corresponding Scalar or Waveform child.
 */
public class Stream extends DeviceBase implements StreamDevice {
    
    public static final int TIMEOUT_START_STREAMING = 10000;

    Thread thread;
    final Map<String, StreamChannel> channels;
    final List<String> channelNames;
    final List<Readable> readables;
    static MatlabByteConverter converter;
    volatile boolean reading;
    volatile AtomicBoolean started = new AtomicBoolean(false);
    volatile AtomicBoolean closing = new AtomicBoolean(false);
    Receiver receiver;
    Boolean fixedChildren;
    final Boolean privateProvider;
    boolean createMatrix;
    MainHeaderAnalyzer  mainHeaderAnalyzer;
    Filter filter = null;

    @Override
    public StreamConfig getConfig() {
        return (StreamConfig) super.getConfig();
    }

    public String getAddress() {
        try{
            return ((Provider) getParent()).getAddress();
        } catch (Exception ex){
            return null;
        }
    }

    public String getStreamSocket() {
        try{
            return ((Provider) getParent()).getStreamSocket(this);
        } catch (Exception ex){
            return null;
        }
    }

    public boolean getAnalizeHeader() {
        try{
            return ((Provider) getParent()).getAnalizeHeader();
        } catch (Exception ex){
            return false;
        }
    }

    public void setAnalizeHeader(boolean value) {
        try{
            ((Provider) getParent()).setAnalizeHeader(value);
        } catch (Exception ex){            
        }
    }

    
    public int getSocketType() {
        try{
            return ((Provider) getParent()).getSocketType();
        } catch (Exception ex){
            return -1;
        }            
    }

    public String getSocketTypeStr() {
        return switch (getSocketType()) {
            case ZMQ.PULL -> SocketType.PULL.toString();
            case ZMQ.SUB -> SocketType.SUB.toString();
            default -> "";
        };
    }

    public Stream(String name, boolean persisted) {
        this(name, (Provider) null, persisted);
    }

    @Override
    public Class _getElementType() {
        return Long.class;
    }

    /**
     * Direct URL
     */    
    public Stream(String name, String address, boolean pull) {
        this(name, address, pull ? SocketType.PULL : ProviderConfig.SocketType.SUB);
    }

    public Stream(String name, String address, SocketType socketType) {
        this(name, null, address, socketType, false);
    }    

    public Stream(String name, String address, boolean pull, Incomplete incomplete) {
        this(name, address, pull);
        setIncomplete(incomplete);
    }

    public Stream(String name, String address, SocketType socketType, Incomplete incomplete) {
        this(name, address, socketType);
        setIncomplete(incomplete);
    }    

    
    /**
     * If provider is null then uses default provider.
     */
    public Stream(String name, Provider provider, boolean persisted) {
        this(name, provider, null, null, persisted);
    }

    protected Stream(String name, Provider provider, String address, SocketType socketType, boolean persisted) {
        super(name, persisted ? new StreamConfig() : null);
        if (converter == null) {
            converter = new MatlabByteConverter();
        }
        if (provider == null) {
            if (address == null) {
                provider = Provider.getOrCreateDefault();
                privateProvider = (provider != Provider.getDefault());
            } else {
                provider = new Provider(name + "_provider", address, socketType);
                try {
                    provider.initialize();
                } catch (Exception ex) {
                    Logger.getLogger(Stream.class.getName()).log(Level.SEVERE, null, ex);
                }
                privateProvider = true;
            }
        } else {
            privateProvider = false;
        }

        setParent(provider);
        channels = new HashMap<>();
        channelNames = new ArrayList<>();
        readables = new ArrayList<>();
        setMonitored(true);
        if (persisted) {
            setFilter(getConfig().filter);
            ArrayList<StreamChannelConfig> cfg = getConfig().getChannels();
            for (StreamChannelConfig cc : cfg) {
                if (cc != null) {
                    if (cc.id.startsWith("[")) {
                        addChild(new Waveform(cc.id.substring(1), this, cc.id.substring(1), cc.precision, cc.offset));
                    } else {
                        addChild(new Scalar(cc.id, this, cc.id, cc.precision, cc.offset));
                    }
                }
            }
        }
    }

    public Stream(String name) {
        this(name, (Provider) null);
    }

    public Stream(String name, Provider provider) {
        this(name, provider, false);
    }

    public Stream(String name, String filter) {
        this(name, null, filter);
    }

    public Stream(String name, Provider provider, String filter) {
        this(name, provider);
        setFilter(filter);
    }

    public Stream(String name, Provider provider, String filter, Incomplete incomplete) {
        this(name, provider, filter);
        setIncomplete(incomplete);
    }

    public Stream(String name, StreamChannel... channels) {
        this(name, null, channels);
    }

    public Stream(String name, Provider provider, StreamChannel... channels) {
        this(name, provider, false);
        setChildren(channels);
    }

    public Stream(String name, Provider provider, Incomplete incomplete, StreamChannel... channels) {
        this(name, provider, channels);
        setIncomplete(incomplete);
    }

    public Stream(String name, Provider provider, String filter, Incomplete incomplete, StreamChannel... channels) {
        this(name, provider, incomplete, channels);
        setFilter(filter);
    }
    
    //Constructor for merger classes
    public Stream(String name, Stream... components) {
        super(name);
        channels = new HashMap<>();
        channelNames = new ArrayList<>();
        readables = new ArrayList<>();        
        privateProvider=null;
        setComponents(components);
    }    

    public Incomplete mappingIncomplete;

    public void setIncomplete(String mappingIncomplete) {
        setIncomplete(Incomplete.valueOf(mappingIncomplete));
    }

    public void setIncomplete(Incomplete mappingIncomplete) {
        this.mappingIncomplete = mappingIncomplete;
    }

    public Incomplete getIncomplete() {
        if (mappingIncomplete != null) {
            return mappingIncomplete;
        }
        if (getConfig() != null) {
            return getConfig().mappingIncomplete;
        }
        return null;
    }

    public boolean getCreateMatrix(){
        return createMatrix;
    }
    
    public void setCreateMatrix(boolean value){
        createMatrix = value;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        stop();        
        if((fixedChildren!=null) &&  (!fixedChildren)){
            setChildren(new Device[0]);
        }
        channels.clear();
        channelNames.clear();
        readables.clear();
        readables.add(this);
        for (Device dev : getChildren()) {
            if (dev instanceof StreamChannel streamChannel) {
                appendChild(streamChannel);
            }
        }
        super.doInitialize();
        //fixed by first initialize
        if (fixedChildren == null) {
            fixedChildren = Arr.containsClass(getChildren(), StreamChannel.class);
        }
    }

    void appendChild(StreamChannel c) {
        channels.put(c.getId(), c);
        channelNames.add(c.getId());
        readables.add(c);
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        if (getState() == State.Busy) {
            if (!isPaused()){
                read();
            }
        } else if (getState() == State.Ready) {
            start(false);
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

    
    public StreamChannel addChannel(String id) throws IOException, InterruptedException {
        return addChannel(id, id);
    }
    
    public StreamChannel addChannel(String name, String id) throws IOException, InterruptedException {
           return addChannel(name, id, DEFAULT_MODULO, DEFAULT_OFFSET);
    }
     
    public StreamChannel addChannel(String name, String id, int modulo, int offset) throws IOException, InterruptedException {
        synchronized (lock) {
            assertStateNot(State.Busy);
            StreamChannel channel = new StreamChannel(name, this, id, modulo, offset);
            addChild(channel);
            if (isInitialized()) {
                doInitialize();
            }
            return channel;
        }
    }
    
    public StreamChannel addScalar(String id) throws IOException, InterruptedException {
        return addScalar(id, id);
    }
    
    public StreamChannel addScalar(String name, String id) throws IOException, InterruptedException {
           return addScalar(name, id, DEFAULT_MODULO, 0);
    }
     
    public StreamChannel addScalar(String name, String id, int modulo, int offset) throws IOException, InterruptedException {
        synchronized (lock) {
            assertStateNot(State.Busy);
            StreamChannel scalar = new Scalar(name, this, id, modulo, offset);
            addChild(scalar);
            if (isInitialized()) {
                doInitialize();
            }   
            return scalar;
        }
    }

    public StreamChannel addWaveform(String id) throws IOException, InterruptedException {
        return addWaveform(id, id);
    }
        
    public StreamChannel addWaveform(String name, String id) throws IOException, InterruptedException {
           return addWaveform(name, id, DEFAULT_MODULO, DEFAULT_OFFSET);
    }
    
    public Waveform addWaveform(String name, String id, int modulo, int offset) throws IOException, InterruptedException {
        return addWaveform(name, id, modulo, offset, SIZE_MAX);
    }

    public StreamChannel addWaveform(String name, String id, int size) throws IOException, InterruptedException {
        return addWaveform(name, id, DEFAULT_MODULO, DEFAULT_OFFSET, size);
    }
    
    public Waveform addWaveform(String name, String id, int modulo, int offset, int size) throws IOException, InterruptedException {
        synchronized (lock) {
            assertStateNot(State.Busy);
            Waveform waveform = new Waveform(name, this, id, modulo, offset, size);
            addChild(waveform);
            if (isInitialized()) {
                doInitialize();
            }
            return waveform;
        }
    }

    public StreamChannel addMatrix(String id) throws IOException, InterruptedException {
        return addMatrix(id, id);
    }
    
    public Matrix addMatrix(String name, String id) throws IOException, InterruptedException {
        return addMatrix(name, id, DEFAULT_MODULO, DEFAULT_OFFSET);
    }
    
    public Matrix addMatrix(String name, String id, int modulo, int offset) throws IOException, InterruptedException {
        return addMatrix(name, id, modulo,offset, SIZE_MAX, SIZE_MAX);
    }
    
    
    public Matrix addMatrix(String name, String id, int modulo, int offset, int width, int height) throws IOException, InterruptedException {
        synchronized (lock) {
            assertStateNot(State.Busy);
            Matrix matrix = new Matrix(name, this, id, modulo, offset, width, height);
            addChild(matrix);
            if (isInitialized()) {
                doInitialize();
            }
            return matrix;
        }
    }

    void receiverTask() {
        try {
            getLogger().finer("Entering reveiver task");

            int minModulo = 100;
            for (String channel : channels.keySet()) {
                minModulo = Math.min(minModulo, channels.get(channel).getModulo());
            }

            if (filter!=null){
                for (Filter.FilterCondition filterCondition : filter.getConditions()) {
                    if (!Arr.containsEqual(channels.keySet().toArray(), filterCondition.id)) {
                        getLogger().log(Level.INFO, "Adding filter condition id to stream: {0}", filterCondition.id);
                        channels.put(filterCondition.id, new Scalar(filterCondition.id, this, filterCondition.id, minModulo, DEFAULT_OFFSET));
                    }
                }
            }

            ((Provider) getParent()).createStream(this);
            onStart();
            ReceiverConfig config = ((Provider) getParent()).getReceiverConfig(this);
            ArrayList<ch.psi.bsread.configuration.Channel> channelsConfig = new ArrayList<>();
            for (String channel : channels.keySet()) {
                StreamChannel c = channels.get(channel);
                ch.psi.bsread.configuration.Channel channelConfig = new ch.psi.bsread.configuration.Channel(
                        c.getId(), 
                        (c.getModulo() <=0 ) ? 1 : c.getModulo(), 
                        (c.getOffset() < 0 ) ? 0 : c.getOffset() );
                channelsConfig.add(channelConfig);
            }
            config.setRequestedChannels(channelsConfig);
            getLogger().log(Level.FINE, "Connecting to: {0} ({1})", new Object[]{config.getAddress(), config.getSocketType()});
            receiver = new Receiver(config);
            receiver.connect();
            if (getAnalizeHeader()){
                mainHeaderAnalyzer = new MainHeaderAnalyzer( config.getAddress()) {
                     protected void warn(String log, Object... args){
                         try{
                            int i = 0;
                            while(log.contains("{}")) {
                                log = log.replaceFirst(Pattern.quote("{}"), "{"+ i++ +"}");
                            }                             
                            Logger.getLogger(Stream.class.getName()).severe(MessageFormat.format(log, args));
                         } catch (Exception ex){                                 
                         }
                     }
                };
            }

            while (!Thread.currentThread().isInterrupted() && started.get()) {
                Message msg = receiver.receive();
                if (msg == null) {
                    started.set(false);
                } else {
                    if (mainHeaderAnalyzer!=null){
                        if (!mainHeaderAnalyzer.analyze(msg.getMainHeader())){
                            continue;
                        }
                    }
                    
                    if (isMonitored() || reading) {
                        Map<String, ValueImpl> data = msg.getValues();
                        if (data != null) {
                            if ((filter==null) || (filter.check(data))) {
                                reading = false;
                                long pulseId = msg.getMainHeader().getPulseId();
                                long timestamp = msg.getMainHeader().getGlobalTimestamp().getAsMillis();
                                long nanosOffset = msg.getMainHeader().getGlobalTimestamp().getNs() % 1000000L;
                                Map<String, ChannelConfig> channelConfig = msg.getDataHeader().getChannelsMapping();
                                onMessage(pulseId, timestamp, nanosOffset, data, channelConfig);
                            }
                        }
                    }
                }
            }
            if (started.get()) {
                getLogger().finer("Receiver thread was interrupted");
            } else {
                getLogger().finer("Receiver was closed");
            }
            onStop(null);
        } catch (Throwable ex) {
            getLogger().log(Level.FINE, null, ex);
            onStop(ex);
        } finally {
            reading = false;
            closeReceiver();
            closeStream();
            getLogger().fine("Quitting receiver task");
            setState(State.Ready);
        }
    }

    public void start(Boolean async) {
        if (async != null) {
            setMonitored(async);
        }
        start();
    }

    volatile boolean async;
    @Override
    public void start() {        
        if (started.compareAndSet(false, true)) {
            async = isMonitored();  
            thread = new Thread(() -> {
                receiverTask();
            });
            thread.setName("Stream receiver: " + getName());
            thread.setDaemon(true);
            setState(State.Busy);
            thread.start();
            channelPrefix = ((Provider) getParent()).getAddress();
        }
    }

    @Override
    public void stop() {
        boolean paused = false;
        if (isStarted()){
            paused = isPaused();
            getLogger().fine("Stopping");            
            started.set(false);                    
        }
        channelPrefix = null;
        closeReceiver();
        if (thread != null) {
            try {
                long start = System.currentTimeMillis();
                while ((thread != null) && (thread.isAlive())) {
                    if (System.currentTimeMillis() - start > 1000) {
                        getLogger().log(Level.WARNING, "Receiver did't quit: interrupting thread");
                        //TODO: Killing thread because it blocks  if no message is received
                        Threading.stop(thread, true, 2000);
                        break;
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException ex) {
                //TODO: Filtering InterruptedException. But stop() should not throw InterruptedException;
                getLogger().log(Level.WARNING, null, ex);
            }
            thread = null;
        }
        if (paused){
            setMonitored(true); //Restore initial config for async
        }        
    }
    
    @Override
    public boolean isStarted() {
        return started.get();
    }
    
    //Pause commands only valid for async streams
    public boolean isPaused(){
        return isStarted() && async && !isMonitored();
    }

    public void pause(){
        if (isStarted() && async){
            setMonitored(false);
        }                
    }

    public void resume(){
        if (isStarted() && async){
            setMonitored(true);
        }                
    }

    @Hidden
    public Receiver getReceiver(){
        return receiver;
    }
    
    void closeReceiver() {
        if (closing.compareAndSet(false, true)) {
            try {
                if (receiver != null) {
                    getLogger().log(Level.FINE, "Closing receiver");
                    try {
                        receiver.close();
                    } catch (Exception ex) {
                        getLogger().log(Level.WARNING, null, ex);
                    }
                    receiver = null;
                }
            } finally {
                closing.compareAndSet(true, false);
            }
        }
    }

    void closeStream() {
        try {
            ((Provider) getParent()).closeStream(this);
        } catch (Exception ex) {
            getLogger().log(Level.FINE, null, ex);
        }
    }

    protected void onStart() {
    }

    protected void onStop(Throwable ex) {
    }

    @Override
    public StreamValue read() throws IOException, InterruptedException {
        try {
            return read(-1);
        } catch (java.util.concurrent.TimeoutException ex) {
            //Cannot happen
            Logger.getLogger(Stream.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    
    public StreamValue read(int timeout) throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        assertState(State.Busy);
        Object cache = take();
        long start = System.currentTimeMillis();
        reading = true;
        while (true) {
            try {
                waitValueNot(cache, 100);
                break;
            } catch (TimeoutException | DeviceTimeoutException ex) {
                assertState(State.Busy);
                if (timeout > 0) {
                    if ((System.currentTimeMillis() - start) > timeout) {
                        throw new java.util.concurrent.TimeoutException();
                    }                                        
                }                
            }
        }
        return take();
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

    public void setFilter(String filter){
        this.filter = ((filter==null)||(filter.isBlank())) ? null : new Filter(filter) {
            @Override
            //have to redefine the check method to get the value from ValueImpl
            public boolean check(Map<String, ? extends Object> data) {
                try {
                    for (FilterCondition filterCondition : getConditions()) {
                        ValueImpl v = (ValueImpl) data.get(filterCondition.id);
                        Comparable val = (Comparable) v.getValue();
                        if (!filterCondition.check(val)) {
                            return false;
                        }
                    }
                } catch (Exception ex) {
                    return false;
                }
                return true;
             }

        };
    }    
    
    public String getFilter(){
        return (filter==null) ? null : filter.get();
    }
       
    
    LinkedHashMap<String, Object> streamCache = new LinkedHashMap<>();

    protected void onMessage(long pulse_id, long timestamp, long nanosOffset, Map<String, ValueImpl> data, Map<String, ChannelConfig> config) {
        boolean fill_null = (getIncomplete() == Incomplete.fill_null) && fixedChildren;

        if (!fixedChildren) {
            streamCache.clear();
        } else if (fill_null) {
            for (String name : channelNames) {
                streamCache.put(name, null);
            }
        }

        for (String channel : config.keySet()) {
            StreamChannel c = channels.get(channel);
            ChannelConfig cfg = config.get(channel);
            ValueImpl v = data.get(channel);
            Object val = (v==null) ? null : v.getValue();
            long devTimestamp = timestamp;
            long devNanosOffset = nanosOffset;
            try {
                if ((!fixedChildren) && (c == null)) {
                    int[] shape = cfg.getShape();
                    Type type = cfg.getType();
                    
                    if ((shape==null) || (shape.length==0)){
                        c=new Scalar(channel, this, channel);
                    } else if ((shape.length==1) && (shape[0]<=1)){
                        c=new Scalar(channel, this, channel);
                    } else if ((shape.length==2) && (getCreateMatrix())){
                        c = new Matrix(channel, this, channel);
                    } else {
                        c = new Waveform(channel, this, channel);
                    }
                    appendChild(c);
                    try {
                        c.initialize();
                    } catch (Exception ex) {
                    }
                }
                if (c != null) {
                    if (v!=null) {
                        if (c.getUseLocalTimestamp() && (v.getTimestamp() != null)) {
                            devTimestamp = v.getTimestamp().getAsMillis();
                            devNanosOffset = v.getTimestamp().getNs() % 1000000L;
                        }
                    }
                    c.set(pulse_id, devTimestamp, devNanosOffset, val, cfg);
                }
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
                if (c != null) {
                    c.set(pulse_id, devTimestamp, devNanosOffset, null, cfg);
                }
            }
            if (debug) {
                System.out.println(channel + ": " + Str.toString(val, 100));
            }
            if (!fixedChildren) {
                streamCache.put(channel, c.take());
            } else if (fill_null) {
                if (channelNames.contains(channel)) {
                    streamCache.put(channel, c.take());
                }
            }
        }

        if (pidReader != null) {
            setCache((DeviceBase) pidReader, (Object) pulse_id, timestamp, nanosOffset);
        }
        if (timestampReader != null) {
            setCache((DeviceBase) timestampReader, (Object) timestamp, timestamp, nanosOffset);
        }

        
        List<String> identifiers = channelNames;
        List values;
        if (fixedChildren) {
            //If ids are declared, value list is fixed and ordered 
            if (getIncomplete() == Incomplete.fill_null) {
                //Null values are cached
                values = new ArrayList(streamCache.values());
            } else {
                //If received null value caches last value
               values = Arrays.asList(getChildrenValues());
            }
        } else {
            //Else caches everything received            
            identifiers =  new ArrayList(streamCache.keySet());
            values =  new ArrayList(streamCache.values());
        }
        
        StreamValue streamValue;
        if (Sys.hasJython()){
            streamValue=new StreamValueSubscriptable(pulse_id, timestamp, nanosOffset, identifiers, values,config);
        } else {
            streamValue=new StreamValue(pulse_id, timestamp, nanosOffset, identifiers, values,config);
        }
        setCache(streamValue, timestamp, nanosOffset);
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
    
    public  ChannelConfig  getChannelConfig(String id) {
         return getCurrentValue().getChannelConfig(id);
    }     
    
    public  ChannelConfig  getChannelConfig(int index) {
         return getCurrentValue().getChannelConfig(index);
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
        Stream stream = new Stream(null);
        try {
            for (String name : names) {
                stream.addScalar(name, name, modulo, offset);
            }
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
        stop();
        channels.clear();
        if (privateProvider) {
            try {
                getParent().close();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
        super.doClose();
    }

    public class PidReader extends ReadonlyAsyncRegisterBase<Long> {

        PidReader() {
            super("PID");
            setParent(Stream.this);
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
            setParent(Stream.this);
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
    
    //Used to implemente visualization playback when dispaly is paused in CamServerViewer
    @Hidden
    public void setValue(StreamValue value){
         for (String channel : value.getIdentifiers()) {
            try {
                StreamChannel c = channels.get(channel);
                ChannelConfig cfg = value.getChannelConfig(channel);
                Object val = value.getValue(channel);
                long devTimestamp = value.getTimestamp();
                long devNanosOffset = value.getTimestampNanos();
                long pulse_id = value.getPulseId();

                if (c != null) {
                    c.set(pulse_id, devTimestamp, devNanosOffset, val, cfg);
                }
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
            }
        }
         setCache(value);
    }

}
