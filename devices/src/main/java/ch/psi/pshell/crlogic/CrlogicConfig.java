package ch.psi.pshell.crlogic;

import ch.psi.pshell.devices.App;
import ch.psi.pshell.devices.Setup;
import java.io.IOException;
import java.util.logging.Logger;


public class CrlogicConfig extends ch.psi.pshell.utils.Config{
   
    public String prefix = "";
    public String ioc = "";
    public String channel = "";
    public boolean abortable = true;
    public boolean simulated;

    
    static CrlogicConfig config;

    public static CrlogicConfig getConfig(){
        if (config==null){
            config = new CrlogicConfig();
            try {
                config.load(Setup.expandPath("{config}/crlogic.properties"));
            } catch (IOException ex) {
                 Logger.getLogger(CrlogicConfig.class.getName()).severe("Cannot generate crlogic configuration file");
            }        
        }
        return config;
    }
    
    public static String getPrefix() {
        if (App.hasAdditionalArgumentValue("crlogic.prefix")) {
            return App.getAdditionalArgumentValue("crlogic.prefix");
        }
        return getConfig().prefix;
    }

    public static String getIoc() {
        if (App.hasAdditionalArgumentValue("crlogic.ioc")) {
            return App.getAdditionalArgumentValue("crlogic.ioc");
        }
        return getConfig().ioc;
    }

    public static String getChannel() {
        if (App.hasAdditionalArgumentValue("crlogic.channel")) {
            return App.getAdditionalArgumentValue("crlogic.channel");
        }
        return getConfig().channel;
    }

    public static boolean getAbortable() {
        if (App.hasAdditionalArgumentValue("crlogic.abortable")){
            return App.getBoolAdditionalArgumentValue("crlogic.abortable");
        }
        return getConfig().abortable;
    }

    public static boolean isSimulated() {
        if (Setup.isSimulation()){
            return true;
        }
        if (App.hasAdditionalArgumentValue("crlogic.simulated")){
            return App.getBoolAdditionalArgumentValue("crlogic.simulated");
        }
        return getConfig().simulated;
    }    
    
}
