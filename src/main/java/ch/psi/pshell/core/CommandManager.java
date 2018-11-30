package ch.psi.pshell.core;

import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.utils.Chrono;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.python.core.PyBaseException;

/**
 *
 */
public class CommandManager implements AutoCloseable {

    final List<CommandInfo> commandInfo = new ArrayList<>();
    //final HashMap<Thread, CommandInfo> commandInfo = new HashMap<>();
    public static int COMMAND_INFO_SIZE = 1000;              //Tries cleanup when contains 100 command records
    public static int COMMAND_INFO_TIME_TO_LIVE = 600000;   //Cleanup commands older than 10 minutes 

    void initCommandInfo(CommandInfo info) {
        if (info != null) {
            synchronized (commandInfo) {
                commandInfo.add(0, info);
                //commandInfo.put(Thread.currentThread(), info);
                onCommandStarted(info);
            }
        }
    }

    boolean requestedCleanup;

    void finishCommandInfo(Object result) {
        synchronized (commandInfo) {
            CommandInfo info = getCurrentCommand();
            if (info != null) {
                if ((result!=null) && (result instanceof PyBaseException)){
                    result = new Exception(result.toString());
                }
                info.result = result;
                info.end = System.currentTimeMillis();
                //commandInfo.remove(Thread.currentThread());
                //commandInfo.remove(info);
                commandInfo.notifyAll();
                onCommandFinished(info);
            }
        }
        if (commandInfo.size() > COMMAND_INFO_SIZE) {
            cleanupCommands();
        }
    }

    void cleanupCommands() {
        List old = new ArrayList<>();
        synchronized (commandInfo) {

            for (CommandInfo ci : getCommands()) {
                if ((!ci.isRunning()) && (ci.getAge() > COMMAND_INFO_TIME_TO_LIVE)) { //retains info for 10min 
                    old.add(ci);
                }
            }
            commandInfo.removeAll(old);
            /*
            for (Thread thread : commandInfo.keySet()) {
                CommandInfo ci = commandInfo.get(thread);
                if ((ci == null) || ((!ci.isRunning()) && (ci.getAge() > COMMAND_INFO_TIME_TO_LIVE))) {//retains info for at least 10min 
                    old.add(thread);
                }
            }
            commandInfo.keySet().removeAll(old);
             */
        }
    }

    public CommandInfo getCurrentCommand() {
        return getCurrentCommand(Thread.currentThread());
    }

    public CommandInfo getCurrentCommand(Thread thread) {
        CommandInfo threadCommand = getThreadCommand(thread);
        if ((threadCommand != null) && (threadCommand.isRunning())) {
            return threadCommand;
        }
        return null;
    }

    public CommandInfo getThreadCommand(Thread thread) {

        for (CommandInfo info : getCommands()) {
            if ((info.thread == thread)) {
                return info;
            }
        }
        return null;
        /*
        synchronized (commandInfo) {
            return commandInfo.get(thread);
        }
         */
    }

    public CommandInfo getInterpreterThreadCommand() {
        return getThreadCommand(Context.getInstance().interpreterThread);
    }

    public List<CommandInfo> getCommands() {
        synchronized (commandInfo) {
            return new ArrayList(commandInfo);
            //return new ArrayList(commandInfo.values());
        }
    }

    public CommandInfo getCommand(long id) {
        if (id < 0) {
            return getInterpreterThreadCommand();
        }
        for (CommandInfo ci : getCommands()) {
            if (ci.id == id) {
                return ci;
            }
        }
        return null;
    }

    public List<CommandInfo> getCurrentCommands() {
        List currentCommands = new ArrayList<>();
        for (CommandInfo ci : getCommands()) {
            if (ci.isRunning()) {
                currentCommands.add(ci);
            }
        }
        return currentCommands;
    }

    public boolean abort(final CommandSource source, long id) throws InterruptedException {
        boolean aborted = false;
        for (CommandInfo ci : getCommands()) {
            if (id == -1) {
                if (ci.background) {
                    ci.abort();
                    aborted = true;
                }
            } else if (ci.id == id) {
                ci.abort();
                aborted = true;
                break;
            }
        }
        return aborted;
    }

    public boolean join(long id) throws InterruptedException {
        CommandInfo cmd = getCommand(id);
        if (cmd != null) {
            cmd.join();
            return true;
        }
        return false;
    }

    public boolean isRunning(long id) {
        CommandInfo cmd = getCommand(id);
        if (cmd != null) {
            return cmd.isRunning();
        }
        return false;
    }

    public Map getResult(long id) throws Exception {
        CommandInfo cmd;
        if (id < 0) {
            cmd = getInterpreterThreadCommand();
            if (cmd != null) {
                id = cmd.id;
            }
        } else {
            cmd = getCommand(id);
        }
        Map ret = new HashMap();
        ret.put("id", id);
        ret.put("exception", null);
        ret.put("return", null);
        String status;
        if (cmd == null) {
            if (id == 0) {
                status = "unlaunched";
            } else {
                status = (id >= CommandInfo.commandId) ? "invalid" : "removed";
            }
        } else {
            if (cmd.isRunning()) {
                status = "running";
            } else {
                if (cmd.isAborted()) {
                    status = "aborted";
                } else if ((cmd.result != null) && (cmd.result instanceof Exception)) {
                    status = "failed";
                    ret.put("exception", ((Exception) cmd.result).toString());
                } else if ((cmd.result != null) &&  (cmd.result instanceof InterpreterResult)){
                    InterpreterResult res = (InterpreterResult)cmd.result;
                    if (res.complete == false){
                        status = "aborted";
                    } else if (res.exception != null){
                        status = "failed";
                        ret.put("exception", res.exception.toString());                        
                    } else {
                        status = "completed";
                        ret.put("return", res.result);                        
                    }
                } else {
                    status = "completed";
                    ret.put("return", cmd.result);
                }
            }
        }
        ret.put("status", status);
        return ret;
    }

    final Object newCommandLock = new Object();
    private long newCommandId;
    private Thread newCommandThread;

    void onNewCommand(long id) {
        synchronized (newCommandLock) {
            newCommandId = id;
            newCommandThread = Thread.currentThread();
            newCommandLock.notifyAll();
        }
    }

    public long waitNewCommand(Thread thread, int timeout) throws InterruptedException {
        while (true) {

            try {
                Chrono chrono = new Chrono();
                synchronized (newCommandLock) {
                    newCommandLock.wait(timeout);
                }
                if ((thread == null) || (newCommandThread == thread)) {
                    return newCommandId;
                }
                timeout = timeout - chrono.getEllapsed();
                if (timeout < 0) {
                    break;
                }
            } catch (Exception ex) {
                break;
            }
        }
        return 0;
    }
    
    
    //Callbacks for triggering script handlers to command start/finish
    void onCommandStarted(CommandInfo info){  
        if (Context.getInstance().config.commandExecutionEvents){
            try {
                String var_name =  "_command_info_" + Thread.currentThread().getId();
                Context.getInstance().scriptManager.getEngine().put(var_name, info);
                Context.getInstance().scriptManager.getEngine().eval("on_command_started(" + var_name + ")");
            } catch (Exception ex) {
                Logger.getLogger(CommandManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    
    void onCommandFinished(CommandInfo info){
        if (Context.getInstance().config.commandExecutionEvents){
            try {
                String var_name = "_command_info_" + Thread.currentThread().getId();
                Context.getInstance().scriptManager.getEngine().put(var_name, info);
                Context.getInstance().scriptManager.getEngine().eval("on_command_finished(" + var_name + ")");
            } catch (Exception ex) {
                Logger.getLogger(CommandManager.class.getName()).log(Level.WARNING, null, ex);
            }                     
        }
    }     

    @Override
    public void close() throws Exception {
        commandInfo.clear();
    }
}
