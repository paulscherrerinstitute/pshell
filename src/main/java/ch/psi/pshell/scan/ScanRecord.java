package ch.psi.pshell.scan;

import ch.psi.pshell.scripting.Subscriptable;
import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.Reflection.Hidden;
import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * The ScanRecord class contains the data acquired for one point during a scan. It encapsulates the
 * following fields: - index (int): the sequential index of the present record within the scan. -
 * timestamp (long): the system time in milliseconds after sampling the record sensor. - setpoints
 * (array of numbers): the setpoint for each positioner. - positions (array of numbers): the
 * readback for each positioner. - values (array of Objects): the readout of each sensor in the
 * scan, which can be a scalar or an array).
 */
public class ScanRecord implements Subscriptable.MappedSequence<Object,Object>{

    int index;   
    int dimensions;
    int indexInPass;
    Number[] setpoints;
    Number[] positions;
    Object[] values;
    long localTimestamp; //Always set  
    Long timestamp;   
    Long[] deviceTimestamps;
    int pass;
    long id;
    boolean sent;
    Scan scan;

    public int getIndex() {
        return index;
    }

    public int getPass() {
        return pass;
    }
    
    public int getIndexInPass() {
        return indexInPass;
    }      
    
    @Transient
    public int getDimensions() {
        return scan.getDimensions();
    }

    public Number[] getSetpoints() {
        return setpoints;
    }

    public Number[] getPositions() {
        return positions;
    }

    public Object[] getReadables() {
        return values;
    }

    //Overridden for SubscriptableArray
    //Deprecated to avoid confusion with values() method of Dict class: ScanRecord.values is not valid any more
    @Hidden
    @Deprecated
    @Transient
    public List getValues() { return values(); }

    public Number getSetpoint(Object index) {
        return setpoints[scan.getWritableIndex(index)];
    }

    public Number getPosition(Object index) {
        return positions[scan.getWritableIndex(index)];
    }

    public Object getReadable(Object index) {
        return values[scan.getReadableIndex(index)];
    }

    @Hidden
    @Deprecated
    public Object getValue(Object index) { return __getitem__(index);}

    public long getTimestamp() {
        return (timestamp == null) ? localTimestamp : timestamp;
    }
    
    public long getLocalTimestamp() {
        return localTimestamp;
    }
    
    public Long getRemoteTimestamp() {
        return timestamp;
    }  
    
    public Long[] getDeviceTimestamps() {
        return deviceTimestamps;
    }    
    
    public long getId() {
        return id;
    }
    
    @Hidden
    public void setId(long value) {
        id = value;
    }   
    
    @Transient
    public boolean isSent() {
        return sent;
    }    
    
    @Hidden
    public ScanRecord copy() {
        ScanRecord ret = new ScanRecord();
        ret.scan = this.scan;
        ret.index = index;
        ret.indexInPass = indexInPass;
        ret.pass = pass;
        ret.setpoints = Arr.copy(setpoints);
        ret.positions = Arr.copy(positions);
        ret.values = Arr.copy(values);
        ret.id = id;
        ret.timestamp = timestamp;
        ret.localTimestamp = localTimestamp;
        ret.deviceTimestamps = Arr.copy(deviceTimestamps);
        ret.sent = sent;
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
        for (Object value : getReadables()) {
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

    @Hidden
    @Override
    public int toItemIndex(Object key) {
        return scan.getDeviceIndex(key);
    }

    @Hidden
    @Override
    public List<Object> getKeys() {
        return Arrays.asList(Convert.toObjectArray(scan.getDeviceNames()));
    }

    @Hidden
    @Override
    public Object getItem(int index){
        if (index<values.length) {
            return values[index];
        }
        index-=values.length;
        if (index<positions.length) {
            return positions[index];
        }
        throw new IllegalArgumentException("Index");
    }

    @Hidden
    @Override
    @Transient
    public int getLenght() {
        return scan.getDevices().length;
    }
}
