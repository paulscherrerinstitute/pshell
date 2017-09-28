package ch.psi.pshell.scan;

/**
 * The listener interface for receiving scan events.
 */
public interface ScanListener {

    default void onScanStarted(Scan scan, String plotTitle) {
    }

    default void onNewRecord(Scan scan, ScanRecord record) {
    }

    default void onScanEnded(Scan scan, Exception ex) {
    }
}
