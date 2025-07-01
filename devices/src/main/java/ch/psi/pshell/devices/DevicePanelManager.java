package ch.psi.pshell.devices;

import ch.psi.pshell.app.App;
import ch.psi.pshell.app.MainFrame;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.bs.StreamChannel;
import ch.psi.pshell.camserver.CamServerService;
import ch.psi.pshell.camserver.CamServerStream;
import ch.psi.pshell.device.Camera;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.DiscretePositioner;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.HistogramGenerator;
import ch.psi.pshell.device.MasterPositioner;
import ch.psi.pshell.device.Motor;
import ch.psi.pshell.device.MotorGroup;
import ch.psi.pshell.device.ProcessVariable;
import ch.psi.pshell.device.ReadonlyRegister;
import ch.psi.pshell.device.Slit;
import ch.psi.pshell.epics.Scaler;
import ch.psi.pshell.epics.Scienta;
import ch.psi.pshell.imaging.Renderer;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.swing.CamServerServicePanel;
import ch.psi.pshell.swing.CamServerStreamPanel;
import ch.psi.pshell.swing.CameraPanel;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.pshell.swing.DeviceValueChart;
import ch.psi.pshell.swing.DiscretePositionerPanel;
import ch.psi.pshell.swing.HistogramGeneratorPanel;
import ch.psi.pshell.swing.HistoryChart;
import ch.psi.pshell.swing.MasterPositionerPanel;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.MotorGroupPanel;
import ch.psi.pshell.swing.MotorPanel;
import ch.psi.pshell.swing.ProcessVariablePanel;
import ch.psi.pshell.swing.ScalerPanel;
import ch.psi.pshell.swing.ScientaPanel;
import ch.psi.pshell.swing.SlitPanel;
import ch.psi.pshell.swing.StreamChannelPanel;
import ch.psi.pshell.swing.StreamPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.State;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;

/**
 *
 */
public class DevicePanelManager {
    
    static DevicePanelManager instance;

    public static DevicePanelManager getInstance() {
        if (instance == null) {
            instance = new DevicePanelManager(App.getMainFrame());
        }
        return instance;
    }    

    final Map<GenericDevice, JDialog> deviceDialogs;
    final Map<Device, JDialog> historyDialogs;
    final MainFrame view;
    final Logger logger = Logger.getLogger(DevicePanelManager.class.getName());

    boolean persistRendererWindows;
    boolean showImageStatusBar=true;    
    boolean backgroundRendering;
    DefaultPanel[] defaultPanels;
            
    

    DevicePanelManager(MainFrame view) {
        this.view = view;
        this.deviceDialogs = new HashMap<>();
        this.historyDialogs = new HashMap<>();        
    }
    
    public void configure( boolean persistRendererWindows, boolean showImageStatusBar, boolean backgroundRendering){
        this.persistRendererWindows=persistRendererWindows;
        this.showImageStatusBar=showImageStatusBar;
        this.backgroundRendering=backgroundRendering;
    }
    
    public void setDefaultPanels(DefaultPanel[] defaultPanels){
        this.defaultPanels = defaultPanels;
    }
    
    public MonitoredPanel showPanel(final String name) {
        return showPanel(name, view);
    }

    public MonitoredPanel showPanel(final String name, Window parent) {
        return showPanel(DevicePool.getInstance().getByName(name), parent);
    }

    public MonitoredPanel showPanel(final GenericDevice dev) {
        return showPanel(dev, view);
    }
               
    public MonitoredPanel showPanel(final GenericDevice dev, Window parent) {

        if ((dev == null) || (dev.getName() == null)) {
            return null;
        }
        if (deviceDialogs.containsKey(dev)) {
            JDialog dlg = deviceDialogs.get(dev);
            if (dlg.isDisplayable()) {
                dlg.requestFocus();
                Class type = dev instanceof Source ? Renderer.class : DevicePanel.class;
                Component[] ret = SwingUtils.getComponentsByType(dlg, type);
                if ((ret.length == 0) || !(type.isAssignableFrom(ret[0].getClass()))) {
                    ret = SwingUtils.getComponentsByType(dlg, HistoryChart.class);
                     if (ret.length == 0){
                        return null;
                     }
                }
                if (dev instanceof Source source) {
                    ((Renderer) ret[0]).setDevice(source);
                }
                return (MonitoredPanel) ret[0];
            }
        }
        MonitoredPanel panel = dev instanceof Source ? newRenderer((Source) dev, parent) : newPanel((Device) dev, parent);         
        
        if ((panel==null) && (dev instanceof Device device)) {
            panel = showHistory(device);
        }
        panel.requestFocus();
        return panel;
    }
    
    public HistoryChart showHistory(final Device dev) {
        return showHistory(dev, view);
    }    
    
    public HistoryChart showHistory(Device dev, Window parent)  {
        if (historyDialogs.containsKey(dev)) {
            JDialog dlg = historyDialogs.get(dev);
            if (dlg.isDisplayable()) {
                dlg.requestFocus();
                HistoryChart[] ret = SwingUtils.getComponentsByType(dlg, HistoryChart.class);
                return ret[0];

            }
        }
        try {        
            HistoryChart chart = HistoryChart.create(dev);
            JDialog dlg = SwingUtils.showDialog(parent, dev.getName(), null, chart);
            historyDialogs.put(dev, dlg);
            dev.addListener (new DeviceListener() {
                @Override
                public void onStateChanged(Device device, State state, State former) {
                    if (state == State.Closing) {
                        if (historyDialogs.containsKey(device)) {
                            for (HistoryChart hc : SwingUtils.getComponentsByType(historyDialogs.get(device), HistoryChart.class)) {
                                hc.close();
                            }
                            historyDialogs.get(device).setVisible(false);
                        }
                    }
                }
            });
            
            return chart;
        } catch (Exception ex) {
            return null;
        }
    }    

    public static final String RENDERER_DIALOG_NAME_PREFIX = "Renderer ";

    Renderer newRenderer(final Source source, Window parent) {
        final String name = source.getName();
        final Renderer renderer = new Renderer() {
            @Override
            protected void onHide() {
                super.onHide();
                if ((view!=null) && (parent==view)) {
                    if ((App.getInstance().getState() != State.Closing) || (!persistRendererWindows)) {
                        view.removePersistedWindow(deviceDialogs.get(source));
                    }
                }
                GenericDevice source = DevicePool.getInstance().getByName(name);
                if (source != null) {
                    source.removeListener(this);
                }
            }
        };
        renderer.setDevice(source);
        renderer.setAutoScroll(true);
        renderer.setShowStatus(showImageStatusBar);
        renderer.setBackgroundRendering(backgroundRendering);
        JDialog dlg = SwingUtils.showDialog(parent, source.getName(), new Dimension(600, 400), renderer);
        dlg.setName(RENDERER_DIALOG_NAME_PREFIX + source.getName());
        deviceDialogs.put(source, dlg);
        if (persistRendererWindows) {
            if ((view!=null) && (parent==view)){
                view.addPersistedWindow(dlg);
            }            
            Path persistFile = Paths.get(Setup.getContextPath(), "Renderer_" + source.getName() + ".bin");
            renderer.setPersistenceFile(persistFile);
        }
        return renderer;
    }

    DevicePanel newPanel(Device dev, Window parent) {
        try {
            DefaultPanel defaultPanel = getDefaultPanel(dev);
            if (defaultPanel!=null) {
                DevicePanel ret = (DevicePanel) defaultPanel.getPanelClass().newInstance();
                ret.setDevice(dev);
                dev.addListener (new DeviceListener() {
                    @Override
                    public void onStateChanged(Device device, State state, State former) {
                        if (state == State.Closing) {
                            if (device != null) {
                                if (deviceDialogs.containsKey(device)) {
                                    deviceDialogs.get(device).setVisible(false);
                                    deviceDialogs.get(device).dispose();
                                    deviceDialogs.remove(device);
                                }
                            }
                        }
                    }
                });
                JDialog dlg = SwingUtils.showDialog(parent, dev.getName(), null, ret);
                dlg.setMinimumSize(dlg.getPreferredSize());
                deviceDialogs.put(dev, dlg);
                return ret;
            }
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
        return null;
    }

    public boolean hasControlPanel(GenericDevice device) {
        return getDefaultPanel(device) != null;
    } 

    public DefaultPanel getDefaultPanel(GenericDevice device) {
        if (device!=null){
            if (defaultPanels==null) {
                defaultPanels = getDefaultPanels();
            }
            for (DefaultPanel entry : defaultPanels) { 
                try {
                    if (entry.getDeviceClass().isAssignableFrom(device.getClass())) {
                        return entry;
                    }
                } catch (Exception ex) {
                    logger.log(Level.INFO, null, ex);
                }
            }
        }
        return null;
    }
    
    
    public JDialog getPanelDialog(GenericDevice device) {
        return deviceDialogs.get(device);
    }
    
    public JDialog getPanelDialog(final String name) {
        if (name == null) {
            return null;
        }
        for (GenericDevice dev : deviceDialogs.keySet()){
            if (name.equals(dev.getName())){
                return (deviceDialogs.get(dev));
            }
        }
        return null;
    }
    
    
    public boolean hidePanel(final GenericDevice device) {
        return hidePanel(getPanelDialog(device));
    }  

    public boolean hidePanel(final String device) {
        return hidePanel(getPanelDialog(device));
    }
    
    boolean hidePanel( JDialog dlg) {
        if (dlg==null){
            return false;
        }
        dlg.setVisible(false);
        dlg.dispose();
        return true;
    }    

    public boolean isShowingPanel(final String name) {        
        return getPanelDialog(name)!=null;
    }
            
    public boolean isShowingPanel(final GenericDevice device) {
        return getPanelDialog(device)!=null;
    }

    public void restartRenderer(final String name) {
        JDialog dlg = getPanelDialog(name); 
        if (dlg != null) {
            if (dlg.isDisplayable()) {
                for (Renderer renderer : SwingUtils.getComponentsByType(dlg.getContentPane(), Renderer.class)) {
                    renderer.setBackgroundRendering(backgroundRendering);
                    Source source = (Source) DevicePool.getInstance().getByName(name);
                    if (source != null) {
                        source.addListener(renderer);
                    }
                    break;
                }
            }
        }
    }
    
    public void checkWindowRestart(){
        if (view != null){
            for (String windowName : view.getPersistedWindowNames()) {
                if (windowName.startsWith(RENDERER_DIALOG_NAME_PREFIX)) {
                    try {
                        String source = windowName.substring(RENDERER_DIALOG_NAME_PREFIX.length()).trim();
                        if (isShowingPanel(source)) {
                            restartRenderer(source);
                        } else if (persistRendererWindows) {
                            showPanel(source);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }   
            }
        }
    }
        
    public static DefaultPanel[] getDefaultPanels() {
        return new DefaultPanel[]{
            new DefaultPanel(Scaler.class.getName(), ScalerPanel.class.getName()),
            new DefaultPanel(Scienta.class.getName(), ScientaPanel.class.getName()),
            new DefaultPanel(Motor.class.getName(), MotorPanel.class.getName()),
            new DefaultPanel(MasterPositioner.class.getName(), MasterPositionerPanel.class.getName()),
            new DefaultPanel(ProcessVariable.class.getName(), ProcessVariablePanel.class.getName()),
            new DefaultPanel(MotorGroup.class.getName(), MotorGroupPanel.class.getName()),
            new DefaultPanel(DiscretePositioner.class.getName(), DiscretePositionerPanel.class.getName()),
            new DefaultPanel(Camera.class.getName(), CameraPanel.class.getName()),
            new DefaultPanel(Slit.class.getName(), SlitPanel.class.getName()),
            new DefaultPanel(HistogramGenerator.class.getName(), HistogramGeneratorPanel.class.getName()),
            new DefaultPanel(Stream.class.getName(), StreamPanel.class.getName()),               
            new DefaultPanel(StreamChannel.class.getName(), StreamChannelPanel.class.getName()),   
            new DefaultPanel(CamServerStream.class.getName(), CamServerStreamPanel.class.getName()),   
            new DefaultPanel(CamServerService.class.getName(), CamServerServicePanel.class.getName()),               
            new DefaultPanel(ReadonlyRegister.class.getName(), DeviceValueChart.class.getName())
        };
    }    
}
