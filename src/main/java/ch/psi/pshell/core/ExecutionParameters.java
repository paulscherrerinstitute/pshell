/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.core;

import ch.psi.pshell.core.VersioningManager.Revision;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.Provider;
import ch.psi.pshell.plotter.PlotLayout;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.utils.Arr;
import ch.psi.utils.Reflection.Hidden;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * TODO: Not working for background commands Unify with CommandInfo so execution
 * parameters are specific to each command
 */
public class ExecutionParameters {

    final String[] executionOptions = new String[]{"defaults", "group", "open", "reset", "name", "type", "path", "tag",
        "layout", "provider", "save", "persist", "flush", "preserve", "keep", "accumulate", "depth_dim", "compression",
        "shuffle", "contiguous", "then"};

    final String[] viewOptions = new String[]{"plot_disabled", "table_disabled", "enabled_plots", "plot_layout",
        "plot_types", "print_scan", "auto_range", "manual_range", "domain_axis", "status"};

    final String[] shortcutOptions = new String[]{"display", "line_plots", "plot_list", "range"};

    long start;
    int offset;

    Map scriptOptions = new HashMap();
    Map commandOptions = new HashMap();

    Map<Thread, Map> childThreadCommandOptions = new HashMap<>();

    //Data control variables
    File outputFile;
    File lastOutputFile;
    boolean changedLayout;
    int scanIndex;
    Layout dataLayout;
    Provider dataProvider;

    static class ScanInfo {

        final int index;
        final boolean persisted;

        ScanInfo(int index, boolean persisted) {
            this.index = index;
            this.persisted = persisted;
        }
    }

    final public HashMap<Scan, ScanInfo> currentScans = new HashMap<>();

    public boolean isOpen() {
        return (outputFile != null);
    }

    public boolean getChangedLayout() {
        Object layout = getLayout();
        return (layout != null) && (!layout.equals(Context.getInstance().getConfig().dataLayout));
    }

    public boolean getChangedProvider() {
        Object provider = getProvider();
        return (provider != null) && (!provider.equals(Context.getInstance().getConfig().dataProvider));
    }

    public String getOutput() {
        if (outputFile == null) {
            return null;
        }
        return outputFile.getPath();
    }

    public File getOutputFile() {
        return outputFile;
    }

    public String getLastOutput() {
        if (lastOutputFile == null) {
            return null;
        }
        return lastOutputFile.getPath();
    }

    public int getIndex() {
        return scanIndex;
    }

    public void setIndex(int index) {
        scanIndex = index;
    }

    public int getCount() {
        return scanIndex - offset;
    }

    public int getIndex(Scan scan) {
        synchronized (currentScans) {
            if (!currentScans.containsKey(scan)) {
                return -1;
            }
            return currentScans.get(scan).index;
        }
    }

    public Scan[] getCurrentScans() {
        synchronized (currentScans) {
            return currentScans.keySet().toArray(new Scan[0]);
        }
    }

    public Scan getCurrentScan() {
        synchronized (currentScans) {
            for (Scan scan : getCurrentScans()) {
                if (currentScans.get(scan).index == getIndex()) {
                    return scan;
                }
            }
        }
        return null;
    }

    void onStartChildThread() {
        childThreadCommandOptions.put(Thread.currentThread(), new HashMap());
    }

    void onFinishedChildThread() {
        childThreadCommandOptions.remove(Thread.currentThread());
    }

    @Hidden
    public void initializeData() throws IOException {
        try {
            if (getChangedLayout()) {
                Object layout = getLayout();
                if (layout instanceof Layout) {
                    setDataLayout((Layout) layout);
                } else if (layout instanceof Class) {
                    setDataLayout((Layout) ((Class) layout).newInstance());
                } else if (layout instanceof String) {
                    setDataLayout((Layout) DataManager.getLayoutClass((String) layout).newInstance());
                } else {
                    throw new Exception("Invalid layout parameter type");
                }
            }

            if (getChangedProvider()) {
                Object provider = getProvider();

                if (provider instanceof Provider) {
                    setDataProvider((Provider) provider);
                } else if (provider instanceof Class) {
                    setDataProvider((Provider) ((Class) provider).newInstance());
                } else if (provider instanceof String) {
                    setDataProvider((Provider) DataManager.getProviderClass((String) provider).newInstance());
                } else {
                    throw new Exception("Invalid provider parameter type");
                }
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Hidden
    public void setDataPath(File dataPath) {
        outputFile = dataPath;
        if (dataPath != null) {
            lastOutputFile = outputFile;
        }
    }

    @Hidden
    public void setDataLayout(Layout layout) {
        dataLayout = layout;
        if (dataLayout != null) {
            dataLayout.initialize();
        }
    }

    @Hidden
    public Layout getDataLayout() {
        return dataLayout;
    }

    @Hidden
    public Provider getDataProvider() {
        return dataProvider;
    }

    @Hidden
    public void setDataProvider(Provider provider) {
        dataProvider = provider;
    }

    @Hidden
    public boolean isScanPersisted(Scan scan) {
        synchronized (currentScans) {
            if (currentScans.containsKey(scan)) {
                return currentScans.get(scan).persisted;
            }
        }
        return false;
    }

    @Hidden
    public int getScanIndex(Scan scan) {
        synchronized (currentScans) {
            if (currentScans.containsKey(scan)) {
                return currentScans.get(scan).index;
            }
        }
        return -1;
    }

    @Hidden
    public void addScan(Scan scan) {
        synchronized (currentScans) {
            scanIndex++;
            currentScans.put(scan, new ScanInfo(scanIndex, getSave()));
        }
    }

    void setScriptOptions(Map options) {
        pathName = null;
        if (options.containsKey("defaults")) {
            reset();
        }
        this.scriptOptions.putAll(options);
        checkOptions(options);
    }

    public void setCommandOptions(Object command, Map options) {
        pathName = null;
        if (childThreadCommandOptions.containsKey(Thread.currentThread())) {
            childThreadCommandOptions.put(Thread.currentThread(), options);
        } else {
            commandOptions = options;
        }
        checkOptions(options);
    }

    public void clearCommandOptions() {
        if (childThreadCommandOptions.containsKey(Thread.currentThread())) {
            childThreadCommandOptions.put(Thread.currentThread(), new HashMap());
        } else {
            commandOptions = new HashMap();
        }
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
        if (getOption("line_plots") != null) {
            Map types = new HashMap();
            List linePlots = (List) getOption("line_plots");
            for (Object obj : linePlots) {
                types.put(obj, 1);
            }
            Map plotTypes = (Map) options.get("plot_types");
            if (plotTypes == null) {
                options.put("plot_types", types);
            } else {
                plotTypes.putAll(types);
            }
        }

        if (getOption("plot_list") != null) {
            Object plot_list = getOption("plot_list");
            options.put("enabled_plots", "all".equals(plot_list) ? null : plot_list);
        }

        if (getOption("range") != null) {
            Object range = getOption("range");
            if ("none".equals(range)) {
                options.put("auto_range", null);
            } else if ("auto".equals(range)) {
                options.put("auto_range", Boolean.TRUE);
            } else {
                options.put("manual_range", range);
            }
        }

        for (Object key : options.keySet()) {
            if (Arr.containsEqual(viewOptions, key)) {
                Object val = options.get(key);
                if (val instanceof List) {
                    val = ((List) val).toArray();
                }
                setPlotPreference(ViewPreference.valueOf(key.toString().toUpperCase()), val);
            }
        }

        Object display = getOption("display");
        if (Boolean.FALSE.equals(display)) {
            setPlotPreference(ViewPreference.PLOT_DISABLED, true);
            setPlotPreference(ViewPreference.TABLE_DISABLED, true);
            setPlotPreference(ViewPreference.PRINT_SCAN, false);
        }
    }

    public Map getScriptOptions() {
        return (scriptOptions == null) ? new HashMap() : scriptOptions;
    }

    public Map getCommandOptions() {
        Map ret = (childThreadCommandOptions.containsKey(Thread.currentThread()))
                ? childThreadCommandOptions.get(Thread.currentThread())
                : commandOptions;
        return (ret == null) ? new HashMap() : ret;
    }
    
    void clearOption(String option){
        scriptOptions.remove(option);
        commandOptions.remove(option) ;      
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
        String script = getScript();
        if (script == null) {
            if (Context.getInstance().getRunningStatement() != null) {
                return "script";
            }
            return "console";
        }
        return script;
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

    public Object getLayout() {
        Object option = getOption("layout");
        return (option != null) ? option : Context.getInstance().getConfig().dataLayout;
    }

    public Object getProvider() {
        Object option = getOption("provider");
        return (option != null) ? option : Context.getInstance().getConfig().dataProvider;
    }

    public Boolean getSave() {
        Object option = getOption("save");
        if (option == null) {
            option = getOption("persist"); //backward compatibility
        }
        return (option != null) ? (Boolean) option : Context.getInstance().getConfig().autoSaveScanData;
    }

    public int getDepthDimension() {
        int depthDimension = Context.getInstance().getConfig().getDepthDim();
        Object option = getOption("depth_dim");
        if ((option != null) && (option instanceof Number)) {
            depthDimension = ((Number) option).intValue();
        }
        return (((depthDimension < 0) || (depthDimension > 2)) ? 0 : depthDimension);
    }

    public Boolean getFlush() {
        Object option = getOption("flush");
        return (option != null) ? (Boolean) option : Context.getInstance().getConfig().dataScanFlushRecords;
    }

    public Boolean getPreserve() {
        Object option = getOption("preserve");
        return (option != null) ? (Boolean) option : Context.getInstance().getConfig().dataScanPreserveTypes;
    }
    
    public class ExecutionStage{
        final String command;
        final boolean onSuccess;
        final boolean onException;
        ExecutionStage(String command, boolean onSuccess, boolean onException){
            this.command = command;
            this.onSuccess = onSuccess;
            this.onException = onException;
        }
    }
        
    public ExecutionStage getThen() {
        Object option = getOption("then");
        String then = (option == null) ? null : String.valueOf(option);
        if ((then==null) || (then.trim().isEmpty())){
            return null;
        }
        
        //Command prefixes:
        //  !       Execute always
        //  ?       Execute only if completed with exception 
        //  default Execute only if completed succeffully
        
        boolean onSuccess = true;
        boolean onException = false;
        if (then.startsWith("!")){
            onException = true;
            then = then.substring(1);
        } else if (then.startsWith("?")){
            onException = true;
            onSuccess = false;
            then = then.substring(1);
        }    
        return new ExecutionStage(then, onSuccess, onException);
    }    

    public Boolean getKeep() {
        Object option = getOption("keep");
        if (option == null) {
            option = getOption("accumulate"); //backward compatibility
        }
        return (option != null) ? (Boolean) option : !Context.getInstance().getConfig().dataScanReleaseRecords;
    }

    boolean isOptionForDevice(Object option, Nameable device) {
        if ((option != null) && (device != null)) {
            if (option == device) {
                return true;
            }
            String name = Context.getInstance().getDataManager().getAlias(device);
            if (option.equals(name)) {
                return true;
            }
            if (option instanceof List) {
                List l = (List) option;
                if ((l.contains(device)) | (l.contains(name))) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map getStorageFeatures(Nameable device) {
        Map ret = null;
        Object compression = getOption("compression");
        if (compression != null) {
            if ((compression instanceof Number) || Boolean.TRUE.equals(compression) || (compression instanceof String)) {
                ret = new HashMap();
                ret.put("compression", compression);
            } else if (isOptionForDevice(compression, device)) {
                ret = new HashMap();
                ret.put("compression", true);
            }
            if (ret != null) {
                Object shuffle = getOption("shuffle");
                if (shuffle != null) {
                    if ((Boolean.TRUE.equals(shuffle)) || isOptionForDevice(shuffle, device)) {
                        ret.put("shuffle", true);
                    }
                }
                return ret;
            }

        }

        Object contiguous = getOption("contiguous");
        if (contiguous != null) {
            if ((Boolean.TRUE.equals(contiguous)) || isOptionForDevice(contiguous, device)) {
                ret = new HashMap();
                ret.put("layout", "contiguous");
                return ret;
            }
        }
        return null;
    }

    public String getTag() {
        return (String) getOption("tag");
    }

    public String getScript() {
        CommandInfo cmd = getCommand();
        return (cmd != null) ? Context.getInstance().getRunningScriptName(cmd.script) : null;
    }

    public File getScriptFile() {
        CommandInfo cmd = getCommand();
        return (cmd != null) ? Context.getInstance().getScriptFile(cmd.script) : null;
    }

    public String getStatement() {
        CommandInfo cmd = getCommand();
        return (cmd != null) ? cmd.command : null;
    }

    public String getScriptVersion() throws IOException {
        File file = getScriptFile();
        if (file != null) {
            Revision rev;
            try {
                rev = Context.getInstance().getFileRevision(file.getPath());
                if (rev != null) {
                    return rev.id;
                }
            } catch (Exception ex) {
            }
        }
        return null;
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

    public CommandSource getSource() {
        CommandInfo cmd = getCommand();
        return (cmd == null) ? null : cmd.source;
    }

    public Object getArgs() {
        CommandInfo cmd = getCommand();
        return (cmd == null) ? null : cmd.args;
    }

    public List<CommandInfo> getCommands() {
        return Context.getInstance().getCommandManager().getCommands();
    }

    public CommandInfo getCommand() {
        List<CommandInfo> commands = getCommands();
        for (CommandInfo cmd : commands) {
            if (cmd.thread == Thread.currentThread()) {
                return cmd;
            }
        }
        //If not in background command, return foreground command
        //TODO: Not considering threads created by background command
        for (CommandInfo cmd : commands) {
            if (cmd.background == false) {
                return cmd;
            }
        }
        return null;
    }

    public boolean isBackground() {
        CommandInfo cmd = getCommand();
        return (cmd != null) ? cmd.background : !Context.getInstance().isInterpreterThread();
    }

    @Override
    public String toString() {
        return getName() + (((getType() != null) && (getType().length() > 0)) ? " [" + getType() + "]" : "");
    }

    void init() {
        if (!isInitialized()) {
            reset();
            start = System.currentTimeMillis();
            scanIndex = 0;
        }
    }

    void finish() {
        start = -1;
        currentScans.clear();
        dataLayout = null;
        dataProvider = null;
        clearOption("then");
    }

    boolean isInitialized() {
        return (start > 0);
    }

    public void reset() {
        lastOutputFile = null;
        pathName = null;
        offset = 0;
        scriptOptions = new HashMap();
        commandOptions = new HashMap();
        childThreadCommandOptions = new HashMap<>();
        plotPreferences.init();
        dataLayout = null;
        dataProvider = isBackground() ? Context.getInstance().getDataManager().cloneProvider() : null;
    }

    void onScanStarted(Scan scan) {
    }

    void onScanEnded(Scan scan) {
        for (Object key : getCommandOptions().keySet()) {
            if (Arr.containsEqual(viewOptions, key)) {
                ViewPreference pref = ViewPreference.valueOf(key.toString().toUpperCase());
                if (scriptOptions.containsKey(key)) {
                    setPlotPreference(pref, scriptOptions.get(key));
                } else {
                    setPlotPreference(pref, null);
                }
            }
        }
        clearCommandOptions();
    }

    @Hidden
    public void onExecutionStarted() {
        Context.getInstance().getDataManager().closeOutput();
        init();
    }

    @Hidden
    public void onExecutionEnded() {
        // Must be called before finish, as calls appendLog, and must do before layourt is reset.
        Context.getInstance().getDataManager().closeOutput();
        finish();
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
            case PLOT_LAYOUT:
                plotPreferences.setPlotLayout((value == null) ? null : PlotLayout.valueOf(value.toString()));
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
