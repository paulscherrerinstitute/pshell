package ch.psi.pshell.console;

import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.DataServer;
import ch.psi.pshell.device.Interlock;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.notification.NotificationManager;
import ch.psi.pshell.pkg.PackageManager;
import ch.psi.pshell.plugin.PluginManager;
import ch.psi.pshell.scan.ScanStreamer;
import ch.psi.pshell.scripting.ScriptManager;
import ch.psi.pshell.security.UsersManager;
import ch.psi.pshell.sequencer.CommandSource;
import ch.psi.pshell.sequencer.Interpreter;
import ch.psi.pshell.sequencer.Interpreter.InterpreterStateException;
import ch.psi.pshell.session.SessionManager;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Configurable;
import ch.psi.pshell.utils.History;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.versioning.VersioningConfig;
import ch.psi.pshell.versioning.VersioningManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The application singleton object.
 */
public class App extends ch.psi.pshell.framework.App implements Configurable{    

    private static final Logger logger = Logger.getLogger(App.class.getName());
    
    Setup setup;
    final Configuration config;    
    final PluginManager pluginManager;
    final UsersManager usersManager;
    final DataManager dataManager;
    final SessionManager sessionManager;
    VersioningManager versioningManager;
    Interpreter interpreter;
    NotificationManager notificationManager;
    PackageManager packageManager;
    DevicePool devicePool;    
    ScanStreamer scanStreamer;
    DataServer dataStreamer;
        
    
    static public App getInstance() {
        return (App) instance;
    }

    private static App createInstance() {
        if (instance==null){
            instance = new App();
        }
        return (App) instance;
    }
       
    
    public Configuration getConfig() {
        return config;
    }
        
    protected App() {
        Setup.mkdirs();
        config = new Configuration();
        try {
            config.load(setup.getConfigFile());
        } catch (IOException ex) {
            if (!Setup.isLocal()) {
                throw new RuntimeException("Cannot generate configuration file");
            } else {
                logger.warning("Cannot generate configuration file");
            }
        }
        Context.setConfig(config);               
                              
        Context.setConfigFilePermissions(config.filePermissionsConfig);
        Context.setDataFilePermissions(config.filePermissionsData);
        Context.setLogFilePermissions(config.filePermissionsLogs);
        Context.setScriptFilePermissions(config.filePermissionsScripts);
        //!!!
        Config.setDefaultPermissions(config.filePermissionsConfig);
        History.setDefaultPermissions(config.filePermissionsConfig);
              
        String contextName = (config.instanceName.length() > 0) ? config.instanceName : "unknown";
        if (config.instanceName.length() > 0) {
            System.out.println("\n[" + contextName + "]\n");
        }

        if ((config.hostName != null) && (!config.hostName.trim().isEmpty()) && (!config.hostName.equalsIgnoreCase("null"))) 

        config.backup();

        if (System.getProperty(ScriptManager.PROPERTY_PYTHON_HOME) == null) {
            System.setProperty(ScriptManager.PROPERTY_PYTHON_HOME, setup.getScriptsPath());
        }
        
        restartLogger();

        logger.log(Level.INFO, "Process: {0}", Sys.getProcessName());
        logger.log(Level.INFO, "Context: {0}", contextName);
        logger.log(Level.INFO, "Pars: {0}", Sys.getCommand());
        logger.log(Level.INFO, "User: {0}", Sys.getUserName());
        logger.log(Level.INFO, "Host: {0}", Sys.getLocalHost());
                
        dataManager = new DataManager();        
        pluginManager = new PluginManager();
        interpreter = new Interpreter(config.hostName);
        usersManager = new UsersManager(null);
        devicePool = new DevicePool();
        sessionManager = new SessionManager();
        var packages =  getPackageArgs();
        if ((packages != null) && (packages.size()>0)) {            
            packageManager = new PackageManager(packages.toArray(new File[0]));
        }        
        logger.info("Created application");
    }        
    
    
    void initializeData() throws Exception{
        Context.setDataFilePattern(config.dataPath); 
        dataManager.setDefaultDepthDimension(config.depthDimension);
        dataManager.initialize(config.getDataFormat(), config.getDataLayout());                                
    }
        
    
    @Override
    protected void onStart() {                
        super.onStart();        
        try {
            initializeData();
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, null, ex);
        }        
        launchApp();
    }
    
    protected void launchApp() {
        launchApp(null);                        
    }
    
    @Override
    protected void onRestart() throws InterpreterStateException {
        //A new file for each session
        boolean firstRun = (Context.getState() == State.Invalid);
        Config.setDefaultPermissions(config.filePermissionsConfig);
        String configuredPythonHome = config.getPythonHome();
        if (configuredPythonHome != null) {
            try {
                logger.log(Level.WARNING, "Attempt to override variable PYTHONHOME: {0}", configuredPythonHome);
                Sys.setEnvironmentVariable("PYTHONHOME", configuredPythonHome);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Cannot set PYTHONHOME: {0}", ex.getMessage());
            }
        }
        interpreter.setTerminalPort(config.isTerminalEnabled() ? config.terminalPort : 0);
        interpreter.setServerPort(config.isServerEnabled() ? config.serverPort : 0);    
        interpreter.setServerLight(!Setup.isServerMode());
        sessionManager.setMode(config.sessionHandling);

        for (AutoCloseable ac : new AutoCloseable[]{scanStreamer, dataStreamer, packageManager, notificationManager,
            devicePool, versioningManager}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        Epics.destroy();
        Nameable.clear();
        Interlock.clear();

        //A new file for each session
        if (!firstRun) {
            restartLogger();
            System.gc();
        }

        logger.info("Initializing context");
        try {
            if (!Setup.isLocal()) {
                if (config.isScanStreamerEnabled()) {
                    scanStreamer = new ScanStreamer(config.scanStreamerPort);
                }
                if (config.isDataStreamerEnabled()) {
                    dataStreamer = new DataServer(config.dataServerPort);
                }
            }
            usersManager.initialize(config.userManagement, config.userAuthenticator);
            String userProperty = System.getProperty(ch.psi.pshell.framework.Options.USER.toProperty());
            if (( userProperty != null) && (interpreter.getRunCount() == 0)) {
                try {
                    if (!usersManager.selectUser(userProperty, CommandSource.ctr)) {
                        exit("No password provided");
                    }
                } catch (Exception ex) {
                    exit(ex);
                }
            }

            dataManager.setFilePermissions(config.filePermissionsData);

            if ((!firstRun) || (!dataManager.isInitialized())) {
                initializeData();
            }
            notificationManager = new NotificationManager();
            
            notificationManager.initialize();
            interpreter.setNotificationLevel(config.notificationLevel);
            interpreter.setNotifiedTasks(config.getNotifiedTasks());
            
            Epics.create(Setup.getDefaultEpicsConfigFile(), Setup.isParallelInit());
                        
            devicePool = new DevicePool();
            logger.log(Level.INFO, "Loading Device Pool");
            try {
                devicePool.initialize(Setup.getDevicePoolFile());
            } catch (FileNotFoundException | NoSuchFileException ex) {
                logger.log(Level.FINER, null, ex);
            }
            
            //interpreter.restart(source); //!!! TODO: Solve source crap
            interpreter.restart(); 
            interpreter.getCommandManager().setSaveCommandStatistics(config.saveCommandStatistics);
            if (config.isVersioningEnabled()) {        
                VersioningConfig versioningConfig = new VersioningConfig( 
                    Setup.getHomePath(), 
                    config.versionTrackingRemote, 
                    config.versionTrackingLogin, 
                    !config.versionTrackingManual,
                    new String[] {Setup.getDevicesPath(), Setup.getScriptsPath(), Setup.getConfigPath(), Setup.getPluginsPath()});
                versioningManager = new VersioningManager(versioningConfig);
                versioningManager.setUserInterface(Context.getUserInterface());
            }
            if (pluginManager != null) {
                pluginManager.onInitialize(interpreter.getRunCount());
                pluginManager.setReloadable(Setup.isReloadable());
            }
            //Only instantiate it if state goes ready
            if (packageManager != null) {
                packageManager.initialize();
            }
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
               
    public void restartLogger() {        
        setupLogger(getConfig().logPath, config.getLogLevel(), config.logDaysToLive, config.filePermissionsLogs);
    }
    
    @Override
    protected void disable(){
        super.disable();
        for (AutoCloseable ac : new AutoCloseable[]{ packageManager, devicePool, versioningManager}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }        
        Epics.destroy();
    }    
   
    @Override
    protected void onExit() {
        super.onExit();
        for (AutoCloseable ac : new AutoCloseable[]{interpreter, scanStreamer, packageManager,devicePool, versioningManager, pluginManager, dataManager, usersManager}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        Epics.destroy();
        if (versioningManager != null) {
            if (!getConfig().versionTrackingManual) {
                versioningManager.startPushUpstream(true, false);  //In different process
            }
        }
    }
        
    //For logging
    @Override
    public String toString() {
        return "Application";
    }
    
    static public void create() {
       try {            
            if (!Setup.isCli()){
                ch.psi.pshell.framework.Options.SERVER.set();
            }
           
            createInstance().start();            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } 
    }
    
    static public void main(String[] args) {
        Options.add();
        init(args);            
        create();            
    }
}
