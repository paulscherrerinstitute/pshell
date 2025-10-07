package ch.psi.pshell.swing;

import ch.psi.pshell.app.App;
import ch.psi.pshell.app.Setup;
import ch.psi.pshell.data.Converter;
import ch.psi.pshell.data.DataAddress;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.DataSlice;
import ch.psi.pshell.data.Format;
import ch.psi.pshell.data.DataStore;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.LayoutBase;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.framework.Processor;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotPanel;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.Sequencer;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Range;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.versioning.VersionControl;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
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
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 *
 */
public final class DataPanel extends MonitoredPanel implements UpdatablePanel {

    /**
     * The listener interface for receiving data panel manager events.
     */
    public interface DataPanelListener {

        void plotData(DataManager dataManager, String root, String path) throws Exception;

        void plotData(Object array, Range range, String name, double[] x) throws Exception;

        JPanel openFile(String fileName) throws Exception;

        JPanel openScript(String script, String name) throws Exception;
    }

    JTable tableData;
    JTable tableRowHeader;
    JScrollPane scrollPaneTableData;
    ValueSelection pageSelection;
    String[] fieldNames = null;
    DataPanelListener listener;
    String[] processingScripts;
    List<String> visibleFiles = new ArrayList<>();
    int selectedDataCol;
    PlotPanel plotPanel;
    
    public DataPanel() {
        initComponents();
        setPlottingScripts(null);
        treeFolder.addTreeSelectionListener((TreeSelectionEvent event) -> {
            setCurrentFilePath(null);
            showData(null);
            File file = (File) treeFolder.getLastSelectedPathComponent();
            showFileProps(file, false);
            if (file != null) {
                try {
                    if (hasVisibleExtension(file)) {
                        setCurrentPath(file);
                    } else {
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
                    }
                } catch (Exception ex) {
                    textProperties.append("\n" + ex.getMessage(), SwingUtils.getColorError());
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
                showException(ex);
            }
        });
        filePopupMenu.add(menuOpen);

        JMenuItem menuCopy = new JMenuItem("Copy");
        menuCopy.addActionListener((ActionEvent e) -> {
            try {
                File selected = getFolderTreeSelectedFile();
                if (selected != null) {
                    String filename = selected.getCanonicalPath();
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new FileTransferHandler(filename), (Clipboard clipboard1, Transferable contents) -> {
                        //Do nothing
                    });
                }
            } catch (Exception ex) {
                showException(ex);
            }
        });
        filePopupMenu.add(menuCopy);

        JMenuItem menuPlot = new JMenuItem("Plot");
        menuPlot.addActionListener((ActionEvent e) -> {
            try {
                File selected = getFolderTreeSelectedFile();
                if (selected != null) {
                    Logger.getLogger(DataPanel.class.getName()).fine("Plotting: " + String.valueOf(selected));
                    if (listener != null) {
                        listener.plotData(dataManager, selected.getAbsolutePath(), null);
                    }
                }
            } catch (Exception ex) {
                showException(ex);
            }
        });
        filePopupMenu.add(menuPlot);

        JMenu menuConvertFile = new JMenu("Convert");
        filePopupMenu.add(menuConvertFile);

        JMenuItem menuBrowse = new JMenuItem("");
        menuBrowse.addActionListener((ActionEvent e) -> {
            try {
                File selected = getFolderTreeSelectedFile();
                if (selected != null) {
                    Logger.getLogger(DataPanel.class.getName()).fine("Opening desktop for: " + String.valueOf(selected));
                    Desktop.getDesktop().open(selected);
                }
            } catch (Exception ex) {
                showException(ex);
            }
        });
        filePopupMenu.add(menuBrowse);

        JMenu menuProcessing = new JMenu("Run");
        filePopupMenu.add(menuProcessing);

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
        menuFileOrder.addSeparator();
        JCheckBoxMenuItem menuSeparateFolders = new JCheckBoxMenuItem("Separate Folders");
        menuSeparateFolders.addActionListener((ActionEvent e) -> {
            if (menuSeparateFolders.isSelected() != getSeparateFolders()) {
                setSeparateFolders(menuSeparateFolders.isSelected());
                repaintTreePath(treeFolder.getSelectionPath(), true);
            }
        });
        menuFileOrder.add(menuSeparateFolders);

        menuFileOrder.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                for (Component item : menuFileOrder.getMenuComponents()) {
                    if (item instanceof JMenuItem menuItem) {
                        menuItem.setSelected(getFileOrder().toString().equals(menuItem.getText()));
                    }
                }
                menuSeparateFolders.setSelected(getSeparateFolders());
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
            refresh();
        });
        filePopupMenu.add(menuRefresh);

        JMenuItem menuCalcSize = new JMenuItem("Calculate size");
        menuCalcSize.addActionListener((ActionEvent e) -> {
            try {
                File selected = getFolderTreeSelectedFile();
                showFileProps(selected, true);
            } catch (Exception ex) {
                showException(ex);
            }
        });
        filePopupMenu.add(menuCalcSize);

        treeFolder.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
                try {
                    if (e.getClickCount() == 2) {
                        if (currentFile != null) {                            
                            if ((!embedded) || hasVisibleExtension(currentFile)) {
                                Logger.getLogger(DataPanel.class.getName()).fine("Opening: " + String.valueOf(currentFile));
                                if (listener != null) {
                                    try {
                                        if (Processor.canProcessorsPlot(currentFile.getAbsolutePath(), null, dataManager)) {
                                            listener.plotData(dataManager, currentFile.getAbsolutePath(), null);
                                        } else {
                                            //!!! transfer this logic to listener
                                            //boolean xscan = !currentFile.isDirectory() ? false : ProcessorXScan.isFdaSerializationFolder(currentFile)
                                            //        || // If FDA serialization then never opens a root with double-click if there are children - to emulate old FDA viewer.
                                            //        (Context.getInstance().getConfig().fdaSerialization && (dataManager.getFormat() instanceof FormatFDA));
                                            //
                                            //if ((treeFolderModel.getChildCount(currentFile) == 0) || (!xscan)) {
                                                listener.openFile(currentFile.getCanonicalPath());
                                            //}
                                        }
                                    } catch (Exception ex) {
                                        Logger.getLogger(DataPanel.class.getName()).log(Level.SEVERE, null, ex);
                                    }
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

            void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        TreePath path = treeFolder.getPathForLocation(e.getX(), e.getY());
                        treeFolder.setSelectionPath(path);

                        File file = (File) treeFolder.getLastSelectedPathComponent();
                        boolean isRoot = isRoot(file);
                        File selected = getFolderTreeSelectedFile();
                        if (selected != null) {                            
                            boolean isProcessorDataFile = Processor.canProcessorsPlot(file.getAbsolutePath(), null, dataManager);
                            menuOpen.setVisible(isRoot || hasVisibleExtension(file));
                            menuPlot.setVisible(isProcessorDataFile);
                            menuBrowse.setText(selected.isDirectory() ? "Browse folder" : "Open external editor");
                            menuBrowse.setVisible(isRoot || selected.isDirectory());
                            menuCalcSize.setVisible(selected.isDirectory() && (path.getPathCount() > 1));
                            menuRefresh.setVisible(selected.isDirectory());
                            menuFileOrder.setVisible(path.getPathCount() == 1);
                            menuConvertFile.setVisible(false);
                            //if (isAdditionaExtension){
                            menuConvertFile.removeAll();
                            for (Converter converter : Converter.getServiceProviders()) {
                                TreePath tp = treeFolder.getSelectionPath();
                                if (converter.canConvert(dataManager, file)) {
                                    JMenuItem item = new JMenuItem(converter.getName());
                                    item.addActionListener((a) -> {
                                        converter.startConvert(file, DataPanel.this).handle((ret, ex) -> {
                                            if (ex != null) {
                                                showException((Exception) ex);
                                            } else if (ret != null) {
                                                showMessage("Success", "Success creating:\n" + String.valueOf(ret));
                                            }
                                            return ret;
                                        });
                                    });
                                    menuConvertFile.add(item);
                                }
                            }
                            menuConvertFile.setVisible(menuConvertFile.getMenuComponentCount() > 0);
                            //}

                            if (isRoot && Sequencer.hasInstance()) {
                                setupProcessMenu(menuProcessing);
                                menuProcessing.setVisible(true);
                            } else {
                                menuProcessing.setVisible(false);
                            }
                            filePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
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
        JMenu menuPlotAgainst = new JMenu("Plot against");
        JMenu menuConvert = new JMenu("Convert");
        menuPlotData.addActionListener((ActionEvent e) -> {
            try {
                TreePath path = treeFile.getSelectionPath();
                String dataPath = getDataPath(path);
                if (listener != null) {
                    listener.plotData(dataManager, currentFile.getPath(), dataPath);
                }
            } catch (Exception ex) {
                showException(ex);
            }
        });
        popupMenu.add(menuPlotData);
        popupMenu.add(menuPlotAgainst);

        Separator menuPlotDataSeparator = new Separator();
        popupMenu.add(menuPlotDataSeparator);

        popupMenu.add(menuConvert);
        Separator menuConvertSeparator = new Separator();
        popupMenu.add(menuConvertSeparator);

        JMenuItem menuCopyLink = new JMenuItem("Copy link");
        menuCopyLink.addActionListener((ActionEvent e) -> {
            try {
                TreePath path = treeFile.getSelectionPath();
                String root = getCurrentRoot();
                String dataPath = getDataPath(path);
                if (dataPath == null) {
                    dataPath = "/";
                }
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("\"" + root + "|" + dataPath + "\""), null);
            } catch (Exception ex) {
                showException(ex);
            }
        });
        popupMenu.add(menuCopyLink);

        JMenuItem menuOpenScript = new JMenuItem("Open producing script");
        menuOpenScript.addActionListener((ActionEvent e) -> {
            if (listener != null) {
                try {
                    String sourceName = (String) dataManager.getAttribute(currentFile.getPath(), "/", Layout.ATTR_SOURCE_NAME);
                    String sourceRevision = (String) dataManager.getAttribute(currentFile.getPath(), "/", Layout.ATTR_SOURCE_REVISION);
                    if (sourceRevision != null) {
                        try {
                            String script = VersionControl.getFileContents(sourceName, sourceRevision);
                            listener.openScript(script, new File(sourceName).getName() + "_" + sourceRevision.substring(0, Math.min(8, sourceRevision.length())));
                            return;
                        } catch (Exception ex) {
                        }
                    }  
                    listener.openFile(sourceName);
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });
        popupMenu.add(menuOpenScript);

        JMenuItem menuAssign = new JMenuItem("Assign to variable");
        menuAssign.addActionListener((ActionEvent e) -> {
            try {
                TreePath path = treeFile.getSelectionPath();
                String root = getCurrentRoot();
                String dataPath = getDataPath(path);
                if (dataPath == null) {
                    dataPath = "/";
                }
                String var = getString("Enter variable name:", null);
                if ((var != null) && (!var.trim().isEmpty())) {
                    Sequencer.getInstance().tryEvalLineBackground(var.trim() + "=load_data(\"" + root + "|" + dataPath + "\")");
                }
            } catch (Exception ex) {
                showException(ex);
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
                    showException(ex);
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
                        if (dataPath == null) {
                            return;
                        }                       
                        Map<String, Object> info = dataManager.getInfo(currentFile.getPath(), dataPath);
                        menuPlotData.setVisible(false);
                        menuPlotAgainst.setVisible(false);
                        menuAssign.setVisible(false);
                        menuConvert.setVisible(false);
                        if (info != null) {
                            String type = String.valueOf(info.get(Format.INFO_TYPE));
                            if ((type.equals(Format.INFO_VAL_TYPE_GROUP))) {
                                for (String child : dataManager.getChildren(currentFile.getPath(), dataPath)) {                                    
                                    info = dataManager.getInfo(currentFile.getPath(), child);
                                    if (dataManager.isPlottable(info)) {
                                        menuPlotData.setVisible(true);
                                        break;
                                    }
                                }
                                //Nexus plots
                                if (!menuPlotData.isVisible()){
                                    String defaultChild =  dataManager.getAttributes(currentFile.getPath(), dataPath).getOrDefault("default", "").toString();
                                    if ((defaultChild!=null) && (!defaultChild.isBlank())){
                                        Map<String, Object> defaultInfo = dataManager.getInfo(currentFile.getPath(), dataPath+ "/" + defaultChild);
                                        String defaultType = String.valueOf(defaultInfo.get(Format.INFO_TYPE));
                                        if ((defaultType.equals(Format.INFO_VAL_TYPE_GROUP))) {
                                            menuPlotData.setVisible(true);                                        
                                        }
                                    }
                                }
                            } else if (type.equals(Format.INFO_VAL_TYPE_DATASET) || type.equals(Format.INFO_VAL_TYPE_SOFTLINK)) {
                                menuAssign.setVisible(Sequencer.hasInstance());
                                if (dataManager.isPlottable(info)) {
                                    menuConvert.removeAll();
                                    for (Converter converter : Converter.getServiceProviders()) {
                                        TreePath tp = treeFile.getSelectionPath();
                                        if (converter.canConvert(dataManager, currentFile.getPath(), dataPath, info)) {
                                            JMenuItem item = new JMenuItem(converter.getName());
                                            item.addActionListener((a) -> {
                                                converter.startConvert(dataManager, currentFile.getPath(), getDataPath(tp), DataPanel.this).handle((ret, ex) -> {
                                                    if (ex != null) {
                                                        showException((Exception) ex);
                                                    } else if (ret != null) {
                                                        showMessage("Success", "Success creating:\n" + String.valueOf(ret));
                                                    }
                                                    return ret;
                                                });
                                            });
                                            menuConvert.add(item);
                                        }
                                    }
                                    menuPlotAgainst.removeAll();
                                    menuConvert.setVisible(menuConvert.getMenuComponentCount() > 0);
                                    menuPlotData.setVisible(true);
                                    try{
                                        Integer rank = ((Number) info.getOrDefault(Format.INFO_RANK, 0)).intValue();
                                        Long elements = ((Number) info.getOrDefault(Format.INFO_ELEMENTS, -1)).longValue();
                                        boolean compound = info.getOrDefault(Format.INFO_DATA_TYPE, Format.INFO_VAL_DATA_TYPE_COMPOUND) == Format.INFO_VAL_DATA_TYPE_COMPOUND;
                                        if ((rank == 1) && (elements>0) && ! compound){
                                            String self = path.getLastPathComponent().toString();
                                            String parentPath = getDataPath(path.getParentPath());
                                            String[] siblings = dataManager.getChildren(currentFile.getPath(), parentPath);
                                            siblings = Arr.append(siblings, dataManager.getChildren(currentFile.getPath(), parentPath+"/"+LayoutBase.PATH_META));
                                            for (String sibling : siblings) {
                                                String name = getShortName(sibling);
                                                if (!name.equals(self)){
                                                    Map siblingInfo = dataManager.getInfo(currentFile.getPath(), sibling);
                                                    if (dataManager.isPlottable(info)) {
                                                        if (siblingInfo.getOrDefault(Format.INFO_DATA_TYPE, Format.INFO_VAL_DATA_TYPE_COMPOUND) != Format.INFO_VAL_DATA_TYPE_COMPOUND) {
                                                            if (siblingInfo.getOrDefault(Format.INFO_RANK, 0) == Integer.valueOf(1)){
                                                                if (elements.equals(((Number)siblingInfo.getOrDefault(Format.INFO_ELEMENTS, -1)).longValue())){                                                            
                                                                    JMenuItem item = new JMenuItem(name);
                                                                    item.addActionListener((ActionEvent ae) -> {
                                                                        try{
                                                                            Object array = dataManager.getData(currentFile.getPath(), dataPath).sliceData;                                                                
                                                                            double[] x = (double[]) Convert.toDouble(dataManager.getData(currentFile.getPath(), sibling).sliceData);
                                                                            listener.plotData(array, null, self + " (X axis: " + name + ")", x);
                                                                        } catch (Exception ex){
                                                                            showException(ex);
                                                                        }
                                                                    });
                                                                    menuPlotAgainst.add(item);
                                                                    menuPlotAgainst.setVisible(true);
                                                                }
                                                            }
                                                        }                                                    
                                                    }
                                                }
                                            }                                        
                                        }
                                    } catch (Exception ex){
                                        showException(ex);
                                    }
                                }
                            }
                        }
                        menuOpenScript.setVisible((info != null) && "/".equals(dataPath) && (dataManager.getAttribute(currentFile.getPath(), dataPath, Layout.ATTR_SOURCE_NAME) != null));
                        menuPlotDataSeparator.setVisible(menuPlotData.isVisible());
                        menuConvertSeparator.setVisible(menuConvert.isVisible());
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } catch (Exception ex) {
                    showException(ex);
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

        
        JPopupMenu tableDataPopupMenu = new JPopupMenu();
        JMenuItem menuPlotTableData = new JMenuItem("Plot data");
        menuPlotTableData.addActionListener((ActionEvent e) -> {
            plotTableSelection();
        });
        tableDataPopupMenu.add(menuPlotTableData);
        
        JPopupMenu tableDataColPopupMenu = new JPopupMenu();
        JMenuItem menuPlotCol = new JMenuItem("Plot column");
        JMenu menuPlotColAgainst = new JMenu("Plot against");
        menuPlotCol.addActionListener((ActionEvent ev) -> {
            plotColumnData(selectedDataCol, null, null);
        });
        tableDataColPopupMenu.add(menuPlotCol);
        tableDataColPopupMenu.add(menuPlotColAgainst);
        
        
        tableData.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (!e.isPopupTrigger()) && !tableDataColPopupMenu.isVisible()) {
                    if (tableData.getSelectedRow() >= 0) {
                        plotTableSelection();
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
                        int r = tableData.rowAtPoint(e.getPoint());
                        if (r >= 0 && r < tableData.getRowCount()) {
                            Object selection = getTableSelection();
                            if (tableData.getSelectedRowCount() == 1) {
                                menuPlotTableData.setText("Plot row");
                            } else if ((tableData.getSelectedRowCount() == 0) || (tableData.getSelectedRowCount() >= tableData.getRowCount())) {
                                menuPlotTableData.setText("Plot data");
                            } else {
                                menuPlotTableData.setText("Plot row selection");
                            }
                            menuPlotTableData.setEnabled(isTableSelectionPlottable(selection));
                            tableDataPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        tableData.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (!e.isPopupTrigger()) && !tableDataColPopupMenu.isVisible()) {
                    int c = tableData.columnAtPoint(e.getPoint());
                    if (c >= 0 && c < tableData.getColumnCount()) {
                        plotColumnData(c, null, null);
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
                        menuPlotColAgainst.setVisible(false);
                        int c = tableData.columnAtPoint(e.getPoint());
                        if (c >= 0 && c < tableData.getColumnCount()) {
                            selectedDataCol = c;
                            menuPlotCol.setEnabled(isTableSelectionPlottable(getColumnSelection(c)));
                            try{
                                if (menuPlotCol.isEnabled()){
                                    if (tableData.getColumnCount()>1){
                                        String self = getTableColHeader(selectedDataCol);
                                        if (self!=null){
                                            menuPlotColAgainst.removeAll();
                                            for (int i=0;i<tableData.getColumnCount(); i++){
                                                if (i!=selectedDataCol){
                                                    int selectedDataColX=i;
                                                    String name = getTableColHeader(selectedDataColX);
                                                    if (name!=null){
                                                        JMenuItem item = new JMenuItem(name);
                                                        item.addActionListener((ActionEvent ae) -> {
                                                            try{
                                                                plotColumnData(selectedDataCol, null, selectedDataColX);
                                                            } catch (Exception ex){
                                                                showException(ex);
                                                            }
                                                        });
                                                        menuPlotColAgainst.add(item);
                                                        menuPlotColAgainst.setVisible(true);

                                                    }
                                                }
                                            }
                                        }

                                    }
                                }
                            } catch (Exception ex){
                                showException(ex);
                            }                            
                            tableDataColPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

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
    
    public PlotPanel getPlotPanel(){
        return plotPanel;
    }
    
    public void  setPlotPanel(PlotPanel panel){
        plotPanel = panel;
    }

    String getShortName(String path){
        String[] tokens = path.split("/");
        return tokens[tokens.length-1];
    }

    boolean embedded = true;

    public boolean getEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean value) {
        if (embedded != value) {
            embedded = value;
            dataPanel.setVisible(embedded);
            splitFile.setVisible(embedded);
            splitSource.setDividerSize(value ? splitFile.getDividerSize() : 0);
            splitFolder.setDividerSize(value ? splitFile.getDividerSize() : 0);
            if ((splitSource.getDividerLocation() >= splitSource.getWidth() - splitSource.getDividerSize() - 10)) {
                splitSource.setDividerLocation(0.70);
            }
            if ((splitFolder.getDividerLocation() >= splitFolder.getWidth() - splitFolder.getDividerSize() - 10)) {
                splitFolder.setDividerLocation(0.70);
            }
        }
    }

    public boolean isRoot(File file) {
        if (file != null) {
            if (dataManager.isDataPacked()) {
                return dataManager.getDataFileType().equals(IO.getExtension(file));
            } else {
                return dataManager.isRoot(file.getPath()) && file.isDirectory();
            }
        }
        return false;
    }

    void setupProcessMenu(JMenuItem menuItem) {
        List<ImmutablePair<String, String>> scripts = new ArrayList<>();
        Set<String> categories = new LinkedHashSet<>();
        if (processingScripts != null) {
            for (String script : processingScripts) {
                String[] tokens = script.split("\\|");
                String file = tokens[0].trim();
                String category = ((tokens.length == 1) || (tokens[1].isBlank())) ? "" : tokens[1].trim();
                if (Sequencer.hasInstance()){
                    File f = Sequencer.getInstance().getScriptFile(file);
                    if ((f != null) && (f.exists())) {
                        scripts.add(new ImmutablePair(file, category));
                        if (!category.isEmpty()) {
                            categories.add(category);
                        }
                    }
                }
            }
        }
        menuItem.removeAll();
        if (scripts.size() == 0) {
            menuItem.setEnabled(false);
        } else {
            menuItem.setEnabled(true);
            for (String category : categories) {
                JMenu categoryMenu = new JMenu(category);
                categoryMenu.setName(category);
                menuItem.add(categoryMenu);
            }
            for (ImmutablePair<String, String> script : scripts) {
                String file = script.left;
                String category = script.right;
                JMenuItem item = new JMenuItem(IO.getPrefix(file));
                item.addActionListener((ActionEvent ae) -> {
                    try {
                        Sequencer.getInstance().evalFileBackgroundAsync(file, List.of(getCurrentRoot())).handle((ret, ex) -> {
                            if (ex != null) {
                                showException((Exception) ex);
                            }
                            return ret;
                        });
                    } catch (Exception ex) {
                        showException(ex);
                    }
                });
                if (category.isEmpty()) {
                    menuItem.add(item);
                } else {
                    ((JMenu) SwingUtils.getComponentByName(menuItem, category)).add(item);
                }
            }
        }
    }

    String getCurrentRoot() {
        String root = currentFile.getPath();
        if (IO.isSubPath(root, Setup.getDataPath())) {
            root = IO.getRelativePath(root, Setup.getDataPath());
        } else {
            root = root.replace("\\", "\\\\");
        }
        return root;
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

    public void setPlottingScripts(String[] plottingScripts) {
        if (plottingScripts == null) {
            plottingScripts = new String[0];
        }
        this.processingScripts = plottingScripts;
    }

    public void setListener(DataPanelListener listener) {
        this.listener = listener;
    }

    public void initialize(DataManager manager) {
        dataManager = manager;
        setBaseFolder(dataManager.getDataFolder());
        try {
            setCurrentPath(null);
        } catch (IOException ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        update();        
    }
    
    public void initialize() {
        if ((!DataStore.isDefault())&& (DataStore.getGlobal() instanceof DataManager dataManager)){       
            initialize(dataManager);        
        }
    }
    
    public void initialize(String[] visibleFiles) {        
        setVisibleFiles(visibleFiles);
        initialize();
    }
    
    public boolean isInitialized(){
        return dataManager!=null;
    }
    
    public void refresh(){    
        try {
            if (dataManager!=null){
                dataManager.resetFileFilter();
            }
            repaintTreePath(treeFolder.getSelectionPath(), true);
        } catch (Exception ex) {
            showException(ex);
        }
    }
    
    public void setVisibleFiles(String[] visibleFiles){
        this.visibleFiles = (visibleFiles==null) ?  new ArrayList<>() : List.of(visibleFiles);
    }
        
    public boolean hasVisibleExtension(File file){
        return file.isFile() &&  
                ( Arr.containsEqual(DataStore.listVisibleExtensions(visibleFiles), IO.getExtension(file)) ||
                  visibleFiles.contains("*")
                );
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
        WatchKey watchKey = (watchService == null) ? null : watchService.poll();
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

    boolean separateFolders = true;

    public void setSeparateFolders(boolean separateFolders) {
        if (this.separateFolders != separateFolders) {
            this.separateFolders = separateFolders;
            if (treeFolderModel != null) {
                treeFolderModel.invalidate();
            }
        }
    }

    public boolean getSeparateFolders() {
        return separateFolders;
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
                ret = IO.listFiles(f, dataManager.getFileFilter(visibleFiles));                
                
                switch (fileOrder) {
                    case Modified -> IO.orderByModified(ret);
                    case Name -> IO.orderByName(ret);

                }

                if (separateFolders) {
                    //Put the folders before
                    Arrays.sort(ret, (a, b) -> Boolean.compare(b.isDirectory(), a.isDirectory()));
                }

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

    public void showFileProps() {        
        if (fileName != null) {
            doShowFileProps(new File(fileName), true);     
        }
    }
        
    void showFileProps(File file, boolean calculateFolderSize) {
        textProperties.clear();
        if ((file != null) && (treeFolderModel != null)) {
            if (!file.equals(treeFolderModel.root)) { //Not to compute the whole folder size
                doShowFileProps(file, calculateFolderSize);     
            }            
        }
    }
    
    void doShowFileProps(File file, boolean calculateFolderSize){
        textProperties.clear();
        textProperties.append("Type:     " + (file.isDirectory() ? "Folder" : IO.getExtension(file)) + "\n", SwingUtils.getColorStdout());
        try {
            BasicFileAttributes attr = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
            textProperties.append("Creation: " + Chrono.getTimeStr(attr.creationTime().toMillis(), "dd/MM/YY HH:mm\n"), SwingUtils.getColorStdout());
            textProperties.append("Accessed: " + Chrono.getTimeStr(attr.lastAccessTime().toMillis(), "dd/MM/YY HH:mm\n"), SwingUtils.getColorStdout());
            textProperties.append("Modified: " + Chrono.getTimeStr(attr.lastModifiedTime().toMillis(), "dd/MM/YY HH:mm\n"), SwingUtils.getColorStdout());
        } catch (Exception ex) {
        }
        if (file.isFile() || calculateFolderSize) {
            textProperties.append("Size:     " + IO.getSize(file) / 1024 + "KB\n", SwingUtils.getColorStdout());
        }
        textProperties.setCaretPosition(0);
    }

    File currentFile;

    void setCurrentPath(File f) throws IOException {
        if ((currentFile != f) || (f == null)) {
            currentFile = f;
            if (embedded) {
                updateFileTree();
            }
        }
    }

    void updateFileTree() throws IOException {
        treeFile.setModel(new DefaultTreeModel(null));
        if ((currentFile != null) && (dataManager != null) && isRoot(currentFile)) {
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
            if (nodeSpecifier instanceof Object[] ne) {
                child = createTreeNode(ne);
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
                Color c = SwingUtils.getColorStdout();
                if (key.equals("Exception")) {
                    c = SwingUtils.getColorError();
                }
                textProperties.append(key + " = " + Str.toString(info.get(key), 100) + "\n", c);
            }

            Map<String, Object> attrs = dataManager.getAttributes(currentFile.getPath(), path);
            if (attrs.keySet().size() > 0) {
                textProperties.append("\nAttributes:\n", SwingUtils.getColorInput());
                for (String key : attrs.keySet()) {
                    Color c = SwingUtils.getColorInput();
                    if (key.equals("Exception")) {
                        c = SwingUtils.getColorError();
                    }
                    textProperties.append(key + " = " + Str.toString(attrs.get(key), 100) + "\n", c);
                }
            }
            try {
                fieldNames = (String[]) info.get(Format.INFO_FIELD_NAMES);
            } catch (Exception ex) {
                fieldNames = null;
            }
            textProperties.setCaretPosition(0);
        }
    }

    public boolean selectDataPath(String path) {
        return SwingUtils.selectTreePath(treeFile, path);
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

                if (dataManager.isExtLink(currentFile.getPath(), path)) {
                    Map<String, Object> info = dataManager.getInfo(currentFile.getPath(), path);
                    String linkRoot = (String) info.get(Format.INFO_LINK_ROOT);
                    String linkPath = (String) info.get(Format.INFO_LINK_PATH);
                    if (listener != null) {
                        DataPanel pn = (DataPanel) listener.openFile(linkRoot);
                        pn.selectDataPath(linkPath);
                    }
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
                        pageSelection.setMaxValue(dataSlice.getNumberSlices(dataManager.getDepthDimension()) - 1);
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
        }
    }

    void plotColumnData(int colY, String name, Integer colX) {
        try {
            Object y = getColumnSelection(colY);           
            if (isTableSelectionPlottable(y)) {
                if (name==null){
                    name  = getTableColHeader(colY);
                }
                double[] x = null;
                if (colX!=null){
                    Object xdata =getColumnSelection(colX);           
                    if (isTableSelectionPlottable(xdata)) {
                        x =(double[]) Convert.toDouble(xdata);
                        if (name!=null){
                            String nameX = getTableColHeader(colX);
                            if (nameX!=null){
                                name = name + " (X axis: " + nameX + ")" ;
                            }
                        }
                    }
                }
                if (listener != null) {
                    listener.plotData(y, null, name, x);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }
    
    String getTableColHeader(int index){
        try{
            if (headers != null){
                String  header  = headers[index];
                if ((header!=null) && (!header.isEmpty())){
                    return header.trim();
                }
            }
        } catch (Exception ex){                    
        }
        return null;
    }

    void plotTableSelection() {
        try {
            if (listener != null) {
                Object selection = getTableSelection();
                if (isTableSelectionPlottable(selection)) {
                    Range range = getTableSelectionRange();
                    //Simplify plotting if  showing only 1 element                
                    listener.plotData(selection, range, null, null);
                } else if (selection instanceof String name) {
                    File file = new File(name);
                    if (!file.exists()) {
                        file = Paths.get(baseFolder,name).toFile();
                    }
                    if (file.exists()) {
                        listener.openFile(file.getCanonicalPath());
                    } else {
                        DataAddress address = DataManager.getAddress(name);
                        if (address != null) {
                            File root = new File(address.root);
                            if (!root.exists()) {
                                root = Paths.get(baseFolder, address.root).toFile();
                            }
                            if (root.exists()) {
                                JPanel panel = listener.openFile(root.getCanonicalPath());
                                if (panel instanceof DataPanel dataPanel) {
                                    dataPanel.selectDataPath(address.path);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            showException(ex);
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
            if (element == null) {
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
            showException(ex);
        }
    }

    String fileName;

    public void load(String fileName) throws Exception {
        load(fileName, null, null);
    }
    
    public void load(String fileName, String format) throws Exception {
         load(fileName, format, null);
    }
    
    public void load(String fileName, String format, String layout) throws Exception {
        File file = new File(fileName);
        if ((format==null) || (format.isBlank())){
            if (file.isFile()){
               format =  IO.getExtension(file);
            }             
            if ((format==null) || (format.isBlank())){
                if (DataStore.isDefault()){
                    if (file.isFile()){
                        format = "h5";
                    } else {
                        format = "txt";
                    }
                } else {                    
                    format = DataStore.getGlobal().getFormat().getId();
                }                
            }
        } 
        if ((layout==null) || (layout.isBlank())){
            if ((!DataStore.isDefault()) && (DataStore.getGlobal() instanceof DataManager dataManager)){     
                layout = dataManager.getLayout().getId();
            } else {
                layout = "default";
            }
        }
        
        String root = fileName;
        if (IO.isSubPath(fileName, Setup.getDataPath())) {
            root = Setup.getDataPath();
        } 
        
        load (fileName, new DataManager(root, format, layout));
    }

    public void load(String fileName, DataManager dm) throws Exception {
        File file = new File(fileName);
        this.fileName = fileName;
        scrollFolder.setVisible(false);
        splitFolder.setDividerSize(0);
        dataManager = dm;
        setCurrentPath(file);        
    }
    
    public String getFileName() {
        return fileName;
    }

    class DefaultDataPanelListener implements DataPanelListener {

        Window getParent() {
            return getWindow();
        }

        @Override
        public void plotData(DataManager dataManager, String root, String path) throws Exception {
            ViewPreference.PlotPreferences prefs = dataManager.getPlotPreferences(root, path);
            try {
                plot(getParent(), dataManager.getFullPath(root, path), dataManager.getScanPlots(root, path).toArray(new PlotDescriptor[0]), prefs);
            } catch (IOException ex) {
                //If cannot open file, try with external processors
                if (!Processor.tryProcessorsPlot(root, path, dataManager)) {
                    throw ex;
                }
            }
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
            String title = "Array";
            if (name!=null){
                title = name;
            }
            if (range != null){
                title += " range: " + range.toString();
            }
            plot(getParent(), title, new PlotDescriptor[]{new PlotDescriptor(name, array, x)}, null);
        }

        @Override
        public JPanel openFile(String fileName) throws Exception {
            if (ScriptType.isScript(fileName)) {
                return openScript(new String(Files.readAllBytes(Paths.get(fileName))), fileName);
            } else {
                DataPanel panel = new DataPanel();
                panel.load(fileName);
                panel.setListener(this);
                SwingUtils.showDialog(getParent(), fileName, new Dimension(800, 600), panel);
                return panel;
            }
        }

        @Override
        public JPanel openScript(String script, String name) throws Exception {
            TextEditor editor = new TextEditor();
            editor.setText((script == null) ? "" : script);
            editor.setReadOnly(true);
            SwingUtils.showDialog(getParent(), name, new Dimension(800, 600), editor);
            return editor;
        }

    }

    public void setDefaultDataPanelListener() {
        setListener(new DefaultDataPanelListener());
    }

    public static DataPanel create(File path) {
        DataPanel panel = new DataPanel();
        try {
            if ((path != null) && (path.exists())) {
                panel.load(path.getAbsolutePath());
            } else {
                panel.initialize((String[])null); //!!! Should get parameter from somewhere?
            }
        } catch (Exception ex) {
            Logger.getLogger(DataPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        panel.setDefaultDataPanelListener();
        return panel;
    }

    public static DataPanel createDialog(Window parent, String root, String path) {
        File file = root.startsWith("/") ? new File(root)
                : Paths.get(Setup.getDataPath(), root).toFile();
        if (file.isFile()) {
            DataPanel dp = DataPanel.create(file);
            SwingUtils.showDialog(parent, root, new Dimension(1000, 600), dp);
            if ((path != null) && (!path.isBlank())) {
                dp.selectDataPath(path.trim());
            }
            return dp;
        }
        return null;
    }
    
    
    public static DataPanel createDialog(Window parent, String fileName, String format, String layout) {
        try{
            DataPanel panel = new DataPanel();
            panel.load(fileName, format, layout);
            panel.setDefaultDataPanelListener();
            panel.showFileProps();
            SwingUtils.showDialog(parent, fileName, new Dimension(800, 600), panel);
            return panel;
        } catch (Exception ex) {
            SwingUtils.showException(parent, ex);
        }
        return null;
    }
   
    List<Plot> plot(Window parent, String title, PlotDescriptor[] plots, ViewPreference.PlotPreferences preferences) throws Exception {
        return plot(plotPanel, parent, title, plots, preferences);
    }
    
    static List<Plot> plot(PlotPanel plotPanel, Window parent, String title, PlotDescriptor[] plots, ViewPreference.PlotPreferences preferences) throws Exception {
        ArrayList<Plot> ret = new ArrayList<>();
        plotPanel = (plotPanel==null) ? new PlotPanel() : plotPanel;
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

    public int getSelectedFilesCount() {
        return treeFolder.getSelectionCount();
    }

    public List<String> getSelectedFiles() {
        List<String> ret = new ArrayList<>();
        for (TreePath tp : treeFolder.getSelectionPaths()) {
            String file = getDataPath(tp);
            if (file.startsWith("/")) {
                file = file.substring(1);
            }
            ret.add(file);
        }
        return ret;
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
