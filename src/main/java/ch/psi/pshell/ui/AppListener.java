package ch.psi.pshell.ui;

import ch.psi.utils.State;

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
