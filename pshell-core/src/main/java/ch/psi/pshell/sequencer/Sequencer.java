package ch.psi.pshell.sequencer;

import ch.psi.pshell.data.DataAddress;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.data.Table;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.devices.DevicePoolListener;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Options;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.framework.Setup.LockMode;
import ch.psi.pshell.notification.Notifier.NotificationLevel;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.extension.Plugin;
import ch.psi.pshell.framework.App;
import ch.psi.pshell.scan.PlotScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanBase;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scan.ScanResult;
import ch.psi.pshell.scripting.InterpreterResult;
import ch.psi.pshell.scripting.Interpreter;
import ch.psi.pshell.scripting.Interpreter.StatementsEvalListener;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.scripting.ViewPreference.PlotPreferences;
import ch.psi.pshell.security.User;
import ch.psi.pshell.sequencer.ExecutionParameters.ExecutionStage;
import ch.psi.pshell.swing.ConfigDialog;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.UserInterface;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Condition;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.History;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.ObservableBase;
import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.State.StateException;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Threading;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * Global singleton managing local and remote execution of commands
 */
public class Sequencer extends ObservableBase<SequencerListener> implements AutoCloseable {
    
    private static Sequencer instance;
    public static Sequencer getInstance(){
        if (instance == null){
            throw new RuntimeException("Sequencer not instantiated.");
        }        
        return instance;
    }
    
    public static boolean hasInstance(){
        return instance!=null;
    }

    static final Logger logger = Logger.getLogger(Sequencer.class.getName());

    public static final int DEFAULT_COMMAND_BUS_SIZE = -1;              //Maximum bus size (-1 for illimited)
    public static final int DEFAULT_COMMAND_BUS_TIME_TO_LIVE = 600000;   //Cleanup commands older than 10 minutes 
    
    Server server;
    final History history;
    final HashMap<Thread, ExecutionParameters> executionPars = new HashMap<>();
    ExecutorService interpreterExecutor;
    Thread interpreterThread;
    Interpreter interpreter;
    TaskScheduler taskScheduler;
    ScriptStdio scriptStdio;
    TerminalServer terminalServer;
    CommandBus commandBus;
    volatile String next;
    volatile boolean aborted;
    volatile Exception foregroundException;
    volatile FilePermissions filePermissionsConfig;
    volatile boolean extractedUtilities;
    private String[] builtinFunctionsNames;
    private Map<String, String> builtinFunctionsDoc;   

    int runCount;
    
    public int getRunCount() {
        return runCount;
    }

    public static final String INTERPRETER_THREAD = "MainThread";    

    public Sequencer() {
        this(DEFAULT_COMMAND_BUS_SIZE,DEFAULT_COMMAND_BUS_TIME_TO_LIVE);
    }
    
    public Sequencer(int commandBusSize, int commandBusTimeToLive) {
        instance = this;        

        if (System.getProperty(Interpreter.PROPERTY_PYTHON_HOME) == null){
            System.setProperty(Interpreter.PROPERTY_PYTHON_HOME, Setup.getScriptsPath());
        }

        if (isLocalMode()) {
            //In local mode inherits the history, but not persist changes
            history = new History(getCommandHistoryFile(), 1000, false);
            history.restore();

        } else {
            history = new History(getCommandHistoryFile(), 1000, true);
        }

        commandBus = new CommandBus(commandBusSize, commandBusTimeToLive){
            @Override
            protected void onCommandStarted(CommandInfo info) {        
                Sequencer.this.onCommandStarted(info);
            }

            @Override
            protected void onCommandFinished(CommandInfo info) { 
                Sequencer.this.onCommandFinished(info);
            }
        };

        setStdioListener(null);

        addScanListener(scanListener);

        executionPars.put(null, new ExecutionParameters());
        builtinFunctionsNames = new String[0];
        builtinFunctionsDoc = new HashMap<>();
    }
    
    //Configuration
    public record TerminalConfig(int port) {
        public boolean isEnabled(){
            return port>0;
        }
    }    
    private TerminalConfig terminalConfig = new TerminalConfig(0); 

    public void setTerminalConfig(TerminalConfig config) {
        terminalConfig = config;
    }

    public TerminalConfig getTerminalConfig() {
        return terminalConfig;
    }
    
    public record ServerConfig(String hostname, int port, boolean https, boolean light) {
        public boolean isEnabled(){
            return port>0;
        }
        public String getHostname(){
            if ((hostname == null) || (hostname.isBlank()) || (hostname.equalsIgnoreCase("null")) || (hostname.equalsIgnoreCase("0.0.0.0"))) {
                return null;
            }
            return hostname.trim();
        }
    }

    private ServerConfig serverConfig = new ServerConfig(null, 0,false,false); 
    
    public void setServerConfig(ServerConfig config) {
        serverConfig = config;
                
        String hostname = config.getHostname();
        if (!isLocalMode() && serverConfig.isEnabled() &&  (hostname != null)){            
            if (!hostname.equals(Sys.getLocalHost())){
                try {
                    InetAddress[] addresses = InetAddress.getAllByName(Sys.getLocalHost());
                    for (InetAddress address : addresses) {
                        if (address.getHostAddress().equals(hostname)) {
                            return;
                        }
                    } 
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                exit("Application must run on host: " + hostname + " or else in local mode (-l option)");
            }                    
        }
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }
    
    final ArrayList<String> notifiedTasks = new ArrayList<>();    

    public void setNotifiedTasks(List<String> notifiedTasks){
        this.notifiedTasks.clear();
        this.notifiedTasks.addAll(notifiedTasks);
    }

    public ArrayList<String>  getNotifiedTasks(){
        return notifiedTasks;
    }
    
    private NotificationLevel notificationLevel = NotificationLevel.User;    

    public void setNotificationLevel(NotificationLevel value){
        notificationLevel = value;
    }

    public NotificationLevel getNotificationLevel(){
        return notificationLevel;
    }       
    
    private boolean saveConsoleSessions=false;    

    public void setSaveConsoleSessions(boolean value){
        saveConsoleSessions = value;
    }

    public boolean getSaveConsoleSessions(){
        return saveConsoleSessions;
    }    
        
           
    private String localStartupFilePrefix="local";    

    public void setLocalStartupFilePrefix(String value){
        localStartupFilePrefix = value;
    }

    public String getLocalStartupFilePrefix(){
        return localStartupFilePrefix;
    }        

    // Fixed configuration
    public String getCommandHistoryFile() {
        return Paths.get(Setup.getContextPath(), "CommandHistory.dat").toString();
    }
    

    volatile State state = State.Invalid;

    protected void setState(State state) throws StateException {
        if (this.state != state) {
            if ((this.state == State.Closing)
                    || //disposed is definitive
                    (this.state.isRunning() && (state == State.Busy))
                    || //One high-level action at a time -
                    ((this.state != State.Busy) && (state == State.Paused))
                    || //Can pause
                    ((this.state == State.Fault) && (state != State.Initializing)) //To quit Fault state must restart
                    ) {
                this.state.throwTransitionException(state);
            }
            State former = this.state;
            this.state = state;
            Context.setState(state);
            logger.fine("State: " + state);
            triggerStateChanged(state, former);
            if (Context.hasExtensions()) {
                Context.getExtensions().onStateChange(state, former);
            }
            if (!state.isProcessing()) {
                runningScript = null;
            }
        }
    }

    @Hidden
    public void assertReady() throws StateException {
        assertState(State.Ready);
    }

    @Hidden
    public void assertStarted() throws StateException {
        assertNotState(State.Initializing);
        state.assertActive();
    }

    @Hidden
    public void assertNotRunning() throws StateException {
        assertNotState(State.Busy);
        assertNotState(State.Paused);
    }

    @Hidden
    public void assertRunning() throws StateException {
        State state = getState();
        if ((state!=State.Busy) && (state!=State.Paused)){
            state.throwStateException();
        }
    }

    @Hidden
    public void assertState(State state) throws StateException {
        this.state.assertIs(state);
    }

    @Hidden
    public void assertNotState(State state) throws StateException {
        this.state.assertNot(state);
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

    public void waitStateNotProcessing(int timeout) throws IOException, InterruptedException {
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

    public boolean isLocalMode() {
        return Setup.isLocal();
    }

    public boolean isGenericMode() {
        return Setup.isGeneric();
    }

    public boolean isInterpreterEnabled() {
        return !Setup.isDisabled();
    }
    
    public boolean isVolatileMode() {
        return Setup.isVolatile();
    }
    
    public boolean isServerEnabled() {
        return serverConfig.isEnabled() && !isLocalMode();
    }

    public boolean isTerminalEnabled() {
        return terminalConfig.isEnabled() && !isLocalMode();
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

    public Thread getInterpreterThread() {
        return interpreterThread;
    }

    //
    public class InterpreterThreadException extends Exception {

        InterpreterThreadException() {
            super("Not in interpreter thread");
        }
    }

    void assertInterpreterThread() throws InterpreterThreadException {
        if (!isInterpreterThread()) {
            throw new InterpreterThreadException();
        }
    }

    //Event Triggering
    protected void triggerShellCommand(final CommandSource source, final String command) {
        if (command != null) {
            for (SequencerListener listener : getListeners()) {
                try {
                    listener.onShellCommand(source, command);
                } catch (Throwable ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void triggerShellResult(final CommandSource source, Object result) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onShellResult(source, result);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerShellStdout(String str) {
        if (str != null) {
            for (SequencerListener listener : getListeners()) {
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
            for (SequencerListener listener : getListeners()) {
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
            for (SequencerListener listener : getListeners()) {
                try {
                    listener.onShellStdin(str);
                } catch (Throwable ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void triggerStateChanged(final State state, final State former) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onStateChanged(state, former);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerInitialized(int runCount) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onInitialized(runCount);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerNewStatement(final Statement statement) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onNewStatement(statement);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerExecutingStatement(final Statement statement) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onExecutingStatement(statement);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerExecutedStatement(final Statement statement) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onExecutedStatement(statement);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void triggerExecutingFile(final String fileName) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onExecutingFile(fileName);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        Context.triggerStartExecution(fileName);
    }

    protected void triggerExecutedFile(final String fileName, final Object result) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onExecutedFile(fileName, result);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        if (Context.hasExtensions()) {
            Context.getExtensions().onExecutedFile(fileName, result);
        }        
                
        try {
            if (Context.isNotificationEnabled() & (getNotificationLevel() != NotificationLevel.Off)) {
                notifyExecution(fileName, result);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
        
    }
    
    protected void triggerWillEval(final CommandSource source, final String code) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.willEval(source, code);
            } catch (SecurityException ex){
                throw ex;
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onStartEval(source, code);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }        
    }

    protected void triggerWillRun(final CommandSource source, final String fileName, final Object args) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.willRun(source, fileName, args);
            } catch (SecurityException ex){
                throw ex;
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onStartRun(source, fileName, args);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }    
    
 
    protected void notifyExecution(final String fileName, final Object result) throws IOException{
        boolean notifyTask = true;
        boolean error = (result instanceof Throwable) && !aborted;
        
        List<String> tasks = getNotifiedTasks();
        if (tasks.size() > 0) {
            notifyTask = false;
            String taskName = IO.getPrefix(fileName);
            for (String task : tasks) {
                if (task.equalsIgnoreCase(taskName)) {
                    notifyTask = true;
                }
            }
        }

        if (notifyTask) {
            if (error && (getNotificationLevel() != NotificationLevel.User)) {
                    Context.notify("Execution error", "File: " + fileName + "\n" + ((Throwable) result).getMessage(), null);
            } else if (getNotificationLevel() == NotificationLevel.Completion) {
                if (aborted) {
                    Context.notify("Execution aborted", "File: " + fileName, null);
                } else {
                    Context.notify("Execution success", "File: " + fileName + "\nReturn:" + String.valueOf(result), null);
                }
            }
        }
    }

    protected void triggerPreferenceChange(final ViewPreference preference, final Object value) {
        for (SequencerListener listener : getListeners()) {
            try {
                listener.onPreferenceChange(preference, value);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    public ScriptType getScriptType() {
        return Context.getScriptType();
    }

    public static String getDefaultStartupScript() {
        String file = Context.getScriptType().getDefaultStartupFile();
        return Paths.get(Setup.getStandardLibraryPath(), file).toString();
    }                        
    
    public String getStartupScript() {        
        if (disableStartupScript){
            return null;
        }
        String file = Setup.getStartupScript();
        if (file!=null){
            return file;
        }        
        try {
            Path path = Files.createTempDirectory("pshell");
            IO.extractZipFile(new File(Setup.getJarFile()), path.toString(), "script");
            logger.log(Level.INFO, "Cannot locate startup script - using temporary directory {0}", path.toString());
            String scriptLibPath = Paths.get(path.toString(), "script", "Lib").toString();
            addLibraryPath(scriptLibPath);
            return Paths.get(scriptLibPath, Context.getScriptType().getDefaultStartupFile()).toString();
        } catch (Exception ex) {
            Logger.getLogger(Sequencer.class.getName()).log(Level.WARNING, null, ex);
        }
        throw new RuntimeException("Cannot locate startup script");
    }
    
    
    boolean saveCommandStatistics;
    
    public boolean getSaveCommandStatistics(){
        return saveCommandStatistics;
    }

    public void setSaveCommandStatistics(boolean value){
        saveCommandStatistics = value;
    }
    
    boolean scriptCallbacksEnabled=true;
    
    public boolean isScriptCallbacksEnabled(){
        return scriptCallbacksEnabled && (interpreter != null) && getState().isInitialized();
    }

    public void setScriptCallbacksEnabled(boolean value){
        scriptCallbacksEnabled = value;
    }        
    
   
    
    boolean disableStartupScript;
    boolean disableLocalStartupScript;
    
    public void disableStartupScript() {  
        disableStartupScript = true;
        setScriptCallbacksEnabled(false);
    }
    
    public boolean isStartupScriptDisabled() {  
        return disableStartupScript;
    }    
    
    public void disableLocalStartupScript() {  
        disableLocalStartupScript = true;
    }

    public boolean isLocalStartupScriptDisabled() {  
        return disableLocalStartupScript;
    }    
    
    public void disableStartupScriptsExtraction() {  
        Options.EXTRACT.setProperty("false");        
    }

    public void disableStartupScriptsExecution() {  
        disableStartupScript();
        disableLocalStartupScript();
        disableStartupScriptsExtraction();
    }
        
    
    public String getLocalStartupScript() {
        if (disableLocalStartupScript){
            return null;
        }              
        return Setup.expandPath(Setup.getLocalStartupScript());
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
                if (Setup.isGui()){
                    stdinInput.wait();
                } else {
                    try{
                        stdinInput.str =  App.getConsole().readLine();
                    } catch (IOException | RuntimeException ex) {
                        stdinInput.str = "";
                    }
                    stdinInput.waiting = false;
                }
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

    public void setStdioListener(ScriptStdioListener listener) {
        scriptStdioListener = listener;

        if (interpreter != null) {
            if (scriptStdio != null) {
                scriptStdio.close();
            }
            if (scriptStdioListener != null) {
                scriptStdio = new ScriptStdio(interpreter);
                scriptStdio.setListener(listener);
            } else {
                //If no listener then use to System stdio
                interpreter.setWriter(new PrintWriter(System.out));
                interpreter.setErrorWriter(new PrintWriter(System.err));
            }
        }
    }

    public void addScriptStdio(ScriptStdio scriptStdio) {
        if (scriptStdioListener != null) {
            scriptStdio.setListener(scriptStdioListener);
        }
    }

    //Access methods
    public CommandBus getCommandBus() {
        return commandBus;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
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
        startTask,
        stopTask,
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
        Level level = source.getLogLevel(sb.toString());                
        sb.append(" ");
        source.putLogTag(sb);
        String cmd = sb.toString();
        logger.log(level, cmd);
        //TODO: Could add security check here?
    }

    public void shutdown(final CommandSource source) {
        onCommand(Command.shutdown, null, source);
        System.exit(0);
    }

    public void restart(final CommandSource source) throws StateException {
        onCommand(Command.restart, null, source);
        boolean firstRun = (getState() == State.Invalid);
        
        if (getState() == State.Invalid) { 
            //First run
            onCreation();
        }
            
        filePermissionsConfig = Context.getConfigFilePermissions();
        Config.setDefaultPermissions(filePermissionsConfig);

        if (interpreter != null) {
            try {
                if (!interpreter.isThreaded()) {
                    if (getState().isProcessing()) {
                        abort();
                        waitStateNotProcessing(20000);
                    }
                    if (getState().isProcessing()) {
                        logger.warning("Error aborting running task in single-threaded interpreter");
                    } else {
                        evalLine(CommandSource.ctr, "on_system_restart()");
                    }
                } else {
                    evalLineBackground(CommandSource.ctr, "on_system_restart()");
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }

        setState(State.Initializing);
        aborted = false;
        foregroundException = null;

        for (AutoCloseable ac : new AutoCloseable[]{taskScheduler, interpreter}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }

        logger.info("Initializing context");
        restartThread = Thread.currentThread();
        try {
            if (!isLocalMode()) {
                if ((terminalServer != null) && ((!isTerminalEnabled()) || (terminalConfig.port() != terminalServer.getPort()))) {
                    terminalServer.close();
                    terminalServer = null;
                }
                if ((terminalServer == null) && isTerminalEnabled()) {
                    try {
                        terminalServer = new TerminalServer(terminalConfig.port);
                        logger.log(Level.INFO, "Started terminal server on port {0}", terminalConfig.port);
                    } catch (Exception ex) {
                        throw new Exception("Error initializing terminal server on port " + terminalConfig.port, ex);
                    }
                }

                if ((server != null) && ((!isServerEnabled()) || (serverConfig.port != server.getPort()))) {
                    server.close();
                    server = null;
                }
                //Must restart sw if server changes
                if ((server == null) && isServerEnabled()) {
                    try {
                        server = new Server(serverConfig.getHostname(), serverConfig.port, serverConfig.https, serverConfig.light);
                    } catch (Exception ex) {
                        throw new Exception("Error initializing server on port " + serverConfig.port, ex);
                    }
                }
            }
            //Remove script listeners
            ArrayList<SequencerListener> scriptListeners = new ArrayList();
            for (SequencerListener listener : getListeners()) {
                if (listener.getClass().getName().startsWith(Interpreter.JYTHON_OBJ_CLASS)) {
                    scriptListeners.add(listener);
                }
                //TODO: Only removing python listeners
            }
            for (SequencerListener listener : scriptListeners) {
                removeListener(listener);
            }

            if (interpreterExecutor != null) {
                interpreterExecutor.shutdownNow();
                Threading.stop(interpreterThread, true, 3000);
            }

            DevicePool.addStaticListener(new DevicePoolListener() {
                @Override
                public void onDeviceAdded(GenericDevice dev) {
                    if (interpreter != null) {
                        interpreter.addInjection(dev.getName(), dev);
                    }
                }

                @Override
                public void onDeviceRemoved(GenericDevice dev) {
                    if (interpreter != null) {
                        interpreter.removeInjection(dev.getName());
                    }
                }
            });


            final HashMap<String, Object> injections = new HashMap<>();
            if (Context.hasDevicePool()){
                for (GenericDevice dev : Context.getDevicePool().getAllDevices()) {
                    injections.put(dev.getName(), dev);
                }
            }

            injections.put("run_count", runCount);

            interpreterExecutor = Executors.newSingleThreadExecutor((Runnable runnable) -> {
                interpreterThread = new Thread(Thread.currentThread().getThreadGroup(), runnable, INTERPRETER_THREAD);
                return interpreterThread;
            });

            if (isInterpreterEnabled()) {
                runInInterpreterThread(null, (Callable<InterpreterResult>) () -> {
                    String[] libraryPath = Setup.getLibraryPath();                    
                    try {
                        interpreter = new Interpreter(getScriptType(), libraryPath, injections, Context.getScriptFilePermissions(), Setup.getNoBytecodes());
                        interpreter.setConsoleJournalFilePath((getSaveConsoleSessions() && !isLocalMode()) ? Setup.getConsolePath() : null);
                        setStdioListener(scriptStdioListener);
                        String startupScript = getStartupScript();
                        if (startupScript != null) {
                            interpreter.evalFile(startupScript);
                            logger.info("Executed startup script");
                        }
                                                
                        interpreter.injectVars(); //Do it again because builtin classes in startup script may shadow a device name.
                        if (!isGenericMode()) {
                            String localStartupScript = getLocalStartupScript();
                            if (localStartupScript!=null){
                                try {
                                    interpreter.evalFile(localStartupScript);                                    
                                    logger.info("Executed local startup script: " + interpreter.getLibrary().resolveFile(localStartupScript));
                                    interpreter.resetLineNumber(); //So first statement will be number 1
                                } catch (Exception ex) {
                                    if ((ex instanceof FileNotFoundException) && ex.getMessage().equals(localStartupScript)) {
                                        if (!Setup.isVolatile() || Setup.redefinedStartupScript()){
                                            logger.warning("Local initialization script is not present");
                                        }
                                    } else {
                                        ex.printStackTrace();
                                        logger.log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                        }
                        if (startupScript != null) {
                            Object obj = interpreter.eval("_getBuiltinFunctionNames()").result;
                            Object[] array = null;
                            if (obj instanceof List list) {
                                array = list.toArray();
                            } else if (obj instanceof Map map) {
                                array = map.values().toArray();
                            } else {
                                array = (Object[]) obj;
                            }
                            builtinFunctionsNames = (array == null) ? null : Convert.toStringArray(array);
                            if (!interpreter.isThreaded()) {
                                for (String name : builtinFunctionsNames) {
                                    String doc = String.valueOf(interpreter.eval("_getFunctionDoc(" + name + ")").result);
                                    builtinFunctionsDoc.put(name, doc);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        logger.log(Level.SEVERE, null, ex);
                        builtinFunctionsNames = new String[0];
                        builtinFunctionsDoc.clear();
                    }
                    return null;
                });
                if (interpreter == null) {
                    throw new Exception("Error instantiating script manager");
                } else {
                    if (extractedUtilities) {
                        //TODO: remove this  when Jython fixes this: https://github.com/jython/jython/issues/93                
                        if (!Setup.getNoBytecodes() && !isVolatileMode()) {
                            new Thread(() -> {
                                interpreter.fixClassFilesPermissions();
                            }).start();
                        }
                        extractedUtilities = false;
                    }
                }
            }
            //Only instantiate it if state goes ready
            taskScheduler = new TaskScheduler();
            if (!isLocalMode()) {
                taskScheduler.initialize(Setup.getTasksFile());
            }
            setState(State.Ready);
            triggerInitialized(runCount);
            runCount++;
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, null, ex);
            setState(State.Fault);
        }
    }

    public boolean abort(final CommandSource source, long commandId) throws InterruptedException {
        onCommand(Command.abort, new Object[]{commandId}, source);
        return commandBus.abort(source, commandId);
    }

    boolean join(long commandId) throws InterruptedException {
        return commandBus.join(commandId);
    }

    boolean isRunning(long commandId) {
        return commandBus.isRunning(commandId);
    }

    public Map getResult(long commandId) throws Exception {
        return commandBus.getResult(commandId);
    }

    public Map getResult() throws Exception {
        return getResult(-1);
    }
  
    public long waitAsyncCommand(CompletableFuture cf) throws InterruptedException {
        if (cf instanceof Threading.VisibleCompletableFuture vcf){
            return commandBus.waitAsyncCommand(vcf);
        }
        throw new RuntimeException ("Can only wait for async command in VisibleCompletableFuture"); 
    }


    public Object runInInterpreterThread(CommandInfo info, Callable callable) throws ScriptException, IOException, InterruptedException {
        assertInterpreterEnabled();
        Object result = null;
        foregroundException = null;
        try {
            if (isInterpreterThread()) {
                result = callable.call();
            } else {
                try {
                    synchronized (interpreterExecutor) {
                        result = interpreterExecutor.submit(callable).get();
                    }
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof ScriptException scriptException) {
                        throw scriptException;
                    }
                    if ((ex.getCause() instanceof RuntimeException runtimeException) && (getScriptType() == ScriptType.js)) {
                        //On JS exceptions are RuntimeException
                        throw new ScriptException(runtimeException);
                    }
                    if (ex.getCause() instanceof InterruptedException interruptedException) {
                        throw interruptedException;
                    }
                    if (ex.getCause() instanceof IOException ioException) {
                        throw ioException;
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
            if (result instanceof InterpreterResult interpreterResult) {
                if (((InterpreterResult) result).exception != null) {
                    result = interpreterResult.exception;
                } else if (((InterpreterResult) result).result != null) {
                    result = interpreterResult.result;
                }
            }
            if (info != null) {
                commandBus.commandFinished(info, result);
            }
            if (result instanceof Exception) {
                foregroundException = (Exception) result;
            }
        }
    }

    public Object evalLine(final CommandSource source, final String command) throws ScriptException, IOException, StateException, InterruptedException {
        assertInterpreterEnabled();
        assertStarted();
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
                        triggerWillRun(source, command.toString(), null);                        
                    } else if (controlCommand.isEval()) {
                        triggerWillEval(source, command);
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
                InterpreterResult result = (InterpreterResult) runInInterpreterThread(info, (Callable<InterpreterResult>) () -> interpreter.eval(line));
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
                    if (source.isSavable()) {
                        history.put(command);
                    }
                }
            } catch (Exception ex) {
            }
        }
    }
        
    public void evalLineAfter(final CommandSource source, final String command, boolean onSuccess, boolean onException) throws ScriptException, IOException, StateException, InterruptedException {        
        assertRunning();
        onCommand(Command.then, new Object[]{command}, source);
        if (getState().isProcessing()) {
            getExecutionParsForeground().setThen(command,  onSuccess, onException);
        }
    }

    CompletableFuture<?> getInterpreterFuture(final Threading.SupplierWithException<?> supplier) {
        return Threading.getFuture(supplier, interpreterExecutor);
    }
    
    CompletableFuture<?> getBackgroundFuture(final Threading.SupplierWithException<?> supplier) {
         //return Threading.getFuture(supplier);
         //return Threading.getPrivateThreadFuture(supplier);
         return Threading.getVolatileThreadFuture(supplier);
    }

    public CompletableFuture<?> evalLineAsync(final CommandSource source, final String line) throws StateException {
        if (ControlCommand.isBackground(line)){
            return getBackgroundFuture(() -> evalLine(source, line));
        } else {
            assertReady();  //TODO: This is not strict, state can change before the thread starts
            return getInterpreterFuture(() -> evalLine(source, line));
        }
    }

    public CompletableFuture<?> evalFileAsync(final CommandSource source, final String fileName) throws StateException {
        return evalFileAsync(source, fileName, null);
    }

    public CompletableFuture<?> evalFileAsync(final CommandSource source, final String fileName, final Object args) throws StateException {
        assertReady();  //TODO: This is not strict, state can change before the thread starts
        return getInterpreterFuture(() -> evalFile(source, fileName, args));
    }

    public Object evalFileBackground(final CommandSource source, final String fileName) throws ScriptException, IOException, StateException, InterruptedException {
        return evalFileBackground(source, fileName, null);
    }

    public Object evalFileBackground(final CommandSource source, final String fileName, final Object args) throws ScriptException, IOException, StateException, InterruptedException {
        assertInterpreterEnabled();
        assertStarted();
        if (fileName == null) {
            return null;
        }
        final String scriptName = getStandardScriptName(fileName);
        final Map<String, Object> argsDict = parseArgs(args);
        ArrayList pars = new ArrayList();
        pars.add(scriptName + " [background]");        
        for (String key : argsDict.keySet()) {
            pars.add(key + "=" + argsDict.get(key));
        }
        onCommand(Command.run, pars.toArray(), source);
        assertStarted();
        triggerWillRun(source, fileName, args);
        //Command source in statements execution will be CommandSource.script 
        Object result = null;
        CommandInfo info = new CommandInfo(source, fileName, null, args, true);
        commandBus.commandStarted(info);
        try {
            createExecutionContext();
            //TODO: args passing is not theread safe
            for (String key : argsDict.keySet()) {
                interpreter.setVar(key, argsDict.get(key));
            }
            result = interpreter.evalFileBackground(fileName);
            return result;
        } catch (Exception ex) {
            result = ex;
            throw ex;
        } finally {
            disposeExecutionContext();
            commandBus.commandFinished(info, result);
        }
    }

    public CompletableFuture<?> evalFileBackgroundAsync(final CommandSource source, final String fileName) {
        return getBackgroundFuture(() -> evalFileBackground(source, fileName));
    }

    public CompletableFuture<?> evalFileBackgroundAsync(final CommandSource source, final String fileName, final Object args) {
        return getBackgroundFuture(() -> evalFileBackground(source, fileName, args));
    }
    
    public Object evalLineBackground(final CommandSource source, final String line) throws ScriptException, IOException, StateException, InterruptedException {
        assertInterpreterEnabled();
        assertStarted();
        triggerWillEval(source, line);

        Object result = null;
        CommandInfo info = new CommandInfo(source, null, line, null, true);
        commandBus.commandStarted(info);
        try {
            createExecutionContext();
            InterpreterResult ir = interpreter.evalBackground(line);
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
            commandBus.commandFinished(info, result);
        }
    }
    
    public CompletableFuture<?> evalLineBackgroundAsync(final CommandSource source, final String line) {
        return getBackgroundFuture(() -> evalLineBackground(source, line));
    }
    

    Object tryEvalLineBackground(final CommandSource source, final String line) throws ScriptException, IOException, StateException, InterruptedException {
        if (interpreter.isThreaded()) {
            return evalLineBackground(source, line);
        } else {
            return evalLine(source, line);
        }
    }

    public Task startTask(final CommandSource source, String script, int delay, int interval) throws IOException, StateException {
        assertInterpreterEnabled();
        assertStarted();
        onCommand(Command.startTask, new Object[]{script, delay, interval}, source);
        Task task = taskScheduler.create(script, delay, interval);
        taskScheduler.start(task);
        return task;
    }

    public Task startTask(final CommandSource source, Task task) throws IOException, StateException {
        assertInterpreterEnabled();
        assertStarted();
        onCommand(Command.startTask, new Object[]{task}, source);
        taskScheduler.add(task);
        taskScheduler.start(task);
        return task;
    }

    public void stopTask(final CommandSource source, String script, boolean abort) throws IOException, StateException {
        assertInterpreterEnabled();
        assertStarted();
        onCommand(Command.stopTask, new Object[]{script, abort}, source);
        taskScheduler.remove(script, abort);
    }

    public void stopTask(final CommandSource source, Task task, boolean abort) throws IOException, StateException {
        assertInterpreterEnabled();
        assertStarted();
        onCommand(Command.stopTask, new Object[]{task, abort}, source);
        taskScheduler.remove(task, abort);
    }

    public Task getTask(String name) throws IOException, StateException {
        assertInterpreterEnabled();
        assertStarted();
        return getTaskScheduler().get(name);
    }

    public Task[] getTasks() throws IOException, StateException {
        assertInterpreterEnabled();
        assertStarted();
        return taskScheduler.getAll();
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
        String then = getThen(getExecutionPars().getCommandInfo());
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

    String runningScript;

    public String getRunningScriptName() {
        return getScriptPrefix(runningScript);
    }

    public File getRunningScriptFile() {
        return getScriptFile(runningScript);
    }

    public String getScriptPrefix(String script) {
        return (script == null) ? null : IO.getPrefix(script);
    }
    
    public void addLibraryPath(String path){
        interpreter.addLibraryPath(Setup.expandPath(path.toString()));                                        
    }
        

    public File getScriptFile(String script) {
        if (interpreter != null) {
            try {
                //!!! Is this invoking still needed?
                if (!interpreter.isThreaded()) {
                    return (File) runInInterpreterThread(null, (Callable<File>) () -> {
                        return interpreter.getScriptFile(script);
                    });
                } else {
                    return interpreter.getScriptFile(script);
                }
            } catch (Exception ex) {                
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
    
    public ExecutionParameters getExecutionParsForeground() {
        return getExecutionPars(null);
    }

    public void setExecutionPars(String name) {
        Map pars = new HashMap();
        pars.put("name", name);
        setExecutionPars(pars);
    }

    public void setExecutionPar(String name, Object value) {
        Map pars = new HashMap();
        pars.put(name, value);
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
        if (IO.isSubPath(fileName, Setup.getScriptsPath())) {
            return IO.getRelativePath(fileName, Setup.getScriptsPath());
        }
        return fileName;
    }

    Map<String, Object> parseArgs(Object args) {
        Map ret = new HashMap<>();
        if (args != null) {
            if (args instanceof Map map) {
                return map;
            } else {
                if (args.getClass().isArray()) {
                    args = Arrays.asList(args);
                }
                if (args instanceof List list) {
                    ret.put("args", list);
                } else {
                    ret.put("args", List.of(args));
                }
            }
        }
        return ret;
    }

    public Object evalFile(final CommandSource source, final String fileName, final Object args) throws ScriptException, IOException, StateException, InterruptedException {
        return evalFile(source, fileName, args, true);
    }

    public Object evalFile(final CommandSource source, final String fileName, final Object args, final boolean batch) throws ScriptException, IOException, StateException, InterruptedException {
        assertInterpreterEnabled();
        assertStarted();
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
                result = runInInterpreterThread(info, (Callable) () -> {
                    Object ret = null;
                    for (String key : argsDict.keySet()) {
                        interpreter.setVar(key, argsDict.get(key));
                    }
                    if (batch) {
                        ret = interpreter.evalFile(fileName);
                    } else {
                        int exceptionLine = -1;

                        final Statement[] statements = parseFile(fileName);
                        ret = interpreter.eval(statements);
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
    
    public Object evalStatement(final CommandSource source, final Statement statement) throws ScriptException, IOException, StateException, InterruptedException {
        if (statement == null) {
            return null;
        }
        return evalStatements(source, new Statement[]{statement}, false, null);
    }

    public Object evalStatements(final CommandSource source, final Statement[] statements, final boolean pauseOnStart, final Object args) throws ScriptException, IOException, StateException, InterruptedException {
        String fileName = null;
        if ((statements != null) && (statements.length > 0)) {
            fileName = statements[0].fileName;
        }
        return evalStatements(source, statements, pauseOnStart, fileName, args);
    }

    public Object evalStatements(final CommandSource source, final Statement[] statements, final boolean pauseOnStart, final String fileName, final Object args) throws ScriptException, IOException, StateException, InterruptedException {
        assertInterpreterEnabled();
        assertStarted();
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
                interpreter.pauseStatementListExecution();
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
                result = runInInterpreterThread(info, (Callable) () -> {
                    Object ret = interpreter.eval(statements, listener);
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
        if (interpreter == null) {
            return null;
        }
        try {
            if (!interpreter.isThreaded()) {
                return runInInterpreterThread(null, (Callable<Object>) () -> {
                    return interpreter.getLastResult();
                });
            }
            return interpreter.getLastResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public Object getLastScriptResult() {
        if (interpreter == null) {
            return null;
        }
        try {
            if (!interpreter.isThreaded()) {
                return runInInterpreterThread(null, (Callable<Object>) () -> {
                    return interpreter.getScriptResult();
                });
            }
            return interpreter.getScriptResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public Object getInterpreterVariable(String name) {
        if (interpreter == null) {
            return null;
        }
        try {
            if (!interpreter.isThreaded()) {
                return runInInterpreterThread(null, (Callable<Object>) () -> {
                    return interpreter.getVar(name);
                });
            }
            return interpreter.getVar(name);
        } catch (Exception ex) {
            return null;
        }
    }

    public void setInterpreterVariable(String name, Object value) {
        if (interpreter != null) {
            try {
                if (!interpreter.isThreaded()) {
                    runInInterpreterThread(null, () -> {
                        interpreter.setVar(name, value);
                        return null;
                    });
                } else {
                    interpreter.setVar(name, value);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error setting interpreter variable: {0}", name);
            }
        }
    }

    public String interpreterVariableToString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (!interpreter.isThreaded()) {
                return (String) runInInterpreterThread(null, (Callable<String>) () -> {
                    return obj.toString();
                });
            }
            return obj.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public void injectVars(final CommandSource source) {
        onCommand(Command.injectVars, null, source);
        try {
            runInInterpreterThread(null, new Callable() {
                @Override
                public Object call() throws Exception {
                    interpreter.injectVars();
                    return null;
                }
            });

        } catch (Exception ex) {
            Logger.getLogger(Context.class
                    .getName()).log(Level.WARNING, null, ex);
        }
    }

    public void abort(final CommandSource source) throws InterruptedException {
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
            CommandInfo cmd = commandBus.getInterpreterThreadCommand(false);
            if (cmd != null) {
                cmd.setAborted();
            }
            //TODO: This is also killing background acans. Should not be only foreground?
            for (Scan scan : getRunningScans()) {
                scan.abort();
            }
            synchronized (runningScans) {
                runningScans.clear();
            }
            if (interpreter != null) {
                interpreter.abort();
            }
        }

        if (interpreter != null) {
            interpreter.resetInterpreter();
        }
    }

    public boolean isAborted() {
        return aborted;
    }

    public boolean isFailed() {
        return (foregroundException != null);
    }

    public boolean isSuccess() {
        return !getState().isProcessing() && !isAborted() && !isFailed();
    }

    public Exception getForegroundException() {
        return foregroundException;
    }

    public void pause(final CommandSource source) throws InterruptedException {
        onCommand(Command.pause, null, source);
        for (Scan scan : getRunningScans()) {
            scan.pause();
        }
        if (getState() == State.Busy) {
            try {
                setState(State.Paused);
            } catch (StateException ex) {
            }
        }
        if (isRunningStatements()) {
            interpreter.pauseStatementListExecution();
        }
    }

    public void resume(final CommandSource source) throws InterruptedException {
        onCommand(Command.resume, null, source);
        for (Scan scan : getRunningScans()) {
            scan.resume();
        }
        if (getState() == State.Paused) {
            try {
                setState(State.Busy);
            } catch (StateException ex) {
            }
        }
        if (isRunningStatements()) {
            if (interpreter.isStatementListExecutionPaused()) {
                interpreter.resumeStatementListExecution();
            }
        }
    }

    public void step(final CommandSource source) throws InterruptedException {
        onCommand(Command.step, null, source);
        if (isRunningStatements()) {
            if (hasPausedScan()) {
                for (Scan scan : getRunningScans()) {
                    scan.resume();
                }
            } else {
                if (interpreter.isStatementListExecutionPaused()) {
                    interpreter.stepStatementListExecution();
                }
            }
        }
    }

    //These methods are made public in order to plugins control state
    //Start execution in interpreter thread (foreground task)
    @Hidden
    public void startExecution(final CommandSource source, String fileName, String command, Object args, boolean background) throws StateException {
        CommandInfo info = new CommandInfo(source, fileName, command, args, background);
        startExecution(source, fileName, info);
    }

    @Hidden
    public CommandInfo startExecution(final CommandSource source, String fileName, Object args, boolean background) throws StateException {
        CommandInfo info = new CommandInfo(source, fileName, null, args, background);
        startExecution(source, fileName, info);
        return info;
    }

    @Hidden
    public void startExecution(final CommandSource source, String fileName, CommandInfo info) throws StateException {
        assertReady();
        commandBus.commandStarted(info);
        if (fileName != null) {
            triggerWillRun(source, fileName, info.args);
            runningScript = fileName;
        } else {
            assertConsoleCommandAllowed(source);
        }
        next = null;
        aborted = false;
        setState(State.Busy);
        getExecutionPars().onExecutionStarted();
    }

    @Hidden
    public void clearAborted() throws StateException {
        assertReady();
        aborted = false;
    }

    Object evalNextStage(CommandInfo currentInfo, final String command) throws ScriptException, IOException, StateException, InterruptedException {
        onCommand(Command.then, new Object[]{command}, currentInfo.source);
        triggerShellCommand(currentInfo.source, "Then: " + command);
        CommandInfo info = new CommandInfo(currentInfo.source, null, command, null, false);
        commandBus.commandStarted(info);
        next = null;
        aborted = false;
        getExecutionPars().onExecutionStarted();
        try {
            InterpreterResult result = (InterpreterResult) runInInterpreterThread(info, (Callable<InterpreterResult>) () -> interpreter.eval(command));
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
    public void endExecution() throws StateException {
        endExecution(null);
    }

    @Hidden
    public void endExecution(CommandInfo info) throws StateException {
        endExecution(info, null);
    }

    @Hidden
    public void endExecution(CommandInfo info, Object result) throws StateException {
        if (info != null) {
            if (info.isRunning()) {
                commandBus.commandFinished(info, result);
            }
        }
        String then = (info == null) ? null : getThen(info);
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
        if (then != null) {
            boolean success = (!info.isError()) && (!info.isAborted());
            if (success && (then.onSuccess != null)) {
                return then.onSuccess;
            }
            if (!success && (then.onException != null)) {
                return then.onException;
            }
        }

        if (!info.background) {
            if (next != null) {
                return next;
            }
        }
        return null;
    }

    public void setNext(String nextStatement) throws State.StateException {
        this.getState().assertProcessing();
        next = nextStatement;
    }

    public String getNext() {
        return next;
    }

    public void updateAll(final CommandSource source) {
        onCommand(Command.updateAll, null, source);
        if (getState().isInitialized() & Context.hasDevicePool()) {
            Context.getDevicePool().updateAll();
        }
    }

    public void stopAll(final CommandSource source) {
        onCommand(Command.stopAll, null, source);
        if (getState().isInitialized() & Context.hasDevicePool()) {
            Context.getDevicePool().stopAll();
        }
    }

    public void stop(final CommandSource source, final GenericDevice dev) {
        onCommand(Command.stopDevice, null, source);
        if (getState().isInitialized() & Context.hasDevicePool()) {
            Context.getDevicePool().stop(dev);
        }
    }

    public ArrayList<GenericDevice> reinit(final CommandSource source) {
        onCommand(Command.reinit, new Object[0], source);
        if (getState().isInitialized() && Context.hasDevicePool()) {
            return Context.getDevicePool().retryInitializeDevices();
        }
        return new ArrayList<>();
    }

    public void reinit(final CommandSource source, GenericDevice device) throws IOException, InterruptedException {
        onCommand(Command.reinit, new Object[]{device}, source);
        if (getState().isInitialized()  && Context.hasDevicePool()) {
            Context.getDevicePool().retryInitializeDevice(device);
        }
    }
    
    
    
   //Script callbacks
    protected void onCommandStarted(CommandInfo info) {        
        if (isScriptCallbacksEnabled()){
            try {
                String var_name = "_command_info_" + Thread.currentThread().getId();
                if (interpreter.isThreaded()) {
                    interpreter.getEngine().put(var_name, info);
                    interpreter.getEngine().eval("on_command_started(" + var_name + ")");
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    protected void onCommandFinished(CommandInfo info) {
        if (isScriptCallbacksEnabled()){
            try {
                String var_name = "_command_info_" + Thread.currentThread().getId();
                if (interpreter.isThreaded()) {
                    interpreter.getEngine().put(var_name, info);
                    interpreter.getEngine().eval("on_command_finished(" + var_name + ")");
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        if (saveCommandStatistics) {
            try {
                CommandStatistics.save(info);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    void onChangeDataPath(File dataPath) {
        if (isScriptCallbacksEnabled()){
            try {
                String filename = (dataPath==null)? "None" : ("'" + dataPath.getCanonicalPath() + "'");
                if (interpreter.isThreaded()) {
                    interpreter.getEngine().eval("on_change_data_path(" + filename + ")");
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    public void onSessionStarted(int id) {
        if (isScriptCallbacksEnabled()){
            try {
                if (interpreter.isThreaded()) {
                    interpreter.getEngine().eval("on_session_started(" + id + ")");
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    public void onSessionFinished(int id) {
        if (isScriptCallbacksEnabled()){
            try {
                if (interpreter.isThreaded()) {
                    interpreter.getEngine().eval("on_session_finished(" + id + ")");
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }
     

    //UI aceess functions
    volatile UserInterface remoteUserInterface;

    void assertDefinedUI(UserInterface ui) {
        if (ui == null) {
            throw new java.lang.UnsupportedOperationException();
        }
    }

    public String getString(String message, String defaultValue, String[] alternatives) throws InterruptedException {
        UserInterface ui = getUI();
        if (alternatives != null) {
            return ui.getString(message, defaultValue, alternatives);
        } else {
            return ui.getString(message, defaultValue);
        }
    }

    public String getPassword(String message, String title) throws InterruptedException {
        UserInterface ui = getUI();
        return ui.getPassword(message, title);
    }

    public String getOption(String message, String type) throws InterruptedException {
        UserInterface ui = getUI();
        return ui.getOption(message, type);
    }

    public void showMessage(String message, String title, boolean blocking) throws InterruptedException {
        UserInterface ui = getUI();
        ui.showMessage(message, title, blocking);
    }

    public MonitoredPanel showPanel(GenericDevice dev) throws InterruptedException {
        UserInterface ui = getUI();
        return ui.showPanel(dev);
    }
    
    public MonitoredPanel showPanel(ScanResult result) throws InterruptedException {
        UserInterface ui = getUI();
        return ui.showPanel(result);
    }    

    public MonitoredPanel showPanel(String text, String title) throws InterruptedException {
        UserInterface ui = getUI();
        return ui.showPanel(text, title);
    }    
    
    public ConfigDialog showPanel(Config config) throws InterruptedException {
        UserInterface ui = getUI();
        return ui.showConfig(config);
    }
    public int waitKey(int timeout) throws InterruptedException {
        UserInterface ui = getUI();
        assertDefinedUI(ui);
        return ui.waitKey(timeout);
    }

    
    CommandSource sourceUI = CommandSource.ui;
    
    public UserInterface getUI() {
        if (sourceUI.isRemote()) {
            if (remoteUserInterface == null) {
               throw new RuntimeException("Remote user interface not defined");
            }
            return remoteUserInterface;
        }
        return Context.getUserInterface();
    }

    @Hidden
    public void setSourceUI(CommandSource source) {
        sourceUI = source;
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
    
    public List<Plot> plot(String root, String title) throws Exception {
        DataAddress add = DataAddress.fromFullPath(root);            
        return plot(add.root, add.path, title);
    }
    
    public List<Plot> plot(String root, String path, String title) throws Exception {     
        PlotDescriptor[] plots;        
        boolean endingSlash = (path!=null) && path.trim().endsWith("/");
        if (!Context.getDataManager().isGroup(root, path) && endingSlash){  //Clear ending slash if it is a dataset
            path = path.trim();
            path = path.substring(0, path.length()-1);            
        }
        if (!endingSlash){
            //tries to display the scan plot
            plots = Context.getDataManager().getScanPlots(root, path).toArray(new PlotDescriptor[0]);            
        } else {
            if (Context.getDataManager().isGroup(root, path)){                
                plots = Context.getDataManager().getChildrenPlots(root, path).toArray(new PlotDescriptor[0]);
            } else {
                plots = Context.getDataManager().getPlots(root, path).toArray(new PlotDescriptor[0]);
            }
        }
        return plot(plots, title);
        
    }    
    

    @Hidden
    public List<Plot> plot(PlotDescriptor plots[], String title) throws Exception {
        List ret = null;
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
                ret = plotListener.plot(title, plots);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
            throw ex;
        }
        return ret;
    }

    @Hidden
    public List<Plot> plot(PlotDescriptor plot, String title) throws Exception {
        if ( (plot.data!=null) && (plot.data instanceof Object[] rows) && Arr.getComponentType(plot.data)==Object.class){
            if ((rows.length>0) && (rows[0].getClass().isArray()) && (rows[0] instanceof Object[] firstRow)){
                var plots = new ArrayList<PlotDescriptor>();
                //composite value                
                for (int col=0; col<firstRow.length; col++){
                    var data = new ArrayList();
                    for (int row=0; row<rows.length; row++){
                        data.add(((Object[])rows[row])[col]);
                    }                    
                    
                    plots.add(new PlotDescriptor(data.toArray()));
                }
                return plot(plots.toArray(PlotDescriptor[]::new), title);
            }
        }
        return plot(new PlotDescriptor[]{plot}, title);
    }

    public List<String> getPlotTitles() {
        if (plotListener != null) {
            try {
                return plotListener.getTitles();
            } catch (Exception ex) {
                logger.log(Level.FINE, null, ex);
            }
        }
        return Arrays.asList(new String[]{PlotListener.DEFAULT_PLOT_TITLE});
    }

    public boolean removePlotContext(String context) {
        if ((context != null) && (!context.isEmpty()) && (!context.equals(PlotListener.DEFAULT_PLOT_TITLE))) {
            if (!getPlotTitles().contains(context)) {
                return false;
            }
            if (plotListener != null) {
                try {
                    plotListener.onTitleClosed(context);
                } catch (Exception ex) {
                    logger.log(Level.FINE, null, ex);
                }
            }
            if (serverPlotListener != null) {
                try {
                    serverPlotListener.onTitleClosed(context);
                } catch (Exception ex) {
                    logger.log(Level.FINE, null, ex);
                }
            }
            return true;
        }
        return false;
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
            if (xdata instanceof String col) {
                xdata = table.getColIndex(col);
            }
            if (xdata instanceof Integer index) {
                indexX = index;
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
        if (data instanceof List list) {
            data = list.toArray();
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
    public boolean hasPausedScan() {
        for (Scan scan : getRunningScans()) {
            if (scan.isPaused() && !scan.isAborted()) {
                return true;
            }
        }
        return false;
    }

    @Hidden
    public boolean hasRunningScan() {
        for (Scan scan : getRunningScans()) {
            if (!scan.isPaused() && !scan.isAborted()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPausableScan() {
        for (Scan scan : getRunningScans()) {
            if (!scan.isPaused() && !scan.isAborted() && scan.canPause()) {
                return true;
            }
        }
        return false;
    }

    @Hidden
    public boolean canPause() {
        if (hasPausableScan()) {
            return true;
        }
        if (isRunningStatements()) {
            if (!interpreter.isStatementListExecutionPaused()) {
                return true;
            }
        }
        return false;
    }

    @Hidden
    public boolean canStep() {
        if (isRunningStatements()) {
            if (hasPausedScan()) {
                return true;
            }
            if (interpreter.isStatementListExecutionPaused()) {
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
            if (interpreter.isStatementListExecutionPaused()) {
                return true;
            }
        }
        return false;
    }

    @Hidden
    public boolean isRunningStatements() {
        if (interpreter != null) {
            if ((getState() == State.Busy) || (getState() == State.Paused)) {
                if (interpreter.isRunningStatementList()) {
                    return true;
                }
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
        return interpreter.getRunningStatement();
    }

    //File parsing
    @Hidden
    public Statement[] parseFile(String fileName) throws ScriptException, IOException, InterruptedException {
        assertInterpreterEnabled();
        if (!interpreter.isThreaded()) {
            return (Statement[]) runInInterpreterThread(null, (Callable<Statement[]>) () -> {
                return interpreter.parse(fileName);
            });
        }
        return interpreter.parse(fileName);
    }

    @Hidden
    public Statement[] parseString(String script, String name) throws ScriptException, IOException, InterruptedException {
        assertInterpreterEnabled();
        if (!interpreter.isThreaded()) {
            return (Statement[]) runInInterpreterThread(null, (Callable<Statement[]>) () -> {
                return interpreter.parse(script, name);
            });
        }
        return interpreter.parse(script, name);
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
        if (interpreter == null) {
            return "[" + getState() + "] ";
        }

        String cursor = "[" + interpreter.getLineNumber() + "]";
        if (interpreter.getStatementLineCount() > 0) {
            cursor += "...";
        } else {
            cursor += "   ";
        }
        return cursor;

    }

    String processControlCommand(final CommandSource source, final ControlCommand command, final String parameters) throws ScriptException, IOException, StateException, InterruptedException {

        final String[] args = parameters.isEmpty() ? new String[0] : parameters.split(" ");

        StringBuilder sb = new StringBuilder();
        switch (command) {
            case abort:
                abort(source);
                return null;
            case pause:
                pause(source);
                return null;
            case resume:
                resume(source);
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
                    if ((args.length == 1) && (Context.hasSecurity())){
                        Context.getSecurity().selectUser(Context.getSecurity().getUser(args[0]), source);
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
                    for (String s : getHistoryEntries()) {
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
                for (GenericDevice dev : Context.getDevicePool().getAllDevices()) {
                    sb.append(dev.getName()).append("\n");
                }
                break;
            case run:
                assertReady();
                try {
                    evalFile(source, Paths.get(Setup.getScriptsPath(), args[0]).toString(), (args.length > 1) ? Arr.remove(args, 0) : null);
                } catch (Exception ex) {
                }
                return null;
            case inject:
                injectVars();
                break;
            case tasks:
                for (Task task : taskScheduler.getAll()) {
                    sb.append(task.toString()).append("\n");
                }
                break;
            case users:         
                if  (Context.hasSecurity()){
                    for (User user : Context.getSecurity().getUsers()) {
                        sb.append(user.toString()).append("\n");
                    }
                }
                sb.append("\nCurrent: " + Context.getUserName());
                break;
        }
        return sb.toString();
    }

    @Hidden
    public void scriptingLog(String msg) {
        logger.warning(msg);
    }

    @Hidden
    public String[] getBuiltinFunctionsNames() {
        return builtinFunctionsNames;
    }

    @Hidden
    public String getBuiltinFunctionDoc(String name) {
        if (interpreter.isThreaded()) {
            if (Arr.containsEqual(builtinFunctionsNames, name)) {
                if (!builtinFunctionsDoc.containsKey(name)) {
                    try {
                        builtinFunctionsDoc.put(name, (String) evalLineBackground("_getFunctionDoc(" + String.valueOf(name) + ")"));
                    } catch (Exception ex) {
                        Logger.getLogger(Context.class.getName()).log(Level.WARNING, null, ex);
                    }
                }
            }
        }
        if (builtinFunctionsDoc.containsKey(name)) {
            return builtinFunctionsDoc.get(name);
        } else {
            return "";
        }
    }
 
    //History
    void clearHistory(CommandSource source) {
        onCommand(Command.clearHistory, null, source);
        history.clear();
    }

    public List<String> getHistoryEntries() {
        return history.get();
    }

    public List<String> searchHistory(String text) {
        return history.search(text);
    }

    public History getHistory() {
        return history;
    }
    
    /**
     * Hints to graphical layer
     */
    public void setPreference(CommandSource source, ViewPreference preference, Object value) {
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

    
    //Session data
    public void writeSessionMetadata(String location, boolean attributes) throws IOException {
        if (Context.isHandlingSessions()) {
            if (location == null) {
                location = "/";
            }
            Map<String, Object> metadata = Context.getSessions().getMetadata(true);
            for (String key : metadata.keySet()) {
                Object value = metadata.get(key);
                if (value != null) {
                    if (value instanceof List list) {
                        value = list.toArray(new String[0]);
                    }
                    try {
                        if (attributes) {
                            Context.getDataManager().setAttribute(location, key, value);
                        } else {
                            Context.getDataManager().setDataset(location + "/" + key, value);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(Sequencer.class.getName()).log(Level.WARNING, null, ex);
                    }
                }
            }
        }
    }

    //Scans
    //!!!he scanListeners should be somewhere else
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
    
    public Scan[] getVisibleScans() {
        var ret = new ArrayList<Scan>();
        for (Scan scan: getRunningScans()){
            if (!scan.isHidden()){
                ret.add(scan);
            }
        }
        return ret.toArray(new Scan[0]);            
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
            try {
                Context.getUserInterface().showMessage(msg, "Error", true);
            } catch (InterruptedException ex) {
            }
        }
        System.exit(0);
    }

    protected void onCreation() {
        if (!isLocalMode()) {
            LockMode lockMode = Setup.getLockMode();
            if (lockMode != LockMode.none) {
                if (lockFile == null) {
                    String path = lockMode==LockMode.global ? new File(Setup.getConfigFile()).getParent() : Setup.getContextPath();
                    Path lockFilePath = Paths.get(path, "Lock.dat");
                    try {
                        lockFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
                        FileLock lock = lockFile.getChannel().tryLock();
                        if (lock == null) {
                            throw new Exception("Cannot have access to lock file");
                        }
                        lockFile.setLength(0);
                        lockFile.write(Sys.getProcessName().getBytes());
                        if (lockMode==LockMode.global){
                            IO.setFilePermissions(lockFilePath.toString(), Context.getConfigFilePermissions());
                        }
                        lock.release();
                        lock = lockFile.getChannel().tryLock(0, Long.MAX_VALUE, true); //So other process can read active process
                    } catch (FileNotFoundException ex) {
                        String message = "Error acessing lock file: " + ex.toString();
                        message += ".\nApplication can be started in local mode (-l option)";
                        exit(message);
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
                if (terminalConfig.isEnabled()) {
                    try (ServerSocket socket = new ServerSocket(terminalConfig.port)) {
                    } catch (IOException ex) {
                        exit("Another instance of this application is running.\nApplication can be started in local mode (-l option)");
                    }
                }
            }
            //TODO: Clear trash left by native libraries in temp folder (Windows only)
            if (Sys.getOSFamily() == Sys.OSFamily.Windows) {
                for (String pattern : new String[]{"jansi*.*", "jffi*.*", "jhdf*.so", "nativedata*.so", "liblz4*.so", "libbitshuffle*.dll", "BridJExtractedLibraries*"}) {
                    try {
                        for (File f : IO.listFiles(Sys.getTempFolder(), pattern)) {
                            try {
                                if (f.isDirectory()) {
                                    IO.deleteRecursive(f.getAbsolutePath());
                                    logger.log(Level.FINER, "Deleted temp folder: {0}", f.getName());
                                } else {
                                    f.delete();
                                    logger.log(Level.FINER, "Deleted temp file: {0}", f.getName());
                                }
                            } catch (Exception ex) {
                                logger.log(Level.FINE, "Cannot delete temp {0}: {1}", new Object[]{(f.isDirectory()) ? "folder" : "file", f.getName()});
                            }
                        }
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }
            }
        }

        if (Setup.isSimulation()) {
            logger.warning("Simulation mode");
        }
        
        if (Setup.isDebug()) {
            logger.warning("Debug mode");
        }

        if (!isInterpreterEnabled()) {
            logger.warning("Disabled mode: interpreter is not started");
        } else if (isGenericMode()) {
            logger.warning("Generic mode: local startup script is not executed");
        }

        if (Context.hasExtensions()) {
            if (Setup.isBare()) {
                logger.warning("Bare mode: plugins folder is not loaded");
            } else {                
                //Load  plugins
                Context.getExtensions().loadPluginFolder(Setup.getPluginsConfigurationFile());
            }
            //Even if isBareMode because plugins may be loaded by command line        
            Context.getExtensions().startPlugins();
        }
        
        
        if (isLocalMode()) {
            logger.warning("Local mode");
            if (Boolean.TRUE.equals(Setup.isEnableExtract())) {
                extractUtilities();
            }
        } else {
            extractUtilities();
        }
    }

    void extractUtilities() {
        if (Boolean.FALSE.equals(Setup.isEnableExtract())) {
            return;
        }
        //Extract script path if not present
        //TODO: Does not work with a Linux hard link
        String jar = Setup.getJarFile();
        if ((jar != null) && (IO.getExecutingJar(getClass())!=null)) { //If not in IDE
            File jarFile = new File(jar);
            File startupScript = new File(getDefaultStartupScript());

            boolean defaultLibPathConfig = (startupScript != null)
                    && //(IO.isSubPath(Setup.getScriptsPath(), setup.getHomePath())) &&
                    (IO.isSubPath(startupScript.getParent(), Setup.getScriptsPath()));
            //Only extracts binary files if startup script is inside script path
            if (defaultLibPathConfig) {
                //Only extracts the full library contents in the first run
                if ((new File(Setup.getScriptsPath())).listFiles().length == 0) {
                    logger.warning("Extracting script library folder");
                    extractedUtilities = true;
                    try {
                        IO.extractZipFile(jarFile, Setup.getHomePath(), "script");
                        if (Context.getScriptFilePermissions() != FilePermissions.Default) {
                            IO.setFilePermissions(IO.listFilesRecursive(Paths.get(Setup.getHomePath(), "script").toString()),Context.getScriptFilePermissions());
                        }
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                } else {
                    if (!startupScript.exists() || (startupScript.lastModified() < jarFile.lastModified()) || (Boolean.TRUE.equals(Setup.isEnableExtract()))) {
                        logger.info("Extracting startup script and utilities");
                        extractedUtilities = true;
                        try {
                            logger.log(Level.FINE, "Extracting: {0}", startupScript.getName());
                            IO.extractZipFileContent(jarFile, "script/Lib/" + startupScript.getName(), startupScript.getCanonicalPath());
                            IO.setFilePermissions(startupScript, Context.getScriptFilePermissions());
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, null, ex);
                        }

                        try {
                            for (String file : IO.getJarChildren(new JarFile(jarFile), "script/Lib/")) {
                                String ext = IO.getExtension(file);
                                String prefix = IO.getPrefix(file);
                                String name = prefix + "." + ext;
                                if (ext.equalsIgnoreCase(getScriptType().getExtension())) {
                                    File scriptFile = Paths.get(Setup.getStandardLibraryPath(), name).toFile();
                                    if (!scriptFile.equals(startupScript)) {
                                        logger.fine("Extracting: " + name);
                                        IO.extractZipFileContent(jarFile, "script/Lib/" + name, scriptFile.getCanonicalPath());
                                        IO.setFilePermissions(scriptFile, Context.getScriptFilePermissions());
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
                File indexFile = new File(Setup.getWwwIndexFile());
                if (!indexFile.exists()) {
                    try {
                        logger.warning("Extracting www folder");
                        IO.extractZipFile(jarFile, new File(Setup.getWwwPath()).getParent(), new File(Setup.getWwwPath()).getName());
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                } else if ((indexFile.lastModified() < jarFile.lastModified())) {
                    if (!Setup.isRunningInIde()){
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
    }
    
    File extractTempScript(String resourceFile) throws IOException{
        File file = null;
        String prefix = IO.getPrefix(resourceFile);
        String suffix = "."+IO.getExtension(resourceFile);        
        String jar = Setup.getJarFile();
        if (jar != null) {            
            File jarFile = new File(jar);        
            file = File.createTempFile(prefix, suffix);
            logger.log(Level.FINE, "Extracting: {0}", resourceFile);
            IO.extractZipFileContent(jarFile, "script/Lib/" + resourceFile, file.getCanonicalPath());            
        }
        return file;
    }
    

    void reloadPlugins(CommandSource source) {
        onCommand(Command.reloadPlugins, null, source);
        if (Context.hasExtensions()) {
            if (Setup.isBare()) {
                for (ch.psi.pshell.extension.Plugin p : Context.getExtensions().getLoadedPlugins()) {
                    Context.getExtensions().reloadPlugin(p);
                }
            } else {
                Context.getExtensions().reloadPlugins();
            }
        }
    }

    public Plugin[] getPlugins() {
        if (Context.hasExtensions()) {
            return  Context.getExtensions().getLoadedPlugins();
        }
        return new Plugin[0];
    }

    public Plugin getPlugin(String name) {
        for (Plugin p : getPlugins()) {
            if (IO.getPrefix(p.getPluginName()).equals(name)) {
                return p;
            }
        }
        return null;
    }

    public File[] getExtensions() {
        if (Context.hasExtensions()) {
            return  Context.getExtensions().getExtensionLibraries().toArray(new File[0]);
        }
        return new File[0];
    }
    
    /**
     * Search class in system class loader and plugins
     */
    public Class getClassByName(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className, true, Sys.getDynamicClassLoader());
        } catch (ClassNotFoundException ex) {
            if (Context.hasExtensions()) {
                Class cls = Context.getExtensions().getDynamicClass(className);
                if (cls != null) {
                    return cls;
                }
                for (Plugin p : Context.getExtensions().getLoadedPlugins()) {
                    if (p.getClass().getName().equals(className)) {
                        return p.getClass();
                    }
                }
            }
            try {
                Object cls = evalLineBackground(className);
                if (cls instanceof Class c) {
                    return c;
                }
            } catch (Exception ex1) {
            }
            throw ex;
        }
    }

    //Public command interface    
    CommandSource getPublicCommandSource() {
        CommandInfo ret = commandBus.getCurrentCommand(false);
        return (ret == null) ? CommandSource.ui : ret.source;
    }

    //For scripts to check permissions when running files
    @Hidden
    public CommandInfo startScriptExecution(String fileName, Object args) {
        triggerWillRun(getPublicCommandSource(), fileName, args); 
        CommandInfo parent = commandBus.getCurrentCommand(false);
        CommandInfo info = new CommandInfo(parent, fileName, args);
        commandBus.commandStarted(info);
        return info;
    }

    @Hidden
    public void finishScriptExecution(CommandInfo info, Object result) {
        commandBus.commandFinished(info, result);
    }



    @Hidden
    public void disable() {
        if (getState().isNormal()) {
            logger.warning("Setting offline mode");
            for (AutoCloseable ac : new AutoCloseable[]{taskScheduler, interpreter}) {
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

    public void restart() throws StateException {
        restart(getPublicCommandSource());
    }

    public void startRestart() throws StateException {
        new Thread(() -> {
            try {
                restart();
            } catch (Exception ex) {
                Logger.getLogger(Context.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, "Restart task").start();

    }

    public Object evalLine(String line) throws ScriptException, IOException, StateException, InterruptedException {
        return evalLine(getPublicCommandSource(), line);
    }

    public CompletableFuture<?> evalLineAsync(final String line) throws StateException {
        return evalLineAsync(getPublicCommandSource(), line);
    }

    public Object evalLineBackground(String line) throws ScriptException, IOException, StateException, InterruptedException {
        return evalLineBackground(getPublicCommandSource(), line);
    }
    
    
    public void evalLineAfter( final String line, boolean onSuccess, boolean onException) throws ScriptException, IOException, StateException, InterruptedException {
        evalLineAfter(getPublicCommandSource(), line, onSuccess, onException);
    }    

    public Object tryEvalLineBackground(String line) throws ScriptException, IOException, StateException, InterruptedException {
        return tryEvalLineBackground(getPublicCommandSource(), line);
    }

    public CompletableFuture<?> evalLineBackgroundAsync(final String line) {
        return evalLineBackgroundAsync(getPublicCommandSource(), line);
    }

    public Object evalFile(String fileName, Object args) throws ScriptException, IOException, StateException, InterruptedException {
        return evalFile(getPublicCommandSource(), fileName, args);
    }

    public CompletableFuture<?> evalFileAsync(final String fileName) throws StateException {
        return evalFileAsync(getPublicCommandSource(), fileName);
    }

    public CompletableFuture<?> evalFileAsync(String fileName, Object args) throws StateException{
        return evalFileAsync(getPublicCommandSource(), fileName, args);
    }

    public Object evalFileBackground(final String fileName) throws ScriptException, IOException, StateException, InterruptedException {
        return evalFileBackground(getPublicCommandSource(), fileName);
    }

    public Object evalFileBackground(final String fileName, final Object args) throws ScriptException, IOException, StateException, InterruptedException {
        return evalFileBackground(getPublicCommandSource(), fileName, args);
    }

    public CompletableFuture<?> evalFileBackgroundAsync(final String fileName) {
        return evalFileBackgroundAsync(getPublicCommandSource(), fileName);
    }

    public CompletableFuture<?> evalFileBackgroundAsync(final String fileName, final Object args) {
        return evalFileBackgroundAsync(getPublicCommandSource(), fileName, args);
    }

    public Object evalStatement(Statement statement) throws ScriptException, IOException, StateException, InterruptedException {
        return evalStatement(getPublicCommandSource(), statement);
    }

    public Object evalStatements(Statement[] statements, boolean pauseOnStart, String fileName, Object args) throws ScriptException, IOException, StateException, InterruptedException {
        return evalStatements(getPublicCommandSource(), statements, pauseOnStart, fileName, args);
    }

    public Task startTask(String script, int delay, int interval) throws IOException, StateException {
        return startTask(getPublicCommandSource(), script, delay, interval);
    }

    public Task startTask(Task task) throws IOException, StateException {
        return startTask(getPublicCommandSource(), task);
    }

    public void stopTask(String script, boolean abort) throws IOException, StateException {
        stopTask(getPublicCommandSource(), script, abort);
    }

    public void stopTask(Task task, boolean abort) throws IOException, StateException {
        stopTask(getPublicCommandSource(), task, abort);
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

    public void pause() throws InterruptedException {
        pause(getPublicCommandSource());
    }

    public void resume() throws InterruptedException {
        resume(getPublicCommandSource());
    }

    public void step() throws InterruptedException {
        step(getPublicCommandSource());
    }

    public void reloadPlugins() {
        reloadPlugins(getPublicCommandSource());
    }

    public void clearHistory() {
        clearHistory(getPublicCommandSource());
    }

    public void setPreference(ViewPreference preference, Object value) {
        setPreference(getPublicCommandSource(), preference, value);
    }

    void assertConsoleCommandAllowed(CommandSource source) {
        if (!source.isInternal()) {
            if (Context.hasSecurity()){
                Context.getSecurity().getCurrentRights(source.isRemote()).assertConsoleAllowed();
            }
        }
    }

    void assertRunAllowed(CommandSource source) {
        if (!source.isInternal()) {
            if (Context.hasSecurity()){
                Context.getSecurity().getCurrentRights(source.isRemote()).assertRunAllowed();
            }
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
    
    void closeLockFile() {
        if (lockFile != null) {
            try {
                lockFile.close();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error closing lock file: {0}", ex.toString());
            }
            lockFile = null;
        }
    }
    
         
    //Disposing
    @Override
    public void close() {
        try {
            setState(State.Closing);
        } catch (StateException ex) {
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

        for (AutoCloseable ac : new AutoCloseable[]{taskScheduler, interpreter, server}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        if (interpreter != null) {
            if (!Setup.isRunningInIde()){
                //TODO: remove this  when Jython fixes this: https://github.com/jython/jython/issues/93                
                if (!Setup.getNoBytecodes() && !isVolatileMode()) {
                    interpreter.startFixClassFilesPermissions();
                }
            }
        }
        if (interpreterExecutor != null) {
            interpreterExecutor.shutdownNow();
        }
        closeLockFile();
    }
}
