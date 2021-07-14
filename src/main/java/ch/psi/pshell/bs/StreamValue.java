package ch.psi.pshell.bs;

import ch.psi.pshell.scripting.Subscriptable;
import ch.psi.utils.Reflection.Hidden;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity containing the current value for a stream, including a list of identifiers, their values,
 * a pulse id and a timestamp.
 */
public class StreamValue extends Number implements Subscriptable.MappedList<String, Object>{

    final long pulseId;
    final long timestamp;
    final long nanosOffset;
    final List<String> identifiers;
    final List values;

    StreamValue(long pulseId, long timestamp, List<String> identifiers, List values) {
        this(pulseId, timestamp, 0, identifiers, values);
    }

    StreamValue(long pulseId, long timestamp, long nanosOffset, List<String> identifiers, List values) {
        this.pulseId = pulseId;
        this.values = values;
        this.timestamp = timestamp;
        this.nanosOffset = nanosOffset;
        this.identifiers = identifiers;
    }

    public long getPulseId() {
        return pulseId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getNanosOffset() {
        return nanosOffset;
    }

    public long getTimestampNanos() {
        return (timestamp * 1000000) + nanosOffset;
    }

    //Backward compatibility
    @Hidden
    public List<String> getIdentifiers() {
        return getKeys();
    }

    @Override
    public java.util.List<String> getKeys(){
        return new ArrayList<>(identifiers);
    }  
    
    @Override
    public List getValues() {
        return new ArrayList(values);
    }

    public Object getValue(String id) {
        return __getitem__(id);
    }

    public Object getValue(int index) {
        return __getitem__(index);
    }

    @Override
    public String toString() {
        return String.valueOf(pulseId);
    }

    @Hidden
    @Override
    public int intValue() {
        return (int) pulseId;
    }

    @Hidden
    @Override
    public long longValue() {
        return pulseId;
    }

    @Hidden
    @Override
    public float floatValue() {
        return pulseId;
    }

    @Hidden
    @Override
    public double doubleValue() {
        return pulseId;
    }

    @Override
    @Hidden
    public int toItemIndex(String itemKey){
        for (int i = 0; i < identifiers.size(); i++) {
            if (identifiers.get(i).equals(itemKey)) {
                return i;
            }
        }
        return -1;
    }      
}
