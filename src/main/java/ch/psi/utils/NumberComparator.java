package ch.psi.utils;

import java.util.Comparator;

/**
 *
 */
public class NumberComparator<T extends Number> implements Comparator<T>{
    final double precision;
    
    public NumberComparator(){
        this(0.0);
    }
    
    public NumberComparator(double precision){
        this.precision = precision;
    }
    
    @Override
    public int compare(T o1, T o2) {
            if ((o1 == null) && (o2 == null)) {
                return 0;
            }
            if ((o1 == null) || (o2 == null)) {
                return 1;
            }
            if (Math.abs(o1.doubleValue() - o2.doubleValue()) <= Math.abs(precision)) {
                return 0;
            }
            return Double.valueOf(o1.doubleValue()).compareTo(o2.doubleValue());
    }
            
}
