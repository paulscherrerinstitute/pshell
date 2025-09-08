package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;


public class CommandStatistics{ 
    public enum FileRange {
        Daily,
        Monthly,
        Yearly
    }


    public static String getPath() {
        return Setup.getOutputPath() + "/statistics";
    }    


    public static class CommandStatisticsConfig extends Config {

        public boolean saveAllScripts = true;
        public boolean saveAllConsoleCommands = false;
        public String savedScripts = "";
        public String savedConsoleCommands = "";
        public FileRange fileRange = FileRange.Monthly;
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

    static CommandStatisticsConfig config;

    public static CommandStatisticsConfig getConfig() {
        return config;
    }

    public static void save(CommandInfo info) throws IOException {
        if (config == null) {
               config = new CommandStatisticsConfig();
               Path path = Paths.get(getPath(), "config.properties");
               path.toFile().getParentFile().mkdirs();
               config.load(path.toString());
           }
        if (config.isSaved(info)) {
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
            String prefix = switch (config.fileRange) {
                case Daily -> "YYYY_MM_dd";
                case Monthly -> "YYYY_MM";
                case Yearly -> "YYYY";
            };
            Path path = Paths.get(getPath(), Chrono.getTimeStr(info.start, prefix) + ".csv");
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
}