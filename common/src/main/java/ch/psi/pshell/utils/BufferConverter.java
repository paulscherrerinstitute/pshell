package ch.psi.pshell.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

public class BufferConverter {

    public static <V> V fromArray(byte[] array, String type) {
        return fromArray(array, type, null);
    }

    public static <V> V fromArray(byte[] array, Type type) {
        return fromArray(array, type, null);
    }

    public static <V> V fromArray(byte[] array, String type, String byteOrder) {
        ByteOrder order = null;
        if (byteOrder.equalsIgnoreCase("little")){
            order = ByteOrder.LITTLE_ENDIAN;
        }
        if (byteOrder.equalsIgnoreCase("big")){
            order = ByteOrder.BIG_ENDIAN;
        }
        if (byteOrder.equalsIgnoreCase("native")){
            order = ByteOrder.nativeOrder();
        }
        return fromArray(array, Type.fromString(type), order);
    }

    public static <V> V fromArray(byte[] array, Type type, ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        if (byteOrder != null){
             buffer.order(byteOrder);
        }
        return fromBuffer(buffer, type, true);
    }
    
    
    public static <V> V fromBuffer(ByteBuffer buffer, String type, boolean array) {
         return fromBuffer(buffer, Type.fromString(type), array);
    }
    
    public static <V> V fromBuffer(ByteBuffer buffer, Type type, boolean array) {

        switch (type) {
            case Bool:
                if (array) {
                    final ByteBuffer finalReceivedValueBytes = buffer;
                    boolean[] values = new boolean[finalReceivedValueBytes.remaining()];
                    int startPos = finalReceivedValueBytes.position();

                    IntStream stream = IntStream.range(0, values.length);
                    stream.forEach(i -> values[i] = (finalReceivedValueBytes.get(startPos + i) & 1) != 0);

                    return (V) values;
                } else {
                    Boolean value = (buffer.get(buffer.position()) & 1) != 0;
                    return (V) value;
                }
            case Int8:
                if (array) {
                    return (V) copyToByteArray(buffer);
                } else {
                    return (V) ((Byte) buffer.get(buffer.position()));
                }
            case Int16:
                if (array) {
                    short[] values = new short[buffer.remaining() / Short.BYTES];
                    buffer.asShortBuffer().get(values);
                    return (V) values;
                } else {
                    return (V) ((Short) buffer.getShort(buffer.position()));
                }
            case Int32:
                if (array) {
                    int[] values = new int[buffer.remaining() / Integer.BYTES];
                    buffer.asIntBuffer().get(values);
                    return (V) values;
                } else {
                    return (V) ((Integer) buffer.getInt(buffer.position()));
                }
            case Int64:
                if (array) {
                    long[] values = new long[buffer.remaining() / Long.BYTES];
                    buffer.asLongBuffer().get(values);
                    return (V) values;
                } else {
                    return (V) ((Long) buffer.getLong(buffer.position()));
                }
            case UInt8:
                if (array) {
                    final ByteBuffer finalReceivedValueBytes = buffer;
                    short[] values = new short[finalReceivedValueBytes.remaining()];
                    int startPos = finalReceivedValueBytes.position();

                    IntStream stream = IntStream.range(0, values.length);
                    stream.forEach(i -> values[i] = (short) (finalReceivedValueBytes.get(startPos + i) & 0xff));

                    return (V) values;
                } else {
                    Short value = (short) (buffer.get(buffer.position()) & 0xff);
                    return (V) value;
                }
            case UInt16:
                if (array) {
                    final ShortBuffer finalReceivedValueBytes = buffer.asShortBuffer();
                    int[] values = new int[finalReceivedValueBytes.remaining()];

                    IntStream stream = IntStream.range(0, values.length);
                    stream.forEach(i -> values[i] = finalReceivedValueBytes.get(i) & 0xffff);

                    return (V) values;
                } else {
                    return (V) ((Integer) (buffer.getShort(buffer.position()) & 0xffff));
                }
            case UInt32:
                if (array) {
                    final IntBuffer finalReceivedValueBytes = buffer.asIntBuffer();
                    long[] values = new long[finalReceivedValueBytes.remaining()];

                    IntStream stream = IntStream.range(0, values.length);
                    stream.forEach(i -> values[i] = finalReceivedValueBytes.get(i) & 0xffffffffL);

                    return (V) values;
                } else {
                    return (V) ((Long) (buffer.getInt() & 0xffffffffL));
                }
            case UInt64:
                if (array) {
                    final LongBuffer finalReceivedValueBytes = buffer.asLongBuffer();
                    BigInteger[] values = new BigInteger[finalReceivedValueBytes.remaining()];

                    IntStream stream = IntStream.range(0, values.length);
                    stream.forEach(i -> {
                        long val = finalReceivedValueBytes.get(i);
                        BigInteger bigInt = BigInteger.valueOf(val & 0x7fffffffffffffffL);
                        if (val < 0) {
                            bigInt = bigInt.setBit(Long.SIZE - 1);
                        }
                        values[i] = bigInt;
                    });

                    return (V) values;
                } else {
                    long val = buffer.getLong(buffer.position());
                    BigInteger bigInt = BigInteger.valueOf(val & 0x7fffffffffffffffL);
                    if (val < 0) {
                        bigInt = bigInt.setBit(Long.SIZE - 1);
                    }
                    return (V) bigInt;
                }
            case Float32:
                if (array) {
                    float[] values = new float[buffer.remaining() / Float.BYTES];
                    buffer.asFloatBuffer().get(values);
                    return (V) values;
                } else {
                    return (V) ((Float) buffer.getFloat(buffer.position()));
                }
            case Float64:
                if (array) {
                    double[] values = new double[buffer.remaining() / Double.BYTES];
                    buffer.asDoubleBuffer().get(values);
                    return (V) values;
                } else {
                    return (V) ((Double) buffer.getDouble(buffer.position()));
                }
            case String:
                return (V) StandardCharsets.UTF_8.decode(buffer.duplicate()).toString();
            default:
                throw new RuntimeException("Type " + type + " not supported");
        }

    }

    public static byte[] copyToByteArray(ByteBuffer buffer) {
        byte[] copy = new byte[buffer.remaining()];
        buffer.duplicate().order(buffer.order()).get(copy);
        return copy;
    }

    /**
     * Extracts a byte array that contains the content of the ByteBuffer (might reuse underlying byte arrays of some
     * ByteBuffer implementations).
     *
     * @param buffer The ByteBuffer
     * @return byte[] The byte array
     */
    public static byte[] extractByteArray(ByteBuffer buffer) {
        if (buffer.hasArray() && buffer.position() == 0 && buffer.remaining() == buffer.capacity()) {
            return buffer.array();
        } else {
            return copyToByteArray(buffer);
        }
    }
}   