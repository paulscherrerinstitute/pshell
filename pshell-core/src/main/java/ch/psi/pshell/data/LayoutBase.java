package ch.psi.pshell.data;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ReadonlyRegister;
import ch.psi.pshell.devices.InlineDevice;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.ExecutionParameters;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.IO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common layouts utilities
 */
public abstract class LayoutBase implements Layout {
    //Default groups
    public static String PATH_META = "meta/";   
    public static String PATH_MONITORS = "monitors/";
    public static String PATH_SNAPS = "snapshots/";
    public static String PATH_DIAGS = "diagnostics/";
    public static String PATH_LOGS = "logs/";   
    
    public static final String SETPOINTS_DATASET_SUFFIX = "_setpoint";

    boolean writeSessionMetadata=true;
    String sessionMetadataPath="/";
    boolean sessionMetadataAttributes=true;

    boolean saveSource = false;

    public boolean getSaveSource() {
        return saveSource;
    }

    public void setSaveSource(boolean value) {
        saveSource = value;
    }    
    
    public void setWriteSessionMetadata(boolean value, String path, boolean attributes) {
        writeSessionMetadata = value;
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
    
    public String getSourceFilePath() {
        return getLogsPath() + "source/";
    }        
    
    protected String checkLogFile() throws IOException{        
        DataManager dataManager = getDataManager();
        String logFile = getLogFilePath();
        if (logFile != null) {
            logFile=dataManager.adjustPath(logFile);
            if (!dataManager.exists(logFile)) {
                dataManager.createDataset(logFile, String.class);
                dataManager.getFormat().checkLogFile(logFile);
            }                 
        }
        return logFile;
    }
    
    @Override
    public void appendLog(String log) throws IOException {        
        if (getCreateLogs()){
            String logFile = checkLogFile();
            if (logFile!=null){
                DataManager dataManager = getDataManager();
                String time = Chrono.getTimeStr(System.currentTimeMillis(), "dd/MM/YY HH:mm:ss.SSS - ");
                dataManager.appendItem(logFile, time + log);
            }
        }
    }

    public void saveFile(File file, String destination) throws IOException {
        String contents = new String(Files.readAllBytes(file.toPath()));
        DataManager dataManager = getDataManager();
        if (destination != null) {
            if (destination.endsWith("/")){
                destination += file.getName();
            }
            if (dataManager.getFormat() instanceof FormatText providerText){
                Path path = providerText.getFilePath(destination, false);
                path.toFile().getParentFile().mkdirs();
                Files.writeString(path, contents);
                IO.setFilePermissions(path.toFile(), Context.getDataFilePermissions());
            } else {
                dataManager.setDataset(destination, contents);
            }
        }
    }    

    protected void writeSessionMetadata(Map<String, Object> metadata, String location, boolean attributes) throws IOException {
        if (location == null) {
            location = "/";
        }        
        for (String key : metadata.keySet()) {
            Object value = metadata.get(key);
            if (value != null) {
                if (value instanceof List list) {
                    value = list.toArray(new String[0]);
                }
                try {
                    if (attributes) {
                        getDataManager().setAttribute(location, key, value);
                    } else {
                        getDataManager().setDataset(location + "/" + key, value);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(LayoutBase.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
        
    }
    
    @Override
    public void writeSessionMetadata() throws IOException {
        if (writeSessionMetadata){
            try{
                Map<String, Object> metadata = Context.getSessionMetadata();                
                if (metadata!=null){
                    writeSessionMetadata(metadata, sessionMetadataPath, sessionMetadataAttributes);
                }
            } catch (Exception ex) {
                 Logger.getLogger(LayoutBase.class.getName()).log(Level.WARNING, null, ex);
            }
        }
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
            getDataManager().setAttribute(getScanPathName(scan), ATTR_TYPE, scan.getType());  
        }
    }   

    protected void setPlotPreferencesAttibutes(Scan scan) throws IOException {
        String scanPath = getScanPath(scan);
        if (scanPath != null) {
            ViewPreference.PlotPreferences pp = Context.getExecutionPars().getPlotPreferences();
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
            getDataManager().setAttribute("/", ATTR_SOURCE_NAME, fileName);
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
            getDataManager().setAttribute("/", ATTR_SOURCE_REVISION, version);
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
        if (dev instanceof Device device){    
            try{
                String desc = device.getDescription();
                if ((desc!=null)&&(!desc.isBlank())){
                    getDataManager().setAttribute(dataset, ATTR_DEVICE_DESC, desc.trim());
                }       
            } catch (Exception ex){
            }  
            try{
                String channel = InlineDevice.getChannelName(device);
                if (channel!=null){
                    getDataManager().setAttribute(dataset, ATTR_DEVICE_CHANNEL, channel);
                }      
            } catch (Exception ex){
            } 
            if (dev instanceof ReadonlyRegister reg){
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
 