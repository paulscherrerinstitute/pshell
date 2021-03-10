package ch.psi.utils;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.math.BigInteger;

/**
 * Utilities to convert scalar and array types.
 */
public class Convert {

    //Math transformations
    public static double roundDouble(double value, int decimals) {
        if ((Double.isInfinite(value)) || (Double.isNaN(value))) {
            return value;
        }
        return Math.round(value * Math.pow(10, decimals)) / Math.pow(10, decimals);
    }

    static public double toDegrees0To360(double value) {
        return value - 360.0 * Math.floor(value / 360.0);
    }

    static public double toDegreesMin180To180(double value) {
        return value - 360.0 * Math.round(value / 360.0);
    }

    static public double toDegreesOffset(double diff) {
        double offset = toDegrees0To360(Math.abs(diff));
        return Math.min(offset, 360.0 - offset);
    }

    /**
     * Integer check less expensive than using Integer.parseInt. String must already be trimmed.
     */
    static public boolean isInteger(String str) {
        if ((str != null) && !str.isEmpty()) {
            for (int i = 0; i < str.length(); i++) {
                if (i == 0 && str.charAt(i) == '-') {
                    if (str.length() == 1) {
                        return false;
                    }
                } else if (Character.digit(str.charAt(i), 10) < 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    //Array type convertion
    public static double[] intToDouble(int[] array) {
        if (array == null) {
            return null;
        }
        double[] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i];
        }
        return ret;
    }

    public static double[] byteToDouble(byte[] array) {
        if (array == null) {
            return null;
        }
        double[] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i];
        }
        return ret;
    }

    public static double[] shortToDouble(short[] array) {
        if (array == null) {
            return null;
        }
        double[] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i];
        }
        return ret;
    }

    public static double[] booleanToDouble(boolean[] array) {
        if (array == null) {
            return null;
        }
        double[] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i] ? 1.0 : 0.0;
        }
        return ret;
    }

    public static double[] longToDouble(long[] array) {
        if (array == null) {
            return null;
        }
        double[] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i];
        }
        return ret;
    }

    public static double[] bigIntToDouble(BigInteger[] array) {
        if (array == null) {
            return null;
        }
        double[] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i].doubleValue();
        }
        return ret;
    }

    public static double[] floatToDouble(float[] array) {
        if (array == null) {
            return null;
        }
        double[] ret = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i];
        }
        return ret;
    }

    public static double[][] intToDouble(int[][] array) {
        if (array == null) {
            return null;
        }
        double[][] ret = new double[array.length][];
        for (int i = 0; i < array.length; i++) {
            ret[i] = intToDouble(array[i]);
        }
        return ret;
    }

    public static double[][] byteToDouble(byte[][] array) {
        if (array == null) {
            return null;
        }
        double[][] ret = new double[array.length][];
        for (int i = 0; i < array.length; i++) {
            ret[i] = byteToDouble(array[i]);
        }
        return ret;
    }

    public static double[][] shortToDouble(short[][] array) {
        if (array == null) {
            return null;
        }
        double[][] ret = new double[array.length][];
        for (int i = 0; i < array.length; i++) {
            ret[i] = shortToDouble(array[i]);
        }
        return ret;
    }

    public static double[][] longToDouble(long[][] array) {
        if (array == null) {
            return null;
        }
        double[][] ret = new double[array.length][];
        for (int i = 0; i < array.length; i++) {
            ret[i] = longToDouble(array[i]);
        }
        return ret;
    }

    public static double[][] floatToDouble(float[][] array) {
        if (array == null) {
            return null;
        }
        double[][] ret = new double[array.length][];
        for (int i = 0; i < array.length; i++) {
            ret[i] = floatToDouble(array[i]);
        }
        return ret;
    }

    public static Object toDouble(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof Number) {
            return ((Number) data).doubleValue();
        }
        if (data instanceof List) {
            data = ((List) data).toArray();
        }
        if ((data instanceof double[])
                || (data instanceof double[][])
                || (data instanceof double[][][])) {
            return data;
        }
        if (data instanceof Boolean) {
            return ((Boolean) data) ? 1.0 : 0.0;
        }
        if (!data.getClass().isArray()) {
            throw new IllegalArgumentException("Type cannot be converted to double: " + data.getClass());
        }

        Class componentType = data.getClass();
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
        }
        if (isWrapperClass(componentType)) {
            return (double[]) toPrimitiveArray(data, Double.class);
        }
        int arrayLength = Array.getLength(data);

        if (data instanceof int[]) {
            return intToDouble((int[]) data);
        } else if (data instanceof byte[]) {
            return byteToDouble((byte[]) data);
        } else if (data instanceof short[]) {
            return shortToDouble((short[]) data);
        } else if (data instanceof long[]) {
            return longToDouble((long[]) data);
        } else if (data instanceof BigInteger[]) {
            return bigIntToDouble((BigInteger[]) data);
        } else if (data instanceof float[]) {
            return floatToDouble((float[]) data);
        } else if (data instanceof boolean[]) {
            return booleanToDouble((boolean[]) data);
        } else if (data instanceof Object[]) {
            Object[] array = (Object[]) data;
            if ((arrayLength == 0) || (array[0] instanceof Number)) {
                double[] ret = new double[arrayLength];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = array[i] == null ? Double.NaN : ((Number) array[i]).doubleValue();
                }
                return ret;
            } else if ((array[0] instanceof double[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = (double[]) array[i];
                }
                return ret;
            } else if ((array[0] instanceof int[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = intToDouble((int[]) array[i]);
                }
                return ret;
            } else if ((array[0] instanceof byte[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = byteToDouble((byte[]) array[i]);
                }
                return ret;
            } else if ((array[0] instanceof short[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = shortToDouble((short[]) array[i]);
                }
                return ret;
            } else if ((array[0] instanceof long[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = longToDouble((long[]) array[i]);
                }
                return ret;
            } else if ((array[0] instanceof BigInteger[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = bigIntToDouble((BigInteger[]) array[i]);
                }
                return ret;
            } else if ((array[0] instanceof float[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = floatToDouble((float[]) array[i]);
                }
                return ret;
            } else if ((array[0] instanceof boolean[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = booleanToDouble((boolean[]) array[i]);
                }
                return ret;
            } else if ((array[0] instanceof Object[])) {
                double[][] ret = new double[arrayLength][];
                for (int i = 0; i < arrayLength; i++) {
                    ret[i] = (double[]) toDouble((Object[]) array[i]);
                }
                return ret;
            }
        } else if (data instanceof int[][]) {
            double[][] ret = new double[arrayLength][];
            for (int i = 0; i < arrayLength; i++) {
                ret[i] = intToDouble(((int[][]) data)[i]);
            }
            return ret;
        } else if (data instanceof byte[][]) {
            double[][] ret = new double[arrayLength][];
            for (int i = 0; i < arrayLength; i++) {
                ret[i] = intToDouble(((int[][]) data)[i]);
            }
            return ret;
        } else if (data instanceof short[][]) {
            double[][] ret = new double[arrayLength][];
            for (int i = 0; i < arrayLength; i++) {
                ret[i] = intToDouble(((int[][]) data)[i]);
            }
            return ret;
        } else if (data instanceof long[][]) {
            double[][] ret = new double[arrayLength][];
            for (int i = 0; i < arrayLength; i++) {
                ret[i] = intToDouble(((int[][]) data)[i]);
            }
            return ret;
        } else if (data instanceof BigInteger[][]) {
            double[][] ret = new double[arrayLength][];
            for (int i = 0; i < arrayLength; i++) {
                ret[i] = bigIntToDouble(((BigInteger[][]) data)[i]);
            }
            return ret;
        } else if (data instanceof float[][]) {
            double[][] ret = new double[arrayLength][];
            for (int i = 0; i < arrayLength; i++) {
                ret[i] = intToDouble(((int[][]) data)[i]);
            }
            return ret;
        } else if (data instanceof boolean[][]) {
            double[][] ret = new double[arrayLength][];
            for (int i = 0; i < arrayLength; i++) {
                ret[i] = booleanToDouble(((boolean[][]) data)[i]);
            }
            return ret;
        }

        return null;
    }

    public static int[] doubleToInt(double[] array) {
        if (array == null) {
            return null;
        }
        int[] ret = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = (int) (Math.round(array[i]));
        }
        return ret;
    }

    //Unsigned 
    public static short toUnsigned(byte value) {
        return (short) ((value < 0) ? 0x100 + value : value);
    }

    public static int toUnsigned(short value) {
        return (value < 0) ? 0x10000 + value : value;
    }

    public static long toUnsigned(int value) {
        return (value < 0) ? 0x100000000L + value : value;
    }

    public static BigInteger toUnsigned(long value) {
        BigInteger bi = BigInteger.valueOf(value);
        
        if (value < 0) {
            bi = bi.add(BigInteger.ONE.shiftLeft(64));
        }
        return bi;
    }

    //Unsigned arrays
    public static short[] toUnsigned(byte[] array) {
        if (array == null) {
            return null;
        }
        short[] ret = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = toUnsigned(array[i]);
        }
        return ret;
    }

    public static int[] toUnsigned(short[] array) {
        if (array == null) {
            return null;
        }
        int[] ret = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = toUnsigned(array[i]);
        }
        return ret;
    }

    public static long[] toUnsigned(int[] array) {
        if (array == null) {
            return null;
        }
        long[] ret = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = toUnsigned(array[i]);
        }
        return ret;
    }

    public static BigInteger[] toUnsigned(long[] array) {
        if (array == null) {
            return null;
        }
        BigInteger[] ret = new BigInteger[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = toUnsigned(array[i]);
        }
        return ret;
    }

    public static Object toUnsigned(Object data) {
        if (data instanceof Byte) {
            return Convert.toUnsigned((byte) data);
        }
        if (data instanceof Short) {
            return Convert.toUnsigned((short) data);
        }
        if (data instanceof Integer) {
            return Convert.toUnsigned((int) data);
        }
        if (data instanceof Integer) {
            return Convert.toUnsigned((long) data);
        }
        if (data instanceof byte[]) {
            return Convert.toUnsigned((byte[]) data);
        }
        if (data instanceof short[]) {
            return Convert.toUnsigned((short[]) data);
        }
        if (data instanceof int[]) {
            return Convert.toUnsigned((int[]) data);
        }
        if (data instanceof byte[][]) {
            short[][] ret = new short[Array.getLength(data)][];
            for (int i = 0; i < Array.getLength(data); i++) {
                ret[i] = Convert.toUnsigned(((byte[][]) data)[i]);
            }
            return ret;
        }
        if (data instanceof short[][]) {
            int[][] ret = new int[Array.getLength(data)][];
            for (int i = 0; i < Array.getLength(data); i++) {
                ret[i] = Convert.toUnsigned(((short[][]) data)[i]);
            }
            return ret;
        }
        if (data instanceof int[][]) {
            long[][] ret = new long[Array.getLength(data)][];
            for (int i = 0; i < Array.getLength(data); i++) {
                ret[i] = Convert.toUnsigned(((int[][]) data)[i]);
            }
            return ret;
        }
        if (data instanceof long[][]) {
            BigInteger[][] ret = new BigInteger[Array.getLength(data)][];
            for (int i = 0; i < Array.getLength(data); i++) {
                ret[i] = Convert.toUnsigned(((long[][]) data)[i]);
            }
            return ret;
        }
        return data;
    }

    //"Casting"
    public static Object cast(byte[] array, Class type) {
        return cast(array, type, ByteOrder.nativeOrder());
    }

    public static Object cast(byte[] array, Class type, ByteOrder order) {
        if (array == null) {
            return null;
        }
        if (type.isArray()) {
            type = type.getComponentType();
        }
        if (type == byte.class) {
            return array;
        }

        //Didn't use ByteBuffer because manually creating new array is faster
        /*
         ByteBuffer buffer = ByteBuffer.wrap(array).order(order);
         if (type == int.class){
         IntBuffer newBuffer = buffer.asIntBuffer();
         int[] ret = new int[newBuffer.remaining()];
         newBuffer.get(ret);
         return ret;
         } else if (type == short.class){
         ShortBuffer newBuffer = buffer.asShortBuffer();
         short[] ret = new short[newBuffer.remaining()];
         newBuffer.get(ret);
         return ret;            
         } else if (type == long.class){
         LongBuffer newBuffer = buffer.asLongBuffer();
         long[] ret = new long[newBuffer.remaining()];
         newBuffer.get(ret);
         return ret;
         } else if (type == float.class){
         FloatBuffer newBuffer = buffer.asFloatBuffer();
         float[] ret = new float[newBuffer.remaining()];
         newBuffer.get(ret);
         return ret;
         } else if (type == double.class){
         DoubleBuffer newBuffer = buffer.asDoubleBuffer();
         double[] ret = new double[newBuffer.remaining()];
         newBuffer.get(ret);
         return ret;
         }         
         */
        if (type == short.class) {
            short[] ret = new short[array.length / 2];
            if (order == ByteOrder.BIG_ENDIAN) {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = (short) ((((int) (array[2 * i]) & 0xff) << 8) + ((int) (array[2 * i + 1]) & 0xff));
                }
            } else {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = (short) ((((int) (array[2 * i + 1]) & 0xff) << 8) + ((int) (array[2 * i]) & 0xff));
                }
            }
            return ret;
        } else if (type == int.class) {
            int[] ret = new int[array.length / 4];
            if (order == ByteOrder.BIG_ENDIAN) {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = (int) ((((long) (array[4 * i]) & 0xff) << 24) + (((long) (array[4 * i + 1]) & 0xff) << 16)
                            + (((long) (array[4 * i + 2]) & 0xff) << 8) + ((long) (array[4 * i + 3]) & 0xff));
                }
            } else {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = (int) ((((long) (array[4 * i + 3]) & 0xff) << 24) + (((long) (array[4 * i + 2]) & 0xff) << 16)
                            + (((long) (array[4 * i + 1]) & 0xff) << 8) + ((long) (array[4 * i]) & 0xff));
                }
            }
            return ret;
        } else if (type == long.class) {
            long[] ret = new long[array.length / 8];
            if (order == ByteOrder.BIG_ENDIAN) {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = ((((long) (array[8 * i]) & 0xff) << 56) + (((long) (array[8 * i + 1]) & 0xff) << 48)
                            + (((long) (array[8 * i + 2]) & 0xff) << 40) + (((long) (array[8 * i + 3]) & 0xff) << 32)
                            + (((long) (array[8 * i + 4]) & 0xff) << 24) + (((long) (array[8 * i + 4]) & 0xff) << 16)
                            + (((long) (array[8 * i + 6]) & 0xff) << 8) + ((long) (array[8 * i + 7]) & 0xff));
                }
            } else {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = ((((long) (array[8 * i + 7]) & 0xff) << 56) + (((long) (array[8 * i + 6]) & 0xff) << 48)
                            + (((long) (array[8 * i + 5]) & 0xff) << 40) + (((long) (array[8 * i + 4]) & 0xff) << 32)
                            + (((long) (array[8 * i + 3]) & 0xff) << 24) + (((long) (array[8 * i + 2]) & 0xff) << 16)
                            + (((long) (array[8 * i + 1]) & 0xff) << 8) + ((long) (array[8 * i]) & 0xff));
                }
            }
            return ret;
        } else if (type == float.class) {
            float[] ret = new float[array.length / 4];
            if (order == ByteOrder.BIG_ENDIAN) {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = Float.intBitsToFloat((int) ((((long) (array[4 * i]) & 0xff) << 24) + (((long) (array[4 * i + 1]) & 0xff) << 16)
                            + (((long) (array[4 * i + 2]) & 0xff) << 8) + ((long) (array[4 * i + 3]) & 0xff)));
                }
            } else {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = Float.intBitsToFloat((int) ((((long) (array[4 * i + 3]) & 0xff) << 24) + (((long) (array[4 * i + 2]) & 0xff) << 16)
                            + (((long) (array[4 * i + 1]) & 0xff) << 8) + ((long) (array[4 * i]) & 0xff)));
                }
            }
            return ret;
        } else if (type == double.class) {
            double[] ret = new double[array.length / 8];
            if (order == ByteOrder.BIG_ENDIAN) {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = Double.longBitsToDouble(((((long) (array[8 * i]) & 0xff) << 56) + (((long) (array[8 * i + 1]) & 0xff) << 48)
                            + (((long) (array[8 * i + 2]) & 0xff) << 40) + (((long) (array[8 * i + 3]) & 0xff) << 32)
                            + (((long) (array[8 * i + 4]) & 0xff) << 24) + (((long) (array[8 * i + 4]) & 0xff) << 16)
                            + (((long) (array[8 * i + 6]) & 0xff) << 8) + ((long) (array[8 * i + 7]) & 0xff)));
                }
            } else {
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = Double.longBitsToDouble(((((long) (array[8 * i + 7]) & 0xff) << 56) + (((long) (array[8 * i + 6]) & 0xff) << 48)
                            + (((long) (array[8 * i + 5]) & 0xff) << 40) + (((long) (array[8 * i + 4]) & 0xff) << 32)
                            + (((long) (array[8 * i + 3]) & 0xff) << 24) + (((long) (array[8 * i + 2]) & 0xff) << 16)
                            + (((long) (array[8 * i + 1]) & 0xff) << 8) + ((long) (array[8 * i]) & 0xff)));
                }
            }
            return ret;
        }

        throw new IllegalArgumentException("Unsuppored tye");
    }

    /**
     * Flattens a multi-dimentional array. If the array is already one-dimentional, returns a copy
     * of the array.
     */
    public static Object flatten(Object array) {
        if (array == null) {
            return null;
        }
        if (array instanceof List) {
            array = ((List) array).toArray();
        }    
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array");
        }

        Class type = Arr.getComponentType(array);
        int[] shape = Arr.getShape(array);
        int elements = 1;
        for (int i : shape) {
            elements *= i;
        }
        Object ret = Array.newInstance(type, elements);

        if (shape.length == 2) {
            //For 2d arrays (most usual conversion), use a faster implementation
            int width = shape[1];
            int height = shape[0];
            for (int i = 0; i < height; i++) {
                System.arraycopy(Array.get(array, i), 0, ret, i * width, width);
            }
        } else {
            //The generic implementation
            int index = 0;
            int[] pos = new int[shape.length - 1];
            int chunck = shape[shape.length - 1];

            while (index < elements) {
                Object arr = array;
                for (int i = 0; i < pos.length; i++) {
                    arr = Array.get(arr, pos[i]);
                }
                System.arraycopy(arr, 0, ret, index, chunck);
                index += chunck;
                for (int i = pos.length - 1; i >= 0; i--) {
                    pos[i]++;
                    if (pos[i] < shape[i]) {
                        break;
                    }
                    pos[i] = 0;
                }
            }
        }
        return ret;
    }

    /**
     * Changes dimensionality of array. Number of elements of destination must coincide with the
     * origin array. If array has already the destination shape, returns a copy of the array. The
     * contiguous dimension correspond to the highest index e.g: depth, height, width
     */
    public static Object reshape(Object array, int... shape) {
        if (array == null) {
            return null;
        }
        if (array instanceof List) {
            array = ((List) array).toArray();
        } 
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array");
        }

        if (Arr.getRank(array) > 1) {
            array = flatten(array);
        }

        int elements = 1;
        for (int i : shape) {
            elements *= i;
        }
        if (elements > Array.getLength(array)) {
            throw new IllegalArgumentException("Invalid array shape");
        }

        Class type = Arr.getComponentType(array);
        Object ret = Array.newInstance(type, shape);
        int index = 0;

        if (shape.length == 2) {
            //For 2d arrays (most usual conversion), use a faster implementation
            int width = shape[1];
            int height = shape[0];
            for (int i = 0; i < height; i++) {
                Object row = Array.newInstance(type, width);
                System.arraycopy(array, index, row, 0, width);
                Array.set(ret, i, row);
                index += width;
            }
        } else {
            //The generic implementation
            int[] pos = new int[shape.length - 1];
            int chunck = shape[shape.length - 1];

            while (index < elements) {
                Object arr = ret;
                for (int i = 0; i < pos.length; i++) {
                    arr = Array.get(arr, pos[i]);
                }
                System.arraycopy(array, index, arr, 0, chunck);
                index += chunck;
                for (int i = pos.length - 1; i >= 0; i--) {
                    pos[i]++;
                    if (pos[i] < shape[i]) {
                        break;
                    }
                    pos[i] = 0;
                }
            }
        }
        return ret;
    }

    public static Class getWrapperClass(Class primitiveType) {
        if (primitiveType.isPrimitive()) {
            if (primitiveType == boolean.class) {
                return Boolean.class;
            }
            if (primitiveType == byte.class) {
                return Byte.class;
            }
            if (primitiveType == char.class) {
                return Character.class;
            }
            if (primitiveType == double.class) {
                return Double.class;
            }
            if (primitiveType == float.class) {
                return Float.class;
            }
            if (primitiveType == int.class) {
                return Integer.class;
            }
            if (primitiveType == long.class) {
                return Long.class;
            }
            if (primitiveType == short.class) {
                return Short.class;
            }
        }
        return null;
    }

    public static Class getPrimitiveClass(Class wrapperClass) {
        if (wrapperClass == Boolean.class) {
            return boolean.class;
        }
        if (wrapperClass == Byte.class) {
            return byte.class;
        }
        if (wrapperClass == Character.class) {
            return char.class;
        }
        if (wrapperClass == Double.class) {
            return double.class;
        }
        if (wrapperClass == Float.class) {
            return float.class;
        }
        if (wrapperClass == Integer.class) {
            return int.class;
        }
        if (wrapperClass == Long.class) {
            return long.class;
        }
        if (wrapperClass == Short.class) {
            return short.class;
        }
        return null;
    }

    public static boolean isWrapperClass(Class wrapperClass) {
        return (getPrimitiveClass(wrapperClass) != null);
    }

    public static String arrayToString(Object array, String separator) {
        return arrayToString(array, separator, -1);
    }

    public static String arrayToString(Object array, String separator, int maxElements) {
        if (array instanceof List) {
            array = ((List) array).toArray();
        }         
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array");
        }
        StringJoiner sj = new StringJoiner(separator);
        int length = Array.getLength(array);
        boolean truncated = false;
        if ((maxElements >= 0) && (maxElements < length)) {
            length = maxElements;
            truncated = true;
        }
        for (int i = 0; i < length; i++) {
            Object val = Array.get(array, i);
            sj.add(String.valueOf(val));
        }
        if (truncated) {
            sj.add("...");
        }
        return sj.toString();
    }

    public static String arrayToHexString(Object array, String separator) {
        return arrayToHexString(array, separator, -1);
    }

    public static String arrayToHexString(Object array, String separator, int maxElements) {
        if (array instanceof List) {
            array = ((List) array).toArray();
        }         
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array");
        }
        StringJoiner sj = new StringJoiner(separator);
        int length = Array.getLength(array);
        boolean truncated = false;
        if ((maxElements >= 0) && (maxElements < length)) {
            length = maxElements;
            truncated = true;
        }
        for (int i = 0; i < length; i++) {
            Object val = Array.get(array, i);
            sj.add(String.format("%02X", val));
        }
        if (truncated) {
            sj.add("...");
        }
        return sj.toString();
    }

    public static String[] toStringArray(Object array) {
        if (array instanceof List) {
            array = ((List) array).toArray();
        }         
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array");
        }
        int length = Array.getLength(array);
        String[] ret = new String[length];
        for (int i = 0; i < length; i++) {
            Object val = Array.get(array, i);
            ret[i] = (val == null) ? null : String.valueOf(val);
        }
        return ret;
    }

    public static Object[] toObjectArray(Object array) {
        if (array instanceof List) {
            array = ((List) array).toArray();
        }         
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array");
        }
        int length = Array.getLength(array);
        Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            ret[i] = Array.get(array, i);
        }
        return ret;
    }

    public static Object toPrimitiveArray(String str, String separator, Class componentType) {
        if (str.startsWith(separator)) {
            str = str.substring(separator.length());
        }
        return toPrimitiveArray(str.split(separator), componentType);
    }

    public static Object toPrimitiveArray(String[] tokens, Class componentType) {
        if (!componentType.isPrimitive()) {
            throw new IllegalArgumentException("Not a primitive type");
        }
        Object ret = Array.newInstance(componentType, tokens.length);

        if (componentType == double.class) {
            for (int i = 0; i < tokens.length; i++) {
                ((double[]) ret)[i] = (double) Double.valueOf(tokens[i]);
            }
        } else if (componentType == float.class) {
            for (int i = 0; i < tokens.length; i++) {
                ((float[]) ret)[i] = (float) Float.valueOf(tokens[i]);
            }
        } else if (componentType == long.class) {
            for (int i = 0; i < tokens.length; i++) {
                ((long[]) ret)[i] = (long) Long.valueOf(tokens[i]);
            }
        } else if (componentType == int.class) {
            for (int i = 0; i < tokens.length; i++) {
                ((int[]) ret)[i] = (int) Integer.valueOf(tokens[i]);
            }
        } else if (componentType == short.class) {
            for (int i = 0; i < tokens.length; i++) {
                ((short[]) ret)[i] = (short) Short.valueOf(tokens[i]);
            }
        } else if (componentType == byte.class) {
            for (int i = 0; i < tokens.length; i++) {
                ((byte[]) ret)[i] = (byte) Byte.valueOf(tokens[i]);
            }
        } else if (componentType == boolean.class) {
            for (int i = 0; i < tokens.length; i++) {
                ((boolean[]) ret)[i] = (boolean) Boolean.valueOf(tokens[i]);
            }
        } else if (componentType == char.class) {
            for (int i = 0; i < tokens.length; i++) {
                ((char[]) ret)[i] = tokens[i].charAt(0);
            }
        }
        return ret;
    }

    public static Number stringToNumber(String str, Class type) {
        if (type.isPrimitive()) {
            type = getWrapperClass(type);
        }
        if (type == Double.class) {
            return Double.valueOf(str);
        } else if (type == Float.class) {
            return Float.valueOf(str);
        } else if (type == Long.class) {
            return Long.valueOf(str);
        } else if (type == Integer.class) {
            return Integer.valueOf(str);
        } else if (type == Short.class) {
            return Short.valueOf(str);
        } else if (type == Byte.class) {
            return Byte.valueOf(str);
        } else {
            throw new IllegalArgumentException("Not a number type");
        }
    }

    public static Number toType(Number number, Class type) {
        if (type.isPrimitive()) {
            type = getWrapperClass(type);
        }
        if (type == Double.class) {
            return number.doubleValue();
        } else if (type == Float.class) {
            return number.floatValue();
        } else if (type == Long.class) {
            return number.longValue();
        } else if (type == Integer.class) {
            return number.intValue();
        } else if (type == Short.class) {
            return number.shortValue();
        } else if (type == Byte.class) {
            return number.byteValue();
        } else {
            throw new IllegalArgumentException("Not a number type");
        }
    }

    //TODO: Optimize
    public static Object toPrimitiveArray(Object wrapperArray) {
        if (wrapperArray instanceof List) {
            wrapperArray = ((List) wrapperArray).toArray();
        }
        Class type = wrapperArray.getClass().getComponentType();
        if (type.isArray()) {
            type = Arr.getComponentType(wrapperArray);
        }
        return Convert.toPrimitiveArray(wrapperArray, type);
    }

    public static Object toPrimitiveArray(Object wrapperArray, Class type) {
        if (wrapperArray instanceof List) {
            wrapperArray = ((List) wrapperArray).toArray();
        }
        if (!wrapperArray.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array");
        }

        Class destinationClass = getPrimitiveClass(type);
        if (type.isPrimitive()) {
            destinationClass = type;
        }
        if (destinationClass == null) {
            throw new IllegalArgumentException("Invalid primitive type: " + type.getName());
        }

        Class componentType = Arr.getComponentType(wrapperArray);
        Class primitiveComponentClass = getPrimitiveClass(componentType);
        if (componentType.isPrimitive()) {
            primitiveComponentClass = componentType;
        }
        if (componentType.isPrimitive() && (primitiveComponentClass == destinationClass)) {
            return wrapperArray;
        }

        Object ret = null;
        int size = Array.getLength(wrapperArray);
        int rank = Arr.getRank(wrapperArray);

        if (rank > 1) {
            int[] shape = Arr.getShape(wrapperArray);
            ret = Array.newInstance(destinationClass, shape);
            for (int i = 0; i < size; i++) {
                Array.set(ret, i, toPrimitiveArray(Array.get(wrapperArray, i), type));
            }
        } else {
            ret = Array.newInstance(destinationClass, size);
            if (ret == null) {
                throw new IllegalArgumentException("Not a wrapper type");
            }
            if (primitiveComponentClass == destinationClass) {
                for (int i = 0; i < size; i++) {
                    Array.set(ret, i, Array.get(wrapperArray, i));
                }
            } else {
                for (int i = 0; i < size; i++) {
                    Array.set(ret, i, toType((Number) Array.get(wrapperArray, i), destinationClass));
                }
            }
        }
        return ret;
    }

    public static Object toWrapperArray(Object primitiveArray) {
        if (primitiveArray instanceof List) {
            primitiveArray = ((List) primitiveArray).toArray();
        }        
        if (!primitiveArray.getClass().isArray()) {
            throw new IllegalArgumentException("Parameter is not an array");
        }
        Class type = Arr.getComponentType(primitiveArray);
        if (!type.isPrimitive()) {
            return primitiveArray;
        }
        Object ret = null;
        int size = Array.getLength(primitiveArray);
        int rank = Arr.getRank(primitiveArray);

        if (rank > 1) {
            int[] shape = Arr.getShape(primitiveArray);
            ret = Array.newInstance(getWrapperClass(type), shape);
            for (int i = 0; i < size; i++) {
                Array.set(ret, i, toWrapperArray(Array.get(primitiveArray, i)));
            }
        } else {
            ret = Array.newInstance(getWrapperClass(type), size);
            for (int i = 0; i < size; i++) {
                Array.set(ret, i, Array.get(primitiveArray, i));
            }
        }
        return ret;
    }

    public static int getPrimitiveTypeSize(Class type) {
        if (type.isPrimitive()) {
            type = getWrapperClass(type);
        }
        if (type == Double.class) {
            return Double.BYTES;
        } else if (type == Float.class) {
            return Float.BYTES;
        } else if (type == Long.class) {
            return Long.BYTES;
        } else if (type == Integer.class) {
            return Integer.BYTES;
        } else if (type == Short.class) {
            return Short.BYTES;
        } else if (type == Byte.class) {
            return Byte.BYTES;
        } else if (type == Character.class) {
            return Character.BYTES;
        } else {
            throw new IllegalArgumentException("Not a number type");
        }
    }

    public static byte[] toByteArray(Object data) {
        if (data == null) {
            return null;
        }
        Class type = Arr.getComponentType(data);
        if (Convert.isWrapperClass(type)) {
            type = Convert.getPrimitiveClass(type);
        }
        if (!type.isPrimitive()) {
            throw new IllegalArgumentException("Invalid data type");
        }
        int rank = Arr.getRank(data);
        //Transform to 1d array
        if (rank == 0) {
            Object arr = Array.newInstance(type, 1);
            Array.set(arr, 0, data);
            data = arr;
        } else if (rank > 1) {
            data = Convert.flatten(data);
        }
        int elements = Array.getLength(data);
        int elementSize = (type == boolean.class) ? 1 : getPrimitiveTypeSize(type);

        ByteBuffer buffer = ByteBuffer.allocate(elements * elementSize);

        if (type == double.class) {
            for (double v : (double[]) data) {
                buffer.putDouble(v);
            }
        } else if (type == float.class) {
            for (float v : (float[]) data) {
                buffer.putFloat(v);
            }
        } else if (type == long.class) {
            for (long v : (long[]) data) {
                buffer.putLong(v);
            }
        } else if (type == int.class) {
            for (int v : (int[]) data) {
                buffer.putInt(v);
            }
        } else if (type == short.class) {
            for (short v : (short[]) data) {
                buffer.putShort(v);
            }
        } else if (type == byte.class) {
            buffer.put((byte[]) data);
        } else if (type == char.class) {
            for (char v : (char[]) data) {
                buffer.putChar(v);
            }
        } else if (type == boolean.class) {
            for (boolean v : (boolean[]) data) {
                buffer.put(v ? (byte) 1 : (byte) 0);
            }
        }
        return buffer.array();
    }

    public static Object fromByteArray(byte[] array, Class type) {
        if (array == null) {
            return null;
        }

        if (Convert.isWrapperClass(type)) {
            type = Convert.getPrimitiveClass(type);
        }
        if (!type.isPrimitive()) {
            throw new IllegalArgumentException("Invalid data type");
        }
        int elementSize = (type == boolean.class) ? 1 : getPrimitiveTypeSize(type);
        int elements = array.length / elementSize;

        ByteBuffer buffer = ByteBuffer.wrap(array);

        if (type == byte.class) {
            return buffer.array();
        }

        Object ret = Array.newInstance(type, elements);
        if (type == double.class) {
            double[] arr = (double[]) ret;
            for (int i = 0; i < elements; i++) {
                arr[i] = buffer.getDouble();
            }
        } else if (type == float.class) {
            float[] arr = (float[]) ret;
            for (int i = 0; i < elements; i++) {
                arr[i] = buffer.getFloat();
            }
        } else if (type == long.class) {
            long[] arr = (long[]) ret;
            for (int i = 0; i < elements; i++) {
                arr[i] = buffer.getLong();
            }
        } else if (type == int.class) {
            int[] arr = (int[]) ret;
            for (int i = 0; i < elements; i++) {
                arr[i] = buffer.getInt();
            }
        } else if (type == short.class) {
            short[] arr = (short[]) ret;
            for (int i = 0; i < elements; i++) {
                arr[i] = buffer.getShort();
            }
        } else if (type == char.class) {
            char[] arr = (char[]) ret;
            for (int i = 0; i < elements; i++) {
                arr[i] = buffer.getChar();
            }
        } else if (type == boolean.class) {
            boolean[] arr = (boolean[]) ret;
            for (int i = 0; i < elements; i++) {
                arr[i] = (buffer.get() == 0) ? false : true;
            }
        }
        return ret;
    }

    public static double[][] transpose(double[][] array) {
        double[][] ret = new double[array[0].length][array.length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                ret[j][i] = array[i][j];
            }
        }
        return ret;
    }

    public static Object transpose(Object array) {
        return matrixOp(array, true, false, false);
    }

    public static Object matrixOp(Object array, boolean transp, boolean mirrorh, boolean mirrorv) {
        if ((array == null) || ((transp == false) && (mirrorh == false) && (mirrorv == false))) {
            return array;
        }
        Class type = Arr.getComponentType(array);
        if (type == double.class) {
            return matrixOp((double[][]) array, transp, mirrorh, mirrorv);
        }
        if (type == float.class) {
            return matrixOp((float[][]) array, transp, mirrorh, mirrorv);
        }
        if (type == long.class) {
            return matrixOp((long[][]) array, transp, mirrorh, mirrorv);
        }
        if (type == int.class) {
            return matrixOp((int[][]) array, transp, mirrorh, mirrorv);
        }
        if (type == short.class) {
            return matrixOp((short[][]) array, transp, mirrorh, mirrorv);
        }
        if (type == byte.class) {
            return matrixOp((byte[][]) array, transp, mirrorh, mirrorv);
        }
        if (type == byte.class) {
            return matrixOp((byte[][]) array, transp, mirrorh, mirrorv);
        }
        if (type == Object.class) {
            return matrixOp((Object[][]) array, transp, mirrorh, mirrorv);
        }
        throw new IllegalArgumentException("Invalid array type");
    }

    public static double[][] matrixOp(double[][] array, boolean transp, boolean mirrorh, boolean mirrorv) {
        int h = array.length;
        int w = array[0].length;
        double[][] ret = transp ? new double[w][h] : new double[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                ret[transp ? j : i][transp ? i : j] = array[mirrorv ? h - i - 1 : i][mirrorh ? w - 1 - j : j];
            }
        }
        return ret;
    }

    public static float[][] matrixOp(float[][] array, boolean transp, boolean mirrorh, boolean mirrorv) {
        int h = array.length;
        int w = array[0].length;
        float[][] ret = transp ? new float[w][h] : new float[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                ret[transp ? j : i][transp ? i : j] = array[mirrorv ? h - i - 1 : i][mirrorh ? w - 1 - j : j];
            }
        }
        return ret;
    }

    public static long[][] matrixOp(long[][] array, boolean transp, boolean mirrorh, boolean mirrorv) {
        int h = array.length;
        int w = array[0].length;
        long[][] ret = transp ? new long[w][h] : new long[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                ret[transp ? j : i][transp ? i : j] = array[mirrorv ? h - i - 1 : i][mirrorh ? w - 1 - j : j];
            }
        }
        return ret;
    }

    public static int[][] matrixOp(int[][] array, boolean transp, boolean mirrorh, boolean mirrorv) {
        int h = array.length;
        int w = array[0].length;
        int[][] ret = transp ? new int[w][h] : new int[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                ret[transp ? j : i][transp ? i : j] = array[mirrorv ? h - i - 1 : i][mirrorh ? w - 1 - j : j];
            }
        }
        return ret;
    }

    public static short[][] matrixOp(short[][] array, boolean transp, boolean mirrorh, boolean mirrorv) {
        int h = array.length;
        int w = array[0].length;
        short[][] ret = transp ? new short[w][h] : new short[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                ret[transp ? j : i][transp ? i : j] = array[mirrorv ? h - i - 1 : i][mirrorh ? w - 1 - j : j];
            }
        }
        return ret;
    }

    public static byte[][] matrixOp(byte[][] array, boolean transp, boolean mirrorh, boolean mirrorv) {
        int h = array.length;
        int w = array[0].length;
        byte[][] ret = transp ? new byte[w][h] : new byte[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                ret[transp ? j : i][transp ? i : j] = array[mirrorv ? h - i - 1 : i][mirrorh ? w - 1 - j : j];
            }
        }
        return ret;
    }

    public static Object[][] matrixOp(Object[][] array, boolean transp, boolean mirrorh, boolean mirrorv) {
        int h = array.length;
        int w = array[0].length;
        Object[][] ret = transp ? new Object[w][h] : new Object[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                ret[transp ? j : i][transp ? i : j] = array[mirrorv ? h - i - 1 : i][mirrorh ? w - 1 - j : j];
            }
        }
        return ret;
    }

    public static Object matrixRoi(Object array, int x, int y, int width, int height) {
        int[] shape = Arr.getShape(array);
        if (shape.length < 2) {
            return null;
        }
        int w = shape[shape.length - 1];
        int h = shape[shape.length - 2];
        x = Math.min(Math.max(x, 0), w);
        y = Math.min(Math.max(y, 0), h);
        if (width < 0) {
            width = w;
        }
        if (height < 0) {
            height = h;
        }
        width = Math.min(width, w - x);
        height = Math.min(height, h - y);

        if ((x == 0) && (y == 0) && (w == width) && (h == height)) {
            return array;
        }

        Class type = Arr.getComponentType(array);
        if (type == double.class) {
            return matrixRoi((double[][]) array, x, y, width, height);
        }
        if (type == float.class) {
            return matrixRoi((float[][]) array, x, y, width, height);
        }
        if (type == long.class) {
            return matrixRoi((long[][]) array, x, y, width, height);
        }
        if (type == int.class) {
            return matrixRoi((int[][]) array, x, y, width, height);
        }
        if (type == short.class) {
            return matrixRoi((short[][]) array, x, y, width, height);
        }
        if (type == byte.class) {
            return matrixRoi((byte[][]) array, x, y, width, height);
        }
        if (type == Object.class) {
            return matrixRoi((Object[][]) array, x, y, width, height);
        }
        throw new IllegalArgumentException("Invalid array type");
    }

    public static double[][] matrixRoi(double[][] array, int x, int y, int width, int height) {
        int h = array.length;
        int w = array[0].length;
        double[][] ret = new double[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ret[i][j] = array[i + y][j + x];
            }
        }
        return ret;
    }

    public static float[][] matrixRoi(float[][] array, int x, int y, int width, int height) {
        int h = array.length;
        int w = array[0].length;
        float[][] ret = new float[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ret[i][j] = array[i + y][j + x];
            }
        }
        return ret;
    }

    public static long[][] matrixRoi(long[][] array, int x, int y, int width, int height) {
        int h = array.length;
        int w = array[0].length;
        long[][] ret = new long[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ret[i][j] = array[i + y][j + x];
            }
        }
        return ret;
    }

    public static int[][] matrixRoi(int[][] array, int x, int y, int width, int height) {
        int h = array.length;
        int w = array[0].length;
        int[][] ret = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ret[i][j] = array[i + y][j + x];
            }
        }
        return ret;
    }

    public static short[][] matrixRoi(short[][] array, int x, int y, int width, int height) {
        int h = array.length;
        int w = array[0].length;
        short[][] ret = new short[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ret[i][j] = array[i + y][j + x];
            }
        }
        return ret;
    }

    public static byte[][] matrixRoi(byte[][] array, int x, int y, int width, int height) {
        int h = array.length;
        int w = array[0].length;
        byte[][] ret = new byte[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ret[i][j] = array[i + y][j + x];
            }
        }
        return ret;
    }

    public static Object[][] matrixRoi(Object[][] array, int x, int y, int width, int height) {
        int h = array.length;
        int w = array[0].length;
        Object[][] ret = new Object[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ret[i][j] = array[i + y][j + x];
            }
        }
        return ret;
    }

    /**
     * Recursivelly converts list to array of objects
     */
    public static Object[] toArray(List list) {
        Object[] ret = list.toArray();
        for (int i = 0; i < ret.length; i++) {
            if (ret[i] instanceof List) {
                ret[i] = toArray((List) ret[i]);
            } else if (ret[i] instanceof Map) {
                ret[i] = toArray((Map) ret[i]);
            }
        }
        return ret;
    }

    public static Object[] toArray(Map map) {
        return toArray(new ArrayList(map.values()));
    }
}
