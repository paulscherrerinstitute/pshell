package ch.psi.pshell.data;

import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.Averager;
import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Writable.WritableArray;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This data layout stores each positioner and sensor as an individual dataset
 */
public class LayoutDefault extends LayoutBase implements Layout {

    public static final String ATTR_SCAN_DIMENSION = "Dimensions";
    public static final String ATTR_SCAN_STEPS = "Steps";
    public static final String ATTR_SCAN_PASSES = "Passes";
    public static final String ATTR_SCAN_WRITABLES = "Writables";
    public static final String ATTR_SCAN_READABLES = "Readables";
    public static final String ATTR_READABLE_INDEX = "Readable Index";
    public static final String ATTR_WRITABLE_INDEX = "Writable Index";
    public static final String ATTR_WRITABLE_DIMENSION = "Writable Dimension";

    public static final String META_GROUP = "meta/";
    public static final String TIMESTAMPS_DATASET = "Timestamps";
    public static final String DEVICE_MIN_DATASET = "_min";
    public static final String DEVICE_MAX_DATASET = "_max";
    public static final String DEVICE_STDEV_DATASET = "_stdev";

    public static final String SETPOINTS_DATASET_SUFFIX = "_setpoint";

    @Override
    public void initialize() {
    }

    @Override
    public String getLogFilePath() {
        return "logs/logs";
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

        int dimension = 1;
        int index = 0;
        for (Writable writable : scan.getWritables()) {
            String name = dataManager.getAlias(writable);
            //Positioners are always saved as double
            if (writable instanceof WritableArray) {
                dataManager.createDataset(getPath(scan, name), Double.class, new int[]{0, ((WritableArray) writable).getSize()});
            } else {
                dataManager.createDataset(getPath(scan, name), Double.class, new int[]{0});
            }
            if (persistSetpoints) {
                if (writable instanceof WritableArray) {
                    dataManager.createDataset(getPath(scan, name) + SETPOINTS_DATASET_SUFFIX, Double.class, new int[]{0, ((WritableArray) writable).getSize()});
                } else {
                    dataManager.createDataset(getPath(scan, name) + SETPOINTS_DATASET_SUFFIX, Double.class, new int[]{0});
                }
            }

            dataManager.setAttribute(getPath(scan, name), ATTR_WRITABLE_INDEX, index++);
            dataManager.setAttribute(getPath(scan, name), ATTR_WRITABLE_DIMENSION, dimension);

            if (scan.getDimensions() > 1) {
                //TODO: assuming for area scan one Writable for each dimension
                dimension++;
            }
        }

        index = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = dataManager.getAlias(readable);
            if (readable instanceof ReadableMatrix) {
                //This is very slow
                //dataManager.createDataset(getPath(name), getDeviceType(readable), new int[]{0, 0, 0});                    
                dataManager.createDataset(getPath(scan, name), getDeviceType(readable), dataManager.getReadableMatrixDimension((ReadableMatrix) readable));
                if (readable instanceof ReadableCalibratedMatrix) {
                    MatrixCalibration cal = ((ReadableCalibratedMatrix) readable).getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(getPath(scan, name), ATTR_CALIBRATION, new double[]{cal.scaleX, cal.scaleY, cal.offsetX, cal.offsetY});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else if (readable instanceof ReadableArray) {
                //dataManager.createDataset(getPath(name), getDeviceType(readable), new int[]{0, 0});
                dataManager.createDataset(getPath(scan, name), getDeviceType(readable), new int[]{0, ((ReadableArray) readable).getSize()});
                if (readable instanceof ReadableCalibratedArray) {
                    ArrayCalibration cal = ((ReadableCalibratedArray) readable).getCalibration();
                    if (cal != null) {
                        dataManager.setAttribute(getPath(scan, name), ATTR_CALIBRATION, new double[]{cal.scale, cal.offset});
                    } else {
                        dataManager.appendLog("Calibration unavailable for: " + name);
                    }
                }
            } else {
                dataManager.createDataset(getPath(scan, name), getDeviceType(readable), new int[]{0});
                if (Averager.isAverager(readable)) {
                    dataManager.createDataset(getPath(scan, META_GROUP + name + DEVICE_MIN_DATASET), Double.class, new int[]{0});
                    dataManager.createDataset(getPath(scan, META_GROUP + name + DEVICE_MAX_DATASET), Double.class, new int[]{0});
                    dataManager.createDataset(getPath(scan, META_GROUP + name + DEVICE_STDEV_DATASET), Double.class, new int[]{0});
                }
            }
            dataManager.setAttribute(getPath(scan, name), ATTR_READABLE_INDEX, index++);
        }
        dataManager.createDataset(getPath(scan, META_GROUP + TIMESTAMPS_DATASET), Long.class, new int[]{0});
        dataManager.setAttribute(group, ATTR_SCAN_DIMENSION, scan.getDimensions());
        dataManager.setAttribute(group, ATTR_SCAN_STEPS, (scan.getNumberOfSteps().length > 0) ? scan.getNumberOfSteps() : new int[]{-1});
        dataManager.setAttribute(group, ATTR_SCAN_PASSES, scan.getNumberOfPasses());
        dataManager.setAttribute(group, ATTR_SCAN_READABLES, scan.getReadableNames());
        dataManager.setAttribute(group, ATTR_SCAN_WRITABLES, scan.getWritableNames());

        super.onStart(scan);
    }

    @Override
    public void onRecord(Scan scan, ScanRecord record) throws IOException {
        DataManager dataManager = getDataManager();
        Number[] positions = record.getPositions();
        Object[] values = record.getValues();
        int index = getIndex(scan, record);
        int deviceIndex = 0;
        for (Writable writable : scan.getWritables()) {
            String path = getPath(scan, dataManager.getAlias(writable));
            if (persistSetpoints) {
                dataManager.setItem(path + SETPOINTS_DATASET_SUFFIX, record.getSetpoints()[deviceIndex], index);
            }
            dataManager.setItem(path, positions[deviceIndex++], index);
        }
        deviceIndex = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            String name = dataManager.getAlias(readable);
            String path = getPath(scan, name);
            Object value = values[deviceIndex++];
            if (!getPreserveTypes()) {
                //TODO: These conversions are not automativally managed by h5 provider. Should be managed there?
                if ((value instanceof Boolean) || (value instanceof boolean[])) {
                    value = Convert.toDouble(value);
                }
            }
            dataManager.setItem(path, value, index);
            if (Averager.isAverager(readable)) {
                DescStatsDouble v = (DescStatsDouble) value;
                dataManager.setItem(getPath(scan, META_GROUP + name + DEVICE_MIN_DATASET), (v == null) ? null : v.getMin(), index);
                dataManager.setItem(getPath(scan, META_GROUP + name + DEVICE_MAX_DATASET), (v == null) ? null : v.getMax(), index);
                dataManager.setItem(getPath(scan, META_GROUP + name + DEVICE_STDEV_DATASET), (v == null) ? null : v.getStdev(), index);
            }
        }
        dataManager.setItem(getPath(scan, META_GROUP + TIMESTAMPS_DATASET), record.getTimestamp(), index);
    }

    @Override
    public void onFinish(Scan scan) throws IOException {
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            if (Averager.isAverager(readable)) {
                try {
                    getDataManager().flush();
                    String name = getDataManager().getAlias(readable);
                    double[] stdev = (double[]) getDataManager().getData(getPath(scan, META_GROUP + name + DEVICE_STDEV_DATASET)).sliceData;
                    getDataManager().setAttribute(getPath(scan, name), ATTR_ERROR_VECTOR, stdev);
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
                            DataSlice data = dm.getData(root, child.substring(0, child.lastIndexOf("/") + 1) + META_GROUP + descriptor.name + DEVICE_STDEV_DATASET);
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

    protected String getPath(Scan scan, String device) {
        if (device == null) {
            device = "data";
        }
        return getScanPath(scan) + device;
    }

}
