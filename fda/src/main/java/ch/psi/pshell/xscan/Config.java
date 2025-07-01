package ch.psi.pshell.xscan;

import ch.psi.pshell.data.LayoutFDA;
import ch.psi.pshell.framework.App;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.utils.EventBus;
import java.io.IOException;
import java.util.logging.Logger;

public class Config extends ch.psi.pshell.utils.Config{

    public static transient final EventBus.Mode eventBusModeAcq = EventBus.Mode.SYNC;
    public static transient final EventBus.Mode eventBusModePlot = EventBus.Mode.ASYNC;    
    
    public int moveTimeout = 600; // 10 Minutes maximum move time 
    public boolean appendSuffix = true;
    public boolean continuousUpdate;
    public boolean fdaSerialization;
    public int channelCreationRetries = 1;
    @Defaults(values = {"h5", "txt", "txtd","csv", "fda", "tiff"})
    public String dataFormat = "h5";
    @Defaults(values = {"default", "table", "sf", "fda", "nx"})
    public String dataLayout = "default";
    public String dataPath = Setup.TOKEN_DATA + "/" + Setup.TOKEN_YEAR + "_" + Setup.TOKEN_MONTH + "/" + 
                    Setup.TOKEN_DATE + "/" + Setup.TOKEN_DATE + "_" + Setup.TOKEN_TIME + "_" + Setup.TOKEN_EXEC_NAME;
    
    
    

    static Config config;

    public static Config getConfig(){
        if (config==null){
            config = new Config();
            try {
                config.load(Setup.expandPath("{config}/xscan.properties"));
            } catch (IOException ex) {
                 Logger.getLogger(Config.class.getName()).severe("Cannot generate xscan configuration file");
            }        
        }
        return config;
    }
        
    public static boolean isContinuousUpdate() {
        if (App.hasAditionalArgumentValue("xscan.continuous.update")){
            return App.getBoolAditionalArgumentValue("xscan.continuous.update");
        }
        return getConfig().continuousUpdate;
    }    
    
    public static String getDataFilePattern() {
        String ret = Context.getDataFilePattern();
        if (isFdaSerialization()) {
            ret = ret.replaceAll("./$", "");
            return ret + "/" + LayoutFDA.getFilePrefix();
        }
        return ret;
    }

    public static int getActorMoveTimeout() {
        if (App.hasAditionalArgumentValue("xscan.move.timeout")){
            return Integer.valueOf(App.getAditionalArgumentValue("xscan.move.timeout"));
        }
        if (App.hasAditionalArgumentValue("move.timeout")) {
            return Integer.valueOf(App.getAditionalArgumentValue("move.timeout"));
        }
        return getConfig().moveTimeout;
    }

    public static boolean getAppendSuffix() {
        if (App.hasAditionalArgumentValue("xscan.suffix")){
            return App.getBoolAditionalArgumentValue("xscan.suffix");
        }
        if (App.hasAditionalArgumentValue("fdanosuffix")) {
            return false;
        }
        return getConfig().appendSuffix;
    }

    public static int getChannelCreationRetries() {
        if (App.hasAditionalArgumentValue("xscan.channel.retries")){
            return Integer.valueOf(App.getAditionalArgumentValue("xscan.channel.retries"));
        }
        return getConfig().channelCreationRetries;
    }
    
    public static boolean isFdaSerialization() {
        if (App.hasAditionalArgumentValue("xscan.fda.serialization")){
            return App.getBoolAditionalArgumentValue("xscan.fda.serialization");
        }
        return getConfig().fdaSerialization;
    }
    
    public String getDataFormat() {
        String format = Setup.getDataFormat();
        return format == null ? dataFormat : format;
    }

    public String getDataLayout() {
        String layout = Setup.getDataLayout();
        return layout == null ? dataLayout : layout;
    }    

}
