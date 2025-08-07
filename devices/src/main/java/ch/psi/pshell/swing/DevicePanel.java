package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.Motor;
import ch.psi.pshell.device.MotorListener;
import ch.psi.pshell.device.MotorStatus;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.ReadonlyRegister;
import ch.psi.pshell.device.Startable;
import ch.psi.pshell.devices.App;
import ch.psi.pshell.devices.DevicePanelManager;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.devices.DevicePoolListener;
import ch.psi.pshell.devices.InlineDevice;
import ch.psi.pshell.utils.State;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

/**
 * Device panels should only read cached values (take) or execute direct device access in private
 * thread.
 */
public class DevicePanel extends MonitoredPanel {

    public DevicePanel() {
    }
   
    public Color getTextDisplayBackgroundColor(){
        return ValueSelection.getTextDisplayBackgroundColor();
    }
        
    public Color getTextEditBackgroundColor(){
        return ValueSelection.getTextDisplayBackgroundColor();
    }

    public Color getTextReadonlyBackgroundColor(){
        return ValueSelection.getTextDisplayBackgroundColor();
    }

    protected Logger getLogger() {
        Device dev = getDevice();
        if (dev == null) {
            return Logger.getLogger(DevicePanel.class.getName());
        }
        return Logger.getLogger(DevicePanel.class.getName() + "-" + dev.getName());
    }
    
    @Override
    protected void onLafChange() {  
        checkBackColor();
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
        if (getShowTitle() && (getBorder() instanceof TitledBorder titledBorder)) {
            SwingUtilities.invokeLater(() -> {
                String title = deviceName;
                if ((title == null) && (device != null)) {
                    title = device.getName();
                }
                if (title == null) {
                    title = "Undefined";
                }
                titledBorder.setTitle(title);
                updateUI();
            });
        }
    }
    
    protected void checkBackColor() {        
    }

    void doInitialCallbacks() {
        onDeviceStateChanged(device.getState(), null);
        if (device.take() != null) {
            onDeviceValueChanged(device.take(), null);
        }
        if (device instanceof ReadbackDevice readbackDevice) {
            Object readback = readbackDevice.getReadback().take();
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
        if (devicePoolListener != null) {
            DevicePool.removeStaticListener(devicePoolListener);
            devicePoolListener = null;
        }       
        if ((deviceName != null) && (!deviceName.isEmpty())) {
            devicePoolListener = new DevicePoolListener() {
                @Override
                public void onInitialized() {                    
                    setDevice(DevicePool.getInstance().getByName(deviceName, Device.class));
                }
                
                @Override
                public  void onClosing() {
                    setDevice(null);
                }
                
                @Override
                public void onDeviceAdded(GenericDevice dev) {
                    if (dev instanceof Device device){
                        if (deviceName.equals(device.getName())){
                            if (DevicePool.getInstance().isInitialized()){
                                onInitialized();
                            }
                        }                            
                    }
                }

                @Override
                public  void onDeviceRemoved(GenericDevice dev) {
                 if (dev instanceof Device device){
                        if (deviceName.equals(device.getName())){
                            onClosing();
                        }                            
                    }                    
                }                
            };
            DevicePool.addStaticListener(devicePoolListener);
            if (DevicePool.hasInstance()){
                devicePoolListener.onInitialized();           
            }
        }
    }


    DevicePoolListener devicePoolListener;

    @Override
    protected void onDesactive() {
        stopTimer();
        if (devicePoolListener != null) {
            DevicePool.getInstance().removeListener(devicePoolListener);
            devicePoolListener = null;
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
    final DeviceListener listener = new MotorListener() {

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
    
    public static ConfigDialog showConfigEditor(Component parent, GenericDevice device, boolean modal, boolean readOnly) {
        if (device==null){
            SwingUtils.showMessage(parent, "Error", "Device not set");
            return null;
        }
        return ConfigDialog.showConfigEditor(parent , device.getConfig(), modal, readOnly);
    }  
    
    public ConfigDialog showConfigEditor(boolean modal, boolean readOnly) {
        return showConfigEditor(this, getDevice(), modal, readOnly);
    }        
    

    public static MonitoredPanel showDevicePanel(Component parent, GenericDevice device) {
        Window window = (parent instanceof Window w) ? w : SwingUtils.getWindow(parent);
        return DevicePanelManager.getInstance().showPanel(device, window);
    }

    public MonitoredPanel showDevicePanel(GenericDevice device) {
        return showDevicePanel(getFrame() == null ? this : getFrame(), device);
    }
    
    static public boolean hideDevicePanel(GenericDevice device) {
        return DevicePanelManager.getInstance().hidePanel(device);
    }

    protected String getDeviceTooltip(){
        if (device==null){
            return null;
        }
        String tooltip = device.getName();
        String desc = device.getDescription();
        if ((desc!=null) && (!desc.isBlank())){
            tooltip  = tooltip + " (" + desc.trim() + ")";
        }
        return tooltip;
    }
    
    
    static int DEFAULT_PANEL_PRECISION = 6;
    
    public static void setDefaultPanelPrecision(int value){
        DEFAULT_PANEL_PRECISION = Math.max(value, 0);
    }
    
    protected int getDisplayDecimals(int panelDecimals){
        int deviceDecimals = (getDevice() instanceof ReadonlyRegister reg) ? reg.getPrecision() : Device.UNDEFINED_PRECISION;
        if (panelDecimals<0){
            panelDecimals = deviceDecimals;
        }
        if (panelDecimals<0){
            panelDecimals = DEFAULT_PANEL_PRECISION;
        }
        int decimals = deviceDecimals < 0 ? panelDecimals : Math.min(panelDecimals, deviceDecimals);
        return Math.max(decimals, 0);        
    }
    
    
    public static DevicePanel createFrame(Device device) throws Exception {
        return createFrame(device, null);
    }

    public static DevicePanel createFrame(Device device, Window parent) throws Exception {
        return createFrame(device, parent, null);
    }

    public static DevicePanel createFrame(Device device, Window parent, String title) throws Exception {
        return createFrame(device, parent, title, null, null);
    }
    
    public static DevicePanel createFrame(Device device, Window parent, String title, Dimension size) throws Exception {
        return createFrame(device, parent, title, null, size);
    }
    
    public static DevicePanel createFrame(Device device, Window parent, String title, DevicePanel panel) throws Exception {
         return createFrame(device, parent, title, panel, null);
    }
    
    public static DevicePanel createFrame(Device device, Window parent, String title, DevicePanel panel, Dimension size) throws Exception {
        DevicePanel devicePanel = (panel==null) ? 
                (DevicePanel) DevicePanelManager.getInstance().getDefaultPanel(device).getPanelClass().getDeclaredConstructor().newInstance() :
                panel;

        SwingUtilities.invokeLater(() -> {
            try {                                
                device.initialize();
                if (device instanceof Startable startable){
                    if (!startable.isStarted()){
                        startable.start();
                    }
                }                
                devicePanel.setDevice(device);                
                Dimension panelSize = (size==null) ? devicePanel.getPreferredSize() : size;               
                JFrame window = SwingUtils.showFrame(parent,((title==null) ? device.getName() : title),  panelSize, devicePanel);
                window.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
                SwingUtils.centerComponent(parent, window);        
                window.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);                
            } catch (Exception ex) {
                Logger.getLogger(DevicePanel.class.getName()).log(Level.SEVERE, null, ex);
                SwingUtils.showException(parent, ex);
            }
        });
        return devicePanel;        
    }    
    

    public static void main(String[] args) {
        try {
            App.init(args);
            String url = args[0];
            //url = "ca://TESTIOC:TESTSINUS:SinCalc?monitored=true";            
            Device device = InlineDevice.create(url, null);
            device.initialize();
            createFrame(device);
        } catch (Exception ex) {
            SwingUtils.showException(null, ex);
        }
    }    
}
