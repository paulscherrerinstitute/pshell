package ch.psi.pshell.device;

import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import ch.psi.utils.NamedThreadFactory;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * The base class for Device implementations
 */
public abstract class DeviceBase extends GenericDeviceBase<DeviceListener> implements Device {

    static ScheduledExecutorService schedulerSimulation;
    final protected Object valueWaitLock = new Object();
    final protected Object cacheUpdateLock = new Object();

    //Construction 
    /**
     * Anonymous device
     */
    protected DeviceBase() {
        super();
    }

    /**
     * Device with no persisted configuration
     */
    protected DeviceBase(String name) {
        super(name);
    }

    /**
     * Device with configuration
     */
    protected DeviceBase(String name, DeviceConfig config) {
        super(name, config);
    }

    //State
    
    /**
     * This method does not return if state is ready, but should return true if last command execution is finished.
     * In this way waitReady can be used to synchronize sequential commands
     * By default returns if state is not a processing state (Initializing, Paused, or Busy);
     */
    @Override
    public boolean isReady() throws IOException, InterruptedException {
        return !getState().isProcessing();
    }

    /**
     * Waits for last command execution to finish (isRead()==true).
     */
    @Override
    public void waitReady(int timeout) throws IOException, InterruptedException {
        //For devices with default isReady implementation, waitStateNotProcessing() is preferable
        //because avoid the for-sleep loop.
        boolean isReadyDefault = false;
        try {
            isReadyDefault = (getClass().getMethod("isReady").getDeclaringClass() == DeviceBase.class);
        } catch (Exception ex) {
        }

        if (isReadyDefault) {
            waitConditionOnState(() -> !getState().isProcessing(), timeout, "Timeout waiting ready");
        } else {
            waitCondition(() -> {
                try {
                    return isReady();
                } catch (Exception ex) {
                    return false;
                }
            }, timeout, "Timeout waiting ready");
        }
    }

    @Override
    /**
     * Unlike getState().isInitialized() and super.isInitialized() returns false if parent
     * initialization failed.
     */
    public boolean isInitialized() {
        Device parent = getParent();
        while (parent != null) {
            State parent_state = parent.getState();
            if ((parent_state == State.Invalid) || (parent_state == State.Closing)) {
                return false;
            }
            parent = parent.getParent();
        }
        return super.isInitialized();
    }

    /**
     * Waits for a condition in value change events
     */
    void waitConditionOnValue(Condition condition, int timeout, String timeoutMsg) throws IOException, InterruptedException {
        waitConditionOnLock(condition, timeout, timeoutMsg, valueWaitLock);
    }

    void waitConditionOnCache(Condition condition, int timeout, String timeoutMsg) throws IOException, InterruptedException {
        waitConditionOnLock(condition, timeout, timeoutMsg, cacheUpdateLock);
    }
    
    @Override
    public void waitValue(Object value, int timeout) throws IOException, InterruptedException {
        waitConditionOnValue(() -> !hasChanged(value, take()),  timeout, "Timeout waiting value: " + value);
    }
    
    @Override
    public void waitValueNot(Object value, int timeout) throws IOException, InterruptedException {
        waitConditionOnValue(() -> hasChanged(value, take()), timeout, "Timeout waiting value not: " + value);
    }

    //Waiting for next different value
    @Override
    public boolean waitValueChange(int timeout) throws InterruptedException {
        int wait = Math.max(timeout, 0);
        Object cur = cache;
        synchronized (valueWaitLock) {
            valueWaitLock.wait(wait);
        }
        return hasChanged(cache, cur);
    }

    //Waiting for next sampling
    @Override
    public boolean waitCacheChange(int timeout) throws InterruptedException {
        int wait = Math.max(timeout, 0);
        Object cur = chronoValue;
        synchronized (cacheUpdateLock) {
            cacheUpdateLock.wait(wait);
        }
        return chronoValue != cur;
    }

    //Cache
    volatile Chrono chronoValue;
    volatile Object cache;
    volatile Object lastTriggeredCache;

    @Override
    public Object take() {
        return cache;
    }

    @Hidden
    final public void resetCache() {
        if (isSimulated()) {
            return;
        }
        setCache(null);
    }

    final protected void setCache(Object value) {
        setCache(value, null);
    }

    //TODO: Cannot make it final: http://bugs.jython.org/issue2634
    protected void setCache(Object value, Long timestamp) {
        setCache(value, timestamp, 0L);
    }

    protected void setCache(Object value, Long timestamp, Long nanosOffset) {
        Object former = cache;
        synchronized (cacheUpdateLock) {
            chronoValue = (timestamp == null) ? new Chrono() : new Chrono(timestamp, nanosOffset);
            cache = value;
            cacheUpdateLock.notifyAll();
        }
        try {
            boolean valueChange = hasChanged(cache, lastTriggeredCache);
            triggerCacheChanged(cache, former, chronoValue.getTimestamp(), valueChange);
            if (valueChange) {
                former = lastTriggeredCache;
                //Set before triggering to prevent re-entrances in som e particular cases as AreaDetector.readImageDescriptor in listener.
                lastTriggeredCache = cache;
                triggerValueChanged(cache, former);
                synchronized (valueWaitLock) {
                    valueWaitLock.notifyAll();
                }
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }

    final protected void setCache(DeviceBase child, Object value) {
        setCache(value, System.currentTimeMillis());
    }

    protected void setCache(DeviceBase child, Object value, Long timestamp) {
        setCache(child, value, timestamp, 0L);
    }

    protected void setCache(DeviceBase child, Object value, Long timestamp, Long nanosOffset) {
        if (isChild(child)) {
            child.setCache(value, timestamp, nanosOffset);
        } else {
            getLogger().warning("Attempt to set cache of not child device: " + child.getName());
        }
    }

    @Override
    public Integer getAge() {
        if (chronoValue == null) {
            return null;
        }
        return chronoValue.getEllapsed();
    }

    @Override
    public Long getTimestamp() {
        if (chronoValue == null) {
            return null;
        }
        return chronoValue.getTimestamp();
    }

    @Override
    public Long getTimestampNanos() {
        if (chronoValue == null) {
            return null;
        }
        return chronoValue.getTimestampNanos();
    }

    @Override
    public TimestampedValue takeTimestamped() {
        synchronized (cacheUpdateLock) {
            if (chronoValue == null) {
                return null;
            }
            return new TimestampedValue(cache, chronoValue.getTimestamp(), chronoValue.getNanosOffset());
        }
    }

    protected boolean hasChanged(Object value, Object former) {
        if (former == null) {
            return (value != null);
        }
        if (value == former) {
            return false;
        }
        return !former.equals(value);
    }

    //Children
    Device[] children;
    Device parent;

    protected void setChildren(Device[] children) {
        if (this.children != null) {
            if (trackChildren) {
                for (Device child : this.children) {
                    child.removeListener(trackedChildrenListener);
                }
            }
        }
        this.children = null;
        addChildren(children);
    }

    protected void addChildren(Device[] children) {
        for (Device child : children) {
            addChild(child);
        }
    }

    protected void addChild(Device child) {
        if (children == null) {
            children = new Device[0];
        }
        if (child != null) {
            if (!Arr.contains(children, child)) {
                children = Arr.append(children, child);
                if (child instanceof DeviceBase) {
                    ((DeviceBase) child).setParent(this);
                }
                if (trackChildren) {
                    child.addListener(trackedChildrenListener);
                    child.setMonitored(isMonitored());
                }
            }
        }
    }

    @Override
    public Device[] getChildren() {
        if (children == null) {
            return new DeviceBase[0];
        }
        return children;
    }

    @Hidden
    public Object[] getChildrenValues() {
        Device[] children = getChildren();
        Object[] values = new Object[children.length];
        for (int i = 0; i < children.length; i++) {
            values[i] = children[i].take();
        }
        return values;
    }

    @Override
    public Device getChild(String name) {
        if ((children == null) || (name == null)) {
            return null;
        }
        for (Device child : children) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    protected void setParent(DeviceBase parent) {
        if (this.parent != parent) {
            if (parent == this) {
                this.parent = this;
                getLogger().severe("Attempt to assign device parent as itself");
                return;
            }
            this.parent = parent;
            if (!Arr.contains(parent.getChildren(), this)) {
                parent.setChildren(Arr.append(parent.getChildren(), this));
            }
        }
    }

    @Override
    public Device getParent() {
        return parent;
    }

    boolean trackChildren;

    /**
     * Tracked children have the monitored flag propagated, and generate onChildValueChange and
     * onChildStateChange events.
     */
    protected void setTrackChildren(boolean value) {
        if (trackChildren != value) {
            trackChildren = value;
            if (trackChildren) {
                trackedChildrenListener = new ReadbackDeviceAdapter() {
                    @Override
                    public void onValueChanged(Device device, Object value, Object former) {
                        onChildValueChange(device, value, former);
                    }

                    @Override
                    public void onStateChanged(Device device, State state, State former) {
                        onChildStateChange(device, state, former);
                    }

                    @Override
                    public void onReadbackChanged(Device device, Object value) {
                        onChildReadbackChange(device, value);
                    }
                };
                for (Device child : getChildren()) {
                    child.addListener(trackedChildrenListener);
                }
            } else {
                for (Device child : getChildren()) {
                    child.removeListener(trackedChildrenListener);
                }
                trackedChildrenListener = null;
            }
            for (Device child : getChildren()) {
                child.setMonitored(isMonitored());
            }
        }
    }

    protected boolean getTrackChildren() {
        return trackChildren;
    }

    DeviceListener trackedChildrenListener;

    //Components
    Device[] components;

    protected void setComponents(Device[] components) {
        this.components = components;
    }

    @Override
    public Device[] getComponents() {
        if (components == null) {
            return new DeviceBase[0];
        }
        return components;
    }

    @Override
    public Device getComponent(String name) {
        if ((components == null) || (name == null)) {
            return null;
        }
        for (Device component : components) {
            if (name.equals(component.getName())) {
                return component;
            }
        }
        return null;
    }

    HashMap<String, Object> simulatedValues;

    protected void setSimulatedValue(String name, Object value) {
        if (simulatedValues == null) {
            simulatedValues = new HashMap<>();
        }
        simulatedValues.put(name, value);
    }

    protected Object getSimulatedValue(String name) {
        if (simulatedValues == null) {
            return null;
        }
        return simulatedValues.get(name);
    }

    protected Object getSimulatedValue(String name, Object defaultValue) {
        Object ret = getSimulatedValue(name);
        if (ret == null) {
            return defaultValue;
        }
        return ret;
    }

    ScheduledFuture simulationScheduledFuture;

    /**
     * Starts a 100ms timer for simulated devices to update contents. (onSimulationTimer).
     */
    protected void startSimulationTimer() {
        if (schedulerSimulation == null) {
            schedulerSimulation = Executors.newSingleThreadScheduledExecutor(
                    new NamedThreadFactory("Device simulation scheduler"));
        }
        simulationScheduledFuture = schedulerSimulation.scheduleWithFixedDelay(() -> {
            try {
                if (isInitialized()) {
                    onSimulationTimer();
                }
            } catch (Exception ex) {
                getLogger().log(Level.FINER, null, ex);
            }
        }, 1000, 100, TimeUnit.MILLISECONDS);
    }

    protected void stopSimulationTimer() {
        if (simulationScheduledFuture != null) {
            simulationScheduledFuture.cancel(true);
            simulationScheduledFuture = null;
        }
    }

    //Access type checking    
    protected void assertReadEnabled() throws ReadAccessException {
        if (getAccessType() == AccessType.Write) {
            throw new ReadAccessException();
        }
    }

    protected void assertWriteEnabled() throws WriteAccessException {
        if (getAccessType() == AccessType.Read) {
            throw new WriteAccessException();
        }
    }

    /**
     * Timer callback for simulated to update their values.
     */
    protected void onSimulationTimer() throws IOException, InterruptedException {
    }

    //Properties
    @Override
    public DeviceConfig getConfig() {
        return (DeviceConfig) super.getConfig();
    }

    //Device Exceptions
    public class DeviceException extends IOException {

        public DeviceException(String message) {
            super(message);
        }

        public DeviceException(String message, Throwable cause) {
            super(message, cause);
        }

        public DeviceException(Throwable cause) {
            super(cause.getMessage(), cause);
        }

        public Device getDevice() {
            return DeviceBase.this;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage());
            if ((getCause() == null) || (!(getCause() instanceof DeviceException)) || (((DeviceException) getCause()).getDevice() != getDevice())) {
                sb.append(" [").append(getName()).append("]");
            }
            return sb.toString();
        }
    }

    public class DeviceTimeoutException extends DeviceException {

        public DeviceTimeoutException() {
            super("Timeout");
        }

        public DeviceTimeoutException(String message) {
            super(message);
        }
    }

    public class DeviceStateException extends DeviceException {

        public DeviceStateException() {
            super("Invalid state: " + getState());
        }

        public DeviceStateException(Object desiredState) {
            super("Not in state: " + desiredState);
        }
    }

    public class DeviceInvalidParameterException extends DeviceException {

        public DeviceInvalidParameterException() {
            super("Invalid parameter");
        }

        public DeviceInvalidParameterException(String parameter, Object value) {
            super("Invalid parameter: " + parameter + "=" + value);
        }
    }

    /**
     * Unlike DeviceInvalidParameterException. this is a runtime exception./
     */
    public class InvalidValueException extends IllegalArgumentException {

        public InvalidValueException(String str) {
            super(str + (" [") + getName() + "]");
        }

        public InvalidValueException(Object value) {
            this("Invalid value: " + Str.toString(value, 100));
        }

        public InvalidValueException(Object value, Object min, Object max) {
            this("Invalid value: " + String.valueOf(value) + " (range is " + min + " to " + max + ")");
        }
    }

    public class StopNotConfiguredException extends DeviceException {

        public StopNotConfiguredException() {
            super("Cannot stop device: no stop parameters configured");
        }
    }

    public class DeviceValueVetoException extends DeviceException {

        public DeviceValueVetoException(Exception cause) {
            super(((cause.getMessage() == null) ? cause.toString().trim() : cause.getMessage()), cause);
        }

        public DeviceValueVetoException(Object value) {
            super("Cannot set value to " + Str.toString(value, 10));
        }
    }

    public class ReadAccessException extends DeviceException {

        public ReadAccessException() {
            super("Device is write-only");
        }
    }

    public class WriteAccessException extends DeviceException {

        public WriteAccessException() {
            super("Device is read-only");
        }
    }

    //Overridables
    @Override
    /**
     * If overriding call super.doClose() to propagate to children
     */
    protected void doClose() throws IOException {
        stopSimulationTimer();
        for (Device trigger : getTriggers()) {
            trigger.removeListener(triggerListener);
        }
        for (Device child : getChildren()) {
            try {
                child.close();
            } catch (Exception ex) {
                getLogger().fine("Error closing child device " + child.getName() + ": " + ex.getMessage());
            }
        }
        cache = null;
        synchronized (valueWaitLock) {
            valueWaitLock.notifyAll();
        }
    }

    @Override
    /**
     * If overriding call super.doInitialize() to propagate to children
     */
    protected void doInitialize() throws IOException, InterruptedException {
        for (Device child : getChildren()) {
            child.initialize();
        }
    }

    @Override
    /**
     * If overriding call super.doSetMonitored() to propagate to children
     */
    protected void doSetMonitored(boolean value) {
        if (trackChildren) {
            for (Device child : getChildren()) {
                child.setMonitored(value);
            }
        }
    }

    /**
     * Only called once
     */
    @Override
    /**
     * If overriding call super.doSetSimulated() to propagate to children
     */
    protected void doSetSimulated() {
        for (Device dev : getChildren()) {
            dev.setSimulated();
        }
    }

    @Override
    /**
     * If overriding call super.doUpdate() to propagate to children
     */
    protected void doUpdate() throws IOException, InterruptedException {
        if (trackChildren) {
            long start = System.currentTimeMillis();
            for (Device child : getChildren()) {
                try {
                    Integer childAge = child.getAge();
                    //Won't update a child that has been updated or have has a new value since start of update
                    if ((childAge == null) || (childAge < 0) || (childAge > (System.currentTimeMillis() - start))) {
                        child.update();
                    }
                } catch (IOException ex) {
                    getLogger().fine("Error updating child device " + child.getName() + ": " + ex.getMessage());
                }
            }
        }
    }

    Device[] triggers;

    public void setTriggers(Device[] triggers) {
        for (Device trigger : getTriggers()) {
            trigger.removeListener(triggerListener);
        }
        this.triggers = Arr.copy(triggers);
        for (Device trigger : getTriggers()) {
            if (triggerListener == null) {
                triggerListener = new DeviceAdapter() {
                    public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                        DeviceBase.this.request();
                    }
                };
            }
            trigger.addListener(triggerListener);
        }
    }

    public Device[] getTriggers() {
        return (triggers != null) ? triggers : new Device[0];
    }

    DeviceListener triggerListener;

    protected void triggerStateChanged(State state, State former) {
        onStateChange(state, former);
        for (DeviceListener listener : getListeners()) {
            try {
                listener.onStateChanged(this, state, former);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerCacheChanged(Object value, Object former, long timestamp, boolean valueChange) {
        onCacheChange(value, former, timestamp, valueChange);
        for (DeviceListener listener : getListeners()) {
            try {
                listener.onCacheChanged(this, value, former, timestamp, valueChange);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerValueChanged(Object value, Object former) {
        onValueChange(value, former);
        for (DeviceListener listener : getListeners()) {
            try {
                listener.onValueChanged(this, value, former);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerValueChanging(Object value) throws DeviceValueVetoException {
        onValueChanging(value, cache);
        for (DeviceListener listener : getListeners()) {
            try {
                listener.onValueChanging(this, value, cache);
            } catch (Exception ex) {
                throw new DeviceValueVetoException(ex);
            }
        }
    }

    protected void triggerReadbackChanged(Double value) {
        onReadbackChanged(value);
        for (DeviceListener listener : getListeners()) {
            if (listener instanceof ReadbackDeviceListener) {
                try {
                    ((ReadbackDeviceListener) listener).onReadbackChanged(this, value);
                } catch (Exception ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void onValueChanging(Object value, Object former) throws DeviceValueVetoException {
    }

    protected void onValueChange(Object value, Object former) {
    }

    protected void onCacheChange(Object value, Object former, long timestamp, boolean valueChanged) {
    }

    protected void onReadbackChanged(Double value) {
    }

    protected void onStateChange(State state, State former) {
    }

    /**
     * Called if tracking children
     */
    protected void onChildValueChange(Device child, Object value, Object former) {
    }

    protected void onChildReadbackChange(Device child, Object value) {
    }

    /**
     * Called if tracking children
     */
    protected void onChildStateChange(Device child, State state, State former) {
    }
}
