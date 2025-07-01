
package ch.psi.pshell.devices;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Str;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class App extends ch.psi.pshell.app.App{
                
    static public void init(String[] args) {
        init(args, null);
    }
    
    static public void init(String[] args, String[] commands) {
         init(args, commands, true);
    }
    
    static public void init(String[] args, String[] commands, boolean strict) {
        ch.psi.pshell.app.App.init(args, commands, strict);
        String epicsConfig = Setup.getEpicsConfig();
        if ((epicsConfig!=null) && !epicsConfig.isBlank()) {
            String jcae = epicsConfig.trim();
            String fileName = Setup.expandPath(jcae);
            if (new File(fileName).isFile()) {
                System.setProperty(Epics.PROPERTY_EPICS_CONFIG_FILE, fileName);
            } else if (new File(fileName +".properties").isFile()) {
                System.setProperty(Epics.PROPERTY_EPICS_CONFIG_FILE, fileName + ".properties");
            } else if (jcae.contains("=")) {
                try {
                    Path folder = Files.createTempDirectory("epics");
                    Path file = Path.of(folder.toString(), "jcae.properties");
                    System.setProperty(Epics.PROPERTY_EPICS_DEFAULT_CONFIG, jcae.replaceAll("\\|", "\n"));
                    System.setProperty(Epics.PROPERTY_EPICS_CONFIG_FILE, file.toString());
                } catch (Exception ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }                
            }
        }      
    }
    
    public static void create(String pars) {
        Epics.create(Setup.isParallelInit());
        try {
            String[] tokens = Str.splitIgnoringQuotesAndMultSpaces(pars);
            String className = tokens[0];
            String[] args = Arr.getSubArray(tokens, 1);
            Method main = Class.forName(className).getMethod("main", String[].class);
            main.invoke(null, new Object[]{args});
        } catch (Exception ex) {
            //try an inline device string 
            try{
                Device device = InlineDevice.create(pars, null);
                device.initialize();
                DevicePanel pn = DevicePanel.createFrame(device);                                        
                //TODO: Not working, must wait/handle window close
                while(!pn.isShowing()){
                    Thread.sleep(10);
                }
            } catch (Exception e) {                        
                e.printStackTrace();
            }                                        
        }
    }
    
    static public String getDeviceViewerArgs() {
        return getAditionalArgument();
    }        
    
    
    /**
     */
    public static void main(String args[]) throws Exception {
        Options.add();
        App.init(args);        
        create(getDeviceViewerArgs());
    }    
}