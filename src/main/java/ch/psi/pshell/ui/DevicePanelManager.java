package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.imaging.Renderer;
import ch.psi.pshell.imaging.Source;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.pshell.swing.DevicePoolPanel;
import static ch.psi.pshell.ui.View.logger;
import ch.psi.utils.State;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 *
 */
public class DevicePanelManager {

    final Map<String, JDialog> deviceDialogs;
    final View view;
    final Preferences preferences;

    DevicePanelManager(View view) {
        this.view = view;
        this.deviceDialogs = new HashMap<>();
        if (view == null) {
            preferences = Preferences.load(Context.getInstance().getSetup().getContextPath());
        } else {
            preferences = view.getPreferences();
        }
    }
    
    public JPanel showPanel(final String name) {
        return showPanel(name, view);
    }

    public JPanel showPanel(final String name, Frame parent) {
        return showPanel(Context.getInstance().getDevicePool().getByName(name), parent);
    }
    public JPanel showPanel(final GenericDevice dev) {
        return showPanel(dev, view);
    }
    
    public JPanel showPanel(final GenericDevice dev, Frame parent) {

        if ((dev == null) || (dev.getName() == null)) {
            return null;
        }
        String name = dev.getName();
        if (deviceDialogs.containsKey(name)) {
            JDialog dlg = deviceDialogs.get(name);
            if (dlg.isDisplayable()) {
                dlg.requestFocus();
                Class type = dev instanceof Source ? Renderer.class : DevicePanel.class;
                Component[] ret = SwingUtils.getComponentsByType(dlg, type);
                if ((ret.length == 0) || !(type.isAssignableFrom(ret[0].getClass()))) {
                    return null;
                }
                if (dev instanceof Source) {
                    ((Renderer) ret[0]).setDevice((Source) dev);
                }
                return (JPanel) ret[0];
            }
        }
        JPanel panel = dev instanceof Source ? newRenderer((Source) dev, parent) : newPanel((Device) dev, parent);
        return panel;
    }

    public static final String RENDERER_DIALOG_NAME_PREFIX = "Renderer ";

    Renderer newRenderer(final Source source, Frame parent) {
        final String name = source.getName();
        final Renderer renderer = new Renderer() {
            @Override
            protected void onHide() {
                super.onHide();
                if ((view!=null) && (parent==view) && (!App.isLocalMode())) {
                    if ((App.getInstance().getState() != State.Closing) || (!preferences.persistRendererWindows)) {
                        view.removePersistedWindow(deviceDialogs.get(source.getName()));
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
        deviceDialogs.put(source.getName(), dlg);
        if ((view!=null) && (parent==view) && (!App.isLocalMode())) {
            view.addPersistedWindow(dlg);
        }
        if (preferences.persistRendererWindows) {
            Path persistFile = Paths.get(Context.getInstance().getSetup().getContextPath(), "Renderer_" + source.getName() + ".bin");
            renderer.setPersistenceFile(persistFile);
        }
        return renderer;
    }

    DevicePanel newPanel(Device dev, Frame parent) {
        for (DevicePoolPanel.DefaultPanel entry : preferences.defaultPanels) {
            try {
                if (entry.getDeviceClass().isAssignableFrom(dev.getClass())) {
                    DevicePanel ret = (DevicePanel) entry.getPanelClass().newInstance();
                    ret.setDevice(dev);
                    dev.addListener(new DeviceAdapter() {
                        @Override
                        public void onStateChanged(Device device, State state, State former) {
                            if (state == State.Closing) {
                                String name = device.getName();
                                if (name != null) {
                                    if (deviceDialogs.containsKey(name)) {
                                        deviceDialogs.get(name).setVisible(false);
                                        deviceDialogs.get(name).dispose();
                                        deviceDialogs.remove(name);
                                    }
                                }
                            }
                        }
                    });

                    JDialog dlg = SwingUtils.showDialog(parent, dev.getName(), null, ret);
                    dlg.setMinimumSize(dlg.getPreferredSize());
                    deviceDialogs.put(dev.getName(), dlg);

                    return ret;
                }
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        }
        return null;
    }

    public boolean hasControlPanel(GenericDevice device) {
        for (DevicePoolPanel.DefaultPanel entry : preferences.defaultPanels) {
            try {
                if (entry.getDeviceClass().isAssignableFrom(device.getClass())) {
                    return true;
                }
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        }
        return false;
    }

    public boolean isShowingPanel(final String name) {
        if (name == null) {
            return false;
        }
        return (deviceDialogs.containsKey(name));
    }
    
    public JDialog getPanelDialog(final String name) {
        if (name == null) {
            return null;
        }
        return (deviceDialogs.get(name));
    }
    

    public boolean isShowingPanel(final GenericDevice device) {
        if (device == null) {
            return false;
        }
        return isShowingPanel(device.getName());
    }

    void restartRenderer(final String name) {
        if (deviceDialogs.containsKey(name)) {
            JDialog dlg = deviceDialogs.get(name);
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
