package ch.psi.pshell.device;

import ch.psi.pshell.core.LogManager;
import ch.psi.pshell.core.Nameable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import ch.psi.utils.Config;
import ch.psi.utils.NamedThreadFactory;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.State;

/**
 * Base class for GenericDevice implementations.
 */
public abstract class GenericDeviceBase<T> extends ObservableBase<T> implements GenericDevice<T> {

    final String name;
    final Config config;
    final Object stateWaitLock;
    final Object updateWaitLock;
    ScheduledExecutorService schedulerPolling;

    final AtomicBoolean requesting;
    final AtomicBoolean updating;
    final AtomicBoolean closed;
    
    int waitSleep = 5;
    String alias;

    //Construction 
    /**
     * Anonymous device
     */
    protected GenericDeviceBase() {
        this(null);
    }

    /**
     * Device with no persisted configuration
     */
    protected GenericDeviceBase(String name) {
        this(name, null);
    }

    /**
     * Device with configuration
     */
    protected GenericDeviceBase(String name, Config config) {
        if ((name != null) && (name.trim().isEmpty())) {
            name = null;
        }
        this.name = name;
        this.config = config;
        stateWaitLock = new Object();
        updateWaitLock = new Object();
        if ((config != null) && (name != null)) {
            try {
                config.load(getConfigFileName());
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Error creating config file", ex);
            }
        }
        closed = new AtomicBoolean(false);
        updating = new AtomicBoolean(false);
        requesting = new AtomicBoolean(false);
    }

    public final String getConfigFileName() {
        if ((config != null) && (name != null)) {
            return GenericDevice.getConfigFileName(name);
        }
        return null;
    }

    //State
    volatile State state = State.Invalid;

    @Override
    public State getState() {
        return state;
    }

    protected void setState(State state) {
        if (this.state != state) {
            State former = this.state;
            //Only allowing comming back from closing in initialize
            if (former == State.Closing) {
                if (state == State.Initializing) {
                    closed.set(false);
                } else {
                    return;
                }
            }
            this.state = state;
            Level level = (name == null) ? Level.FINEST : Level.FINER; //Anonnymous  devices have lower log level
            getLogger().log(level, "State: " + state);
            triggerStateChanged(state, former);
            synchronized (stateWaitLock) {
                stateWaitLock.notifyAll();
            }
        }
    }

    /**
     * Waits for a condition in a for-sleep loop
     */
    void waitCondition(Condition condition, int timeout, String timeoutMsg) throws IOException, InterruptedException {
        Chrono chrono = new Chrono();
        while (!condition.evaluate()) {
            assertInitialized();
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            if (timeout >= 0) {
                if (chrono.isTimeout(timeout)) {
                    throw new TimeoutException(timeoutMsg, this);
                }
            }
            Thread.sleep(waitSleep); 
        }
    }

    /**
     * Waits for a condition in state change events
     */
    void waitConditionOnLock(Condition condition, int timeout, String timeoutMsg, Object lock) throws IOException, InterruptedException {
        Chrono chrono = new Chrono();
        int wait = Math.max(timeout, 0);
        while (!condition.evaluate()) {
            //Normally interrupt the waiting if the device initializes, but waiting on states are only interrupted if device is closes.
            if(lock ==stateWaitLock){
                assertStateNot(State.Closing);
            } else {
                assertInitialized();
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            synchronized (lock) {
                lock.wait(wait);
            }
            if (wait > 0) {
                wait = timeout - chrono.getEllapsed();
                if (wait <= 0) {
                    throw new TimeoutException(timeoutMsg, this);
                }
            }
        }
    }
    
    void waitConditionOnState(Condition condition, int timeout, String timeoutMsg) throws IOException, InterruptedException {
        waitConditionOnLock(condition, timeout, timeoutMsg, stateWaitLock);
    }

    @Override
    public void waitState(State state, int timeout) throws IOException, InterruptedException {
        waitConditionOnState(() -> getState() == state, timeout, "Timeout waiting state: " + state);
    }

    @Override
    public void waitStateNot(State state, int timeout) throws IOException, InterruptedException {
        waitConditionOnState(() -> getState() != state, timeout, "Timeout waiting state not: " + state);
    }
    
    @Override
    public void waitInitialized(int timeout) throws IOException, InterruptedException{
        waitConditionOnState(() -> { if (getState()==State.Closing){ throw new RuntimeException("Device is closed");}; 
                                     return getState().isInitialized();}, timeout, "Timeout waiting initialized");
    }

    //Initialization
    //Real device access can be done here
    @Override
    final public void initialize() throws IOException, InterruptedException {
        //assertStateNot(State.Closing);
        setState(State.Initializing);
        int pollInterval = this.pollInterval;
        setPolling(0);
        try {
            if (config != null) {
                if (config.getFileName() == null) {
                    if ((name == null)) {
                        getLogger().fine("Annonymous device configuration cannot be persisted");
                    } else {
                        getLogger().warning("Configuration file name not set");
                    }
                }
            }
            doInitialize();
            //If doInitialize has changed the state, keep it.
            if (state == State.Initializing) {
                setState(State.Ready);
            }
        } catch (IOException | InterruptedException ex) {
            getLogger().log(Level.WARNING, null, ex);
            setState(State.Invalid);
            throw ex;
        } catch (Throwable ex) {
            getLogger().log(Level.WARNING, null, ex);
            setState(State.Invalid);
            throw new IOException(ex);
        } finally {
            if (pollInterval != 0) {
                setPolling(pollInterval);
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return (getState().isInitialized());
    }

    boolean simulated;

    @Override
    public boolean isSimulated() {
        return simulated;
    }

    /**
     * Called once after instantiation if device is simulated
     */
    @Hidden
    @Override
    final public void setSimulated() {
        if (!simulated) {
            if (state.isInitialized()) {
                getLogger().severe("Attempt to set simulated after initialization");
                return;
            }
            simulated = true;
            doSetSimulated();
        }
    }

    //Monitoring
    private volatile boolean monitored;

    @Override
    public boolean isMonitored() {
        return monitored;
    }

    @Override
    final public void setMonitored(boolean value) {
        if (value != monitored) {
            monitored = value;
            doSetMonitored(value);
        }
    }

    public void setWaitSleep(int value){
        waitSleep = value;
    }
    
    public int getWaitSleep(){
        return waitSleep;
    }
    
    //Updating
    @Override
    final public void update() throws IOException, InterruptedException {
        if (isInitialized()) {
            if (updating.compareAndSet(false, true)) {
                try {
                    doUpdate();
                } finally {
                    updating.set(false);
                    requesting.set(false);
                    synchronized (updateWaitLock) {
                        updateWaitLock.notifyAll();
                    }
                }
            } else {
                while (updating.get()) {
                    synchronized (updateWaitLock) {
                        updateWaitLock.wait();
                    }
                }
            }
        } else {
            requesting.set(false);
        }
    }

    @Override
    public Object request() {
        if (requesting.compareAndSet(false, true)) {
            updateAsync();
        }
        return take();
    }

    private volatile int pollInterval;

    @Override
    public int getPolling() {
        return Math.abs(pollInterval);
    }

    public boolean isPollingBackground() {
        return pollInterval > 0;
    }

    protected void pollingTask() {
        try {
            if ((isPollingBackground()) || (getListeners().size() > 0)) {
                update();
            }
        } catch (Exception ex) {
            getLogger().log(Level.FINER, null, ex);
        }
    }

    @Override
    public void setPolling(int interval) {
        pollInterval = interval;

        if (schedulerPolling != null) {
            schedulerPolling.shutdown();
            schedulerPolling = null;
        }
        if (state.isInitialized()) {
            if (getPolling() > 0) {
                schedulerPolling = newPollingScheduller(10, getPolling(), () -> {
                    if (isInitialized()) {
                        pollingTask();
                    }
                });
            }
        }
    }

    private volatile AccessType accessMode;

    @Hidden
    @Override
    /**
     * Write once: If set by pool then cannot be changed later.
     */
    public void setAccessType(AccessType mode) {
        if (accessMode == null) {
            accessMode = mode;
        }
    }

    @Override
    public AccessType getAccessType() {
        if (accessMode == null) {
            return AccessType.ReadWrite;
        }
        return accessMode;
    }

    /**
     * Default implementation creates a private thread to poll the device, with fixed delay.
     */
    protected ScheduledExecutorService newPollingScheduller(long delay, int interval, Runnable r) {
        ScheduledExecutorService ret = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("Polling scheduler: " + getName()));
        ret.scheduleWithFixedDelay(r, delay, interval, TimeUnit.MILLISECONDS);
        return ret;
    }

    void onTimerUpdate() {
    }

    //Closing
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    final public void close() {
        if (closed.compareAndSet(false, true)) {
            setState(State.Closing);
            setPolling(0);//Stop polling timer
            super.close(); //Done after setState in order the listener to receive the close event
            try {
                doClose();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
            if (config != null) {
                config.close();
            }
        }
    }

    //State
    @Hidden
    public void assertInitialized() throws StateException {
        if (!isInitialized()) {
            throw new StateException(isClosed() ? "Closed" : "Not initialized", this);
        }
    }

    @Hidden
    public void assertNotInitialized() throws StateException {
        if (isInitialized()) {
            throw new StateException("Already initialized", this);
        }
    }

    @Hidden
    public void assertState(State state) throws StateException {
        if (this.getState() != state) {
            throw new StateException(this);
        }
    }

    @Hidden
    public void assertStateNot(State state) throws StateException {
        if (this.getState() == state) {
            throw new StateException(this);
        }
    }

    //Properties
    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        return GenericDevice.super.getName();
    }

    @Override
    public void setAlias(String alias) {
        this.alias=alias;
    }

    @Override
    public String getAlias() {
        if ((alias!=null)&&(!alias.isBlank())) {
            return alias;
        }
        return getName();
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public String toString() {
        String className = Nameable.getShortClassName(getClass());
        return (getName() == null) ? className : className + " " + getName();
    }

    //Named devices have one logger
    protected Logger getLogger() {
        return Logger.getLogger(LogManager.ROOT_LOGGER + "." + getName());
    }

    //Device Exceptions
    //TODO: Generic classes can not extend throwable: cannot have a class Exception as in Device
    public static class TimeoutException extends IOException {

        TimeoutException(String message, GenericDeviceBase obj) {
            super(message + " [" + obj.getName() + "]");
        }
    }

    public static class StateException extends IOException {

        StateException(GenericDeviceBase obj) {
            this("Invalid state: " + obj.getState(), obj);
        }

        StateException(String message, GenericDeviceBase obj) {
            super(message + " [" + obj.getName() + "]");
        }
    }

    //Overridables
    protected void doClose() throws IOException {

    }

    protected void doInitialize() throws IOException, InterruptedException {
    }

    protected void doSetMonitored(boolean value) {
    }

    /**
     * Only called once
     */
    protected void doSetSimulated() {

    }

    //Updates variables from HW
    protected void doUpdate() throws IOException, InterruptedException {

    }

    protected void triggerStateChanged(State state, State former) {
    }

}
