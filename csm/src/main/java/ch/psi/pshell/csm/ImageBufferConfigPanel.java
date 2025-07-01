package ch.psi.pshell.csm;


import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.StreamValue;
import ch.psi.pshell.csm.DataBuffer.CameraInfo;
import ch.psi.pshell.swing.CommitDialog;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Str;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class ImageBufferConfigPanel extends MonitoredPanel {

    final DefaultTableModel modelGroups;
    final DefaultTableModel modelCameras;
    List<CameraInfo> cameras = new ArrayList<CameraInfo>();
    List<CameraInfo> groupCameras= new ArrayList<CameraInfo>();
    boolean updating;
    Set<String> groups;

    public ImageBufferConfigPanel() {
        initComponents();
        modelGroups = (DefaultTableModel) tableGroups.getModel();
        modelCameras = (DefaultTableModel) tableCameras.getModel();
        tableGroups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableCameras.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        buttonApply.setEnabled(false);
        buttonCheckStreams.setEnabled(false);
        
        tableCameras.getColumnModel().getColumn(0).setMaxWidth(70);
        tableCameras.getColumnModel().getColumn(3).setMinWidth(0);
        tableCameras.getColumnModel().getColumn(3).setMaxWidth(0);
        
        
        tableGroups.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                try{
                    if (!event.getValueIsAdjusting()) {
                        updating = true;
                        modelCameras.setNumRows(0);
                        tableCameras.getColumnModel().getColumn(3).setMinWidth(0);
                        tableCameras.getColumnModel().getColumn(3).setMaxWidth(0);                        
                        groupCameras = new ArrayList<CameraInfo>();
                        buttonApply.setEnabled(false);
                        updateButtons();
                        if (tableGroups.getSelectedRow() >= 0) {
                            String group = currentGroup();
                            for (CameraInfo camera:cameras){
                                if (group.equals(camera.group)){
                                    groupCameras.add(camera);
                                    modelCameras.addRow(new Object[]{camera.enabled, camera.name, camera.url, ""});
                                }
                            }                                                        
                        }
                    }
                } catch (Exception ex){
                    SwingUtils.showException(ImageBufferConfigPanel.this, ex);
                } finally{
                    updateButtons();
                    updating = false;                   
                }
            }
        });
        
        modelCameras.addTableModelListener((TableModelEvent e) -> {
            boolean changed=false; 
            if (!updating){
                for (int i=0; i< groupCameras.size(); i++){
                    if (!modelCameras.getValueAt(i, 0).equals(groupCameras.get(i).enabled)){
                        changed=true;
                        break;
                    }
                }
            }
            buttonApply.setEnabled(changed);
        });
    }
    
    void updateFile(String selectedGroup){
        modelGroups.setRowCount(0);
        modelCameras.setRowCount(0);
        tableGroups.setEnabled(false);
        new Thread(()->{
            try{                    
                cameras =  DataBuffer.getImageBufferConfig();          
                groups = new HashSet<>();
                for (CameraInfo camera:cameras){
                    if ((camera.group!=null) && (!camera.group.isEmpty())){
                        groups.add(camera.group);
                    }
                }
                groups = new TreeSet<>(groups);
                for (String group: groups){
                    modelGroups.addRow(new Object[]{group});
                }
                tableGroups.setEnabled(true);   //Ordering
                
                
                if (selectedGroup!=null){                
                    for (int i=0;i<modelGroups.getRowCount();i++){
                        if (selectedGroup.equals(modelGroups.getValueAt(i, 0))){
                            tableGroups.setRowSelectionInterval(i, i);
                            break;
                        }
                    }
                }
                
            } catch (Exception ex){
                Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
                showException(ex);                    
            } finally{
                updateButtons();
            }                
        },"IB Get Config").start();                                            
    }
    
    
    String currentGroup() {
        try{
            return (String) modelGroups.getValueAt(tableGroups.getSelectedRow(), 0);
        } catch (Exception ex){
            return null;
        }
    }
    
    void updateButtons(){
        if (!SwingUtilities.isEventDispatchThread()){
            SwingUtilities.invokeLater(()->{updateButtons();});
            return;
        }        
        
        buttonCheckStreams.setEnabled((modelCameras.getRowCount()>0));
    }
    
    @Override
    protected void onShow(){
        updateFile(null);
    }
    
    
    void apply() throws Exception{
        if (!updating){            
            CommitDialog dlg = new CommitDialog(ImageBufferConfigPanel.this.getFrame(), true);
            dlg.setVisible(true);
            if (dlg.getResult()){                
                buttonApply.setEnabled(false);
                String group = currentGroup();
                List<CameraInfo> updatedCameras = new ArrayList<CameraInfo>();            
                for (int i=0; i< groupCameras.size(); i++){
                    if (!modelCameras.getValueAt(i, 0).equals(groupCameras.get(i).enabled)){
                        updatedCameras.add(groupCameras.get(i).copyToggleEnable());
                    }
                }
                tableGroups.clearSelection();    
                updateButtons();                
                JDialog dialogMessage = showSplash("Image Buffer", new Dimension(500,200), "Updating image buffer configuration");            
                new Thread(()->{
                    try{                    
                        String ret = DataBuffer.updateImageBufferConfig(updatedCameras, dlg.getUser(), dlg.getPassword(), dlg.getMesssage());
                        updateFile(group);
                        showScrollableMessage( "Success",  "Success updating image buffer configuration", ret);
                    } catch (Exception ex){
                        Logger.getLogger(DataBufferPanel.class.getName()).log(Level.WARNING, null, ex);     
                        showException(ex);                    
                    } finally{
                        dialogMessage.setVisible(false);
                        updateButtons();
                    }                
                },"Update IB config").start();                                    
            }        
        }
    }
    
    void checkStreams() throws Exception{
        String group = currentGroup();
        for (int i=0; i< groupCameras.size(); i++){
            modelCameras.setValueAt("", i, 3);
            String url = (String) modelCameras.getValueAt(i, 2);
            String camera = (String) modelCameras.getValueAt(i, 1);
            int index = i;        
            if (tableCameras.getColumnModel().getColumn(3).getMaxWidth()==0){
                tableCameras.getColumnModel().getColumn(3).setMinWidth(15);
                tableCameras.getColumnModel().getColumn(3).setMaxWidth(tableCameras.getColumnModel().getColumn(2).getMaxWidth());
                tableCameras.getColumnModel().getColumn(3).setPreferredWidth(70);
            }
            Thread thread = new Thread(() -> {
                String ret = "no";
                String address = url.replace("daqsf", "sf");
                try (Stream st = new Stream("stream", address, true )) {
                    st.start();
                    StreamValue sv = st.read(2000);
                    if (sv!=null){
                        try{ 
                            String channel = sv.getKeys().get(0);
                            String suffix = ":FPICTURE";
                            if (channel.endsWith(suffix)) {
                                String cam = channel.substring(0, channel.length() - suffix.length());
                                if (!cam.equals(camera)){
                                    ret = cam;
                                } else {
                                    ret = "yes";
                                    Object val = sv.getValue(channel);
                                    int[] shape = sv.getShape(channel);
                                    String type =  Str.toString(sv.getType(channel));
                                    if ((shape==null) && (val!=null)){
                                        shape = Arr.getShape(val);
                                    }        
                                    String size = Convert.arrayToString(shape, " x ");
                                    String desc = type + " [" + Convert.arrayToString(shape, "x") + "] ";
                                    ret = desc;
                                }
                                
                            } else {
                                ret = "invalid";
                            }                                                        
                        } catch (Exception ex){
                            ret = ex.getMessage();
                        }
                        
                    }
                } catch (Exception ex) {
                } finally {
                }         
                String streaming = ret;
                SwingUtilities.invokeLater(()->{
                    if (group.equals(currentGroup())){
                        modelCameras.setValueAt(streaming, index, 3);
                    }
                });
            });
            thread.setDaemon(true);
            thread.start();            
        }        
    }    
    
        
    public static void main(String[] args) {        
        View.setLookAndFeel(ch.psi.pshell.app.MainFrame.LookAndFeelType.dark);
        ImageBufferConfigPanel pn = new ImageBufferConfigPanel();
        JFrame frame = SwingUtils.showFrame(null, "Image Buffer Sources", null, pn);
        SwingUtils.centerComponent(null, frame);
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
        tableGroups = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableCameras = new javax.swing.JTable();
        buttonApply = new javax.swing.JButton();
        buttonCheckStreams = new javax.swing.JButton();

        tableGroups.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Groups"
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
        tableGroups.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableGroups.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableGroupsMouseReleased(evt);
            }
        });
        tableGroups.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableGroupsKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(tableGroups);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Cameras"));

        tableCameras.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Name", "URL", "Streaming"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableCameras.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tableCameras);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 656, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                .addContainerGap())
        );

        buttonApply.setText("Commit Changes");
        buttonApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonApplyActionPerformed(evt);
            }
        });

        buttonCheckStreams.setText("Check Streams");
        buttonCheckStreams.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCheckStreamsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonCheckStreams)
                .addGap(18, 18, 18)
                .addComponent(buttonApply)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonApply, buttonCheckStreams});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonApply)
                    .addComponent(buttonCheckStreams))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tableGroupsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableGroupsKeyReleased
        updateButtons();
    }//GEN-LAST:event_tableGroupsKeyReleased

    private void tableGroupsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableGroupsMouseReleased
        updateButtons();
    }//GEN-LAST:event_tableGroupsMouseReleased

    private void buttonApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonApplyActionPerformed
        try{
            apply();            
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonApplyActionPerformed

    private void buttonCheckStreamsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCheckStreamsActionPerformed
        try{
            checkStreams();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonCheckStreamsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonApply;
    private javax.swing.JButton buttonCheckStreams;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tableCameras;
    private javax.swing.JTable tableGroups;
    // End of variables declaration//GEN-END:variables
}
