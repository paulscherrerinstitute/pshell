package ch.psi.pshell.swing;

import ch.psi.pshell.camserver.CamServerService;
import ch.psi.pshell.camserver.CamServerStream;
import ch.psi.pshell.camserver.CameraStream;
import ch.psi.pshell.camserver.PipelineClient;
import ch.psi.pshell.camserver.PipelineStream;
import ch.psi.pshell.camserver.ProxyClient;
import ch.psi.pshell.device.Device;
import static ch.psi.pshell.swing.DevicePanel.createFrame;
import ch.psi.pshell.ui.App;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.NamedThreadFactory;
import ch.psi.utils.Str;
import ch.psi.utils.swing.ScriptDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class CamServerServicePanel extends DevicePanel {

    Timer timer;
    final DefaultTableModel model;
    ScheduledExecutorService schedulerPolling;
    CamServerStream stream;

    public CamServerServicePanel() {
        initComponents();
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuConfig = new JMenuItem("Configuration");
        JMenuItem menuInspect = new JMenuItem("Inspect");
        JMenuItem menuStop = new JMenuItem("Stop");
        JMenuItem menuDuplicate= new JMenuItem("Duplicate");
        menuConfig.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String currentInstance = getCurrentInstance();
                    if (currentInstance!=null){                        
                        Map <String, Object> cfg = getDevice().getClient().getInstanceConfig(currentInstance);
                        if (cfg!=null){
                            String json = EncoderJson.encode(cfg, true);
                            ScriptDialog dlg = new ScriptDialog(getWindow(), true, currentInstance, json, "json");
                            dlg.setVisible(true);
                            if (dlg.getResult()){
                                json = dlg.getText();
                                cfg = (Map) EncoderJson.decode(json, Map.class);
                                getDevice().getClient().setInstanceConfig(currentInstance, cfg);
                            }                    
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });        

        menuInspect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                        String currentInstance = getCurrentInstance();
                         if (currentInstance!=null){
                            showInstance(currentInstance);
                         }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });        

        menuStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String currentInstance = getCurrentInstance();
                    if (currentInstance!=null){
                        getDevice().getProxy().stopInstance(currentInstance);
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });                
        
        menuDuplicate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String currentInstance = getCurrentInstance();
                    if (currentInstance!=null){
                        Map <String, Object> cfg =  getDevice().getClient().getInstanceConfig(currentInstance);;
                        if (cfg!=null){
                            cfg.put("port", 0); //Make sure get new port
                            cfg.put("no_client_timeout", 60.0);    //Set a timeout to 1min                                                    
                            String instanceName = getDuplicatedInstanceName(cfg.get("name"));
                            List<String> ret = ((PipelineClient)getDevice().getClient()).createFromConfig(cfg, instanceName);
                            String instance_id = ret.get(0);                            
                            showMessage("Success", "Duplicated instance name:\n" +instance_id);
                        }
                    }                    
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });  
        popupMenu.add(menuConfig);
        popupMenu.add(menuInspect);
        popupMenu.addSeparator();
        popupMenu.add(menuDuplicate);             
        popupMenu.add(menuStop);
        
        
        model = (DefaultTableModel) table.getModel();                
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger()) && !popupMenu.isVisible()) {
                        String currentInstance = getCurrentInstance();
                        showInstance(currentInstance);
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopupMenu(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkPopupMenu(e);
            }

            void checkPopupMenu(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        menuDuplicate.setEnabled(getType() == CamServerService.Type.Pipeline);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } catch (Exception ex) {
                }
            }

        });

    }

    public String getCurrentInstance(){
        int index = table.convertRowIndexToModel(table.getSelectedRow());
        return (index >= 0) ? model.getValueAt(index, 0).toString() : null;
    }
    
    public void showInstance(String instance) throws IOException, InterruptedException{
        if (stream != null) {
            stream.close();
            stream = null;
        }
        if (getType() == CamServerService.Type.Camera) {
            stream = new CameraStream("Camera Stream", getProxy().getUrl(), instance);
        } else {
            stream = new PipelineStream("Pipeline Stream", getProxy().getUrl(), instance);
        }
        stream.setMonitored(true);
        stream.initialize();
        App.getDevicePanelManager().showPanel(stream, getWindow());        
    }
    
    @Override
    public CamServerService getDevice() {
        return (CamServerService) super.getDevice();
    }

    public CamServerService.Type getType() {        
        return getDevice().getType();
    }

    public ProxyClient getProxy() {
        CamServerService device = getDevice();
        return (device == null) ? null : device.getProxy();
    }

    @Override
    public void setDevice(Device device) {
        if (stream != null) {
            stream.close();
        }
        super.setDevice(device);
        if (getDevice() != null) {
        }
    }
    
    protected String getDuplicatedInstanceName(Object pipelineName) throws IOException{            
        //Get next name not used
        int instanceIndex = 1;
        String instanceName = null;
        if (pipelineName!=null){
            while (true){
                instanceName = pipelineName + "_" + instanceIndex;
                if (!getDevice().getProxy().getInstances().containsKey(instanceName)) {
                    break;
                }
                instanceIndex++;
            }     
        }
        return instanceName;
    }           

    @Override
    protected void onShow() {
        try {
            schedulerPolling = Executors.newSingleThreadScheduledExecutor(
                    new NamedThreadFactory("CamServerServicePanel update thread"));
            schedulerPolling.scheduleWithFixedDelay(() -> {
                if (isShowing()) {
                    update();
                } else {
                    Logger.getLogger(getClass().getName()).info("");
                }
            }, 10, 3000, TimeUnit.MILLISECONDS);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onHide() {
        try {
            schedulerPolling.shutdownNow();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDesactive() {
        if (stream != null) {
            stream.close();
        }
    }

    void update() {
        ProxyClient proxy = getProxy();
        try {
            if (proxy != null) {
                Map<String, Object> info = proxy.getInfo();
                Map<String, Map<String, Object>> servers = (Map<String, Map<String, Object>>) info.get("servers");
                Map<String, Map<String, Object>> active_instances = (Map<String, Map<String, Object>>) info.get("active_instances");

                SwingUtilities.invokeLater(() -> {
                    update(servers, active_instances);
                });
            } else {
                model.setRowCount(0);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, null, ex);
        }
    }

    void update(Map<String, Map<String, Object>> servers, Map<String, Map<String, Object>> active_instances) {
        try {
            ArrayList<String> keys = new ArrayList<>(active_instances.keySet());
            Collections.sort(keys);
            model.setRowCount(keys.size());
            for (int i = 0; i < keys.size(); i++) {
                String instanceName = keys.get(i);
                int col = 0;
                model.setValueAt(instanceName, i, col++);
                Map data = active_instances.getOrDefault(instanceName, new HashMap());
                Map stats = (Map) data.getOrDefault("statistics", new HashMap());
                //modelInstances.setValueAt(data.getOrDefault("stream_address", ""), i, col++);
                model.setValueAt(CamServerStreamPanel.getDisplayValue(stats.getOrDefault("time", "")), i, col++);
                model.setValueAt(CamServerStreamPanel.getDisplayValue(stats.getOrDefault("clients", "")), i, col++);
                model.setValueAt(CamServerStreamPanel.getDisplayValue(Str.toString(stats.getOrDefault("rx", "")).split(" -")[0]), i, col++);
                model.setValueAt(CamServerStreamPanel.getDisplayValue(Str.toString(stats.getOrDefault("tx", "")).split(" -")[0]), i, col++);
                model.setValueAt(CamServerStreamPanel.getDisplayValue(stats.getOrDefault("cpu", "")), i, col++);
                model.setValueAt(CamServerStreamPanel.getDisplayValue(stats.getOrDefault("memory", "")), i, col++);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static CamServerServicePanel createFrame(String url, Window parent, String title) throws Exception {
        return createFrame(url, null, parent, title);
    }

    public static CamServerServicePanel createFrame(String url, CamServerService.Type type, Window parent, String title) throws Exception {
        if (type == null) {
            type = CamServerService.Type.Pipeline;
            try {
                //Default CameraServer ports are even
                if ((Integer.valueOf(url.substring(url.length() - 1)) % 2) == 0) {
                    type = CamServerService.Type.Camera;
                }
            } catch (Exception ex) {
            }
        }
        CamServerService camServerService = new CamServerService(url, url, type);
        CamServerServicePanel viewer = new CamServerServicePanel();
        return (CamServerServicePanel) createFrame(camServerService, parent, title, viewer);
    }

    ////////
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setPreferredSize(new java.awt.Dimension(873, 600));

        table.setModel(new javax.swing.table.DefaultTableModel(
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
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables

    public static void main(String[] args) {
        try {
            App.init(args);
            //String url="localhost:8889";            
            String url = args[0];
            createFrame(url, null, null);
        } catch (Exception ex) {
            SwingUtils.showException(null, ex);
        }
    }

}
