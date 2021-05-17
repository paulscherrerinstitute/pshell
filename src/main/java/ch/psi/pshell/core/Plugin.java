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
import java.util.Map;

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
        return run(scriptName, args, false);
    }
    
    default Object run(String scriptName, Object args, boolean background) throws ScriptException, IOException, Context.ContextStateException, InterruptedException {
        if (background){
            return getContext().evalFileBackground(CommandSource.plugin, scriptName, args);
        } else {
            return getContext().evalFile(CommandSource.plugin, scriptName, args);
        }
    }    

    default CompletableFuture<?> runAsync(String scriptName) throws ScriptException, IOException, Context.ContextStateException, InterruptedException {
        return runAsync(scriptName, null);
    }

    default CompletableFuture<?> runAsync(String scriptName, Object args) throws Context.ContextStateException {
        return runAsync(scriptName, args, false);
    }
    
    default CompletableFuture<?> runAsync(String scriptName, Object args, boolean background) throws Context.ContextStateException {
        if (background){
            return getContext().evalFileBackgroundAsync(CommandSource.plugin, scriptName, args);
        } else {
            return getContext().evalFileAsync(CommandSource.plugin, scriptName, args);
        }
    }    

    default CompletableFuture<?> runBackground(String scriptName) throws Context.ContextStateException {
        return getContext().evalFileBackgroundAsync(CommandSource.plugin, scriptName);
    }

    default Object getGlobalVar(String name) {
        return getContext().getScriptManager().getVar(name);
    }

    default void setGlobalVar(String name, Object val){
        getContext().getScriptManager().setVar(name, val);
    }

    default void setGlobalsVars(HashMap<String, Object> vars)  {
        for (String key : vars.keySet()) {
            setGlobalVar(key, vars.get(key));
        }
    }

    /**
     * Start a background task
     */
    default void startTask(String scriptName, int delay) throws IOException, Context.ContextStateException {
        startTask(scriptName, delay, -1);
    }

    default void startTask(String scriptName, int delay, int interval) throws IOException, Context.ContextStateException {
        getContext().startTask(CommandSource.plugin, scriptName, delay, interval);
    }

    /**
     * Stop a background task
     */
    default void stopTask(String scriptName) throws IOException, Context.ContextStateException {
        getContext().stopTask(CommandSource.plugin, scriptName, false);
    }

    default void stopTask(String scriptName, boolean force) throws IOException, Context.ContextStateException {
        getContext().stopTask(CommandSource.plugin, scriptName, force);
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
    
    default void setSetting(String name, Object value) throws IOException{
        getContext().setSetting(name, value);
    }
    
    default String getSetting(String name) throws IOException{
        return getContext().getSetting(name);
    }

    default Map<String, String> getSettings() throws IOException{    
        return getContext().getSettings();
    }

    default void waitState(State state, int timeout) throws IOException, InterruptedException{    
        getContext().waitState(state, timeout);
    }   
    
    default void waitStateNot(State state, int timeout) throws IOException, InterruptedException{    
        getContext().waitStateNot(state, timeout);
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
    
    default boolean addDevice(GenericDevice device, boolean initialize) {
        DevicePool pool = getContext().getDevicePool();
        if (pool != null) {
            return pool.addDevice(device, initialize);
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
    
    default boolean removeDevice(GenericDevice device, boolean close) {
        DevicePool pool = getContext().getDevicePool();
        if (pool != null) {
            return pool.removeDevice(device, close);
        }
        return false;
    }
}
