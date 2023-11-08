package ch.psi.pshell.swing;

import ch.psi.pshell.bs.AddressableDevice;
import ch.psi.pshell.bs.ProviderConfig.SocketType;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.StreamValue;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.ui.App;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.zeromq.ZMQ;

/**
 *
 */
public class StreamPanel extends DevicePanel {

    final DefaultTableModel model;
    volatile boolean cacheChanged;
    volatile boolean updating;
    volatile AddressableDevice monitoredDevice;
    int updateInterval = 200;

    public StreamPanel() {
        initComponents();
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuPlotChannel = new JMenuItem("Plot channel");
        JMenuItem menuCopyChannelId = new JMenuItem("Copy identifier");
        menuPlotChannel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    plotChannel();
                } catch (Exception ex) {
                }
            }
        });

        menuCopyChannelId.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String channelId = getCurrentChannelId();
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new StringSelection(channelId), (Clipboard clipboard1, Transferable contents) -> {
                    });
                    showMessage("Channel Identifier", "Copied to clipboard: " + channelId);
                } catch (Exception ex) {
                }
            }
        });
        popupMenu.add(menuPlotChannel);
        popupMenu.addSeparator();
        popupMenu.add(menuCopyChannelId);

        model = (DefaultTableModel) table.getModel();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    String channel = getCurrentChannel();
                    if (channel != null) {
                        table.setToolTipText(getCurrentChannelId());

                        if ((e.getClickCount() == 2) && (!e.isPopupTrigger()) && !popupMenu.isVisible()) {
                            plotChannel();
                        }
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
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } catch (Exception ex) {
                }
            }

        });
    }

    public void setUpdateInterval(int value) {
        updateInterval = value;
        Stream device = getDevice();
        if (device != null) {
            setDevice(device);
        }
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    @Override
    public void setReadOnly(boolean value) {
        super.setReadOnly(value);
        ckMonitored.setVisible(!value);

    }

    String getCurrentChannel() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
    }

    String getCurrentChannelId() {
        String channel = getCurrentChannel();
        if ((monitoredDevice != null) && (channel != null)) {
            String address = monitoredDevice.getChannelPrefix();
            if ((address == null) || (address.isBlank())) {
                address = Str.toString(monitoredDevice.getAddress());
            }
            String channelId = address.trim() + " " + channel;
            return channelId;
        }
        return null;
    }

    void plotChannel() {
        String channel = getCurrentChannel();
        Stream device = getDevice();
        if ((device != null) && (channel != null)) {
            Device child = device.getChild(channel);
            showDevicePanel(child);
        }
    }

    @Override
    public Stream getDevice() {
        return (Stream) super.getDevice();
    }

    @Override
    public void setDevice(Device device) {
        super.setDevice(device);
        monitoredDevice = getDevice();
        if (getDevice() != null) {
            onDeviceStateChanged(getDevice().getState(), null);
            if (updateInterval > 0) {
                startTimer(updateInterval, 10);
            }
        } else {
            clear();
        }
        updateTable();
        updateButtons();
    }

    protected void updateSocket() {
        String socket = getDevice().getStreamSocket();
        String address;
        String type;
        if (socket == null) {
            address = getDevice().getAddress();
            if (address == null) {
                address = "";
            }
            type = "";
        } else {
            address = socket;
            type = getDevice().getSocketType() == ZMQ.PULL
                    ? SocketType.PULL.toString()
                    : SocketType.SUB.toString();
        }
        if (!address.equals(textAddress.getText())) {
            textAddress.setText(address);
        }
        if (!type.equals(textType.getText())) {
            textType.setText(type);
        }
    }

    protected void setMonitoredDevice(AddressableDevice device) {
        monitoredDevice = device;
    }

    void updateButtons() {
    }

    void clear() {
        stopTimer();
        textAddress.setText("");
        textTimestamp.setText("");
        textId.setText("");
        textType.setText(null);
        model.setNumRows(0);
    }

    public void updateTable() {
        Stream device = getDevice();
        StreamValue sv = (device == null) ? null : device.take();

        if (sv == null) {
            textTimestamp.setText("");
            textId.setText("");
            model.setNumRows(0);
            return;
        }
        long id = sv.getPulseId();
        textId.setText(Str.toString(id));
        textTimestamp.setText(Str.toString(sv.getTimestamp()));
        textIdOff.setText(String.format("%06X", id % 1000000L));
        textTimestampOff.setText(String.format("%06X", sv.getNanosOffset()));

        int index = 0;
        List<String> keys = sv.getKeys();
        Collections.sort(keys);
        model.setNumRows(keys.size());
        for (String key : keys) {
            Object val = sv.getValue(key);
            int[] shape = sv.getShape(key);
            String type = Str.toString(sv.getType(key));
            if (val != null) {
                if (shape == null) {
                    if (val instanceof String) {
                        shape = new int[]{((String) val).length()};
                    } else if (val.getClass().isArray()) {
                        shape = Arr.getShape(val);
                    }
                }
            }
            String size = Convert.arrayToString(shape, " x ");
            if (index >= model.getRowCount()) {
                model.addRow(new Object[]{"", "", "", ""});
            } else {
                model.setValueAt(key, index, 0);
                model.setValueAt(type, index, 1);
                model.setValueAt(size, index, 2);
                model.setValueAt(Str.toString(val, 10), index, 3);
            }

            index++;
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

        textAddress = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        textType = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        textId = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        textTimestamp = new javax.swing.JTextField();
        ckMonitored = new javax.swing.JCheckBox();
        textIdOff = new javax.swing.JTextField();
        textTimestampOff = new javax.swing.JTextField();

        textAddress.setEditable(false);

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Channel", "Type", "Size", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(table);

        jLabel1.setText("Address:");

        textType.setEditable(false);
        textType.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel2.setText("Type:");

        jLabel3.setText("ID:");

        textId.setEditable(false);
        textId.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel4.setText("Timestamp:");

        textTimestamp.setEditable(false);
        textTimestamp.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        ckMonitored.setText("Monitored");
        ckMonitored.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckMonitoredActionPerformed(evt);
            }
        });

        textIdOff.setEditable(false);
        textIdOff.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        textTimestampOff.setEditable(false);
        textTimestampOff.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textAddress)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textType, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ckMonitored))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textId, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(textIdOff, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textTimestamp, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(textTimestampOff, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(textType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ckMonitored))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(textTimestamp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textIdOff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textTimestampOff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void ckMonitoredActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckMonitoredActionPerformed
        try {
            if (!updating) {
                if (monitoredDevice != null) {
                    monitoredDevice.setMonitored(ckMonitored.isSelected());
                }
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_ckMonitoredActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox ckMonitored;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    private javax.swing.JTextField textAddress;
    private javax.swing.JTextField textId;
    private javax.swing.JTextField textIdOff;
    private javax.swing.JTextField textTimestamp;
    private javax.swing.JTextField textTimestampOff;
    private javax.swing.JTextField textType;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void onDeviceStateChanged(State state, State former) {
        updateButtons();
    }

    @Override
    protected void onDeviceCacheChanged(Object value, Object former, long timestamp, boolean valueChange) {
        if (cacheChanged == false) {
            cacheChanged = true;
            if (updateInterval <= 0) {
                SwingUtilities.invokeLater(() -> {
                    onTimer();
                });
            }
        }
    }

    @Override
    protected void onTimer() {
        try {
            if ((getDevice() != null)) {
                if (cacheChanged) {
                    updateTable();
                } else {
                    updateSocket();
                }
                updating = true;
                ckMonitored.setSelected(monitoredDevice.isMonitored());
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        } finally {
            cacheChanged = false;
            updating = false;
        }
    }

    public static StreamPanel createFrame(String url, SocketType type, Window parent, String title) throws Exception {
        Stream stream = new Stream(url, url, (type == null) ? SocketType.SUB : type);
        return (StreamPanel) createFrame(stream, parent, title);
    }

    public static void main(String[] args) {
        try {
            App.init(args);
            SocketType type = SocketType.SUB;
            //String url = "tcp://localhost:5554";
            String url = args[0];
            if ((args.length > 1) && (args[1].toUpperCase().equals(SocketType.PULL.toString()))) {
                type = SocketType.PULL;
            }
            StreamPanel panel = createFrame(url, type, null, null);
        } catch (Exception ex) {
            SwingUtils.showException(null, ex);
        }
    }
}
