package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.plot.PlotPanel;
import ch.psi.pshell.plot.TimePlotBase;
import ch.psi.pshell.plot.TimePlotSeries;
import ch.psi.pshell.utils.NamedThreadFactory;
import ch.psi.pshell.utils.TimestampedValue;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

/**
 *
 */
public class HistoryChart extends MonitoredPanel implements AutoCloseable {

    TimePlotBase chart;
    int interval;
    ScheduledExecutorService scheduler;
    final JCheckBoxMenuItem menuAsyncUpdates;
    static boolean defaultAsync;

    public static void setDefaultAsync(boolean value) {
        defaultAsync = value;
    }

    public static boolean setDefaultAsync() {
        return defaultAsync;
    }

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
                String str = getString("Enter update interval in milliseconds (0 to disable):", String.valueOf(HistoryChart.this.interval));
                if (str != null) {
                    setInterval(Integer.valueOf(str));
                }
            } catch (Exception ex) {
                showException(ex);
            }
        });
        menuAsyncUpdates = new JCheckBoxMenuItem("Asynchronous Updates");
        menuAsyncUpdates.addActionListener((ActionEvent e) -> {
            try {
                setAsyncUpdates(menuAsyncUpdates.isSelected());
            } catch (Exception ex) {
                showException(ex);
            }
        });

        chart.addPopupMenuItem(menuInterval);
        chart.addPopupMenuItem(menuAsyncUpdates);
    }

    public TimePlotBase getPlot() {
        return chart;
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

    public void removeAllDevices() {
        for (Device d : getDevices()) {
            removeDevice(d);
        }
    }

    public void addDevice(String name, Device device) {
        if (device != null) {
            if (name == null) {
                name = device.getName();
            }
            TimePlotSeries graph = new TimePlotSeries(name);
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

    DeviceListener deviceListener = new DeviceListener() {
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
        Double doubleValue;
        if (value == null) {
            doubleValue = Double.NaN;
        } else if (value instanceof Number number) {
            doubleValue = number.doubleValue();
        } else if (value instanceof Boolean) {
            doubleValue = Boolean.TRUE.equals(value) ? 1.0 : 0;
        } else {
            return;
        }
        Device[] devices = getDevices();
        for (int i = 0; i < devices.length; i++) {
            if (devices[i] == device) {
                chart.add(i, (timestamp == null) ? System.currentTimeMillis() : timestamp, doubleValue);
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
        chart.setAsyncUpdates(defaultAsync);
        chart.addDevice(dev.getName(), dev);
        if (dev instanceof ReadbackDevice readbackDevice) {
            Device readback = readbackDevice.getReadback();
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
