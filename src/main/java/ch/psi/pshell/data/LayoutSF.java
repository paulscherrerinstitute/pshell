package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Writable.WritableArray;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
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
    public static final String ATTR_DATASET_USER = ATTR_GROUP_GENERAL + "user";
    public static final String ATTR_DATASET_PROCESS = ATTR_GROUP_GENERAL + "process";
    public static final String ATTR_DATASET_INSTRUMENT = ATTR_GROUP_GENERAL + "instrument";
    
    public static final String ATTR_DATASET_LOG_DESCRIPTION = ATTR_GROUP_LOGS + "description";
    public static final String ATTR_DATASET_LOG_TIMESTAMP = ATTR_GROUP_LOGS + "timestamp";
    
    public static final String ATTR_GROUP_METHOD = "method/";
    public static final String ATTR_GROUP_DATA = "data/";
    public static final String ATTR_GROUP_PROCESSED= "processed/";
    public static final String ATTR_GROUP_META= "meta/";
    
    public static final String ATTR_DATASET_READBACK= "readback";
    public static final String ATTR_DATASET_SETPOINT= "setpoint";
    public static final String ATTR_DATASET_TIMESTAMP = "timestamp";
    
    public static final String DEVICE_MIN_DATASET = "min";
    public static final String DEVICE_MAX_DATASET = "max";
    public static final String DEVICE_STDEV_DATASET = "stdev";    
    
    public static final String ATTR_SCAN_DIMENSION = "Dimensions";
    public static final String ATTR_SCAN_STEPS = "Steps";
    public static final String ATTR_SCAN_WRITABLES = "Writables";
    public static final String ATTR_SCAN_READABLES = "Readables";
    public static final String ATTR_READABLE_INDEX = "Readable Index";
    public static final String ATTR_WRITABLE_INDEX = "Writable Index";
    public static final String ATTR_WRITABLE_DIMENSION = "Writable Dimension";





    String group;

    final static Context context = Context.getInstance();
    final DataManager dataManager = getDataManager();
    
    static List<Readable> experimentArguments = new ArrayList<>();
    
    public static void setExperimentArguments(List<Readable> args){
        experimentArguments = args;
    }

    public static List<Readable> getExperimentArguments(){
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
        dataManager.setDataset(ATTR_DATASET_CREATED, new String[]{Chrono.getTimeStr(System.currentTimeMillis(), "dd/MM/YY HH:mm:ss.SSS ")} );
        dataManager.setDataset(ATTR_DATASET_USER, new String[]{Sys.getUserName()} );
        dataManager.setDataset(ATTR_DATASET_PROCESS, new String[]{"PShell"} );
        dataManager.setDataset(ATTR_DATASET_INSTRUMENT, new String[]{(context == null) ? "" : context.getConfig().getName()} );     
        dataManager.createDataset(ATTR_DATASET_LOG_DESCRIPTION, String.class);
        dataManager.createDataset(ATTR_DATASET_LOG_TIMESTAMP, Long.class);
        
        dataManager.setDataset(ATTR_GROUP_EXPERIMENT + "author", new String[]{context.getUser().name} );
        dataManager.setDataset(ATTR_GROUP_EXPERIMENT + "command", new String[]{String.valueOf(dataManager.getExecutionPars().getStatement())} );
        dataManager.setDataset(ATTR_GROUP_EXPERIMENT + "parameters", new String[]{Str.toString(dataManager.getExecutionPars().getArgs())} );
        dataManager.setDataset(ATTR_GROUP_EXPERIMENT + "script", new String[]{String.valueOf(dataManager.getExecutionPars().getPath())} );
        dataManager.setDataset(ATTR_GROUP_EXPERIMENT + "type", new String[]{String.valueOf(dataManager.getExecutionPars().getName())} );
        
        for (Readable r : getExperimentArguments()){
            try {
                Object val = r.read();
                if (!val.getClass().isArray()){
                    Object arr = Array.newInstance(val.getClass(), 1);
                    Array.set(arr, 0, val);
                    val = arr;
                }
                dataManager.setDataset(ATTR_GROUP_EXPERIMENT + r.getName(), val );
            } catch (InterruptedException ex) {
               Thread.currentThread().interrupt();
            }
        }
    }

    
    @Override
    public void appendLog(String log) throws IOException{
        dataManager.appendItem(ATTR_DATASET_LOG_TIMESTAMP, log);
        dataManager.appendItem(ATTR_DATASET_LOG_DESCRIPTION, log);
    }

    @Override
    public String getDefaultGroup(Scan scan) {
        return scan.getTag();
    }

    @Override
    public void onStart(Scan scan) throws IOException {
        String group = getScanPath(scan);
        dataManager.createGroup(group);
        dataManager.createGroup(group + ATTR_GROUP_METHOD);
        dataManager.createGroup(group + ATTR_GROUP_DATA);
        dataManager.createGroup(group + ATTR_GROUP_PROCESSED);
        dataManager.createGroup(group + ATTR_GROUP_META);
        
        
        int dimension = 1;
        int index = 0;
        for (Writable writable : scan.getWritables()) {
            String name = dataManager.getAlias(writable);
            String groupDev = getDataPath(scan, name);
            dataManager.createGroup(groupDev);
            if (writable instanceof WritableArray) {
                dataManager.createDataset(groupDev + ATTR_DATASET_READBACK, Double.class, new int[]{0, ((WritableArray) writable).getSize()});
            } else {
                dataManager.createDataset(groupDev + ATTR_DATASET_READBACK, Double.class, new int[]{0});
            }
            if (writable instanceof WritableArray) {
                dataManager.createDataset(groupDev + ATTR_DATASET_SETPOINT, Double.class, new int[]{0, ((WritableArray) writable).getSize()});
            } else {
                dataManager.createDataset(groupDev + ATTR_DATASET_SETPOINT, Double.class, new int[]{0});
            }

            dataManager.setAttribute(groupDev, ATTR_WRITABLE_INDEX, index++);
            dataManager.setAttribute(groupDev, ATTR_WRITABLE_DIMENSION, dimension);

            if (scan.getDimensions() > 1) {
                //TODO: assuming for area scan one Writable for each dimension
                dimension++;
            }                               
        }

        index = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = dataManager.getAlias(readable);
            String groupDev = getDataPath(scan, name);
            if (readable instanceof ReadableMatrix) {        
                dataManager.createDataset(groupDev + ATTR_DATASET_READBACK, getDeviceType(readable), dataManager.getReadableMatrixDimension((ReadableMatrix) readable));
                if (readable instanceof ReadableCalibratedMatrix) {
                    MatrixCalibration cal = ((ReadableCalibratedMatrix) readable).getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(groupDev, ATTR_CALIBRATION, new double[]{cal.scaleX, cal.scaleY, cal.offsetX, cal.offsetY});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else if (readable instanceof ReadableArray) {
                dataManager.createDataset(groupDev + ATTR_DATASET_READBACK, getDeviceType(readable), new int[]{0, ((ReadableArray) readable).getSize()});
                if (readable instanceof ReadableCalibratedArray) {
                    ArrayCalibration cal = ((ReadableCalibratedArray) readable).getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(groupDev, ATTR_CALIBRATION, new double[]{cal.scale, cal.offset});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else {
                dataManager.createDataset(groupDev + ATTR_DATASET_READBACK, getDeviceType(readable), new int[]{0});
                if (Averager.isAverager(readable)) {
                    dataManager.createDataset(getMetaPath(scan, name) + DEVICE_MIN_DATASET, Double.class, new int[]{0});
                    dataManager.createDataset(getMetaPath(scan, name) + DEVICE_MAX_DATASET, Double.class, new int[]{0});
                    dataManager.createDataset(getMetaPath(scan, name) + DEVICE_STDEV_DATASET, Double.class, new int[]{0});
                }
            }
            dataManager.setAttribute(groupDev + ATTR_DATASET_READBACK, ATTR_READABLE_INDEX, index++);
            dataManager.createDataset(groupDev + ATTR_DATASET_TIMESTAMP, Long.class, new int[]{0});
        }
        dataManager.createDataset(group + ATTR_DATASET_TIMESTAMP, Long.class, new int[]{0});
        dataManager.setAttribute(group, ATTR_SCAN_DIMENSION, scan.getDimensions());
        dataManager.setAttribute(group, ATTR_SCAN_STEPS, scan.getNumberOfSteps());
        dataManager.setAttribute(group, ATTR_SCAN_READABLES, scan.getReadableNames());
        dataManager.setAttribute(group, ATTR_SCAN_WRITABLES, scan.getWritableNames());

        setStartTimestampAttibute(scan);
        setPlotPreferencesAttibutes(scan);        
    }

    @Override
    public void onRecord(Scan scan, ScanRecord record) throws IOException {
        String group = getScanPath(scan);
        Number[] positions = record.getPositions();
        Object[] values = record.getValues();
        int index = getIndex(scan, record);
        int deviceIndex = 0;
        for (Writable writable : scan.getWritables()) {
            String path = getDataPath(scan, dataManager.getAlias(writable));
            dataManager.setItem(path + ATTR_DATASET_SETPOINT, record.getSetpoints()[deviceIndex], index);
            dataManager.setItem(path + ATTR_DATASET_READBACK, positions[deviceIndex++], index);
        }
        deviceIndex = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = dataManager.getAlias(readable);
            String path = getDataPath(scan, name);
            Object value = values[deviceIndex++];
            if (!getPreserveTypes()) {
                if ((value instanceof Boolean) || (value instanceof boolean[])) {
                    value = Convert.toDouble(value);
                }
            }
            dataManager.setItem(path + ATTR_DATASET_READBACK, value, index);
            if (Averager.isAverager(readable)) {
                DescStatsDouble v = (DescStatsDouble) value;
                dataManager.setItem(getMetaPath(scan, name) + DEVICE_MIN_DATASET, (v == null) ? null : v.getMin(), index);
                dataManager.setItem(getMetaPath(scan, name) + DEVICE_MAX_DATASET, (v == null) ? null : v.getMax(), index);
                dataManager.setItem(getMetaPath(scan, name) + DEVICE_STDEV_DATASET, (v == null) ? null : v.getStdev(), index);
            }
            dataManager.setItem(path + ATTR_DATASET_READBACK, value, index);
        }
        dataManager.setItem(group + ATTR_DATASET_TIMESTAMP, record.getTimestamp(), index);
    }

    @Override
    public void onFinish(Scan scan) throws IOException {
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            if (Averager.isAverager(readable)) {
                try {
                    getDataManager().flush();
                    String name = getDataManager().getAlias(readable);
                    double[] stdev = (double[]) getDataManager().getData(getMetaPath(scan, name) + DEVICE_STDEV_DATASET).sliceData;
                    getDataManager().setAttribute(getDataPath(scan, name) + ATTR_DATASET_READBACK , ATTR_ERROR_VECTOR, stdev);
                } catch (Exception ex) {
                    Logger.getLogger(LayoutDefault.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
        super.onFinish(scan);
    }

    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        dm = (dm == null) ? getDataManager() : dm;
        Map<String, Object> info = dm.getInfo(root, path);
        if ((String.valueOf(info.get(Provider.INFO_TYPE)).equals(Provider.INFO_VAL_TYPE_GROUP))) {
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
            //Object dimensions =dm.getAttribute(root, path, ATTR_SCAN_DIMENSION);            
            Object steps = dm.getAttribute(root, path, ATTR_SCAN_STEPS);
            for (String child : ch.psi.utils.Arr.copy(children)) {
                Object dim = dm.getAttribute(root, child, ATTR_WRITABLE_DIMENSION);
                Object index = dm.getAttribute(root, child, ATTR_WRITABLE_INDEX);
                if ((dim != null) && (dim instanceof Integer)) {
                    DataSlice data = dm.getData(root, child);
                    if (data.sliceData instanceof double[]) {
                        switch ((Integer) dim) {
                            case 1:

                                if ((index != null) && (index instanceof Integer)) {
                                    if (((Integer) index) > 0) {
                                        break;
                                    }
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

                    if ((steps != null) && (steps instanceof int[])) {
                        descriptor.steps = (int[]) steps;
                    }

                    try {
                        //Getting stdev if available and error not yet set by DeviceManager(if error vector is too big for an attribute)
                        if (descriptor.error == null) {
                            DataSlice data = dm.getData(root, child.substring(0, child.lastIndexOf("/") + 1) + ATTR_GROUP_META + descriptor.name + DEVICE_STDEV_DATASET);
                            descriptor.error = (double[]) data.sliceData;
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
        return ((dm.getAttribute(root, path, ATTR_READABLE_INDEX) != null)
                || (dm.getAttribute(root, path, ATTR_WRITABLE_INDEX) != null));
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