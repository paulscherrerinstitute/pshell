package ch.psi.pshell.security;

import java.io.Serializable;

/**
 *
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_USER_NAME = "Default";
    public static final String REMOTE_USER_NAME = "Remote";

    public String name;
    public boolean authentication;
    public boolean autoLogon;
    public AccessLevel accessLevel;

    public static User getDefault() {
        User ret = new User();
        ret.authentication = false;
        ret.autoLogon = true;
        ret.accessLevel = AccessLevel.administrator;
        ret.name = DEFAULT_USER_NAME;
        return ret;
    }

    public static User getRemote() {
        User ret = new User();
        ret.authentication = false;
        ret.autoLogon = false;
        ret.accessLevel = AccessLevel.remote;
        ret.name = REMOTE_USER_NAME;
        return ret;
    }

    public boolean isDefault() {
        return this.equals(getDefault());
    }

    public boolean isRemote() {
        return this.equals(getRemote());
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof User other)) {
            return (other.name.equals(name) && (other.authentication == authentication)
                    && (other.autoLogon == autoLogon) && (other.accessLevel == accessLevel));
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(name).append(" [").append(accessLevel).append("] ");
        if (authentication) {
            ret.append("!");
        }
        if (autoLogon) {
            ret.append("*");
        }
        return ret.toString();
    }
}
