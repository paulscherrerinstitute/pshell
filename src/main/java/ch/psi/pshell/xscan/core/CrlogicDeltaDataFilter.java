package ch.psi.pshell.xscan.core;

/**
 * Filter calculating the delta of two subsequent values
 */
public class CrlogicDeltaDataFilter {

    private Double lastValue = null;

    public void reset() {
        lastValue = null;
    }

    public Double delta(Double value) {

        Double lvalue = lastValue;
        lastValue = value;

        if (lvalue == null) {
            return Double.NaN;
        } else {
            // Return delta
            return (value - lvalue);
        }
    }
}
