package ch.psi.pshell.data;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.scan.AreaScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.TimestampedValue;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This data layout stores all positioners and sensors in a single table
 */
public class LayoutTable extends LayoutBase {

    public static final String ATTR_SCAN_WRITABLE_DIMS = "Writable Dims";

    @Override
    public String getId() {
        return "table";
    }
    
    @Override
    public void initialize() {
    }

    @Override
    public String getDefaultGroup(Scan scan) {
        return "/";
    }

    @Override
    public String getScanPathName(Scan scan) {
        return getCurrentGroup(scan) + getDatasetName(scan);
    }

    @Override
    public String getLogsPath() {
        return getDefaultGroup(null);
    }

    @Override
    public String getLogFilePath() {
        return getLogsPath() + getLogFileName();
    }

    protected String getLogFileName() {
        return "logs";
    }
    
    public String getDiagsPathName(Scan scan) {
        return getScanPathName(scan) + "_" + getDiagsPath();
    }
    public String getMonitorsPathName(Scan scan) {
        return getScanPathName(scan) + "_" +  getMonitorsPath();
    }
    public String getSnapsPathName(Scan scan) {
        return getScanPathName(scan) + "_" +  getSnapsPath();
    }    

    protected String getDatasetName(Scan scan) {
        return scan.getTag();
    }

    @Override
    public void onStart(Scan scan) throws IOException {
        initialize(); //format may have changed
        String path = getScanPath(scan);

        int fields = scan.getWritables().length + scan.getReadables().length;
        if (getCreateTimestamps()) {
            fields +=  scan.getReadables().length;
        }
        String[] fieldNames = new String[fields];
        Class[] fieldTypes = new Class[fields];
        int[] fieldLength = new int[fields];
        int[] writableDims = new int[scan.getWritables().length];

        int index = 0;
        int dimension = (scan instanceof AreaScan) ? scan.getWritables().length : 1;
        for (Writable writable : scan.getWritables()) {
            fieldTypes[index] = Double.class;
            writableDims[index] = (dimension > 1) ? dimension-- : dimension;
            fieldNames[index++] = writable.getAlias();
        }
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            if (getCreateTimestamps()) {
                fieldTypes[index] = Long.TYPE;
                fieldNames[index++] = readable.getAlias() + " timestamp";
            }
            Class type = getDatasetType(readable);
            if (readable instanceof ReadableMatrix readableMatrix) {
                fieldTypes[index] = Array.newInstance(Convert.getPrimitiveClass(type), new int[]{0, 0}).getClass();
                fieldLength[index] = readableMatrix.getHeight();
            } else if (readable instanceof ReadableArray readableArray) {
                fieldTypes[index] = Array.newInstance(Convert.getPrimitiveClass(type), 0).getClass();
                fieldLength[index] = readableArray.getSize();
            } else {
                fieldTypes[index] = type;
            }
            fieldNames[index++] = readable.getAlias();
        }        
        DataManager dm = getDataManager();
        Map features =  dm.getStorageFeatures(null);
        dm.createDataset(path, fieldNames, fieldTypes, fieldLength, features);
        dm.setAttribute(path, ATTR_SCAN_WRITABLE_DIMS, (writableDims.length > 0) ? writableDims : new int[]{-1});
        dm.setAttribute(path, ATTR_SCAN_STEPS, (scan.getNumberOfSteps().length > 0) ? scan.getNumberOfSteps() : new int[]{-1});
        dm.setAttribute(path, ATTR_SCAN_PASSES, scan.getNumberOfPasses());        
        super.onStart(scan);
    }

    @Override
    public void onRecord(Scan scan, ScanRecord record) throws IOException {
        int index = 0;
        int deviceIndex = 0;
        Number[] positions = record.getPositions();
        Object[] values = record.getReadables();
        int fields = scan.getWritables().length + scan.getReadables().length;
        if (getCreateTimestamps()) {
            fields +=  scan.getReadables().length;
        }
        
        Object[] data = new Object[fields];

        for (Writable writable : scan.getWritables()) {
            data[index++] = positions[deviceIndex++];
        }

        deviceIndex = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            if (getCreateTimestamps()) {
                Long timestamp = record.getTimestamp();
                if (values[deviceIndex] instanceof  TimestampedValue timestampedValue){
                    timestamp = timestampedValue.getTimestamp();
                } else if (readable instanceof Device device){
                    timestamp = device.takeTimestamped().getTimestamp();
                }                
                data[index++] = timestamp;
            }            
            data[index++] = values[deviceIndex++];
        }
        getDataManager().setItem(getScanPath(scan), data, getIndex(scan, record));
    }

    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        dm = (dm == null) ? getDataManager() : dm;
        Map<String, Object> info = dm.getInfo(root, path);
        if ((String.valueOf(info.get(Format.INFO_TYPE)).equals(Format.INFO_VAL_TYPE_DATASET))) {
            if (info.get(Format.INFO_DATA_TYPE) == Format.INFO_VAL_DATA_TYPE_COMPOUND) {
                ArrayList<PlotDescriptor> ret = new ArrayList<>();
                DataSlice slice = dm.getData(root, path);
                Object[][] sliceData = (Object[][]) slice.sliceData;
                int records = slice.sliceShape[0];
                String[] names = (String[]) info.get(Format.INFO_FIELD_NAMES);
                int fields = (names != null) ? names.length : ((sliceData.length > 0) ? sliceData[0].length : 0);
                if (names == null) {
                    names = new String[fields];
                }

                int positioners = 0;
                int sensors = fields;
                int[] dims = (int[]) dm.getAttribute(root, path, ATTR_SCAN_WRITABLE_DIMS);
                int dimensions = 1;

                if ((dims != null) && (dims.length > 0) && (dims[0] >= 0)) {
                    dimensions = dims[0];
                    if (dims.length < fields) {
                        positioners = dims.length;
                        sensors = fields - positioners;
                    }
                }
                int[] size = null;
                if (dimensions == 2) {
                    size = new int[]{0, 0};
                    for (int i = 0; i < records; i++) {
                        if (!sliceData[0][0].equals(sliceData[i][0])) {
                            break;
                        }
                        size[1]++;
                    }
                    for (int j = 0; j < records; j += size[1]) {
                        size[0]++;
                    }
                }
                int[] steps = (size == null) || (size[0] == 0) || (size[1] == 0) ? null : new int[]{size[0] - 1, size[1] - 1};
                Object attrSteps = dm.getAttribute(root, path, ATTR_SCAN_STEPS);
                if (attrSteps instanceof int[] intSteps) {
                     steps = intSteps;
                }                
                Object passes = dm.getAttribute(root, path, ATTR_SCAN_PASSES);
                    
                double[] xdata = null;
                double[] ydata = null;

                if (positioners > 0) {
                    xdata = new double[records];
                    for (int i = 0; i < records; i++) {
                        xdata[i] = (Double) sliceData[i][0];
                    }
                    if (dimensions > 1) {
                        ydata = new double[records];
                        for (int i = 0; i < records; i++) {
                            ydata[i] = (Double) sliceData[i][1];
                        }
                    }
                }
                Object[][] data = new Object[sensors][records];
                for (int i = 0; i < records; i++) {
                    for (int j = 0; j < sensors; j++) {
                        data[j][i] = (sliceData[i][j + positioners]);
                    }
                }
                for (int j = 0; j < sensors; j++) {
                    String name = names[j + positioners];
                    ret.add(new PlotDescriptor(name, root, path + "/" + name, data[j], xdata, ydata));
                }

                String label = ((positioners > 0) && (names.length > 0)) ? names[0] : null;
                for (PlotDescriptor plot : ret) {
                    plot.labelX = label;
                    plot.steps = steps;
                    if (passes instanceof Number number) {
                        plot.passes = number.intValue();
                    }                    
                }
                return ret;
            }
        }

        //Uses default data manager plot parsing
        return null;
    }

    @Override
    public Object getData(Scan scan, String device, DataManager dm) {
        dm = (dm == null) ? getDataManager() : dm;
        DataAddress scanPath = DataManager.getAddress(scan.getPath());
        Object[] data = null;
        try {
            Map<String, Object> info = dm.getInfo(scanPath.root, scanPath.path);
            String[] names = (String[]) info.get(Format.INFO_FIELD_NAMES);
            int index = Arr.getIndexEqual(  names, device);
            DataSlice slice = dm.getData(scanPath.root, scanPath.path);
            Object[][] sliceData = (Object[][]) slice.sliceData;
            data = Arr.getColumn(sliceData, index);
        } catch (IOException e) {
        }
        return data;
    }

    @Override
    public boolean isScanDataset(String root, String path, DataManager dm) {
        dm = (dm == null) ? getDataManager() : dm;
        if (!path.contains("/")) {
            return false;
        }
        path = path.substring(0, path.lastIndexOf("/"));
        return (dm.getAttribute(root, path, ATTR_SCAN_WRITABLE_DIMS) != null);
    }
}
