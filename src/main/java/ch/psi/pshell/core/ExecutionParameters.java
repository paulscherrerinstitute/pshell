/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.core;

import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 */
public class ExecutionParameters implements ScanListener {

    long start;
    int offset;

    Map scriptOptions = new HashMap();
    Map commandOptions = new HashMap();

    void setScriptOptions(Map options) {
        pathName = null;
        if (options.containsKey("defaults")) {
            reset();
        }
        this.scriptOptions.putAll(options);
        checkOptions(options);
    }

    //TODO: handle threading / parallel scans
    public void setCommandOptions(Object command, Map options) {
        pathName = null;
        commandOptions = options;
        checkOptions(options);
    }
    
    void checkOptions(Map options){
        Object group = getOption("group");
        if (group != null) {
            Context.getInstance().dataManager.setCurrentGroup(String.valueOf(group));
        }

        Object open = getOption("open");
        if ((Boolean.TRUE.equals(open)) && (!Context.getInstance().dataManager.isOpen())) {
            try {
                Context.getInstance().dataManager.openOutput();
            } catch (IOException ex) {
                Context.getInstance().logger.log(Level.WARNING, null, ex);
            }
        } else if ((Boolean.FALSE.equals(open)) && (Context.getInstance().dataManager.isOpen())) {
            Context.getInstance().dataManager.closeOutput();
        }

        Object reset = getOption("reset");
        if (Boolean.TRUE.equals(reset)) {
            offset = Context.getInstance().dataManager.isOpen() ? Context.getInstance().dataManager.getScanIndex() : 0;
            start = System.currentTimeMillis();
        }        
    }

    public Map getScriptOptions() {
        return (scriptOptions == null) ? new HashMap() : scriptOptions;
    }

    public Map getCommandOptions() {
        return (commandOptions == null) ? new HashMap() : commandOptions;
    }

    Object getOption(String option) {
        if (getCommandOptions().containsKey(option)) {
            return getCommandOptions().get(option);
        }
        if (getScriptOptions().containsKey(option)) {
            return getScriptOptions().get(option);
        }
        return null;
    }

    public String getName() {
        Object option = getOption("name");
        if (option != null) {
            return String.valueOf(option);
        }
        return (Context.getInstance().runningScriptName != null) ? Context.getInstance().runningScriptName : "console";
    }

    public String getType() {
        Object option = getOption("type");
        return (option != null) ? String.valueOf(option) : "";
    }
    String pathName;

    public String getPath() {
        if (pathName == null) {
            String path = (String) getOption("path");
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
        Object option = getOption("layout");
        return (option != null) ? option : Context.getInstance().getConfig().dataLayout;
    }

    public Boolean getPersist() {
        Object option = getOption("persist");
        return (option != null) ? (Boolean) option : Context.getInstance().getConfig().autoSaveScanData;
    }

    public Boolean getFlush() {
        Object option = getOption("flush");
        return (option != null) ? (Boolean) option : Context.getInstance().getConfig().dataScanFlushRecords;
    }

    public Boolean getPreserve() {
        Object option = getOption("preserve");
        return (option != null) ? (Boolean) option : Context.getInstance().getConfig().dataScanPreserveTypes;
    }

    public Boolean getAccumulate() {
        Object option = getOption("accumulate");
        return (option != null) ? (Boolean) option : !Context.getInstance().getConfig().dataScanReleaseRecords;
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
        scriptOptions = new HashMap();
        commandOptions = new HashMap();
    }

    @Override
    public void onScanStarted(Scan scan, String plotTitle) {
    }

    @Override
    public void onNewRecord(Scan scan, ScanRecord record) {
    }

    @Override

    public void onScanEnded(Scan scan, Exception ex) {
        commandOptions = new HashMap();
    }

}
