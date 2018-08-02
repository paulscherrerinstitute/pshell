package ch.psi.pshell.core;

import ch.psi.utils.Chrono;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class CommandManager implements AutoCloseable{

    final List<CommandInfo> commandInfo = new ArrayList<>();
    //final HashMap<Thread, CommandInfo> commandInfo = new HashMap<>();
    public static int COMMAND_INFO_SIZE = 1000;              //Tries cleanup when contains 100 command records
    public static int COMMAND_INFO_TIME_TO_LIVE = 600000;   //Cleanup commands older than 10 minutes 

    void initCommandInfo(CommandInfo info) {
        if (info != null) {
            synchronized (commandInfo) {
                commandInfo.add(0, info);
                //commandInfo.put(Thread.currentThread(), info);
            }
        }
    }

    boolean requestedCleanup;

    void finishCommandInfo(Object result) {
        synchronized (commandInfo) {
            CommandInfo info = getCurrentCommand();
            if (info != null) {
                info.result = result;
                info.end = System.currentTimeMillis();
                //commandInfo.remove(Thread.currentThread());
                //commandInfo.remove(info);
                commandInfo.notifyAll();
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
                if ((!ci.isRunning()) && (ci.getAge() > COMMAND_INFO_TIME_TO_LIVE)){ //retains info for 10min 
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
        
        for (CommandInfo info: getCommands()){
            if ((info.thread == thread)){
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
            cmd =  getInterpreterThreadCommand();
            if (cmd!=null){
                id = cmd.id;
            }
        } else {   
            cmd = getCommand(id);
        }
        Map ret = new HashMap();
        ret.put("id", id);
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
                } else if ((cmd != null) && (cmd.result instanceof Exception)) {
                    status = "failed";
                    ret.put("exception", ((Exception) cmd.result).toString());
                } else {
                    status = "completed";
                    ret.put("return", cmd.result);
                }
            }
        }
        ret.put("status", status);
        return ret;
    }

    CommandInfo getNewCommand() throws TimeoutException, InterruptedException {
        Chrono chrono = new Chrono();
        List<CommandInfo> commands = getCommands();
        CommandInfo ret = null;
        try {
            chrono.waitCondition(() -> {
                List<CommandInfo> cmds = getCommands();
                cmds.removeAll(commands);
                if (cmds.size() >= 1) {
                    commands.clear();
                    commands.addAll(cmds);
                    return true;
                }
                return false;
            }, 100);
        } catch (TimeoutException ex) {
            return null;
        }
        return commands.size() > 0 ? commands.get(0) : null;
    }

    @Override
    public void close() throws Exception {
        commandInfo.clear();
    }
}
