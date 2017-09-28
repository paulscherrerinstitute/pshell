package ch.psi.utils;

/**
 * Structure representing a value range, min and max values.
 */
public class Range {

    public Double min;
    public Double max;

    public Range() {
    }

    public Range(Double min, Double max) {
        this.min = min;
        this.max = max;
    }

    public Range(Integer min, Integer max) {
        this.min = min.doubleValue();
        this.max = max.doubleValue();
    }

    public Double getExtent() {
        return max.doubleValue() - min.doubleValue();
    }
}
