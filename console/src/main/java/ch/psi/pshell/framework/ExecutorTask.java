package ch.psi.pshell.framework;

import java.io.File;
import java.util.Map;
import javax.script.ScriptException;

/**
 *
 */
public class ExecutorTask extends Task {
    
    final Executor executor;
    final File file;
    final Map<String, Object> args;
    final String statement;

    public ExecutorTask(Executor executor, Map<String, Object> args) {
        super();
        this.executor = executor;
        this.file = new File(executor.getFileName());
        this.args = args;
        this.statement = null;
    }

    public ExecutorTask(File file, Map<String, Object> args) {
        super();
        this.executor = null;
        this.file = file;
        this.args = args;
        this.statement = null;
    }

    public ExecutorTask(String statement) {
        super();
        this.executor = null;
        this.file = null;
        this.args = null;
        this.statement = statement;
    }

    public ExecutorTask(App.ExecutionStage stage) {
        super();
        this.executor = null;
        this.file = stage.file;
        this.args = stage.args;
        this.statement = stage.statement;
    }

    @Override
    protected Object doInBackground() throws Exception {
        try {
            setCommand((file != null) ? file.toString() : statement);
            setProgress(0);
            Object ret = null;
            if (file != null) {
                ret = Context.getApp().evalFile(file, args, true);
            } else if (statement != null) {
                ret = Context.getApp().evalStatement(statement);
            }
            setProgress(100);
            checkShowReturn(ret);
            return ret;
        } catch (ScriptException ex) {
            checkShowException(ex);
            throw ex;
        } catch (Exception ex) {
            sendErrorApp(ex);
            throw ex;
        } finally {

        }
    }
    
}
