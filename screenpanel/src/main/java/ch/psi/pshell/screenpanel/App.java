
package ch.psi.pshell.screenpanel;

import ch.psi.pshell.device.GenericDeviceBase;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.swing.SwingUtils;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 */
public class App extends ch.psi.pshell.devices.App{    
            
    public static CamServerViewer create() {
        Dimension size = Setup.isFullScreen() ? SwingUtils.getDefaultScreenUsableArea().getSize() : Setup.getSize();
        return create((Window)null, null, size);
    }

    public static CamServerViewer create(Window parent, String title, Dimension size) {
        String dialogTitle = (title==null) ? Optional.ofNullable(Setup.getTitle()).orElse("ScreenPanel") : title;
        Epics.create(true);
        CamServerViewer viewer = new CamServerViewer();
        SwingUtilities.invokeLater(() -> {
            try {                
                Window window = SwingUtils.showFrame(parent, dialogTitle, (size==null)? new Dimension(800, 600):size, viewer);
                SwingUtils.centerComponent(null, window);
                window.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
                Integer bufferSize = Options.BUFFER_SIZE.getInt(null);
                if (bufferSize!=null) {
                    try {
                        viewer.setBufferLength(bufferSize);
                    } catch (Exception ex) {
                        Logger.getLogger(CamServerViewer.class.getName()).log(Level.WARNING, null, ex);
                    }     
                }
                viewer.setTypeList(Options.TYPE.hasValue() ? List.of(Options.TYPE.getString(null).split(",")) : null);
                viewer.setStreamList(Options.STREAM_LIST.hasValue() ? Arrays.asList(Options.STREAM_LIST.getString(null).split("\\|")) : null);
                viewer.setConsoleEnabled(Options.CONSOLE.getBool(false));
                viewer.setSidePanelVisible(Options.SIDEBAR.getBool(false));       
                viewer.setCameraServerUrl(Setup.getCameraServer());
                viewer.setPipelineServerUrl(Setup.getPipelineServer());
                viewer.setShared(Options.SHARED.getBool(false));
                if (Setup.getContextPath()!=null) {
                    viewer.setPersistenceFile(Paths.get(Setup.getContextPath(), "CamServer_Viewer.bin"));
                    viewer.getRenderer().clear();
                }                
                if (Options.STREAM.hasValue()) {
                    viewer.setStartupStream(Options.STREAM.getString(null));
                }
                if (Options.CAM_NAME.hasValue()) {
                    viewer.setStartupStream(Options.CAM_NAME.getString(null));
                }
                if (Options.PERSIST.defined()) {
                    if (!"image".equals(Options.PERSIST.getString(null))){
                        if (Options.PERSIST.getBool(false)){
                            viewer.setPersistCameraState(true);
                        } else{
                            GenericDeviceBase.setPersistenceEnabled(false);
                        }
                    }
                }
                viewer.initialize(App.getArgumentValue(Options.SP_MODE.getString(null)));                   
                       
            } catch (Exception ex) {
                Logger.getLogger(CamServerViewer.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return viewer;
    }
    
    /**
     */
    public static void main(String args[]) {
        Options.add();
        App.init(args);        
        create();
    }
    
}
