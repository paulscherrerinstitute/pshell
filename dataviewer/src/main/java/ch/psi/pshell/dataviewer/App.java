
package ch.psi.pshell.dataviewer;

import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.devices.Setup;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Sys;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;


/**
 *
 */
public class App extends ch.psi.pshell.app.App{
             
    public static DataDialog create() {        
        Dimension size = Setup.isFullScreen() ? SwingUtils.getDefaultScreenUsableArea().getSize() : Setup.getSize();
        return create(null, null, false, getFileArg(), size);
    }
    
    public static DataDialog create(Window parent, String title, boolean modal, File path, Dimension size) {            
        String format = ch.psi.pshell.framework.Options.DATA_FORMAT.getString("h5");
        return create(parent, title, modal, path, size, format);
    }
        
    public static DataDialog create(Window parent, String title, boolean modal, File path, Dimension size, String format) {            
        String layout = ch.psi.pshell.framework.Options.DATA_LAYOUT.getString("default");
        return create(parent, title, modal, path, size, format, layout);
    }

    public static DataDialog create(Window parent, String title, boolean modal, File path, Dimension size, String format, String layout) {    
        return create(parent, title, modal, path, size, format, layout, false);
    }
    
    public static DataDialog create(Window parent, String title, boolean modal, File path, Dimension size, String format, String layout, boolean single) {    
        String dialogTitle = (title==null) ? Optional.ofNullable(Setup.getTitle()).orElse("DataViewer") : title;
        DataDialog dialog = new DataDialog(parent, modal, dialogTitle);        
        DataPanel panel = dialog.getDataPanel();        
        File file = ((path ==null) && !Context.hasDataManager()) ? new File(Sys.getUserHome()) : path;
        java.awt.EventQueue.invokeLater(() -> {            
            try {
                if (file != null){
                    if (file.isFile() || single){
                        panel.load(file.getAbsolutePath(), format, layout);
                    } else {
                        panel.initialize(new DataManager(file.getAbsolutePath(), format, layout));
                    }
                    dialog.setTitle(dialogTitle + " - " + file.getCanonicalPath());
                } else if (Context.hasDataManager()){
                    panel.initialize(null,null); //!!! Should get parameter from somewhere?
                } 
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
            panel.setDefaultDataPanelListener();            
            
            dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(getResourceUrl("IconSmall.png")));
            if (size!=null){
                dialog.setSize(size);
            }            
            SwingUtils.centerComponent(null, dialog);
            if (dialog.getOwner() != null) {
                dialog.getOwner().setIconImage(Toolkit.getDefaultToolkit().getImage(getResourceUrl("IconSmall.png")));
            }
            dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            dialog.requestFocus();
            
        });
        return dialog;
    }
    
    public static void main(String args[]) {
        Options.add();
        init(args);
        DataDialog dialog = create();
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {                    
                System.exit(0);
            }
        });
        
    }
    
}
