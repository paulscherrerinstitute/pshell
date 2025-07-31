package ch.psi.pshell.utils;

/**
 *
 */
public interface EventBusListener {

    void onMessage(final Message message) throws Exception;
    
}
