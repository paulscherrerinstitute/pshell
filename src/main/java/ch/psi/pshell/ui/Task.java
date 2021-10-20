package ch.psi.pshell.ui;

import javax.script.ScriptException;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.core.CommandSource;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.swing.Executor;
import ch.psi.pshell.ui.Preferences.ScriptPopupDialog;
import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import ch.psi.utils.IO;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.swing.SwingUtils;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Abstraction for a background activity in the Workbench, extending
 * SwingWorker.
 */
public abstract class Task extends SwingWorker<Object, Void> {

    protected Task() {
        if (App.getInstance().getStatusBar() != null) {
            addPropertyChangeListener(App.getInstance().getStatusBar());
            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    String propertyName = e.getPropertyName();
                    if ("state".equals(propertyName)) {
                        StateValue state = (StateValue) (e.getNewValue());
                        switch (state) {
                            case STARTED:
                                startTimestamp = System.currentTimeMillis();
                                if (trackTime) {
                                    firePropertyChange("timer", -1L, 0L);
                                    if (timerTask != null) {
                                        timerTask.stop();
                                    }
                                    timerTask = new Timer(1000, (ActionEvent ae) -> {
                                        firePropertyChange("timer", -1L, getExecutionTime());
                                    });
                                    timerTask.start();
                                }
                                break;
                            case DONE:
                                endTimestamp = System.currentTimeMillis();
                                if (trackTime) {
                                    firePropertyChange("timer", -1L, -1L);
                                    if (timerTask != null) {
                                        timerTask.stop();
                                        timerTask = null;
                                    }
                                }
                                break;
                        }
                    }
                }
            });

        }
    }

    @Override
    public String toString() {
        return Str.toTitleCase(this.getClass().getSimpleName());
    }
    String message = "";
    boolean trackTime = true;

    protected void setMessage(String message) {

        String oldMessage, newMessage;
        synchronized (this) {
            oldMessage = this.message;
            this.message = message;
            newMessage = this.message;
        }
        firePropertyChange("message", oldMessage, newMessage);
    }

    protected void setCommand(String command) {
        firePropertyChange("command", null, command);
    }

    public String getMessage() {
        return message;
    }

    Timer timerTask;

    volatile long startTimestamp;
    volatile long endTimestamp;

    public long getStartTime() {
        if (getState() != StateValue.PENDING) {
            long timespan = System.currentTimeMillis() - startTimestamp;
            return System.currentTimeMillis() - timespan;
        }
        return -1;
    }

    public long getExecutionTime() {
        if (getState() == StateValue.STARTED) {
            return System.currentTimeMillis() - startTimestamp;
        } else if (endTimestamp != 0) {
            return endTimestamp - startTimestamp;
        } else {
            return -1;
        }
    }

    /**
     * Task to run or debug scripts.
     */
    public static class ScriptExecution extends Task {

        volatile Statement currentStatement;

        public ScriptExecution(String fileName, Statement[] statements, Object args, boolean pauseOnStart, boolean debug) {
            super();

            this.fileName = fileName;
            this.statements = statements;
            this.args = args;
            this.pauseOnStart = pauseOnStart;
            this.debug = debug;

            Context.getInstance().addListener(new ContextAdapter() {
                @Override
                public void onNewStatement(final Statement statement) {
                    currentStatement = statement;
                }

                @Override
                public void onExecutedStatement(Statement statement) {
                    if (Context.getInstance().isRunningStatements()) {
                        if (!isDone() && statements != null) {
                            View view = App.getInstance().getMainFrame();
                            if ((view != null) && (!view.hasOngoingScan())) {   //Scan progress has priority
                                setProgress((int) (((double) statement.number) * 100 / statements.length));
                            }
                        }
                    }
                }

                @Override
                public void onExecutedFile(String fileName, Object result) {
                    Context.getInstance().removeListener(this);
                }

            });

        }

        public Statement getCurrentStatement() {
            return currentStatement;
        }

        String scriptName;

        final String fileName;
        final Statement[] statements;
        final Object args;
        final boolean pauseOnStart;
        final boolean debug;

        public String getScriptName() {
            return scriptName;
        }

        @Override
        protected Object doInBackground() throws Exception {
            currentStatement = null;
            scriptName = Context.getInstance().getStandardScriptName(fileName);

            setCommand(scriptName);
            setProgress(0);
            try {
                Object ret = null;
                if (debug) {
                    ret = Context.getInstance().evalStatements(statements, pauseOnStart, fileName, args);
                } else {
                    ret = Context.getInstance().evalFile(fileName, args);
                }
                setProgress(100);
                if (App.getInstance().getMainFrame().getPreferences().getScriptPopupDialog() == ScriptPopupDialog.Return) {
                    if ((!Context.getInstance().isAborted()) && (ret != null)) {
                        SwingUtils.showMessage(App.getInstance().getMainFrame(), "Script Return", String.valueOf(ret));
                    }
                }
                return ret;
            } catch (ScriptException ex) {
                if (App.getInstance().getMainFrame().getPreferences().getScriptPopupDialog() != ScriptPopupDialog.None) {
                    if (!Context.getInstance().isAborted()) {
                        SwingUtils.showMessage(App.getInstance().getMainFrame(), "Script Error", ex.getMessage(), -1, JOptionPane.ERROR_MESSAGE);
                    }
                }
                throw ex;
            } catch (Exception ex) {
                App.getInstance().sendError(ex.toString());
                throw ex;
            } finally {
                currentStatement = null;
            }
        }
    }

    public static class ExecutorTask extends Task {

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
                setCommand(((file != null) ? file.toString() : statement));
                setProgress(0);
                //App.getInstance().sendTaskInit(msg);
                Object ret = null;
                if (file != null) {
                    ret = App.getInstance().evalFile(file, args, true);
                } else if (statement != null) {
                    ret = App.getInstance().evalStatement(statement);
                }
                setProgress(100);
                if (App.getInstance().getMainFrame().getPreferences().getScriptPopupDialog() == ScriptPopupDialog.Return) {
                    if ((!Context.getInstance().isAborted()) && (ret != null)) {
                        SwingUtils.showMessage(App.getInstance().getMainFrame(), "Script Return", String.valueOf(ret));
                    }
                }
                return ret;
            } catch (ScriptException ex) {
                if (App.getInstance().getMainFrame().getPreferences().getScriptPopupDialog() != ScriptPopupDialog.None) {
                    if (!Context.getInstance().isAborted()) {
                        SwingUtils.showMessage(App.getInstance().getMainFrame(), "Script Error", ex.getMessage(), -1, JOptionPane.ERROR_MESSAGE);
                    }
                }
                throw ex;
            } catch (Exception ex) {
                App.getInstance().sendError(ex.toString());
                throw ex;
            } finally {
                //App.getInstance().sendTaskFinish(msg);
                //Context.getInstance().endExecution();
            }
        }
    }

    /**
     * Task to commit changes to the local Git repository.
     */
    public static class Commit extends Task {

        final String commitMessage;

        public Commit(String commitMessage) {
            super();
            this.commitMessage = commitMessage;
        }

        @Override
        protected String doInBackground() throws Exception {
            String msg = "Commiting";
            setMessage(msg);
            setProgress(0);
            try {
                App.getInstance().sendTaskInit(msg);
                Context.getInstance().assertVersioningEnabled();
                Context.getInstance().commit(commitMessage, false);
                msg = "Success commiting";
                App.getInstance().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                setMessage("Error commiting");
                App.getInstance().sendError(ex.toString());                
                throw ex;
            } finally {
                App.getInstance().sendTaskFinish(msg);
            }
        }
    }

    public static class QueueTask {

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
            filename = Context.getInstance().getSetup().expandPath(filename).trim();
            if (filename.isEmpty()) {
                return null;
            }
            String ext = IO.getExtension(filename);
            if (ext.isEmpty()) {
                filename = filename + "." + Context.getInstance().getScriptType().toString();
            }

            File file = null;
            for (String path : new String[]{Context.getInstance().getSetup().getScriptPath(),
                Context.getInstance().getSetup().getHomePath()}) {
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

            final public String name;
            final public Object var;

            InterpreterVariable(String name, Object var) {
                this.name = name;
                this.var = var;
            }

            public String toString() {
                return name;
            }
        }

        static Object expandVariables(Object obj) {
            if (obj instanceof String) {
                String str = (String) obj;
                if (str.startsWith("$")) {
                    Object var = Context.getInstance().getInterpreterVariable(str.substring(1));
                    if (var != null) {
                        obj = var;
                    }
                }

            } else if (obj instanceof List) {
                List list = (List) obj;
                for (int i = 0; i < list.size(); i++) {
                    list.set(i, expandVariables(list.get(i)));
                }
            } else if (obj instanceof Map) {
                Map map = (Map) obj;
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
                    ret = (Map) JsonSerializer.decode(sb.toString(), Map.class);
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
                                ret.put(name, JsonSerializer.decode(value, Object.class));
                            } catch (Exception e) {
                                Object var = Context.getInstance().getInterpreterVariable(value);
                                ret.put(name, (var == null) ? value
                                        : (forDisplay ? new InterpreterVariable(value, var) : var));
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

    public enum QueueTaskErrorAction {
        Resume,
        Retry,
        Once,
        Abort;
    }

    public interface QueueExecutionListener {

        void onStartTask(QueueTask task, int index);

        void onFinishedTask(QueueTask task, int index, Object ret, Exception ex);

        void onAborted(QueueTask task, int index, boolean userAbort, boolean skipped);

        void onFinishedExecution(QueueTask task);
    }

    /**
     * Task to run a queue of scripts
     */
    public static class QueueExecution extends Task {

        QueueExecutionListener listener;
        volatile QueueTask currentTask;
        volatile int currentIndex;
        QueueTask[] queue;
        boolean skipped;
        boolean aborted;
        boolean paused;

        public QueueExecution(QueueTask[] queue, QueueExecutionListener listener) {
            this.queue = queue;
            this.listener = listener;
            currentIndex = -1;
        }

        @Override
        protected Object doInBackground() throws Exception {
            try {
                for (QueueTask task : queue) {
                    currentTask = task;
                    currentIndex++;
                    boolean retried = false;
                    
                    while (!isDone() && !aborted) {
                        try {
                            skipped = false;
                                                        
                            if (listener != null) {
                                listener.onStartTask(task, currentIndex);
                            }
                            if (task.enabled) {
                                Chrono chrono = new Chrono();
                                chrono.waitCondition(new Condition() {
                                    @Override
                                    public boolean evaluate() throws InterruptedException {
                                        return ((paused==false) || aborted);
                                    }
                                }, -1);     
                                if (aborted){
                                    break;
                                }
                                
                                Context.getInstance().waitState(State.Ready, 5000); //Tries to keep up with some concurrent execution.

                                Object ret = (task.file != null)
                                        ? App.getInstance().evalFile(task.file, task.args)
                                        : App.getInstance().evalStatement(task.statement);

                                if (Context.getInstance().getState().isProcessing()) {
                                    Logger.getLogger(QueueExecution.class.getName()).info("State busy after task: waiting completion of next stage");
                                    Context.getInstance().waitStateNotProcessing(-1);
                                }
                                if (Context.getInstance().isAborted()) {
                                    if (listener != null) {
                                        listener.onAborted(task, currentIndex, true, skipped);
                                    }
                                    if (skipped) {
                                        break;
                                    }
                                    return null;
                                }
                                listener.onFinishedTask(task, currentIndex, ret, null);
                            }
                            break;
                        } catch (Exception ex) {
                            Logger.getLogger(QueueExecution.class.getName()).log(Level.WARNING, null, ex);
                            if (aborted || Context.getInstance().isAborted() || skipped) {
                                if (listener != null) {
                                    listener.onAborted(task, currentIndex, true, skipped);
                                }
                                if (!aborted && skipped) {
                                    break;
                                }
                                return null;
                            } else {
                                if (listener != null) {
                                    listener.onFinishedTask(task, currentIndex, null, ex);
                                }
                                if (task.error == QueueTaskErrorAction.Abort) {
                                    if (listener != null) {
                                        listener.onAborted(task, currentIndex, false, false);
                                    }
                                    throw ex;
                                }
                                if (task.error == QueueTaskErrorAction.Resume) {
                                    break;
                                }
                                if (task.error == QueueTaskErrorAction.Once) {
                                    if (retried) {
                                        break;
                                    }
                                    retried = true;
                                }

                                Thread.sleep(1000); //Delays restart to handle skipping and avoid high number of exceptions

                                if (aborted || Context.getInstance().isAborted() || skipped) {
                                    if (listener != null) {
                                        listener.onAborted(task, currentIndex, true, skipped);
                                    }
                                    if (!aborted && skipped) {
                                        break;
                                    }
                                    return null;
                                }
                            }
                        }
                    }
                    if (aborted || Context.getInstance().isAborted() || skipped) {
                        if (listener != null) {
                            listener.onAborted(task, currentIndex, true, false);
                        }
                        return null;
                    }
                }
                return "Success running queue";
            } finally {
                currentIndex = -1;
                if (listener != null) {
                    listener.onFinishedExecution(currentTask);
                }
                currentTask = null;
            }
        }

        public QueueTask getCurrentTask() {
            return currentTask;
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public void skip() {
            skipped = true;
            try {
                App.getInstance().abortEval(currentTask.file);
            } catch (InterruptedException ex) {
                Logger.getLogger(QueueExecution.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public void pause() {
            paused = true;
        }        

        public void resume() {
            paused = false;
        }        

        public void abort() {
            aborted = true;
        }
    }

    /**
     * Task to checkout another brunch in the local Git repository.
     */
    public static class Checkout extends Task {

        final boolean branch;
        final String name;

        public Checkout(boolean branch, String name) {
            super();
            this.branch = branch;
            this.name = name;
        }

        @Override
        protected String doInBackground() throws Exception {
            String msg;
            if (branch) {
                msg = "Checking out branch: " + name;
            } else {
                msg = "Checking out tag: " + name;
            }
            setMessage(msg);
            setProgress(0);
            try {
                App.getInstance().sendTaskInit(msg);
                Context.getInstance().assertVersioningEnabled();

                if (branch) {
                    Context.getInstance().checkoutLocalBranch(name);
                } else {
                    Context.getInstance().checkoutTag(name);
                }

                msg = "Success checking out";
                App.getInstance().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                setMessage("Error checking out");
                App.getInstance().sendError(ex.toString());
                throw ex;
            } finally {
                App.getInstance().sendTaskFinish(msg);
            }
        }
    }

    /**
     * Task to pull from remote Git repository.
     */
    public static class PullUpstream extends Task {

        @Override
        protected String doInBackground() throws Exception {
            String msg = "Pulling history";
            setMessage(msg);
            setProgress(0);
            try {
                App.getInstance().sendTaskInit(msg);
                Context.getInstance().assertRemoteRepoEnabled();
                Context.getInstance().setSourceUI(CommandSource.ui); //Ensure authentication dialog comes to local interface
                Context.getInstance().pullFromUpstream();
                msg = "Success pulling from upstream";
                App.getInstance().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                setMessage("Error pulling from upstream");
                App.getInstance().sendError(ex.toString());
                throw ex;
            } finally {
                App.getInstance().sendTaskFinish(msg);
            }
        }
    }

    /**
     * Task to push to remote Git repository.
     */
    public static class PushUpstream extends Task {

        final boolean allBranches;
        final boolean force;
        final boolean tags;

        public PushUpstream(boolean allBranches, boolean force, boolean tags) {
            super();
            this.allBranches = allBranches;
            this.force = force;
            this.tags = tags;
        }

        @Override
        protected String doInBackground() throws Exception {
            String msg = "Pushing history";
            setMessage(msg);
            setProgress(0);
            try {
                App.getInstance().sendTaskInit(msg);
                Context.getInstance().assertRemoteRepoEnabled();
                Context.getInstance().setSourceUI(CommandSource.ui); //Ensure authentication dialog comes to local interface
                Context.getInstance().pushToUpstream(allBranches, force, tags);
                msg = "Success pushing to upstream";
                App.getInstance().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                setMessage("Error pushing to upstream");
                App.getInstance().sendError(ex.toString());
                throw ex;
            } finally {
                App.getInstance().sendTaskFinish(msg);
            }
        }
    }

    /**
     * Task to restart the context.
     */
    public static class Restart extends Task {

        public Restart() {
            super();
            trackTime = false;
        }

        @Override
        protected String doInBackground() throws Exception {
            try {
                Context.getInstance().restart();
                return "Ok";
            } catch (Exception ex) {
                App.getInstance().sendError(ex.toString());
                throw ex;
            } finally {
            }
        }
    }

}
