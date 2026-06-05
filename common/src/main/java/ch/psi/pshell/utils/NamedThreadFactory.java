package ch.psi.pshell.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A ThreadFactory that allows setting the thread name.
 */
public class NamedThreadFactory implements ThreadFactory {

    final String name;
    final Boolean daemon;
    final boolean firstIndexed;
    final ThreadGroup threadGroup;
    volatile int count;    
    volatile Thread last;
    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    public NamedThreadFactory(String name) {
        this(name, null);
    }

    public NamedThreadFactory(String name, Boolean daemon) {
        this(name, daemon, false);
    }
    
    public NamedThreadFactory(String name, Boolean daemon, ThreadGroup threadGroup) {
        this(name, daemon, threadGroup, false);
    }    
    
    public NamedThreadFactory(String name, Boolean daemon, boolean firstIndexed) {
        this(name, daemon, null, firstIndexed);
    }

    public NamedThreadFactory(String name, Boolean daemon, ThreadGroup threadGroup, boolean firstIndexed) {
        this.name = (name==null) ? "named_pool-" + poolNumber.getAndIncrement() : name;
        this.daemon = daemon;
        this.firstIndexed = firstIndexed;
        this.threadGroup = threadGroup;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = (threadGroup!=null) ? 
                new Thread(threadGroup,r):
                Executors.defaultThreadFactory().newThread(r);
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
