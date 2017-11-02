/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.core;

import ch.psi.pshell.scan.Scan;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 */
public class ExecutionParameters {

    long start;
    Map options = new HashMap();
    int offset;

    void setOptions(Map options) {
        if (options.containsKey("defaults")) {
            reset();
        }
        this.options.putAll(options);
        if (options.containsKey("group")) {
            Context.getInstance().dataManager.setCurrentGroup(String.valueOf(options.get("group")));
        }
        if ((Boolean.TRUE.equals(options.get("open"))) && (!Context.getInstance().dataManager.isOpen())) {
            try {
                Context.getInstance().dataManager.openOutput();
            } catch (IOException ex) {
                Context.getInstance().logger.log(Level.WARNING, null, ex);
            }
        } else if ((Boolean.FALSE.equals(options.get("open"))) && (Context.getInstance().dataManager.isOpen())) {
            Context.getInstance().dataManager.closeOutput();
        }
        if (Boolean.TRUE.equals(options.get("reset"))) {
            offset = Context.getInstance().dataManager.isOpen() ? Context.getInstance().dataManager.getScanIndex() : 0;
            start = System.currentTimeMillis();
        }
    }

    public String getName() {
        if (options.containsKey("name")) {
            return String.valueOf(options.get("name"));
        }
        return (Context.getInstance().runningScriptName != null) ? Context.getInstance().runningScriptName : "console";
    }

    public String getType() {
        return (options.containsKey("type")) ? String.valueOf(options.get("type")) : "";
    }
    String pathName;

    public String getPath() {
        if (pathName == null) {
            String path = (options.containsKey("path")) ? String.valueOf(options.get("path")) : null;
            pathName = Context.getInstance().getSetup().expandPath(((path != null) && (!path.isEmpty())) ? path : Context.getInstance().getConfig().dataPath, start);
            if (Context.getInstance().dataManager != null) {
                //This is done in Data manager but duplicate here to store the full file name
                pathName = Context.getInstance().dataManager.getProvider().getRootFileName(pathName);
            }
        }
        return pathName;
    }

    public boolean isPacked() {
        return Context.getInstance().dataManager.isDataPacked();
    }

    public boolean isOpen() {
        return Context.getInstance().dataManager.isOpen();
    }

    public Object getLayout() {
        return (options.containsKey("layout")) ? String.valueOf(options.get("layout")) : Context.getInstance().getConfig().dataLayout;
    }

    public Boolean getPersist() {
        return (options.containsKey("persist")) ? (Boolean) (options.get("persist")) : Context.getInstance().getConfig().autoSaveScanData;
    }

    public Boolean getFlush() {
        return (options.containsKey("flush")) ? (Boolean) (options.get("flush")) : Context.getInstance().getConfig().dataScanFlushRecords;
    }

    public Boolean getPreserve() {
        return (options.containsKey("preserve")) ? (Boolean) (options.get("preserve")) : Context.getInstance().getConfig().dataScanPreserveTypes;
    }

    public Boolean getAccumulate() {
        return (options.containsKey("accumulate")) ? (Boolean) (options.get("accumulate")) : !Context.getInstance().getConfig().dataScanReleaseRecords;
    }

    public int getIndex() {
        return Context.getInstance().dataManager.getScanIndex();
    }

    public int getCount() {
        return Context.getInstance().dataManager.getScanIndex() - offset;
    }

    public String getScript() {
        return Context.getInstance().runningScriptName;
    }

    public String getGroup() {
        return Context.getInstance().dataManager.getCurrentGroup();
    }

    public String getScanPath() {
        return Context.getInstance().dataManager.getScanPath();
    }

    public Scan getScan() {
        return Context.getInstance().dataManager.getCurrentScan();
    }

    public long getStart() {
        return start;
    }

    public long getExecutionTime() {
        return System.currentTimeMillis() - start;
    }

    public boolean getAborted() {
        return Context.getInstance().aborted;
    }

    //TODO: check multiple parallel calls
    public CommandSource getSource() {
        Context.CommandInfo ret = Context.getInstance().commandInfo.get(Thread.currentThread());
        return (ret == null) ? null : ret.source;
    }

    //TODO: check multiple parallel calls
    public Object getArgs() {
        Context.CommandInfo ret = Context.getInstance().commandInfo.get(Thread.currentThread());
        return (ret == null) ? null : ret.args;
    }

    //TODO: threads created by foreground script return background
    public boolean isBackground() {
        return !Context.getInstance().isInterpreterThread();
    }

    @Override
    public String toString() {
        return getName() + (((getType() != null) && (getType().length() > 0)) ? " [" + getType() + "]" : "");
    }

    void init() {
        if (!isInitialized()) {
            reset();
            start = System.currentTimeMillis();
        }
    }

    void finish() {
        start = -1;
    }

    boolean isInitialized() {
        return (start > 0);
    }

    public void reset() {
        pathName = null;
        offset = 0;
        options = new HashMap();
    }

}
