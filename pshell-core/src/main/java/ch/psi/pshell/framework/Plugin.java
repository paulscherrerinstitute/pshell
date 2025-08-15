package ch.psi.pshell.framework;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.imaging.DeviceRenderer;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.swing.ConfigDialog;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Config;
import java.awt.Frame;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * An extension to the core Plugin class, which must be loaded by the Workbench. Adds to the base
 * class functionalities such as access to Workbench's MainFrame and Application object and private
 * panels management.
 */
public interface Plugin extends ch.psi.pshell.sequencer.Plugin {

    final HashMap<Plugin, List<Window>> windows = new HashMap<>();

    @Override
    default void onStart() {
    }

    @Override
    default public void onStop() {
        closeWindows();
    }

    default App getApp() {
        return Context.getApp();
    }

    default MainFrame getView() {
        return Context.getView();
    }

    default void abort() throws InterruptedException {
        Context.abort();
    }

    default Frame getTopLevel() {
        return getView();
    }

    default ConfigDialog showDeviceConfigDialog(GenericDevice device, boolean readOnly) {
        ConfigDialog dlg = DevicePanel.showConfigEditor(getTopLevel(), device, false, readOnly);
        if (dlg !=null){
            dlg.setTitle("Device Configuration: " + device.getName());
        }
        return dlg;
    }

    default MonitoredPanel showDevicePanel(String name) {
        GenericDevice c = getDevice(name);
        if (c instanceof Device device) {
            return showDevicePanel(device);
        }
        return null;
    }

    default MonitoredPanel showDevicePanel(Device device) {
        return (MonitoredPanel) App.getDevicePanelManager().showPanel(device, getTopLevel());
    }

    default boolean hideDevicePanel(String device) {
        return App.getDevicePanelManager().hidePanel(device);
    }    
    
    default boolean hideDevicePanel(Device device) {
        return App.getDevicePanelManager().hidePanel(device);
    }        
    
    default DeviceRenderer showRenderer(String name) {
        GenericDevice c = getDevice(name);
        if (c instanceof Source source) {
            return showRenderer(source);
        }
        return null;
    }

    default DeviceRenderer showRenderer(Source source) {
        return (DeviceRenderer) App.getDevicePanelManager().showPanel(source, getTopLevel());
    }

    default boolean hideRenderer(String source) {
        return App.getDevicePanelManager().hidePanel(source);
    }    
    
    default boolean hideRenderer(Source source) {
        return App.getDevicePanelManager().hidePanel(source);
    }    
    
    
    default void showSettingsEditor(boolean modal) {
        showPropertiesEditor(Context.getSettingsFile(), modal);
    }
    
    default void showPropertiesEditor(String fileName, boolean modal) {
        MainFrame.showPropertiesEditor(null, getTopLevel(), fileName, modal, false);
    }
    
    
    default void showConfigEditor(Config cfg, boolean modal) {
        try {
            final ConfigDialog dlg = new ConfigDialog(getTopLevel(), modal);
            dlg.setTitle(cfg.getFileName());
            dlg.setConfig(cfg);
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setListener((StandardDialog sd, boolean accepted) -> {
                if (sd.getResult()) {
                    try {
                        cfg.save();
                    } catch (IOException ex) {
                        SwingUtils.showException(dlg, ex);
                    }
                }
            });
            dlg.setLocationRelativeTo(getTopLevel());
            dlg.setVisible(true);
            dlg.requestFocus();

        } catch (Exception ex) {
            SwingUtils.showException(getTopLevel(), ex);
        }   
    }    

    /**
     * Shows a Frame or Dialog having the Mainframe as parent. Window is close when the plugin is
     * stoped.
     */
    default void showWindow(Window window) {
        if (getView() != null) {
            getView().showChildWindow(window);
        } else{
            SwingUtils.centerComponent(getTopLevel(), window);
            SwingUtilities.updateComponentTreeUI(window);
            window.setVisible(true);
            if ((window.isDisplayable()) && (window.isShowing())) {
                window.requestFocus();
            }            
        }
        synchronized (windows) {
            List<Window> list = windows.get(this);
            if (list == null) {
                list = new ArrayList<>();
                windows.put(this, list);
            }
            for (Window w : list.toArray(new Window[0])) {
                if (!w.isShowing()) {
                    list.remove(w);
                }
            }
            list.add(window);
        }
    }

    default void closeWindows() {
        for (Window w : getWindows()) {
            w.setVisible(false);
        }
        windows.clear();
    }

    default Window[] getWindows() {
        synchronized (windows) {
            List<Window> ret = windows.get(this);
            if (ret == null) {
                return new Window[0];
            }
            for (Window w : ret.toArray(new Window[0])) {
                if (!w.isShowing()) {
                    ret.remove(w);
                }
            }            
            return ret.toArray(new Window[0]);
        }
    }

    default void showException(Exception ex) {
        SwingUtils.showException(getTopLevel(), ex);
    }
    
    default void showMessage(String title, String message) {
        SwingUtils.showMessage(getTopLevel(), title, message);
    }    

    default void sendOutput(String str) {
        Context.getApp().sendOutput(str);
    }

    default void sendError(String str) {
        Context.getApp().sendError(str);
    }
    
}
