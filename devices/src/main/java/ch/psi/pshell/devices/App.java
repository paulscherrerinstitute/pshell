
package ch.psi.pshell.devices;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.swing.DevicePanel;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Str;
import java.lang.reflect.Method;

/**
 *
 */
public class App extends ch.psi.pshell.app.App{                
    
    public static void create(String pars) {
        Epics.create();
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
        return getAdditionalArgument();
    }        
    
    
    /**
     */
    public static void main(String args[]) throws Exception {
        Options.add();
        App.init(args);        
        create(getDeviceViewerArgs());
    }    
}