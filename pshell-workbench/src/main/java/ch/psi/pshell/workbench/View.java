package ch.psi.pshell.workbench;

import ch.psi.pshell.swing.SessionPanel;
import ch.psi.pshell.app.AboutDialog;
import ch.psi.pshell.app.Importer;
import ch.psi.pshell.archiverviewer.ArchiverPanel;
import ch.psi.pshell.camserver.CamServerService;
import ch.psi.pshell.data.FormatHDF5;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePanelManager;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Executor;
import ch.psi.pshell.framework.MainFrame;
import ch.psi.pshell.framework.Options;
import ch.psi.pshell.framework.Panel;
import ch.psi.pshell.framework.Processor;
import ch.psi.pshell.framework.QueueProcessor;
import ch.psi.pshell.framework.ScriptEditor;
import ch.psi.pshell.framework.ScriptProcessor;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.framework.StatusBar;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.imaging.Utils;
import ch.psi.pshell.logging.LogLevel;
import ch.psi.pshell.logging.LogManager;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.security.AccessLevel;
import ch.psi.pshell.security.Rights;
import ch.psi.pshell.security.User;
import ch.psi.pshell.sequencer.Interpreter;
import ch.psi.pshell.sequencer.InterpreterListener;
import ch.psi.pshell.sequencer.PlotListener;
import ch.psi.pshell.sequencer.Server;
import ch.psi.pshell.session.SessionManager;
import ch.psi.pshell.session.SessionManager.ChangeType;
import ch.psi.pshell.stripchart.StripChart;
import ch.psi.pshell.swing.CamServerServicePanel;
import ch.psi.pshell.swing.ConfigDialog;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.pshell.swing.DevicePoolEditor;
import ch.psi.pshell.swing.DeviceUpdateStrategyEditor;
import ch.psi.pshell.swing.Editor;
import ch.psi.pshell.swing.Editor.EditorDialog;
import ch.psi.pshell.swing.EpicsConfigDialog;
import ch.psi.pshell.swing.ExtensionFileFilter;
import ch.psi.pshell.swing.HistoryChart;
import ch.psi.pshell.swing.LoggerPanel;
import ch.psi.pshell.swing.MetadataEditor;
import ch.psi.pshell.swing.MotorPanel;
import ch.psi.pshell.swing.OutputPanel;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.swing.PluginsEditor;
import ch.psi.pshell.swing.RepositoryChangesDialog;
import ch.psi.pshell.swing.ScanPanel;
import ch.psi.pshell.swing.SearchPanel;
import ch.psi.pshell.swing.SessionReopenDialog;
import ch.psi.pshell.swing.SessionsDialog;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.SwingUtils.OptionResult;
import ch.psi.pshell.swing.SwingUtils.OptionType;
import ch.psi.pshell.swing.TasksEditor;
import ch.psi.pshell.swing.Terminal;
import ch.psi.pshell.swing.UsersEditor;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.ControlChar;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Sys.OSFamily;
import ch.psi.pshell.versioning.VersioningListener;
import ch.psi.pshell.versioning.VersioningManager;
import ch.psi.pshell.workbench.Preferences.PanelLocation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/**
 * The main dialog of the Workbench.
 */
public class View extends MainFrame{

    static final Logger logger = Logger.getLogger(View.class.getName());     

    public View() {
        super();
        try {
            initComponents();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, null, t);
            throw t;
        }
        
        tabLeft.setVisible(false);
        tabLeft.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                checkTabLeftVisibility();
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                checkTabLeftVisibility();
            }
        });        
        loggerPanel.setOutputLength(1000);
        //loggerPanel.setInverted(true);
        loggerPanel.start();

        Context.getInterpreter().addListener(interpreterListener);
        if (Context.hasVersioningManager()){
            Context.getVersioningManager().addListener(versioningListener);
        }
        MainFrame.setPersistenceFilesPermissions(Context.getConfigFilePermissions());
        
        menuVersioning.setVisible(false);             
        menuSessions.setVisible(Context.isHandlingSessions());      
        menuCamServer.setVisible(Setup.getPipelineServer()!=null);
        manuDaqbuf.setVisible(ch.psi.pshell.devices.Options.ARCHIVER.hasValue());

        ActionListener logLevelListener = (ActionEvent e) -> {
            Context.setLogLevel(Level.parse(e.getActionCommand().toUpperCase()));
        };
        for (LogLevel level : LogLevel.values()) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(level.toString());
            item.addActionListener(logLevelListener);
            menuSetLogLevel.add(item);
        }
        tabDoc.setMinimumSize(new Dimension(tabDoc.getMinimumSize().width, 30));
        tabStatus.setMinimumSize(new Dimension(tabStatus.getMinimumSize().width, 30));
        buttonRestart.setEnabled(false);
        menuRestart.setEnabled(false);
        shell.setNextFocusableComponent(tabStatus);
        menuUsers.setVisible(false);
        labelUser.setFont(labelUser.getFont().deriveFont(Font.BOLD));

        for (PanelLocation location : PanelLocation.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(location.toString());
            item.addActionListener((java.awt.event.ActionEvent evt) -> {
                if (!Setup.isLocal()) {
                    preferences.consoleLocation = location;
                    preferences.save();
                    applyPreferences();
                } else {
                    setConsoleLocation(location);
                }
            });
            menuConsoleLocation.add(item);
            
            
            item = new JRadioButtonMenuItem(location.toString());
            item.addActionListener((java.awt.event.ActionEvent evt) -> {
                if (!Setup.isLocal()) {
                    preferences.dataPanelLocation = location;
                    preferences.save();
                    applyPreferences();
                } else {
                    setDataPanelLocation(location);
                }
            });
            menuDataPanelLocation.add(item);
        }
        menuDataPanelLocation.addSeparator();
        JCheckBoxMenuItem item = new JCheckBoxMenuItem("Open in Documents Tab");
        item.addActionListener((java.awt.event.ActionEvent evt) -> {
            if (!Setup.isLocal()) {
                preferences.openDataFilesInDocTab = item.getState();
                preferences.save();
                applyPreferences();
            } else {
                dataPanel.setEmbedded(!preferences.openDataFilesInDocTab);
            }
        });        
        menuDataPanelLocation.add(item);
        
        for (LookAndFeelType laf : LookAndFeelType.getFullNames()) {
            try{
                if (LookAndFeelType.class.getField(laf.toString()).getAnnotation(Deprecated.class)!=null){
                    continue;
                }
            } catch (Exception ex){                
            }
           
            JRadioButtonMenuItem lafItem = new JRadioButtonMenuItem(laf.toString());
            lafItem.addActionListener((java.awt.event.ActionEvent evt) -> {
                if (!updatingLafMenu){
                    if (!Setup.isLocal()) {
                       //!!! Save user selection ?
                    } 
                    setLookAndFeel(laf.toString());
                    updateLafMenu();
                }
            });
            menuLaf.add(lafItem);
        }
                
                
        menuPull.setVisible(false); //Disabling this menu because may be dangerous for users have such easy access
        Utils.setSelectedImageFolder(Setup.getImagesPath());
        toolBar.setRollover(true);
        toolBar.setFloatable(false); //By default true in nimbus

        if (Setup.isPlotOnly()) {
            splitterVert.setVisible(false);
            splitterHoriz.setDividerSize(0);
            menuBar.setVisible(false);
            Component[] visible = new Component[]{buttonAbort, buttonPause, buttonRun, buttonAbout, separatorInfo};
            for (Component c : toolBar.getComponents()) {
                if (!Arr.contains(visible, c)) {
                    toolBar.remove(c);
                }
            }
        }
        if (Sys.getOSFamily() == OSFamily.Mac) {
            SwingUtils.adjustMacMenuBarAccelerators(menuBar);
            //Not to collide with voice over shortcut
            menuDebug.setAccelerator(KeyStroke.getKeyStroke(menuDebug.getAccelerator().getKeyCode(), KeyEvent.SHIFT_MASK));
            //F3 is not ideal on mac to search next
            menuFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.META_MASK));
        }
        dropListner = new DropTargetListener();
        dropListner.enable();

        //Done in base class asynchronously, repeating here to set not visible before drawing the window.
        checkHiddenComponents(); 

        if (Setup.isOffline()) {
            setConsoleLocation(PanelLocation.Hidden);
            menuUpdateAll.setEnabled(false);
            menuReinit.setEnabled(false);
            tabStatus.remove(loggerPanel);
            tabStatus.remove(devicesPanel);
            tabStatus.remove(imagingPanel);
            tabStatus.remove(scanPanel);
            tabStatus.remove(outputPanel);
            menuShell.setVisible(false);
        }        
    }

    public boolean isHidden(Component component){
        if (component==null){
            return true;
        }
        return Options.HIDE_COMPONENT.getStringList().contains(component.getName());
    }
       
    
    //Callbacks from MainFrame
    /**
     * Called once when frame is created, before being visible
     */
    @Override
    protected void onCreate() {
        restorePreferences();
        super.onCreate();
        if (Context.isHandlingSessions() &&  !Setup.isOffline()){
            try {
                Context.getSessionManager().addListener((id, type) -> {
                    if (type == ChangeType.STATE) {
                        SwingUtilities.invokeLater(() -> {
                            checkSessionPanel();
                        });
                    }
                });
                checkSessionPanel();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }       
        //if (!App.isFullScreen() && Context.getApp().isViewPersisted()) {
        //    //!!! restoring again (also in App.startup) to take into avcount hidden panels displayed by restorePreferences
        //    restoreState();
        //}

        if (tabDoc.getTabCount() > 0) {
            tabDoc.setSelectedIndex(0);
        }        
    }
    
    @Override
    protected void onDispose() {
    }
    
    @Override
    public void onPathChange(String pathId) {
        if (Setup.TOKEN_DATA.equals(pathId)) {
            dataPanel.initialize(preferences.getDataPanelAdditionalExtensions(), preferences.getDataPanelAdditionalFiles());
        } else if (Setup.TOKEN_SCRIPT.equals(pathId)) {
            scriptsPanel.initialize();
        } else if (Setup.TOKEN_LOGS.equals(pathId)) {
            logger.log(Level.INFO, "New user: restarting logger");
            App.getInstance().restartLogger();            
        } else if (Setup.TOKEN_SESSIONS.equals(pathId)) {
            if (App.getInstance().getConfig().saveConsoleSessionFiles && !Setup.isLocal()) {
                Context.getInterpreter().getScriptManager().setSessionFilePath(Setup.getConsolePath());
            }       
        }
    } 
    
    @Override
    protected void onUserChange(User user, User former) {
        closeSearchPanels();
        setConsoleLocation(consoleLocation);//User may have no rights
        setDataPanelLocation(dataPanelLocation);//User may have no rights  
        setStatusTabVisible(devicesPanel, !Context.getRights().hideDevices);
        setStatusTabVisible(imagingPanel, !Context.getRights().hideDevices);
        setStatusTabVisible(scriptsPanel, !Context.getRights().hideScripts);
        toolBar.setVisible(!Context.getRights().hideToolbar);

        labelUser.setText((Context.isUsersEnabled() && !User.DEFAULT_USER_NAME.equals(user.name)) ? user.name : "");

        updateViewState();

        for (Window window : JDialog.getWindows()) {
            if (window instanceof ConfigDialog configDialog) {
                //Configuration dialog is modal so does not need to be checked
                configDialog.setReadOnly(Context.getRights().denyDeviceConfig);
            } else if (window instanceof EditorDialog editorDialog) {
                Editor editor = editorDialog.getEditor();
                if ((editor instanceof DevicePoolEditor)
                        || (editor instanceof DeviceUpdateStrategyEditor)
                        || (editor instanceof PluginsEditor)
                        || (editor instanceof TasksEditor)
                        || (editor instanceof UsersEditor) //|| ((editor instanceof TextEditor) && (editor.getFileName().equals(Epics.getConfigFile())))
                        ) {
                    editor.setReadOnly(Context.getRights().denyConfig);
                }
            }

            for (Editor editor : getEditors()) {
                editor.setReadOnly(Context.getRights().denyEdit);
            }
            for (ScriptEditor editor : getScriptEditors()) {
                editor.setReadOnly(Context.getRights().denyEdit);
            }
        }
    }
    
      
    /*
    @Override
    public void run() throws Exception{
        if ((Context.getState()==State.Ready) && (Setup.isPlotOnly())){
            File file = App.getFileArg();
            if (file != null) {
                Context.getInterpreter().evalFileAsync(file.toString(), App.getInterpreterArgs());
            }
            updateViewState();
        } else {
            super.run();
        }
    }
    */
    @Override
    public void updateViewState() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                updateViewState();
            });
            return;
        }
        Rights rights = Context.getRights();
        boolean allowEdit = (rights != null) && !rights.denyEdit;
        boolean allowRun = (rights != null) && !rights.denyRun;
        Component selectedDocument = tabDoc.getSelectedComponent();
        State state = Context.getApp().getState();
        Processor runningProcessor = getRunningProcessor();
        if (state == State.Ready && (runningProcessor != null) && (runningProcessor instanceof QueueProcessor)) {
            state = State.Busy;
        }
        boolean showingScript = isShowingScriptEditor();
        boolean showingExecutor = isShowingExecutor();
        boolean ready = (state == State.Ready);
        boolean busy = (state == State.Busy);
        boolean paused = (state == State.Paused);

        Interpreter interp = Context.getInterpreter();
        buttonRun.setEnabled(allowRun && (((ready && (showingExecutor|| Setup.isPlotOnly())) || (paused && !interp.isRunningStatements()))));
        buttonDebug.setEnabled((ready && showingScript && allowRun) || (paused && interp.isRunningStatements()));
        buttonPause.setEnabled(interp.canPause() || ((runningProcessor != null) && (runningProcessor.canPause())));
        buttonStep.setEnabled(((ready && showingScript) || interp.canStep() || ((runningProcessor != null) && (runningProcessor.canStep()))) && allowRun);
        buttonAbort.setEnabled(busy || paused || (state == State.Initializing));
        menuRun.setEnabled(buttonRun.isEnabled());
        menuDebug.setEnabled(buttonDebug.isEnabled());
        menuPause.setEnabled(buttonPause.isEnabled());
        menuStep.setEnabled(buttonStep.isEnabled());
        menuAbort.setEnabled(buttonAbort.isEnabled());

        menuCheckSyntax.setEnabled(showingScript);

        buttonStopAll.setEnabled(state.isInitialized() && !state.isProcessing());
        menuStopAll.setEnabled(buttonStopAll.isEnabled());

        menuNew.setEnabled(allowEdit);

        menuSave.setEnabled(showingExecutor && getSelectedExecutor().canSave() && allowEdit);
        menuSaveAs.setEnabled(menuSave.isEnabled());
        buttonSave.setEnabled(menuSave.isEnabled());

        if (Context.getState().isProcessing()) {
            menuRunNext.setVisible(true);
            menuRunNext.setEnabled(isShowingExecutor());
        } else {
            menuRunNext.setVisible(false);
        }
    }

    boolean updatingLafMenu;
    void updateLafMenu(){
        try{
            updatingLafMenu=true;
            for (Component item : menuLaf.getMenuComponents()) {            
                if (item instanceof JRadioButtonMenuItem radioButtonMenuItem){
                    String text = "";
                    boolean enabled = false;
                    try{
                        text = radioButtonMenuItem.getText();
                        enabled = text.equals(getLookAndFeelType().toString());
                    } catch(Exception ex){                
                    }
                    radioButtonMenuItem.setSelected(enabled);
                }
            }
        } finally {
            updatingLafMenu=false;
        }
    }
            
    void onFirstStart() {
        //setupPanelsMenu()
        //if (Context.getInstanceName().length() > 0) {
        //    setTitle(App.getApplicationTitle() + " [" + Context.getInstanceName()+ "]");
        //}
        //if (Context.getApp().isContextPersisted()) {
        //    restoreOpenedFiles();
        //}

        //In case openCmdLineFiles() in onStart didn't open because Context was not instantiated
        openCmdLineFiles(true);
    }

    void onStart() {
        MainFrame.setPersistenceFilesPermissions(Context.getConfigFilePermissions());
        
        //User menu
        menuChangeUser.setEnabled(Context.isUsersEnabled());

        //Device menu
        menuDevicesConfig.removeAll();
        ActionListener deviceConfigListener = (java.awt.event.ActionEvent evt) -> {
            try {
                String ac = evt.getActionCommand();
                final GenericDevice dev = Context.getDevicePool().getByName(ac);
                final ConfigDialog dlg = new ConfigDialog(View.this, false);
                dlg.setTitle("Device Configuration: " + ac);
                dlg.setConfig(dev.getConfig());
                dlg.setReadOnly(Context.getRights().denyDeviceConfig);
                dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                dlg.setListener((StandardDialog sd, boolean accepted) -> {
                    if (sd.getResult()) {
                        try {
                            Context.saveConfiguration(dev);
                        } catch (IOException ex) {
                            showException(ex);
                        }
                    }
                });
                showChildWindow(dlg);
            } catch (Exception ex) {
                showException(ex);
            }
        };

        for (String deviceName : Context.getDevicePool().getAllDeviceNames()) {
            GenericDevice dev = Context.getDevicePool().getByName(deviceName);
            if (dev.getConfig() != null) {
                JMenuItem item = new JMenuItem(deviceName);
                item.addActionListener(deviceConfigListener);
                menuDevicesConfig.add(item);
            }
        }
        
        DevicePanelManager.getInstance().checkWindowRestart();

        menuVersioning.setVisible(App.getInstance().getConfig().isVersioningEnabled());        
        menuPush.setEnabled(App.getInstance().getConfig().isVersioningTrackingRemote());
        menuPull.setEnabled(menuPush.isEnabled());

        //Avoid re-initializing when application is first open
        if (Context.getRunCount() > 0) {
            dataPanel.initialize(preferences.getDataPanelAdditionalExtensions(), preferences.getDataPanelAdditionalFiles());
        }
        
        if (Setup.getPlotServer() == null ){
            Context.getInterpreter().setPlotListener(new PlotListener() {            
                @Override
                public List<Plot> plot(String title, PlotDescriptor[] plots) throws Exception {    
                    return (List<Plot>) SwingUtils.invokeAndWait(() -> {
                        return View.this.plotData(title, plots);
                    });
                }

                @Override
                public List<Plot> getPlots(String title) {
                    PlotPanel plotPanel = getPlotPanel(title, false);
                    if (plotPanel != null) {
                        return plotPanel.getPlots();
                    }
                    return new ArrayList<Plot>();
                }

                @Override
                public List<String> getTitles(){
                    return getPlotTitles();
                }       

                @Override
                public void onTitleClosed(String title){
                    //Do nothing... only close titles graphically
                }            
            });
        }
    }

    /**
     * Called once in the first time the frame is shown
     */
    @Override
    protected void onOpen() {
        super.onOpen();

        devicesPanel.setType(Device.class);
        imagingPanel.setType(Source.class);

        shell.initialize();
        scanPanel.initialize();
        scanPlot.initialize(); //!!! DOne also in superclass
        outputPanel.initialize();
        devicesPanel.initialize();
        scriptsPanel.initialize();
        scriptsPanel.setListener((File file) -> {
            try {
                openScriptOrProcessor(file.getCanonicalPath());
            } catch (Exception ex) {
                showException(ex);
            }
        });
        dataPanel.initialize(preferences.getDataPanelAdditionalExtensions(), preferences.getDataPanelAdditionalFiles());//!!! DOne also in superclass

        menuFullScreen.setSelected(isFullScreen());

        if (Setup.isOutputRedirected()) {
            //TODO: This is an issue in Jython 2.7. If not setting get this error when redirect stdout:
            //    console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0
            System.setProperty("python.console.encoding", "UTF-8");
            System.setOut(new PrintStream(new ConsoleStream(false)));
            System.setErr(new PrintStream(new ConsoleStream(true)));
        }

        openCmdLineFiles(false);
    }

    void openCmdLineFiles(boolean showExceptions) {
        if (!Setup.isPlotOnly()) {
            for (File file : App.getFileArgs()) {
                try {
                    if (file != null) {
                        openScriptOrProcessor(file.getPath());
                    }
                } catch (Exception ex) {
                    if (showExceptions) {
                        showException(ex);
                    }
                }
            }
        }
    }
    
    final InterpreterListener interpreterListener = new InterpreterListener() {
        @Override
        public void onInitialized(int runCount) {
            if (runCount == 0) {
                onFirstStart();
            }
            onStart();
        }

        @Override
        public void onStateChanged(State state, State former) {
            if ((state == State.Busy)&&(!former.isProcessing())) {
                //synchronized (plotTitles) {
                //    plotTitles.clear();
                //}
                //Doing every time because each script can overide this
                restorePreferences();
            }
            buttonRestart.setEnabled((state != State.Initializing) && !Setup.isOffline());
            menuRestart.setEnabled(buttonRestart.isEnabled());
        }

        void restorePreferences() {
            setScanTableDisabled(preferences.scanTableDisabled);
            setScanPlotDisabled(preferences.scanPlotDisabled);
        }

        @Override
        public void onPreferenceChange(ViewPreference preference, Object value) {            
            switch (preference) {
                case PLOT_DISABLED -> setScanPlotDisabled((value == null) ? preferences.scanPlotDisabled : (Boolean) value);
                case TABLE_DISABLED -> setScanTableDisabled((value == null) ? preferences.scanTableDisabled : (Boolean) value);
                case DEFAULTS -> restorePreferences();
            }
        }
    };
    
    final VersioningListener versioningListener = new VersioningListener() {
        @Override
        public void onCheckout(String branch) {
            for (ScriptEditor se : getScriptEditors()) {
                if (se.getFileName() != null) {
                    try {
                        se.reload();
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }
            }
        }

    };

    @Override
    public ScriptPopupMode getScriptPopupMode() {
        return ScriptPopupMode.Exception;
    }

    @Override
    public void showAboutDialog() {
        AboutDialog aboutDialog = new AboutDialog(this, true);
        aboutDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        showChildWindow(aboutDialog);
    }
    

    class ConsoleStream extends OutputStream {

        final boolean is_err;
        StringBuilder sb;

        ConsoleStream(boolean is_err) {
            this.is_err = is_err;
            sb = new StringBuilder();
        }

        @Override
        public void write(int b) throws IOException {
            if (b == ControlChar.LF) {
                if (outputPanel.isDisplayable()) {
                    if (is_err) {
                        outputPanel.putError(sb.toString());
                    } else {
                        outputPanel.putOutput(sb.toString());
                    }
                }
                sb = new StringBuilder();
            } else {
                sb.append((char) b);
            }
        }
    }
            
    public void clearScanDisplays(String title){
        getPlotPanel(title).clear();                                
        scanPanel.clear();
    }

    public void setScanDisplays(Scan scan, String title){
        startScanPlot(scan, title);
        getScanPanel().startScan(scan, title);
    }

    @Override
    public ScanPanel getScanPanel(){
        return scanPanel;
    }

    public PlotPanel getScanPlot(){
        return scanPlot;
    }
    
    @Override
    public OutputPanel getOutputPanel() {
        return outputPanel;
    }
    
    @Override
    public JMenu getMenuFileNew() {
         return menuFileNew;
     }
    
    @Override
     public DataPanel getDataPanel() {
         return dataPanel;
     }



    /**
     * Called every time the frame is shown (also before open is called)
     */
    @Override
    protected void onShow() {
    }

    /**
     * Called every time the frame is hidden (also before disposed)
     */
    @Override
    protected void onHide() {
    }

    /**
     * Called every second if the frame is visible
     */
    @Override
    protected void onTimer() {
        super.onTimer();
    }
    

    boolean isPlotsVisible() {
        if (plotsDetached) {
            return plotFrame.isVisible();
        } else {
            return tabPlots.isVisible();
        }
    }

    void setScanPlotVisible(boolean value) {
        if (!isHidden(plotFrame)){
            if (isPlotsVisible() != value) {
                setTabPlotVisible(value);
                if (plotsDetached) {
                    plotFrame.setVisible(value);
                } else if (value) {
                    if ((splitterHoriz.getDividerLocation() >= splitterHoriz.getWidth() - splitterHoriz.getDividerSize() - 10)) {
                        splitterHoriz.setDividerLocation(0.70);
                    }
                }
            }
        }
    }

    void setTabPlotVisible(boolean value) {
        if (!isHidden(tabPlots)){
            tabPlots.setVisible(value);
            splitterHoriz.setDividerSize(value ? splitterVert.getDividerSize() : 0);
        }
    }

    JFrame plotFrame;
    boolean plotsDetached;

    void setScanPlotDetached(boolean value) {
        if (plotsDetached != value) {
            if (Setup.isPlotOnly()) {
                return;
            }
            if (isHidden(plotFrame)){
                return;
            }
            boolean visible = isPlotsVisible();
            if (value) {
                splitterHoriz.setRightComponent(null);
                plotFrame = new JFrame("PShell Plots");
                plotFrame.setDefaultCloseOperation(HIDE_ON_CLOSE);
                plotFrame.setName("Plots");
                plotFrame.setSize(600, 400);
                plotFrame.setContentPane(tabPlots);
                plotFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getIcon()));
                addPersistedWindow(plotFrame);
                plotFrame.setVisible(visible);
            } else {
                plotFrame.setVisible(false);
                setTabPlotVisible(visible);
                plotFrame.remove(tabPlots);
                splitterHoriz.setRightComponent(tabPlots);
                removePersistedWindow(plotFrame);
                plotFrame.dispose();
                if ((splitterHoriz.getDividerLocation() >= splitterHoriz.getWidth() - splitterHoriz.getDividerSize() - 1)) {
                    splitterHoriz.setDividerLocation(0.70);
                }
            }
            plotsDetached = value;
            radioPlotsDetached.setSelected(value);
        }
    }
    
    void setScanPanelVisible(boolean value){
        if (!Setup.isOffline()) {
            if (!isHidden(scanPanel)){
                if (preferences.hideScanPanel != (tabStatus.indexOfComponent(scanPanel)<0)){
                    if (preferences.hideScanPanel){
                        tabStatus.remove(scanPanel);
                    } else {
                        int index = Math.min(3, tabStatus.getTabCount());
                        tabStatus.add(scanPanel, index); 
                        tabStatus.setTitleAt(index, App.getResourceBundleValue(View.class, "View.scanPanel.TabConstraints.tabTitle"));                    
                    }
                }
            }
        }                
    }
    
    boolean isScanPanelVisible(){
        return (tabStatus.indexOfComponent(scanPanel)>=0);
    }

    void setOutputPanelVisible(boolean value){
        if (!isHidden(outputPanel)){
            if (!Setup.isOffline()) {
                if (preferences.hideOutputPanel != (tabStatus.indexOfComponent(outputPanel)<0)){
                    if (preferences.hideOutputPanel) {
                        tabStatus.remove(outputPanel);
                    } else {
                        int index = Math.min(isScanPanelVisible()?4:3, tabStatus.getTabCount());
                        tabStatus.add(outputPanel, index); 
                        tabStatus.setTitleAt(index, App.getResourceBundleValue(View.class, "View.outputPanel.TabConstraints.tabTitle"));                    
                    }
                }
            }                       
        }
    }
    
    public void setScanTableDisabled(boolean value) {     
        scanPanel.setActive(preferences.hideScanPanel ||  !value);
    }

    public boolean isScanTableDisabled() {
        return !scanPanel.isActive();
    }

    boolean scanPlotDisabled = false;

    public void setScanPlotDisabled(boolean value) {
        scanPlotDisabled = value;
    }

    public boolean isScanPlotDisabled() {
        return scanPlotDisabled || !isPlotsVisible() || !Context.getApp().isScanPlottingActive();
    }
    
        
     
    boolean isOutputPanelVisible(){
        return (tabStatus.indexOfComponent(outputPanel)>=0);
    }    

    PanelLocation getLocation(JPanel panel) {
        if (!panel.isDisplayable()) {
            return PanelLocation.Hidden;
        }
        Container parent = panel.getParent();
        if (parent == tabDoc) {
            return PanelLocation.Document;
        }
        if (parent == tabStatus) {
            return PanelLocation.Status;
        }
        if (parent == tabPlots) {
            return PanelLocation.Plot;
        }
        if (parent == tabLeft) {
            return PanelLocation.Left;
        }
        return PanelLocation.Detached;
    }

    PanelLocation consoleLocation = Preferences.DEFAULT_CONSOLE_LOCATION;
    PanelLocation dataPanelLocation = Preferences.DEFAULT_DATA_PANEL_LOCATION;

    void setConsoleLocation(PanelLocation location) {
        if (Setup.isPlotOnly()||(location==null)) {
            return;
        }
        consoleLocation = location;

        if ((Context.getRights().hideConsole) || Setup.isOffline()) {
            location = PanelLocation.Hidden;
        }

        PanelLocation current = getLocation(shell);
        if (current != location) {
            Container topLevel = shell.getTopLevelAncestor();
            Container parent = shell.getParent();
            if (parent != null) {
                parent.remove(shell);
            }
            if (topLevel instanceof Window window) {
                if ((topLevel != this) && (Arr.contains(getPersistedWindows(), topLevel))) {
                    removePersistedWindow(window);
                    window.dispose();
                }
            }

            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/workbench/View");
            String title = bundle.getString("View.shell.TabConstraints.tabTitle");
            switch (location) {
                case Document:
                    int index = 0;
                    for (index = 0; index < tabDoc.getTabCount(); index++) {
                        if (!(tabDoc.getComponentAt(index) instanceof Panel)) {
                            break;
                        }
                    }
                    tabDoc.add(shell, index);
                    tabDoc.setTitleAt(index, title);
                    break;
                case Status:
                    tabStatus.add(shell, 0);
                    tabStatus.setTitleAt(0, title);
                    break;
                case Left:
                    tabLeft.add(shell, 0);
                    tabLeft.setTitleAt(0, title);
                    break;
                case Plot:
                    tabPlots.add(shell, 0);
                    tabPlots.setTitleAt(0, title);
                    break;
                case Detached:
                    JDialog dlg = new JDialog(this, title, false);
                    dlg.setName(title);
                    dlg.setSize(400, 600);
                    dlg.add(shell);
                    showChildWindow(dlg);
                    dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    addPersistedWindow(dlg);
                    break;
            }
            checkTabLeftVisibility();
        }
    }
//setStatusTabVisible(dataPanel, !Context.getRights().hideData);
    void setDataPanelLocation(PanelLocation location) {
        if (Setup.isPlotOnly() || (location==null)) {
            return;
        }
        dataPanelLocation = location;

        if (Context.getRights().hideData) {
            location = PanelLocation.Hidden;
        }

        PanelLocation current = getLocation(dataPanel);
        if (current != location) {
            Container topLevel = dataPanel.getTopLevelAncestor();
            Container parent = dataPanel.getParent();
            if (parent != null) {
                parent.remove(dataPanel);
            }
            if (topLevel instanceof Window window) {
                if ((topLevel != this) && (Arr.contains(getPersistedWindows(), topLevel))) {
                    removePersistedWindow(window);
                    window.dispose();
                }
            }

            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/workbench/View");
            String title = bundle.getString("View.dataPanel.TabConstraints.tabTitle");

            switch (location) {
                case Document:
                    int index = 0;
                    for (index = 0; index < tabDoc.getTabCount(); index++) {
                        if (!(tabDoc.getComponentAt(index) instanceof Panel)) {
                            break;
                        }
                    }
                    tabDoc.add(dataPanel, index);
                    tabDoc.setTitleAt(index, title);
                    break;
                case Status:
                    for (index = tabStatus.getTabCount()-1; index >=0; index--) {
                        if (!SwingUtils.isTabClosable(tabStatus, index)) {
                            break;
                        }
                    }                    
                    tabStatus.add(dataPanel, index+1);
                    tabStatus.setTitleAt(index+1, title);
                    break;
                case Left:
                    tabLeft.add(dataPanel,tabLeft.getTabCount());
                    tabLeft.setTitleAt(tabLeft.getTabCount()-1, title);
                    break;
                case Plot:
                    tabPlots.add(dataPanel, 0);
                    tabPlots.setTitleAt(0, title);
                    break;
                case Detached:
                    JDialog dlg = new JDialog(this, title, false);
                    dlg.setName(title);
                    dlg.setSize(800, 600);
                    dlg.add(dataPanel);
                    showChildWindow(dlg);
                    dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    addPersistedWindow(dlg);
                    break;
            }
            checkTabLeftVisibility();
        }
    }
    

    public static void setLookAndFeel(String laf){    
        MainFrame.setLookAndFeel(laf);
        if (isDark()) {
            UIManager.put("FileView.directoryIcon", new ImageIcon(App.getResourceImage("FolderClosed.png")));
            UIManager.put("FileChooser.homeFolderIcon", new ImageIcon(App.getResourceImage("Home.png")));
            UIManager.put("FileView.computerIcon", new ImageIcon(App.getResourceImage("Computer.png")));
            UIManager.put("FileView.floppyDriveIcon", new ImageIcon(App.getResourceImage("Floppy.png")));
            UIManager.put("FileView.hardDriveIcon", new ImageIcon(App.getResourceImage("HardDrive.png")));
            UIManager.put("FileChooser.upFolderIcon", new ImageIcon(App.getResourceImage("FolderUp.png")));
            UIManager.put("FileChooser.newFolderIcon", new ImageIcon(App.getResourceImage("FolderNew.png")));
            UIManager.put("FileView.fileIcon", new ImageIcon(App.getResourceImage("File.png")));
            UIManager.put("FileChooser.listViewIcon", new ImageIcon(App.getResourceImage("List.png")));
            UIManager.put("FileChooser.detailsViewIcon", new ImageIcon(App.getResourceImage("Details.png")));
            UIManager.put("Tree.openIcon", new ImageIcon(App.getResourceImage("FolderOpen.png")));
            UIManager.put("Tree.closedIcon", new ImageIcon(App.getResourceImage("FolderClosed.png")));
            UIManager.put("Tree.leafIcon", new ImageIcon(App.getResourceImage("File.png")));
            if (isBlack()) {
                UIManager.put("ComboBox.background",  UIManager.get("TextField.background"));
            }
        }            
        try{
            if (Context.getView().isVisible()){
                ((View)Context.getView()).applyPreferences();
            }
        } catch (Exception ex){            
        }
    }
    
    
    void checkTabLeftVisibility() {
        boolean tabLeftVisible = tabLeft.getTabCount() > 0;
        if (tabLeftVisible != tabLeft.isVisible()) {
            tabLeft.setVisible(tabLeftVisible);
            splitterDoc.setDividerSize(tabLeftVisible ? splitterVert.getDividerSize() : 0);
            if (tabLeftVisible && (splitterDoc.getDividerLocation() < 0.1)) {
                splitterDoc.setDividerLocation(0.40);
            }
        }
    }

    void setStatusTabVisible(JPanel panel, boolean visible) {
        if (visible != panel.isDisplayable()) {
            if (visible) {
                int index;
                if (panel == devicesPanel) {
                    index = 1;
                } else if (panel == imagingPanel) {
                    index = 1;
                } else if (panel == dataPanel) {
                    index = tabStatus.getTabCount();
                } else {
                    index = (dataPanel.isDisplayable()) ? tabStatus.getTabCount() : tabStatus.getTabCount() - 1;
                }
                java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/workbench/View");
                String title = bundle.getString("View." + panel.getName() + ".TabConstraints.tabTitle");
                tabStatus.add(panel, index);
                tabStatus.setTitleAt(index, title);
            } else {
                tabStatus.remove(panel);
            }
        }
    }

    void checkSessionPanel() {        
        try {
            if (Context.getSessionManager().getCurrentSession() > 0) {
                showSessionPanel();
            } else {
                hideSessionPanel();
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    SessionPanel getSessionPanel() {
        for (int i = tabStatus.getTabCount() - 1; i > 0; i--) {
            if (tabStatus.getComponentAt(i) instanceof SessionPanel sessionPanel) {
                return sessionPanel;
            }
        }
        return null;
    }

    void showSessionPanel() {
        SessionPanel sessionPanel = getSessionPanel();
        if (sessionPanel == null) {
            sessionPanel = new SessionPanel();
            int i = 0;
            for (; i < tabStatus.getTabCount(); i++) {
                if (tabStatus.getComponentAt(i) instanceof DataPanel) {
                    i++;
                    break;
                }
            }
            tabStatus.add(sessionPanel, i);
            tabStatus.setTitleAt(i, "Session");
            tabStatus.setSelectedComponent(sessionPanel);
        }        
    }

    void hideSessionPanel() {
        for (int i = tabStatus.getTabCount() - 1; i > 0; i--) {
            if (tabStatus.getComponentAt(i) instanceof SessionPanel) {
                tabStatus.remove(i);
                break;
            }
        }
    }

    //Preferences
    Preferences preferences;

    public Preferences getPreferences() {
        return preferences;
    }
    
    @Override
    public Object getPreference(String name){
        try{
            return Preferences.class.getField(name).get(preferences);
        } catch (Exception ex){
            return null;
        }
    }

    public void restorePreferences() {
        preferences = Preferences.load();
        applyPreferences();
        if (Context.getApp().isViewPersisted()) {
            restoreOpenedFiles();
        }        
    }
   

    public void applyPreferences() {
        DevicePanelManager.getInstance().setDefaultPanels(preferences.defaultPanels);
        DevicePanelManager.getInstance().configure(
                preferences.persistRendererWindows && !Setup.isLocal(), 
                preferences.showImageStatusBar,
                preferences.backgroundRendering
        );
      
        if (preferences.fontShellPanel != null){
            shell.setTextPaneFont(preferences.fontShellPanel.toFont());
        }
        if (preferences.fontShellCommand != null){        
            shell.setTextInputFont(preferences.fontShellCommand.toFont());
        }
        setScriptEditorPreferences(preferences.getScriptEditorPreferences());
        shell.setPropagateVariableEvaluation(!preferences.noVariableEvaluationPropagation);
        ScriptEditor.setPropagateVariableEvaluation(!preferences.noVariableEvaluationPropagation);
        if (preferences.fontOutput != null){
            outputPanel.setTextPaneFont(preferences.fontOutput.toFont());
        }
        devicesPanel.setAsyncUpdate(preferences.asyncViewersUpdate);
        DevicePanel.setDefaultPanelPrecision(preferences.getDefaultPanelPrecision());
        dataPanel.setCached(preferences.cachedDataPanel);
        dataPanel.setPlottingScripts(preferences.processingScripts);
        MotorPanel.setDefaultShowHoming(preferences.showHomingButtons);
        MotorPanel.setDefaultShowJog(preferences.showJogButtons);
        for (int i = 0; i < tabDoc.getTabCount(); i++) {
            if (tabDoc.getComponentAt(i) instanceof ScriptEditor scriptEditor) {
                ScriptEditor editor = scriptEditor;
                if (preferences.fontEditor != null){
                    editor.setTextPaneFont(preferences.fontEditor.toFont());
                }
                editor.setContentWidth((preferences.contentWidth <= 0) ? DEFAULT_CONTENT_WIDTH : preferences.contentWidth);
                editor.setEditorForeground(preferences.getEditorForegroundColor());
                editor.setEditorBackground(preferences.getEditorBackgroundColor());
            }
        }
        showEmergencyStop(preferences.showEmergencyStop && !Setup.isOffline());
        PlotBase.setPlotBackground(preferences.getPlotBackgroundColor());
        PlotBase.setGridColor(preferences.getPlotGridColor());
        PlotBase.setOutlineColor(preferences.getPlotOutlineColor());
        if (preferences.fontPlotLabel != null){
            PlotBase.setDefaultLabelFont(preferences.fontPlotLabel.toFont());
        }
        if (preferences.fontPlotTick != null){
            PlotBase.setDefaultTickFont(preferences.fontPlotTick.toFont());
        }
        PlotBase.setOffscreenBuffer(!preferences.disableOffscreenBuffer);
        if (preferences.fontPlotTitle != null){
            PlotPanel.setTitleFont(preferences.fontPlotTitle.toFont());
        }
        HistoryChart.setDefaultAsync(preferences.asyncHistoryPlotsUpdate);

        if (preferences.linePlot != null) {
            System.setProperty(PlotPanel.PROPERTY_PLOT_IMPL_LINE, preferences.linePlot);
        }
        if (preferences.matrixPlot != null) {
            System.setProperty(PlotPanel.PROPERTY_PLOT_IMPL_MATRIX, preferences.matrixPlot);
        }
        if (preferences.surfacePlot != null) {
            System.setProperty(PlotPanel.PROPERTY_PLOT_IMPL_SURFACE, preferences.surfacePlot);
        }
        if (preferences.timePlot != null) {
            System.setProperty(PlotPanel.PROPERTY_PLOT_IMPL_TIME, preferences.timePlot);
        }
        if (preferences.quality != null) {            
            System.setProperty(PlotPanel.PROPERTY_PLOT_QUALITY, String.valueOf(preferences.quality));
        }
        if (preferences.plotLayout != null) {
            System.setProperty(PlotPanel.PROPERTY_PLOT_LAYOUT, String.valueOf(preferences.plotLayout));
        }
        if (preferences.defaultPlotColormap != null) {
            PlotBase.setDefaultColormap(preferences.defaultPlotColormap);
        }
        System.setProperty(PlotBase.PROPERTY_PLOT_MARKER_SIZE, String.valueOf(preferences.markerSize));

        //if (!App.isLocalMode()) {
            setScanPlotVisible(!preferences.plotsHidden);
            setScanPlotDetached(preferences.plotsDetached);
            setConsoleLocation(preferences.consoleLocation);
        //}
        
        setDataPanelLocation(preferences.dataPanelLocation);
        dataPanel.setEmbedded(!preferences.openDataFilesInDocTab);
        
        statusBar.setShowDataFileName(true);
        
        setScanPanelVisible(!preferences.hideScanPanel);
        setOutputPanelVisible(!preferences.hideOutputPanel);        
    }


    void showEmergencyStop(boolean value) {
        separatorStopAll.setVisible(value);
        buttonStopAll.setVisible(value);
        menuStopAll.setVisible(value);
    }

    //Public interface to Plugins
    public JTabbedPane getDocumentsTab() {
        return tabDoc;
    }

    public JTabbedPane getPlotsTab() {
        return tabPlots;
    }

    public JTabbedPane getStatusTab() {
        return tabStatus;
    }

    public JTabbedPane getLeftTab() {
        return tabLeft;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public JMenuBar getMenu() {
        return menuBar;
    }

    public ch.psi.pshell.swing.Shell getShell() {
        return shell;
    }
    
    @Override
    public Object openFile(File f, Processor processor) throws Exception {
        String ext = IO.getExtension(f);
        if (StripChart.FILE_EXTENSION.equals(ext)) {
            StripChart stripChart = new StripChart(View.this, false, null);
            openComponent(f.getName().trim(), stripChart.getPlotPanel());
            stripChart.open(f);
            stripChart.start();
        }
        return super.openFile(f, processor);
    }    
    

    //Drag and drop
    private DropTargetListener dropListner;
    volatile boolean dropping;

    private class DropTargetListener implements java.awt.dnd.DropTargetListener {

        Cursor dropCursor;
        DropTarget target;

        void enable() {
            target = new DropTarget(tabDoc, DnDConstants.ACTION_COPY, this);
            //In other platforms a move cursor is added by the system
            if (Sys.getOSFamily() == OSFamily.Mac) {
                try {
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    Image image = toolkit.getImage(View.this.getClass().getResource("/ch/psi/pshell/ui/Add.png"));
                    dropCursor = toolkit.createCustomCursor(image, new Point(12, 10), "img");
                } catch (Exception ex) {
                    dropCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                }
            }
        }

        boolean isEnabled() {
            return target != null;
        }

        private boolean isSupported(File file) {
            if (file.isFile()) {
                return true;
            }
            return false;
        }

        void start() {
            if (isEnabled()) {
                dropping = true;
                if (dropCursor != null) {
                    tabDoc.setCursor(dropCursor);
                }
                tabDoc.repaint();
            }
        }

        void stop() {
            if (isEnabled()) {
                dropping = false;
                if (dropCursor != null) {
                    tabDoc.setCursor(Cursor.getDefaultCursor());
                }
                tabDoc.repaint();
            }
        }

        private List<File> getFiles(DropTargetDropEvent e) {
            ArrayList<File> ret = new ArrayList<>();
            try {
                List<File> fileList = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : fileList) {
                    if (isSupported(file)) {
                        ret.add(file);
                    }
                }
            } catch (Exception ex) {
            }
            try {
                String fileName = (String) e.getTransferable().getTransferData(DataFlavor.stringFlavor);
                File file = new File(fileName);
                if (isSupported(file)) {
                    ret.add(file);
                }
            } catch (Exception ex) {
            }
            return ret;
        }

        @Override
        public void dragEnter(DropTargetDragEvent e) {
            start();
        }

        @Override
        public void dragExit(DropTargetEvent e) {
            stop();
        }

        @Override
        public void dragOver(DropTargetDragEvent e) {
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent e) {
        }

        @Override
        public void drop(DropTargetDropEvent e) {
            try {
                e.acceptDrop(DnDConstants.ACTION_COPY);
                List<File> fileList = getFiles(e);
                if (getFiles(e).size() > 0) {
                    for (File file : fileList) {
                        openFile(file);
                    }
                } else {
                    e.rejectDrop();
                }
                e.getDropTargetContext().dropComplete(true);
            } catch (Exception ex) {
                logger.log(Level.FINE, null, ex);
            }
            stop();
        }
    }
    
    Terminal terminal;
    
    
    
    public int getTerminalIndexTabStatus() {
        for (int i = 0; i < tabStatus.getTabCount(); i++) {
            Component c = tabStatus.getComponentAt(i);
            if (c == terminal) {
                return i;
            }
        }    
        return -1;
    }    
    
    boolean isTerminalVisible(){
        return getTerminalIndexTabStatus()>=0;
    }
    
    void showTerminal() throws IOException{
        if (isTerminalVisible()){
            int index = getTerminalIndexTabStatus();
            if (index>=0){
                tabStatus.setSelectedComponent(terminal);
                return;
            }
        }
        terminal = new Terminal(Setup.getHomePath(), preferences.fontTerminal.toFont());
        tabStatus.addTab("Terminal", terminal);
        int index = tabStatus.getTabCount() - 1;
        SwingUtils.setTabClosable(tabStatus, index);
        tabStatus.setSelectedComponent(terminal);
    }
    
    void hideTerminal(){
        int index = getTerminalIndexTabStatus();
        if (index>=0){
            tabStatus.remove(terminal);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupPlotsVisibility = new javax.swing.ButtonGroup();
        mainPanel = new javax.swing.JPanel();
        splitterHoriz = new javax.swing.JSplitPane();
        splitterVert = new javax.swing.JSplitPane();
        tabStatus = new javax.swing.JTabbedPane();
        loggerPanel = new ch.psi.pshell.swing.LoggerPanel();
        devicesPanel = new ch.psi.pshell.swing.DevicePoolPanel();
        imagingPanel = new ch.psi.pshell.swing.DevicePoolPanel();
        scanPanel = new ch.psi.pshell.swing.ScanPanel();
        outputPanel = new ch.psi.pshell.swing.OutputPanel();
        scriptsPanel = new ch.psi.pshell.swing.ScriptsPanel();
        dataPanel = new ch.psi.pshell.swing.DataPanel();
        splitterDoc = new javax.swing.JSplitPane();
        tabLeft = new javax.swing.JTabbedPane();
        tabDoc =
        new javax.swing.JTabbedPane() {
            @Override
            public void paintComponent(Graphics g){
                try{
                    super.paintComponent(g);
                    if (dropping){
                        g.setColor(new Color(44, 144, 254));
                        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                    }
                } catch (Exception ex){
                }
            }
        };
        shell = new ch.psi.pshell.swing.Shell();
        tabPlots = new javax.swing.JTabbedPane();
        scanPlot = new ch.psi.pshell.swing.PlotPanel();
        statusBar = new ch.psi.pshell.framework.StatusBar();
        toolBar = new javax.swing.JToolBar();
        buttonNew = new javax.swing.JButton();
        buttonOpen = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        buttonRestart = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        buttonRun = new javax.swing.JButton();
        buttonDebug = new javax.swing.JButton();
        buttonStep = new javax.swing.JButton();
        buttonPause = new javax.swing.JButton();
        buttonAbort = new javax.swing.JButton();
        separatorStopAll = new javax.swing.JToolBar.Separator();
        buttonStopAll = new javax.swing.JButton();
        separatorInfo = new javax.swing.JToolBar.Separator();
        buttonAbout = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        labelUser = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuFileNew = new javax.swing.JMenu();
        menuNew = new javax.swing.JMenuItem();
        menuOpen = new javax.swing.JMenuItem();
        menuSave = new javax.swing.JMenuItem();
        menuSaveAs = new javax.swing.JMenuItem();
        menuAddToQueue = new javax.swing.JMenu();
        menuOpenRecent = new javax.swing.JMenu();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        menuImport = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        menuLogQuery = new javax.swing.JMenuItem();
        menuOpenLogFile = new javax.swing.JMenuItem();
        jSeparator24 = new javax.swing.JPopupMenu.Separator();
        menuDataFile = new javax.swing.JMenuItem();
        menuPlugins = new javax.swing.JMenuItem();
        menuTasks = new javax.swing.JMenuItem();
        menuUsers = new javax.swing.JMenuItem();
        menuXscan = new javax.swing.JMenuItem();
        menuConfiguration = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem menuExit = new javax.swing.JMenuItem();
        menuEdit = new javax.swing.JMenu();
        menuUndo = new javax.swing.JMenuItem();
        menuRedo = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        menuCut = new javax.swing.JMenuItem();
        menuCopy = new javax.swing.JMenuItem();
        menuPaste = new javax.swing.JMenuItem();
        menuBlock = new javax.swing.JMenu();
        menuIndent = new javax.swing.JMenuItem();
        menuUnindent = new javax.swing.JMenuItem();
        jSeparator21 = new javax.swing.JPopupMenu.Separator();
        menuComment = new javax.swing.JMenuItem();
        menuUncomment = new javax.swing.JMenuItem();
        menuToggleComment = new javax.swing.JMenuItem();
        jSeparator18 = new javax.swing.JPopupMenu.Separator();
        menuFind = new javax.swing.JMenuItem();
        menuFindNext = new javax.swing.JMenuItem();
        menuFindInFiles = new javax.swing.JMenuItem();
        menuShell = new javax.swing.JMenu();
        menuRestart = new javax.swing.JMenuItem();
        jSeparator19 = new javax.swing.JPopupMenu.Separator();
        menuSetLogLevel = new javax.swing.JMenu();
        menuSettings = new javax.swing.JMenuItem();
        menuChangeUser = new javax.swing.JMenuItem();
        menuCPython = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        menuCheckSyntax = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        menuRun = new javax.swing.JMenuItem();
        menuRunNext = new javax.swing.JMenuItem();
        menuDebug = new javax.swing.JMenuItem();
        menuStep = new javax.swing.JMenuItem();
        menuPause = new javax.swing.JMenuItem();
        menuAbort = new javax.swing.JMenuItem();
        menuDevices = new javax.swing.JMenu();
        menuDevicesDefinition = new javax.swing.JMenuItem();
        menuDevicesConfig = new javax.swing.JMenu();
        menuDevicesEpics = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        menuUpdateAll = new javax.swing.JMenuItem();
        menuReinit = new javax.swing.JMenuItem();
        menuStopAll = new javax.swing.JMenuItem();
        jSeparator22 = new javax.swing.JPopupMenu.Separator();
        menuCamServer = new javax.swing.JMenu();
        menuCamServerViewer = new javax.swing.JMenuItem();
        menuCamServerCameras = new javax.swing.JMenuItem();
        menuCamServerPipelines = new javax.swing.JMenuItem();
        menuArchiver = new javax.swing.JMenu();
        manuDaqbuf = new javax.swing.JMenuItem();
        menuStripChart = new javax.swing.JMenuItem();
        menuSessions = new javax.swing.JMenu();
        menuSessionStart = new javax.swing.JMenuItem();
        menuSessionReopen = new javax.swing.JMenuItem();
        menuSessionPause = new javax.swing.JMenuItem();
        menuSessionResume = new javax.swing.JMenuItem();
        menuSessionStop = new javax.swing.JMenuItem();
        jSeparator25 = new javax.swing.JPopupMenu.Separator();
        menuSessionCreate = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        menuSessionsMetadata = new javax.swing.JMenuItem();
        jSeparator23 = new javax.swing.JPopupMenu.Separator();
        menuSessionHistory = new javax.swing.JMenuItem();
        menuVersioning = new javax.swing.JMenu();
        menuSetCurrentBranch = new javax.swing.JMenuItem();
        menuCreateBranch = new javax.swing.JMenuItem();
        menuDeleteBranch = new javax.swing.JMenuItem();
        jSeparator14 = new javax.swing.JPopupMenu.Separator();
        menuSetCurrentBranch1 = new javax.swing.JMenuItem();
        menuCreateTag = new javax.swing.JMenuItem();
        menuDeleteTag = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        menuChanges = new javax.swing.JMenuItem();
        menuCommit = new javax.swing.JMenuItem();
        menuPull = new javax.swing.JMenuItem();
        menuPush = new javax.swing.JMenuItem();
        menuView = new javax.swing.JMenu();
        menuFullScreen = new javax.swing.JCheckBoxMenuItem();
        menuLaf = new javax.swing.JMenu();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        menuViewPanels = new javax.swing.JMenu();
        menuConsoleLocation = new javax.swing.JMenu();
        menuDataPanelLocation = new javax.swing.JMenu();
        menuPlotWindow = new javax.swing.JMenu();
        radioPlotsVisible = new javax.swing.JRadioButtonMenuItem();
        radioPlotsDetached = new javax.swing.JRadioButtonMenuItem();
        radioPlotsHidden = new javax.swing.JRadioButtonMenuItem();
        menuViewPanels1 = new javax.swing.JMenu();
        menuOutput = new javax.swing.JCheckBoxMenuItem();
        menuScanPanel = new javax.swing.JCheckBoxMenuItem();
        menuTerminal = new javax.swing.JCheckBoxMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        menuCloseAllPlots = new javax.swing.JMenuItem();
        menuCloseAll = new javax.swing.JMenuItem();
        jSeparator20 = new javax.swing.JPopupMenu.Separator();
        menuPreferences = new javax.swing.JMenuItem();
        javax.swing.JMenu menuHelp = new javax.swing.JMenu();
        menuHelpContents = new javax.swing.JMenuItem();
        jSeparator15 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem menuSetup = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem menuAbout = new javax.swing.JMenuItem();

        setName("MainFrame"); // NOI18N

        mainPanel.setName("mainPanel"); // NOI18N

        splitterHoriz.setDividerLocation(600);
        splitterHoriz.setResizeWeight(1.0);
        splitterHoriz.setName("splitterHoriz"); // NOI18N

        splitterVert.setDividerLocation(420);
        splitterVert.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitterVert.setResizeWeight(1.0);
        splitterVert.setName("splitterVert"); // NOI18N

        tabStatus.setMinimumSize(new java.awt.Dimension(94, 0));
        tabStatus.setName("tabStatus"); // NOI18N
        tabStatus.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabStatusStateChanged(evt);
            }
        });

        loggerPanel.setName("loggerPanel"); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/workbench/View"); // NOI18N
        tabStatus.addTab(bundle.getString("View.loggerPanel.TabConstraints.tabTitle"), loggerPanel); // NOI18N

        devicesPanel.setName("devicesPanel"); // NOI18N
        tabStatus.addTab(bundle.getString("View.devicesPanel.TabConstraints.tabTitle"), devicesPanel); // NOI18N

        imagingPanel.setName("imagingPanel"); // NOI18N
        tabStatus.addTab(bundle.getString("View.imagingPanel.TabConstraints.tabTitle"), imagingPanel); // NOI18N

        scanPanel.setName("scanPanel"); // NOI18N
        tabStatus.addTab(bundle.getString("View.scanPanel.TabConstraints.tabTitle"), scanPanel); // NOI18N

        outputPanel.setName("outputPanel"); // NOI18N
        tabStatus.addTab(bundle.getString("View.outputPanel.TabConstraints.tabTitle"), outputPanel); // NOI18N

        scriptsPanel.setName("scriptsPanel"); // NOI18N
        tabStatus.addTab(bundle.getString("View.scriptsPanel.TabConstraints.tabTitle"), scriptsPanel); // NOI18N

        dataPanel.setName("dataPanel"); // NOI18N
        tabStatus.addTab(bundle.getString("View.dataPanel.TabConstraints.tabTitle"), dataPanel); // NOI18N

        splitterVert.setBottomComponent(tabStatus);

        splitterDoc.setDividerSize(0);
        splitterDoc.setName("splitterDoc"); // NOI18N

        tabLeft.setName("tabLeft"); // NOI18N
        tabLeft.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabLeftStateChanged(evt);
            }
        });
        splitterDoc.setLeftComponent(tabLeft);

        tabDoc.setName("tabDoc"); // NOI18N
        tabDoc.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabDocStateChanged(evt);
            }
        });

        shell.setName("shell"); // NOI18N
        tabDoc.addTab(bundle.getString("View.shell.TabConstraints.tabTitle"), shell); // NOI18N

        splitterDoc.setRightComponent(tabDoc);

        splitterVert.setLeftComponent(splitterDoc);

        splitterHoriz.setLeftComponent(splitterVert);

        tabPlots.setName("tabPlots"); // NOI18N
        tabPlots.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabPlotsStateChanged(evt);
            }
        });

        scanPlot.setName("scanPlot"); // NOI18N

        javax.swing.GroupLayout scanPlotLayout = new javax.swing.GroupLayout(scanPlot);
        scanPlot.setLayout(scanPlotLayout);
        scanPlotLayout.setHorizontalGroup(
            scanPlotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 439, Short.MAX_VALUE)
        );
        scanPlotLayout.setVerticalGroup(
            scanPlotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 581, Short.MAX_VALUE)
        );

        tabPlots.addTab(bundle.getString("View.scanPlot.TabConstraints.tabTitle"), scanPlot); // NOI18N

        splitterHoriz.setRightComponent(tabPlots);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitterHoriz, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitterHoriz, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)
        );

        statusBar.setName("statusBar"); // NOI18N

        toolBar.setName("toolBar"); // NOI18N

        buttonNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/New.png"))); // NOI18N
        buttonNew.setText(bundle.getString("View.buttonNew.text")); // NOI18N
        buttonNew.setToolTipText(bundle.getString("View.buttonNew.toolTipText")); // NOI18N
        buttonNew.setFocusable(false);
        buttonNew.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonNew.setName("buttonNew"); // NOI18N
        buttonNew.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuNewActionPerformed(evt);
            }
        });
        toolBar.add(buttonNew);

        buttonOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Open.png"))); // NOI18N
        buttonOpen.setText(bundle.getString("View.buttonOpen.text")); // NOI18N
        buttonOpen.setToolTipText(bundle.getString("View.buttonOpen.toolTipText")); // NOI18N
        buttonOpen.setFocusable(false);
        buttonOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOpen.setName("buttonOpen"); // NOI18N
        buttonOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenActionPerformed(evt);
            }
        });
        toolBar.add(buttonOpen);

        buttonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Save.png"))); // NOI18N
        buttonSave.setText(bundle.getString("View.buttonSave.text")); // NOI18N
        buttonSave.setToolTipText(bundle.getString("View.buttonSave.toolTipText")); // NOI18N
        buttonSave.setFocusable(false);
        buttonSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSave.setName("buttonSave"); // NOI18N
        buttonSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveActionPerformed(evt);
            }
        });
        toolBar.add(buttonSave);

        jSeparator5.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator5.setName("jSeparator5"); // NOI18N
        jSeparator5.setPreferredSize(new java.awt.Dimension(20, 0));
        jSeparator5.setRequestFocusEnabled(false);
        toolBar.add(jSeparator5);

        buttonRestart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Reset.png"))); // NOI18N
        buttonRestart.setToolTipText(bundle.getString("View.buttonRestart.toolTipText")); // NOI18N
        buttonRestart.setFocusable(false);
        buttonRestart.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRestart.setName("buttonRestart"); // NOI18N
        buttonRestart.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRestartActionPerformed(evt);
            }
        });
        toolBar.add(buttonRestart);

        jSeparator6.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator6.setName("jSeparator6"); // NOI18N
        jSeparator6.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(jSeparator6);

        buttonRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Play.png"))); // NOI18N
        buttonRun.setText(bundle.getString("View.buttonRun.text")); // NOI18N
        buttonRun.setToolTipText(bundle.getString("View.buttonRun.toolTipText")); // NOI18N
        buttonRun.setFocusable(false);
        buttonRun.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRun.setName("buttonRun"); // NOI18N
        buttonRun.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRunActionPerformed(evt);
            }
        });
        toolBar.add(buttonRun);

        buttonDebug.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Step.png"))); // NOI18N
        buttonDebug.setToolTipText(bundle.getString("View.buttonDebug.toolTipText")); // NOI18N
        buttonDebug.setFocusable(false);
        buttonDebug.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonDebug.setName("buttonDebug"); // NOI18N
        buttonDebug.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDebugActionPerformed(evt);
            }
        });
        toolBar.add(buttonDebug);

        buttonStep.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/StepInto.png"))); // NOI18N
        buttonStep.setToolTipText(bundle.getString("View.buttonStep.toolTipText")); // NOI18N
        buttonStep.setFocusable(false);
        buttonStep.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonStep.setName("buttonStep"); // NOI18N
        buttonStep.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStepActionPerformed(evt);
            }
        });
        toolBar.add(buttonStep);

        buttonPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Pause.png"))); // NOI18N
        buttonPause.setText(bundle.getString("View.buttonPause.text")); // NOI18N
        buttonPause.setToolTipText(bundle.getString("View.buttonPause.toolTipText")); // NOI18N
        buttonPause.setFocusable(false);
        buttonPause.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonPause.setName("buttonPause"); // NOI18N
        buttonPause.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPauseActionPerformed(evt);
            }
        });
        toolBar.add(buttonPause);

        buttonAbort.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Stop.png"))); // NOI18N
        buttonAbort.setText(bundle.getString("View.buttonAbort.text")); // NOI18N
        buttonAbort.setToolTipText(bundle.getString("View.buttonAbort.toolTipText")); // NOI18N
        buttonAbort.setFocusable(false);
        buttonAbort.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonAbort.setName("buttonAbort"); // NOI18N
        buttonAbort.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAbortActionPerformed(evt);
            }
        });
        toolBar.add(buttonAbort);

        separatorStopAll.setMaximumSize(new java.awt.Dimension(20, 32767));
        separatorStopAll.setName("separatorStopAll"); // NOI18N
        separatorStopAll.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(separatorStopAll);

        buttonStopAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Abort.png"))); // NOI18N
        buttonStopAll.setToolTipText(bundle.getString("View.buttonStopAll.toolTipText")); // NOI18N
        buttonStopAll.setFocusable(false);
        buttonStopAll.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonStopAll.setName("buttonStopAll"); // NOI18N
        buttonStopAll.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonStopAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuStopAllActionPerformed(evt);
            }
        });
        toolBar.add(buttonStopAll);

        separatorInfo.setMaximumSize(new java.awt.Dimension(20, 32767));
        separatorInfo.setName("separatorInfo"); // NOI18N
        separatorInfo.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(separatorInfo);

        buttonAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Info.png"))); // NOI18N
        buttonAbout.setText(bundle.getString("View.buttonAbout.text")); // NOI18N
        buttonAbout.setToolTipText(bundle.getString("View.buttonAbout.toolTipText")); // NOI18N
        buttonAbout.setFocusable(false);
        buttonAbout.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonAbout.setName("buttonAbout"); // NOI18N
        buttonAbout.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAboutActionPerformed(evt);
            }
        });
        toolBar.add(buttonAbout);

        filler1.setName("filler1"); // NOI18N
        toolBar.add(filler1);

        labelUser.setText(bundle.getString("View.labelUser.text_1")); // NOI18N
        labelUser.setName("labelUser"); // NOI18N
        toolBar.add(labelUser);

        filler2.setName("filler2"); // NOI18N
        toolBar.add(filler2);

        filler3.setName("filler3"); // NOI18N

        menuBar.setName("menuBar"); // NOI18N

        menuFile.setText(bundle.getString("View.menuFile.text")); // NOI18N
        menuFile.setName("menuFile"); // NOI18N
        menuFile.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuFileStateChanged(evt);
            }
        });

        menuFileNew.setText(bundle.getString("View.menuFileNew.text_1")); // NOI18N
        menuFileNew.setName("menuFileNew"); // NOI18N
        menuFileNew.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuFileNewMouseClicked(evt);
            }
        });

        menuNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuNew.setText(bundle.getString("View.menuNew.text")); // NOI18N
        menuNew.setName("menuNew"); // NOI18N
        menuNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuNewActionPerformed(evt);
            }
        });
        menuFileNew.add(menuNew);

        menuFile.add(menuFileNew);

        menuOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuOpen.setText(bundle.getString("View.menuOpen.text")); // NOI18N
        menuOpen.setName("menuOpen"); // NOI18N
        menuOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenActionPerformed(evt);
            }
        });
        menuFile.add(menuOpen);

        menuSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuSave.setText(bundle.getString("View.menuSave.text")); // NOI18N
        menuSave.setName("menuSave"); // NOI18N
        menuSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveActionPerformed(evt);
            }
        });
        menuFile.add(menuSave);

        menuSaveAs.setText(bundle.getString("View.menuSaveAs.text")); // NOI18N
        menuSaveAs.setName("menuSaveAs"); // NOI18N
        menuSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveAsActionPerformed(evt);
            }
        });
        menuFile.add(menuSaveAs);

        menuAddToQueue.setText(bundle.getString("View.menuAddToQueue.text")); // NOI18N
        menuAddToQueue.setName("menuAddToQueue"); // NOI18N
        menuFile.add(menuAddToQueue);

        menuOpenRecent.setText(bundle.getString("View.menuOpenRecent.text")); // NOI18N
        menuOpenRecent.setName("menuOpenRecent"); // NOI18N
        menuFile.add(menuOpenRecent);

        jSeparator2.setName("jSeparator2"); // NOI18N
        menuFile.add(jSeparator2);

        menuImport.setText(bundle.getString("View.menuImport.text")); // NOI18N
        menuImport.setName("menuImport"); // NOI18N
        menuImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuImportActionPerformed(evt);
            }
        });
        menuFile.add(menuImport);

        jSeparator3.setName("jSeparator3"); // NOI18N
        menuFile.add(jSeparator3);

        menuLogQuery.setText(bundle.getString("View.menuLogQuery.text")); // NOI18N
        menuLogQuery.setName("menuLogQuery"); // NOI18N
        menuLogQuery.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLogQueryActionPerformed(evt);
            }
        });
        menuFile.add(menuLogQuery);

        menuOpenLogFile.setText(bundle.getString("View.menuOpenLogFile.text")); // NOI18N
        menuOpenLogFile.setName("menuOpenLogFile"); // NOI18N
        menuOpenLogFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenLogFileActionPerformed(evt);
            }
        });
        menuFile.add(menuOpenLogFile);

        jSeparator24.setName("jSeparator24"); // NOI18N
        menuFile.add(jSeparator24);

        menuDataFile.setText(bundle.getString("View.menuDataFile.text")); // NOI18N
        menuDataFile.setName("menuDataFile"); // NOI18N
        menuDataFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDataFileActionPerformed(evt);
            }
        });
        menuFile.add(menuDataFile);

        menuPlugins.setText(bundle.getString("View.menuPlugins.text")); // NOI18N
        menuPlugins.setName("menuPlugins"); // NOI18N
        menuPlugins.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPluginsActionPerformed(evt);
            }
        });
        menuFile.add(menuPlugins);

        menuTasks.setText(bundle.getString("View.menuTasks.text")); // NOI18N
        menuTasks.setName("menuTasks"); // NOI18N
        menuTasks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuTasksActionPerformed(evt);
            }
        });
        menuFile.add(menuTasks);

        menuUsers.setText(bundle.getString("View.menuUsers.text")); // NOI18N
        menuUsers.setName("menuUsers"); // NOI18N
        menuUsers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuUsersbuttonAbortActionPerformed(evt);
            }
        });
        menuFile.add(menuUsers);

        menuXscan.setText(bundle.getString("View.menuXscan.text")); // NOI18N
        menuXscan.setName("menuXscan"); // NOI18N
        menuXscan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuXscanActionPerformed(evt);
            }
        });
        menuFile.add(menuXscan);

        menuConfiguration.setText(bundle.getString("View.menuConfiguration.text")); // NOI18N
        menuConfiguration.setName("menuConfiguration"); // NOI18N
        menuConfiguration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuConfigurationActionPerformed(evt);
            }
        });
        menuFile.add(menuConfiguration);

        jSeparator11.setName("jSeparator11"); // NOI18N
        menuFile.add(jSeparator11);

        menuExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuExit.setText(bundle.getString("View.menuExit.text")); // NOI18N
        menuExit.setName("menuExit"); // NOI18N
        menuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExitActionPerformed(evt);
            }
        });
        menuFile.add(menuExit);

        menuBar.add(menuFile);

        menuEdit.setText(bundle.getString("View.menuEdit.text_1")); // NOI18N
        menuEdit.setName("menuEdit"); // NOI18N
        menuEdit.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuEditStateChanged(evt);
            }
        });

        menuUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuUndo.setText(bundle.getString("View.menuUndo.text")); // NOI18N
        menuUndo.setName("menuUndo"); // NOI18N
        menuUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuUndoActionPerformed(evt);
            }
        });
        menuEdit.add(menuUndo);

        menuRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuRedo.setText(bundle.getString("View.menuRedo.text")); // NOI18N
        menuRedo.setName("menuRedo"); // NOI18N
        menuRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRedoActionPerformed(evt);
            }
        });
        menuEdit.add(menuRedo);

        jSeparator17.setName("jSeparator17"); // NOI18N
        menuEdit.add(jSeparator17);

        menuCut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuCut.setText(bundle.getString("View.menuCut.text")); // NOI18N
        menuCut.setName("menuCut"); // NOI18N
        menuCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCutActionPerformed(evt);
            }
        });
        menuEdit.add(menuCut);

        menuCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuCopy.setText(bundle.getString("View.menuCopy.text")); // NOI18N
        menuCopy.setName("menuCopy"); // NOI18N
        menuCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCopyActionPerformed(evt);
            }
        });
        menuEdit.add(menuCopy);

        menuPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuPaste.setText(bundle.getString("View.menuPaste.text")); // NOI18N
        menuPaste.setName("menuPaste"); // NOI18N
        menuPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPasteActionPerformed(evt);
            }
        });
        menuEdit.add(menuPaste);

        menuBlock.setText(bundle.getString("View.menuBlock.text_1")); // NOI18N
        menuBlock.setName("menuBlock"); // NOI18N

        menuIndent.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PERIOD, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuIndent.setText(bundle.getString("View.menuIndent.text")); // NOI18N
        menuIndent.setName("menuIndent"); // NOI18N
        menuIndent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuIndentActionPerformed(evt);
            }
        });
        menuBlock.add(menuIndent);

        menuUnindent.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_COMMA, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuUnindent.setText(bundle.getString("View.menuUnindent.text")); // NOI18N
        menuUnindent.setName("menuUnindent"); // NOI18N
        menuUnindent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuUnindentActionPerformed(evt);
            }
        });
        menuBlock.add(menuUnindent);

        jSeparator21.setName("jSeparator21"); // NOI18N
        menuBlock.add(jSeparator21);

        menuComment.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PERIOD, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuComment.setText(bundle.getString("View.menuComment.text")); // NOI18N
        menuComment.setName("menuComment"); // NOI18N
        menuComment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCommentActionPerformed(evt);
            }
        });
        menuBlock.add(menuComment);

        menuUncomment.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_COMMA, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuUncomment.setText(bundle.getString("View.menuUncomment.text")); // NOI18N
        menuUncomment.setName("menuUncomment"); // NOI18N
        menuUncomment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuUncommentActionPerformed(evt);
            }
        });
        menuBlock.add(menuUncomment);

        menuToggleComment.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SLASH, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuToggleComment.setText(bundle.getString("View.menuToggleComment.text")); // NOI18N
        menuToggleComment.setName("menuToggleComment"); // NOI18N
        menuToggleComment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuToggleCommentActionPerformed(evt);
            }
        });
        menuBlock.add(menuToggleComment);

        menuEdit.add(menuBlock);

        jSeparator18.setName("jSeparator18"); // NOI18N
        menuEdit.add(jSeparator18);

        menuFind.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFind.setText(bundle.getString("View.menuFind.text")); // NOI18N
        menuFind.setName("menuFind"); // NOI18N
        menuFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFindActionPerformed(evt);
            }
        });
        menuEdit.add(menuFind);

        menuFindNext.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        menuFindNext.setText(bundle.getString("View.menuFindNext.text")); // NOI18N
        menuFindNext.setName("menuFindNext"); // NOI18N
        menuFindNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFindNextActionPerformed(evt);
            }
        });
        menuEdit.add(menuFindNext);

        menuFindInFiles.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFindInFiles.setText(bundle.getString("View.menuFindInFiles.text")); // NOI18N
        menuFindInFiles.setName("menuFindInFiles"); // NOI18N
        menuFindInFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFindInFilesActionPerformed(evt);
            }
        });
        menuEdit.add(menuFindInFiles);

        menuBar.add(menuEdit);

        menuShell.setText(bundle.getString("View.menuShell.text")); // NOI18N
        menuShell.setName("menuShell"); // NOI18N
        menuShell.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuShellStateChanged(evt);
            }
        });

        menuRestart.setText(bundle.getString("View.menuRestart.text")); // NOI18N
        menuRestart.setName("menuRestart"); // NOI18N
        menuRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRestartActionPerformed(evt);
            }
        });
        menuShell.add(menuRestart);

        jSeparator19.setName("jSeparator19"); // NOI18N
        menuShell.add(jSeparator19);

        menuSetLogLevel.setText(bundle.getString("View.menuSetLogLevel.text")); // NOI18N
        menuSetLogLevel.setName("menuSetLogLevel"); // NOI18N
        menuShell.add(menuSetLogLevel);

        menuSettings.setText(bundle.getString("View.menuSettings.text")); // NOI18N
        menuSettings.setName("menuSettings"); // NOI18N
        menuSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSettingsbuttonAbortActionPerformed(evt);
            }
        });
        menuShell.add(menuSettings);

        menuChangeUser.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuChangeUser.setText(bundle.getString("View.menuChangeUser.text")); // NOI18N
        menuChangeUser.setEnabled(false);
        menuChangeUser.setName("menuChangeUser"); // NOI18N
        menuChangeUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuChangeUserActionPerformed(evt);
            }
        });
        menuShell.add(menuChangeUser);

        menuCPython.setText(bundle.getString("View.menuCPython.text")); // NOI18N
        menuCPython.setName("menuCPython"); // NOI18N
        menuCPython.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCPythonActionPerformed(evt);
            }
        });
        menuShell.add(menuCPython);

        jSeparator4.setName("jSeparator4"); // NOI18N
        menuShell.add(jSeparator4);

        menuCheckSyntax.setText(bundle.getString("View.menuCheckSyntax.text")); // NOI18N
        menuCheckSyntax.setName("menuCheckSyntax"); // NOI18N
        menuCheckSyntax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCheckSyntaxbuttonRunActionPerformed(evt);
            }
        });
        menuShell.add(menuCheckSyntax);

        jSeparator9.setName("jSeparator9"); // NOI18N
        menuShell.add(jSeparator9);

        menuRun.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        menuRun.setText(bundle.getString("View.menuRun.text")); // NOI18N
        menuRun.setName("menuRun"); // NOI18N
        menuRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRunActionPerformed(evt);
            }
        });
        menuShell.add(menuRun);

        menuRunNext.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, java.awt.event.InputEvent.ALT_DOWN_MASK));
        menuRunNext.setText(bundle.getString("View.menuRunNext.text")); // NOI18N
        menuRunNext.setName("menuRunNext"); // NOI18N
        menuRunNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRunNextbuttonRunActionPerformed(evt);
            }
        });
        menuShell.add(menuRunNext);

        menuDebug.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuDebug.setText(bundle.getString("View.menuDebug.text")); // NOI18N
        menuDebug.setName("menuDebug"); // NOI18N
        menuDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDebugActionPerformed(evt);
            }
        });
        menuShell.add(menuDebug);

        menuStep.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        menuStep.setText(bundle.getString("View.menuStep.text")); // NOI18N
        menuStep.setName("menuStep"); // NOI18N
        menuStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStepActionPerformed(evt);
            }
        });
        menuShell.add(menuStep);

        menuPause.setText(bundle.getString("View.menuPause.text")); // NOI18N
        menuPause.setName("menuPause"); // NOI18N
        menuPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPauseActionPerformed(evt);
            }
        });
        menuShell.add(menuPause);

        menuAbort.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuAbort.setText(bundle.getString("View.menuAbort.text")); // NOI18N
        menuAbort.setName("menuAbort"); // NOI18N
        menuAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAbortActionPerformed(evt);
            }
        });
        menuShell.add(menuAbort);

        menuBar.add(menuShell);

        menuDevices.setText(bundle.getString("View.menuDevices.text")); // NOI18N
        menuDevices.setName("menuDevices"); // NOI18N

        menuDevicesDefinition.setText(bundle.getString("View.menuDevicesDefinition.text")); // NOI18N
        menuDevicesDefinition.setName("menuDevicesDefinition"); // NOI18N
        menuDevicesDefinition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDevicesDefinitionActionPerformed(evt);
            }
        });
        menuDevices.add(menuDevicesDefinition);

        menuDevicesConfig.setText(bundle.getString("View.menuDevicesConfig.text")); // NOI18N
        menuDevicesConfig.setName("menuDevicesConfig"); // NOI18N
        menuDevices.add(menuDevicesConfig);

        menuDevicesEpics.setText(bundle.getString("View.menuDevicesEpics.text")); // NOI18N
        menuDevicesEpics.setName("menuDevicesEpics"); // NOI18N
        menuDevicesEpics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDevicesEpicsActionPerformed(evt);
            }
        });
        menuDevices.add(menuDevicesEpics);

        jSeparator10.setName("jSeparator10"); // NOI18N
        menuDevices.add(jSeparator10);

        menuUpdateAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuUpdateAll.setText(bundle.getString("View.menuUpdateAll.text")); // NOI18N
        menuUpdateAll.setName("menuUpdateAll"); // NOI18N
        menuUpdateAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuUpdateAllActionPerformed(evt);
            }
        });
        menuDevices.add(menuUpdateAll);

        menuReinit.setText(bundle.getString("View.menuReinit.text")); // NOI18N
        menuReinit.setName("menuReinit"); // NOI18N
        menuReinit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuReinitActionPerformed(evt);
            }
        });
        menuDevices.add(menuReinit);

        menuStopAll.setText(bundle.getString("View.menuStopAll.text")); // NOI18N
        menuStopAll.setName("menuStopAll"); // NOI18N
        menuStopAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuStopAllActionPerformed(evt);
            }
        });
        menuDevices.add(menuStopAll);

        jSeparator22.setName("jSeparator22"); // NOI18N
        menuDevices.add(jSeparator22);

        menuCamServer.setText(bundle.getString("View.menuCamServer.text_1")); // NOI18N
        menuCamServer.setName("menuCamServer"); // NOI18N

        menuCamServerViewer.setText(bundle.getString("View.menuCamServerViewer.text")); // NOI18N
        menuCamServerViewer.setName("menuCamServerViewer"); // NOI18N
        menuCamServerViewer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCamServerViewerActionPerformed(evt);
            }
        });
        menuCamServer.add(menuCamServerViewer);

        menuCamServerCameras.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuCamServerCameras.setText(bundle.getString("View.menuCamServerCameras.text")); // NOI18N
        menuCamServerCameras.setName("menuCamServerCameras"); // NOI18N
        menuCamServerCameras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCamServerCamerasActionPerformed(evt);
            }
        });
        menuCamServer.add(menuCamServerCameras);

        menuCamServerPipelines.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_DOWN_MASK));
        menuCamServerPipelines.setText(bundle.getString("View.menuCamServerPipelines.text")); // NOI18N
        menuCamServerPipelines.setName("menuCamServerPipelines"); // NOI18N
        menuCamServerPipelines.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCamServerPipelinesActionPerformed(evt);
            }
        });
        menuCamServer.add(menuCamServerPipelines);

        menuDevices.add(menuCamServer);

        menuArchiver.setText(bundle.getString("View.menuArchiver.text_1")); // NOI18N
        menuArchiver.setName("menuArchiver"); // NOI18N

        manuDaqbuf.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        manuDaqbuf.setText(bundle.getString("View.manuDaqbuf.text")); // NOI18N
        manuDaqbuf.setName("manuDaqbuf"); // NOI18N
        manuDaqbuf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manuDaqbufActionPerformed(evt);
            }
        });
        menuArchiver.add(manuDaqbuf);

        menuDevices.add(menuArchiver);

        menuStripChart.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
        menuStripChart.setText(bundle.getString("View.menuStripChart.text")); // NOI18N
        menuStripChart.setName("menuStripChart"); // NOI18N
        menuStripChart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuStripChartActionPerformed(evt);
            }
        });
        menuDevices.add(menuStripChart);

        menuBar.add(menuDevices);

        menuSessions.setText(bundle.getString("View.menuSessions.text_1")); // NOI18N
        menuSessions.setName("menuSessions"); // NOI18N
        menuSessions.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuSessionsStateChanged(evt);
            }
        });

        menuSessionStart.setText(bundle.getString("View.menuSessionStart.text")); // NOI18N
        menuSessionStart.setName("menuSessionStart"); // NOI18N
        menuSessionStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSessionStartActionPerformed(evt);
            }
        });
        menuSessions.add(menuSessionStart);

        menuSessionReopen.setText(bundle.getString("View.menuSessionReopen.text")); // NOI18N
        menuSessionReopen.setName("menuSessionReopen"); // NOI18N
        menuSessionReopen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSessionReopenActionPerformed(evt);
            }
        });
        menuSessions.add(menuSessionReopen);

        menuSessionPause.setText(bundle.getString("View.menuSessionPause.text")); // NOI18N
        menuSessionPause.setName("menuSessionPause"); // NOI18N
        menuSessionPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSessionPauseActionPerformed(evt);
            }
        });
        menuSessions.add(menuSessionPause);

        menuSessionResume.setText(bundle.getString("View.menuSessionResume.text")); // NOI18N
        menuSessionResume.setName("menuSessionResume"); // NOI18N
        menuSessionResume.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSessionResumeActionPerformed(evt);
            }
        });
        menuSessions.add(menuSessionResume);

        menuSessionStop.setText(bundle.getString("View.menuSessionStop.text")); // NOI18N
        menuSessionStop.setName("menuSessionStop"); // NOI18N
        menuSessionStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSessionStopbuttonAbortActionPerformed(evt);
            }
        });
        menuSessions.add(menuSessionStop);

        jSeparator25.setName("jSeparator25"); // NOI18N
        menuSessions.add(jSeparator25);

        menuSessionCreate.setText(bundle.getString("View.menuSessionCreate.text")); // NOI18N
        menuSessionCreate.setName("menuSessionCreate"); // NOI18N
        menuSessionCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSessionCreateActionPerformed(evt);
            }
        });
        menuSessions.add(menuSessionCreate);

        jSeparator7.setName("jSeparator7"); // NOI18N
        menuSessions.add(jSeparator7);

        menuSessionsMetadata.setText(bundle.getString("View.menuSessionsMetadata.text")); // NOI18N
        menuSessionsMetadata.setName("menuSessionsMetadata"); // NOI18N
        menuSessionsMetadata.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSessionsMetadataActionPerformed(evt);
            }
        });
        menuSessions.add(menuSessionsMetadata);

        jSeparator23.setName("jSeparator23"); // NOI18N
        menuSessions.add(jSeparator23);

        menuSessionHistory.setText(bundle.getString("View.menuSessionHistory.text")); // NOI18N
        menuSessionHistory.setName("menuSessionHistory"); // NOI18N
        menuSessionHistory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSessionHistorybuttonAbortActionPerformed(evt);
            }
        });
        menuSessions.add(menuSessionHistory);

        menuBar.add(menuSessions);

        menuVersioning.setText(bundle.getString("View.menuVersioning.text")); // NOI18N
        menuVersioning.setName("menuVersioning"); // NOI18N

        menuSetCurrentBranch.setText(bundle.getString("View.menuSetCurrentBranch.text")); // NOI18N
        menuSetCurrentBranch.setName("menuSetCurrentBranch"); // NOI18N
        menuSetCurrentBranch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSetCurrentBranchActionPerformed(evt);
            }
        });
        menuVersioning.add(menuSetCurrentBranch);

        menuCreateBranch.setText(bundle.getString("View.menuCreateBranch.text")); // NOI18N
        menuCreateBranch.setName("menuCreateBranch"); // NOI18N
        menuCreateBranch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCreateBranchActionPerformed(evt);
            }
        });
        menuVersioning.add(menuCreateBranch);

        menuDeleteBranch.setText(bundle.getString("View.menuDeleteBranch.text")); // NOI18N
        menuDeleteBranch.setName("menuDeleteBranch"); // NOI18N
        menuDeleteBranch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDeleteBranchActionPerformed(evt);
            }
        });
        menuVersioning.add(menuDeleteBranch);

        jSeparator14.setName("jSeparator14"); // NOI18N
        menuVersioning.add(jSeparator14);

        menuSetCurrentBranch1.setText(bundle.getString("View.menuSetCurrentBranch1.text")); // NOI18N
        menuSetCurrentBranch1.setName("menuSetCurrentBranch1"); // NOI18N
        menuSetCurrentBranch1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSetCurrentBranch1ActionPerformed(evt);
            }
        });
        menuVersioning.add(menuSetCurrentBranch1);

        menuCreateTag.setText(bundle.getString("View.menuCreateTag.text")); // NOI18N
        menuCreateTag.setName("menuCreateTag"); // NOI18N
        menuCreateTag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCreateTagActionPerformed(evt);
            }
        });
        menuVersioning.add(menuCreateTag);

        menuDeleteTag.setText(bundle.getString("View.menuDeleteTag.text")); // NOI18N
        menuDeleteTag.setName("menuDeleteTag"); // NOI18N
        menuDeleteTag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDeleteTagActionPerformed(evt);
            }
        });
        menuVersioning.add(menuDeleteTag);

        jSeparator12.setName("jSeparator12"); // NOI18N
        menuVersioning.add(jSeparator12);

        menuChanges.setText(bundle.getString("View.menuChanges.text")); // NOI18N
        menuChanges.setName("menuChanges"); // NOI18N
        menuChanges.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuChangesActionPerformed(evt);
            }
        });
        menuVersioning.add(menuChanges);

        menuCommit.setText(bundle.getString("View.menuCommit.text")); // NOI18N
        menuCommit.setName("menuCommit"); // NOI18N
        menuCommit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCommitActionPerformed(evt);
            }
        });
        menuVersioning.add(menuCommit);

        menuPull.setText(bundle.getString("View.menuPull.text")); // NOI18N
        menuPull.setName("menuPull"); // NOI18N
        menuPull.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPullActionPerformed(evt);
            }
        });
        menuVersioning.add(menuPull);

        menuPush.setText(bundle.getString("View.menuPush.text")); // NOI18N
        menuPush.setName("menuPush"); // NOI18N
        menuPush.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPushActionPerformed(evt);
            }
        });
        menuVersioning.add(menuPush);

        menuBar.add(menuVersioning);

        menuView.setText(bundle.getString("View.menuView.text")); // NOI18N
        menuView.setName("menuView"); // NOI18N
        menuView.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuViewStateChanged(evt);
            }
        });

        menuFullScreen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFullScreen.setSelected(true);
        menuFullScreen.setText(bundle.getString("View.menuFullScreen.text")); // NOI18N
        menuFullScreen.setName("menuFullScreen"); // NOI18N
        menuFullScreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFullScreenActionPerformed(evt);
            }
        });
        menuView.add(menuFullScreen);

        menuLaf.setText(bundle.getString("View.menuLaf.text_1")); // NOI18N
        menuLaf.setName("menuLaf"); // NOI18N
        menuView.add(menuLaf);

        jSeparator13.setName("jSeparator13"); // NOI18N
        menuView.add(jSeparator13);

        menuViewPanels.setText(bundle.getString("View.menuViewPanels.text_1")); // NOI18N
        menuViewPanels.setName("menuViewPanels"); // NOI18N
        menuViewPanels.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuViewPanelsStateChanged(evt);
            }
        });
        menuView.add(menuViewPanels);

        menuConsoleLocation.setText(bundle.getString("View.menuConsoleLocation.text_1")); // NOI18N
        menuConsoleLocation.setName("menuConsoleLocation"); // NOI18N
        menuView.add(menuConsoleLocation);

        menuDataPanelLocation.setText(bundle.getString("View.menuDataPanelLocation.text")); // NOI18N
        menuDataPanelLocation.setName("menuDataPanelLocation"); // NOI18N
        menuView.add(menuDataPanelLocation);

        menuPlotWindow.setText(bundle.getString("View.menuPlotWindow.text")); // NOI18N
        menuPlotWindow.setName("menuPlotWindow"); // NOI18N

        buttonGroupPlotsVisibility.add(radioPlotsVisible);
        radioPlotsVisible.setSelected(true);
        radioPlotsVisible.setText(bundle.getString("View.radioPlotsVisible.text")); // NOI18N
        radioPlotsVisible.setName("radioPlotsVisible"); // NOI18N
        radioPlotsVisible.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioPlotsVisibleActionPerformed(evt);
            }
        });
        menuPlotWindow.add(radioPlotsVisible);

        buttonGroupPlotsVisibility.add(radioPlotsDetached);
        radioPlotsDetached.setText(bundle.getString("View.radioPlotsDetached.text")); // NOI18N
        radioPlotsDetached.setName("radioPlotsDetached"); // NOI18N
        radioPlotsDetached.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioPlotsVisibleActionPerformed(evt);
            }
        });
        menuPlotWindow.add(radioPlotsDetached);

        buttonGroupPlotsVisibility.add(radioPlotsHidden);
        radioPlotsHidden.setText(bundle.getString("View.radioPlotsHidden.text")); // NOI18N
        radioPlotsHidden.setName("radioPlotsHidden"); // NOI18N
        radioPlotsHidden.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioPlotsVisibleActionPerformed(evt);
            }
        });
        menuPlotWindow.add(radioPlotsHidden);

        menuView.add(menuPlotWindow);

        menuViewPanels1.setText(bundle.getString("View.menuViewPanels1.text")); // NOI18N
        menuViewPanels1.setName("menuViewPanels1"); // NOI18N

        menuOutput.setText(bundle.getString("View.menuOutput.text")); // NOI18N
        menuOutput.setName("menuOutput"); // NOI18N
        menuOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOutputActionPerformed(evt);
            }
        });
        menuViewPanels1.add(menuOutput);

        menuScanPanel.setText(bundle.getString("View.menuScanPanel.text")); // NOI18N
        menuScanPanel.setName("menuScanPanel"); // NOI18N
        menuScanPanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuScanPanelActionPerformed(evt);
            }
        });
        menuViewPanels1.add(menuScanPanel);

        menuView.add(menuViewPanels1);

        menuTerminal.setText(bundle.getString("View.menuTerminal.text")); // NOI18N
        menuTerminal.setName("menuTerminal"); // NOI18N
        menuTerminal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuTerminalActionPerformed(evt);
            }
        });
        menuView.add(menuTerminal);

        jSeparator8.setName("jSeparator8"); // NOI18N
        menuView.add(jSeparator8);

        menuCloseAllPlots.setText(bundle.getString("View.menuCloseAllPlots.text")); // NOI18N
        menuCloseAllPlots.setName("menuCloseAllPlots"); // NOI18N
        menuCloseAllPlots.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCloseAllPlotsActionPerformed(evt);
            }
        });
        menuView.add(menuCloseAllPlots);

        menuCloseAll.setText(bundle.getString("View.menuCloseAll.text")); // NOI18N
        menuCloseAll.setName("menuCloseAll"); // NOI18N
        menuCloseAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCloseAllActionPerformed(evt);
            }
        });
        menuView.add(menuCloseAll);

        jSeparator20.setName("jSeparator20"); // NOI18N
        menuView.add(jSeparator20);

        menuPreferences.setText(bundle.getString("View.menuPreferences.text")); // NOI18N
        menuPreferences.setName("menuPreferences"); // NOI18N
        menuPreferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPreferencesActionPerformed(evt);
            }
        });
        menuView.add(menuPreferences);

        menuBar.add(menuView);

        menuHelp.setText(bundle.getString("View.menuHelp.text")); // NOI18N

        menuHelpContents.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        menuHelpContents.setText(bundle.getString("View.menuHelpContents.text")); // NOI18N
        menuHelpContents.setName("menuHelpContents"); // NOI18N
        menuHelpContents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuHelpContentsActionPerformed(evt);
            }
        });
        menuHelp.add(menuHelpContents);

        jSeparator15.setName("jSeparator15"); // NOI18N
        menuHelp.add(jSeparator15);

        menuSetup.setText(bundle.getString("View.menuSetup.text")); // NOI18N
        menuSetup.setName("menuSetup"); // NOI18N
        menuSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSetupActionPerformed(evt);
            }
        });
        menuHelp.add(menuSetup);

        jSeparator16.setName("jSeparator16"); // NOI18N
        menuHelp.add(jSeparator16);

        menuAbout.setText(bundle.getString("View.menuAbout.text")); // NOI18N
        menuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAboutActionPerformed(evt);
            }
        });
        menuHelp.add(menuAbout);

        menuBar.add(menuHelp);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusBar, javax.swing.GroupLayout.DEFAULT_SIZE, 1069, Short.MAX_VALUE)
            .addComponent(toolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(statusBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menuOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(Setup.getScriptsPath());
            //FileNameExtensionFilter filter = new FileNameExtensionFilter("Script files", "py", "groovy", "js");
            //chooser.setFileFilter(filter);
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Script files (*." + Context.getScriptType().getExtension() + ")", 
                    new String[]{String.valueOf(Context.getScriptType().getExtension())}));
            //chooser.addChoosableFileFilter(new ExtensionFileFilter("Script files (*.py, *.groovy, *.js)", new String[]{"py", "groovy", "js"}));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Java files (*.java)", new String[]{"java"}));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Text files (*.txt, *.csv, *.log)", textFileExtensions));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Data files (*.h5)", dataFileExtensions));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Strip Chart definition file (*." + StripChart.FILE_EXTENSION + ")", StripChart.FILE_EXTENSION));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Image files (*.png, *.bmp, *.gif, *.jpg)", imageFileExtensions));

            HashMap<FileFilter, Processor> processors = new HashMap<>();
            for (Processor processor : Processor.getServiceProviders()) {
                if ((processor.getExtensions().length > 0) & processor.canSave()) {
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(processor.getDescription(), processor.getExtensions());
                    chooser.addChoosableFileFilter(filter);
                    processors.put(filter, processor);
                }
            }
            chooser.setAcceptAllFileFilterUsed(true);
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                openFile(chooser.getSelectedFile(), processors.get(chooser.getFileFilter()));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuOpenActionPerformed

    private void menuNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuNewActionPerformed
        try {
            ScriptEditor editor = newScriptEditor(null);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuNewActionPerformed

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
        Context.getApp().exit(this);
    }//GEN-LAST:event_menuExitActionPerformed

    private void menuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAboutActionPerformed
        AboutDialog aboutDialog = new AboutDialog(this, true);
        aboutDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        showChildWindow(aboutDialog);
    }//GEN-LAST:event_menuAboutActionPerformed

    private void menuRestartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRestartActionPerformed
        try {
            if (Context.getState() != State.Initializing) {
                //Context.getApp().startTask(new Restart());
                Context.getApp().startTask(new App.Restart()); //!!! Canot have if factorized in ch.psi.pshell.ui.Restart?
                shell.clear();
                outputPanel.clear();
                saveContext();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuRestartActionPerformed

    private void buttonDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDebugActionPerformed
        try {
            debugScript(false);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDebugActionPerformed

    private void buttonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPauseActionPerformed
        try {
            pause();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonPauseActionPerformed

    private void buttonStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStepActionPerformed
        try {
            step();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStepActionPerformed

    private void buttonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAbortActionPerformed

        try {
           abort();
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_buttonAbortActionPerformed

    private void tabDocStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabDocStateChanged
        updateViewState();
        Component c = tabDoc.getSelectedComponent();
        for (ch.psi.pshell.plugin.Plugin plugin : Context.getPlugins()) {
            try{
                if (plugin instanceof Panel panel) {
                    if (panel == c) {
                        panel.restore();
                    }
                }
            } catch (Exception ex){
                logger.log(Level.SEVERE, null, ex);
            }
        }
        updateSelectedPanel(tabDoc);
    }//GEN-LAST:event_tabDocStateChanged

    private void menuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveActionPerformed
        try {
            save();
        } catch (Exception ex) {
            showException(ex);
        }        
    }//GEN-LAST:event_menuSaveActionPerformed

    private void menuSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveAsActionPerformed
        try {
            saveAs();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSaveAsActionPerformed

    private void menuFileStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuFileStateChanged
        if (menuFile.isSelected()) {
            try {
                menuOpenRecent.removeAll();
                ActionListener listener = (ActionEvent e) -> {
                    String file = e.getActionCommand();
                    try {
                        openScriptOrProcessor(file);
                    } catch (Exception ex) {
                        showException(ex);                        
                    }
                };
                for (String file : getFileHistory().get()) {
                    JMenuItem item = new JMenuItem(file);
                    item.addActionListener(listener);
                    menuOpenRecent.add(item, 0);
                }

                Executor executor = getSelectedExecutor();
                Processor runningProcessor = getRunningProcessor();
                QueueProcessor queueProcessor = getSelectedQueueProcessor();

                List<QueueProcessor> queues = getQueues();
                if (queueProcessor != null) {
                    queues.remove(queueProcessor);
                }
                menuAddToQueue.setVisible(isShowingExecutor());
                menuAddToQueue.removeAll();
                if (executor != null) {
                    String _filename = null;
                    try{
                        _filename = executor.getFileName();
                    } catch (Exception ex){                        
                    }
                    Map<String, Object> _args=null;
                    if ((_filename==null) && (executor instanceof ScriptProcessor scriptProcessor)){
                        try{
                            _filename = scriptProcessor.getScript();
                            _args = scriptProcessor.getArgs();
                         } catch (Exception ex){  
                            _filename = null;
                        }
                    }
                    
                    if (executor instanceof Processor processor){
                        String home = processor.getHomePath();
                        if ((home!=null) && (!home.isBlank()) && (_filename!=null)){
                            if (IO.isSubPath(_filename, home)) {
                                //If in script folder then use only relative
                                _filename =  IO.getRelativePath(_filename, home);
                            }
                        }
                    }

                    String filename = _filename;
                    Map<String, Object> args = _args;
                    menuAddToQueue.setEnabled(filename != null);
                    if (filename != null) {
                        if (queues.size() == 0) {
                            JMenuItem item = new JMenuItem("New");
                            item.addActionListener((e) -> {
                                try {
                                    QueueProcessor tq = openProcessor(QueueProcessor.class, null);
                                    tq.addNewFile(filename, args);
                                } catch (Exception ex) {
                                    showException(ex);
                                }
                            });
                            menuAddToQueue.add(item);

                        } else {
                            for (int i = 0; i < queues.size(); i++) {
                                if (queues.get(i) != executor) {
                                    String queueFilename = queues.get(i).getFileName();
                                    if (queueFilename == null) {
                                        queueFilename = "Unknown";
                                    }
                                    JMenuItem item = new JMenuItem(IO.getPrefix(queueFilename));
                                    item.setEnabled(queues.get(i) != runningProcessor);
                                    int index = i;
                                    item.addActionListener((e) -> {
                                        try {
                                            queues.get(index).addNewFile(filename, args);
                                            tabDoc.setSelectedComponent(queues.get(index));
                                        } catch (Exception ex) {
                                            showException(ex);
                                        }
                                    });
                                    menuAddToQueue.add(item);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                showException(ex);
            }

            menuUsers.setVisible(Context.isUsersEnabled() &&  Context.getUsersManager().getCurrentUser().accessLevel == AccessLevel.administrator);
        }
    }//GEN-LAST:event_menuFileStateChanged

    private void menuConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuConfigurationActionPerformed
        try {
            ConfigDialog dlg = new ConfigDialog(this, true);
            dlg.setTitle("Configuration");
            dlg.setConfig(App.getInstance().getConfig());
            dlg.setReadOnly(Context.getRights().denyConfig);
            dlg.setSize(600, 480);
            showChildWindow(dlg);
            if (dlg.getResult()) {
                Context.saveConfiguration(App.getInstance());
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuConfigurationActionPerformed

    private void menuViewStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuViewStateChanged
        try {
            if (menuView.isSelected()) {
                boolean plotsVisible = isPlotsVisible();                
                radioPlotsDetached.setSelected(plotsVisible && plotsDetached);
                radioPlotsVisible.setSelected(plotsVisible && !plotsDetached);
                radioPlotsHidden.setSelected(!plotsVisible);
                menuFullScreen.setSelected(isFullScreen());
                menuTerminal.setSelected(isTerminalVisible());
                menuScanPanel.setSelected(isScanPanelVisible());
                menuOutput.setSelected(isOutputPanelVisible());
                updateLafMenu();
                for (Component item : menuConsoleLocation.getMenuComponents()) {
                    ((JRadioButtonMenuItem) item).setSelected(((JRadioButtonMenuItem) item).getText().equals(consoleLocation.toString()));
                }
                for (Component item : menuDataPanelLocation.getMenuComponents()) {
                    if (item instanceof JRadioButtonMenuItem radioButtonMenuItem){
                        radioButtonMenuItem.setSelected(radioButtonMenuItem.getText().equals(dataPanelLocation.toString()));
                    } else if (item instanceof JCheckBoxMenuItem checkBoxMenuItem){
                        checkBoxMenuItem.setSelected(preferences.openDataFilesInDocTab);
                    }
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_menuViewStateChanged

    EditorDialog devicePoolEditorDlg;

    private void menuDevicesDefinitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDevicesDefinitionActionPerformed
        try {
            if ((devicePoolEditorDlg == null) || !devicePoolEditorDlg.isDisplayable()) {
                DevicePoolEditor devicePoolEditor;
                devicePoolEditor = new DevicePoolEditor();
                devicePoolEditorDlg = devicePoolEditor.getDialog(this, false);
                devicePoolEditorDlg.setSize(900, 400);
                devicePoolEditor.setFilePermissions(Context.getConfigFilePermissions());
                devicePoolEditor.load(Setup.getDevicePoolFile());
                devicePoolEditor.setReadOnly(Context.getRights().denyConfig);
                devicePoolEditor.setTitle("Device Pool Definition");
            }
            showChildWindow(devicePoolEditorDlg);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDevicesDefinitionActionPerformed

    private void menuDevicesEpicsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDevicesEpicsActionPerformed
        try {
            EpicsConfigDialog dlg = new EpicsConfigDialog(this, true, Context.getConfigFilePermissions());
            dlg.setReadOnly(Context.getRights().denyConfig);
            dlg.setTitle("EPICS Properties");
            showChildWindow(dlg);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDevicesEpicsActionPerformed

    private void menuOpenLogFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenLogFileActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(Setup.getLogPath());
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Log files", "log");
            chooser.setFileFilter(filter);
            chooser.setSelectedFile(new File(App.getInstance().getLogFileName()));
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                Editor editor = openTextFile(chooser.getSelectedFile().getPath());
                editor.setReadOnly(true);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuOpenLogFileActionPerformed

    private void buttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunActionPerformed
        try {
            run();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonRunActionPerformed

    private void menuPreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPreferencesActionPerformed
        try {
            PreferencesDialog dlg = new PreferencesDialog(this, true);
            dlg.set(preferences);
            dlg.setReadOnly(Context.getRights().denyPrefs);
            showChildWindow(dlg);
            if (dlg.getResult()) {
                preferences.save();
                applyPreferences();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuPreferencesActionPerformed

    private void menuStopAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuStopAllActionPerformed
        try {
            Context.getInterpreter().stopAll();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuStopAllActionPerformed

    private void menuPluginsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPluginsActionPerformed
        try {
            PluginsEditor editor = new PluginsEditor();  //new TextEditor();
            EditorDialog dlg = editor.getDialog(this, false);
            editor.setFilePermissions(Context.getConfigFilePermissions());
            editor.load(Setup.getPluginsConfigurationFile());
            editor.setReadOnly(Context.getRights().denyConfig);
            dlg.setContentPane(editor);
            dlg.setSize(480, 320);
            showChildWindow(dlg);
            editor.setTitle("Plugins");
            editor.setListener((File file) -> {
                try {
                    openScript(file.getCanonicalPath());
                } catch (Exception ex) {
                    showException(ex);
                }
            });

        } catch (FileNotFoundException | NoSuchFileException ex) {
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_menuPluginsActionPerformed

    private void tabStatusStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabStatusStateChanged
        updateSelectedPanel(tabStatus);
    }//GEN-LAST:event_tabStatusStateChanged

    private void menuUpdateAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuUpdateAllActionPerformed
        try {
            Context.getInterpreter().updateAll();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuUpdateAllActionPerformed

    private void menuPushActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPushActionPerformed
        try {            
            JCheckBox checkAll = new JCheckBox("All Branches");            
            JCheckBox checkForce = new JCheckBox("Force");            
            JCheckBox checkTags = new JCheckBox("Push Tags");
            
            checkAll.setSelected(true);
                        
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(checkAll);
            panel.add(checkForce);
            panel.add(checkTags);                                    

            if (showOption("Push to remote repository", panel, OptionType.OkCancel) == OptionResult.Yes) {            
                Context.getApp().startTask(new VersioningTasks.PushUpstream(checkAll.isSelected(), checkForce.isSelected(), checkTags.isSelected()));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuPushActionPerformed

    private void menuSetCurrentBranchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSetCurrentBranchActionPerformed
        try {
            List<String> branches = Context.getVersioningManager().getLocalBranches();
            String current = Context.getVersioningManager().getCurrentBranch();
            String branch = getString("Select working branch:", branches.toArray(new String[0]), current);
            if ((branch != null) && (!branch.equals(current))) {
                Context.getApp().startTask(new VersioningTasks.Checkout(true, branch));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSetCurrentBranchActionPerformed

    private void menuCreateBranchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCreateBranchActionPerformed
        try {
            String branch = getString("Enter branch name:", null);
            if (branch != null) {
                Context.getVersioningManager().createBranch(branch);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCreateBranchActionPerformed

    private void menuDeleteBranchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDeleteBranchActionPerformed
        try {
            List<String> branches = Context.getVersioningManager().getLocalBranches();
            String current = Context.getVersioningManager().getCurrentBranch();
            branches.remove(VersioningManager.MASTER_BRANCH);
            //branches.remove(current);
            String ret = getString("Select branch to be deleted:", branches.toArray(new String[0]), null);
            if (ret != null) {
                Context.getVersioningManager().deleteBranch(ret);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDeleteBranchActionPerformed

    private void menuPullActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPullActionPerformed
        try {
            Context.getVersioningManager(); //assert
            Context.getApp().startTask(new VersioningTasks.PullUpstream());
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuPullActionPerformed

    private void menuTasksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTasksActionPerformed
        try {
            Editor editor = new TasksEditor();
            EditorDialog dlg = editor.getDialog(this, false);
            dlg.setSize(480, 320);
            showChildWindow(dlg);
            editor.setFilePermissions(Context.getConfigFilePermissions());
            editor.load(Setup.getTasksFile());            
            editor.setReadOnly(Context.getRights().denyConfig);
            editor.setTitle("Tasks");
        } catch (FileNotFoundException | NoSuchFileException ex) {
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuTasksActionPerformed

    private void menuImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuImportActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(Setup.getScriptsPath());
            chooser.setAcceptAllFileFilterUsed(false);
            HashMap<FileFilter, Importer> importers = new HashMap<>();
            for (Importer importer : Importer.getServiceProviders()) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(importer.getDescription(), importer.getExtensions());
                chooser.addChoosableFileFilter(filter);
                importers.put(filter, importer);
            }
            if (importers.isEmpty()) {
                throw new Exception("No import service available");
            }

            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                File file = new File(chooser.getSelectedFile().getCanonicalPath());
                if (!file.exists()) {
                    throw new java.io.FileNotFoundException();
                }
                Importer importer = importers.get(chooser.getFileFilter());
                String script = importer.importFile(file);
                ScriptEditor editor = newScriptEditor(null);
                editor.setText(script);
                editor.setFileName(Paths.get(Setup.getScriptsPath(), IO.getPrefix(file) + "." + Context.getScriptType().getExtension()).toString());
                tabDoc.setTitleAt(tabDoc.indexOfComponent(editor), editor.getScriptName() + "*");
            }
        } catch (Exception ex) {
            showException(ex);
        } catch (Error e) {
            logger.severe(e.toString());
        }
    }//GEN-LAST:event_menuImportActionPerformed

    private void menuFullScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFullScreenActionPerformed
        try {
            setFullScreen(menuFullScreen.isSelected());
        } catch (Exception ex) {
            showException(ex);
        } catch (Error e) {
            logger.severe(e.toString());
        }
    }//GEN-LAST:event_menuFullScreenActionPerformed

    private void menuCreateTagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCreateTagActionPerformed
        try {
            JPanel panel = new JPanel();
            //panel.setLayout(new GridLayout(2,2));
            GridBagLayout layout = new GridBagLayout();
            layout.columnWidths = new int[]{0, 180};   //Minimum width
            panel.setLayout(layout);
            JTextField name = new JTextField();
            JTextField message = new JTextField();
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            panel.add(new JLabel("Enter tag name:"), c);
            c.gridy = 1;
            panel.add(new JLabel("Commit message:"), c);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            panel.add(message, c);
            c.gridy = 0;
            panel.add(name, c);

            if (showOption("Create Tag", panel, OptionType.OkCancel) == OptionResult.Yes) {
                if (name.getText().length() > 0) {
                    Context.getVersioningManager().createTag(name.getText(), message.getText().isEmpty() ? null : message.getText());
                } else {
                    throw new Exception("Invalid tag name");
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCreateTagActionPerformed

    private void menuDeleteTagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDeleteTagActionPerformed
        try {
            List<String> tags = Context.getVersioningManager().getTags();
            String ret = getString("Select tag to be deleted:", tags.toArray(new String[0]), null);
            if (ret != null) {
                Context.getVersioningManager().deleteTag(ret);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDeleteTagActionPerformed

    private void menuSetCurrentBranch1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSetCurrentBranch1ActionPerformed
        try {
            List<String> tags = Context.getVersioningManager().getTags();
            String tag = getString("Select tag to checkout:", tags.toArray(new String[0]), null);
            if (tag != null) {
                Context.getApp().startTask(new VersioningTasks.Checkout(false, tag));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSetCurrentBranch1ActionPerformed

    private void menuUsersbuttonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuUsersbuttonAbortActionPerformed
        try {
            Editor editor = new UsersEditor(Context.getUsersManager());
            editor.setFilePermissions(Context.getConfigFilePermissions());
            EditorDialog dlg = editor.getDialog(this, false);
            dlg.setSize(640, 360);
            showChildWindow(dlg);
            editor.load(null);            
            editor.setReadOnly(Context.getRights().denyConfig);
            editor.setTitle("Users");
        } catch (FileNotFoundException | NoSuchFileException ex) {
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuUsersbuttonAbortActionPerformed

    private void menuViewPanelsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuViewPanelsStateChanged
        menuViewPanels.removeAll();
        if (menuViewPanels.isSelected()) {
            for (ch.psi.pshell.plugin.Plugin plugin : Context.getPlugins()) {
                try{
                    if (plugin instanceof Panel panel) {
                        JCheckBoxMenuItem item = new JCheckBoxMenuItem(panel.getPluginName(), panel.isLoaded());
                        menuViewPanels.add(item);
                        item.addActionListener((ActionEvent e) -> {
                            panel.setLoaded(item.isSelected());
                        });
                    }
                } catch (Exception ex){
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_menuViewPanelsStateChanged

    private void menuChangeUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuChangeUserActionPerformed
        try {
            String current = Context.getUsersManager().getCurrentUser().name;
            String name = getString("Select user:", Context.getUsersManager().getUserNames(), current);
            if ((name != null) && (!name.equals(current))) {
                Context.getUsersManager().selectUser(name);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuChangeUserActionPerformed

    HelpContentsDialog dialogHelp;    

    private void menuHelpContentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHelpContentsActionPerformed
        try {
            if ((dialogHelp == null) || (!dialogHelp.isDisplayable())) {
                dialogHelp = new HelpContentsDialog(this, false);
                dialogHelp.setDefaultCloseOperation(HIDE_ON_CLOSE);
                dialogHelp.setSize(800, 600);
            } else if (!dialogHelp.isShowing()) {
                dialogHelp.initialize();
            }
            showChildWindow(dialogHelp);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuHelpContentsActionPerformed

    private void menuCommitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCommitActionPerformed
        try {
            String commitMessage = getString("Enter commit message:", "");
            if (commitMessage != null) {
                Context.getApp().startTask(new VersioningTasks.Commit(commitMessage));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCommitActionPerformed


    private void menuCloseAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCloseAllActionPerformed
        try {
            closeSearchPanels();
            closeDocuments();   
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCloseAllActionPerformed

    private void menuSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSetupActionPerformed
        try {
            ArrayList<String> plugins = new ArrayList<>();
            for (ch.psi.pshell.plugin.Plugin p : Context.getPlugins()) {
                try{
                    plugins.add(p.getPluginName());
                } catch (Exception ex){
                    logger.log(Level.SEVERE, null, ex);
                }                    
            }

            ArrayList<String> extensions = new ArrayList<>();
            for (File f : Context.getExtensions()) {
                extensions.add(f.getName());
            }
                        

            Server server = Context.getInterpreter().getServer();
            String[][] entries = new String[][]{
                {"Process", Sys.getProcessName()},
                {"User", Sys.getUserName()},
                {"Version", App.getApplicationBuildInfo()},
                {"Java", /*System.getProperty("java.vendor") + " " + */
                    System.getProperty("java.vm.name") + " (" + System.getProperty("java.version") + ")"},
                {"Jar file", String.valueOf(IO.getExecutingJar(App.getInstance().getClass()))},
                {"HDF5", Convert.arrayToString(FormatHDF5.getVersion(), ".")},
                {"Arguments", String.join(" ", App.getCommandLineArguments())},
                {"Plugins", String.join("; ", plugins)},                
                {"Extensions", String.join("; ", extensions)},
                {"Packages", Context.hasPackageManager() ? String.join("; ", Context.getPackageManager().getPackagePaths()) : ""},                
                {"Current folder", new File(".").getCanonicalPath()},
                {"Home path", Setup.getHomePath()},
                {"Output path", Setup.getOutputPath()},
                {"Config path", Setup.getConfigPath()},
                {"Devices path", Setup.getDevicesPath()},
                {"Script path", Setup.getScriptsPath()},
                {"Library path", String.join("; ", Setup.getLibraryPath())},
                {"Plugins path", Setup.getPluginsPath()},
                {"Extensions path", Setup.getExtensionsPath()},
                {"WWW path", Setup.getWwwPath()},
                {"Data path", Setup.getDataPath()},
                {"Log path", Setup.getLogPath()},
                {"Context path", Setup.getContextPath()},
                {"Sessions path", Setup.getSessionsPath()},
                {"Help path", Setup.getHelpPath()},
                {"Startup script", Context.getInterpreter().getStartupScript()},
                {"Config file", Context.getConfig().getFileName()},
                {"Settings file", Setup.getSettingsFile()},
                {"Device pool", Setup.getDevicePoolFile()},
                {"Preferences", Preferences.getFile().toString()},
                {"Server URL", server == null ? "" : server.getStaticURL()},};

            JTable table = new JTable();
            table.setModel(new DefaultTableModel(
                    entries,
                    new String[]{
                        "Parameter", "Value"
                    }
            ) {
                public Class
                        getColumnClass(int columnIndex) {
                    return String.class;
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return (columnIndex == 1);
                }
            });
            //table.setPreferredSize(new Dimension(400,200));
            table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            table.getColumnModel().getColumn(0).setPreferredWidth(150);
            table.getColumnModel().getColumn(1).setPreferredWidth(450);
            JTextField textField = new JTextField();
            textField.setEditable(false);
            DefaultCellEditor editor = new DefaultCellEditor(textField);
            table.getColumnModel().getColumn(1).setCellEditor(editor);
            StandardDialog dlg = new StandardDialog(this, "Setup", true);
            dlg.setContentPane(table);
            dlg.pack();
            dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            //dlg.setResizable(false);
            showChildWindow(dlg);

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSetupActionPerformed

    private void menuReinitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuReinitActionPerformed
        try {
            new Thread(() -> {
                Context.getInterpreter().reinit();
            }).start();

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuReinitActionPerformed

    private void menuCloseAllPlotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCloseAllPlotsActionPerformed
        try {
            closePlots();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCloseAllPlotsActionPerformed

    JTextField findInFilesText;
    JCheckBox findInFilesCaseInsensitive;
    JCheckBox findInFilesWholeWords;

    private void menuFindInFilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFindInFilesActionPerformed
        try {
            if (findInFilesText == null) {
                findInFilesText = new JTextField();
                findInFilesCaseInsensitive = new JCheckBox("Case insensitive");
                findInFilesWholeWords = new JCheckBox("Whole Words");
            }
            JPanel panel = new JPanel(new BorderLayout());
            ((BorderLayout) panel.getLayout()).setVgap(10);
            JPanel p1 = new JPanel(new BorderLayout());
            ((BorderLayout) p1.getLayout()).setHgap(5);
            p1.add(new JLabel("Search text: "), BorderLayout.WEST);
            p1.add(findInFilesText, BorderLayout.EAST);
            panel.add(p1, BorderLayout.NORTH);
            JPanel p2 = new JPanel(new BorderLayout());
            ((BorderLayout) p2.getLayout()).setVgap(6);
            p2.add(findInFilesCaseInsensitive, BorderLayout.NORTH);
            p2.add(findInFilesWholeWords, BorderLayout.SOUTH);
            panel.add(p2, BorderLayout.SOUTH);
            findInFilesText.setPreferredSize(new Dimension(200, findInFilesText.getPreferredSize().height));
            SwingUtils.requestFocusDeferred(findInFilesText);

            if ((showOption("Find in Files", panel, OptionType.OkCancel) == OptionResult.Yes) && (findInFilesText.getText().length() > 0)) {
                String[] ignore = new String[]{Setup.getDefaultScriptLibPath()};
                SearchPanel pn = new SearchPanel(Setup.getScriptsPath(), "*." + Context.getScriptType().getExtension(),
                        findInFilesText.getText(), findInFilesCaseInsensitive.isSelected(), findInFilesWholeWords.isSelected(), ignore, true) {
                    @Override
                    protected void onDoubleClick(File file, int row, String text) throws IOException {
                        ScriptEditor editor = View.this.openScript(file.toString());
                        editor.getTextEditor().scrollToVisible(row);
                    }
                };
                tabStatus.addTab("Search", pn);
                int index = tabStatus.getTabCount() - 1;
                SwingUtils.setTabClosable(tabStatus, index);
                setTabDetachable(tabStatus, index);
                tabStatus.setSelectedComponent(pn);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuFindInFilesActionPerformed

    private void menuUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuUndoActionPerformed
        try {
            ScriptEditor editor;
            if ((editor = getSelectedEditor()) != null) {
                editor.getTextEditor().undo();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuUndoActionPerformed

    private void menuRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRedoActionPerformed
        try {
            ScriptEditor editor;
            if ((editor = getSelectedEditor()) != null) {
                editor.getTextEditor().redo();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuRedoActionPerformed

    private void menuCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCutActionPerformed
        try {
            ScriptEditor editor;
            if ((editor = getSelectedEditor()) != null) {
                editor.getTextEditor().cut();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCutActionPerformed

    private void menuCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCopyActionPerformed
        try {
            ScriptEditor editor;
            if ((editor = getSelectedEditor()) != null) {
                editor.getTextEditor().copySelection();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCopyActionPerformed

    private void menuPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPasteActionPerformed
        try {
            ScriptEditor editor;
            if ((editor = getSelectedEditor()) != null) {
                editor.getTextEditor().paste();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuPasteActionPerformed

    private void menuFindActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFindActionPerformed
        try {
            ScriptEditor editor;
            if ((editor = getSelectedEditor()) != null) {
                editor.getTextEditor().search();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuFindActionPerformed

    private void menuFindNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFindNextActionPerformed
        try {
            ScriptEditor editor;
            if ((editor = getSelectedEditor()) != null) {
                editor.getTextEditor().searchAgain();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuFindNextActionPerformed

    private void menuEditStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuEditStateChanged
        ScriptEditor editor = getSelectedEditor();
        boolean editing = (editor != null);
        menuUndo.setEnabled(editing && editor.getTextEditor().canUndo());
        menuRedo.setEnabled(editing && editor.getTextEditor().canRedo());
        menuPaste.setEnabled(editing);
        menuCopy.setEnabled(editing && editor.getTextEditor().canCopySelection());
        menuCut.setEnabled(menuCopy.isEnabled());
        menuBlock.setEnabled(menuCopy.isEnabled());
        menuFind.setEnabled(editing);
        menuFindNext.setEnabled(editing && editor.getTextEditor().canFindNext());
        menuToggleComment.setVisible(editing && editor.hasSyntaxHighlight());
    }//GEN-LAST:event_menuEditStateChanged

    private void menuIndentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuIndentActionPerformed
        try {
            ScriptEditor editor = getSelectedEditor();
            if (editor != null) {
                editor.indent();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuIndentActionPerformed

    private void menuUnindentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuUnindentActionPerformed
        try {
            ScriptEditor editor = getSelectedEditor();
            if (editor != null) {
                editor.unindent();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuUnindentActionPerformed

    private void menuCommentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCommentActionPerformed
        try {
            ScriptEditor editor = getSelectedEditor();
            if (editor != null) {
                editor.comment();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCommentActionPerformed

    private void menuUncommentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuUncommentActionPerformed
        try {
            ScriptEditor editor = getSelectedEditor();
            if (editor != null) {
                editor.uncomment();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuUncommentActionPerformed

    private void menuStripChartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuStripChartActionPerformed
        try {
            JDialog dlg = new StripChart(View.this, false, null);
            showChildWindow(dlg);
        } catch (Exception ex) {
            showException(ex);
        }
     }//GEN-LAST:event_menuStripChartActionPerformed

    RepositoryChangesDialog repositoryChangesDialog;
    private void menuChangesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuChangesActionPerformed
        try {
            if (repositoryChangesDialog != null) {
                repositoryChangesDialog.setVisible(false);
                repositoryChangesDialog = null;
            }
            final List<org.eclipse.jgit.diff.DiffEntry> diff = Context.getVersioningManager().diff();
            if (diff.size() == 0) {
                showMessage("Repository Changes", "No changes in repository");
            } else {
                repositoryChangesDialog = new RepositoryChangesDialog(this, false);
                showChildWindow(repositoryChangesDialog);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuChangesActionPerformed

    JFileChooser logQueryChooser;
    JTextField logQueryText;
    JTextField logQueryOrigin;
    JComboBox logQueryLevel;

    private void menuLogQueryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuLogQueryActionPerformed
        try {
            if (logQueryChooser == null) {
                logQueryChooser = new JFileChooser(Setup.getLogPath());
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Log files", "log");
                logQueryChooser.setFileFilter(filter);
                logQueryChooser.setMultiSelectionEnabled(true);
                logQueryChooser.setSelectedFile(new File(App.getInstance().getLogFileName()));
            }

            int rVal = logQueryChooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                File[] files = logQueryChooser.getSelectedFiles();
                if (files.length > 0) {
                    JPanel panel = new JPanel(new BorderLayout());
                    if (logQueryText == null) {
                        logQueryText = new JTextField();
                    }
                    if (logQueryOrigin == null) {
                        logQueryOrigin = new JTextField();
                    }
                    if (logQueryLevel == null) {
                        logQueryLevel = new JComboBox();
                        SwingUtils.setEnumCombo(logQueryLevel, LogLevel.class);
                        logQueryLevel.remove(0); //Remove LogLevel.Off
                        logQueryLevel.setSelectedItem(LogLevel.Info);
                    }
                    ((BorderLayout) panel.getLayout()).setVgap(10);
                    JPanel p1 = new JPanel(new BorderLayout());
                    ((BorderLayout) p1.getLayout()).setHgap(5);
                    p1.add(new JLabel("Level: "), BorderLayout.WEST);
                    p1.add(logQueryLevel, BorderLayout.EAST);
                    panel.add(p1, BorderLayout.NORTH);
                    JPanel p2 = new JPanel(new BorderLayout());
                    ((BorderLayout) p2.getLayout()).setHgap(5);
                    p2.add(new JLabel("Origin: "), BorderLayout.WEST);
                    p2.add(logQueryOrigin, BorderLayout.EAST);
                    panel.add(p2, BorderLayout.CENTER);
                    JPanel p3 = new JPanel(new BorderLayout());
                    ((BorderLayout) p3.getLayout()).setHgap(5);
                    p3.add(new JLabel("Text: "), BorderLayout.WEST);
                    p3.add(logQueryText, BorderLayout.EAST);
                    panel.add(p3, BorderLayout.SOUTH);
                    logQueryText.setPreferredSize(new Dimension(200, logQueryText.getPreferredSize().height));
                    logQueryOrigin.setPreferredSize(new Dimension(200, logQueryOrigin.getPreferredSize().height));
                    SwingUtils.requestFocusDeferred(logQueryText);

                    if (showOption("Search logs", panel, OptionType.OkCancel) == OptionResult.Yes) {
                        List<String> fileNames = new ArrayList<>();
                        List<String> shortNames = new ArrayList<>();
                        for (File file : files) {
                            fileNames.add(file.getAbsolutePath());
                            shortNames.add(file.getName());
                        }

                        final LoggerPanel pn = new LoggerPanel();
                        Level level = Level.parse(logQueryLevel.getSelectedItem().toString().toUpperCase());
                        String origin = logQueryOrigin.getText().trim();
                        String text = logQueryText.getText().trim();

                        String title = "Log Query - Level: " + logQueryLevel.getSelectedItem().toString();
                        if (logQueryOrigin.getText().trim().length() > 0) {
                            title += "  Origin: " + logQueryOrigin.getText().trim();
                        }
                        if (logQueryText.getText().trim().length() > 0) {
                            title += "  Text: " + logQueryText.getText().trim();
                        }
                        title += ((shortNames.size() > 1) ? "  Files: " : "  File: ") + Convert.arrayToString(shortNames.toArray(new String[0]), ", ", 5);

                        JDialog dlg = new JDialog(this, title, false);
                        dlg.setSize(800, 400);
                        dlg.add(pn);
                        showChildWindow(dlg);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                List<String[]> ret;
                                try {
                                    ret = LogManager.search(fileNames, Level.parse(logQueryLevel.getSelectedItem().toString().toUpperCase()), logQueryOrigin.getText(), logQueryText.getText(), null, null);
                                    pn.load(ret);
                                } catch (IOException ex) {
                                    logger.log(Level.WARNING, null, ex);
                                }
                            }
                        }).start();
                    }
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuLogQueryActionPerformed

    private void menuShellStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuShellStateChanged
        if (menuShell.isSelected()) {
            String level = Context.getLogLevel().toString();
            for (Component c : menuSetLogLevel.getMenuComponents()) {
                if (c instanceof JMenuItem menuItem) {
                    JMenuItem item = menuItem;
                    item.setSelected(item.getText().equalsIgnoreCase(level));
                }
            }

            try {
                menuSettings.setEnabled(!Context.getSettings().isEmpty());
            } catch (IOException ex) {
                menuSettings.setEnabled(false);
            }
            updateViewState();
        }
    }//GEN-LAST:event_menuShellStateChanged

    private void menuCheckSyntaxbuttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCheckSyntaxbuttonRunActionPerformed
        try {
            getSelectedEditor().parse();
            showMessage("Syntax Check", "Script syntax ok.", JOptionPane.INFORMATION_MESSAGE);
        } catch (ScriptException ex) {
            showMessage("Syntax Check", ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCheckSyntaxbuttonRunActionPerformed

    private void menuToggleCommentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuToggleCommentActionPerformed
        try {
            ScriptEditor editor = getSelectedEditor();
            if (editor != null) {
                editor.toggleComment();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuToggleCommentActionPerformed

    private void menuFileNewMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_menuFileNewMouseClicked
        menuNewActionPerformed(null);
        MenuSelectionManager.defaultManager().clearSelectedPath();
    }//GEN-LAST:event_menuFileNewMouseClicked

    private void menuDataFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDataFileActionPerformed
        try {
            DataFileDialog dlg = new DataFileDialog(this, false);
            dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            showChildWindow(dlg);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDataFileActionPerformed

    private void menuSettingsbuttonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSettingsbuttonAbortActionPerformed
        try {
            showSettingsEditor(this, true, Context.getRights().denyConfig);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSettingsbuttonAbortActionPerformed

    private void menuRunNextbuttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRunNextbuttonRunActionPerformed
        try {
            Executor executor = getSelectedExecutor();
            if (executor!=null){
                String filename = executor.getFileName();
                Map<String, Object> args=null;
                if ((filename==null) && (executor instanceof ScriptProcessor scriptProcessor)){
                    filename = scriptProcessor.getScript();
                    args = scriptProcessor.getArgs();
                }

                if (filename != null) {
                    File file = new File(filename);
                    if (Context.getState().isProcessing()) {
                        Context.getApp().evalFileNext(file, args);
                    } else {
                        Context.getApp().evalFile(file, args);
                    }
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuRunNextbuttonRunActionPerformed

    private void menuSessionStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionStartActionPerformed
        try {
            if (!Context.getSessionManager().isStarted()) {
                String name = getString("Enter the new session name:", "");
                if (name != null) {
                    Context.getSessionManager().start(name);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSessionStartActionPerformed

    private void menuSessionHistorybuttonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionHistorybuttonAbortActionPerformed
        try {
            SessionsDialog dlg = new SessionsDialog(this, false);
            dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            showChildWindow(dlg);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSessionHistorybuttonAbortActionPerformed

    private void menuSessionStopbuttonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionStopbuttonAbortActionPerformed
        try {
            if (Context.getSessionManager().isStarted()) {
                SessionPanel sessionPanel = getSessionPanel();
                if (sessionPanel!=null){
                    tabStatus.setSelectedComponent(sessionPanel);
                }
                int id = Context.getSessionManager().getCurrentSession();
                String name = Context.getSessionManager().getCurrentName();
                String session = name.isBlank() ? String.valueOf(id) : String.valueOf(id) + "-" + name;
                
                //If no session data cancels this session instead of completing
                int numberRuns=0;
                try{
                    numberRuns = Context.getSessionManager().getNumberRuns();
                } catch (IOException ex) {
                    String msg = String.format("Cannot access current session folder to close it consistently.\nAbort the current session?", session);
                    if (SwingUtils.showOption(this, "Session", msg , OptionType.YesNo) == OptionResult.Yes) {
                        Context.getSessionManager().abort();                    
                    }
                    return;
                }
                
                if (numberRuns==0){
                    String msg = String.format("Do you want to cancel session %s with no runs?", session);
                    if (SwingUtils.showOption(this, "Session", msg , OptionType.YesNo) == OptionResult.Yes) {
                        Context.getSessionManager().cancel();                    
                    }
                    return;
                }
                                
                String msg = String.format("Do you want to complete session %s?", session);
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout(0,30));
                JLabel label = new JLabel(msg);     
                panel.add(label, BorderLayout.CENTER);        
                JCheckBox checkArchive = new JCheckBox("Archive session");     
                panel.add(checkArchive, BorderLayout.SOUTH);  
                panel.setPreferredSize(new Dimension(400, panel.getPreferredSize().height));
                
                if (showOption("Session", panel , OptionType.YesNo) == OptionResult.Yes) {
                    Context.getSessionManager().stop();
                    if (checkArchive.isSelected()){
                        SessionsDialog dlg = new SessionsDialog(this, false);
                        dlg.setSingleSession(id);
                        dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);                        
                        showChildWindow(dlg);
                    }
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSessionStopbuttonAbortActionPerformed

    private void menuSessionsMetadataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionsMetadataActionPerformed
        try{
            MetadataEditor editor = new MetadataEditor(Convert.toStringArray(SessionManager.MetadataType.values()));  //new TextEditor();
            EditorDialog dlg = editor.getDialog(this, true);
            editor.setFilePermissions(Context.getConfigFilePermissions());
            editor.load(Setup.getSessionMetadataDefinitionFile());
            editor.setReadOnly(Context.getRights().denyConfig);
            dlg.setContentPane(editor);
            dlg.setSize(480, 320);
            editor.setTitle("Session Metadata");
            showChildWindow(dlg);            
            if (editor.wasSaved()){
                Context.getSessionManager().onMetadataDefinitionChanged();
            }
        } catch (Exception ex) {
            showException(ex);
        }
        
        
    }//GEN-LAST:event_menuSessionsMetadataActionPerformed

    private void menuSessionPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionPauseActionPerformed
        try {
            Context.getSessionManager().pause();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSessionPauseActionPerformed

    private void menuSessionResumeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionResumeActionPerformed
        try {
            Context.getSessionManager().resume();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSessionResumeActionPerformed

    private void menuSessionsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuSessionsStateChanged
        try{                
            boolean enabled = !Setup.isOffline();
            boolean sessionStarted = Context.getSessionManager().isStarted();
            Boolean paused = sessionStarted ? Context.getSessionManager().isPaused(): false;     
            menuSessionStop.setEnabled(enabled && sessionStarted);
            menuSessionStart.setEnabled(enabled && !sessionStarted);           
            menuSessionReopen.setEnabled(enabled);           
            menuSessionPause.setEnabled(enabled && sessionStarted && !paused);
            menuSessionResume.setEnabled(enabled && sessionStarted && paused);                              
            
            menuSessionCreate.setEnabled(dataPanel.isShowing() && dataPanel.getSelectedFilesCount() > 0);
        } catch (Exception ex) {
        }
    }//GEN-LAST:event_menuSessionsStateChanged

    private void menuSessionReopenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionReopenActionPerformed
        try{                
            SessionReopenDialog dlg = new SessionReopenDialog(this,true, "Reopen Session");
            this.showChildWindow(dlg);
            if (dlg.getResult()) {
                Context.getSessionManager().restart(dlg.getSelectedSession());
            }          
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSessionReopenActionPerformed

    private void menuSessionCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionCreateActionPerformed
        try{
            String name = getString("Enter the session name created with the files selected in the data panel:", "");
            if (name != null) {
                List<String> files = dataPanel.getSelectedFiles();
                int id = Context.getSessionManager().create(name, files, null, null);
                showScrollableMessage("Success", "Success creating session " + id + "-" + name + " with the files:", String.join("\n", files));
            }
        } catch (Exception ex) {
            showException(ex);
        }        
    }//GEN-LAST:event_menuSessionCreateActionPerformed

    private void menuTerminalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTerminalActionPerformed
        try{
            if (menuTerminal.isSelected()){
                showTerminal();               
            } else {
                hideTerminal(); 
            }
        } catch (Exception ex) {
            showException(ex);
        }  
    }//GEN-LAST:event_menuTerminalActionPerformed

    private void menuScanPanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuScanPanelActionPerformed
        try {
            if (!Setup.isLocal()) {
                preferences.hideScanPanel = !menuScanPanel.isSelected();
                preferences.save();
            }
            setScanPanelVisible(menuScanPanel.isSelected());
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuScanPanelActionPerformed

    private void menuOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOutputActionPerformed
        try {
            if (!Setup.isLocal()) {
                preferences.hideOutputPanel = !menuOutput.isSelected();
                preferences.save();
            }
            setOutputPanelVisible(menuOutput.isSelected());
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuOutputActionPerformed

    private void tabPlotsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabPlotsStateChanged
            updateSelectedPanel(tabPlots);
    }//GEN-LAST:event_tabPlotsStateChanged

    private void tabLeftStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabLeftStateChanged
         updateSelectedPanel(tabLeft);
    }//GEN-LAST:event_tabLeftStateChanged

    private void menuCamServerViewerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCamServerViewerActionPerformed
        try {
            ch.psi.pshell.screenpanel.App.create(this, null, null);            
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCamServerViewerActionPerformed

    private void menuCamServerPipelinesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCamServerPipelinesActionPerformed
        try {
            CamServerServicePanel.createFrame(Setup.getPipelineServer(), CamServerService.Type.Pipeline, this, "Pipeline Instances");            
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCamServerPipelinesActionPerformed

    private void menuCamServerCamerasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCamServerCamerasActionPerformed
        try {
            CamServerServicePanel.createFrame(Setup.getCameraServer(), CamServerService.Type.Camera, this, "Camera Instances");            
        } catch (Exception ex) {
            showException(ex);
    }    }//GEN-LAST:event_menuCamServerCamerasActionPerformed

    private void radioPlotsVisibleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioPlotsVisibleActionPerformed
        if (!Setup.isLocal()) {
            preferences.plotsHidden = radioPlotsHidden.isSelected();
            preferences.plotsDetached = radioPlotsDetached.isSelected();
            preferences.save();
        }
        setScanPlotVisible(!radioPlotsHidden.isSelected());
        setScanPlotDetached(radioPlotsDetached.isSelected());
    }//GEN-LAST:event_radioPlotsVisibleActionPerformed

    CPythonDialog dialogCPython;
    
    private void menuCPythonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCPythonActionPerformed
        try {
            if ((dialogCPython == null) || (!dialogCPython.isDisplayable())) {
                dialogCPython = new CPythonDialog(this, false);
                dialogCPython.setDefaultCloseOperation(HIDE_ON_CLOSE);
                dialogCPython.setReadOnly(Context.getRights().denyConfig);
                dialogCPython.setSize(600, 400);
            } 
            showChildWindow(dialogCPython);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCPythonActionPerformed

    private void manuDaqbufActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manuDaqbufActionPerformed
        try {
            String url = Setup.getArchiver();
            String backend = Setup.getBackend();
            JDialog dlg = new ArchiverPanel(this, url, backend, null, false, ArchiverPanel.getDaqbufFolderArg());
            showChildWindow(dlg);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_manuDaqbufActionPerformed

    private void menuXscanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuXscanActionPerformed
        try{
            ConfigDialog dlg = new ConfigDialog(this, true);
            dlg.setDisabledKeys(new String[]{"dataFormat", "dataLayout", "dataPath"});
            dlg.setTitle("XScan");
            dlg.setConfig(ch.psi.pshell.xscan.Config.getConfig());
            dlg.setReadOnly(Context.getRights().denyConfig);
            dlg.setSize(600, 480);
            showChildWindow(dlg);
            if (dlg.getResult()) {
                ch.psi.pshell.xscan.Config.getConfig().save();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuXscanActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAbort;
    private javax.swing.JButton buttonAbout;
    private javax.swing.JButton buttonDebug;
    private javax.swing.ButtonGroup buttonGroupPlotsVisibility;
    private javax.swing.JButton buttonNew;
    private javax.swing.JButton buttonOpen;
    private javax.swing.JButton buttonPause;
    private javax.swing.JButton buttonRestart;
    private javax.swing.JButton buttonRun;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonStep;
    private javax.swing.JButton buttonStopAll;
    private ch.psi.pshell.swing.DataPanel dataPanel;
    private ch.psi.pshell.swing.DevicePoolPanel devicesPanel;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private ch.psi.pshell.swing.DevicePoolPanel imagingPanel;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator14;
    private javax.swing.JPopupMenu.Separator jSeparator15;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator18;
    private javax.swing.JPopupMenu.Separator jSeparator19;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator20;
    private javax.swing.JPopupMenu.Separator jSeparator21;
    private javax.swing.JPopupMenu.Separator jSeparator22;
    private javax.swing.JPopupMenu.Separator jSeparator23;
    private javax.swing.JPopupMenu.Separator jSeparator24;
    private javax.swing.JPopupMenu.Separator jSeparator25;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JLabel labelUser;
    private ch.psi.pshell.swing.LoggerPanel loggerPanel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuItem manuDaqbuf;
    private javax.swing.JMenuItem menuAbort;
    private javax.swing.JMenu menuAddToQueue;
    private javax.swing.JMenu menuArchiver;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu menuBlock;
    private javax.swing.JMenuItem menuCPython;
    private javax.swing.JMenu menuCamServer;
    private javax.swing.JMenuItem menuCamServerCameras;
    private javax.swing.JMenuItem menuCamServerPipelines;
    private javax.swing.JMenuItem menuCamServerViewer;
    private javax.swing.JMenuItem menuChangeUser;
    private javax.swing.JMenuItem menuChanges;
    private javax.swing.JMenuItem menuCheckSyntax;
    private javax.swing.JMenuItem menuCloseAll;
    private javax.swing.JMenuItem menuCloseAllPlots;
    private javax.swing.JMenuItem menuComment;
    private javax.swing.JMenuItem menuCommit;
    private javax.swing.JMenuItem menuConfiguration;
    private javax.swing.JMenu menuConsoleLocation;
    private javax.swing.JMenuItem menuCopy;
    private javax.swing.JMenuItem menuCreateBranch;
    private javax.swing.JMenuItem menuCreateTag;
    private javax.swing.JMenuItem menuCut;
    private javax.swing.JMenuItem menuDataFile;
    private javax.swing.JMenu menuDataPanelLocation;
    private javax.swing.JMenuItem menuDebug;
    private javax.swing.JMenuItem menuDeleteBranch;
    private javax.swing.JMenuItem menuDeleteTag;
    private javax.swing.JMenu menuDevices;
    private javax.swing.JMenu menuDevicesConfig;
    private javax.swing.JMenuItem menuDevicesDefinition;
    private javax.swing.JMenuItem menuDevicesEpics;
    private javax.swing.JMenu menuEdit;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenu menuFileNew;
    private javax.swing.JMenuItem menuFind;
    private javax.swing.JMenuItem menuFindInFiles;
    private javax.swing.JMenuItem menuFindNext;
    private javax.swing.JCheckBoxMenuItem menuFullScreen;
    private javax.swing.JMenuItem menuHelpContents;
    private javax.swing.JMenuItem menuImport;
    private javax.swing.JMenuItem menuIndent;
    private javax.swing.JMenu menuLaf;
    private javax.swing.JMenuItem menuLogQuery;
    private javax.swing.JMenuItem menuNew;
    private javax.swing.JMenuItem menuOpen;
    private javax.swing.JMenuItem menuOpenLogFile;
    private javax.swing.JMenu menuOpenRecent;
    private javax.swing.JCheckBoxMenuItem menuOutput;
    private javax.swing.JMenuItem menuPaste;
    private javax.swing.JMenuItem menuPause;
    private javax.swing.JMenu menuPlotWindow;
    private javax.swing.JMenuItem menuPlugins;
    private javax.swing.JMenuItem menuPreferences;
    private javax.swing.JMenuItem menuPull;
    private javax.swing.JMenuItem menuPush;
    private javax.swing.JMenuItem menuRedo;
    private javax.swing.JMenuItem menuReinit;
    private javax.swing.JMenuItem menuRestart;
    private javax.swing.JMenuItem menuRun;
    private javax.swing.JMenuItem menuRunNext;
    private javax.swing.JMenuItem menuSave;
    private javax.swing.JMenuItem menuSaveAs;
    private javax.swing.JCheckBoxMenuItem menuScanPanel;
    private javax.swing.JMenuItem menuSessionCreate;
    private javax.swing.JMenuItem menuSessionHistory;
    private javax.swing.JMenuItem menuSessionPause;
    private javax.swing.JMenuItem menuSessionReopen;
    private javax.swing.JMenuItem menuSessionResume;
    private javax.swing.JMenuItem menuSessionStart;
    private javax.swing.JMenuItem menuSessionStop;
    private javax.swing.JMenu menuSessions;
    private javax.swing.JMenuItem menuSessionsMetadata;
    private javax.swing.JMenuItem menuSetCurrentBranch;
    private javax.swing.JMenuItem menuSetCurrentBranch1;
    private javax.swing.JMenu menuSetLogLevel;
    private javax.swing.JMenuItem menuSettings;
    private javax.swing.JMenu menuShell;
    private javax.swing.JMenuItem menuStep;
    private javax.swing.JMenuItem menuStopAll;
    private javax.swing.JMenuItem menuStripChart;
    private javax.swing.JMenuItem menuTasks;
    private javax.swing.JCheckBoxMenuItem menuTerminal;
    private javax.swing.JMenuItem menuToggleComment;
    private javax.swing.JMenuItem menuUncomment;
    private javax.swing.JMenuItem menuUndo;
    private javax.swing.JMenuItem menuUnindent;
    private javax.swing.JMenuItem menuUpdateAll;
    private javax.swing.JMenuItem menuUsers;
    private javax.swing.JMenu menuVersioning;
    private javax.swing.JMenu menuView;
    private javax.swing.JMenu menuViewPanels;
    private javax.swing.JMenu menuViewPanels1;
    private javax.swing.JMenuItem menuXscan;
    private ch.psi.pshell.swing.OutputPanel outputPanel;
    private javax.swing.JRadioButtonMenuItem radioPlotsDetached;
    private javax.swing.JRadioButtonMenuItem radioPlotsHidden;
    private javax.swing.JRadioButtonMenuItem radioPlotsVisible;
    private ch.psi.pshell.swing.ScanPanel scanPanel;
    private ch.psi.pshell.swing.PlotPanel scanPlot;
    private ch.psi.pshell.swing.ScriptsPanel scriptsPanel;
    private javax.swing.JToolBar.Separator separatorInfo;
    private javax.swing.JToolBar.Separator separatorStopAll;
    private ch.psi.pshell.swing.Shell shell;
    private javax.swing.JSplitPane splitterDoc;
    private javax.swing.JSplitPane splitterHoriz;
    private javax.swing.JSplitPane splitterVert;
    private ch.psi.pshell.framework.StatusBar statusBar;
    private javax.swing.JTabbedPane tabDoc;
    private javax.swing.JTabbedPane tabLeft;
    private javax.swing.JTabbedPane tabPlots;
    private javax.swing.JTabbedPane tabStatus;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables

}
