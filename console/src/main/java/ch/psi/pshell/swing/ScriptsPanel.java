package ch.psi.pshell.swing;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Sys.OSFamily;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 */
public class ScriptsPanel extends MonitoredPanel implements UpdatablePanel {

    final DefaultTableModel model;
    ScriptsPanelListener listener;
    JPopupMenu popupMenu;
    JMenuItem menuRevisionHistory;
    JMenuItem menuEdit;
    JMenuItem menuEditExt;
    JMenuItem menuReadOnly;
    JMenuItem menuBrowse;
    JMenuItem menuRun;

    final boolean orderByFileName;
    final boolean disableRowSorterOnUpdate;

    /**
     * The listener interface for receiving scripts panel events.
     */
    public interface ScriptsPanelListener {

        void onScriptSelected(File file);
    }

    public ScriptsPanel() {
        initComponents();
        model = (DefaultTableModel) table.getModel();

        popupMenu = new JPopupMenu();

        menuEdit = new JMenuItem("Edit");
        menuEdit.addActionListener((ActionEvent e) -> {
            String script = getSelectedScript();
            if (script != null) {
                try {
                    if (listener != null) {
                        listener.onScriptSelected(Paths.get(currentPath, script).toFile());
                    }

                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        menuEditExt = new JMenuItem("Open external editor");
        menuEditExt.addActionListener((ActionEvent e) -> {
            String script = getSelectedScript();
            if (script != null) {
                try {
                    File file = Paths.get(currentPath, script).toFile();
                    if (file.exists()) {
                        Logger.getLogger(DataPanel.class.getName()).fine("Opening desktop for: " + String.valueOf(file));
                        Desktop.getDesktop().open(file);
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        menuReadOnly = new JMenuItem("Set read-only");
        menuReadOnly.addActionListener((ActionEvent e) -> {
            String script = getSelectedScript();
            if (script != null) {
                try {
                    File file = Paths.get(currentPath, script).toFile();
                    if (file.exists()) {
                        Logger.getLogger(DataPanel.class.getName()).fine(menuReadOnly.getText() + ": " + String.valueOf(file));                        
                        file.setWritable(menuReadOnly.getText().equals("Set read-only") ? false : true);
                        buildList();
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });
        
        menuBrowse = new JMenuItem("Browse containing folder");
        menuBrowse.addActionListener((ActionEvent e) -> {
            String script = getSelectedScript();
            if (script != null) {
                try {
                    File file = Paths.get(currentPath, script).toFile();
                    if (file.exists()) {
                        Logger.getLogger(DataPanel.class.getName()).fine("Opening desktop for: " + String.valueOf(file.getParentFile()));
                        Desktop.getDesktop().open(file.getParentFile());
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        menuRun = new JMenuItem("Run");
        menuRun.addActionListener((ActionEvent e) -> {
            String script = getSelectedScript();
            if (script != null) {
                try {
                    File file = Paths.get(currentPath, script).toFile();
                    if (file.exists()) {
                        Context.getInterpreter().evalFileAsync(file.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        menuRevisionHistory = new JMenuItem("Revision history");
        menuRevisionHistory.addActionListener((ActionEvent e) -> {
            String script = getSelectedScript();
            if (script != null) {
                try {
                    showHistory(script);
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        popupMenu.add(menuEdit);
        popupMenu.add(menuEditExt);
        popupMenu.add(menuReadOnly);
        popupMenu.add(menuBrowse);
        popupMenu.addSeparator();
        popupMenu.add(menuRun);
        popupMenu.addSeparator();
        popupMenu.add(menuRevisionHistory);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger() && !popupMenu.isVisible())) {
                        String file = getSelectedScript();
                        if (file != null) {
                            //Opening file
                            if (listener != null) {
                                listener.onScriptSelected(Paths.get(currentPath, file).toFile());
                            }
                        } else {
                            file = getSelectedFolder();
                            if (file != null) {
                                //Navigating to sub-folder
                                if (file.equals("..")) {
                                    setPath(Paths.get(currentPath).getParent().toString());
                                } else {
                                    setPath(Paths.get(currentPath, file).toString());
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
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
                        int r = table.rowAtPoint(e.getPoint());
                        if (r >= 0 && r < table.getRowCount()) {
                            table.setRowSelectionInterval(r, r);
                        } else {
                            table.clearSelection();
                        }
                        String script = getSelectedScript();
                        if (script != null) {                            
                            boolean allowRun =  !Context.getRights().denyRun;
                            menuRun.setEnabled(allowRun && (Context.getState().isReady()));
                            menuRevisionHistory.setEnabled(Context.hasVersioningManager());

                            try{
                                File file = Paths.get(currentPath, script).toFile();      
                                //if (!file.getParentFile().canWrite()){
                                //    menuReadOnly.setVisible(false);
                                //} else {
                                    menuReadOnly.setText(file.canWrite() ? "Set read-only" : "Set read-write");
                                    menuReadOnly.setVisible(true);
                                //}
                            } catch (Exception ex) {
                                menuReadOnly.setVisible(false);
                            }
                            
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }

            }

        });

        TableColumn column = table.getColumnModel().getColumn(0);
        column.setCellRenderer((TableCellRenderer) new DefaultTableCellRenderer() {
            Color fileColor = null;
            Color folderColor = null;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (fileColor == null) {
                    fileColor = comp.getForeground();
                    folderColor = Color.GRAY;
                    try {
                        //Dark LAF
                        Color back = UIManager.getColor("nimbusLightBackground");
                        if (back.equals(folderColor)) {
                            folderColor = folderColor.darker();
                        }
                    } catch (Exception ex) {

                    }

                }
                int index = table.convertRowIndexToModel(row);
                String file = (String) table.getModel().getValueAt(index, 0);
                if (file.equals("..") || file.endsWith("/")) {
                    comp.setForeground(folderColor);
                } else {
                    comp.setForeground(fileColor);
                }
                return comp;
            }
        });
        orderByFileName = Sys.getOSFamily() == OSFamily.Mac; //List files on mac is not ordered
        disableRowSorterOnUpdate = Sys.getOSFamily() == OSFamily.Mac; //On mac row sorter acts on new data

        table.setDragEnabled(true);

        table.setTransferHandler(new TransferHandler() {

            @Override
            public int getSourceActions(JComponent c) {
                return DnDConstants.ACTION_COPY_OR_MOVE;
            }

            @Override
            public Transferable createTransferable(JComponent comp) {
                StringSelection transferable = null;
                String filename = getSelectedScript();
                if (filename != null) {
       
                        Path path = Paths.get(currentPath, filename);
                        if (path.toFile().isFile()){
                            transferable = new StringSelection(path.toString());
                        }
                }
                return transferable;
            }
        });

    }

    String getSelectedScript() {
        try {
            //Since can reorder...
            int index = table.convertRowIndexToModel(table.getSelectedRow());
            String ret = (String) model.getValueAt(index, 0);
            if (Paths.get(currentPath, ret).toFile().isFile()) {
                return ret;
            }
        } catch (Exception ex) {
        }
        return null;
    }

    String getSelectedFolder() {
        try {
            //Since can reorder...
            int index = table.convertRowIndexToModel(table.getSelectedRow());
            String ret = (String) model.getValueAt(index, 0);

            if (ret.endsWith("/")) {
                ret = ret.substring(0, ret.length() - 1);
            }
            if (ret.equals("..")) {
                return "..";
            } else {
                File file = Paths.get(currentPath, ret).toFile();
                if (file.isDirectory()) {
                    return file.getName();
                }
            }
        } catch (Exception ex) {
        }
        return null;
    }

    public void setListener(ScriptsPanelListener listener) {
        this.listener = listener;
    }

    String homePath;
    String[] extensions;

    public void initialize() {
        
        String[] extensions = new String[]{
                   Context.getScriptType().getExtension(),
                   ScanEditorPanel.EXTENSION
               };
        //!!!
        //if (!((View)Context.getView()).getPreferences().showXScanFileBrowser) {
        //    extensions = Arr.append(extensions, ProcessorXScan.EXTENSION);
        //} 
        //if (!((View)Context.getView()).getPreferences().showQueueBrowser) {
        //    extensions = Arr.append(extensions, QueueProcessor.EXTENSION);
        //} 
                
        initialize(Setup.getScriptsPath(),extensions);
    }

    public void initialize(String homePath, String[] extensions) {
        this.homePath = Setup.expandPath(homePath);
        this.extensions = extensions;
        setPath(this.homePath);
    }

    String currentPath;

    void setPath(String path) {
        currentPath = path;
        buildList();
        registerFolderEvents(Paths.get(currentPath));
    }

    @Override
    public void update() {
        WatchKey watchKey = watchService.poll();
        if (watchKey == null) {
            return;
        }
        watchKey.pollEvents();
        watchKey.reset();

        try {
            buildList();
        } catch (Exception ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    File[] getSubFolders() {
        ArrayList<File> folders = new ArrayList<>();
        for (File file : IO.listSubFolders(currentPath)) {
            if (!Arr.containsEqual(new String[]{"Lib", "cachedir"}, file.getName())) {
                folders.add(file);
            }
        }
        File[] ret = folders.toArray(new File[0]);
        if (orderByFileName) {
            IO.orderByName(ret);
        }
        return ret;
    }

    File[] getFiles() {
        File[] ret = IO.listFiles(currentPath, extensions);
        if (orderByFileName) {
            IO.orderByName(ret);
        }
        return ret;
    }

    void buildList() {
        if (disableRowSorterOnUpdate) {
            table.setAutoCreateRowSorter(false);
        }
        model.setNumRows(0);
        try {
            if (!new File(currentPath).getCanonicalFile().equals(new File(homePath).getCanonicalFile())) {
                model.addRow(new Object[]{"..", ""});
            }
        } catch (Exception ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.WARNING, null, ex);
        }

        for (File file : getSubFolders()) {
            model.addRow(new Object[]{file.getName() + "/", ""});
        }
        for (File file : getFiles()) {
            String modified = Chrono.getTimeStr(file.lastModified(), "YY/MM/dd HH:mm:ss");
            /*
            if (!file.canWrite()){
                modified = modified + " (read-only)";
            }
            */
            model.addRow(new Object[]{file.getName(), modified});
        }
        if (disableRowSorterOnUpdate) {
            table.setAutoCreateRowSorter(true);
        }
    }

    void showHistory(String script) throws Exception {
        String fileName = Paths.get(currentPath, script).toString();
        Frame parent = (Frame) this.getTopLevelAncestor();
        RevisionHistoryDialog dlg = new RevisionHistoryDialog(parent, false, fileName);
        SwingUtils.centerComponent(parent, dlg);
        dlg.setVisible(true);
    }

    WatchService watchService;

    void registerFolderEvents(Path path) {
        try {
            try {
                if (watchService != null) {
                    watchService.close();
                    watchService = null;
                }
            } catch (Exception ex) {
                Logger.getLogger(ScriptsPanel.class.getName()).log(Level.WARNING, null, ex);
            }

            watchService = FileSystems.getDefault().newWatchService();
            if (watchService != null) {
                path.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(ScriptsPanel.class.getName()).log(Level.WARNING, null, ex);
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

        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        table.setAutoCreateRowSorter(true);
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Modified"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 523, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
