package ch.psi.pshell.security;

import ch.psi.pshell.core.Setup;
import java.security.Permission;
import java.util.HashMap;

/**
 *
 */
public class SecurityManager extends java.lang.SecurityManager {

    static User user = User.getDefault();
    final HashMap<AccessLevel, Rights> rights;
    final Setup setup;
    final String configPath;
    final String scriptPath;

    SecurityManager(HashMap<AccessLevel, Rights> rights, Setup setup) {
        this.rights = rights;
        this.setup = setup;
        configPath = new java.io.File(setup.getConfigPath()).getPath();
        scriptPath = new java.io.File(setup.getScriptPath()).getPath();

    }

    //By default all permissions granted
    @Override
    public void checkPermission(Permission perm) {
    }

    //This should run as fast as possible, SecurityManager may be expensive.
    public void checkWrite(String file) {
        if (rights.size() > 0) {
            if ((rights.get(user.accessLevel).denyConfig && file.startsWith(configPath))
                    || (rights.get(user.accessLevel).denyEdit && file.startsWith(scriptPath))) {
                throw new UserAccessException();
            }
        }
    }
}
