package ch.psi.pshell.xscan;

import ch.psi.jcae.ChannelService;
import ch.psi.jcae.impl.DefaultChannelService;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.FormatFDA;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.MainFrame;
import ch.psi.pshell.framework.Processor;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.framework.Task;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.PlotPanel;
import ch.psi.pshell.sequencer.CommandInfo;
import ch.psi.pshell.sequencer.CommandSource;
import ch.psi.pshell.sequencer.ScriptStdio;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.SwingUtils.OptionType;
import ch.psi.pshell.utils.EventBus;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.xscan.model.Configuration;
import ch.psi.pshell.xscan.model.Data;
import ch.psi.pshell.xscan.model.Recipient;
import ch.psi.pshell.xscan.model.Scan;
import ch.psi.pshell.xscan.model.Variable;
import ch.psi.pshell.xscan.plot.VDescriptor;
import ch.psi.pshell.xscan.plot.Visualizer;
import ch.psi.pshell.xscan.ui.ConfigurationPanel;
import ch.psi.pshell.xscan.ui.ModelUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Loops can be executed from script as: ProcessorXScan().execute("test1.xml")
 */
public final class ProcessorXScan extends MonitoredPanel implements Processor {

    public static final String EXTENSION = "xml";
    public static final String SCAN_TYPE = "XScan";
    public static final String BROWSER_TITLE = "XScan Data";
    
    public static String getScriptPath(){
        return Options.XSCAN_PATH.getString("{script}");
    }

    private ConfigurationPanel panelConfig;
    final Config configuration;
    final String homePath;
        
    public ProcessorXScan() {
        initComponents();
        configuration = new Config();
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane1.getHorizontalScrollBar().setUnitIncrement(16);
        homePath = getScriptPath();
    }

    public ProcessorXScan(File file) throws IOException {
        this();
        if (file != null) {
            open(file.getAbsolutePath());
        }
        changed = false;
        ModelUtil.getInstance().clearModified();
    }

    public static ProcessorXScan getCurrent() {
        Processor p = Context.getView().getSelectedProcessor();
        if (p instanceof ProcessorXScan processorXScan) {
            return processorXScan;
        }
        return null;
    }

    public ConfigurationPanel getConfigPanel() {
        return panelConfig;
    }

    @Override
    public String getType() {
        return "XScan";
    }

    @Override
    public String getDescription() {
        return "XScan configuration file  (*." + EXTENSION + ")";
    }
    
    public String getScanType(){
        return SCAN_TYPE;
    }
    

    @Override
    public String[] getExtensions() {
        return new String[]{EXTENSION};
    }

    @Override
    public boolean createMenuNew() {
        return true;
    }

    @Override
    public void open(String fileName) throws IOException {
        open(fileName, true);
    }

    void open(String fileName, boolean showConfig) throws IOException {
        if (fileName == null) {
            if (!showConfig) {
                throw new IOException("Undefined file name");
            }
            setFile(null);
            config = new Configuration();
            config.setScan(new Scan());
        } else {
            File file = new File(fileName);
            if (!file.getName().endsWith("." + EXTENSION)) {
                file = new File(file.getAbsolutePath() + "." + EXTENSION);
            }
            setFile(file);
            try {
                if (!file.exists()) {
                    if (!showConfig) {
                        throw new IOException("Invalid file name: " + file);
                    }
                    //New file
                    config = new Configuration();
                    config.setScan(new Scan());
                    ModelManager.marshall(config, file);
                } else {
                    config = load(file);
                }
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        if (showConfig) {
            this.jPanel1.removeAll();
            this.panelConfig = new ConfigurationPanel(config);
            this.jPanel1.add(panelConfig, BorderLayout.CENTER);
            this.jPanel1.revalidate();
            this.jPanel1.repaint();
        }
    }

    public Configuration getConfig() {
        if (panelConfig != null) {
            return panelConfig.getObject();
        }
        return config;
    }

    File file;

    private void setFile(File f) {
        this.file = f;
        setName((f==null) ? "Unknown": IO.getPrefix(f.getName()));
    }

    @Override
    public JPanel getPanel() {
        //Creating new
        if (panelConfig == null) {
            try{
                open(null);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return Processor.super.getPanel();
    }

    @Override
    public void save() throws IOException {
        File copy = null;
        try {
            if (this.file != null) {
                if (!file.canWrite()) {
                    if (!hasChanged()) {
                        return;
                    } else {
                        throw new IOException(file.getName() + " is read-only");
                    }
                }
                copy = new File(file.getAbsolutePath() + ".tmp");
                IO.copy(file.getAbsolutePath(), copy.getAbsolutePath());
            }
            ModelManager.marshall(getConfig(), file);
            changed = false;
            ModelUtil.getInstance().clearModified();

        } catch (Exception ex) {
            if (copy != null) {
                file.delete();
                IO.copy(copy.getAbsolutePath(), file.getAbsolutePath());
                open(file.getAbsolutePath());
            }
            throw (ex instanceof IOException iex) ? iex: new IOException(ex);
        } finally {
            if (copy != null) {
                copy.delete();
            }
        }
    }

    @Override
    public void saveAs(String fileName) throws IOException {
        Configuration model = getConfig();

        try {
            File f = new File(fileName);
            ModelManager.marshall(model, f);
            setFile(f);
            changed = false;
            ModelUtil.getInstance().clearModified();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String getHomePath() {
        return homePath;
    }

    boolean changed = false;

    @Override
    public boolean hasChanged() {
        changed = changed || ModelUtil.getInstance().wasModified();
        return changed;
    }

    @Override
    public boolean checkChangeOnClose() throws IOException {
        if (hasChanged() /*&& (!isReadOnly())*/) {
            switch (SwingUtils.showOption(this, "Closing", "Document has changed. Do you want to save it?", OptionType.YesNoCancel)) {
                case Yes -> save();
                case No -> {
                    return true;
                }
                case Cancel -> {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String getFileName() {
        return (file != null) ? file.getPath() : null;
    }

    @Override
    public void setEnabled(boolean value) {
        boolean hadChanged = hasChanged();  //setEnabled trigger JComboBox valueChange...
        super.setEnabled(value);
        for (Component c : SwingUtils.getComponentsByType(panelConfig, Component.class)) {
            c.setEnabled(value);
        }
        if (!hadChanged) {
            changed = false;
            ModelUtil.getInstance().clearModified();
        }
    }

    class XscanTask extends Task {

        final String file;
        final Configuration configuration;

        public XscanTask(String file, Configuration configuration) {
            super();
            this.file = file;
            this.configuration = configuration;
        }

        @Override
        protected String doInBackground() throws Exception {
            CommandInfo info = null;
            String msg = "Running " + getFileName();
            try {
                info = Context.getInterpreter().startExecution(CommandSource.ui, file, null, true);                
                setEnabled(false);
                setMessage(msg);
                setProgress(0);
                Context.getApp().sendTaskInit(msg);
                doExecution(false, null);
                setProgress(100);
                return msg;
            } catch (InterruptedException ex) {
                Context.getApp().sendOutput("Execution aborted");
                throw ex;
            } catch (Exception ex) {
                ex.printStackTrace(); //!!!
                Context.getApp().sendError(ex.toString());
                if (Context.getView().getScriptPopupMode() != MainFrame.ScriptPopupMode.None) {
                    if (!Context.getInterpreter().isAborted()) {
                        SwingUtils.showMessage(Context.getView(), "Script Error", ex.getMessage(), -1, JOptionPane.ERROR_MESSAGE);
                    }
                }
                throw ex;
            } finally {
                Context.getApp().sendTaskFinish(msg);
                Context.getInterpreter().endExecution(info);
                setEnabled(true);
            }
        }

        protected void _setProgress(int progress) {
            setProgress(progress);
        }
    }

    @Override
    public boolean isExecuting() {
        return !isEnabled() || (Setup.isPlotOnly() && Context.getState().isProcessing());
    }

    public static void setProgress(int progress) {
        if (Context.getApp() != null) {
            Task task = Context.getApp().getRunningTask();
            if (task instanceof XscanTask xscanTask) {
                xscanTask._setProgress(progress);
            }
        }
    }

    public static void setPlots(List<JPanel> plots, String title) {
        PlotPanel panel;
        if (Context.getView() != null) {
            panel = Context.getView().getPlotPanel(title, true);
        } else {
            panel = Context.getApp().getPlotPanel(title, null);
        }

        panel.clear();
        for (JPanel plot : plots) {
            panel.addPlot((PlotBase) plot);
        }
        if (Context.getView() != null) {
            Context.getView().getPlotsTab().setSelectedComponent(panel);
        }
        panel.validate();
        panel.repaint();
    }

    public static void setIcon(JLabel c, URL url) {
        try {
            if (MainFrame.isDark()) {
                c.setIcon(new ImageIcon(SwingUtils.invert(Toolkit.getDefaultToolkit().getImage(url))));
            } else {
                c.setIcon(new ImageIcon(url));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void setIcon(JButton c, URL url) {
        try {
            if (MainFrame.isDark()) {
                c.setIcon(new ImageIcon(SwingUtils.invert(Toolkit.getDefaultToolkit().getImage(url))));
            } else {
                c.setIcon(new ImageIcon(url));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void execute() throws Exception {
        Context.getApp().startTask(new XscanTask(getFileName(), getConfig()));
    }

    @Override
    public void abort() throws InterruptedException {
        if (acquisition != null) {
            try {
                acquisition.abort();

            } catch (Exception e) {
                Logger.getLogger(ProcessorXScan.class
                        .getName()).log(Level.WARNING, null, e);
            }
        }
    }

    @Override
    public boolean canPause() {
        return (acquisition != null) && (!acquisition.canPause());
    }

    @Override
    public void pause() {
        if (acquisition != null) {
            try {
                acquisition.pause();

            } catch (Exception e) {
                Logger.getLogger(ProcessorXScan.class
                        .getName()).log(Level.WARNING, null, e);
            }
        }
    }

    public void resume() {
        if (acquisition != null) {
            try {
                acquisition.resume();

            } catch (Exception e) {
                Logger.getLogger(ProcessorXScan.class
                        .getName()).log(Level.WARNING, null, e);
            }
        }
    }

    @Override
    public boolean createFilePanel() {
        //!!! Align preferences x command line names
        return Context.getView().getPreference("showXScanFileBrowser")==Boolean.TRUE || App.hasAdditionalArgument("xscan_panel");
    }

    public  Config getConfiguration() {
        return configuration;
    }

    @Override
    public void execute(String file, Map<String, Object> vars) throws Exception {
        file = Setup.expandPath(file);
        if (!new File(file).exists()) {
            file = Paths.get(getHomePath(), file).toString();
        }
        file = Setup.expandPath(file);
        if (!new File(file).exists()) {
            throw new IOException("Invalid file: " + file);
        }
        open(file, false);

        State initialState = Context.getInterpreter().getState();
        CommandInfo info = null;
        if (initialState == State.Ready) {
            info = Context.getInterpreter().startExecution(CommandSource.terminal, file, null, false);
        }
        try {
            doExecution(!Setup.isGui(), vars);
        } catch (Exception ex) {
            Context.getApp().sendError(ex.toString());
            throw ex;
        } finally {
            if (initialState == State.Ready) {
                Context.getInterpreter().endExecution(info);
            }
        }
    }

    public void startExecute(String file, Map<String, Object> vars) throws Exception {
        new Thread(() -> {
            try {
                execute(file, vars);

            } catch (Exception ex) {
                Logger.getLogger(ProcessorXScan.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    public void testNotification(String recipient) {
        NotificationAgent a = new NotificationAgent();
        Recipient r = new Recipient();
        r.setValue(recipient);
        r.setError(true);
        r.setSuccess(true);
        a.getRecipients().add(r);
        a.sendNotification("Test Notification", "This is a test notification", true, true);
    }

    Thread executionThread;
    Acquisition acquisition;
    Configuration config;

    public Map<String, Object> getVariables() {
        return getVariables(getConfig());
    }

    public Map<String, Object> getVariables(Configuration model) {
        Map<String, Object> variables = new LinkedHashMap<>();
        for (Variable v : model.getVariable()) {
            variables.put(v.getName(), v.getValue());
        }
        return variables;
    }

    void injectVariables() {
        injectVariables(getConfig());
    }

    void injectVariables(Configuration model) {
        for (Map.Entry<String, Object> entry : getVariables(model).entrySet()) {
            try {
                Object value = entry.getValue();
                if ((value instanceof String str) && ((String) value).startsWith("=")) {
                    value = eval(str.substring(1).trim());
                }
                setInterpreterVariable(entry.getKey(), value);

            } catch (Exception ex) {
                Logger.getLogger(ProcessorXScan.class
                        .getName()).severe("Error setting variable " + entry.getKey() + ": " + ex.getMessage());
            }
        }
    }

    void doExecution(boolean batch, Map<String, Object> vars) throws Exception {
        executionThread = Thread.currentThread();
        //System.gc();
        ChannelService channelService = new DefaultChannelService();
        Visualizer visualizer = null;
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            setProgress(25);
            ModelUtil.getInstance().setConfigurationPanel(panelConfig);
            EventBus ebus = new EventBus(Config.eventBusModeAcq);
            acquisition = new Acquisition(channelService, getConfiguration(), vars);
            if (Context.getView()!=null){
                Context.getView().updateViewState();   //Update pause button state
            }

            injectVariables();

            acquisition.initalize(ebus, getConfig());
            if (!batch /* && App.isScanPlottingActive() //!!! needed?*/) {
                visualizer = new Visualizer(Acquisition.mapVisualizations(getConfig().getVisualization()));
                
                if (!getConfiguration().isContinuousUpdate()){
                    if (config.getScan() != null && config.getScan().getCdimension() != null) {
                        // If there is a continuous dimension only update the plot a the end of a line.
                        // Improvement of performance
                        visualizer.setUpdateAtStreamElement(false);
                        visualizer.setUpdateAtStreamDelimiter(true);
                        visualizer.setUpdateAtEndOfStream(false);
                    }
                }
                 
                ebus.register(visualizer);
                setPlots(visualizer.getPlotPanels(), null);
            }
            if (Context.getInterpreter().getState() == State.Paused) {
                acquisition.pause();
            }
            acquisition.execute();
        } catch (InterruptedException ex) {
            throw ex;

        } catch (Throwable t) {
            Logger.getLogger(ProcessorXScan.class
                    .getName()).log(Level.WARNING, null, t);
            t.printStackTrace();
            throw t;
        } finally {
            try {
                ModelUtil.getInstance().setConfigurationPanel(null);
                Logger.getLogger(ProcessorXScan.class.getName()).log(Level.FINER, "Destroy acquisition");
                if (acquisition != null) {
                    acquisition.destroy();
                    acquisition = null;

                }
            } catch (Exception e) {
                Logger.getLogger(ProcessorXScan.class.getName()).log(Level.FINER, "Unable to destroy acquisition", e);
            }
            Logger.getLogger(ProcessorXScan.class.getName()).log(Level.FINER, "Destroy channel service");
            try {
                channelService.destroy();
            } catch (Exception e) {
                Logger.getLogger(ProcessorXScan.class.getName()).log(Level.FINER, "Unable to destroy channel access service", e);
            }

            Logger.getLogger(ProcessorXScan.class.getName()).log(Level.FINER, "Stop visualizer");

            //TODO: giving more time to gert latest events. Is there a better way to know the event bus is empty?
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                    }
                    try {
                        executor.shutdown();

                    } catch (Exception e) {
                        Logger.getLogger(ProcessorXScan.class.getName()).log(Level.FINER, "Unable to stop executor service", e);
                    }
                }
            }).start();
            executionThread = null;
        }
    }

    @Override
    public boolean isPlottable(File file, String path, DataManager dm) {
        try {
            if ((file != null) && file.exists()) {
                if (path != null) {
                    //Within data root
                    if (SCAN_TYPE.equalsIgnoreCase(String.valueOf(dm.getAttribute(file.getAbsolutePath(), path, Layout.ATTR_TYPE)))) {
                        return true;
                    }
                    file = Paths.get(file.toString(), path + ".txt").toFile();
                }

                //Isolated file
                if (file.exists() && file.isFile()) {
                    if (IO.getExtension(file).equals("txt")) {
                        File dir = file.getParentFile();
                        String name = file.getName();
                        name = name.replaceAll("_[0-9]*.txt$", "");
                        name = name.replaceAll(".txt$", "");
                        File cfile = new File(dir, name + "." + EXTENSION);
                        if (cfile.exists()) {
                            return true;
                        } else {
                            cfile = new File(dir.toString() + "." + EXTENSION);
                            if (cfile.exists()) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
        return false;
    }

    @Override
    public void plotDataFile(File file, String path, DataManager dm) {        
        // Try to determine configuration file from data file name
        // 
        //boolean deserializerTXT = (path == null);
        Map<String, Object> info = dm.getInfo(file.toString(), path);
        boolean deserializerTXT = info.containsKey(FormatFDA.INFO_FIELD_DIMENSIONS); // File generated with XScan persistence
        File dir = file.getParentFile();
        String name = file.getName();
        name = name.replaceAll("_[0-9]*.txt$", "");
        String ext = IO.getExtension(name);
        if (!ext.isBlank()) {
            name = name.replaceAll("." + IO.getExtension(name) + "$", "");
        }
        File cfile = new File(dir, name + "." + EXTENSION);

        // Check existence of files 
        if (!file.exists()) {
            throw new IllegalArgumentException("Data file [" + file.getAbsolutePath() + "] does not exist");
        }
        if (!cfile.exists()) {
            File cf = new File(dir.toString() + "." + EXTENSION);
            if (cf.exists()) {
                //Reading a text file in file browser generated with PShell setializer
                cfile = cf;
            } else {
                cf = Paths.get(dir.toString(), name, name + "." + EXTENSION).toFile();
                if (!cf.exists()){
                    //Reading with DataPanel inside a folder generated with text serializer
                    cf = Paths.get(dir.toString(), name + "." + EXTENSION).toFile();
                }
                if (!cf.exists()){
                    //Reading with DataPanel inside a folder generated in flat storage
                    //Remove the scan index
                    String prefix = path;
                    int lastUnderscoreIndex = prefix.lastIndexOf('_');
                    if (lastUnderscoreIndex != -1) {
                        String lastToken = prefix.substring(lastUnderscoreIndex + 1);
                        if (lastToken.matches("\\d+") && lastToken.length() >=4) {
                            prefix = prefix.substring(0, lastUnderscoreIndex);
                        }
                    }                    
                    cf = Paths.get(file.toString() , prefix + "." + EXTENSION).toFile();
                } 
                if (file.isDirectory() && cf.exists()) {                    
                    cfile = cf;
                    file = new File(file, path + ".txt");
                    path = "/";
                } else {
                    throw new IllegalArgumentException("Configuration file [" + cfile.getAbsolutePath() + "] does not exist");
                }
            }
        }

        EventBus ebus = new EventBus(Config.eventBusModePlot);
        Deserializer deserializer = deserializerTXT ? new DeserializerTXT(ebus, file) : new DeserializerPShell(ebus, file, path, dm);

        Configuration c = load(cfile);
        VDescriptor vdescriptor;
        synchronized(interpreterLock){
            try{
                setPlottingInterpreter(true);
                injectVariables(c);
                vdescriptor = Acquisition.mapVisualizations(c.getVisualization());
            } finally{
                setPlottingInterpreter(false);
            }
        }

        Visualizer visualizer = new Visualizer(vdescriptor);
        visualizer.setUpdateAtStreamElement(false);
        visualizer.setUpdateAtStreamDelimiter(false);
        visualizer.setUpdateAtEndOfStream(true);

        ebus.register(visualizer);

        //tc.updatePanel(visualizer.getPlotPanels());
        ProcessorXScan.setPlots(visualizer.getPlotPanels(), "Data");

        Logger.getLogger(ProcessorXScan.class.getName()).log(Level.INFO, "Visualize file: {0}", file.getAbsolutePath());
        String filename = file.getName();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    deserializer.read();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtils.showMessage(Context.getView(), "Error", "An error occured while visualizing '" + filename + "':\n" + ex.getMessage());
                }
            }
        });
        t.start();
    }

    public static boolean isFdaSerializationFolder(File file) {
        if ((file != null) && (file.isDirectory())) {
            if (new File(file, file.getName() + ".xml").isFile()) {
                if (new File(file, file.getName() + ".xml").isFile()) {
                    if (new File(file, file.getName() + ".xml").isFile()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static Configuration load(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + file.getAbsolutePath() + " does not exist");
        }

        Configuration c;
        try {
            c = ModelManager.unmarshall(file);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Unable to deserialize configuration: " + e.getMessage(), e);
        }

        // Set data file name
        // Determine name used for the data file
        String name = file.getName();
        name = name.replaceAll("\\." + EXTENSION + "$", "");

        // Check if scan name exists, if not, replace with name of the file
        if (c.getData() == null) {
            c.setData(new Data());
        }
        if (c.getData().getFileName() == null || c.getData().getFileName().trim().equals("")) {
            //c.getData().setFileName(dataObject.getPrimaryFile().getName());
            c.getData().setFileName(file.getName());
        }

        // Fix configuration if iterations is specified with 0 and no iterations option is specified
        if (c.getNumberOfExecution() == 0) {
            c.setNumberOfExecution(1);
        }
        return c;
    }

    private static ScriptEngine privateEngine;
    private static ScriptEngine plottingEngine;
    static final Object interpreterLock = new Object();


    private static ScriptEngine createEngine() {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");

        if (engine == null) {
            Logger.getLogger(ProcessorXScan.class
                    .getName()).severe("Error instantiating script engine in XScan");
            throw new RuntimeException("Error instantiating script engine in XScan");
        }

        try {
            ScriptStdio stdio = new ScriptStdio(engine);
            Context.getInterpreter().addScriptStdio(stdio);
        } catch (Exception ex) {
            engine.getContext().setWriter(new PrintWriter(System.out));
            engine.getContext().setErrorWriter(new PrintWriter(System.err));
        }      
        return engine;
    }
    
    static ScriptEngine getPrivateEngine() {
        if (privateEngine == null) {
            privateEngine =createEngine();
        }
        return privateEngine;
    }

    static ScriptEngine getPlottingEngine() {
        if (plottingEngine == null) {
            plottingEngine =createEngine();
        }
        return plottingEngine;
    }        
    
    static boolean plottingInterpreter;
    
    static void setPlottingInterpreter(boolean value){
        plottingInterpreter = value;
    }
          
    public static Object eval(String script) throws Exception {
        synchronized(interpreterLock){
            if (plottingInterpreter){
                return getPlottingEngine().eval(script);            
            } else if (Context.getInterpreter().isInterpreterEnabled()) {
                return Context.getInterpreter().evalLineBackground(script);
            } else {
                return getPrivateEngine().eval(script);
            }
        }
    }

    public static void setInterpreterVariable(String name, Object value) {
        synchronized(interpreterLock){
            if (plottingInterpreter){
                getPlottingEngine().put(name, value);     
            } else if (Context.getInterpreter().isInterpreterEnabled()) {
                Context.getInterpreter().setInterpreterVariable(name, value);
            } else {
                getPrivateEngine().put(name, value);
            }
        }
    }

    public static Object getInterpreterVariable(String name) {
        synchronized(interpreterLock){
            if (plottingInterpreter){
                return getPlottingEngine().get(name);   
            } else if (Context.hasInterpreter()) {
                return Context.getInterpreter().getInterpreterVariable(name);
            } else {
                return getPrivateEngine().get(name);
            }
        }
    }

    public static boolean isSimpleCodeEditor() {
        return false ; //!!! todo return App.getInstance().getMainFrame().getPreferences().simpleEditor;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                formMouseExited(evt);
            }
        });

        jPanel1.setLayout(new java.awt.BorderLayout());
        jScrollPane1.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 499, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseExited
        if (hasChanged()) {
            try {
                if (Context.getView().getDocumentsTab().getSelectedComponent() == this) {
                    int index = Context.getView().getDocumentsTab().getSelectedIndex();
                    JTabbedPane tabDoc = Context.getView().getDocumentsTab();
                    tabDoc.setTitleAt(index, new File(getFileName()).getName() + "*");
                    SwingUtils.CloseButtonTabComponent tabComponent = (SwingUtils.CloseButtonTabComponent) tabDoc.getTabComponentAt(index);
                    tabComponent.updateUI();
                }
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }//GEN-LAST:event_formMouseExited

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

}
