package ch.psi.pshell.console;

import ch.psi.pshell.framework.Config;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Context.DataTransferMode;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.logging.LogLevel;
import ch.psi.pshell.notification.Notifier.NotificationLevel;
import ch.psi.pshell.scan.ScanConfig;
import ch.psi.pshell.sequencer.Sequencer;
import ch.psi.pshell.session.Sessions.SessionHandling;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.Str;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * An entity class holding the system configuration.
 */
public class Configuration extends Config {
    
    public boolean consoleJournal;
    public static String DEFAULT_LOG_PATH = Setup.TOKEN_LOGS + "/" + Setup.TOKEN_DATE + "_" + Setup.TOKEN_TIME + "." + 
                    Setup.TOKEN_MODE;
    public static String DEFAULT_DATA_PATH = Setup.TOKEN_DATA + "/" + Setup.TOKEN_YEAR + "_" + Setup.TOKEN_MONTH + "/" + 
                    Setup.TOKEN_DATE + "/" + Setup.TOKEN_DATE + "_" + Setup.TOKEN_TIME + "_" + Setup.TOKEN_EXEC_NAME;
    

    public String dataPath = DEFAULT_DATA_PATH;
    @Defaults(values = {"h5", "txt","csv", "fda", "tiff"})
    public String dataFormat = "h5";
    @Defaults(values = {"default", "table", "sf", "fda", "nx"})
    public String dataLayout = "default";
    public boolean dataEmbeddedAttributes = false;
    public int dataDepthDimension = 0;    
    public DataTransferMode dataTransferMode = DataTransferMode.Off;
    public String dataTransferPath = "";
    public String dataTransferUser = "";
    public int dataServerPort = -1;
    public FilePermissions filePermissionsData = FilePermissions.Default;
    public FilePermissions filePermissionsLogs = FilePermissions.Default;
    public FilePermissions filePermissionsScripts = FilePermissions.Default;
    public FilePermissions filePermissionsConfig = FilePermissions.Default;
    public SessionHandling sessionHandling = SessionHandling.Off;    
    public String logPath = DEFAULT_LOG_PATH ;
    public int logDaysToLive = -1;
    public LogLevel logLevel = LogLevel.Info;
    public NotificationLevel notificationLevel = NotificationLevel.Off;
    public String notificationTasks = "";
    public String pythonHome= "";
    public boolean scanAutoSave = true;
    public boolean scanFlushRecords = false;
    public boolean scanReleaseRecords = false;
    public boolean scanPreserveTypes = false;
    public boolean scanSaveOutput = false;
    public boolean scanSaveScript = false;
    public boolean scanSaveSetpoints = false;
    public boolean scanSaveTimestamps = false;
    public boolean scanLazyTableCreation = false;
    public int scanStreamerPort = -1;
    public boolean scanSaveLogs = true;
    public String scanDefaultTag = Context.DEFAULT_SCAN_TAG;    
    public boolean versionTrackingEnabled;
    public boolean versionTrackingManual;
    public String versionTrackingRemote = "";
    public String versionTrackingLogin = "";        
    public int commandQueueSize  = Sequencer.DEFAULT_COMMAND_BUS_SIZE;      
    public int commandTimeToLive  = Sequencer.DEFAULT_COMMAND_BUS_TIME_TO_LIVE; 
    public boolean commandStatistics;
    public boolean serverEnabled;    
    public boolean serverCommandsHidden = false;
    public String serverHostName  = "";        
    public int serverPort = 8080;
    public boolean terminalEnabled;
    public int terminalPort = 3579;
    public boolean userManagement;
    public String userAuthenticator = "";
    public String instanceName = "";    

    //Set fields mising from config file (backward compatibility)
    @Override
    public void updateFields() {
        super.updateFields();
        if (filePermissionsData==null){
            filePermissionsData = FilePermissions.Default;
        }
        if (filePermissionsLogs==null){
            filePermissionsLogs = FilePermissions.Default;
        }
        if (filePermissionsScripts==null){
            filePermissionsScripts = FilePermissions.Default;
        }
        if (filePermissionsConfig==null){
            filePermissionsConfig = FilePermissions.Default;
        }
        if (notificationLevel==null){
            notificationLevel = NotificationLevel.Off;
        }
        if (dataTransferMode==null){
            dataTransferMode = DataTransferMode.Off;
        }
        if (dataTransferPath==null){
            dataTransferPath = "";
        }   
        if (dataTransferUser==null){
            dataTransferUser = "";
        }    
        if (instanceName==null){
            instanceName = "";
        }          
        if (sessionHandling==null){
            sessionHandling = SessionHandling.Off;
        }         
        if (notificationTasks==null){
            notificationTasks = "";
        }             
        if (pythonHome==null){
            pythonHome= "";
        }
        updateScanConfig();
    }
    
    public void save() throws IOException {
        super.save();
        updateScanConfig();
    }

    public Level getLogLevel() {
        return Level.parse(logLevel.toString().toUpperCase());
    }
    
    public String getDataFormat() {
        String format = Setup.getDataFormat();
        return format == null ? dataFormat : format;
    }

    public String getDataLayout() {
        String layout = Setup.getDataLayout();
        return layout == null ? dataLayout : layout;
    }
    
    public List<String> getNotifiedTasks() {
        List<String> ret = new ArrayList<>();
        String[] tokens = Str.split(notificationTasks, new String[]{"|", ";", ","});
        for (String str : tokens) {
            str = str.trim();
            if (!str.isEmpty()) {
                ret.add(str);
            }
        }
        return ret;
    }

    public int getDepthDim() {
        return (((dataDepthDimension < 0) || (dataDepthDimension > 2)) ? 0 : dataDepthDimension);
    }
    
    public String getPythonHome(){        
        String prop = Setup.getPythonHome();
        if (prop != null){
            return prop;
        } 
        if ((pythonHome==null) || (pythonHome.isBlank())){
            return null;
        }
        return Setup.expandPath(pythonHome.trim());        
    }    
    
    private ScanConfig scanConfig;
    private void updateScanConfig(){
        scanConfig = new ScanConfig(
            scanAutoSave,
            scanFlushRecords,
            scanReleaseRecords,
            scanPreserveTypes,
            scanSaveOutput,
            scanSaveScript,
            scanSaveSetpoints,
            scanSaveTimestamps,
            scanSaveLogs,
            scanLazyTableCreation);        
    }
    
    
    @Override
    public ScanConfig getScanConfig(){
        if (scanConfig==null){
            updateScanConfig();
        }
        return scanConfig;
    }
    

    public boolean isServerEnabled() {
        return (Setup.isServerMode() || serverEnabled) && (serverPort > 0) && !Setup.isLocal();
    }

    public boolean isTerminalEnabled() {
        return (Setup.isServerMode() || terminalEnabled) && (terminalPort > 0) && !Setup.isLocal();
    }

    public boolean isScanStreamerEnabled() {
        return /*(serverMode || config.serverEnabled) && */ (scanStreamerPort > 0) && !Setup.isLocal();
    }

    public boolean isDataStreamerEnabled() {
        return /*(serverMode || config.serverEnabled) && */ (dataServerPort > 0) && !Setup.isLocal();
    }       
    
    public boolean isVersioningEnabled() {
        if (Boolean.FALSE.equals(Setup.getForceVersioning())) {
            return false;
        }
        return versionTrackingEnabled && (!Setup.isLocal() ||  Boolean.TRUE.equals(Setup.getForceVersioning()));
    }

    public boolean isVersioningManual() {
        if (Setup.isLocal()) {
            return true;
        }
        return versionTrackingManual;
    }
    
    public boolean isVersioningTrackingRemote() {
        return isVersioningEnabled() && versionTrackingRemote!=null && !versionTrackingRemote.isBlank();
    }        
    
    public String getLogPath(){
        if (logPath==null){
            return DEFAULT_LOG_PATH;
        }
        return logPath;
    }
    
    public String getDataPath(){
        if (dataPath==null){
            return DEFAULT_DATA_PATH;
        }
        return dataPath;        
    }
    
    public String getDefaultScanTag(){
        if (scanDefaultTag==null){
            return Context.DEFAULT_SCAN_TAG;
        }
        return scanDefaultTag;
    }
}
