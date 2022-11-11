package ch.psi.pshell.bs;

import ch.psi.bsread.message.ChannelConfig;
import ch.psi.bsread.message.Type;
import ch.psi.pshell.device.ReadonlyAsyncRegisterBase;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Represents a scalar element in a BS stream.
 */
public class StreamChannel<T> extends ReadonlyAsyncRegisterBase<T> {

    public static final int DEFAULT_MODULO = 1;
    public static final int DEFAULT_OFFSET = 0;

    private String id;
    private int modulo = DEFAULT_MODULO;
    private int offset = DEFAULT_OFFSET;

    public int TIMEOUT_UPDATE = 10000;

    final Object lock = new Object();
    boolean useLocalTimestamp = true;

    private volatile Long pulseId;
    volatile ChannelConfig config;

    @Override
    public StreamChannelConfig getConfig() {
        return (StreamChannelConfig) super.getConfig();
    }

    protected StreamChannel(String name, Stream stream, StreamChannelConfig config) {
        super(name, config);
        setParent(stream);
    }

    public StreamChannel(String name, Stream stream) {
        this(name, stream, new StreamChannelConfig());
    }

    public StreamChannel(String name, Stream stream, String id) {
        this(name, stream, id, 1);
    }

    public StreamChannel(String name, Stream stream, String id, int modulo) {
        this(name, stream, id, modulo, 0);
    }

    public StreamChannel(String name, Stream stream, String id, int modulo, int offset) {
        super(name);
        this.id = id;
        this.modulo = modulo;
        this.offset = offset;
        setParent(stream);
    }

    public void setUseLocalTimestamp(boolean value){
        useLocalTimestamp = value;
    }
    
    public boolean getUseLocalTimestamp(){
        return useLocalTimestamp;
    }   
    
    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        if (getConfig() != null) {
            id = getConfig().id;
            modulo = getConfig().modulo;
            offset = getConfig().offset;
        }
    }

    @Override
    public Stream getParent() {
        return (Stream) super.getParent();
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        boolean started = false;
        if (getParent().getState() == State.Ready) {
            getParent().start(true);
            started = true;
        }
        try {
            Long id = getPulseId();
            long start = System.currentTimeMillis();
            while ((id == getPulseId()) && ((System.currentTimeMillis() - start) < TIMEOUT_UPDATE)) {
                getParent().update();
                Thread.sleep(5);
            }
        } finally {
            if (started) {
                getParent().stop();
            }
        }
    }

    public String getId() {
        return id;
    }

    public int getModulo() {
        return modulo;
    }

    public int getOffset() {
        return offset;
    }

    public Long getPulseId() {
        return pulseId;
    }

    protected void set(long pulseId, long timestamp, long nanosOffset, T value, ChannelConfig config) {
        synchronized (cacheUpdateLock) {
            this.pulseId = pulseId;
            this.config = config;
            setCache(convertFromRead((T) value), timestamp, nanosOffset);
        }
    }
    
    public  ChannelConfig  getChannelConfig() {
        //return getParent().getChannelConfig(id);
        return config;
    }     
    
    @Override
    public int[]  getShape() {
        ChannelConfig  config = getChannelConfig();
        return (config==null) ? null : config.getShape();
    }     
    
    public Type getType() {
        ChannelConfig  config = getChannelConfig();
        return (config==null) ? null : config.getType();
    }         

    @Override
    public Class getElementType(){
        Type type = getType();
        if (type!=null){
            switch (type){
                case Bool:
                    return Boolean.class;
                case String:
                     return String.class;
                case Float64:
                     return Double.class;
                case Float32:
                     return Float.class;
                case Int8:
                     return Byte.class;
                case UInt8:
                     return Short.class;
                case Int16:
                     return Short.class;
                case UInt16:
                     return Integer.class;
                case Int32:
                     return Integer.class;
                case UInt32:
                     return Long.class;
                case Int64:
                     return Long.class;
                case UInt64:
                     return BigInteger.class;
            }
        }
        return super.getElementType();
    }
    
    public Object toImage(Object value){
        int[] shape = getShape();
        if (shape.length!=2){
            return null;
        }
        return Convert.reshape(take(), new int[]{shape[1],shape[0]});
    }
    
    public Object toImage(){
        return toImage(take());
    }
    
    //public double[][] toImage(){
    //    int[] shape = getShape();
    //    if (shape.length!=2){
    //        return null;
    //    }
    //    return (double[][]) Convert.reshape((double[])Convert.toDouble(take()), new int[]{shape[1],shape[0]});
   //}
    public String getChannelName(){
        String prefix = getParent().getChannelPrefix();
        return (prefix==null) ? id : prefix+":"+id;
    } 
    
    @Override
    public BeamSynchronousValue takeTimestamped() {
        synchronized (cacheUpdateLock) {
            TimestampedValue val = super.takeTimestamped();
            return (val == null) ? null : new BeamSynchronousValue(val.getValue(), val.getTimestamp(), val.getNanosOffset(), pulseId);
        }
    }
}
