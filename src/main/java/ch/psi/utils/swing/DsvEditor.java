package ch.psi.utils.swing;

import ch.psi.utils.Convert;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.undo.UndoManager;

/**
 * Editor for files containing delimiter-separated values, displayed in a table. Can also edit
 * property files, setting the property name to the first column.
 */
public class DsvEditor extends Editor {

    UndoManager undoManager;
    Integer contentWidth;
    final String[] columns;
    final Class[] types;
    final boolean[] editable;
    final String separator;
    final boolean persistHeader;
    final DefaultTableModel model;

    /**
     * Implementation of DSV (delimiter-separated values) documents .
     */
    public static class DsvDocument extends Document {

        DsvEditor editor;

        DsvDocument() {
        }

        @Override
        public void clear() {
            editor.setData(null);
            setChanged(false);
        }

        @Override
        public void load(String fileName) throws IOException {
            ArrayList<Object[]> data = new ArrayList();
            try {
                boolean properties = editor.isPropertiesFile();
                for (String line : Files.readAllLines(Paths.get(fileName))) {
                    if (properties) {
                        line = line.replaceFirst("=", editor.separator);
                    }
                    String[] tokens = line.split(editor.separator);
                    for (int i = 0; i < tokens.length; i++) {
                        tokens[i] = tokens[i].trim();
                    }
                    data.add(tokens);
                }
                if (editor.persistHeader) {
                    data.remove(0);
                }
                editor.setData(data);
            } catch (NoSuchFileException ex) {
                clear();
            }
            setChanged(false);
        }

        @Override
        public void save(String fileName) throws IOException {
            Files.write(Paths.get(fileName), getLines());
            setChanged(false);
        }

        List<String> getLines() {
            boolean properties = editor.isPropertiesFile();
            List<Object[]> data = editor.getData();
            List<String> lines = new ArrayList<>();
            if (editor.persistHeader) {
                lines.add(String.join(editor.separator, editor.columns));
            }
            for (Object[] row : data) {
                String[] rowStr = Convert.toStringArray(row);
                String str = String.join(editor.separator, rowStr);
                if (properties) {
                    str = str.replaceFirst(editor.separator, "=");
                }
                lines.add(str);
            }
            return lines;
        }

        @Override
        public String getContents() {
            return String.join("\n", getLines());
        }

    }

    public String getSeparator() {
        return separator;
    }

    public DsvEditor(String[] columns) {
        this(columns, null);
    }

    public DsvEditor(String[] columns, Class[] types) {
        this(columns, types, ";");
    }

    public DsvEditor(String[] columns, Class[] types, String separator) {
        this(columns, types, separator, false);
    }

    public DsvEditor(String[] columns, Class[] types, String separator, boolean persistHeader) {
        super(new DsvDocument());        
        this.columns = columns;
        this.types = types;
        this.separator = separator;
        this.persistHeader = persistHeader;
        this.editable = new boolean[columns.length];
        for (int i = 0; i < editable.length; i++) {
            editable[i] = true;
        }
        ((DsvDocument) getDocument()).editor = this;
        initComponents();

        JMenuItem menuFont;
        menuFont = new JMenuItem("Set font...");
        menuFont.addActionListener((ActionEvent e) -> {
            FontDialog dlg = new FontDialog(SwingUtils.getFrame(this), true, getTableFont());
            dlg.setVisible(true);
            if (dlg.getResult()) {
                setTableFont(dlg.getSelectedFont());
            }
        });
        getPopupMenu().addSeparator();
        getPopupMenu().add(menuFont);

        model = new DefaultTableModel(new Object[][]{}, columns) {
            public Class getColumnClass(int columnIndex) {
                return (types == null) ? String.class : types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return (!readOnly) && editable[columnIndex];
            }
        };
        table.setModel(model);
        model.addTableModelListener((TableModelEvent e) -> {
            getDocument().setChanged(true);
        });
        table.setComponentPopupMenu(menuPopup);
        setShowClearButton(false);
    }
    
    public boolean getShowSaveButton(){
        return buttonSave.isVisible();
    }

    public void setShowSaveButton(boolean value){
        buttonSave.setVisible(value);
    }
    
    public boolean getShowClearButton(){
        return buttonClear.isVisible();
    }

    public void setShowClearButton(boolean value){
        buttonClear.setVisible(value);
    }    

    private void updateButtons() {
        int rows = model.getRowCount();
        int cur = table.getSelectedRow();
        buttonUp.setEnabled((rows > 0) && (cur > 0));
        buttonDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)));
        buttonDelete.setEnabled(rows > 0);
    }

    Object[] getRow(String[] data) {
        if (data == null) {
            data = new String[columns.length];
        }
        Object[] ret = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            try {
                ret[i] = null;
                boolean empty = ((data == null) || (i >= data.length) || (data[i] == null));
                if ((types == null) || (types[i] == String.class)) {
                    String val = empty ? "" : data[i];
                    ret[i] = val;
                } else if (Number.class.isAssignableFrom(types[i])) {
                    String val = empty ? "0" : data[i];
                    ret[i] = types[i].getConstructor(String.class).newInstance(val);
                } else if (types[i] == Boolean.class) {
                    String val = empty ? "false" : data[i];
                    ret[i] = Boolean.valueOf(val);
                }
            } catch (Exception ex) {
            }
        }
        return ret;
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
        sidePanel = new javax.swing.JPanel();
        buttonUp = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        buttonInsert = new javax.swing.JButton();
        buttonClear = new javax.swing.JButton();

        table.getTableHeader().setReorderingAllowed(false);
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

        buttonInsert.setText("Insert");
        buttonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });

        buttonClear.setText("Clear");
        buttonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout sidePanelLayout = new javax.swing.GroupLayout(sidePanel);
        sidePanel.setLayout(sidePanelLayout);
        sidePanelLayout.setHorizontalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonUp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonDown, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonSave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonInsert, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonClear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        sidePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonSave, buttonUp});

        sidePanelLayout.setVerticalGroup(
            sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonInsert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDelete)
                .addGap(18, 18, 18)
                .addComponent(buttonUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDown)
                .addGap(18, 18, 18)
                .addComponent(buttonClear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 92, Short.MAX_VALUE)
                .addComponent(buttonSave)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(sidePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(sidePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try {
            save();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        try {
            model.insertRow(table.getSelectedRow() + 1, getRow(null));
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        try {
            if (table.getSelectedRow() >= 0) {
                model.removeRow(table.getSelectedRow());
                updateButtons();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        updateButtons();
    }//GEN-LAST:event_tableMouseReleased

    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        updateButtons();
    }//GEN-LAST:event_tableKeyReleased

    private void buttonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearActionPerformed
        try {
            model.setNumRows(0);
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonClearActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonClear;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonUp;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables

    public JTable getTable() {
        return table;
    }

    //Properties
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        table.setEnabled(value);
    }

    boolean readOnly;

    @Override
    public void setReadOnly(boolean value) {
        readOnly = true;
        model.fireTableStructureChanged();
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setEditable(int column, boolean value) {
        if ((column < 0) || (column >= editable.length)) {
            throw new java.lang.IllegalArgumentException();
        }
        editable[column] = value;
        model.fireTableStructureChanged();
    }

    public boolean isEditable(int column) {
        if ((column < 0) || (column >= editable.length)) {
            throw new java.lang.IllegalArgumentException();
        }
        return editable[column];
    }

    public void setData(List<Object[]> data) {

        List<Object[]> aux = new ArrayList<Object[]>();
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                aux.add(getRow(Convert.toStringArray(data.get(i))));
            }
        }
        model.setDataVector(aux.toArray(new Object[0][0]), Convert.toObjectArray(columns));
        updateButtons();
    }

    public List<Object[]> getData() {
        List<Object[]> ret = new ArrayList<Object[]>();
        List data = model.getDataVector();
        for (int i = 0; i < data.size(); i++) {
            Object row = data.get(i);
            if (row instanceof List) {
                ret.add(((List) row).toArray(new Object[0]));
            } else if (row instanceof Object[]) {
                ret.add((Object[])row);
            } else {
                Logger.getLogger(DsvEditor.class.getName()).severe("Invalid table data vector row");
            }
        }
        return ret;
    }

    @Override
    public DsvDocument getDocument() {
        return (DsvDocument) document;
    }

    public Font getTableFont() {
        return table.getFont();
    }

    public void setTableFont(Font font) {
        if (font != null) {
            table.setFont(font);
        }
    }

    public void setReorderingAllowd(boolean value) {
        buttonUp.setVisible(value);
        buttonDown.setVisible(value);
    }

    public boolean isReorderingAllowd() {
        return buttonUp.isVisible();
    }

    public void setInsertAllowd(boolean value) {
        buttonInsert.setVisible(value);
    }

    public boolean isInsertAllowd() {
        return buttonInsert.isVisible();
    }

    public void setDeleteAllowd(boolean value) {
        buttonDelete.setVisible(value);
    }

    public boolean isDeleteAllowd() {
        return buttonDelete.isVisible();
    }

    public void setShowsidePanel(boolean value) {
        sidePanel.setVisible(value);
    }

    public boolean getShowSidePanel() {
        return sidePanel.isVisible();
    }

    //If property file substitute first separator by '='
    public boolean isPropertiesFile() {
        return (getFileName() != null) && (getFileName().endsWith(".properties"));
    }
}
