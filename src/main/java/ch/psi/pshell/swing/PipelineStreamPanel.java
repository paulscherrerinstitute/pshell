package ch.psi.pshell.swing;


import ch.psi.pshell.camserver.PipelineStream;
import ch.psi.pshell.device.Device;
import ch.psi.utils.Arr;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.swing.ScriptDialog;
import ch.psi.utils.swing.SwingUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class PipelineStreamPanel extends DevicePanel {

    final DefaultTableModel modelConfig;
    final DefaultTableModel modelStats;
    Integer initCountTx;
    Integer initCountRx;
    Long  initTime;    
    volatile Map instanceInfo;
    
    public PipelineStreamPanel() {
        initComponents();
        modelConfig =(DefaultTableModel) tableConfig.getModel();  
        modelStats =(DefaultTableModel) tableStats.getModel();  
    }
    
    @Override
    public PipelineStream getDevice() {
        return (PipelineStream) super.getDevice();
    }
    
    @Override
    public void setDevice(Device device) {
        super.setDevice(device);
        streamPanel.setDevice((device!=null) ? ((PipelineStream)device).getStream(): null);
        streamPanel.setMonitoredDevice(getDevice());
              
        clearInfo();   
        if (getDevice() != null) {
            textInstance.setText(getDisplayValue(getDevice().getInstance()));
            textPipeline.setText(getDisplayValue(getDevice().getPipeline()));
            onDeviceStateChanged(getDevice().getState(), null);
            this.startTimer(1000, 10);
        } else {
            clear();
        }
        updateButtons();  
    }
   
    
    void updateButtons(){      
        if (getDevice()!=null){
            textInstance.setText(getDisplayValue(getDevice().getInstance()));
            textPipeline.setText(getDisplayValue(getDevice().getPipeline()));
            buttonConfig.setEnabled(getDevice().isInitialized());
        } else {
            textInstance.setText("");
            textPipeline.setText("");
            buttonConfig.setEnabled(false);
        }
    }
    
    void clear(){
        stopTimer();
        updateButtons();  
        //model.setNumRows(0);
    }
    
    @Override
    public void setReadOnly(boolean value) {
        super.setReadOnly(value);
        buttonConfig.setVisible(!value);
        streamPanel.setReadOnly(value);
    }
    
    static String getDisplayValue( Object obj){
        if (obj==null) {
            return "";
        }        
        if (obj instanceof Number){
            Number n= (Number)obj;        
            Double d = n.doubleValue() ;
            if (d >= 1e12) {
                return String.format("%1.2fT", d / 1e12);
            } else if (d >= 1e9) {
                return String.format("%1.2fG", d / 1e9);
            } else if (d >= 1e6) {
                return String.format("%1.2fM", d / 1e6);
            } else if (d >= 1e3) {
                return String.format("%1.2fK", d / 1e3);
            } else if ((n instanceof Float)||(n instanceof Double)){
                return String.format("%1.2f", d);
            }
        }
        return String.valueOf(obj);
    }
    
    String getInstance(){
        if ( getDevice()==null){
            return null;
        }
        return getDevice().getInstance();
    }
    
    
    void clearInfo(){
        instanceInfo = null;
        modelConfig.setNumRows(0);
        modelStats.setNumRows(0);
        initCountTx = null;
        initCountRx = null;
        initTime = null;    
    }
    
    void updateInfo(Map<String, Object> instanceInfo){
        String instance = getInstance();
        boolean instanceSelected = (instance !=null) && (instanceInfo!=null);     
        if (instanceSelected){
                                                                     
            Map<String, Object> cfg = (Map<String, Object>) instanceInfo.getOrDefault("config", new HashMap());            
           
            String keys[] = Arr.sort(cfg.keySet().toArray(new String[0]));
            modelConfig.setNumRows(0);
            for (String key:keys){
                modelConfig.addRow(new Object[]{key, Str.toString(cfg.get(key))});
            }
            

            Map<String, Object> stats = (Map<String, Object>) instanceInfo.getOrDefault("statistics", new HashMap());  
            stats.put("start_time", instanceInfo.getOrDefault("last_start_time", ""));   
            Integer rx=null, tx=null;
            try{
                rx = Integer.valueOf(((String)stats.get("rx")).split(" - ")[1]);
            } catch (Exception ex){                                        
            }
            try{
                tx = Integer.valueOf(((String)stats.get("tx")).split(" - ")[1]);
            } catch (Exception ex){                                        
            }
            if ((initTime == null) && (tx!=null) && (rx!=null)){
                initCountTx = tx;
                initCountRx = rx;
                initTime = System.currentTimeMillis();
            }
            if (!stats.containsKey("frame_shape")){
                stats.put("frame_shape", "unknown        ");
            }
            stats.put("average_rx", "undefined        ");
            stats.put("average_tx", "undefined        ");
            if (initTime!=null){
                double span = (System.currentTimeMillis()-initTime)/1000.0;
                if (span>0){
                    double fpsrx = (rx-initCountRx) / span;
                    double fpstx = (tx-initCountTx) / span;
                    stats.put("average_rx", String.format("%1.2f fps     ", fpsrx));
                    stats.put("average_tx", String.format("%1.2f fps     ", fpstx));
                }
            }
            keys = Arr.sort(stats.keySet().toArray(new String[0]));
            modelStats.setNumRows(0);
            for (String key:keys){
                modelStats.addRow(new Object[]{key, getDisplayValue(stats.get(key))});
            }
        
           
        } else {
            modelConfig.setNumRows(0);
            modelStats.setNumRows(0);
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

        buttonConfig = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        textInstance = new javax.swing.JTextField();
        textPipeline = new javax.swing.JTextField();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        streamPanel = new ch.psi.pshell.swing.StreamPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableStats = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tableConfig = new javax.swing.JTable();

        buttonConfig.setText("Config");
        buttonConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonConfigActionPerformed(evt);
            }
        });

        jLabel1.setText("Instance Name: ");

        jLabel2.setText("Pipeline Name:");

        textInstance.setEditable(false);

        textPipeline.setEditable(false);

        jTabbedPane1.addTab("Stream", streamPanel);

        tableStats.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
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
        jScrollPane3.setViewportView(tableStats);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Statistics", jPanel1);

        tableConfig.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Name", "Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
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
        jScrollPane4.setViewportView(tableConfig);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Configuration", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textInstance)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(textPipeline)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonConfig)
                .addContainerGap())
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 593, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(textInstance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textPipeline, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonConfig))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonConfigActionPerformed
        try{
            String instance = getInstance();
            if ((instance!=null) && (instanceInfo!=null)){
                Map cfg = (Map) instanceInfo.getOrDefault("config", new HashMap());
                String json = EncoderJson.encode(cfg, true);
                ScriptDialog dlg = new ScriptDialog(getWindow(), true, instance, json, "json");
                dlg.setVisible(true);
                if (dlg.getResult()){
                    json = dlg.getText();
                    cfg = (Map) EncoderJson.decode(json, Map.class);
                    getDevice().setInstanceConfig(cfg);
                }
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonConfigActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonConfig;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private ch.psi.pshell.swing.StreamPanel streamPanel;
    private javax.swing.JTable tableConfig;
    private javax.swing.JTable tableStats;
    private javax.swing.JTextField textInstance;
    private javax.swing.JTextField textPipeline;
    // End of variables declaration//GEN-END:variables


    @Override
    protected void onDeviceStateChanged(State state, State former) {
        updateButtons();
    }

    volatile Thread updateInfoThread=null;
    @Override
    protected void onTimer() {
        try{
            if ((updateInfoThread==null) || (!updateInfoThread.isAlive())){
                String instance=getInstance();
                if (instance!=null){
                  updateInfoThread = new Thread(()->{
                      
                      try {
                          instanceInfo = getDevice().getProxy().getInstance(instance);
                          try {
                              SwingUtilities.invokeLater(()->{
                                  updateInfo(instanceInfo);
                              });
                          } catch (Exception ex) {
                              Logger.getLogger(PipelineStreamPanel.class.getName()).log(Level.WARNING, null, ex);
                          }
                      } catch (IOException ex) {
                          Logger.getLogger(PipelineStreamPanel.class.getName()).log(Level.WARNING, null, ex);
                          clearInfo();
                      }
                      
                  });
                  updateInfoThread.setDaemon(showTitle);
                  updateInfoThread.run();
              }
            }
        } catch (Exception ex){
            getLogger().log(Level.WARNING, null, ex);
        } 
    }    
}
