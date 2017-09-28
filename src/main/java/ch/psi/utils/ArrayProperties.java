package ch.psi.utils;

import java.util.Iterator;
import java.util.List;

/**
 * Computes min and max values and index of arrays.
 */
public class ArrayProperties extends Range {

    public int minIndex = -1;
    public int maxIndex = -1;

    public ArrayProperties() {
        super(Double.MAX_VALUE, Double.MIN_VALUE);
    }

    public static ArrayProperties get(Object array) {
        if (Arr.getComponentType(array).isPrimitive()) {
            array = Convert.toWrapperArray(array);
        }
        List list = Arr.toList(array);
        Iterator i = list.iterator();
        ArrayProperties props = new ArrayProperties();
        int index = 0;
        while (i.hasNext()) {
            Double next = ((Number) i.next()).doubleValue();
            if (next.compareTo(props.min) < 0) {
                props.min = next;
                props.minIndex = index;
            }
            if (next.compareTo(props.max) > 0) {
                props.max = next;
                props.maxIndex = index;
            }
            index++;
        }
        return props;
    }

    @Override
    public String toString() {
        return "Min: " + min + " [" + minIndex + "] " + " Max: " + max + " [" + maxIndex + "] ";
    }
}
