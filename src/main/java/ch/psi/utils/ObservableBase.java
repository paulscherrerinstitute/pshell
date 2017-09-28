package ch.psi.utils;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Observable, managing listeners : "observer" objects in the Observer pattern.
 */
public class ObservableBase<T> implements Observable<T>, AutoCloseable {

    private final transient List<T> listeners = new ArrayList<>();

    @Override
    public void addListener(T listener) {
        synchronized (listeners) {
            if ((listener != null) && (!listeners.contains(listener))) {
                listeners.add(listener);
                onListenersChanged();
            }
        }
    }

    @Override
    public void removeListener(T listener) {
        synchronized (listeners) {
            if (listeners.remove(listener)) {
                onListenersChanged();
            }
        }
    }

    @Override
    public void removeAllListeners() {
        synchronized (listeners) {
            if (listeners.size() > 0) {
                listeners.clear();
                onListenersChanged();
            }
        }
    }

    @Override
    @Transient
    public List<T> getListeners() {
        synchronized (listeners) {
            ArrayList<T> ret = new ArrayList<>();
            ret.addAll(listeners);
            return ret;
        }
    }

    protected void onListenersChanged() {
    }

    @Override
    public void close() {
        synchronized (listeners) {
            listeners.clear();
        }
    }
}
