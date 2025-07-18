package ch.psi.pshell.framework;

import ch.psi.pshell.swing.ExtensionFileFilter;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.EncoderJson;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.State;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

/**
 *
 */
public final class QueueProcessor extends PanelProcessor {

    public static final String  EXTENSION = "que";
    public static String DEFAULT_INFO_COLUMN = null;
    
    String fileName;
    DefaultTableModel model;
    boolean modified;
    QueueExecution processingTask;
    public final int INDEX_ENABLED = 0;
    public final int INDEX_FILE = 1;
    public final int INDEX_ARGS = 2;
    public final int INDEX_ON_ERROR = 3;
    public final int INDEX_STATUS = 4;
    public final int INDEX_INFO = 5;

    String infoColumn;

    DefaultTableModel createModel(String infoColumn) {
        this.infoColumn = infoColumn;
        String[] tit = new String[]{"Enabled", "File ", "Arguments", "On Error", "Status"};
        Class[] types = new Class[]{java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class};
        Boolean[] canEdit = new Boolean[]{true, true, true, true, false, false};
        if (infoColumn != null) {
            tit = Arr.append(tit, infoColumn);
        }
        return new DefaultTableModel(new Object[][]{}, tit) {
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };
    }
    
    final String homePath;

    public QueueProcessor() {
        initComponents();
        model = (DefaultTableModel) table.getModel();
        TableModelListener modelChartsListener = (TableModelEvent e) -> {
            modified = true;
        };
        model.addTableModelListener(modelChartsListener);
        initializeTable();
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setFillsViewportHeight(true);
        table.setTransferHandler(new TransferHandler() {

            @Override
            public int getSourceActions(JComponent c) {
                return DnDConstants.ACTION_COPY_OR_MOVE;
            }

            @Override
            public boolean canImport(TransferHandler.TransferSupport info) {
                return info.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {

                if (!support.isDrop()) {
                    return false;
                }

                if (!canImport(support)) {
                    return false;
                }

                try {
                    JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
                    String filename = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    addNewFile(filename, dl.getRow());
                } catch (Exception ex) {
                    return false;
                }
                return true;
            }

        });
        
        homePath = Setup.getQueuePath();
    }        

    public void setTableInfoCol(String name) {
        table.setModel(createModel(name));
        model = (DefaultTableModel) table.getModel();
        TableModelListener modelChartsListener = (TableModelEvent e) -> {
            modified = true;
        };
        model.addTableModelListener(modelChartsListener);
        initializeTable();
        clear();
    }

    public String getTableInfoCol() {
        return infoColumn;
    }

    public void addNewFile(String filename) {
        addNewFile(filename, model.getRowCount());
    }

    public void addNewFile(String filename, int index) {
        addNewFile(filename, "", index);
    }

    public void addNewFile(String filename, String args) {
        addNewFile(filename, args, model.getRowCount());
    }
    
    public void addNewFile(String filename, Map<String, Object> args) throws IOException {
        addNewFile(filename, (args==null) ? "" : encodeArgs(args));
    }

    public void addNewFile(String filename, String args, int index) {
        addNewFile(filename, args, index, null);
    }

    public void addNewFile(String filename, Map<String, Object> args, int index) throws IOException {
        addNewFile(filename, (args==null) ? "" : encodeArgs(args), index);
    }
    
    public void addNewFile(String filename, String args, String info) {
        addNewFile(filename, args, model.getRowCount(), info);
    }

    public void addNewFile(String filename, Map<String, Object> args, String info) throws IOException {
        addNewFile(filename, (args==null) ? "" : encodeArgs(args), info);
    }

    public void addNewFile(String filename, String args, int index, String info) {
        if (args==null){
            args = "";
        }
        if (filename != null) {
            for (String path : new String[]{Setup.getScriptsPath(),
                Setup.getHomePath()}) {
                if (IO.isSubPath(filename, path)) {
                    filename = IO.getRelativePath(filename, path);
                    break;
                }
            }
        }
        Object[] data = new Object[]{true, filename, args, QueueTask.QueueTaskErrorAction.Resume, ""};
        if ((info != null) && (getTableInfoCol() != null)) {
            data = Arr.append(data, info);
        }
        model.insertRow(index, data);
        model.fireTableDataChanged();
        table.getSelectionModel().setSelectionInterval(index, index);
        updateButtons();
    }
    
    public void addNewFile(String filename, Map<String, Object> args, int index,String info) throws IOException {
        addNewFile(filename, (args==null) ? "" : encodeArgs(args), index, info);
    }    

    public JTable getTable() {
        return table;
    }

    public DefaultTableModel getModel() {
        return model;
    }

    public void setTableTitle(int column, String title) {
        SwingUtils.setColumTitle(table, column, title);
    }

    public String getTableTitle(int column) {
        return model.getColumnName(column);
    }
    
    public String getRowInfo(int row){
        if (getTableInfoCol()==null){
            return null;
        }
        return (String) model.getValueAt(row, INDEX_INFO);
    }

    public void setRowInfo(int row, String info){
        if (getTableInfoCol()!=null){
            model.setValueAt(info, row, INDEX_INFO);
        }
    }

    public void initializeTable() {
        class FileEditorPanel extends JPanel {

            private final JTextField field = new JTextField();
            private final Action parsEditAction = new AbstractAction("...") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        JFileChooser chooser = new JFileChooser(Setup.getScriptsPath());
                        chooser.addChoosableFileFilter(new ExtensionFileFilter("Script files (*." + Context.getScriptType().getExtension() + ")",
                                new String[]{String.valueOf(Context.getScriptType().getExtension())}));
                        HashMap<FileNameExtensionFilter, Processor> processors = new HashMap<>();
                        for (Processor processor : Processor.getServiceProviders()) {
                            FileNameExtensionFilter filter = new FileNameExtensionFilter(processor.getDescription(), processor.getExtensions());
                            chooser.addChoosableFileFilter(filter);
                            processors.put(filter, processor);
                        }
                        chooser.setAcceptAllFileFilterUsed(true);
                        String filename = field.getText().trim();
                        File file = QueueTask.getFile(filename);
                        if (filename != null) {
                            chooser.setSelectedFile(file);
                        }
                        int rVal = chooser.showOpenDialog(QueueProcessor.this);
                        if (rVal == JFileChooser.APPROVE_OPTION) {
                            filename = chooser.getSelectedFile().toString();
                            if (IO.isSubPath(filename, Setup.getScriptsPath())) {
                                filename = IO.getRelativePath(filename, Setup.getScriptsPath());
                            }
                            field.setText(filename);
                        }
                    } catch (Exception ex) {
                        showException(ex);
                    }
                }
            };
            private final JButton button = new JButton(parsEditAction);
            private Class type;
            private HashMap<String, Class> referencedDevices;

            public FileEditorPanel() {
                field.setBorder(null);
                field.addActionListener((ActionEvent e) -> {
                    table.getCellEditor().stopCellEditing();
                });
                button.setBorder(javax.swing.BorderFactory.createEtchedBorder());

                GroupLayout layout = new GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(field)
                                        .addComponent(button, 20, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(field, GroupLayout.PREFERRED_SIZE, table.getRowHeight(), table.getRowHeight())
                                .addComponent(button, GroupLayout.Alignment.CENTER, GroupLayout.PREFERRED_SIZE, table.getRowHeight() - 2, table.getRowHeight() - 2)
                );

            }

        }

        class FileEditor extends AbstractCellEditor implements TableCellEditor {

            private final FileEditorPanel editor = new FileEditorPanel();

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                editor.field.setText((String) value);
                return editor;
            }

            @Override
            public Object getCellEditorValue() {
                return editor.field.getText();
            }

            @Override
            public boolean isCellEditable(EventObject ev) {
                if (ev instanceof MouseEvent mouseEvent) {
                    //2 clicks to start
                    return mouseEvent.getClickCount() >= 2;
                }
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject ev) {
                return false;
            }
        }
        table.getColumnModel().getColumn(INDEX_FILE).setCellEditor(new FileEditor());

        class ParsEditorPanel extends JPanel {

            private final JTextField field = new JTextField();
            private final Action parsEditAction = new AbstractAction("...") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        QueueParsDialog dlg = new QueueParsDialog(getFrame(), true);
                        dlg.setLocationRelativeTo(QueueProcessor.this);
                        dlg.setText(field.getText());
                        dlg.setVisible(true);
                        if (dlg.getResult()) {
                            field.setText(dlg.getText());

                            TableCellEditor editor = table.getCellEditor();
                            if (editor != null) {
                                editor.stopCellEditing();
                            }
                            model.fireTableDataChanged();
                        }

                    } catch (Exception ex) {
                        showException(ex);
                    }
                }
            };
            private final JButton button = new JButton(parsEditAction);
            private Class type;
            private HashMap<String, Class> referencedDevices;

            public ParsEditorPanel() {
                field.setBorder(null);
                field.addActionListener((ActionEvent e) -> {
                    table.getCellEditor().stopCellEditing();
                });
                button.setBorder(javax.swing.BorderFactory.createEtchedBorder());

                GroupLayout layout = new GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(field)
                                        .addComponent(button, 20, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(field, GroupLayout.PREFERRED_SIZE, table.getRowHeight(), table.getRowHeight())
                                .addComponent(button, GroupLayout.Alignment.CENTER, GroupLayout.PREFERRED_SIZE, table.getRowHeight() - 2, table.getRowHeight() - 2)
                );

            }

        }

        class ParsEditor extends AbstractCellEditor implements TableCellEditor {

            private final ParsEditorPanel editor = new ParsEditorPanel();

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                editor.field.setText((String) value);
                editor.button.setVisible(!(String.valueOf(model.getValueAt(row, INDEX_FILE))).isBlank());
                return editor;
            }

            @Override
            public Object getCellEditorValue() {
                return editor.field.getText();
            }

            @Override
            public boolean isCellEditable(EventObject ev) {
                if (ev instanceof MouseEvent mouseEvent) {
                    //2 clicks to start
                    return mouseEvent.getClickCount() >= 2;
                }
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject ev) {
                return false;
            }
        }
        table.getColumnModel().getColumn(INDEX_ARGS).setCellEditor(new ParsEditor());

        SwingUtils.setEnumTableColum(table, INDEX_ON_ERROR, QueueTask.QueueTaskErrorAction.class);
        updateButtons();
    }

    @Override
    public void onStateChanged(State state, State former) {
        updateButtons();
    }

    protected void updateButtons() {
        boolean editing = !isExecuting(); //!Context.getInstance().getState().isProcessing();
        int rows = model.getRowCount();
        int cur = table.getSelectedRow();
        buttonUp.setEnabled((rows > 0) && (cur > 0) && editing);
        buttonDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)) && editing);
        buttonDelete.setEnabled((rows > 0) && (cur >= 0) && editing);
        buttonInsert.setEnabled(editing);
        table.setEnabled(editing);
    }

    @Override
    public String getType() {
        return "Queue";
    }

    @Override
    public String getDescription() {
        return "Execution queue (*." + EXTENSION + ")";
    }

    @Override
    public String[] getExtensions() {
        return new String[]{EXTENSION};
    }
    
    @Override
    public boolean createFilePanel() {
        return  Context.getView().getPreference("showQueueBrowser")==Boolean.TRUE ||  App.hasAditionalArgument("queue_panel");
    }    

    @Override
    public void execute() throws Exception {
        ArrayList<QueueTask> queue = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean enabled = (Boolean) model.getValueAt(i, INDEX_ENABLED);
            String filename = (String) model.getValueAt(i, INDEX_FILE);
            String args = (String) model.getValueAt(i, INDEX_ARGS);
            QueueTask.QueueTaskErrorAction errorAction = QueueTask.QueueTaskErrorAction.valueOf(String.valueOf(model.getValueAt(i, INDEX_ON_ERROR)));
            queue.add(QueueTask.newInstance(enabled, filename, args, errorAction));

        }
        QueueExecution task = new QueueExecution(queue.toArray(new QueueTask[0]), new QueueExecution.QueueExecutionListener() {
            @Override
            public void onStartTask(QueueTask task, int index) {
                if ((index >= 0) && (index < model.getRowCount())) {
                    table.getColumnModel().getColumn(INDEX_FILE).getCellEditor().cancelCellEditing();
                    table.getSelectionModel().setSelectionInterval(index, index);
                    model.setValueAt(task.enabled ? "Running" : "Disabled", index, INDEX_STATUS);
                }
            }

            @Override
            public void onFinishedTask(QueueTask task, int index, Object ret, Exception ex) {
                if ((index >= 0) && (index < model.getRowCount())) {
                    model.setValueAt((ex == null) ? "Success" : (Context.isAborted() ? "Aborted" : "Failure"), index, INDEX_STATUS);
                }
            }

            @Override
            public void onAborted(QueueTask task, int index, boolean userAbort, boolean skipped) {
                if (skipped) {
                    model.setValueAt("Skipped", index, INDEX_STATUS);
                } else {
                    model.setValueAt(userAbort ? "Aborted" : "Failure", index, INDEX_STATUS);
                    //for (int i = index + 1; i < model.getRowCount(); i++) {
                    //    model.setValueAt("Skipped", i, INDEX_STATUS);
                    //}
                }
            }

            @Override
            public void onFinishedExecution(QueueTask task) {
                table.getSelectionModel().clearSelection();
                processingTask = null;
                updateButtons();
                if (Context.hasView()) {
                    Context.getView().updateViewState();
                }
            }
        });
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt("", i, INDEX_STATUS);
        }
        Context.getApp().startTask(task);
        processingTask = task;
    }

    @Override
    public void abort() throws InterruptedException {
        if (processingTask != null) {
            processingTask.abort();
        }
        Context.abort();
    }

    @Override
    public String getHomePath() {
        return homePath;
    }

    @Override
    public boolean isExecuting() {
        return processingTask != null;
    }

    @Override
    public void open(String fileName) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(fileName)));
        Object[][][] vector = (Object[][][]) EncoderJson.decode(json, Object[][][].class);

        Object[][] tableData = vector[0];
        for (int i = 0; i < tableData.length; i++) {
            Object info = (tableData[i].length > 5) ? tableData[i][5] : null;
            tableData[i] = new Object[]{
                tableData[i][0],
                tableData[i][1],
                tableData[i][2],
                QueueTask.QueueTaskErrorAction.valueOf(String.valueOf(tableData[i][3])),
                ""};
            if ((info!=null) && (getTableInfoCol()!=null)){
                tableData[i] = Arr.append(tableData[i], info);
            }

        }
        model.setDataVector(tableData, SwingUtils.getTableColumnNames(table));
        this.fileName = fileName;
        modified = false;
        initializeTable();
    }

    @Override
    public void saveAs(String fileName) throws IOException {
        ArrayList data = new ArrayList();
        data.add(model.getDataVector());
        String json = EncoderJson.encode(data, true);
        Files.write(Paths.get(fileName), json.getBytes());
        this.fileName = fileName;
        modified = false;
    }

    @Override
    public void clear() {
        model.setRowCount(0);
        updateButtons();
    }

    @Override
    public boolean hasChanged() {
        return modified;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean createMenuNew() {
        return true;
    }

    @Override
    public boolean canStep() {
        return (processingTask != null) && (table.getSelectionModel().getMinSelectionIndex() < (model.getRowCount()));
    }

    @Override
    public void step() {
        if (processingTask != null) {
            Logger.getLogger(QueueProcessor.class.getName()).warning("Skipping task: " + processingTask.getCurrentIndex());
            processingTask.skip();

        }
    }

    private void checkPopup(MouseEvent e) {
        try {
            if (e.isPopupTrigger()) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && row < table.getRowCount()) {
                    table.setRowSelectionInterval(row, row);
                    File file = QueueTask.getFile(String.valueOf(model.getValueAt(table.getSelectedRow(), 1)));
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem menuOpen = new JMenuItem("Open File");
                    menuOpen.addActionListener((evt) -> {
                        try {
                            Context.getView().openScriptOrProcessor(file.getCanonicalPath());
                        } catch (Exception ex) {
                            showException(ex);
                        }
                    });
                    popupMenu.add(menuOpen);
                    menuOpen.setEnabled((file != null) && (file.exists()));
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }

    }

    public String encodeArgs(Map<String, Object> args) throws IOException {
        //Non-encodable fields such as plots are set to null
        Map<String, Object> ret = new HashMap<>();
        ret.putAll(args);
        for (Object s: ret.keySet().toArray()){
            try{                
                EncoderJson.encode(ret.get(s), false);
            } catch (Exception ex) {
                ret.put(s.toString(), null);
            }
        }
        return EncoderJson.encode(ret, false);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonUp = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();

        table.setModel(createModel(DEFAULT_INFO_COLUMN));
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tableMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableMouseReleased(evt);
            }
        });
        table.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableKeyReleased(evt);
            }
        });
        scrollPane.setViewportView(table);

        buttonUp.setText("Move Up");
        buttonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpActionPerformed(evt);
            }
        });

        buttonDown.setText("Move Down");
        buttonDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDownActionPerformed(evt);
            }
        });

        buttonInsert.setText("Insert");
        buttonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });

        buttonDelete.setText("Delete");
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonInsert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDelete)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(scrollPane)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonUp});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonDown)
                    .addComponent(buttonUp))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpActionPerformed
        try {
            int rows = model.getRowCount();
            int cur = table.getSelectedRow();
            model.moveRow(cur, cur, cur - 1);
            table.setRowSelectionInterval(cur - 1, cur - 1);
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonUpActionPerformed

    private void buttonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownActionPerformed
        try {
            int rows = model.getRowCount();
            int cur = table.getSelectedRow();
            model.moveRow(cur, cur, cur + 1);
            table.setRowSelectionInterval(cur + 1, cur + 1);
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDownActionPerformed

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        Object[] data = new Object[]{true, "", "", QueueTask.QueueTaskErrorAction.Resume, ""};
        int index;
        if (table.getSelectedRow() >= 0) {
            index = table.getSelectedRow() + 1;
            model.insertRow(index, data);
        } else {
            model.addRow(data);
            index = model.getRowCount();
        }
        model.fireTableDataChanged();
        table.getSelectionModel().setSelectionInterval(index, index);
        updateButtons();
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (model.getRowCount() > 0) {
            model.removeRow(Math.max(table.getSelectedRow(), 0));
            updateButtons();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        updateButtons();
    }//GEN-LAST:event_tableKeyReleased

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        updateButtons();
        checkPopup(evt);
    }//GEN-LAST:event_tableMouseReleased

    private void tableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMousePressed
        checkPopup(evt);
    }//GEN-LAST:event_tableMousePressed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonUp;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables

}
