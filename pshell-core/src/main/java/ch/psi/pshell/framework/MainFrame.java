package ch.psi.pshell.framework;

import ch.psi.pshell.app.AboutDialog;
import ch.psi.pshell.app.AppListener;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.framework.App.ExecutionStage;
import ch.psi.pshell.imaging.FileSource;
import ch.psi.pshell.imaging.DeviceRenderer;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotPanel;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.Statement;
import ch.psi.pshell.scripting.ViewPreference.PlotPreferences;
import ch.psi.pshell.security.User;
import ch.psi.pshell.security.UsersManagerListener;
import ch.psi.pshell.sequencer.InterpreterListener;
import ch.psi.pshell.sequencer.PlotListener;
import ch.psi.pshell.swing.CodeEditor;
import ch.psi.pshell.swing.ConfigDialog;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.Document;
import ch.psi.pshell.swing.DocumentListener;
import ch.psi.pshell.swing.Editor;
import ch.psi.pshell.swing.OutputPanel;
import ch.psi.pshell.swing.PropertiesDialog;
import ch.psi.pshell.swing.ScanPanel;
import ch.psi.pshell.swing.ScriptsPanel;
import ch.psi.pshell.swing.SearchPanel;
import ch.psi.pshell.swing.Shell;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.TextEditor;
import ch.psi.pshell.swing.UpdatablePanel;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.History;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Range;
import ch.psi.pshell.utils.State;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * */
public abstract class MainFrame extends ch.psi.pshell.app.MainFrame{
    static final Logger logger = Logger.getLogger(MainFrame.class.getName());
    public enum ScriptPopupMode {
        None,
        Exception,
        Return
    }
        
    protected String[] imageFileExtensions = new String[]{"png", "bmp", "gif", "tif", "tiff", "jpg", "jpeg"};
    protected String[] textFileExtensions = new String[]{"txt", "csv", "log"};
    protected String[] dataFileExtensions = new String[]{"h5", "hdf5"};
    
    
    public static MainFrame getInstance(){
        return (MainFrame) INSTANCE;
    }
    
    private SwingUtils.CloseableTabListener closableTabListener;
    private History fileHistory;
    private String openedFilesFileName;
    private Properties openedFiles;    
    private ScriptEditor currentScriptEditor;
    private Processor currentProcessor;
    private Processor topLevelProcessor;
    final private NextStagesPanel nextStagesPanel;   
    
    //Access functions    
    public JTabbedPane getDocumentsTab() {return null;}
    public JTabbedPane getPlotsTab() {return null;}
    public JTabbedPane getStatusTab() {return null;}
    public JTabbedPane getLeftTab() {return null;}
    public StatusBar getStatusBar() {return null;}
    public JToolBar getToolBar() {return null;}
    public OutputPanel getOutputPanel() {return null;}
    public ScanPanel getScanPanel(){return null;}
    public DataPanel getDataPanel() {return null;}
    public JMenu getMenuFileNew() {return null;}
    public JMenuBar getMenu() {return null;}
    public Shell getShell() {return null;}
    public ch.psi.pshell.swing.PlotPanel getScanPlot() {return null;}
   

    protected void onPathChange(String pathId) {}
    protected void onUserChange(User user, User former){}
    
    public void updateViewState() {}
    public Object getPreferences(){return null;};
    
    public Object getPreference(String name){
        try{
            Object preferences = getPreferences();
            return preferences.getClass().getField(name).get(preferences);
        } catch (Exception ex){
            return null;
        }
    }    
    
    
    
    protected MainFrame(){        
        setIcon(App.getResourceUrl("IconSmall.png"));
        
        closableTabListener = (JTabbedPane tabbedPane, int index) -> {
            try {
                if (tabbedPane.getComponentAt(index) instanceof ScriptEditor scriptEditor) {
                    if (!(tabbedPane.getSelectedComponent() == scriptEditor)) {
                        tabbedPane.setSelectedComponent(scriptEditor);
                    }
                    return scriptEditor.getTextEditor().checkChangeOnClose();
                } else if (tabbedPane.getComponentAt(index) instanceof Processor processor) {
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
        
        fileHistory = new History(getPersistencePath() + "/FileHistory.dat", 10, true);
        openedFiles = new Properties();
        openedFilesFileName = getPersistencePath() + "/OpenedFiles.dat";
        
        try (FileInputStream in = new FileInputStream(openedFilesFileName)) {
            openedFiles.load(in);
        } catch (Throwable ex) {
            logger.log(Level.FINE, null, ex);
        }
        nextStagesPanel = new NextStagesPanel();
    }    
    
   protected void checkHiddenComponents(){
        for (String name : Setup.getHiddenComponents()) {
            Component component = SwingUtils.getComponentByName(this, name);
            if (component != null) {
                component.setVisible(false);
            }
        }
   }
    
    @Override
    protected void afterConstruction(){
        checkHiddenComponents();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher((KeyEvent ke) -> {
            switch (ke.getID()) {
                case KeyEvent.KEY_PRESSED -> ctrlPressed = (ke.getKeyCode() == KeyEvent.VK_CONTROL);
                case KeyEvent.KEY_RELEASED -> ctrlPressed = false;
            }
            return false;
        });           
        super.afterConstruction();
    }   

    @Override
    public boolean isHidden() {
        return Setup.isHidden();
    }

    @Override
    public boolean isPersisted() {
        return Context.getApp().isViewPersisted();
    }    
    
    private final HashMap<Scan, String> plotTitles = new HashMap<>();
    /**
     * Called once in the first time the frame is shown
     */    
    @Override
    protected void onOpen() {
        Context.getInterpreter().addScanListener(new ScanListener() {
            @Override
            public void onScanStarted(Scan scan, final String plotTitle) {
                startScanPlot(scan, plotTitle);
                updateViewState();
            }

            @Override
            public void onNewRecord(Scan scan, ScanRecord record) {
                ch.psi.pshell.swing.PlotPanel plottingPanel = null;
                synchronized (plotTitles) {
                    plottingPanel = getPlotPanel(plotTitles.get(scan), false);
                }                
                if (plottingPanel != null) {
                    if (scan.isPlottingActive()) {
                        plottingPanel.triggerOnNewRecord(scan, record);
                    }
                }
            }

            @Override
            public void onScanEnded(Scan scan, Exception ex) {
                ch.psi.pshell.swing.PlotPanel plottingPanel = null;
                synchronized (plotTitles) {
                    plottingPanel = getPlotPanel(plotTitles.get(scan), false);
                    plotTitles.remove(scan);
                }
                if (scan.isPlottingActive()) {
                    if (plottingPanel != null) {
                        plottingPanel.triggerScanEnded(scan, ex);
                    }
                }
                updateViewState();
                //Request repaint of current file tree in Data viewer to update scan contents
                if (getDataPanel()!=null){  
                    getDataPanel().onScanEnded();                
                }
            }
        });

        if (getDataPanel()!=null){            
            getDataPanel().setListener(dataPanelListener);
        }
        
        if (getScanPlot()!=null){            
            getScanPlot().initialize();
        }
    }
    
    public void startScanPlot(Scan scan, String title){
        synchronized (plotTitles) {
            plotTitles.put(scan, title);
            if (!Context.getExecutionPars().isScanDisplayed(scan)){
                return;
            }
            ch.psi.pshell.swing.PlotPanel plottingPanel = getPlotPanel(plotTitles.get(scan));
            scan.setPlottingActive(!isScanPlotDisabled());
            plottingPanel.setPreferences(Context.getExecutionPars().getPlotPreferences());
            if (scan.isPlottingActive()) {
                plottingPanel.triggerScanStarted(scan, title);
                JTabbedPane tabPlots = getPlotsTab();
                if (tabPlots != null){                
                    if (plottingPanel.isDisplayable() && SwingUtils.containsComponent(tabPlots, plottingPanel)) {
                        SwingUtilities.invokeLater(() -> {
                            tabPlots.setSelectedComponent(plottingPanel);
                        });
                    }
                }
            }
        }  
        updateViewState();
    }
    
    public boolean isScanPlotDisabled() {
        return !Context.getApp().isScanPlottingActive();
    }
    

    public Scan[] getCurrentScans() {
        return plotTitles.keySet().toArray(new Scan[0]);
    }

    public boolean hasOngoingScan() {
        return !plotTitles.isEmpty();
    }
    
    @Override
    protected void onLafChange() {        
        JToolBar toolBar = getToolBar();
        if (toolBar != null){
            for (JButton b : SwingUtils.getComponentsByType(toolBar, JButton.class)) {
                try{
                    if (MainFrame.isDark()) {
                        b.setIcon(new ImageIcon(App.getResourceUrl("dark/" + new File(((JButton) b).getIcon().toString()).getName())));
                    } else {
                        b.setIcon(new ImageIcon(App.getResourceUrl(new File(((JButton) b).getIcon().toString()).getName())));
                    }
                } catch (Exception ex){                    
                }
            }        
        }
    }
    
    @Override
    protected void onCreate() {
        if (Context.getInstanceName()!=null) {
            setTitle(App.getApplicationTitle() + " [" + Context.getInstanceName()+ "]");
        } else if (Setup.isPlotOnly() && (App.getFileArg()!=null)){
            setTitle(App.getApplicationTitle() + " - " + App.getFileArg().toString());
        }
        if (isPersisted()) {
            restoreOpenedFiles();
        }
        
        Context.getInterpreter().addListener(new InterpreterListener() {
            @Override
            public void onStateChanged(State state, State former) {
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
                if ((state == State.Busy) && !former.isProcessing()) {
                    synchronized (plotTitles) {
                        plotTitles.clear();
                    }
                }
                
                for (Processor p : getProcessors()) {
                    p.onStateChanged(state, former);
                }
                updateViewState();
            }
            
        @Override
        public void onNewStatement(final Statement statement) {
            updateViewState();
            if (currentScriptEditor != null) {
                currentScriptEditor.onExecutingStatement(statement);
            }
        }

        @Override
        public void onExecutingStatement(Statement statement) {
            updateViewState();
        }

        @Override
        public void onExecutedStatement(Statement statement) {
            updateViewState();
        }
            
        });
        if (Context.hasUsersManager()){
            Context.getUsersManager().addListener(new UsersManagerListener() {
                @Override
                public void onUserChange(User user, User former) {
                    MainFrame.this.onUserChange(user, former);
                    if (former != null) {
                        for (String token : new String[]{Setup.TOKEN_LOGS, Setup.TOKEN_SESSIONS, Setup.TOKEN_DATA, Setup.TOKEN_IMAGES, Setup.TOKEN_SCRIPT, Setup.TOKEN_CONTEXT}){
                            if (Setup.getField(token).contains(Setup.TOKEN_USER)) {
                                onPathChange(token);
                            }
                        }
                        logger.log(Level.INFO, "User: {0}", user.toString());

                    }
                }
            });
        }    

        for (Processor processor : Processor.getServiceProviders()) {
            addProcessorComponents(processor);
        }
        
        Context.getApp().addListener(new AppListener() {
            @Override
            public boolean canExit(Object source) {
                JTabbedPane tabDoc = getDocumentsTab();
                if (tabDoc!=null){                    
                    for (int i = 0; i < tabDoc.getTabCount(); i++) {
                        if (!closableTabListener.canClose(tabDoc, i)) {
                            return false;
                        }
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
    
    @Override
    protected void onTimer() {
        updateSelectedPanels();
        Processor processor = getSelectedProcessor();
        if ((getDocumentsTab()!=null) && (processor != null) && (processor.getPanel().isEnabled())) {
            updateProcessorTabTitle(processor,  getDocumentsTab().getSelectedIndex());
        }
    }
   
    
    int getIndexLastScriptPanel() {
        JTabbedPane tabStatus = getStatusTab();
        for (int i = tabStatus.getComponentCount()-1; i >=0 ; i--) {
            if (tabStatus.getComponentAt(i) instanceof  ScriptsPanel) {                
                return i;
            }
        }
        return -1;
    }    
    void addProcessorComponents(Processor processor) {
        JMenu menuFile = getMenuFileNew();   
        if ((menuFile!=null) && processor.createMenuNew()) {       
            boolean toplevel = (menuFile.getParent() == getMenu());
            JMenuItem item = new JMenuItem( toplevel ? "New" : processor.getType());
            item.addActionListener((java.awt.event.ActionEvent evt) -> {
                try {
                    openProcessor(processor.getClass(), null);
                } catch (Exception ex) {
                    showException(ex);
                }
            });
            if (toplevel){
                menuFile.add(item,0);
            } else {
                menuFile.add(item);
            }
        }
        
        JTabbedPane tabStatus = getStatusTab();
        if (tabStatus != null){        
            if (processor.createFilePanel()) {
                ScriptsPanel pn = new ScriptsPanel();
                int index = getIndexLastScriptPanel(); //SwingUtils.getComponentIndex(tabStatus, scriptsPanel);     
                index =  (index <0 ) ? tabStatus.getTabCount() : index +1;
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
    }

    void removeProcessorComponents(Processor processor) {       
        JMenu menuFile = getMenuFileNew();
        if (menuFile!=null){
            if (processor.createMenuNew() && (menuFile!=null)) {            
                for (int i = menuFile.getMenuComponents().length - 1; i > 0; i--) {
                    Component c = menuFile.getMenuComponents()[i];
                    if (c instanceof JMenuItem jMenuItem) {
                        if (jMenuItem.getText().equals(processor.getType())) {
                            menuFile.remove(jMenuItem);
                            break;
                        }
                    }
                }
            }
        }
        JTabbedPane tabStatus = getStatusTab();
        if (tabStatus != null){        
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
    }
    
    /**
     * Called when window is being closed
     */
    @Override
    protected void onClosing() {
        Context.getApp().exit(this);
    }
    
    public void onTaskFinished(Task task) {
        if (!Context.getState().isProcessing() && (currentScriptEditor != null)) {
            currentScriptEditor.stopExecution();
            currentScriptEditor = null;
        }
        for (Processor p : getProcessors()) {
            p.onTaskFinished(task);
        }
    }
    
    
    protected void updateSelectedPanels() {        
        updateSelectedPanel(getStatusTab());
        updateSelectedPanel(getLeftTab());
        updateSelectedPanel(getPlotsTab());
        updateSelectedPanel(getDocumentsTab());        
    }
    
    public void updateSelectedPanel(JTabbedPane tab) {
        try {
            if (tab!=null){
                Component panel = tab.getSelectedComponent();
                if (panel instanceof UpdatablePanel updatablePanel) {
                    updatablePanel.update();
                }
            }
        } catch (Exception ex) {
        }
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

    public boolean isShowingExecutor() {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc==null){  
            return false;
        }        
        Component selectedDocument = tabDoc.getSelectedComponent();
        return (selectedDocument != null) && (selectedDocument instanceof Executor);
    }

    public boolean isShowingProcessor() {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc==null){  
            return false;
        }        
        Component selectedDocument = tabDoc.getSelectedComponent();
        return (selectedDocument != null) && (selectedDocument instanceof Processor);
    }

    public boolean isShowingScriptEditor() {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc==null){  
            return false;
        }        
        Component selectedDocument = tabDoc.getSelectedComponent();
        return (selectedDocument != null) && (selectedDocument instanceof ScriptEditor);
    }

    public boolean isShowingQueueProcessor() {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc==null){  
            return false;
        }        
        Component selectedDocument = tabDoc.getSelectedComponent();
        return (selectedDocument != null) && (selectedDocument instanceof QueueProcessor);
    }


    public void debugScript(final boolean pauseOnStart) throws Exception {
        try {
            if (Context.getInterpreter().isPaused()) {
                Context.getInterpreter().resume();
                return;
            }
            Context.getInterpreter().assertState(State.Ready);
            currentScriptEditor = getSelectedEditor();
            if (currentScriptEditor != null) {
                final Statement[] statements = currentScriptEditor.parse();
                currentScriptEditor.startExecution();
                Context.getApp().startTask(new ScriptExecution(currentScriptEditor.getFileName(), statements, null, pauseOnStart, true));
            }
        } catch (ScriptException ex) {
            if (getOutputPanel()!=null){
                if (getOutputPanel().isDisplayable()) {
                    getOutputPanel().putError(ex.getMessage());
                }
            }
            showMessage("Script Parsing Error", ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        }
    }

    public void runScript() throws Exception {
        //Context.getInterpreter().assertState(State.Ready);
        Context.getState().assertIs(State.Ready);
        currentScriptEditor = getSelectedEditor();
        if (currentScriptEditor != null) {
            if (currentScriptEditor.hasChanged()) {
                if (showOption("Save", "Document has changed. Do you want to save it?", SwingUtils.OptionType.YesNo) != SwingUtils.OptionResult.Yes) {
                    return;
                }
                save();
            }
            if (currentScriptEditor.getFileName() != null) {
                currentScriptEditor.startExecution();
                Context.getApp().startTask(new ScriptExecution(currentScriptEditor.getFileName(), null, null, false, false));
            }
        } else {
            setCurrentProcessor(getSelectedProcessor(), true);
            if (currentProcessor != null) {
                logger.log(Level.INFO, "Run processor: {0} file: {1}", new Object[]{currentProcessor.getType(), currentProcessor.getFileName()});
                currentProcessor.execute();
            }
        }
    }
    
    public void execCommandLineFile() throws Exception {
        App.getInstance().execCommandLineFile(true);
    }
    
    public void run() throws Exception{
        if (Context.getState()==State.Paused){
            Context.getInterpreter().resume();
            Processor runningProcessor = getRunningProcessor();
            if ((runningProcessor != null) && runningProcessor.canPause()){
                runningProcessor.resume();
            }
        } else if (Setup.isPlotOnly()) {
            Context.getState().assertIs(State.Ready);
            //File file = App.getFileArg();
            //if (file != null) {
            //    Context.getInterpreter().evalFileAsync(file.toString(), App.getInterpreterArgs());
            //}
            execCommandLineFile();
        } else {
            runScript();
        }                
        updateViewState();
    }
    
    public void pause() throws InterruptedException{
        Context.getInterpreter().pause();
        Processor runningProcessor = getRunningProcessor();
        if (runningProcessor != null) {
            runningProcessor.pause();
        } 
        updateViewState();
    }
    
    public void step() throws Exception{
        Processor runningProcessor = getRunningProcessor();
        if (runningProcessor != null) {
            runningProcessor.step();
        } else {
            if (Context.getState().isReady()) {
                debugScript(true);
            } else {
                Context.getInterpreter().step();
            }
        }
        updateViewState();
    }    
    
    public void abort() throws InterruptedException{
        if (currentProcessor != null) {
            currentProcessor.abort();
            currentProcessor = null;
        }
        if (topLevelProcessor != null) {
            topLevelProcessor.abort();
            topLevelProcessor = null;
        }
        Context.abort();
        updateViewState();
    }
    

    public void setCurrentProcessor(Processor processor, boolean topLevel) {
        if (topLevel) {
            topLevelProcessor = processor;
        }
        currentProcessor = processor;
    }    
    
    public void saveContext() {
        if (isPersisted()) {
            saveOpenedFiles();
        }
    }    
    
    
    //File history and Opened files
    
    public History getFileHistory (){
        return fileHistory;
     }
    
    void saveOpenedFiles() {
        openedFiles.clear();
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){  
            for (int i = 0; i < tabDoc.getTabCount(); i++) {
                if (tabDoc.getComponentAt(i) instanceof ScriptEditor scriptEditor) {
                    ScriptEditor editor = scriptEditor;
                    if (editor.getFileName() != null) {
                        openedFiles.setProperty(editor.getFileName(), "");
                    }
                } else if (tabDoc.getComponentAt(i) instanceof Processor processor) {             
                    if (processor.getFileName() != null) {
                        openedFiles.setProperty(processor.getFileName(), "");
                    }
                }
            }
        };
        try (FileOutputStream out = new FileOutputStream(openedFilesFileName)) {
            openedFiles.store(out, null);
            IO.setFilePermissions(openedFilesFileName, Context.getConfigFilePermissions());
        } catch (Exception ex) {
            logger.log(Level.FINE, null, ex);
        }
    }

    protected void restoreOpenedFiles(){        
        for (String file : openedFiles.stringPropertyNames()) {
            try {
                if (isProcessorFile(file)){
                    if (isScriptOpen(file)){
                        //Opened before plugin was loaded
                        closeFile(file);
                    }
                }
                if (!isScriptOrProcessorOpen(file)){
                    openScriptOrProcessor(file);
                }
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        }
    }    
            
    //TODO: This flag is used to re-inject detached windows to original tabs.
    //Better doing with drad/drop instead of double/click + ctrl+close
    boolean ctrlPressed;
    
    
    //Component access utilities
    boolean sameFile(String f1, String f2) throws IOException{
        if ((f1==null) || (f2==null)){
            return false;
        }
        return(new File(f1).getCanonicalFile()).equals((new File(f2).getCanonicalFile()));
    }
    
            
    HashMap<String, Component> detachedComponents = new HashMap<>();
    
    public HashMap<String, Component> getDetachedComponents(){
        return detachedComponents;
    }
    
    public <T extends Component> HashMap<String, T> getDetachedComponents(Class<T> type){
        HashMap<String, T> ret = new HashMap<>();
        for (String name: detachedComponents.keySet()) {
            Component component = detachedComponents.get(name);
             if ((type == null) || type.isAssignableFrom(component.getClass())) {
                ret.put(name, (T) component);
            }
        }
        return ret;        
    }
        
    public HashMap<String, PlotPanel> getDetachedPlots(){
        return getDetachedComponents(PlotPanel.class);
    }
        
    public void closePlots(){
        for (PlotPanel panel : getPlotPanels()) {
            JTabbedPane tabPlots = getPlotsTab();
            if (tabPlots != null){
                if (tabPlots.indexOfComponent(panel) >= 0) {
                    tabPlots.remove(panel);
                } 
            }
            if (detachedComponents.containsValue(panel)) {
                detachedComponents.values().removeIf(val -> val == panel);
                panel.getTopLevelAncestor().setVisible(false);
            }
        }
        if (getScanPlot()!=null){
            getScanPlot().clear();
        }
    }
    
    public void closeSearchPanels() {
        JTabbedPane tabStatus = getStatusTab();
        if (tabStatus != null){        
            for (SearchPanel panel : getSearchPanels()) {
                tabStatus.remove(panel);
            }
        }
    }
    
    
    public void closeDocuments(){
        for ( JPanel panel : getDocumentPanels()) {                
            JTabbedPane tabDoc = getDocumentsTab();
            if (tabDoc!=null){                    
                if (tabDoc.indexOfComponent(panel) >= 0) {
                    int index = tabDoc.indexOfComponent(panel);
                    if (SwingUtils.isTabClosable(tabDoc, index)){
                        tabDoc.remove(panel);
                    }
                }
            }
            if (getDetachedComponents().containsValue(panel)) {
                getDetachedComponents().values().removeIf(val -> val == panel);
                panel.getTopLevelAncestor().setVisible(false);
            }
        }        
    }
    
    
    public List<QueueProcessor> getQueues() {
        ArrayList<QueueProcessor> ret = new ArrayList();
        for (Processor processor : getProcessors()) {
            if (processor instanceof QueueProcessor queueProcessor) {
                ret.add(queueProcessor);
            }
        }
        return ret;
    }

    public List<ch.psi.pshell.swing.PlotPanel> getPlotPanels() {
        ArrayList<ch.psi.pshell.swing.PlotPanel> ret = new ArrayList();
        JTabbedPane tabPlots = getPlotsTab();
        if (tabPlots != null){
            for (int i = 0; i < tabPlots.getTabCount(); i++) {
                Component c = tabPlots.getComponentAt(i);
                if (c instanceof ch.psi.pshell.swing.PlotPanel plotPanel) {
                    if (c!=getScanPlot()){
                        ret.add(plotPanel);
                    }
                }
            }
            var detachedPlots = getDetachedPlots();
            for (String key : detachedPlots.keySet()) {
                ret.add((ch.psi.pshell.swing.PlotPanel) detachedPlots.get(key));
            }
        }
        return ret;
    }

    public List<SearchPanel> getSearchPanels() {
        ArrayList<SearchPanel> ret = new ArrayList();
        JTabbedPane tabStatus = getStatusTab();
        if (tabStatus != null){
            for (int i = 0; i < tabStatus.getTabCount(); i++) {
                Component c = tabStatus.getComponentAt(i);
                if (c instanceof SearchPanel searchPanel) {
                    ret.add(searchPanel);
                }
            }
        }
        return ret;
    }
    
    public List<ScriptEditor> getScriptEditors() {
        return getPanels(ScriptEditor.class);
    }

    public List<Processor> getProcessors() {
        //return getPanels(Processor.class);
        ArrayList<Processor> ret = new ArrayList();
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){                    
            for (int i = 0; i < tabDoc.getTabCount(); i++) {
                Component c = tabDoc.getComponentAt(i);
                if (c instanceof Processor p) {
                    ret.add(p);
                }
            }
          for (Component c: detachedComponents.values()) {
                if (c instanceof Processor p) {
                    ret.add(p);
                }
          }
        }
        return ret;         
    }
    
    public List<String> getPlotTitles() {
        List<String> ret = new ArrayList<>();
        ret.add(PlotListener.DEFAULT_PLOT_TITLE);
        for (int i = 0; i < getPlotsTab().getTabCount(); i++) {
            if (getPlotsTab().getComponentAt(i) != getScanPlot()) {
                if (getPlotsTab().getComponentAt(i) instanceof ch.psi.pshell.swing.PlotPanel) {
                    ret.add(getPlotsTab().getTitleAt(i));
                }
            }
        }
        for (String key : getDetachedPlots().keySet()) {
             ret.add(key);
        }
        return ret;
    }
   
    
    public List<DataPanel> getDataFilePanels() {
        return getPanels(DataPanel.class);
    }    
    
    public List<Editor> getEditors() {
        return getPanels(Editor.class);
    }

    public List<DeviceRenderer> getRenderers() {
        return getPanels(DeviceRenderer.class);
    }

    public List<JPanel> getDocumentPanels(){
        List<JPanel> ret = new ArrayList<>();
        ret.addAll(getEditors());
        ret.addAll(getRenderers());
        ret.addAll(getDataFilePanels());
        ret.addAll(getScriptEditors());
        for (Processor p : getProcessors()) {
            ret.add(p.getPanel());
        }        
        return ret;
    }
            
    public <T extends Component> List<T> getPanels(Class<T> type) {
        ArrayList<T> ret = new ArrayList();
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){                    
            for (int i = 0; i < tabDoc.getTabCount(); i++) {
                Component c = tabDoc.getComponentAt(i);
                if (type.isAssignableFrom(c.getClass())) {
                    ret.add((T) c);
                }
            }
            var detached = getDetachedComponents(type);
            ret.addAll(detached.values());
        }
        return ret; 
    }
    
    public void  selectPanel(JComponent panel){
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){        
            if (tabDoc.indexOfComponent(panel) >= 0) {
                tabDoc.setSelectedComponent(panel);
            } else if (getDetachedComponents().containsValue(panel)) {
                panel.getTopLevelAncestor().requestFocus();
            }
        }
    }
    

    public ScriptEditor getSelectedEditor() {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){
            if (tabDoc.getSelectedComponent() instanceof ScriptEditor scriptEditor) {
                return scriptEditor;
            }
        }
        return null;
    }

    public Processor getSelectedProcessor() {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){        
            if (tabDoc.getSelectedComponent() instanceof Processor processor) {
                return processor;
            }
        }
        return null;
    }

    public Executor getSelectedExecutor() {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){
            if (tabDoc.getSelectedComponent() instanceof Executor executor) {
                return executor;
            }
        }
        return null;
    }

    public QueueProcessor getSelectedQueueProcessor() {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){        
            if (tabDoc.getSelectedComponent() instanceof QueueProcessor queueProcessor) {
                return queueProcessor;
            }
        }
        return null;
    }
    
    public ch.psi.pshell.swing.PlotPanel getPlotPanel(String plotTitle) {
        return getPlotPanel(plotTitle, true);
    }
    
    public ch.psi.pshell.swing.PlotPanel getPlotPanel(String plotTitle, boolean create) {
        if ((plotTitle == null) || (plotTitle.isEmpty()) || (plotTitle.equals(PlotListener.DEFAULT_PLOT_TITLE))) {
            return getScanPlot();
        }        
        JTabbedPane tabPlots = getPlotsTab ();
        if (tabPlots != null){
            for (int i = 0; i < tabPlots.getTabCount(); i++) {
                if (tabPlots.getTitleAt(i).equals(plotTitle)) {
                    if (tabPlots.getComponentAt(i) instanceof ch.psi.pshell.swing.PlotPanel plotPanel) {
                        return plotPanel;
                    }
                }
            }
            var detachedPlots = getDetachedPlots();
            for (String key : detachedPlots.keySet()) {
                if (key.endsWith(plotTitle)) {
                    return (ch.psi.pshell.swing.PlotPanel) detachedPlots.get(key);
                }
            }

            if (create) {
                ch.psi.pshell.swing.PlotPanel plotPanel = new ch.psi.pshell.swing.PlotPanel();
                plotPanel.initialize();
                plotPanel.setPlotTitle(plotTitle);
                plotPanel.clear();
                tabPlots.add(plotTitle, plotPanel);
                tabPlots.setSelectedComponent(plotPanel);

                int index = tabPlots.getTabCount() - 1;
                SwingUtils.setTabClosable(tabPlots, index, (JTabbedPane tabbedPane, int tabIndex) -> {
                    Context.getInterpreter().removePlotContext(tabbedPane.getTitleAt(tabIndex));
                    return true;
                });
                setTabDetachable(tabPlots, index);
                return plotPanel;                
            }
        }        
        return null;
    }
    
    boolean canDetach(Component c) {
        if (c == currentScriptEditor) {
            return !currentScriptEditor.isExecuting();
        }
        return true;
    }

    public void setTabDetachable(final JTabbedPane tabbedPane, int index) {
        Component component = tabbedPane.getComponentAt(index);
        SwingUtils.CloseButtonTabComponent tabComponent = (SwingUtils.CloseButtonTabComponent) tabbedPane.getTabComponentAt(index);

        final MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if ((e.getClickCount() == 2) && (tabbedPane.getSelectedComponent() == component) && canDetach(component)) {
                    int index = tabbedPane.indexOfComponent(component);
                    if (index < 0) {
                        return;
                    }
                    String title = tabbedPane.getTitleAt(index);
                    tabbedPane.remove(component);
                    JDialog dlg = new JDialog(MainFrame.this, title, false);
                    dlg.setSize(component.getSize());
                    dlg.add(component);
                    dlg.addWindowListener(null);
                    dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    showChildWindow(dlg);
                    detachedComponents.put(title, component);
                    dlg.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            detachedComponents.values().removeIf(val -> val == component);
                            if (ctrlPressed) {
                                tabbedPane.add(dlg.getTitle(), component);
                                int index = tabbedPane.getTabCount() - 1;
                                SwingUtils.setTabClosable(tabbedPane, index, closableTabListener);
                                setTabDetachable(tabbedPane, index);
                                tabbedPane.setSelectedIndex(index);
                                if (component instanceof ScriptEditor scriptEditor) {
                                    updateScriptEditorTabTitle(scriptEditor, index);
                                } else if (component instanceof Processor processor) {
                                    updateProcessorTabTitle(processor, index);
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
            ((SwingUtils.CloseButtonTabComponent) tabbedPane.getTabComponentAt(index)).getLabel().addMouseListener(listener);
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

    
    public ScriptPopupMode getScriptPopupMode() {
        return ScriptPopupMode.Exception;
    }

    public void showAboutDialog() {
        AboutDialog aboutDialog = new AboutDialog(this, true);
        aboutDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        showChildWindow(aboutDialog);
    }
    
    
    public static PropertiesDialog showSettingsEditor(Window parent, boolean modal, boolean readOnly) {
        return showPropertiesEditor("Settings", parent, Context.getSettingsFile(), modal, readOnly);
    }

    public static PropertiesDialog showPropertiesEditor(String title, Window parent, String fileName, boolean modal, boolean readOnly) {
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
                        IO.setFilePermissions(fileName, Context.getConfigFilePermissions());                
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

    public static ConfigDialog showConfigEditor(String title, Window parent, Config cfg, boolean modal, boolean readOnly) {
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
    
    final DocumentListener scriptChangeListener = new DocumentListener() {
        @Override
        public void onDocumentChanged(Document doc) {

            for (int i = 0; i < getDocumentsTab().getTabCount(); i++) {
                Component c = getDocumentsTab().getComponentAt(i);
                if (c instanceof ScriptEditor scriptEditor) {
                    ScriptEditor editor = scriptEditor;
                    if (doc == editor.getDocument()) {
                        updateScriptEditorTabTitle(editor, i);
                        return;
                    }
                }
            }
        }
        @Override
        public void onDocumentSaved(Document doc){            
        }
    };

    void updateScriptEditorTabTitle(ScriptEditor editor, int index) {
        try {
            String title = editor.getScriptName() + (editor.hasChanged() ? "*" : "");
            if (!title.equals(getDocumentsTab().getTitleAt(index))) {
                getDocumentsTab().setTitleAt(index, title);
                SwingUtils.CloseButtonTabComponent tabComponent = (SwingUtils.CloseButtonTabComponent) getDocumentsTab().getTabComponentAt(index);
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
                    getDocumentsTab().setTitleAt(index, new File(processor.getFileName()).getName() + "*");
                    SwingUtils.CloseButtonTabComponent tabComponent = (SwingUtils.CloseButtonTabComponent) getDocumentsTab().getTabComponentAt(index);
                    tabComponent.updateUI();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }
    
    public static Font getDefaultEditorFont(){
        return SwingUtils.hasFont("Lucida Console")
        ? new Font("Lucida Console", 0, 11)
        : new Font(Font.MONOSPACED, 0, 13);            
    }

    public static final int DEFAULT_CONTENT_WIDTH = 2000;

    public record ScriptEditorPreferences (
        boolean simple,
        boolean hideLineNumbers,
        boolean hideContextMenu, 
        int tabSize, 
        Font font,
        Color background,
        Color foreground,
        int contentWidth
    ){}   
    
    ScriptEditorPreferences scriptEditorPreferences = new ScriptEditorPreferences(false, false, false, 4, null, null, null, 0);
    
    public ScriptEditorPreferences getScriptEditorPreferences(){
        return scriptEditorPreferences;
    }
    
    public void setScriptEditorPreferences(ScriptEditorPreferences prefs){
        scriptEditorPreferences = prefs;
    }
       

    public ScriptEditor newScriptEditor(String file) throws IOException {
        ScriptEditorPreferences preferences = getScriptEditorPreferences();
        final ScriptEditor editor = new ScriptEditor(!preferences.simple, !preferences.hideLineNumbers, !preferences.hideContextMenu);
        editor.addDocumentChangeListener(scriptChangeListener);
        editor.getTextEditor().setFilePermissions(Context.getScriptFilePermissions());
        if (file != null) {
            editor.load(file);
        }
        openComponent(editor.getScriptName(), editor);
        formatScriptEditor(editor);
        return editor;
    }
    
    public void formatScriptEditor(ScriptEditor editor){
        ScriptEditorPreferences preferences = getScriptEditorPreferences();
        editor.setTabSize(preferences.tabSize);
        editor.setTextPaneFont((preferences.font == null) ? getDefaultEditorFont() : preferences.font);
        editor.setEditorForeground((preferences.font == null) ? CodeEditor.getForegroundColor() : preferences.foreground);
        editor.setEditorBackground((preferences.background == null) ? CodeEditor.getBackgroundColor() : preferences.background);
        editor.setReadOnly(Context.getRights().denyEdit);        

        //TODO: Why is not working if calling sync?
        SwingUtilities.invokeLater(() -> {
            editor.setContentWidth((preferences.contentWidth <= 0) ? DEFAULT_CONTENT_WIDTH : preferences.contentWidth);
        });
    }    
    


    public boolean isScriptOpen(String file) throws IOException {
        for (ScriptEditor se : getScriptEditors()) {
            if ((se.getFileName() != null) && sameFile(file, se.getFileName())){
                return true;
            }
        }
        return false;
    }        

    public void closeFile(String file) throws IOException {
        JTabbedPane tabDoc = getDocumentsTab();
        if (tabDoc!=null){              
            for (Editor se : getEditors()) {
                if ((se.getFileName() != null) && sameFile(file, se.getFileName())){
                    if (tabDoc.indexOfComponent(se) >= 0) {
                        tabDoc.remove(se);
                    }
                }
            }  
            for (ScriptEditor se : getScriptEditors()) {
                if ((se.getFileName() != null) && sameFile(file, se.getFileName())){
                    if (tabDoc.indexOfComponent(se) >= 0) {
                        tabDoc.remove(se);
                    }
                }
            }          
            for (Processor p : getProcessors()) {
                try {
                    if (sameFile(file, p.getFileName())){
                        if (tabDoc.indexOfComponent(p.getPanel()) >= 0) {
                            tabDoc.remove(p.getPanel());
                        }
                    }
                } catch (Exception ex) {
                }
            }
        }
    }        

    public ScriptEditor openScript(String file) throws IOException {
        if (file == null) {
            return null;
        }

        for (ScriptEditor se : getScriptEditors()) {
            if (se.getFileName() != null) {
                if (sameFile(file, se.getFileName())){
                    selectPanel(se);
                    return se;
                }
            }
        }
        try{
            final ScriptEditor editor = newScriptEditor(file);
            fileHistory.put(file);
            saveContext();
            return editor;
        } catch (Exception ex){
            fileHistory.remove(file);
            throw ex;
        }
    }

    /**
     * Non-script files go to the document tab but is not added file to history
     */
    public void openComponent(String title, Component c) {
        openComponent(title, c, getDocumentsTab());
    }

    public void openComponent(String title, Component c, JTabbedPane pane) {
        pane.add(title, c);
        int index = pane.getTabCount() - 1;
        SwingUtils.setTabClosable(pane, index, closableTabListener);
        setTabDetachable(pane, index);
        pane.setSelectedIndex(index);
        updateViewState();
    }

    public Editor openTextFile(String file) throws IOException {
        if (file == null) {
            return null;
        }
        for (Editor ed : getEditors()) {
            if (ed.getFileName() != null) {
               if (sameFile(file, ed.getFileName())){
                   if (ed.changedOnDisk()){
                       closeFile(file);
                   } else {
                        selectPanel(ed);
                        return ed;
                   }
                }
            }
        }
        TextEditor editor = new TextEditor();
        editor.setFilePermissions(Context.getScriptFilePermissions());
        openComponent(new File(file).getName(), editor);
        editor.load(file);
        return editor;
    }

    public DataPanel openDataFile(String file) throws Exception {
        if (file == null) {
            return null;
        }

        for (DataPanel dp : getDataFilePanels()) {
            if (dp.getFileName() != null) {
               if (sameFile(file, dp.getFileName())){
                   
                    selectPanel(dp);
                    dp.load(file);
                    return dp;
                }
            }
        }
        DataPanel panel = new DataPanel();
        openComponent(new File(file).getName(), panel);
        panel.load(file);
        panel.setListener(dataPanelListener);
        return panel;
    }
    
    public Editor openEditor(Editor editor) throws IOException {
        return openEditor(null, editor);
    }
    
    public Editor openEditor(String title, Editor editor) throws IOException {
        String file = editor.getFileName();
        if (file!=null){
            for (Editor ed : getEditors()) {
                if (ed.getFileName() != null) {
                   if  ((ed.getClass() == editor.getClass()) && (sameFile(file, ed.getFileName()))){
                       if (ed.changedOnDisk()){
                           closeFile(file);
                       } else {
                           selectPanel(ed);
                           return ed;
                       }
                    }
                }
            }
        }
        editor.setFilePermissions(Context.getScriptFilePermissions());
        if (title==null){
            title = ((file!=null)&&(!file.isBlank())) ? new File(file).getName() : "Unknown";
        }
        openComponent(title, editor);
        editor.load(file);
        return editor;
    }
    
    
    public DataPanel showDataFileWindow(String fileName, String path) throws Exception {
        if (fileName == null) {
            return null;
        }        
        File file = fileName.startsWith("/") ? new File(fileName) : Paths.get(Setup.getDataPath(), fileName).toFile();
        if (!file.exists()){
            return null;
        }
        DataPanel panel = new DataPanel();
        showDialog(fileName, new Dimension(1000, 600), panel);
        panel.load(file.toString());
        panel.setListener(dataPanelListener);        
        if ((path!=null) && (!path.isBlank())){
            panel.selectDataPath(path);
        }        
        return panel;
    }
    

    public DeviceRenderer openImageFile(String file) throws IOException, InterruptedException {
        if (file == null) {
            return null;
        }
        
        for (DeviceRenderer renderer : getRenderers()) {
            try{
                String filename = ((FileSource)renderer.getDevice()).getUrl();
                if (filename != null) {
                    if (sameFile(file, filename)){
                        selectPanel(renderer);
                        return renderer;
                    }
                }
            } catch (Exception ex){                
            }
        }        
        DeviceRenderer renderer = new DeviceRenderer();
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

    public boolean isProcessorOpen(String file) {    
        for (Processor p : getProcessors()) {
            try {
                if (sameFile(file, p.getFileName())){
                    return true;
                }
            } catch (Exception ex) {
            }
        }
        return false;
    }
    
    public <T extends Processor> T openProcessor(Class<T> cls, String file) throws IOException, InstantiationException, IllegalAccessException {
        if (file != null) {
            for (Processor p : getProcessors()) {
                if ((p.getClass().isAssignableFrom(cls)) && p.getFileName() != null) {
                    try {
                        if (sameFile(file, p.getFileName())){
                            selectPanel(p.getPanel());
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
                    getDocumentsTab().setSelectedComponent(processor.getPanel());
                }
            }
        } else {
            try{
                processor = cls.getDeclaredConstructor().newInstance();
                if (file != null) {
                    file = processor.resolveFile(file);
                    processor.open(file);
                    openComponent(new File(processor.getFileName()).getName(), processor.getPanel());
                    fileHistory.put(file);
                    saveContext();
                } else {
                    openComponent("Unknown", processor.getPanel());
                    processor.clear();
                }
            } catch (Exception ex){      
                fileHistory.remove(file);               
            }
        }        
        return processor;
    }

    
    public boolean isScriptOrProcessorOpen(String file) throws IOException {
        return isScriptOpen(file) || isProcessorOpen(file);
    }
    
    
    public Processor getProcessorForFile(String file){
        String extension = IO.getExtension(file);
        if (!extension.isEmpty()) {
            for (Processor processor : Processor.getServiceProviders()) {
                if (Arr.containsEqual(processor.getExtensions(), extension)) {                    
                    return processor;
                }
            }
        }
        return null;
    }
    
    
    public boolean isProcessorFile(String file){
        return (getProcessorForFile(file) != null);
    }
    
    public JPanel openScriptOrProcessor(String file) throws IOException, InstantiationException, IllegalAccessException {
        String extension = IO.getExtension(file);
        if (!extension.isEmpty()) {
            for (Processor processor : Processor.getServiceProviders()) {
                if (Arr.containsEqual(processor.getExtensions(), extension)) {
                    return openProcessor(processor.getClass(), file).getPanel();
                }
            }
        }
        return openScript(file);
    }
    
    
    public Set<String> getProcessorExtensions() throws IOException, InstantiationException, IllegalAccessException {
        Set<String>ret = new HashSet<>();
        for (Processor processor : Processor.getServiceProviders()) {
           for (String ext:processor.getExtensions()){
               ret.add(ext);
           }
        }
        return ret;
    }
    
    
    public List<Plot> plotData(String contextName, PlotDescriptor[] plots, PlotPreferences preferences) throws Exception {
        ArrayList<Plot> ret = new ArrayList<>();
        ch.psi.pshell.swing.PlotPanel plotPanel = getPlotPanel(contextName);
        if (preferences == null) {
            preferences = new PlotPreferences();
        }
        plotPanel.setPreferences(preferences);
        plotPanel.clear();
        if (plotPanel.isDisplayable() && SwingUtils.containsComponent(getPlotsTab(), plotPanel)) {
            getPlotsTab().setSelectedComponent(plotPanel);
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
    
    public List<Plot> plotData(String contextName, PlotDescriptor[] plots) throws Exception {
        return plotData(contextName, plots, Context.getExecutionPars().getPlotPreferences());
    }

    public void plotData(String contextName, String root, String path) throws Exception {
        plotData(contextName, root, path, Context.getExecutionPars().getPlotPreferences());
    }

    public void plotData(String contextName, String root, String path, PlotPreferences preferences) throws Exception {
        plotData(contextName, root, path, preferences, Context.getDataManager());
    }

    public void plotData(String contextName, String root, String path, PlotPreferences preferences, DataManager dm) throws Exception {
        try {
             List<Plot> plots = plotData(contextName, dm.getScanPlots(root, path).toArray(new PlotDescriptor[0]), preferences);
             if (plots.isEmpty()){
                 Processor.tryProcessorsPlot(root, path, dm);
             }
        } catch (Exception ex) {
            //If cannot open file, try with external processors
            if (!Processor.tryProcessorsPlot(root, path, dm)) {
                throw ex;
            }
        }
    }    
    
    
    final DataPanel.DataPanelListener dataPanelListener = new DataPanel.DataPanelListener() {
        @Override
        public void plotData(DataManager dataManager, String root, String path) throws Exception {
            PlotPreferences prefs = (path==null) ? null : dataManager.getPlotPreferences(root, path);
            MainFrame.this.plotData("Data", root, path, prefs, dataManager);
        }

        @Override
        public void plotData(Object array, Range range, String name, double[] x) throws Exception {
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

            if (x==null){
                if (range != null) {
                    if (range.getExtent().intValue() == Array.getLength(array)) {
                        x = new double[Array.getLength(array)];
                        for (int i = 0; i < x.length; i++) {
                            x[i] = i + range.min.intValue();
                        }
                    }
                }
            }
            if (name==null){
                name = "";
            }
            MainFrame.this.plotData("Data", new PlotDescriptor[]{new PlotDescriptor(name, array, x)}, null);
        }

        @Override
        public JPanel openFile(String fileName) throws Exception {
            String ext=IO.getExtension(fileName);
            if (ext.equalsIgnoreCase(Context.getScriptType().getExtension())) {
                return MainFrame.this.openScript(fileName);
            } else if (getProcessorExtensions().contains(ext)) {
                return openScriptOrProcessor(fileName);
            } else if (Arr.containsEqual(dataFileExtensions, ext)){
                return openDataFile(fileName);
            } else if (Arr.containsEqual(imageFileExtensions, ext)){
                return openImageFile(fileName);
            } else if ((new File(fileName).isDirectory()) && Context.getDataManager().isRoot(fileName)){
                return openDataFile(fileName);
            }  else {
                return openTextFile(fileName);
            }
        }
        
        @Override
        public ScriptEditor openScript(String script, String name) throws Exception {
            JTabbedPane tabDoc = getDocumentsTab();
            ScriptEditor editor = newScript(script);
            tabDoc.setTitleAt(tabDoc.indexOfComponent(editor), name);
            tabDoc.setSelectedComponent(editor);
            return editor;
        }
    };
    
    
    public JPanel showPanel(final String name) {
        return Context.getApp().getDevicePanelManager().showPanel(name);
    }


    public JPanel showPanel(final GenericDevice dev) {
        return Context.getApp().getDevicePanelManager().showPanel(dev);
    }

    public JPanel showHistory(final Device dev) {
        return Context.getApp().getDevicePanelManager().showHistory(dev);
    }

    public Object openFile(File f) throws Exception {
        return openFile(f, null);
    }

    public Object openFile(File f, Processor processor) throws Exception {
        String fileName = f.getPath().trim();
        String ext = IO.getExtension(f);
        if (ext != null) {
            ext = ext.toLowerCase();
        }
        if (Arr.containsEqual(imageFileExtensions, ext)) {
            return openImageFile(fileName);
        } else if (Arr.containsEqual(textFileExtensions, ext)) {
            Editor ret = openTextFile(fileName);
            ret.setReadOnly(true);
            return ret;
        } else if (Arr.containsEqual(dataFileExtensions, ext)) {
            return openDataFile(fileName);
        } else {
            if (processor == null) {
                return openScript(fileName);
            } else {
                return openProcessor(processor.getClass(), fileName);
            }
        }
    }    
    

    public boolean save() throws IOException{
        ScriptEditor editor = getSelectedEditor();
        if (editor != null) {
            save(editor);
            return true;
        }
        Processor processor = getSelectedProcessor();
        if (processor != null) {
            save(processor);
            return true;
        }            
        return false;
    }

    public void save(ScriptEditor editor) throws IOException{
        if (editor.getFileName() == null) {
            saveAs(editor);
        } else {
            editor.save();
            getDocumentsTab().setTitleAt(getDocumentsTab().getSelectedIndex(), editor.getScriptName());
            saveContext();
        }
    }

    public void save(Processor processor) throws IOException{
        if (processor.getFileName() == null) {
            saveAs(processor);
        } else {
            processor.save(Context.getScriptFilePermissions());
            if (processor.getFileName() != null) {
                if (processor.isTabNameUpdated()) {
                    getDocumentsTab().setTitleAt(getDocumentsTab().getSelectedIndex(), new File(processor.getFileName()).getName());
                }
            }
            saveContext();
        }
    }
    
       
    
    
    public boolean saveAs() throws IOException{
        ScriptEditor editor = getSelectedEditor();
        if (editor != null) {
            return saveAs(editor);
        }
        Processor processor = getSelectedProcessor();
        if (processor != null) {
            return saveAs(processor);
        }        
        return false;
    }
    
    public boolean saveAs(ScriptEditor editor) throws IOException{
        JFileChooser chooser = new JFileChooser(Setup.getScriptsPath());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Script file", Context.getScriptType().getExtension());
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
            if ((IO.getExtension(fileName).isEmpty()) && (Context.getScriptType() != null)) {
                String extension = "." + Context.getScriptType().getExtension();
                fileName += extension;
            }
            editor.saveAs(fileName);
            fileHistory.put(fileName);
            getDocumentsTab().setTitleAt(getDocumentsTab().getSelectedIndex(), editor.getScriptName());
            saveContext();
            return true;
        }
        return false;
    }
    
    public boolean saveAs(Processor processor) throws IOException{
        JFileChooser chooser = new JFileChooser(Setup.expandPath(processor.getHomePath()));
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
            processor.saveAs(fileName, Context.getScriptFilePermissions());
            if (processor.getFileName() != null) {
                fileHistory.put(processor.getFileName());
                if (processor.isTabNameUpdated()) {
                    getDocumentsTab().setTitleAt(getDocumentsTab().getSelectedIndex(), new File(processor.getFileName()).getName());
                }
                saveContext();
            }
            return true;
        }            
        return false;
    }
    
    
    //Queues
    
    public void onExecQueueChanged(ExecutionStage[] queue) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                onExecQueueChanged(queue);
            });
            return;
        }
        JTabbedPane tabStatus = getStatusTab();
        if (tabStatus != null){        
            if (queue.length == 0) {
                if (nextStagesPanel.isDisplayable()) {
                    boolean isSelected = tabStatus.getSelectedComponent() == nextStagesPanel;
                    tabStatus.remove(nextStagesPanel);
                    if (isSelected) {
                        if (getOutputPanel()!=null){                        
                            if (getOutputPanel().isDisplayable()) {
                                tabStatus.setSelectedComponent(getOutputPanel());
                            } else {
                                tabStatus.setSelectedComponent(getOutputPanel());
                            }
                        }
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
    }
    
}
