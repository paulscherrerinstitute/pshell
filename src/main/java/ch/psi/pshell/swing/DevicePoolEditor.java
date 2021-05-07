package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.DevicePool;
import ch.psi.pshell.core.DevicePool.DeviceAttributes;
import ch.psi.pshell.device.AccessType;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.utils.swing.Document;
import ch.psi.utils.swing.Editor;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/**
 *
 */
public class DevicePoolEditor extends Editor {

    final DefaultTableModel model;

    public DevicePoolEditor() {
        super(new DevicePoolDocument());
        ((DevicePoolDocument) getDocument()).editor = this;
        initComponents();

        model = (DefaultTableModel) table.getModel();
        model.addTableModelListener((TableModelEvent e) -> {
            getDocument().setChanged(true);
            update();
        });
        update();
    }

    @Override
    protected void update() {
        boolean readOnly = isReadOnly();
        int rows = model.getRowCount();
        int cur = table.getSelectedRow();
        buttonUp.setEnabled((rows > 0) && (cur > 0) && (!readOnly));
        buttonDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)) && (!readOnly));
        buttonDelete.setEnabled((rows > 0) && (cur >= 0) && (!readOnly));
        buttonInsert.setEnabled(!readOnly);
        buttonSave.setEnabled(!readOnly && getDocument().hasChanged());
    }

    public static class DevicePoolDocument extends Document {

        DevicePoolEditor editor;

        @Override
        public void clear() {
            editor.model.setNumRows(0);
            //Fix bug of nimbus rendering Boolean in table
            ((JComponent) editor.table.getDefaultRenderer(Boolean.class)).setOpaque(true);
            editor.table.getColumnModel().getColumn(0).setResizable(true);
            editor.table.getColumnModel().getColumn(0).setPreferredWidth(60);
            editor.table.getColumnModel().getColumn(1).setPreferredWidth(100);
            editor.table.getColumnModel().getColumn(2).setPreferredWidth(275);
            editor.table.getColumnModel().getColumn(3).setPreferredWidth(225);
            editor.table.getColumnModel().getColumn(4).setPreferredWidth(52);
            editor.table.getColumnModel().getColumn(5).setPreferredWidth(52);
            editor.table.getColumnModel().getColumn(6).setPreferredWidth(68);
            editor.table.getColumnModel().getColumn(7).setPreferredWidth(70);

            //If prefering full ccontrol of AccessType:
            /*
             TableColumn accessTypeColumn = editor.table.getColumnModel().getColumn(5);
             JComboBox comboBox = new JComboBox();
             SwingUtils.setEnumCombo(comboBox, AccessType.class);
             ((DefaultComboBoxModel)comboBox.getModel()).insertElementAt(null, 0);
             accessTypeColumn.setCellEditor(new DefaultCellEditor(comboBox));
            
             //AccessType access = ((AccessType) editor.model.getValueAt(i, 6));
             */
            TableColumn classColumn = editor.table.getColumnModel().getColumn(2);
            JComboBox comboBoxClass = new JComboBox();
            editor.table.setRowHeight(Math.max(editor.table.getRowHeight(), comboBoxClass.getPreferredSize().height - 3));
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            for (String type : getKnownClassNames()) {
                model.addElement(type);
            }
            comboBoxClass.setModel(model);
            comboBoxClass.setEditable(true);
            DefaultCellEditor cellEditor = new DefaultCellEditor(comboBoxClass);
            cellEditor.setClickCountToStart(2);
            classColumn.setCellEditor(cellEditor);

            TableColumn parsColumn = editor.table.getColumnModel().getColumn(3);

            class ParsEditorPanel extends JPanel {

                private final JTextField field = new JTextField();
                private final Action parsEditAction = new AbstractAction("...") {
                    public void actionPerformed(ActionEvent e) {
                        Window parent = (Window) editor.getTopLevelAncestor();
                        DevicePoolParametersEditor dlg = null;
                        if (parent instanceof Dialog) {
                            dlg = new DevicePoolParametersEditor((Dialog) parent, true, type, field.getText(), referencedDevices);
                        } else {
                            dlg = new DevicePoolParametersEditor((Frame) parent, true, type, field.getText(), referencedDevices);
                        }
                        dlg.setVisible(true);
                        if (dlg.getResult()) {
                            field.setText(dlg.getValue());
                        }
                    }
                };
                private final JButton button = new JButton(parsEditAction);
                private Class type;
                private HashMap<String, Class> referencedDevices;

                public ParsEditorPanel() {
                    field.setBorder(null);
                    field.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editor.table.getCellEditor().stopCellEditing();
                        }
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
                                    .addComponent(field, GroupLayout.PREFERRED_SIZE, editor.table.getRowHeight(), editor.table.getRowHeight())
                                    .addComponent(button, GroupLayout.Alignment.CENTER, GroupLayout.PREFERRED_SIZE, editor.table.getRowHeight() - 2, editor.table.getRowHeight() - 2)
                    );

                }

            }

            class ParsEditor extends AbstractCellEditor implements TableCellEditor {

                private final ParsEditorPanel editor = new ParsEditorPanel();

                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    editor.field.setText((String) value);
                    editor.referencedDevices = new HashMap<>();
                    for (int i = 0; i < row; i++) {
                        try {
                            editor.referencedDevices.put((String) table.getValueAt(i, 1), Class.forName(String.valueOf(table.getValueAt(i, 2))));
                        } catch (Exception ex) {
                        }
                    }
                    try {
                        editor.type = Class.forName(String.valueOf(table.getValueAt(row, 2)));
                        if (!GenericDevice.class.isAssignableFrom(editor.type)) {
                            throw new Exception();
                        }
                        editor.button.setEnabled(true);
                    } catch (Exception ex) {
                        editor.type = null;
                        editor.button.setEnabled(false);
                    }
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
            parsColumn.setCellEditor(new ParsEditor());
            setChanged(false);
            editor.update();
        }

        String[] getKnownClassNames() {
            List<String> knownClasses = new ArrayList(Arrays.asList(new String[]{
                ch.psi.pshell.device.Averager.class.getName(),
                ch.psi.pshell.device.ArrayAverager.class.getName(),
                ch.psi.pshell.device.Delta.class.getName(),
                ch.psi.pshell.device.DummyMotor.class.getName(),
                ch.psi.pshell.device.DummyPositioner.class.getName(),
                ch.psi.pshell.device.DummyRegister.class.getName(),
                ch.psi.pshell.device.HistogramGenerator.class.getName(),
                ch.psi.pshell.device.MotorGroupBase.class.getName(),
                ch.psi.pshell.device.MotorGroupDiscretePositioner.class.getName(),
                ch.psi.pshell.device.RegisterCache.class.getName(),
                ch.psi.pshell.device.Slit.class.getName(),
                ch.psi.pshell.device.Timestamp.class.getName(),
                ch.psi.pshell.epics.AreaDetector.class.getName(),
                ch.psi.pshell.epics.BinaryPositioner.class.getName(),
                ch.psi.pshell.epics.ChannelByte.class.getName(),
                ch.psi.pshell.epics.ChannelByteArray.class.getName(),
                ch.psi.pshell.epics.ChannelByteMatrix.class.getName(),
                ch.psi.pshell.epics.ChannelDouble.class.getName(),
                ch.psi.pshell.epics.ChannelDoubleArray.class.getName(),
                ch.psi.pshell.epics.ChannelDoubleMatrix.class.getName(),
                ch.psi.pshell.epics.ChannelFloat.class.getName(),
                ch.psi.pshell.epics.ChannelFloatArray.class.getName(),
                ch.psi.pshell.epics.ChannelFloatMatrix.class.getName(),
                ch.psi.pshell.epics.ChannelInteger.class.getName(),
                ch.psi.pshell.epics.ChannelIntegerArray.class.getName(),
                ch.psi.pshell.epics.ChannelIntegerMatrix.class.getName(),
                ch.psi.pshell.epics.ChannelShort.class.getName(),
                ch.psi.pshell.epics.ChannelShortArray.class.getName(),
                ch.psi.pshell.epics.ChannelShortMatrix.class.getName(),
                ch.psi.pshell.epics.ChannelString.class.getName(),
                ch.psi.pshell.epics.ControlledVariable.class.getName(),
                ch.psi.pshell.epics.DiscretePositioner.class.getName(),
                ch.psi.pshell.epics.GenericChannel.class.getName(),
                ch.psi.pshell.epics.GenericArray.class.getName(),
                ch.psi.pshell.epics.GenericMatrix.class.getName(),
                ch.psi.pshell.epics.Manipulator.class.getName(),
                ch.psi.pshell.epics.Motor.class.getName(),
                ch.psi.pshell.epics.Positioner.class.getName(),
                ch.psi.pshell.epics.ProcessVariable.class.getName(),
                ch.psi.pshell.epics.ReadonlyProcessVariable.class.getName(),
                ch.psi.pshell.epics.Scaler.class.getName(),
                ch.psi.pshell.epics.Scienta.class.getName(),
                ch.psi.pshell.epics.Slit.class.getName(),
                ch.psi.pshell.epics.AreaDetectorSource.class.getName(),
                ch.psi.pshell.epics.ArraySource.class.getName(),
                ch.psi.pshell.epics.ByteArraySource.class.getName(),
                ch.psi.pshell.epics.PsiCamera.class.getName(),
                ch.psi.pshell.serial.SerialPortDevice.class.getName(),
                ch.psi.pshell.serial.TcpDevice.class.getName(),
                ch.psi.pshell.serial.UdpDevice.class.getName(),
                ch.psi.pshell.modbus.ModbusTCP.class.getName(),
                ch.psi.pshell.modbus.ModbusUDP.class.getName(),
                ch.psi.pshell.modbus.ModbusSerial.class.getName(),
                ch.psi.pshell.modbus.AnalogInput.class.getName(),
                ch.psi.pshell.modbus.AnalogInputArray.class.getName(),
                ch.psi.pshell.modbus.AnalogOutput.class.getName(),
                ch.psi.pshell.modbus.AnalogOutputArray.class.getName(),
                ch.psi.pshell.modbus.DigitalInput.class.getName(),
                ch.psi.pshell.modbus.DigitalInputArray.class.getName(),
                ch.psi.pshell.modbus.DigitalOutput.class.getName(),
                ch.psi.pshell.modbus.DigitalOutputArray.class.getName(),
                ch.psi.pshell.modbus.Register.class.getName(),
                ch.psi.pshell.modbus.ReadonlyProcessVariable.class.getName(),
                ch.psi.pshell.modbus.ProcessVariable.class.getName(),
                ch.psi.pshell.modbus.ControlledVariable.class.getName(),
                ch.psi.pshell.bs.Provider.class.getName(),
                ch.psi.pshell.bs.Dispatcher.class.getName(),
                ch.psi.pshell.bs.Stream.class.getName(),
                ch.psi.pshell.bs.Scalar.class.getName(),
                ch.psi.pshell.bs.Waveform.class.getName(),
                ch.psi.pshell.bs.Matrix.class.getName(),
                ch.psi.pshell.bs.StreamCamera.class.getName(),
                ch.psi.pshell.bs.CameraServer.class.getName(),
                ch.psi.pshell.bs.PipelineServer.class.getName(),
                ch.psi.pshell.imaging.CameraSource.class.getName(),
                ch.psi.pshell.imaging.ColormapAdapter.class.getName(),
                ch.psi.pshell.imaging.DirectSource.class.getName(),
                ch.psi.pshell.imaging.FileSource.class.getName(),
                ch.psi.pshell.imaging.MjpegSource.class.getName(),
                ch.psi.pshell.imaging.RegisterArraySource.class.getName(),
                ch.psi.pshell.imaging.RegisterMatrixSource.class.getName(),
                ch.psi.pshell.imaging.Webcam.class.getName(),
            }));

            for (Class type : Context.getInstance().getPluginManager().getDynamicClasses(GenericDevice.class)) {
                knownClasses.add(type.getName());
            }
            return knownClasses.toArray(new String[0]);
        }

        @Override
        public void load(String fileName) throws IOException {
            clear();

            try {
                for (String line : Files.readAllLines(Paths.get(fileName))) {
                    DeviceAttributes attr = DevicePool.parseConfigEntry(line);
                    if (attr != null) {
                        editor.model.addRow(new Object[]{attr.isEnabled(),
                            attr.getName(), attr.getClassName(),
                            String.join(" ", attr.getParameters()),
                            attr.getPolling(), attr.isMonitored(),
                            AccessType.Read.equals(attr.getAccessType()), attr.isSimulated()});
                    }
                }
            } catch (FileNotFoundException | NoSuchFileException ex) {
            }
            setChanged(false);
            editor.update();
        }

        @Override
        public void save(String fileName) throws IOException {
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> lines = new ArrayList<>();
            for (int i = 0; i < editor.model.getRowCount(); i++) {
                Boolean enabled = (Boolean) editor.model.getValueAt(i, 0);
                String name = ((String) editor.model.getValueAt(i, 1)).trim();
                String className = ((String) editor.model.getValueAt(i, 2)).trim();
                String args = ((String) editor.model.getValueAt(i, 3)).trim();
                Integer polling = (Integer) editor.model.getValueAt(i, 4);
                Boolean monitor = (Boolean) editor.model.getValueAt(i, 5);
                Boolean readonly = ((Boolean) editor.model.getValueAt(i, 6));
                Boolean simulated = (Boolean) editor.model.getValueAt(i, 7);

                if (names.contains(name)) {
                    throw new IOException("Name is used more than once: " + name);
                }
                names.add(name);
                AccessType access = (readonly != null) && (readonly) ? AccessType.Read : null;
                String config = DevicePool.createConfigEntry(name.replace(" ", "\\u0020"), enabled, simulated, className, args, access, polling, monitor);
                if (config != null) {
                    lines.add(config);
                }
            }
            Files.write(Paths.get(fileName), lines);
            setChanged(false);
            editor.update();
        }
    }

    @Override
    public void setReadOnly(boolean value) {
        table.setEnabled(!value);
        update();
    }

    @Override
    public boolean isReadOnly() {
        return !table.isEnabled();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        buttonUp = new javax.swing.JButton();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Name", "Class", "Parameters", "Polling", "Monitor", "Readonly", "Simulated"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
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
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        buttonSave.setText("Save");
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        buttonDown.setText("Move Down");
        buttonDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDownActionPerformed(evt);
            }
        });

        buttonUp.setText("Move Up");
        buttonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonInsert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDelete)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonSave, buttonUp});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonSave)
                    .addComponent(buttonDown)
                    .addComponent(buttonUp))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        model.insertRow(table.getSelectedRow() + 1, new Object[]{Boolean.TRUE, "", "", "", null, null, null, Boolean.FALSE});
        update();
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (model.getRowCount() > 0) {
            model.removeRow(Math.max(table.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try {
            save();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownActionPerformed
        try {
            int rows = model.getRowCount();
            int cur = table.getSelectedRow();
            model.moveRow(cur, cur, cur + 1);
            table.setRowSelectionInterval(cur + 1, cur + 1);
            update();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDownActionPerformed

    private void buttonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpActionPerformed
        try {
            int rows = model.getRowCount();
            int cur = table.getSelectedRow();
            model.moveRow(cur, cur, cur - 1);
            table.setRowSelectionInterval(cur - 1, cur - 1);
            update();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonUpActionPerformed

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        update();
    }//GEN-LAST:event_tableMouseReleased

    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        update();
    }//GEN-LAST:event_tableKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonUp;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
