package ch.psi.pshell.sequencer;

import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.Sequencer.StateException;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.script.ScriptException;

/**
 */
public interface Plugin extends ch.psi.pshell.extension.Plugin{
    

    default Object eval(String str) throws ScriptException, IOException, StateException, InterruptedException {
        return eval(str, false);
    }

    default Object eval(String str, boolean background) throws ScriptException, IOException, StateException, InterruptedException {
        if (background) {
            return Context.getSequencer().evalLineBackground(CommandSource.plugin, str);
        } else {
            return Context.getSequencer().evalLine(CommandSource.plugin, str);
        }
    }

    default CompletableFuture<?> evalAsync(String str) throws StateException {
        return evalAsync(str, false);
    }

    default CompletableFuture<?> evalAsync(String str, boolean background) throws StateException {
        if (background) {
            return Context.getSequencer().evalLineBackgroundAsync(CommandSource.plugin, str);
        } else {
            return Context.getSequencer().evalLineAsync(CommandSource.plugin, str);
        }
    }

    default Object run(String scriptName) throws ScriptException, IOException, StateException, InterruptedException {
        return run(scriptName, null);
    }

    default Object run(String scriptName, Object args) throws ScriptException, IOException, StateException, InterruptedException {
        return run(scriptName, args, false);
    }
    
    default Object run(String scriptName, Object args, boolean background) throws ScriptException, IOException, StateException, InterruptedException {
        if (background){
            return Context.getSequencer().evalFileBackground(CommandSource.plugin, scriptName, args);
        } else {
            return Context.getSequencer().evalFile(CommandSource.plugin, scriptName, args);
        }
    }    

    default CompletableFuture<?> runAsync(String scriptName) throws ScriptException, IOException, StateException, InterruptedException {
        return runAsync(scriptName, null);
    }

    default CompletableFuture<?> runAsync(String scriptName, Object args) throws StateException {
        return runAsync(scriptName, args, false);
    }
    
    default CompletableFuture<?> runAsync(String scriptName, Object args, boolean background) throws StateException {
        if (background){
            return Context.getSequencer().evalFileBackgroundAsync(CommandSource.plugin, scriptName, args);
        } else {
            return Context.getSequencer().evalFileAsync(CommandSource.plugin, scriptName, args);
        }
    }    

    default CompletableFuture<?> runBackground(String scriptName) throws StateException {
        return Context.getSequencer().evalFileBackgroundAsync(CommandSource.plugin, scriptName);
    }

    default Object getGlobalVar(String name) {
        return Context.getInterpreter().getVar(name);
    }

    default void setGlobalVar(String name, Object val){
        Context.getInterpreter().setVar(name, val);
    }

    default void setGlobalsVars(HashMap<String, Object> vars)  {
        for (String key : vars.keySet()) {
            setGlobalVar(key, vars.get(key));
        }
    }

    /**
     * Start a background task
     */
    default void startTask(String scriptName, int delay) throws IOException, StateException {
        startTask(scriptName, delay, -1);
    }

    default void startTask(String scriptName, int delay, int interval) throws IOException, StateException {
        Context.getSequencer().startTask(CommandSource.plugin, scriptName, delay, interval);
    }

    /**
     * Stop a background task
     */
    default void stopTask(String scriptName) throws IOException, StateException {
        Context.getSequencer().stopTask(CommandSource.plugin, scriptName, false);
    }

    default void stopTask(String scriptName, boolean force) throws IOException, StateException {
        Context.getSequencer().stopTask(CommandSource.plugin, scriptName, force);
    }

    default void abort() throws InterruptedException {
        Context.getSequencer().abort(CommandSource.plugin);
    }

    default void updateAll() {
        Context.getSequencer().updateAll(CommandSource.plugin);
    }

    default void stopAll() {
        Context.getSequencer().stopAll(CommandSource.plugin);
    }

    default void injectVars() {
        Context.getSequencer().injectVars(CommandSource.plugin);
    }

    default void setPreference(CommandSource source, ViewPreference name, Object value) {
        Context.getSequencer().setPreference(CommandSource.plugin, name, value);
    }
    
    default void setSetting(String name, Object value) throws IOException{
        Context.setSetting(name, value);
    }
    
    default String getSetting(String name) throws IOException{
        return Context.getSetting(name);
    }

    default Map<String, String> getSettings() throws IOException{    
        return Context.getSettings();
    }

    default GenericDevice getDevice(String name) {
        DevicePool pool = Context.getDevicePool();
        if (pool != null) {
            return pool.getByName(name);
        }
        return null;
    }

    default boolean addDevice(GenericDevice device) {
        DevicePool pool = Context.getDevicePool();
        if (pool != null) {
            return pool.addDevice(device);
        }
        return false;
    }
    
    default boolean addDevice(GenericDevice device, boolean initialize) {
        DevicePool pool = Context.getDevicePool();
        if (pool != null) {
            return pool.addDevice(device, initialize);
        }
        return false;
    }    

    default boolean removeDevice(GenericDevice device) {
        DevicePool pool = Context.getDevicePool();
        if (pool != null) {
            return pool.removeDevice(device);
        }
        return false;
    }
    
    default boolean removeDevice(GenericDevice device, boolean close) {
        DevicePool pool = Context.getDevicePool();
        if (pool != null) {
            return pool.removeDevice(device, close);
        }
        return false;
    }
    
    default State getState() {
        return Context.getState();
    }
    
    default void waitState(State state, int timeout) throws IOException, InterruptedException{    
        Context.waitState(state, timeout);
    }   
    
    default void waitStateNot(State state, int timeout) throws IOException, InterruptedException{    
        Context.waitStateNot(state, timeout);
    }  
    
}
