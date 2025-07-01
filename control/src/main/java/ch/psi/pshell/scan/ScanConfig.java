package ch.psi.pshell.scan;


/**
 *
 */
public record ScanConfig (
    boolean autoSave,
    boolean flushRecords,
    boolean releaseRecords,
    boolean preserveTypes,
    boolean saveOutput,
    boolean saveScript,
    boolean saveSetpoints,
    boolean saveTimestamps,
    boolean saveLogs,
    boolean lazyTableCreation) {
        public ScanConfig() {       
                this(true, true, false, true, false, false, true, true, true, false);
       }    
    }

