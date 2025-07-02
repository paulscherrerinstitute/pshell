package ch.psi.pshell.framework;

import ch.psi.pshell.app.AppListener;
import ch.psi.pshell.app.StatusBar;
import ch.psi.pshell.data.DataAddress;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePanelManager;
import ch.psi.pshell.logging.LogManager;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plotter.Client;
import ch.psi.pshell.plotter.Plotter;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.InterpreterListener;
import ch.psi.pshell.sequencer.PlotListener;
import ch.psi.pshell.swing.ConfigDialog;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.OutputPanel;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.swing.ScanEditorPanel;
import ch.psi.pshell.swing.Shell;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.TextEditor;
import ch.psi.pshell.swing.UserInterface;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.EncoderJson;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Str;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker.StateValue;
import javax.swing.WindowConstants;

/**
 * The application singleton object.
 */
public class App extends ch.psi.pshell.devices.App {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    
    Processor runningProcessor;
    Shell shell;
    Console console;
    LogManager logManager;
        
    static public App getInstance() {
        return (App)instance;
    }
    
    public State getState() {
        return Context.getState();
    }
    
    public static MainFrame getMainFrame(){
        return MainFrame.getInstance();
    }            
        
    protected App(){
        if (Setup.isServerMode()) {
            SwingUtils.setHeadless();
        }         
        if (Setup.isVolatile()) {            
            try {
                Path tempDir = Files.createTempDirectory("pshell_home");
                IO.deleteFolderOnExit(tempDir.toFile());
                Setup.redefinePath(ch.psi.pshell.app.Options.HOME_PATH, tempDir.toString());                
                Setup.mkdirs();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        }                          
    }

    public void start() throws Exception {        
        //Raise exception if wrong formatting;
        getTaskArgs();
        super.start(Setup.isCli());
    }    
    
    @Override
    protected void onStart() {
        logger.log(Level.INFO, "Version: {0}", getApplicationBuildInfo());
        if (Context.hasPluginManager()) {
            Context.getPluginManager().loadExtensionsFolder();
        }
        if (Context.hasPackageManager()) {
            Context.getPackageManager().loadExtensionsFolders();
        }        
        
        Context.getInterpreter().addListener(new InterpreterListener() {
            @Override
            public void onStateChanged(State state, State former) {
                try {
                    if ((former == State.Initializing) && (state == State.Ready)) {
                        for (ch.psi.pshell.sequencer.Task task : getTaskArgs()) {
                            Context.getInterpreter().startTask(task);
                        }
                    }
                    for (AppListener listener : getListeners()) {
                        try {
                            listener.onStateChanged(state, former);
                        } catch (Exception ex) {
                        }
                    }
                    if (getPropertyChangeSupport() != null) {
                        getPropertyChangeSupport().firePropertyChange("appstate", former, state);
                    }
                    if (state == State.Ready) {
                        SwingUtilities.invokeLater(() -> {
                            checkNext();
                        });
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }

            @Override
            public void onPreferenceChange(ViewPreference preference, Object value) {
                switch (preference) {
                    case STATUS:
                        if (value == null) {
                            value = Context.getState().toString();
                        }
                        if (getPropertyChangeSupport() != null) {
                            getPropertyChangeSupport().firePropertyChange("message", "", String.valueOf(value));
                        }
                        break;
                }
            }

            @Override
            public void onShellStdout(String str) {
                if (Setup.isDetached()) {
                    if (!Setup.isOutputRedirected()) {
                        System.out.println(str);
                    }
                }
            }

            @Override
            public void onShellStderr(String str) {
                if (Setup.isDetached()) {
                    if (!Setup.isOutputRedirected()) {
                        System.err.println(str);
                    }
                }
            }

        });

        if (!Setup.isHeadless()) {
            Context.setUserInterface(new UserInterface() {

                @Override
                public String getString(String message, String defaultValue) {
                    return SwingUtils.getString(getMainFrame(), message, defaultValue);
                }

                @Override
                public String getString(String message, String defaultValue, String[] alternatives) {

                    return SwingUtils.getString(getMainFrame(), message, alternatives, defaultValue);
                }

                @Override
                public String getPassword(String message, String title) {
                    return SwingUtils.getPassword(getMainFrame(), title, message);
                }

                @Override
                public String getOption(String message, String type) {
                    SwingUtils.OptionResult ret = SwingUtils.showOption(getMainFrame(), null, message, SwingUtils.OptionType.valueOf(type));
                    if (ret == SwingUtils.OptionResult.Closed) {
                        ret = SwingUtils.OptionResult.Cancel;
                    }
                    return ret.toString();
                }

                @Override
                public void showMessage(String message, String title, boolean blocking) {
                    if (blocking) {
                        SwingUtils.showMessageBlocking(getMainFrame(), title, message);
                    } else {
                        SwingUtils.showMessage(getMainFrame(), title, message);
                    }
                }

                @Override
                public MonitoredPanel showPanel(Nameable dev) {
                    if (dev instanceof GenericDevice genericDevice){
                        return DevicePanelManager.getInstance().showPanel(genericDevice);
                    }
                    return null;
                }

                @Override
                public ConfigDialog showConfig(Config config) throws InterruptedException {
                    return ConfigDialog.showConfigEditor(getMainFrame(), config, false, false);
                }
                
                @Override
                public MonitoredPanel showPanel(DataAddress address, String message) throws InterruptedException {                                       
                    try{
                        DataPanel dp =  (getMainFrame() != null) ? 
                                getMainFrame().showDataFileWindow(address.root, address.path):
                                DataPanel.createDialog(getMainFrame(), address.root, address.path);
                        if (dp!=null){
                            return dp;
                        }
                        //Only in-memory
                    } catch (InterruptedException ex){
                        throw ex;
                    } catch (Exception ex){                        
                    }            
                    return null;
                }
                @Override
                public MonitoredPanel showPanel(String text, String title) throws InterruptedException{
                        TextEditor editor = new TextEditor();
                        editor.setText(text);
                        editor.setReadOnly(true);
                        editor.setFont(new Font("Courrier New", 1, 13));
                        SwingUtils.showDialog(getMainFrame(),title, new Dimension(1000, 600), editor);                        
                        return editor;
                }

                @Override
                public int waitKey(int timeout) throws InterruptedException {
                    return Shell.waitKey(timeout);
                }
            });

        }
        loadCommandLinePlugins();
        setupScanPrinting();
        Context.getInterpreter().redirectScriptStdio();
        String plotServer = Setup.getPlotServer();
        if (plotServer != null) {
            startPlotServerConnection(plotServer, 5000);
        }
    }
    
    
    String logFileName;
    public String getLogFileName() {
        return logFileName;
    }    
                
    public void setupLogger(String path, Level level, int daysToLive, FilePermissions permissions) {
        if (LogManager.hasInstance()){
            //!!! Should change permissions if changed?
        } else {
            logManager = new LogManager(permissions);
        }
        String ext = Setup.isLocal() ? "local.log" : "log";
        logFileName = Setup.expandPath(path + "." + ext);
        LogManager.getInstance().start(logFileName, Setup.isLocal() ? -1 : daysToLive);
        LogManager.getInstance().setLevel(level);
    }
   
    protected void startRestart() {
        try {
            new Thread(() -> {
                restart();                    
            }, "Restart task").start();        
        } catch (Exception ex) {
             logger.log(Level.SEVERE, null, ex);
        }
    }           
    
    protected void onRestart() throws Exception {
        
    }
    
    public void restart() {
        try {            
            for (AutoCloseable ac : new AutoCloseable[]{}) {
                try {
                    if (ac != null) {
                        ac.close();
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
            onRestart();
        } catch (Exception ex) {
             logger.log(Level.SEVERE, null, ex);
        }        
    }
        
    protected <T extends MainFrame> void launchApp(Class<T> viewClass) {
        if ((viewClass==null) && !Setup.isCli()){
            Options.SERVER.set();
        }
        if (Setup.isGui()) {
            if (Setup.isShell()) {
                startShell();
                logger.log(Level.INFO, "Create Shell");
            } else if (Setup.isDetached()) {
                    logger.log(Level.INFO, "Create Panels");
                    if (Setup.isDetachedPlots()) {
                        setConsolePlotEnvironment(null);
                        setupConsoleScanPlotting();
                    }
            } else {
                logger.log(Level.INFO, "Create Application MainFrame");
                registerProcessors();
                MainFrame view;
                try {
                    view = viewClass.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                view.setVisible(!Setup.isHidden());
                outputPanel = view.getOutputPanel();
            }
            
            if (!Setup.isOffline()) {
                new Thread(() -> {
                    restart();          
                    if (hasCommandLineTask()){
                         execCommandLineTasks();
                    }
                }, "Startup task").start();        
            } else {
                disable();
            }
        } else {
            restart();       
            setConsolePlotEnvironment(null);                
            
            if (Setup.isHeadless()) {
                logger.log(Level.WARNING, "Headless mode");
            }
            if (Setup.isCli()) {
                logger.log(Level.INFO, "Start console");
                startConsole();
            } else if (Setup.isServerMode()) {
                logger.log(Level.INFO, "Start server");
                if (Setup.isOffscreenPlotting()) {
                     setupConsoleScanPlotting();
                }
                if (hasCommandLineTask()){
                     execCommandLineTasks();
                }                     
            } else {
                if (Setup.isHeadless()) {
                    throw new RuntimeException("If headless the application must start in cli or server mode.");
                }
                throw new RuntimeException("Invalid application arguments");
            }
        }
    }    
    
    @Override
    protected void applyViewOptions(){
        if (hasMainFrame()){
            getMainFrame().applyOptions();
        }
    }
    
    public boolean isViewPersisted() {
       return !Setup.isLocal() ;
    }
    
    public void persistViewState() {
        if (isViewPersisted()){
            super.persistViewState();
        }                    
    }
    
    protected void disable(){
        if (Context.hasInterpreter()){
            Context.getInterpreter().disable();
        }
        for (AutoCloseable ac : new AutoCloseable[]{}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }        
        
    }    
    
    
    @Override
    protected void onExit() {
        super.onExit();
        for (AutoCloseable ac : new AutoCloseable[]{}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }    

    static public File getFileArg() {
        List<File> args = getFileArgs();
        if ((args==null) || (args.size()==0)){
            return null;
        }
        return args.get(0);
    }
    
    static public List<File> getFileArgs() {
        ArrayList<File> ret = new ArrayList<File>();
        var files = Setup.getFileArgs();
        for (String fileName : files) {
            try {
                fileName = Setup.expandPath(fileName);
                File file = new File(fileName);
                if (!file.exists()) {
                    File aux = Context.getInterpreter().getScriptFile(file.getName());
                    if ((aux!=null) && (aux.exists())){
                        file = aux;
                    }
                }                            
                ret.add(file);
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }     
        return ret;
    }   
    
    static public List<File> getPackageArgs() {
        var ret = new ArrayList<File>();
        for (String path : Setup.getPackageArgs()) {
            try {
                path = ch.psi.pshell.app.Setup.expandPath(path);
                File file = new File(path);
                if (file.exists() && file.isDirectory()) {
                    ret.add(file);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        return ret;
    }        

    static public List<ch.psi.pshell.sequencer.Task> getTaskArgs() {
        List<ch.psi.pshell.sequencer.Task> ret = new ArrayList<>();
        //Check formatting
        for (String task : Setup.getTaskArgs()) {
            ret.add(ch.psi.pshell.sequencer.Task.fromString(task));
        }
        return ret;
    }            
    
    static public Map<String, Object> getInterpreterArgs() {
        try {
            List<String> args = Setup.getInterpreterArgs();
            if ((args==null) || args.isEmpty()) {
                return null;
            }
            HashMap<String, Object> ret = new HashMap<>();
            for (String str: args){
                for (String token : str.split(",")) {
                    if (token.contains(":")) {
                        String name = token.substring(0, token.indexOf(":")).trim();
                        String value = token.substring(token.indexOf(":") + 1).trim();
                        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                            ret.put(name, value.substring(1, value.length() - 1));
                        } else if (value.equalsIgnoreCase("false")) {
                            ret.put(name, Boolean.FALSE);
                        } else if (value.equalsIgnoreCase("true")) {
                            ret.put(name, Boolean.TRUE);
                        } else {
                            try {
                                ret.put(name, Integer.valueOf(value));
                            } catch (Exception ex) {
                                try {
                                    ret.put(name, Double.valueOf(value));
                                } catch (Exception e) {
                                }
                            }
                        }
                        if (!ret.containsKey(name)) {
                            System.err.println("Invalid argument value: " + value);
                        }
                    }
                }
            }
            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    
    
    //Acceps multiple -p options or plugin names can be separetate by ','
    void loadCommandLinePlugins() {
        if (Context.hasPluginManager()) {
            for (String arg : Setup.getPluginArgs()) {
                for (String pluginName : arg.split(",")) {                    
                    if (new File(pluginName).exists()) {
                        Context.getPluginManager().loadPlugin(pluginName);
                    } else if (new File(Setup.expandPath(pluginName)).exists()) {
                        Context.getPluginManager().loadPlugin(Setup.expandPath(pluginName));
                    } else if (Paths.get(Setup.getPluginsPath(), pluginName).toFile().exists()) {
                        Context.getPluginManager().loadPlugin(Paths.get(Setup.getPluginsPath(), pluginName).toString());
                    } else {
                        //Try class name
                        Context.getPluginManager().loadPluginClass(pluginName);
                    }
                }
            }
        }
    }    
    
    boolean hasCommandLineTask(){
        if (getFileArg() != null){
            //If a MainFrame present then by opens the file, but just runs if -start is set
            if (Setup.getStartArg() || !Context.hasView()){ 
                return true;
            }
        }
        return (getEvalArg() != null);
    }
            
    protected void execCommandLineTasks() {
        if (hasCommandLineEval()){
            execCommandLineEval(false);
        }
        
        if (hasCommandLineFile()){
            execCommandLineFile(false);
        }
        if (hasCommandLineTask()){            
            checkAutoClose();
        }
    }
    
    protected void checkAutoClose(){
        if (Setup.isAutoClose()) {
            if (Context.getState().isProcessing()){
                logger.log(Level.INFO, "Waiting for processing to finish");              
                try {
                    Context.waitStateNotProcessing(-1);      
                    logger.log(Level.WARNING, "Auto-close: Finished command line task - closing the application");   
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            exit(this);
        }                              
    }
    
    public boolean hasCommandLineEval(){
        return (getEvalArg() != null);
    }
    
    public boolean execCommandLineEval(boolean async) {
        var evals = Setup.getEvalArgs();
        if ((evals != null)){
            if (async){
                new Thread(() -> {
                    try {
                        execCommandLineEval(false);
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }, "CommandLine Eval Execution Thread").start();                 
                return true;
            }                
            for (String eval : evals){
                runStatement(eval);
            }
            return true;            
        }            
        return false;
    }
    
    public boolean hasCommandLineFile(){
        return (getFileArg() != null);
    }    
    public boolean execCommandLineFile(boolean async) {
        File file = getFileArg();
        if (file!=null){
            if (async){
                new Thread(() -> {
                    try {
                        execCommandLineFile(false);
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }, "CommandLine File Execution Thread").start();                 
                return true;
            }                
            if (file != null) {
                runFile(file);
            } 
            return true;            
        }            
        return false;
    }
    
    
    



    public Object evalFile(File file) throws Exception {
        return evalFile(file,  null);
    }
    
    public Object evalFile(File file, Map<String, Object> args) throws Exception {
        return evalFile(file, args, false);
    }

    public Object evalFile(File file, Map<String, Object> args, boolean topLevel) throws Exception {
        logger.log(Level.INFO, "Eval file: {0} args: {1} top_level: {2}", new Object[]{file.getPath(), Str.toString(args, 20), topLevel});
        Context.getInterpreter().clearAborted();
        Processor processor = getProcessor(file);
        if (processor != null) {
            if (getMainFrame()!= null) {
                getMainFrame().setCurrentProcessor(processor, topLevel);
            }
            runningProcessor = processor;
            try {
                processor.execute(processor.resolveFile(file.getPath()), args);
                Thread.sleep(100); //Give som time if processor does not change app state immediatelly;
                return processor.waitComplete(-1);
            } finally {
                runningProcessor = null;
            }
        }
        return Context.getInterpreter().evalFile(file.getPath(), args);
    }
            
    public Object evalStatement(String statement) throws Exception {
        logger.log(Level.INFO, "Eval statement: {0}", statement);
        Context.getInterpreter().clearAborted();
        return Context.getInterpreter().evalLine(statement);
    }

    public void abortEval(File file) throws InterruptedException {
        logger.log(Level.INFO, "Abort eval{0}", file.getPath());
        if (runningProcessor != null) {
            runningProcessor.abort();
        }
        Context.abort();
    }

    Processor getProcessor(File file) throws Exception {
        if (!IO.getExtension(file).isEmpty()) {
            for (Processor processor : Processor.getServiceProviders()) {
                if (Arr.containsEqual(processor.getExtensions(), IO.getExtension(file))) {
                    return processor;
                }
            }
        }
        return null;
    }

    public static class ExecutionStage {

        final public File file;
        final public Map<String, Object> args;
        final public String statement;

        ExecutionStage(File file, Map<String, Object> args) {
            this.file = file;
            this.args = args;
            this.statement = null;
        }

        ExecutionStage(String statement) {
            this.file = null;
            this.args = null;
            this.statement = statement;
        }

        public String getArgsStr() {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (String key : args.keySet()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    Object value = args.get(key);
                    String text;
                    try {
                        text = EncoderJson.encode(value, false);
                    } catch (Exception ex) {
                        text = String.valueOf(value);
                    }
                    sb.append(key).append(":").append(text);
                }
            }
            return sb.toString();
        }

        public String toString() {
            if (file != null) {
                String args = getArgsStr();
                return file.toString() + (args.isEmpty() ? " " : "(" + args + ")");
            } else {
                return statement;
            }
        }
    }

    protected void onExecQueueChanged(){
        if (getMainFrame()!=null){
            getMainFrame().onExecQueueChanged(getExecutionQueue());
        }
    }
    
    final List<ExecutionStage> executionQueue = new ArrayList<>();

    public void evalFileNext(File file) throws State.StateException {
        evalFileNext(file, null);
    }

    public void evalFileNext(File file, Map<String, Object> args) throws State.StateException {
        Context.getState().assertProcessing();        
        ExecutionStage stage = new ExecutionStage(file, args);
        synchronized (executionQueue) {
            logger.log(Level.INFO, "Next stage: {0}", stage.toString());
            executionQueue.add(stage);
        }
        onExecQueueChanged();
        getStatusBar().setTransitoryStatusMessage("Run next: " + file.toString(), 5000);
    }

    public void evalStatementNext(String statement) throws State.StateException {
        Context.getState().assertProcessing();
        ExecutionStage stage = new ExecutionStage(statement);
        synchronized (executionQueue) {
            logger.log(Level.INFO, "Next stage: {0}", stage.toString());
            executionQueue.add(stage);
        }
        onExecQueueChanged();
        getStatusBar().setTransitoryStatusMessage("Run next: " + statement, 5000);
    }

    public ExecutionStage[] getExecutionQueue() {
        synchronized (executionQueue) {
            return executionQueue.toArray(new ExecutionStage[0]);
        }
    }

    public void cancelExecutionStage(int index) {
        synchronized (executionQueue) {
            if ((index >= 0) & (index < executionQueue.size())) {
                logger.log(Level.INFO, "Cancel execution stage index: {0} - {1}", new Object[]{index, executionQueue.get(index).toString()});
                executionQueue.remove(index);
            }
        }
        onExecQueueChanged();
    }

    public void cancelExecutionQueue() {
        synchronized (executionQueue) {
            logger.info("Cancel execution queue");
            executionQueue.clear();
        }
        onExecQueueChanged();
    }

    public boolean hasNextStage() {
        synchronized (executionQueue) {
            return executionQueue.size() > 0;
        }
    }

    ExecutionStage popNextStage() {
        synchronized (executionQueue) {
            if (executionQueue.size() > 0) {
                return executionQueue.remove(0);
            }
            return null;
        }
    }

    volatile ExecutorTask currentStage;

    void checkNext() {
        if (hasNextStage()){
            State state = Context.getState();
            if ((state == State.Ready) && (currentTask == null) && (currentStage == null)) {
                ExecutionStage nextStage = popNextStage();
                if (nextStage != null) {
                    try {
                        onExecQueueChanged();
                        currentStage = new ExecutorTask(nextStage);
                        startTask(currentStage);
                    } catch (Exception ex) {
                        currentStage = null;
                        logger.log(Level.SEVERE, null, ex);
                    };
                }
            } else {
                SwingUtils.invokeDelayed(() -> {
                    checkNext();
                }, 100);
            }
        }
    }

    StatusBar statusBar;

    public StatusBar getStatusBar() {
        if (getMainFrame() != null) {
            return (StatusBar) getMainFrame().getStatusBar();
        }
        return statusBar;


    }
    
    //Helper methods to tasks
    OutputPanel outputPanel;
    
    protected void setOutputPanel(OutputPanel panel){
        outputPanel = panel;
    }
    

    public void sendOutput(String str) {
        if ((outputPanel != null) && (outputPanel.isDisplayable())) {
            outputPanel.putOutput(str);
        }
    }

    public void sendTaskInit(String task) {
        sendOutput(OutputPanel.getTaskInitMessage(task));
    }

    public void sendTaskFinish(String task) {
        sendOutput(OutputPanel.getTaskFinishMessage(task));
    }

    public void sendError(String str) {
        if ((outputPanel != null) && (outputPanel.isDisplayable())) {
            outputPanel.putError(str);
        }
    }
    

    volatile Task currentTask;

    public void startTask(Task task) throws State.StateException {
        task.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                if ("state".equals(propertyName)) {
                    StateValue state = (StateValue) (e.getNewValue());
                    switch (state) {
                        case STARTED:
                            break;
                        case DONE:
                            try {
                            if (getMainFrame() != null) {
                                getMainFrame().onTaskFinished(task);
                            }
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, null, ex);
                        }
                        task.removePropertyChangeListener(this);
                        if (task == currentStage) {
                            currentStage = null;
                        }
                        if (task == currentTask) {
                            currentTask = (currentStage != null) ? currentStage : null;
                        }
                        break;
                    }
                }
            }
        });
        if (task instanceof Restart) {
            getState().assertNot(State.Initializing);
            abort();
        } else {
            getState().assertReady();
        }
        currentTask = task;
        task.execute();
    }

    public Task getRunningTask() {
        return currentTask;
    }

    public void abort() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }
         
    public static DevicePanelManager getDevicePanelManager() {
        return DevicePanelManager.getInstance();
    }
    
    
    HashMap<String, PlotPanel> plotPanels = new HashMap<>();

    String checkPlotsTitle(String title) {
        if ((title == null) || (title.isEmpty())) {
            title = PlotListener.DEFAULT_PLOT_TITLE;
        }
        return title;
    }
    

    public PlotPanel getPlotPanel(String title, Window parent) {
        return getPlotPanel(title, parent, true);
    }

    public PlotPanel getPlotPanel(String title, Window parent, boolean create) {
        title = checkPlotsTitle(title);
        PlotPanel plotPanel = plotPanels.get(title);
        if ((plotPanel != null) && (!Setup.isOffscreenPlotting() && !plotPanel.isDisplayable())) {
            plotPanels.remove(title);
            plotPanel = null;
        }
        if ((plotPanel == null) && create) {
            plotPanel = new PlotPanel();
            plotPanels.put(title, plotPanel);
            if (!Setup.isOffscreenPlotting()) {
                JFrame frame = SwingUtils.showFrame(parent, title, new Dimension(600, 400), plotPanel);
                frame.setIconImage(Toolkit.getDefaultToolkit().getImage(ch.psi.pshell.app.App.getResourceUrl("IconSmall.png")));
            }
            plotPanel.initialize();
            plotPanel.setPlotTitle(title);
        }
        return plotPanel;
    }

    public List<String> getPlotPanels() {
        return new ArrayList(plotPanels.keySet());
    }

    public void removePlotPanel(String title) {
        title = checkPlotsTitle(title);
        if (!title.equals(checkPlotsTitle(null))) {
            plotPanels.remove(title);
        }
    }
    
    PlotterBinder pmb;
    
    void startPlotServerConnection(String url, int timeout) {
        Client pc = new Client(url, timeout);
        Plotter pm = pc.getProxy();
        pmb = new PlotterBinder(pm);
    }
    
    
    public boolean hasStatusBarMenu(){
        return Setup.isDetachedAppendStatusBar() || Setup.isPlotOnly();
    }     
    
    public static boolean isScanPlottingActive() {
        return !Setup.isServerMode() && !Setup.isScanPlottingDisabled();
    }
    
    public static boolean isScanPrintingActive() {
        return !Setup.isServerMode() && !Setup.isScanPrintingDisabled() && !Setup.isPlotOnly();
    }

    public static void setupScanPrinting() {
        boolean active = isScanPrintingActive();        
        ch.psi.pshell.framework.Console.setDefaultPrintScan(active);
        Shell.setDefaultPrintScan(active);
        if (getConsole() != null) {
            getConsole().setPrintScan(active);
        }
        if (getShell() != null) {
            getShell().setPrintScan(active);
        }
    }
    
    
    //

    public static Shell getShell() {
        if (getInstance() != null) {
            if (getInstance().shell != null) {
                return getInstance().shell;
            }
            if (getInstance().getMainFrame()!=null) {
                return getInstance().getMainFrame().getShell();
            }
        }
        return null;
    }

    public static Console getConsole() {
        if (getInstance() != null) {
            return (getInstance().console);
        }
        return null;
    }
        
    protected void startShell() {
        statusBar = new StatusBar();
        shell = new Shell();
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(shell, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        setupScanPrinting();
        shell.initialize();
        JFrame frame = new JFrame();
        frame.setIconImage(getIconSmall());
        frame.setTitle("PShell Console");
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        SwingUtils.centerComponent(null, frame);
        frame.setVisible(true);
        shell.requestFocus();
        setConsolePlotEnvironment(frame);
        setupConsoleScanPlotting();
    }    
    
    protected void startConsole(){
        try {
            console = new Console();
            setupScanPrinting();
            setupConsoleScanPlotting();
            if (hasCommandLineTask()){
                execCommandLineTasks();
            }                         
            console.run(System.in, System.out, !Setup.isServerMode());
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        } finally {
            console.close();
        }        
    }
    
    protected void setupConsoleScanPlotting() {
        try {
            PlotPanel scanPlot = new PlotPanel();
            plotPanels.put(checkPlotsTitle(null), scanPlot);
            scanPlot.initialize();
            scanPlot.setPlotTitle(checkPlotsTitle(null));

            Context.getInterpreter().addScanListener(new ScanListener() {
                final HashMap<Scan, String> plotTitles = new HashMap<>();

                @Override
                public void onScanStarted(Scan scan, String title) {
                    if (isScanPlottingActive()) {
                        title = checkPlotsTitle(title);
                        synchronized (plotTitles) {
                            plotTitles.put(scan, title);
                            PlotPanel plottingPanel = getPlotPanel(title, null, true);
                            plottingPanel.setPreferences(Context.getInterpreter().getPlotPreferences());
                            plottingPanel.triggerScanStarted(scan, title);
                        }
                    }
                }

                @Override
                public void onNewRecord(Scan scan, ScanRecord record) {
                    if (isScanPlottingActive()) {
                        PlotPanel plottingPanel = null;
                        synchronized (plotTitles) {
                            plottingPanel = getPlotPanel(plotTitles.get(scan), null, false);
                        }
                        if (plottingPanel != null) {
                            plottingPanel.triggerOnNewRecord(scan, record);
                        }
                    }
                }

                @Override
                public void onScanEnded(Scan scan, Exception ex) {
                    if (isScanPlottingActive()) {
                        PlotPanel plottingPanel = null;
                        synchronized (plotTitles) {
                            plottingPanel = getPlotPanel(plotTitles.get(scan), null, false);
                            plotTitles.remove(scan);
                        }
                        if (plottingPanel != null) {
                            plottingPanel.triggerScanEnded(scan, ex);
                        }
                    }
                }

            });
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }

    }
        
    protected void setConsolePlotEnvironment(Window parent) {        
        Context.getInterpreter().setPlotListener(new PlotListener() {
            @Override
            public List<Plot> plot(String title, PlotDescriptor[] plots) throws Exception {
                if (!SwingUtilities.isEventDispatchThread()){
                    return SwingUtils.invokeAndWait(() -> {
                        return plot(title, plots);
                    });
                }
                ArrayList<Plot> ret = new ArrayList<>();
                PlotPanel plotPanel = getPlotPanel(title, parent, true);
                plotPanel.clear();
                if ((plots != null) && (plots.length > 0)) {
                    for (PlotDescriptor plot : plots) {
                        try {
                            if (plot != null) {
                                ret.add(plotPanel.addPlot(plot));
                            } else {
                                ret.add(null);
                            }
                        } catch (Exception ex) {
                            if (plot == null) {
                            } else {
                                System.err.println("Error creating plot: " + String.valueOf((plot != null) ? plot.name : null));
                            }
                        }
                    }
                }
                return ret;
            }

            @Override
            public List<Plot> getPlots(String title) {
                title = checkPlotsTitle(title);
                PlotPanel plotPanel = plotPanels.get(title);
                if (plotPanel != null) {
                    return plotPanel.getPlots();
                }
                return new ArrayList<Plot>();
            }

            @Override
            public void onTitleClosed(String title) {
                if (Setup.isOffscreenPlotting()) {
                    removePlotPanel(title);
                    logger.log(Level.INFO, "Plot context closed: {0}", title);
                }
            }

            @Override
            public List<String> getTitles() {
                return new ArrayList(plotPanels.keySet());
            }
        });
    }
    
    
    public void runFile(File file) {
        logger.log(Level.INFO, "Run file: {0}", file.getPath());
        registerProcessors();
        ch.psi.pshell.framework.Console console =  (this.console==null) ? new ch.psi.pshell.framework.Console() : null;
        try {   
            Context.waitStateNot(State.Invalid,-1);
            if (Context.getState().isProcessing()){
                logger.log(Level.INFO, "Waiting for interpreter to be ready");
                Context.waitStateNotProcessing(-1);
            }
            setupScanPrinting();
            Object ret = evalFile(file, getInterpreterArgs(), true);

            if (ret != null) {
                System.out.println(ret);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.WARNING, null, ex);
        } finally{
            if (console!=null){
                console.close();
            }
        }
    }

    public void runStatement(String statement){
        Console console =  (this.console==null) ? new Console() : null;
        try {
            Context.waitStateNot(State.Invalid,-1);
            if (Context.getState().isProcessing()){
                logger.log(Level.INFO, "Waiting for interpreter to be ready");
                Context.waitStateNotProcessing(-1);
            }        
            Object ret = evalStatement(statement);
            if (ret != null) {
                System.out.println(ret);
            }   
            
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.WARNING, null, ex);
        } finally{
            if (console!=null){
                console.close();
            }
        }
    }    
     
    void registerProcessors() {
        Processor.addServiceProvider(ScanEditorPanel.class);
        Processor.addServiceProvider(QueueProcessor.class);
    }           
    
}
