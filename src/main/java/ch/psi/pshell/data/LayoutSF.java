package ch.psi.pshell.data;

import ch.psi.pshell.bs.BsScan;
import ch.psi.pshell.bs.Stream;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.InlineDevice;
import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.ReadonlyProcessVariableConfig;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Writable.WritableArray;
import ch.psi.pshell.scan.AreaScan;
import ch.psi.pshell.scan.BinarySearch;
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
import ch.psi.pshell.ui.App;
import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Config;
import ch.psi.utils.Convert;
import ch.psi.utils.Sys;
import ch.psi.utils.Str;
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
public class LayoutSF extends LayoutBase implements Layout {

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
    public static final String ATTR_DEVICE_CHANNEL = "channel";   
    public static final String ATTR_DEVICE_UNIT= "unit";  

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

    final static Context context = Context.getInstance();
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
        dataManager.setDataset(ATTR_DATASET_APP_NAME, new String[]{App.getApplicationName()});
        dataManager.setDataset(ATTR_DATASET_APP_VERSION, new String[]{App.getApplicationVersion()});
        dataManager.setDataset(ATTR_DATASET_INSTRUMENT, new String[]{(context == null) ? "" : context.getConfig().getName()});
        dataManager.setDataset(ATTR_DATASET_AUTHOR, new String[]{context.getUser().name});
        String script = dataManager.getExecutionPars().getScript();
        dataManager.setDataset(ATTR_DATASET_COMMAND, new String[]{(script==null) ? String.valueOf(dataManager.getExecutionPars().getStatement()): script});
        dataManager.setDataset(ATTR_DATASET_PARAMETERS, new String[]{Str.toString(dataManager.getExecutionPars().getArgs())});
         
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
                if (r instanceof Device){
                    Config c = ((Device)r).getConfig();
                    if ((c!=null) && (c instanceof ReadonlyProcessVariableConfig)){
                        String unit = String.valueOf(((ReadonlyProcessVariableConfig)c).unit);
                        dataManager.setAttribute(datasetName, ATTR_DEVICE_UNIT, unit);
                    }
                    String channel = InlineDevice.getChannelName((Device)r);
                    if (channel!=null){
                        dataManager.setAttribute(datasetName, ATTR_DEVICE_CHANNEL, channel);
                    }                       
                }
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

    Stream getStream(Scan scan) {
        if (scan instanceof BsScan) {
            return ((BsScan) scan).getStream();
        }
        if (scan instanceof MonitorScan) {
            return ((MonitorScan) scan).getStream();
        }
        return null;
    }

    Stream stream;

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
        dataManager.setDataset(group + ATTR_DATASET_VERSION, new String[]{App.getApplicationVersion()});
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
            String name = dataManager.getAlias(writable);
            String groupDev = getDataPath(scan, name);
            dataManager.createGroup(groupDev);
            if (writable instanceof WritableArray) {
                dataManager.createDataset(groupDev + DATASET_READBACK, Double.class, new int[]{samples, ((WritableArray) writable).getSize()});
            } else {
                dataManager.createDataset(groupDev + DATASET_READBACK, Double.class, new int[]{samples});
            }
            if (writable instanceof WritableArray) {
                dataManager.createDataset(groupDev + DATASET_SETPOINT, Double.class, new int[]{samples, ((WritableArray) writable).getSize()});
            } else {
                dataManager.createDataset(groupDev + DATASET_SETPOINT, Double.class, new int[]{samples});
            }
            String channel = InlineDevice.getChannelName(writable);
            if (channel!=null){
                dataManager.setAttribute(groupDev, ATTR_DEVICE_CHANNEL, channel);
            }        
        }
        
        dataManager.createDataset(group + DATASET_TIMESTAMP, Long.class, new int[]{samples});
        if (stream != null) {
            dataManager.createDataset(group + DATASET_PID, Long.class, new int[]{samples});
            dataManager.createDataset(group + DATASET_GLOBAL_TIMESTAMP, Long.class, new int[]{samples});
        }        

        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = dataManager.getAlias(readable);
            String groupDev = getDataPath(scan, name);
            features = dataManager.getStorageFeatures(readable); 
            contiguous = dataManager.isStorageFeaturesContiguous(features);
            samples = contiguous ? scan.getNumberOfRecords() : 0;
            if (readable instanceof ReadableMatrix) {
                int[] dims = dataManager.getReadableMatrixDimension((ReadableMatrix) readable);
                if (contiguous){
                    dims[dataManager.getDepthDimension()] = scan.getNumberOfRecords();
                }                
                dataManager.createDataset(groupDev + DATASET_VALUE, getDeviceType(readable), dims, features);
                if (readable instanceof ReadableCalibratedMatrix) {
                    MatrixCalibration cal = ((ReadableCalibratedMatrix) readable).getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(groupDev, ATTR_CALIBRATION, new double[]{cal.scaleX, cal.scaleY, cal.offsetX, cal.offsetY});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else if (readable instanceof ReadableArray) {
                dataManager.createDataset(groupDev + DATASET_VALUE, getDeviceType(readable), new int[]{samples, ((ReadableArray) readable).getSize()}, features);
                if (readable instanceof ReadableCalibratedArray) {
                    ArrayCalibration cal = ((ReadableCalibratedArray) readable).getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(groupDev, ATTR_CALIBRATION, new double[]{cal.scale, cal.offset});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else {
                dataManager.createDataset(groupDev + DATASET_VALUE, getDeviceType(readable), new int[]{samples});
                if (Averager.isAverager(readable)) {
                    dataManager.createDataset(groupDev + DATASET_MIN, Double.class, new int[]{samples});
                    dataManager.createDataset(groupDev + DATASET_MAX, Double.class, new int[]{samples});
                    dataManager.createDataset(groupDev + DATASET_STDEV, Double.class, new int[]{samples});
                }
            }
            String channel = InlineDevice.getChannelName(readable);
            if (channel!=null){
                dataManager.setAttribute(groupDev, ATTR_DEVICE_CHANNEL, channel);
            }   
            
            dataManager.createDataset(groupDev + DATASET_TIMESTAMP, Long.class, new int[]{samples});
        }

        setStartTimestampAttibute(scan);
        setPlotPreferencesAttibutes(scan);
    }

    @Override
    public void onRecord(Scan scan, ScanRecord record) throws IOException {
        String group = getScanPath(scan);
        Number[] positions = record.getPositions();
        Object[] values = record.getValues();
        Long[] deviceTimestamps = record.getDeviceTimestamps();
        int index = getIndex(scan, record);
        int deviceIndex = 0;

        if (stream != null) {
            dataManager.setItem(group + DATASET_PID, record.getId(), index);
            dataManager.setItem(group + DATASET_GLOBAL_TIMESTAMP, record.getRemoteTimestamp(), index);
        }

        for (Writable writable : scan.getWritables()) {
            String path = getDataPath(scan, dataManager.getAlias(writable));
            dataManager.setItem(path + DATASET_SETPOINT, record.getSetpoints()[deviceIndex], index);
            dataManager.setItem(path + DATASET_READBACK, positions[deviceIndex++], index);
        }
        deviceIndex = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = dataManager.getAlias(readable);
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
                    String name = getDataManager().getAlias(readable);
                    String path = getDataPath(scan, name);
                    double[] stdev = (double[]) getDataManager().getData(path + DATASET_STDEV).sliceData;
                    //Not using error vector, but stde dataset 
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
        if ((String.valueOf(info.get(Provider.INFO_TYPE)).equals(Provider.INFO_VAL_TYPE_GROUP)) && dm.isGroup(root, path + ATTR_GROUP_METHOD)) {
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
                for (String child : ch.psi.utils.Arr.copy(children)) {
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
                        if (data.sliceData instanceof double[]) {
                            switch ((Integer) dim) {
                                case 1:
                                    if ((index != null) && (index > 0)) {
                                        break;

                                    }
                                    scanDimX = (double[]) data.sliceData;
                                    break;
                                case 2:
                                    scanDimY = (double[]) data.sliceData;
                                    break;
                                case 3:
                                    scanDimZ = (double[]) data.sliceData;
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

                    if ((steps != null) && (steps instanceof int[])) {
                        descriptor.steps = (int[]) steps;
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
