package ch.psi.pshell.framework;

import ch.psi.pshell.sequencer.InterpreterListener;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.SwingUtils.OptionResult;
import ch.psi.pshell.swing.SwingUtils.OptionType;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.State;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.BeanInfo;
import java.beans.ExceptionListener;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JToolBar.Separator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Plugin implementation and JPanel extension, consisting of a panel to be
 * loaded to the Workbench as a tab ins the documents - or else to be executed
 * stand alone (detached mode).
 */
public class Panel extends MonitoredPanel implements Plugin {

    boolean detached = Setup.isDetached();
    boolean nested;
    Frame parent;

    public void setDetached(boolean detached) {
        setDetached(detached, null);
    }
    public void setDetached(boolean detached, Frame parent) {
        if (this.isStarted()) {
            throw new RuntimeException("Plugin is already started");
        }
        if (!detached && Setup.isDetached()) {
            throw new RuntimeException("Plugin must be detached");
        }
        this.detached = detached;
        this.parent = parent;
        nested = detached;
    }

    public boolean isDetached() {
        return detached;
    }

    public boolean isNested() {
        return nested;
    }
    
    public boolean isDetachedDialog() {
        return detached && (parent!=null);
    }

    @Override
    public void onStart() {
        if (isActive()) {
            Plugin.super.onStart();
            load();
            if (!Setup.isLocal() || Setup.isDetachedPanelsPersisted()) {
                loadComponentsState();
            }
        }
    }

    @Override
    public void onStop() {
        if (isActive()) {
            stopTimer();
            Plugin.super.onStop();
            if (!Setup.isLocal() || Setup.isDetachedPanelsPersisted()) {
                saveComponentsState();
            }
            unload();
        }
    }

    public boolean isLoaded() {
        if (isDetached()) {
            return isShowing();
        }
        return (getTabIndex() >= 0);
    }

    public void setLoaded(boolean value) {
        if (value != isLoaded()) {
            if (value) {
                load();
            } else {
                unload();
            }
        }
    }

    public boolean isActive() {
        if (Setup.isDetached()) {
            List<String> names = Setup.getDetachedPanels();            
            if (names != null){
                for (String name : names){
                    if (IO.getPrefix(name).equals(getTitle())){
                        return true;
                    }
                }                
                return false;
            }
        }
        return true;
    }

    static int frameCount;

    public String getTitle() {
        String title = Plugin.super.getPluginName();
        if (title.contains(".")) {
            title = title.substring(0, title.lastIndexOf("."));
        }
        return title;
    }

    void load() {
        if (!isActive()) {
            return;
        }
        if (!isLoaded()) {
            String title = getTitle();
            if (isDetached()) {
                if (isDetachedDialog()) {
                    JDialog dlg = new JDialog(getTopLevel(), title, false);
                    dlg.setIconImage(App.getIconSmall());
                    dlg.setContentPane(this);
                    dlg.setName(((getName() == null) ? getClass().getSimpleName() : getName()) + "Dialog");
                    dlg.pack();
                    SwingUtils.centerComponent(getTopLevel(), dlg);
                    dlg.setVisible(true);
                    dlg.requestFocus();
                    dlg.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            unload();
                        }
                    });                    
                } else {
                    boolean fullScreen = (frameCount == 0) && Setup.isFullScreen();
                    JFrame frame = new JFrame(title);
                    frame.setIconImage(App.getIconSmall());
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    if (Setup.isDetachedAppendToolBar()) {     
                        if (this instanceof Processor){
                            panel.add(getToolBar(), BorderLayout.NORTH);
                        }
                    }
                    panel.add(this, BorderLayout.CENTER);
                    if (Setup.isDetachedAppendStatusBar()) {                    
                        panel.add(new StatusBar(), BorderLayout.SOUTH);
                    }                        
                    frame.setContentPane(panel);
                    frame.setName(((getName() == null) ? getClass().getSimpleName() : getName()) + "Frame");
                    frame.pack();

                    if (fullScreen) {
                        SwingUtils.setFullScreen(frame, true);
                    } else {
                        if (!Setup.isDetachedPanelsPersisted()) {
                            Dimension size = Setup.getSize();
                            if (size!=null){
                                frame.setSize(size);
                            }
                        }
                        SwingUtils.centerComponent(null, frame);
                        if (Setup.isDetachedPanelsPersisted()) {
                            loadWindowState();
                        }
                    }
                    frame.setVisible(true);
                    frame.requestFocus();
                    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            if (isNested()) {
                                if (Setup.isDetachedPanelsPersisted()) {
                                    saveWindowState();
                                }       
                                unload();
                            } else {
                                String msg = "Do you want to close the panel";
                                msg += (frameCount == 1) ? " and finish the application?" : "?";
                                if ((Setup.isQuiet())
                                        || (SwingUtils.showOption(frame, "Close", msg, OptionType.YesNo) == OptionResult.Yes)) {
                                    if (Setup.isDetachedPanelsPersisted()) {
                                        saveWindowState();
                                    }
                                    unload();
                                    frameCount--;
                                    if (frameCount == 0) {
                                        Context.getApp().exit(this);
                                    }
                                }
                            }
                        }
                    });
                    if (!isNested()) {
                        frameCount++;
                    }
                }
            } else {
                int index = 0;
                getView().getDocumentsTab().add(this, index);
                getView().getDocumentsTab().setTitleAt(index, title);
                getView().getDocumentsTab().setSelectedIndex(0);
            }
            onLoaded();
        }
    }

    void unload() {
        if (!isActive()) {
            return;
        }
        closeWindows();
        if (isLoaded()) {
            if (isDetached()) {

                Window window = getWindow();
                if (window != null) {
                    window.setVisible(false);
                }
            } else {
                getView().getDocumentsTab().remove(this);
            }
            onUnloaded();
        }
    }

    int getTabIndex() {
        if (getView() != null) {
            for (int i = 0; i < getView().getDocumentsTab().getTabCount(); i++) {
                if (getView().getDocumentsTab().getComponentAt(i) == this) {
                    return i;
                }
            }
        }
        return -1;
    }

    //Timer
    Timer timer;
    boolean disregardedTimer;

    public void startTimer(int interval) {
        startTimer(interval, -1);
    }

    public void startTimer(int interval, int delay) {
        stopTimer();
        timer = new Timer(interval, (ActionEvent e) -> {
            try {
                if (backgroundUpdate || isShowing()) {
                    disregardedTimer = false;
                    onTimer();
                } else {
                    disregardedTimer = true;
                }
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
            }
        });
        if (delay >= 0) {
            timer.setInitialDelay(delay);
        }
        timer.start();
    }

    public void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    protected void onTimer() {
    }

    //State persistence
    protected String getContextPath() {
        return Setup.getCachePath("panels");
    }

    /**
     * Must be called in constructor
     */
    Component[] persistedComponents;

    protected void setPersistedComponents(Component[] components) {
        persistedComponents = components;
    }

    protected Component[] getPersistedComponents() {
        return persistedComponents;
    }

    protected Path getComponentsStatePath() {
        return Paths.get(getContextPath(), getClass().getSimpleName() + "_" + "PersistedComponents.xml");
    }

    protected void clearComponentsState() {
        try {
            if (Files.exists(getComponentsStatePath())) {
                Files.delete(getComponentsStatePath());
            }
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }

    protected Path getWindowStatePath() {
        return Paths.get(getContextPath(), getClass().getSimpleName() + "_" + "WindowState.xml");
    }

    protected void saveWindowState() {
        try {
            if (isDetached()) {
                if (getWindow() != null) {
                    MainFrame.save(getWindow(), getWindowStatePath());
                }
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }

    protected void loadWindowState() {
        try {
            if (isDetached()) {
                if (getWindow() != null) {
                    MainFrame.restore(getWindow(), getWindowStatePath());
                }
            }
        } catch (Exception ex) {
            getLogger().log(Level.INFO, null, ex);
        }
    }

    protected void saveComponentsState() {
        try {
            if ((persistedComponents != null) && (persistedComponents.length > 0)) {
                File stateFile = getComponentsStatePath().toFile();
                XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(stateFile)));
                //Don't want to print to stdout
                e.setExceptionListener(new ExceptionListener() {
                    @Override
                    public void exceptionThrown(Exception ex) {
                        getLogger().log(Level.FINER, null, ex);
                    }
                });
                for (Component c : persistedComponents) {
                    e.writeObject(c);
                }
                e.close();
                IO.setFilePermissions(stateFile, Context.getConfigFilePermissions());
            } else {
                clearComponentsState();
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }

    protected void loadComponentsState() {
        try {
            File stateFile = getComponentsStatePath().toFile();
            if ((stateFile.exists()) && (stateFile.lastModified() < getPluginFile().lastModified())) {
                clearComponentsState();
            }
            if ((persistedComponents != null) && (persistedComponents.length > 0)) {

                XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(stateFile)));
                ArrayList<Component> state = new ArrayList<>();
                while (true) {
                    try {
                        Object obj = d.readObject();
                        if (obj == null) {
                            break;
                        }
                        state.add((Component) obj);
                    } catch (Exception ex) {
                        break;
                    }
                }

                if (persistedComponents.length != state.size()) {
                    throw new Exception("Invalid persistence file");
                }
                for (int i = 0; i < persistedComponents.length; i++) {
                    if (persistedComponents[i].getClass() != state.get(i).getClass()) {
                        throw new Exception("Invalid persistence file");
                    }
                }
                for (int i = 0; i < persistedComponents.length; i++) {
                    BeanInfo info = Introspector.getBeanInfo(persistedComponents[i].getClass());
                    for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                        if ((pd.getReadMethod() != null) && (pd.getWriteMethod() != null)) {
                            Object val = pd.getReadMethod().invoke(state.get(i));
                            try {
                                if ((val != null) && (val.getClass().isPrimitive()
                                        || Convert.isWrapperClass(val.getClass())
                                        || val.getClass() == String.class
                                        || val.getClass().isEnum())) {
                                    pd.getWriteMethod().invoke(persistedComponents[i], new Object[]{val});
                                }
                            } catch (Exception ex) {
                                getLogger().log(Level.WARNING, null, ex);
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException ex){
            getLogger().log(Level.FINE, null, ex);        
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
            clearComponentsState();
        }
    }

    /**
     * Requests doUpdate to be called in event loop, filtering multiple calls
     * coming before execution.
     */
    private final AtomicBoolean updating = new AtomicBoolean(false);
    boolean disregardedUpdateRequest;

    boolean backgroundUpdate;

    protected void setBackgroundUpdate(boolean value) {
        backgroundUpdate = value;
    }

    protected boolean getBackgroundUpdate() {
        return backgroundUpdate;
    }

    public void update() {
        if (backgroundUpdate || isShowing()) {
            if (updating.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(() -> {
                    updating.set(false);
                    doUpdate();
                });
            }
            disregardedUpdateRequest = false;
        } else {
            disregardedUpdateRequest = true;
        }
    }

    @Override
    protected void onShow() {
        //The panel won't limit the parent's resizing.
        setMinimumSize(new Dimension(0, 0));
        //GUI update is done only if showing. If had been requested, do it now
        if (disregardedUpdateRequest) {
            update();
        }
        if (disregardedTimer) {
            onTimer();
        }
    }

    protected void onLoaded() {

    }

    protected void onUnloaded() {

    }

    /**
     * Callback to perform update - in event thread
     */
    protected void doUpdate() {
    }
    
    public void restore(){
        onShow();
    }

    @Override
    public Frame getTopLevel() {
        if (parent!=null){
            return parent;
        }
        if (getView() != null) {
            return getView();
        }
        return (Frame) getTopLevelAncestor();
    }

    
    JToolBar getToolBar(){
        Processor p = (Processor) this;
        JToolBar toolBar=new JToolBar();
        JButton buttonNew = new JButton();
        JButton buttonOpen = new JButton();
        JButton buttonSave = new JButton();
        Separator jSeparator5 = new Separator();
        JButton buttonRestart = new JButton();
        Separator jSeparator6 = new javax.swing.JToolBar.Separator();
        JButton buttonRun = new JButton();
        JButton buttonDebug = new JButton();
        JButton buttonStep = new JButton();
        JButton buttonPause = new JButton();
        JButton buttonAbort = new JButton();
        Separator separatorStopAll = new javax.swing.JToolBar.Separator();
        JButton buttonStopAll = new JButton();
        Separator separatorInfo = new javax.swing.JToolBar.Separator();
        JButton buttonAbout = new JButton();
        
        toolBar.setRollover(true);
        toolBar.setFloatable(false);
        toolBar.setVisible(!Context.getRights().hideToolbar);
        
        ResourceBundle bundle = ResourceBundle.getBundle("ch/psi/pshell/ui/View");
        buttonNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/New.png")));
        buttonNew.setText(bundle.getString("View.buttonNew.text"));
        buttonNew.setToolTipText(bundle.getString("View.buttonNew.toolTipText"));
        buttonNew.setName("buttonNew");
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    p.clear();
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });
        toolBar.add(buttonNew);

        buttonOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Open.png")));
        buttonOpen.setText(bundle.getString("View.buttonOpen.text"));
        buttonOpen.setToolTipText(bundle.getString("View.buttonOpen.toolTipText"));
        buttonOpen.setName("buttonOpen");
        buttonOpen.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
                p.open();
            } catch (Exception ex) {
                showException(ex);
            }
        });
        buttonOpen.setEnabled(p.canSave());
        toolBar.add(buttonOpen);

        buttonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Save.png")));
        buttonSave.setText(bundle.getString("View.buttonSave.text"));
        buttonSave.setToolTipText(bundle.getString("View.buttonSave.toolTipText"));
        buttonSave.setName("buttonSave");
        buttonSave.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
                p.saveAs(Context.getScriptFilePermissions());
            } catch (Exception ex) {
                showException(ex);
            }
        });
        buttonSave.setEnabled(p.canSave());
        toolBar.add(buttonSave);

        jSeparator5.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator5.setPreferredSize(new java.awt.Dimension(20, 0));
        jSeparator5.setRequestFocusEnabled(false);
        toolBar.add(jSeparator5);

        buttonRestart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Reset.png")));
        buttonRestart.setText(bundle.getString("View.buttonRestart.text"));
        buttonRestart.setToolTipText(bundle.getString("View.buttonRestart.toolTipText"));
        buttonRestart.setName("buttonRestart");
        buttonRestart.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
                Context.getInterpreter().startRestart();
            } catch (Exception ex) {
                showException(ex);
            }            
        });
        toolBar.add(buttonRestart);

        jSeparator6.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator6.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(jSeparator6);

        buttonRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Play.png")));
        buttonRun.setText(bundle.getString("View.buttonRun.text"));
        buttonRun.setToolTipText(bundle.getString("View.buttonRun.toolTipText"));
        buttonRun.setName("buttonRun");
        buttonRun.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
                p.execute();
            } catch (Exception ex) {
                showException(ex);
            }            
        });
        toolBar.add(buttonRun);

        /*
        buttonDebug.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Step.png")));
        buttonDebug.setText(bundle.getString("View.buttonDebug.text"));
        buttonDebug.setToolTipText(bundle.getString("View.buttonDebug.toolTipText"));
        buttonDebug.setName("buttonDebug");
        buttonDebug.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
                p.step();
            } catch (Exception ex) {
                showException(ex);
            }              
        });
        toolBar.add(buttonDebug);
        */

        buttonStep.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/StepInto.png")));
        buttonStep.setText(bundle.getString("View.buttonStep.text"));
        buttonStep.setToolTipText(bundle.getString("View.buttonStep.toolTipText"));
        buttonStep.setName("buttonStep");
        buttonStep.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
                p.step();
            } catch (Exception ex) {
                showException(ex);
            }              
        });
        buttonStep.setEnabled(p.canStep());
        toolBar.add(buttonStep);
        
        buttonPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Pause.png")));
        buttonPause.setText(bundle.getString("View.buttonPause.text"));
        buttonPause.setToolTipText(bundle.getString("View.buttonPause.toolTipText"));
        buttonPause.setName("buttonPause");
        buttonPause.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
                p.pause();
            } catch (Exception ex) {
                showException(ex);
            }                         
        });
        buttonPause.setEnabled(p.canPause());
        toolBar.add(buttonPause);

        buttonAbort.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Stop.png")));
        buttonAbort.setText(bundle.getString("View.buttonAbort.text"));
        buttonAbort.setToolTipText(bundle.getString("View.buttonAbort.toolTipText"));
        buttonAbort.setName("buttonAbort");
        buttonAbort.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
               Context.abort();
            } catch (Exception ex) {
                showException(ex);
            }              
        });
        toolBar.add(buttonAbort);

        separatorInfo.setMaximumSize(new java.awt.Dimension(20, 32767));
        separatorInfo.setName("separatorInfo");
        separatorInfo.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(separatorInfo);

        buttonAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Info.png")));
        buttonAbout.setText(bundle.getString("View.buttonAbout.text"));
        buttonAbout.setToolTipText(bundle.getString("View.buttonAbout.toolTipText"));
        buttonAbout.setName("buttonAbout");
        buttonAbout.addActionListener((java.awt.event.ActionEvent evt) -> {
            try {
                getView().showAboutDialog();
            } catch (Exception ex) {
                showException(ex);
            }              
        });
        toolBar.add(buttonAbout);        
                
        for (JButton button : SwingUtils.getComponentsByType(toolBar, JButton.class)) {
            button.setFocusable(false);
            button.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
            button.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);            
            if (MainFrame.isDark()) {
                button.setIcon(new ImageIcon(App.getResourceUrl("dark/" + new File(((JButton) button).getIcon().toString()).getName())));        
            }            
        }            
        
        Context.getInterpreter().addListener(new InterpreterListener() {
            @Override
            public void onStateChanged(State state, State former) {
                boolean ready = (state == State.Ready);
                boolean busy = (state == State.Busy);
                boolean paused = (state == State.Paused);
                buttonRun.setEnabled(ready);
                buttonDebug.setEnabled((ready) || (paused));
                buttonPause.setEnabled(p.canPause() && Context.getInterpreter().canPause());
                buttonAbort.setEnabled(busy || paused || (state == State.Initializing));                
                buttonRestart.setEnabled((state != State.Initializing) && !Setup.isOffline());
            }
        });
        
        return toolBar;
    }
}
