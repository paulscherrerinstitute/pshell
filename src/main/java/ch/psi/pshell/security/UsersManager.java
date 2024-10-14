package ch.psi.pshell.security;

import ch.psi.pshell.core.CommandSource;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Setup;
import ch.psi.utils.Arr;
import ch.psi.utils.IO;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.Serializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
 public class UsersManager extends ObservableBase<UsersManagerListener> implements AutoCloseable {

    final String configFolder;
    final HashMap<AccessLevel, Rights> rights = new HashMap<>();

    boolean enabled;
    String userAuthenticatorConfig;
    UserAuthenticator userAuthenticator;
    ArrayList<User> users;
    User currentUser;
    User remoteUser;

    public User[] getUsers() {
        return users.toArray(new User[0]);
    }

    public String[] getUserNames() {
        ArrayList<String> ret = new ArrayList<>();
        for (User user : getUsers()) {
            ret.add(user.name);
        }
        return ret.toArray(new String[0]);
    }

    public Path getUsersFile() {
        return Paths.get(configFolder, "users");
    }

    public Path getRightsFile(AccessLevel level) {
        return Paths.get(configFolder, level.toString() + ".properties");
    }

    public UsersManager(Setup setup) {
        this.configFolder = setup.getConfigPath();
        users = new ArrayList<>();
    }

    void assertEnabled() throws IOException {
        if (!enabled) {
            throw new IOException("User management disabled");
        }
    }

    public void initialize() {
        enabled = Context.getInstance().getConfig().userManagement;
        userAuthenticatorConfig = Context.getInstance().getConfig().userAuthenticator;

        try {
            assertEnabled();
            users = (ArrayList<User>) Serializer.decode(Files.readAllBytes(getUsersFile()), Serializer.EncoderType.bin);
            if (users.size() == 0) {
                throw new Exception("Invalid user file");
            }
        } catch (Exception ex) {
            users = new ArrayList<>();
            users.add(User.getDefault());
        }

        //Create user authenticator
        rights.clear();
        if (enabled) {
            Logger.getLogger(UsersManager.class.getName()).info("Initializing " + getClass().getSimpleName());
            for (AccessLevel level : AccessLevel.values()) {
                Rights rights = new Rights();
                try {
                    rights.load(getRightsFile(level).toString());
                } catch (Exception ex) {
                }
                this.rights.put(level, rights);
            }

            try {
                String[] tokens = userAuthenticatorConfig.split("\\|");
                for (int i = 0; i < tokens.length; i++) {
                    tokens[i] = tokens[i].trim();
                }
                Class cls = Class.forName(tokens[0]);
                String[] pars = Arr.remove(tokens, 0);
                if (pars.length == 0) {
                    userAuthenticator = (UserAuthenticator) cls.newInstance();
                } else {
                    userAuthenticator = (UserAuthenticator) cls.getConstructor(new Class[]{String[].class}).newInstance(new Object[]{pars});
                }
                Logger.getLogger(UsersManager.class.getName()).info("Finished " + getClass().getSimpleName() + " initialization");
            } catch (Exception ex) {
                userAuthenticator = null;
            }
        }

        if (currentUser == null) {
            for (User user : users) {
                if (user.autoLogon) {
                    currentUser = user;
                    triggerUserChange(user, null);
                    break;
                }
            }
        }
    }

    public void setUsers(User[] users) throws IOException {
        assertEnabled();
        this.users.clear();
        this.users.addAll(Arrays.asList(users));
        Path path = getUsersFile();
        Files.write(path, Serializer.encode(this.users, Serializer.EncoderType.bin));
        IO.setFilePermissions(path.toFile(), Context.getInstance().getConfig().filePermissionsConfig);
    }

    public boolean selectUser(String name) throws IOException, InterruptedException {
        return selectUser(name, CommandSource.ui);
    }

    public boolean selectUser(String name, CommandSource source) throws IOException, InterruptedException {
        assertEnabled();
        for (User user : users) {
            if (user.name.equals(name)) {
                return selectUser(user, source);
            }
        }
        throw new IOException("Invalid user");
    }

    public boolean selectUser(User user, CommandSource source) throws IOException, InterruptedException {
        assertEnabled();
        if (user == currentUser) {
            return true;
        }
        if (user == null) {
            throw new IOException("Invalid user");
        }
        if (user.authentication) {
            if (userAuthenticator == null) {
                throw new IOException("No user authenticator defined");
            }
            Context.getInstance().setSourceUI(source);
            String pwd = Context.getInstance().getPassword("Enter password:", "Set User");
            if (pwd == null) {
                return false;
            }
            if (!userAuthenticator.authenticate(user.name, pwd)) {
                throw new IOException("Invalid user or password");
            }
        }
        User former = currentUser;
        currentUser = user;
        triggerUserChange(user, former);
        return true;

    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * If undefined return default user
     */
    public User getCurrentUser() {
        return getCurrentUser(false);
    }

    public AccessLevel getCurrentLevel() {
        return getCurrentLevel(false);
    }

    public Rights getCurrentRights() {
        return getCurrentRights(false);
    }

    public User getCurrentUser(boolean remote) {
        if (remote) {
            if (remoteUser == null) {
                return User.getRemote();
            }
            return remoteUser;
        } else {
            if (currentUser == null) {
                return User.getDefault();
            }
            return currentUser;
        }
    }

    public AccessLevel getCurrentLevel(boolean remote) {
        return getCurrentUser(remote).accessLevel;
    }

    public Rights getCurrentRights(boolean remote) {
        if (enabled) {
            return rights.get(getCurrentLevel(remote));
        }
        return new Rights();
    }

    public User getUser(String name) {
        for (User user : users) {
            if (user.name.equals(name)) {
                return user;
            }
        }
        return null;
    }

    void triggerUserChange(User user, User former) {
        for (UsersManagerListener listener : getListeners()) {
            try {
                listener.onUserChange(user, former);
            } catch (Exception ex) {
                Logger.getLogger(UsersManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    @Override
    public void close() {
        super.close();
    }

}
