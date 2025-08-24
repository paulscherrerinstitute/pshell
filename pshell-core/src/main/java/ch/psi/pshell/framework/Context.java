package ch.psi.pshell.framework;

import ch.psi.pshell.crlogic.CrlogicConfig;
import ch.psi.pshell.data.DataAddress;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Format;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.logging.LogManager;
import ch.psi.pshell.notification.NotificationManager;
import ch.psi.pshell.pkg.PackageManager;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plugin.PluginManager;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanConfig;
import ch.psi.pshell.scan.ScanResult;
import ch.psi.pshell.scripting.ScriptManager;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.security.Rights;
import ch.psi.pshell.security.User;
import ch.psi.pshell.security.UserAccessException;
import ch.psi.pshell.security.UsersManager;
import ch.psi.pshell.sequencer.CommandManager;
import ch.psi.pshell.sequencer.ExecutionParameters;
import ch.psi.pshell.sequencer.Interpreter;
import ch.psi.pshell.session.SessionManager;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.UserInterface;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Condition;
import ch.psi.pshell.utils.Configurable;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.SortedProperties;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.versioning.VersioningManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 */
public class Context {
     
    static Config config;    
    public static void setConfig(Config value) {
        config = value;
    }    
        
    public static Config getConfig() {
        return config;
    }        
    
    public static boolean hasConfig() {
        return config!=null;
    }        

    public static Object getConfigEntry(String name){        
        try{
            return config.getFieldValue(name);
        } catch (Exception ex){
            return null;
        }
    }                
    
    public static boolean isDebug() {
        return Setup.isDebug();
    }      
       
    public static boolean isSimulation() {
        return Setup.isSimulation();
    }  
        
            
    public static App getApp(){
        return App.getInstance();
    }
    
    public static MainFrame getView(){
        return MainFrame.getInstance();
    }
    
    public static boolean hasApp(){
        return getApp() !=null;
    }
    
    public static boolean hasView(){
        return getView()!=null;
    }      
    
    
    static State state = State.Invalid;
    public static void setState(State value){
        state = value;
        if (getApp()!=null){
            getApp().setState(state);
        }
    }
        
    public static State getState(){
        return state;
    }
    
    public static void waitState(State state, int timeout) throws IOException, InterruptedException {
        Chrono chrono = new Chrono();
        try {
            chrono.waitCondition(new Condition() {
                @Override
                public boolean evaluate() throws InterruptedException {
                    return getState() == state;
                }
            }, timeout);
        } catch (TimeoutException ex) {
        }
    }    
    
    public static void waitStateNot(State state, int timeout) throws IOException, InterruptedException {
        Chrono chrono = new Chrono();
        try {
            chrono.waitCondition(new Condition() {
                @Override
                public boolean evaluate() throws InterruptedException {
                    return getState() != state;
                }
            }, timeout);
        } catch (TimeoutException ex) {
        }
    }

    public static void waitStateNotProcessing(int timeout) throws IOException, InterruptedException {
        Chrono chrono = new Chrono();
        try {
            chrono.waitCondition(new Condition() {
                @Override
                public boolean evaluate() throws InterruptedException {
                    return !getState().isProcessing();
                }
            }, timeout);
        } catch (TimeoutException ex) {
        }
    }   
    
    public void assertState(State state) throws State.StateException {
        getState().assertIs(state);
    }    
        
    public static void restart() throws Exception {
        getInterpreter().restart();
    }
    
    public static Class getClassByName(String className) throws ClassNotFoundException {
        return PluginManager.getClass(className);
    }               
    
    public static ScriptType getScriptType() {                
        return Setup.getScriptType();
    }       
    public static Scan getCurrentScan(){
        return getExecutionPars().getScan();
    }
    
    public static ScanConfig getScanConfig(){
        if (hasConfig()){
            return getConfig().getScanConfig();
        }
        return new ScanConfig();
    }     
    
    public static ch.psi.pshell.plugin.Plugin[] getPlugins() {
        PluginManager pluginManager  = getPluginManager();
        if (pluginManager != null) {
            return pluginManager.getLoadedPlugins();
        }
        return new ch.psi.pshell.plugin.Plugin[0];
    }

    public static ch.psi.pshell.plugin.Plugin getPlugin(String name) {
        PluginManager pluginManager  = getPluginManager();
        for (ch.psi.pshell.plugin.Plugin p : getPlugins()) {
            if (IO.getPrefix(p.getPluginName()).equals(name)) {
                return p;
            }
        }
        return null;
    }

    public static  File[] getExtensions() {
        PluginManager pluginManager  = getPluginManager();
        if (pluginManager != null) {
            return pluginManager.getExtensions().toArray(new File[0]);
        }
        return new File[0];
    }
    
    public static boolean hasScriptManager(){
        return ScriptManager.getInstance()!=null;
    }
    
    public static ScriptManager getScriptManager(){
        ScriptManager sm = ScriptManager.getInstance();
        if ( sm== null){
            throw new RuntimeException("Script Manager not instantiated.");
        }
        return sm;
    }    
    
    //Plugins
    public static boolean hasPluginManager(){
        return PluginManager.hasInstance();
    }
        
    public static PluginManager getPluginManager(){
        return PluginManager.getInstance();
    }

    public static boolean hasVersioningManager(){ 
        return VersioningManager.hasInstance();
    }
    
    public static VersioningManager getVersioningManager(){
        return VersioningManager.getInstance();
    }
        
    public static boolean hasLogManager(){
        return LogManager.hasInstance();
    }
    
    public static LogManager getLogManager(){
        return LogManager.getInstance();
    }

    public static Level getLogLevel() {
        return hasLogManager() ? getLogManager().getLevel() : null;
    }
    
    //Logging
    public static void setLogLevel(Level level) {
        if (hasLogManager()) {
            getLogManager().setLevel(level);
        }
    }        
    
    static DataManager dataManager;    
    public static void setDataManager(DataManager value) {
        dataManager = value;
        dataManager.setGlobal();
    }        
    
    public static DataManager getDataManager() {
        if (dataManager == null){
            throw new RuntimeException("Data Manager not set.");
        }        
        return dataManager;
    }     
    
    public static boolean hasDataManager() {
        return dataManager!=null;
    }         
        
    public static boolean hasDevicePool(){
        return DevicePool.hasInstance();
    }
    
    public static DevicePool getDevicePool(){
        return DevicePool.getInstance();
    }
        

    static void triggerConfigurationChange(final Configurable obj) {   
         String message = "Configuration change: " + obj.toString();
         Logger.getLogger(Context.class.getName()).info(message);
         if (hasVersioningManager() && (obj.getConfig()!=null)){
            try {
                String fileName = obj.getConfig().getFileName();
                if (fileName != null) {
                    getVersioningManager().autoCommit(fileName, message);
                }
            } catch (Throwable ex) {
                Logger.getLogger(Context.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    
   public static void saveConfiguration(Configurable obj) throws IOException {        
        try {
            getRights().assertConfigAllowed();
            obj.getConfig().save();
            triggerConfigurationChange(obj);            
        } catch (UserAccessException ex) { //If no rights
            obj.getConfig().restore();   //If no rights restore previous saved state.
            throw ex;
        }
    }    

    public static CrlogicConfig getCrlogicConfig(){
        return CrlogicConfig.getConfig();
    }                
    
    public static boolean hasInterpreter(){
        return Interpreter.hasInstance();
    }
    
    //!!! Interpreter must be singletoin,as no rtr-instatiation is considered when  as listeners are added. Interpreter.restart is used for softy restart.
    public static Interpreter getInterpreter(){
        return Interpreter.getInstance();
    }

    public static CommandManager getCommandManager() {
        return getInterpreter().getCommandManager();
    }

    
    public static boolean hasSessionManager(){
        return SessionManager.hasInstance();
    }
    
    public static SessionManager getSessionManager(){
        return SessionManager.getInstance();
    }

    public static boolean hasNotificationManager(){
        return NotificationManager.hasInstance();
    }

    public static boolean isNotificationEnabled(){
        return hasNotificationManager() && (getNotificationManager().isEnabled());
    }
    
    public static NotificationManager getNotificationManager(){
        return NotificationManager.getInstance();
    }
    
    public static boolean hasPackageManager(){
        return PackageManager.hasInstance();
    }
    
    public static PackageManager getPackageManager(){
        return PackageManager.getInstance();
    }    
    
    public static boolean hasUsersManager(){
        return UsersManager.hasInstance();
    }
    
    public static UsersManager getUsersManager(){
        return UsersManager.getInstance();
    }
    
    public static Rights getRights(){
        if (hasUsersManager()){
            return getUsersManager().getCurrentRights();
        }
        return new Rights();
    }
    
    public static boolean isUsersEnabled(){
        return hasUsersManager() && (getUsersManager().isEnabled());
    }        
    
    public static String getUserName() {
        if (hasUsersManager()){
            return getUsersManager().getCurrentUser().name;
        }
        return User.DEFAULT_USER_NAME;
    }
            
    public static User getUser() {
        if (hasUsersManager()){
            return getUsersManager().getCurrentUser();
        }
        return User.getDefault();
        
    }

    public static int getSessionId() {
        if (isHandlingSessions()) {
            return getSessionManager().getCurrentSession();
        }
        return SessionManager.UNDEFINED_SESSION_ID;
    }

    public static String getSessionName() {
        if (isHandlingSessions()) {
            return getSessionManager().getCurrentName();
        }
        return SessionManager.UNDEFINED_SESSION_NAME;
    }    
    
    public static void addDetachedFileToSession(File file) throws IOException {
        if (isHandlingSessions()) {
            getSessionManager().onCreateDetachedFile(file);
        }
    }    

    public static boolean isHandlingSessions(){
        if (hasSessionManager()){
            return SessionManager.isHandlingSessions();
        }
        return false;        
    }
                             

    public static Layout getLayout() {
        return hasDataManager() ? getDataManager().getLayout() : null;
    }    

    public static Format getFormat() {
        return hasDataManager() ? getDataManager().getFormat() : null;
    }    
     
    public static final String DEFAULT_DATA_FILE_PATTERN = Setup.TOKEN_DATA + "/" + Setup.TOKEN_YEAR + "_" + Setup.TOKEN_MONTH + "/" + 
                    Setup.TOKEN_DATE + "/" + Setup.TOKEN_DATE + "_" + Setup.TOKEN_TIME + "_" + Setup.TOKEN_EXEC_NAME;
    
    static String dataFilePattern = DEFAULT_DATA_FILE_PATTERN;
    public static void setDataFilePattern(String pattern) {
        dataFilePattern = pattern;
    }
    
    public static String getDataFilePattern() {
        return dataFilePattern;
    }
    
    public static String getDataFileName() {
        return Setup.expandPath(dataFilePattern);
    }
    
    static FilePermissions dataFilePermissions = FilePermissions.Default;
    public static void setDataFilePermissions(FilePermissions filePermissions) {
        dataFilePermissions = (filePermissions == null) ? IO.FilePermissions.Default : filePermissions;
    }
    
    public static FilePermissions getDataFilePermissions() {
        return dataFilePermissions;
    }

    static FilePermissions configFilePermissions = FilePermissions.Default;
    public static void setConfigFilePermissions(FilePermissions filePermissions) {
        configFilePermissions = (configFilePermissions == null) ? IO.FilePermissions.Default : configFilePermissions;
    }
    
    public static FilePermissions getConfigFilePermissions() {
        return configFilePermissions;
    }
    
    static FilePermissions scriptFilePermissions = FilePermissions.Default;
    public static void setScriptFilePermissions(FilePermissions filePermissions) {
        scriptFilePermissions = (configFilePermissions == null) ? IO.FilePermissions.Default : configFilePermissions;
    }
    
    public static FilePermissions getScriptFilePermissions() {
        return scriptFilePermissions;
    }
    
    static FilePermissions logFilePermissions = FilePermissions.Default;
    public static void setLogFilePermissions(FilePermissions filePermissions) {
        logFilePermissions = (configFilePermissions == null) ? IO.FilePermissions.Default : configFilePermissions;
    }
    
    public static FilePermissions getLogFilePermissions() {
        return logFilePermissions;
    }            
    
    static boolean serverCommandsHidden;   
    
    public static boolean isServerCommandsHidden() {        
        return serverCommandsHidden;        
    }

    public static void setServerCommandsHidden(boolean value) {        
        serverCommandsHidden = value;        
    }      
    
    //UI   
    public static UserInterface getUI() {
        return getInterpreter().getUI();
    }

    //Sequential numbers
    final static String LAST_RUN_DATE = "LastRunDate";
    final static String FILE_SEQUENTIAL_NUMBER = "FileSequentialNumber";
    final static String DAY_SEQUENTIAL_NUMBER = "DaySequentialNumber";
    
    public static int getFileSequentialNumber() {
        try {
            return Integer.valueOf(Context.getVariable(FILE_SEQUENTIAL_NUMBER));
        } catch (Exception ex) {
            return 0;
        }
    }
    
    public static int getDaySequentialNumber() {
        try {
            if (!Chrono.getTimeStr(System.currentTimeMillis(), "YYMMdd").equals(Context.getVariable(LAST_RUN_DATE))) {
                Context.setVariable(DAY_SEQUENTIAL_NUMBER, 0);
            }
            return Integer.valueOf(Context.getVariable(DAY_SEQUENTIAL_NUMBER));
        } catch (Exception ex) {
            return 0;
        }
    }
    
    public static void setFileSequentialNumber(int seq) throws IOException {
        Context.setVariable(FILE_SEQUENTIAL_NUMBER, seq);
    }

    
    public static void incrementSequentialNumbers() {
        try {
            incrementFileSequentialNumber();
        } catch (Exception ex) {
            Logger.getLogger(Context.class.getName()).log(Level.WARNING, null, ex);
        }
        try {
            incrementDaySequentialNumber();
        } catch (Exception ex) {
            Logger.getLogger(Context.class.getName()).log(Level.WARNING, null, ex);
        }
    }
    
    static void setDaySequentialNumber(int seq) throws IOException {
        int current = getDaySequentialNumber();
        if (seq <= current) {
            throw new IOException("Invalid day run index: " + seq + " - Current: " + current);
        }
        Context.setVariable(DAY_SEQUENTIAL_NUMBER, seq);
        Context.setVariable(LAST_RUN_DATE, Chrono.getTimeStr(System.currentTimeMillis(), "YYMMdd"));
    }

    static void incrementFileSequentialNumber() throws IOException {
        int current = getFileSequentialNumber();
        setFileSequentialNumber(current + 1);
    }

    static void incrementDaySequentialNumber() throws IOException {
        setDaySequentialNumber(getDaySequentialNumber() + 1);
    }    
    
    //Execution Context   
    
    public static ExecutionParameters getExecutionPars() {
        if (!hasInterpreter()){
            return null;
        }
        return getInterpreter().getExecutionPars();
    }    
    
            
    public static Map getSessionMetadata(){
        //Map<String, Object> metadata = getSessionManager().getMetadata(true);
        return null;
    }
        
           
    public static String getScanTag() {
        //!!! TODO
        //return (scanTag == null) ? "scan {index}%d" : scanTag;
        return "scan {index}%d";
    }    
       
    public static String getApplicationName() {    
        return App.getApplicationTitle();
    }
    
    public static String getApplicationVersion() {            
        return App.getApplicationVersion();
    }

    public static String getInstanceName() {    
        Object ret = getConfigEntry("instanceName");
        if (ret instanceof String str){
            return str.isBlank() ? null : str;
        }
        return null;
    }
    
    
    public enum DataTransferMode {
        Off,
        Copy,
        Move
    }            
    public static record DataTransferConfig(String path, String user, DataTransferMode mode) {};        
    
    public static DataTransferConfig getDataTransferConfig(){
        return new DataTransferConfig(null, null, DataTransferMode.Off); //!!!
    }
    
    public static boolean isDataTransferEnabled(){
        return getDataTransferConfig().mode != DataTransferMode.Off;
    }
    
           
    //Mailing //!!! Should move somewhere wlse?
    public static void notify(String subject, String text, List<String> attachments) throws IOException {
        notify(subject, text, attachments, null);
    }

    public static void notify(String subject, String text, List<String> attachments, List<String> to) throws IOException {
        if (isNotificationEnabled()){            
            File[] att = null;
            if (attachments != null) {
                ArrayList<File> files = new ArrayList();
                for (String attachment : attachments) {
                    files.add(new File(Setup.expandPath(attachment)));
                }
                att = files.toArray(new File[0]);
            }

            if (to == null) {
                getNotificationManager().send(subject, text, att);
            } else {
                getNotificationManager().send(subject, text, att, to.toArray(new String[0]));
            }
        }
    }
    
    //UI
    static UserInterface userInterface;

    public static void setUserInterface (UserInterface ui) {
        userInterface = ui;
    }

    public static UserInterface getUserInterface() {
        if (userInterface == null) {
            throw new RuntimeException("User interface not defined");
        }
        return userInterface;
    }
        
    public static boolean hasUserInterface() {
        return  userInterface != null;
    }
    
    
    //UI 
    public static MonitoredPanel showPanel(GenericDevice dev) throws InterruptedException {
        UserInterface ui = getUserInterface();
        return ui.showPanel(dev);
    }

    public static MonitoredPanel showPanel(ScanResult result) throws InterruptedException {
        UserInterface ui = getUserInterface();
        DataAddress scanPath = DataManager.getAddress(result.getScan().getPath());  
        String scanMessage = result.getScan().toString();
        MonitoredPanel ret=  ui.showPanel(scanPath, scanMessage);
        if (ret!=null){
            return ret;
        }
        String table = result.print("\t");
        return ui.showPanel(table, scanMessage); 

    }   
    
    public static boolean isOffscreenPlotting(){
        return Plot.isOffscreen();
    }       
    
    //Commnands
    public static void abort() throws InterruptedException {      
        if (hasInterpreter()){
             getInterpreter().abort();
        }
    }
    
    public static boolean isAborted(){
        return hasInterpreter() ? getInterpreter().isAborted() : false;
    }        
    
    public static void triggerStartExecution(final String fileName) {        
         if (hasVersioningManager() && (fileName!=null)){
            try {
                getVersioningManager().autoCommit(fileName, "Executing: " + fileName);
            } catch (Throwable ex) {
                Logger.getLogger(Context.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }    
        
    
    public static int getRunCount(){
        if (hasInterpreter()){
            return getInterpreter().getRunCount();
        }
        return 0;
    }       
    
    //Porperties
    //Settings (persisted user properties) and variables (persisted system properties)
    public static void setProperty(String filename, String name, Object value) throws IOException {
        Properties properties = new SortedProperties();
        try (FileInputStream in = new FileInputStream(filename)) {
            properties.load(in);
        } catch (FileNotFoundException ex) {
        }
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, String.valueOf(value));
        }
        try (FileOutputStream out = new FileOutputStream(filename)) {
            properties.store(out, null);
            IO.setFilePermissions(filename, getConfigFilePermissions());
        }
    }

    public static String getProperty(String filename, String name) throws IOException {
        return getProperties(filename).get(name);
    }

    public static Map<String, String> getProperties(String filename) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(filename)) {
            properties.load(in);
            //return Maps.fromProperties(properties);
            Map<String, String> ret = new HashMap<>();
            for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                ret.put(key, properties.getProperty(key));
            }
            return ret;
        } catch (FileNotFoundException ex) {
            return new HashMap<>();
        }
    }

    public static  void setSetting(String name, Object value) throws IOException {
        setProperty(getSettingsFile(), name, value);
    }

    public static  String getSetting(String name) throws IOException {
        return getProperty(getSettingsFile(), name);
    }

    public static  Map<String, String> getSettings() throws IOException {
        return getProperties(getSettingsFile());
    }
    
    public static  String getSettingsFile() {
        return Setup.getSettingsFile();            
    }
    
    public static String getVariablesFile() {
        return Setup.getVariablesFile();            
    }    
    
    public static void setVariable(String name, Object value) throws IOException {
        setProperty(getVariablesFile(), name, value);
    }

    public static String getVariable(String name) throws IOException {
        return getProperty(getVariablesFile(), name);
    }

    public static Map<String, String> getVariables() throws IOException {
        return getProperties(getVariablesFile());
    }       
    
    //A global map for plugins to communicate to each other
    final static Map<String, Object> globals = new HashMap<>();

    //A global map for plugins to communicate to each other
    public static void setGlobal(String name, Object value) {
        synchronized (globals) {
            globals.put(name, value);
        }
    }

    public static Object getGlobal(String name) {
        synchronized (globals) {
            return globals.get(name);
        }
    }

    public static boolean hasGlobal(String name) {
        synchronized (globals) {
            return globals.containsKey(name);
        }
    }

    public static void removeGlobal(String name) {
        synchronized (globals) {
            globals.remove(name);
        }
    }

    public Map<String, Object> getGlobals() {
        synchronized (globals) {
            return (Map<String, Object>) ((HashMap) globals).clone();
        }
    }

    public static void clearGlobals() {
        synchronized (globals) {
            globals.clear();
        }
    }        
    
    
}
