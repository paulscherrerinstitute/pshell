package ch.psi.utils;

import ch.psi.pshell.swing.StripChart;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * This class posts objects to the event loop. Multiple sequential calls are buffered in a single
 * invoke.
 */
public abstract class InvokingProducer<T> {

    final ArrayList<T> elementBuffer;
    final AtomicBoolean updating;

    public InvokingProducer() {
        elementBuffer = new ArrayList<>();
        updating = new AtomicBoolean(false);
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
                SwingUtilities.invokeLater(() -> {
                    dispatch();
                });
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
}
