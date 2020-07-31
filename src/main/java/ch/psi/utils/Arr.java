package ch.psi.utils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.math.BigInteger;

/**
 * Array manipulation utilities.
 */
public class Arr {

    static <T> T[] newArray(T[] original, int size) {

        T[] ret = (T[]) Array.newInstance(original.getClass().getComponentType(), size);
        return ret;
    }

    public static <T> T[] remove(T[] array, int index) {
        if ((array == null) || (index < 0) || (index >= array.length)) {
            return null;
        }
        T[] ret = newArray(array, array.length - 1);
        System.arraycopy(array, 0, ret, 0, index);
        System.arraycopy(array, index + 1, ret, index, array.length - index - 1);
        return ret;
    }

    public static <T> T[] remove(T[] array, Object object) {
        if (array == null) {
            return null;
        }
        T[] aux = newArray(array, array.length);
        int index = 0;
        for (T item : array) {
            if (item != object) {
                aux[index] = item;
                index++;
            }
        }
        T[] ret = newArray(array, index);
        System.arraycopy(aux, 0, ret, 0, index);
        return ret;
    }

    public static <T> T[] remove(T[] array, T[] objects) {
        for (Object object : objects) {
            array = remove(array, (Object) object);
        }
        return array;
    }

    public static <T> T[] removeEquals(T[] array, Object object) {
        if (array == null) {
            return null;
        }
        T[] aux = newArray(array, array.length);
        int index = 0;
        for (T item : array) {
            if (!item.equals(object)) {
                aux[index] = item;
                index++;
            }
        }
        T[] ret = newArray(array, index);
        System.arraycopy(aux, 0, ret, 0, index);
        return ret;
    }

    public static <T> T[] remove(T[] array, Class type) {
        if (array == null) {
            return null;
        }
        T[] aux = newArray(array, array.length);
        int index = 0;
        for (T item : array) {
            if ((item == null) || (!type.isAssignableFrom(item.getClass()))) {
                aux[index] = item;
                index++;
            }
        }
        T[] ret = newArray(array, index);
        System.arraycopy(aux, 0, ret, 0, index);
        return ret;
    }

    public static <T> T[] removeNulls(T[] array) {
        return remove(array, (Object) null);
    }

    public static <T> T[] insert(T[] array, T item, int index) {
        if ((array == null) || (index < 0) || (index > array.length)) {
            return null;
        }
        T[] ret = newArray(array, array.length + 1);
        System.arraycopy(array, 0, ret, 0, index);
        ret[index] = item;
        System.arraycopy(array, index, ret, index + 1, array.length - index);
        return ret;
    }

    public static <T> T[] append(T[] array, T item) {
        if (array == null) {
            return null;
        }
        T[] ret = newArray(array, array.length + 1);
        System.arraycopy(array, 0, ret, 0, array.length);
        ret[array.length] = item;
        return ret;
    }

    public static <T> T[] append(T[] array, T[] items) {
        if (array == null) {
            return null;
        }
        if (items == null) {
            return array;
        }
        T[] ret = newArray(array, array.length + items.length);
        System.arraycopy(array, 0, ret, 0, array.length);
        System.arraycopy(items, 0, ret, array.length, items.length);
        return ret;
    }

    public static <T> boolean contains(T[] array, Object item) {
        return getIndex(array, item) >= 0;
    }

    public static <T> boolean containsEqual(T[] array, Object item) {
        return getIndexEqual(array, item) >= 0;
    }
    
    public static <T> boolean containsClass(T[] array, Class cls) {
        return getIndexClass(array, cls) >= 0;
    }
    
    public static <T> boolean containsAll(T[] array, Object[] items) {
        for (Object item : items){
            if (!contains(array, item)){
                return false;
            }
        }
        return true;
    }

    public static <T> boolean containsAllEqual(T[] array, Object[] items) {
        for (Object item : items){
            if (!containsEqual(array, item)){
                return false;
            }
        }
        return true;
    }        

    public static <T> boolean containsAny(T[] array, Object[] items) {
        for (Object item : items){
            if (contains(array, item)){
                return true;
            }
        }
        return false;
    }

    public static <T> boolean containsAnyEqual(T[] array, Object[] items) {
        for (Object item : items){
            if (containsEqual(array, item)){
                return true;
            }
        }
        return false;
    }        

    public static <T> int getIndex(T[] array, Object item) {
        if ((array == null) || (item == null)) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (array[i] == item) {
                return i;
            }
        }
        return -1;
    }

    public static <T> int getIndexEqual(T[] array, Object item) {
        if ((array == null) || (item == null)) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (item.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }
    
    public static <T> int getIndexClass(T[] array, Class cls) {
        if ((array == null) || (cls == null)) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (array[i]!=null){
                if (cls.isAssignableFrom(array[i].getClass())) {
                    return i;
                }
            }
        }
        return -1;
    }    

    public static <T> T[] getSubArray(T[] array, int index) {
        if (array == null) {
            return null;
        }
        return getSubArray(array, index, array.length - index);
    }

    public static <T> T[] getSubArray(T[] array, int index, int size) {
        if (array == null) {
            return null;
        }
        T[] ret = newArray(array, size);
        System.arraycopy(array, index, ret, 0, size);
        return ret;
    }
    
    public static <T> T[] getSubArray(Object[] array, Class<T> type) {
        if (array == null) {
            return null;
        }
        T[] aux = (T[]) Array.newInstance(type, array.length);
        int index = 0;
        for (Object item : array) {
            if ((item != null) && (type.isAssignableFrom(item.getClass()))) {
                aux[index] = (T) item;
                index++;
            }
        }
        T[] ret = (T[]) Array.newInstance(type, index);
        System.arraycopy(aux, 0, ret, 0, index);
        return ret;
    }    

    public static <T> T[] copy(T[] array) {
        if (array == null) {
            return null;
        }
        T[] ret = newArray(array, array.length);
        System.arraycopy(array, 0, ret, 0, array.length);
        return ret;
    }

    public static String[] sort(String[] data) {
        String[] array = copy(data);
        try {
            java.util.Arrays.sort(array, new java.text.RuleBasedCollator("< a < b < c < d"));
        } catch (Exception ex) {
        }
        return array;
    }

    public static int getRank(Object array) {
        int rank = 0;
        if (array != null) {
            Class type = array.getClass();
            while (type.isArray()) {
                rank++;
                type = type.getComponentType();
            }
        }
        return rank;
    }

    public static <T> T[] getColumn(T[][] matrix, int index) {
        if (matrix==null){
            return null;
        }
        T[] ret = newArray(matrix[0], matrix.length);
        for(int i=0; i<ret.length; i++){
            ret[i] = matrix[i][index];
        }
        return ret;
    }

    /**
     * [..., Y, X]
     */
    public static int[] getShape(Object array) {
        int rank = getRank(array);
        int[] ret = new int[rank];
        Object obj = array;
        for (int i = 0; i < rank; i++) {
            ret[i] = Array.getLength(obj);
            if (ret[i] == 0) {
                break;
            }
            obj = Array.get(obj, 0);
        }
        return ret;
    }

    public static Class getComponentType(Object array) {
        if (array != null) {
            Class type = array.getClass();
            while (type.isArray()) {
                type = type.getComponentType();
            }
            return type;
        }
        return null;
    }

    public static double[] onesDouble(int size) {
        double[] ret = new double[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = 1;
        }
        return ret;
    }

    public static double[] indexesDouble(int size) {
        double[] ret = new double[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = i;
        }
        return ret;
    }

    public static int[] onesInt(int size) {
        int[] ret = new int[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = 1;
        }
        return ret;
    }

    public static int[] indexesInt(int size) {
        int[] ret = new int[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = i;
        }
        return ret;
    }

    public static byte[] onesByte(int size) {
        byte[] ret = new byte[size];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = 1;
        }
        return ret;
    }

    public static byte[] indexesByte(int size) {
        byte[] ret = new byte[size];
        for (byte i = 0; i < ret.length; i++) {
            ret[i] = i;
        }
        return ret;
    }

    public static double[] gradient(double[] array) {
        double[] ret = new double[array.length - 1];
        for (int i = 0; i < array.length - 1; i++) {
            ret[i] = array[i + 1] - array[i];
        }
        return ret;
    }

    public static double[] abs(double[] data) {
        double[] ret = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            ret[i] = Math.abs(data[i]);
        }
        return ret;
    }

    public static List toList(Object array) {
        if (array instanceof List ){
            return (List) array;
        }
        Class type = array.getClass().getComponentType();
        if (type.isPrimitive()) {
            array = Convert.toWrapperArray(array);
            type = array.getClass().getComponentType();
        }
        if (type == Double.class) {
            return Arrays.asList((Double[]) array);
        }
        if (type == Float.class) {
            return Arrays.asList((Float[]) array);
        }
        if (type == BigInteger.class) {
            return Arrays.asList((BigInteger[]) array);
        }
        if (type == Long.class) {
            return Arrays.asList((Long[]) array);
        }
        if (type == Integer.class) {
            return Arrays.asList((Integer[]) array);
        }
        if (type == Short.class) {
            return Arrays.asList((Short[]) array);
        }
        if (type == Integer.class) {
            return Arrays.asList((Integer[]) array);
        }
        if (type == Byte.class) {
            return Arrays.asList((Byte[]) array);
        }
        if (type == Character.class) {
            return Arrays.asList((Character[]) array);
        }
        return Arrays.asList(Convert.toObjectArray(array));
    }

    public static Object getMin(Object array) {
        List list = toList(array);
        return Collections.min(list);
    }

    public static Object getMax(Object array) {
        List list = toList(array);
        return Collections.max(list);
    }

    public static boolean endsWith(byte[] arr, int finalIndex, byte[] trailer) {
        if (finalIndex < 0) {
            finalIndex = arr.length;
        }
        if (finalIndex - trailer.length < 0) {
            return false;
        }
        for (int i = 0; i < trailer.length; i++) {
            if (arr[finalIndex - 1 - i] != trailer[trailer.length - 1 - i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean startsWith(byte[] arr, int startIndex, byte[] header) {
        if (startIndex < 0) {
            startIndex = 0;
        }
        if ((header.length + startIndex) > arr.length) {
            return false;
        }
        for (int i = 0; i < header.length; i++) {
            if (arr[startIndex + i] != header[i]) {
                return false;
            }
        }
        return true;
    }
}
