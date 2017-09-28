package ch.psi.pshell.core;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the creation and disposal of tasks (background script executions), periodic or single
 * execution.
 */
public class TaskManager implements AutoCloseable {

    final ArrayList<Task> backgroundTasks = new ArrayList<>();

    /**
     * Load and start tasks defined in properties file.
     */
    public void initialize() {
        Logger.getLogger(TaskManager.class.getName()).fine("Initializing " + getClass().getSimpleName());
        try {
            Properties properties = new Properties();
            try (FileInputStream in = new FileInputStream(Context.getInstance().getSetup().getTasksFile())) {
                properties.load(in);
            }

            for (String script : properties.stringPropertyNames()) {
                Logger.getLogger(TaskManager.class.getName()).info("Starting task: " + script);
                try {
                    String value = properties.getProperty(script);
                    Integer interval = Integer.valueOf(value) * 1000; //Convert to ms
                    if (interval > 0) {
                        create(script, interval, interval);
                    } else {
                        create(script, 1000, -1);
                    }
                    start(script);
                } catch (Exception ex) {
                    Logger.getLogger(TaskManager.class.getName()).log(Level.WARNING, null, ex);
                }
            }
            Logger.getLogger(TaskManager.class.getName()).fine("Finished " + getClass().getSimpleName() + " initialization");
        } catch (Exception ex) {
            Logger.getLogger(TaskManager.class.getName()).log(Level.FINE, null, ex);
        }
    }

    public void create(String script, int delay, int interval) {
        synchronized (backgroundTasks) {
            remove(script);
            Task task = new Task(script, delay, interval);
            backgroundTasks.add(task);
        }
    }

    public void start(String script) {
        synchronized (backgroundTasks) {
            Task task = get(script);
            if (task != null) {
                task.start();
            }
        }
    }

    public void stop(String script) {
        stop(script, false);
    }

    public void stop(String script, boolean force) {
        synchronized (backgroundTasks) {
            Task task = get(script);
            if (task != null) {
                task.stop(force);
            }
        }
    }

    public void remove(String script) {
        remove(script, false);
    }

    public void remove(String script, boolean force) {
        synchronized (backgroundTasks) {
            stop(script, force);
            Task task = get(script);
            if (task != null) {
                backgroundTasks.remove(task);
            }
        }
    }

    public Task get(String script) {
        synchronized (backgroundTasks) {
            for (Task task : backgroundTasks) {
                if (task.getScript().equals(script)) {
                    return task;
                }
            }
        }
        return null;
    }

    public Task[] getAll() {
        synchronized (backgroundTasks) {
            return backgroundTasks.toArray(new Task[0]);
        }
    }

    void removeAll() {
        synchronized (backgroundTasks) {
            for (Task task : backgroundTasks) {
                task.close();
            }
            backgroundTasks.clear();
        }
    }

    @Override
    public void close() throws Exception {
        removeAll();
    }
}
