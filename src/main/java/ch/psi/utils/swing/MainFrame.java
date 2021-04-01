package ch.psi.utils.swing;

import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.Serializer;
import ch.psi.utils.Sys;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.applet.Applet;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.swing.ImageIcon;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;

/**
 * A base class to implement application main window, based on the Swing Application Framework (JSR
 * 296).
 */
public abstract class MainFrame extends JFrame {

    static final Logger logger = Logger.getLogger(MainFrame.class.getName());
    URL iconURL;
    final Timer timer1s;

    public MainFrame() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                try {
                    onOpen();
                } catch (Exception ex) {
                    logger.log(Level.FINE, null, ex);
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    onClosing();
                } catch (Exception ex) {
                    logger.log(Level.FINE, null, ex);
                }
            }
        });
        addHierarchyListener((HierarchyEvent e) -> {
            if ((HierarchyEvent.SHOWING_CHANGED & e.getChangeFlags()) != 0) {
                if (isShowing()) {
                    try {
                        onShow();
                        MainFrame.this.timer1s.start();
                    } catch (Exception ex) {
                        logger.log(Level.FINE, null, ex);
                    }
                } else {
                    try {
                        MainFrame.this.timer1s.stop();
                        onHide();
                    } catch (Exception ex) {
                        logger.log(Level.FINE, null, ex);
                    }
                }
            }
        });
        timer1s = new Timer(1000, (ActionEvent e) -> {
            try {
                onTimer();
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        });
        SwingUtils.enableFullScreen(this);  //This is for macos           

    }

    public Window getActiveWindow() {
        for (Window w : getOwnedWindows()) {
            if (w.isActive()) {
                return w;
            }
        }
        return this;
    }

    Dimension normalSize;

    protected boolean isFullScreen() {
        return SwingUtils.isFullScreen(this);
    }

    protected void setFullScreen(boolean value) {
        if (value) {
            normalSize = getSize();
        }
        SwingUtils.setFullScreen(this, value);
        if (!value) {
            if (normalSize == null) {
                normalSize = getPreferredSize();
            }
            setSize(normalSize);
            SwingUtils.centerComponent(null, this);
        }
    }

    @Override
    public void dispose() {
        try {
            onDispose();
        } catch (Exception ex) {
            logger.log(Level.FINE, null, ex);
        }
        super.dispose();
    }

    Boolean visible;

    @Override
    public void setVisible(boolean visible) {
        if (this.visible == null) {
            onCreate();
        }
        this.visible = visible;
        super.setVisible(visible);
    }

    //For logging
    @Override
    public String toString() {
        return "MainFrame";
    }

    /**
     * Called once when frame is created, before being visible
     */
    protected void onCreate() {
    }

    /**
     * Called once in the first time the frame is shown
     */
    protected void onOpen() {
    }

    /**
     * Called once when the frame is about to be disposed
     */
    protected void onDispose() {
    }

    /**
     * Called every time the frame is shown (also before open is called)
     */
    protected void onShow() {
    }

    /**
     * Called every time the frame is hidden (also before disposed)
     */
    protected void onHide() {
    }

    /**
     * Called every second if the frame is visible
     */
    protected void onTimer() {
    }

    /**
     * Called when window is being closed
     */
    protected void onClosing() {
    }

    public void setIcon(URL url) {
        iconURL = url;
        Image icon = Toolkit.getDefaultToolkit().getImage(url);
        setIconImage(icon);
    }

    public URL getIcon() {
        return iconURL;
    }
    
    public static ImageIcon searchIcon(String name, Class appClass, String resourcePath) {
        try {
            //First look for icons in current folder and resource folder
            for (String path : new String[]{"./", "./resources/"}) {
                try {
                    if (new File(path + name + ".png").exists()) {
                        return new javax.swing.ImageIcon("./" + name + ".png");
                    }
                } catch (Exception ex) {
                }
            }
            Class[] classes = new Class[]{appClass};
            try {
                Class cls = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
                classes = Arr.insert(classes, cls, 0);

            } catch (Exception ex) {
            }

            //Look for icons in resource folder under the caller jar file 
            for (Class cls : classes) {
                try {
                    String dir = cls.getProtectionDomain().getCodeSource().getLocation().getPath();
                    for (String path : new String[]{dir, dir + "resources/"}) {
                        if (new File(path + name + ".png").exists()) {
                            return new ImageIcon(path + name + ".png");
                        }
                    }
                } catch (Exception ex) {
                }
            }
            //Look at internal icons, checking if a dark version exists            
            if (isDark()) {
                try {
                    return new ImageIcon(appClass.getResource(resourcePath + "dark/" +name + ".png")); 
                } catch (Exception e) {
                }
            }
            return new ImageIcon(appClass.getResource(resourcePath + name + ".png"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
 

    //GUI utils
    public void showChildWindow(Window window) {
        window.setIconImage(Toolkit.getDefaultToolkit().getImage(getIcon()));
        centerWindow(window);
        SwingUtilities.updateComponentTreeUI(window);
        window.setVisible(true);
        if ((window.isDisplayable()) && (window.isShowing())) {
            window.requestFocus();
        }
    }

    public void hideChildWindow(Window window) {
        window.setVisible(false);
    }

    public void setIconToWindow(Window window) {
        window.setIconImage(Toolkit.getDefaultToolkit().getImage(iconURL));
    }

    public void centerWindow(Window window) {
        SwingUtils.centerComponent(this, window);
    }

    public boolean isMaximized() {
        return SwingUtils.isMaximized(this);
    }

    public Component getComponentByName(Container parent, String name) {
        return SwingUtils.getComponentByName(this, name);
    }

    public int getComponentIndex(Container parent, Component component) {
        return SwingUtils.getComponentIndex(this, component);
    }

    public boolean containsComponent(Container parent, Component component) {
        return SwingUtils.containsComponent(this, component);
    }

    public Component[] getComponentsByType(Class type) {
        return SwingUtils.getComponentsByType(this, type);
    }

    public void showMessage(String title, String msg) {
        SwingUtils.showMessage(this, title, msg);
    }

    public void showMessage(String title, String msg, int messageType) {
        SwingUtils.showMessage(this, title, msg, -1, messageType);
    }

    public void showException(Exception ex) {
        SwingUtils.showException(this, ex);
    }

    public void showException(Exception ex, String title) {
        SwingUtils.showException(this, ex, title);
    }

    public OptionResult showOption(String title, String msg, OptionType type) {
        return SwingUtils.showOption(this, title, msg, type);
    }

    public OptionResult showOption(String title, Component componment, OptionType type) {
        return SwingUtils.showOption(this, title, componment, type);
    }

    public String getString(String msg, Object current) {
        return SwingUtils.getString(this, msg, current);
    }

    //LAF
    public static String getNimbusLookAndFeel() {
        return SwingUtils.getNimbusLookAndFeel();
    }

    public static String getDarculaLookAndFeel() {
        return SwingUtils.getDarculaLookAndFeel();
    }
    
    public static String getDarkLookAndFeel() {
        return SwingUtils.getDarkLookAndFeel();
    }

    public static String getFlatLookAndFeel() {
        return SwingUtils.getFlatLookAndFeel();
    }
    
    public enum LookAndFeelType {
        s, system,
        m, metal,
        n, nimbus,
        d, darcula,
        f, flat,
        b, dark;
        static public LookAndFeelType[] getFullNames(){
            return new LookAndFeelType[]{system, metal, nimbus, darcula, flat, dark};
        }
        static public LookAndFeelType[] getShortNames(){
            return new LookAndFeelType[]{s, m, n, d, f, b};
        }
        public boolean hasTabHoverEffect(){
            return Arr.contains(new LookAndFeelType[]{n, nimbus, f, flat, b, dark}, this);
        }
    }

    public static LookAndFeelType getLookAndFeelType(){
        String laf = UIManager.getLookAndFeel().getClass().getName();
        for (LookAndFeelType type : LookAndFeelType.getFullNames()) {
            if (getLookAndFeel(type).equalsIgnoreCase(laf)){
                return type;
            }
        }

        return null;
    }

    public static String getLookAndFeel() {
        return UIManager.getLookAndFeel().getClass().getName();
    }

    public static String getLookAndFeel(LookAndFeelType type) {
        switch (type){
            case s: case system: return UIManager.getSystemLookAndFeelClassName();
            case m: case metal: return UIManager.getCrossPlatformLookAndFeelClassName();
            case n: case nimbus: return getNimbusLookAndFeel();
            case d: case darcula: return MainFrame.getDarculaLookAndFeel();
            case f: case flat: return MainFrame.getFlatLookAndFeel();
            case b: case dark: return MainFrame.getDarkLookAndFeel();
        }
        return null;
    }
 
    static Boolean dark; 
    public static boolean isDark() {
        if (dark==null){
            dark = SwingUtils.isDark();
    }
        return dark;
    }

    public static boolean isNimbus() {
        return SwingUtils.isNimbus();
    }

    public static void setLookAndFeel(LookAndFeelType type) {
        setLookAndFeel(type.toString());
    }
    public static void setLookAndFeel(String className) {
        if (className == null) {
            return;
        }
        if (Arr.containsEqual(Convert.toStringArray(LookAndFeelType.values()), className)){
            className = getLookAndFeel(LookAndFeelType.valueOf(className));
        }
        SwingUtils.setLookAndFeel(className);
    }

    //Window state persistence
    public String getSessionPath() {
        return Paths.get(Sys.getUserHome(), "." + getClass().getName()).toString();
    }

    //Other windows with persisted state   
    ArrayList<Window> persistedWindows;
    ArrayList<String> persistedWindowNames;

    public void addPersistedWindow(Window window) {
        if (window != null) {
            if (persistedWindows == null) {
                persistedWindows = new ArrayList<Window>();
            }
            persistedWindows.add(window);
            String name = window.getName();
            if (name != null) {
                if (persistedWindowNames == null) {
                    loadPersistedWindowNames();
                }
                if (!persistedWindowNames.contains(name)) {
                    persistedWindowNames.add(name);
                    savePersistedWindowNames();
                }
            }
            if (hasWindowState(window)) {
                restoreWindowState(window);
            }
        }
    }

    public void removePersistedWindow(Window window) {
        if ((window != null) && (persistedWindows != null)) {
            if (persistedWindows.contains(window)) {
                persistedWindows.remove(window);
                String name = window.getName();
                if (name != null) {
                    if (persistedWindowNames == null) {
                        loadPersistedWindowNames();
                    }
                    if (persistedWindowNames.contains(name)) {
                        persistedWindowNames.remove(name);
                        savePersistedWindowNames();
                    }
                }
                deleteWindowState(window);
            }
        }
    }

    public Window[] getPersistedWindows() {
        Window[] aux = new Window[0];
        if (persistedWindows == null) {
            return aux;
        }
        return persistedWindows.toArray(aux);
    }

    public String[] getPersistedWindowNames() {
        String[] aux = new String[0];
        if (persistedWindowNames == null) {
            loadPersistedWindowNames();
        }
        return persistedWindowNames.toArray(aux);
    }

    void loadPersistedWindowNames() {
        persistedWindowNames = new ArrayList<String>();
        String fileName = "Windows." + sessionEncoder.toString();
        try {
            byte[] array = Files.readAllBytes(Paths.get(getSessionPath(), fileName));
            persistedWindowNames = (ArrayList<String>) Serializer.decode(array);
        } catch (FileNotFoundException | NoSuchFileException ex) {
            logger.log(Level.FINE, null, ex);
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
    }

    void savePersistedWindowNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (Window w : persistedWindows) {
            if (w.getName() != null) {
                names.add(w.getName());
            }
        }
        String fileName = "Windows." + sessionEncoder.toString();
        try {
            Files.write(Paths.get(getSessionPath(), fileName),
                    Serializer.encode(persistedWindowNames, sessionEncoder));
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
    }

    public void saveState() {
        try {
            saveWindowState(this);
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
    }

    public void savePersistedWindowsStates() {
        if (persistedWindows != null) {
            for (Window window : persistedWindows) {
                if (window.isDisplayable()) {
                    saveWindowState(window);
                } else {
                    deleteWindowState(window);
                }
            }
        }
    }

    public void saveAllWindowsStates() {
        for (Window window : SwingUtils.getVisibleWindows()) {
            saveWindowState(window);
        }
    }

    public void saveWindowState(Window window) {
        if (window == null) {
            return;
        }
        String fileName = getSessionFilename(window);
        if (fileName != null) {
            try {
                save(window, fileName);
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        }
    }

    public void deleteWindowState(Window window) {
        if (window == null) {
            return;
        }
        String fileName = getSessionFilename(window);
        if (fileName != null) {
            try {
                Path stateFile = Paths.get(getSessionPath(), fileName);
                if (Files.exists(stateFile)) {
                    Files.delete(stateFile);
                }
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        }
    }

    public void restoreState() {
        try {
            Dimension screenSize = getToolkit().getScreenSize();

            restoreWindowState(this);

            Point defaultLocation = defaultLocation(this);
            if (!isFullScreen()
                    && (!isLocationByPlatform())
                    && (getX() == defaultLocation.getX())
                    && (getY() == defaultLocation.getY())) {

                Dimension windowSize = getSize();

                if (screenSize.getWidth() / windowSize.getWidth() > 1.25
                        && screenSize.getHeight() / windowSize.getHeight() > 1.25) {
                    Component owner = getOwner();
                    setLocationRelativeTo(owner);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    public void deleteState() {
        deleteWindowState(this);
    }

    public boolean hasWindowState(Window window) {
        if (window == null) {
            return false;
        }
        String fileName = getSessionFilename(window);
        if (fileName != null) {
            return (new File(getSessionPath(), fileName).exists());
        }
        return false;
    }

    public void restoreWindowState(Window window) {
        if (window == null) {
            return;
        }
        try {
            String fileName = getSessionFilename(window);
            if (fileName != null) {
                if (!(new File(getSessionPath(), fileName).exists())) {
                    logger.fine("No session file for window " + getComponentName(window));
                } else {
                    restore(window, fileName);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    public Map<String, Object> getPersistedComponentState(Component component) {
        try {
            if (component == null) {
                return null;
            }
            String fileName = getSessionFilename(this);
            if (fileName != null) {
                if (!(new File(getSessionPath(), fileName).exists())) {
                    logger.fine("No session file for component " + getComponentName(component));
                } else {
                    checkSaveRestoreArgs(component, fileName);
                    byte[] array = Files.readAllBytes(Paths.get(getSessionPath(), fileName));
                    Map<String, Object> stateMap = (Map<String, Object>) Serializer.decode(array);
                    return stateMap;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
        return null;
    }

    public void restoreComponent(Component component) {
        if (component == null) {
            return;
        }
        Map<String, Object> componentState = getPersistedComponentState(component);
        if (componentState != null) {
            String pathname = getComponentPathname(component);
            Object state = componentState.get(pathname);
            if (state != null) {
                setSessionState(component, state);
            } else {
                logger.fine("No session state for component " + getComponentName(component));
            }
        }
    }

    protected String getSessionFilename(Window window) {
        return getComponentName(window) + ".session." + sessionEncoder.toString();
    }

    public void save(Component root, String fileName) {
        checkSaveRestoreArgs(root, fileName);
        save(root, Paths.get(getSessionPath(), fileName));
    }

    public void restore(Component root, String fileName) {
        checkSaveRestoreArgs(root, fileName);
        restore(root, Paths.get(getSessionPath(), fileName));
    }

    void checkSaveRestoreArgs(Component root, String fileName) {
        if (root == null) {
            throw new IllegalArgumentException("null root");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
    }

    public static void save(Component root, Path path) {
        Map<String, Object> stateMap = new HashMap<String, Object>();
        saveTree(Collections.singletonList(root), stateMap);
        path.toFile().getParentFile().mkdirs();
        try {
            Files.write(path, Serializer.encode(stateMap, sessionEncoder));
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
    }

    public static void restore(Component root, Path path) {
        try {
            byte[] array = Files.readAllBytes(path);
            Map<String, Object> stateMap = (Map<String, Object>) Serializer.decode(array, sessionEncoder);
            if (stateMap != null) {
                restoreTree(Collections.singletonList(root), stateMap);
            }
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
    }

    static protected Serializer.EncoderType sessionEncoder = Serializer.EncoderType.bin;

    static void saveTree(List<Component> roots, Map<String, Object> stateMap) {
        if (stateMap != null) {
            List<Component> allChildren = new ArrayList<Component>();
            for (Component root : roots) {
                if (root != null) {
                    try {
                        String pathname = getComponentPathname(root);
                        if (pathname != null) {
                            Object state = getSessionState(root);
                            if (state != null) {
                                stateMap.put(pathname, state);
                            }
                        }
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                    if (root instanceof Container) {
                        Component[] children = ((Container) root).getComponents();
                        if ((children != null) && (children.length > 0)) {
                            Collections.addAll(allChildren, children);
                        }
                    }
                }
            }
            if (allChildren.size() > 0) {
                saveTree(allChildren, stateMap);
            }
        }
    }

    static void restoreTree(List<Component> roots, Map<String, Object> stateMap) {
        List<Component> allChildren = new ArrayList<Component>();
        for (Component root : roots) {
            if (root != null) {
                try {
                    String pathname = getComponentPathname(root);
                    if (pathname != null) {
                        Object state = stateMap.get(pathname);
                        if (state != null) {
                            setSessionState(root, state);
                        } else {
                            logger.finest("No session state for " + getComponentName(root));
                        }
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }

                if (root instanceof Container) {
                    Component[] children = ((Container) root).getComponents();
                    if ((children != null) && (children.length > 0)) {
                        Collections.addAll(allChildren, children);
                    }
                }
            }
        }
        if (allChildren.size() > 0) {
            restoreTree(allChildren, stateMap);
        }
    }

    static void checkClassArg(Class cls) {
        if (cls == null) {
            throw new IllegalArgumentException("null class");
        }
    }

    static protected String getComponentName(Component c) {
        if ((c.getName() != null) && (!c.getName().isEmpty())) {
            return c.getName();
        }
        return c.getClass().getSimpleName();
    }

    static String getComponentPathname(Component c) {
        String name = getComponentName(c);
        if (name == null) {
            return null;
        }
        StringJoiner path = new StringJoiner("/");
        path.add(name);
        while ((c.getParent() != null) && !(c instanceof Window) && !(c instanceof Applet)) {
            c = c.getParent();
            name = getComponentName(c);
            if (name == null) {
                int n = c.getParent().getComponentZOrder(c);
                if (n >= 0) {
                    Class cls = c.getClass();
                    name = cls.getSimpleName();
                    if (name.isEmpty()) {
                        name = "Anonymous" + cls.getSuperclass().getSimpleName();
                    }
                    name = name + n;
                } else {
                    // Implies that the component tree is changing
                    // while we're computing the path. Punt.
                    logger.warning("Couldn't compute pathname for " + c);
                    return null;
                }
            }
            path.add(name);
        }
        return path.toString();
    }

    static class WindowState implements Serializable {

        private static final long serialVersionUID = 1L;

        Rectangle bounds;
        Rectangle gcBounds = null;
        int screenCount;
        int frameState = Frame.NORMAL;
        boolean fullScreen;

        WindowState(Rectangle bounds, Rectangle gcBounds, int screenCount, int frameState, boolean fullScreen) {
            if (bounds == null) {
                throw new IllegalArgumentException("null bounds");
            }
            if (screenCount < 1) {
                throw new IllegalArgumentException("invalid screen count");
            }
            this.bounds = bounds;
            this.gcBounds = gcBounds;
            this.screenCount = screenCount;
            this.frameState = frameState;
            this.fullScreen = fullScreen;
        }

        boolean isDefined() {
            return (gcBounds != null) && (!gcBounds.isEmpty());
        }
    }

    static class TableState implements Serializable {

        private static final long serialVersionUID = 1L;

        int[] columnWidths = {};

        TableState(int[] columnWidths) {
            this.columnWidths = columnWidths;
        }
    }

    static class TabbedPaneState implements Serializable {

        private static final long serialVersionUID = 1L;

        String selectedTitle;
        int selectedIndex;
        int tabCount;

        TabbedPaneState(String selectedTitle, int selectedIndex, int tabCount) {
            if (tabCount < 0) {
                throw new IllegalArgumentException("invalid tab count");
            }
            if ((selectedIndex < -1) || (selectedIndex > tabCount)) {
                throw new IllegalArgumentException("invalid selected index");
            }
            this.selectedIndex = selectedIndex;
            this.tabCount = tabCount;
            this.selectedTitle = selectedTitle;
        }
    }

    static class SplitPaneState implements Serializable {

        private static final long serialVersionUID = 1L;

        int dividerLocation;
        int orientation = JSplitPane.HORIZONTAL_SPLIT;

        public SplitPaneState(int dividerLocation, int orientation) {
            if ((orientation != JSplitPane.HORIZONTAL_SPLIT) && (orientation != JSplitPane.VERTICAL_SPLIT)) {
                throw new IllegalArgumentException("invalid orientation");
            }
            if (dividerLocation < -1) {
                throw new IllegalArgumentException("invalid divider location");
            }
            this.dividerLocation = dividerLocation;
            this.orientation = orientation;
        }
    }

    static void setSessionState(Component c, Object state) {
        if (c instanceof Window) {
            Window w = (Window) c;
            WindowState windowState = (WindowState) state;
            if (windowState.fullScreen) {
                SwingUtils.setFullScreen((Frame) w, true);
            } else if (windowState.isDefined()) {
                putWindowNormalBounds(w, windowState.bounds);
                if (!w.isLocationByPlatform() && (state != null)) {

                    Rectangle gcBounds = windowState.gcBounds;
                    if (gcBounds != null && SwingUtils.isResizable(w)) {
                        if (computeVirtualGraphicsBounds().contains(gcBounds.getLocation())) {
                            w.setBounds(windowState.bounds);
                        } else {
                            w.setSize(windowState.bounds.getSize());
                        }
                    }
                    if (w instanceof Frame) {
                        ((Frame) w).setExtendedState(windowState.frameState);

                    }
                }
            }
        } else if (c instanceof JTabbedPane) {
            JTabbedPane p = (JTabbedPane) c;
            TabbedPaneState tps = (TabbedPaneState) state;
            int selectedIndex = -1;

            for (int i = 0; i < p.getTabCount(); i++) {
                if (String.valueOf(p.getTitleAt(i)).equals(tps.selectedTitle)) {
                    selectedIndex = i;
                    break;
                }
            }
            if (selectedIndex < 0) {
                if (p.getTabCount() == tps.tabCount) {
                    selectedIndex = tps.selectedIndex;
                }
            }
            if (selectedIndex >= 0) {
                p.setSelectedIndex(selectedIndex);
            }
        } else if (c instanceof JSplitPane) {
            final JSplitPane p = (JSplitPane) c;
            final SplitPaneState sps = (SplitPaneState) state;
            if (p.getOrientation() == sps.orientation) {
                //Need to temporize if maximized
                if (SwingUtils.isMaximized(SwingUtils.getFrame(c))) {
                    SwingUtils.invokeDelayed(() -> {
                        p.setDividerLocation(sps.dividerLocation);
                    }, 500);
                } else {
                    p.setDividerLocation(sps.dividerLocation);
                }
            }
        } else if (c instanceof JTable) {
            final JTable table = (JTable) c;
            final int[] columnWidths = ((TableState) state).columnWidths;
            if (table.getColumnCount() == columnWidths.length) {
                for (int i = 0; i < columnWidths.length; i++) {
                    if (columnWidths[i] != -1) {
                        TableColumn tc = table.getColumnModel().getColumn(i);
                        if (tc.getResizable()) {
                            tc.setPreferredWidth(columnWidths[i]);
                        }
                    }
                }
            }
        }
    }

    static Object getSessionState(Component c) {

        if (c instanceof Window) {
            int frameState = Frame.NORMAL;
            boolean fullScreen = false;
            if (c instanceof Frame) {
                frameState = ((Frame) c).getExtendedState();
                fullScreen = SwingUtils.isFullScreen((Frame) c);
            }
            GraphicsConfiguration gc = c.getGraphicsConfiguration();
            Rectangle gcBounds = (gc == null) ? null : gc.getBounds();
            Rectangle frameBounds = c.getBounds();
            if ((c instanceof JFrame) && (0 != (frameState & Frame.MAXIMIZED_BOTH))) {
                if (!(fullScreen)) {
                    Rectangle normalBounds = getWindowNormalBounds((JFrame) c);
                    if (normalBounds != null) {
                        frameBounds = normalBounds;
                    }
                }
            }
            return new WindowState(frameBounds, gcBounds, GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length, frameState, fullScreen);
        } else if (c instanceof JTabbedPane) {
            JTabbedPane p = (JTabbedPane) c;
            int index = p.getSelectedIndex();
            if (index < 0) {
                return null;
            }
            return new TabbedPaneState(p.getTitleAt(index), index, p.getTabCount());
        } else if (c instanceof JSplitPane) {
            JSplitPane p = (JSplitPane) c;
            return new SplitPaneState(p.getUI().getDividerLocation(p), p.getOrientation());
        } else if (c instanceof JTable) {
            JTable table = (JTable) c;
            int[] columnWidths = new int[table.getColumnCount()];
            boolean resizableColumnExists = false;
            for (int i = 0; i < columnWidths.length; i++) {
                TableColumn tc = table.getColumnModel().getColumn(i);
                columnWidths[i] = (tc.getResizable()) ? tc.getWidth() : -1;
                if (tc.getResizable()) {
                    resizableColumnExists = true;
                }
            }
            return (resizableColumnExists) ? new TableState(columnWidths) : null;

        }
        return null;
    }

    static Point defaultLocation(Window window) {
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets insets = window.getToolkit().getScreenInsets(gc);
        int x = bounds.x + insets.left;
        int y = bounds.y + insets.top;
        return new Point(x, y);
    }

    static final String WINDOW_STATE_NORMAL_BOUNDS = "WindowState.normalBounds";

    static Rectangle getWindowNormalBounds(Window window) {
        if (window instanceof JFrame) {
            Object res = ((JFrame) window).getRootPane().getClientProperty(WINDOW_STATE_NORMAL_BOUNDS);
            if (res instanceof Rectangle) {
                return (Rectangle) res;
            }
        }
        return null;
    }

    static void putWindowNormalBounds(Window window, Rectangle bounds) {
        if (window instanceof JFrame) {
            ((JFrame) window).getRootPane().putClientProperty(WINDOW_STATE_NORMAL_BOUNDS, bounds);
        }
    }

    static Rectangle computeVirtualGraphicsBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd : gs) {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            virtualBounds = virtualBounds.union(gc.getBounds());
        }
        return virtualBounds;
    }
}
