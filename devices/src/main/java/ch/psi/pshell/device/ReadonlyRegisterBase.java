package ch.psi.pshell.device;

import ch.psi.pshell.logging.LogManager;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.TimestampedValue;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.logging.Level;

/**
 * Base class for readonly registers.
 */
public abstract class ReadonlyRegisterBase<T> extends DeviceBase implements ReadonlyRegister<T> {
    private int precision = UNDEFINED_PRECISION;

    /**
     * Persisted configuration
     */
    protected ReadonlyRegisterBase(String name, RegisterConfig config) {
        super(name, config);
        if (config != null) {
            precision = config.precision;
        }
        if (isReadonlyRegister()) {
            setAccessType(AccessType.Read);
        }

    }
    
    @Override
    public Class getElementType() {
        if (elementType != null) {
            return elementType;
        }
        return ReadonlyRegister.super.getElementType();
    }

    Class elementType;

    public void setElementType(Class type) {
        elementType = type;
    }

    @Override
    public Boolean isElementUnsigned() {
        if (elementUnsigned != null) {
            return elementUnsigned;
        }
        return ReadonlyRegister.super.isElementUnsigned();
    }

    Boolean elementUnsigned;

    public void setElementUnsigned(Boolean value) {
        elementUnsigned = value;
    }

    /*
     * Volatile configuration
     */
    protected ReadonlyRegisterBase() {
        this(null);
    }

    protected ReadonlyRegisterBase(String name) {
        this(name, UNDEFINED_PRECISION);
    }

    protected ReadonlyRegisterBase(String name, int precision) {
        super(name);
        this.precision = precision;
        if (isReadonlyRegister()) {
            setAccessType(AccessType.Read);
        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        if (getConfig() != null) {
            precision = getConfig().precision;
        }
        super.doInitialize();
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        read();
    }

    @Override
    public int getPrecision(){
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    @Override
    public RegisterConfig getConfig() {
        return (RegisterConfig) super.getConfig();
    }

    @Override
    public T read() throws IOException, InterruptedException {
        assertInitialized();
        try {
            assertReadEnabled();
            T cache = take();
            if (updatingCache || asyncUpdate) {
                return cache;
            }
            if (isTrustedMonitor() && isMonitored() && (cache != null)) {
                return cache;
            }
            T readout = (isSimulated())
                    ? convertForWrite(cache)
                    : doRead();
            onReadout(readout);
            if (getState() == State.Offline) {
                setState(State.Ready);
            }
            T ret = take();
            if (getLogger().isLoggable(Level.FINEST)) {
                getLogger().log(Level.FINEST, "Read: {0}", LogManager.getLogForValue(ret));
            }
            return ret;
        } catch (IOException ex) {
            getLogger().log(Level.FINER, null, ex);
            resetCache();
            if (ex instanceof DeviceTimeoutException) {
                setState(State.Offline);
            }
            throw ex;
        }
    }

    protected void onReadout(Object value) {
        setCache(convertFromRead((T) value));
    }

    @Override
    protected void setCache(Object cache, Long timestamp, Long nanosOffset) {
        T value = adjustPrecision((T) cache);
        super.setCache(value, timestamp, nanosOffset);
    }

    protected T convertFromRead(T value) {
        return value;
    }

    protected T convertForWrite(T value) throws IOException {
        return value;
    }

    protected T enforceRange(T value) {
        return value;
    }

    protected T adjustPrecision(T value) {
        if (value != null) {
            int precision = getPrecision();
            if (precision >= 0) {
                if (value instanceof Double aDouble) {
                    value = (T) adjustDouble(aDouble, precision);
                } else if ((value.getClass() == Double[].class) || (value.getClass() == double[].class)) {
                    for (int i = 0; i < Array.getLength(value); i++) {
                        Array.setDouble(value, i, adjustDouble(Array.getDouble(value, i), precision));
                    }
                } else {
                    if (value instanceof List list) {
                        for (int i = 0; i < list.size(); i++) {
                            Object val = list.get(i);
                            if (val instanceof Double d) {
                                list.set(i, adjustDouble(d, precision));
                            }
                        }
                    }
                }
            }
        }
        return value;
    }

    Double adjustDouble(Double value, int precision) {
        return Convert.roundDouble(value, precision);
    }

    public boolean isValidValue(T value) {
        return true;
    }

    private volatile boolean trustedMonitor = false;

    /**
     * If true then read() returns device cache if device is monitored.
     */
    public boolean isTrustedMonitor() {
        return trustedMonitor;
    }

    public void setTrustedMonitor(boolean value) {
        trustedMonitor = value;
    }

    private volatile boolean asyncUpdate = false;

    /**
     * If true then read() returns device cache always
     */
    public boolean isAsyncUpdate() {
        return asyncUpdate;
    }

    public void setAsyncUpdate(boolean value) {
        asyncUpdate = value;
    }

    @Override
    public T take() {
        return (T) super.take();
    }

    @Override
    public T request() {
        super.request();
        return take();
    }
    
    @Override
    public TimestampedValue<T> popBuffer(){
        return (TimestampedValue<T>) super.popBuffer();
    }
    
    @Override
    public T getValue() throws IOException, InterruptedException {
        if (isMonitored()) {
            T value = take();
            if (value != null) {
                return value;
            }
        }
        return read();
    }

    @Override
    public void waitValueInRange(T value, T range, int timeout) throws IOException, InterruptedException {
        if (!(value instanceof Number)) {
            throw new UnsupportedOperationException("Not supported for arrays");
        }
        Number val = (Number) value;
        Number res = (Number) range;
        Chrono chrono = new Chrono();
        while (true) {
            Number cur = (Number) getValue();
            if (Math.abs(cur.doubleValue() - val.doubleValue()) <= Math.abs(res.doubleValue())) {
                return;
            }
            if (timeout >= 0) {
                if (chrono.isTimeout(timeout)) {
                    throw new DeviceTimeoutException("Timeout waiting value: " + value);
                }
            }
            Thread.sleep(getWaitSleep());
        }

    }

    //Abstracts
    abstract protected T doRead() throws IOException, InterruptedException;
}
