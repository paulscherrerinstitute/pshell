package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Task;
import ch.psi.pshell.core.TaskManager;
import ch.psi.utils.IO;
import ch.psi.utils.swing.Document;
import ch.psi.utils.swing.Editor;
import ch.psi.utils.swing.SwingUtils;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class TasksEditor extends Editor {

    final DefaultTableModel model;
    final DefaultTableModel modelLoaded;
    Timer timer;

    public TasksEditor() {
        super(new TasksEditorDocument());
        ((TasksEditorDocument) getDocument()).editor = this;
        initComponents();

        model = (DefaultTableModel) table.getModel();
        model.addTableModelListener((TableModelEvent e) -> {
            getDocument().setChanged(true);
        });
        modelLoaded = (DefaultTableModel) tableLoaded.getModel();
        update();

        timer = new Timer(1000, (ActionEvent e) -> {
            try {
                if (!isShowing()) {
                    timer.stop();
                } else {
                    updateTables();
                }
            } catch (Exception ex) {
            }

        });
        timer.setInitialDelay(10);
        timer.start();

        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            updateTables();
        });

        tableLoaded.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            updateTables();
        });

        tableLoaded.getColumnModel().getColumn(0).setPreferredWidth(160);
        tableLoaded.getColumnModel().getColumn(1).setPreferredWidth(60);
        tableLoaded.getColumnModel().getColumn(2).setPreferredWidth(60);
        tableLoaded.getColumnModel().getColumn(2).setPreferredWidth(60);

    }

    void updateTables() {
        TaskManager taskManager = Context.getInstance().getTaskManager();
        if (taskManager != null) {
            Task[] tasks = taskManager.getAll();
            if (tasks.length != modelLoaded.getRowCount()) {
                modelLoaded.setNumRows(tasks.length);
            }
            for (int i = 0; i < modelLoaded.getRowCount(); i++) {
                modelLoaded.setValueAt(tasks[i].getScript(), i, 0);
                modelLoaded.setValueAt(tasks[i].getInterval() / 1000, i, 1);
                modelLoaded.setValueAt(tasks[i].isStarted(), i, 2);
                modelLoaded.setValueAt(tasks[i].isRunning(), i, 3);
            }

            String currentTaskEnabling = (table.getSelectedRow() >= 0) ? ((String) model.getValueAt(table.getSelectedRow(), 1)).trim() : null;
            Task task = taskManager.get(currentTaskEnabling);
            buttonLoad.setEnabled((currentTaskEnabling != null) && (task == null));

            String currentTaskLoaded = (tableLoaded.getSelectedRow() >= 0) ? ((String) modelLoaded.getValueAt(tableLoaded.getSelectedRow(), 0)).trim() : null;
            task = taskManager.get(currentTaskLoaded);
            buttonUnload.setEnabled(task != null);
            buttonStart.setEnabled((task != null) && !task.isStarted());
            buttonStop.setEnabled((task != null) && task.isStarted());
        }
    }

    @Override
    protected void update() {
        buttonDelete.setEnabled(model.getRowCount() > 0);
        updateTables();
    }

    public static class TasksEditorDocument extends Document {

        TasksEditor editor;

        @Override
        public void clear() {
            editor.model.setNumRows(0);
            //Fix bug of nimbus rendering Boolean in table
            ((JComponent) editor.table.getDefaultRenderer(Boolean.class)).setOpaque(true);
            editor.table.getColumnModel().getColumn(0).setResizable(true);
            editor.table.getColumnModel().getColumn(0).setPreferredWidth(70);
            editor.table.getColumnModel().getColumn(1).setPreferredWidth(203);
            editor.table.getColumnModel().getColumn(2).setPreferredWidth(70);
            setChanged(false);
        }

        @Override
        public void load(String fileName) throws IOException {
            clear();
            for (String line : Files.readAllLines(Paths.get(fileName))) {
                line = line.trim();
                String[] tokens = line.split("=");
                if (tokens.length >= 2) {
                    Boolean enabled = true;
                    String name = tokens[0].trim();
                    if (name.startsWith(IO.PROPERTY_FILE_COMMENTED_FLAG)) {
                        name = name.substring(1);
                        enabled = false;
                    }
                    String val = tokens[1].trim();
                    try {
                        Integer interval = Integer.valueOf(val);
                        editor.model.addRow(new Object[]{enabled, name, interval});
                    } catch (Exception ex) {
                    }
                }
            }
            setChanged(false);
        }

        @Override
        public void save(String fileName) throws IOException {
            ArrayList<String> lines = new ArrayList<>();
            for (int i = 0; i < editor.model.getRowCount(); i++) {
                Boolean enabled = (Boolean) editor.model.getValueAt(i, 0);
                String name = ((String) editor.model.getValueAt(i, 1)).trim();
                Integer interval = ((Integer) editor.model.getValueAt(i, 2));
                if (!name.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    if (!enabled) {
                        sb.append(IO.PROPERTY_FILE_COMMENTED_FLAG);
                    } else {
                        for (String line : lines) {
                            if (line.split("=")[0].equals(name)) {
                                throw new IOException("Script is used more than once: " + name);
                            }
                        }
                    }
                    sb.append(name).append("=");
                    sb.append(String.valueOf(interval));
                    lines.add(sb.toString());
                }
            }
            Files.write(Paths.get(fileName), lines);
            setChanged(false);
        }
    }

    @Override
    public void setReadOnly(boolean value) {
        table.setEnabled(!value);
        buttonInsert.setEnabled(!value);
        buttonDelete.setEnabled(!value);
    }

    @Override
    public boolean isReadOnly() {
        return !table.isEnabled();
    }

    void run(String taskName) throws Exception {
        if (taskName != null) {
            Task task = Context.getInstance().getTaskManager().get(taskName);
            if (task != null) {
                task.run();
            } else {
                Context.getInstance().getTaskManager().create(taskName, 0, -1);
                Context.getInstance().getTaskManager().start(taskName);
                Context.getInstance().getTaskManager().get(taskName).waitRunning(3000);
                Context.getInstance().getTaskManager().remove(taskName, false);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();
        butonSave = new javax.swing.JButton();
        buttonLoad = new javax.swing.JButton();
        buttonRun = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableLoaded = new javax.swing.JTable();
        buttonStart = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();
        buttonUnload = new javax.swing.JButton();
        buttonRunLoaded = new javax.swing.JButton();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Script", "Interval (s)"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setResizable(false);
        }

        buttonInsert.setText("Insert");
        buttonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });

        buttonDelete.setText("Delete");
        buttonDelete.setMinimumSize(new java.awt.Dimension(86, 0));
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        butonSave.setText("Save");
        butonSave.setMinimumSize(new java.awt.Dimension(86, 0));
        butonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butonSaveActionPerformed(evt);
            }
        });

        buttonLoad.setText("Load");
        buttonLoad.setMinimumSize(new java.awt.Dimension(86, 0));
        buttonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadActionPerformed(evt);
            }
        });

        buttonRun.setText("Run");
        buttonRun.setMinimumSize(new java.awt.Dimension(86, 0));
        buttonRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRunActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(buttonInsert, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(buttonDelete, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(butonSave, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(buttonLoad, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonRun, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {butonSave, buttonDelete, buttonInsert, buttonLoad, buttonRun});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonInsert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDelete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(buttonLoad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRun, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 53, Short.MAX_VALUE)
                .addComponent(butonSave, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Enabling", jPanel2);

        tableLoaded.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Script", "Interval (s)", "Started", "Running"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableLoaded.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(tableLoaded);
        if (tableLoaded.getColumnModel().getColumnCount() > 0) {
            tableLoaded.getColumnModel().getColumn(0).setResizable(false);
        }

        buttonStart.setText("Start");
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });

        buttonStop.setText("Stop");
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        buttonUnload.setText("Unload");
        buttonUnload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUnloadActionPerformed(evt);
            }
        });

        buttonRunLoaded.setText("Run");
        buttonRunLoaded.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRunLoadedActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonStart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonStop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonUnload, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonRunLoaded))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonRunLoaded, buttonStart, buttonStop, buttonUnload});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonStart)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonStop)
                .addGap(18, 18, 18)
                .addComponent(buttonUnload)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRunLoaded)
                .addContainerGap(91, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        jTabbedPane1.addTab("Loaded", jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        model.insertRow(table.getSelectedRow() + 1, new Object[]{Boolean.FALSE, "", 10});
        update();
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (model.getRowCount() > 0) {
            model.removeRow(Math.max(table.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartActionPerformed
        try {
            String task = (tableLoaded.getSelectedRow() >= 0) ? ((String) modelLoaded.getValueAt(tableLoaded.getSelectedRow(), 0)).trim() : null;
            if (task != null) {
                Context.getInstance().getTaskManager().start(task);
            }
            updateTables();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonStartActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        try {
            String task = (tableLoaded.getSelectedRow() >= 0) ? ((String) modelLoaded.getValueAt(tableLoaded.getSelectedRow(), 0)).trim() : null;
            if (task != null) {
                Context.getInstance().getTaskManager().stop(task, true);
            }
            updateTables();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonStopActionPerformed

    private void butonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butonSaveActionPerformed
        try {
            save();
        } catch (IOException ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_butonSaveActionPerformed

    private void buttonUnloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUnloadActionPerformed
        try {
            String task = (tableLoaded.getSelectedRow() >= 0) ? ((String) modelLoaded.getValueAt(tableLoaded.getSelectedRow(), 0)).trim() : null;
            if (task != null) {
                Context.getInstance().getTaskManager().remove(task, true);
            }
            updateTables();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonUnloadActionPerformed

    private void buttonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadActionPerformed
        try {
            String task = (table.getSelectedRow() >= 0) ? ((String) model.getValueAt(table.getSelectedRow(), 1)).trim() : null;
            if (task != null) {
                Integer interval = (Integer) model.getValueAt(table.getSelectedRow(), 2);
                Context.getInstance().getTaskManager().create(task, interval, interval);
                Context.getInstance().getTaskManager().start(task);
            }
            updateTables();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonLoadActionPerformed

    private void buttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunActionPerformed
        try {
            String task = (table.getSelectedRow() >= 0) ? ((String) model.getValueAt(table.getSelectedRow(), 1)).trim() : null;
            run(task);
            updateTables();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonRunActionPerformed

    private void buttonRunLoadedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunLoadedActionPerformed
        try {
            String task = (tableLoaded.getSelectedRow() >= 0) ? ((String) modelLoaded.getValueAt(tableLoaded.getSelectedRow(), 0)).trim() : null;
            run(task);
            updateTables();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonRunLoadedActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton butonSave;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonLoad;
    private javax.swing.JButton buttonRun;
    private javax.swing.JButton buttonRunLoaded;
    private javax.swing.JButton buttonStart;
    private javax.swing.JButton buttonStop;
    private javax.swing.JButton buttonUnload;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable table;
    private javax.swing.JTable tableLoaded;
    // End of variables declaration//GEN-END:variables
}
