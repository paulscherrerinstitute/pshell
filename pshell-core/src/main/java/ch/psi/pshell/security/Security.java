package ch.psi.pshell.security;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.devices.DevicePoolListener;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.sequencer.CommandSource;
import ch.psi.pshell.sequencer.InterpreterListener;
import ch.psi.pshell.swing.DevicePoolPanel;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Config.ConfigListener;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.ObservableBase;
import ch.psi.pshell.utils.Serializer;
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
 public class Security extends ObservableBase<SecurityListener> implements AutoCloseable {
    static Security INSTANCE;
    
    public static Security getInstance(){
        if (INSTANCE == null){
            throw new RuntimeException("Users Manager not instantiated.");
        }          
        return INSTANCE;
    }
    
    public static boolean hasInstance(){
        return INSTANCE!=null;
    }    
                     
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

    public Security(String path) {
        INSTANCE = this;
        this.configFolder = (path==null) ? Setup.getConfigPath() : path;
        users = new ArrayList<>();
    }

    void assertEnabled() throws IOException {
        if (!enabled) {
            throw new IOException("User management disabled");
        }
    }
    
    public void initialize(boolean enabled, String userAuthenticatorConfig) {
        this.enabled = enabled ;
        this.userAuthenticatorConfig = userAuthenticatorConfig ; 

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
            Logger.getLogger(Security.class.getName()).info("Initializing " + getClass().getSimpleName());
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
                Logger.getLogger(Security.class.getName()).info("Finished " + getClass().getSimpleName() + " initialization");
            } catch (Exception ex) {
                userAuthenticator = null;
            }
            addListeners();

        } else {
            removeListeners();
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
    
    
    protected void addListeners(){
        DevicePool.addStaticListener(devicePoolListener);        
        if (Context.hasDevicePool()) {
            for (GenericDevice dev : Context.getDevicePool().getAllDevices()) {
                if (dev instanceof ch.psi.pshell.device.Device device) {
                    device.addListener(deviceWriteAccessListener);
                }
                if (dev.getConfig() != null) {
                    dev.getConfig().addListener(deviceConfigChangeListener);
                }
            }
        }
        if (Context.hasConfig()){
            Context.getConfig().addListener(configChangeListener);
        }
        if (Context.hasInterpreter()){
            Context.getInterpreter().addListener(interpreterListener);
        }
    }
    
    protected void removeListeners(){
        DevicePool.removeStaticListener(devicePoolListener);        
        if (Context.hasDevicePool()) {
            for (GenericDevice dev : Context.getDevicePool().getAllDevices()) {
                if (dev instanceof ch.psi.pshell.device.Device device) {
                    device.removeListener(deviceWriteAccessListener);
                }
                if (dev.getConfig() != null) {
                    dev.getConfig().removeListener(deviceConfigChangeListener);
                }
            }
        }
        if (Context.hasConfig()){
            Context.getConfig().removeListener(configChangeListener);
        }
        if (Context.hasInterpreter()){
            Context.getInterpreter().removeListener(interpreterListener);
        }  
    }
    
    DevicePoolListener devicePoolListener = new DevicePoolListener() {
        @Override
        public void onDeviceAdded(GenericDevice dev) {
            if (Context.hasSecurity()) {
                if (dev instanceof ch.psi.pshell.device.Device) {
                    dev.addListener(deviceWriteAccessListener);
                }
                if (dev.getConfig() != null) {
                    dev.getConfig().addListener(deviceConfigChangeListener);
                }
            }
        }               
    };
    
    DeviceListener deviceWriteAccessListener = new DeviceListener() {
        @Override
        public void onValueChanging(Device device, Object value, Object former) throws Exception {
            getCurrentRights().assertDeviceWriteAllowed();
        }
    };

    ConfigListener deviceConfigChangeListener = new ConfigListener() {
        @Override
        public void onSaving(Config config) {
            getCurrentRights().assertDeviceConfigAllowed();
        }
    };
    
    
    ConfigListener configChangeListener = new ConfigListener() {
        @Override
        public void onSaving(Config config) {
            getCurrentRights().assertConfigAllowed();
        }
    };
    
    InterpreterListener interpreterListener = new InterpreterListener(){    
        public void willEval(CommandSource source, String code) throws SecurityException {
            if (!source.isInternal()) {
                getCurrentRights(source.isRemote()).assertConsoleAllowed();
            }
        }
        public void willRun(CommandSource source, String fileName, Object args) throws SecurityException{
            if (!source.isInternal()) {
                getCurrentRights(source.isRemote()).assertRunAllowed();
            }
        }
    };    
    

    public void setUsers(User[] users) throws IOException {
        assertEnabled();
        this.users.clear();
        this.users.addAll(Arrays.asList(users));
        Path path = getUsersFile();
        Files.write(path, Serializer.encode(this.users, Serializer.EncoderType.bin));
        IO.setFilePermissions(path.toFile(), Context.getScriptFilePermissions());
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
            Context.getInterpreter().setSourceUI(source);
            String pwd = Context.getInterpreter().getPassword("Enter password:", "Set User");            
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
        return Rights.DEFAULT_RIGHTS;
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
        DevicePoolPanel.setDeviceConfigPanelEnabled(!getCurrentRights().denyDeviceConfig);
        for (SecurityListener listener : getListeners()) {
            try {
                listener.onUserChange(user, former);
            } catch (Exception ex) {
                Logger.getLogger(Security.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    @Override
    public void close() {
        super.close();
    }

}
