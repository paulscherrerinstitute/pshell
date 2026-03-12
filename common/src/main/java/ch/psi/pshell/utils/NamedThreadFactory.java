package ch.psi.pshell.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A ThreadFactory that allows setting the thread name.
 */
public class NamedThreadFactory implements ThreadFactory {

    final String name;
    final Boolean daemon;
    final boolean firstIndexed;
    volatile int count;    
    volatile Thread last;

    public NamedThreadFactory(String name) {
        this(name, null);
    }

    public NamedThreadFactory(String name, Boolean daemon) {
        this(name, daemon, false);
    }

    public NamedThreadFactory(String name, Boolean daemon, boolean firstIndexed) {
        this.name = name;
        this.daemon = daemon;
        this.firstIndexed = firstIndexed;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        int id;
        synchronized(this) {
            id = ++count;
        }
        if ((firstIndexed) || (id > 1)) {
            thread.setName(String.format("%s - %d", name, id));
        } else {
            thread.setName(name);
        }
        if (daemon!=null){
            thread.setDaemon(daemon);
        }
        onCreateThread(thread);
        last=thread;        
        return thread;
    }
    
    public void onCreateThread(Thread thread){                
    }
    
    public Thread getLast(){
        return last;
    }
}
