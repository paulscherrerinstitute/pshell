package ch.psi.utils;

/**
 *
 */
public interface EventBusListener {

    void onMessage(final Message message) throws Exception;
    
}
