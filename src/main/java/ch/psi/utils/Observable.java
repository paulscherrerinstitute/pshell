package ch.psi.utils;

import java.util.List;

/**
 * Interface for "subject" objects in the Observer pattern.
 */
public interface Observable<T> {

    void addListener(T listener);

    List<T> getListeners();

    void removeListener(T listener);

    default void removeAllListeners() {
        for (T listener : getListeners()) {
            removeListener(listener);
        }
    }
}
