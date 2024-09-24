package ch.psi.utils;

/**
 * Structure representing a value range, min and max values.
 */
public class Range {

    final public Double min;
    final public Double max;


    public Range(Double min, Double max) {
        this.min = min;
        this.max = max;
    }

    public Range(Integer min, Integer max) {
        this.min = min.doubleValue();
        this.max = max.doubleValue();
    }

    public Range(Long min, Long max) {
        this.min = min.doubleValue();
        this.max = max.doubleValue();
    }

    public Double getExtent() {
        return max - min;
    }
       
    public Double getCentralValue() {
        return (max + min)/2.0;
    }
    
    @Override
    public String toString(){
        return min + " to " + max;
    }
}
