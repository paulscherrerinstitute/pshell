package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.ReadbackDeviceListener;
import ch.psi.pshell.device.ReadonlyProcessVariable;
import ch.psi.pshell.device.Stoppable;
import ch.psi.pshell.devices.DevicePanelFactory;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.devices.DevicePoolListener;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.logging.Logging;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.State;
import ch.psi.pshell.versioning.VersionControl;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class DevicePoolPanel extends MonitoredPanel implements UpdatablePanel {

    final DefaultTableModel model;

    public DevicePoolPanel() {
        initComponents();
        model = (DefaultTableModel) table.getModel();        
        setupMenu();
    }
    
    static boolean deviceConfigPanelEnabled;    
    
    public static void setDeviceConfigPanelEnabled(boolean value) {
           deviceConfigPanelEnabled = value;
    }    
    
    public static boolean isDeviceConfigPanelEnabled() {
           return deviceConfigPanelEnabled;
    }
    

    DevicePool pool;
    boolean asyncUpdate;
    JPopupMenu popupMenu;
    //JMenu menuConfig;
    JMenuItem menuUpdate;
    JMenuItem menuInitialize;
    JMenuItem menuStop;
    JMenuItem menuPanel;
    JMenuItem menuHistory;
    JMenuItem menuConfig;
    JMenuItem menuRevisionHistory;

    final void setupMenu() {
        popupMenu = new JPopupMenu();
        menuHistory = new JMenuItem("History chart");
        menuPanel = new JMenuItem("Panel");
        menuConfig = new JMenuItem("Configuration");
        menuRevisionHistory = new JMenuItem("Revision history");
        menuUpdate = new JMenuItem("Update");
        menuInitialize = new JMenuItem("Initialize");
        menuStop = new JMenuItem("Stop");

        menuUpdate.addActionListener((ActionEvent e) -> {
            GenericDevice dev = getSelectedDevice();
            if (dev != null) {
                try {
                    dev.request();
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });
        menuInitialize.addActionListener((ActionEvent e) -> {
            GenericDevice dev = getSelectedDevice();
            if (dev != null) {
                new Thread(() -> {
                    try {
                        //dev.initialize();
                        if (DevicePool.hasInstance()) {
                            DevicePool.getInstance().retryInitializeDevice(dev);
                        }                        
                    } catch (Exception ex) {
                        Logger.getLogger(DevicePoolPanel.class.getName()).log(Level.WARNING, null, ex);
                    }
                }, "Device Stopping Thread - " + dev.getName()).start();
            }
        });
        menuStop.addActionListener((ActionEvent e) -> {
            GenericDevice dev = getSelectedDevice();
            if (dev instanceof Stoppable stoppable) {
                new Thread(() -> {
                    try {
                        stoppable.stop();
                    } catch (Exception ex) {
                        Logger.getLogger(DevicePoolPanel.class.getName()).log(Level.WARNING, null, ex);
                    }
                }, "Device Stopping Thread - " + dev.getName()).start();
            }
        });
        menuConfig.addActionListener((ActionEvent e) -> {
            final GenericDevice dev = getSelectedDevice();
            if ((dev != null) && (dev.getConfig() != null)) {
                try {
                    final ConfigDialog dlg = new ConfigDialog((Frame) getTopLevelAncestor(), false);
                    dlg.setTitle("Device Configuration: " + dev.getName());
                    dlg.setConfig(dev.getConfig());
                    dlg.setReadOnly(!isDeviceConfigPanelEnabled());
                    dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    dlg.setListener((StandardDialog sd, boolean accepted) -> {
                        if (sd.getResult()) {
                            if (dev.getState() == State.Closing) {
                                SwingUtils.showMessage(getTopLevelAncestor(), "Error", "Device has been disposed");
                            } else {
                                try {
                                    dev.getConfig().save();
                                } catch (Exception ex) {
                                    Logger.getLogger(DevicePoolPanel.class.getName()).log(Level.WARNING, null, ex);
                                }
                            }
                        }
                    });
                    dlg.setVisible(true);
                    SwingUtils.centerComponent(getTopLevelAncestor(), dlg);
                    dlg.requestFocus();

                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        menuRevisionHistory.addActionListener((ActionEvent e) -> {
            GenericDevice dev = getSelectedDevice();
            if (dev != null) {
                try {
                    showConfigHistory(dev);
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        menuHistory.addActionListener((ActionEvent e) -> {
            GenericDevice dev = getSelectedDevice();
            if (dev != null) {
                try {
                    if (dev instanceof Device d) {
                         DevicePanelFactory.getInstance().showHistory(d);
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });
        menuPanel.addActionListener((ActionEvent e) -> {
            GenericDevice dev = getSelectedDevice();
            if (dev != null) {
                try {
                    onDoubleClick(dev);
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });

        popupMenu.add(menuHistory);
        popupMenu.add(menuPanel);
        popupMenu.add(menuConfig);
        popupMenu.add(menuRevisionHistory);
        popupMenu.addSeparator();
        popupMenu.add(menuUpdate);
        popupMenu.add(menuInitialize);
        popupMenu.add(menuStop);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger()) && !popupMenu.isVisible()) {
                        if (getSelectedDevice() != null) {
                            onDoubleClick(getSelectedDevice());
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        int r = table.rowAtPoint(e.getPoint());
                        if (r >= 0 && r < table.getRowCount()) {
                            table.setRowSelectionInterval(r, r);
                        } else {
                            table.clearSelection();
                        }

                        if (getSelectedDevice() != null) {
                            menuStop.setEnabled(getSelectedDevice() instanceof Stoppable);
                            menuConfig.setEnabled(getSelectedDevice().getConfig() != null);
                            menuRevisionHistory.setEnabled(menuConfig.isEnabled() && VersionControl.hasInstance());
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                            menuHistory.setEnabled(getSelectedDevice() instanceof Device);
                            menuPanel.setEnabled(DevicePanelFactory.getInstance().hasControlPanel(getSelectedDevice()));
                            menuPanel.setText((getSelectedDevice() instanceof Source) ? "Render" : "Control panel");
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }

            }
        }
        );

    }
    public void initialize() {
        addDevicePoolListener();
    }

    void addDevicePoolListener() {
        DevicePool.getInstance().addListener(new DevicePoolListener() {

            @Override
            public void onDeviceAdded(GenericDevice dev) {
                pool = null;
            }

            @Override
            public void onDeviceRemoved(GenericDevice dev) {
                pool = null;
            }
        });
    }

    public void setAsyncUpdate(boolean value) {
        this.asyncUpdate = value;
    }

    GenericDevice getSelectedDevice() {
        return getRowDevice(table.getSelectedRow());
    }

    DeviceListener listener = new ReadbackDeviceListener() {
        @Override
        public void onStateChanged(Device device, State state, State former) {
            if (asyncUpdate && isShowing()) {
                int row = getDeviceRow(device);
                if (row >= 0) {
                    model.setValueAt(state, row, 2);
                }
            }
        }

        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            if (asyncUpdate && isShowing()) {
                if (!(device instanceof ReadbackDevice)) {
                    setValue(device, value);
                }
            }
        }

        @Override
        public void onReadbackChanged(Device device, Object value) {
            if (asyncUpdate && isShowing()) {
                setValue(device, value);
            }
        }

        void setValue(Device device, Object value) {
            int row = getDeviceRow(device);
            if (row >= 0) {
                String units = (device instanceof ReadonlyProcessVariable pv) ? " " + pv.getUnit() : "";
                model.setValueAt(Logging.getLogForValue(value) + units, row, 3);
                model.setValueAt("00:00:00", row, 4);
            }
        }

    };

    @Override
    protected void onShow() {
//        if (asyncUpdate) {
//            addListeners();
//        }
    }

    @Override
    protected void onHide() {
//        if (asyncUpdate) {
//            removeListeners();
//        }
    }

    void addListeners() {
        if ((type == null) || (type == GenericDevice.class) || (Device.class.isAssignableFrom(type))) {
            for (GenericDevice dev : DevicePool.getInstance().getAllDevices(type)) {
                if (dev instanceof Device device) {
                    device.addListener(listener);
                }
            }
        }
    }

    void removeListeners() {
        for (Device dev : DevicePool.getInstance().getAllDevices(Device.class)) {
            dev.removeListener(listener);
        }
    }

    int getDeviceRow(GenericDevice dev) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (dev.getName().equals(model.getValueAt(i, 0))) {
                return i;
            }
        }
        return -1;
    }

    GenericDevice getRowDevice(int row) {
        try {
            return DevicePool.getInstance().getByName((String) model.getValueAt(row, 0), type);
        } catch (Exception ex) {
            return null;
        }
    }

    Class type = null;

    public void setType(Class type) {
        removeListeners();
        this.type = type;
        pool = null;
    }

    public Class getType() {
        return type;
    }

    @Override
    public void update() {
        try {
            if (!DevicePool.hasInstance()) {
                model.setRowCount(0);
            } else {
                int deviceCount = DevicePool.getInstance().getDeviceCount(type);
                if ((DevicePool.getInstance() != pool) || (deviceCount != model.getRowCount())) {
                    if (DevicePool.getInstance() != pool) {
                        addDevicePoolListener();
                    }
                    model.setRowCount(deviceCount);
                    for (int i = 0; i < deviceCount; i++) {
                        GenericDevice dev = DevicePool.getInstance().getByIndex(i, type);
                        String type = Nameable.getShortClassName(dev.getClass());

                        model.setValueAt(dev.getName(), i, 0);
                        model.setValueAt(type, i, 1);

                    }
                    setAsyncUpdate(asyncUpdate);
                    addListeners();
                }
                pool = DevicePool.getInstance();
                int row = 0;
                for (GenericDevice dev : pool.getAllDevices(type)) {
                    String[] value = DevicePool.getDeviceInfo(dev, 10);
                    model.setValueAt(String.valueOf(dev.getState()), row, 2);
                    model.setValueAt(value[0], row, 3);
                    model.setValueAt(value[1], row, 4);
                    row++;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DevicePoolPanel.class.getName()).log(Level.INFO, null, ex);
            model.setRowCount(0);
        }
    }

    void showConfigHistory(GenericDevice dev) throws Exception {
        String fileName = dev.getConfig().getFileName();
        Frame parent = (Frame) this.getTopLevelAncestor();
        RevisionHistoryDialog dlg = new RevisionHistoryDialog(parent, false, fileName);
        SwingUtils.centerComponent(parent, dlg);
        dlg.setVisible(true);
    }

    protected void onDoubleClick(GenericDevice dev) throws Exception {
        DevicePanelFactory.getInstance().showPanel(dev);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Device", "Type", "State", "Value", "Age"
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
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
