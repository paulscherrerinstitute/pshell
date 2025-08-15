package ch.psi.pshell.scan;

import ch.psi.pshell.utils.Str;

/**
 *
 */
public class SearchResult extends ScanResult {

    Object optimal = null;
    Object optimalPosition = null;

    public SearchResult(Scan scan) {
        super(scan);
    }

    public Object getOptimalValue() {
        return optimal;
    }

    public Object getOptimalPosition() {
        return optimalPosition;
    }

    @Override
    public String print(String separator) {
        String ret = super.print(separator);
        String opt = "\nOptimal = " + Str.toString(optimal) + "\nPosition = " + Str.toString(optimalPosition);
        return ret + opt;
    }

}
