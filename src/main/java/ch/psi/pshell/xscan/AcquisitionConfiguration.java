package ch.psi.pshell.xscan;

import ch.psi.pshell.core.Configuration;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.ui.App;
import ch.psi.utils.EventBus;

public class AcquisitionConfiguration {

    public static transient final EventBus.Mode eventBusModeAcq = EventBus.Mode.SYNC;
    public static transient final EventBus.Mode eventBusModePlot = EventBus.Mode.ASYNC;
    public static transient int channelCreationRetries = 1;
    public static transient String dataFilePrefix = "";

    static Configuration getConfig(){
        return Context.getInstance().getConfig();
    }
    
    public static String getCrlogicPrefix() {
        if (App.hasArgument("crlogic.prefix")) {
            return App.getArgumentValue("crlogic.prefix");
        }
        return getConfig().XScanCrlogicPrefix;
    }

    public static String getCrlogicIoc() {
        if (App.hasArgument("crlogic.ioc")) {
            return App.getArgumentValue("crlogic.ioc");
        }
        return getConfig().XScanCrlogicIoc;
    }

    public static String getCrlogicChannel() {
        if (App.hasArgument("crlogic.channel")) {
            return App.getArgumentValue("crlogic.channel");
        }
        return getConfig().XScanCrlogicChannel;
    }

    public static boolean getCrlogicAbortable() {
        if (App.hasArgument("crlogic.abortable")){
            return App.getBoolArgumentValue("crlogic.abortable");
        }
        return getConfig().XScanCrlogicAbortable;
    }

    public static boolean getScrlogicSimulated() {
        if (App.hasArgument("crlogic.simulated")){
            return App.getBoolArgumentValue("crlogic.simulated");
        }
        return getConfig().XScanCrlogicSimulated;
    }    
    public static String getDataFilePrefix() {
        return getConfig().getXScanDataFileName();
    }

    public static int getActorMoveTimeout() {
        if (App.hasArgument("xscan.move.timeout")){
            return Integer.valueOf(App.getArgumentValue("xscan.move.timeout"));
        }
        if (App.hasArgument("move.timeout")) {
            return Integer.valueOf(App.getArgumentValue("move.timeout"));
        }
        return getConfig().XScanMoveTimeout;
    }

    public static boolean getAppendSuffix() {
        if (App.hasArgument("xscan.suffix")){
            return App.getBoolArgumentValue("xscan.suffix");
        }
        if (App.hasArgument("fdanosuffix")) {
            return false;
        }
        return getConfig().XScanAppendSuffix;
    }

    public static int getChannelCreationRetries() {
        if (App.hasArgument("xscan.channel.retries")){
            return Integer.valueOf(App.getArgumentValue("xscan.channel.retries"));
        }
        return channelCreationRetries;
    }

}
