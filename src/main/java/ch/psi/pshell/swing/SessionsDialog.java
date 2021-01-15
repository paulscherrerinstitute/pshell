package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.SessionManager;
import ch.psi.utils.Str;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;


public class SessionsDialog extends StandardDialog {

    final SessionManager manager;    
    final DefaultTableModel modelSessions;
    final DefaultTableModel modelMetadata;
    final DefaultTableModel modelRuns;
    final DefaultTableModel modelFiles;
    

    public SessionsDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        manager = Context.getInstance().getSessionManager();
        modelSessions = (DefaultTableModel) tableSessions.getModel();
        modelMetadata = (DefaultTableModel) tableMetadata.getModel();
        modelRuns = (DefaultTableModel) tableRuns.getModel();
        modelFiles = (DefaultTableModel) tableFiles.getModel();
        
        tableRuns.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(0).setMaxWidth(60);
        tableRuns.getColumnModel().getColumn(1).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(1).setMaxWidth(60);
        tableRuns.getColumnModel().getColumn(2).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(2).setMaxWidth(60);
        tableRuns.getColumnModel().getColumn(3).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(3).setMaxWidth(60);
        tableRuns.getColumnModel().getColumn(0).setResizable(false);                
        tableRuns.getColumnModel().getColumn(1).setResizable(false);                
        tableRuns.getColumnModel().getColumn(2).setResizable(false);                
        tableRuns.getColumnModel().getColumn(3).setResizable(false);                
        
        tableRuns.getColumnModel().getColumn(5).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(5).setResizable(false);
        
        
        update();        
        tableSessions.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent lse) {
                if (!lse.getValueIsAdjusting()) {         
                    int row = tableSessions.getSelectedRow();
                    if (row >=0){
                        selectSession(Integer.valueOf(tableSessions.getValueAt(row, 0).toString()));
                    }
                }
            }
        });
    }
        
    void updateButtons(){
        int sessionRow = tableSessions.getSelectedRow();
        String sessionState = (sessionRow>=0) ? Str.toString(modelSessions.getValueAt(sessionRow, 4)) : "";

        buttonAddFile.setEnabled(currentSession>=0);
        buttonRemoveFile.setEnabled(buttonAddFile.isEnabled() && tableFiles.getSelectedRow()>=0);
        
        buttonScicatInjestion.setEnabled(sessionState.equals("completed"));
    }
    
    void update(){
        List<Integer> ids = manager.getIDs();
        
        modelSessions.setNumRows(0);
        for (int i=0; i<ids.size(); i++){
            try{
                int id = ids.get(i);
                Map<String, Object> info = manager.getInfo(id);
                String name = (String)info.getOrDefault("name", "");
                String start = SessionPanel.getTimeStr((Number)info.getOrDefault("start", 0));
                String stop = SessionPanel.getTimeStr((Number)info.getOrDefault("stop", 0));
                String state = (String)info.getOrDefault("state", "unknown");
                modelSessions.addRow(new Object[]{String.valueOf(id), name, start, stop, state});
            } catch (Exception ex){
                Logger.getLogger(SessionsDialog.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        updateButtons();
    }
    

    int currentSession = -1;
    void selectSession(int session){
       currentSession = session; 
        Map<String, Object> metadata;
        try {            
            metadata = manager.getMetadata(session);
            Set<Map.Entry<Object, Object>> entries = manager.getMetadataDefinition();
            modelMetadata.setNumRows(entries.size());
            int index=0;
            for(Map.Entry entry : entries){
                modelMetadata.setValueAt(entry.getKey(), index, 0);
                modelMetadata.setValueAt(metadata.getOrDefault(entry.getKey(), ""), index++, 1);
            }
        } catch (Exception ex) {
            modelMetadata.setNumRows(0);
        }
        
        try{
            List<Map<String, Object>> runs = manager.getRuns(session);            
            modelRuns.setNumRows(runs.size());
            String dataHome = Context.getInstance().getSetup().getDataPath();
            int index=0;
            for(int i=0; i<runs.size(); i++ ){
                Map<String, Object> run = runs.get(i);                
                modelRuns.setValueAt(true, index, 0);
                modelRuns.setValueAt(SessionPanel.getDateStr((Number)run.getOrDefault("start", 0)), index, 1);
                modelRuns.setValueAt(SessionPanel.getTimeStr((Number)run.getOrDefault("start", 0)), index, 2);
                modelRuns.setValueAt(SessionPanel.getTimeStr((Number)run.getOrDefault("stop", 0)), index, 3);
                modelRuns.setValueAt(SessionPanel.getDisplayFileName(Str.toString(run.getOrDefault("data", "")), dataHome), index, 4);
                modelRuns.setValueAt(run.getOrDefault("state", ""), index++, 5);
            }
        } catch (Exception ex){
            modelRuns.setNumRows(0);
        }        
        
        try{
            List<String> files = manager.getAdditionalFiles(session);            
            modelFiles.setNumRows(files.size());
            for (int i=0; i<files.size(); i++){
                modelFiles.setValueAt(files.get(i), i,0);
            }       
        } catch (Exception ex){
            modelFiles.setNumRows(0);
        }  
        updateButtons();
    }
    
    void setAdditionalFiles() throws IOException{
        if (currentSession>=0){
            List<String> files = new ArrayList<>();         
            for (int i=0; i<modelFiles.getRowCount(); i++){
                files.add(modelFiles.getValueAt(i,0).toString());
            }       
            manager.setAdditionalFiles(currentSession, files);          
        }
        updateButtons();
    }   

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSessions = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableRuns = new javax.swing.JTable();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableMetadata = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tableFiles = new javax.swing.JTable();
        buttonAddFile = new javax.swing.JButton();
        buttonRemoveFile = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        buttonScicatInjestion = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Sessions"));

        tableSessions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Id", "Name", "Start", "Finish", "State"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableSessions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableSessions.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tableSessions);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Runs"));

        tableRuns.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Selected", "Date", "Start", "Stop", "Data", "State"
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
        jScrollPane3.setViewportView(tableRuns);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addGap(7, 7, 7))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(300);
        jSplitPane1.setDividerSize(5);
        jSplitPane1.setResizeWeight(0.5);

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
                false, false
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
        jScrollPane2.setViewportView(tableMetadata);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(jPanel2);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Additional Files"));

        tableFiles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableFiles.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        tableFiles.getTableHeader().setReorderingAllowed(false);
        jScrollPane4.setViewportView(tableFiles);

        buttonAddFile.setText("Add");
        buttonAddFile.setEnabled(false);
        buttonAddFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddFileActionPerformed(evt);
            }
        });

        buttonRemoveFile.setText("Remove");
        buttonRemoveFile.setEnabled(false);
        buttonRemoveFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveFileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addGap(0, 40, Short.MAX_VALUE)
                        .addComponent(buttonAddFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonRemoveFile)
                        .addGap(0, 40, Short.MAX_VALUE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonAddFile, buttonRemoveFile});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonRemoveFile)
                    .addComponent(buttonAddFile))
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel4);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Archive"));

        buttonScicatInjestion.setText("SciCat Injestion");
        buttonScicatInjestion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScicatInjestionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonScicatInjestion)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonScicatInjestion)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jSplitPane1)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jSplitPane1)
                .addGap(0, 0, 0)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonAddFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddFileActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(Context.getInstance().getSetup().getDataPath());
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setAcceptAllFileFilterUsed(true);
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                ((DefaultTableModel)tableFiles.getModel()).addRow(new Object[]{chooser.getSelectedFile().getCanonicalPath()});
                setAdditionalFiles();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }          
    }//GEN-LAST:event_buttonAddFileActionPerformed

    private void buttonRemoveFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveFileActionPerformed
        try {
            if(tableFiles.getSelectedRow()>=0){
                tableFiles.removeRowSelectionInterval(tableFiles.getSelectedRow(), tableFiles.getSelectedRow());
                setAdditionalFiles();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }            
    }//GEN-LAST:event_buttonRemoveFileActionPerformed

    private void buttonScicatInjestionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScicatInjestionActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_buttonScicatInjestionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddFile;
    private javax.swing.JButton buttonRemoveFile;
    private javax.swing.JButton buttonScicatInjestion;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable tableFiles;
    private javax.swing.JTable tableMetadata;
    private javax.swing.JTable tableRuns;
    private javax.swing.JTable tableSessions;
    // End of variables declaration//GEN-END:variables
}
