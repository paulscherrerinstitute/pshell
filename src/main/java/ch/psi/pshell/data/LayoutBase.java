package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ExecutionParameters;
import ch.psi.pshell.core.InlineDevice;
import static ch.psi.pshell.data.Layout.ATTR_DEVICE_CHANNEL;
import static ch.psi.pshell.data.Layout.ATTR_DEVICE_DESC;
import static ch.psi.pshell.data.Layout.ATTR_DEVICE_UNIT;
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
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ReadonlyRegister;
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
    //Default groups
    public static String PATH_MONITORS = "monitors/";
    public static String PATH_SNAPS = "snaps/";
    public static String PATH_DIAGS = "diags/";
    public static String PATH_LOGS = "logs/";   
    public static String PATH_META = "meta/";   

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
    public String getMetaPath(){
        return PATH_META;        
    }    
    
    @Override
    public String getMonitorsPath(){
        return PATH_MONITORS;        
    }    
    
    @Override
    public String getSnapsPath(){
        return PATH_SNAPS;        
    }    

    @Override
    public String getDiagsPath(){
        return PATH_DIAGS;        
    }           
    
    @Override
    public String getLogsPath() {
        return PATH_LOGS;
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
        setScanType(scan);
    }
    
    @Override
    public void onFinish(Scan scan) throws IOException {
        try{
            setEndTimestampAttibute(scan);
        } finally{
            resetScanPath(scan);
        }
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
    
    protected void setScanType(Scan scan) throws IOException {
        String scanPath = getScanPath(scan);
        if (scanPath != null) {
            getDataManager().setAttribute(getScanPathName(scan), ATTR_TYPE, scan.getClass().getSimpleName());  
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
            if (pp.rangeY != null) {
                getDataManager().setAttribute(scanPath, ATTR_PLOT_RANGE_Y, new double[]{pp.rangeY.min, pp.rangeY.max});
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
                return scanFiles.get(scan);
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
    
    
    public void writeDeviceMetadataAttrs(String dataset, Object dev) throws IOException{
        if (dev instanceof Device){    
            try{
                String desc = ((Device)dev).getDescription();
                if ((desc!=null)&&(!desc.isBlank())){
                    getDataManager().setAttribute(dataset, ATTR_DEVICE_DESC, desc.trim());
                }       
            } catch (Exception ex){
            }  
            try{
                String channel = InlineDevice.getChannelName((Device)dev);
                if (channel!=null){
                    getDataManager().setAttribute(dataset, ATTR_DEVICE_CHANNEL, channel);
                }      
            } catch (Exception ex){
            } 
            if (dev instanceof ReadonlyRegister){
                ReadonlyRegister reg = (ReadonlyRegister)dev;
                try{
                    String unit = reg.getUnit();
                    if((unit!=null)&&(!unit.isBlank())){
                        getDataManager().setAttribute(dataset, ATTR_DEVICE_UNIT, unit.trim());
                    }                
                } catch (Exception ex){
                }          
                try{
                    int precision = reg.getPrecision();
                    if (precision!=ReadonlyRegister.UNDEFINED_PRECISION){
                        getDataManager().setAttribute(dataset, ATTR_DEVICE_PREC, precision);
                    }       
                } catch (Exception ex){
                }  
                try{
                    int[] shape = reg.getShape();
                    if (shape!=ReadonlyRegister.UNDEFINED_SHAPE){
                        getDataManager().setAttribute(dataset, ATTR_DEVICE_SHAPE, shape);
                    }       
                } catch (Exception ex){
                }                  
            }
        }
    }    
}
 