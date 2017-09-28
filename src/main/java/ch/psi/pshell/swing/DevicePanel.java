package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.Motor;
import ch.psi.pshell.device.MotorAdapter;
import ch.psi.pshell.device.MotorStatus;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.State;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import ch.psi.pshell.core.ContextListener;

/**
 * Device panels should only read cached values (take) or execute direct device access in private
 * thread.
 */
public class DevicePanel extends MonitoredPanel {

    public final static Color TEXT_DISPLAY_BACKGROUND_COLOR = MainFrame.isDark() ? new Color(69, 73, 74) : new Color(236, 238, 244);
    public final static Color TEXT_EDIT_BACKGROUND_COLOR = MainFrame.isDark() ? new Color(43, 43, 43) : Color.WHITE;
    public final static Color TEXT_READONLY_BACKGROUND_COLOR = MainFrame.isDark() ? new Color(92, 92, 92) : new Color(237, 239, 242);

    public DevicePanel() {
    }

    protected Logger getLogger() {
        Device dev = getDevice();
        if (dev == null) {
            return Logger.getLogger(DevicePanel.class.getName());
        }
        return Logger.getLogger(DevicePanel.class.getName() + "-" + dev.getName());
    }

    Device device;

    boolean showTitle = false;

    public void setShowTitle(boolean value) {
        showTitle = value;
        updateTitle();
    }

    public boolean getShowTitle() {
        return showTitle;
    }

    public void setDevice(final Device device) {
        if (this.device != null) {
            this.device.removeListener(listener);
        }
        this.device = device;
        if (device != null) {
            if (this.isShowing()) {
                device.addListener(listener);
            }
            doInitialCallbacks();
            device.request();
        }

        setEnabled(device != null);
        updateTitle();
    }

    void updateTitle() {
        if (getShowTitle() && (getBorder() instanceof TitledBorder)) {
            SwingUtilities.invokeLater(() -> {
                String title = deviceName;
                if ((title == null) && (device != null)) {
                    title = device.getName();
                }
                if (title == null) {
                    title = "Undefined";
                }
                ((TitledBorder) getBorder()).setTitle(title);
                updateUI();
            });
        }
    }

    void doInitialCallbacks() {
        onDeviceStateChanged(device.getState(), null);
        if (device.take() != null) {
            onDeviceValueChanged(device.take(), null);
        }
        if (device instanceof ReadbackDevice) {
            Object readback = ((ReadbackDevice) device).getReadback().take();
            if (readback != null) {
                onDeviceReadbackChanged(readback);
            }
        }
    }

    public Device getDevice() {
        return device;
    }

    private boolean readOnly;

    public void setReadOnly(boolean value) {
        readOnly = value;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    private String deviceName;

    public void setDeviceName(String value) {
        deviceName = value;
        Context context = Context.getInstance();
        if (context != null) {
            if (contextListener != null) {
                context.removeListener(contextListener);
                contextListener = null;
            }
            if (context.getState().isInitialized()) {
                setDevice(context.getDevicePool().getByName(deviceName, Device.class));
            }
            if ((value != null) && (!value.isEmpty())) {
                contextListener = new ContextAdapter() {
                    @Override
                    public void onContextInitialized(int runCount) {
                        setDevice(context.getDevicePool().getByName(deviceName, Device.class));
                    }
                };
                context.addListener(contextListener);
            }
        }
    }

    ContextListener contextListener;

    @Override
    protected void onDesactive() {
        if (contextListener != null) {
            Context.getInstance().removeListener(contextListener);
            contextListener = null;
        }
    }

    public String getDeviceName() {
        if (deviceName == null) {
            return "";
        }
        return deviceName;
    }

    //Background device update
    //Future backgroundUpdateTask;
    //ExecutorService backgroundUpdateExecutor;
    Thread backgroundUpdateTask;

    /**
     * Triggers a background update (returns if one is ongoing)
     */
    public void startBackgroundUpdate() {

        if (backgroundUpdateTask != null) {
            if (backgroundUpdateTask.isAlive()) {
                return;
            }
        }
        //backgroundUpdateTask = backgroundUpdateExecutor.submit(() -> {
        backgroundUpdateTask = new Thread(() -> {
            try {
                final Object ret = doBackgroundUpdate();
                SwingUtilities.invokeAndWait(() -> {
                    if (isShowing() && getDevice().isInitialized()) {
                        try {
                            backgroundUpdate = true;
                            onBackgroundUpdateFinished(ret);
                        } catch (Exception ex) {
                            getLogger().log(Level.FINE, null, ex);
                        } finally {
                            backgroundUpdate = false;
                        }
                    }
                });
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
            }
        }, "Device background updater: " + getDevice().getName());
        backgroundUpdateTask.setDaemon(true);
        backgroundUpdateTask.start();
    }

    volatile boolean backgroundUpdate = false;

    protected boolean isBackgroundUpdate() {
        return backgroundUpdate;
    }

    /**
     * Implementations implement direct device access in this callback. Runs on private thread.
     */
    protected Object doBackgroundUpdate() throws IOException, InterruptedException {
        return null;
    }

    /**
     * Callback to implementation on event thread with results of doBackgroundUpdate
     */
    protected void onBackgroundUpdateFinished(Object data) {
    }

    //Device callbacks
    final DeviceListener listener = new MotorAdapter() {

        @Override
        public void onStateChanged(final Device dev, final State state, final State former) {
            if (dev == device) {
                SwingUtilities.invokeLater(() -> {
                    onDeviceStateChanged(state, former);
                });
            }
            if (state == State.Closing) {
                setDevice(null);
                setEnabled(false);
            }

            if (!state.isInitialized()) {
                if ((backgroundUpdateTask != null) && (backgroundUpdateTask.isAlive())) {
                    getLogger().log(Level.WARNING, "Stopping devide background update task");
                    backgroundUpdateTask.interrupt();
                    backgroundUpdateTask = null;
                }
            }
        }

        @Override
        public void onCacheChanged(final Device dev, final Object value, final Object former, final long timestamp, final boolean valueChange) {
            if (dev == device) {
                SwingUtilities.invokeLater(() -> {
                    onDeviceCacheChanged(value, former, timestamp, valueChange);
                });
            }
        }

        @Override
        public void onValueChanged(final Device dev, final Object value, final Object former) {
            if (dev == device) {
                SwingUtilities.invokeLater(() -> {
                    onDeviceValueChanged(value, former);
                });
            }
        }

        @Override
        public void onReadbackChanged(final Device dev, final Object value) {
            if (dev == device) {
                SwingUtilities.invokeLater(() -> {
                    onDeviceReadbackChanged(value);
                });
            }
        }

        @Override
        public void onSpeedChanged(final Motor dev, final Double value) {
            if (dev == device) {
                SwingUtilities.invokeLater(() -> {
                    onDeviceSpeedChanged(value);
                });
            }
        }

        @Override
        public void onMotorStatusChanged(final Motor dev, final MotorStatus value) {
            if (dev == device) {
                SwingUtilities.invokeLater(() -> {
                    onDeviceStatusChanged(value);
                });
            }
        }
    };

    protected void onDeviceStateChanged(State state, State former) {
    }

    protected void onDeviceCacheChanged(Object value, Object former, long timestamp, boolean valueChange) {
    }

    protected void onDeviceValueChanged(Object value, Object former) {
    }

    protected void onDeviceReadbackChanged(Object value) {
    }

    protected void onDeviceSpeedChanged(Double value) {
    }

    protected void onDeviceStatusChanged(Object value) {
    }

    @Override
    protected void onShow() {
        if (device != null) {
            device.addListener(listener);
            doInitialCallbacks();
        }
    }

    @Override
    protected void onHide() {
        if (device != null) {
            device.removeListener(listener);
        }
    }

    //A configurable timer
    Timer timer;

    public void startTimer(int interval) {
        startTimer(interval, -1);
    }

    int interval;

    public void startTimer(int interval, int delay) {
        stopTimer();
        this.interval = interval;
        timer = new Timer(interval, (ActionEvent e) -> {
            try {
                if (isShowing()) {
                    onTimer();
                }
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
            }
        });
        if (delay >= 0) {
            timer.setInitialDelay(delay);
        }
        timer.start();
    }

    public void stopTimer() {
        this.interval = 0;
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    public int getTimerInteval() {
        return interval;
    }

    protected void onTimer() {
    }

    protected void showException(Exception ex) {
        SwingUtils.showException(this, ex);
    }

    protected void showMessage(String title, String message) {
        SwingUtils.showMessage(this, title, message);
    }
}
