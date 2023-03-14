package ch.psi.pshell.core;

import static ch.psi.pshell.core.Setup.TOKEN_DATA;
import static ch.psi.pshell.core.Setup.TOKEN_LOGS;
import static ch.psi.pshell.core.Setup.TOKEN_DATE;
import static ch.psi.pshell.core.Setup.TOKEN_EXEC_NAME;
import static ch.psi.pshell.core.Setup.TOKEN_MONTH;
import static ch.psi.pshell.core.Setup.TOKEN_TIME;
import static ch.psi.pshell.core.Setup.TOKEN_YEAR;
import ch.psi.pshell.data.LayoutFDA;
import ch.psi.utils.Config;
import ch.psi.utils.IO.FilePermissions;
import ch.psi.utils.Str;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * An entity class holding the system configuration.
 */
public class Configuration extends Config {

    public static transient final String PROPERTY_CONSOLE_LOG = "ch.psi.pshell.console.log";
    public static transient final String PROPERTY_DATA_PROVIDER = "ch.psi.pshell.data.provider";
    public static transient final String PROPERTY_DATA_LAYOUT = "ch.psi.pshell.data.layout";
    public static transient final String PROPERTY_NO_BYTECODE_FILES = "ch.psi.pshell.data.nbcf";

    public boolean autoSaveScanData = true;
    public boolean saveConsoleSessionFiles;

    public String dataPath = TOKEN_DATA + "/" + TOKEN_YEAR + "_" + TOKEN_MONTH + "/" + TOKEN_DATE + "/" + TOKEN_DATE + "_" + TOKEN_TIME + "_" + TOKEN_EXEC_NAME;
    @Defaults(values = {"h5", "txt", "csv", "fda"})
    public String dataProvider = "h5";
    @Defaults(values = {"default", "table", "sf", "fda", "nx"})
    public String dataLayout = "default";
    public int depthDimension = 0;
    public boolean dataScanFlushRecords = false;
    public boolean dataScanReleaseRecords = false;
    public boolean dataScanPreserveTypes = false;
    public boolean dataScanSaveOutput = false;
    public boolean dataScanSaveScript = false;
    public boolean dataScanSaveSetpoints = false;
    public boolean dataScanLazyTableCreation = false;
    public DataTransferMode dataTransferMode = DataTransferMode.Off;
    public String dataTransferPath = "";
    public String dataTransferUser = "";
    public boolean disableDataFileLogs = false;
    public boolean disableEmbeddedAttributes = false;
    public FilePermissions filePermissionsData = FilePermissions.Default;
    public FilePermissions filePermissionsLogs = FilePermissions.Default;
    public FilePermissions filePermissionsScripts = FilePermissions.Default;
    public FilePermissions filePermissionsConfig = FilePermissions.Default;
    public SessionHandling sessionHandling = SessionHandling.Off;
    public String hostName  = "";
    public boolean hideServerMessages = false;
    public String logPath = TOKEN_LOGS + "/" + TOKEN_DATE + "_" + TOKEN_TIME;
    public int logDaysToLive = -1;
    public LogLevel logLevel = LogLevel.Info;
    public LogLevel logLevelConsole = LogLevel.Off;
    public NotificationLevel notificationLevel = NotificationLevel.Off;
    public String notifiedTasks = "";
    public boolean noBytecodeFiles = false;
    public boolean simulation;
    public boolean versionTrackingEnabled;
    public boolean versionTrackingManual;
    public String versionTrackingRemote = "";
    public String versionTrackingLogin = "";
    public int scanStreamerPort = -1;
    public int dataServerPort = -1;
    public boolean serverEnabled;
    public int serverPort = 8080;
    public boolean terminalEnabled;
    public int terminalPort = 3579;
    public boolean userManagement;
    public String userAuthenticator = "";
    public String instanceName = "";
    public boolean saveCommandStatistics;
    public boolean parallelInitialization;
    public boolean fdaSerialization;
    
    public String XScanCrlogicPrefix = "";
    public String XScanCrlogicIoc = "";
    public String XScanCrlogicChannel = "";
    public boolean XScanCrlogicAbortable = true;
    public boolean XScanCrlogicSimulated;
    public int XScanMoveTimeout = 600; // 10 Minutes maximum move time 
    public boolean XScanAppendSuffix = true;

    public enum LogLevel {

        Off,
        Severe,
        Warning,
        Info,
        Fine,
        Finer,
        Finest
    }

    public enum NotificationLevel {
        Off,
        User,
        Error,
        Completion
    }

    public enum DataTransferMode {
        Off,
        Copy,
        Move
    }
    
    public enum SessionHandling {
        Off,
        Files,
        Exclusive,
        On
    }


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
        if (notifiedTasks==null){
            notifiedTasks = "";
        }                         
    }

    public Level getLogLevel() {
        return Level.parse(logLevel.toString().toUpperCase());
    }

    public Level getConsoleLogLevel() {
        String consoleLogLevel = System.getProperty(PROPERTY_CONSOLE_LOG);
        return Level.parse((consoleLogLevel != null)
                ? consoleLogLevel.toUpperCase()
                : logLevelConsole.toString().toUpperCase());
    }

    public String getDataProvider() {
        String provider = System.getProperty(PROPERTY_DATA_PROVIDER);
        return provider == null ? dataProvider : provider;
    }

    public String getDataLayout() {
        String layout = System.getProperty(PROPERTY_DATA_LAYOUT);
        return layout == null ? dataLayout : layout;
    }
    
    public List<String> getNotifiedTasks() {
        List<String> ret = new ArrayList<>();
        String[] tokens = Str.split(notifiedTasks, new String[]{"|", ";", ","});
        for (String str : tokens) {
            str = str.trim();
            if (!str.isEmpty()) {
                ret.add(str);
            }
        }
        return ret;
    }

    public boolean getNoBytecodeFiles(){
        boolean ret = noBytecodeFiles;
        String prop = System.getProperty(PROPERTY_NO_BYTECODE_FILES);
        if ((prop != null) && (prop.length() > 0)) {
            ret =  Boolean.valueOf(prop);
        }
        return ret;
    }

    public int getDepthDim() {
        return (((depthDimension < 0) || (depthDimension > 2)) ? 0 : depthDimension);
    }

    public boolean isParallelInitialization() {
        String prop = System.getProperty(Setup.PROPERTY_PARALLEL_INIT);
        if ((prop != null) && (prop.length() > 0)) {
            return Boolean.valueOf(prop);
        }
        return parallelInitialization;
    }
    
    public static String getXScanDataFileName() {
        String ret = Context.getInstance().getConfig().dataPath;
        if (Context.getInstance().getConfig().fdaSerialization) {
            ret = ret.replaceAll("./$", "");
            return ret + "/" + LayoutFDA.getFilePrefix();
        }
        return ret;
    }        
    
}
