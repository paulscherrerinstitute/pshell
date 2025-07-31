package ch.psi.pshell.utils;

/**
 * Structure representing a value range, min and max values.
 */
public class Range {

    final public Double min;
    final public Double max;


    public Range(Number min, Number max) {
        this.min =(min==null) ? Double.NEGATIVE_INFINITY : min.doubleValue();
        this.max =(max==null) ? Double.POSITIVE_INFINITY : max.doubleValue();
    }
    
    
    boolean contains(Number n) {
        return (n.doubleValue()>=min) && (n.doubleValue()<=max);
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
