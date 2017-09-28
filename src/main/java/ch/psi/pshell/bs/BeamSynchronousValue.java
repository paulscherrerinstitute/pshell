package ch.psi.pshell.bs;

import ch.psi.pshell.device.TimestampedValue;

/**
 * A timestamped value containing a pulse ID.
 */
public class BeamSynchronousValue<T> extends TimestampedValue<T> {

    long pulseId;

    public BeamSynchronousValue(T value, long timestamp, long pulseId) {
        this(value, timestamp, 0, pulseId);
    }

    public BeamSynchronousValue(T value, long timestamp, long nanosOffset, long pulseId) {
        super(value, timestamp, nanosOffset);
        this.pulseId = pulseId;
    }

    public long getPulseId() {
        return pulseId;
    }

    protected void setPulseId(long value) {
        this.pulseId = value;
    }
}
