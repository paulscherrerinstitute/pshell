package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.pshell.scan.Otf;
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
        return getCurrentGroup(scan) + "monitors/";
    }

    default String getMonitorPathName(Scan scan, Device dev) {
        return getMonitorsPathName(scan)+ dev.getAlias();
    }
    default void onOpened(File output) throws IOException {
    }

    default void onClosed(File output) throws IOException {
    }

    void onStart(Scan scan) throws IOException;

    void onRecord(Scan scan, ScanRecord record) throws IOException;


    default void onInitMonitors(Scan scan) throws IOException{
        for (Device dev : scan.getMonitorDevices()){
            try{
                String path = getMonitorPathName(scan, dev);
                dev.update();
                TimestampedValue v = dev.takeTimestamped();
                Class type =  v.getValue().getClass();
                getDataManager().createDataset(path, new String[]{"timestamp", "value"}, new Class[]{Long.class, type}, 
                        new int[]{0,(type.isArray()) ? Array.getLength(v.getValue()): 0});
                onMonitor(scan, dev, v.getValue(), v.getTimestamp());
            } catch (Exception ex){        
                appendLog("Error creating monitor dataset: " + dev.getAlias());
                Logger.getLogger(Layout.class.getName()).log(Level.WARNING, null, ex);
            }
        }  
    }

    
    default void onMonitor(Scan scan, Device dev, Object value, long timestamp) throws IOException{
        try{
            String path = getMonitorPathName(scan, dev);
            getDataManager().appendItem(path, new Object[]{timestamp, value});
        } catch (Exception ex){    
            Logger.getLogger(Layout.class.getName()).log(Level.FINE, null, ex);
        }
    }
    
    default Object getMonitor(Scan scan, String device, DataManager dm) {
        try{
            DataManager.DataAddress scanPath = DataManager.getAddress(scan.getPath());
            Device dev = scan.getMonitorDevices()[scan.getMonitorIndex(device)];
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

    String getOutputFilePath();


}
