package ch.psi.pshell.ui;

import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.imaging.Renderer;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.utils.Config;
import ch.psi.utils.swing.ConfigDialog;
import ch.psi.utils.swing.PropertiesDialog;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Frame;
import java.awt.Window;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.swing.WindowConstants;

/**
 * An extension to the core Plugin class, which must be loaded by the Workbench. Adds to the base
 * class functionalities such as access to Workbench's MainFrame and Application object and private
 * panels management.
 */
public interface Plugin extends ch.psi.pshell.core.Plugin {

    final HashMap<Plugin, List<Window>> windows = new HashMap<>();

    @Override
    default void onStart() {
    }

    @Override
    default public void onStop() {
        closeWindows();
    }

    default App getApp() {
        return App.getInstance();
    }

    default View getView() {
        return (View) getApp().getMainFrame();
    }

    @Override
    default void abort() throws InterruptedException {
        ch.psi.pshell.core.Plugin.super.abort();
        getApp().abort();
    }

    default Frame getTopLevel() {
        return getView();
    }

    default ConfigDialog showDeviceConfigDialog(GenericDevice device, boolean readOnly) {
        try {
            final ConfigDialog dlg = new ConfigDialog(getTopLevel(), false);
            dlg.setTitle("Device Configuration: " + device.getName());
            dlg.setConfig(device.getConfig());
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setListener((StandardDialog sd, boolean accepted) -> {
                if (sd.getResult()) {
                    try {
                        device.getConfig().save();
                    } catch (IOException ex) {
                        showException(ex);
                    }
                }
            });
            SwingUtils.centerComponent(getTopLevel(), dlg);
            dlg.setVisible(true);         
            dlg.requestFocus();
            return dlg;

        } catch (Exception ex) {
            showException(ex);
        }
        return null;
    }

    default DevicePanel showDevicePanel(String name) {
        GenericDevice c = getDevice(name);
        if (c instanceof Device) {
            return showDevicePanel((Device) c);
        }
        return null;
    }

    default DevicePanel showDevicePanel(Device device) {
        return (DevicePanel) getApp().getDevicePanelManager().showPanel(device, getTopLevel());
    }

    default Renderer showRenderer(String name) {
        GenericDevice c = getDevice(name);
        if (c instanceof Source) {
            return showRenderer((Source) c);
        }
        return null;
    }

    default Renderer showRenderer(Source source) {
        return (Renderer) getApp().getDevicePanelManager().showPanel(source, getTopLevel());
    }
    default void showSettingsEditor(boolean modal) {
        showPropertiesEditor(getContext().getSettingsFile(), modal);
    }
    
    default void showPropertiesEditor(String fileName, boolean modal) {
        try {
            final PropertiesDialog dlg = new PropertiesDialog(getTopLevel(), modal);
            dlg.setTitle(fileName);
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(fileName)) {
                props.load(in);
            } 
            dlg.setProperties(props);
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setListener((StandardDialog sd, boolean accepted) -> {
                if (sd.getResult()) {
                    try (FileOutputStream out = new FileOutputStream(fileName);) {
                        props.store(out, null);
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
        getApp().sendOutput(str);
    }

    default void sendError(String str) {
        getApp().sendError(str);
    }
    
}
