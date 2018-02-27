package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Layout implementations define the structure of the acquired data on file.
 */
public interface Layout {

    //Common attributes
    public static final String ATTR_NAME = "Name";
    public static final String ATTR_FILE= "File";
    public static final String ATTR_VERSION = "Version";
    public static final String ATTR_COMMAND= "Command";
    public static final String ATTR_LAYOUT = "Layout";

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
    default String getScanPath(Scan scan) {
        return getCurrentGroup(scan);
    }

    String getLogFilePath();

    default void onOpened(File output) throws IOException {
    }

    default void onClosed(File output) throws IOException {
    }

    void onStart(Scan scan) throws IOException;

    void onRecord(Scan scan, ScanRecord record) throws IOException;

    void onFinish(Scan scan) throws IOException;


    default List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        //Uses default data manager plot parsing
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

    default Class getDeviceType(Object device) throws IOException {
        return getDataManager().getScanDeviceDatasetType(device);
    }

    default int getIndex(Scan scan, ScanRecord record) throws IOException {
        return record.getIndex() - scan.getRecordIndexOffset();
    }

}
