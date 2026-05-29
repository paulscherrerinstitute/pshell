package ch.psi.pshell.framework;

import ch.psi.pshell.sequencer.CommandBus;
import ch.psi.pshell.sequencer.CommandInfo;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Time;
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;

/**
 *
 */
public class CommandsPanel extends Panel{
    DefaultTableModel model;
    
    public CommandsPanel() {
        initComponents();
        model = (DefaultTableModel) table.getModel();
        showDetails(false);
        
        // Add key binding for Escapeto remove sorting
        InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = table.getActionMap();

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "reset-sort");
        am.put("reset-sort", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (table.getRowSorter() instanceof TableRowSorter trs) {
                    trs.setSortKeys(null);
                }
            }
        });            
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        checkUpdate = new javax.swing.JCheckBox();
        buttonSave = new javax.swing.JButton();
        splitter = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        panelDetails = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtId = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtParent = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtThread = new javax.swing.JTextField();
        txtSource = new javax.swing.JTextField();
        checkBack = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        txtCmd = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtArgs = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        txtStart = new javax.swing.JTextField();
        txtEnd = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        scrollResult = new javax.swing.JScrollPane();
        txtResult = new javax.swing.JTextArea();
        jLabel11 = new javax.swing.JLabel();
        txtStatus = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        ckeckDetails = new javax.swing.JCheckBox();
        buttonCopy = new javax.swing.JButton();

        checkUpdate.setText("Update");
        checkUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkUpdateActionPerformed(evt);
            }
        });

        buttonSave.setText("Save");
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        splitter.setResizeWeight(1.0);

        table.setAutoCreateRowSorter(true);
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Parent", "Thread", "Source", "Back", "Command", "Args", "Status", "Start", "End", "Result"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false
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

        splitter.setLeftComponent(jScrollPane1);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("ID:");

        txtId.setEditable(false);
        txtId.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Parent:");

        txtParent.setEditable(false);
        txtParent.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Source:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Thread:");

        txtThread.setEditable(false);
        txtThread.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        txtSource.setEditable(false);
        txtSource.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Cmd:");

        txtCmd.setEditable(false);
        txtCmd.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Args:");

        txtArgs.setEditable(false);
        txtArgs.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Start:");

        txtStart.setEditable(false);
        txtStart.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        txtEnd.setEditable(false);
        txtEnd.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("End:");

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText("Result:");

        txtResult.setEditable(false);
        txtResult.setColumns(20);
        txtResult.setRows(5);
        scrollResult.setViewportView(txtResult);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Status:");

        txtStatus.setEditable(false);
        txtStatus.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Back:");

        javax.swing.GroupLayout panelDetailsLayout = new javax.swing.GroupLayout(panelDetails);
        panelDetails.setLayout(panelDetailsLayout);
        panelDetailsLayout.setHorizontalGroup(
            panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDetailsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING))
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtStart)
                    .addComponent(txtEnd)
                    .addComponent(txtThread, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                    .addComponent(txtStatus)
                    .addComponent(scrollResult)
                    .addComponent(txtCmd)
                    .addComponent(txtArgs)
                    .addComponent(txtSource)
                    .addComponent(txtParent)
                    .addComponent(checkBack)
                    .addComponent(txtId))
                .addContainerGap())
        );

        panelDetailsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel10, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6, jLabel7, jLabel8, jLabel9});

        panelDetailsLayout.setVerticalGroup(
            panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelDetailsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtParent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtSource, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5)
                    .addComponent(checkBack))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtThread, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtCmd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtArgs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(txtStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtEnd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelDetailsLayout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(scrollResult, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE))
                .addContainerGap())
        );

        splitter.setRightComponent(panelDetails);

        ckeckDetails.setText("Details");
        ckeckDetails.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckeckDetailsActionPerformed(evt);
            }
        });

        buttonCopy.setText("Copy");
        buttonCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCopyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkUpdate)
                .addGap(26, 26, 26)
                .addComponent(ckeckDetails)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonCopy)
                .addGap(18, 18, 18)
                .addComponent(buttonSave)
                .addContainerGap())
            .addComponent(splitter, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(splitter)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(checkUpdate)
                    .addComponent(buttonSave)
                    .addComponent(ckeckDetails)
                    .addComponent(buttonCopy))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void checkUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkUpdateActionPerformed
        if (checkUpdate.isSelected()){
            startTimer(100,1000);
        } else {
            stopTimer();
        }
    }//GEN-LAST:event_checkUpdateActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV files", "csv");
            chooser.setFileFilter(filter);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setAcceptAllFileFilterUsed(true);            
            int rVal = chooser.showSaveDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                File file =  chooser.getSelectedFile();
                if (IO.getExtension(file).isBlank()){
                    file = new File(file.getCanonicalPath()+".csv");
                }     
                if (file.exists()){
                    if (showOption("Overwrite", "File " + file.getName() + " already exists.\nDo you want to overwrite it?", SwingUtils.OptionType.YesNo) != SwingUtils.OptionResult.Yes) {
                        return;
                    }
                }
                SwingUtils.saveTableToCsv(table, file.getAbsolutePath());
            }
        } catch (Exception ex) {
            showException(ex);
        }                  
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        updateDetails();
    }//GEN-LAST:event_tableKeyReleased

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        updateDetails();
    }//GEN-LAST:event_tableMouseReleased

    private void ckeckDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckeckDetailsActionPerformed
        showDetails(ckeckDetails.isSelected());
    }//GEN-LAST:event_ckeckDetailsActionPerformed

    private void buttonCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCopyActionPerformed
        try {
            SwingUtils.copyTableAsCsv(table);
        } catch (Exception ex) {
            showException(ex);
        }     
    }//GEN-LAST:event_buttonCopyActionPerformed

    @Override
    public void onStart() {
        super.onStart();
        update();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    
    
    @Override
    protected void onUnloaded() {
        super.onUnloaded();
    }
    
    @Override
    protected void onTimer() {    
        update();
    }
    
    protected void doUpdate() {
        CommandBus cb = Context.getCommandBus();
        List<CommandInfo> commands = cb.getCommands();
        model.setRowCount(commands.size());
        for (int i=0; i<commands.size(); i++){
            CommandInfo cmd = commands.get(i);
            model.setValueAt(cmd.id, i, 0);
            model.setValueAt((cmd.parent==null) ? null : cmd.parent.id, i, 1);            
            model.setValueAt(cmd.thread.getName(), i, 2);
            model.setValueAt(cmd.source.toString(), i, 3);
            model.setValueAt(cmd.background, i, 4);            
            if ((cmd.script!=null) && (!cmd.script.isBlank())){
                model.setValueAt(cmd.script, i, 5);
                model.setValueAt(Str.toString(cmd.args), i, 6);                
            } else {
                model.setValueAt(cmd.command, i, 5);
                model.setValueAt("", i, 6);
            }
            CommandInfo.Status status = cmd.getStatus();
            model.setValueAt(status.toString(), i, 7);
            model.setValueAt((cmd.start<=0) ? "": Time.timestampToStr(cmd.start), i, 8);
            model.setValueAt((cmd.end<=0) ? "": Time.timestampToStr(cmd.end), i, 9);
            model.setValueAt((status==CommandInfo.Status.Running) ? "" : Str.toString(cmd.getResult()), i, 10);            
        }
        updateDetails();
    }    
    
    void updateText(JTextComponent field, String str){
        String cur = field.getText();
        if ((cur==null) || ((str!=null) && !cur.equals(str))){
            field.setText(str);
            field.setCaretPosition(0);
        }
    }
   
    void updateDetails(){
        if (isDetailsVisible()){
            int row = table.getSelectedRow();
            if (row >=0){
                int i = table.convertRowIndexToModel(row);            
                updateText(txtId, Str.toString(model.getValueAt(i, 0)));
                updateText(txtParent,(model.getValueAt(i, 1)==null) ? "" : Str.toString(model.getValueAt(i, 1)));
                updateText(txtThread,Str.toString(model.getValueAt(i, 2)));
                updateText(txtSource,Str.toString(model.getValueAt(i, 3)));
                checkBack.setSelected(Str.toString(model.getValueAt(i, 3)).equalsIgnoreCase("true"));
                updateText(txtCmd,Str.toString(model.getValueAt(i, 5)));
                updateText(txtArgs,Str.toString(model.getValueAt(i, 6)));
                updateText(txtStatus,Str.toString(model.getValueAt(i, 7)));
                updateText(txtStart,(model.getValueAt(i, 8)==null) ? "" : Str.toString(model.getValueAt(i, 8)));
                updateText(txtEnd,(model.getValueAt(i, 9)==null) ? "" : Str.toString(model.getValueAt(i, 9)));
                updateText(txtResult,Str.toString(model.getValueAt(i, 10)));
            }
        }
    }
    
    boolean isDetailsVisible(){
        return splitter.getRightComponent()!=null;
    }
    
    void showDetails(boolean value) {
        if (isDetailsVisible() != value) {
            if (value) {
                splitter.setRightComponent(panelDetails);
                if ((splitter.getDividerLocation() >= splitter.getWidth() - splitter.getDividerSize() - 1)) {
                    splitter.setDividerLocation(0.70);
                }
                updateDetails();
            } else {
                splitter.setRightComponent(null);
            }
        }
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCopy;
    private javax.swing.JButton buttonSave;
    private javax.swing.JCheckBox checkBack;
    private javax.swing.JCheckBox checkUpdate;
    private javax.swing.JCheckBox ckeckDetails;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel panelDetails;
    private javax.swing.JScrollPane scrollResult;
    private javax.swing.JSplitPane splitter;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtArgs;
    private javax.swing.JTextField txtCmd;
    private javax.swing.JTextField txtEnd;
    private javax.swing.JTextField txtId;
    private javax.swing.JTextField txtParent;
    private javax.swing.JTextArea txtResult;
    private javax.swing.JTextField txtSource;
    private javax.swing.JTextField txtStart;
    private javax.swing.JTextField txtStatus;
    private javax.swing.JTextField txtThread;
    // End of variables declaration//GEN-END:variables
}
