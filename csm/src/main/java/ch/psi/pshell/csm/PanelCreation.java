package ch.psi.pshell.csm;

import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.camserver.CameraClient;
import ch.psi.pshell.camserver.PipelineClient;
import ch.psi.pshell.camserver.ProxyClient;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class PanelCreation extends MonitoredPanel {

    ProxyClient proxy;
    Map<String, Object> serverCfg = null;
    List<String> instanceCfgNames = new ArrayList<>();
    boolean isPipeline;
    
    final DefaultTableModel modelConfigs;
    String currentConfig = "" ;
        
    final int SMALL_COL_SZIE = 80;
    
    
   public boolean getPipeline(){
       return isPipeline;
   }

   public void setPipeline(boolean value){
       isPipeline = value;
       butttonCreateFromConfig.setVisible(value);
   }    
    
    public PanelCreation() {
        initComponents();
               
        modelConfigs = (DefaultTableModel)  tableConfigurations.getModel();
        
        tableConfigurations.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                    try{
                        if (currentConfig!=null){
                            String config = proxy.getNamedConfig(currentConfig);
                            ScriptEditor dlg = new ScriptEditor(SwingUtils.getFrame(PanelCreation.this), true, currentConfig, config, "json");
                            dlg.setReadOnly(true);
                            dlg.setVisible(true);
                        }
                    } catch (Exception ex){
                        SwingUtils.showException(PanelCreation.this, ex);
                    }                 
                }
            }
        });
                        
        tableConfigurations.setDragEnabled(true);
        tableConfigurations.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return DnDConstants.ACTION_COPY_OR_MOVE;
            }
            @Override
            public Transferable createTransferable(JComponent comp) {
                onTableInstancesSelection();
                return (currentConfig != null) ? new StringSelection(currentConfig) : null;
            }
        });
        
        
        
        updateButtons();
    }
    
    
    void updateButtons(){
        if (!SwingUtilities.isEventDispatchThread()){
            SwingUtilities.invokeLater(()->{updateButtons();});
            return;
        }        
        butttonCreateFromName.setEnabled( (currentConfig != null) &&  (!currentConfig.isBlank()) );
        butttonCreateFromConfig.setEnabled(!textInstance.getText().isBlank());
    }
    
    
    Thread updateConfigs(){
        Thread t = new Thread(()->{
            try {
                instanceCfgNames =proxy.getConfigNames();
                Collections.sort(instanceCfgNames); //, String.CASE_INSENSITIVE_ORDER);
                modelConfigs.setRowCount(0);
                for (String instance : instanceCfgNames){
                    modelConfigs.addRow(new Object[]{instance});
                }   
                currentConfig = "";
                updateButtons();
            } catch (Exception ex) {
                Logger.getLogger(PanelCreation.class.getName()).log(Level.WARNING, null, ex);
            }             
        }, "PT Update Config");
        t.start();
        return t;
    }
    
    
           
    @Override
    protected void onActive() {      
    }           

    @Override
    protected void onShow() {   
        updateButtons();
        updateConfigs();
    }
    
        
    public void setProxy(ProxyClient proxy){
        this.proxy = proxy;        
    }
    
    public ProxyClient getProxy(){
       return proxy;
    }   
            
    public String getUrl(){
       if (proxy==null){
           return null;
       }
       return proxy.getUrl();
    }    
          
    
    void onTableInstancesSelection(){
        int row=tableConfigurations.getSelectedRow();
        if (row<0){
            currentConfig = "";
        } else {
            currentConfig = String.valueOf(tableConfigurations.getValueAt(row, 0));
        }
        textInstance.setText(currentConfig);
        updateButtons();
    }
        
    static boolean isPush(String config){
        try {
            Map instanceData = new HashMap();
            instanceData.put("config", (Map) JsonSerializer.decode(config, Map.class));
            return PanelStatus.isPush(instanceData);
        } catch (IOException ex) {
            return false;
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

        panelConfigurations = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableConfigurations = new javax.swing.JTable();
        streamPanel = new ch.psi.pshell.swing.StreamPanel();
        butttonCreateFromName = new javax.swing.JButton();
        butttonCreateFromConfig = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        textInstance = new javax.swing.JTextField();
        buttonStop = new javax.swing.JButton();

        panelConfigurations.setBorder(javax.swing.BorderFactory.createTitledBorder("Saved Configurations"));
        panelConfigurations.setPreferredSize(new java.awt.Dimension(320, 320));

        tableConfigurations.setModel(new javax.swing.table.DefaultTableModel(
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
        tableConfigurations.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableConfigurations.getTableHeader().setReorderingAllowed(false);
        tableConfigurations.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableConfigurationsMouseReleased(evt);
            }
        });
        tableConfigurations.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableConfigurationsKeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(tableConfigurations);

        javax.swing.GroupLayout panelConfigurationsLayout = new javax.swing.GroupLayout(panelConfigurations);
        panelConfigurations.setLayout(panelConfigurationsLayout);
        panelConfigurationsLayout.setHorizontalGroup(
            panelConfigurationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelConfigurationsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        panelConfigurationsLayout.setVerticalGroup(
            panelConfigurationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelConfigurationsLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        streamPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Stream"));

        butttonCreateFromName.setText("Create from Saved Config");
        butttonCreateFromName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butttonCreateFromNameActionPerformed(evt);
            }
        });

        butttonCreateFromConfig.setText("Create with Config Editor...");
        butttonCreateFromConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butttonCreateFromConfigActionPerformed(evt);
            }
        });

        jLabel1.setText("Instance name:");

        textInstance.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        textInstance.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                textInstanceMouseReleased(evt);
            }
        });
        textInstance.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textInstanceKeyReleased(evt);
            }
        });

        buttonStop.setText("Stop Instance");
        buttonStop.setEnabled(false);
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(butttonCreateFromName)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                                .addComponent(jLabel1))
                            .addComponent(butttonCreateFromConfig))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buttonStop)
                            .addComponent(textInstance, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(streamPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panelConfigurations, javax.swing.GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonStop, butttonCreateFromConfig, butttonCreateFromName, textInstance});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelConfigurations, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(butttonCreateFromName)
                    .addComponent(jLabel1)
                    .addComponent(textInstance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonStop)
                    .addComponent(butttonCreateFromConfig))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(streamPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tableConfigurationsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableConfigurationsKeyReleased
        try{
            onTableInstancesSelection();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_tableConfigurationsKeyReleased

    private void tableConfigurationsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableConfigurationsMouseReleased
        try{
            onTableInstancesSelection();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_tableConfigurationsMouseReleased

    private void butttonCreateFromNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butttonCreateFromNameActionPerformed
        try{
            streamPanel.setDevice(null);
            buttonStop.setEnabled(false);
            textInstance.setEditable(true);
            String name = textInstance.getText();                                
            if ((currentConfig!=null) && (!currentConfig.isBlank() &&name!=null && (!name.isBlank()))){
                name = name.trim();
                String stream = null;                
                if (isPipeline){
                    PipelineClient client = new PipelineClient(getUrl());  
                    List<String> ret = client.createFromName(currentConfig, name);
                    name = ret.get(0);
                    stream = ret.get(1);
                } else {
                    CameraClient client = new CameraClient(getUrl());  
                    stream = client.getStream(name);
                }
                textInstance.setText(name);
                buttonStop.setEnabled(true);
                textInstance.setEditable(false);
                
                String config = proxy.getNamedConfig(currentConfig);
                boolean pull = !isPush(config);
                streamPanel.setDevice( new Stream("stream", stream, pull));
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_butttonCreateFromNameActionPerformed

    private void butttonCreateFromConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butttonCreateFromConfigActionPerformed
        try{
            streamPanel.setDevice(null);
            buttonStop.setEnabled(false);
            textInstance.setEditable(true);
            if (isPipeline){                                           
                PipelineClient client = new PipelineClient(getUrl());  
                //String name = SwingUtils.getString(this, "Enter name of the new instance: ", "");
                String name = textInstance.getText();
                if ((name!=null && (!name.isBlank()))){
                    name = name.trim();
                    ScriptEditor dlg = new ScriptEditor(SwingUtils.getFrame(PanelCreation.this), true, name, "{}", "json");
                    dlg.setVisible(true);
                    if (dlg.getResult()){                    
                        Map config = (Map) JsonSerializer.decode(dlg.ret, Map.class);
                        List<String> ret = client.createFromConfig(config, name);
                        textInstance.setText(ret.get(0));
                        buttonStop.setEnabled(true);
                        textInstance.setEditable(false);
                        String stream = ret.get(1);
                        boolean pull = !PanelStatus.isPush(config);
                        streamPanel.setDevice( new Stream("stream", stream, pull));
                   
                    }
                }
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_butttonCreateFromConfigActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        try{
            String instance = textInstance.getText().trim();
            if (!instance.isEmpty()){
                proxy.stopInstance(instance);
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        } finally{
            streamPanel.setDevice(null);
            textInstance.setText("");
            buttonStop.setEnabled(false);     
            textInstance.setEditable(true);
            tableConfigurations.clearSelection();
        }
    }//GEN-LAST:event_buttonStopActionPerformed

    private void textInstanceMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textInstanceMouseReleased
        updateButtons();
    }//GEN-LAST:event_textInstanceMouseReleased

    private void textInstanceKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textInstanceKeyReleased
        updateButtons();
    }//GEN-LAST:event_textInstanceKeyReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonStop;
    private javax.swing.JButton butttonCreateFromConfig;
    private javax.swing.JButton butttonCreateFromName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel panelConfigurations;
    private ch.psi.pshell.swing.StreamPanel streamPanel;
    private javax.swing.JTable tableConfigurations;
    private javax.swing.JTextField textInstance;
    // End of variables declaration//GEN-END:variables
}
