package ch.psi.pshell.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A ThreadFactory that allows setting the the thread name.
 */
public class NamedThreadFactory implements ThreadFactory {

    final String name;
    int count;
    volatile Thread last;

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        if (count++ > 1) {
            thread.setName(String.format("%s - %d", name, count));
        } else {
            thread.setName(name);
        }
        last=thread;
        return thread;
    }
    
    public void onCreateThread(Thread thread){        
    }
    
    public Thread getLast(){
        return last;
    }
}
