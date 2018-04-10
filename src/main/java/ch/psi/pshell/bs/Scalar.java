package ch.psi.pshell.bs;

import ch.psi.pshell.device.ReadonlyAsyncRegisterBase;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.utils.State;
import java.io.IOException;

/**
 * Represents a scalar element in a BS stream.
 */
public class Scalar<T> extends ReadonlyAsyncRegisterBase<T> {

    public static final int DEFAULT_MODULO = 10;
    public static final int DEFAULT_OFFSET = 0;

    private String id;
    private int modulo = DEFAULT_MODULO;
    private int offset = DEFAULT_OFFSET;
    

    public int TIMEOUT_UPDATE = 10000;

    final Object lock = new Object();
    boolean useLocalTimestamp = true;

    private volatile Long pulseId;

    @Override
    public ScalarConfig getConfig() {
        return (ScalarConfig) super.getConfig();
    }

    protected Scalar(String name, Stream stream, ScalarConfig config) {
        super(name, config);
        setParent(stream);
    }

    public Scalar(String name, Stream stream) {
        this(name, stream, new ScalarConfig());
    }

    public Scalar(String name, Stream stream, String id) {
        this(name, stream, id, 1);
    }

    public Scalar(String name, Stream stream, String id, int modulo) {
        this(name, stream, id, modulo, 0);
    }

    public Scalar(String name, Stream stream, String id, int modulo, int offset) {
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

    void set(long pulseId, long timestamp, long nanosOffset, T value) {
        synchronized (cacheUpdateLock) {
            this.pulseId = pulseId;
            setCache(convertFromRead((T) value), timestamp, nanosOffset);
        }
    }
    
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
