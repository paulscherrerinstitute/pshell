package ch.psi.pshell.framework;

import ch.psi.pshell.framework.QueueTask.QueueTaskErrorAction;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Condition;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Task to run a queue of scripts
 */
public class QueueExecution extends Task {
    

    public interface QueueExecutionListener {

        void onStartTask(QueueTask task, int index);

        void onFinishedTask(QueueTask task, int index, Object ret, Exception ex);

        void onAborted(QueueTask task, int index, boolean userAbort, boolean skipped);

        void onFinishedExecution(QueueTask task);
    }

    

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
                                    return (paused == false) || aborted;
                                }
                            }, -1);
                            if (aborted) {
                                break;
                            }
                            Context.waitState(ch.psi.pshell.utils.State.Ready, 5000); //Tries to keep up with some concurrent execution.
                            Object ret = (task.file != null) ? Context.getApp().evalFile(task.file, task.args) : Context.getApp().evalStatement(task.statement);
                            if (Context.getState().isProcessing()) {
                                Logger.getLogger(QueueExecution.class.getName()).info("State busy after task: waiting completion of next stage");
                                Context.waitStateNotProcessing(-1);
                            }
                            if (Context.isAborted()) {
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
                        if (aborted || Context.isAborted() || skipped) {
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
                            if (aborted || Context.isAborted() || skipped) {
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
                if (aborted || Context.isAborted() || skipped) {
                    if (listener != null) {
                        listener.onAborted(task, currentIndex, true, false);
                    }
                    if (aborted) {
                        return null;
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
            Context.getApp().abortEval(currentTask.file);
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
