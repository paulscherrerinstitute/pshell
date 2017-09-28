package ch.psi.pshell.swing;

import ch.psi.pshell.plot.TimePlotBase;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.pshell.plot.TimePlotSeries;
import ch.psi.utils.NamedThreadFactory;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 *
 */
public class HistoryChart extends JPanel implements AutoCloseable {

    TimePlotBase chart;
    int interval;
    ScheduledExecutorService scheduler;
    final JCheckBoxMenuItem menuAsyncUpdates;

    public HistoryChart() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        super();
        chart = (TimePlotBase) Class.forName(getTimePlotImpl()).newInstance();
        chart.setQuality(PlotPanel.getQuality());
        chart.setPreferredSize(new java.awt.Dimension(680, 420));
        setLayout(new java.awt.BorderLayout());
        add(chart);
        setInterval(1000);
        chart.setDurationMillis(60000);
        chart.setLegendVisible(false);
        JMenuItem menuInterval = new JMenuItem("Update Interval...");
        menuInterval.addActionListener((ActionEvent e) -> {
            try {
                String str = SwingUtils.getString(HistoryChart.this, "Enter update interval in milliseconds (0 to disable):", String.valueOf(HistoryChart.this.interval));
                if (str != null) {
                    setInterval(Integer.valueOf(str));
                }
            } catch (Exception ex) {
                SwingUtils.showException(HistoryChart.this, ex);
            }
        });
        menuAsyncUpdates = new JCheckBoxMenuItem("Asynchronous Updates");
        menuAsyncUpdates.addActionListener((ActionEvent e) -> {
            try {
                setAsyncUpdates(menuAsyncUpdates.isSelected());
            } catch (Exception ex) {
                SwingUtils.showException(HistoryChart.this, ex);
            }
        });

        chart.addPopupMenuItem(menuInterval);
        chart.addPopupMenuItem(menuAsyncUpdates);
    }

    static Class DEFAULT_PLOT_IMPL_TIME = ch.psi.pshell.plot.TimePlotJFree.class;

    public static String getTimePlotImpl() {
        String impl = System.getProperty(PlotPanel.PROPERTY_PLOT_IMPL_TIME);
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? DEFAULT_PLOT_IMPL_TIME.getName() : impl;
    }

    public void setInterval(int value) {
        if (interval != value) {
            interval = value;
            if (!graphs.isEmpty()) {
                if (interval > 0) {
                    startTimer();
                } else {
                    stopTimer();
                }
            }
        }
    }

    boolean asyncUpdates;

    public void setAsyncUpdates(boolean value) {
        if (asyncUpdates != value) {
            asyncUpdates = value;
            for (Device device : getDevices()) {
                if (asyncUpdates) {
                    device.addListener(deviceListener);
                } else {
                    device.removeListener(deviceListener);
                }
            }
            menuAsyncUpdates.setSelected(asyncUpdates);
        }
    }

    public boolean getAsyncUpdates() {
        return asyncUpdates;
    }

    final HashMap<Device, TimePlotSeries> graphs = new LinkedHashMap<>();

    public Device[] getDevices() {
        return graphs.keySet().toArray(new Device[0]);
    }

    public boolean hasDevice(Device device) {
        return (device != null) && (graphs.keySet().contains(device));
    }

    public void removeDevice(Device device) {
        if (hasDevice(device)) {
            chart.removeSeries(graphs.get(device));
            graphs.remove(device);
            if (graphs.isEmpty()) {
                stopTimer();
            }
            device.removeListener(deviceListener);
        }
    }

    static Color[] COLORS = new Color[]{Color.BLUE, Color.GREEN, Color.RED, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.BLACK};
    int colorIndex = 0;

    public void addDevice(String name, Device device) {
        Color color = COLORS[colorIndex];
        colorIndex = (colorIndex + 1) % COLORS.length;
        if (device != null) {
            if (name == null) {
                name = device.getName();
            }
            TimePlotSeries graph = new TimePlotSeries(name, color);
            chart.addSeries(graph);
            graphs.put(device, graph);
            if (graphs.size() == 1) {
                startTimer();
            }
            if (getAsyncUpdates()) {
                device.addListener(deviceListener);
            }
        }
    }

    DeviceListener deviceListener = new DeviceAdapter() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            add(device, value, null);
        }
    };

    void add(Device device, Object value, Long timestamp) {
        if (timestamp == null) {
            TimestampedValue tValue = device.takeTimestamped();
            if (tValue != null) {
                //To Make sure  value/timestamp pair is correct
                value = tValue.getValue();
                timestamp = tValue.getTimestamp();
            }
        }
        if ((value == null) || (value instanceof Number)) {
            Double d = (value == null) ? Double.NaN : ((Number) value).doubleValue();
            Device[] devices = getDevices();
            for (int i = 0; i < devices.length; i++) {
                if (devices[i] == device) {
                    chart.add(i, (timestamp == null) ? System.currentTimeMillis() : timestamp, d);
                }
            }
        }
    }

    void startTimer() {
        stopTimer();
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("History chart scheduler"));
        //Very fast read(only cache) so don't need Threading.scheduleAtFixedRateNotRetriggerable
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                Device[] devices = getDevices();
                for (int i = 0; i < devices.length; i++) {
                    add(devices[i], devices[i].take(), (devices[i].getAge() > interval) ? System.currentTimeMillis() : null);
                }
            } catch (Exception ex) {
                chart.addTerminators();
            }
        }, 10, interval, TimeUnit.MILLISECONDS);
    }

    void stopTimer() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    boolean started() {
        return (scheduler != null);
    }

    @Override
    public void close() {
        for (Device device : getDevices()) {
            device.removeListener(deviceListener);
        }

        stopTimer();
        chart.close();
    }

    public static HistoryChart create(Device dev) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        HistoryChart chart = new HistoryChart();
        //((HistoryChart)component).setAsyncUpdates(asyncUpdate);
        chart.addDevice(dev.getName(), dev);
        if (dev instanceof ReadbackDevice) {
            Device readback = ((ReadbackDevice) dev).getReadback();
            if (readback != dev) {
                chart.addDevice(dev.getName() + " readback", readback);
            }
        }
        for (Device component : dev.getComponents()) {
            chart.addDevice(component.getName(), component);
        }
        return chart;
    }
}
