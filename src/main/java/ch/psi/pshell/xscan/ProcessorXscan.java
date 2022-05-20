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
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.ui.Preferences;
import ch.psi.pshell.ui.Processor;
import ch.psi.pshell.ui.Task;
import ch.psi.pshell.ui.View;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Loops can be executed from script as: ProcessorXScan().execute("test1.xml")
 */
public final class ProcessorXscan extends MonitoredPanel implements Processor {

    public static final String SCAN_TYPE = "XScan";
    public static final String BROWSER_TITLE = "Xscan Browser";

    static {
        View view = App.getInstance().getMainFrame();
        if (view != null) {
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
            try {
                if ((App.hasArgument("fda_browser"))
                        || ("true".equalsIgnoreCase(Context.getInstance().getSetting("FdaBrowser")))) {
                    showDataBrowser();
                }
            } catch (IOException ex) {
            }
        }
    }

    public static boolean isDataBrowserVisible() {
        if (App.getInstance().getMainFrame() != null) {
            JTabbedPane tab = App.getInstance().getMainFrame().getLeftTab();
            for (int i = 0; i < tab.getTabCount(); i++) {
                Component c = tab.getComponentAt(i);
                if (c instanceof DataBrowser) {
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
                if (c instanceof DataBrowser) {
                    tab.setSelectedComponent(c);
                    return;
                }
            }
            DataBrowser panel = new DataBrowser();
            panel.setCached(App.getInstance().getMainFrame().getPreferences().cachedDataPanel);
            panel.initialize(null);
            App.getInstance().getMainFrame().openComponent(BROWSER_TITLE, panel, tab);
            if (!App.isLocalMode()) {
                try {
                    Context.getInstance().setSetting("FdaBrowser", true);
                } catch (IOException ex) {
                }
            }
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
            if (!App.isLocalMode()) {
                try {
                    Context.getInstance().setSetting("FdaBrowser", false);
                } catch (IOException ex) {
                }
            }
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

    public ProcessorXscan() {
        initComponents();
        configuration = new AcquisitionConfiguration();
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane1.getHorizontalScrollBar().setUnitIncrement(16);
        if ((App.getInstance() != null) && (App.hasArgument("fdahome"))) {
            homePath = Context.getInstance().getSetup().expandPath(App.getArgumentValue("fdahome"));
        } else {
            homePath = Context.getInstance().getSetup().getScriptPath();
        }
    }

    public ProcessorXscan(File file) throws IOException {
        this();
        if (file != null) {
            open(file.getAbsolutePath());
        }
        changed = false;
        ModelUtil.getInstance().clearModified();
    }

    public static ProcessorXscan getCurrent() {
        Processor p = App.getInstance().getMainFrame().getSelectedProcessor();
        if ((p != null) && (p instanceof ProcessorXscan)) {
            return (ProcessorXscan) p;
        }
        return null;
    }

    public ConfigurationPanel getConfigPanel() {
        return panelConfig;
    }

    @Override
    public String getType() {
        return "Xscan";
    }

    @Override
    public String getDescription() {
        return "Xscan configuration file  (*.xml)";
    }

    @Override
    public String[] getExtensions() {
        return new String[]{"xml"};
    }

    @Override
    public boolean createMenuNew() {
        return true;
    }

    @Override
    public void open(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.getName().endsWith("." + getExtensions()[0])) {
            file = new File(file.getAbsolutePath() + "." + getExtensions()[0]);
        }
        setFile(file);
        Configuration c = null;

        try {
            if (!file.exists()) {
                //New file
                c = new Configuration();
                c.setScan(new Scan());
                ModelManager.marshall(c, file);
            } else {
                c = load(file);
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        this.jPanel1.removeAll();
        this.panelConfig = new ConfigurationPanel(c);
        this.jPanel1.add(panelConfig, BorderLayout.CENTER);
        this.jPanel1.revalidate();
        this.jPanel1.repaint();

    }

    File file;

    private void setFile(File f) {
        this.file = f;
        setName(IO.getPrefix(f.getName()));
    }

    @Override
    public JPanel getPanel() {
        //Creating new
        if (file == null) {
            JFileChooser chooser = new JFileChooser(getHomePath());
            FileNameExtensionFilter filter = new FileNameExtensionFilter(getDescription(), getExtensions());
            chooser.setFileFilter(filter);
            chooser.setDialogTitle("Enter new file name");
            if (chooser.showSaveDialog(SwingUtils.getFrame(this)) != JFileChooser.APPROVE_OPTION) {
                throw new RuntimeException("File name must be set");
            }

            try {
                File f = chooser.getSelectedFile();
                if (!String.valueOf(IO.getExtension(f)).toLowerCase().equals("xml")) {
                    f = new File(f.getCanonicalPath() + ".xml");
                }
                if (f.exists()) {
                    throw new RuntimeException("File already exists");
                }
                open(f.getCanonicalPath());
                save();
                SwingUtilities.invokeLater(() -> {
                    JTabbedPane tabDoc = App.getInstance().getMainFrame().getDocumentsTab();
                    tabDoc.setTitleAt(tabDoc.getSelectedIndex(), new File(getFileName()).getName());
                });
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
            ModelManager.marshall(panelConfig.getObject(), file);
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
        Configuration model = panelConfig.getObject();

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
                if (App.getInstance().getMainFrame().getPreferences().getScriptPopupDialog() != Preferences.ScriptPopupDialog.None) {
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
        App.getInstance().startTask(new XscanTask(getFileName(), panelConfig.getObject()));
    }

    @Override
    public void abort() throws InterruptedException {
        if (acquisition != null) {
            try {
                acquisition.abort();
            } catch (Exception e) {
                Logger.getLogger(ProcessorXscan.class.getName()).log(Level.WARNING, null, e);
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
                Logger.getLogger(ProcessorXscan.class.getName()).log(Level.WARNING, null, e);
            }
        }
    }

    public void resume() {
        if (acquisition != null) {
            try {
                acquisition.resume();
            } catch (Exception e) {
                Logger.getLogger(ProcessorXscan.class.getName()).log(Level.WARNING, null, e);
            }
        }
    }

    @Override
    public boolean createFilePanel() {
        return true;
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
        open(file);

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

    public void testNotification(String recipient) {
        NotificationAgent a = new NotificationAgent(getConfiguration().getSmptServer(), "pshell.test.notification@psi.ch");
        Recipient r = new Recipient();
        r.setValue(recipient);
        r.setError(true);
        r.setSuccess(true);
        a.getRecipients().add(r);
        a.sendNotification("Test Notification", "This is a test notification", true, true);
    }

    Thread executionThread;
    Acquisition acquisition;

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
            Configuration c = panelConfig.getObject();
            acquisition.initalize(ebus, c);
            if (!batch && App.isScanPlottingActive()) {
                visualizer = new Visualizer(Acquisition.mapVisualizations(c.getVisualization()));
                if (c.getScan() != null && c.getScan().getCdimension() != null) {
                    // If there is a continuous dimension only update the plot a the end of a line.
                    // Improvement of performance
                    visualizer.setUpdateAtStreamElement(false);
                    visualizer.setUpdateAtStreamDelimiter(false);
                    visualizer.setUpdateAtEndOfStream(false);
                }
                ebus.register(visualizer);
                ProcessorXscan.setPlots(visualizer.getPlotPanels(), null);
            }
            if (Context.getInstance().getState() == State.Paused) {
                acquisition.pause();
            }
            acquisition.execute();
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Throwable t) {
            Logger.getLogger(ProcessorXscan.class.getName()).log(Level.WARNING, null, t);
            t.printStackTrace();
            throw t;
        } finally {
            ModelUtil.getInstance().setConfigurationPanel(null);
            Logger.getLogger(ProcessorXscan.class.getName()).log(Level.FINER, "Destroy acquisition");
            if (acquisition != null) {
                acquisition.destroy();
                acquisition = null;
            }
            Logger.getLogger(ProcessorXscan.class.getName()).log(Level.FINER, "Destroy channel service");
            try {
                channelService.destroy();
            } catch (Exception e) {
                Logger.getLogger(ProcessorXscan.class.getName()).log(Level.FINER, "Unable to destroy channel access service", e);
            }

            Logger.getLogger(ProcessorXscan.class.getName()).log(Level.FINER, "Stop visualizer");

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
                        Logger.getLogger(ProcessorXscan.class.getName()).log(Level.FINER, "Unable to stop executor service", e);
                    }
                }
            }).start();
            executionThread = null;
        }
    }

    @Override
    public void plotDataFile(final File file, String path) {

        Logger.getLogger(ProcessorXscan.class.getName()).log(Level.INFO, "Visualize file: {0}", file.getAbsolutePath());
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    // Try to determine configuration file from data file name
                    File dir = file.getParentFile();
                    String name = file.getName();
                    name = name.replaceAll("_[0-9]*.txt$", "");
                    //If no suffix
                    if (file.isFile()) {
                        name = IO.getPrefix(file);
                    }
                    File cfile = new File(dir, name + ".xml");

                    // Check existence of files
                    if (!file.exists()) {
                        throw new IllegalArgumentException("Data file [" + file.getAbsolutePath() + "] does not exist");
                    }
                    if (!cfile.exists()) {
                        throw new IllegalArgumentException("Configuration file [" + cfile.getAbsolutePath() + "] does not exist");
                    }

                    EventBus ebus = new EventBus(AcquisitionConfiguration.eventBusModePlot);
                    Deserializer deserializer = (path == null) ? new DeserializerTXT(ebus, file) : new DeserializerPShell(ebus, file, path);

                    Configuration c = load(cfile);
                    VDescriptor vdescriptor = Acquisition.mapVisualizations(c.getVisualization());

                    Visualizer visualizer = new Visualizer(vdescriptor);
                    visualizer.setUpdateAtStreamElement(false);
                    visualizer.setUpdateAtStreamDelimiter(false);
                    visualizer.setUpdateAtEndOfStream(true);

                    ebus.register(visualizer);

                    //tc.updatePanel(visualizer.getPlotPanels());
                    ProcessorXscan.setPlots(visualizer.getPlotPanels(), "Data");

                    deserializer.read();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtils.showMessage(App.getInstance().getMainFrame(), "Error", "An error occured while visualizing '" + file.getName() + "':\n" + ex.getMessage());
                }
            }
        });
        t.start();
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
        name = name.replaceAll("\\.xml$", "");

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
