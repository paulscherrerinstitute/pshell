
package ch.psi.pshell.dataviewer;

import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.devices.Setup;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Str;
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
        
    public static DataDialog create(Window parent, String title, boolean modal, File file, Dimension size, String format) {    
        String dialogTitle = (title==null) ? Optional.ofNullable(Setup.getTitle()).orElse("DataViewer") : title;
        DataDialog dialog = new DataDialog(parent, modal, dialogTitle);        
        DataPanel panel = dialog.getDataPanel();                
        java.awt.EventQueue.invokeLater(() -> {            
            try {
                if (file != null){
                    panel.load(file.getAbsolutePath(), format, null);
                    dialog.setTitle(dialogTitle + " - " + file.getCanonicalPath());
                } else {
                    File path = Context.getDefaultDataPath();        
                    panel.setVisibleFiles(getDataPanelVisibleFiles());
                    panel.initialize(new DataManager(path.toString(), format, null));
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
    
    public static String[] getDataPanelVisibleFiles(){
        String dataVisibleFiles = Options.VISIBLE_FILES.getString(null);
        if ((dataVisibleFiles!=null) && (!dataVisibleFiles.isBlank())){
            String[] ret = Str.split(dataVisibleFiles.trim(), new String[]{"|", ";", ",", " "});
            ret = Arr.removeEquals(ret, "");
            return ret;
        }
        return new String[0];
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
