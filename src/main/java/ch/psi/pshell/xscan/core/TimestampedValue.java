package ch.psi.pshell.xscan.core;

public class TimestampedValue {

    private final Double value;
    private final long timestamp;
    private final long nanosecondsOffset;

    public TimestampedValue(Double value, long timestamp, long nanosecondsOffset) {
        this.value = value;
        this.timestamp = timestamp;
        this.nanosecondsOffset = nanosecondsOffset;
    }

    public Double getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getNanosecondsOffset() {
        return nanosecondsOffset;
    }
}
