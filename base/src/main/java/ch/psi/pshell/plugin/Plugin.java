package ch.psi.pshell.plugin;

import ch.psi.pshell.app.Setup;
import ch.psi.pshell.utils.State;
import java.io.File;
import java.util.logging.Logger;

/**
 * Interface to be implemented by extension classes. Default methods are used to implement the
 * functionality. State inheritance is possible with the assumption that no more than one instance
 * of a plugin is loaded, and using the static PluginManager.getProperties(Plugin),
 */
public interface Plugin {

    //Properties
    default String getPluginName() {
        return PluginManager.getProperties(this).name;
    }

    default boolean isStarted() {
        return PluginManager.getProperties(this).started;
    }

    default File getPluginFile() {
        return PluginManager.getProperties(this).file;
    }

    //Overridable callbacks
    default void onStart() {

    }

    default void onStop() {

    }

    default void onStateChange(State state, State former) {

    }

    default void onExecutedFile(String fileName, Object result) {

    }

    default void onInitialize(int runCount) {

    }

    default void onUpdatedDevices() {

    }

    default void onStoppedDevices() {

    }
       
    //API
    default Logger getLogger() {
        return Logger.getLogger(Setup.getRootLogger() + "." + getPluginName());
    }
}
