package ch.psi.pshell.security;

/**
 * The listener interface for receiving user manager events.
 */
public interface UsersManagerListener {

    void onUserChange(User user, User former);
}
