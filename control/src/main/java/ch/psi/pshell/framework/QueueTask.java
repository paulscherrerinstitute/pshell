package ch.psi.pshell.framework;

import ch.psi.pshell.utils.EncoderJson;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class QueueTask {
    
    public enum QueueTaskErrorAction {
        Resume,
        Retry,
        Once,
        Abort;
    }

    
    final boolean enabled;
    final File file;
    final String statement;
    final Map<String, Object> args;
    final QueueTaskErrorAction error;
    

    public QueueTask(boolean enabled, File file, Map<String, Object> args, QueueTaskErrorAction error) {
        this.enabled = enabled;
        this.file = file;
        this.statement = null;
        this.args = args;
        this.error = error;
    }

    public QueueTask(boolean enabled, String statement, QueueTaskErrorAction error) {
        this.enabled = enabled;
        this.file = null;
        this.statement = statement;
        this.args = null;
        this.error = error;
    }

    public static QueueTask newInstance(boolean enabled, String file, String args, QueueTaskErrorAction error) {
        file = (file == null) ? "" : file.trim();
        args = (args == null) ? "" : args.trim();
        if (!file.isEmpty()) {
            return new QueueTask(enabled, getFile(file), getArgsFromString(args, false), error);
        } else if (!args.isEmpty()) {
            return new QueueTask(enabled, args, error);
        } else {
            return new QueueTask(false, null, getArgsFromString(args, false), error);
        }
    }

    public static File getFile(String filename) {
        filename = Setup.expandPath(filename).trim();
        if (filename.isEmpty()) {
            return null;
        }
        String ext = IO.getExtension(filename);
        if (ext.isEmpty()) {
            filename = filename + "." + Context.getInterpreter().getScriptType().getExtension();
        }
        File file = null;
        for (String path : new String[]{Setup.getScriptsPath(), Setup.getHomePath()}) {
            file = Paths.get(path, filename).toFile();
            if (file.exists()) {
                break;
            }
        }
        if ((file == null) || (!file.exists())) {
            file = new File(filename);
        }
        return file;
    }

    public static class InterpreterVariable {

        public final String name;
        public final Object var;

        InterpreterVariable(String name, Object var) {
            this.name = name;
            this.var = var;
        }

        public String toString() {
            return name;
        }
    }

    static Object expandVariables(Object obj) {
        if (obj instanceof String str) {
            if (str.startsWith("$")) {
                Object var = Context.getInterpreter().getInterpreterVariable(str.substring(1));
                if (var != null) {
                    obj = var;
                }
            }
        } else if (obj instanceof List list) {
            for (int i = 0; i < list.size(); i++) {
                list.set(i, expandVariables(list.get(i)));
            }
        } else if (obj instanceof Map map) {
            for (Object key : map.keySet()) {
                map.put(key, expandVariables(map.get(key)));
            }
        }
        return obj;
    }

    static Map<String, Object> getArgsFromString(String args, boolean forDisplay) {
        Map<String, Object> ret = new HashMap<>();
        Exception originalException = null;
        try {
            try {
                //First try converting as a JSON: supports nested brackets. Script variables names have the prefix $
                args = args.trim();
                StringBuilder sb = new StringBuilder();
                if (!args.startsWith("{")) {
                    sb.append("{");
                }
                sb.append(args);
                if (!args.startsWith("{")) {
                    sb.append("}");
                }
                ret = (Map) EncoderJson.decode(sb.toString(), Map.class);
            } catch (Exception ex) {
                //Then parses the string: support script variables but not nested brackets.
                originalException = ex;
                if (args.startsWith("{")) {
                    args = args.substring(1, args.length() - 1);
                }
                ret = new LinkedHashMap<>();
                for (String token : Str.splitIgnoringBrackets(args, ",")) {
                    if (token.contains(":")) {
                        String name = token.substring(0, token.indexOf(":")).trim();
                        if (name.startsWith("\"")) {
                            name = name.substring(1);
                        }
                        if (name.endsWith("\"")) {
                            name = name.substring(0, name.length() - 1);
                        }
                        name = name.trim();
                        String value = token.substring(token.indexOf(":") + 1).trim();
                        try {
                            ret.put(name, EncoderJson.decode(value, Object.class));
                        } catch (Exception e) {
                            Object var = Context.getInterpreter().getInterpreterVariable(value);
                            ret.put(name, (var == null) ? value : (forDisplay ? new InterpreterVariable(value, var) : var));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(QueueExecution.class.getName()).log(Level.WARNING, args, originalException);
        }
        if (!forDisplay) {
            expandVariables(ret);
        }
        return ret;
    }
    
}
