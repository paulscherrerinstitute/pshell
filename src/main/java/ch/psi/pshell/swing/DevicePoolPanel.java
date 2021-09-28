package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.DevicePool;
import ch.psi.pshell.core.DevicePoolListener;
import ch.psi.pshell.core.LogManager;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.ProcessVariable;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.ReadbackDeviceAdapter;
import ch.psi.pshell.device.ReadonlyProcessVariable;
import ch.psi.pshell.device.Stoppable;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.ui.View;
import ch.psi.utils.swing.ConfigDialog;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
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
        historyDialogs = new HashMap<>();
        setupMenu();
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
                        Context.getInstance().reinit(dev);
                    } catch (Exception ex) {
                        Logger.getLogger(DevicePoolPanel.class.getName()).log(Level.WARNING, null, ex);
                    }
                }, "Device Stopping Thread - " + dev.getName()).start();
            }
        });
        menuStop.addActionListener((ActionEvent e) -> {
            GenericDevice dev = getSelectedDevice();
            if ((dev != null) && (dev instanceof Stoppable)) {
                new Thread(() -> {
                    try {
                        Context.getInstance().stop(dev);
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
                    dlg.setReadOnly(Context.getInstance().getRights().denyDeviceConfig);
                    dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    dlg.setListener((StandardDialog sd, boolean accepted) -> {
                        if (sd.getResult()) {
                            if (dev.getState() == State.Closing) {
                                SwingUtils.showMessage(getTopLevelAncestor(), "Error", "Device has been disposed");
                            } else {
                                Context.getInstance().saveDeviceConfiguration(dev);// Not doing with cfg in order to give the context a change to log
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
                    if (dev instanceof Device) {
                        showHistory((Device) dev);
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
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
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
                            menuRevisionHistory.setEnabled(menuConfig.isEnabled() && Context.getInstance().isVersioningEnabled());
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                            menuHistory.setEnabled(getSelectedDevice() instanceof Device);
                            menuPanel.setEnabled(App.getInstance().getDevicePanelManager().hasControlPanel(getSelectedDevice()));
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

    public static class DefaultPanel implements Serializable {

        private static final long serialVersionUID = 1L;

        public DefaultPanel(String deviceClassName, String panelClassName) {
            this.deviceClassName = deviceClassName;
            this.panelClassName = panelClassName;
        }
        public String deviceClassName;
        public String panelClassName;

        public Class getDeviceClass() throws ClassNotFoundException {
            return Context.getInstance().getClassByName(deviceClassName);
        }

        public Class getPanelClass() throws ClassNotFoundException {
            return Context.getInstance().getClassByName(panelClassName);
        }
    }

    public void initialize() {
        addDevicePoolListener();
    }

    void addDevicePoolListener() {
        Context.getInstance().getDevicePool().addListener(new DevicePoolListener() {

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

    DeviceListener listener = new ReadbackDeviceAdapter() {
        @Override
        public void onStateChanged(Device device, State state, State former) {
            if (asyncUpdate && isShowing()) {
                int row = getDeviceRow(device);
                if (row >= 0) {
                    model.setValueAt(state, row, 2);
                }
            }
            if (state == State.Closing) {
                if (historyDialogs.containsKey(device)) {
                    for (Component hc : SwingUtils.getComponentsByType(historyDialogs.get(device), HistoryChart.class)) {
                        ((HistoryChart) hc).close();
                    }
                    historyDialogs.get(device).setVisible(false);
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
                String units = (device instanceof ReadonlyProcessVariable) ? " " + ((ReadonlyProcessVariable) device).getUnit() : "";
                model.setValueAt(Str.toString(value, 10) + units, row, 3);
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
            for (GenericDevice dev : Context.getInstance().getDevicePool().getAllDevices(type)) {
                if (dev instanceof Device) {
                    ((Device) dev).addListener(listener);
                }
            }
        }
    }

    void removeListeners() {
        for (Device dev : Context.getInstance().getDevicePool().getAllDevices(Device.class)) {
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
            return Context.getInstance().getDevicePool().getByName((String) model.getValueAt(row, 0), type);
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
        Context context = Context.getInstance();
        try {
            if (context.getDevicePool() == null) {
                model.setRowCount(0);
            } else {
                int deviceCount = context.getDevicePool().getDeviceCount(type);
                if ((context.getDevicePool() != pool) || (deviceCount != model.getRowCount())) {
                    if (context.getDevicePool() != pool) {
                        addDevicePoolListener();
                    }
                    model.setRowCount(deviceCount);
                    for (int i = 0; i < deviceCount; i++) {
                        GenericDevice dev = context.getDevicePool().getByIndex(i, type);
                        String type = Nameable.getShortClassName(dev.getClass());

                        model.setValueAt(dev.getName(), i, 0);
                        model.setValueAt(type, i, 1);

                    }
                    setAsyncUpdate(asyncUpdate);
                    addListeners();
                }
                pool = context.getDevicePool();
                int row = 0;
                for (GenericDevice dev : context.getDevicePool().getAllDevices(type)) {
                    String[] value = LogManager.getDeviceInfo(dev, 10);
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

    final Map<Device, JDialog> historyDialogs;

    void showHistory(Device dev) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Frame frame = (Frame) getTopLevelAncestor();
        if (historyDialogs.containsKey(dev)) {
            JDialog dlg = historyDialogs.get(dev);
            if (dlg.isDisplayable()) {
                dlg.requestFocus();
                return;
            }
        }
        HistoryChart chart = HistoryChart.create(dev);
        JDialog dlg = SwingUtils.showDialog(frame, dev.getName(), null, chart);
        historyDialogs.put(dev, dlg);
    }

    protected void onDoubleClick(GenericDevice dev) throws Exception {
        if (((View) App.getInstance().getMainFrame()).showPanel(dev) == null) {
            //Update value in table if has no panel
            if (dev instanceof Device) {
                //dev.request();
                showHistory((Device) dev);
            }
        }
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
