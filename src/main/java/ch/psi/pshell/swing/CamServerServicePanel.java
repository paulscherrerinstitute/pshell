package ch.psi.pshell.swing;

import ch.psi.pshell.camserver.CamServerService;
import ch.psi.pshell.camserver.CamServerStream;
import ch.psi.pshell.camserver.CameraStream;
import ch.psi.pshell.camserver.PipelineClient;
import ch.psi.pshell.camserver.PipelineStream;
import ch.psi.pshell.camserver.ProxyClient;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceListener;
import static ch.psi.pshell.swing.DevicePanel.createFrame;
import ch.psi.pshell.ui.App;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.NamedThreadFactory;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.ScriptDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.stream.Collectors;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 *
 */
public class CamServerServicePanel extends DevicePanel {

    Timer timer;
    final DefaultTableModel model;
    ScheduledExecutorService schedulerPolling;
    final Map<String, ImmutablePair<CamServerStream, Window>> streams = new HashMap<>();

    public CamServerServicePanel() {
        initComponents();
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuConfig = new JMenuItem("Configuration");
        JMenuItem menuInspect = new JMenuItem("Inspect");
        JMenuItem menuStop = new JMenuItem("Stop");
        JMenuItem menuDuplicate = new JMenuItem("Duplicate");
        menuConfig.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String currentInstance = getCurrentInstance();
                    if (currentInstance != null) {
                        Map<String, Object> cfg = getDevice().getClient().getInstanceConfig(currentInstance);
                        if (cfg != null) {
                            String json = EncoderJson.encode(cfg, true);
                            ScriptDialog dlg = new ScriptDialog(getWindow(), true, currentInstance, json, "json");
                            dlg.setVisible(true);
                            if (dlg.getResult()) {
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
                    if (currentInstance != null) {
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
                    if (currentInstance != null) {
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
                    if (currentInstance != null) {
                        Map<String, Object> cfg = getDevice().getClient().getInstanceConfig(currentInstance);;
                        if (cfg != null) {
                            cfg.put("port", 0); //Make sure get new port
                            cfg.put("no_client_timeout", 60.0);    //Set a timeout to 1min                                                    
                            String instanceName = getDuplicatedInstanceName(cfg.get("name"));
                            List<String> ret = ((PipelineClient) getDevice().getClient()).createFromConfig(cfg, instanceName);
                            String instance_id = ret.get(0);
                            showMessage("Success", "Duplicated instance name:\n" + instance_id);
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

    public String getCurrentInstance() {
        int index = table.convertRowIndexToModel(table.getSelectedRow());
        return (index >= 0) ? model.getValueAt(index, 0).toString() : null;
    }

    public void showInstance(String instance) throws IOException, InterruptedException {        
        for (String stream :streams.keySet()) {
            if (stream.equals(instance)){
                Window dlg = streams.get(stream).right;
                if (dlg.isShowing()){
                    dlg.requestFocus();
                    return;
                }
            }
        }
        CamServerStream stream = 
            (getType() == CamServerService.Type.Camera) ?
            new CameraStream(instance, getProxy().getUrl(), instance) :
            new PipelineStream(instance, getProxy().getUrl(), instance);
        stream.setMonitored(true);
        stream.initialize();
        stream.addListener(new DeviceAdapter() {
            @Override
            public void onStateChanged(Device device, State state, State former) {
                if (state==State.Closing){
                    ImmutablePair<CamServerStream, Window> pair = streams.remove(instance);
                    try{
                        pair.left.close();
                    } catch (Exception ex){
                       Logger.getLogger(getClass().getName()).log(Level.INFO, null, ex);
                    }                    
                }
            }
        });
        MonitoredPanel panel = App.getDevicePanelManager().showPanel(stream, getWindow());
        Window window = panel.getWindow();
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try{
                    stream.close();
                } catch (Exception ex){
                   Logger.getLogger(getClass().getName()).log(Level.INFO, null, ex);
                }
                streams.remove(instance);
            }
        });
        streams.put(instance, new ImmutablePair(stream, window));
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
        onDesactive();
        super.setDevice(device);
        if (getDevice() != null) {
        }
    }

    protected String getDuplicatedInstanceName(Object pipelineName) throws IOException {
        //Get next name not used
        int instanceIndex = 1;
        String instanceName = null;
        if (pipelineName != null) {
            while (true) {
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
                update();
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
        for (ImmutablePair<CamServerStream, Window> stream :streams.values()) {
            stream.left.close();
        }
    }

    void update() {
        if (isShowing()) {
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
    }

    void update(Map<String, Map<String, Object>> servers, Map<String, Map<String, Object>> active_instances) {
        try {
            List<String> keys = new ArrayList<>(active_instances.keySet());

            if ((filterName != null) && (!filterName.isBlank())) {
                keys = keys
                        .stream()
                        .filter(c -> c.toLowerCase().contains(filterName))
                        .collect(Collectors.toList());
            }

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

    void setFilter(String str) {
        if (str == null) {
            str = "";
        }
        if (!str.equals(filterName)) {
            filterName = str.trim();
            schedulerPolling.submit(() -> {
                update();
            }
            );
        }
    }

    String filterName;

    void onFilter() {
        setFilter(textFilter.getText().trim().toLowerCase());
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
        jLabel1 = new javax.swing.JLabel();
        textFilter = new javax.swing.JTextField();

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

        jLabel1.setText("Filter Name:");

        textFilter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textFilterKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 861, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFilter)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void textFilterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFilterKeyReleased
        try {
            onFilter();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_textFilterKeyReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable table;
    private javax.swing.JTextField textFilter;
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
