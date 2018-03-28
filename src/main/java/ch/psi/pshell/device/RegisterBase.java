package ch.psi.pshell.device;

import ch.psi.pshell.core.LogManager;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.State;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for registers (device containing a raw numeric or array value).
 */
public abstract class RegisterBase<T> extends DeviceBase implements Register<T> {

    private int precision = -1;

    /**
     * Persisted configuration
     */
    protected RegisterBase(String name, RegisterConfig config) {
        super(name, config);
        if (config != null) {
            precision = config.precision;
        }
    }

    /*
     * Volatile configuration
     */
    protected RegisterBase() {
        this(null);
    }

    protected RegisterBase(String name) {
        this(name, -1);
    }

    protected RegisterBase(String name, int precision) {
        super(name);
        this.precision = precision;
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
    
    /**
     * This is called in write(). Derived classes can wait for conditions to validate the write.
     */
    protected void waitSettled() throws IOException, InterruptedException {
        if (settlingCondition!=null){
            settlingCondition.waitSettled();
        }
    }   

    SettlingCondition settlingCondition;
    
    public void setSettlingCondition(SettlingCondition settlingCondition){
        this.settlingCondition =  settlingCondition;
        settlingCondition.register = this;
    }    
    
    public SettlingCondition getSettlingCondition(){
        return settlingCondition;
    }    
    
    @Override
    public int getPrecision() {
        return precision;
    }

    @Hidden
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
        if (updatingCache){
            return take();
        }
        Logger logger = getLogger();
        try {
            assertReadEnabled();
            T cache = take();
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
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Read: " + LogManager.getLogForValue(ret));
            }
            return ret;
        } catch (IOException ex) {
            logger.log(Level.FINER, null, ex);
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
                if (value instanceof Double) {
                    value = (T) adjustDouble((Double) value, precision);
                } else if ((value.getClass() == Double[].class) || (value.getClass() == double[].class)) {
                    for (int i = 0; i < Array.getLength(value); i++) {
                        Array.setDouble(value, i, adjustDouble(Array.getDouble(value, i), precision));
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

    @Override
    public void assertValidValue(T value) throws IllegalArgumentException {
        if (!isValidValue(value)) {
            throw new InvalidValueException(value);
        }
    }

    @Override
    public void write(T value) throws IOException, InterruptedException {
        assertInitialized();
        assertValidValue(value);
        triggerValueChanging(value);
        Logger logger = getLogger();
        try {
            assertWriteEnabled();
            value = enforceRange(value);
            value = adjustPrecision(value);
            T setValue = convertForWrite(value);
            if (!isSimulated()) {
                doWrite(setValue);
            }
            if (isTrustedWrite() || isSimulated()) {
                setCache(value);
            } else {
                if (!isMonitored()) {
                    request();  //Cache has not been updated
                }
            }
            if (getState() == State.Offline) {
                setState(State.Ready);
            }
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Write: " + LogManager.getLogForValue(value));
            }            
        } catch (IOException ex) {
            logger.log(Level.FINE, null, ex);
            resetCache();
            request();
            if (ex instanceof DeviceTimeoutException) {
                setState(State.Offline);
            }
            throw ex;
        }
        waitSettled();
    }

    private volatile boolean trustedWrite = true;

    /**
     * If write operation returns ok then cache is updated,
     */
    @Hidden
    public boolean isTrustedWrite() {
        return trustedWrite;
    }

    @Hidden
    public void setTrustedWrite(boolean value) {
        trustedWrite = value;
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
            Thread.sleep(10);
        }

    }

    //Abstracts
    abstract protected T doRead() throws IOException, InterruptedException;

    abstract protected void doWrite(T value) throws IOException, InterruptedException;

}
