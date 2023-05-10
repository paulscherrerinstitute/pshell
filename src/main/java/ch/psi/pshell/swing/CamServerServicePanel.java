package ch.psi.pshell.swing;

import ch.psi.pshell.camserver.CamServerService;
import ch.psi.pshell.camserver.CamServerStream;
import ch.psi.pshell.camserver.CameraStream;
import ch.psi.pshell.camserver.PipelineStream;
import ch.psi.pshell.camserver.ProxyClient;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.ui.App;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.NamedThreadFactory;
import ch.psi.utils.Str;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        model = (DefaultTableModel) table.getModel();
    }
    
    @Override
    public CamServerService getDevice() {
        return (CamServerService) super.getDevice();
    }
    
    public CamServerService.Type getType(){
        return getDevice().getType();
    }
    
    public ProxyClient getProxy() {
        CamServerService device = getDevice();
        return (device==null) ? null : device.getProxy();
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

    public static CamServerServicePanel create(Window parent, String url, String title, CamServerService.Type type) {
        CamServerServicePanel viewer = new CamServerServicePanel();
        SwingUtilities.invokeLater(() -> {
            try {                
                viewer.setDevice(new CamServerService("CamServer"+type.toString()+"Service", url, type));
                Window window = SwingUtils.showFrame(parent, title, new Dimension(800, 600), viewer);
                window.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            } catch (Exception ex) {
                Logger.getLogger(CamServerServicePanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return viewer;
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
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMouseClicked(evt);
            }
        });
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

    private void tableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseClicked
        try {
            if ((evt.getClickCount() == 2) && (!evt.isPopupTrigger())) {
                int index = table.convertRowIndexToModel(table.getSelectedRow());
                String currentInstance = (index >= 0) ? model.getValueAt(index, 0).toString() : null;
                if (stream != null) {
                    stream.close();
                    stream = null;
                }
                if (getType() == CamServerService.Type.Camera) {
                    stream = new CameraStream("Camera Stream", getProxy().getUrl(), currentInstance);
                } else {
                    stream = new PipelineStream("Pipeline Stream", getProxy().getUrl(), currentInstance);
                }
                stream.setMonitored(true);
                stream.initialize();
                App.getDevicePanelManager().showPanel(stream, getWindow());
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_tableMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
