package ch.psi.pshell.csm;


import ch.psi.pshell.camserver.CamServerClient;
import ch.psi.pshell.camserver.CameraClient;
import ch.psi.pshell.camserver.PipelineClient;
import ch.psi.pshell.camserver.ProxyClient;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.NamedThreadFactory;
import ch.psi.pshell.utils.Str;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class PanelStatus extends MonitoredPanel {
    ProxyClient proxy;
    ScheduledExecutorService schedulerPolling;       
    final DefaultTableModel model;
    final DefaultTableModel modelInstances;    
    String currentServer;
    String currentInstance;
    InfoDialog infoDialog;
    boolean isPipeline;
    
    
    public PanelStatus() {
        initComponents();
        model = (DefaultTableModel) table.getModel();
        modelInstances = (DefaultTableModel) tableInstances.getModel();        
        
        
        tableInstances.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                        if (currentInstance != null) {
                            if ((infoDialog==null) || !(infoDialog.isShowing())){
                                infoDialog = new InfoDialog(getFrame(), false);
                                infoDialog.setVisible(true);
                            }                            
                            infoDialog.setInstance(currentInstance);
                            infoDialog.update(instanceInfo);
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });
        
        for (int i=1; i<model.getColumnCount();i++){
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment( JLabel.CENTER );
            table.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );
        }
        
        for (int i=1; i<modelInstances.getColumnCount();i++){
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment( JLabel.CENTER );
            tableInstances.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );
        }       
        table.getColumnModel().getColumn(0).setPreferredWidth(230);
        tableInstances.getColumnModel().getColumn(0).setPreferredWidth(230);
    }
    
    public void setProxy(ProxyClient proxy){
        this.proxy = proxy;
        textProxy.setText(getUrl());
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
    
   public boolean getPipeline(){
       return isPipeline;
   }

   public void setPipeline(boolean value){
       isPipeline = value;
       buttonFunction.setText(isPipeline ? "Script" : "Delete");
   }

   @Override
    protected void onShow() {
        schedulerPolling = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("PanelServers update thread"));
        schedulerPolling.scheduleWithFixedDelay( () -> {
            if (isShowing()){
                update();                           
            }  else {
                Logger.getLogger(getClass().getName()).info("");
            }     
        } , 10, 2000, TimeUnit.MILLISECONDS);
        updateControls();        
    }

    @Override
    protected void onHide() {
        schedulerPolling.shutdownNow();
    }
    
    void update(){
        try {
            Map<String, Object> info = proxy.getInfo();
            Map<String, Map<String, Object>> servers = (Map<String, Map<String, Object>>) info.get("servers");
            Map<String, Map<String, Object>> active_instances = (Map<String, Map<String, Object>>) info.get("active_instances");
            
            SwingUtilities.invokeLater(()->{
                update(servers, active_instances);
            });                 
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, null, ex);
        }
    }    
   
    final LinkedHashMap<String,Map<String, Object>> serverInfo = new LinkedHashMap();
    final Map<String,Map<String, Object>> instanceInfo = new HashMap<>();
    
    void update(Map<String, Map<String, Object>> servers, Map<String, Map<String, Object>> active_instances){
        try {            
            ArrayList<String> keys = new ArrayList<>(servers.keySet());
            Collections.sort(keys);
            serverInfo.clear();
            instanceInfo.clear();
            instanceInfo.putAll(active_instances);
            
            if (model.getRowCount() != keys.size()){
                model.setRowCount(keys.size());
            }
                
            for (int i=0; i<keys.size(); i++){
                String url = keys.get(i);
                Map<String, Object> info = servers.get(url);
                String version = getDisplayValue(info.get("version"));
                String load = getDisplayValue(info.get("load"));
                String cpu = getDisplayValue(info.get("cpu"));
                String memory = getDisplayValue(info.get("memory"));
                String tx = getDisplayValue(info.get("tx")); 
                String rx = getDisplayValue(info.get("rx")); 
                //String instances =  String.valueOf(info.get("instances"));       
                int col=0;
                model.setValueAt(url, i, col++);
                model.setValueAt(version, i, col++);
                model.setValueAt(load, i, col++);
                model.setValueAt(rx, i, col++);                
                model.setValueAt(tx, i, col++);
                model.setValueAt(cpu, i, col++);
                model.setValueAt(memory, i, col++);
                serverInfo.put(url, info);
            }   
            updateControls();
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
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
    
    void updateControls(){
        int serverIndex = table.getSelectedRow();
        boolean serverSelected = serverIndex>=0;        
        currentServer = serverSelected ? model.getValueAt(serverIndex, 0).toString() : null;         
        try{            
            
            buttonServerLogs.setEnabled(serverSelected);
            buttonServerRestart.setEnabled(serverSelected);
            buttonServerStop.setEnabled(serverSelected);       
            List<String> instances = new ArrayList<>();
            if (serverSelected){
                try{                    
                    instances = (List) serverInfo.get(currentServer).get("instances");
                } catch (Exception ex){                
                }
            }

            if (modelInstances.getRowCount() != instances.size()){
                modelInstances.setRowCount(instances.size());
            }           
            for (int i =0; i< instances.size(); i++){
                String instanceName = instances.get(i);
                int col=0;
                modelInstances.setValueAt(instanceName, i, col++);  
                Map data = instanceInfo.getOrDefault(instanceName, new HashMap());
                Map stats = (Map) data.getOrDefault("statistics", new HashMap());                     
                //modelInstances.setValueAt(data.getOrDefault("stream_address", ""), i, col++);
                modelInstances.setValueAt(getDisplayValue(stats.getOrDefault("time", "")), i, col++);
                modelInstances.setValueAt(getDisplayValue(stats.getOrDefault("clients", "")), i, col++);
                //modelInstances.setValueAt(getDisplayValue(stats.getOrDefault("throughput", "")), i, col++);
                modelInstances.setValueAt(getDisplayValue(Str.toString(stats.getOrDefault("rx", "")).split(" -")[0]), i, col++);
                modelInstances.setValueAt(getDisplayValue(Str.toString(stats.getOrDefault("tx", "")).split(" -")[0]), i, col++);
                modelInstances.setValueAt(getDisplayValue(stats.getOrDefault("cpu", "")), i, col++);
                modelInstances.setValueAt(getDisplayValue(stats.getOrDefault("memory", "")), i, col++);
            }
            
            if (searchInstance != null){
                for (int i=0; i< instances.size(); i++){
                    if (searchInstance.equals(instances.get(i))){
                        tableInstances.setRowSelectionInterval(i, i);
                        break;
                    }
                }
                searchInstance = null;
            }
        } catch (Exception ex){
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        
        int instanceIndex = tableInstances.getSelectedRow();
        boolean instanceSelected = instanceIndex>=0;
        currentInstance = instanceSelected ? modelInstances.getValueAt(instanceIndex, 0).toString() : null;            
        try{     
            buttonInstanceStop.setEnabled(instanceSelected);
            buttonRead.setEnabled(instanceSelected);
            buttonConfig.setEnabled(instanceSelected);
            buttonFunction.setEnabled(instanceSelected);
            buttonInstanceLogs.setEnabled(instanceSelected);
        } catch (Exception ex){
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }     
        try{
            if ((infoDialog!=null) &&  infoDialog.isShowing()){
                infoDialog.setInstance(currentInstance);        
                infoDialog.update(instanceInfo);        
            }
        } catch (Exception ex){
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }          
    }
   
    
    static boolean isPush(Map instanceData){
        if (instanceData==null){
            return false;
        }
        Map cfg = (Map) instanceData.getOrDefault("config", new HashMap());    
        if (cfg.getOrDefault("pipeline_type", "").equals("store")){
            return true;
        }
        if (cfg.getOrDefault("mode", "").equals("PUSH")){
            return true;
        }
        if (cfg.getOrDefault("mode", "").equals("PULL")){
            return false;
        }
        if (cfg.getOrDefault("pipeline_type", "").equals("fanout")){
            return true;
        }
        return false;
    }

    Map getInstanceCfg(String instance){
        return instanceInfo.get(instance);
    } 
    
    boolean isPush(String instance){
        return isPush((Map)getInstanceCfg(instance));
    }    
        
    String searchString;
    String searchInstance;
    void onSearch(){
        String str = textSearch.getText().trim().toLowerCase();
        if (!str.equals(searchString)){
            searchString = str;
            if (!str.isBlank()){                
                for (String instance:instanceInfo.keySet()){
                    if (instance.toLowerCase().contains(str)){
                        Map instanceData = instanceInfo.get(instance);
                        String server = Str.toString(instanceData.getOrDefault("host", "")).replace("http://","").toLowerCase();                        
                        for (int i=0; i<model.getRowCount();i++){
                            if (Str.toString(model.getValueAt(i, 0)).replace("http://","").toLowerCase().equals(server)){
                                table.setRowSelectionInterval(i, i);
                                searchInstance = instance;
                                updateControls();
                                return;
                            }
                        }
                    }
                }                              
            }
            searchInstance = null;
            table.clearSelection();
            updateControls();
        }
    }
    
    
    public static void main(String[] args) {
        String server = "http://localhost:8889";
        PanelStatus pn = new PanelStatus();
        pn.setProxy(new ProxyClient(server));
        SwingUtils.showFrame(null, pn.getUrl(), null, pn);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        proxyPanel = new javax.swing.JPanel();
        buttonProxyLogs = new javax.swing.JButton();
        buttonProxyRestart = new javax.swing.JButton();
        textProxy = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        textSearch = new javax.swing.JTextField();
        split = new javax.swing.JSplitPane();
        panelInstances = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableInstances = new javax.swing.JTable();
        buttonRead = new javax.swing.JButton();
        buttonConfig = new javax.swing.JButton();
        buttonInstanceStop = new javax.swing.JButton();
        buttonFunction = new javax.swing.JButton();
        buttonInstanceLogs = new javax.swing.JButton();
        panelServers = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonServerLogs = new javax.swing.JButton();
        buttonServerRestart = new javax.swing.JButton();
        buttonServerStop = new javax.swing.JButton();

        proxyPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proxy"));

        buttonProxyLogs.setText("Get Logs");
        buttonProxyLogs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonProxyLogsActionPerformed(evt);
            }
        });

        buttonProxyRestart.setText("Restart");
        buttonProxyRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonProxyRestartActionPerformed(evt);
            }
        });

        textProxy.setEditable(false);

        jLabel1.setText("Search Instance:");

        textSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textSearchKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout proxyPanelLayout = new javax.swing.GroupLayout(proxyPanel);
        proxyPanel.setLayout(proxyPanelLayout);
        proxyPanelLayout.setHorizontalGroup(
            proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(textProxy)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(textSearch)
                .addGap(18, 18, 18)
                .addComponent(buttonProxyLogs, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonProxyRestart, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        proxyPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonProxyLogs, buttonProxyRestart});

        proxyPanelLayout.setVerticalGroup(
            proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxyPanelLayout.createSequentialGroup()
                .addGroup(proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonProxyLogs)
                    .addComponent(buttonProxyRestart)
                    .addComponent(textProxy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(textSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        split.setDividerSize(4);
        split.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.5);

        panelInstances.setBorder(javax.swing.BorderFactory.createTitledBorder("Instances"));
        panelInstances.setPreferredSize(new java.awt.Dimension(593, 164));

        tableInstances.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Instance", "Time", "Clients", "RX", "TX", "CPU", "Mem"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableInstances.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableInstances.getTableHeader().setReorderingAllowed(false);
        tableInstances.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableInstancesMouseReleased(evt);
            }
        });
        tableInstances.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableInstancesKeyReleased(evt);
            }
        });
        jScrollPane3.setViewportView(tableInstances);

        buttonRead.setText("Inspect");
        buttonRead.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReadActionPerformed(evt);
            }
        });

        buttonConfig.setText("Config");
        buttonConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonConfigActionPerformed(evt);
            }
        });

        buttonInstanceStop.setText("Stop");
        buttonInstanceStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInstanceStopActionPerformed(evt);
            }
        });

        buttonFunction.setText("Script");
        buttonFunction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFunctionActionPerformed(evt);
            }
        });

        buttonInstanceLogs.setText("Get Logs");
        buttonInstanceLogs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInstanceLogsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelInstancesLayout = new javax.swing.GroupLayout(panelInstances);
        panelInstances.setLayout(panelInstancesLayout);
        panelInstancesLayout.setHorizontalGroup(
            panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInstancesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 474, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(buttonRead, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(buttonConfig, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(buttonInstanceStop, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(buttonInstanceLogs, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(buttonFunction, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        panelInstancesLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonConfig, buttonInstanceStop, buttonRead});

        panelInstancesLayout.setVerticalGroup(
            panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInstancesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelInstancesLayout.createSequentialGroup()
                        .addComponent(buttonRead)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonInstanceStop)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonFunction)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonConfig)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonInstanceLogs)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        split.setBottomComponent(panelInstances);

        panelServers.setBorder(javax.swing.BorderFactory.createTitledBorder("Servers"));
        panelServers.setPreferredSize(new java.awt.Dimension(602, 164));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Host", "Version", "Load", "RX", "TX", "CPU", "Memory"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
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

        buttonServerLogs.setText("Get Logs");
        buttonServerLogs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonServerLogsActionPerformed(evt);
            }
        });

        buttonServerRestart.setText("Restart");
        buttonServerRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonServerRestartActionPerformed(evt);
            }
        });

        buttonServerStop.setText("Stop All ");
        buttonServerStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonServerStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelServersLayout = new javax.swing.GroupLayout(panelServers);
        panelServers.setLayout(panelServersLayout);
        panelServersLayout.setHorizontalGroup(
            panelServersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelServersLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelServersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelServersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(buttonServerLogs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(buttonServerStop, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(buttonServerRestart, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        panelServersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonServerLogs, buttonServerRestart, buttonServerStop});

        panelServersLayout.setVerticalGroup(
            panelServersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelServersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelServersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelServersLayout.createSequentialGroup()
                        .addComponent(buttonServerLogs)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonServerRestart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonServerStop)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );

        split.setTopComponent(panelServers);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(split)
                    .addComponent(proxyPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(proxyPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(split, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        updateControls();
    }//GEN-LAST:event_tableMouseReleased

    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        updateControls();
    }//GEN-LAST:event_tableKeyReleased

    private void buttonServerLogsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonServerLogsActionPerformed
        try{
            if (currentServer==null){
                throw new Exception("No server selected");
            }            
            String server = currentServer;
            PanelLogs.show(this,  "Servers Logs - " + currentServer, schedulerPolling, ()->{return proxy.getLogs(server);});
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonServerLogsActionPerformed

    private void buttonServerStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonServerStopActionPerformed
        try{
            if (currentServer==null){
                throw new Exception("No server selected");
            }            
            schedulerPolling.submit(()->{
                try{
                    //InstanceManagerClient client = new InstanceManagerClient(currentServer, PipelineClient.prefix);
                    //client.stopAllInstances();
                    proxy.stopAllInstances(currentServer);
                } catch (Exception ex){
                    SwingUtils.showException(this, ex);
                }                    
            });           
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonServerStopActionPerformed

    private void buttonServerRestartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonServerRestartActionPerformed
        try{
            if (currentServer==null){
                throw new Exception("No server selected");
            }            
            schedulerPolling.submit(()->{
                try{
                    CamServerClient client = new CamServerClient(currentServer,"");
                    client.reset();
                } catch (Exception ex){
                    SwingUtils.showException(this, ex);
                }                    
            });           
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonServerRestartActionPerformed

    private void buttonInstanceStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInstanceStopActionPerformed
        try{
            if (currentServer==null){
                throw new Exception("No server selected");
            }
            if (currentInstance==null){
                throw new Exception("No insatance selected");
            }
                        
            schedulerPolling.submit(()->{
                try{
                    //InstanceManagerClient client = new InstanceManagerClient(currentServer, PipelineClient.prefix);
                    //client.stopInstance(currentInstance);                    
                        proxy.stopInstance(currentInstance);
                } catch (Exception ex){
                    SwingUtils.showException(this, ex);
                }                    
            });           
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonInstanceStopActionPerformed

    private void tableInstancesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableInstancesKeyReleased
        updateControls();
    }//GEN-LAST:event_tableInstancesKeyReleased

    private void tableInstancesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableInstancesMouseReleased
        updateControls();
    }//GEN-LAST:event_tableInstancesMouseReleased

    private void buttonReadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReadActionPerformed
        try{
            Map instanceData = instanceInfo.get(currentInstance);
            String address = (String)instanceData.get("stream_address");
            StreamDialog dlg = new StreamDialog(SwingUtils.getWindow(this), false, address, isPush(instanceData));
            dlg.setVisible(true);
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonReadActionPerformed

    private void buttonConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonConfigActionPerformed
        try{       
            if (currentServer==null){
                throw new Exception("No server selected");
            }
            if (currentInstance==null){
                throw new Exception("No insatance selected");
            }
            
            Map instanceData = instanceInfo.get(currentInstance);
            Map cfg = (Map) instanceData.getOrDefault("config", new HashMap());    
            String json = JsonSerializer.encode(cfg, true);
            ScriptEditor dlg = new ScriptEditor(SwingUtils.getFrame(this), true, currentInstance, json, "json");
            dlg.setVisible(true);
            if (dlg.getResult()){
                json = dlg.ret;
                cfg = (Map) JsonSerializer.decode(json, Map.class);
                
                if (isPipeline){
                    PipelineClient client = new PipelineClient(currentServer);
                    client.setInstanceConfig(currentInstance, cfg);                    
                } else {
                    CameraClient client = new CameraClient(currentServer);
                    client.setConfig(currentInstance, cfg);
                    proxy.setNamedConfig(currentInstance, dlg.ret);
                }                
            }        
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }        
    }//GEN-LAST:event_buttonConfigActionPerformed

    private void buttonProxyLogsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonProxyLogsActionPerformed
        try{
            PanelLogs.show(this,  "Proxy Logs", schedulerPolling, ()->{return proxy.getLogs();});
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonProxyLogsActionPerformed

    private void buttonProxyRestartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonProxyRestartActionPerformed
        try{
            schedulerPolling.submit(()->{
                try{
                    proxy.reset();
                } catch (Exception ex){
                    SwingUtils.showException(this, ex);
                }                    
            });           
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonProxyRestartActionPerformed

    private void buttonFunctionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFunctionActionPerformed
        try{     
            if (isPipeline){
                PipelineClient client = new PipelineClient(getUrl());
                Map instanceData = instanceInfo.get(currentInstance);
                List<String> ret = client.getScripts();
                Collections.sort(ret);
                String[] scripts = ret.toArray(new String[0]);            
                Map cfg = (Map) instanceData.getOrDefault("config", new HashMap());                
                String current = (String) cfg.getOrDefault("function", null);       
                current = Arr.containsEqual(scripts, current) ? current : null;
                String msg = "Select the processing script for for the pipeline" + currentInstance + ": ";
                String script = SwingUtils.getString(this, msg, scripts, current);       
                if (script!=null){
                    client.setFunction(currentInstance, script);
                }
            } else {                        
                schedulerPolling.submit(()->{
                    try{
                        proxy.deleteInstance(currentInstance);
                    } catch (Exception ex){
                        SwingUtils.showException(this, ex);
                    }                    
                });             
            }        
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }        
    }//GEN-LAST:event_buttonFunctionActionPerformed

    private void textSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textSearchKeyReleased
        try{     
            onSearch();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
     }//GEN-LAST:event_textSearchKeyReleased

    private void buttonInstanceLogsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInstanceLogsActionPerformed
        try{
            if (currentServer==null){
                throw new Exception("No server selected");
            }            
            
            if (currentInstance==null){
                throw new Exception("No instance selected");
            }
            String server = currentServer;
            String instance = currentInstance;
            PanelLogs.show(this,  "Instance Logs - " + currentInstance, schedulerPolling, ()->{return proxy.getLogs(server, instance);});
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonInstanceLogsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonConfig;
    private javax.swing.JButton buttonFunction;
    private javax.swing.JButton buttonInstanceLogs;
    private javax.swing.JButton buttonInstanceStop;
    private javax.swing.JButton buttonProxyLogs;
    private javax.swing.JButton buttonProxyRestart;
    private javax.swing.JButton buttonRead;
    private javax.swing.JButton buttonServerLogs;
    private javax.swing.JButton buttonServerRestart;
    private javax.swing.JButton buttonServerStop;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPanel panelInstances;
    private javax.swing.JPanel panelServers;
    private javax.swing.JPanel proxyPanel;
    private javax.swing.JSplitPane split;
    private javax.swing.JTable table;
    private javax.swing.JTable tableInstances;
    private javax.swing.JTextField textProxy;
    private javax.swing.JTextField textSearch;
    // End of variables declaration//GEN-END:variables
}
