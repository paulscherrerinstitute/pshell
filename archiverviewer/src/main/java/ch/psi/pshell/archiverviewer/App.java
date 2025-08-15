
package ch.psi.pshell.archiverviewer;

import ch.psi.pshell.devices.Setup;
import ch.psi.pshell.swing.SwingUtils;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;


/**
 *
 */
public class App extends ch.psi.pshell.devices.App{
    
        
    public static void create() {
        Dimension size = Setup.isFullScreen() ? SwingUtils.getDefaultScreenUsableArea().getSize() : Setup.getSize();        
        create(Setup.getArchiver(), Setup.getBackend(), false, Setup.getTitle(), null, size );
    }
    
    public static void create(String url, String backend, boolean modal, String title, File defaultFolder, Dimension size) {
        java.awt.EventQueue.invokeLater(() -> {
            ArchiverPanel dialog = new ArchiverPanel(null, url, backend, title, modal,  (defaultFolder==null) ? ArchiverPanel.getDaqbufFolderArg() : defaultFolder);
            
            dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(getResourceUrl("IconSmall.png")));
            if (size!=null){
                dialog.setSize(size);
            }            
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    dialog.onClosed();
                    System.exit(0);
                }
            });
            SwingUtils.centerComponent(null, dialog);
            if (dialog.getOwner() != null) {
                dialog.getOwner().setIconImage(Toolkit.getDefaultToolkit().getImage(getResourceUrl("IconSmall.png")));
            }
            dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            dialog.openArgs();
            dialog.requestFocus();
        });
    }
    
    public static void main(String args[]) {
        Options.add();
        init(args);
        create();
    }
    
}
