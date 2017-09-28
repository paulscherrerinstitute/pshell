package ch.psi.pshell.scan;

import java.io.IOException;

/**
 *
 */
public class ScanAbortedException extends IOException {

    ScanAbortedException() {
        super("Scan was aborted");
    }
}
