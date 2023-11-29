package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.imaging.Renderer;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.pshell.swing.DevicePoolPanel;
import ch.psi.pshell.swing.HistoryChart;
import static ch.psi.pshell.ui.View.logger;
import ch.psi.utils.State;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JDialog;

/**
 *
 */
public class DevicePanelManager {

    final Map<GenericDevice, JDialog> deviceDialogs;
    final Map<Device, JDialog> historyDialogs;
    final View view;
    final Preferences preferences;

    DevicePanelManager(View view) {
        this.view = view;
        this.deviceDialogs = new HashMap<>();
        this.historyDialogs = new HashMap<>();
        if (view == null) {
            preferences = Preferences.load();
        } else {
            preferences = view.getPreferences();
        }
    }
    
    public MonitoredPanel showPanel(final String name) {
        return showPanel(name, view);
    }

    public MonitoredPanel showPanel(final String name, Window parent) {
        return showPanel(Context.getInstance().getDevicePool().getByName(name), parent);
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
                if (dev instanceof Source) {
                    ((Renderer) ret[0]).setDevice((Source) dev);
                }
                return (MonitoredPanel) ret[0];
            }
        }
        MonitoredPanel panel = dev instanceof Source ? newRenderer((Source) dev, parent) : newPanel((Device) dev, parent);         
        
        if ((panel==null) && (dev instanceof Device)) {
            return showHistory((Device) dev);
        }

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
                Component[] ret = SwingUtils.getComponentsByType(dlg, HistoryChart.class);
                return (HistoryChart) ret[0];

            }
        }
        try {        
            HistoryChart chart = HistoryChart.create(dev);
            JDialog dlg = SwingUtils.showDialog(parent, dev.getName(), null, chart);
            historyDialogs.put(dev, dlg);
            dev.addListener (new DeviceAdapter() {
                @Override
                public void onStateChanged(Device device, State state, State former) {
                    if (state == State.Closing) {
                        if (historyDialogs.containsKey(device)) {
                            for (Component hc : SwingUtils.getComponentsByType(historyDialogs.get(device), HistoryChart.class)) {
                                ((HistoryChart) hc).close();
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
                if ((view!=null) && (parent==view) && (!App.isLocalMode())) {
                    if ((App.getInstance().getState() != State.Closing) || (!preferences.persistRendererWindows)) {
                        view.removePersistedWindow(deviceDialogs.get(source));
                    }
                }
                GenericDevice source = Context.getInstance().getDevicePool().getByName(name);
                if (source != null) {
                    source.removeListener(this);
                }
            }
        };
        renderer.setDevice(source);
        renderer.setAutoScroll(true);
        renderer.setShowStatus(preferences.showImageStatusBar);
        renderer.setBackgroundRendering(preferences.backgroundRendering);
        JDialog dlg = SwingUtils.showDialog(parent, source.getName(), new Dimension(600, 400), renderer);
        dlg.setName(RENDERER_DIALOG_NAME_PREFIX + source.getName());
        deviceDialogs.put(source, dlg);
        if ((view!=null) && (parent==view) && (!App.isLocalMode())) {
            view.addPersistedWindow(dlg);
        }
        if (preferences.persistRendererWindows) {
            Path persistFile = Paths.get(Context.getInstance().getSetup().getContextPath(), "Renderer_" + source.getName() + ".bin");
            renderer.setPersistenceFile(persistFile);
        }
        return renderer;
    }

    DevicePanel newPanel(Device dev, Window parent) {
        try {
            DevicePoolPanel.DefaultPanel defaultPanel = getDefaultPanel(dev);
            if (defaultPanel!=null) {
                DevicePanel ret = (DevicePanel) defaultPanel.getPanelClass().newInstance();
                ret.setDevice(dev);
                dev.addListener (new DeviceAdapter() {
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

    public DevicePoolPanel.DefaultPanel getDefaultPanel(GenericDevice device) {
        if (device!=null){
            for (DevicePoolPanel.DefaultPanel entry : preferences.defaultPanels) {
                try {
                    if (entry.getDeviceClass().isAssignableFrom(device.getClass())) {
                        return entry;
                    }
                } catch (Exception ex) {
                    logger.log(Level.INFO, null, ex);
                }
            }
            for (DevicePoolPanel.DefaultPanel entry : Preferences.getDefaultPanels()) {
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

    void restartRenderer(final String name) {
        JDialog dlg = getPanelDialog(name); 
        if (dlg != null) {
            if (dlg.isDisplayable()) {
                for (Component c : SwingUtils.getComponentsByType(dlg.getContentPane(), Renderer.class)) {
                    if (c instanceof Renderer) {
                        Renderer renderer = (Renderer) c;
                        renderer.setBackgroundRendering(preferences.backgroundRendering);
                        Source source = (Source) Context.getInstance().getDevicePool().getByName(name);
                        if (source != null) {
                            source.addListener(renderer);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    void checkWindowRestart(){
        if (view != null){
            for (String windowName : view.getPersistedWindowNames()) {
                if (windowName.startsWith(RENDERER_DIALOG_NAME_PREFIX)) {
                    try {
                        String source = windowName.substring(RENDERER_DIALOG_NAME_PREFIX.length()).trim();
                        if (isShowingPanel(source)) {
                            restartRenderer(source);
                        } else if (preferences.persistRendererWindows && !App.isLocalMode()) {
                            showPanel(source);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }   
            }
        }
    }

}
