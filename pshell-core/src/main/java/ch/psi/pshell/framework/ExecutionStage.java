package ch.psi.pshell.framework;

import ch.psi.pshell.utils.EncoderJson;
import java.io.File;
import java.util.Map;

/**
 *
 */
public class ExecutionStage {
    
    public final File file;
    public final Map<String, Object> args;
    public final String statement;

    ExecutionStage(File file, Map<String, Object> args) {
        this.file = file;
        this.args = args;
        this.statement = null;
    }

    ExecutionStage(String statement) {
        this.file = null;
        this.args = null;
        this.statement = statement;
    }

    public String getArgsStr() {
        StringBuilder sb = new StringBuilder();
        if (args != null) {
            for (String key : args.keySet()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                Object value = args.get(key);
                String text;
                try {
                    text = EncoderJson.encode(value, false);
                } catch (Exception ex) {
                    text = String.valueOf(value);
                }
                sb.append(key).append(":").append(text);
            }
        }
        return sb.toString();
    }

    public String toString() {
        if (file != null) {
            String args = getArgsStr();
            return file.toString() + (args.isEmpty() ? " " : "(" + args + ")");
        } else {
            return statement;
        }
    }
    
}
