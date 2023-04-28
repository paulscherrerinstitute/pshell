package ch.psi.utils.swing;

import ch.psi.utils.Convert;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 * Editor for files containing delimiter-separated values, displayed in a table. Can also edit
 * property files, setting the property name to the first column.
 */
public class JsonTableEditor extends Editor {

    String[] columns;
    Class[] types;
    Object[] defaultValues;
    boolean[] editable;
    final DefaultTableModel model;

    /**
     * Implementation of Json table documents .
     */
    public static class JsonTableDocument extends Document {

        JsonTableEditor editor;

        JsonTableDocument() {
        }

        @Override
        public void clear() {
            editor.setData(null);
            setChanged(false);
        }

        @Override
        public void load(String fileName) throws IOException {
            List<Map> data = new ArrayList();
            try {
                String json = Files.readString(Paths.get(fileName));
                data = (List) EncoderJson.decode(json, List.class);
                editor.setData(data);
            } catch (NoSuchFileException ex) {
                clear();
            }
            setChanged(false);
        }

        @Override
        public void save(String fileName) throws IOException {
            if (IO.getExtension(fileName).isEmpty()){
                fileName += ".json";
            }
            Files.writeString(Paths.get(fileName), getContents());
            setChanged(false);
        }
        @Override
        public String getContents() {
            try{
                List<Map> data = editor.getData();
                return EncoderJson.encode(data, true);
            } catch (Exception ex){
                Logger.getLogger(JsonTableEditor.class.getName()).log(Level.SEVERE, null, ex);
                return "{}";
            }
        }

    }
    
    public JsonTableEditor() {
        this(null, null);        
    }    
   
    public JsonTableEditor(String[] columns) {
        this(columns, null);
    }

    public JsonTableEditor(String[] columns, Class[] types) {
        super(new JsonTableDocument());        
        this.columns = columns;
        this.types = types;
        if (columns!=null){
            this.editable = new boolean[columns.length];
            for (int i = 0; i < editable.length; i++) {
                editable[i] = true;
            }
        }
        ((JsonTableDocument) getDocument()).editor = this;
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
    
        
    public void setDefaultValues(Object[] defaultValues){
        this.defaultValues = defaultValues;
    }
    
     public Object[] getDefaultValues(){
        return defaultValues;
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

    Object[] getRow(Map data) {
        Object[] ret = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            try {
                ret[i] = null;
                Object value =(data == null)? null : data.get(columns[i]);
                boolean empty = (value==null);
                String defaultValue =  ((defaultValues!=null) && (defaultValues[i]!=null)) ?  Str.toString(defaultValues[i]) : null;                
                
                                
                if ((types == null) || (types[i] == String.class)) {
                    String val = empty ? ((defaultValue==null) ? "" : defaultValue) : Str.toString(value);
                    ret[i] = val;
                } else if (Number.class.isAssignableFrom(types[i])) {
                    String val = empty ? ((defaultValue==null) ? "0" : defaultValue) : Str.toString(value);
                    ret[i] = types[i].getConstructor(String.class).newInstance(val);
                } else if (types[i] == Boolean.class) {
                    String val = empty ? ((defaultValue==null) ? "false" : defaultValue) : Str.toString(value);
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
                    .addComponent(buttonClear, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    public void setData(List<Map> data) {
        List<Object[]> aux = new ArrayList<Object[]>();
        if (data != null) {
            if ((columns==null) && (data.size()>0)){
                Map row = data.get(0);
                columns = Convert.toStringArray(row.keySet().toArray());
                this.editable = new boolean[columns.length];
                for (int i = 0; i < editable.length; i++) {
                    editable[i] = true;
                }                
                if (types==null){
                    types = new Class[columns.length];
                    for (int i=0; i< columns.length; i++){
                        Object val = row.get(columns[i]);
                        types[i] = (val==null) ? String.class : val.getClass();
                    }
                }
            }
            for (Map row : data) {
                aux.add(getRow(row));
            }
        }
        model.setDataVector(aux.toArray(new Object[0][0]), Convert.toObjectArray(columns));
        updateButtons();
    }

    public List<Map> getData() {
        List<Map> ret = new ArrayList<>();
        List data = model.getDataVector();
        for (int i = 0; i < data.size(); i++) {
            Object row = data.get(i);
            Object[] values;
            Map map = new HashMap();
            if (row instanceof List) {
                row = ((List) row).toArray(new Object[0]);
            }            
            if (row instanceof Object[]) {
                values = (Object[])row;
                if (values.length == columns.length){
                    for (int j=0; j< columns.length; j++){
                        map.put(columns[j], values[j]);
                    }
                } else {
                    Logger.getLogger(DsvEditor.class.getName()).severe("Invalid table data vector row size");
                }
            } else {
                Logger.getLogger(DsvEditor.class.getName()).severe("Invalid table data vector row");
            }
            
            ret.add(map);
        }
        return ret;
    }

    @Override
    public JsonTableDocument getDocument() {
        return (JsonTableDocument) document;
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

    
    public static void main(String[] args) throws InterruptedException, IOException {
        String[] columns = new String[]{"string", "int", "float", "bool"};
        Class[] types = new Class[]{String.class, Integer.class, Float.class, Boolean.class};
        JsonTableEditor editor = new JsonTableEditor(columns, types);
        
        editor.load("/Users/gobbo_a/test/tst.json");
        //if ((args.length > 0) && (args[0] != null) & (!args[0].trim().isEmpty())) {
        //    editor.load(args[0]);
        //}
        javax.swing.JFrame frame = editor.getFrame();
        frame.setSize(600, 400);
        frame.setVisible(true);
    }    
}
