package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.SessionManager;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class SessionReopenDialog extends StandardDialog {

    final DefaultTableModel modelSessions;  
    volatile boolean updating; 
    final SessionManager manager;  
    
    public SessionReopenDialog(java.awt.Frame parent, boolean modal, String title) {
        super(parent, modal);
        setTitle(title);
        initComponents();
        manager = Context.getInstance().getSessionManager();
        modelSessions = (DefaultTableModel) tableSessions.getModel();    
        update();
        int sessions = tableSessions.getRowCount();
        if (sessions>0){
            SwingUtils.scrollToVisible(tableSessions, sessions-1, 0);
        }
                
        tableSessions.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent lse) {
                if (!lse.getValueIsAdjusting() && !updating) {         
                    int row = tableSessions.getSelectedRow();
                    selectSession((row < 0)? -1 : Integer.valueOf(tableSessions.getValueAt(row, 0).toString()));
                }
            }
        });        
    }
    
    int selectedSession = -1;
    void selectSession(int session){
        selectedSession = session; 
        buttonOk.setEnabled(selectedSession>=1);
    }
    
   public int getSelectedSession(){
       return selectedSession;
   }

    void update(){
        updating = true;
        try{
            List<Integer> ids = manager.getIDs(null, Sys.getUserName());            
            modelSessions.setNumRows(ids.size());
            for (int i=0; i<ids.size(); i++){
                try{
                    int id = ids.get(i);
                    Map<String, Object> info = manager.getInfo(id);
                    String name = (String)info.getOrDefault("name", "");
                    String start = SessionPanel.getTimeStr((Number)info.getOrDefault("start", 0));
                    String stop = SessionPanel.getTimeStr((Number)info.getOrDefault("stop", 0));
                    String state = (String)info.getOrDefault("state", "unknown");
                    if (SessionManager.isSessionEditable(state)){
                        modelSessions.setValueAt(String.valueOf(id), i, 0);
                        modelSessions.setValueAt(name, i, 1);
                        modelSessions.setValueAt(start, i, 2);
                        modelSessions.setValueAt(stop, i, 3);
                        modelSessions.setValueAt(state, i, 4);
                    }
                    //modelSessions.addRow(new Object[]{String.valueOf(id), name, start, stop, root, state});
                } catch (Exception ex){
                    Logger.getLogger(SessionsDialog.class.getName()).log(Level.WARNING, null, ex);
                }
            }
            tableSessions.clearSelection();
            buttonOk.setEnabled(false);
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

        jScrollPane1 = new javax.swing.JScrollPane();
        tableSessions = new javax.swing.JTable();
        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addGap(33, 33, 33)
                        .addComponent(buttonCancel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOk});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        if (selectedSession<0){
            cancel();
        }else {
            accept();
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        selectedSession = -1;
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SessionReopenDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SessionReopenDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SessionReopenDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SessionReopenDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                SessionReopenDialog dialog = new SessionReopenDialog(new javax.swing.JFrame(), true, "Reopen Session");
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOk;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tableSessions;
    // End of variables declaration//GEN-END:variables
}
