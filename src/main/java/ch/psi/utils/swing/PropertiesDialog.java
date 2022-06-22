package ch.psi.utils.swing;

import ch.psi.utils.Arr;
import ch.psi.utils.Config;
import ch.psi.utils.Convert;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Properties;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Dialog for editing of property files.
 */
public class PropertiesDialog extends StandardDialog {

    PropertiesPanel propertiesPanel;
    Properties properties;
    boolean readOnly;

    public PropertiesDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setResizable(true);
    }

    public void setProperties(Properties properties) {
        panel.removeAll();
        this.properties = properties;
        propertiesPanel = newPropertiesPanel();
        panel.setLayout(new BorderLayout());
        panel.add(propertiesPanel);
        propertiesPanel.setProperties(properties);
        propertiesPanel.setListener(new EditionListener() {
            @Override
            public void onChangedEditedRows(ArrayList<Integer> editedRows) {
                updateButtons();
            }
        });
        pack();
        updateButtons();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (propertiesPanel!=null){
            propertiesPanel.setReadOnly(readOnly);
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    protected PropertiesPanel newPropertiesPanel() {
        PropertiesPanel ret = new PropertiesPanel();
        ret.setReadOnly(readOnly);
        return ret;
    }

    void updateButtons() {
        boolean changed = propertiesPanel.changed.size() > 0;
        btOk.setEnabled(changed);
        btUndo.setEnabled(changed);
    }

    /**
     * The listener interface for receiving edition events.
     */
    public static interface EditionListener {

        void onChangedEditedRows(ArrayList<Integer> editedRows);
    }

    /**
     * The panel performing the elements edition. Can be added to other dialogs than
     * PropertiesDialog.
     */
    public static class PropertiesPanel extends MonitoredPanel {

        ArrayList<Integer> changed = new ArrayList<>();
        Properties properties;
        JTable table;
        RowEditor rowEditor;
        RowRenderer rowRenderer;

        public PropertiesPanel() {
            JScrollPane scrollPane = new JScrollPane();
            setLayout(new BorderLayout());
            table = new JTable();
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setResizingAllowed(true);
            scrollPane.setViewportView(table);
            add(scrollPane);
        }

        EditionListener listener;

        public void setListener(EditionListener listener) {
            this.listener = listener;
        }

        boolean readOnly;

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
            table.setEnabled(!readOnly);
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        protected void onChangedEditedRows(ArrayList<Integer> editedRows) {
            if (listener != null) {
                listener.onChangedEditedRows(editedRows);
            }
        }

        public void setProperties(Properties properties) {
            changed.clear();
            onChangedEditedRows(changed);
            if (properties == this.properties) {
                return;
            }
            this.properties = properties;
            String keys[] = Arr.sort(properties.keySet().toArray(new String[0]));
            int numberOfRows = keys.length;

            DefaultTableModel model = new DefaultTableModel();
            model = new DefaultTableModel(new Object[numberOfRows][2], new String[]{"Name", "Value"}) {
                public Class getColumnClass(int columnIndex) {
                    return Object.class;
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return (!readOnly) && (columnIndex == 1);
                }
            };

            rowEditor = new RowEditor(table, changed, new EditionListener() {
                @Override
                public void onChangedEditedRows(ArrayList<Integer> editedRows) {
                    PropertiesPanel.this.onChangedEditedRows(editedRows);
                }
            });
            rowRenderer = new RowRenderer(table, changed, this);

            int height = (new JComboBox()).getPreferredSize().height;
            if (numberOfRows > 0) {
                for (int i = 0; i < numberOfRows; i++) {
                    String key = keys[i];
                    model.setValueAt(getDisplayName(key), i, 0);
                    model.setValueAt(getPropertyValue(key), i, 1);
                    DefaultCellEditor editor = getPropertyEditor(key);
                    rowEditor.setEditorAt(i, editor);
                    height = Math.max(height, editor.getComponent().getPreferredSize().height);
                }
            }
            table.setModel(model);
            table.getColumn("Name").setCellRenderer(rowRenderer);
            table.getColumn("Value").setCellRenderer(rowRenderer);
            table.getColumn("Value").setCellEditor(rowEditor);
            setReadOnly(readOnly);
            table.setRowHeight(height);
        }

        protected Object getPropertyType(String key) {
            return String.class;
        }

        protected Object getPropertyValue(String key) {
            return properties.getProperty(key, "");
        }

        protected String getPropertyText(int row) {
            String key = getKey((String) table.getValueAt(row, 0));
            Object value = table.getValueAt(row, 1);
            return (value == null) ? "" : String.valueOf(value);
        }

        protected DefaultCellEditor getPropertyEditor(String key) {
            Object value = getPropertyValue(key);
            if (value == null) {
                value = "";
            }
            final Class type = value.getClass();

            if ((type == Boolean.class) || (type == boolean.class)) {
                JCheckBox editor = new JCheckBox();
                editor.setSelected((Boolean) value);
                return new DefaultCellEditor(editor);
            }
            if ((type.isEnum())) {
                JComboBox editor = new JComboBox();
                SwingUtils.setEnumCombo(editor, type);
                editor.setSelectedItem(value);
                return new DefaultCellEditor(editor);
            }

            final JTextField editor = new JTextField();
            editor.setText(String.valueOf(value));
            editor.setFont(table.getFont());

            for (Class c : new Class[]{Double.class, Float.class, Long.class, Integer.class, Short.class, Byte.class}) {
                if ((type == c) || (type == Convert.getPrimitiveClass(c))) {
                    return new DefaultCellEditor(editor) {
                        @Override
                        public Object getCellEditorValue() {
                            try {
                                String text = editor.getText();
                                if (Config.fromString(c, text) != null) {
                                    return text;
                                }
                            } catch (Exception ex) {
                            }
                            SwingUtils.showMessage(PropertiesPanel.this, "Type Error", "Field type is: " + c.getSimpleName().toLowerCase());
                            cancelCellEditing();
                            return null;
                        }
                    };
                }
            }
            return new DefaultCellEditor(editor);
        }

        protected Component getPropertyRenderer(String key, Object value, boolean changed, Color backColor, Component defaultComponent) {
            if (value instanceof Boolean) {
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                JCheckBox cb = new JCheckBox("", (Boolean) value);
                cb.setBackground(backColor);
                panel.add(cb);
                panel.setBorder(((JComponent) defaultComponent).getBorder());
                return panel;
            }

            if (value.getClass().isEnum()) {
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                JComboBox cb = new JComboBox();
                SwingUtils.setEnumCombo(cb, value.getClass());
                cb.setSelectedItem(value);
                if (changed) {
                    cb.setFont(cb.getFont().deriveFont(Font.BOLD));
                }
                panel.add(cb);
                return panel;
            }
            return null;
        }

        protected String getEditorValue(Component c) {
            return ((JTextField) c).getText();
        }

        protected String getDisplayName(String key) {
            return key;
        }

        protected String getKey(String displayName) {
            return displayName;
        }

        public Properties getProperties() {
            return properties;
        }

        public void stopEdit() {
            if (table.isEditing()) {
                TableCellEditor editor = table.getCellEditor();
                if (editor != null) {
                    editor.stopCellEditing();
                }
            }
        }

        public void undo() {
            stopEdit();
            changed.clear();
            onChangedEditedRows(changed);
            for (int i = 0; i < table.getRowCount(); i++) {
                try {
                    String key = getKey((String) table.getValueAt(i, 0));
                    table.setValueAt(getPropertyValue(key), i, 1);
                    table.setValueAt(getDisplayName(key), i, 0); // just to repaint
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        }

        public void apply() {
            stopEdit();
            changed.clear();
            onChangedEditedRows(changed);
            for (int i = 0; i < table.getRowCount(); i++) {
                try {
                    String key = getKey((String) table.getValueAt(i, 0));
                    properties.setProperty(key, getPropertyText(i));
                } catch (Exception ex) {
                    showException(ex);
                }
            }
            undo();//Just to repaint 
        }
    }

    static class RowRenderer implements TableCellRenderer {

        final PropertiesPanel panel;
        final JTable table;
        final ArrayList<Integer> changed;
        final DefaultTableCellRenderer defaultRenderer;

        public RowRenderer(JTable table, ArrayList<Integer> changed, PropertiesPanel panel) {
            this.table = table;
            this.changed = changed;
            this.panel = panel;
            defaultRenderer = new DefaultTableCellRenderer();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component defaultComponent = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Component component = defaultComponent;
            boolean changed = this.changed.contains(row);

            if (changed) {
                if (component.getFont() != null) {
                    component.setFont(component.getFont().deriveFont(Font.BOLD));
                }
            }

            Color backColor = table.getBackground();
            if (isSelected) {
                backColor = table.getSelectionBackground();
            } else {
                Color alternateColor = UIManager.getColor("Table.alternateRowColor");
                if (alternateColor != null) { //Nimbus                    
                    backColor = (row % 2 == 0) ? nimbusRowColor : alternateColor;
                }
            }

            if (column == 1) {
                String key = panel.getKey((String) table.getValueAt(row, 0));
                Component ret = panel.getPropertyRenderer(key, value, changed, backColor, defaultComponent);
                if (ret != null) {
                    component = ret;
                }
            }
            component.setBackground(backColor);
            //Doing this to avoid focus to be on renderer ehich avoid closing window when ESC is pressed
            table.setColumnSelectionInterval(0, 0);
            return component;
        }
    }

    static Color nimbusRowColor = Color.WHITE;

    public static void setNimbusRowColor(Color color) {
        nimbusRowColor = color;
    }

    static class RowEditor implements TableCellEditor {

        final JTable table;
        final ArrayList<Integer> changed;
        final DefaultCellEditor defaultEditor;
        final HashMap<Integer, DefaultCellEditor> editors;
        DefaultCellEditor editor;
        final EditionListener editionListener;

        public RowEditor(JTable table, ArrayList<Integer> changed, EditionListener editionListener) {
            this.table = table;
            this.changed = changed;
            editors = new HashMap<>();
            defaultEditor = new DefaultCellEditor(new JTextField());
            defaultEditor.setClickCountToStart(1);
            this.editionListener = editionListener;
        }

        public void setEditorAt(int row, DefaultCellEditor editor) {
            editor.setClickCountToStart(1);
            editors.put(row, editor);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            Component defaultComponent = defaultEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
            editor = editors.get(row);
            if (editor == null) {
                editor = defaultEditor;
            }
            Component component = editor.getTableCellEditorComponent(table, value, isSelected, row, column);
            component.setBackground(defaultComponent.getBackground());
            return component;
        }

        @Override
        public Object getCellEditorValue() {
            Object val = editor.getCellEditorValue();            
            if (val==null){                
                //If edition have been cancelled (cancelCellEditing), JTable.editingStopped()  an exception when 
                //this function returns because it does not check if  editor has been removed here.
                //Raise an exception instead so we have a more radable message
                throw new RuntimeException("Invalid value");
            }
            Object cur = table.getValueAt(table.getSelectedRow(), 1);
            if ((!val.equals(cur)) && (!val.equals(String.valueOf(cur)))) {
                changed.add(table.getSelectedRow());
                if (editionListener != null) {
                    editionListener.onChangedEditedRows(changed);
                }
            }            
            return val;
        }

        @Override
        public boolean stopCellEditing() {
            return editor.stopCellEditing();
        }

        @Override
        public void cancelCellEditing() {
            editor.cancelCellEditing();
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            selectEditor((MouseEvent) anEvent);
            return editor.isCellEditable(anEvent);
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            editor.addCellEditorListener(l);
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            editor.removeCellEditorListener(l);
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            selectEditor((MouseEvent) anEvent);
            return editor.shouldSelectCell(anEvent);
        }

        protected void selectEditor(MouseEvent e) {
            int row;
            if (e == null) {
                row = table.getSelectionModel().getAnchorSelectionIndex();
            } else {
                row = table.rowAtPoint(e.getPoint());
            }
            editor = editors.get(row);
            if (editor == null) {
                editor = defaultEditor;
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

        panel = new javax.swing.JPanel();
        btOk = new javax.swing.JButton();
        btCancel = new javax.swing.JButton();
        btUndo = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setAutoRequestFocus(false);

        panel.setPreferredSize(new java.awt.Dimension(0, 30));
        panel.setLayout(new java.awt.BorderLayout());

        btOk.setText("Ok");
        btOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btOkActionPerformed(evt);
            }
        });

        btCancel.setText("Cancel");
        btCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btCancelActionPerformed(evt);
            }
        });

        btUndo.setText("Undo");
        btUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btUndoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 103, Short.MAX_VALUE)
                        .addComponent(btOk)
                        .addGap(18, 18, 18)
                        .addComponent(btUndo)
                        .addGap(18, 18, 18)
                        .addComponent(btCancel)
                        .addGap(0, 103, Short.MAX_VALUE))
                    .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btCancel, btOk, btUndo});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btOk)
                    .addComponent(btCancel)
                    .addComponent(btUndo))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btOkActionPerformed
        if (isReadOnly()) {
            cancel();
        } else {
            propertiesPanel.apply();
            accept();
        }
    }//GEN-LAST:event_btOkActionPerformed

    private void btCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btCancelActionPerformed
        cancel();
    }//GEN-LAST:event_btCancelActionPerformed

    private void btUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btUndoActionPerformed
        propertiesPanel.undo();
    }//GEN-LAST:event_btUndoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btCancel;
    private javax.swing.JButton btOk;
    private javax.swing.JButton btUndo;
    private javax.swing.JPanel panel;
    // End of variables declaration//GEN-END:variables
}
