package ch.psi.pshell.security;

/**
 * The listener interface for receiving user manager events.
 */
public interface SecurityListener {

    void onUserChange(User user, User former);
}
