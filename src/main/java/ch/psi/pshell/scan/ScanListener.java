package ch.psi.pshell.scan;

import ch.psi.pshell.device.Device;

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
    
    default void onMonitor(Scan scan, Device dev, Object value, long timestamp) {
    }    
}
