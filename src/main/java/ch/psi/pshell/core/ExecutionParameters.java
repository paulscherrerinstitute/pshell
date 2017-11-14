/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.core;

import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.utils.Arr;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 */
public class ExecutionParameters  {

    final String[] executionOptions = new String[]{"defaults", "group", "open", "reset", "name", "type", "path",
        "layout", "persist", "flush", "preserve", "accumulate", "depth_dim"};

    final String[] viewOptions = new String[]{"plot_disabled", "table_disabled", "enabled_plots", 
        "plot_types", "print_scan", "auto_range", "manual_range","domain_axis", "status"};        
    
    final String[] shortcutOptions = new String[]{"line_plots", "plot_list", "range"};        
    
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

    void checkOptions(Map options) {
        if (options != null) {
            for (Object key : options.keySet()) {
                if (!Arr.containsEqual(executionOptions, key)) {
                    if (!Arr.containsEqual(viewOptions, key)) {
                        if (!Arr.containsEqual(shortcutOptions, key)) {                        
                            throw new RuntimeException("Invalid option: " + key);
                        }
                    }
                }
            }
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
        
        //Process shortcuts
        if (getOption("line_plots")!=null){
            Map types = new HashMap();
            List linePlots =    (List)getOption("line_plots");
            for (Object obj: linePlots){
                types.put(obj, 1);
            }
            Map plotTypes = (Map) options.get("plot_types");
            if (plotTypes == null){
                options.put("plot_types", types);
            } else {
                plotTypes.putAll(types);
            }
        }
        
        if (getOption("plot_list")!=null){
            Object plot_list = getOption("plot_list");
            options.put("enabled_plots", "all".equals(plot_list) ? null : plot_list);
        }
        
        if (getOption("range")!=null){
            Object range = getOption("range");
            if ("none".equals(range)){
                options.put("auto_range", null);
            } else if ("auto".equals(range)){
                options.put("auto_range", Boolean.TRUE);
            } else {
                options.put("manual_range", range);
            }
        }        
        
        for (Object key : options.keySet()) {
            if (Arr.containsEqual(viewOptions, key)) {
                Object val = options.get(key);
                if (val instanceof List){
                    val = ((List)val).toArray();
                }
                setPlotPreference(ViewPreference.valueOf(key.toString().toUpperCase()), val);
            }
        }                
    }

    public Map getScriptOptions() {
        return (scriptOptions == null) ? new HashMap() : scriptOptions;
    }

    public Map getCommandOptions() {
        return (commandOptions == null) ? new HashMap() : commandOptions;
    }

    public Object getOption(String option) {
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

    public int getDepthDimension() {
        int depthDimension = Context.getInstance().getConfig().getDepthDim();
        Object option = getOption("depth_dim");        
        if ((option != null) && (option instanceof Number)){
            depthDimension = ((Number)option).intValue();
        }
        return (((depthDimension<0) || (depthDimension>2)) ? 0 : depthDimension);
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

    public void onScanStarted(Scan scan) {
    }

    public void onScanEnded(Scan scan) {
        for (Object key : commandOptions.keySet()) {
            if (Arr.containsEqual(viewOptions, key)) {                
                ViewPreference pref = ViewPreference.valueOf(key.toString().toUpperCase());
                if (scriptOptions.containsKey(key)){
                    setPlotPreference(pref, scriptOptions.get(key));
                } else {
                    setPlotPreference(pref, null);
                }
            }
        }        
        commandOptions = new HashMap();        
    }
    
    void onStateChange(State state){
        if (state.isProcessing()) {
            init();
            if (state == State.Busy) {
                plotPreferences.init();
            }
        } else {
            finish();
        }        
    }  
    
    final ViewPreference.PlotPreferences plotPreferences = new ViewPreference.PlotPreferences();

    /**
     * Hints to graphical layer
     */
    void setPlotPreference(ViewPreference preference, Object value) {
        switch (preference) {
            case ENABLED_PLOTS:
                if (value == null) {
                    plotPreferences.setEnabledPlots(null);
                } else {
                    ArrayList<String> plots = new ArrayList<>();
                    for (Object obj : (Object[]) value) {
                        if (obj instanceof Nameable) {
                            plots.add(Context.getInstance().getDataManager().getAlias((Nameable) obj));
                        } else {
                            plots.add(String.valueOf(obj));
                        }
                    }
                    plotPreferences.setEnabledPlots(plots);
                }
                break;

            case PLOT_TYPES:
                if (value == null) {
                    plotPreferences.resetPlotTypes();
                } else {
                    plotPreferences.setPlotTypes((Map) value);
                }
                break;
            case AUTO_RANGE:
                if (value == null) {
                    plotPreferences.setFixedRange();
                } else {
                    plotPreferences.setAutoRange((Boolean) value);
                }
                break;
            case MANUAL_RANGE:
                if (value == null) {
                    plotPreferences.setFixedRange();
                } else {
                    plotPreferences.setManualRange((Object[]) value);
                }
                break;
            case DOMAIN_AXIS:
                plotPreferences.setDomainAxis((String) value);
                break;
            case DEFAULTS:
                plotPreferences.init();
                break;
        }
        Context.getInstance().triggerPreferenceChange(preference, value);
    }

    public ViewPreference.PlotPreferences getPlotPreferences() {
        return plotPreferences;
    }

}
