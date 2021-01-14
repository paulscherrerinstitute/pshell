package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.SessionManager;
import ch.psi.pshell.core.SessionManager.SessionManagerListener;
import ch.psi.utils.Chrono;
import ch.psi.utils.Str;
import ch.psi.utils.swing.MonitoredPanel;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class SessionPanel extends MonitoredPanel implements SessionManagerListener{

    final SessionManager manager;    
    final DefaultTableModel modelMetadata;
    final DefaultTableModel modelRuns;
    
    public SessionPanel() {
        initComponents();
        manager = Context.getInstance().getSessionManager();
        modelMetadata = (DefaultTableModel) tableMetadata.getModel();
        modelRuns = (DefaultTableModel) tableRuns.getModel();
        tableRuns.getColumnModel().getColumn(0).setPreferredWidth(120);
        tableRuns.getColumnModel().getColumn(0).setMaxWidth(120);
        tableRuns.getColumnModel().getColumn(0).setResizable(false);                
        tableRuns.getColumnModel().getColumn(2).setResizable(false);
           
        modelMetadata.addTableModelListener((TableModelEvent e) -> {
            if (!updating){
                if (e.getType() == TableModelEvent.UPDATE){
                    int col=e.getColumn();
                    if (e.getColumn()==1){
                        int row = e.getFirstRow();
                        String key = Str.toString(modelMetadata.getValueAt(row, 0));
                        Object value = modelMetadata.getValueAt(row, 1);
                        try {
                            manager.addMetadata(key, value);
                        } catch (IOException ex) {
                            Logger.getLogger(SessionPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    }
                }
            }
        });
        
    }    
    
    @Override
    public void onChange(SessionManager.ChangeType type) {
        SwingUtilities.invokeLater(()->{
            switch(type){
                case INFO:
                    updateInfo();
                    break;
                case METADATA:
                    updateMetadata();
                    break;
            }
        });
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
    
    
    String getTimeStr(long timestamp){
        return (timestamp>0) ? Chrono.getTimeStr(timestamp, "dd.MM HH:mm:ss") : "";
    }
    
    public void update(){
        updateInfo();
        updateMetadata();
    }
    volatile boolean updating;
    public void updateInfo(){
        updating = true;
        try{
            textId.setText(Str.toString(manager.getCurrentId()));
            textName.setText(Str.toString(manager.getCurrentName()));
            try {        
                textStart.setText(getTimeStr(manager.getStart()));
            } catch (Exception ex) {
                textStart.setText("");
            }
            try{
                List<Map<String, Object>> runs = manager.getRuns();            
                modelRuns.setNumRows(runs.size());
                int index=0;
                for(int i=0; i<runs.size(); i++ ){
                    Map<String, Object> run = runs.get(i);                
                    try {        
                        textStart.setText(getTimeStr(manager.getStart()));
                        modelRuns.setValueAt(getTimeStr((Long)run.get("start")), index, 0);
                    } catch (Exception ex) {
                        modelRuns.setValueAt(run.getOrDefault("start", ""), index, 0);
                    }
                    modelRuns.setValueAt(getTimeStr((Long)run.getOrDefault("start", 0)), index, 0);
                    modelRuns.setValueAt(run.getOrDefault("data", ""), index, 1);
                    modelRuns.setValueAt(run.getOrDefault("state", ""), index++, 2);
                }
            } catch (Exception ex){
                modelRuns.setNumRows(0);
            }
        } finally{
            updating = false;
        }
    }

    public void updateMetadata(){
        Map<String, Object> metadata;
        updating = true;
        try {            
            metadata = manager.getMetadata();
            Set<Map.Entry<Object, Object>> entries = manager.getMetadataDefinition();
            modelMetadata.setNumRows(entries.size());
            int index=0;
            for(Map.Entry entry : entries){
                modelMetadata.setValueAt(entry.getKey(), index, 0);
                modelMetadata.setValueAt(metadata.getOrDefault(entry.getKey(), ""), index++, 1);
            }
        } catch (Exception ex) {
            modelMetadata.setNumRows(0);
        } finally{
            updating=false;
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

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textId, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textName, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textStart, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3});

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {textId, textName, textStart});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        splitSessionData.setBorder(null);
        splitSessionData.setDividerLocation(200);
        splitSessionData.setDividerSize(5);

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
        tableMetadata.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
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
                "Start", "Data", "State"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
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
    // End of variables declaration//GEN-END:variables


}
