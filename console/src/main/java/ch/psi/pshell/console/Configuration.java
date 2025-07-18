package ch.psi.pshell.console;

import ch.psi.pshell.framework.Config;
import ch.psi.pshell.framework.Context.DataTransferMode;
import ch.psi.pshell.framework.Options;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.logging.LogLevel;
import ch.psi.pshell.notification.NotificationManager.NotificationLevel;
import ch.psi.pshell.scan.ScanConfig;
import ch.psi.pshell.session.SessionManager.SessionHandling;
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
    
    public boolean saveConsoleSessionFiles;

    public String dataPath = Setup.TOKEN_DATA + "/" + Setup.TOKEN_YEAR + "_" + Setup.TOKEN_MONTH + "/" + 
                    Setup.TOKEN_DATE + "/" + Setup.TOKEN_DATE + "_" + Setup.TOKEN_TIME + "_" + Setup.TOKEN_EXEC_NAME;
    @Defaults(values = {"h5", "txt", "txtd","csv", "fda", "tiff"})
    public String dataFormat = "h5";
    @Defaults(values = {"default", "table", "sf", "fda", "nx"})
    public String dataLayout = "default";
    public int dataDepthDimension = 0;    
    public boolean dataScanAutoSave = true;
    public boolean dataScanFlushRecords = false;
    public boolean dataScanReleaseRecords = false;
    public boolean dataScanPreserveTypes = false;
    public boolean dataScanSaveOutput = false;
    public boolean dataScanSaveScript = false;
    public boolean dataScanSaveSetpoints = false;
    public boolean dataScanSaveTimestamps = false;
    public boolean dataScanLazyTableCreation = false;
    public int dataScanStreamerPort = -1;
    public boolean dataScanSaveLogs = true;
    public DataTransferMode dataTransferMode = DataTransferMode.Off;
    public String dataTransferPath = "";
    public String dataTransferUser = "";
    public FilePermissions filePermissionsData = FilePermissions.Default;
    public FilePermissions filePermissionsLogs = FilePermissions.Default;
    public FilePermissions filePermissionsScripts = FilePermissions.Default;
    public FilePermissions filePermissionsConfig = FilePermissions.Default;
    public SessionHandling sessionHandling = SessionHandling.Off;    
    public String logPath = Setup.TOKEN_LOGS + "/" + Setup.TOKEN_DATE + "_" + Setup.TOKEN_TIME;
    public int logDaysToLive = -1;
    public LogLevel logLevel = LogLevel.Info;
    public NotificationLevel notificationLevel = NotificationLevel.Off;
    public String notificationTasks = "";
    public boolean pythonNoBytecodeFiles = false;
    public String pythonHome= "";
    public boolean versionTrackingEnabled;
    public boolean versionTrackingManual;
    public String versionTrackingRemote = "";
    public String versionTrackingLogin = "";    
    public int dataServerPort = -1;
    public boolean serverEnabled;    
    public boolean serverCommandsHidden = false;
    public String serverHostName  = "";        
    public int serverPort = 8080;
    public boolean terminalEnabled;
    public int terminalPort = 3579;
    public boolean userManagement;
    public String userAuthenticator = "";
    public String instanceName = "";
    public boolean saveCommandStatistics;

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

    public boolean getNoBytecodeFiles(){
        boolean ret = pythonNoBytecodeFiles;
        String prop = System.getProperty(Options.NO_BYTECODE.toProperty());
        if ((prop != null) && (prop.length() > 0)) {
            ret =  Boolean.valueOf(prop);
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
            dataScanAutoSave,
            dataScanFlushRecords,
            dataScanReleaseRecords,
            dataScanPreserveTypes,
            dataScanSaveOutput,
            dataScanSaveScript,
            dataScanSaveSetpoints,
            dataScanSaveTimestamps,
            dataScanSaveLogs,
            dataScanLazyTableCreation);        
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
        return /*(serverMode || config.serverEnabled) && */ (dataScanStreamerPort > 0) && !Setup.isLocal();
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
}
