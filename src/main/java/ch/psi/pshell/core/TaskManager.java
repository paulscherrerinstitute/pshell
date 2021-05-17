package ch.psi.pshell.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
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
                    Integer interval = ((Double)((Double.valueOf(value)) * 1000)).intValue(); //Convert to ms
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
        } catch (FileNotFoundException | NoSuchFileException ex) {
            Logger.getLogger(TaskManager.class.getName()).log(Level.FINER, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(TaskManager.class.getName()).log(Level.WARNING, null, ex);
        }
        Logger.getLogger(TaskManager.class.getName()).fine("Finished " + getClass().getSimpleName() + " initialization");
    }

    
    public void add(Task task) {
        synchronized (backgroundTasks) {
            remove(task.script);
            backgroundTasks.add(task);
        }
    }

        
    public Task create(String script, int delay, int interval) {
        synchronized (backgroundTasks) {
            Task task = new Task(script, delay, interval);
            add(task);
            return task;
        }
    }

    public void start(Task task) {
        if (task != null) {
            synchronized (backgroundTasks) {            
                task.start();
            }
        }        
    }
    
    public void start(String script) {
        start(get(script));
    }
   

    public void stop(String script) {
        stop(script, false);
    }

    public void stop(String script, boolean force) {
        stop(get(script), force);
    }
    
    public void stop(Task task, boolean force) {
        if (task != null) {
            synchronized (backgroundTasks) {            
                task.stop(force);
            }
        }
    }

    public void remove(String script) {
        remove(script, false);
    }

    public void remove(String script, boolean force) {
        remove(get(script), force);
    }
    
    public void remove(Task task, boolean force) {
        synchronized (backgroundTasks) {            
            if (task != null) {
                stop(task, force);
                backgroundTasks.remove(task);
            }
        }
    }

    public Task get(String script) {
        if (script!=null){
            synchronized (backgroundTasks) {
                for (Task task : backgroundTasks) {
                    if (task.getScript().equals(script)) {
                        return task;
                    }
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
