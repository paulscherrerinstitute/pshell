package ch.psi.pshell.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import javax.script.ScriptException;
import ch.psi.utils.State;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.scripting.ViewPreference;
import java.io.File;

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
        return Logger.getLogger(LogManager.ROOT_LOGGER + "." + getPluginName());
    }

    default Context getContext() {
        return Context.getInstance();
    }

    default State getState() {
        return getContext().getState();
    }

    default Object eval(String str) throws ScriptException, IOException, Context.ContextStateException, InterruptedException {
        return eval(str, false);
    }

    default Object eval(String str, boolean background) throws ScriptException, IOException, Context.ContextStateException, InterruptedException {
        if (background) {
            return getContext().evalLineBackground(CommandSource.plugin, str);
        } else {
            return getContext().evalLine(CommandSource.plugin, str);
        }
    }

    default CompletableFuture<?> evalAsync(String str) throws Context.ContextStateException {
        return evalAsync(str, false);
    }

    default CompletableFuture<?> evalAsync(String str, boolean background) throws Context.ContextStateException {
        if (background) {
            return getContext().evalLineBackgroundAsync(CommandSource.plugin, str);
        } else {
            return getContext().evalLineAsync(CommandSource.plugin, str);
        }
    }

    default Object run(String scriptName) throws ScriptException, IOException, Context.ContextStateException, InterruptedException {
        return run(scriptName, null);
    }

    default Object run(String scriptName, Object args) throws ScriptException, IOException, Context.ContextStateException, InterruptedException {
        return getContext().evalFile(CommandSource.plugin, scriptName, args);
    }

    default CompletableFuture<?> runAsync(String scriptName) throws ScriptException, IOException, Context.ContextStateException, InterruptedException {
        return runAsync(scriptName, null);
    }

    default CompletableFuture<?> runAsync(String scriptName, Object args) throws Context.ContextStateException {
        return getContext().evalFileAsync(CommandSource.plugin, scriptName, args);
    }

    default CompletableFuture<?> runBackground(String scriptName) throws Context.ContextStateException {
        return getContext().evalFileBackgroundAsync(CommandSource.plugin, scriptName);
    }

    default void setGlobalVar(String name, Object val) throws Context.ContextStateException {
        getContext().getScriptManager().setVar(name, val);
    }

    default void setGlobalsVars(HashMap<String, Object> vars) throws Context.ContextStateException {
        for (String key : vars.keySet()) {
            setGlobalVar(key, vars.get(key));
        }
    }

    /**
     * Start a background task
     */
    default void startTask(String scriptName, int delay) {
        startTask(scriptName, delay, -1);
    }

    default void startTask(String scriptName, int delay, int interval) {
        getContext().taskManager.create(scriptName, delay, interval);
        getContext().taskManager.start(scriptName);
    }

    /**
     * Stop a background task
     */
    default void stopTask(String scriptName) {
        getContext().taskManager.remove(scriptName, false);
    }

    default void stopTask(String scriptName, boolean force) {
        getContext().taskManager.remove(scriptName, force);
    }

    default void abort() throws InterruptedException {
        getContext().abort(CommandSource.plugin);
    }

    default void updateAll() {
        getContext().updateAll(CommandSource.plugin);
    }

    default void stopAll() {
        getContext().stopAll(CommandSource.plugin);
    }

    default void injectVars() {
        getContext().injectVars(CommandSource.plugin);
    }

    default void setPreference(CommandSource source, ViewPreference name, Object value) {
        getContext().setPreference(CommandSource.plugin, name, value);
    }

    default GenericDevice getDevice(String name) {
        DevicePool pool = getContext().getDevicePool();
        if (pool != null) {
            return pool.getByName(name);
        }
        return null;
    }

    default boolean addDevice(GenericDevice device) {
        DevicePool pool = getContext().getDevicePool();
        if (pool != null) {
            return pool.addDevice(device);
        }
        return false;
    }

    default boolean removeDevice(GenericDevice device) {
        DevicePool pool = getContext().getDevicePool();
        if (pool != null) {
            return pool.removeDevice(device);
        }
        return false;
    }
}
