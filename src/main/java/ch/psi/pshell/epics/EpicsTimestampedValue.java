package ch.psi.pshell.epics;

import ch.psi.pshell.device.TimestampedValue;

/**
 * A timestamped value containing a pulse ID.
 */
public class EpicsTimestampedValue<T> extends TimestampedValue<T> {

    Severity severity;

    public EpicsTimestampedValue(T value, long timestamp, Severity severity) {
        this(value, timestamp, 0, severity);
    }

    public EpicsTimestampedValue(T value, long timestamp, long nanosOffset, Severity severity) {
        super(value, timestamp, nanosOffset);
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }

    protected void setSeverity(Severity value) {
        this.severity = value;
    }
}
