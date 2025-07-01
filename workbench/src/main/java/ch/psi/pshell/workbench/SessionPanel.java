package ch.psi.pshell.workbench;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.session.SessionManager;
import ch.psi.pshell.session.SessionManager.MetadataType;
import ch.psi.pshell.session.SessionManager.SessionManagerListener;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SessionsDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Str;
import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 *
 */
public class SessionPanel extends MonitoredPanel implements SessionManagerListener {

    final SessionManager manager;
    final DefaultTableModel modelMetadata;
    final DefaultTableModel modelRuns;

    boolean scrollRunsTable = true;

    public SessionPanel() {
        initComponents();        
        int minColSize =  (UIManager.getLookAndFeel().getName().equalsIgnoreCase("nimbus"))? 68:60;
        manager = Context.isHandlingSessions() ? Context.getSessionManager() : null;
        modelMetadata = (DefaultTableModel) tableMetadata.getModel();
        modelRuns = (DefaultTableModel) tableRuns.getModel();
        tableRuns.getColumnModel().getColumn(0).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(0).setMaxWidth(minColSize);
        tableRuns.getColumnModel().getColumn(0).setResizable(false);
        tableRuns.getColumnModel().getColumn(1).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(1).setMaxWidth(minColSize);
        tableRuns.getColumnModel().getColumn(1).setResizable(false);
        tableRuns.getColumnModel().getColumn(2).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(2).setMaxWidth(minColSize);
        tableRuns.getColumnModel().getColumn(2).setResizable(false);
        tableRuns.getColumnModel().getColumn(3).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(3).setMaxWidth(minColSize);
        tableRuns.getColumnModel().getColumn(3).setResizable(false);
        tableRuns.getColumnModel().getColumn(5).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(5).setResizable(false);

        modelMetadata.addTableModelListener((TableModelEvent e) -> {
            if (!updating) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    if (e.getColumn() == 1) {
                        int row = e.getFirstRow();
                        String key = Str.toString(modelMetadata.getValueAt(row, 0));
                        Object value = modelMetadata.getValueAt(row, 1);
                        addMetadata(key, value);
                    }
                }
            }
        });

        modelRuns.addTableModelListener((TableModelEvent e) -> {
            if (!updating) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    if (e.getColumn() == 0) {
                        int runIndex = e.getFirstRow();
                        try {
                            Boolean value = (Boolean) modelRuns.getValueAt(runIndex, 0);
                            scrollRunsTable = false;
                            manager.setRunEnabled(runIndex, value);
                        } catch (IOException ex) {
                            Logger.getLogger(SessionPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    }
                }
            }
        });
        tableMetadata.getColumnModel().getColumn(1).setCellRenderer(new MetadataValueRenderer());
        
        /*
        tableMetadata.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                        int row = tableMetadata.getSelectedRow();
                        int col = tableMetadata.getSelectedColumn();
                        if ((col == 0) && (row >= 0)) {
                            String key = String.valueOf(tableMetadata.getModel().getValueAt(row, 0));
                            String value = String.valueOf(tableMetadata.getModel().getValueAt(row, 1));
                            MetadataType type = manager.getMetadataType(key);
                            if (type == MetadataType.Map) {
                                TextEditor editor = new TextEditor();
                                editor.setText(value);
                                editor.setTitle(key);
                                Editor.EditorDialog dlg = editor.getDialog(getFrame(), true);
                                dlg.setSize(480, 240);
                                dlg.setLocationRelativeTo(tableMetadata);                                
                                dlg.setVisible(true);                                
                                editor.getText();
                                addMetadata(key, editor.getText());
                            }
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });
        */
    }

    void addMetadata(String key, Object value) {
        try {
            MetadataType type = manager.getMetadataType(key);
            if (value instanceof String str){
                if (type==MetadataType.List){
                    if (!str.startsWith("[")){
                        value = "[" + value;
                    }
                    if (!str.endsWith("]")){
                        value = value + "]";
                    }
                }
                else if (type==MetadataType.Map){
                    if (!str.startsWith("{")){
                        value = "{" + value;
                    }
                    if (!str.endsWith("}")){
                        value = value + "}";
                    }
                }
            }  
            manager.fromString(type, Str.toString(value));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                showException(ex);
            });
            //If cannot parse value according to type, keeps the change
            //and displays value with error background                
        }
        try {
            manager.setMetadata(key, value);
        } catch (IOException ex) {
            Logger.getLogger(SessionPanel.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    class MetadataValueRenderer extends DefaultTableCellRenderer {

        Color backgroundColor = getBackground();
        Color foregroundColor = getForeground();
        final Color errorColor = Color.red.darker();
        final Color errorSelectedColor = Color.red.darker().darker();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);            
            try {
                String key = Str.toString(modelMetadata.getValueAt(row, 0));
                manager.fromString(manager.getMetadataType(key), Str.toString(value));
                c.setBackground(backgroundColor);
            } catch (Exception ex) {
                if (!isSelected) {
                    c.setBackground(errorColor);
                } else {
                    c.setBackground(errorSelectedColor);
                }
            }
            c.setForeground(foregroundColor);
            return c;
        }
    }

    @Override
    public void onChange(int id, SessionManager.ChangeType type) {
        if (id==manager.getCurrentSession()){
            SwingUtilities.invokeLater(() -> {
                switch (type) {
                    case INFO -> {
                        updateInfo();
                        if (scrollRunsTable) {
                            if (modelRuns.getRowCount() > 0) {
                                SwingUtils.scrollToVisible(tableRuns, modelRuns.getRowCount() - 1, 0);
                            }
                        } else {
                            scrollRunsTable = true;
                        }
                    }
                    case METADATA -> updateMetadata();
                }
            });
        }
    }

    @Override
    protected void onShow() {
        update();
        manager.addListener(this);
    }

    @Override
    protected void onHide() {
        manager.removeListener(this);
    }


    public void update() {
        updateInfo();
        updateMetadata();
    }
    volatile boolean updating;

    public void updateInfo() {
        updating = true;
        try {
            textId.setText(Str.toString(manager.getCurrentSession()));
            try {                
                textName.setText(Str.toString(manager.getName()));
            } catch (Exception ex) {
                textName.setText("");
            }
            try {
                textStart.setText(SessionsDialog.getDateTimeStr(manager.getStart()));
            } catch (Exception ex) {
                textStart.setText("");
            }
            try {
                textState.setText(manager.getState());
            } catch (Exception ex) {
                textState.setText("");
            }

            try {
                List<Map<String, Object>> runs = manager.getRuns(true);
                modelRuns.setNumRows(runs.size());
                int index = 0;
                for (int i = 0; i < runs.size(); i++) {
                    Map<String, Object> run = runs.get(i);
                    modelRuns.setValueAt(run.getOrDefault("enabled", true), index, 0);
                    modelRuns.setValueAt(SessionsDialog.getDateStr((Number) run.getOrDefault("start", 0)), index, 1);
                    modelRuns.setValueAt(SessionsDialog.getTimeStr((Number) run.getOrDefault("start", 0)), index, 2);
                    modelRuns.setValueAt(SessionsDialog.getTimeStr((Number) run.getOrDefault("stop", 0)), index, 3);
                    modelRuns.setValueAt(Str.toString(run.getOrDefault("data", "")), index, 4);
                    modelRuns.setValueAt(run.getOrDefault("state", ""), index++, 5);
                }
            } catch (Exception ex) {
                modelRuns.setNumRows(0);
            }
        } finally {
            updating = false;
        }
    }

    public void updateMetadata() {
        updating = true;
        try {
            List<ImmutablePair<String,Object>>  metadata= manager.getDisplayableMetadata();
            modelMetadata.setNumRows(metadata.size());
            int index = 0;
            for (ImmutablePair<String,Object> entry : metadata) {
                modelMetadata.setValueAt(entry.getLeft(), index, 0);
                modelMetadata.setValueAt(entry.getRight(), index++, 1);
            }
        } catch (Exception ex) {
            modelMetadata.setNumRows(0);
        } finally {
            updating = false;
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

        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        textId = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textStart = new javax.swing.JTextField();
        textName = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        textState = new javax.swing.JTextField();
        splitSessionData = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableMetadata = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableRuns = new javax.swing.JTable();

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Info"));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Id:");

        textId.setEditable(false);
        textId.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Name:");

        textStart.setEditable(false);
        textStart.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        textName.setEditable(false);
        textName.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Start:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("State:");

        textState.setEditable(false);
        textState.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textId, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textName, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textStart, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textState, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4});

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {textId, textName, textStart});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textState, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        splitSessionData.setDividerLocation(200);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Metadata"));

        tableMetadata.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableMetadata.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        tableMetadata.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tableMetadata);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                .addGap(7, 7, 7))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        splitSessionData.setLeftComponent(jPanel2);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Runs"));

        tableRuns.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Date", "Start", "Stop", "Data", "State"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableRuns.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tableRuns);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE)
                .addGap(7, 7, 7))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        splitSessionData.setRightComponent(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(splitSessionData)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(splitSessionData)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane splitSessionData;
    private javax.swing.JTable tableMetadata;
    private javax.swing.JTable tableRuns;
    private javax.swing.JTextField textId;
    private javax.swing.JTextField textName;
    private javax.swing.JTextField textStart;
    private javax.swing.JTextField textState;
    // End of variables declaration//GEN-END:variables

}
