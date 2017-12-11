package ch.psi.pshell.scan;

import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.Reflection.Hidden;
import java.util.ArrayList;

/**
 * The ScanRecord class contains the data acquired for one point during a scan. It encapsulates the
 * following fields: - index (int): the sequential index of the present record within the scan. -
 * timestamp (long): the system time in milliseconds after sampling the record sensor. - setpoints
 * (array of numbers): the setpoint for each positioner. - positions (array of numbers): the
 * readback for each positioner. - values (array of Objects): the readout of each sensor in the
 * scan, which can be a scalar or an array).
 */
public class ScanRecord {

    int index;
    long timestamp;
    int dimensions;
    Number[] setpoints;
    Number[] positions;
    Object[] values;
    int pass;

    public int getIndex() {
        return index;
    }

    public int getPass() {
        return pass;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getDimensions() {
        return dimensions;
    }

    public Number[] getSetpoints() {
        return setpoints;
    }

    public Number[] getPositions() {
        return positions;
    }

    public Object[] getValues() {
        return values;
    }

    @Hidden
    public ScanRecord copy() {
        ScanRecord ret = new ScanRecord();
        ret.index = index;
        ret.pass = pass;
        ret.timestamp = timestamp;
        ret.dimensions = dimensions;
        ret.setpoints = Arr.copy(setpoints);
        ret.positions = Arr.copy(positions);
        ret.values = Arr.copy(values);
        return ret;
    }

    @Hidden
    public ScanRecord copy(int maxValueRank) {
        ScanRecord ret = copy();
        if (ret.values != null) {
            for (int i = ret.values.length - 1; i >= 0; i--) {
                if (Arr.getRank(ret.values[i]) > 1) {
                    ret.values = Arr.remove(ret.values, i);
                }
            }
        }
        return ret;
    }

    /*
     public int[] getValuesRank(){
     int[] ret = new int[values.length];
     for (int i=0; i<values.length; i++){
     ret[i] = Arr.getRank(values[i]);
     }
     return ret;
     }   
     */
    @Override
    public String toString() {
        return print();
    }

    boolean invalidated;

    boolean canceled;

    public String print() {
        return print("\t");
    }

    public String print(String separator) {
        StringBuilder sb = new StringBuilder();
        String time = Chrono.getTimeStr(getTimestamp(), "HH:mm:ss.SSS");
        ArrayList<String> values = new ArrayList<>();
        values.add(time);
        values.add(String.format("%04d", getIndex()));
        for (Number value : getPositions()) {
            values.add(String.valueOf(value));
        }
        for (Object value : getValues()) {
            if ((value != null) && (value.getClass().isArray())) {
                int[] shape = Arr.getShape(value);
                if (shape.length == 1) {
                    values.add("[" + String.join(", ", Convert.toStringArray(value)) + "]");
                } else {
                    values.add("[" + Convert.arrayToString(shape, " x ") + "]");
                }

            } else {
                values.add(String.valueOf(value));
            }
        }
        return String.join(separator, values);
    }

    /**
     * Flags the scan to re-sample current scan record
     */
    public void invalidate() {
        invalidated = true;
    }

    /**
     * Flags this record to be discarded.
     */
    public void cancel() {
        canceled = true;
    }

    public Object __getitem__(int index) {
        return values[index];
    }

    public Object __len__() {
        return values.length;
    }

}
