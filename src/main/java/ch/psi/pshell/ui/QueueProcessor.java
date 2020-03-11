package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.ui.Task.QueueExecution;
import ch.psi.pshell.ui.Task.QueueTask;
import ch.psi.utils.IO;
import ch.psi.utils.State;
import ch.psi.utils.swing.ExtensionFileFilter;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
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
public final class QueueProcessor extends MonitoredPanel implements Processor {

    String fileName;
    final DefaultTableModel model;
    boolean modified;
    Task.QueueExecution processingTask;
    final int INDEX_STATUS = 4;  

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
                    JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
                    String filename = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);         
                    addNewFile(filename, dl.getRow());                  
                } catch (Exception ex) {
                    return false;
                }                
                return true;
            }

        });
               
    }
    
    public void addNewFile(String filename){
        addNewFile(filename, model.getRowCount());
    }
    
    public void addNewFile(String filename, int index){
        for (String path : new String[]{Context.getInstance().getSetup().getScriptPath(), 
                                        Context.getInstance().getSetup().getHomePath()}) {
            if (IO.isSubPath(filename, path)) {
                filename = IO.getRelativePath(filename, path);
                break;
            }
        }        
        Object[] data = new Object[]{true, filename, "", Task.QueueTaskErrorAction.Resume, ""};
        model.insertRow(index, data);
        model.fireTableDataChanged();        
        table.getSelectionModel().setSelectionInterval(index, index);
        update();  
    }
    
    public void initializeTable() {

        class FileEditorPanel extends JPanel {

            private final JTextField field = new JTextField();
            private final Action parsEditAction = new AbstractAction("...") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        JFileChooser chooser = new JFileChooser(Context.getInstance().getSetup().getScriptPath());
                        chooser.addChoosableFileFilter(new ExtensionFileFilter("Script files (*." + Context.getInstance().getScriptType() + ")", new String[]{String.valueOf(Context.getInstance().getScriptType())}));
                        HashMap<FileNameExtensionFilter, Processor> processors = new HashMap<>();
                        for (Processor processor : Processor.getServiceProviders()) {
                            FileNameExtensionFilter filter = new FileNameExtensionFilter(processor.getDescription(), processor.getExtensions());
                            chooser.addChoosableFileFilter(filter);
                            processors.put(filter, processor);
                        }
                        chooser.setAcceptAllFileFilterUsed(true);
                        String filename = field.getText().trim();
                        File file = QueueTask.getFile(filename);
                        if (filename!=null) {
                            chooser.setSelectedFile(file);
                        }
                        int rVal = chooser.showOpenDialog(QueueProcessor.this);
                        if (rVal == JFileChooser.APPROVE_OPTION) {
                            filename = chooser.getSelectedFile().toString();
                            if (IO.isSubPath(filename, Context.getInstance().getSetup().getScriptPath())) {
                                filename = IO.getRelativePath(filename, Context.getInstance().getSetup().getScriptPath());
                            }
                            field.setText(filename);
                        }
                    } catch (Exception ex) {
                        SwingUtils.showException(QueueProcessor.this, ex);
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
                if (ev instanceof MouseEvent) {
                    //2 clicks to start
                    return ((MouseEvent) ev).getClickCount() >= 2;
                }
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject ev) {
                return false;
            }
        }
        table.getColumnModel().getColumn(1).setCellEditor(new FileEditor());

        
        class ParsEditorPanel extends JPanel {

            private final JTextField field = new JTextField();
            private final Action parsEditAction = new AbstractAction("...") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        QueueParsDialog dlg = new QueueParsDialog(SwingUtils.getFrame(QueueProcessor.this),true);
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
                        SwingUtils.showException(QueueProcessor.this, ex);
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
                editor.button.setVisible(!(String.valueOf(model.getValueAt(row, 1))).trim().isEmpty());                
                return editor;
            }

            @Override
            public Object getCellEditorValue() {
                return editor.field.getText();
            }

            @Override
            public boolean isCellEditable(EventObject ev) {
                if (ev instanceof MouseEvent) {
                    //2 clicks to start
                    return ((MouseEvent) ev).getClickCount() >= 2;
                }
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject ev) {
                return false;
            }
        }
        table.getColumnModel().getColumn(2).setCellEditor(new ParsEditor());        
        
        SwingUtils.setEnumTableColum(table, 3, Task.QueueTaskErrorAction.class);
        update();
    }

    @Override
    public void onStateChanged(State state, State former) {
        update();
    }

    protected void update() {
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
        return "Execution queue (*.que)";
    }

    @Override
    public String[] getExtensions() {
        return new String[]{"que"};
    }

    @Override
    public void execute() throws Exception {
        ArrayList<QueueTask> queue = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean enabled = (Boolean) model.getValueAt(i, 0);
            String filename = (String) model.getValueAt(i, 1);
            String args = (String) model.getValueAt(i, 2);
            Task.QueueTaskErrorAction errorAction = Task.QueueTaskErrorAction.valueOf(String.valueOf(model.getValueAt(i, 3)));
            queue.add(QueueTask.newInstance(enabled, filename, args, errorAction));

        }
        QueueExecution task = new QueueExecution(queue.toArray(new QueueTask[0]), new Task.QueueExecutionListener() {
            @Override
            public void onStartTask(QueueTask task, int index) {
                if ((index >= 0) && (index < model.getRowCount())) {
                    table.getColumnModel().getColumn(1).getCellEditor().cancelCellEditing();
                    table.getSelectionModel().setSelectionInterval(index, index);
                    model.setValueAt(task.enabled ? "Running" : "Disabled", index, INDEX_STATUS);
                }
            }

            @Override
            public void onFinishedTask(QueueTask task, int index, Object ret, Exception ex) {
                if ((index >= 0) && (index < model.getRowCount())) {
                    model.setValueAt((ex == null) ? "Success" : (Context.getInstance().isAborted() ? "Aborted" : "Failure"), index, INDEX_STATUS);
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
                update();
                if (App.getInstance().getMainFrame() != null) {
                    App.getInstance().getMainFrame().updateButtons();
                }
            }
        });
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt("", i, INDEX_STATUS);
        }
        App.getInstance().startTask(task);
        processingTask = task;
    }

    @Override
    public void abort() throws InterruptedException {
        if (processingTask != null) {
            processingTask.abort();
        }        
        Context.getInstance().abort();
    }

    @Override
    public String getHomePath() {
        return Context.getInstance().getSetup().getScriptPath();
    }

    @Override
    public boolean isExecuting() {
        return processingTask != null;
    }

    @Override
    public void open(String fileName) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(fileName)));
        Object[][][] vector = (Object[][][]) JsonSerializer.decode(json, Object[][][].class);

        Object[][] tableData = vector[0];
        for (int i = 0; i < tableData.length; i++) {            
            tableData[i] = new Object[]{
                tableData[i][0],
                tableData[i][1],
                tableData[i][2],
                Task.QueueTaskErrorAction.valueOf(String.valueOf(tableData[i][3])),
                ""};

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
        String json = JsonSerializer.encode(data, true);
        Files.write(Paths.get(fileName), json.getBytes());
        this.fileName = fileName;
        modified = false;
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
            processingTask.skip();
        }
    }

    @Override
    public Object waitComplete(int timeout) throws Exception {
        while (App.getInstance().getRunningTask() != null) {
            Context.getInstance().waitStateNotProcessing(timeout);
        }
        return null;
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
                    menuOpen.addActionListener((evt)->{
                       try {
                            App.getInstance().getMainFrame().openScriptOrProcessor(file.getCanonicalPath());
                       } catch (Exception ex) {
                           SwingUtils.showException(this, ex);
                       }
                    });
                    popupMenu.add(menuOpen);                                            
                    menuOpen.setEnabled((file!=null) && (file.exists()));
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
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

        scrollPane = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonUp = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "File ", "Arguments", "On Error", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, true, true, false
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
            update();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonUpActionPerformed

    private void buttonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownActionPerformed
        try {
            int rows = model.getRowCount();
            int cur = table.getSelectedRow();
            model.moveRow(cur, cur, cur + 1);
            table.setRowSelectionInterval(cur + 1, cur + 1);
            update();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonDownActionPerformed

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        Object[] data = new Object[]{true, "", "", Task.QueueTaskErrorAction.Resume, ""};
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
        update();
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (model.getRowCount() > 0) {
            model.removeRow(Math.max(table.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        update();
    }//GEN-LAST:event_tableKeyReleased

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        update();
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
