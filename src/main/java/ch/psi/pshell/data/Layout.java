package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.python.bouncycastle.util.Arrays;


/**
 * Layout implementations define the structure of the acquired data on file.
 */
public interface Layout {
    //Common attributes
    public static final String ATTR_NAME = "Name";
    public static final String ATTR_FILE = "File";
    public static final String ATTR_VERSION = "Version";
    public static final String ATTR_COMMAND = "Command";
    public static final String ATTR_LAYOUT = "Layout";
    public static final String ATTR_SCRIPT = "Script";
    public static final String ATTR_ID = "ID";

    public static final String ATTR_START_TIMESTAMP = "Start";
    public static final String ATTR_END_TIMESTAMP = "End";
    public static final String ATTR_CALIBRATION = "Calibration";
    public static final String ATTR_ERROR_VECTOR = "Error Vector";
    public static final String ATTR_PLOT_ENABLE = "PlotEnable";
    public static final String ATTR_PLOT_RANGE = "PlotRange";
    public static final String ATTR_PLOT_DOMAIN = "PlotDomain";
    public static final String ATTR_PLOT_TYPES = "PlotTypes";
    public static final String ATTR_PLOT_TYPES_SEPARATOR = "; ";

    void initialize();

    String getDefaultGroup(Scan scan);

    default String getCurrentGroup(Scan scan) {
        String ret;
        Object group = getDataManager().getExecutionPars().getOption("group");
        if ((group != null) && (!group.toString().isEmpty())) {
            ret = Context.getInstance().getSetup().expandPath(group.toString());
        } else {
            ret = getDefaultGroup(scan);
        }
        if (!ret.endsWith("/")) {
            ret = ret + "/";
        }
        return ret;
    }

    /**
     * Must be redefined of scan is not contained in a private group
     */
    default String getScanPathName(Scan scan) {
        return getCurrentGroup(scan);
    }

    default String getMonitorsPathName(Scan scan) {
        return getCurrentGroup(scan) + getMonitorsPath();
    }

    default String getMonitorPathName(Scan scan, Device dev) {
        return getMonitorsPathName(scan)+ dev.getAlias();
    }
        
    default String getSnapsPathName(Scan scan) {
        return getCurrentGroup(scan) + getSnapsPath();
    }

    default String getSnapPathName(Scan scan, String name) {
        return Layout.this.getSnapsPathName(scan)+ name;
    }    

    default String getSnapPathName(Scan scan, Readable snap) {
        return Layout.this.getSnapPathName(scan, scan.getReadableName(snap));
    }
    
    default String getDiagsPathName(Scan scan) {
        return getCurrentGroup(scan) + getDiagsPath();
    }

    default String getDiagPathName(Scan scan, String name) {
        return getDiagsPathName(scan)+ name;
    }        
    
    default String getDiagPathName(Scan scan, Readable snap) {
        return getDiagPathName(scan, scan.getReadableName(snap));
    }    
    
    default void onOpened(File output) throws IOException {
    }

    default void onClosed(File output) throws IOException {
    }

    void onStart(Scan scan) throws IOException;

    void onRecord(Scan scan, ScanRecord record) throws IOException;


    default void onInitSnaps(Scan scan) throws IOException{
        for (Readable readable : scan.getSnaps()){
            try{
                String path = getSnapPathName(scan, readable);
                Object value = readable.read();
                getDataManager().setDataset(path, value);
            } catch (Exception ex){        
                String msg = "Error creating snap dataset for " + readable.getName()+ ": " + ex.getMessage();
                appendLog(msg);
                Logger.getLogger(Layout.class.getName()).warning(msg);
            }
        }  
    }   
    
    default Object getSnap(Scan scan, String name, DataManager dm) {
        try{
            DataManager.DataAddress scanPath = DataManager.getAddress(scan.getPath());
            String path = Layout.this.getSnapPathName(scan, name);
            dm = (dm == null) ? getDataManager() : dm;
            Object sliceData = getDataManager().getData(scanPath.root, path).sliceData;  
            return sliceData;
        } catch (Exception ex){    
            Logger.getLogger(Layout.class.getName()).log(Level.WARNING, null, ex);
        }            
        return null;
    }
    
    default void onInitDiags(Scan scan) throws IOException{
        for (Readable diag : scan.getDiags()){
            try{
                String path = getDiagPathName(scan, diag);
                Object value = diag.read();
                Class type =  value.getClass();
                
                if (type.isArray()){
                    type =  Arr.getComponentType(value) ;
                    if (type.isPrimitive()) {
                        type =  Convert.getWrapperClass(type);
                    }
                    int[] shape = Arr.getShape(value);
                    shape = Arrays.copyOf(shape, shape.length + 1);                
                    getDataManager().createDataset(path, type, shape);      
                } else {    
                    getDataManager().createDataset(path, type);      
                }
            } catch (Exception ex){        
                String msg = "Error creating diag dataset for " + diag.getName()+ ": " + ex.getMessage();
                appendLog(msg);
                Logger.getLogger(Layout.class.getName()).warning(msg);
            }
        }  
    }   
    
    default void onDiags(Scan scan) throws IOException{
        for (Readable diag : scan.getDiags()){
            try{
                String path = getDiagPathName(scan, diag);
                Object value = diag.read();
                getDataManager().appendItem(path, value);
            } catch (Exception ex){        
                String msg = "Error adding to diag dataset for " + scan.getReadableName(diag) + ": " + ex.getMessage();
                Logger.getLogger(Layout.class.getName()).finer(msg);
            }                    
        }
    }        
    
    default Object getDiag(Scan scan, String name, DataManager dm) {
        try{
            DataManager.DataAddress scanPath = DataManager.getAddress(scan.getPath());
            String path = getDiagPathName(scan, name);
            dm = (dm == null) ? getDataManager() : dm;
            return getDataManager().getData(scanPath.root, path).sliceData;          
        } catch (Exception ex){    
            Logger.getLogger(Layout.class.getName()).log(Level.WARNING, null, ex);
        }            
        return null;
    }
        
    default void onInitMonitors(Scan scan) throws IOException{
        for (Device dev : scan.getMonitors()){
            try{
                String path = getMonitorPathName(scan, dev);
                dev.update();
                TimestampedValue v = dev.takeTimestamped();
                Class type =  v.getValue().getClass();
                getDataManager().createDataset(path, new String[]{"timestamp", "value"}, new Class[]{Long.class, type}, 
                        new int[]{0,(type.isArray()) ? Array.getLength(v.getValue()): 0});
                onMonitor(scan, dev, v.getValue(), v.getTimestamp());
            } catch (Exception ex){        
                String msg = "Error creating monitor dataset for " + dev.getAlias() + ": " + ex.getMessage();
                appendLog(msg);
                Logger.getLogger(Layout.class.getName()).warning(msg);
            }
        }  
    }       

    
    default void onMonitor(Scan scan, Device dev, Object value, long timestamp) throws IOException{
        try{
            String path = getMonitorPathName(scan, dev);
            getDataManager().appendItem(path, new Object[]{timestamp, value});
        } catch (Exception ex){    
            String msg = "Error adding to monitor dataset for " + dev.getAlias() + ": " + ex.getMessage();
            Logger.getLogger(Layout.class.getName()).finer(msg);
        }
    }
    
    default Object getMonitor(Scan scan, String device, DataManager dm) {
        try{
            DataManager.DataAddress scanPath = DataManager.getAddress(scan.getPath());
            Device dev = scan.getMonitors()[scan.getMonitorIndex(device)];
            String path = getMonitorPathName(scan, dev);
            dm = (dm == null) ? getDataManager() : dm;
            Object sliceData = getDataManager().getData(scanPath.root, path).sliceData;  
            try{
                Object[][] data = (Object[][]) sliceData;  
                if (data.length>0){
                    long[] timestamps = new long[data.length];
                    Object[] record = (Object[]) data[0];
                    Object values = Array.newInstance(record[1].getClass(), data.length);
                    for (int i=0;i< data.length; i++){
                        timestamps[i] = (Long)data[i][0];
                        Array.set(values, i, data[i][1]);
                    }
                    List ret = new ArrayList();
                    ret.add(Arr.toList(timestamps));
                    ret.add(Arr.toList(values));
                    return ret;
                }
            } catch (Exception ex){
                return sliceData;
            }            
        } catch (Exception ex){    
            Logger.getLogger(Layout.class.getName()).log(Level.WARNING, null, ex);
        }            
        return null;
    }
    
    default long[] getTimestamps(Scan scan, DataManager dm) {
        try{
            DataManager.DataAddress scanPath = DataManager.getAddress(scan.getPath());                    
            String timestamps = getTimestampsDataset(scan.getPath());
            if (timestamps != null) {
                return (long[]) Convert.toPrimitiveArray(Context.getInstance().getDataManager().getData(timestamps).sliceData, Long.class);
            }    
        } catch (Exception ex){    
            Logger.getLogger(Layout.class.getName()).log(Level.WARNING, null, ex);
        }            
        return null;
    }
    
    void onFinish(Scan scan) throws IOException;


    default List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        //Uses default data manager plot parsing
        return null;
    }

    default Object getData(Scan scan, String device, DataManager dm) {
        return null;
    }

    //returns true if the path belongs to a scan
    boolean isScanDataset(String root, String path, DataManager dm);

    default DataManager getDataManager() {
        return Context.getInstance().getDataManager();
    }

    default boolean getPreserveTypes() {
        return getDataManager().getPreserveTypes();
    }

    default Class getDatasetType(ch.psi.pshell.device.Readable device) throws IOException {
        return getDataManager().getScanDatasetType(device);
    }

    default int getIndex(Scan scan, ScanRecord record) throws IOException {
        return record.getIndex() - scan.getRecordIndexOffset();
    }

    default public String getId() {
        return getClass().getName();
    }

    default public void appendLog(String log) throws IOException {
    }

    default public void saveScript(String name, String contents) throws IOException {
    }
    
    default public void writeSessionMetadata() throws IOException {
    }    

    String getScanPath(Scan scan);

    void resetScanPath(Scan scan);

    default public String getTimestampsDataset(String scanPath) {
        return null;
    }

    default public boolean getCreateLogs() {
        return true;
    }

    String getLogsPath();

    String getScriptsPath();

    String getLogFilePath();

    String getMonitorsPath();
    
    String getSnapsPath();

    String getDiagsPath();

    String getOutputFilePath();
}
