package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanRecord;
import java.io.IOException;
import java.util.List;

/**
 * Layout implementations define the structure of the acquired data on file.
 */
public interface Layout {

    void initialize();

    String getDefaultGroup(Scan scan);

    default String getCurrentGroup(Scan scan) {
        String ret;
        Object group = Context.getInstance().getExecutionPars().getOption("group");
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

    default public boolean getPreserveTypes() {
        return getDataManager().getPreserveTypes();
    }

    default public Class getDeviceType(Object device) throws IOException {
        return getDataManager().getScanDeviceDatasetType(device);
    }

    default public int getIndex(Scan scan, ScanRecord record) throws IOException {
        return record.getIndex() - scan.getRecordIndexOffset();
    }
   
}
