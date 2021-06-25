package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.SessionManager;
import ch.psi.pshell.core.SessionManager.MetadataType;
import ch.psi.utils.IO;
import ch.psi.utils.OrderedProperties;
import ch.psi.utils.SciCat;
import ch.psi.utils.swing.Document;
import ch.psi.utils.swing.Editor;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 *
 */
public class MetadataEditor extends Editor {

    final DefaultTableModel model;
    final OrderedProperties properties;
    final String[] knownTypes;
    boolean saved;

    public MetadataEditor(String[] knownTypes) {
        super(new DevicePoolDocument());
        ((DevicePoolDocument) getDocument()).editor = this;
        initComponents();
        this.knownTypes = knownTypes;
        model = (DefaultTableModel) table.getModel();
        model.addTableModelListener((TableModelEvent e) -> {
            getDocument().setChanged(true);
            update();
        });
        update();
        properties = new OrderedProperties();
        
    }

    @Override
    protected void update() {
        boolean readOnly = isReadOnly();
        int rows = model.getRowCount();
        int cur = table.getSelectedRow();
        buttonDelete.setEnabled((rows > 0) && (cur >= 0) && (!readOnly));
        buttonInsert.setEnabled(!readOnly);
        buttonScicat.setEnabled(!readOnly);
        buttonOk.setEnabled(!readOnly && getDocument().hasChanged());
    }   

    public static class DevicePoolDocument extends Document {

        MetadataEditor editor;

        @Override
        public void clear() {
            editor.properties.clear();     
            editor.model.setNumRows(0);
            TableColumn classColumn = editor.table.getColumnModel().getColumn(1);
            JComboBox comboBoxClass = new JComboBox();
            editor.table.setRowHeight(Math.max(editor.table.getRowHeight(), comboBoxClass.getPreferredSize().height - 3));
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            for (String type : editor.knownTypes) {
                model.addElement(type);
            }
            comboBoxClass.setModel(model);
            comboBoxClass.setEditable(true);
            DefaultCellEditor cellEditor = new DefaultCellEditor(comboBoxClass);
            cellEditor.setClickCountToStart(2);
            classColumn.setCellEditor(cellEditor);
            
            setChanged(false);
            editor.update();
        }

        @Override
        public void load(String fileName) throws IOException {
            clear();

            editor.properties.clear();        
            try (FileInputStream in = new FileInputStream(fileName)) {
                editor.properties.load(in);
            } catch (Exception ex) {
                Logger.getLogger(MetadataEditor.class.getName()).log(Level.WARNING, null, ex);
            }

            for (Object key : editor.properties.keySet()){
                try {
                    String type = editor.properties.getOrDefault(key, editor.knownTypes[0]).toString().trim();
                    String def = null;
                    if (type.contains(";")){
                        def = type.substring(type.indexOf(";") + 1).trim();
                        type = type.substring(0, type.indexOf(";")).trim();                        
                    } else {
                        def = String.valueOf(SessionManager.getDefaultValue(type));
                    }
                    editor.model.addRow(new Object[]{key,type, def});
                } catch (Exception ex) {
                }            
            }            
            setChanged(false);
            editor.update();
        }

        @Override
        public void save(String fileName) throws IOException {
            editor.properties.clear(); 
            for (int i = 0; i<editor.model.getRowCount();i++ ){
                String name = editor.model.getValueAt(i, 0).toString();
                String type = editor.model.getValueAt(i, 1).toString();
                String def = editor.model.getValueAt(i, 2).toString().trim();
                if (!def.isBlank()){
                    type = type + ";" + def;
                }
                editor.properties.put(name, type);
            }
            try (FileOutputStream out = new FileOutputStream(fileName)) {
                editor.properties.store(out, null);
                IO.setFilePermissions(fileName, Context.getInstance().getConfig().filePermissionsConfig);                
            }            
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
    
    public boolean wasSaved() {
        return saved;
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
        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        buttonScicat = new javax.swing.JButton();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Type", "Default Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
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

        buttonOk.setText("Ok");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonScicat.setText("SciCat");
        buttonScicat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScicatActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonInsert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonScicat)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDelete)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonCancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonOk)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonDelete, buttonInsert, buttonOk});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel)
                    .addComponent(buttonScicat))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        model.insertRow(table.getSelectedRow() + 1, new Object[]{"", knownTypes[0], ""});
        update();
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (model.getRowCount() > 0) {
            model.removeRow(Math.max(table.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        try {
            save();
            saved = true;
            closeWindow(true);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        update();
    }//GEN-LAST:event_tableMouseReleased

    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        update();
    }//GEN-LAST:event_tableKeyReleased

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        closeWindow(true);
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonScicatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScicatActionPerformed
        try {
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout(0,10));
            JLabel label = new JLabel("Select the SciCat metadata field");     
            panel.add(label, BorderLayout.NORTH);        
            JComboBox comboBox = new JComboBox();
            ((DefaultComboBoxModel)comboBox.getModel()).addAll(SciCat.metadataFields.keySet());
            comboBox.setSelectedIndex(0);
            panel.add(comboBox, BorderLayout.CENTER);        
            if (showOption("Session", panel , SwingUtils.OptionType.OkCancel) == SwingUtils.OptionResult.Yes) {                
                String key = comboBox.getSelectedItem().toString();
                MetadataType type = SciCat.metadataFields.get(key);
                Object def = SessionManager.getDefaultValue(type);
                model.insertRow(table.getSelectedRow() + 1, new Object[]{key,type.toString(), def});
                update();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonScicatActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonScicat;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
