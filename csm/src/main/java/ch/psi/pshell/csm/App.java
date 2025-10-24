package ch.psi.pshell.csm;

import ch.psi.pshell.app.MainFrame.LookAndFeelType;
import ch.psi.pshell.devices.Setup;
import ch.psi.pshell.swing.SwingUtils;
import java.awt.event.WindowAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class App extends ch.psi.pshell.devices.App{
   
    public static String getCameraProxy() {
        String url = Setup.getCameraServer();
        if (url.startsWith("http://")){            
            url = url.substring(7);
        }
        if (!url.contains(":")){
            url = url + ":8888";
        }
        url = "http://" + url;
        return url;        
    }    

    public static String getPipelineProxy() {
        String url = Setup.getPipelineServer();
        if (url.startsWith("http://")){
            url = url.substring(7);
        }
        if (!url.contains(":")){
            url = url + ":8889";
        }
        url = "http://" + url;
        return url;        
    }    
    
    public static boolean isExpert() {
         return Options.EXPERT.getBool(false);
    }
    
    public static String getDataBufferSourcesRepo(){
        //return Options.DB_SRC_REPO.getString("https://git.psi.ch/archiver_config/sf_databuffer.git");
        return Options.DB_SRC_REPO.getString("https://gitea.psi.ch/archiver_config/sf_databuffer.git");
    }

    public static String getImageBufferSourcesRepo(){
        //return Options.IB_SRC_REPO.getString("https://git.psi.ch/archiver_config/sf_imagebuffer.git");
        return Options.DB_SRC_REPO.getString("https://gitea.psi.ch/archiver_config/sf_imagebuffer.git");
    }
            
    @Override
    protected void onStart() {
        try {            
            View view = new View();
            view.setVisible(true);
            view.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.exit(0);
                }
            });
            applyViewOptions();
            SwingUtils.centerComponent(null, view);
                        
        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }          
    
        

    public static void main(String args[]) throws Exception {        
        setLookAndFeelDefault(LookAndFeelType.dark);
        Options.add();
        init(args);                 
        new App().start(false);
    }
}
