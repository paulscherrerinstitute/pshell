package ch.psi.pshell.scan;

import ch.psi.pshell.core.Nameable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Readable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ScanResult objects package all the acquired data during a scan.
 */
public class ScanResult {

    final ArrayList<ScanRecord> records;
    final Scan scan;

    ScanResult(Scan scan) {
        this.scan = scan;
        records = new ArrayList<>();
    }

    public List<ScanRecord> getRecords() {
        return records;
    }

    public List<Object> getReadable(int index) {
        if ((index < 0) || (index >= getReadables().size())) {
            throw new IllegalArgumentException("Index");
        }

        ArrayList<Object> ret = new ArrayList<>();
        for (ScanRecord record : records) {
            ret.add(record.values[index]);
        }
        return ret;
    }

    public List<Number> getSetpoints(int index) {
        if ((index < 0) || (index >= getWritables().size())) {
            throw new IllegalArgumentException("Index");
        }

        ArrayList<Number> ret = new ArrayList<>();
        for (ScanRecord record : records) {
            ret.add(record.setpoints[index]);
        }
        return ret;
    }

    public List<Number> getPositions(int index) {
        if ((index < 0) || (index >= getWritables().size())) {
            throw new IllegalArgumentException("Index");
        }

        ArrayList<Number> ret = new ArrayList<>();
        for (ScanRecord record : records) {
            ret.add(record.positions[index]);
        }
        return ret;
    }

    public int getSize() {
        return records.size();
    }

    public List<Readable> getReadables() {
        return Arrays.asList(scan.getReadables());
    }

    public List<Writable> getWritables() {
        return Arrays.asList(scan.getWritables());
    }

    public int getIndex() {
        return scan.getIndex();
    }

    public String getRoot() {
        return (scan.getPath() == null) ? null : getPath().split("\\|")[0].trim();
    }

    public String getGroup() {
        return (scan.getPath() == null) ? null : getPath().split("\\|")[1].trim();
    }

    public String getPath() {
        return scan.getPath();
    }

    public int getDimensions() {
        return scan.getDimensions();
    }

    public String print() {
        return print("\t");
    }

    int errorCode = 0;

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns a table of the scan results: the column names are "Time", "Index", positioners' names
     * and sensors' names.
     */
    public String print(String separator) {
        String lineSeparator = "\n";
        StringBuilder sb = new StringBuilder();

        sb.append(scan.getHeader(separator)).append(lineSeparator);
        ArrayList<String> data = new ArrayList<>();
        for (ScanRecord record : records) {
            sb.append(record.print(separator)).append(lineSeparator);
        }
        return sb.toString();
    }

    boolean completed;

    public boolean isCompleted() {
        return completed;
    }

    public long getTimeElapsed() {
        return records.get(records.size() - 1).getTimestamp() - records.get(0).getTimestamp();
    }
    
    public Scan getScan(){
        return scan;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(scan.toString() + ": ");
        if (scan != null) {
            ArrayList<String> names = new ArrayList<>();
            for (Nameable nameable : scan.getWritables()) {
                names.add(nameable.getName());
            }
            sb.append("positioners=[").append(String.join(",", names)).append("], ");
            names.clear();
            for (Nameable nameable : scan.getReadables()) {
                names.add(nameable.getName());
            }
            sb.append("sensors=[").append(String.join(",", names)).append("], ");
        }
        sb.append("records=").append(getSize()).append("/").append(scan.getNumberOfRecords());
        return sb.toString();
    }

    public Object __getitem__(int key) {
        return records.get(key);
    }

    public Object __len__() {
        return records.size();
    }

}
