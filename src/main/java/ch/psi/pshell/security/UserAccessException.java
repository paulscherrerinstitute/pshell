package ch.psi.pshell.security;

/**
 *
 */
public class UserAccessException extends SecurityException {

    UserAccessException() {
        super("User has no privileges to perform this operation");
    }

    UserAccessException(String operation) {
        super("User has no privileges to perform " + operation);
    }
}
