package ch.psi.pshell.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import ch.psi.pshell.data.ProviderHDF5;
import ch.psi.utils.Arr;
import ch.psi.utils.ControlChar;
import ch.psi.utils.swing.ConfigDialog;
import ch.psi.utils.History;
import ch.psi.utils.IO;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.CloseableTabListener;
import ch.psi.utils.swing.SwingUtils.CloseButtonTabComponent;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import ch.psi.utils.State;
import ch.psi.utils.swing.Document;
import ch.psi.utils.swing.DocumentListener;
import ch.psi.utils.swing.Editor;
import ch.psi.utils.swing.Editor.EditorDialog;
import ch.psi.pshell.core.Configuration;
import ch.psi.pshell.core.Configuration.LogLevel;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.core.PlotListener;
import ch.psi.pshell.core.Server;
import ch.psi.pshell.core.Setup;
import ch.psi.pshell.core.VersioningManager;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.utils.Range;
import ch.psi.pshell.imaging.Renderer;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.security.AccessLevel;
import ch.psi.pshell.security.Rights;
import ch.psi.pshell.security.UsersManagerListener;
import ch.psi.pshell.security.User;
import ch.psi.pshell.swing.DataPanel.DataPanelListener;
import ch.psi.pshell.swing.DevicePoolEditor;
import ch.psi.pshell.swing.DeviceUpdateStrategyEditor;
import ch.psi.pshell.swing.EpicsConfigDialog;
import ch.psi.pshell.swing.HelpContentsDialog;
import ch.psi.pshell.swing.LoggerPanel;
import ch.psi.pshell.swing.PluginsEditor;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.swing.ScriptEditor;
import ch.psi.pshell.swing.ScriptsPanel;
import ch.psi.pshell.swing.SearchPanel;
import ch.psi.pshell.swing.TasksEditor;
import ch.psi.pshell.swing.UpdatablePanel;
import ch.psi.pshell.swing.UsersEditor;
import ch.psi.pshell.swing.SessionPanel;
import ch.psi.pshell.swing.SessionsDialog;
import ch.psi.pshell.swing.SessionReopenDialog;
import ch.psi.pshell.ui.Preferences.PanelLocation;
import ch.psi.utils.Convert;
import ch.psi.utils.swing.ExtensionFileFilter;
import ch.psi.utils.swing.MainFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import ch.psi.pshell.core.ContextListener;
import ch.psi.pshell.core.LogManager;
import ch.psi.pshell.core.SessionManager;
import ch.psi.pshell.core.SessionManager.ChangeType;
import ch.psi.pshell.imaging.FileSource;
import ch.psi.pshell.imaging.Utils;
import ch.psi.pshell.scripting.ViewPreference.PlotPreferences;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.Executor;
import ch.psi.pshell.swing.HistoryChart;
import ch.psi.pshell.swing.MetadataEditor;
import ch.psi.pshell.swing.MotorPanel;
import ch.psi.pshell.swing.RepositoryChangesDialog;
import ch.psi.pshell.swing.NextStagesPanel;
import ch.psi.utils.Config;
import ch.psi.utils.Sys;
import ch.psi.utils.Sys.OSFamily;
import ch.psi.utils.swing.PropertiesDialog;
import ch.psi.utils.swing.Terminal;
import ch.psi.utils.swing.TextEditor;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.WindowConstants;

/**
 * The main dialog of the Workbench.
 */
public class View extends MainFrame {

    static final Logger logger = Logger.getLogger(View.class.getName());

    final Context context;
    final History fileHistory;
    final String openedFilesFileName;
    final Properties openedFiles;
    final CloseableTabListener closableTabListener;
    final NextStagesPanel nextStagesPanel;

    String[] imageFileExtensions = new String[]{"png", "bmp", "gif", "tif", "tiff", "jpg", "jpeg"};
    String[] textFileExtensions = new String[]{"txt", "csv", "log"};
    String[] dataFileExtensions = new String[]{"h5", "hdf5"};

    public View() {
        super();
        try {
            initComponents();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, null, t);
            throw t;
        }
        String title = App.getArgumentValue("title");
        setTitle((title == null) ? App.getApplicationTitle() : title);
        setIcon(App.getResourceUrl("IconSmall.png"));
        if (MainFrame.isDark()) {
            for (Component b : SwingUtils.getComponentsByType(toolBar, JButton.class)) {
                //((JButton)b).setIcon(new ImageIcon(SwingUtils.invert(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl(new File(((JButton)b).getIcon().toString()).getName())))));
                //Using specific icons for dark. Could come up with icons that fit both.
                ((JButton) b).setIcon(new ImageIcon(App.getResourceUrl("dark/" + new File(((JButton) b).getIcon().toString()).getName())));
            }
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

        context = Context.getInstance();
        context.addListener(contextListener);
        MainFrame.setPersistenceFilesPermissions(context.getConfig().filePermissionsConfig);
        
        menuVersioning.setVisible(false);             
        menuSessions.setVisible(context.isHandlingSessions());        

        fileHistory = new History(getSessionPath() + "/FileHistory.dat", 10, true);
        openedFiles = new Properties();
        openedFilesFileName = getSessionPath() + "/OpenedFiles.dat";
        try (FileInputStream in = new FileInputStream(openedFilesFileName)) {
            openedFiles.load(in);
        } catch (Throwable ex) {
            logger.log(Level.FINE, null, ex);
        }

        ActionListener logLevelListener = (ActionEvent e) -> {
            context.setLogLevel(Level.parse(e.getActionCommand().toUpperCase()));
        };
        for (LogLevel level : Configuration.LogLevel.values()) {
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
        closableTabListener = (JTabbedPane tabbedPane, int index) -> {
            try {
                if (tabbedPane.getComponentAt(index) instanceof ScriptEditor) {
                    ScriptEditor editor = ((ScriptEditor) tabbedPane.getComponentAt(index));
                    if (!(tabbedPane.getSelectedComponent() == editor)) {
                        tabbedPane.setSelectedComponent(editor);
                    }
                    return editor.getTextEditor().checkChangeOnClose();
                } else if (tabbedPane.getComponentAt(index) instanceof Processor) {
                    Processor processor = ((Processor) tabbedPane.getComponentAt(index));
                    if (!(tabbedPane.getSelectedComponent() == processor)) {
                        tabbedPane.setSelectedComponent((Component) processor);
                    }
                    return processor.checkChangeOnClose();
                }
            } catch (Exception ex) {
                showException(ex);
            }
            return true;
        };
        for (PanelLocation location : PanelLocation.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(location.toString());
            item.addActionListener((java.awt.event.ActionEvent evt) -> {
                if (!App.isLocalMode()) {
                    preferences.consoleLocation = location;
                    preferences.save();
                    applyPreferences();
                } else {
                    setConsoleLocation(location);
                }
            });
            menuConsoleLocation.add(item);
        }
        context.getUsersManager().addListener(new UsersManagerListener() {
            @Override
            public void onUserChange(User user, User former) {
                closeSearchPanels();
                setConsoleLocation(consoleLocation);//User may have no rights
                setStatusTabVisible(devicesPanel, !context.getRights().hideDevices);
                setStatusTabVisible(imagingPanel, !context.getRights().hideDevices);
                setStatusTabVisible(scriptsPanel, !context.getRights().hideScripts);
                setStatusTabVisible(dataPanel, !context.getRights().hideData);
                toolBar.setVisible(!context.getRights().hideToolbar);

                labelUser.setText((context.isSecurityEnabled() && !User.DEFAULT_USER_NAME.equals(user.name)) ? user.name : "");

                updateButtons();

                for (Window window : JDialog.getWindows()) {
                    if (window instanceof ConfigDialog) {
                        //Configuration dialog is modal so does not need to be checked
                        ((ConfigDialog) window).setReadOnly(context.getRights().denyDeviceConfig);
                    } else if (window instanceof EditorDialog) {
                        Editor editor = ((EditorDialog) window).getEditor();
                        if ((editor instanceof DevicePoolEditor)
                                || (editor instanceof DeviceUpdateStrategyEditor)
                                || (editor instanceof PluginsEditor)
                                || (editor instanceof TasksEditor)
                                || (editor instanceof UsersEditor) //|| ((editor instanceof TextEditor) && (editor.getFileName().equals(Epics.getConfigFile())))
                                ) {
                            editor.setReadOnly(context.getRights().denyConfig);
                        }
                    }

                    for (ScriptEditor editor : getEditors()) {
                        editor.setReadOnly(context.getRights().denyEdit);
                    }
                }
            }
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                switch (ke.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        ctrlPressed = (ke.getKeyCode() == KeyEvent.VK_CONTROL);
                        break;
                    case KeyEvent.KEY_RELEASED:
                        ctrlPressed = false;
                }
                return false;
            }
        });
        menuPull.setVisible(false); //Disabling this menu because may be dangerous for users have such easy access
        Utils.setSelectedImageFolder(context.getSetup().getImagesPath());
        toolBar.setRollover(true);

        if (App.isPlotOnly()) {
            splitterVert.setVisible(false);
            splitterHoriz.setDividerSize(0);
            menuBar.setVisible(false);
            for (Component c : toolBar.getComponents()) {
                if ((c != buttonAbort) && (c != buttonAbout) && (c != separatorInfo)) {
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

        for (String name : App.getArgumentValues("hide")) {
            Component component = SwingUtils.getComponentByName(this, name);
            if (component != null) {
                component.setVisible(false);
            }
        }
        if (App.isOffline()) {
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
        nextStagesPanel = new NextStagesPanel();
    }

    //TODO: This flag is used to re-inject detached windows to original tabs.
    //Better doing with drad/drop instead of double/click + ctrl+close
    boolean ctrlPressed;

    @Override
    public String getSessionPath() {
        return context.getSetup().getContextPath();
    }

    //Callbacks from MainFrame
    /**
     * Called once when frame is created, before being visible
     */
    @Override
    protected void onCreate() {
        context.addListener(new ContextAdapter() {
            @Override
            public void onContextStateChanged(State state, State former) {
                if (!state.isProcessing()) {
                    if (currentScriptEditor != null) {
                        currentScriptEditor.stopExecution();
                        currentScriptEditor = null;
                    }
                    if (topLevelProcessor != null) {
                        if (!topLevelProcessor.isExecuting()) {
                            topLevelProcessor = null;
                        }
                    }
                    currentProcessor = null;
                }
                for (Processor p : getProcessors()) {
                    p.onStateChanged(state, former);
                }
                updateButtons();
            }

            @Override
            public void onPathChange(String pathId) {
                if (Setup.TOKEN_DATA.equals(pathId)) {
                    dataPanel.initialize();
                } else if (Setup.TOKEN_SCRIPT.equals(pathId)) {
                    scriptsPanel.initialize();
                }
            }
        });

        if (context.isHandlingSessions() &&  !App.isOffline()){
            try {
                context.getSessionManager().addListener((id, type) -> {
                    if (type == ChangeType.STATE) {
                        SwingUtilities.invokeLater(() -> {
                            checkSessionPanel();
                        });
                    }
                });
                checkSessionPanel();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        restorePreferences();
        if (!App.isFullScreen() && App.getInstance().isContextPersisted()) {
            //restoring again (also in App.startup) to take into avcount hidden panels displayed by restorePreferences
            restoreState();
        }

        if (tabDoc.getTabCount() > 0) {
            tabDoc.setSelectedIndex(0);
        }

        for (Processor processor : Processor.getServiceProviders()) {
            addProcessorComponents(processor);
        }

        App.getInstance().addListener(new AppListener() {
            @Override
            public boolean canExit(Object source) {
                for (int i = 0; i < tabDoc.getTabCount(); i++) {
                    if (!closableTabListener.canClose(tabDoc, i)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void willExit(Object source) {
                saveContext();
            }
        });
    }

    void addProcessorComponents(Processor processor) {
        if (processor.createMenuNew()) {
            JMenuItem item = new JMenuItem(processor.getType());
            item.addActionListener((java.awt.event.ActionEvent evt) -> {
                try {
                    openProcessor(processor.getClass(), null);
                } catch (Exception ex) {
                    showException(ex);
                }
            });
            menuFileNew.add(item);
        }
        if (processor.createFilePanel()) {
            ScriptsPanel pn = new ScriptsPanel();
            int index = tabStatus.getTabCount() - 1;
            tabStatus.add(pn, index);
            tabStatus.setTitleAt(index, processor.getType());
            pn.initialize(processor.getHomePath(), processor.getExtensions());
            pn.setListener((File file) -> {
                try {
                    openProcessor(processor.getClass(), file.getAbsolutePath());
                } catch (Exception ex) {
                    showException(ex);
                }
            });
        }
    }

    void removeProcessorComponents(Processor processor) {
        if (processor.createMenuNew()) {
            for (int i = menuFileNew.getMenuComponents().length - 1; i > 0; i--) {
                Component c = menuFileNew.getMenuComponents()[i];
                if (c instanceof JMenuItem) {
                    if (((JMenuItem) c).getText().equals(processor.getType())) {
                        menuFileNew.remove((JMenuItem) c);
                        break;
                    }
                }
            }
        }
        if (processor.createFilePanel()) {
            for (int i = tabStatus.getTabCount() - 1; i > 0; i--) {
                if (tabStatus.getComponentAt(i) instanceof ScriptsPanel) {
                    if (tabStatus.getTitleAt(i).equals(processor.getType())) {
                        tabStatus.remove(i);
                        break;
                    }
                }
            }
        }
    }

    void updateButtons() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                updateButtons();
            });
            return;
        }
        if (context != null) {
            Rights rights = context.getRights();
            boolean allowEdit = (rights != null) && !rights.denyEdit;
            boolean allowRun = (rights != null) && !rights.denyRun;
            Component selectedDocument = tabDoc.getSelectedComponent();
            State state = App.getInstance().getState();
            Processor runningProcessor = getRunningProcessor();
            if (state == State.Ready && (runningProcessor != null) && (runningProcessor instanceof QueueProcessor)) {
                state = State.Busy;
            }
            boolean showingScript = isShowingScriptEditor();
            boolean showingExecutor = isShowingExecutor();
            boolean ready = (state == State.Ready);
            boolean busy = (state == State.Busy);
            boolean paused = (state == State.Paused);
            
            buttonRun.setEnabled(allowRun && (((ready && showingExecutor) || (paused && !context.isRunningStatements()))));
            buttonDebug.setEnabled((ready && showingScript && allowRun) || (paused && context.isRunningStatements()));
            buttonPause.setEnabled(context.canPause() || ((runningProcessor != null) && (runningProcessor.canPause())));
            buttonStep.setEnabled(((ready && showingScript) || context.canStep() || ((runningProcessor != null) && (runningProcessor.canStep()))) && allowRun);
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
            menuSave.setEnabled(showingExecutor && allowEdit);
            menuSaveAs.setEnabled(showingExecutor && allowEdit);
            buttonSave.setEnabled(showingExecutor && getSelectedExecutor().canSave() && allowEdit);

            if (context.getState().isProcessing()) {
                menuRunNext.setVisible(true);
                menuRunNext.setEnabled(isShowingExecutor());
            } else {
                menuRunNext.setVisible(false);
            }
        }
    }

    void onFirstStart() {
        //setupPanelsMenu()
        if (context.getConfig().instanceName.length() > 0) {
            setTitle(App.getApplicationTitle() + " [" + context.getConfig().instanceName + "]");
        }
        //In case openCmdLineFiles() in onStart didn't open because context was not instantiated
        openCmdLineFiles(true);
    }

    void onStart() {
        MainFrame.setPersistenceFilesPermissions(context.getConfig().filePermissionsConfig);
        
        //User menu
        menuChangeUser.setEnabled(context.isSecurityEnabled());

        //Device menu
        menuDevicesConfig.removeAll();
        ActionListener deviceConfigListener = (java.awt.event.ActionEvent evt) -> {
            try {
                String ac = evt.getActionCommand();
                final GenericDevice dev = context.getDevicePool().getByName(ac);
                final ConfigDialog dlg = new ConfigDialog(View.this, false);
                dlg.setTitle("Device Configuration: " + ac);
                dlg.setConfig(dev.getConfig());
                dlg.setReadOnly(context.getRights().denyDeviceConfig);
                dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                dlg.setListener((StandardDialog sd, boolean accepted) -> {
                    if (sd.getResult()) {
                        context.saveDeviceConfiguration(dev);
                    }
                });
                showChildWindow(dlg);
            } catch (Exception ex) {
                showException(ex);
            }
        };

        for (String deviceName : context.getDevicePool().getAllDeviceNames()) {
            GenericDevice dev = context.getDevicePool().getByName(deviceName);
            if (dev.getConfig() != null) {
                JMenuItem item = new JMenuItem(deviceName);
                item.addActionListener(deviceConfigListener);
                menuDevicesConfig.add(item);
            }
        }

        App.getInstance().getDevicePanelManager().checkWindowRestart();

        menuVersioning.setVisible(context.isVersioningEnabled());        
        menuPush.setEnabled(context.isVersioningEnabled()
                && (context.getConfig().versionTrackingRemote != null)
                && (!context.getConfig().versionTrackingRemote.trim().isEmpty()));
        menuPull.setEnabled(menuPush.isEnabled());

        //Avoid re-initializing when application is first open
        if (context.getRunCount() > 0) {
            dataPanel.initialize();
        }
        context.setPlotListener(new PlotListener() {            
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
            public  void onTitleClosed(String title){
                //Do nothing... only close titles graphically
            }            
        });
    }

    /**
     * Called once in the first time the frame is shown
     */
    final HashMap<Scan, String> plotTitles = new HashMap<>();

    @Override
    protected void onOpen() {
        context.addScanListener(new ScanListener() {
            boolean plottingActive;

            @Override
            public void onScanStarted(Scan scan, final String plotTitle) {
                synchronized (plotTitles) {
                    plotTitles.put(scan, plotTitle);
                    if (!context.getExecutionPars().isScanDisplayed(scan)){
                        return;
                    }
                    PlotPanel plottingPanel = getPlotPanel(plotTitles.get(scan));
                    plottingActive = !isScanPlotDisabled();
                    plottingPanel.setPreferences(context.getPlotPreferences());
                    if (plottingActive) {
                        plottingPanel.triggerScanStarted(scan, plotTitle);
                        if (plottingPanel.isDisplayable() && SwingUtils.containsComponent(tabPlots, plottingPanel)) {
                            SwingUtilities.invokeLater(() -> {
                                tabPlots.setSelectedComponent(plottingPanel);
                            });
                        }
                    }
                }
                updateButtons();
            }

            @Override
            public void onNewRecord(Scan scan, ScanRecord record) {
                PlotPanel plottingPanel = null;
                synchronized (plotTitles) {
                    plottingPanel = getPlotPanel(plotTitles.get(scan), false);
                }
                if (plottingActive) {
                    if (plottingPanel != null) {
                        plottingPanel.triggerOnNewRecord(scan, record);
                    }
                }
            }

            @Override
            public void onScanEnded(Scan scan, Exception ex) {
                PlotPanel plottingPanel = null;
                synchronized (plotTitles) {
                    plottingPanel = getPlotPanel(plotTitles.get(scan), false);
                    plotTitles.remove(scan);
                }
                if (plottingActive) {
                    if (plottingPanel != null) {
                        plottingPanel.triggerScanEnded(scan, ex);
                    }
                }
                updateButtons();
                //Request repaint of current file tree in Data viewer to update scan contents
                dataPanel.onScanEnded();                
            }
        });

        devicesPanel.setType(Device.class);
        imagingPanel.setType(Source.class);

        shell.initialize();
        scanPanel.initialize();
        scanPlot.initialize();
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
        dataPanel.initialize();
        dataPanel.setListener(dataPanelListener);

        menuFullScreen.setSelected(isFullScreen());

        if (App.isOutputRedirected()) {
            //TODO: This is an issue in Jython 2.7. If not setting get this error when redirect stdout:
            //    console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0
            System.setProperty("python.console.encoding", "UTF-8");
            System.setOut(new PrintStream(new ConsoleStream(false)));
            System.setErr(new PrintStream(new ConsoleStream(true)));
        }

        openCmdLineFiles(false);
    }

    void openCmdLineFiles(boolean showExceptions) {
        if (!App.isPlotOnly()) {
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

    final DataPanelListener dataPanelListener = new DataPanelListener() {
        @Override
        public void plotData(DataManager dataManager, String root, String path) throws Exception {
            PlotPreferences prefs = dataManager.getPlotPreferences(root, path);
            View.this.plotData("Data", root, path, prefs, dataManager);
        }

        @Override
        public void plotData(Object array, Range range) throws Exception {
            Class type = Arr.getComponentType(array);
            if (type.isPrimitive()) {
                type = Convert.getWrapperClass(type);
            }
            if ((Arr.getRank(array) == 0) || (type == null) || !(Number.class.isAssignableFrom(type))) {
                return;
            }
            //Maintain the standard of displaying x dimension in the vertical axis (to align with scalar sampling)
            if (Arr.getRank(array) == 2) {
                array = Convert.transpose(Convert.toDouble(array));
            }

            double[] x = null;
            if (range != null) {
                if (range.getExtent().intValue() == Array.getLength(array)) {
                    x = new double[Array.getLength(array)];
                    for (int i = 0; i < x.length; i++) {
                        x[i] = i + range.min.intValue();
                    }
                }
            }
            View.this.plotData("Data", new PlotDescriptor[]{new PlotDescriptor("", array, x)}, null);
        }

        @Override
        public void openFile(String fileName) throws Exception {
            if (IO.getExtension(fileName).equalsIgnoreCase(context.getScriptType().getExtension())) {
                View.this.openScript(fileName);
            } else {
                View.this.openDataFile(fileName);
            }
        }

        @Override
        public void openScript(String script, String name) throws Exception {
            ScriptEditor editor = newScript(script);
            tabDoc.setTitleAt(tabDoc.indexOfComponent(editor), name);
            tabDoc.setSelectedComponent(editor);
        }
    };

    final ContextListener contextListener = new ContextAdapter() {
        @Override
        public void onContextInitialized(int runCount) {
            if (runCount == 0) {
                onFirstStart();
            }
            onStart();
        }

        @Override
        public void onContextStateChanged(State state, State former) {
            if ((state == State.Busy)&&(!former.isProcessing())) {
                synchronized (plotTitles) {
                    plotTitles.clear();
                }
                //Doing every time because each script can overide this
                restorePreferences();
            }
            buttonRestart.setEnabled((state != State.Initializing) && !App.isOffline());
            menuRestart.setEnabled(buttonRestart.isEnabled());
        }

        void restorePreferences() {
            setScanTableDisabled(preferences.scanTableDisabled);
            setScanPlotDisabled(preferences.scanPlotDisabled);
        }

        @Override
        public void onPreferenceChange(ViewPreference preference, Object value) {
            switch (preference) {
                case PLOT_DISABLED:
                    setScanPlotDisabled((value == null) ? preferences.scanPlotDisabled : (Boolean) value);
                    break;
                case TABLE_DISABLED:
                    setScanTableDisabled((value == null) ? preferences.scanTableDisabled : (Boolean) value);
                    break;
                case DEFAULTS:
                    restorePreferences();
                    break;
            }
        }

        @Override
        public void onNewStatement(final Statement statement) {
            updateButtons();
            if (currentScriptEditor != null) {
                currentScriptEditor.onExecutingStatement(statement);
            }

        }

        @Override
        public void onExecutingStatement(Statement statement) {
            updateButtons();
        }

        @Override
        public void onExecutedStatement(Statement statement) {
            updateButtons();
        }

        @Override
        public void onBranchChange(String branch) {
            for (ScriptEditor se : getEditors()) {
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

    public PlotPanel getPlotPanel(String plotTitle) {
        return getPlotPanel(plotTitle, true);
    }

    public List<String> getPlotTitles() {
        List<String> ret = new ArrayList<>();
        ret.add(PlotListener.DEFAULT_PLOT_TITLE);
        for (int i = 0; i < tabPlots.getTabCount(); i++) {
            if (tabPlots.getComponentAt(i) != scanPlot) {
                if (tabPlots.getComponentAt(i) instanceof PlotPanel) {
                    ret.add(tabPlots.getTitleAt(i));
                }
            }
        }
        for (String key : detachedPlots.keySet()) {
             ret.add(key);
        }
        return ret;
    }
    
    public PlotPanel getPlotPanel(String plotTitle, boolean create) {
        if ((plotTitle == null) || (plotTitle.isEmpty()) || (plotTitle.equals(PlotListener.DEFAULT_PLOT_TITLE))) {
            return scanPlot;
        }

        for (int i = 0; i < tabPlots.getTabCount(); i++) {
            if (tabPlots.getComponentAt(i) != scanPlot) {
                if (tabPlots.getTitleAt(i).equals(plotTitle)) {
                    if (tabPlots.getComponentAt(i) instanceof PlotPanel) {
                        return (PlotPanel) tabPlots.getComponentAt(i);
                    }
                }
            }
        }
        for (String key : detachedPlots.keySet()) {
            if (key.endsWith(plotTitle)) {
                return (PlotPanel) detachedPlots.get(key);
            }
        }

        if (!create) {
            return null;
        }
        PlotPanel plotPanel = new PlotPanel();
        plotPanel.initialize();
        plotPanel.setPlotTitle(plotTitle);
        plotPanel.clear();
        tabPlots.add(plotTitle, plotPanel);
        tabPlots.setSelectedComponent(plotPanel);

        int index = tabPlots.getTabCount() - 1;
        SwingUtils.setTabClosable(tabPlots, index, (JTabbedPane tabbedPane, int tabIndex) -> {
            context.removePlotContext(tabbedPane.getTitleAt(tabIndex));
            return true;
        });
        setTabDetachable(tabPlots, index, detachedPlots);
        return plotPanel;
    }
    HashMap<String, Component> detachedPlots = new HashMap<>();
    HashMap<String, Component> detachedScripts = new HashMap<>();

    boolean canDetach(Component c) {
        if (c == currentScriptEditor) {
            return !currentScriptEditor.isExecuting();
        }
        return true;
    }

    void setTabDetachable(final JTabbedPane tabbedPane, int index, final HashMap<String, Component> detachedMap) {
        Component component = tabbedPane.getComponentAt(index);
        CloseButtonTabComponent tabComponent = (CloseButtonTabComponent) tabbedPane.getTabComponentAt(index);

        final MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if ((e.getClickCount() == 2) && (tabbedPane.getSelectedComponent() == component) && canDetach(component)) {
                    int index = tabbedPane.indexOfComponent(component);
                    if (index < 0) {
                        logger.warning("Error retrieving tab index");
                        return;
                    }
                    String title = tabbedPane.getTitleAt(index);
                    tabbedPane.remove(component);
                    JDialog dlg = new JDialog(View.this, title, false);
                    dlg.setSize(component.getSize());
                    dlg.add(component);
                    dlg.addWindowListener(null);
                    dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    showChildWindow(dlg);
                    if (detachedMap != null) {
                        detachedMap.put(title, component);
                    }
                    dlg.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            if (detachedMap != null) {
                                detachedMap.values().removeIf(val -> val == component);
                            }
                            if (ctrlPressed) {
                                tabbedPane.add(dlg.getTitle(), component);
                                int index = tabbedPane.getTabCount() - 1;
                                SwingUtils.setTabClosable(tabbedPane, index, closableTabListener);
                                setTabDetachable(tabbedPane, index, detachedMap);
                                tabbedPane.setSelectedIndex(index);
                                if (component instanceof ScriptEditor) {
                                    updateScriptEditorTabTitle((ScriptEditor) component, index);
                                } else if (component instanceof Processor) {
                                    updateProcessorTabTitle((Processor) component, index);
                                }
                            }
                        }
                    });

                    /*
                    //Tried to drag detached panel back to tabDoc but didn' find way to identify mouse release on JDialog after JDialog move.
                    dlg.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentMoved(ComponentEvent e) {
                            System.out.println("Moved to: " + dlg.getLocationOnScreen());
                            Point locationWindow = dlg.getLocationOnScreen();
                            Point locationTab = tabDoc.getLocationOnScreen();
                            Rectangle rectWindow = new Rectangle(locationWindow.x, locationWindow.y, dlg.getWidth(), 20);
                            Rectangle rectTab = new Rectangle(locationTab.x, locationTab.y, tabDoc.getWidth(), 20);
                            if (rectWindow.intersects(rectTab)) {
                                dropListner.start();
                            } else {
                                dropListner.stop();
                            }
                        }
                    });
                     */
                    e.consume();
                } else {
                    tabbedPane.setSelectedComponent(component);
                }
            }
        };
        LookAndFeelType laf = MainFrame.getLookAndFeelType();
        if ((laf == null) || (!laf.hasTabHoverEffect())) {
            ((CloseButtonTabComponent) tabbedPane.getTabComponentAt(index)).getLabel().addMouseListener(listener);
        } else {
            //TODO: This is a workaround to the fact that, when using dark, flat or nimbus laf,
            //      adding the listener will make the tab hover background color not be drawn on the tab.
            //      So we keep adding/removing as needed.
            tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
                @Override
                public void stateChanged(javax.swing.event.ChangeEvent evt) {
                    if (tabComponent.isDisplayable()) {
                        if (tabbedPane.getSelectedComponent() == component) {
                            if (!tabbedPane.hasFocus()) {
                                tabbedPane.requestFocus();
                            } else {
                                if (!Arr.contains(tabComponent.getLabel().getMouseListeners(), listener)) {
                                    tabComponent.getLabel().addMouseListener(listener);
                                }
                            }
                        } else {
                            tabComponent.getLabel().removeMouseListener(listener);
                        }
                    } else {
                        tabbedPane.removeChangeListener(this);
                    }
                }
            });
            tabbedPane.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    tabComponent.getLabel().removeMouseListener(listener);
                    if (tabbedPane.getSelectedComponent() == component) {
                        if (!Arr.contains(tabComponent.getLabel().getMouseListeners(), listener)) {
                            tabComponent.getLabel().addMouseListener(listener);
                        }
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    tabComponent.getLabel().removeMouseListener(listener);
                }
            });
        }
    }

    public void setScanTableDisabled(boolean value) {
        scanPanel.setActive(!value);
    }

    public boolean isScanTableDisabled() {
        return !scanPanel.isActive();
    }

    boolean scanPlotDisabled = false;

    public void setScanPlotDisabled(boolean value) {
        scanPlotDisabled = value;
    }

    public boolean isScanPlotDisabled() {
        return scanPlotDisabled || !isPlotsVisible() || !App.isScanPlottingActive();
    }

    public Scan[] getCurrentScans() {
        return plotTitles.keySet().toArray(new Scan[0]);
    }

    public boolean hasOngoingScan() {
        return !plotTitles.isEmpty();
    }

    public List<Plot> plotData(String contextName, PlotDescriptor[] plots) throws Exception {
        return plotData(contextName, plots, context.getPlotPreferences());
    }

    public List<Plot> plotData(String contextName, PlotDescriptor[] plots, PlotPreferences preferences) throws Exception {
        ArrayList<Plot> ret = new ArrayList<>();
        PlotPanel plotPanel = getPlotPanel(contextName);
        if (preferences == null) {
            preferences = new PlotPreferences();
        }
        plotPanel.setPreferences(preferences);
        plotPanel.clear();
        if (plotPanel.isDisplayable() && SwingUtils.containsComponent(tabPlots, plotPanel)) {
            tabPlots.setSelectedComponent(plotPanel);
        }

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
                        logger.log(Level.WARNING, null, ex);
                    }
                }
            }
        }
        return ret;
    }

    public void plotData(String contextName, String root, String path) throws Exception {
        plotData(contextName, root, path, context.getPlotPreferences());
    }

    public void plotData(String contextName, String root, String path, PlotPreferences preferences) throws Exception {
        plotData(contextName, root, path, preferences, context.getDataManager());
    }

    public void plotData(String contextName, String root, String path, PlotPreferences preferences, DataManager dm) throws Exception {
        try {
            plotData(contextName, dm.getScanPlots(root, path).toArray(new PlotDescriptor[0]), preferences);
        } catch (IOException ex) {
            //If cannot open file, try with external processors
            if (!Processor.checkProcessorsPlotting(root, path, dm)) {
                throw ex;
            }
        }
    }

    void onExecQueueChanged(App.ExecutionStage[] queue) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                onExecQueueChanged(queue);
            });
            return;
        }
        if (queue.length == 0) {
            if (nextStagesPanel.isDisplayable()) {
                boolean isSelected = tabStatus.getSelectedComponent() == nextStagesPanel;
                tabStatus.remove(nextStagesPanel);
                if (isSelected) {
                    tabStatus.setSelectedComponent(outputPanel);
                }
            }
        } else {
            if (!nextStagesPanel.isDisplayable()) {
                tabStatus.addTab("Next Stages", nextStagesPanel);
                tabStatus.setSelectedComponent(nextStagesPanel);
            }
            nextStagesPanel.setExecutionQueue(queue);
        }
    }

    @Override
    protected void onDispose() {
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
        updateStatusPanels();
        Processor processor = getSelectedProcessor();
        if ((processor != null) && (processor.getPanel().isEnabled())) {
            updateProcessorTabTitle(processor, tabDoc.getSelectedIndex());
        }
    }

    /**
     * Called when window is being closed
     */
    @Override
    protected void onClosing() {
        App.getInstance().exit(View.this);
    }

    void onTaskFinished(Task task) {
        if (!context.getState().isProcessing() && (currentScriptEditor != null)) {
            currentScriptEditor.stopExecution();
            currentScriptEditor = null;
        }
        for (Processor p : getProcessors()) {
            p.onTaskFinished(task);
        }
    }

    void updateStatusPanels() {
        try {
            Component panel = tabStatus.getSelectedComponent();
            if (panel instanceof UpdatablePanel) {
                ((UpdatablePanel) panel).update();
            }
        } catch (Exception ex) {
        }
    }

    public List<ScriptEditor> getEditors() {
        ArrayList<ScriptEditor> ret = new ArrayList();
        for (int i = 0; i < tabDoc.getTabCount(); i++) {
            Component c = tabDoc.getComponentAt(i);
            if (c instanceof ScriptEditor) {
                ret.add((ScriptEditor) c);
            }
        }
        for (String key : detachedScripts.keySet()) {
            if (detachedScripts.get(key) instanceof ScriptEditor) {
                ret.add((ScriptEditor) detachedScripts.get(key));
            }
        }
        return ret;
    }

    public List<Processor> getProcessors() {
        ArrayList<Processor> ret = new ArrayList();
        for (int i = 0; i < tabDoc.getTabCount(); i++) {
            Component c = tabDoc.getComponentAt(i);
            if (c instanceof Processor) {
                ret.add((Processor) c);
            }
        }
        for (String key : detachedScripts.keySet()) {
            if (detachedScripts.get(key) instanceof Processor) {
                ret.add((Processor) detachedScripts.get(key));
            }
        }
        return ret;
    }

    public List<QueueProcessor> getQueues() {
        ArrayList<QueueProcessor> ret = new ArrayList();
        for (Processor processor : getProcessors()) {
            if (processor instanceof QueueProcessor) {
                ret.add((QueueProcessor) processor);
            }
        }
        return ret;
    }

    public List<PlotPanel> getPlotPanels() {
        ArrayList<PlotPanel> ret = new ArrayList();
        for (int i = 0; i < tabPlots.getTabCount(); i++) {
            Component c = tabPlots.getComponentAt(i);
            if ((c != scanPlot) && (c instanceof PlotPanel)) {
                ret.add((PlotPanel) c);
            }
        }
        for (String key : detachedPlots.keySet()) {
            ret.add((PlotPanel) detachedPlots.get(key));
        }
        return ret;
    }

    public List<SearchPanel> getSearchPanels() {
        ArrayList<SearchPanel> ret = new ArrayList();
        for (int i = 0; i < tabStatus.getTabCount(); i++) {
            Component c = tabStatus.getComponentAt(i);
            if (c instanceof SearchPanel) {
                ret.add((SearchPanel) c);
            }
        }
        return ret;
    }

    public ScriptEditor getSelectedEditor() {
        if (tabDoc.getSelectedComponent() instanceof ScriptEditor) {
            return ((ScriptEditor) tabDoc.getSelectedComponent());
        }
        return null;
    }

    public Processor getSelectedProcessor() {
        if (tabDoc.getSelectedComponent() instanceof Processor) {
            return ((Processor) tabDoc.getSelectedComponent());
        }
        return null;
    }

    public Executor getSelectedExecutor() {
        if (tabDoc.getSelectedComponent() instanceof Executor) {
            return ((Executor) tabDoc.getSelectedComponent());
        }
        return null;
    }

    public QueueProcessor getSelectedQueueProcessor() {
        if (tabDoc.getSelectedComponent() instanceof QueueProcessor) {
            return ((QueueProcessor) tabDoc.getSelectedComponent());
        }
        return null;
    }

    public Processor getRunningProcessor() {
        return getRunningProcessor(true);
    }

    public Processor getRunningProcessor(boolean toplevel) {
        if (toplevel){
            return ((topLevelProcessor != null) && (topLevelProcessor.isExecuting())) ? topLevelProcessor : null;
        } else {
           return  ((currentProcessor != null) && (currentProcessor.isExecuting())) ? currentProcessor : null;
        }
    }   

    boolean isShowingExecutor() {
        Component selectedDocument = tabDoc.getSelectedComponent();
        return (selectedDocument != null) && (selectedDocument instanceof Executor);
    }

    boolean isShowingProcessor() {
        Component selectedDocument = tabDoc.getSelectedComponent();
        return (selectedDocument != null) && (selectedDocument instanceof Processor);
    }

    boolean isShowingScriptEditor() {
        Component selectedDocument = tabDoc.getSelectedComponent();
        return (selectedDocument != null) && (selectedDocument instanceof ScriptEditor);
    }

    boolean isShowingQueueProcessor() {
        Component selectedDocument = tabDoc.getSelectedComponent();
        return (selectedDocument != null) && (selectedDocument instanceof QueueProcessor);
    }

    ScriptEditor currentScriptEditor;
    Processor currentProcessor;
    Processor topLevelProcessor;

    void debugScript(final boolean pauseOnStart) throws Exception {
        try {
            if (context.isPaused()) {
                context.resume();
                return;
            }
            App.getInstance().getContext().assertState(State.Ready);
            currentScriptEditor = getSelectedEditor();
            if (currentScriptEditor != null) {
                final Statement[] statements = currentScriptEditor.parse();
                currentScriptEditor.startExecution();
                App.getInstance().startTask(new Task.ScriptExecution(currentScriptEditor.getFileName(), statements, null, pauseOnStart, true));
            }
        } catch (ScriptException ex) {
            if (outputPanel.isDisplayable()) {
                outputPanel.putError(ex.getMessage());
            }
            showMessage("Script Parsing Error", ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
    }

    void runScript() throws Exception {
        App.getInstance().getContext().assertState(State.Ready);
        currentScriptEditor = getSelectedEditor();
        if (currentScriptEditor != null) {
            if (currentScriptEditor.hasChanged()) {
                if (showOption("Save", "Document has changed. Do you want to save it?", OptionType.YesNo) != OptionResult.Yes) {
                    return;
                }
                menuSaveActionPerformed(null);
            }
            if (currentScriptEditor.getFileName() != null) {
                currentScriptEditor.startExecution();
                App.getInstance().startTask(new Task.ScriptExecution(currentScriptEditor.getFileName(), null, null, false, false));
            }
        } else {
            setCurrentProcessor(getSelectedProcessor(), true);
            if (currentProcessor != null) {
                logger.info("Run processor: " + currentProcessor.getType() + " file: " + currentProcessor.getFileName());
                currentProcessor.execute();
            }
        }
    }

    void setCurrentProcessor(Processor processor, boolean topLevel) {
        if (topLevel) {
            topLevelProcessor = processor;
        }
        currentProcessor = processor;
    }

    final DocumentListener scriptChangeListener = new DocumentListener() {
        @Override
        public void onDocumentChanged(Document doc) {

            for (int i = 0; i < tabDoc.getTabCount(); i++) {
                Component c = tabDoc.getComponentAt(i);
                if (c instanceof ScriptEditor) {
                    ScriptEditor editor = ((ScriptEditor) c);
                    if (doc == editor.getDocument()) {
                        updateScriptEditorTabTitle(editor, i);
                        return;
                    }
                }
            }
        }
    };

    void updateScriptEditorTabTitle(ScriptEditor editor, int index) {
        try {
            String title = editor.getScriptName() + (editor.hasChanged() ? "*" : "");
            if (!title.equals(tabDoc.getTitleAt(index))) {
                tabDoc.setTitleAt(index, title);
                CloseButtonTabComponent tabComponent = (CloseButtonTabComponent) tabDoc.getTabComponentAt(index);
                tabComponent.updateUI();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    void updateProcessorTabTitle(Processor processor, int index) {
        if (processor.hasChanged() && processor.isTabNameUpdated()) {
            try {
                if (processor.getFileName() != null) {
                    tabDoc.setTitleAt(index, new File(processor.getFileName()).getName() + "*");
                    CloseButtonTabComponent tabComponent = (CloseButtonTabComponent) tabDoc.getTabComponentAt(index);
                    tabComponent.updateUI();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    static final int DEFAULT_CONTENT_WIDTH = 2000;

    ScriptEditor newScriptEditor(String file) throws IOException {
        final ScriptEditor editor = new ScriptEditor(!preferences.simpleEditor, !preferences.hideEditorLineNumbers, !preferences.hideEditorContextMenu);
        editor.addDocumentChangeListener(scriptChangeListener);
        editor.getTextEditor().setFilePermissions(context.getConfig().filePermissionsScripts);
        if (file != null) {
            editor.load(file);
        }

        openComponent(editor.getScriptName(), editor);

        editor.setTabSize(preferences.tabSize);
        editor.setTextPaneFont(preferences.fontEditor);
        editor.setEditorForeground(preferences.getEditorForeground());
        editor.setEditorBackground(preferences.getEditorBackground());
        editor.setReadOnly(context.getRights().denyEdit);        

        //TODO: Why is not working if calling sync?
        SwingUtilities.invokeLater(() -> {
            editor.setContentWidth((preferences.contentWidth <= 0) ? DEFAULT_CONTENT_WIDTH : preferences.contentWidth);
        });

        return editor;
    }

    public ScriptEditor openScript(String file) throws IOException {
        if (file == null) {
            return null;
        }

        for (ScriptEditor se : getEditors()) {
            if (se.getFileName() != null) {
                if ((new File(file).getCanonicalFile()).equals((new File(se.getFileName()).getCanonicalFile()))) {
                    if (tabDoc.indexOfComponent(se) >= 0) {
                        tabDoc.setSelectedComponent(se);
                    } else if (detachedScripts.containsValue(se)) {
                        se.getTopLevelAncestor().requestFocus();
                    }
                    return se;
                }
            }
        }

        final ScriptEditor editor = newScriptEditor(file);
        fileHistory.put(file);
        return editor;
    }

    /**
     * Non-script files go to the document tab but is not added file to history
     */
    public void openComponent(String title, Component c) {
        openComponent(title, c, tabDoc);
    }

    public void openComponent(String title, Component c, JTabbedPane pane) {
        pane.add(title, c);
        int index = pane.getTabCount() - 1;
        SwingUtils.setTabClosable(pane, index, closableTabListener);
        setTabDetachable(pane, index, detachedScripts);
        pane.setSelectedIndex(index);
    }

    public Editor openTextFile(String file) throws IOException {
        if (file == null) {
            return null;
        }

        TextEditor editor = new TextEditor();
        editor.setFilePermissions(context.getConfig().filePermissionsScripts);
        openComponent(new File(file).getName(), editor);
        editor.load(file);
        return editor;
    }

    public DataPanel openDataFile(String file) throws Exception {
        if (file == null) {
            return null;
        }
        DataPanel panel = new DataPanel();
        openComponent(new File(file).getName(), panel);
        panel.load(file);
        panel.setListener(dataPanelListener);
        return panel;
    }

    public Renderer openImageFile(String file) throws IOException, InterruptedException {
        if (file == null) {
            return null;
        }
        Renderer renderer = new Renderer();
        openComponent(new File(file).getName(), renderer);
        FileSource source = new FileSource(new File(file).getName(), file);
        renderer.setDevice(source);
        source.initialize();
        return renderer;
    }

    public ScriptEditor newScript(String code) throws IOException {
        final ScriptEditor editor = newScriptEditor(null);
        if (code != null) {
            editor.setText(code);
        }
        return editor;
    }

    public <T extends Processor> T openProcessor(Class<T> cls, String file) throws IOException, InstantiationException, IllegalAccessException {
        if (file != null) {
            for (Processor p : getProcessors()) {
                if ((p.getClass().isAssignableFrom(cls)) && p.getFileName() != null) {
                    try {
                        if ((new File(p.resolveFile(file)).getCanonicalFile()).equals((new File(p.getFileName()).getCanonicalFile()))) {
                            if (tabDoc.indexOfComponent(p.getPanel()) >= 0) {
                                tabDoc.setSelectedComponent(p.getPanel());
                            } else if (detachedScripts.containsValue(p.getPanel())) {
                                p.getPanel().getTopLevelAncestor().requestFocus();
                            }
                            return (T) p;
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        }
        T processor = null;
        //QueueProcessorcan be loaded as a Processor (multiple instances in Workbench) or Plugin(single instance when detached)
        if (PanelProcessor.class.isAssignableFrom(cls) && !QueueProcessor.class.isAssignableFrom(cls)) {
            for (Processor p : getProcessors()) {
                if (p.getClass() == cls) {
                    processor = (T) p;
                    processor.open(file);
                    tabDoc.setSelectedComponent(processor.getPanel());
                }
            }
        } else {
            processor = cls.newInstance();
            if (file != null) {
                file = processor.resolveFile(file);
                processor.open(file);
                openComponent(new File(processor.getFileName()).getName(), processor.getPanel());
                fileHistory.put(file);
            } else {
                openComponent("Unknown", processor.getPanel());
                processor.clear();
            }
        }
        return processor;
    }

    public void openScriptOrProcessor(String file) throws IOException, InstantiationException, IllegalAccessException {
        String extension = IO.getExtension(file);
        if (!extension.isEmpty()) {
            for (Processor processor : Processor.getServiceProviders()) {
                if (Arr.containsEqual(processor.getExtensions(), extension)) {
                    openProcessor(processor.getClass(), file);
                    return;
                }
            }
        }
        openScript(file);
    }

    public static PropertiesDialog showSettingsEditor(Frame parent, boolean modal, boolean readOnly) {
        return showPropertiesEditor("Settings", parent, Context.getInstance().getSettingsFile(), modal, readOnly);
    }

    public static PropertiesDialog showPropertiesEditor(String title, Frame parent, String fileName, boolean modal, boolean readOnly) {
        try {
            final PropertiesDialog dlg = new PropertiesDialog(parent, modal);
            dlg.setTitle((title == null) ? fileName : title);
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(fileName)) {
                props.load(in);
            }
            dlg.setProperties(props);
            dlg.setReadOnly(readOnly);
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setListener((StandardDialog sd, boolean accepted) -> {
                if (sd.getResult()) {
                    try (FileOutputStream out = new FileOutputStream(fileName);) {
                        props.store(out, null);
                        IO.setFilePermissions(fileName, Context.getInstance().getConfig().filePermissionsConfig);                
                    } catch (IOException ex) {
                        SwingUtils.showException(dlg, ex);
                    }
                }
            });
            dlg.setLocationRelativeTo(parent);
            dlg.setVisible(true);
            dlg.requestFocus();
            return dlg;

        } catch (Exception ex) {
            SwingUtils.showException(parent, ex);
        }
        return null;
    }

    public static ConfigDialog showConfigEditor(String title, Frame parent, Config cfg, boolean modal, boolean readOnly) {
        try {
            final ConfigDialog dlg = new ConfigDialog(parent, modal);
            dlg.setTitle((title == null) ? cfg.getFileName() : title);
            dlg.setConfig(cfg);
            dlg.setReadOnly(readOnly);
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setListener((StandardDialog sd, boolean accepted) -> {
                if (sd.getResult()) {
                    try {
                        cfg.save();
                    } catch (IOException ex) {
                        SwingUtils.showException(dlg, ex);
                    }
                }
            });
            dlg.setLocationRelativeTo(parent);
            dlg.setVisible(true);
            dlg.requestFocus();
            return dlg;

        } catch (Exception ex) {
            SwingUtils.showException(parent, ex);
        }
        return null;
    }

    boolean isPlotsVisible() {
        if (plotsDetached) {
            return plotFrame.isVisible();
        } else {
            return tabPlots.isVisible();
        }
    }

    void setScanPlotVisible(boolean value) {
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

    void setTabPlotVisible(boolean value) {
        tabPlots.setVisible(value);
        splitterHoriz.setDividerSize(value ? splitterVert.getDividerSize() : 0);
    }

    JFrame plotFrame;
    boolean plotsDetached;

    void setScanPlotDetached(boolean value) {
        if (plotsDetached != value) {
            if (App.isPlotOnly()) {
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
            menuPlotWindowDetached.setSelected(value);
        }
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

    void setConsoleLocation(PanelLocation location) {
        if (App.isPlotOnly()) {
            return;
        }
        consoleLocation = location;

        if ((context.getRights().hideConsole) || App.isOffline()) {
            location = PanelLocation.Hidden;
        }

        PanelLocation current = getLocation(shell);
        if (current != location) {
            Container topLevel = shell.getTopLevelAncestor();
            Container parent = shell.getParent();
            if (parent != null) {
                parent.remove(shell);
            }
            if (topLevel instanceof Window) {
                if ((topLevel != this) && (Arr.contains(getPersistedWindows(), topLevel))) {
                    removePersistedWindow((Window) topLevel);
                    ((Window) topLevel).dispose();
                }
            }

            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/ui/View");
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
                java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/ui/View");
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
            if (context.getSessionManager().getCurrentSession() > 0) {
                showSessionPanel();
            } else {
                hideSessionPanel();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    SessionPanel getSessionPanel() {
        for (int i = tabStatus.getTabCount() - 1; i > 0; i--) {
            if (tabStatus.getComponentAt(i) instanceof SessionPanel) {
                return (SessionPanel) tabStatus.getComponentAt(i);
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

    public void restorePreferences() {
        preferences = Preferences.load(context.getSetup().getContextPath());
        applyPreferences();
        if (App.getInstance().isContextPersisted()) {
            for (String file : openedFiles.stringPropertyNames()) {
                try {
                    openScriptOrProcessor(file);
                } catch (Exception ex) {
                    logger.log(Level.INFO, null, ex);
                }
            }
        }
    }

    public void applyPreferences() {
        shell.setTextPaneFont(preferences.fontShellPanel);
        shell.setTextInputFont(preferences.fontShellCommand);
        shell.setPropagateVariableEvaluation(!preferences.noVariableEvaluationPropagation);
        ScriptEditor.setPropagateVariableEvaluation(!preferences.noVariableEvaluationPropagation);
        outputPanel.setTextPaneFont(preferences.fontOutput);
        devicesPanel.setAsyncUpdate(preferences.asyncViewersUpdate);
        dataPanel.setCached(preferences.cachedDataPanel);
        dataPanel.setPlottingScripts(preferences.processingScripts);
        MotorPanel.setDefaultShowHoming(preferences.showHomingButtons);
        MotorPanel.setDefaultShowJog(preferences.showJogButtons);
        for (int i = 0; i < tabDoc.getTabCount(); i++) {
            if (tabDoc.getComponentAt(i) instanceof ScriptEditor) {
                ScriptEditor editor = ((ScriptEditor) tabDoc.getComponentAt(i));
                editor.setTextPaneFont(preferences.fontEditor);
                editor.setContentWidth((preferences.contentWidth <= 0) ? DEFAULT_CONTENT_WIDTH : preferences.contentWidth);
                editor.setEditorForeground(preferences.getEditorForeground());
                editor.setEditorBackground(preferences.getEditorBackground());
            }
        }
        showEmergencyStop(preferences.showEmergencyStop && !App.isOffline());
        PlotBase.setPlotBackground(preferences.plotBackground);
        PlotBase.setGridColor(preferences.gridColor);
        PlotBase.setOutlineColor(preferences.outlineColor);
        PlotBase.setDefaultLabelFont(preferences.fontPlotLabel);
        PlotBase.setDefaultTickFont(preferences.fontPlotTick);
        PlotBase.setOffscreenBuffer(!preferences.disableOffscreenBuffer);
        PlotPanel.setTitleFont(preferences.fontPlotTitle);
        HistoryChart.setDefaultAsync(preferences.asyncViewersUpdate);

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

        if (!App.isLocalMode()) {
            setScanPlotDetached(preferences.plotsDetached);
            setConsoleLocation(preferences.consoleLocation);
        }

        statusBar.setShowDataFileName(!preferences.hideFileName);
    }

    void saveOpenedFiles() {
        openedFiles.clear();
        for (int i = 0; i < tabDoc.getTabCount(); i++) {
            if (tabDoc.getComponentAt(i) instanceof ScriptEditor) {
                ScriptEditor editor = ((ScriptEditor) tabDoc.getComponentAt(i));
                if (editor.getFileName() != null) {
                    openedFiles.setProperty(editor.getFileName(), "");
                }
            } else if (tabDoc.getComponentAt(i) instanceof Processor) {
                Processor processor = ((Processor) tabDoc.getComponentAt(i));
                if (processor.getFileName() != null) {
                    openedFiles.setProperty(processor.getFileName(), "");
                }
            }
        };
        try (FileOutputStream out = new FileOutputStream(openedFilesFileName)) {
            openedFiles.store(out, null);
            IO.setFilePermissions(openedFilesFileName, context.getConfig().filePermissionsConfig);
        } catch (Exception ex) {
            logger.log(Level.FINE, null, ex);
        }
    }

    void showEmergencyStop(boolean value) {
        separatorStopAll.setVisible(value);
        buttonStopAll.setVisible(value);
        menuStopAll.setVisible(value);
    }

    void saveContext() {
        if (App.getInstance().isContextPersisted()) {
            saveOpenedFiles();
        }
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

    public JPanel showPanel(final String name) {
        return App.getInstance().getDevicePanelManager().showPanel(name);
    }

    public ch.psi.pshell.swing.Shell getShell() {
        return shell;
    }

    ;

    public JPanel showPanel(final GenericDevice dev) {
        return App.getInstance().getDevicePanelManager().showPanel(dev);
    }

    void openFile(File f) throws Exception {
        openFile(f, null);
    }

    void openFile(File f, Processor processor) throws Exception {
        String fileName = f.getPath();
        String ext = IO.getExtension(f);
        if (ext != null) {
            ext = ext.toLowerCase();
        }
        if (Arr.containsEqual(imageFileExtensions, ext)) {
            openImageFile(fileName);
        } else if (Arr.containsEqual(textFileExtensions, ext)) {
            openTextFile(fileName).setReadOnly(true);
        } else if (Arr.containsEqual(dataFileExtensions, ext)) {
            openDataFile(fileName);
        } else if (StripChart.FILE_EXTENSION.equals(ext)) {
            StripChart stripChart = new StripChart(View.this, false, App.getStripChartFolderArg());
            openComponent(f.getName(), stripChart.getPlotPanel());
            stripChart.open(f);
            stripChart.start();
        } else {
            if (processor == null) {
                openScript(fileName);
            } else {
                openProcessor(processor.getClass(), fileName);
            }
        }
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
        terminal = new Terminal(context.getSetup().getHomePath(), preferences.terminalFontSize);
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
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                if (dropping){
                    g.setColor(new Color(44, 144, 254));
                    g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
            }
        };
        shell = new ch.psi.pshell.swing.Shell();
        tabPlots = new javax.swing.JTabbedPane();
        scanPlot = new ch.psi.pshell.swing.PlotPanel();
        statusBar = new ch.psi.pshell.ui.StatusBar();
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
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        menuViewPanels = new javax.swing.JMenu();
        menuConsoleLocation = new javax.swing.JMenu();
        menuPlotWindow = new javax.swing.JMenu();
        menuViewPlotWindow = new javax.swing.JCheckBoxMenuItem();
        menuPlotWindowDetached = new javax.swing.JCheckBoxMenuItem();
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

        splitterVert.setBorder(null);
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
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/ui/View"); // NOI18N
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

        splitterDoc.setBorder(null);
        splitterDoc.setDividerSize(0);
        splitterDoc.setName("splitterDoc"); // NOI18N

        tabLeft.setName("tabLeft"); // NOI18N
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

        scanPlot.setName("scanPlot"); // NOI18N

        javax.swing.GroupLayout scanPlotLayout = new javax.swing.GroupLayout(scanPlot);
        scanPlot.setLayout(scanPlotLayout);
        scanPlotLayout.setHorizontalGroup(
            scanPlotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 439, Short.MAX_VALUE)
        );
        scanPlotLayout.setVerticalGroup(
            scanPlotLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 649, Short.MAX_VALUE)
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
            .addComponent(splitterHoriz, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE)
        );

        statusBar.setName("statusBar"); // NOI18N

        toolBar.setFloatable(false);
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
        buttonRestart.setText(bundle.getString("View.buttonRestart.text")); // NOI18N
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
        buttonDebug.setText(bundle.getString("View.buttonDebug.text")); // NOI18N
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
        buttonStep.setText(bundle.getString("View.buttonStep.text")); // NOI18N
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
        buttonStopAll.setText(bundle.getString("View.buttonStopAll.text")); // NOI18N
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

        menuPlotWindow.setText(bundle.getString("View.menuPlotWindow.text")); // NOI18N
        menuPlotWindow.setName("menuPlotWindow"); // NOI18N

        menuViewPlotWindow.setSelected(true);
        menuViewPlotWindow.setText(bundle.getString("View.menuViewPlotWindow.text")); // NOI18N
        menuViewPlotWindow.setName("menuViewPlotWindow"); // NOI18N
        menuViewPlotWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuViewPlotWindowActionPerformed(evt);
            }
        });
        menuPlotWindow.add(menuViewPlotWindow);

        menuPlotWindowDetached.setText(bundle.getString("View.menuPlotWindowDetached.text")); // NOI18N
        menuPlotWindowDetached.setName("menuPlotWindowDetached"); // NOI18N
        menuPlotWindowDetached.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPlotWindowDetachedActionPerformed(evt);
            }
        });
        menuPlotWindow.add(menuPlotWindowDetached);

        menuView.add(menuPlotWindow);

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
            .addComponent(statusBar, javax.swing.GroupLayout.DEFAULT_SIZE, 1050, Short.MAX_VALUE)
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
            JFileChooser chooser = new JFileChooser(context.getSetup().getScriptPath());
            //FileNameExtensionFilter filter = new FileNameExtensionFilter("Script files", "py", "groovy", "js");
            //chooser.setFileFilter(filter);
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Script files (*." + context.getScriptType().getExtension() + ")", 
                    new String[]{String.valueOf(context.getScriptType().getExtension())}));
            //chooser.addChoosableFileFilter(new ExtensionFileFilter("Script files (*.py, *.groovy, *.js)", new String[]{"py", "groovy", "js"}));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Java files (*.java)", new String[]{"java"}));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Text files (*.txt, *.csv, *.log)", textFileExtensions));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Data files (*.h5)", dataFileExtensions));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Strip Chart definition file (*." + StripChart.FILE_EXTENSION + ")", StripChart.FILE_EXTENSION));
            chooser.addChoosableFileFilter(new ExtensionFileFilter("Image files (*.png, *.bmp, *.gif, *.jpg)", imageFileExtensions));

            HashMap<FileNameExtensionFilter, Processor> processors = new HashMap<>();
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
        App.getInstance().exit(this);
    }//GEN-LAST:event_menuExitActionPerformed

    private void menuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAboutActionPerformed
        AboutDialog aboutDialog = new AboutDialog(this, true);
        aboutDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        showChildWindow(aboutDialog);
    }//GEN-LAST:event_menuAboutActionPerformed

    private void menuRestartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRestartActionPerformed
        try {
            if (context.getState() != State.Initializing) {
                App.getInstance().startTask(new Task.Restart());
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
            context.pause();
            Processor runningProcessor = getRunningProcessor();
            if (runningProcessor != null) {
                runningProcessor.pause();
            } 
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonPauseActionPerformed

    private void buttonStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStepActionPerformed
        try {
            Processor runningProcessor = getRunningProcessor();
            if (runningProcessor != null) {
                runningProcessor.step();
            } else {
                if (context.getState().isReady()) {
                    debugScript(true);
                } else {
                    context.step();
                }
            }
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStepActionPerformed

    private void buttonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAbortActionPerformed

        try {
            if (currentProcessor != null) {
                currentProcessor.abort();
                currentProcessor = null;
            }
            if (topLevelProcessor != null) {
                topLevelProcessor.abort();
                topLevelProcessor = null;
            }
            context.abort();
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_buttonAbortActionPerformed

    private void tabDocStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabDocStateChanged
        updateButtons();
        Component c = tabDoc.getSelectedComponent();
        if (context != null) {
            for (ch.psi.pshell.core.Plugin plugin : context.getPlugins()) {
                if (plugin instanceof Panel) {
                    Panel panel = (Panel) plugin;
                    if (panel == c) {
                        panel.onShow();
                    }
                }
            }
        }
    }//GEN-LAST:event_tabDocStateChanged

    private void menuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveActionPerformed
        try {
            ScriptEditor editor = getSelectedEditor();
            if (editor != null) {
                if (editor.getFileName() == null) {
                    menuSaveAsActionPerformed(null);
                } else {
                    editor.save();
                    tabDoc.setTitleAt(tabDoc.getSelectedIndex(), editor.getScriptName());
                }
            } else {
                Processor processor = getSelectedProcessor();
                if (processor != null) {
                    if (processor.getFileName() == null) {
                        menuSaveAsActionPerformed(null);
                    } else {
                        processor.save(context.getConfig().filePermissionsScripts);
                        if (processor.getFileName() != null) {
                            if (processor.isTabNameUpdated()) {
                                tabDoc.setTitleAt(tabDoc.getSelectedIndex(), new File(processor.getFileName()).getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
        saveContext();
    }//GEN-LAST:event_menuSaveActionPerformed

    private void menuSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveAsActionPerformed
        try {
            ScriptEditor editor = getSelectedEditor();
            if (editor != null) {
                JFileChooser chooser = new JFileChooser(context.getSetup().getScriptPath());
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Script file", context.getScriptType().getExtension());
                chooser.setFileFilter(filter);
                if (editor.getFileName() != null) {
                    try {
                        chooser.setSelectedFile(new File(editor.getFileName()));
                    } catch (Exception ex) {
                    }
                }

                int rVal = chooser.showSaveDialog(this);
                if (rVal == JFileChooser.APPROVE_OPTION) {
                    String fileName = chooser.getSelectedFile().getAbsolutePath();
                    if ((IO.getExtension(fileName).isEmpty()) && (context.getScriptType() != null)) {
                        String extension = "." + context.getScriptType().getExtension();
                        fileName += extension;
                    }
                    editor.saveAs(fileName);
                    fileHistory.put(fileName);
                    tabDoc.setTitleAt(tabDoc.getSelectedIndex(), editor.getScriptName());
                }
            } else {
                Processor processor = getSelectedProcessor();
                if (processor != null) {
                    JFileChooser chooser = new JFileChooser(context.getSetup().expandPath(processor.getHomePath()));
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(processor.getDescription(), processor.getExtensions());
                    chooser.setFileFilter(filter);
                    if (processor.getFileName() != null) {
                        try {
                            chooser.setSelectedFile(new File(processor.getFileName()));
                        } catch (Exception ex) {
                        }
                    }

                    int rVal = chooser.showSaveDialog(this);
                    if (rVal == JFileChooser.APPROVE_OPTION) {
                        String fileName = chooser.getSelectedFile().getAbsolutePath();
                        if (IO.getExtension(fileName).isEmpty()) {
                            String extension = "." + processor.getExtensions()[0];
                            fileName += extension;
                        }
                        processor.saveAs(fileName, context.getConfig().filePermissionsScripts);
                        if (processor.getFileName() != null) {
                            fileHistory.put(processor.getFileName());
                            if (processor.isTabNameUpdated()) {
                                tabDoc.setTitleAt(tabDoc.getSelectedIndex(), new File(processor.getFileName()).getName());
                            }
                        }
                    }
                }
            }
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
                        fileHistory.remove(file);
                    }
                };
                for (String file : fileHistory.get()) {
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
                    String filename = executor.getFileName();
                    menuAddToQueue.setEnabled(filename != null);
                    if (filename != null) {
                        if (queues.size() == 0) {
                            JMenuItem item = new JMenuItem("New");
                            item.addActionListener((e) -> {
                                try {
                                    QueueProcessor tq = openProcessor(QueueProcessor.class, null);
                                    tq.addNewFile(filename);
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
                                            queues.get(index).addNewFile(filename);
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

            User user = context.getUser();
            menuUsers.setVisible(context.isSecurityEnabled() && user.accessLevel == AccessLevel.administrator);
        }
    }//GEN-LAST:event_menuFileStateChanged

    private void menuConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuConfigurationActionPerformed
        try {
            ConfigDialog dlg = new ConfigDialog(this, true);
            dlg.setTitle("Configuration");
            dlg.setConfig(context.getConfig());
            dlg.setReadOnly(context.getRights().denyConfig);
            dlg.setSize(600, 480);
            showChildWindow(dlg);
            if (dlg.getResult()) {
                context.saveConfiguration();// Not doing with cfg in order to give the context a change to log
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuConfigurationActionPerformed

    private void menuViewStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuViewStateChanged
        if (menuView.isSelected()) {
            try {
                menuViewPlotWindow.setSelected(isPlotsVisible());
                menuFullScreen.setSelected(isFullScreen());
                menuTerminal.setSelected(isTerminalVisible());
                for (Component item : menuConsoleLocation.getMenuComponents()) {
                    ((JRadioButtonMenuItem) item).setSelected(((JRadioButtonMenuItem) item).getText().equals(consoleLocation.toString()));
                }
            } catch (Exception ex) {
            }
        }
    }//GEN-LAST:event_menuViewStateChanged

    private void menuViewPlotWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuViewPlotWindowActionPerformed
        setScanPlotVisible(menuViewPlotWindow.isSelected());
    }//GEN-LAST:event_menuViewPlotWindowActionPerformed

    EditorDialog devicePoolEditorDlg;

    private void menuDevicesDefinitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDevicesDefinitionActionPerformed
        try {
            if ((devicePoolEditorDlg == null) || !devicePoolEditorDlg.isDisplayable()) {
                DevicePoolEditor devicePoolEditor;
                devicePoolEditor = new DevicePoolEditor();
                devicePoolEditorDlg = devicePoolEditor.getDialog(this, false);
                devicePoolEditorDlg.setSize(900, 400);
                devicePoolEditor.setFilePermissions(context.getConfig().filePermissionsConfig);
                devicePoolEditor.load(context.getSetup().getDevicePoolFile());
                devicePoolEditor.setReadOnly(context.getRights().denyConfig);
                devicePoolEditor.setTitle("Device Pool Definition");
            }
            showChildWindow(devicePoolEditorDlg);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDevicesDefinitionActionPerformed

    private void menuDevicesEpicsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDevicesEpicsActionPerformed
        try {
            EpicsConfigDialog dlg = new EpicsConfigDialog(this, true);
            dlg.setReadOnly(context.getRights().denyConfig);
            dlg.setTitle("EPICS Properties");
            showChildWindow(dlg);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDevicesEpicsActionPerformed

    private void menuOpenLogFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenLogFileActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(context.getSetup().getLogPath());
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Log files", "log");
            chooser.setFileFilter(filter);
            chooser.setSelectedFile(new File(context.getLogFileName()));
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
            if (context.getState()==State.Paused){
                context.resume();
                Processor runningProcessor = getRunningProcessor();
                if ((runningProcessor != null) && runningProcessor.canPause()){
                    runningProcessor.resume();
                }
            } else {
                runScript();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonRunActionPerformed

    private void menuPreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPreferencesActionPerformed
        try {
            PreferencesDialog dlg = new PreferencesDialog(this, true);
            dlg.set(preferences);
            dlg.setReadOnly(context.getRights().denyPrefs);
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
            context.stopAll();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuStopAllActionPerformed

    private void menuPluginsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPluginsActionPerformed
        try {
            PluginsEditor editor = new PluginsEditor();  //new TextEditor();
            EditorDialog dlg = editor.getDialog(this, false);
            editor.setFilePermissions(context.getConfig().filePermissionsConfig);
            editor.load(context.getSetup().getPluginsConfigurationFile());
            editor.setReadOnly(context.getRights().denyConfig);
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
        updateStatusPanels();
    }//GEN-LAST:event_tabStatusStateChanged

    private void menuUpdateAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuUpdateAllActionPerformed
        try {
            context.updateAll();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuUpdateAllActionPerformed

    private void menuPushActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPushActionPerformed
        try {
            context.assertVersioningEnabled();
            
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
                App.getInstance().startTask(new Task.PushUpstream(checkAll.isSelected(), checkForce.isSelected(), checkTags.isSelected()));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuPushActionPerformed

    private void menuSetCurrentBranchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSetCurrentBranchActionPerformed
        try {
            context.assertVersioningEnabled();
            List<String> branches = context.getVersioningManager().getLocalBranches();
            String current = context.getVersioningManager().getCurrentBranch();
            String branch = getString("Select working branch:", branches.toArray(new String[0]), current);
            if ((branch != null) && (!branch.equals(current))) {
                App.getInstance().startTask(new Task.Checkout(true, branch));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSetCurrentBranchActionPerformed

    private void menuCreateBranchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCreateBranchActionPerformed
        try {
            context.assertVersioningEnabled();
            String branch = getString("Enter branch name:", null);
            if (branch != null) {
                context.getVersioningManager().createBranch(branch);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCreateBranchActionPerformed

    private void menuDeleteBranchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDeleteBranchActionPerformed
        try {
            context.assertVersioningEnabled();
            List<String> branches = context.getVersioningManager().getLocalBranches();
            String current = context.getVersioningManager().getCurrentBranch();
            branches.remove(VersioningManager.MASTER_BRANCH);
            //branches.remove(current);
            String ret = getString("Select branch to be deleted:", branches.toArray(new String[0]), null);
            if (ret != null) {
                context.getVersioningManager().deleteBranch(ret);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDeleteBranchActionPerformed

    private void menuPullActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPullActionPerformed
        try {
            context.assertVersioningEnabled();
            App.getInstance().startTask(new Task.PullUpstream());
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
            editor.setFilePermissions(context.getConfig().filePermissionsConfig);
            editor.load(context.getSetup().getTasksFile());            
            editor.setReadOnly(context.getRights().denyConfig);
            editor.setTitle("Tasks");
        } catch (FileNotFoundException | NoSuchFileException ex) {
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuTasksActionPerformed

    private void menuImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuImportActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(context.getSetup().getScriptPath());
            chooser.setAcceptAllFileFilterUsed(false);
            HashMap<FileNameExtensionFilter, Importer> importers = new HashMap<>();
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
                editor.setFileName(Paths.get(Context.getInstance().getSetup().getScriptPath(), IO.getPrefix(file) + "." + context.getScriptType().getExtension()).toString());
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
            context.assertVersioningEnabled();
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
                    context.getVersioningManager().createTag(name.getText(), message.getText().isEmpty() ? null : message.getText());
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
            context.assertVersioningEnabled();
            List<String> tags = context.getVersioningManager().getTags();
            String ret = getString("Select tag to be deleted:", tags.toArray(new String[0]), null);
            if (ret != null) {
                context.getVersioningManager().deleteTag(ret);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuDeleteTagActionPerformed

    private void menuSetCurrentBranch1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSetCurrentBranch1ActionPerformed
        try {
            context.assertVersioningEnabled();
            List<String> tags = context.getVersioningManager().getTags();
            String tag = getString("Select tag to checkout:", tags.toArray(new String[0]), null);
            if (tag != null) {
                App.getInstance().startTask(new Task.Checkout(false, tag));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSetCurrentBranch1ActionPerformed

    private void menuUsersbuttonAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuUsersbuttonAbortActionPerformed
        try {
            Editor editor = new UsersEditor(context.getUsersManager());
            editor.setFilePermissions(context.getConfig().filePermissionsConfig);
            EditorDialog dlg = editor.getDialog(this, false);
            dlg.setSize(640, 360);
            showChildWindow(dlg);
            editor.load(null);            
            editor.setReadOnly(context.getRights().denyConfig);
            editor.setTitle("Users");
        } catch (FileNotFoundException | NoSuchFileException ex) {
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuUsersbuttonAbortActionPerformed

    private void menuViewPanelsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuViewPanelsStateChanged
        menuViewPanels.removeAll();
        if (menuViewPanels.isSelected()) {
            for (ch.psi.pshell.core.Plugin plugin : context.getPlugins()) {
                if (plugin instanceof Panel) {
                    Panel panel = (Panel) plugin;
                    JCheckBoxMenuItem item = new JCheckBoxMenuItem(panel.getPluginName(), panel.isLoaded());
                    menuViewPanels.add(item);
                    item.addActionListener((ActionEvent e) -> {
                        panel.setLoaded(item.isSelected());
                    });
                }
            }
        }
    }//GEN-LAST:event_menuViewPanelsStateChanged

    private void menuChangeUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuChangeUserActionPerformed
        try {
            String current = context.getUser().name;
            String name = getString("Select user:", context.getUsersManager().getUserNames(), current);
            if ((name != null) && (!name.equals(current))) {
                context.getUsersManager().selectUser(name);
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
            context.assertVersioningEnabled();
            String commitMessage = getString("Enter commit message:", "");
            if (commitMessage != null) {
                App.getInstance().startTask(new Task.Commit(commitMessage));
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCommitActionPerformed

    private void closeSearchPanels() {
        for (SearchPanel panel : getSearchPanels()) {
            tabStatus.remove(panel);
        }
    }

    private void menuCloseAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCloseAllActionPerformed
        try {
            closeSearchPanels();
            for (ScriptEditor editor : getEditors()) {
                if (tabDoc.indexOfComponent(editor) >= 0) {
                    tabDoc.remove(editor);
                } else if (detachedScripts.containsValue(editor)) {
                    detachedScripts.values().removeIf(val -> val == editor);
                    editor.getTopLevelAncestor().setVisible(false);
                }
            }
            for (Processor processor : getProcessors()) {
                JPanel panel = processor.getPanel();
                int index = tabDoc.indexOfComponent(panel);
                if (index >= 0) {
                    if (SwingUtils.isTabClosable(tabDoc, index)){
                        tabDoc.remove(processor.getPanel());
                    }
                } else if (detachedScripts.containsValue(processor)) {
                    detachedScripts.values().removeIf(val -> val == processor);
                    panel.getTopLevelAncestor().setVisible(false);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCloseAllActionPerformed

    private void menuSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSetupActionPerformed
        try {
            ArrayList<String> plugins = new ArrayList<>();
            for (ch.psi.pshell.core.Plugin p : context.getPlugins()) {
                plugins.add(p.getPluginName());
            }

            ArrayList<String> extensions = new ArrayList<>();
            for (File f : context.getExtensions()) {
                extensions.add(f.getName());
            }
                        

            Setup setup = context.getSetup();
            Server server = context.getServer();
            String[][] entries = new String[][]{
                {"Process", Sys.getProcessName()},
                {"User", Sys.getUserName()},
                {"Version", App.getApplicationBuildInfo()},
                {"Java", /*System.getProperty("java.vendor") + " " + */
                    System.getProperty("java.vm.name") + " (" + System.getProperty("java.version") + ")"},
                {"Jar file", String.valueOf(context.getSetup().getJarFile())},
                {"HDF5", Convert.arrayToString(ProviderHDF5.getVersion(), ".")},
                {"Arguments", String.join(" ", App.getArguments())},
                {"Plugins", String.join("; ", plugins)},                
                {"Extensions", String.join("; ", extensions)},
                {"Packages", String.join("; ", context.getPackages())},                
                {"Current folder", new File(".").getCanonicalPath()},
                {"Home path", setup.getHomePath()},
                {"Output path", setup.getOutputPath()},
                {"Config path", setup.getConfigPath()},
                {"Devices path", setup.getDevicesPath()},
                {"Script path", setup.getScriptPath()},
                {"Library path", String.join("; ", setup.getLibraryPath())},
                {"Plugins path", setup.getPluginsPath()},
                {"Extensions path", setup.getExtensionsPath()},
                {"WWW path", setup.getWwwPath()},
                {"Data path", setup.getDataPath()},
                {"Log path", setup.getLogPath()},
                {"Context path", setup.getContextPath()},
                {"Sessions path", setup.getSessionsPath()},
                {"Help path", setup.getHelpPath()},
                {"Startup script", setup.getStartupScript()},
                {"Config file", setup.getConfigFile()},
                {"Settings file", setup.getSettingsFile()},
                {"Device pool", setup.getDevicePoolFile()},
                {"Server URL", server == null ? "" : server.getInterfaceURL()},};

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
                context.reinit();
            }).start();

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuReinitActionPerformed

    private void menuCloseAllPlotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCloseAllPlotsActionPerformed
        try {
            for (PlotPanel panel : getPlotPanels()) {
                if (tabPlots.indexOfComponent(panel) >= 0) {
                    tabPlots.remove(panel);
                } else if (detachedPlots.containsValue(panel)) {
                    detachedPlots.values().removeIf(val -> val == panel);
                    panel.getTopLevelAncestor().setVisible(false);
                }
            }
            scanPlot.clear();
            //scanPanel.clear();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuCloseAllPlotsActionPerformed

    private void menuPlotWindowDetachedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPlotWindowDetachedActionPerformed
        try {
            if (!App.isLocalMode()) {
                preferences.plotsDetached = menuPlotWindowDetached.isSelected();
                preferences.save();
            }
            setScanPlotDetached(menuPlotWindowDetached.isSelected());
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuPlotWindowDetachedActionPerformed

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
                String[] ignore = new String[]{context.getSetup().getDefaultScriptLibPath()};
                SearchPanel pn = new SearchPanel(context.getSetup().getScriptPath(), "*." + context.getScriptType().getExtension(),
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
                setTabDetachable(tabStatus, index, null);
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
            JDialog dlg = new StripChart(View.this, false, App.getStripChartFolderArg());
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
            context.assertVersioningEnabled();
            final List<org.eclipse.jgit.diff.DiffEntry> diff = context.diff();
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
                logQueryChooser = new JFileChooser(context.getSetup().getLogPath());
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Log files", "log");
                logQueryChooser.setFileFilter(filter);
                logQueryChooser.setMultiSelectionEnabled(true);
                logQueryChooser.setSelectedFile(new File(context.getLogFileName()));
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
                        SwingUtils.setEnumCombo(logQueryLevel, Configuration.LogLevel.class);
                        logQueryLevel.remove(0); //Remove LogLevel.Off
                        logQueryLevel.setSelectedItem(Configuration.LogLevel.Info);
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
            String level = context.getLogLevel().toString();
            for (Component c : menuSetLogLevel.getMenuComponents()) {
                if (c instanceof JMenuItem) {
                    JMenuItem item = ((JMenuItem) c);
                    item.setSelected(item.getText().equalsIgnoreCase(level));
                }
            }

            try {
                menuSettings.setEnabled(!context.getSettings().isEmpty());
            } catch (IOException ex) {
                menuSettings.setEnabled(false);
            }
            updateButtons();
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
            showSettingsEditor(this, true, context.getRights().denyConfig);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSettingsbuttonAbortActionPerformed

    private void menuRunNextbuttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRunNextbuttonRunActionPerformed
        try {
            String filename = getSelectedExecutor().getFileName();
            if (filename != null) {
                if (context.getState().isProcessing()) {
                    for (String path : new String[]{context.getSetup().getScriptPath(), context.getSetup().getHomePath()}) {
                        if (IO.isSubPath(filename, path)) {
                            filename = IO.getRelativePath(filename, path);
                            break;
                        }
                    }
                    App.getInstance().evalFileNext(new File(filename));
                } else {
                    runScript();
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuRunNextbuttonRunActionPerformed

    private void menuSessionStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionStartActionPerformed
        try {
            if (!context.getSessionManager().isStarted()) {
                String name = getString("Enter the new session name:", "");
                if (name != null) {
                    context.getSessionManager().start(name);
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
            if (context.getSessionManager().isStarted()) {
                SessionPanel sessionPanel = getSessionPanel();
                if (sessionPanel!=null){
                    tabStatus.setSelectedComponent(sessionPanel);
                }
                int id = context.getSessionManager().getCurrentSession();
                String name = context.getSessionManager().getCurrentName();
                String session = name.isBlank() ? String.valueOf(id) : String.valueOf(id) + "-" + name;
                
                //If no session data cancels this session instead of completing
                if (context.getSessionManager().getNumberRuns()==0){
                    String msg = String.format("Do you want to cancel session %s with no runs?", session);
                    if (SwingUtils.showOption(this, "Session", msg , OptionType.YesNo) == OptionResult.Yes) {
                        context.getSessionManager().cancel();                    
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
                    context.getSessionManager().stop();
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
            editor.setFilePermissions(context.getConfig().filePermissionsConfig);
            editor.load(context.getSetup().getSessionMetadataDefinitionFile());
            editor.setReadOnly(context.getRights().denyConfig);
            dlg.setContentPane(editor);
            dlg.setSize(480, 320);
            editor.setTitle("Session Metadata");
            showChildWindow(dlg);            
            if (editor.wasSaved()){
                context.getSessionManager().onMetadataDefinitionChanged();
            }
        } catch (Exception ex) {
            showException(ex);
        }
        
        
    }//GEN-LAST:event_menuSessionsMetadataActionPerformed

    private void menuSessionPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionPauseActionPerformed
        try {
            context.getSessionManager().pause();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSessionPauseActionPerformed

    private void menuSessionResumeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSessionResumeActionPerformed
        try {
            context.getSessionManager().resume();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_menuSessionResumeActionPerformed

    private void menuSessionsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuSessionsStateChanged
        try{                
            boolean enabled = !App.isOffline();
            boolean sessionStarted = context.getSessionManager().isStarted();
            Boolean paused = sessionStarted ? context.getSessionManager().isPaused(): false;     
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
                context.getSessionManager().restart(dlg.getSelectedSession());
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
                int id = context.getSessionManager().create(name, files, null, null);
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAbort;
    private javax.swing.JButton buttonAbout;
    private javax.swing.JButton buttonDebug;
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
    private javax.swing.JMenuItem menuAbort;
    private javax.swing.JMenu menuAddToQueue;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu menuBlock;
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
    private javax.swing.JMenuItem menuLogQuery;
    private javax.swing.JMenuItem menuNew;
    private javax.swing.JMenuItem menuOpen;
    private javax.swing.JMenuItem menuOpenLogFile;
    private javax.swing.JMenu menuOpenRecent;
    private javax.swing.JMenuItem menuPaste;
    private javax.swing.JMenuItem menuPause;
    private javax.swing.JMenu menuPlotWindow;
    private javax.swing.JCheckBoxMenuItem menuPlotWindowDetached;
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
    private javax.swing.JCheckBoxMenuItem menuViewPlotWindow;
    private ch.psi.pshell.swing.OutputPanel outputPanel;
    private ch.psi.pshell.swing.ScanPanel scanPanel;
    private ch.psi.pshell.swing.PlotPanel scanPlot;
    private ch.psi.pshell.swing.ScriptsPanel scriptsPanel;
    private javax.swing.JToolBar.Separator separatorInfo;
    private javax.swing.JToolBar.Separator separatorStopAll;
    private ch.psi.pshell.swing.Shell shell;
    private javax.swing.JSplitPane splitterDoc;
    private javax.swing.JSplitPane splitterHoriz;
    private javax.swing.JSplitPane splitterVert;
    private ch.psi.pshell.ui.StatusBar statusBar;
    private javax.swing.JTabbedPane tabDoc;
    private javax.swing.JTabbedPane tabLeft;
    private javax.swing.JTabbedPane tabPlots;
    private javax.swing.JTabbedPane tabStatus;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables

}
