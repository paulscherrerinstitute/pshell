package ch.psi.pshell.xscan;

import ch.psi.pshell.xscan.model.Configuration;
import ch.psi.pshell.xscan.model.Data;
import ch.psi.pshell.xscan.model.Recipient;
import ch.psi.pshell.xscan.model.Scan;
import ch.psi.pshell.xscan.plot.VDescriptor;
import ch.psi.pshell.xscan.plot.Visualizer;
import ch.psi.pshell.xscan.ui.ConfigurationPanel;
import ch.psi.pshell.xscan.ui.ModelUtil;
import ch.psi.jcae.ChannelService;
import ch.psi.jcae.impl.DefaultChannelService;
import ch.psi.pshell.core.CommandInfo;
import ch.psi.pshell.core.CommandSource;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ScriptStdio;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.ui.Preferences;
import ch.psi.pshell.ui.Processor;
import ch.psi.pshell.ui.Task;
import ch.psi.pshell.xscan.model.Variable;
import ch.psi.utils.EventBus;
import ch.psi.utils.IO;
import ch.psi.utils.State;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.CloseButtonTabComponent;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Loops can be executed from script as: ProcessorXScan().execute("test1.xml")
 */
public final class ProcessorXScan extends MonitoredPanel implements Processor {

    public static final String EXTENSION = "xml";
    public static final String SCAN_TYPE = "XScan";
    public static final String BROWSER_TITLE = "XScan Data";

    static {
        if (App.getInstance().getMainFrame() != null) {
            /*
            JMenuBar menu = App.getInstance().getMainFrame().getMenu();
            JMenu menuView = (JMenu) SwingUtils.getComponentByName(menu, "menuView");
            JCheckBoxMenuItem menuDataBrowser = new JCheckBoxMenuItem(BROWSER_TITLE);
            menuDataBrowser.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK));
            menuView.insert(menuDataBrowser, menuView.getMenuComponentCount() - 1);
            menuView.insertSeparator(menuView.getMenuComponentCount() - 1);
            menuDataBrowser.setSelected(false);
            menuDataBrowser.addActionListener((e) -> {
                if (isDataBrowserVisible()) {
                    closeDataBrowser();
                } else {
                    showDataBrowser();
                }
            });
            menuView.addChangeListener((e) -> {
                menuDataBrowser.setSelected(isDataBrowserVisible());
            });

            //TODO: On Mac, Java >10,  meta+B generates 2 events (with and without modifier)
            //https://bugs.openjdk.java.net/browse/JDK-8208712
            SwingUtils.adjustMacMenuBarAccelerator(menuDataBrowser);
             */
            try {
                if ((App.hasArgument("fda_browser")) //|| ("true".equalsIgnoreCase(Context.getInstance().getSetting("FdaBrowser")))
                        ) {
                    showDataBrowser();
                }
            } catch (Exception ex) {
                SwingUtils.showException(App.getInstance().getMainFrame(), ex);
            }
        }
    }

    public static boolean isDataBrowserVisible() {
        if (App.getInstance().getMainFrame() != null) {
            JTabbedPane tab = App.getInstance().getMainFrame().getLeftTab();
            for (int i = 0; i < tab.getTabCount(); i++) {
                Component c = tab.getComponentAt(i);
                if (c instanceof DataViewer) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void showDataBrowser() {
        if (App.getInstance().getMainFrame() != null) {
            JTabbedPane tab = App.getInstance().getMainFrame().getLeftTab();
            for (int i = 0; i < tab.getTabCount(); i++) {
                Component c = tab.getComponentAt(i);
                if (c instanceof DataViewer) {
                    tab.setSelectedComponent(c);
                    return;
                }
            }
            DataViewer panel = new DataViewer();
            panel.setCached(App.getInstance().getMainFrame().getPreferences().cachedDataPanel);
            panel.initialize(null);
            App.getInstance().getMainFrame().openComponent(BROWSER_TITLE, panel, tab);
            setDataBrowserFixed();
        }
    }

    static boolean isDataBrowser(Component c) {
        return ((c != null) && (c instanceof CloseButtonTabComponent) && ((CloseButtonTabComponent) c).getLabel().getText().equals(BROWSER_TITLE));
    }

    public static void setDataBrowserFixed() {
        if (App.getInstance().getMainFrame() != null) {
            if (App.getInstance().getMainFrame().getLeftTab().isVisible()) {
                for (int i = 0; i < App.getInstance().getMainFrame().getLeftTab().getTabCount(); i++) {
                    Component t = App.getInstance().getMainFrame().getLeftTab().getTabComponentAt(i);
                    if (isDataBrowser(t)) {
                        CloseButtonTabComponent tab = (CloseButtonTabComponent) t;
                        tab.getButton().setVisible(false);
                        for (MouseListener l : tab.getLabel().getMouseListeners()) {
                            tab.getLabel().removeMouseListener(l);
                        }
                    }
                }
            }
        }
    }

    public static void closeDataBrowser() {
        if (App.getInstance().getMainFrame() != null) {
            for (int i = 0; i < App.getInstance().getMainFrame().getLeftTab().getTabCount(); i++) {
                Component t = App.getInstance().getMainFrame().getLeftTab().getTabComponentAt(i);
                if (isDataBrowser(t)) {
                    App.getInstance().getMainFrame().getLeftTab().removeTabAt(i);
                }
            }
        }
    }

    private ConfigurationPanel panelConfig;
    final AcquisitionConfiguration configuration;
    final String homePath;

    public ProcessorXScan() {
        initComponents();
        configuration = new AcquisitionConfiguration();
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane1.getHorizontalScrollBar().setUnitIncrement(16);
        if ((App.getInstance() != null) && (App.hasArgument("xpath"))) {
            homePath = Context.getInstance().getSetup().expandPath(App.getArgumentValue("xpath"));
        } else {
            homePath = Context.getInstance().getSetup().getXScanPath();
        }
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
        Processor p = App.getInstance().getMainFrame().getSelectedProcessor();
        if ((p != null) && (p instanceof ProcessorXScan)) {
            return (ProcessorXScan) p;
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
            throw (ex instanceof IOException) ? (IOException) ex : new IOException(ex);
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
                case Yes:
                    save();
                    break;
                case No:
                    break;
                case Cancel:
                    return false;
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
            CommandInfo info = Context.getInstance().startExecution(CommandSource.ui, file, null, true);
            String msg = "Running " + getFileName();
            try {
                setEnabled(false);
                setMessage(msg);
                setProgress(0);
                App.getInstance().sendTaskInit(msg);
                doExecution(false, null);
                setProgress(100);
                return msg;
            } catch (InterruptedException ex) {
                App.getInstance().sendOutput("Execution aborted");
                throw ex;
            } catch (Exception ex) {
                App.getInstance().sendError(ex.toString());
                if (App.getInstance().getMainFrame().getPreferences().getScriptPopupDlg() != Preferences.ScriptPopupDialog.None) {
                    if (!Context.getInstance().isAborted()) {
                        SwingUtils.showMessage(App.getInstance().getMainFrame(), "Script Error", ex.getMessage(), -1, JOptionPane.ERROR_MESSAGE);
                    }
                }
                throw ex;
            } finally {
                App.getInstance().sendTaskFinish(msg);
                Context.getInstance().endExecution(info);
                setEnabled(true);
            }
        }

        protected void _setProgress(int progress) {
            setProgress(progress);
        }
    }

    @Override
    public boolean isExecuting() {
        return !isEnabled();
    }

    public static void setProgress(int progress) {
        if (App.getInstance() != null) {
            Task task = App.getInstance().getRunningTask();
            if ((task != null) && (task instanceof XscanTask)) {
                ((XscanTask) task)._setProgress(progress);
            }
        }
    }

    public static void setPlots(List<JPanel> plots, String title) {
        PlotPanel panel;
        if (App.getInstance().getMainFrame() != null) {
            panel = App.getInstance().getMainFrame().getPlotPanel(title);
        } else {
            panel = App.getInstance().getPlotPanel(title, null);
        }

        panel.clear();
        for (JPanel plot : plots) {
            panel.addPlot((PlotBase) plot);
        }
        if (App.getInstance().getMainFrame() != null) {
            App.getInstance().getMainFrame().getPlotsTab().setSelectedComponent(panel);
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
        App.getInstance().startTask(new XscanTask(getFileName(), getConfig()));
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

        //if (executionThread != null) {
        //    executionThread.interrupt();
        //}        
        //if (App.getInstance() != null){
        //    App.getInstance().abort();
        //}
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
        return App.getInstance().getMainFrame().getPreferences().showXScanFileBrowser || App.hasArgument("xscan_panel");
    }

    public AcquisitionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void execute(String file, Map<String, Object> vars) throws Exception {
        if (!new File(file).exists()) {
            file = Paths.get(getHomePath(), file).toString();
        }
        if (!new File(file).exists()) {
            throw new IOException("Invalid file: " + file);
        }
        open(file, false);

        State initialState = Context.getInstance().getState();
        CommandInfo info = null;
        if (initialState == State.Ready) {
            info = Context.getInstance().startExecution(CommandSource.terminal, file, null, false);
        }
        try {
            doExecution(!App.isGui(), vars);
        } catch (Exception ex) {
            App.getInstance().sendError(ex.toString());
            throw ex;
        } finally {
            if (initialState == State.Ready) {
                Context.getInstance().endExecution(info);
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
                if ((value instanceof String) && ((String) value).startsWith("=")) {
                    value = eval(((String) value).substring(1).trim());
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
            EventBus ebus = new EventBus(AcquisitionConfiguration.eventBusModeAcq);
            acquisition = new Acquisition(channelService, getConfiguration(), vars);
            App.getInstance().getMainFrame().updateButtons();   //Update pause button state

            injectVariables();

            acquisition.initalize(ebus, getConfig());
            if (!batch && App.isScanPlottingActive()) {
                visualizer = new Visualizer(Acquisition.mapVisualizations(getConfig().getVisualization()));
                /*
                if (config.getScan() != null && config.getScan().getCdimension() != null) {
                    // If there is a continuous dimension only update the plot a the end of a line.
                    // Improvement of performance
                    visualizer.setUpdateAtStreamElement(false);
                    visualizer.setUpdateAtStreamDelimiter(true);
                    visualizer.setUpdateAtEndOfStream(false);
                }
                 */
                ebus.register(visualizer);
                setPlots(visualizer.getPlotPanels(), null);
            }
            if (Context.getInstance().getState() == State.Paused) {
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
                Logger
                        .getLogger(ProcessorXScan.class
                                .getName()).log(Level.FINER, "Destroy acquisition");
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
                    if (ProcessorXScan.SCAN_TYPE.toString().equalsIgnoreCase(String.valueOf(dm.getAttribute(file.getAbsolutePath(), path, Layout.ATTR_TYPE)))) {
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
        boolean deserializerTXT = (path == null);
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
                deserializerTXT = false;
            } else {
                cf = Paths.get(dir.toString(), name, name + "." + EXTENSION).toFile();
                if (file.isDirectory() && cf.exists()) {
                    //Reading with DataPanel inside a dolder generated with text serializer
                    cfile = cf;
                    file = new File(file, path + ".txt");
                    deserializerTXT = true;
                } else {
                    throw new IllegalArgumentException("Configuration file [" + cfile.getAbsolutePath() + "] does not exist");
                }
            }
        }

        EventBus ebus = new EventBus(AcquisitionConfiguration.eventBusModePlot);
        Deserializer deserializer = deserializerTXT ? new DeserializerTXT(ebus, file) : new DeserializerPShell(ebus, file, path, dm);

        Configuration c = load(cfile);
        injectVariables(c);
        VDescriptor vdescriptor = Acquisition.mapVisualizations(c.getVisualization());

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
                    SwingUtils.showMessage(App.getInstance().getMainFrame(), "Error", "An error occured while visualizing '" + filename + "':\n" + ex.getMessage());
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

    private static ScriptEngine engine;

    private static void createPrivateEngine() {
        if (engine == null) {
            engine = new ScriptEngineManager().getEngineByName("python");

            if (engine == null) {
                Logger.getLogger(ProcessorXScan.class
                        .getName()).severe("Error instantiating script engine in XScan");
                throw new RuntimeException("Error instantiating script engine in XScan");
            }

            try {
                ScriptStdio stdio = new ScriptStdio(engine);
                Context.getInstance().addScriptStdio(stdio);
            } catch (Exception ex) {
                engine.getContext().setWriter(new PrintWriter(System.out));
                engine.getContext().setErrorWriter(new PrintWriter(System.err));
            }
        }
    }

    public static Object eval(String script) throws Exception {
        if (Context.getInstance().isInterpreterEnabled()) {
            return Context.getInstance().evalLineBackground(script);
        } else {
            createPrivateEngine();
            return engine.eval(script);
        }
    }

    public static void setInterpreterVariable(String name, Object value) {
        if (Context.getInstance().isInterpreterEnabled()) {
            Context.getInstance().setInterpreterVariable(name, value);
        } else {
            createPrivateEngine();
            engine.put(name, value);
        }
    }

    public static Object getInterpreterVariable(String name) {
        if (Context.getInstance().isInterpreterEnabled()) {
            return Context.getInstance().getInterpreterVariable(name);
        } else {
            createPrivateEngine();
            return engine.get(name);
        }
    }

    public static boolean isSimpleCodeEditor() {
        return App.getInstance().getMainFrame().getPreferences().simpleEditor;
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
                if (App.getInstance().getMainFrame().getDocumentsTab().getSelectedComponent() == this) {
                    int index = App.getInstance().getMainFrame().getDocumentsTab().getSelectedIndex();
                    JTabbedPane tabDoc = App.getInstance().getMainFrame().getDocumentsTab();
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
