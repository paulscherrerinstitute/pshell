package ch.psi.pshell.xscan;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.LayoutFDA;
import ch.psi.pshell.ui.App;
import ch.psi.utils.Config;
import ch.psi.utils.EventBus;

public class AcquisitionConfiguration extends Config {

    public static transient final EventBus.Mode eventBusModeAcq = EventBus.Mode.SYNC;
    public static transient final EventBus.Mode eventBusModePlot = EventBus.Mode.ASYNC;

    public String crlogicPrefix = "";
    public String crlogicIoc = "";
    public String crlogicChannel = "";
    public boolean crlogicAbortable;
    public boolean crlogicSimulated;
    public String dataBaseDirectory = "{data}";
    public String dataFilePrefix = "";
    public int actorMoveTimeout = 600; // 10 Minutes maximum move time 
    public boolean appendSuffix = true;
    public int channelCreationRetries = 1;

    public static String getDataFileNameDefault() {
        String ret = Context.getInstance().getConfig().dataPath;
        if (Context.getInstance().getConfig().fdaSerialization) {
            ret = ret.replaceAll("./$", "");
            return ret + "/" + LayoutFDA.getFilePrefix();
        }
        return ret;
    }

    public String getCrlogicPrefix() {
        if (App.hasArgument("crlogic.prefix")) {
            return App.getArgumentValue("crlogic.prefix");
        }
        return crlogicPrefix;
    }

    public void setCrlogicPrefix(String crlogicPrefix) {
        this.crlogicPrefix = crlogicPrefix;
    }

    public String getCrlogicIoc() {
        if (App.hasArgument("crlogic.ioc")) {
            return App.getArgumentValue("crlogic.ioc");
        }
        return crlogicIoc;
    }

    public String getCrlogicChannel() {
        if (App.hasArgument("crlogic.channel")) {
            return App.getArgumentValue("crlogic.channel");
        }
        return crlogicChannel;
    }

    public String getDataBaseDirectory() {
        return Context.getInstance().getSetup().expandPath(dataBaseDirectory);
    }

    public String getDataFilePrefix() {
        if ((dataFilePrefix == null) || (dataFilePrefix.trim().length() == 0)) {
            return getDataFileNameDefault();
        }
        return dataFilePrefix;
    }

    public int getActorMoveTimeout() {
        if (App.hasArgument("move.timeout")) {
            return Integer.valueOf(App.getArgumentValue("move.timeout"));
        }
        return actorMoveTimeout;
    }

    public boolean getAppendSuffix() {
        if (App.hasArgument("fdanosuffix")) {
            return false;
        }
        return appendSuffix;
    }

    public int getChannelCreationRetries() {
        if (App.hasArgument("xscan.channel.retries")){
            return Integer.valueOf(App.getArgumentValue("xscan.channel.retries"));
        }
        return channelCreationRetries;
    }

    public boolean getCrlogicAbortable() {
        return crlogicAbortable;
    }

    public boolean getScrlogicSimulated() {
        return crlogicSimulated;
    }
}
