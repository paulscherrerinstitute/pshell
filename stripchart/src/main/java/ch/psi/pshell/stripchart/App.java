
package ch.psi.pshell.stripchart;

import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Sys;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.ConnectException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class App extends ch.psi.pshell.framework.App{
    
    static public boolean isStripChart() {
        //return getCommand().equals(COMAMND_STRIP_CHART);
        return true; //TODO        
    }
    
    static public boolean isStripChartServer() {
        return isStripChart() && (isAttach() || (Setup.isServerMode()));
    }            
    
    static public boolean isAttach() {
        return Setup.isGui() &&  Options.ATTACH.getBool(false);
    }           
    
    public static void create(){        
        Dimension size = Setup.isFullScreen() ? SwingUtils.getDefaultScreenUsableArea().getSize() : Setup.getSize();        
        create(null, (Setup.getFileArg()==null) ? null : new File(Setup.getFileArg()), Setup.getConfigArg(), null, Setup.getStartArg(), true, size);
    }
    
    public static void create(String title, File file, String config, File defaultFolder, boolean start, boolean modal, Dimension size) {
        String dialogTitle = (title==null) ? Setup.getTitle() : title;
        Epics.create();
        java.awt.EventQueue.invokeLater(() -> {
            File arg = StripChart.getStripChartFolderArg();
            File defaultFolderFinal = (arg!=null) ? arg : defaultFolder;
            StripChart dialog = new StripChart(null, modal, defaultFolderFinal, dialogTitle);
            dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            if (size!=null){
                dialog.setSize(size);
            }
            try {
                if (file != null) {
                    File f = StripChart.resolveFile(file, defaultFolderFinal);
                    dialog.open(f);
                } else if (config != null) {
                    dialog.open(config);
                } 
                if (start) {
                    dialog.startIfConfigured();
                }
            } catch (Exception ex) {
                Logger.getLogger(StripChart.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    dialog.onClosed();
                    if (modal) {
                        System.exit(0);
                    } else if (isAttach()) {
                        if (SwingUtils.getVisibleWindows().length == 1) {
                            System.out.println("Last StripChart window closes: finishing process" + Sys.getProcessName() + "\n");
                            System.exit(0);
                        }
                    }
                }
            });
            SwingUtils.centerComponent(null, dialog);
            if (dialog.getOwner()!=null){
                dialog.getOwner().setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            }
            dialog.setVisible(true);
            dialog.requestFocus();
        });
    }
        
    public static StripChartServer createAttached(){   
        try {
            String ret = StripChartServer.create(getFileArg(), Setup.getConfigArg(), Setup.getStartArg());
            System.out.println("Panel handled by server: " + ret);
        } catch (Exception ex) {
            if ((ex.getCause() != null) && (ex.getCause() instanceof ConnectException)) {
                System.out.println("Server not found");
                ch.psi.pshell.stripchart.App.create(null, getFileArg(), Setup.getConfigArg(), null, Setup.getStartArg(), true, Setup.getSize());
                return new StripChartServer();
            } else {
                ex.printStackTrace();                
            }
        }    
        return null;
    }
    
    
    /**
     */
    public static void main(String args[]) {
        Options.add();
        App.init(args);   
        create();
    }
}
