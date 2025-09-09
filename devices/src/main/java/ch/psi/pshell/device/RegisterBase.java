package ch.psi.pshell.device;

import ch.psi.pshell.logging.Logging;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Base class for registers (device containing a raw numeric or array value).
 */
public abstract class RegisterBase<T> extends ReadonlyRegisterBase<T> implements Register<T> {

    /**
     * Persisted configuration
     */
    protected RegisterBase(String name, RegisterConfig config) {
        super(name, config);
    }   

    /*
     * Volatile configuration
     */
    protected RegisterBase() {
        this(null);
    }

    protected RegisterBase(String name) {
        this(name, UNDEFINED_PRECISION);
    }

    protected RegisterBase(String name, int precision) {
        super(name, precision);
    }

    protected void initializeAccessType() {
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
                //if (!isMonitored()) {
                    request();  //Update cache asynchronously
                //}
            }
            if (getState() == State.Offline) {
                setState(State.Ready);
            }
            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.FINER, "Write: {0}", Logging.getLogForValue(value));
            }
        } catch (IOException ex) {
            getLogger().log(Level.FINE, null, ex);
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
    public boolean isTrustedWrite() {
        return trustedWrite;
    }

    public void setTrustedWrite(boolean value) {
        trustedWrite = value;
    }

    /**
     * This is called in write(). Derived classes can wait for conditions to
     * validate the write.
     */
    protected void waitSettled() throws IOException, InterruptedException {
        if (settlingCondition != null) {
            settlingCondition.waitSettled();
        }
    }

    SettlingCondition settlingCondition;

    public void setSettlingCondition(SettlingCondition settlingCondition) {
        this.settlingCondition = settlingCondition;
        if (settlingCondition!=null){
            settlingCondition.register = this;
        }
    }

    public SettlingCondition getSettlingCondition() {
        return settlingCondition;
    }
    
    abstract protected void doWrite(T value) throws IOException, InterruptedException;
}
