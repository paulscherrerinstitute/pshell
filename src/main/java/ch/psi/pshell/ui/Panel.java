package ch.psi.pshell.ui;

import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Plugin implementation and JPanel extension, consisting of a panel to be
 * loaded to the Workbench as a tab ins the documents - or else to be executed
 * stand alone (detached mode).
 */
public class Panel extends MonitoredPanel implements Plugin {

    boolean detached = App.isDetached();
    boolean nested;
    Frame parent;

    public void setDetached(boolean detached) {
        setDetached(detached, null);
    }
    public void setDetached(boolean detached, Frame parent) {
        if (this.isStarted()) {
            throw new RuntimeException("Plugin is already started");
        }
        if (!detached && App.isDetached()) {
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
            if (!getContext().isLocalMode() || App.isDetachedPanelsPersisted()) {
                loadComponentsState();
            }
        }
    }

    @Override
    public void onStop() {
        if (isActive()) {
            stopTimer();
            Plugin.super.onStop();
            if (!getContext().isLocalMode() || App.isDetachedPanelsPersisted()) {
                saveComponentsState();
            }
            unload();
        }
    }

    boolean isLoaded() {
        if (isDetached()) {
            return isShowing();
        }
        return (getTabIndex() >= 0);
    }

    void setLoaded(boolean value) {
        if (value != isLoaded()) {
            if (value) {
                load();
            } else {
                unload();
            }
        }
    }

    public boolean isActive() {

        if (App.isDetached()) {
            if (App.getDetachedPanel() != null) {
                return IO.getPrefix(App.getDetachedPanel()).equals(getTitle());
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
                    boolean fullScreen = (frameCount == 0) && App.isFullScreen();
                    JFrame frame = new JFrame(title);
                    if (fullScreen) {
                        SwingUtils.enableFullScreen(frame);
                    }
                    frame.setIconImage(App.getIconSmall());
                    if (App.isDetachedAppendStatusBar()) {
                        JPanel panel = new JPanel();
                        panel.setLayout(new BorderLayout());
                        panel.add(this, BorderLayout.CENTER);
                        panel.add(new StatusBar(), BorderLayout.SOUTH);
                        frame.setContentPane(panel);
                    } else {
                        frame.setContentPane(this);
                    }
                    frame.setName(((getName() == null) ? getClass().getSimpleName() : getName()) + "Frame");
                    frame.pack();

                    if (fullScreen) {
                        SwingUtils.setFullScreen(frame, true);
                    } else {
                        if (!App.isDetachedPanelsPersisted()) {
                            Dimension size = App.getSize();
                            if (size!=null){
                                frame.setSize(size);
                            }
                        }
                        SwingUtils.centerComponent(null, frame);
                        if (App.isDetachedPanelsPersisted()) {
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
                                if (App.isDetachedPanelsPersisted()) {
                                    saveWindowState();
                                }       
                                unload();
                            } else {
                                String msg = "Do you want to close the panel";
                                msg += (frameCount == 1) ? " and finish the application?" : "?";
                                if ((App.isQuiet())
                                        || (SwingUtils.showOption(frame, "Close", msg, OptionType.YesNo) == OptionResult.Yes)) {
                                    if (App.isDetachedPanelsPersisted()) {
                                        saveWindowState();
                                    }
                                    unload();
                                    frameCount--;
                                    if (frameCount == 0) {
                                        App.getInstance().exit(this);
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
        return getContext().getSetup().getContextPath();
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
                IO.setFilePermissions(stateFile, getContext().getConfig().filePermissionsConfig);
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

}
