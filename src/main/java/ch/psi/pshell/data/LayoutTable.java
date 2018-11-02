package ch.psi.pshell.data;

import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.scan.AreaScan;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.utils.Convert;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This data layout stores all positioners and sensors in a single table
 */
public class LayoutTable extends LayoutBase implements Layout {

    public static final String ATTR_SCAN_WRITABLE_DIMS = "Writable Dims";

    @Override
    public void initialize() {
        if (getDataManager().getProvider() instanceof ProviderText) {
            ((ProviderText) getDataManager().getProvider()).setItemSeparator("\t");
            ((ProviderText) getDataManager().getProvider()).setArraySeparator(" ");
        }
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
    public String getLogFilePath() {
        return getDefaultGroup(null) + getLogFileName();
    }

    protected String getLogFileName() {
        return "logs";
    }

    protected String getDatasetName(Scan scan) {
        return scan.getTag();
    }

    @Override
    public void onStart(Scan scan) throws IOException {
        initialize(); //provider may have changed
        int scanIndex = getDataManager().getScanIndex();
        String path = getScanPath(scan);

        int fields = scan.getWritables().length + scan.getReadables().length;
        String[] fieldNames = new String[fields];
        Class[] fieldTypes = new Class[fields];
        int[] fieldLength = new int[fields];
        int[] writableDims = new int[scan.getWritables().length];

        int index = 0;
        int dimension = (scan instanceof AreaScan) ? scan.getWritables().length : 1;
        for (Writable writable : scan.getWritables()) {
            fieldTypes[index] = Double.class;
            writableDims[index] = (dimension > 1) ? dimension-- : dimension;
            fieldNames[index++] = getDataManager().getAlias(writable);
        }
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            Class type = getDeviceDatasetType(readable);
            if (readable instanceof ReadableMatrix) {
                fieldTypes[index] = Array.newInstance(Convert.getPrimitiveClass(type), new int[]{0, 0}).getClass();
                fieldLength[index] = ((ReadableMatrix) readable).getHeight();
            } else if (readable instanceof ReadableArray) {
                fieldTypes[index] = Array.newInstance(Convert.getPrimitiveClass(type), 0).getClass();
                fieldLength[index] = ((ReadableArray) readable).getSize();
            } else {
                fieldTypes[index] = type;
            }
            fieldNames[index++] = getDataManager().getAlias(readable);
        }

        getDataManager().createDataset(path, fieldNames, fieldTypes, fieldLength);
        getDataManager().setAttribute(path, ATTR_SCAN_WRITABLE_DIMS, (writableDims.length > 0) ? writableDims : new int[]{-1});

        super.onStart(scan);
    }

    @Override
    public void onRecord(Scan scan, ScanRecord record) throws IOException {
        int index = 0;
        int deviceIndex = 0;
        Number[] positions = record.getPositions();
        Object[] values = record.getValues();
        int fields = scan.getWritables().length + scan.getReadables().length;
        Object[] data = new Object[fields];

        for (Writable writable : scan.getWritables()) {
            data[index++] = positions[deviceIndex++];
        }

        deviceIndex = 0;
        for (ch.psi.pshell.device.Readable readable : scan.getReadables()) {
            data[index++] = values[deviceIndex++];
        }
        getDataManager().setItem(getScanPath(scan), data, getIndex(scan, record));
    }

    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        dm = (dm == null) ? getDataManager() : dm;
        Map<String, Object> info = dm.getInfo(root, path);
        if ((String.valueOf(info.get(Provider.INFO_TYPE)).equals(Provider.INFO_VAL_TYPE_DATASET))) {
            if (info.get(Provider.INFO_DATA_TYPE) == Provider.INFO_VAL_DATA_TYPE_COMPOUND) {
                ArrayList<PlotDescriptor> ret = new ArrayList<>();
                DataSlice slice = dm.getData(root, path);
                Object[][] sliceData = (Object[][]) slice.sliceData;
                int records = slice.sliceShape[0];
                String[] names = (String[]) info.get(Provider.INFO_FIELD_NAMES);
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

                if (dimensions == 2) {
                    ArrayList<Double> xarr = new ArrayList<>();
                    ArrayList<Double> yarr = new ArrayList<>();
                    for (int i = 0; i < records; i++) {
                        if (!sliceData[0][0].equals(sliceData[i][0])) {
                            break;
                        }
                        yarr.add((Double) sliceData[i][1]);
                    }
                    for (int j = 0; j < records; j += yarr.size()) {
                        xarr.add((Double) sliceData[j][0]);
                    }
                    double[] xdata = (double[]) Convert.toPrimitiveArray(xarr.toArray(new Double[0]));
                    double[] ydata = (double[]) Convert.toPrimitiveArray(yarr.toArray(new Double[0]));

                    for (int k = 0; k < sensors; k++) {
                        String name = names[k + positioners];
                        if (sliceData[0][k + positioners].getClass().isArray()) {
                            int size = Array.getLength(sliceData[0][k + positioners]);
                            double[][][] array = new double[size][ydata.length][xdata.length];
                            //TODO: FIXME, plot 2s scan of waveform in the same way as LayoutDefault
                            for (int i = 0; i < xdata.length; i++) {
                                for (int j = 0; j < ydata.length; j++) {
                                    for (int l = 0; l < size; l++) {
                                        array[l][j][i] = ((double[]) sliceData[i * ydata.length + j][k + positioners])[l];
                                    }
                                }
                            }
                            ret.add(new PlotDescriptor(name, root, path + "/" + name, array, xdata, ydata));
                        } else {
                            double[][] array = new double[xdata.length][ydata.length];
                            for (int i = 0; i < xdata.length; i++) {
                                for (int j = 0; j < ydata.length; j++) {
                                    array[i][j] = (Double) sliceData[i * ydata.length + j][k + positioners];
                                }
                            }
                            ret.add(new PlotDescriptor(name, root, path + "/" + name, array, xdata, ydata));
                        }
                    }
                } else {
                    double[] xdata = null;
                    if (positioners > 0) {
                        xdata = new double[records];
                        for (int i = 0; i < records; i++) {
                            xdata[i] = (Double) sliceData[i][0];
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
                        ret.add(new PlotDescriptor(name, root, path + "/" + name, data[j], xdata));
                    }
                }
                String label = ((positioners > 0) && (names.length > 0)) ? names[0] : null;
                for (PlotDescriptor plot : ret) {
                    plot.labelX = label;
                }
                return ret;
            }
        }

        //Uses default data manager plot parsing
        return null;
    }

    @Override
    public boolean isScanDataset(String root, String path, DataManager dm) {
        if (!path.contains("/")) {
            return false;
        }
        path = path.substring(0, path.lastIndexOf("/"));
        return (dm.getAttribute(root, path, ATTR_SCAN_WRITABLE_DIMS) != null);
    }
}
