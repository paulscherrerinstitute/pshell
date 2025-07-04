package ch.psi.pshell.sequencer;

import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.Interpreter.InterpreterStateException;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.script.ScriptException;

/**
 */
public interface Plugin extends ch.psi.pshell.plugin.Plugin{
    

    default Object eval(String str) throws ScriptException, IOException, InterpreterStateException, InterruptedException {
        return eval(str, false);
    }

    default Object eval(String str, boolean background) throws ScriptException, IOException, InterpreterStateException, InterruptedException {
        if (background) {
            return Context.getInterpreter().evalLineBackground(CommandSource.plugin, str);
        } else {
            return Context.getInterpreter().evalLine(CommandSource.plugin, str);
        }
    }

    default CompletableFuture<?> evalAsync(String str) throws InterpreterStateException {
        return evalAsync(str, false);
    }

    default CompletableFuture<?> evalAsync(String str, boolean background) throws InterpreterStateException {
        if (background) {
            return Context.getInterpreter().evalLineBackgroundAsync(CommandSource.plugin, str);
        } else {
            return Context.getInterpreter().evalLineAsync(CommandSource.plugin, str);
        }
    }

    default Object run(String scriptName) throws ScriptException, IOException, InterpreterStateException, InterruptedException {
        return run(scriptName, null);
    }

    default Object run(String scriptName, Object args) throws ScriptException, IOException, InterpreterStateException, InterruptedException {
        return run(scriptName, args, false);
    }
    
    default Object run(String scriptName, Object args, boolean background) throws ScriptException, IOException, InterpreterStateException, InterruptedException {
        if (background){
            return Context.getInterpreter().evalFileBackground(CommandSource.plugin, scriptName, args);
        } else {
            return Context.getInterpreter().evalFile(CommandSource.plugin, scriptName, args);
        }
    }    

    default CompletableFuture<?> runAsync(String scriptName) throws ScriptException, IOException, InterpreterStateException, InterruptedException {
        return runAsync(scriptName, null);
    }

    default CompletableFuture<?> runAsync(String scriptName, Object args) throws InterpreterStateException {
        return runAsync(scriptName, args, false);
    }
    
    default CompletableFuture<?> runAsync(String scriptName, Object args, boolean background) throws InterpreterStateException {
        if (background){
            return Context.getInterpreter().evalFileBackgroundAsync(CommandSource.plugin, scriptName, args);
        } else {
            return Context.getInterpreter().evalFileAsync(CommandSource.plugin, scriptName, args);
        }
    }    

    default CompletableFuture<?> runBackground(String scriptName) throws InterpreterStateException {
        return Context.getInterpreter().evalFileBackgroundAsync(CommandSource.plugin, scriptName);
    }

    default Object getGlobalVar(String name) {
        return Context.getInterpreter().getScriptManager().getVar(name);
    }

    default void setGlobalVar(String name, Object val){
        Context.getInterpreter().getScriptManager().setVar(name, val);
    }

    default void setGlobalsVars(HashMap<String, Object> vars)  {
        for (String key : vars.keySet()) {
            setGlobalVar(key, vars.get(key));
        }
    }

    /**
     * Start a background task
     */
    default void startTask(String scriptName, int delay) throws IOException, InterpreterStateException {
        startTask(scriptName, delay, -1);
    }

    default void startTask(String scriptName, int delay, int interval) throws IOException, InterpreterStateException {
        Context.getInterpreter().startTask(CommandSource.plugin, scriptName, delay, interval);
    }

    /**
     * Stop a background task
     */
    default void stopTask(String scriptName) throws IOException, InterpreterStateException {
        Context.getInterpreter().stopTask(CommandSource.plugin, scriptName, false);
    }

    default void stopTask(String scriptName, boolean force) throws IOException, InterpreterStateException {
        Context.getInterpreter().stopTask(CommandSource.plugin, scriptName, force);
    }

    default void abort() throws InterruptedException {
        Context.getInterpreter().abort(CommandSource.plugin);
    }

    default void updateAll() {
        Context.getInterpreter().updateAll(CommandSource.plugin);
    }

    default void stopAll() {
        Context.getInterpreter().stopAll(CommandSource.plugin);
    }

    default void injectVars() {
        Context.getInterpreter().injectVars(CommandSource.plugin);
    }

    default void setPreference(CommandSource source, ViewPreference name, Object value) {
        Context.getInterpreter().setPreference(CommandSource.plugin, name, value);
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
