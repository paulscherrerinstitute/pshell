package ch.psi.pshell.device;

import ch.psi.pshell.core.Setup;
import java.io.IOException;
import java.nio.file.Paths;
import ch.psi.utils.Configurable;
import ch.psi.utils.Observable;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.State;
import ch.psi.utils.Threading;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of GenericDevice, which have a specific listener interface.
 */
public interface GenericDevice<T> extends Observable<T>, Timestamped, AutoCloseable, Configurable, Record {

    public static final String PROPERTY_CONFIG_PATH = "ch.psi.pshell.device.config.path";

    State getState();

    void waitState(State state, int timeout) throws IOException, InterruptedException;

    void waitStateNot(State state, int timeout) throws IOException, InterruptedException;
    
    void initialize() throws IOException, InterruptedException;

    boolean isInitialized();
    
    void waitInitialized(int timeout) throws IOException, InterruptedException;

    //General attributes
    /**
     * Called once after instantiation if device is simulated
     */
    void setSimulated();

    boolean isSimulated();

    boolean isMonitored();

    void setMonitored(boolean monitored);

    void setPolling(int interval);

    int getPolling();

    default boolean isPolled() {
        return getPolling() > 0;
    }

    boolean isPollingBackground();

    void setAccessType(AccessType mode);

    AccessType getAccessType();

    //Cache management and updating
    /**
     * Return a cache of internal contents.
     */
    Object take();

    /**
     * Return cache as a number - used on historical plots.
     */
    @Hidden
    default Number takeAsNumber() {
        Object cache = take();
        if (cache != null) {
            if (cache instanceof Number) {
                return (Number) cache;
            } else if (cache instanceof Boolean) {
                return (cache == Boolean.TRUE) ? 1 : 0;
            }
        }
        return null;
    }

    /**
     * Milliseconds since last cached value update (or -1 if no value read)
     */
    Integer getAge();

    Object request();

    void update() throws IOException, InterruptedException;
    
    void setWaitSleep(int value);
    
    int getWaitSleep();    
    
    public void assertInitialized() throws IOException;
    
    public void assertNotInitialized() throws IOException;
    
    public void assertState(State state) throws IOException;
    
    public void assertStateNot(State state) throws IOException;
    

    default CompletableFuture updateAsync() {
        return (CompletableFuture) Threading.getFuture(() -> update());
    }
    
    public static String getConfigPath() {
        String path = System.getProperty(PROPERTY_CONFIG_PATH);
        if (path == null) {
            path = Setup.DEFAULT_HOME_FOLDER + "/devices";
        }
        return Setup.expand(path);
    }      

    public static String getConfigFileName(String name) {
        return getConfigFileName(null, name);
    }

    public static String getConfigFileName(String configPath, String name) {
        return Paths.get((configPath==null) ? getConfigPath() : configPath, name + ".properties").toString();
    }

}
