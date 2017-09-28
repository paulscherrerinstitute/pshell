package ch.psi.pshell.imaging;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public abstract class DeviceSource extends ColormapSource {

    final Device device;

    public Device getDevice() {
        return device;
    }

    protected DeviceSource(String name, Device device) {
        this(name, device, new ColormapSource.ColormapSourceConfig());
    }

    protected DeviceSource(String name, Device device, ColormapSource.ColormapSourceConfig config) {
        super(name, config);
        this.device = device;
        if (device == null) {
            throw new java.lang.IllegalArgumentException("Device is not defined");
        }
        device.addListener(new DeviceAdapter() {
            @Override
            public void onValueChanged(Device device, Object value, Object former) {
                try {
                    if (value == null) {
                        pushImage(null);
                    } else {
                        onDataReceived(value);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(DeviceSource.class.getName()).log(Level.FINE, null, ex);
                    pushError(ex);
                }
            }
        });
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        if (!device.isInitialized()) {
            device.initialize();
        }
        Object current = device.take();
        if (current != null) {
            try {
                onDataReceived(current);
            } catch (Exception ex) {
                Logger.getLogger(DeviceSource.class.getName()).log(Level.INFO, null, ex);
            }
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        device.update();
    }

    boolean hadListeners;

    @Override
    protected void onListenersChanged() {
        super.onListenersChanged();
        boolean hasListeners = getListeners().size() > 0;
        if (hadListeners != hasListeners) {
            setupMonitor();
            hadListeners = hasListeners;
        }
    }

    boolean hasSetMonitor;

    @Override
    protected void doSetMonitored(boolean value) {
        setupMonitor();
    }

    void setupMonitor() {
        boolean hasListeners = getListeners().size() > 0;
        if (hasListeners && isMonitored()) {
            if (!device.isMonitored()) {
                device.setMonitored(true);
                hasSetMonitor = true;
            }
        } else {
            undoDeviceMonitorAction();
        }
    }

    void undoDeviceMonitorAction() {
        if (hasSetMonitor) {
            device.setMonitored(false);
            hasSetMonitor = false;
        }
    }

    @Override
    protected void doClose() throws IOException {
        removeAllListeners();
        undoDeviceMonitorAction();
        super.doClose();
    }

    abstract protected void onDataReceived(Object deviceData) throws Exception;
}
