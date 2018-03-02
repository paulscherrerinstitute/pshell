package ch.psi.utils;

import java.util.Iterator;
import java.util.List;

/**
 * Computes min and max values and index of arrays.
 */
public class ArrayProperties extends Range {

    public final int minIndex;
    public final int maxIndex;

    public ArrayProperties(double min, double max, int minIndex, int maxIndex) {
        super(min,max);
        this.minIndex = minIndex;
        this.maxIndex =  maxIndex;
        
    }    

    public static ArrayProperties get(Object array) {
        int minIndex = -1;
        int maxIndex = -1;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        if (Arr.getComponentType(array).isPrimitive()) {
            array = Convert.toWrapperArray(array);
        }
        List list = Arr.toList(array);
        Iterator i = list.iterator();
        int index = 0;
        while (i.hasNext()) {
            Double next = ((Number) i.next()).doubleValue();
            if (next.compareTo(min) < 0) {
                min = next;
                minIndex = index;
            }
            if (next.compareTo(max) > 0) {
                max = next;
                maxIndex = index;
            }
            index++;
        }
        return new ArrayProperties(min, max, minIndex, maxIndex);
    }

    @Override
    public String toString() {
        return "Min: " + min + " [" + minIndex + "] " + " Max: " + max + " [" + maxIndex + "] ";
    }
}
