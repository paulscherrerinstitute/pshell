package ch.psi.pshell.scan;


/**
 *
 */
public record ScanConfig (
    boolean autoSave,
    boolean flushRecords,
    boolean releaseRecords,
    boolean preserveTypes,
    boolean saveMeta,
    boolean saveTimestamps,
    boolean saveLogs,
    boolean lazyTableCreation) {
    public ScanConfig() {       
        this(true, false, false, false, true, false, true, false);
    }    
}

