package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ExecutionParameters;
import static ch.psi.pshell.data.Layout.ATTR_END_TIMESTAMP;
import static ch.psi.pshell.data.Layout.ATTR_FILE;
import static ch.psi.pshell.data.Layout.ATTR_NAME;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_DOMAIN;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_ENABLE;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_RANGE;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_TYPES;
import static ch.psi.pshell.data.Layout.ATTR_PLOT_TYPES_SEPARATOR;
import static ch.psi.pshell.data.Layout.ATTR_START_TIMESTAMP;
import static ch.psi.pshell.data.Layout.ATTR_VERSION;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.utils.Chrono;
import ch.psi.utils.IO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Common layouts utilities
 */
public abstract class LayoutBase implements Layout {

    Boolean persistSetpoints;
    boolean writeSessionMetadata=true;
    String sessionMetadataPath="/";
    boolean sessionMetadataAttributes=true;

    public boolean getPersistSetpoints() {
        return (persistSetpoints == null) ? getDataManager().getExecutionPars().getSaveSetpoints() : persistSetpoints;
    }

    boolean createLogs = true;

    public boolean getCreateLogs() {
        return createLogs;
    }

    public void setCreateLogs(boolean value) {
        createLogs = value;
    }

    public void setPersistSetpoints(boolean value) {
        persistSetpoints = value;
    }
    
    public void setWriteSessionMetadata(boolean value, String path, boolean attributes) {
        persistSetpoints = value;
        sessionMetadataPath = path;
        sessionMetadataAttributes = attributes;
    }    

    @Override
    public String getLogsPath() {
        return "logs/";
    }

    @Override
    public String getLogFilePath() {
        return getLogsPath() + "logs";
    }

    @Override
    public String getOutputFilePath() {
        return getLogsPath() + "output";
    }

    @Override
    public void appendLog(String log) throws IOException {
        DataManager dataManager = getDataManager();
        String logFile = getLogFilePath();
        if (logFile != null) {
            dataManager.checkLogFile(logFile);
            String time = Chrono.getTimeStr(System.currentTimeMillis(), "dd/MM/YY HH:mm:ss.SSS - ");
            dataManager.appendItem(logFile, time + log);
        }
    }

    @Override
    public String getScriptsPath() {
        return "scripts/";
    }

    @Override
    public void saveScript(String name, String contents) throws IOException {
        DataManager dataManager = getDataManager();
        String scriptsPath = getScriptsPath();
        if (scriptsPath != null) {
            String filename = scriptsPath + name;
            if (dataManager.getProvider() instanceof ProviderText){
                Path path = ((ProviderText)dataManager.getProvider()).getFilePath(filename, false);
                path.toFile().getParentFile().mkdirs();
                Files.writeString(path, contents);
                IO.setFilePermissions(path.toFile(), Context.getInstance().getConfig().filePermissionsData);
            } else {
                dataManager.setDataset(filename, contents);
            }
        }
    }

    @Override
    public void writeSessionMetadata() throws IOException {
        Context.getInstance().writeSessionMetadata(sessionMetadataPath, sessionMetadataAttributes);
    }    

    @Override
    public void onOpened(File output) throws IOException {
        setFileIdAttribute();
        setLayoutAttribute();
        setNameAttribute();
        setScriptFileAttibute();
    } 
    
    @Override
    public void onClosed(File output) throws IOException {
         setScriptVersionAttibute(); //Doing on close because file is commited asynchronously on start of scan.
    }        
    
   @Override
    public void onStart(Scan scan) throws IOException {
        setStartTimestampAttibute(scan);
        setPlotPreferencesAttibutes(scan);
    }
    
    @Override
    public void onFinish(Scan scan) throws IOException {
        setEndTimestampAttibute(scan);
        resetScanPath(scan);
    }    

    //Set common attributes as expected by DataManager (can be ommited).
    protected void setStartTimestampAttibute(Scan scan) throws IOException {
        String scanPath = getScanPath(scan);
        if (scanPath != null) {
            getDataManager().setAttribute(scanPath, ATTR_START_TIMESTAMP, System.currentTimeMillis());
        }
    }

    protected void setEndTimestampAttibute(Scan scan) throws IOException {
        String scanPath = getScanPath(scan);
        if (scanPath != null) {
            getDataManager().setAttribute(scanPath, ATTR_END_TIMESTAMP, System.currentTimeMillis());
        }
    }

    protected void setPlotPreferencesAttibutes(Scan scan) throws IOException {
        String scanPath = getScanPath(scan);
        if (scanPath != null) {
            ViewPreference.PlotPreferences pp = Context.getInstance().getPlotPreferences();
            if (pp.enabledPlots != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_ENABLE, pp.enabledPlots.toArray(new String[0]));
            }
            if (pp.autoRange != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_RANGE, new double[]{Double.NaN, Double.NaN});
            }
            if (pp.range != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_RANGE, new double[]{pp.range.min, pp.range.max});
            }
            if (pp.domainAxis != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_DOMAIN, pp.domainAxis);
            }
            if (pp.plotTypes != null) {
                ArrayList<String> list = new ArrayList<>();
                for (String key : pp.plotTypes.keySet()) {
                    list.add(key + "=" + pp.plotTypes.get(key));
                }
                getDataManager().setAttribute(scanPath, ATTR_PLOT_TYPES, String.join(ATTR_PLOT_TYPES_SEPARATOR, list));
            }
        }
    }

    protected void setFileIdAttribute() throws IOException {
        getDataManager().setAttribute("/", ATTR_ID, UUID.randomUUID().toString());
    }    
    
    protected void setLayoutAttribute() throws IOException {
        getDataManager().setAttribute("/", ATTR_LAYOUT, getClass().getName());
    }    
    
    protected void setNameAttribute() throws IOException {
        String name = getDataManager().getExecutionPars().getName();
        getDataManager().setAttribute("/", ATTR_NAME, (name == null) ? "" : name);
    }            

    protected void setScriptFileAttibute() throws IOException {
        ExecutionParameters pars =  getDataManager().getExecutionPars();
        File file = pars.getScriptFile();
        if (file != null) {
            String fileName = file.getPath();
            getDataManager().setAttribute("/", ATTR_FILE, fileName);
        } else {
            String command = pars.getStatement();
            if (command!=null){
                getDataManager().setAttribute("/", ATTR_COMMAND, command);
            }
        }
    }

    protected void setScriptVersionAttibute() throws IOException {
        String version = getDataManager().getExecutionPars().getScriptVersion();
        if (version != null) {
            getDataManager().setAttribute("/", ATTR_VERSION, version);
        }
    }
    
    public String getScanPath(Scan scan) {
        synchronized(scanFiles){
            if (scanFiles.containsKey(scan)){
                return  scanFiles.get(scan);
            }
            //Map cleanup
            //for (Scan s : scanFiles.keySet().toArray(new Scan[0])) {
            //    if (s.isCompleted()) {
            //        scanFiles.remove(s);
            //    }
            //}
            String file =  getScanPathName(scan);
            scanFiles.put(scan, file);           
            return file;
        }          
    }
    
    //Storing scan file names
    HashMap<Scan, String> scanFiles = new HashMap<>();
    
    @Override
    public void resetScanPath(Scan scan){
        synchronized(scanFiles){
            scanFiles.remove(scan);
        }        
    }
}
