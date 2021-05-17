package ch.psi.pshell.core;

import ch.psi.utils.Threading;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background execution of scripts by the interpreter.
 */
public class Task implements AutoCloseable {

    final String script;
    final int delay;
    final int interval;
    ScheduledExecutorService scheduler;

    public Task(String script, int delay, int interval) {
        this.script = script;
        this.delay = delay;
        this.interval = interval;
    }

    public void start() {
        if (scheduler == null) {
            scheduler = Threading.scheduleAtFixedRateNotRetriggerable(() -> {
                execute();
            }, delay, interval, TimeUnit.MILLISECONDS, "Background task scheduler: " + script);
        }
    }

    public void stop(boolean force) {
        if (scheduler != null) {
            if (force) {
                scheduler.shutdownNow();
            } else {
                scheduler.shutdown();
            }
            scheduler = null;
        }
    }

    public boolean isStarted() {
        return (scheduler != null);
    }

    public boolean isRunning() {
        return running;
    }

    public String getScript() {
        return script;
    }

    public int getInterval() {
        return interval;
    }

    final Object lock = new Object();
    volatile boolean running;

    public void waitRunning(int timeout) throws InterruptedException {
        while (!running) {
            synchronized (lock) {
                lock.wait(timeout);
            }
        }
    }

    public void waitNotRunning(int timeout) throws InterruptedException {
        while (running) {
            synchronized (lock) {
                lock.wait(timeout);
            }
        }
    }

    public void run() throws Exception {
        if (running) {
            throw new Exception("Task already running: " + script);
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                execute();
            }
        };
        if (isStarted()) {
            scheduler.submit(runnable);
        } else {
            scheduler = Threading.scheduleAtFixedRateNotRetriggerable(runnable, 0, -1, TimeUnit.MILLISECONDS, "Background task scheduler: " + script);
            waitRunning(3000);
            stop(false);
        }
    }

    void execute() {
        if (running) {
            Logger.getLogger(Task.class.getName()).log(Level.WARNING, "Task already running: " + script);
            return;
        }
        try {
            Logger.getLogger(Task.class.getName()).fine("Starting background task: " + script);
            running = true;
            synchronized (lock) {
                lock.notifyAll();
            }
            Context.getInstance().getState().assertActive();
            Context.getInstance().evalFileBackground(CommandSource.task, script);
            Logger.getLogger(Task.class.getName()).fine("Finished background task: " + script);
        } catch (Exception ex) {
            Logger.getLogger(Task.class.getName()).log(Level.WARNING, null, ex);
        } finally {
            running = false;
            synchronized (lock) {
                lock.notifyAll();
            }

        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getScript()).append(" ");
        sb.append(getInterval()).append(" ");
        sb.append(isStarted() ? "started" : "stopped");
        return sb.toString();
    }
    
    @Override
    public void close() {
        stop(true);
    }
}
