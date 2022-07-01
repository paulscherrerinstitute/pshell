package ch.psi.pshell.xscan;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.Converter;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.ui.App;
import ch.psi.utils.Arr;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.Editor;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 */
public final class DataViewer extends MonitoredPanel {

    final Timer timer1s;

    public DataViewer() {
        initComponents();
        if (MainFrame.isDarcula()) {
            treeFolder.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        }

        JPopupMenu filePopupMenu = new JPopupMenu();

        JMenuItem menuPlot = new JMenuItem("Plot data");
        menuPlot.addActionListener((ActionEvent e) -> {
            try {
                File file = (File) treeFolder.getLastSelectedPathComponent();
                if ((file != null) && (file.exists()) && file.isFile()) {
                    plotFile(file);
                }
            } catch (Exception ex) {
                SwingUtils.showException(DataViewer.this, ex);
            }
        });
        filePopupMenu.add(menuPlot);

        JMenuItem menuOpen = new JMenuItem("Open file");
        menuOpen.addActionListener((ActionEvent e) -> {
            try {
                File file = (File) treeFolder.getLastSelectedPathComponent();
                if ((file != null) && (file.exists()) && file.isFile()) {
                    openFile(file);
                }
            } catch (Exception ex) {
                SwingUtils.showException(DataViewer.this, ex);
            }
        });
        filePopupMenu.add(menuOpen);

        JMenu menuConvert = new JMenu("Convert");
        for (Converter converter : new Converter[]{new ConverterMat(), new ConverterMat2d()}) {
            JMenuItem item = new JMenuItem(converter.getName());
            item.addActionListener((a) -> {
                try {
                    File file = (File) treeFolder.getLastSelectedPathComponent();
                    TreePath path = treeFolder.getSelectionPath();
                    if ((file != null) && (file.exists()) && file.isFile()) {
                        DataManager dataManager = new DataManager(Context.getInstance(), "fda", "fda");
                        String output = file.getParent() + "/" + IO.getPrefix(file) + "_" + converter.getName().replaceAll("\\s+", "") + "." + converter.getExtension();
                        converter.startConvert(dataManager, file.getParent(), file.getName(), new File(output)).handle((ret, ex) -> {
                            if (ex != null) {
                                SwingUtils.showException(DataViewer.this, (Exception) ex);
                            } else {
                                try {
                                    repaintTreePath(path.getParentPath(), true);
                                } catch (Exception e) {
                                }
                                SwingUtils.showMessage(DataViewer.this, "Success", "Success creating:\n" + String.valueOf(output));
                            }
                            return ret;
                        });
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(DataViewer.this, ex);
                }
            });
            menuConvert.add(item);
        }

        filePopupMenu.add(menuConvert);

        JMenuItem menuBrowse = new JMenuItem("Browse folder");
        menuBrowse.addActionListener((ActionEvent e) -> {
            try {
                File file = (File) treeFolder.getLastSelectedPathComponent();
                if ((file != null) && (file.exists())) {
                    Logger.getLogger(DataPanel.class.getName()).fine("Opening desktop for: " + String.valueOf(file));
                    Desktop.getDesktop().open(file.isDirectory() ? file : file.getParentFile());
                }
            } catch (Exception ex) {
                SwingUtils.showException(DataViewer.this, ex);
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
                SwingUtils.showException(DataViewer.this, ex);
            }
        });
        filePopupMenu.add(menuRefresh);

        treeFolder.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if ((!e.isPopupTrigger()) && (e.getClickCount() == 2)) {
                    onFileDoubleClick((File) treeFolder.getLastSelectedPathComponent());
                } else {
                    checkPopup(e);
                }
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
                        File file = (File) treeFolder.getLastSelectedPathComponent();
                        if ((file != null) && (file.exists())) {
                            menuFileOrder.setVisible(path.getPathCount() == 1);
                            menuPlot.setVisible(file.isFile() && IO.getExtension(file).equalsIgnoreCase("txt"));
                            menuConvert.setVisible(menuPlot.isVisible());
                            menuOpen.setVisible(file.isFile());
                            menuRefresh.setVisible(!file.isFile());
                            filePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(DataViewer.this, ex);
                }
            }

        });

        setBaseFolder(null);
        timer1s = new Timer(1000, (ActionEvent e) -> {
            try {
                onTimer();
            } catch (Exception ex) {
                Logger.getLogger(DataViewer.class.getName()).log(Level.INFO, null, ex);
            }
        });
        setFileOrder(FileOrder.Name);
    }
    WatchService watchService;
    FileSystemModel treeFolderModel;
    String baseFolder;

    /*
    boolean isDataFile(File file){
        if ((file != null) && file.exists() &&  file.isFile()){
            try{
                String ext = IO.getExtension(file);
                if (ext.equalsIgnoreCase("txt")){
                    return true;
                }
                if (ext.equalsIgnoreCase("h5")){
                    return "fda".equals(Context.getInstance().getDataManager().getAttribute(file.getAbsolutePath(), "/", Layout.ATTR_LAYOUT));
                }
            } catch (Exception ex) {
               
            }            
        }
        return false;
    }
     */
    void onFileDoubleClick(File file) {
        if ((file != null) && (file.exists() && (file.isFile()))) {
            try {
                switch (IO.getExtension(file)) {
                    case "log":
                    case "xml":
                    case "h5":
                        openFile(file);
                        break;
                    case "txt":
                        plotFile(file);
                        break;
                }

            } catch (Exception ex) {
                System.err.println(ex);
            }
        }
    }

    void openFile(File file) throws Exception {
        if ((file != null) && (file.exists() && (file.isFile()))) {
            String filename = file.getCanonicalPath();
            switch (IO.getExtension(file)) {
                case "log":
                case "mat":
                    Editor editor = App.getInstance().getMainFrame().openTextFile(filename);
                    editor.setReadOnly(true);
                    break;
                case "xml":
                    App.getInstance().getMainFrame().openScriptOrProcessor(filename);
                    break;
                case "h5":
                    for (DataPanel pn : App.getInstance().getMainFrame().getDataFilePanels()) {
                        if (file.equals(new File(pn.getFileName()))) {
                            if (App.getInstance().getMainFrame().getDocumentsTab().indexOfComponent(pn) >= 0) {
                                App.getInstance().getMainFrame().getDocumentsTab().setSelectedComponent(pn);
                            } else if (App.getInstance().getMainFrame().getDetachedScripts().containsValue(pn)) {
                                pn.getTopLevelAncestor().requestFocus();
                            }
                            //pn.update();
                            return;
                        }
                    }
                    App.getInstance().getMainFrame().openDataFile(filename.toString());
                    break;
                case "txt":
                    Editor ed = App.getInstance().getMainFrame().openTextFile(filename);
                    ed.setReadOnly(true);
            }
        }
    }

    void plotFile(File file) throws Exception {
        if ((file != null) && (file.exists() && (file.isFile())) && (IO.getExtension(file).equalsIgnoreCase("txt"))) {
            ProcessorXScan processor = new ProcessorXScan();
            processor.plotDataFile(file);

        }
    }

    @Override
    protected void onShow() {
        timer1s.start();
    }

    @Override
    protected void onHide() {
        timer1s.stop();
    }

    public void initialize(String folder) {
        try {
            if (folder == null) {
                folder = Context.getInstance().getSetup().getDataPath();
            }
            if (folder.trim().startsWith("~")) {
                folder = Sys.getUserHome() + Str.trimLeft(folder).substring(1);
            }
            folder = new File(folder).getCanonicalPath();
            setBaseFolder(folder);
            update(true);
        } catch (IOException ex) {
            Logger.getLogger(DataViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void onTimer() {
        update(false);
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
            Logger.getLogger(DataViewer.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    public void update(boolean force) {
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
                Path fullPath = dir.resolve(path);

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
            Logger.getLogger(DataViewer.class.getName()).log(Level.WARNING, null, ex);
        }
        watchKey.reset();
    }

    void setBaseFolder(String path) {
        if ((path == null) || !path.equals(baseFolder)) {
            try {
                if (watchService != null) {
                    watchService.close();
                    watchService = null;
                }
            } catch (Exception ex) {
                Logger.getLogger(DataViewer.class.getName()).log(Level.WARNING, null, ex);
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
                    Logger.getLogger(DataViewer.class.getName()).log(Level.WARNING, null, ex);
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
            List<File> ret = new ArrayList<>();
            if (f.isDirectory()) {
                File[] subfolders = IO.listSubFolders(f);
                switch (fileOrder) {
                    case Modified:
                        IO.orderByModified(subfolders);
                        break;
                    case Name:
                        IO.orderByName(subfolders);
                        break;
                }
                for (File subfolder : subfolders) {
                    if (!subfolder.isHidden()) {
                        ret.add(subfolder);
                    }
                }
                File[] files = IO.listFiles(f, new String[]{"xml", "h5", "txt", "log", "mat"});
                //File[] files = IO.listFiles(f, "*.txt");

                switch (fileOrder) {
                    case Modified:
                        IO.orderByModified(files);
                        break;
                    case Name:
                        IO.orderByName(files);
                        break;
                }
                for (File file : files) {
                    if (!file.isHidden() && file.isFile()) {
                        ret.add(file);
                    }
                }
                //TODO: Is there way to check if has been regestered already?
                registerFolderEvents(((File) parent).getPath());
            }
            File[] files = ret.toArray(new File[0]);
            curChildren.put(f, files);
            return files;
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
                if (((File) child).getName().equals(children[i].getName())) {
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

    public static void createPanel(String path) {
        java.awt.EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame(App.getApplicationTitle());
            frame.setTitle("XScan Data Browser");
            frame.setIconImage(App.getIconSmall());
            DataViewer panel = new DataViewer();
            frame.add(panel);
            frame.setSize(1000, 800);
            SwingUtils.centerComponent(null, frame);
            frame.setVisible(true);
            panel.initialize(path);
        });
    }

    public static void main(String args[]) {
        MainFrame.setLookAndFeel(MainFrame.getNimbusLookAndFeel());
        createPanel(args.length > 1 ? args[1] : "~/dev/pshell/config/home/data");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollFolder = new javax.swing.JScrollPane();
        treeFolder = new javax.swing.JTree();

        setPreferredSize(new java.awt.Dimension(600, 400));

        scrollFolder.setPreferredSize(new java.awt.Dimension(150, 322));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        treeFolder.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        scrollFolder.setViewportView(treeFolder);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollFolder, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollFolder, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollFolder;
    private javax.swing.JTree treeFolder;
    // End of variables declaration//GEN-END:variables
}
