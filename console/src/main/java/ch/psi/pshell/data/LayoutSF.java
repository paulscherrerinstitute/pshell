package ch.psi.pshell.data;

import ch.psi.pshell.bs.StreamDevice;
import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Writable.WritableArray;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.scan.AreaScan;
import ch.psi.pshell.scan.BinarySearch;
import ch.psi.pshell.scan.BsScan;
import ch.psi.pshell.scan.ContinuousScan;
import ch.psi.pshell.scan.HardwareScan;
import ch.psi.pshell.scan.HillClimbingSearch;
import ch.psi.pshell.scan.LineScan;
import ch.psi.pshell.scan.MonitorScan;
import ch.psi.pshell.scan.RegionScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scan.Search;
import ch.psi.pshell.scan.StripScan;
import ch.psi.pshell.scan.TimeScan;
import ch.psi.pshell.scan.VectorScan;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This data layout stores each positioner and sensor as an individual dataset
 */
public class LayoutSF extends LayoutBase {

    public static final String ATTR_GROUP_GENERAL = "general/";
    public static final String ATTR_GROUP_EXPERIMENT = "experiment/";
    public static final String ATTR_GROUP_LOGS = "logs/";

    public static final String ATTR_DATASET_CREATED = ATTR_GROUP_GENERAL + "created";
    public static final String ATTR_DATASET_CLOSED = ATTR_GROUP_GENERAL + "closed";
    public static final String ATTR_DATASET_USER = ATTR_GROUP_GENERAL + "user";
    public static final String ATTR_DATASET_PROCESS = ATTR_GROUP_GENERAL + "process";
    public static final String ATTR_DATASET_INSTRUMENT = ATTR_GROUP_GENERAL + "instrument";
    public static final String ATTR_DATASET_AUTHOR = ATTR_GROUP_GENERAL + "author";
    public static final String ATTR_DATASET_COMMAND = ATTR_GROUP_GENERAL + "command";
    public static final String ATTR_DATASET_PARAMETERS = ATTR_GROUP_GENERAL + "parameters";
    public static final String ATTR_DATASET_APP_NAME= ATTR_GROUP_GENERAL + "application";
    public static final String ATTR_DATASET_APP_VERSION = ATTR_GROUP_GENERAL + "version";

    public static final String ATTR_DATASET_LOG_DESCRIPTION = ATTR_GROUP_LOGS + "description";
    public static final String ATTR_DATASET_LOG_TIMESTAMP = ATTR_GROUP_LOGS + "timestamp";

    public static final String ATTR_GROUP_METHOD = "method/";
    public static final String ATTR_GROUP_DATA = "data/";
    public static final String ATTR_GROUP_PROCESSED = "processed/";
    public static final String ATTR_GROUP_META = "meta/";

    public static final String ATTR_DATASET_TYPE = ATTR_GROUP_METHOD + "type";
    public static final String ATTR_DATASET_VERSION = ATTR_GROUP_METHOD + "version";
    public static final String ATTR_DATASET_SENSORS = ATTR_GROUP_METHOD + "sensors";
    public static final String ATTR_DATASET_ACTUATORS = ATTR_GROUP_METHOD + "actuators";
    public static final String ATTR_DATASET_SAMPLES = ATTR_GROUP_METHOD + "samples";
    public static final String ATTR_DATASET_STEPS = ATTR_GROUP_METHOD + "steps";
    public static final String ATTR_DATASET_PASSES = ATTR_GROUP_METHOD + "passes";
    public static final String ATTR_DATASET_START = ATTR_GROUP_METHOD + "start";
    public static final String ATTR_DATASET_STOP = ATTR_GROUP_METHOD + "stop";
    public static final String ATTR_DATASET_RECORDS = ATTR_GROUP_METHOD + "records";
    public static final String ATTR_DATASET_DIMENSIONS = ATTR_GROUP_METHOD + "dimensions";
    public static final String ATTR_DATASET_BACKGROND = ATTR_GROUP_METHOD + "background";

    public static final String DATASET_READBACK = "readback";
    public static final String DATASET_SETPOINT = "setpoint";
    public static final String DATASET_VALUE = "value";
    public static final String DATASET_TIMESTAMP = "timestamp";
    public static final String DATASET_PID = "pid";
    public static final String DATASET_GLOBAL_TIMESTAMP = "global_timestamp";
    public static final String DATASET_MIN = "min";
    public static final String DATASET_MAX = "max";
    public static final String DATASET_STDEV = "stdev";


    public static final String METHOD_LSCAN = "Line Scan";
    public static final String METHOD_ASCAN = "Multidimentional Scan";
    public static final String METHOD_TSCAN = "Time Series";
    public static final String METHOD_MSCAN = "Change Event Series";
    public static final String METHOD_RSCAN = "Range";
    public static final String METHOD_VSCAN = "Vector";
    public static final String METHOD_CSCAN = "Continuous";
    public static final String METHOD_HSCAN = "OTF";
    public static final String METHOD_BSCAN = "Stream Series";
    public static final String METHOD_SSCAN = "Strip Series";
    public static final String METHOD_BSEARCH = "Binary Search";
    public static final String METHOD_HSEARCH = "Hill Climbing";
    public static final String METHOD_OPTIMIZATION = "Optimization";

    @Override
    public String getId() {
        return "sf";
    }    

    public static String getMethodName(Scan scan) {
        if (scan instanceof LineScan) {
            return METHOD_LSCAN;
        } else if (scan instanceof AreaScan) {
            return METHOD_ASCAN;
        } else if (scan instanceof TimeScan) {
            return METHOD_TSCAN;
        } else if (scan instanceof MonitorScan) {
            return METHOD_MSCAN;
        } else if (scan instanceof RegionScan) {
            return METHOD_RSCAN;
        } else if (scan instanceof VectorScan) {
            return METHOD_VSCAN;
        } else if (scan instanceof ContinuousScan) {
            return METHOD_CSCAN;
        } else if (scan instanceof HardwareScan) {
            return METHOD_HSCAN;
        } else if (scan instanceof BsScan) {
            return METHOD_BSCAN;
        } else if (scan instanceof StripScan) {
            return METHOD_SSCAN;
        } else if (scan instanceof BinarySearch) {
            return METHOD_BSEARCH;
        } else if (scan instanceof HillClimbingSearch) {
            return METHOD_HSEARCH;
        } else if (scan instanceof Search) {
            return METHOD_OPTIMIZATION;
        }
        return "Unknown";
    }

    String group;

    final DataManager dataManager = getDataManager();

    static List<Readable> experimentArguments = new ArrayList<>();

    public static void setExperimentArguments(List<Readable> args) {
        experimentArguments = args;
    }

    public static List<Readable> getExperimentArguments() {
        return experimentArguments;
    }

    @Override
    public void initialize() {
    }
    
    @Override
    public void onOpened(File output) throws IOException {
        super.onOpened(output);
        dataManager.createGroup(ATTR_GROUP_GENERAL);
        dataManager.createGroup(ATTR_GROUP_EXPERIMENT);
        dataManager.createGroup(ATTR_GROUP_LOGS);
        dataManager.setDataset(ATTR_DATASET_CREATED, new String[]{Chrono.getTimeStr(System.currentTimeMillis(), "dd/MM/YY HH:mm:ss.SSS ")});
        dataManager.setDataset(ATTR_DATASET_USER, new String[]{Sys.getUserName()});
        dataManager.setDataset(ATTR_DATASET_PROCESS, new String[]{Sys.getProcessName()});
        dataManager.setDataset(ATTR_DATASET_APP_NAME, new String[]{Context.getApplicationName()});
        dataManager.setDataset(ATTR_DATASET_APP_VERSION, new String[]{Context.getApplicationVersion()});
        dataManager.setDataset(ATTR_DATASET_INSTRUMENT, new String[]{Context.getInstanceName()});
        dataManager.setDataset(ATTR_DATASET_AUTHOR, new String[]{Sys.getUserName()});
        String script = Context.getExecutionPars().getScript();
        dataManager.setDataset(ATTR_DATASET_COMMAND, new String[]{(script==null) ? String.valueOf(Context.getExecutionPars().getStatement()): script});
        dataManager.setDataset(ATTR_DATASET_PARAMETERS, new String[]{Str.toString(Context.getExecutionPars().getArgs())});
         
        dataManager.createDataset(ATTR_DATASET_LOG_DESCRIPTION, String.class);
        dataManager.createDataset(ATTR_DATASET_LOG_TIMESTAMP, Long.class);
 
        for (Readable r : getExperimentArguments()) {
            try {
                Object val = r.read();
                if (val == null){
                    throw new Exception("Null value [" + r.getName()+ "]");
                }
                if (!val.getClass().isArray()) {
                    Object arr = Array.newInstance(val.getClass(), 1);
                    Array.set(arr, 0, val);
                    val = arr;
                }
                String datasetName =ATTR_GROUP_EXPERIMENT + r.getName();
                dataManager.setDataset(datasetName, val);
                writeDeviceMetadataAttrs(datasetName, r);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                String msg = "Exception reading " + r.getName() + ": " + ex.getMessage();
                dataManager.appendLog(msg);
                Logger.getLogger(LayoutSF.class.getName()).log(Level.WARNING, msg, ex);
            }
        }
    }
    
    @Override
    public void onClosed(File output) throws IOException {
         super.onClosed(output);
         dataManager.setDataset(ATTR_DATASET_CLOSED, new String[]{Chrono.getTimeStr(System.currentTimeMillis(), "dd/MM/YY HH:mm:ss.SSS ")});
    }
    
    @Override
    public void appendLog(String log) throws IOException {
        dataManager.appendItem(ATTR_DATASET_LOG_TIMESTAMP, System.currentTimeMillis());
        dataManager.appendItem(ATTR_DATASET_LOG_DESCRIPTION, log);
    }

    @Override
    public String getDefaultGroup(Scan scan) {
        return scan.getTag();
    }

    StreamDevice getStream(Scan scan) {
        if (scan instanceof BsScan bs) {
            return bs.getStream();
        }
        if (scan instanceof MonitorScan ms) {
            return ms.getStream();
        }
        return null;
    }

    StreamDevice stream;

    @Override
    public void onStart(Scan scan) throws IOException {
        stream = getStream(scan);

        String group = getScanPath(scan);
        dataManager.createGroup(group);
        dataManager.createGroup(group + ATTR_GROUP_METHOD);
        dataManager.createGroup(group + ATTR_GROUP_DATA);
        dataManager.createGroup(group + ATTR_GROUP_PROCESSED);
        dataManager.createGroup(group + ATTR_GROUP_META);

        dataManager.setDataset(group + ATTR_DATASET_TYPE, new String[]{getMethodName(scan)});
        dataManager.setDataset(group + ATTR_DATASET_VERSION, new String[]{Context.getApplicationVersion()});
        dataManager.setDataset(group + ATTR_DATASET_SENSORS, scan.getReadableNames());
        dataManager.setDataset(group + ATTR_DATASET_ACTUATORS, scan.getWritableNames());
        dataManager.setDataset(group + ATTR_DATASET_SAMPLES, new int[]{1});
        dataManager.setDataset(group + ATTR_DATASET_STEPS, scan.getNumberOfSteps());
        dataManager.setDataset(group + ATTR_DATASET_PASSES, new int[]{scan.getNumberOfPasses()});
        dataManager.setDataset(group + ATTR_DATASET_START, scan.getStart());
        dataManager.setDataset(group + ATTR_DATASET_STOP, scan.getEnd());
        dataManager.setDataset(group + ATTR_DATASET_RECORDS, new int[]{scan.getNumberOfRecords()});
        dataManager.setDataset(group + ATTR_DATASET_DIMENSIONS, new int[]{scan.getDimensions()});
        
        Map features = dataManager.getStorageFeatures(null);
        boolean contiguous = dataManager.isStorageFeaturesContiguous(features);
        int samples = contiguous ? scan.getNumberOfRecords() : 0;
        
        for (Writable writable : scan.getWritables()) {
            String name = writable.getAlias();
            String groupDev = getDataPath(scan, name);
            dataManager.createGroup(groupDev);
            if (writable instanceof WritableArray writableArray) {
                dataManager.createDataset(groupDev + DATASET_READBACK, Double.class, new int[]{samples, writableArray.getSize()});
            } else {
                dataManager.createDataset(groupDev + DATASET_READBACK, Double.class, new int[]{samples});
            }
            if (writable instanceof WritableArray writableArray) {
                dataManager.createDataset(groupDev + DATASET_SETPOINT, Double.class, new int[]{samples, writableArray.getSize()});
            } else {
                dataManager.createDataset(groupDev + DATASET_SETPOINT, Double.class, new int[]{samples});
            }
            writeDeviceMetadataAttrs(groupDev, writable);    
        }
        
        dataManager.createDataset(group + DATASET_TIMESTAMP, Long.class, new int[]{samples});
        if (stream != null) {
            dataManager.createDataset(group + DATASET_PID, Long.class, new int[]{samples});
            dataManager.createDataset(group + DATASET_GLOBAL_TIMESTAMP, Long.class, new int[]{samples});
        }        

        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = readable.getAlias();
            String groupDev = getDataPath(scan, name);
            dataManager.createDataset(groupDev + DATASET_VALUE, scan, readable);
            if (readable instanceof ReadableMatrix) {                             
                if (readable instanceof ReadableCalibratedMatrix readableCalibratedMatrix) {
                    MatrixCalibration cal = readableCalibratedMatrix.getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(groupDev, ATTR_CALIBRATION, new double[]{cal.scaleX, cal.scaleY, cal.offsetX, cal.offsetY});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else if (readable instanceof ReadableArray) {
                if (readable instanceof ReadableCalibratedArray readableCalibratedArray) {
                    ArrayCalibration cal = readableCalibratedArray.getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(groupDev, ATTR_CALIBRATION, new double[]{cal.scale, cal.offset});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else {
                if (Averager.isAverager(readable)) {
                    dataManager.createDataset(groupDev + DATASET_MIN, Double.class, new int[]{samples});
                    dataManager.createDataset(groupDev + DATASET_MAX, Double.class, new int[]{samples});
                    dataManager.createDataset(groupDev + DATASET_STDEV, Double.class, new int[]{samples});
                }
            }
            writeDeviceMetadataAttrs(groupDev, readable);              
            dataManager.createDataset(groupDev + DATASET_TIMESTAMP, Long.class, new int[]{samples});
        }

        setStartTimestampAttibute(scan);
        setPlotPreferencesAttibutes(scan);
    }

    @Override
    public void onRecord(Scan scan, ScanRecord record) throws IOException {
        String group = getScanPath(scan);
        Number[] positions = record.getPositions();
        Object[] values = record.getReadables();
        Long[] deviceTimestamps = record.getDeviceTimestamps();
        int index = getIndex(scan, record);
        int deviceIndex = 0;

        if (stream != null) {
            dataManager.setItem(group + DATASET_PID, record.getId(), index);
            dataManager.setItem(group + DATASET_GLOBAL_TIMESTAMP, record.getRemoteTimestamp(), index);
        }

        for (Writable writable : scan.getWritables()) {
            String path = getDataPath(scan, writable.getAlias());
            dataManager.setItem(path + DATASET_SETPOINT, record.getSetpoints()[deviceIndex], index);
            dataManager.setItem(path + DATASET_READBACK, positions[deviceIndex++], index);
        }
        deviceIndex = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = readable.getAlias();
            String path = getDataPath(scan, name);
            Long timestamp = deviceTimestamps[deviceIndex];
            Object value = values[deviceIndex++];
            dataManager.setItem(path + DATASET_VALUE, value, index);
            if (Averager.isAverager(readable)) {
                DescStatsDouble v = (DescStatsDouble) value;
                dataManager.setItem(path + DATASET_MIN, (v == null) ? null : v.getMin(), index);
                dataManager.setItem(path + DATASET_MAX, (v == null) ? null : v.getMax(), index);
                dataManager.setItem(path + DATASET_STDEV, (v == null) ? null : v.getStdev(), index);
            }
            dataManager.setItem(path + DATASET_VALUE, value, index);
            dataManager.setItem(path + DATASET_TIMESTAMP, (timestamp == null) ? 0 : timestamp, index);
        }
        dataManager.setItem(group + DATASET_TIMESTAMP, record.getLocalTimestamp(), index);
    }

    @Override
    public void onFinish(Scan scan) throws IOException {
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            if (Averager.isAverager(readable)) {
                try {
                    getDataManager().flush();
                    String name = readable.getAlias();
                    String path = getDataPath(scan, name);
                    //Not using error vector, but stde dataset 
                    //double[] stdev = (double[]) getDataManager().getData(path + DATASET_STDEV).sliceData;                    
                    //getDataManager().setAttribute(path + DATASET_VALUE, ATTR_ERROR_VECTOR, stdev);
                } catch (Exception ex) {
                    Logger.getLogger(LayoutSF.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
        super.onFinish(scan);
    }

    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        dm = (dm == null) ? getDataManager() : dm;
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        Map<String, Object> info = dm.getInfo(root, path);
        if ((String.valueOf(info.get(Format.INFO_TYPE)).equals(Format.INFO_VAL_TYPE_GROUP)) && dm.isGroup(root, path + ATTR_GROUP_METHOD)) {
            ArrayList<PlotDescriptor> ret = new ArrayList<>();
            double[] scanDimX = null;
            double[] scanDimY = null;
            double[] scanDimZ = null;   
            String[] readables = null;
            String[] writables = null;
                
            try {
                readables = (String[]) dm.getData(root, path + ATTR_DATASET_SENSORS).sliceData;
            } catch (Exception ex) {
            }
            try {
                writables = (String[]) dm.getData(root, path + ATTR_DATASET_ACTUATORS).sliceData;
            } catch (Exception ex) {
            }
            int dims = 1;
            try {
                dims = ((int[]) dm.getData(root, path + ATTR_DATASET_DIMENSIONS).sliceData)[0];
            } catch (Exception ex) {
            }              
            Object steps = null;
            try {
                steps = dm.getData(root, path + ATTR_DATASET_STEPS).sliceData;
            } catch (Exception ex) {
            }     
            String[] children = dm.getChildren(root, path + ATTR_GROUP_DATA);
            
            if (writables != null){
                for (String child : ch.psi.pshell.utils.Arr.copy(children)) {
                    String name = child.substring(child.lastIndexOf("/") + 1);
                    Integer index = null;
                    for (int i = 0; i < writables.length; i++) {
                        if (name.equals(writables[i])) {
                            index = i;
                            break;
                        }
                    }
                    if (index!=null){
                        int dim = ((dims > 1)&&(index!=null)) ? index+1 : 1;
                        DataSlice data = dm.getData(root, child + "/" + DATASET_READBACK);
                        if (data.sliceData instanceof double[] slice) {
                            switch ((Integer) dim) {
                                case 1:
                                    if ((index != null) && (index > 0)) {
                                        break;

                                    }
                                    scanDimX = slice;
                                    break;
                                case 2:
                                    scanDimY = slice;
                                    break;
                                case 3:
                                    scanDimZ = slice;
                                    break;

                            }
                        }
                        children = Arr.removeEquals(children, child);
                    }
                }
            } 
            for (String child : children) {
                List<PlotDescriptor> descriptors = dm.getPlots(root, child + "/" + DATASET_VALUE);
                for (PlotDescriptor descriptor : descriptors) {
                    descriptor.name = child.substring(child.lastIndexOf("/") + 1);
                    //1D plot of 2D images 
                    if (descriptor.rank == 3) {
                        if (scanDimX != null) {
                            descriptor.z = scanDimX;
                        }
                    } else {
                        if (scanDimX != null) {
                            descriptor.x = scanDimX;
                        }
                        if (scanDimY != null) {
                            //Propagate dimension if to override it with scan dim
                            if (descriptor.z == null) {
                                descriptor.z = descriptor.y;
                            }
                            descriptor.y = scanDimY;
                        }
                        if (scanDimZ != null) {
                            descriptor.z = scanDimZ;
                        }
                    }

                    if (steps instanceof int[] intSteps) {
                        descriptor.steps = intSteps;
                    }

                    try {
                        if (descriptor.error == null) {
                            if (dm.isDataset(root, child + "/" + DATASET_STDEV)){
                                DataSlice data = dm.getData(root, child + "/" + DATASET_STDEV);
                                descriptor.error = (double[]) data.sliceData;
                            }
                        }
                    } catch (Exception ex) {
                    }
                    descriptor.labelX = ((writables != null) && (writables.length > 0)) ? writables[0] : null;
                }
                ret.addAll(descriptors);
            }
            if (ret != null) {
                //Ordering
                ArrayList<PlotDescriptor> readableDescriptors = new ArrayList<>();
                if (readables != null) {
                    for (String readable : readables) {
                        for (PlotDescriptor plot : ret.toArray(new PlotDescriptor[0])) {
                            if (plot.name.equals(readable)) {
                                readableDescriptors.add(plot);
                                break;
                            }
                        }
                    }
                    ret.removeAll(readableDescriptors);
                    ret.addAll(readableDescriptors);
                }
            }
            return ret;
        }

        //Uses default data manager plot parsing
        return null;
    }


    @Override
    public Object getData(Scan scan, String device, DataManager dm) {
        dm = (dm == null) ? getDataManager() : dm;
        DataAddress scanPath = DataManager.getAddress(scan.getPath());
        Object ret = null;
        try {
            ret = dm.getData(scanPath.root, getDataPath(scan, device)+DATASET_VALUE).sliceData;
        } catch (IOException ex) {
            try {
                ret = device.endsWith(LayoutDefault.SETPOINTS_DATASET_SUFFIX) ?
                        dm.getData(scanPath.root, getDataPath(scan, device.substring(0, device.length()-LayoutDefault.SETPOINTS_DATASET_SUFFIX.length()))+DATASET_SETPOINT).sliceData :
                        dm.getData(scanPath.root, getDataPath(scan, device)+DATASET_READBACK).sliceData;
            } catch (IOException e) {
            }
        }
        return ret;
    }

    @Override
    public boolean isScanDataset(String root, String path, DataManager dm) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            String[] tokens = path.split("/");
            if (Arr.containsEqual(new String[]{DATASET_READBACK, DATASET_SETPOINT, DATASET_VALUE}, tokens[3])
                    && tokens[1].equals(ATTR_GROUP_DATA.substring(0, ATTR_GROUP_DATA.length() - 1))) {
                return true;
            }
        } catch (Exception ex) {

        }
        return false;
    }
    
    @Override
    public String getTimestampsDataset(String scanPath){
        return scanPath + "/" + DATASET_TIMESTAMP;
    }    

    public String getDataPath(Scan scan) {
        return getScanPath(scan) + ATTR_GROUP_DATA;
    }

    public String getMetaPath(Scan scan) {
        return getScanPath(scan) + ATTR_GROUP_META;
    }

    public String getProcessedPath(Scan scan) {
        return getScanPath(scan) + ATTR_GROUP_PROCESSED;
    }

    public String getMethodPath(Scan scan) {
        return getScanPath(scan) + ATTR_GROUP_METHOD;
    }

    protected String getDataPath(Scan scan, String device) {
        if (device == null) {
            device = "data";
        }
        return getDataPath(scan) + device + "/";
    }

    protected String getMetaPath(Scan scan, String device) {
        if (device == null) {
            device = "data";
        }
        return getMetaPath(scan) + device + "/";
    }

    protected String getProcessedPath(Scan scan, String device) {
        if (device == null) {
            device = "data";
        }
        return getProcessedPath(scan) + device + "/";
    }
}
