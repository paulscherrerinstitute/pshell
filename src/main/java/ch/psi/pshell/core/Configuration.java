package ch.psi.pshell.core;

import static ch.psi.pshell.core.Setup.TOKEN_DATA;
import static ch.psi.pshell.core.Setup.TOKEN_LOGS;
import static ch.psi.pshell.core.Setup.TOKEN_DATE;
import static ch.psi.pshell.core.Setup.TOKEN_EXEC_NAME;
import static ch.psi.pshell.core.Setup.TOKEN_MONTH;
import static ch.psi.pshell.core.Setup.TOKEN_TIME;
import static ch.psi.pshell.core.Setup.TOKEN_YEAR;
import ch.psi.utils.Config;
import java.util.logging.Level;

/**
 * An entity class holding the system configuration.
 */
public class Configuration extends Config {

    public boolean autoSaveScanData = true;
    public boolean createSessionFiles;

    public String dataPath = TOKEN_DATA + "/" + TOKEN_YEAR + "_" + TOKEN_MONTH + "/" + TOKEN_DATE + "/" + TOKEN_DATE + "_" + TOKEN_TIME + "_" + TOKEN_EXEC_NAME;
    @Defaults(values = {"h5", "txt"})
    public String dataProvider = "h5";
    @Defaults(values = {"default", "table"})
    public String dataLayout = "default";
    public boolean dataScanFlushRecords = false;
    public boolean dataScanReleaseRecords = false;
    public boolean dataScanPreserveTypes = false;
    public String hostName;
    public String logPath = TOKEN_LOGS + "/" + TOKEN_DATE + "_" + TOKEN_TIME;
    public int logDaysToLive = -1;
    public LogLevel logLevel = LogLevel.Info;
    public LogLevel logLevelConsole = LogLevel.Off;
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

    public enum LogLevel {

        Off,
        Severe,
        Warning,
        Info,
        Fine,
        Finer,
        Finest
    }

    public Level getLogLevel() {
        return Level.parse(logLevel.toString().toUpperCase());
    }

    String commandLineLogLevelConsole;

    public Level getConsoleLogLevel() {
        return Level.parse((commandLineLogLevelConsole != null)
                ? commandLineLogLevelConsole.toUpperCase()
                : logLevelConsole.toString().toUpperCase());
    }

    public String getName() {
        if (instanceName == null) {
            return "";
        }
        return instanceName;
    }
}
