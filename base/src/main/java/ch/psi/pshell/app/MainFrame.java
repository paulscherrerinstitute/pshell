package ch.psi.pshell.app;

import ch.psi.pshell.swing.StandardFrame;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.SwingUtils.OptionResult;
import ch.psi.pshell.swing.SwingUtils.OptionType;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.utils.Serializer;
import ch.psi.pshell.utils.Sys;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;

/**
 * A base class to implement application main window, based on the Swing Application Framework (JSR
 * 296).
 */
public abstract class MainFrame extends StandardFrame{
    protected static MainFrame INSTANCE;
    public static MainFrame getInstance(){
        return INSTANCE;
    }
    
    static final Logger logger = Logger.getLogger(MainFrame.class.getName());
    URL iconURL;


    public MainFrame() {
        Optional.ofNullable(App.getApplicationTitle()).ifPresent(this::setTitle);
        INSTANCE = this;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        SwingUtilities.invokeLater(()->{
            afterConstruction();
        });             
    }
    
    
    protected void afterConstruction(){
        if (MainFrame.isDark()) {
            onLafChange();
        }
        applyOptions();        
    }
    
    static FilePermissions persistenceFilesPermissions = FilePermissions.Default;
    @Hidden
    public static void setPersistenceFilesPermissions(FilePermissions permissions){
        persistenceFilesPermissions = permissions;
    }
    
    @Hidden
    public static FilePermissions getPersistenceFilesPermissions(){
        return persistenceFilesPermissions;
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

    public boolean isFullScreen() {
        return SwingUtils.isFullScreen(this);
    }

    public void setFullScreen(boolean value) {
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
    
    public void applyOptions(){
        if (Setup.isFullScreen()) {
            SwingUtils.setFullScreen(this, true);
        } else if (isPersisted()) {
            restoreState();        
        } else {
            Dimension size = Setup.getSize();
            if (size != null) {
                setSize(size);
            }
            SwingUtils.centerComponent(null, this);
        }
        setVisible(!isHidden());
    }

    
    public boolean isHidden() {
        return false;
    }

    public boolean isPersisted() {
        return !Options.LOCAL.getBool(true);
    }    

    //For logging
    @Override
    public String toString() {
        return "MainFrame";
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
    
    public void showScrollableMessage(String title, String description, String message) {
        SwingUtils.showScrollableMessage(this, title, description, message);
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
    
    public String getString(String msg, String[] options, String current) {
        return SwingUtils.getString(this, msg, options, current);
    }
    
    public String getPassword(Component parent, String title, String msg) {
        return SwingUtils.getPassword(this, title, msg);
    }

    public Object getEnum(Component parent, String msg, Class type, Object current) {
        return SwingUtils.getEnum(this, msg,  type, current);
    }
    
    public JDialog showDialog(String title, Dimension size, JComponent content) {
        return SwingUtils.showDialog(this, title, size, content);
    }
    
    public JFrame showFrame(String title, Dimension size, JComponent content) {
        return SwingUtils.showFrame(this, title, size, content);
    }            

    //LAF
    public static String getNimbusLookAndFeel() {
        return SwingUtils.getNimbusLookAndFeel();
    }

    public static String getDarkLookAndFeel() {
        return SwingUtils.getDarkLookAndFeel();
    }
    
    public static String getBlackLookAndFeel() {
        return SwingUtils.getBlackLookAndFeel();
    }    

    public static String getFlatLookAndFeel() {
        return SwingUtils.getFlatLookAndFeel();
    }
    
    public enum LookAndFeelType {
        s, system,
        m, metal,
        n, nimbus,
        b, black,
        d, dark,
        f, flat;
        static public LookAndFeelType[] getFullNames(){
            return new LookAndFeelType[]{system, metal, nimbus, flat, dark, black};
        }
        static public LookAndFeelType[] getShortNames(){
            return new LookAndFeelType[]{s, m, n, d, f, b};
        }
        public boolean hasTabHoverEffect(){
            return Arr.contains(new LookAndFeelType[]{n, nimbus, f, flat, d, dark, b, black}, this);
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
            case f: case flat: return MainFrame.getFlatLookAndFeel();
            case d: case dark: return MainFrame.getDarkLookAndFeel();
            case b: case black: return MainFrame.getBlackLookAndFeel();
        }
        return null;
    }
 
    public static boolean isDark() {
        return SwingUtils.isDark();
    }

    public static boolean isBlack() {
        return SwingUtils.isBlack();
    }
    
    public static boolean isNimbus() {
        return SwingUtils.isNimbus();
    }
    
    public static void setScaleUI(String scale) {
        if (scale!=null){
            System.setProperty( "flatlaf.uiScale", scale );                
        } else {
            System.clearProperty("flatlaf.uiScale");                
        }
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
    public String getPersistencePath() {
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
            byte[] array = Files.readAllBytes(Paths.get(getPersistencePath(), fileName));
            persistedWindowNames = (ArrayList<String>) Serializer.decode(array);
        } catch (FileNotFoundException | NoSuchFileException ex) {
            logger.log(Level.FINER, null, ex);
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
            Path path = Paths.get(getPersistencePath(), fileName);
            Files.write(path, Serializer.encode(persistedWindowNames, sessionEncoder));    
            IO.setFilePermissions(path.toString(), persistenceFilesPermissions);
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
        String fileName = getPersistenceFilename(window);
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
        String fileName = getPersistenceFilename(window);
        if (fileName != null) {
            try {
                Path stateFile = Paths.get(getPersistencePath(), fileName);
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
        String fileName = getPersistenceFilename(window);
        if (fileName != null) {
            return (new File(getPersistencePath(), fileName).exists());
        }
        return false;
    }

    public void restoreWindowState(Window window) {
        if (window == null) {
            return;
        }
        try {
            String fileName = getPersistenceFilename(window);
            if (fileName != null) {
                if (!(new File(getPersistencePath(), fileName).exists())) {
                    logger.log(Level.FINE, "No session file for window {0}", getComponentName(window));
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
            String fileName = getPersistenceFilename(this);
            if (fileName != null) {
                if (!(new File(getPersistencePath(), fileName).exists())) {
                    logger.log(Level.FINE, "No session file for component {0}", getComponentName(component));
                } else {
                    checkSaveRestoreArgs(component, fileName);
                    byte[] array = Files.readAllBytes(Paths.get(getPersistencePath(), fileName));
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
                logger.log(Level.FINE, "No session state for component {0}", getComponentName(component));
            }
        }
    }

    protected String getPersistenceFilename(Window window) {
        return getComponentName(window) + ".session." + sessionEncoder.toString();
    }

    public void save(Component root, String fileName) {
        checkSaveRestoreArgs(root, fileName);
        save(root, Paths.get(getPersistencePath(), fileName));
    }

    public void restore(Component root, String fileName) {
        checkSaveRestoreArgs(root, fileName);
        restore(root, Paths.get(getPersistencePath(), fileName));
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
            IO.setFilePermissions(path.toString(), persistenceFilesPermissions);
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
                    if (root instanceof Container container) {
                        Component[] children = container.getComponents();
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
                            logger.log(Level.FINEST, "No session state for {0}", getComponentName(root));
                        }
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }

                if (root instanceof Container container) {
                    Component[] children = container.getComponents();
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
        while ((c.getParent() != null) && !(c instanceof Window)) {
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
                    logger.log(Level.WARNING, "Couldn''t compute pathname for {0}", c);
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
        if (c instanceof Window window) {
            WindowState windowState = (WindowState) state;
            if (windowState.fullScreen) {
                if (window instanceof Frame frame) {
                    SwingUtils.setFullScreen(frame, true);
                } 
            } else if (windowState.isDefined()) {
                putWindowNormalBounds(window, windowState.bounds);
                if (!window.isLocationByPlatform() && (state != null)) {

                    Rectangle gcBounds = windowState.gcBounds;
                    if (gcBounds != null && SwingUtils.isResizable(window)) {
                        if (computeVirtualGraphicsBounds().contains(gcBounds.getLocation())) {
                            window.setBounds(windowState.bounds);
                        } else {
                            window.setSize(windowState.bounds.getSize());
                        }
                    }
                    if (window instanceof Frame frame) {
                        frame.setExtendedState(windowState.frameState);

                    }
                }
            }
        } else if (c instanceof JTabbedPane p) {
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
        } else if (c instanceof JSplitPane p) {
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
        } else if (c instanceof JTable table) {
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
            if (c instanceof Frame frame) {
                frameState = frame.getExtendedState();
                fullScreen = SwingUtils.isFullScreen(frame);
            }
            GraphicsConfiguration gc = c.getGraphicsConfiguration();
            Rectangle gcBounds = (gc == null) ? null : gc.getBounds();
            Rectangle frameBounds = c.getBounds();
            if ((c instanceof JFrame frame) && (0 != (frameState & Frame.MAXIMIZED_BOTH))) {
                if (!(fullScreen)) {
                    Rectangle normalBounds = getWindowNormalBounds(frame);
                    if (normalBounds != null) {
                        frameBounds = normalBounds;
                    }
                }
            }
            return new WindowState(frameBounds, gcBounds, GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length, frameState, fullScreen);
        } else if (c instanceof JTabbedPane p) {
            int index = p.getSelectedIndex();
            if (index < 0) {
                return null;
            }
            return new TabbedPaneState(p.getTitleAt(index), index, p.getTabCount());
        } else if (c instanceof JSplitPane p) {
            return new SplitPaneState(p.getUI().getDividerLocation(p), p.getOrientation());
        } else if (c instanceof JTable table) {
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
        if (window instanceof JFrame frame) {
            Object res = frame.getRootPane().getClientProperty(WINDOW_STATE_NORMAL_BOUNDS);
            if (res instanceof Rectangle rect) {
                return rect;
            }
        }
        return null;
    }

    static void putWindowNormalBounds(Window window, Rectangle bounds) {
        if (window instanceof JFrame frame) {
            frame.getRootPane().putClientProperty(WINDOW_STATE_NORMAL_BOUNDS, bounds);
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
