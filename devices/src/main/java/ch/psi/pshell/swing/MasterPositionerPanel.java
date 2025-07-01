package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.MasterPositioner;
import ch.psi.pshell.device.MasterPositionerConfig;
import ch.psi.pshell.device.MotorGroup;
import ch.psi.pshell.device.Positioner;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.swing.SwingUtils.OptionResult;
import ch.psi.pshell.swing.SwingUtils.OptionType;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Config.ConfigListener;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JDialog;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 */
public class MasterPositionerPanel extends DevicePanel {

    javax.swing.ImageIcon iconEmpty;
    javax.swing.ImageIcon iconSet;
    JDialog motorGroupDialog;
    DefaultTableModel modelInterp;
    LinePlotSeries series;
    boolean changed;

    public MasterPositionerPanel() {
        initComponents();
        plot.getAxis(Plot.AxisId.X).setLabel(null);
        plot.getAxis(Plot.AxisId.Y).setLabel(null);
        series = new LinePlotSeries("Slave");
        plot.addSeries(series);
        SwingUtils.setEnumCombo(comboMethod, MasterPositionerConfig.MODE.class);
    }

    @Override
    public MasterPositioner getDevice() {
        return (MasterPositioner) super.getDevice();
    }
    
    @Override
    public void setDevice(Device device) {
        if ((motorGroupDialog!=null) && (motorGroupDialog.isVisible())){
            motorGroupDialog.setVisible(false);
        }
        panelPv.setDevice(device);
        super.setDevice(device);
        buttonMotorGroup.setEnabled(getDevice().getMotorGroup()!=null);
        
        ArrayList<String> header = new ArrayList<>();        
        header.add(getDevice().getMaster().getName());
        for (Positioner pos: getDevice().getSlaves()){
            header.add(pos.getName());
        }
        
        modelInterp = new javax.swing.table.DefaultTableModel(new Object [][] {},header.toArray(new String[0])) {
            public Class getColumnClass(int columnIndex) {
                return Double.class;
            }
        };        
        tableInterp.setModel(modelInterp);        
        

        ArrayList<String> slaves = new ArrayList<>();
        for (Positioner dev:getDevice().getSlaves()){
            slaves.add(dev.getName());
        }
        String[] slaveNames = slaves.toArray(new String[0]);
        comboSlaves.setModel(new javax.swing.DefaultComboBoxModel(slaveNames));
        comboSlaves.setSelectedIndex(-1);
        series.clear();
        
        modelInterp.addTableModelListener((TableModelEvent e) -> {            
            if (e.getType() == TableModelEvent.UPDATE){
                try{
                    changed=true;
                    int row = e.getFirstRow();
                    double[] values = new double[modelInterp.getColumnCount()];
                    for (int i=0; i< modelInterp.getColumnCount(); i++){
                        values[i]=(Double) modelInterp.getValueAt(row, i);
                    }
                    getDevice().delInterpolationTable(row, false);
                    getDevice().addInterpolationTable(values, false);
                    update();
                    updateTable();
                    row = getDevice().getInterpolationEntry(values[0]);
                    if (row>=0){
                        tableInterp.setRowSelectionInterval(row, row);
                    }
                } catch (Exception ex){
                    showException(ex);
                }                
            }            
        });
        
        update();
        updateTable();
        
        getDevice().getConfig().addListener(new ConfigListener() {
            @Override
            public void onSave(Config config) {
                 updateTable();
                 update();
            }
        });
    }
        
    boolean updating;
    void update(){
        updating = true;
        try{
            boolean readOnly = isReadOnly();

            comboMethod.setSelectedItem(getDevice().getConfig().mode);
            comboMethod.setEnabled(!readOnly);

            int rows = modelInterp.getRowCount();
            int cur = tableInterp.getSelectedRow();

            buttonDel.setEnabled((rows > 0) && (cur >= 0) && (!readOnly));
            buttonAdd.setEnabled(!readOnly);
            buttonSave.setEnabled(!readOnly && changed);
            buttonUndo.setEnabled(changed);
        } finally{
            updating = false;
        }
    }
    
    void updateTable(){
        ArrayList<double[]> table;
        modelInterp.setRowCount(0);
        table = getDevice().getInterpolationTable();
        for (int i=0; i<table.size(); i++){
            double[] pos = table.get(i);
            modelInterp.addRow(Convert.toObjectArray(pos));
        }
        comboSlaves.setSelectedItem(null);
        series.clear();
        panelPv.setupValueSelection();
    }

    @Override
    protected void onDeviceStateChanged(State state, State former) {
    }

    @Override
    protected void onDeviceValueChanged(Object value, Object former) {
    }

    @Override
    protected void onDeviceReadbackChanged(Object value) {
    }
    
    @Override
    protected void onDesactive() {
        if (changed){
            try {
                if (SwingUtils.showOption(getFrame(),getDevice().getName(), "Configuration has changed. Do you want to save it?", OptionType.YesNo) == OptionResult.Yes) {
                    getDevice().getConfig().save();
                } else {
                    getDevice().getConfig().load();
                }
            } catch (IOException ex) {
                showException(ex);
            }
        }
        super.onDesactive();
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelPv = new ch.psi.pshell.swing.ProcessVariablePanel();
        jPanel1 = new javax.swing.JPanel();
        buttonMotorGroup = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        comboMethod = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableInterp = new javax.swing.JTable();
        buttonAdd = new javax.swing.JButton();
        buttonDel = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        buttonUndo = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        plot = new ch.psi.pshell.plot.LinePlotJFree();
        jLabel2 = new javax.swing.JLabel();
        comboSlaves = new javax.swing.JComboBox<>();

        setName("Form"); // NOI18N

        panelPv.setName("panelPv"); // NOI18N
        panelPv.setShowLimitButtons(false);

        jPanel1.setName("jPanel1"); // NOI18N

        buttonMotorGroup.setText("Motor Group Panel");
        buttonMotorGroup.setName("buttonMotorGroup"); // NOI18N
        buttonMotorGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMotorGroupActionPerformed(evt);
            }
        });

        jLabel1.setText("Method:");
        jLabel1.setName("jLabel1"); // NOI18N

        comboMethod.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboMethod.setName("comboMethod"); // NOI18N
        comboMethod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboMethodActionPerformed(evt);
            }
        });

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Interpolation Table"));
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        tableInterp.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tableInterp.setName("tableInterp"); // NOI18N
        tableInterp.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableInterp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableInterpMouseReleased(evt);
            }
        });
        tableInterp.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableInterpKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(tableInterp);

        buttonAdd.setText("Add");
        buttonAdd.setName("buttonAdd"); // NOI18N
        buttonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddActionPerformed(evt);
            }
        });

        buttonDel.setText("Delete");
        buttonDel.setName("buttonDel"); // NOI18N
        buttonDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDelActionPerformed(evt);
            }
        });

        buttonSave.setText("Save");
        buttonSave.setName("buttonSave"); // NOI18N
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        buttonUndo.setText("Undo");
        buttonUndo.setName("buttonUndo"); // NOI18N
        buttonUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUndoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboMethod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonMotorGroup))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(buttonAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonUndo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSave)))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonAdd, buttonDel, buttonSave, buttonUndo});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonMotorGroup)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(comboMethod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonAdd)
                    .addComponent(buttonDel)
                    .addComponent(buttonSave)
                    .addComponent(buttonUndo))
                .addGap(5, 5, 5))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot Axis"));
        jPanel2.setName("jPanel2"); // NOI18N

        plot.setName("plot"); // NOI18N
        plot.setTitle("");

        jLabel2.setText("Slave:");
        jLabel2.setName("jLabel2"); // NOI18N

        comboSlaves.setName("comboSlaves"); // NOI18N
        comboSlaves.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboSlavesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(plot, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboSlaves, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboSlaves, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addComponent(plot, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelPv, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelPv, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonMotorGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMotorGroupActionPerformed
        try{
            if ((motorGroupDialog!=null) && (motorGroupDialog.isVisible())){
                motorGroupDialog.requestFocus();
            } else {            
                MotorGroup dev = getDevice().getMotorGroup();
                MotorGroupPanel motorGroupPanel = new MotorGroupPanel();
                motorGroupPanel.setDevice(dev);
                motorGroupDialog = showDialog(dev.getName(), null, motorGroupPanel);
                motorGroupDialog.setMinimumSize(motorGroupDialog.getPreferredSize());                
            }            
        } catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_buttonMotorGroupActionPerformed

    private void comboSlavesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboSlavesActionPerformed
        try{
            String name = (String)comboSlaves.getSelectedItem();
            if (name!=null){
                Positioner dev = (Positioner) getDevice().getComponent(name);
                ArrayList<double[]> plot = getDevice().getInterpolationPlot(dev, 0.01);
                series.setData(plot.get(0), plot.get(1));
            }
        } catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_comboSlavesActionPerformed

    private void buttonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddActionPerformed
        try{
            getDevice().addInterpolationTable(true, false);
            changed = true;
            update();
            updateTable();
        } catch (Exception ex){
            showException(ex);
        }
                
    }//GEN-LAST:event_buttonAddActionPerformed

    private void buttonDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDelActionPerformed
        try{
            int index = tableInterp.getSelectedRow();
            changed = true;            
            getDevice().delInterpolationTable(index, false);
            update();
            updateTable();            
        } catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_buttonDelActionPerformed

    private void buttonUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUndoActionPerformed
        try{
            getDevice().getConfig().load();
            changed = false;
            update();
            updateTable();
        } catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_buttonUndoActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try{
            getDevice().getConfig().save();
            changed = false;
            update();
            updateTable();            
        } catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void comboMethodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboMethodActionPerformed
        try{
            if (!updating){
                getDevice().getConfig().mode = (MasterPositionerConfig.MODE) comboMethod.getSelectedItem();
                changed = true;
            }
        } catch (Exception ex){
            showException(ex);        
        }
    }//GEN-LAST:event_comboMethodActionPerformed

    private void tableInterpKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableInterpKeyReleased
        update();
    }//GEN-LAST:event_tableInterpKeyReleased

    private void tableInterpMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableInterpMouseReleased
        update();
    }//GEN-LAST:event_tableInterpMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAdd;
    private javax.swing.JButton buttonDel;
    private javax.swing.JButton buttonMotorGroup;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonUndo;
    private javax.swing.JComboBox<String> comboMethod;
    private javax.swing.JComboBox<String> comboSlaves;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private ch.psi.pshell.swing.ProcessVariablePanel panelPv;
    private ch.psi.pshell.plot.LinePlotJFree plot;
    private javax.swing.JTable tableInterp;
    // End of variables declaration//GEN-END:variables

}
