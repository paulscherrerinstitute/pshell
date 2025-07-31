package ch.psi.pshell.app;

import ch.psi.pshell.utils.State;

/**
 * The listener interface for receiving application events.
 */
public interface AppListener {

    default void onStateChanged(State state, State former) {
    }

    default boolean canExit(Object source) {
        return true;
    }

    default void willExit(Object source) {
    }
}
