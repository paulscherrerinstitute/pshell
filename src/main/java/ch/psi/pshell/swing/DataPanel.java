package ch.psi.pshell.swing;

import ch.psi.pshell.data.Converter;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Setup;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Provider;
import ch.psi.pshell.data.DataSlice;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.utils.Range;
import ch.psi.pshell.swing.DataPanel.DataPanelListener;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.ui.Processor;
import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.TextEditor;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 */
public final class DataPanel extends MonitoredPanel implements UpdatablePanel {

    /**
     * The listener interface for receiving data panel manager events.
     */
    public interface DataPanelListener {

        void plotData(DataManager dataManager, String root, String path) throws Exception;

        void plotData(Object array, Range range) throws Exception;

        void openFile(String fileName) throws Exception;

        void openScript(String script, String name) throws Exception;
    }

    JTable tableData;
    JTable tableRowHeader;
    JScrollPane scrollPaneTableData;
    ValueSelection pageSelection;
    String[] fieldNames = null;
    DataPanelListener listener;

    public DataPanel() {
        initComponents();
        if (MainFrame.isDark()) {
            treeFolder.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            treeFile.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        }
        treeFolder.addTreeSelectionListener((TreeSelectionEvent event) -> {
            setCurrentFilePath(null);
            showData(null);
            File file = (File) treeFolder.getLastSelectedPathComponent();
            showFileProps(file, false);
            if (file != null) {
                try {
                    if (dataManager.isDataPacked()) {
                        if (file.isFile()) {
                            setCurrentPath(file);
                        } else {
                            setCurrentPath(null);
                        }
                    } else {
                        //String pathName = file.getCanonicalPath();
                        String pathName = file.getPath();
                        if ((dataManager.isRoot(pathName))) { // || (IO.listFiles(pathName, "*." + dataManager.getDataFileType()).length > 0) 
                            setCurrentPath(file);
                        } else {
                            setCurrentPath(null);
                        }
                    }
                } catch (Exception ex) {
                    textProperties.append("\n" + ex.getMessage(), Shell.ERROR_COLOR);
                    textProperties.setCaretPosition(0);
                }
            }
        });

        JPopupMenu filePopupMenu = new JPopupMenu();
        JMenuItem menuOpen = new JMenuItem("Open");
        menuOpen.addActionListener((ActionEvent e) -> {
            try {
                File selected = getFolderTreeSelectedFile();
                if (selected != null) {
                    Logger.getLogger(DataPanel.class.getName()).fine("Opening: " + String.valueOf(selected));
                    if (listener != null) {
                        listener.openFile(selected.getCanonicalPath());
                    }
                }
            } catch (Exception ex) {
                SwingUtils.showException(DataPanel.this, ex);
            }
        });
        filePopupMenu.add(menuOpen);

        JMenuItem menuBrowse = new JMenuItem("");
        menuBrowse.addActionListener((ActionEvent e) -> {
            try {
                File selected = getFolderTreeSelectedFile();
                if (selected != null) {
                    Logger.getLogger(DataPanel.class.getName()).fine("Opening desktop for: " + String.valueOf(selected));
                    Desktop.getDesktop().open(selected);
                }
            } catch (Exception ex) {
                SwingUtils.showException(DataPanel.this, ex);
            }
        });
        filePopupMenu.add(menuBrowse);

        JMenu menuFileOrder = new JMenu("File order");
        for (FileOrder fo : FileOrder.values()) {
            if (fo != FileOrder.System) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(fo.toString());
                item.addActionListener((ActionEvent e) -> {
                    if (fo != getFileOrder()) {
                        setFileOrder(fo);
                        repaintTreePath(treeFolder.getSelectionPath(), true);
                    }
                });
                menuFileOrder.add(item);
            }
        }

        menuFileOrder.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                for (Component item : menuFileOrder.getMenuComponents()) {
                    ((JMenuItem) item).setSelected(getFileOrder().toString().equals(((JMenuItem) item).getText()));
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
        filePopupMenu.add(menuFileOrder);

        JMenuItem menuRefresh = new JMenuItem("Refresh");
        menuRefresh.addActionListener((ActionEvent e) -> {
            try {
                repaintTreePath(treeFolder.getSelectionPath(), true);
            } catch (Exception ex) {
                SwingUtils.showException(DataPanel.this, ex);
            }
        });
        filePopupMenu.add(menuRefresh);

        JMenuItem menuCalcSize = new JMenuItem("Calculate size");
        menuCalcSize.addActionListener((ActionEvent e) -> {
            try {
                File selected = getFolderTreeSelectedFile();
                showFileProps(selected, true);
            } catch (Exception ex) {
                SwingUtils.showException(DataPanel.this, ex);
            }
        });
        filePopupMenu.add(menuCalcSize);

        treeFolder.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        TreePath path = treeFolder.getPathForLocation(e.getX(), e.getY());
                        treeFolder.setSelectionPath(path);

                        boolean isRoot = false;
                        File file = (File) treeFolder.getLastSelectedPathComponent();
                        if (file != null) {
                            if (dataManager.isDataPacked()) {
                                isRoot = dataManager.getDataFileType().equals(IO.getExtension(file));
                            } else {
                                isRoot = dataManager.isRoot(file.getPath()) && file.isDirectory();
                            }
                        }
                        File selected = getFolderTreeSelectedFile();
                        if (selected != null) {
                            menuOpen.setVisible(isRoot);
                            menuBrowse.setText(selected.isDirectory() ? "Browse folder" : "Open external editor");
                            menuBrowse.setVisible(isRoot || selected.isDirectory());
                            menuCalcSize.setVisible(selected.isDirectory() && (path.getPathCount() > 1));
                            menuRefresh.setVisible(selected.isDirectory());
                            menuFileOrder.setVisible(path.getPathCount() == 1);
                            filePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(DataPanel.this, ex);
                }
            }

        });

        treeFile.addTreeSelectionListener((TreeSelectionEvent event) -> {
            if (!updatingCurrentFile) {
                showData(null);
                TreePath tp = treeFile.getSelectionPath();
                String path = getDataPath(tp);
                if (tp != null) {
                    setCurrentFilePath(path);
                }
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuPlotData = new JMenuItem("Plot data");
        JMenu menuConvert = new JMenu("Convert");
        menuPlotData.addActionListener((ActionEvent e) -> {
            try {
                TreePath path = treeFile.getSelectionPath();
                String dataPath = getDataPath(path);
                if (listener != null) {
                    listener.plotData(dataManager, currentFile.getPath(), dataPath);
                }
            } catch (Exception ex) {
                SwingUtils.showException(DataPanel.this, ex);
            }
        });
        popupMenu.add(menuPlotData);

        Separator menuPlotDataSeparator = new Separator();
        popupMenu.add(menuPlotDataSeparator);
        
        popupMenu.add(menuConvert);       
        Separator menuConvertSeparator = new Separator();
        popupMenu.add(menuConvertSeparator);
        
        JMenuItem menuCopyLink = new JMenuItem("Copy link to clipboard");
        menuCopyLink.addActionListener((ActionEvent e) -> {
            try {
                Context context = Context.getInstance();
                TreePath path = treeFile.getSelectionPath();
                String root = currentFile.getPath();
                String dataPath = getDataPath(path);
                if (dataPath == null) {
                    dataPath = "/";
                }

                if (IO.isSubPath(root, context.getSetup().getDataPath())) {
                    root = IO.getRelativePath(root, context.getSetup().getDataPath());
                } else {
                    root = root.replace("\\", "\\\\");
                }
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("\"" + root + "|" + dataPath + "\""), null);
            } catch (Exception ex) {
                SwingUtils.showException(DataPanel.this, ex);
            }
        });
        popupMenu.add(menuCopyLink);

        JMenuItem menuOpenScript = new JMenuItem("Open producing script");
        menuOpenScript.addActionListener((ActionEvent e) -> {
            if (listener != null) {
                try {

                    String fileName = (String) dataManager.getAttribute(currentFile.getPath(), "/", Layout.ATTR_FILE);
                    String revision = (String) dataManager.getAttribute(currentFile.getPath(), "/", Layout.ATTR_VERSION);
                    if (revision != null) {
                        try {
                            String script = Context.getInstance().getFileContents(fileName, revision);
                            listener.openScript(script, new File(fileName).getName() + "_" + revision.substring(0, Math.min(8, revision.length())));
                            return;
                        } catch (Exception ex) {
                        }
                    }
                    listener.openFile(fileName);
                } catch (Exception ex) {
                    SwingUtils.showException(DataPanel.this, ex);
                }
            }
        });
        popupMenu.add(menuOpenScript);

        JMenuItem menuAssign = new JMenuItem("Assign to variable");
        menuAssign.addActionListener((ActionEvent e) -> {
            try {
                Context context = Context.getInstance();

                TreePath path = treeFile.getSelectionPath();
                String root = currentFile.getPath();
                String dataPath = getDataPath(path);

                if (IO.isSubPath(root, context.getSetup().getDataPath())) {
                    root = IO.getRelativePath(root, context.getSetup().getDataPath());
                } else {
                    root = root.replace("\\", "\\\\");
                }
                String var = SwingUtils.getString(this, "Enter variable name:", null);
                if ((var != null) && (!var.trim().isEmpty())) {
                    context.evalLineBackground(var.trim() + "=load_data(\"" + root + "|" + dataPath + "\")");
                }
            } catch (Exception ex) {
                SwingUtils.showException(DataPanel.this, ex);
            }
        });
        popupMenu.add(menuAssign);

        treeFile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if (e.getClickCount() == 2) {
                        TreePath tp = treeFile.getPathForLocation(e.getX(), e.getY());
                        String path = getDataPath(tp);
                        if (path != null) {
                            showData(path);
                        }
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(DataPanel.this, ex);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        TreePath path = treeFile.getPathForLocation(e.getX(), e.getY());
                        treeFile.setSelectionPath(path);
                        String dataPath = getDataPath(path);
                        Map<String, Object> info = dataManager.getInfo(currentFile.getPath(), dataPath);
                        menuPlotData.setVisible(false);
                        menuAssign.setVisible(false);
                        menuConvert.setVisible(false);
                        if (info != null) {
                            String type = String.valueOf(info.get(Provider.INFO_TYPE));
                            if ((type.equals(Provider.INFO_VAL_TYPE_GROUP))) {
                                for (String child : dataManager.getChildren(currentFile.getPath(), dataPath)) {
                                    info = dataManager.getInfo(currentFile.getPath(), child);
                                    if (dataManager.isDisplayablePlot(info)) {
                                        menuPlotData.setVisible(true);
                                        break;
                                    }
                                }
                            } else if (type.equals(Provider.INFO_VAL_TYPE_DATASET)) {
                                menuAssign.setVisible(Context.getInstance().getScriptManager() != null);
                                if (dataManager.isDisplayablePlot(info)) {
                                    menuConvert.removeAll();
                                    for (Converter converter : Converter.getServiceProviders()){
                                        JMenuItem item = new JMenuItem(converter.getName());                                        
                                        item.addActionListener((a)->{    
                                            TreePath tp = treeFile.getSelectionPath();
                                            converter.startConvert(dataManager, currentFile.getPath(),  getDataPath(tp), DataPanel.this).handle((ret,ex)->{
                                                if (ex != null){
                                                    SwingUtils.showException(DataPanel.this, (Exception) ex);
                                                } else{
                                                    SwingUtils.showMessage(DataPanel.this, "Success", "Success creating:\n" + String.valueOf(ret));
                                                }
                                                return ret;
                                            });
                                        });
                                        menuConvert.add(item);
                                    }
                                    
                                    menuPlotData.setVisible(true);
                                    menuConvert.setVisible(menuConvert.getMenuComponentCount()>0);
                                }
                            }
                        }
                        menuOpenScript.setVisible((info != null) && "/".equals(dataPath) && (dataManager.getAttribute(currentFile.getPath(), dataPath, Layout.ATTR_FILE) != null));
                        menuPlotDataSeparator.setVisible(menuPlotData.isVisible());
                        menuConvertSeparator.setVisible(menuConvert.isVisible());
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(DataPanel.this, ex);
                }
            }

        });

        setBaseFolder(null);
        try {
            setCurrentPath(null);
        } catch (IOException ex) {
        }

        //To set a tab size on JTextPane
        //Create data table with row header
        tableData = new JTable();
        tableData.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        tableRowHeader = new JTable();
        tableRowHeader.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableRowHeader.setFocusable(false);
        tableRowHeader.setRowSelectionAllowed(false);

        scrollPaneTableData = new JScrollPane(tableData);

        JViewport viewport = new JViewport();
        viewport.setView(tableRowHeader);
        viewport.setPreferredSize(new Dimension(75, 100000000));

        scrollPaneTableData.setCorner(JScrollPane.UPPER_LEFT_CORNER, tableRowHeader.getTableHeader());
        scrollPaneTableData.setRowHeaderView(viewport);

        pageSelection = new ValueSelection();
        pageSelection.setMinValue(0);
        pageSelection.setMaxValue(10);
        pageSelection.setStep(1);
        pageSelection.setDecimals(0);
        pageSelection.setValue(0);
        pageSelection.addListener((ValueSelection origin, double value, boolean editing) -> {
            onPageChange((int) value);
        });

        tablePanel.add(scrollPaneTableData, BorderLayout.CENTER);
        tablePanel.add(pageSelection, BorderLayout.SOUTH);
        pageSelection.setVisible(false);

        ((CardLayout) dataPanel.getLayout()).show(dataPanel, "table");

        ((DefaultTreeCellRenderer) treeFile.getCellRenderer()).setLeafIcon(new ImageIcon(App.getResourceImage("Data.png")));
    }

    public File getFolderTreeSelectedFile() {
        TreePath selection = treeFolder.getSelectionPath();
        if (selection != null) {
            Object[] treePath = selection.getPath();
            if (treePath.length > 0) {
                Path path = Paths.get(baseFolder);
                for (int i = 1; i < treePath.length; i++) {
                    path = path.resolve(treePath[i].toString());
                }
                File ret = path.toFile();
                if (ret.exists()) {
                    return path.toFile();
                }
            }
        }
        return null;
    }

    DataManager dataManager;
    WatchService watchService;
    FileSystemModel treeFolderModel;
    String baseFolder;

    public String getDataPath(TreePath treePath) {
        if (treePath == null) {
            return null;
        }
        Object[] path = treePath.getPath();
        String ret = "/";
        for (int i = 1; i < path.length; i++) {
            ret += path[i];
            if (i < (path.length - 1)) {
                ret += "/";
            }
        }
        return ret;
    }

    public void setListener(DataPanelListener listener) {
        this.listener = listener;
    }

    public void initialize() {
        dataManager = Context.getInstance().getDataManager();
        setBaseFolder(dataManager.getDataFolder());
        try {
            setCurrentPath(null);
        } catch (IOException ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        update();
    }

    //Filesystem tree    
    void registerFolderEvents(String name) {
        try {
            if (watchService != null) {
                Paths.get(name).register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE
                //StandardWatchEventKinds.ENTRY_MODIFY
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Override
    public void update() {
        WatchKey watchKey = watchService.poll();
        if (watchKey == null) {
            return;
        }
        ArrayList<Path> updatedFolders = new ArrayList<>();
        try {
            List<WatchEvent<?>> keys = watchKey.pollEvents();
            for (WatchEvent<?> watchEvent : keys) {
                Kind<?> watchEventKind = watchEvent.kind();
                Path path = ((Path) watchEvent.context());
                Path dir = (Path) watchKey.watchable();
                Path fullPath = dir.resolve((Path) watchEvent.context());

                if ((watchEventKind == StandardWatchEventKinds.ENTRY_DELETE)
                        || (watchEventKind == StandardWatchEventKinds.ENTRY_CREATE)) {
                    if (isCached() && treeFolderModel.curChildren.keySet().contains(dir.toFile())) {
                        File[] children = treeFolderModel.curChildren.get(dir.toFile());
                        if (watchEventKind == StandardWatchEventKinds.ENTRY_DELETE) {
                            children = Arr.removeEquals(children, fullPath.toFile());
                        } else if (watchEventKind == StandardWatchEventKinds.ENTRY_CREATE) {
                            children = Arr.append(children, fullPath.toFile());
                        }
                        treeFolderModel.curChildren.put(dir.toFile(), children);
                        repaintTreeFolder(dir, false);
                    } else {
                        if (!updatedFolders.contains(dir)) {
                            updatedFolders.add(dir);
                        }
                    }
                }
            }
            for (Path dir : updatedFolders) {
                repaintTreeFolder(dir, true);
            }
        } catch (Exception ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
        }
        watchKey.reset();
    }

    boolean updatingCurrentFile;

    //Update current file contents if selected
    public void onScanEnded() {
        if ((currentFile != null) && (dataManager.isOpen())) {
            if (currentFile.toString().equals(new File(dataManager.getOutput()).getName())) {
                updatingCurrentFile = true;
                try {
                    updateFileTree();
                } catch (Exception ex) {
                    Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
                } finally {
                    updatingCurrentFile = false;
                }
            }
        }
    }

    void setBaseFolder(String path) {
        if ((path == null) || !path.equals(baseFolder)) {
            try {
                if (watchService != null) {
                    watchService.close();
                    watchService = null;
                }
            } catch (Exception ex) {
                Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
            }
            baseFolder = path;
            treeFolderModel = null;
            //DefaultMutableTreeNode node = new DefaultMutableTreeNode("");
            treeFolder.setModel(new DefaultTreeModel(null));
            if (baseFolder != null) {
                try {
                    treeFolderModel = new FileSystemModel(baseFolder);
                    treeFolder.setModel(treeFolderModel);
                    watchService = FileSystems.getDefault().newWatchService();
                    registerFolderEvents(baseFolder);

                } catch (Exception ex) {
                    baseFolder = null;
                    Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
    }

    public void repaintTreeFolder(Path path, boolean invalidate) {
        String pathName = path.toFile().getPath();
        String base = treeFolderModel.root.getPath();
        String relative = (new File(base).toURI().relativize(new File(pathName).toURI())).getPath();

        ArrayList pathList = new ArrayList<>();

        pathList.add(treeFolderModel.root);
        File parent = treeFolderModel.root;
        for (String str : relative.split("/")) {
            if (!str.isEmpty()) {
                pathList.add(new TreeFile(parent, str));
                parent = Paths.get(parent.getPath(), str).toFile();
            }
        }
        repaintTreePath(new TreePath(pathList.toArray()), invalidate);
    }

    public void repaintTreePath(TreePath path, boolean invalidate) {
        if (invalidate) {
            treeFolderModel.invalidate((File) path.getLastPathComponent());
        }
        int childrenCount = treeFolderModel.getChildCount(path.getLastPathComponent());
        int[] changedChildrenIndices = new int[childrenCount];
        Object[] changedChildren = {};
        for (int i = 0; i < childrenCount; i++) {
            changedChildrenIndices[i] = i;
        }
        //TODO: collapsing all internal folders
        treeFolderModel.fireTreeStructureChanged(path, changedChildrenIndices, changedChildren);
    }

    boolean cached = true;

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean value) {
        if (value != cached) {
            cached = value;
            if (!cached) {
                if (treeFolderModel != null) {
                    treeFolderModel.invalidate();
                }
            }
        }
    }

    FileOrder fileOrder = FileOrder.Name;

    public enum FileOrder {
        Name,
        Modified,
        System;
    }

    public void setFileOrder(FileOrder fileOrder) {
        if (fileOrder == null) {
            fileOrder = FileOrder.System;
        }
        if (this.fileOrder != fileOrder) {
            this.fileOrder = fileOrder;
            if (treeFolderModel != null) {
                treeFolderModel.invalidate();
            }
        }
    }

    public FileOrder getFileOrder() {
        return fileOrder;
    }

    class FileSystemModel implements TreeModel {

        private File root;

        final ArrayList<TreeModelListener> listeners;

        public FileSystemModel(String root) {
            this.root = new File(root);
            listeners = new ArrayList<>();
        }

        @Override
        public Object getRoot() {
            return root;
        }

        public void invalidate() {
            curChildren.clear();
        }

        public void invalidate(File file) {
            curChildren.remove(file);
            for (File f : curChildren.keySet().toArray(new File[0])) {
                if (f.toPath().startsWith(file.toPath())) {
                    curChildren.remove(f);
                }
            }

        }

        final HashMap<File, File[]> curChildren = new LinkedHashMap<>();

        public File[] getChildren(Object parent) {
            File f = (File) parent;
            if (curChildren.keySet().contains(f)) {
                File[] ret = curChildren.get(f);
                return (ret == null) ? new File[0] : ret;
            }
            if (!isCached()) {
                for (File x : curChildren.keySet().toArray(new File[0])) {
                    if (!f.toPath().startsWith(x.toPath())) {
                        curChildren.remove(x);
                    }
                }
            }
            File[] ret = new File[0];
            if (f.isDirectory() && (dataManager != null)) {
                File[] files = IO.listFiles(f, dataManager.getFileFilter());

                switch (fileOrder) {
                    case Modified:
                        IO.orderByModified(files);
                        break;
                    case Name:
                        IO.orderByName(files);
                        break;
                }
                ret = files;
                //TODO: Is there way to check if has been regestered already?
                registerFolderEvents(((File) parent).getPath());
            }
            curChildren.put(f, ret);
            return ret;
        }

        @Override
        public Object getChild(Object parent, int index) {
            File[] children = getChildren(parent);
            return new TreeFile((File) parent, children[index]);
        }

        @Override
        public int getChildCount(Object parent) {
            return getChildren(parent).length;
        }

        @Override
        public boolean isLeaf(Object node) {
            return ((File) node).isFile();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            File[] children = getChildren(parent);
            for (int i = 0; i < children.length; i++) {
                if (((File) child).getName().equals(children[i])) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object value) {
            File oldFile = (File) path.getLastPathComponent();
            String fileParentPath = oldFile.getParent();
            String newFileName = (String) value;
            File targetFile = new File(fileParentPath, newFileName);
            oldFile.renameTo(targetFile);
            File parent = new File(fileParentPath);
            int[] changedChildrenIndices = {getIndexOfChild(parent, targetFile)};
            Object[] changedChildren = {targetFile};
            fireTreeNodesChanged(path.getParentPath(), changedChildrenIndices, changedChildren);
        }

        @Override
        public void addTreeModelListener(TreeModelListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener listener) {
            listeners.remove(listener);
        }

        private void fireTreeNodesChanged(TreePath parentPath, int[] indices, Object[] children) {
            TreeModelEvent event = new TreeModelEvent(this, parentPath, indices, children);
            for (TreeModelListener listener : listeners) {
                listener.treeNodesChanged(event);
            }
        }

        private void fireTreeNodesInserted(TreePath parentPath, int[] indices, Object[] children) {
            TreeModelEvent event = new TreeModelEvent(this, parentPath, indices, children);
            for (TreeModelListener listener : listeners) {
                listener.treeNodesInserted(event);
            }
        }

        private void fireTreeNodesRemoved(TreePath parentPath, int[] indices, Object[] children) {
            TreeModelEvent event = new TreeModelEvent(this, parentPath, indices, children);
            for (TreeModelListener listener : listeners) {
                listener.treeNodesRemoved(event);
            }
        }

        private void fireTreeStructureChanged(TreePath parentPath, int[] indices, Object[] children) {
            TreeModelEvent event = new TreeModelEvent(this, parentPath, indices, children);
            for (TreeModelListener listener : listeners) {
                listener.treeStructureChanged(event);
            }
        }

    }

    private class TreeFile extends File {

        final File parent;

        public TreeFile(File parent, String child) {
            super(parent, child);
            this.parent = parent;
        }

        public TreeFile(File parent, File child) {
            this(parent, child.getName());
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    void showFileProps(File file, boolean calculateFolderSize) {
        textProperties.clear();
        if ((file != null) && (treeFolderModel != null)) {
            if (!file.equals(treeFolderModel.root)) { //Not to compute the whole folder size
                textProperties.append("Type:     " + (file.isDirectory() ? "Folder" : IO.getExtension(file)) + "\n", Shell.STDOUT_COLOR);
                try {
                    BasicFileAttributes attr = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
                    textProperties.append("Creation: " + Chrono.getTimeStr(attr.creationTime().toMillis(), "dd/MM/YY HH:mm\n"), Shell.STDOUT_COLOR);
                    textProperties.append("Accessed: " + Chrono.getTimeStr(attr.lastAccessTime().toMillis(), "dd/MM/YY HH:mm\n"), Shell.STDOUT_COLOR);
                    textProperties.append("Modified: " + Chrono.getTimeStr(attr.lastModifiedTime().toMillis(), "dd/MM/YY HH:mm\n"), Shell.STDOUT_COLOR);
                } catch (Exception ex) {
                }
                if (file.isFile() || calculateFolderSize) {
                    textProperties.append("Size:     " + IO.getSize(file) / 1024 + "KB\n", Shell.STDOUT_COLOR);
                }
            }
            textProperties.setCaretPosition(0);
        }
    }

    File currentFile;

    void setCurrentPath(File f) throws IOException {
        if ((currentFile != f) || (f == null)) {
            currentFile = f;
            updateFileTree();
        }
    }

    void updateFileTree() throws IOException {
        treeFile.setModel(new DefaultTreeModel(null));
        if ((currentFile != null) && (dataManager != null)) {
            Object[] data = dataManager.getStructure(currentFile.getPath());
            setTreeData(treeFile, data);
        }
    }

    void setTreeData(JTree tree, Object[] data) {
        tree.setRootVisible(data != null && (data.length > 0));
        DefaultMutableTreeNode root = createTreeNode(data);
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
    }

    DefaultMutableTreeNode createTreeNode(Object[] data) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(data[0]);
        DefaultMutableTreeNode child;
        for (int i = 1; i < data.length; i++) {
            Object nodeSpecifier = data[i];
            if (nodeSpecifier instanceof Object[]) {
                child = createTreeNode((Object[]) nodeSpecifier);
            } else {
                child = new DefaultMutableTreeNode(nodeSpecifier);
            }
            node.add(child);
        }
        return (node);
    }

    void setCurrentFilePath(String path) {
        textProperties.clear();
        if ((currentFile != null) && (path != null)) {
            Map<String, Object> info = dataManager.getInfo(currentFile.getPath(), path);
            for (String key : info.keySet()) {
                Color c = Shell.STDOUT_COLOR;
                if (key.equals("Exception")) {
                    c = Shell.ERROR_COLOR;
                }
                textProperties.append(key + " = " + Str.toString(info.get(key), 100) + "\n", c);
            }

            Map<String, Object> attrs = dataManager.getAttributes(currentFile.getPath(), path);
            if (attrs.keySet().size() > 0) {
                textProperties.append("\nAttributes:\n", Shell.INPUT_COLOR);
                for (String key : attrs.keySet()) {
                    Color c = Shell.INPUT_COLOR;
                    if (key.equals("Exception")) {
                        c = Shell.ERROR_COLOR;
                    }
                    textProperties.append(key + " = " + Str.toString(attrs.get(key), 100) + "\n", c);
                }
            }
            try {
                fieldNames = (String[]) info.get(Provider.INFO_FIELD_NAMES);
            } catch (Exception ex) {
                fieldNames = null;
            }
            textProperties.setCaretPosition(0);
        }
    }

    //Data
    DataSlice dataSlice;
    Object data;
    String[] headers;

    void showData(String path) {
        tableData.setModel(new DefaultTableModel(new Object[][]{}, new String[]{}));
        tableRowHeader.setModel(new DefaultTableModel(new Object[][]{}, new String[]{}));
        tableData.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        data = null;
        dataSlice = null;
        pageSelection.setVisible(false);
        headers = null;
        if ((path != null) && (dataManager != null)) {
            try {
                if (dataManager.isGroup(currentFile.getPath(), path)) {
                    return;
                }

                DataSlice dataSlice = dataManager.getData(currentFile.getPath(), path);
                if (dataSlice != null) {
                    this.dataSlice = dataSlice;
                    this.data = dataSlice.sliceData;
                    if (dataSlice.isCompound()) {
                        headers = fieldNames;
                    }
                    tableData.setModel(tableDataModel);
                    tableRowHeader.setModel(tableRowHeaderModel);
                    tableRowHeader.getColumnModel().getColumn(0).setCellRenderer(tableRowHeaderColumnRenderer);
                    int width = tableRowHeader.getColumnModel().getColumn(0).getPreferredWidth() * tableDataModel.getColumnCount();

                    if (width < scrollPaneTableData.getViewport().getWidth()) {
                        tableData.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                    }
                    if (dataSlice.dataRank == 3) {
                        pageSelection.setValue(0);
                        pageSelection.setMaxValue(dataSlice.getNumberSlices() - 1);
                        pageSelection.setVisible(true);
                    }
                }
            } catch (Exception ex) {
                data = new String[]{ex.getMessage()};
                headers = new String[]{"Exception"};
                tableData.setModel(tableDataModel);
                tableRowHeader.setModel(tableRowHeaderModel);
                tableRowHeader.getColumnModel().getColumn(0).setCellRenderer(tableRowHeaderColumnRenderer);
                tableData.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            }
            tableData.getTableHeader().setReorderingAllowed(false);
            tableRowHeader.getTableHeader().setReorderingAllowed(false);

            JPopupMenu tableDataPopupMenu = new JPopupMenu();
            JMenuItem menuPlot = new JMenuItem("Plot data");
            menuPlot.addActionListener((ActionEvent e) -> {
                plotTableSelection();
            });
            tableDataPopupMenu.add(menuPlot);

            tableData.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                        plotTableSelection();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    checkPopup(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    checkPopup(e);
                }

                private void checkPopup(MouseEvent e) {
                    try {
                        if (e.isPopupTrigger()) {
                            int r = tableData.rowAtPoint(e.getPoint());
                            if (r >= 0 && r < tableData.getRowCount()) {
                                Object selection = getTableSelection();
                                if (tableData.getSelectedRowCount() == 1) {
                                    menuPlot.setText("Plot row");
                                } else if ((tableData.getSelectedRowCount() == 0) || (tableData.getSelectedRowCount() >= tableData.getRowCount())) {
                                    menuPlot.setText("Plot data");
                                } else {
                                    menuPlot.setText("Plot row selection");
                                }
                                menuPlot.setEnabled(isTableSelectionPlottable(selection));
                                tableDataPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    } catch (Exception ex) {
                        SwingUtils.showException(DataPanel.this, ex);
                    }
                }
            });

            tableData.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                        int c = tableData.columnAtPoint(e.getPoint());
                        if (c >= 0 && c < tableData.getColumnCount()) {
                            plotColumnData(c);
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    checkPopup(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    checkPopup(e);
                }

                private void checkPopup(MouseEvent e) {
                    try {
                        if (e.isPopupTrigger()) {
                            int c = tableData.columnAtPoint(e.getPoint());
                            if (c >= 0 && c < tableData.getColumnCount()) {
                                JPopupMenu tableDataColPopupMenu = new JPopupMenu();
                                JMenuItem menuPlotCol = new JMenuItem("Plot column");
                                menuPlotCol.addActionListener((ActionEvent ev) -> {
                                    plotColumnData(c);
                                });
                                tableDataColPopupMenu.add(menuPlotCol);
                                menuPlotCol.setEnabled(isTableSelectionPlottable(getColumnSelection(c)));
                                tableDataColPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    } catch (Exception ex) {
                        SwingUtils.showException(DataPanel.this, ex);
                    }
                }
            });
        }
    }

    void plotColumnData(int column) {
        try {
            Object selection = getColumnSelection(column);
            if (isTableSelectionPlottable(selection)) {
                if (listener != null) {
                    listener.plotData(selection, null);
                }
            }
        } catch (Exception ex) {
            SwingUtils.showException(DataPanel.this, ex);
        }
    }

    void plotTableSelection() {
        try {
            Object selection = getTableSelection();
            if (isTableSelectionPlottable(selection)) {
                Range range = getTableSelectionRange();
                //Simplify plotting if  showing only 1 element
                if (listener != null) {
                    listener.plotData(selection, range);
                }
            }
        } catch (Exception ex) {
            SwingUtils.showException(DataPanel.this, ex);
        }
    }

    Object getTableSelection() {
        if ((data != null) && (data.getClass().isArray()) && Array.getLength(data) > 0) {
            Object selection = data;
            int lenght = Array.getLength(data);
            int start = tableData.getSelectedRow();
            if (start >= 0) {
                int end = Math.min(tableData.getSelectedRow() + tableData.getSelectedRowCount(), lenght);
                selection = Array.newInstance(data.getClass().getComponentType(), end - start);
                System.arraycopy(data, start, selection, 0, Array.getLength(selection));
            }
            if (Array.getLength(selection) == 1) {
                selection = Array.get(selection, 0);
            }
            return selection;
        }
        return null;
    }

    Object getColumnSelection(int column) {
        if ((data != null) && (data.getClass().isArray()) && Array.getLength(data) > 0) {
            int lenght = Array.getLength(data);
            int rank = Arr.getRank(data);
            if (rank == 1) {
                if (lenght == tableData.getRowCount()) {
                    return data;
                }
            } else {
                Class type = Array.get(Array.get(data, 0), column).getClass();
                Object selection = Array.newInstance(type, lenght);

                for (int i = 0; i < lenght; i++) {
                    Array.set(selection, i, Array.get(Array.get(data, i), column));
                }
                return selection;
            }
        }
        return null;
    }

    Range getTableSelectionRange() {
        Range range = null;
        if ((data != null) && (data.getClass().isArray()) && Array.getLength(data) > 0) {
            int lenght = Array.getLength(data);
            int start = tableData.getSelectedRow();
            if (start >= 0) {
                int end = Math.min(tableData.getSelectedRow() + tableData.getSelectedRowCount(), lenght);
                range = new Range((double) start, (double) end);
            }
        }
        return range;
    }

    boolean isTableSelectionPlottable(Object array) {
        if (Arr.getRank(array) == 0) {
            return false;
        }
        Class type = Arr.getComponentType(array);
        if (type.isPrimitive()) {
            type = Convert.getWrapperClass(type);
        }
        if (Array.getLength(array) == 1) {
            array = Array.get(array, 0);
        }
        if ((type == null)) {
            return false;
        }
        return (Number.class.isAssignableFrom(type) || (type == Boolean.class));
    }

    final TableModel tableDataModel = new AbstractTableModel() {
        @Override
        public int getColumnCount() {
            //TODO: Do it based on data info
            if (getRowCount() == 0) {
                return 0;
            }
            if (!data.getClass().isArray()) {
                return 1;
            }
            Object element = Array.get(data, 0);
            if (element == null){
                return 1;
            }
            if (!element.getClass().isArray()) {
                return 1;
            }
            return Array.getLength(element);
        }

        @Override
        public String getColumnName(int column) {
            if (headers != null) {
                if (column < headers.length) {
                    return headers[column];
                }
                return "";
            }
            return String.valueOf(column);
        }

        @Override
        public int getRowCount() {
            //TODO: Do it based on data info
            if (data == null) {
                return 0;
            }
            if (!data.getClass().isArray()) {
                return 1;
            }
            return Array.getLength(data);
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (data != null) {
                if (!data.getClass().isArray()) {
                    return data;
                }
                if (row < Array.getLength(data)) {
                    Object element = Array.get(data, row);
                    if ((dataSlice != null) && (dataSlice.unsigned)) {
                        element = Convert.toUnsigned(element);
                    }

                    if (!element.getClass().isArray()) {
                        return element;
                    }
                    if (column < Array.getLength(element)) {
                        Object ret = Array.get(element, column);
                        if (ret != null) {
                            if (ret.getClass().isArray()) {
                                ret = Convert.arrayToString(ret, " ");
                            }
                        }
                        return ret;
                    }
                }
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

    };

    final TableModel tableRowHeaderModel = new AbstractTableModel() {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public int getRowCount() {
            if (data == null) {
                return 0;
            }
            if (!data.getClass().isArray()) {
                return 1;
            }
            return Array.getLength(data);
        }

        @Override
        public Object getValueAt(int row, int column) {
            return row;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

    };

    DefaultTableCellRenderer tableRowHeaderColumnRenderer = new DefaultTableCellRenderer() {
        //Color backcolor = javax.swing.UIManager.getDefaults().getColor("Table.alternateRowColor");
        Color forecolor = Color.GRAY;

        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //cellComponent.setBackground(backcolor);            
            cellComponent.setForeground(forecolor);
            return cellComponent;
        }
    };

    void onPageChange(int page) {
        try {
            if ((dataSlice != null) && (dataSlice.dataRank == 3) && (pageSelection.isVisible())) {
                //tableData.setModel(new DefaultTableModel(new Object[][]{}, new String[]{}));
                DataSlice dataSlice = dataManager.getData(this.dataSlice.dataFile, this.dataSlice.dataPath, page);
                this.data = dataSlice.sliceData;
                //tablePanel.update(tablePanel.getGraphics());
                tableData.repaint();
                tableRowHeader.getTableHeader().repaint();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }

    String fileName;

    public void load(String fileName) throws Exception {
        File file = new File(fileName);
        this.fileName = fileName;
        scrollFolder.setVisible(false);
        splitFolder.setDividerSize(0);
        initialize();
        String textProvider = Context.getInstance().getConfig().getDataProvider().equalsIgnoreCase("csv") ? "csv" : "txt";
        dataManager = new DataManager(Context.getInstance(), file.isDirectory() ? textProvider : IO.getExtension(file), Context.getInstance().getConfig().getDataLayout());
        setCurrentPath(file);
    }

    class DefaultDataPanelListener implements DataPanelListener {

        Window getParent() {
            return SwingUtils.getWindow(DataPanel.this);
        }

        @Override
        public void plotData(DataManager dataManager, String root, String path) throws Exception {
            ViewPreference.PlotPreferences prefs = dataManager.getPlotPreferences(root, path);
            try{
                plot(getParent(), dataManager.getFullPath(root, path), dataManager.getScanPlots(root, path).toArray(new PlotDescriptor[0]), prefs);
            } catch (IOException ex) {
                //If cannot open file, try with external processors
                if (!Processor.checkProcessorsPlotting(root, path, dataManager)){
                    throw ex;
                }
            }
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
            plot(getParent(), (range == null) ? "Array" : "Array range: " + range.toString(), new PlotDescriptor[]{new PlotDescriptor("", array, x)}, null);
        }

        @Override
        public void openFile(String fileName) throws Exception {
            if (IO.getExtension(fileName).equalsIgnoreCase(Context.getInstance().getScriptType().toString())) {
                openScript(new String(Files.readAllBytes(Paths.get(fileName))), fileName);
            } else {
                DataPanel panel = new DataPanel();
                panel.load(fileName);
                panel.setListener(this);
                SwingUtils.showDialog(getParent(), fileName, new Dimension(800, 600), panel);
            }
        }

        @Override
        public void openScript(String script, String name) throws Exception {
            TextEditor editor = new TextEditor();
            editor.setText((script == null) ? "" : script);
            editor.setReadOnly(true);
            SwingUtils.showDialog(getParent(), name, new Dimension(800, 600), editor);
        }

    }

    public void setDefaultDataPanelListener() {
        setListener(new DefaultDataPanelListener());
    }

    public static DataPanel create(File path) {
        DataPanel panel = new DataPanel();
        try {
            Context.createInstance();
            if ((path != null) && (path.exists())) {
                panel.load(path.getAbsolutePath());
            } else {
                panel.initialize();
            }
        } catch (Exception ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        panel.setDefaultDataPanelListener();
        return panel;
    }

    static Path getWindowStatePath(){
         return Paths.get(Context.getInstance().getSetup().getContextPath(), DataPanel.class.getSimpleName() + "_" + "WindowState.xml");
    }
    
    public static void createPanel(File path) { 
        java.awt.EventQueue.invokeLater(() -> {
            Context.createInstance();
            JFrame frame = new JFrame(App.getApplicationTitle());
            frame.setIconImage(App.getIconSmall());
            DataPanel panel = new DataPanel();
            frame.add(panel);
            frame.setSize(1000, 800);
            SwingUtils.centerComponent(null, frame);            
            //frame.pack();
            if (App.isDetachedPersisted()) {
                try {
                    MainFrame.restore(frame, getWindowStatePath());
                } catch (Exception ex) {
                    Logger.getLogger(DataPanel.class.getName()).log(Level.INFO, null, ex);
                }                 
            } 
            frame.setVisible(true);

            try {
                if ((path != null) && (path.exists())) {
                    panel.load(path.getAbsolutePath());
                    frame.setTitle(path.getCanonicalPath());
                } else {
                    panel.initialize();
                }
            } catch (Exception ex) {
                Logger.getLogger(DataPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            panel.setDefaultDataPanelListener();
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    if (App.isDetachedPersisted()) {
                        try {
                            MainFrame.save(frame, getWindowStatePath());
                        } catch (Exception ex) {
                            Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
                        }                            
                    }                    
                    System.exit(0);
                }
            });
        });
    }

    static List<Plot> plot(Window parent, String title, PlotDescriptor[] plots, ViewPreference.PlotPreferences preferences) throws Exception {
        ArrayList<Plot> ret = new ArrayList<>();
        PlotPanel plotPanel = new PlotPanel();
        plotPanel.initialize();
        plotPanel.setPlotTitle(title);
        plotPanel.clear();
        plotPanel.setPreferences((preferences == null) ? new ViewPreference.PlotPreferences() : preferences);
        plotPanel.clear();

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
                        Logger.getLogger(DataPanel.class.getName()).warning("Error creating plot " + String.valueOf((plot != null) ? plot.name : null) + ": " + ex.getMessage());
                    }
                }
            }
        }
        SwingUtils.showDialog(parent, title, null, plotPanel);
        return ret;
    }
    
    public int getSelectedFilesCount(){
        return treeFolder.getSelectionCount();
    }
    
    public List<String> getSelectedFiles(){
        List<String> ret = new ArrayList<>();
        for (TreePath tp : treeFolder.getSelectionPaths()){
            String file = getDataPath(tp);
            if (file.startsWith("/")) {
                file = file.substring(1);
            }
            ret.add(file);
        }
        return ret;                
    }

    public static void main(String args[]) {      
        App.init(args);
        createPanel(args.length > 1 ? new File(args[1]) : null);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        splitSource = new javax.swing.JSplitPane();
        splitFolder = new javax.swing.JSplitPane();
        scrollFolder = new javax.swing.JScrollPane();
        treeFolder = new javax.swing.JTree();
        splitFile = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        treeFile = new javax.swing.JTree();
        jScrollPane5 = new javax.swing.JScrollPane();
        textProperties = new ch.psi.pshell.swing.OutputTextPane();
        dataPanel = new javax.swing.JPanel();
        tablePanel = new javax.swing.JPanel();

        setPreferredSize(new java.awt.Dimension(600, 400));

        splitSource.setDividerLocation(300);
        splitSource.setResizeWeight(0.5);
        splitSource.setPreferredSize(new java.awt.Dimension(600, 400));

        splitFolder.setDividerLocation(150);
        splitFolder.setResizeWeight(0.5);

        scrollFolder.setPreferredSize(new java.awt.Dimension(150, 322));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        treeFolder.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        scrollFolder.setViewportView(treeFolder);

        splitFolder.setLeftComponent(scrollFolder);

        splitFile.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitFile.setResizeWeight(0.5);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        treeFile.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane2.setViewportView(treeFile);

        splitFile.setTopComponent(jScrollPane2);

        textProperties.setTextLength(10000);
        jScrollPane5.setViewportView(textProperties);

        splitFile.setBottomComponent(jScrollPane5);

        splitFolder.setRightComponent(splitFile);

        splitSource.setLeftComponent(splitFolder);

        dataPanel.setLayout(new java.awt.CardLayout());

        tablePanel.setLayout(new java.awt.BorderLayout());
        dataPanel.add(tablePanel, "table");

        splitSource.setRightComponent(dataPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(splitSource, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitSource, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel dataPanel;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane scrollFolder;
    private javax.swing.JSplitPane splitFile;
    private javax.swing.JSplitPane splitFolder;
    private javax.swing.JSplitPane splitSource;
    private javax.swing.JPanel tablePanel;
    private ch.psi.pshell.swing.OutputTextPane textProperties;
    private javax.swing.JTree treeFile;
    private javax.swing.JTree treeFolder;
    // End of variables declaration//GEN-END:variables
}
