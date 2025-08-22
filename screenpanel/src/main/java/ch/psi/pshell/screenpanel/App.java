
package ch.psi.pshell.screenpanel;

import ch.psi.pshell.devices.Setup;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.swing.SwingUtils;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
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
        Epics.create();
        CamServerViewer viewer = new CamServerViewer();
        SwingUtilities.invokeLater(() -> {
            try {                
                Window window = SwingUtils.showFrame(parent, dialogTitle, (size==null)? new Dimension(800, 600):size, viewer);
                SwingUtils.centerComponent(null, window);
                window.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
                viewer.applyOptions();
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
