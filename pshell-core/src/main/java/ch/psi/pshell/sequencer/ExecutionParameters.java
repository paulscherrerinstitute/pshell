package ch.psi.pshell.sequencer;

import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Format;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.plot.PlotLayout;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.versioning.VersionControl;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class ExecutionParameters {
    
    static final Logger logger = Logger.getLogger(ExecutionParameters.class.getName());

    final String[] executionOptions = new String[]{"defaults", "group", "open", "reset", "name", "type", "path", "tag", "seq", "split",
        "layout", "format", "save", "flush", "preserve", "keep", "lazy", "meta", "logs", "timestamps",
        "depth_dim", "compression", "shuffle", "contiguous", "parallel", "stack", "then", "then_exception", "then_success"};

    final String[] viewOptions = new String[]{"plot_disabled", "table_disabled", "enabled_plots", "plot_layout",
        "plot_types", "print_scan", "auto_range", "manual_range", "manual_range_y", "domain_axis", "status"};

    final String[] shortcutOptions = new String[]{"display", "line_plots", "plot_list", "range"};
    
    final String CMD_OPTION = "_command";

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
    Format dataFormat;

    public static class ScanInfo {

        final int index;
        final boolean persisted;
        final boolean displayed;

        ScanInfo(int index, boolean persisted, boolean displayed) {
            this.index = index;
            this.persisted = persisted;
            this.displayed = displayed;
        }
    }

    final public HashMap<Scan, ScanInfo> currentScans = new HashMap<>();

    public boolean isOpen() {
        return (outputFile != null);
    }

    public boolean getChangedLayout() {
        Object layout = getLayout();
        return (layout != null) && !Context.getDataManager().isSameLayout(layout,Context.getLayout());
    }

    public boolean getChangedFormat() {
        Object format = getFormat();
        return (format != null) && !Context.getDataManager().isSameFormat(format, Context.getFormat());
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
    
    public int getSeq() {
        if (isOpen()){
            return Context.getDataManager().getCurrentFileSequentialNumber();
        } else {            
            return Context.getFileSequentialNumber();
        }    
    }

    public int getDaySeq() {
        if (isOpen()){
            return Context.getDataManager().getCurrentDaySequentialNumber();
        } else {
            return Context.getDaySequentialNumber();
        }
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

    public Scan getScan() {
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
    
    public boolean isHandlingSessions(){
        return Context.isHandlingSessions();
    }
    
    public int getSessionId(){
        return Context.getSessionId();
    }

    public String getSessionName(){
        return Context.getSessionName();
    }

    @Hidden
    public void initializeData() throws IOException {
        try {
            if (getChangedLayout()) {
                Object layout = getLayout();
                if (layout instanceof Layout l) {
                    setDataLayout(l);
                } else if (layout instanceof Class cls) {
                    setDataLayout((Layout) cls.getDeclaredConstructor().newInstance());
                } else if (layout instanceof String str) {
                    setDataLayout((Layout) DataManager.getLayoutClass(str).getDeclaredConstructor().newInstance());
                } else {
                    throw new Exception("Invalid layout parameter type");
                }
            }

            if (getChangedFormat()) {
                Object format = getFormat();

                if (format instanceof Format p) {
                    setDataFormat(p);
                } else if (format instanceof Class cls) {
                    setDataFormat((Format) cls.getDeclaredConstructor().newInstance());
                } else if (format instanceof String str) {
                    setDataFormat((Format) DataManager.getFormatClass(str).getDeclaredConstructor().newInstance());
                } else {
                    throw new Exception("Invalid format parameter type");
                }
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Hidden
    public void setDataPath(File dataPath) {
        if (dataPath!=outputFile) {
            if ((dataPath!=null) && (dataPath.equals(outputFile))){
                return;
            }
            if (dataPath!=null) {
                dataPath.mkdirs();
            }
            outputFile = dataPath;
            if (dataPath != null) {
                lastOutputFile = outputFile;
            }
            Context.getSequencer().onChangeDataPath(dataPath);
            if (isHandlingSessions()){
                try{
                    Context.getSessions().onChangeDataPath(dataPath);
                } catch(Exception ex){
                    logger.log(Level.WARNING, null, ex);
                }
            }
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
    public Format getDataFormat() {
        return dataFormat;
    }

    @Hidden
    public void setDataFormat(Format format) {
        dataFormat = format;
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
    public boolean isScanDisplayed(Scan scan) {
        synchronized (currentScans) {
            if (currentScans.containsKey(scan)) {
                return currentScans.get(scan).displayed;
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
            boolean displayed = !(Boolean.FALSE.equals(getCommandOptions().getOrDefault("display", true)));
            currentScans.put(scan, new ScanInfo(scanIndex, getSave(), displayed));
        }
    }

    public void setScriptOptions(Map options) {
        pathName = null;
        if (options.containsKey("defaults")) {
            reset();
        }
        this.scriptOptions.putAll(options);
        checkOptions(options);
        Object display = getOption("display");
        if (Boolean.FALSE.equals(display)) {
            setPlotPreference(ViewPreference.PLOT_DISABLED, true);
            setPlotPreference(ViewPreference.TABLE_DISABLED, true);
            setPlotPreference(ViewPreference.PRINT_SCAN, false);
        }
        
    }
    
    public Object getCommand(){
        return getCommandOptions().get(CMD_OPTION);
    } 

    public void setCommandOptions(Object command, Map options) {
        pathName = null;
        if (childThreadCommandOptions.containsKey(Thread.currentThread())) {
            childThreadCommandOptions.put(Thread.currentThread(), options);
        } else {
            commandOptions = options;
        }
        options.put(CMD_OPTION, command);
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
                            if (!key.equals(CMD_OPTION)){
                                throw new RuntimeException("Invalid option: " + key);
                            }
                        }
                    }
                }
            }
        }        
        if (getOption("seq") != null) {
            try {
                Number seq = (Number)getOption("seq");
                Context.setFileSequentialNumber(seq.intValue());
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }            
            clearOption("seq");
        }
        
        Object open = getOption("open");
        if (getOption("open")!=null) {
            if ((Boolean.TRUE.equals(open)) && (!isOpen())) {
                try {
                    Context.getDataManager().openOutput();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            } else if ((Boolean.FALSE.equals(open)) && (isOpen())) {
                Context.getDataManager().closeOutput();
                currentScans.clear();
                scanIndex = 0;      
                lastOutputFile = null;
                pathName = null;
                dataLayout = null;
                dataFormat = isBackground() ? Context.getDataManager().cloneFormat() : null;            
                start = System.currentTimeMillis();
                offset = 0;      
            }
            clearOption("open");
        }

        Object reset = getOption("reset");
        if (Boolean.TRUE.equals(reset)) {
            offset = isOpen() ?  getIndex() : 0;
            start = System.currentTimeMillis();
            clearOption("reset");
        }
        
        if (Boolean.TRUE.equals(getCommandOptions().get("split")!=null)) {
            if ( getCommand() instanceof Scan scan){
                scan.setSplitPasses(true);
            }
        }          
        if ((getScriptOptions().get("split")!=null) && (getScriptOptions().get("split") instanceof Scan scan)) {
            try {
                Context.getDataManager().splitScanData(scan);
            } catch (IOException ex) {
                logger.log(Level.WARNING, null, ex);
            }
            clearOption("split");
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
                if (val instanceof List list) {
                    val = list.toArray();
                }
                setPlotPreference(ViewPreference.valueOf(key.toString().toUpperCase()), val);
            }
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
    
    public String getOptionAsString(String option){
        Object value = getOption(option);
        return (value == null) ? null : String.valueOf(value);
    }
    

    public String getName() {
        Object option = getOption("name");
        if (option != null) {
            return String.valueOf(option);
        }
        String script = getScript();
        if (script == null) {
            if (Context.getSequencer().getRunningStatement() != null) {
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
            pathName = Setup.expandPath(((path != null) && (!path.isEmpty())) ? path : Context.getDataFilePattern(), start);
            if (Context.hasDataManager()) {
                //This is done in Data manager but duplicate here to store the full file name
                pathName = Context.getDataManager().getFormat().getRootFileName(pathName);
            }
        }
        return pathName;
    }

    public boolean isPacked() {
        return Context.getDataManager().isDataPacked();
    }

    public Object getLayout() {
        Object option = getOption("layout");
        return (option != null) ? option : Context.getLayout().getId();
    }

    public Object getFormat() {
        Object option = getOption("format");
        return (option != null) ? option : Context.getFormat().getId();
    }

    public Boolean getSave() {
        Object option = getOption("save");
        return (option != null) ? (Boolean) option : Context.getScanConfig().autoSave();
    }

    public Integer getDepthDimension() {
        Object option = getOption("depth_dim");
        if (option instanceof Number number) {
            int depthDimension = number.intValue();
            return (((depthDimension < 0) || (depthDimension > 2)) ? 0 : depthDimension);
        }       
        return null;
    }

    public Boolean getFlush() {
        Object option = getOption("flush");
        return (option != null) ? (Boolean) option : Context.getScanConfig().flushRecords();
    }

    public Boolean getPreserve() {
        Object option = getOption("preserve");
        return (option != null) ? (Boolean) option : Context.getScanConfig().preserveTypes();
    }
    
    public class ExecutionStage{
        final String onSuccess;
        final String onException;
        ExecutionStage(String commandOnSuccess, String commandOnException){
            this.onSuccess = commandOnSuccess;
            this.onException = commandOnException;
        }
    }
        
    public ExecutionStage getThen() {
        String onSuccess = null;
        String onException = null;        
        
        String then =  getOptionAsString("then");
        if ((then!=null) &&(!then.trim().isEmpty())){
            onSuccess = then;
            onException = then;
        }
        
        then =  getOptionAsString("then_exception");
        if ((then!=null) &&(!then.trim().isEmpty())){
            onException = then;
        }        
        
        then =  getOptionAsString("then_success");
        if ((then!=null) &&(!then.trim().isEmpty())){
            onSuccess = then;
        }          
        if ((onException == null) && (onSuccess == null)){
            return null;
        }
        return new ExecutionStage(onSuccess, onException);
    }    

    public Boolean getKeep() {
        Object option = getOption("keep");
        return (option != null) ? (Boolean) option : !Context.getScanConfig().releaseRecords();
    }

    public Boolean getLazy() {
        Object option = getOption("lazy");
        return (option != null) ? (Boolean) option : Context.getScanConfig().lazyTableCreation();
    }
    
    public Boolean getSaveLogs() {
        Object option = getOption("logs");
        return (option != null) ? (Boolean) option : Context.getScanConfig().saveLogs();
    }

    public Boolean getSaveMeta() {
        Object option = getOption("meta");
        return (option != null) ? !Boolean.FALSE.equals(option) : Context.getScanConfig().saveMeta();
    }
    
    public Map<String, Object> getAdditionalMeta() {
        Object option = getOption("meta");
        return ((option != null) && (option instanceof Map map)) ? map : null;
    }

    public Boolean getSaveTimestamps() {
        Object option = getOption("timestamps");
        return (option != null) ? (Boolean) option : Context.getScanConfig().saveTimestamps();
    }
    
    boolean isOptionForDevice(Object option, Nameable device) {
        if ((option != null) && (device != null)) {
            if (option == device) {
                return true;
            }
            String name = device.getAlias();
            if (option.equals(name)) {
                return true;
            }
            if (option instanceof List list) {
                if ((list.contains(device)) | (list.contains(name))) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map getStorageFeatures(Nameable device) {
        Map ret = new HashMap();
        Object compression = getOption("compression");
        if (compression != null) {
            if ((compression instanceof Number) || Boolean.TRUE.equals(compression) || (compression instanceof String)) {
                ret.put("compression", compression);
            } else if (isOptionForDevice(compression, device)) {
                ret.put("compression", true);
            }
            Object shuffle = getOption("shuffle");
            if (shuffle != null) {
                if ((Boolean.TRUE.equals(shuffle)) || isOptionForDevice(shuffle, device)) {
                    ret.put("shuffle", true);
                }
            }
        } else {
            Object contiguous = getOption("contiguous");
            if (contiguous != null) {
                if ((Boolean.TRUE.equals(contiguous)) || isOptionForDevice(contiguous, device)) {
                    ret.put("layout", "contiguous");
                }
            }
        }
        
        Object stack = getOption("stack");
        if (stack != null) {
            if ((Boolean.TRUE.equals(stack)) || isOptionForDevice(stack, device)) {
                ret.put("stack", "true");
            }
        }
        
        Object parallel = getOption("parallel");
        if (parallel != null) {
            if ((Boolean.TRUE.equals(parallel)) || isOptionForDevice(parallel, device)) {
                ret.put("parallel", "true");
            }
        }
        
        return ret;
    }

    public String getTag() {
        return (String) getOption("tag");
    }

    public String getScript() {
        CommandInfo cmd = getCommandInfo();
        return (cmd != null) ? Context.getSequencer().getScriptPrefix(cmd.script) : null;
    }

    public File getScriptFile() {
        CommandInfo cmd = getCommandInfo();
        return (cmd != null) ? Context.getSequencer().getScriptFile(cmd.script) : null;
    }

    public String getStatement() {
        CommandInfo cmd = getCommandInfo();
        return (cmd != null) ? cmd.command : null;
    }

    public String getScriptVersion() throws IOException {
        File file = getScriptFile();
        if (file != null) {
            return VersionControl.getFileRevisionId(file.toString());
        }
        return null;
    }

    public String getGroup() {
        return Context.getDataManager().getCurrentGroup(getScan());
    }
    
    public String getScanPath() {
        return Context.getDataManager().getScanPath();
    }    
                   
    public long getStart() {
        return start;
    }

    public long getExecutionTime() {
        return System.currentTimeMillis() - start;
    }

    public boolean getAborted() {
        return Context.getSequencer().isAborted();
    }

    public CommandSource getSource() {
        CommandInfo cmd = getCommandInfo();
        return (cmd == null) ? null : cmd.source;
    }

    public Object getArgs() {
        CommandInfo cmd = getCommandInfo();
        return (cmd == null) ? null : cmd.args;
    }

    public CommandInfo getCommandInfo() {
        return getCommand(true);
    }
    
    public Object getInnerArgs() {
        CommandInfo cmd = getInnerCommandInfo();
        return (cmd == null) ? null : cmd.args;
    }
    
    public CommandSource getInnerSource() {
        CommandInfo cmd = getInnerCommandInfo();
        return (cmd == null) ? null : cmd.source;
    }    
    
    public String getInnerStatement() {
        CommandInfo cmd = getInnerCommandInfo();
        return (cmd != null) ? cmd.command : null;
    }
    
    public String getInnerScript() {
        CommandInfo cmd = getInnerCommandInfo();
        return (cmd != null) ?  Context.getSequencer().getScriptPrefix(cmd.script) : null;
    }

    public File getInnerScriptFile() {
        CommandInfo cmd = getInnerCommandInfo();
        return (cmd != null) ?  Context.getSequencer().getScriptFile(cmd.script) : null;
    }
    
    public CommandInfo getInnerCommandInfo() {
        return getCommand(false);
    }
    
    public CommandInfo getCommand(boolean parent) {
        CommandInfo ret = Context.getCommandBus().getCurrentCommand(parent);
        //If not in background command, return foreground command
        if (ret==null){
            ret = Context.getCommandBus().getInterpreterThreadCommand(parent);
        }
        return ret;

    }
    
    public boolean isChildThread(Thread thread){
        return childThreadCommandOptions.containsKey(thread); 
    }              

    public boolean isBackground() {
        CommandInfo cmd = getCommandInfo();
        return (cmd != null) ? cmd.background : !Context.getSequencer().isInterpreterThread();
    }
    
    public boolean isDebug() {
        return Context.getSequencer().isRunningStatements();
    }
    
    public boolean isSimulation() {
        return Context.isSimulation();
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
        dataFormat = null;
        for (String option: new String[]{"then", "then_exception", "then_success"}){
            clearOption(option);
        }   

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
        dataFormat = isBackground() ? Context.getDataManager().cloneFormat() : null;
    }

    public void onScanStarted(Scan scan) {
    }

    public void onScanEnded(Scan scan) {
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
        Context.getDataManager().closeOutput();
        init();
    }

    @Hidden
    public void onExecutionEnded() {
        // Must be called before finish, as calls appendLog, and must do before layourt is reset.
        Context.getDataManager().closeOutput();
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
                        if (obj instanceof Nameable nameable) {
                            plots.add(nameable.getAlias());
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
                } else if (value instanceof Object[] arr){
                    if (arr.length==2){
                        plotPreferences.setManualRange(arr);
                    } else if (arr.length==4){
                        plotPreferences.setManualRange(Arr.getSubArray(arr, 0, 2));
                        plotPreferences.setManualRangeY(Arr.getSubArray(arr, 2, 2));
                    }
                }
                break;
            case MANUAL_RANGE_Y:
                if (value == null) {
                    plotPreferences.setFixedRangeY();
                } else {
                    plotPreferences.setManualRangeY((Object[]) value);
                }
                break;                
            case DOMAIN_AXIS:
                plotPreferences.setDomainAxis((String) value);
                break;
            case DEFAULTS:
                plotPreferences.init();
                break;
        }
        Context.getSequencer().triggerPreferenceChange(preference, value);
    }

    public ViewPreference.PlotPreferences getPlotPreferences() {
        return plotPreferences;
    }

}
