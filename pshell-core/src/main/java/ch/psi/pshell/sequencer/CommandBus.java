package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Threading;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.python.core.PyBaseException;

public class CommandBus implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(CommandBus.class.getName());

    //final BlockingQueue<CommandInfo> queue;
    final LinkedBlockingDeque<CommandInfo> queue;
    final int timeToLive;
    final int size;
    //private final Map<Thread, CommandInfo> newestByThread = new ConcurrentHashMap<>();
    final Object newEventLock = new Object();
    boolean alive = true;

    public CommandBus(int size, int timeToLive) {
        this.queue =  (size>0) ? new LinkedBlockingDeque<>(size) : new LinkedBlockingDeque<>();
        //this.queue = new ArrayBlockingQueue<>(size);
        this.size = size;
        this.timeToLive = timeToLive;
    }
    
    //logger.log(Level.FINER, "Command bus full: dropped oldest command {0}", drop);
    
    public void commandStarted(CommandInfo info) {
        if (info != null) {            
            synchronized (cmdBusChangeEvent) {
                // Try to enqueue, drop oldest if full
                if (!queue.offer(info)) {   //offerLast                    
                    //CommandInfo drop = queue.poll(); // drop oldest - pollFirst
                    //Drop the older that is not running
                    
                    boolean dropped = false;

                    // Use iterator for safe removal while scanning
                    Iterator<CommandInfo> it = queue.iterator();
                    while (it.hasNext()) {
                        CommandInfo ci = it.next();
                        if (!ci.isRunning()) {
                            it.remove(); // remove first non-running element
                            dropped = true;
                            break;
                        }
                    }

                    // If no non-running element found, remove absolute oldest
                    if (!dropped) {
                        CommandInfo drop = queue.pollFirst();
                        if (drop != null) {
                            logger.log(Level.WARNING, "Command bus full: dropped oldest running command {0}", drop);
                        }
                    }                    
                    queue.offer(info);                    
                } 
                //newestByThread.put(info.thread, info); 
                synchronized (newEventLock) {
                    newEventLock.notifyAll(); // wake up waiting threads
                }        
                
                //commandInfo.put(Thread.currentThread(), info);
                onCommandStarted(info);
                cmdBusChangeEvent.notifyAll();
            }
        }
    }

    public void commandFinished(CommandInfo info, Object result) {
        synchronized (cmdBusChangeEvent) {
            if (Context.isDebug()){
                if (result instanceof Throwable t){
                    t.printStackTrace();
                }                
            }                            
            if (info != null) {
                if (result instanceof PyBaseException) {
                    result = new Exception(result.toString());
                }
                info.result = result;
                info.end = System.currentTimeMillis();

                int threshold = (int) ((size>0) ? (size * 0.9) : 100);
                if (queue.size() >= threshold) {
                    dropOlderThan(timeToLive); 
                }        

                onCommandFinished(info);
            }            
            cmdBusChangeEvent.notifyAll();
        }        
    }
    
    public final Object cmdBusChangeEvent = new Object();
    
    public void waitCommandBusChangeEvent() throws InterruptedException {
        synchronized (cmdBusChangeEvent) {
            cmdBusChangeEvent.wait();
        }
    }
    

    /** Called by the consumer side, waiting for next command with timeout */
    public long waitThreadCommand(Thread thread, int timeout) throws InterruptedException {        
        if (thread==null){
            thread = Thread.currentThread();
        }
        Chrono chrono = new Chrono();
        while (true) {
            long remaining = timeout < 0 ? Long.MAX_VALUE : Math.max(timeout - chrono.getEllapsed(),1);
            if (!alive){
                return -1;
            }                        
            CommandInfo info = getNewest(thread);
            if (info!=null){
                return info.id;
            }
               
            if (timeout >= 0) {
                if ((timeout - chrono.getEllapsed()) < 0){
                    break;
                }
            }                        
            synchronized (newEventLock) {
                if (remaining > 0) {
                    newEventLock.wait(Math.min(remaining, 100)); // wakeup periodically to check timeout
                }
            }            
        }
        return 0;
    }
    
    public long waitAsyncCommand(Threading.VisibleCompletableFuture cf) throws InterruptedException {
        long now = System.currentTimeMillis();
        Thread thread = cf.waitRunningThread(1000);
        if (thread == null) {
            return -1;
        }
        //CommandInfo current = getThreadCommand(thread, false);
        //if ((current != null) && (current.start >= now)) {
        //    return current.id;
        //}
        long ret = waitThreadCommand(thread, 1000);
        return ret;
    }    
    
    public CommandInfo getNewest(Thread t) {
        Iterator<CommandInfo> it = queue.descendingIterator(); // tail -> head
        while (it.hasNext()) {
            CommandInfo info = it.next();
            if (info.thread == t) {
                return info;
            }
        }
        return null;
        
        //ArrayBlockingQueue implementation
        //CommandInfo newest = null;
        //for (CommandInfo info : getQueue()) { 
        //    if (info.thread == t) {
        //        if (newest == null || info.getAge() < newest.getAge()) {
        //            newest = info;
        //        }
        //    }
        //}
        //return newest;
        
        //ConcurrentHashMap implementation
        //return newestByThread.get(t); 
    }    

    /** Drop all events */
    public void clear() {
        queue.clear();
    }

    /** Drop events whose thread is the current thread */
    public void drop() {
        drop(Thread.currentThread());
    }

    /** Drop events whose matching a specific thread  */
    public void drop(Thread thread) {
        queue.removeIf(cmd -> cmd.thread == thread);
        //newestByThread.remove(thread); 
    }    

    /** Drop all events matching a specific id */
    public void drop(long id) {
        queue.removeIf(cmd -> cmd.id == id);
    }    
    
    public void dropOlderThan(long age) {
        queue.removeIf(cmd -> !cmd.isRunning() && (cmd.getAge()>age));
    }    
    
    public List<CommandInfo> getCommands() {        
        //Tipically synchronization is not required.
        return new ArrayList<>(queue); // uses the Collection constructor
    }           
    
    Collection<CommandInfo> getQueue() {        
        //Thread-safe but not strongly consistent for searches.
        return queue;  
        ////For strongly consistestent:
        //synchronized(cmdBusChangeEvent){
        //    return new ArrayList<>(queue);
        //}
    }           
    
    
    public CommandInfo getCurrentCommand() {
        return getCurrentCommand(Thread.currentThread(), false);
    }

    public CommandInfo getCurrentCommand(boolean parent) {
        return getCurrentCommand(Thread.currentThread(), parent);
    }

    public CommandInfo getCurrentCommand(Thread thread) {
        return getCurrentCommand(thread, false);
    }

    public CommandInfo getCurrentCommand(Thread thread, boolean parent) {
        CommandInfo threadCommand = getThreadCommand(thread, parent);
        if (threadCommand != null) {
            do {
                if (threadCommand.isRunning()) {
                    return threadCommand;
                }
                threadCommand = threadCommand.parent;
            } while (threadCommand != null);
        }
        return null;
    }

    public CommandInfo getThreadCommand(Thread thread) {
        return getThreadCommand(thread, false);
    }

    public CommandInfo getThreadCommand(Thread thread, boolean parent) {
        CommandInfo ret = getNewest(thread);
        if (ret!=null) {
            if (parent) {
                while (ret.parent != null) {
                    ret = ret.parent;
                }
            }
            return ret;
        }
        return null;
    }

    public CommandInfo getInterpreterThreadCommand() {
        return getInterpreterThreadCommand(false);
    }

    public CommandInfo getInterpreterThreadCommand(boolean parent) {
        return getThreadCommand(Context.getSequencer().getInterpreterThread(), parent);
    }
    
    public CommandInfo getCommand(long id) {
        if (id < 0) {
            return getInterpreterThreadCommand(true);
        }        
        for (CommandInfo info : getQueue()) {   
            if (info.id == id) {
                return info;
            }
        }
        return null;
    }

    public List<CommandInfo> getCurrentCommands() {
        return queue.stream()
                    .filter(CommandInfo::isRunning)
                    .toList(); 
    }
    
    public List<CommandInfo> getCurrentCommands(long runningTime) {
        return queue.stream()
                .filter(CommandInfo::isRunning)
                .filter(ci->ci.getRunningTime()>runningTime)                
                .toList(); 
    }    
    
    
    public boolean abort(final CommandSource source, long id) throws InterruptedException {
        boolean aborted = false;
        for (CommandInfo ci : getQueue()) {
            if (id == -1) {
                if (ci.background == false) {
                    ci.abort();
                    aborted = true;
                }
            } else if (ci.id == id) {
                ci.abort();
                aborted = true;
                break;
            }
        }
        return aborted;
    }

    public boolean join(long id) throws InterruptedException {
        CommandInfo cmd = getCommand(id);
        if (cmd != null) {
            cmd.join();
            return true;
        }
        return false;
    }

    public boolean isRunning(long id) {
        CommandInfo cmd = getCommand(id);
        if (cmd != null) {
            return cmd.isRunning();
        }
        return false;
    }
    

    @Override
    public void close() throws Exception {
        alive = false;
        clear();
        synchronized (newEventLock) {
            newEventLock.notifyAll(); // wake any waiting threads immediately
        }        
        synchronized (cmdBusChangeEvent){
            cmdBusChangeEvent.notifyAll();
        }
    }
    
    public int getSize(){
        return queue.size();
    }
        
    public int getMaxSize(){
        return size;
    }

    public Map getResult(long id) throws Exception {
        CommandInfo cmd;
        if (id < 0) {
            cmd = getInterpreterThreadCommand(true);
            if (cmd != null) {
                id = cmd.id;
            }
        } else {
            cmd = getCommand(id);
        }
        cmd = getCommand(id);
        Map ret = new HashMap();
        ret.put("id", id);
        ret.put("exception", null);
        ret.put("return", null);
        String status;
        if (cmd == null) {
            if (id == 0) {
                status = "unlaunched";
            } else {
                status = (id >= CommandInfo.commandId) ? "invalid" : "removed";
            }
        } else {
            if (cmd.isRunning()) {
                status = "running";
            } else {
                if (cmd.isAborted()) {
                    status = "aborted";
                } else if (cmd.result instanceof Exception ex) {
                    status = "failed";
                    ret.put("exception", ex.toString());
                } else if (cmd.result instanceof InterpreterResult res) {
                    if (res.complete == false) {
                        status = "aborted";
                    } else if (res.exception != null) {
                        status = "failed";
                        ret.put("exception", res.exception.toString());
                    } else {
                        status = "completed";
                        ret.put("return", res.result);
                    }
                } else {
                    status = "completed";
                    ret.put("return", cmd.result);
                }
            }
        }
        ret.put("status", status);
        return ret;
    }    
    
    protected void onCommandStarted(CommandInfo info) {        
    }
    
    protected void onCommandFinished(CommandInfo info) { 
    }
    
    
}
