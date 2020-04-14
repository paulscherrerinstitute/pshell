package ch.psi.pshell.ui;

import javax.script.ScriptException;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.core.CommandSource;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.swing.Executor;
import ch.psi.pshell.ui.Preferences.ScriptPopupDialog;
import ch.psi.utils.IO;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.swing.SwingUtils;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
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
                return new QueueTask(enabled, getFile(file), convertArgString(args), error);
            } else if (!args.isEmpty()) {
                return new QueueTask(enabled, args, error);
            } else {
                return new QueueTask(false, null, convertArgString(args), error);
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

        static Map<String, Object> convertArgString(String args) {
            try {
                Map<String, String> data = QueueParsDialog.getParsMap(args);

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                for (String key : data.keySet()) {
                    if (sb.length() > 1) {
                        sb.append(", ");
                    }
                    sb.append("\"").append(key).append("\":").append(data.get(key));
                }
                sb.append("}");

                return (Map) JsonSerializer.decode(sb.toString(), Map.class);
            } catch (IOException ex) {
                Logger.getLogger(QueueExecution.class.getName()).log(Level.WARNING, args, ex);
            }

            return new HashMap<>();
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

        public PushUpstream(boolean allBranches, boolean force) {
            super();
            this.allBranches = allBranches;
            this.force = force;
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
                Context.getInstance().pushToUpstream(allBranches, force);
                msg = "Success pushing to upstream";
                App.getInstance().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
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
