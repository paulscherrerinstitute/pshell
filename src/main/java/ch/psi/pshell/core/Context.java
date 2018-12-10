package ch.psi.pshell.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import ch.psi.utils.Arr;
import ch.psi.utils.Config;
import ch.psi.utils.Config.ConfigListener;
import ch.psi.utils.Configurable;
import ch.psi.utils.Convert;
import ch.psi.utils.History;
import ch.psi.utils.IO;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
import ch.psi.utils.Threading;
import ch.psi.pshell.core.Configuration.NotificationLevel;
import ch.psi.pshell.core.ExecutionParameters.ExecutionStage;
import ch.psi.pshell.core.VersioningManager.Revision;
import ch.psi.pshell.data.DataServer;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Table;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.Interlock;
import ch.psi.pshell.device.Stoppable;
import ch.psi.pshell.scan.PlotScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanBase;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scan.ScanResult;
import ch.psi.pshell.scan.ScanStreamer;
import ch.psi.pshell.security.UsersManager;
import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.pshell.scripting.ScriptManager;
import ch.psi.pshell.scripting.ScriptManager.StatementsEvalListener;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.scripting.ViewPreference.PlotPreferences;
import ch.psi.pshell.security.AccessLevel;
import ch.psi.pshell.security.Rights;
import ch.psi.pshell.security.UsersManagerListener;
import ch.psi.pshell.security.User;
import ch.psi.pshell.security.UserAccessException;
import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import ch.psi.utils.SortedProperties;
import ch.psi.utils.Sys.OSFamily;
import ch.psi.utils.Threading.VisibleCompletableFuture;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarFile;

/**
 * Global singleton managing creation, disposal and holding the state of the
 * system.
 */
public class Context extends ObservableBase<ContextListener> implements AutoCloseable, Configurable {

    static final Logger logger = Logger.getLogger(Context.class.getName());

    Server server;
    final History history;
    final Setup setup;
    final Configuration config;
    final Boolean localMode;
    final Boolean bareMode;
    final Boolean emptyMode;
    final Boolean genericMode;
    final Boolean interpreterEnabled;
    final Boolean serverMode;
    final Boolean simulation;
    final Boolean forceExtract;
    final Boolean fileLockEnabled;
    final LogManager logManager;
    final PluginManager pluginManager;
    final UsersManager usersManager;
    final DataManager dataManager;
    final HashMap<Thread, ExecutionParameters> executionPars = new HashMap<>();
    ExecutorService interpreterExecutor;
    Thread interpreterThread;
    ScriptManager scriptManager;
    VersioningManager versioningManager;
    NotificationManager notificationManager;
    TaskManager taskManager;
    DevicePool devicePool;
    ScriptStdio scriptStdio;
    TerminalServer terminalServer;
    ScanStreamer scanStreamer;
    DataServer dataStreamer;
    CommandManager commandManager;

    int runCount;

    public int getRunCount() {
        return runCount;
    }

    public static final String PROPERTY_SETUP_FILE = "ch.psi.pshell.setup.file";
    public static final String PROPERTY_USER = "ch.psi.pshell.user";
    public static final String PROPERTY_LOCAL_MODE = "ch.psi.pshell.local";
    public static final String PROPERTY_BARE_MODE = "ch.psi.pshell.bare";
    public static final String PROPERTY_EMPTY_MODE = "ch.psi.pshell.empty";
    public static final String PROPERTY_GENERIC_MODE = "ch.psi.pshell.generic";
    public static final String PROPERTY_DISABLED = "ch.psi.pshell.disabled";
    public static final String PROPERTY_SERVER_MODE = "ch.psi.pshell.server";
    public static final String PROPERTY_FILE_LOCK = "ch.psi.pshell.file.lock";
    public static final String PROPERTY_FORCE_EXTRACT = "ch.psi.pshell.force.extract";
    public static final String PROPERTY_SIMULATION = "ch.psi.pshell.simulation";

    private static Context instance;

    private Context() {

        if (System.getProperty(PROPERTY_LOCAL_MODE) != null) {
            localMode = Boolean.valueOf(System.getProperty(PROPERTY_LOCAL_MODE));
        } else {
            localMode = false;
        }

        if (System.getProperty(PROPERTY_BARE_MODE) != null) {
            bareMode = Boolean.valueOf(System.getProperty(PROPERTY_BARE_MODE));
        } else {
            bareMode = false;
        }

        if (System.getProperty(PROPERTY_EMPTY_MODE) != null) {
            emptyMode = Boolean.valueOf(System.getProperty(PROPERTY_EMPTY_MODE));
        } else {
            emptyMode = false;
        }

        if (System.getProperty(PROPERTY_GENERIC_MODE) != null) {
            genericMode = Boolean.valueOf(System.getProperty(PROPERTY_GENERIC_MODE));
        } else {
            genericMode = false;
        }

        if (System.getProperty(PROPERTY_DISABLED) != null) {
            interpreterEnabled = !Boolean.valueOf(System.getProperty(PROPERTY_DISABLED));
        } else {
            interpreterEnabled = true;
        }

        if (System.getProperty(PROPERTY_SERVER_MODE) != null) {
            serverMode = Boolean.valueOf(System.getProperty(PROPERTY_SERVER_MODE));
        } else {
            serverMode = false;
        }

        if (System.getProperty(PROPERTY_SIMULATION) != null) {
            simulation = Boolean.valueOf(System.getProperty(PROPERTY_SIMULATION));
        } else {
            simulation = false;
        }

        if (System.getProperty(PROPERTY_FORCE_EXTRACT) != null) {
            forceExtract = Boolean.valueOf(System.getProperty(PROPERTY_FORCE_EXTRACT));
        } else {
            forceExtract = false;
        }

        if (System.getProperty(PROPERTY_FILE_LOCK) != null) {
            fileLockEnabled = Boolean.valueOf(System.getProperty(PROPERTY_FILE_LOCK));
        } else {
            fileLockEnabled = true;
        }

        setup = new Setup();
        try {
            setup.load(System.getProperty(PROPERTY_SETUP_FILE));
        } catch (IOException ex) {
            throw new RuntimeException("Cannot generate setup file");
        }

        config = new Configuration();
        try {
            config.load(setup.getConfigFile());
        } catch (IOException ex) {
            throw new RuntimeException("Cannot generate configuration file");
        }
        String contextName = (config.getName().length() > 0) ? config.getName() : "unknown";
        if (config.getName().length() > 0) {
            System.out.println("\n[" + contextName + "]\n");
        }

        if ((config.hostName != null) && (!config.hostName.trim().isEmpty()) && (!config.hostName.equalsIgnoreCase("null"))) {
            if (!localMode) {
                String hostName = config.hostName;
                try {
                    hostName = Sys.getLocalHost();
                    if (!hostName.equalsIgnoreCase(config.hostName)) {
                        InetAddress[] addresses
                                = InetAddress.getAllByName(hostName);
                        for (InetAddress address : addresses) {
                            if (address.getHostAddress().equalsIgnoreCase(config.hostName)) {
                                hostName = config.hostName;
                                break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Context.class.getName()).log(Level.WARNING, null, ex);
                }
                if (!hostName.equalsIgnoreCase(config.hostName)) {
                    throw new RuntimeException("Application must run on host: " + config.hostName + " or else in local mode (-l option)");
                }
            }
        }

        config.backup();
        config.addListener(new Config.ConfigListener() {
            @Override
            public void onSaving(Config config) {
                getRights().assertConfigAllowed();
            }
        });

        if (System.getProperty(ScriptManager.PROPERTY_PYTHON_HOME) == null) {
            System.setProperty(ScriptManager.PROPERTY_PYTHON_HOME, setup.getScriptPath());
        }

        logManager = new LogManager();
        restartLogger();

        logger.info("Process: " + Sys.getProcessName());
        logger.info("Context: " + contextName);
        logger.info("Pars: " + Sys.getCommand());
        logger.info("User: " + Sys.getUserName());
        logger.info("Host: " + Sys.getLocalHost());

        if (localMode) {
            //In local mode inherits the history, but not persist changes
            history = new History(setup.getCommandHistoryFile(), 1000, false);
            history.restore();

        } else {
            history = new History(setup.getCommandHistoryFile(), 1000, true);
        }

        dataManager = new DataManager(this);

        pluginManager = new PluginManager();

        commandManager = new CommandManager();

        usersManager = new UsersManager(setup);

        devicePool = new DevicePool();

        setStdioListener(null);

        usersManager.addListener(new UsersManagerListener() {
            @Override
            public void onUserChange(User user, User former) {
                if (former != null) {
                    setup.setUser(user);
                    if (setup.logPath.contains(Setup.TOKEN_USER)) {
                        logger.log(Level.INFO, "New user: restarting logger");
                        restartLogger();
                    }
                    if (setup.sessionsPath.contains(Setup.TOKEN_USER)) {
                        if (getConfig().createSessionFiles && !isLocalMode()) {
                            scriptManager.setSessionFilePath(setup.getSessionsPath());
                        }
                    }
                }
                logger.log(Level.INFO, "User: " + user.toString());
            }
        });

        addScanListener(scanListener);

        executionPars.put(null, new ExecutionParameters());
    }

    /**
     * Returns singleton object, instantiating if needed.
     */
    public static Context createInstance() {
        if (instance == null) {
            instance = new Context();
            instance.pluginManager.loadExtensionsFolder();
            //To provide services even if context does not initialize all right (and in disabled mode).                
            try {
                instance.dataManager.initialize();
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        return instance;
    }

    public static Context getInstance() {
        return instance;
    }

    void restartLogger() {
        String ext = localMode ? "local.log" : "log";
        logFileName = getSetup().expandPath(getConfig().logPath + "." + ext);
        logManager.start(logFileName, isLocalMode() ? -1 : config.logDaysToLive);
        logManager.setLevel(config.getLogLevel());
        logManager.setConsoleLoggerLevel(config.getConsoleLogLevel());
    }

    String logFileName;

    public String getLogFileName() {
        return logFileName;
    }

    //State
    public class ContextStateException extends Exception {

        ContextStateException() {
            super("Invalid state: " + getState());
        }

        ContextStateException(State state) {
            super("Invalid state transition: " + getState() + " to " + state);
        }
    }

    volatile State state = State.Invalid;

    protected void setState(State state) throws ContextStateException {
        if (this.state != state) {
            if ((this.state == State.Closing)
                    || //disposed is definitive
                    (this.state.isRunning() && (state == State.Busy))
                    || //One high-level action at a time -
                    ((this.state != State.Busy) && (state == State.Paused))
                    || //Can pause
                    ((this.state == State.Fault) && (state != State.Initializing)) //To quit Fault state must restart
                    ) {
                throw new ContextStateException(state);
            }
            State former = this.state;
            this.state = state;
            logger.fine("State: " + state);
            triggerContextStateChanged(state, former);
            if (pluginManager != null) {
                pluginManager.onStateChange(state, former);
            }
            if (!state.isProcessing()) {
                runningScript = null;
            }
        }
    }

    @Hidden
    public void assertReady() throws ContextStateException {
        assertState(State.Ready);
    }

    @Hidden
    public void assertStarted() throws ContextStateException {
        assertNotState(State.Initializing);
        if (!state.isActive()) {
            throw new ContextStateException();
        }
    }

    @Hidden
    public void assertNotRunning() throws ContextStateException {
        assertNotState(State.Busy);
        assertNotState(State.Paused);
    }

    @Hidden
    public void assertState(State state) throws ContextStateException {
        if (this.state != state) {
            throw new ContextStateException();
        }
    }

    @Hidden
    public void assertNotState(State state) throws ContextStateException {
        if (this.state == state) {
            throw new ContextStateException();
        }
    }

    public State getState() {
        return state;
    }

    public void waitState(State state, int timeout) throws IOException, InterruptedException {
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

    public void waitStateNot(State state, int timeout) throws IOException, InterruptedException {
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

    public boolean isLocalMode() {
        return localMode;
    }

    public boolean isBareMode() {
        return bareMode;
    }

    public boolean isEmptyMode() {
        return emptyMode;
    }

    public boolean isGenericMode() {
        return genericMode;
    }

    public boolean isInterpreterEnabled() {
        return interpreterEnabled;
    }

    public boolean isServerEnabled() {
        return (serverMode || config.serverEnabled) && (config.serverPort > 0) && !isLocalMode();
    }

    public boolean isTerminalEnabled() {
        return (serverMode || config.terminalEnabled) && (config.terminalPort > 0) && !isLocalMode();
    }

    public boolean isScanStreamerEnabled() {
        return /*(serverMode || config.serverEnabled) && */ (config.scanStreamerPort > 0) && !isLocalMode();
    }

    public boolean isDataStreamerEnabled() {
        return /*(serverMode || config.serverEnabled) && */ (config.dataServerPort > 0) && !isLocalMode();
    }

    public boolean isSimulation() {
        return config.simulation || simulation;
    }

    @Hidden
    public void assertInterpreterEnabled() throws IOException {
        if (!isInterpreterEnabled()) {
            throw new IOException("Interpreter is disabled");
        }
    }

    @Hidden
    public void assertServerEnabled() throws IOException {
        if (!isServerEnabled()) {
            throw new IOException("Server is disabled");
        }
    }

    @Hidden
    public boolean isInterpreterThread() {
        return isInterpreterThread(Thread.currentThread());
    }

    @Hidden
    public boolean isInterpreterThread(Thread thread) {
        return thread == interpreterThread;
    }

    //
    public class InterpreterThreadException extends Exception {

        InterpreterThreadException() {
            super("Not in interpreter thread");
        }
    }

    void assertInterpreterThread(State state) throws InterpreterThreadException {
        if (!isInterpreterThread()) {
            throw new InterpreterThreadException();
        }
    }

    //Event Triggering
    protected void triggerShellCommand(final CommandSource source, final String command) {
        if (command != null) {
            for (ContextListener listener : getListeners()) {
                try {
                    listener.onShellCommand(source, command);
                } catch (Throwable ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void triggerShellResult(final CommandSource source, Object result) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onShellResult(source, result);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerShellStdout(String str) {
        if (str != null) {
            for (ContextListener listener : getListeners()) {
                try {
                    listener.onShellStdout(str);
                } catch (Throwable ex) {
                    logger.log(Level.WARNING, null, ex);
                }

            }
        }
    }

    protected void triggerShellStderr(String str) {
        if (str != null) {
            for (ContextListener listener : getListeners()) {
                try {
                    listener.onShellStderr(str);
                } catch (Throwable ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void triggerShellStdin(String str) {
        if (str != null) {
            for (ContextListener listener : getListeners()) {
                try {
                    listener.onShellStdin(str);
                } catch (Throwable ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void triggerContextStateChanged(final State state, final State former) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onContextStateChanged(state, former);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerContextInitialized(int runCount) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onContextInitialized(runCount);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerNewStatement(final Statement statement) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onNewStatement(statement);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerExecutingStatement(final Statement statement) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onExecutingStatement(statement);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerExecutedStatement(final Statement statement) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onExecutedStatement(statement);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerExecutingFile(final String fileName) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onExecutingFile(fileName);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerExecutedFile(final String fileName, final Object result) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onExecutedFile(fileName, result);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        if (pluginManager != null) {
            pluginManager.onExecutedFile(fileName, result);
        }
        boolean error = (result instanceof Throwable) && !aborted;
        try {
            if (error && config.getNotificationLevel() != NotificationLevel.Off) {
                this.notify("Execution error", "File: " + fileName + "\n" + ((Throwable) result).getMessage(), null);
            }
            if (config.getNotificationLevel() == NotificationLevel.Completion) {
                if (aborted) {
                    notify("Execution aborted", "File: " + fileName, null);
                } else {
                    notify("Execution success", "File: " + fileName + "\nReturn:" + String.valueOf(result), null);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    protected void triggerConfigurationChange(final Configurable obj) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onConfigurationChange(obj);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerBranchChange(final String branch) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onBranchChange(branch);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerPreferenceChange(final ViewPreference preference, final Object value) {
        for (ContextListener listener : getListeners()) {
            try {
                listener.onPreferenceChange(preference, value);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    @Hidden
    public ScriptType getScriptType() {
        return setup.getScriptType();
    }

    String getStartupScript() {
        return setup.getStartupScript();
    }

    //Stdio management
    final ScriptStdioListener defaultScriptStdioListener = new ScriptStdioListener() {
        @Override
        public void onStdout(String str) {
            triggerShellStdout(str);
        }

        @Override
        public void onStderr(String str) {
            triggerShellStderr(str);
        }

        @Override
        public String readStdin() throws InterruptedException {
            //TODO
            synchronized (stdinInput) {
                stdinInput.waiting = true;
                stdinInput.wait();
                return stdinInput.str;
            }
        }
    };

    class StdinInput {

        boolean waiting;
        String str;
    }
    final StdinInput stdinInput = new StdinInput();

    @Hidden
    public boolean waitingStdin() {
        synchronized (stdinInput) {
            return (stdinInput.waiting);
        }
    }

    @Hidden
    public void redirectScriptStdio() {
        setStdioListener(defaultScriptStdioListener);
    }

    ScriptStdioListener scriptStdioListener;

    void setStdioListener(ScriptStdioListener listener) {
        scriptStdioListener = listener;

        if (scriptManager != null) {
            if (scriptStdio != null) {
                scriptStdio.close();
            }
            if (scriptStdioListener != null) {
                scriptStdio = new ScriptStdio(scriptManager);
                scriptStdio.setListener(listener);
            } else {
                //If no listener then use to System stdio
                scriptManager.setWriter(new PrintWriter(System.out));
                scriptManager.setErrorWriter(new PrintWriter(System.err));
            }
        }
    }

    //Access methods
    public DevicePool getDevicePool() {
        return devicePool;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public UsersManager getUsersManager() {
        return usersManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public NotificationManager getMailManager() {
        return notificationManager;
    }

    public VersioningManager getVersioningManager() {
        return versioningManager;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public ScanStreamer getScanStreamer() {
        return (config.scanStreamerPort > 0) ? scanStreamer : null;
    }

    public DataServer getDataStreamer() {
        return (config.dataServerPort > 0) ? dataStreamer : null;
    }

    public Server getServer() {
        return server;
    }

    //High-level commands
    public enum Command {

        restart,
        eval,
        run,
        debug,
        then,
        abort,
        updateAll,
        stopAll,
        stopDevice,
        reinit,
        injectVars,
        reloadPlugins,
        pause,
        resume,
        step,
        setPreference,
        setLogLevel,
        saveConfig,
        saveDeviceConfig,
        clearHistory,
        commit,
        cleanupRepository,
        checkoutTag,
        checkoutBranch,
        checkoutRemote,
        push,
        pull,
        shutdown;

        void putLogTag(StringBuilder sb) {
            sb.append(Str.toTitleCase(toString()));
        }

    }

    protected void onCommand(Command command, Object[] parameters, CommandSource source) {
        StringBuilder sb = new StringBuilder();
        command.putLogTag(sb);
        if (parameters != null) {
            sb.append(": ");
            for (int i = 0; i < parameters.length; i++) {
                sb.append(Str.toString(parameters[i]));
                if (i < (parameters.length - 1)) {
                    sb.append(", ");
                }
            }
        }
        sb.append(" ");
        source.putLogTag(sb);
        logger.info(sb.toString());
        //TODO: Could dadd security check here?
    }

    void shutdown(final CommandSource source) {
        onCommand(Command.shutdown, null, source);
        System.exit(0);
    }

    void restart(final CommandSource source) throws ContextStateException {
        onCommand(Command.restart, null, source);
        //A new file for each session
        boolean firstRun = (getState() == State.Invalid);

        setState(State.Initializing);

        for (AutoCloseable ac : new AutoCloseable[]{scanStreamer, dataStreamer, taskManager, notificationManager, scriptManager, devicePool, versioningManager}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        Interlock.clear();

        //A new file for each session
        if ((logManager != null) && (!firstRun)) {
            restartLogger();
            System.gc();
        }
        System.setProperty(GenericDevice.PROPERTY_CONFIG_PATH, setup.getDevicesPath());

        logger.info("Initializing context");
        restartThread = Thread.currentThread();
        try {
            if (!isLocalMode()) {
                if ((terminalServer != null) && ((!isTerminalEnabled()) || (config.terminalPort != terminalServer.getPort()))) {
                    terminalServer.close();
                    terminalServer = null;
                }
                if ((terminalServer == null) && isTerminalEnabled()) {
                    try {
                        terminalServer = new TerminalServer(config.terminalPort);
                        logger.info("Started terminal server on port " + config.terminalPort);
                    } catch (Exception ex) {
                        throw new Exception("Error initializing terminal server on port " + config.terminalPort, ex);
                    }
                }

                if ((server != null) && ((!isServerEnabled()) || (config.serverPort != server.getPort()))) {
                    server.close();
                    server = null;
                }
                //Must restart sw if server changes
                if ((server == null) && isServerEnabled()) {
                    try {
                        server = new Server(null, config.serverPort);
                    } catch (Exception ex) {
                        throw new Exception("Error initializing server on port " + config.serverPort, ex);
                    }
                }
                if (isScanStreamerEnabled()) {
                    scanStreamer = new ScanStreamer(config.scanStreamerPort);
                }
                if (isDataStreamerEnabled()) {
                    dataStreamer = new DataServer(config.dataServerPort);
                }
            }
            //Remove script listeners
            ArrayList<ContextListener> scriptListeners = new ArrayList();
            for (ContextListener listener : getListeners()) {
                if (listener.getClass().getName().startsWith(ScriptManager.JYTHON_OBJ_CLASS)) {
                    scriptListeners.add(listener);
                }
                //TODO: Only removing python listeners
            }
            for (ContextListener listener : scriptListeners) {
                removeListener(listener);
            }

            if (interpreterExecutor != null) {
                interpreterExecutor.shutdownNow();
                Threading.stop(interpreterThread, true, 3000);
            }

            usersManager.initialize();
            if ((System.getProperty(PROPERTY_USER) != null) && (runCount == 0)) {
                try {
                    if (!usersManager.selectUser(System.getProperty(PROPERTY_USER), CommandSource.ctr)) {
                        exit("No password provided");
                    }
                } catch (Exception ex) {
                    exit(ex);
                }
            }
            if ((!firstRun) || (!dataManager.isInitialized())) {
                dataManager.initialize();
            }
            notificationManager = new NotificationManager();

            devicePool = new DevicePool();
            logger.log(Level.INFO, "Loading Device Pool");
            try {
                devicePool.initialize();
            } catch (FileNotFoundException | NoSuchFileException ex) {
                logger.log(Level.FINE, null, ex);
            }
            devicePool.addListener(new DevicePoolListener() {
                @Override
                public void onDeviceAdded(GenericDevice dev) {
                    if (scriptManager != null) {
                        scriptManager.addInjection(dev.getName(), dev);
                    }
                    if (config.userManagement) {
                        if (dev instanceof ch.psi.pshell.device.Device) {
                            dev.addListener(deviceWriteAccessListener);
                        }
                        if (dev.getConfig() != null) {
                            dev.getConfig().addListener(deviceConfigChangeListener);
                        }
                    }
                }

                @Override
                public void onDeviceRemoved(GenericDevice dev) {
                    if (scriptManager != null) {
                        scriptManager.removeInjection(dev.getName());
                    }
                }
            });

            if (config.userManagement) {
                for (GenericDevice dev : devicePool.getAllDevices()) {
                    if (dev instanceof ch.psi.pshell.device.Device) {
                        dev.addListener(deviceWriteAccessListener);
                    }
                    if (dev.getConfig() != null) {
                        dev.getConfig().addListener(deviceConfigChangeListener);
                    }
                }
            }

            final HashMap<String, Object> injections = new HashMap<>();
            for (GenericDevice dev : devicePool.getAllDevices()) {
                injections.put(dev.getName(), dev);
            }

            injections.put("run_count", runCount);

            interpreterExecutor = Executors.newSingleThreadExecutor((Runnable runnable) -> {
                interpreterThread = new Thread(Thread.currentThread().getThreadGroup(), runnable, "Interpreter Thread");
                return interpreterThread;
            });

            if (isInterpreterEnabled()) {
                runInInterpreterThread((Callable<InterpreterResult>) () -> {
                    scriptManager = new ScriptManager(getScriptType(), setup.getLibraryPath(), injections);
                    scriptManager.setSessionFilePath((getConfig().createSessionFiles && !isLocalMode()) ? setup.getSessionsPath() : null);
                    setStdioListener(scriptStdioListener);
                    String script = getStartupScript();
                    if (script == null) {
                        throw new RuntimeException("Cannot locate startup script");
                    }
                    Object ret = scriptManager.evalFile(script);
                    logger.info("Executed startup script");
                    if (!isGenericMode()) {
                        try {
                            scriptManager.evalFile(getSetup().getLocalStartupScript());
                            logger.info("Executed local startup script");
                            scriptManager.resetLineNumber(); //So first statement will be number 1
                        } catch (Exception ex) {
                            if ((ex instanceof FileNotFoundException) && ex.getMessage().equals(getSetup().getLocalStartupScript())) {
                                logger.warning("Local initialization script is not present");
                            } else {
                                ex.printStackTrace();
                                logger.log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    return null;
                });
                if (scriptManager == null) {
                    throw new Exception("Error instantiating script manager");
                }
            }

            if (isVersioningEnabled()) {
                versioningManager = new VersioningManager();
            }
            if (pluginManager != null) {
                pluginManager.onInitialize(runCount);
            }
            //Only instantiate it if state goes ready                
            taskManager = new TaskManager();
            if (!isLocalMode()) {
                taskManager.initialize();
            }
            setState(State.Ready);
            triggerContextInitialized(runCount);
            runCount++;
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, null, ex);
            setState(State.Fault);
        }
    }

    DeviceListener deviceWriteAccessListener = new DeviceAdapter() {
        @Override
        public void onValueChanging(Device device, Object value, Object former) throws Exception {
            CommandSource source = getPublicCommandSource();
            if (!source.isInternal()) {
                getRights(source).assertDeviceWriteAllowed();
            }
        }
    };

    ConfigListener deviceConfigChangeListener = new Config.ConfigListener() {
        @Override
        public void onSaving(Config config) {
            getRights().assertDeviceConfigAllowed();
        }
    };

    boolean abort(final CommandSource source, long commandId) throws InterruptedException {
        onCommand(Command.abort, new Object[]{commandId}, source);
        return commandManager.abort(source, commandId);
    }

    boolean join(long commandId) throws InterruptedException {
        return commandManager.join(commandId);
    }

    boolean isRunning(long commandId) {
        return commandManager.isRunning(commandId);
    }

    Map getResult(long commandId) throws Exception {
        return commandManager.getResult(commandId);
    }

    long waitNewCommand(CompletableFuture cf) throws InterruptedException {
        long now = System.currentTimeMillis();
        if (cf instanceof VisibleCompletableFuture) {
            Thread thread = ((VisibleCompletableFuture) cf).waitRunningThread(250);
            CommandInfo current = commandManager.getThreadCommand(thread);
            if ((current != null) && (current.start >= now)) {
                return current.id;
            }
            if (thread != null) {
                return waitNewCommand(thread, 250);
            }
        }
        return 0;
    }

    long waitNewCommand(Thread thread, int timeout) throws InterruptedException {
        long ret = commandManager.waitNewCommand(thread, timeout);
        return ret;
    }

    Object runInInterpreterThread(Callable callable) throws ScriptException, IOException, InterruptedException {
        assertInterpreterEnabled();
        Object result = null;
        try {
            if (isInterpreterThread()) {
                result = callable.call();
            } else {
                try {
                    synchronized (interpreterExecutor) {
                        result = interpreterExecutor.submit(callable).get();
                    }
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof ScriptException) {
                        throw (ScriptException) ex.getCause();
                    }
                    if ((ex.getCause() instanceof RuntimeException) && (getScriptType() == ScriptType.js)) { //On JS exceptions are RuntimeException
                        throw new ScriptException((Exception) ((RuntimeException) ex.getCause()).getCause());
                    }
                    if (ex.getCause() instanceof InterruptedException) {
                        throw (InterruptedException) ex.getCause();
                    }
                    if (ex.getCause() instanceof IOException) {
                        throw (IOException) ex.getCause();
                    } else {
                        throw ex;
                    }
                }
            }
            return result;
        } catch (ScriptException | InterruptedException | IOException ex) {
            result = ex;
            throw ex;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, null, t); //Should never happen;
            return null;
        } finally {
            if ((result != null) && (result instanceof InterpreterResult)) {
                if (((InterpreterResult) result).exception != null) {
                    result = ((InterpreterResult) result).exception;
                } else if (((InterpreterResult) result).result != null) {
                    result = ((InterpreterResult) result).result;
                }
            }
            commandManager.finishCommandInfo(commandManager.getInterpreterThreadCommand(), result);
        }
    }

    Object evalLine(final CommandSource source, final String command) throws ScriptException, IOException, ContextStateException, InterruptedException {
        assertInterpreterEnabled();
        synchronized (stdinInput) {
            if (stdinInput.waiting) {
                stdinInput.waiting = false;
                stdinInput.str = command;
                StringBuilder sb = new StringBuilder();
                sb.append("Input: ").append(command).append(" ");
                source.putLogTag(sb);
                logger.info(sb.toString());
                triggerShellStdin(command);
                stdinInput.notifyAll();
                return null;
            }
        }
        onCommand(Command.eval, new Object[]{command}, source);
        triggerShellCommand(source, command);
        //Verify control command
        try {
            final String line = ControlCommand.adjust(command);
            if (ControlCommand.match(line)) {
                ControlCommand controlCommand = ControlCommand.fromString(line);
                if (controlCommand == null) {
                    throw new IOException("Invalid control command");
                } else {
                    if (controlCommand.isScripControl()) {
                        assertRunAllowed(source);
                    } else if (controlCommand.isEval()) {
                        assertConsoleCommandAllowed(source);
                    }
                    String parameters = "";
                    try {
                        parameters = line.substring(controlCommand.toString().length() + 2).trim();
                    } catch (Exception ex) {
                    }
                    Object ret = processControlCommand(source, controlCommand, parameters);
                    if (controlCommand != ControlCommand.run) {
                        triggerShellResult(source, ret);
                    }
                    return ret;
                }
            }

            CommandInfo info = new CommandInfo(source, null, command, null, false);
            startExecution(source, null, info);
            setSourceUI(source);
            try {
                InterpreterResult result = (InterpreterResult) runInInterpreterThread((Callable<InterpreterResult>) () -> scriptManager.eval(line));
                if (result == null) {
                    return null;
                }
                if (result.exception != null) {
                    throw result.exception;
                }
                triggerShellResult(source, result.result);
                return result.result;
            } finally {
                endExecution(info);
            }
        } catch (Throwable t) {
            triggerShellResult(source, t);
            throw t;
        } finally {
            try {
                if (!command.trim().isEmpty()) {
                    history.put(command);
                }
            } catch (Exception ex) {
            }
        }
    }

    CompletableFuture<?> getInterpreterFuture(final Threading.SupplierWithException<?> supplier) {
        return Threading.getFuture(supplier, interpreterExecutor);
    }

    CompletableFuture<?> evalLineAsync(final CommandSource source, final String line) throws ContextStateException {
        assertReady();  //TODO: This is not strict, state can change before the thread starts
        return getInterpreterFuture(() -> evalLine(source, line));
    }

    CompletableFuture<?> evalFileAsync(final CommandSource source, final String fileName) throws ContextStateException {
        return evalFileAsync(source, fileName, null);
    }

    CompletableFuture<?> evalFileAsync(final CommandSource source, final String fileName, final Object args) throws ContextStateException {
        assertReady();  //TODO: This is not strict, state can change before the thread starts
        return getInterpreterFuture(() -> evalFile(source, fileName, args));
    }

    Object evalFileBackground(final CommandSource source, final String fileName) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalFileBackground(source, fileName, null);
    }

    Object evalFileBackground(final CommandSource source, final String fileName, final Object args) throws ScriptException, IOException, ContextStateException, InterruptedException {
        assertInterpreterEnabled();
        if (fileName == null) {
            return null;
        }
        final String scriptName = getStandardScriptName(fileName);
        final Map<String, Object> argsDict = parseArgs(args);
        ArrayList pars = new ArrayList();
        pars.add(scriptName);
        pars.add("Background");
        for (String key : argsDict.keySet()) {
            pars.add(key + "=" + argsDict.get(key));
        }
        onCommand(Command.run, pars.toArray(), source);
        assertStarted();
        assertRunAllowed(source);
        //Command source in statements execution will be CommandSource.script 
        Object result = null;
        commandManager.initCommandInfo(new CommandInfo(source, fileName, null, args, true));
        try {
            createExecutionContext();
            //TODO: args passing is not theread safe
            for (String key : argsDict.keySet()) {
                scriptManager.setVar(key, argsDict.get(key));
            }
            result = scriptManager.evalFileBackground(fileName);
            return result;
        } catch (Exception ex) {
            result = ex;
            throw ex;
        } finally {
            disposeExecutionContext();
            commandManager.finishCommandInfo(result);
        }
    }

    CompletableFuture<?> evalFileBackgroundAsync(final CommandSource source, final String fileName) {
        return Threading.getFuture(() -> evalFileBackground(source, fileName));
    }

    CompletableFuture<?> evalFileBackgroundAsync(final CommandSource source, final String fileName, final Object args) {
        return Threading.getFuture(() -> evalFileBackground(source, fileName, args));
    }

    Object evalLineBackground(final CommandSource source, final String line) throws ScriptException, IOException, ContextStateException, InterruptedException {
        assertInterpreterEnabled();
        assertConsoleCommandAllowed(source);

        Object result = null;
        commandManager.initCommandInfo(new CommandInfo(source, null, line, null, true));
        try {
            createExecutionContext();
            InterpreterResult ir = scriptManager.evalBackground(line);
            if (ir == null) {
                return null;
            }
            if (ir.exception != null) {
                result = ir.exception;
                throw ir.exception;
            }
            result = ir.result;
            return result;
        } finally {
            disposeExecutionContext();
            commandManager.finishCommandInfo(result);
        }
    }

    @Hidden
    public void createExecutionContext() {
        synchronized (executionPars) {
            executionPars.put(Thread.currentThread(), new ExecutionParameters());
        }
        try {
            getExecutionPars().onExecutionStarted();
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }

    }

    @Hidden
    public void disposeExecutionContext() {
        String then = getThen(getExecutionPars().getCommand());
        CommandSource source = getExecutionPars().getSource();
        try {
            getExecutionPars().onExecutionEnded();
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
        synchronized (executionPars) {
            executionPars.remove(Thread.currentThread());
        }
        if (then != null) {
            onCommand(Command.then, new Object[]{then}, source);
            new Thread(() -> {
                try {
                    evalLineBackground(source, then);
                } catch (Exception ex) {
                    Logger.getLogger(Context.class.getName()).log(Level.SEVERE, null, ex);
                }
            }, "Background command continuation task").start();
            return;
        }        
    }

    @Hidden
    public void startedChildThread(Thread parent) {
        getExecutionPars(parent).onStartChildThread();
    }

    @Hidden
    public void finishedChildThread(Thread parent) {
        getExecutionPars(parent).onFinishedChildThread();
    }

    CompletableFuture<?> evalLineBackgroundAsync(final CommandSource source, final String line) {
        return Threading.getFuture(() -> evalLineBackground(source, line));
    }

    String runningScript;

    public String getRunningScriptName() {
        return getRunningScriptName(runningScript);
    }

    public File getRunningScriptFile() {
        return getScriptFile(runningScript);
    }

    String getRunningScriptName(String script) {
        return (script == null) ? null : IO.getPrefix(script);
    }

    public File getScriptFile(String script) {
        if ((script != null)  && (scriptManager!=null)){
            if (scriptManager.getLibrary()!=null){
                script = scriptManager.getLibrary().resolveFile(script);
                if (script != null) {
                    File ret = new File(script);
                    if (ret.exists()) {
                        try {
                            ret = ret.getCanonicalFile();
                        } catch (Exception ex) {
                        }
                        return ret;
                    }
                }
            }
        }
        return null;
    }

    public ExecutionParameters getExecutionPars() {
        return getExecutionPars(Thread.currentThread());
    }

    ExecutionParameters getExecutionPars(Thread thread) {
        synchronized (executionPars) {
            if (executionPars.containsKey(thread)) {
                return executionPars.get(thread);
            }
            for (Thread t : executionPars.keySet()) {
                ExecutionParameters ep = executionPars.get(t);
                if (ep.childThreadCommandOptions.containsKey(thread)) {
                    return ep;
                }
            }
            return executionPars.get(null);
        }
    }

    public void setExecutionPars(String name) {
        Map pars = new HashMap();
        pars.put("name", name);
        setExecutionPars(pars);
    }

    public void setExecutionPars(Map pars) {
        getExecutionPars().setScriptOptions(pars);
    }

    public void setCommandPars(Object command, Map pars) {
        getExecutionPars().setCommandOptions(command, pars);
    }

    String scanTag;

    public String getScanTag() {
        return (scanTag == null) ? "scan {index}%d" : scanTag;
    }

    //Sets default scan tags for layout classes
    public void setScanTag(String value) {
        scanTag = value;
    }

    public String getStandardScriptName(String fileName) {
        if (fileName == null) {
            return "Unknown";
        }
        if (IO.isSubPath(fileName, setup.getScriptPath())) {
            return IO.getRelativePath(fileName, setup.getScriptPath());
        }
        return fileName;
    }

    Map<String, Object> parseArgs(Object args) {
        Map ret = new HashMap<>();
        if (args != null) {
            if (args instanceof Map) {
                return (Map) args;
            } else {
                if (args.getClass().isArray()) {
                    args = Arrays.asList(args);
                }
                if (args instanceof List) {
                    ret.put("args", args);
                }
            }
        }
        return ret;
    }

    Object evalFile(final CommandSource source, final String fileName, final Object args) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalFile(source, fileName, args, true);
    }

    Object evalFile(final CommandSource source, final String fileName, final Object args, final boolean batch) throws ScriptException, IOException, ContextStateException, InterruptedException {
        assertInterpreterEnabled();
        if (fileName == null) {
            return null;
        }
        final String scriptName = getStandardScriptName(fileName);
        Object result = null;
        final Map<String, Object> argsDict = parseArgs(args);
        ArrayList pars = new ArrayList();
        pars.add(scriptName);
        for (String key : argsDict.keySet()) {
            pars.add(key + "=" + argsDict.get(key));
        }
        onCommand(Command.run, pars.toArray(), source);
        triggerShellCommand(source, "Run: " + scriptName + ((args == null) ? "" : "(" + Str.toString(args, 20) + ")"));

        try {
            CommandInfo info = new CommandInfo(source, fileName, null, args, false);
            startExecution(source, fileName, info);
            setSourceUI(source);
            triggerExecutingFile(fileName);
            try {
                result = runInInterpreterThread((Callable) () -> {
                    Object ret = null;
                    for (String key : argsDict.keySet()) {
                        scriptManager.setVar(key, argsDict.get(key));
                    }
                    if (batch) {
                        ret = scriptManager.evalFile(fileName);
                    } else {
                        int exceptionLine = -1;

                        final Statement[] statements = parseFile(fileName);
                        ret = scriptManager.eval(statements);
                    }
                    triggerShellResult(source, ret);
                    return ret;
                });  //Command source in statements execution will be CommandSource.script 
                return result;
            } catch (Throwable t) {
                result = t;
                throw t;
            } finally {
                endExecution(info);
                triggerExecutedFile(fileName, result);
            }
        } catch (Throwable t) {
            triggerShellResult(source, t);
            throw t;
        }
    }

    Object evalStatement(final CommandSource source, final Statement statement) throws ScriptException, IOException, ContextStateException, InterruptedException {
        if (statement == null) {
            return null;
        }
        return evalStatements(source, new Statement[]{statement}, false, null);
    }

    Object evalStatements(final CommandSource source, final Statement[] statements, final boolean pauseOnStart, final Object args) throws ScriptException, IOException, ContextStateException, InterruptedException {
        String fileName = null;
        if ((statements != null) && (statements.length > 0)) {
            fileName = statements[0].fileName;
        }
        return evalStatements(source, statements, pauseOnStart, fileName, args);
    }

    Object evalStatements(final CommandSource source, final Statement[] statements, final boolean pauseOnStart, final String fileName, final Object args) throws ScriptException, IOException, ContextStateException, InterruptedException {
        assertInterpreterEnabled();
        if (statements == null) {
            return null;
        }
        final String scriptName = getStandardScriptName(fileName);
        Object result = null;

        onCommand(Command.debug, new Object[]{scriptName}, source);
        triggerShellCommand(source, "Debug: " + scriptName + ((args == null) ? "" : "(" + Str.toString(args, 20) + ")"));

        try {
            CommandInfo info = new CommandInfo(source, scriptName, null, args, false);
            startExecution(source, fileName, info);
            setSourceUI(source);
            triggerExecutingFile(fileName);

            if (pauseOnStart) {
                onCommand(Command.pause, null, source);
                scriptManager.pauseStatementListExecution();
                setState(State.Paused);
            }

            final StatementsEvalListener listener = new StatementsEvalListener() {
                @Override
                public void onNewStatement(Statement statement) {
                    triggerNewStatement(statement);
                }

                @Override
                public void onStartingStatement(Statement statement) {
                    executingStatement = true;
                    triggerExecutingStatement(statement);
                }

                @Override
                public void onFinishedStatement(Statement statement, InterpreterResult result) {
                    executingStatement = false;
                    triggerExecutedStatement(statement);
                }

            };

            try {
                result = runInInterpreterThread((Callable) () -> {
                    Object ret = scriptManager.eval(statements, listener);
                    triggerShellResult(source, ret);
                    return ret;
                });
                return result;
            } catch (Throwable ex) {
                result = ex;
                throw ex;
            } finally {
                endExecution(info);
                triggerExecutedFile(fileName, result);
            }
        } catch (Throwable t) {
            triggerShellResult(source, t);
            throw t;
        }
    }

    public Object getLastEvalResult() {
        return (scriptManager == null) ? null : scriptManager.getLastResult();
    }

    public Object getInterpreterVariable(String name) {
        return (scriptManager == null) ? null : scriptManager.getVar(name);
    }

    public void setInterpreterVariable(String name, Object value) {
        if (scriptManager != null) {
            scriptManager.setVar(name, value);
        }
    }

    void injectVars(final CommandSource source) {
        onCommand(Command.injectVars, null, source);
        try {
            runInInterpreterThread(new Callable() {
                @Override
                public Object call() throws Exception {
                    scriptManager.injectVars();
                    return null;
                }
            });

        } catch (Exception ex) {
            Logger.getLogger(Context.class
                    .getName()).log(Level.WARNING, null, ex);
        }
    }

    boolean aborted = false;

    void abort(final CommandSource source) throws InterruptedException {
        onCommand(Command.abort, null, source);
        if ((getState() == State.Initializing)) {
            try {
                if (restartThread != null) {
                    restartThread.interrupt();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        } else if ((getState() == State.Busy) || (getState() == State.Paused)) {
            aborted = true;
            CommandInfo cmd = commandManager.getInterpreterThreadCommand();
            if (cmd != null) {
                cmd.aborted = true;
            }
            //TODO: This is also killing background acans. Should not be only foreground?
            for (Scan scan : getRunningScans()) {
                scan.abort();
            }
            synchronized (runningScans) {
                runningScans.clear();
            }
            if (scriptManager != null) {
                scriptManager.abort();
            }
        }

        if (scriptManager != null) {
            scriptManager.resetInterpreter();
        }
    }

    public boolean isAborted() {
        return aborted;
    }

    void pause(final CommandSource source) {
        onCommand(Command.pause, null, source);
        if (canPause()) {
            try {
                scriptManager.pauseStatementListExecution();
                setState(State.Paused);
            } catch (ContextStateException ex) {
            }
        }
    }

    void resume(final CommandSource source) {
        onCommand(Command.resume, null, source);
        if (isRunningStatements()) {
            if (scriptManager.isStatementListExecutionPaused()) {
                try {
                    setState(State.Busy);
                    scriptManager.resumeStatementListExecution();
                } catch (ContextStateException ex) {
                }
            }
        }
    }

    void step(final CommandSource source) {
        onCommand(Command.step, null, source);
        if (isRunningStatements()) {
            if (scriptManager.isStatementListExecutionPaused()) {
                scriptManager.stepStatementListExecution();
            }
        }
    }

    @Hidden
    public boolean canPause() {
        if (isRunningStatements()) {
            if (!scriptManager.isStatementListExecutionPaused()) {
                return true;
            }
        }
        return false;
    }

    //These methods are made public in order to plugins control state
    //Start execution in interpreter thread (foreground task)
    @Hidden
    public void startExecution(final CommandSource source, String fileName) throws ContextStateException {
        startExecution(source, fileName, null);
    }

    @Hidden
    public void startExecution(final CommandSource source, String fileName, CommandInfo info) throws ContextStateException {
        assertReady();
        commandManager.initCommandInfo(info);
        if (fileName != null) {
            assertRunAllowed(source);
            runningScript = fileName;
        } else {
            assertConsoleCommandAllowed(source);
        }
        aborted = false;
        setState(State.Busy);
        getExecutionPars().onExecutionStarted();
    }

    Object evalNextStage(CommandInfo currentInfo, final String command) throws ScriptException, IOException, ContextStateException, InterruptedException {
        onCommand(Command.then, new Object[]{command}, currentInfo.source);
        triggerShellCommand(currentInfo.source, "Then: " + command);
        CommandInfo info = new CommandInfo(currentInfo.source, null, command, null, false);
        commandManager.initCommandInfo(info);
        aborted = false;
        getExecutionPars().onExecutionStarted();
        try {
            InterpreterResult result = (InterpreterResult) runInInterpreterThread((Callable<InterpreterResult>) () -> scriptManager.eval(command));
            if (result == null) {
                return null;
            }
            if (result.exception != null) {
                throw result.exception;
            }
            triggerShellResult(info.source, result.result);
            return result.result;
        } finally {
            endExecution(info);
        }
    }

    @Hidden
    public void endExecution(CommandInfo info) throws ContextStateException {
        String then = getThen(info);
        getExecutionPars().onExecutionEnded();
        if (then != null) {
            new Thread(() -> {
                try {
                    evalNextStage(info, then);
                } catch (Exception ex) {
                    Logger.getLogger(Context.class.getName()).log(Level.SEVERE, null, ex);
                }
            }, "Foreground command continuation task").start();
            return;
        }

        if ((state == State.Busy) || (state == State.Paused)) {
            setState(State.Ready);                  //If state has changed during execution, keep it
        }
    }

    
    public String getThen(CommandInfo info) {
        ExecutionStage then = getExecutionPars().getThen();
        if (then!=null){
            boolean success = (!info.isError()) && (!info.isAborted());
            if (success && (then.onSuccess!=null)){
                return then.onSuccess;
            } if (!success && (then.onException!=null)){
                return then.onException;
            }
        }       
        return null;
    }
    
    void updateAll(final CommandSource source) {
        onCommand(Command.updateAll, null, source);
        if (getState().isInitialized()) {
            //TODO: Should update image sources
            for (GenericDevice dev : getDevicePool().getAllDevices(Device.class)) {
                if (dev.getState().isInitialized()) {
                    dev.request();
                }
            }
            new Thread(() -> {
                pluginManager.onUpdatedDevices();
            }, "Update all notification thread").start();
        }
    }

    void stopAll(final CommandSource source) {
        onCommand(Command.stopAll, null, source);
        if (getState().isInitialized()) {
            for (final GenericDevice dev : devicePool.getAllDevices()) {
                stop(null, dev);
            }
            new Thread(() -> {
                pluginManager.onStoppedDevices();
            }, "Stop all notification thread").start();
        }
    }

    void stop(final CommandSource source, final GenericDevice dev) {
        if (source != null) {
            onCommand(Command.stopDevice, new Object[]{dev.getName()}, source);
        }
        if ((dev instanceof Stoppable) && (dev.isInitialized())) {
            new Thread(() -> {
                try {
                    ((Stoppable) dev).stop();
                } catch (DeviceBase.StopNotConfiguredException ex) {
                    logger.log(Level.FINER, null, ex);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }, "Device Stopping Thread - " + dev.getName()).start();
        }
    }

    ArrayList<GenericDevice> reinit(final CommandSource source) {
        ArrayList<GenericDevice> ret = new ArrayList<>();
        onCommand(Command.reinit, new Object[0], source);
        if (getState().isInitialized()) {
            for (GenericDevice dev : getDevicePool().getAllDevices(Device.class)) {
                if (dev.getState() == State.Invalid) {
                    try {
                        dev.initialize();
                        devicePool.applyDeviceAttributes(dev);
                    } catch (Exception ex) {
                        ret.add(dev);
                    }
                }
            }
        }
        return ret;
    }

    void reinit(final CommandSource source, GenericDevice device) throws IOException, InterruptedException {
        ArrayList<GenericDevice> ret = new ArrayList<>();
        onCommand(Command.reinit, new Object[]{device}, source);
        if (getState().isInitialized()) {
            device.initialize();
            devicePool.applyDeviceAttributes(device);
        }
    }

    //Versioning
    public boolean isVersioningEnabled() {
        return (config.versionTrackingEnabled && (!isLocalMode()));
    }

    @Hidden
    public void assertVersioningEnabled(CommandSource source) throws IOException {
        if (!getConfig().versionTrackingEnabled) {
            throw new IOException("Versioning not enabled");
        }
        if (isLocalMode()) {
            throw new IOException("Versioning disabled in local mode");
        }
        if (versioningManager == null) {
            throw new IOException("Versioning manager not instantiated");
        }
        getRights(source).assertVersioningAllowed();
    }

    @Hidden
    public void assertRemoteRepoEnabled(CommandSource source) throws IOException {
        assertVersioningEnabled(source);
        if ((getConfig().versionTrackingRemote == null)
                || (getConfig().versionTrackingRemote.length() == 0)) {
            throw new IOException("Remote repository not enabled");
        }
    }

    public void commit(final CommandSource source, String message, boolean force) throws IOException {
        onCommand(Command.commit, new Object[]{message}, source);
        assertVersioningEnabled(source);
        if (!force) {
            int diffs = getVersioningManager().diff().size();
            if (diffs == 0) {
                throw new IOException("Nothing to commit");
            }
        }
        getVersioningManager().addHomeFolders();
        getVersioningManager().commitAll(message);
    }

    public List diff() throws IOException {
        assertVersioningEnabled(getPublicCommandSource());
        return getVersioningManager().diff();
    }

    void checkoutTag(final CommandSource source, String tag) throws IOException, InterruptedException, ContextStateException {
        onCommand(Command.checkoutTag, new Object[]{tag}, source);
        assertVersioningEnabled(source);
        try {
            getVersioningManager().checkoutTag(tag);
        } catch (IOException | InterruptedException | ContextStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
        triggerBranchChange(VersioningManager.LOCAL_TAGS_PREFIX + tag);
    }

    void checkoutLocalBranch(final CommandSource source, String branch) throws IOException, InterruptedException, ContextStateException {
        onCommand(Command.checkoutBranch, new Object[]{branch}, source);
        assertVersioningEnabled(source);
        try {
            getVersioningManager().checkoutLocalBranch(branch);
        } catch (IOException | InterruptedException | ContextStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
        triggerBranchChange(branch);
    }

    void checkoutRemoteBranch(final CommandSource source, String branch) throws IOException, InterruptedException, ContextStateException {
        onCommand(Command.checkoutRemote, new Object[]{branch}, source);
        assertVersioningEnabled(source);
        try {
            getVersioningManager().checkoutRemoteBranch(branch);
        } catch (IOException | InterruptedException | ContextStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
        triggerBranchChange(branch);
    }

    void cleanupRepository(final CommandSource source) throws IOException, InterruptedException, ContextStateException {
        assertVersioningEnabled(source);
        onCommand(Command.cleanupRepository, new Object[]{}, source);
        try {
            getVersioningManager().cleanupRepository();
        } catch (IOException | InterruptedException | ContextStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    void pullFromUpstream(final CommandSource source) throws IOException, InterruptedException, ContextStateException {
        onCommand(Command.pull, new Object[]{}, source);
        assertRemoteRepoEnabled(source);
        try {
            getVersioningManager().pullFromUpstream();
            triggerBranchChange(getVersioningManager().getCurrentBranch());
        } catch (IOException | InterruptedException | ContextStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    void pushToUpstream(final CommandSource source, boolean allBranches, boolean force) throws IOException, InterruptedException, ContextStateException {
        onCommand(Command.push, new Object[]{allBranches, force}, source);
        assertRemoteRepoEnabled(source);
        try {
            getVersioningManager().pushToUpstream(allBranches, force);
        } catch (IOException | InterruptedException | ContextStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    public Revision getFileRevision(String fileName) throws IOException, InterruptedException, ContextStateException {
        assertVersioningEnabled();
        Revision ret = null;
        fileName = setup.expandPath(fileName);
        if (IO.isSubPath(fileName, setup.getHomePath())) {
            fileName = IO.getRelativePath(fileName, setup.getHomePath());
            try {
                ret = getVersioningManager().getRevision(fileName);
                if (getVersioningManager().getDiff(fileName).length() > 0) {
                    ret.id += " *";
                }
            } catch (IOException | InterruptedException | ContextStateException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
        return ret;
    }

    public String getFileContents(String fileName, String revisionId) throws IOException, InterruptedException, ContextStateException {
        assertVersioningEnabled();
        fileName = setup.expandPath(fileName);
        if (IO.isSubPath(fileName, setup.getHomePath())) {
            fileName = IO.getRelativePath(fileName, setup.getHomePath());
            try {
                return getVersioningManager().fetch(fileName, revisionId);
            } catch (IOException | InterruptedException | ContextStateException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
        return null;
    }

    //User & Rights
    public User getUser(CommandSource source) {
        return usersManager.getCurrentUser(source.isRemote());
    }

    public AccessLevel getLevel(CommandSource source) {
        return usersManager.getCurrentLevel(source.isRemote());
    }

    public Rights getRights(CommandSource source) {
        return usersManager.getCurrentRights(source.isRemote());
    }

    //UI aceess functions
    volatile UserInterface localUserInterface;
    volatile UserInterface remoteUserInterface;
    volatile UserInterface currentUserInterface;

    @Hidden
    public void setLocalUserInterface(UserInterface ui) {
        localUserInterface = ui;
        currentUserInterface = ui;
    }

    void assertDefinedUI(UserInterface ui) {
        if (ui == null) {
            throw new java.lang.UnsupportedOperationException();
        }
    }

    public String getString(String message, String defaultValue, String[] alternatives) throws InterruptedException {
        UserInterface ui = getUI();
        assertDefinedUI(ui);
        if (alternatives != null) {
            return ui.getString(message, defaultValue, alternatives);
        } else {
            return ui.getString(message, defaultValue);
        }
    }

    public String getPassword(String message, String title) throws InterruptedException {
        UserInterface ui = getUI();
        assertDefinedUI(ui);
        return ui.getPassword(message, title);
    }

    public String getOption(String message, String type) throws InterruptedException {
        UserInterface ui = getUI();
        assertDefinedUI(ui);
        return ui.getOption(message, type);
    }

    public void showMessage(String message, String title, boolean blocking) throws InterruptedException {
        UserInterface ui = getUI();
        assertDefinedUI(ui);
        ui.showMessage(message, title, blocking);
    }

    public Object showPanel(GenericDevice dev) throws InterruptedException {
        UserInterface ui = getUI();
        assertDefinedUI(ui);
        return ui.showPanel(dev);
    }

    UserInterface getUI() {
        return currentUserInterface;
    }

    @Hidden
    public void setSourceUI(CommandSource source) {
        currentUserInterface = source.isRemote() ? remoteUserInterface : localUserInterface;
    }

    //Plot access
    PlotListener plotListener;
    //TODO: the plotting model is messy, working on Swing components. 
    //Should work on abstracts & work on multiple clients (including markers, plot manipuilation, ...)
    PlotListener serverPlotListener;

    @Hidden
    public void setPlotListener(PlotListener listener) {
        plotListener = listener;
    }

    @Hidden
    public List plot(PlotDescriptor plots[], String title) throws Exception {
        if (plots == null) {
            throw new IllegalArgumentException();
        }
        try {
            if (serverPlotListener != null) {
                try {
                    serverPlotListener.plot(title, plots);
                } catch (Exception ex) {
                    logger.log(Level.FINE, null, ex);
                }
            }
            if (plotListener != null) {
                return plotListener.plot(title, plots);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
            throw ex;
        }
        return null;
    }

    @Hidden
    public List plot(PlotDescriptor plot, String title) throws Exception {
        return plot(new PlotDescriptor[]{plot}, title);
    }

    //TODO: manage 2d & 3d scan plots
    @Hidden
    public List plot(ScanResult sr, String title) throws Exception {
        ArrayList<PlotDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < sr.getReadables().size(); i++) {
            double[] x = (sr.getWritables().size() > 0) ? (double[]) Convert.toPrimitiveArray(sr.getPositions(0).toArray(new Double[0])) : null;
            List readable = sr.getReadable(i);
            int rank = (readable.size() == 0) ? 0 : Arr.getRank(readable.get(0)) + 1;
            Object data = null;
            switch (rank) {
                case 1:
                    data = (double[]) Convert.toPrimitiveArray(sr.getReadable(i).toArray(new Double[0]));
                    descriptors.add(new PlotDescriptor(sr.getReadables().get(i).getName(), data, x));
                    break;
                case 2:
                    data = (double[][]) sr.getReadable(i).toArray(new double[0][0]);
                    descriptors.add(new PlotDescriptor(sr.getReadables().get(i).getName(), data, x));
                    break;
                case 3:
                    data = (double[][][]) sr.getReadable(i).toArray(new double[0][0][0]);
                    descriptors.add(new PlotDescriptor(sr.getReadables().get(i).getName(), data, null, null, x));
                    break;
            }
        }
        return plot(descriptors.toArray(new PlotDescriptor[0]), title);
    }

    @Hidden
    public List plot(Table table, Object xdata, int[] columns, String title) throws Exception {
        table.assertDefined();
        int indexX = -1;
        if (xdata != null) {
            if (xdata instanceof String) {
                xdata = table.getColIndex((String) xdata);
            }
            if (xdata instanceof Integer) {
                indexX = (Integer) xdata;
                xdata = table.getCol(indexX);
            }
        }
        int cols = (columns == null) ? table.getCols() : columns.length;
        PlotDescriptor[] descriptors = new PlotDescriptor[cols];
        for (int i = 0; i < descriptors.length; i++) {
            int index = (columns == null) ? i : columns[i];
            descriptors[i] = new PlotDescriptor(table.getHeader()[index], table.getCol(index), (double[]) xdata);
        }
        if ((columns == null) && (indexX >= 0)) {
            descriptors = Arr.remove(descriptors, indexX);
        }
        return plot(descriptors, title);
    }

    @Hidden
    public List plot(Object data, String title) throws Exception {
        if (data instanceof List) {
            data = ((List) data).toArray();
        }
        if ((data == null) || (!data.getClass().isArray())) {
            throw new IllegalArgumentException();
        }
        if (plotListener != null) {
            return plot(new PlotDescriptor(data), title);
        } else {  //Alternative way to plot data, if has no plotListener 
            PlotScan scan = new PlotScan(data);
            scan.setPlotTitle(title);
            scan.start();
            return null;
        }
    }

    @Hidden
    public List getPlots(String title) {
        if (plotListener != null) {
            return plotListener.getPlots(title);
        }
        return new ArrayList();
    }

    //Public properties
    @Hidden
    public boolean canStep() {
        if (isRunningStatements()) {
            if (scriptManager.isStatementListExecutionPaused()) {
                if (!isExecutingStatement()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Hidden
    public boolean isPaused() {
        if (isRunningStatements()) {
            if (scriptManager.isStatementListExecutionPaused()) {
                return true;
            }
        }
        return false;
    }

    @Hidden
    public boolean isRunningStatements() {
        if ((scriptManager != null) && (getState() == State.Busy) || (getState() == State.Paused)) {
            if (scriptManager.isRunningStatementList()) {
                return true;
            }
        }
        return false;
    }

    boolean executingStatement;

    @Hidden
    boolean isExecutingStatement() {
        return executingStatement;
    }

    @Hidden
    public Statement getRunningStatement() {
        if (!isRunningStatements()) {
            return null;
        }
        return scriptManager.getRunningStatement();
    }

    //File parsing
    @Hidden
    public Statement[] parseFile(String fileName) throws ScriptException, IOException {
        assertInterpreterEnabled();
        return scriptManager.parse(fileName);
    }

    @Hidden
    public Statement[] parseString(String script, String name) throws ScriptException, IOException {
        assertInterpreterEnabled();
        return scriptManager.parse(script, name);
    }

    @Hidden
    public String getCursor() {
        return getCursor(null);
    }

    @Hidden
    public String getCursor(String command) {
        if ((command != null) && (ControlCommand.match(command))) {
            return "[CTR] ";
        }
        if (scriptManager == null) {
            return "[" + getState() + "] ";
        }

        String cursor = "[" + scriptManager.getLineNumber() + "]";
        if (scriptManager.getStatementLineCount() > 0) {
            cursor += "...";
        } else {
            cursor += "   ";
        }
        return cursor;

    }

    String processControlCommand(final CommandSource source, final ControlCommand command, final String parameters) throws ScriptException, IOException, ContextStateException, InterruptedException {

        final String[] args = parameters.isEmpty() ? new String[0] : parameters.split(" ");

        StringBuilder sb = new StringBuilder();
        switch (command) {
            case abort:
                abort(source);
                return null;
            case restart:
                assertNotRunning();
                restart(source);
                return null;
            case shutdown:
                assertNotRunning();
                shutdown(source);
                return null;
            case reload:
                assertNotRunning();
                reloadPlugins(source);
                Plugin[] plugins = getPlugins();
                if (plugins.length == 0) {
                    sb.append("No plugin loaded");
                } else {
                    sb.append("Loaded plugins:\n");
                    for (Plugin plugin : plugins) {
                        sb.append("\t").append(plugin.getPluginName()).append("\n");
                    }
                }
                break;
            case login:
                try {
                    if (args.length == 1) {
                        usersManager.selectUser(usersManager.getUser(args[0]), source);
                    }
                } catch (Exception ex) {
                    sb.append(ex.getMessage());
                }
                break;
            case history:
                if (args.length > 0) {
                    for (String s : searchHistory(args[0])) {
                        sb.append(s).append("\n");
                    }
                } else {
                    for (String s : getHistory()) {
                        sb.append(s).append("\n");
                    }
                }
                break;
            case evalb:
                if (args.length > 0) {
                    Object ret = evalLineBackground(source, parameters);
                    if (ret != null) {
                        sb.append(ret);
                    }
                }
                break;
            case devices:
                for (GenericDevice dev : devicePool.getAllDevices()) {
                    sb.append(dev.getName()).append("\n");
                }
                break;
            case run:
                assertReady();
                try {
                    evalFile(source, Paths.get(setup.getScriptPath(), args[0]).toString(), (args.length > 1) ? Arr.remove(args, 0) : null);
                } catch (Exception ex) {
                }
                return null;
            case inject:
                injectVars();
                break;
            case tasks:
                for (Task task : taskManager.getAll()) {
                    sb.append(task.getScript()).append(" ");
                    sb.append(task.getInterval()).append(" ");
                    sb.append(task.isStarted() ? "started" : "stopped").append("\n");
                }
                break;
            case users:
                for (User user : usersManager.getUsers()) {
                    sb.append(user.toString()).append("\n");
                }
                sb.append("\nCurrent: " + getUser().name);
                break;
        }
        return sb.toString();
    }

    @Hidden
    public void scriptingLog(String msg) {
        logger.warning(msg);
    }

    @Hidden
    public String[] getBuiltinFunctionsNames() throws ScriptException, IOException, ContextStateException, InterruptedException {
        Object obj = evalLineBackground("_getBuiltinFunctionNames()");
        Object[] array = null;
        if (obj instanceof List) {
            array = ((List) obj).toArray();
        } else if (obj instanceof Map) {
            array = ((Map) obj).values().toArray();
        } else {
            array = (Object[]) obj;
        }
        return (array == null) ? null : Convert.toStringArray(array);
    }

    @Hidden
    public String getBuiltinFunctionDoc(String name) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return (String) evalLineBackground("_getFunctionDoc(" + String.valueOf(name) + ")");
    }

    //Mailing
    public void notify(String subject, String text, List<String> attachments) throws IOException {
        notify(subject, text, attachments, null);
    }

    public void notify(String subject, String text, List<String> attachments, List<String> to) throws IOException {
        File[] att = null;
        if (attachments != null) {
            ArrayList<File> files = new ArrayList();
            for (String attachment : attachments) {
                files.add(new File(getSetup().expandPath(attachment)));
            }
            att = files.toArray(new File[0]);
        }

        if (to == null) {
            notificationManager.send(subject, text, att);
        } else {
            notificationManager.send(subject, text, att, to.toArray(new String[0]));
        }
    }

    //History
    void clearHistory(CommandSource source) {
        onCommand(Command.clearHistory, null, source);
        history.clear();
    }

    public List<String> getHistory() {
        return history.get();
    }

    public List<String> searchHistory(String text) {
        return history.search(text);
    }

    //Settings (persisted script properties)
    public String getSettingsFile() {
        return setup.expandPath("{context}/Settings.properties");
    }

    public void setSetting(String name, Object value) throws IOException {
        Properties properties = new SortedProperties();
        try (FileInputStream in = new FileInputStream(setup.getSettingsFile())) {
            properties.load(in);
        } catch (FileNotFoundException ex) {
        }
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, String.valueOf(value));
        }
        try (FileOutputStream out = new FileOutputStream(setup.getSettingsFile())) {
            properties.store(out, null);
        }
    }

    public String getSetting(String name) throws IOException {
        return getSettings().get(name);
    }

    public Map<String, String> getSettings() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(setup.getSettingsFile())) {
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

    //Configuration
    public Setup getSetup() {
        return setup;
    }

    public Configuration getConfig() {
        return config;
    }

    void saveConfiguration(CommandSource source) throws IOException, InstantiationException, IllegalAccessException {
        onCommand(Command.saveConfig, null, source);
        try {
            getRights(source).assertConfigAllowed();
            config.save();
            triggerConfigurationChange(this);
        } catch (UserAccessException ex) { //If no rights
            config.restore();   //If no rights restore previous saved state.
            throw ex;
        }
    }

    void saveDeviceConfiguration(CommandSource source, GenericDevice device) {
        onCommand(Command.saveDeviceConfig, new Object[]{device.getName()}, source);
        try {
            device.getConfig().save();
            triggerConfigurationChange(device);
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    /**
     * Hints to graphical layer
     */
    void setPreference(CommandSource source, ViewPreference preference, Object value) {
        onCommand(Command.setPreference, new Object[]{preference, value}, source);
        getExecutionPars().setPlotPreference(preference, value);
    }

    public PlotPreferences getPlotPreferences() {
        return getExecutionPars().getPlotPreferences();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    //Logging
    void setLogLevel(CommandSource source, Level level) {
        onCommand(Command.setLogLevel, null, source);
        getRights(source).assertPrefsAllowed();
        if (logManager != null) {
            logManager.setLevel(level);
        }
    }

    public Level getLogLevel() {

        return logManager == null ? null : logManager.getLevel();
    }

    //Scans
    final List<ScanListener> scanListeners = new ArrayList<>();

    @Hidden
    public final void addScanListener(ScanListener listener) {
        synchronized (scanListeners) {
            if (!scanListeners.contains(listener)) {
                scanListeners.add(listener);
            }
        }
    }

    @Hidden
    public final void removeScanListener(ScanListener listener) {
        synchronized (scanListeners) {
            scanListeners.remove(listener);
        }
    }

    @Hidden
    public final List<ScanListener> getScanListeners() {
        synchronized (scanListeners) {
            List<ScanListener> ret = new ArrayList<>();
            ret.addAll(scanListeners);
            return ret;
        }
    }

    ScanListener scanListener = new ScanListener() {
        @Override
        public void onScanStarted(Scan scan, String plotTitle) {
            if (!scan.isHidden()) {
                synchronized (runningScans) {
                    runningScans.add(scan);
                }
                getExecutionPars(scan.getThread()).onScanStarted(scan);
            }
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
        }

        @Override

        public void onScanEnded(Scan scan, Exception ex) {
            if (!scan.isHidden()) {
                synchronized (runningScans) {
                    runningScans.remove(scan);
                }
                getExecutionPars(scan.getThread()).onScanEnded(scan);
            }
        }
    };

    static final ArrayList<Scan> runningScans = new ArrayList<>();

    public Scan[] getRunningScans() {
        synchronized (runningScans) {
            //Remove scan not properly shut down by calling triggerEnded
            for (ScanBase scan : (ArrayList<ScanBase>) runningScans.clone()) {
                if (scan.isCompleted()) {
                    runningScans.remove(scan);
                }
            }
            return runningScans.toArray(new ScanBase[0]);
        }
    }

    @Hidden
    public void sendEventScanStreamer(String type, Object data) {
        if (config.scanStreamerPort > 0) {
            scanStreamer.sendEvent(type, data);
        }
    }

    Thread restartThread;
    RandomAccessFile lockFile;

    void exit(Throwable ex) {
        exit(ex.getMessage());
    }

    void exit(String msg) {
        logger.severe(msg);
        System.err.println(msg);
        if (System.console() == null) {
            if (localUserInterface != null) {
                try {
                    localUserInterface.showMessage(msg, "Error", true);
                } catch (InterruptedException ex) {
                }
            }
        }
        System.exit(0);
    }

    protected void onCreation() {
        if (isLocalMode()) {
            logger.warning("Local mode");
        } else {
            if (fileLockEnabled) {
                if (lockFile == null) {
                    Path lockFilePath = Paths.get(setup.getContextPath(), "Lock.dat");
                    try {
                        lockFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
                        FileLock lock = lockFile.getChannel().tryLock();
                        if (lock == null) {
                            throw new Exception("Cannot have access to lock file");
                        }
                        lockFile.setLength(0);
                        lockFile.write(Sys.getProcessName().getBytes());
                        lock.release();
                        lock = lockFile.getChannel().tryLock(0, Long.MAX_VALUE, true); //So other process can read active process
                    } catch (Exception ex) {
                        String message = "Another instance of this application is running";
                        String processName = "";
                        try {
                            processName = new String(Files.readAllBytes(lockFilePath)).trim();
                            if (!processName.isEmpty()) {
                                message += " on: " + processName;
                            }
                        } catch (Exception e) {
                        }
                        message += ".\nApplication can be started in local mode (-l option)";
                        exit(message);
                    }
                }
            } else {
                //Only exclusive execution on same pc, if terminal server is activated
                if (config.terminalPort > 0) {
                    try (ServerSocket socket = new ServerSocket(config.terminalPort)) {
                    } catch (IOException ex) {
                        exit("Another instance of this application is running.\nApplication can be started in local mode (-l option)");
                    }
                }
            }
            //TODO: Clear trash left by native libraries in temp folder (Windows only)
            if (Sys.getOSFamily() == OSFamily.Windows) {
                for (String pattern : new String[]{"jansi*.*", "jffi*.*", "jhdf*.so", "nativedata*.so", "liblz4*.so", "libbitshuffle*.dll", "BridJExtractedLibraries*"}) {
                    try {
                        for (File f : IO.listFiles(Sys.getTempFolder(), pattern)) {
                            try {
                                if (f.isDirectory()) {
                                    IO.deleteRecursive(f.getAbsolutePath());
                                    logger.log(Level.FINER, "Deleted temp folder: " + f.getName());
                                } else {
                                    f.delete();
                                    logger.log(Level.FINER, "Deleted temp file: " + f.getName());
                                }
                            } catch (Exception ex) {
                                logger.log(Level.FINE, "Cannot delete temp " + ((f.isDirectory()) ? "folder" : "file") + ": " + f.getName());
                            }
                        }
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }
            }

            //Extract script path if not present - if not in local mode
            //TODO: Does not work with a Linux hard link
            String jar = setup.getJarFile();
            if (jar != null) {              //If not in IDE
                File jarFile = new File(jar);
                File startupScript = new File(setup.getDefaultStartupScript());

                boolean defaultLibPathConfig = (startupScript != null) && (IO.isSubPath(startupScript.getParent(), setup.getScriptPath())) //&& (IO.isSubPath(setup.getScriptPath(), setup.getHomePath()))
                        ;
                //Only extracts binary files if startup script is inside script path
                if (defaultLibPathConfig) {
                    //Only extracts the full library contents in the first run
                    if ((new File(setup.getScriptPath())).listFiles().length == 0) {
                        logger.warning("Extracting script library folder");
                        try {
                            IO.extractZipFile(jarFile, setup.getHomePath(), "script");
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, null, ex);
                        }
                    } else {
                        if (!startupScript.exists() || (startupScript.lastModified() < jarFile.lastModified()) || forceExtract) {
                            logger.warning("Extracting startup script and utilities");
                            try {
                                logger.fine("Extracting: " + startupScript.getName());
                                IO.extractZipFileContent(jarFile, "script/Lib/" + startupScript.getName(), startupScript.getCanonicalPath());
                            } catch (Exception ex) {
                                logger.log(Level.WARNING, null, ex);
                            }

                            try {
                                for (String file : IO.getJarChildren(new JarFile(jarFile), "script/Lib/")) {
                                    String ext = IO.getExtension(file);
                                    String prefix = IO.getPrefix(file);
                                    String name = prefix + "." + ext;
                                    if (ext.equalsIgnoreCase(getScriptType().toString())) {
                                        File scriptFile = Paths.get(setup.getStandardLibraryPath(), name).toFile();
                                        if (!scriptFile.equals(startupScript)) {
                                            logger.fine("Extracting: " + name);
                                            IO.extractZipFileContent(jarFile, "script/Lib/" + name, scriptFile.getCanonicalPath());
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                logger.log(Level.WARNING, null, ex);
                            }
                        }
                    }
                }
                //Extract default www if not present           
                if (isServerEnabled()) {
                    File indexFile = new File(setup.getWwwIndexFile());
                    if (!indexFile.exists()) {
                        try {
                            logger.warning("Extracting www folder");
                            IO.extractZipFile(jarFile, new File(setup.getWwwPath()).getParent(), new File(setup.getWwwPath()).getName());
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, null, ex);
                        }
                    } else if ((indexFile.lastModified() < jarFile.lastModified())) {
                        logger.warning("Extracting index.html");
                        try {
                            IO.extractZipFileContent(jarFile, "www/index.html", indexFile.getCanonicalPath());
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, null, ex);
                        }
                    }
                }
            }
        }

        if (!isInterpreterEnabled()) {
            logger.warning("Disabled mode: interpreter is not started");
        } else if (isGenericMode()) {
            logger.warning("Generic mode: local startup script is not executed");
        }

        if (isBareMode()) {
            logger.warning("Bare mode: plugins folder is not loaded");
        }

        if (isEmptyMode()) {
            logger.warning("Empty mode: device pool is not loaded");
        }

        //Load  plugins
        if (!isBareMode()) {
            pluginManager.loadPluginFolder();
        }
        //Even if isBareMode because plugins may be loaded by command line
        pluginManager.startPlugins();
    }

    void reloadPlugins(CommandSource source) {
        onCommand(Command.stopAll, null, source);
        if (pluginManager != null) {
            if (isBareMode()) {
                for (Plugin p : pluginManager.getLoadedPlugins()) {
                    pluginManager.reloadPlugin(p);
                }
            } else {
                pluginManager.reloadPlugins();
            }
        }
    }

    public Plugin[] getPlugins() {
        if (pluginManager != null) {
            return pluginManager.getLoadedPlugins();
        }
        return new Plugin[0];
    }

    /**
     * Browse dynamically loaded classes in add ition to forName on current
     * ClassLoader
     */
    @Hidden
    public Class getClassByName(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            if (pluginManager != null) {
                Class cls = pluginManager.getDynamicClass(className);
                if (cls != null) {
                    return cls;
                }
            }
            try {
                Object cls = evalLineBackground(className);
                if ((cls != null) && (cls instanceof Class)) {
                    return (Class) cls;
                }
            } catch (Exception ex1) {
            }
            throw ex;
        }
    }

    //Public command interface    
    CommandSource getPublicCommandSource() {
        CommandInfo ret = commandManager.getCurrentCommand();
        return (ret == null) ? CommandSource.ui : ret.source;
    }

    //For scripts to check permissions when running files
    @Hidden
    public void startScriptExecution(Object args) {
        assertRunAllowed(getPublicCommandSource());
        CommandInfo info = commandManager.getCurrentCommand();
        info = new CommandInfo(CommandSource.script, (info == null) ? null : info.script, null, args, (info == null) ? false : info.background);
        commandManager.initCommandInfo(info);
    }

    @Hidden
    public void finishScriptExecution(Object result) {
        commandManager.finishCommandInfo(result);
    }

    @Hidden
    public void start() {
        if ((getState() == State.Invalid) || (getState() == State.Fault)) {
            if (getState() == State.Invalid) { //First run
                onCreation();
            }
            try {
                restart(getPublicCommandSource());
            } catch (ContextStateException ex) {
                //Does not happen
            }
        }
    }

    @Hidden
    public void disable() {
        if (getState().isNormal()) {
            logger.warning("Setting offline mode");
            for (AutoCloseable ac : new AutoCloseable[]{taskManager, scriptManager, devicePool, versioningManager}) {
                try {
                    if (ac != null) {
                        ac.close();
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
            if (interpreterExecutor != null) {
                interpreterExecutor.shutdownNow();
            }
        } else {
            logger.warning("Starting in offline mode");
            if (getState() == State.Invalid) { //First run
                onCreation();
            }
        }
        try {
            setState(State.Disabled);
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    public void restart() throws ContextStateException {
        restart(getPublicCommandSource());
    }

    public Object evalLine(String line) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalLine(getPublicCommandSource(), line);
    }

    public CompletableFuture<?> evalLineAsync(final String line) throws ContextStateException {
        return evalLineAsync(getPublicCommandSource(), line);
    }

    public Object evalLineBackground(String line) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalLineBackground(getPublicCommandSource(), line);
    }

    public CompletableFuture<?> evalLineBackgroundAsync(final String line) {
        return evalLineBackgroundAsync(getPublicCommandSource(), line);
    }

    public Object evalFile(String fileName, Object args) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalFile(getPublicCommandSource(), fileName, args);
    }

    public CompletableFuture<?> evalFileAsync(final String fileName) throws ContextStateException {
        return evalFileAsync(getPublicCommandSource(), fileName);
    }

    public CompletableFuture<?> evalFileAsync(String fileName, Object args) throws Context.ContextStateException {
        return evalFileAsync(getPublicCommandSource(), fileName, args);
    }

    public Object evalFileBackground(final String fileName) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalFileBackground(getPublicCommandSource(), fileName);
    }

    public Object evalFileBackground(final String fileName, final Object args) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalFileBackground(getPublicCommandSource(), fileName, args);
    }

    public CompletableFuture<?> evalFileBackgroundAsync(final String fileName) {
        return evalFileBackgroundAsync(getPublicCommandSource(), fileName);
    }

    public CompletableFuture<?> evalFileBackgroundAsync(final String fileName, final Object args) {
        return evalFileBackgroundAsync(getPublicCommandSource(), fileName, args);
    }

    public Object evalStatement(Statement statement) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalStatement(getPublicCommandSource(), statement);
    }

    public Object evalStatements(Statement[] statements, boolean pauseOnStart, String fileName, Object args) throws ScriptException, IOException, ContextStateException, InterruptedException {
        return evalStatements(getPublicCommandSource(), statements, pauseOnStart, fileName, args);
    }

    public void injectVars() {
        injectVars(getPublicCommandSource());
    }

    public void abort() throws InterruptedException {
        abort(getPublicCommandSource());
    }

    public boolean abort(long commandId) throws InterruptedException {
        return abort(getPublicCommandSource(), commandId);
    }

    public void updateAll() {
        updateAll(getPublicCommandSource());
    }

    public void stopAll() {
        stopAll(getPublicCommandSource());
    }

    public List<GenericDevice> reinit() {
        return reinit(getPublicCommandSource());
    }

    public void reinit(GenericDevice device) throws IOException, InterruptedException {
        reinit(getPublicCommandSource(), device);
    }

    public void stop(GenericDevice device) {
        stop(getPublicCommandSource(), device);
    }

    public void pause() {
        pause(getPublicCommandSource());
    }

    public void resume() {
        resume(getPublicCommandSource());
    }

    public void step() {
        step(getPublicCommandSource());
    }

    public void commit(String message, boolean force) throws IOException, InterruptedException, ContextStateException {
        commit(getPublicCommandSource(), message, force);
    }

    public void checkoutTag(String tag) throws IOException, InterruptedException, ContextStateException {
        checkoutTag(getPublicCommandSource(), tag);
    }

    public void checkoutLocalBranch(String branch) throws IOException, InterruptedException, ContextStateException {
        checkoutLocalBranch(getPublicCommandSource(), branch);
    }

    public void checkoutRemoteBranch(String branch) throws IOException, InterruptedException, ContextStateException {
        checkoutRemoteBranch(getPublicCommandSource(), branch);
    }

    public void cleanupRepository() throws IOException, InterruptedException, ContextStateException {
        cleanupRepository(getPublicCommandSource());
    }

    public void pullFromUpstream() throws IOException, InterruptedException, ContextStateException {
        pullFromUpstream(getPublicCommandSource());
    }

    public void pushToUpstream(boolean allBranches, boolean force) throws IOException, InterruptedException, ContextStateException {
        pushToUpstream(getPublicCommandSource(), allBranches, force);
    }

    public void reloadPlugins() {
        reloadPlugins(getPublicCommandSource());
    }

    public void clearHistory() {
        clearHistory(getPublicCommandSource());
    }

    public void saveConfiguration() throws IOException, InstantiationException, IllegalAccessException {
        saveConfiguration(getPublicCommandSource());
    }

    public void saveDeviceConfiguration(GenericDevice device) {
        saveDeviceConfiguration(getPublicCommandSource(), device);
    }

    public void setLogLevel(Level level) {
        setLogLevel(getPublicCommandSource(), level);
    }

    public void setPreference(ViewPreference preference, Object value) {
        setPreference(getPublicCommandSource(), preference, value);
    }

    @Hidden
    public void assertVersioningEnabled() throws IOException {
        assertVersioningEnabled(getPublicCommandSource());
    }

    @Hidden
    public void assertRemoteRepoEnabled() throws IOException {
        assertVersioningEnabled(getPublicCommandSource());
    }

    public boolean isSecurityEnabled() {
        return usersManager.isEnabled();
    }

    public User getUser() {
        return getUser(getPublicCommandSource());
    }

    public AccessLevel getLevel() {
        return getLevel(getPublicCommandSource());
    }

    public Rights getRights() {
        return getRights(getPublicCommandSource());
    }

    void assertConsoleCommandAllowed(CommandSource source) {
        if (!source.isInternal()) {
            getRights(source).assertConsoleAllowed();
        }
    }

    void assertRunAllowed(CommandSource source) {
        if (!source.isInternal()) {
            getRights(source).assertRunAllowed();
        }
    }

    void closeLockFile() {
        if (lockFile != null) {
            try {
                lockFile.close();
            } catch (Exception ex) {
                logger.severe("Error closing lock file: " + ex.toString());
            }
            lockFile = null;
        }
    }

    //Server events
    final List<EventListener> eventListeners = new ArrayList<>();

    @Hidden
    public final void addEventListener(EventListener listener) {
        synchronized (eventListeners) {
            if (!eventListeners.contains(listener)) {
                eventListeners.add(listener);
            }
        }
    }

    @Hidden
    public final void removeEventListener(EventListener listener) {
        synchronized (eventListeners) {
            eventListeners.remove(listener);
        }
    }

    @Hidden
    public final List<EventListener> getEventListeners() {
        synchronized (eventListeners) {
            List<EventListener> ret = new ArrayList<>();
            ret.addAll(eventListeners);
            return ret;
        }
    }

    public void sendEvent(String name, Object value) {
        for (EventListener listener : getEventListeners()) {
            listener.sendEvent(name, value);
        }
    }

    //Disposing
    @Override
    public void close() {
        try {
            setState(State.Closing);
        } catch (ContextStateException ex) {
            //Does not happen
        }

        //Done after setState in order the listener to receive the close event
        super.close(); //remove listeners but don't send event as in removeAllListeners();

        synchronized (scanListeners) {
            scanListeners.clear();
        }

        synchronized (eventListeners) {
            eventListeners.clear();
        }

        logger.info("Close");

        for (AutoCloseable ac : new AutoCloseable[]{scanStreamer, taskManager, scriptManager, devicePool, versioningManager, pluginManager, dataManager, usersManager, server}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        if (versioningManager != null) {
            if (!getConfig().versionTrackingManual) {
                versioningManager.startPushUpstream(true, false);  //In different process
            }
        }
        if (interpreterExecutor != null) {
            interpreterExecutor.shutdownNow();
        }

        closeLockFile();
    }
}
