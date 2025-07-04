package ch.psi.pshell.device;

import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.utils.TimestampedValue;
import java.io.IOException;
import java.util.List;

/**
 * __Device__ is a notion in PShell representing a real-world hardware or subsystem. A Device encapsulates a set of
 * values (state) and a set of methods (behavior) in order to simplify the statements of the scripts.
 */
public interface Device extends GenericDevice<DeviceListener> {

    //State: 
    /**
     * isReady may represent something different that state=ready, depending on the device
     */
    boolean isReady() throws IOException, InterruptedException;

    void waitReady(int timeout) throws IOException, InterruptedException;

    //Waiting for value
    void waitValue(Object value, int timeout) throws IOException, InterruptedException;

    void waitValueNot(Object value, int timeout) throws IOException, InterruptedException;

    /**
     * Waiting for next value change event. Return false if timeout.
     */
    boolean waitValueChange(int timeout) throws InterruptedException;

    /**
     * Wait device cache update (also when new value equals old value). Return false if timeout.
     */
    boolean waitCacheChange(int timeout) throws InterruptedException;

    //Hierarchical organization
    Device[] getComponents();

    Device getComponent(String name);

    Device[] getChildren();

    Device getChild(String name);

    Device getParent();

    void setTriggers(Device[] triggers);

    Device[] getTriggers();

    TimestampedValue takeTimestamped();



    //Cache Buffering
    public int getBufferCapacity();
    
    public void setBufferCapacity(int queueCapacity);

    public int getBufferSize();

    public TimestampedValue popBuffer();
    
    public void clearBuffer();

    public List<TimestampedValue> getBuffer();

    public boolean waitBuffer(int timeout) throws InterruptedException, IOException;

    @Hidden
    default public boolean isChild(Device device) {
        for (Device child : getChildren()) {
            if (child == device) {
                return true;
            }
        }
        return false;
    }

    default String getDescription() {
        return "";
    }
};
