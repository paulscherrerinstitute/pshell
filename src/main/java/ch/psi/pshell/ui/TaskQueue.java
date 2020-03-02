package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.ui.Task.QueueExecution;
import ch.psi.pshell.ui.Task.QueueTask;
import ch.psi.utils.Arr;
import ch.psi.utils.State;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public final class TaskQueue extends MonitoredPanel implements Processor {

    String fileName;
    final DefaultTableModel model;
    boolean modified;
    Task.QueueExecution processingTask;

    public TaskQueue() {
        initComponents();
        model = (DefaultTableModel) table.getModel();
        TableModelListener modelChartsListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                modified = true;
            }
        };
        initializeTable();
    }

    public void initializeTable() {

        SwingUtils.setEnumTableColum(table, 2, Task.QueueTaskErrorAction.class);
        update();
    }

    @Override
    public void onStateChanged(State state, State former) {
        update();
    }

    @Override
    public void onTaskFinished(Task task) {
        if (task == processingTask) {
            table.getSelectionModel().clearSelection();
            //processingTask = null;
        }
    }

    protected void update() {
        boolean editing = !Context.getInstance().getState().isProcessing();
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
        return "Task Queue";
    }

    @Override
    public String getDescription() {
        return "Task queue (*.que)";
    }

    @Override
    public String[] getExtensions() {
        return new String[]{"que"};
    }

    @Override
    public void execute() throws Exception {
        ArrayList<QueueTask> queue = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String task = (String) model.getValueAt(i, 0);
            String args = (String) model.getValueAt(i, 1);
            Task.QueueTaskErrorAction errorAction = Task.QueueTaskErrorAction.valueOf(String.valueOf(model.getValueAt(i, 2)));
            QueueTask qt = new QueueTask(new File(task), args, errorAction);
            queue.add(qt);

        }
        QueueExecution task = new QueueExecution(queue.toArray(new QueueTask[0]), new Task.QueueExecutionListener() {
            @Override
            public void onStartTask(QueueTask task, int index) {
                if (processingTask != null) {
                    if ((index >= 0) && (index < model.getRowCount())) {
                        table.getSelectionModel().setSelectionInterval(index, index);
                        model.setValueAt("Running", index, 3);
                    }
                }
            }

            @Override
            public void onFinishedTask(QueueTask task, int index, Object ret, Exception ex) {
                if (processingTask != null) {
                    if ((index >= 0) && (index < model.getRowCount())) {
                        model.setValueAt((ex == null) ? "Success" : (Context.getInstance().isAborted() ? "Aborted" : "Failure"), index, 3);
                    }
                }
            }

            @Override
            public void onAborted(QueueTask task, int index, boolean userAbort) {
                if (processingTask != null) {
                    model.setValueAt(userAbort ? "Aborted" : "Failure", index, 3);
                    for (int i = index+1; i < model.getRowCount(); i++) {
                        model.setValueAt("Skipped", i, 3);
                    }
                }
            }
        });
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt("", i, 3);
        }
        App.getInstance().startTask(task);
        processingTask = task;
    }

    @Override
    public void abort() throws InterruptedException {
        Context.getInstance().abort();
    }

    @Override
    public String getHomePath() {
        return Context.getInstance().getSetup().getScriptPath();
    }

    @Override
    public void open(String fileName) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(fileName)));
        Object[][][] vector = (Object[][][]) JsonSerializer.decode(json, Object[][][].class);

        Object[][] tableData = vector[0];
        for (int i = 0; i < tableData.length; i++) {
            tableData[i] = new Object[]{tableData[i][0],
                tableData[i][1],
                Task.QueueTaskErrorAction.valueOf(String.valueOf(tableData[i][2])),
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
    public Object waitComplete(int timeout) throws Exception {
        while (App.getInstance().getRunningTask() != null) {
            Context.getInstance().waitStateNotProcessing(timeout);
        }
        return null;
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
        buttonUp = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Task Name", "Arguments", "On Error", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableMouseReleased(evt);
            }
        });
        table.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(table);

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
            .addComponent(jScrollPane1)
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
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonUp});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
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
        Object[] data = new Object[]{"", "", Task.QueueTaskErrorAction.Resume, ""};
        if (table.getSelectedRow() >= 0) {
            model.insertRow(table.getSelectedRow() + 1, data);
        } else {
            model.addRow(data);
        }
        model.fireTableDataChanged();
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
    }//GEN-LAST:event_tableMouseReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonUp;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables

}
