package ch.psi.pshell.sequencer;

/**
 *
 */
public interface EventListener {
    void sendEvent(String name, Object value);
}
