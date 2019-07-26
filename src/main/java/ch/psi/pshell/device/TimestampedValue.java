package ch.psi.pshell.device;

/**
 * Entity holding a value and a creation timestamp.
 */
public class TimestampedValue<T> {

    T value;
    long timestamp;
    long nanosOffset;

    /**
     * timestamp: epoch in ms
     */
    public TimestampedValue(T value, long timestamp) {
        this(value, timestamp, 0);
    }

    /**
     * timestamp: epoch in ms
     */
    public TimestampedValue(T value, long timestamp, long nanosOffset) {
        this.value = value;
        this.timestamp = timestamp;
        this.nanosOffset = nanosOffset;
    }

    public T getValue() {
        return value;
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

    protected void setValue(T value) {
        this.value = value;
    }

    protected void setTimestamp(long value) {
        this.timestamp = value;
    }

    protected void setNanosOffset(long value) {
        this.nanosOffset = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
    
    @Override
    public boolean equals(Object obj){
        if ((obj==null) || (!(obj instanceof TimestampedValue))){
            return false;
        }
        TimestampedValue other = (TimestampedValue)obj;
        if (value==null){
            if (other.value!=null){
                return false;
            }
        } else {
            if (!value.equals(other.value)){
                return false;
            }
        }
        if ((timestamp != other.timestamp) || (nanosOffset != other.nanosOffset)){
            return false;
        }
        return true;
    }
}
