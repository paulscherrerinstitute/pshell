package ch.psi.pshell.security;

/**
 *
 */
public interface UserAuthenticator {

    public boolean authenticate(String user, String pwd);
}
