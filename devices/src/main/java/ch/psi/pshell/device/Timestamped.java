package ch.psi.pshell.device;

/**
 * Interface for objects having a timestamp - the time of the update of its contents.
 */
public interface Timestamped {

    public Long getTimestamp();

    default public Long getTimestampNanos() {
        return getTimestamp() * 1000000;
    }
}
