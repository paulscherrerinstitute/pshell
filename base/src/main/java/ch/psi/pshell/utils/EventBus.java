package ch.psi.pshell.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 */
public class EventBus extends ObservableBase<EventBusListener> implements AutoCloseable {
    private static Logger logger = Logger.getLogger(EventBus.class.getName());    
    
    
    public enum Mode{            
         SYNC, 
         ASYNC,
         PARALLEL,
         EVENT_LOOP
    }
    
    private final Mode mode;
    ExecutorService executor;        
    Map<EventBusListener, ExecutorService> executors;
    Map<ExecutorService, Thread> threads;
    
    public EventBus(){
        this(Mode.SYNC);
    }
    
    public EventBus(Mode mode){
        this.mode=mode;
        if (mode==Mode.ASYNC){
            createExecutor(null);
        } else if (mode==Mode.PARALLEL){
            executors = new HashMap<>();
            threads  = new HashMap<>();
        }
    }    
    

    private void _sendMessage(EventBusListener listener, Message message){
        try{
            listener.onMessage(message);
        } catch (Exception ex){
            logger.log(Level.FINE, null, ex);
        }        
    }

    private void _sendMessage(Message message){
        for (EventBusListener listener : this.getListeners()){
            _sendMessage(listener, message);  
        }
    }
        
    public void post(Message message){
        post(message, mode);
    }
                
    public void post(Message message, Mode mode){
        switch (mode){
            case SYNC -> _sendMessage(message);
                
            case ASYNC -> executor.submit(() -> {
                    _sendMessage(message);
                });
                
            case PARALLEL -> {
                for (EventBusListener listener : this.getListeners()){
                    ExecutorService executor = executors.get(listener);
                    if (executor!=null){
                        executor.submit(() -> {
                            _sendMessage(listener, message);
                        });
                    }
                }
            }
                
            case EVENT_LOOP -> SwingUtilities.invokeLater(()->{
                    _sendMessage(message);
                });
        }
    }
    
    //Same interface as guava
    public void register(EventBusListener listener){
        addListener(listener);        
        if (mode==Mode.PARALLEL){            
            executors.put(listener, createExecutor(listener));
        }
    }

    public void unregister(EventBusListener listener){
        removeListener(listener);
        if (mode==Mode.PARALLEL){            
            closeExecutor(executors.get(listener));
        }        
    }
    
    private ExecutorService createExecutor(EventBusListener listener){
        ExecutorService _executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("EventBus Thread"){
            @Override
            public void onCreateThread(Thread thread){   
                ExecutorService executor = executors.get(listener);
                if (executor!=null){
                    threads.put(executors.get(listener), thread);
                }
            }
        });

        if (listener!=null){
            executors.put(listener, _executor);
        } else {
            executor = _executor;
        }
        return _executor;
    }
    
    
    private void closeExecutor(ExecutorService es){
        try {                    
            if (es != null) {
                es.shutdownNow();
                Thread thread = threads.get(es);
                if (thread != null){
                    Threading.stop(thread, true, 3000);
                }
            }            
        } catch (Exception e) {
        }   
    }

    @Override
    public void close() {
        if (mode==Mode.ASYNC){
            closeExecutor(executor); 
        } else if (mode==Mode.PARALLEL){
            for (EventBusListener listener : this.getListeners()){
                closeExecutor(executors.get(listener));
            }
        }
        super.close();
    }    
}
