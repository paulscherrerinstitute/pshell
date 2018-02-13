package ch.psi.utils;

import ch.psi.pshell.swing.StripChart;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * This class posts objects to the event loop. Multiple sequential calls are buffered in a single
 * invoke.
 */
public abstract class InvokingProducer<T>  implements AutoCloseable{

    final ArrayList<T> elementBuffer;
    final AtomicBoolean updating;
    int dispatchTimer;
    Timer timer;

    public InvokingProducer() {
        elementBuffer = new ArrayList<>();
        updating = new AtomicBoolean(false);
    }
    
    
    
    public void setDispatchTimer(int dispatchTimer){
        if (this.dispatchTimer != dispatchTimer){
            if (timer!= null){
                timer.stop();
                timer = null;
            }
            this.dispatchTimer = dispatchTimer;
            
            if (dispatchTimer > 0){
                timer = new Timer(dispatchTimer, (ActionEvent e) -> {
                    try {
                        dispatch(); 
                    } catch (Exception ex) {
                    }

                });
                timer.setInitialDelay(Math.min(100, dispatchTimer));
                timer.start();                
            }  
        }
    }
  

    public int getDispatchTimer(){
        return dispatchTimer;
    }
    
    public void reset() {
        synchronized (elementBuffer) {
            elementBuffer.clear();
            updating.set(false);
        }
    }

    public void post(T obj) {
        synchronized (elementBuffer) {
            elementBuffer.add(obj);
            if (updating.compareAndSet(false, true)) {
                if (dispatchTimer<=0){
                    SwingUtilities.invokeLater(() -> {
                        dispatch();
                    });
                }
            }
        }
    }

    void dispatch() {
        try {
            ArrayList<T> elements;
            synchronized (elementBuffer) {
                elements = (ArrayList<T>) elementBuffer.clone();
                elementBuffer.clear();
                updating.set(false);
            }
            for (T element : elements) {
                consume(element);
            }
        } catch (Exception ex) {
            Logger.getLogger(StripChart.class.getName()).log(Level.FINE, null, ex);
        }
    }

    abstract protected void consume(T obj);

    @Override
    public void close() throws Exception {
        setDispatchTimer(0);
    }
}
