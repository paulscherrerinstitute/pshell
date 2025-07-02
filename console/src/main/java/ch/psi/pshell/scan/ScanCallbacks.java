package ch.psi.pshell.scan;

/**
 * The interface for receiving scan callbacks.
 */
public interface ScanCallbacks {
    void onBeforeScan(Scan scan);
    void onAfterScan(Scan scan);      
    void onBeforeReadout(Scan scan, double[] pos);
    void onAfterReadout(Scan scan, ScanRecord record);
    void onBeforePass(Scan scan, int pass);
    void onAfterPass(Scan scan, int pass);
    void onBeforeRegion(Scan scan, int region);  
}
