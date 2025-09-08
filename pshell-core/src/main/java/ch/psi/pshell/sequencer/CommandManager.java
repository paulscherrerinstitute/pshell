package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class CommandManager extends CommandBus implements AutoCloseable {
    
    public static int COMMAND_BUS_SIZE = 1000;              //Tries cleanup when contains 100 command records
    public static int COMMAND_BUS_TIME_TO_LIVE = 600000;   //Cleanup commands older than 10 minutes 

    static  CommandManager INSTANCE;    
    public static CommandManager getInstance(){        
        return INSTANCE;
    }
    
    public CommandManager(){    
        super(COMMAND_BUS_SIZE, COMMAND_BUS_TIME_TO_LIVE);
        INSTANCE  = this;        
    }    
    
    public static String getStatisticsPath() {
        return Setup.getOutputPath() + "/statistics";
    }    

    
    boolean saveCommandStatistics;
    
    public boolean getSaveCommandStatistics(){
        return saveCommandStatistics;
    }

    public void setSaveCommandStatistics(boolean value){
        saveCommandStatistics = value;
    }
    
    boolean scriptCallbacksEnabled=true;
    
    public boolean getScriptCallbacksEnabled(){
        return scriptCallbacksEnabled;
    }

    public void setScriptCallbacksEnabled(boolean value){
        scriptCallbacksEnabled = value;
    }    
    

    public Map getResult(long id) throws Exception {
        CommandInfo cmd;
        if (id < 0) {
            cmd = getInterpreterThreadCommand(true);
            if (cmd != null) {
                id = cmd.id;
            }
        } else {
            cmd = getCommand(id);
        }
        cmd = getCommand(id);
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
                } else if (cmd.result instanceof Exception ex) {
                    status = "failed";
                    ret.put("exception", ex.toString());
                } else if (cmd.result instanceof InterpreterResult res) {
                    if (res.complete == false) {
                        status = "aborted";
                    } else if (res.exception != null) {
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
    

    //Callbacks for triggering script handlers to command start/finish
    protected void onCommandStarted(CommandInfo info) {        
        if (scriptCallbacksEnabled && Context.hasScriptManager()){
            try {
                String var_name = "_command_info_" + Thread.currentThread().getId();
                if (Context.getScriptManager().isThreaded()) {
                    Context.getScriptManager().getEngine().put(var_name, info);
                    Context.getScriptManager().getEngine().eval("on_command_started(" + var_name + ")");
                }
            } catch (Exception ex) {
                Logger.getLogger(CommandManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    protected void onCommandFinished(CommandInfo info) {
        if (scriptCallbacksEnabled && Context.hasScriptManager()){
            try {
                String var_name = "_command_info_" + Thread.currentThread().getId();
                if (Context.getScriptManager().isThreaded()) {
                    Context.getScriptManager().getEngine().put(var_name, info);
                    Context.getScriptManager().getEngine().eval("on_command_finished(" + var_name + ")");
                }
            } catch (Exception ex) {
                Logger.getLogger(CommandManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (saveCommandStatistics) {
            try {
                if (commandStatisticsConfig == null) {
                    commandStatisticsConfig = new CommandStatisticsConfig();
                    Path path = Paths.get(getStatisticsPath(), "config.properties");
                    path.toFile().getParentFile().mkdirs();
                    commandStatisticsConfig.load(path.toString());
                }
                saveCommandStatistics(info);
            } catch (Exception ex) {
                Logger.getLogger(CommandManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    void onChangeDataPath(File dataPath) {
        if (scriptCallbacksEnabled && Context.hasScriptManager()){
            try {
                String filename = (dataPath==null)? "None" : ("'" + dataPath.getCanonicalPath() + "'");
                if (Context.getScriptManager().isThreaded()) {
                    Context.getScriptManager().getEngine().eval("on_change_data_path(" + filename + ")");
                }
            } catch (Exception ex) {
                Logger.getLogger(CommandManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    public void onSessionStarted(int id) {
        if (scriptCallbacksEnabled && Context.hasScriptManager()){
            try {
                if (Context.getScriptManager().isThreaded()) {
                    Context.getScriptManager().getEngine().eval("on_session_started(" + id + ")");
                }
            } catch (Exception ex) {
                Logger.getLogger(CommandManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    public void onSessionFinished(int id) {
        if (scriptCallbacksEnabled && Context.hasScriptManager()){
            try {
                if (Context.getScriptManager().isThreaded()) {
                    Context.getScriptManager().getEngine().eval("on_session_finished(" + id + ")");
                }
            } catch (Exception ex) {
                Logger.getLogger(CommandManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    
    public enum CommandStatisticsFileRange {
        Daily,
        Monthly,
        Yearly
    }

    public class CommandStatisticsConfig extends Config {

        public boolean saveAllScripts = true;
        public boolean saveAllConsoleCommands = false;
        public String savedScripts = "";
        public String savedConsoleCommands = "";
        public CommandStatisticsFileRange fileRange = CommandStatisticsFileRange.Monthly;
        List<String> scripts = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        public boolean isSaved(CommandInfo info) {
            boolean isScript = (info.script != null) && (info.command == null);
            if (isScript) {
                if (saveAllScripts) {
                    return true;
                }
                for (String str : scripts) {
                    if (info.script.trim().startsWith(str)) {
                        return true;
                    }
                }
            }
            if (!isScript) {
                if (saveAllConsoleCommands) {
                    return true;
                }
                for (String str : commands) {
                    if (info.command.trim().startsWith(str)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void save() throws IOException {
            super.save();
            for (String token : savedScripts.split("\\|")) {
                token = token.trim();
                if (!token.isEmpty()) {
                    scripts.add(token);
                }
            }
            for (String token : savedConsoleCommands.split("\\|")) {
                token = token.trim();
                if (!token.isEmpty()) {
                    commands.add(token);
                }
            }
        }
    }

    CommandStatisticsConfig commandStatisticsConfig;

    public CommandStatisticsConfig getCommandStatisticsConfig() {
        return commandStatisticsConfig;
    }

    void saveCommandStatistics(CommandInfo info) throws IOException {
        if (commandStatisticsConfig.isSaved(info)) {
            boolean isScript = (info.script != null) && (info.command == null);
            String result = null;

            if (info.result instanceof InterpreterResult res) {
                if (res.exception != null) {
                    result = InterpreterResult.getPrintableMessage(res.exception);
                } else {
                    result = String.valueOf(res.result);
                }
            } else if (info.result instanceof Throwable t) {
                result = InterpreterResult.getPrintableMessage(t);
            } else {
                result = String.valueOf(info.result);
            }
            result = result.split("\n")[0] + "\n";
            String args = Str.toString(info.args, 10);
            args = args.split("\n")[0];
            String[] data = new String[]{
                isScript ? info.script : info.command,
                args,
                String.valueOf(info.source),
                Chrono.getTimeStr(info.start, "dd/MM/YY HH:mm:ss.SSS"),
                Chrono.getTimeStr(info.end, "dd/MM/YY HH:mm:ss.SSS"),
                String.valueOf(info.background),
                info.isAborted() ? "abort" : (info.isError() ? "error" : "success"),
                result,};
            String prefix = switch (commandStatisticsConfig.fileRange) {
                case Daily -> "YYYY_MM_dd";
                case Monthly -> "YYYY_MM";
                case Yearly -> "YYYY";
            };
            Path path = Paths.get(getStatisticsPath(), Chrono.getTimeStr(info.start, prefix) + ".csv");
            path.toFile().getParentFile().mkdirs();

            if (!path.toFile().exists()) {
                //Header
                final String[] header = new String[]{
                    "Command",
                    "Args",
                    "Source",
                    "Start",
                    "End",
                    "Background",
                    "Result",
                    "Return\n",};
                Files.write(path, String.join(";", header).getBytes());
            }
            Files.write(path, String.join(";", data).getBytes(), StandardOpenOption.APPEND);
            IO.setFilePermissions(path.toFile(), Context.getLogFilePermissions());
        }
    }

    @Override
    public void close() throws Exception {
        super.close();       
    }
}
