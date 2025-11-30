package ch.psi.pshell.data;

import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Writable.WritableArray;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.TimestampedValue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This data layout stores each positioner and sensor as an individual dataset
 */
public class LayoutDefault extends LayoutBase {
    
    public static String PATH_STATISTICS = "statistics/";
    public static String PATH_TIMESTAMPS = "timestamps/";
    public static String PATH_SETPOINTS = "setpoints/";

    public static final String ATTR_READABLE_INDEX = "Readable Index";
    public static final String ATTR_WRITABLE_INDEX = "Writable Index";
    public static final String ATTR_WRITABLE_DIMENSION = "Writable Dimension";

    public static final String TIMESTAMPS_DATASET = "timestamp";
    public static final String DEVICE_MIN_SUFFIX = "min";
    public static final String DEVICE_MAX_SUFFIX = "max";
    public static final String DEVICE_STDEV_SUFFIX = "stdev";
       

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void initialize() {
    }
   
    protected String getDataPath(Scan scan, String device) {
        if (device == null) {
            device = "data";
        }
        return getScanPath(scan) + device;
    }

    
    protected String getMetaPath(Scan scan) {
        return getScanPath(scan) + getMetaPath();
    }
    
    protected String getSetpointsPath(Scan scan) {
        return getMetaPath(scan) + PATH_SETPOINTS;
    }    
    
    protected String getStatisticsPath(Scan scan) {
        return getMetaPath(scan) + PATH_STATISTICS;
    }    

    protected String getSetpointsPath(Scan scan, String device) {
        return getSetpointsPath(scan) + device;
    }    
    
    protected String getTimestampsPath(Scan scan) {
        return getScanPath(scan) + PATH_TIMESTAMPS;
    }    

    protected String getTimestampsPath(Scan scan, String device) {
        return getTimestampsPath(scan) + device;
    }    

    protected String getStatisticsPath(Scan scan, String device, String stat) {
        return getStatisticsPath(scan) + device + "_" + stat;
    }    
    
    protected String getTimestampsDataset(Scan scan) {
        return getTimestampsDataset(getScanPath(scan));        
    }      
    

    @Override
    public String getDefaultGroup(Scan scan) {
        return scan.getTag();
    }        

    @Override
    public void onStart(Scan scan) throws IOException {
        DataManager dataManager = getDataManager();
        String group = getScanPath(scan);
        dataManager.createGroup(group);
        boolean createMeta = getCreateMeta();

        Map features = dataManager.getStorageFeatures(null);
        boolean contiguous = dataManager.isStorageFeaturesContiguous(features);
        int samples = contiguous ? scan.getNumberOfRecords() : 0;

        int dimension = 1;
        int index = 0;
        for (Writable writable : scan.getWritables()) {
            String name = writable.getAlias();
            //Positioners are always saved as double
            if (writable instanceof WritableArray writableArray) {
                dataManager.createDataset(getDataPath(scan, name), Double.class, new int[]{samples, writableArray.getSize()});
            } else {
                dataManager.createDataset(getDataPath(scan, name), Double.class, new int[]{samples});
            }
            if (createMeta) {
                if (writable instanceof WritableArray writableArray) {
                    dataManager.createDataset(getSetpointsPath(scan, name), Double.class, new int[]{samples, writableArray.getSize()});
                } else {
                    dataManager.createDataset(getSetpointsPath(scan, name), Double.class, new int[]{samples});
                }
            }
            dataManager.setAttribute(getDataPath(scan, name), ATTR_WRITABLE_INDEX, index++);
            dataManager.setAttribute(getDataPath(scan, name), ATTR_WRITABLE_DIMENSION, dimension);

            if (scan.getDimensions() > 1) {
                //TODO: assuming for area scan one Writable for each dimension
                dimension++;
            }
            writeDeviceMetadataAttrs(getDataPath(scan, name), writable);
        }
        if (createMeta){
            dataManager.createDataset(getTimestampsDataset(scan), Long.class, new int[]{samples});
        }
        ReadableArray a;
        index = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = readable.getAlias();
            dataManager.createDataset(getDataPath(scan, name), scan, readable);

            if (readable instanceof ReadableMatrix) {
                if (readable instanceof ReadableCalibratedMatrix readableCalibratedMatrix) {
                    MatrixCalibration cal = readableCalibratedMatrix.getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(getDataPath(scan, name), ATTR_CALIBRATION, new double[]{cal.scaleX, cal.scaleY, cal.offsetX, cal.offsetY});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else if (readable instanceof ReadableArray) {
                if (readable instanceof ReadableCalibratedArray readableCalibratedArray) {
                    ArrayCalibration cal = readableCalibratedArray.getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(getDataPath(scan, name), ATTR_CALIBRATION, new double[]{cal.scale, cal.offset});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else {
                if (createMeta){
                    if (Averager.isAverager(readable)) {
                        dataManager.createDataset(getStatisticsPath(scan, name, DEVICE_MIN_SUFFIX), Double.class, new int[]{samples});
                        dataManager.createDataset(getStatisticsPath(scan, name, DEVICE_MAX_SUFFIX), Double.class, new int[]{samples});
                        dataManager.createDataset(getStatisticsPath(scan, name, DEVICE_STDEV_SUFFIX), Double.class, new int[]{samples});
                    }
                }
            }
            if (getCreateTimestamps()) {
                dataManager.createDataset(getTimestampsPath(scan,name) , Long.class, new int[]{samples});
            }

            dataManager.setAttribute(getDataPath(scan, name), ATTR_READABLE_INDEX, index++);
            writeDeviceMetadataAttrs(getDataPath(scan, name), readable);
        }                
        super.onStart(scan);
    }

    @Override
    public void onRecord(Scan scan, ScanRecord record) throws IOException {
        DataManager dataManager = getDataManager();
        Number[] positions = record.getPositions();
        Number[] setpoints = record.getSetpoints();
        Object[] values = record.getReadables();
        boolean createMeta = getCreateMeta();
        int index = getIndex(scan, record);
        int deviceIndex = 0;
        for (Writable writable : scan.getWritables()) {
            String name = writable.getAlias();
            String path = getDataPath(scan, name);
            if (createMeta) {
                dataManager.setItem(getSetpointsPath(scan, name), setpoints[deviceIndex], index);
            }
            dataManager.setItem(path, positions[deviceIndex++], index);
        }
        deviceIndex = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = readable.getAlias();
            String path = getDataPath(scan, name);
            Object value = values[deviceIndex++];
            dataManager.setItem(path, value, index);
            if (getCreateTimestamps()) {
                Long timestamp = record.getTimestamp();
                if (value instanceof TimestampedValue timestampedValue) {
                    timestamp = timestampedValue.getTimestamp();
                } else if (readable instanceof Device device) {
                    timestamp = device.takeTimestamped().getTimestamp();
                }
                dataManager.setItem(getTimestampsPath(scan, name), timestamp, index);
            }
            if (createMeta) {
                if (Averager.isAverager(readable)) {
                    DescStatsDouble v = (DescStatsDouble) value;
                    dataManager.setItem(getStatisticsPath(scan, name, DEVICE_MIN_SUFFIX), (v == null) ? null : v.getMin(), index);
                    dataManager.setItem(getStatisticsPath(scan, name, DEVICE_MAX_SUFFIX), (v == null) ? null : v.getMax(), index);
                    dataManager.setItem(getStatisticsPath(scan, name, DEVICE_STDEV_SUFFIX), (v == null) ? null : v.getStdev(), index);
                }
            }
        }
        if (createMeta) {
            dataManager.setItem(getTimestampsDataset(scan), record.getTimestamp(), index);
        }
    }

    @Override
    public void onFinish(Scan scan) throws IOException {
        DataManager dataManager = getDataManager();
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            if (Averager.isAverager(readable)) {
                try {
                    String name = readable.getAlias();
                    String path = getStatisticsPath(scan, name, DEVICE_STDEV_SUFFIX);
                    if (dataManager.isDataset(path)){
                        dataManager.flush();
                        double[] stdev = (double[]) dataManager.getData(path).sliceData;
                        dataManager.setAttribute(getDataPath(scan, name), ATTR_ERROR_VECTOR, stdev);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(LayoutDefault.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
        super.onFinish(scan);
    }
    
    @Override
    public void onRunStarted(File file) throws IOException {
        if (getCreateLogs()){
            if (getSaveSource()){
                saveFile(file, getSourceFilePath());                
            }
        }
    }
    
    public void appendOutput(String str) throws IOException {        
        if (getCreateLogs()){
            DataManager dataManager = getDataManager();
            String outoutFile = getOutputFilePath();
            if (!dataManager.exists(getOutputFilePath())){
                dataManager.createDataset(getOutputFilePath(), String.class);
            }                        
            dataManager.appendItem(getOutputFilePath(),str);
        }
    }    
    
    
    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        dm = (dm == null) ? getDataManager() : dm;
        Map<String, Object> info = dm.getInfo(root, path);
        if ((String.valueOf(info.get(Format.INFO_TYPE)).equals(Format.INFO_VAL_TYPE_GROUP))) {
            ArrayList<PlotDescriptor> ret = new ArrayList<>();
            double[] scanDimX = null;
            double[] scanDimY = null;
            double[] scanDimZ = null;

            String[] readables = null;
            String[] writables = null;
            try {
                readables = (String[]) dm.getAttribute(root, path, ATTR_SCAN_READABLES);
            } catch (Exception ex) {
            }
            try {
                writables = (String[]) dm.getAttribute(root, path, ATTR_SCAN_WRITABLES);
            } catch (Exception ex) {
            }

            String[] children = dm.getChildren(root, path);

            for (String child : ch.psi.pshell.utils.Arr.copy(children)) {
                Object dim = dm.getAttribute(root, child, ATTR_WRITABLE_DIMENSION);
                Object index = dm.getAttribute(root, child, ATTR_WRITABLE_INDEX);
                if (dim instanceof Integer intDim) {
                    DataSlice data = dm.getData(root, child);
                    if (data.sliceData instanceof double[] sliceDouble) {
                        switch (intDim) {
                            case 1:
                                if (index instanceof Integer intIndex) {
                                    if (intIndex > 0) {
                                        break;
                                    }
                                }
                                scanDimX = sliceDouble;
                                break;
                            case 2:
                                scanDimY = sliceDouble;
                                break;
                            case 3:
                                scanDimZ = sliceDouble;
                                break;
                        }
                    }
                    children = Arr.removeEquals(children, child);
                }
            }

            for (String child : children) {
                List<PlotDescriptor> descriptors = dm.getPlots(root, child);
                for (PlotDescriptor descriptor : descriptors) {
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

                    if (dm.getAttribute(root, path, ATTR_SCAN_STEPS) instanceof int[] isteps) {
                        descriptor.steps = isteps;
                    }
                    if (dm.getAttribute(root, path, ATTR_SCAN_START) instanceof double[] dstart) {
                        descriptor.start = dstart;
                    }
                    if (dm.getAttribute(root, path, ATTR_SCAN_END) instanceof double[] dend) {
                        descriptor.end = dend;
                    }
                    if (dm.getAttribute(root, path, ATTR_SCAN_ZIGZAG) instanceof Boolean bzigzag) {
                        descriptor.zigzag = bzigzag;
                    }                    
                    if (dm.getAttribute(root, path, ATTR_SCAN_PASSES) instanceof Number number) {
                        descriptor.passes = number.intValue();
                    }
                    if (dm.getAttribute(root, path, ATTR_SCAN_DIMENSION) instanceof Number number) {
                        descriptor.dimensions = number.intValue();
                    }
                                       
                    
                    try {
                        //Getting stdev if available and error not yet set by DeviceManager(if error vector is too big for an attribute)
                        if (descriptor.error == null) {
                            DataSlice data = dm.getData(root, child.substring(0, child.lastIndexOf("/") + 1) + getMetaPath() + PATH_STATISTICS + descriptor.name + DEVICE_STDEV_SUFFIX);
                            descriptor.error = (double[]) data.sliceData;
                        }
                    } catch (Exception ex) {
                    }
                    descriptor.labels =  writables;
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
        dm = (dm == null) ? getDataManager() : dm;
        return ((dm.getAttribute(root, path, ATTR_READABLE_INDEX) != null)
                || (dm.getAttribute(root, path, ATTR_WRITABLE_INDEX) != null));
    }

    
    @Override
    public Object getData(Scan scan, String device, DataManager dm) {
        dm = (dm == null) ? getDataManager() : dm;
        DataAddress scanPath = DataManager.getAddress(scan.getPath());
        try {
            if (device.endsWith(SETPOINTS_DATASET_SUFFIX)) {
                device = device.substring(0, device.length() - SETPOINTS_DATASET_SUFFIX.length());
                String path_setpoint = getSetpointsPath(scan, device);
                //If no setpoint is saved, returns the readback values (same behabior as in-memory scan records)
                if (dm.exists(scanPath.root, path_setpoint)) {
                    return dm.getData(scanPath.root, path_setpoint).sliceData;
                }
            }
            return dm.getData(scanPath.root, getDataPath(scan, device)).sliceData;
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    public String getTimestampsDataset(String scanPath) {
        return scanPath + "/" + getMetaPath() + TIMESTAMPS_DATASET;
    }

}
