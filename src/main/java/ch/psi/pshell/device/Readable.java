package ch.psi.pshell.device;

import ch.psi.pshell.device.Cacheable.CacheReadable;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface implemented by sensors in scans.
 */
public interface Readable<T> extends Record {
   
    T read() throws IOException, InterruptedException;

    default CompletableFuture<T> readAsync() {
        return (CompletableFuture<T>) Threading.getFuture(() -> read());
    }

    default Class getElementType() {
        if (this instanceof ReadableType) {
            return ((ReadableType) this)._getElementType();
        }
        if (this instanceof CacheReadable) {
            Object obj = ((CacheReadable)this).getParent();
            if ((obj!=null) && (obj instanceof ReadableType)) {
                return ((ReadableType) obj)._getElementType();
            }
        }        
        return Object.class;
    }

    default Boolean isElementUnsigned() {
        if (this instanceof ReadableType) {
            return ((ReadableType) this)._isElementUnsigned();
        }
        return null;
    }
    
    @Hidden
    default Class resolveElementType() throws IOException, InterruptedException {
        Class type = null;
        if (this instanceof Cacheable) {
            type = Arr.getComponentType(((Cacheable) this).take(Integer.MAX_VALUE));
        } else {
            type = Arr.getComponentType(read());
        }
        if ((type!=null) && (type.isPrimitive())) {
            type = Convert.getWrapperClass(type);
        }
        return type;
    }
    

    public interface ReadableNumber<T extends Number> extends Readable<T> {
    }

    public interface ReadableBoolean extends Readable<Boolean>, BooleanType {
    }

    public interface ReadableString extends Readable<String>, StringType {
    }

    public interface ReadableByte extends ReadableNumber<Byte>, ByteType {
    }
    
    public interface ReadableUnsignedByte extends ReadableNumber<Byte>, UnsignedByteType {
    }    
    
    public interface ReadableShort extends ReadableNumber<Short>, ShortType {
    }
    
    public interface ReadableUnsignedShort extends ReadableNumber<Short>, UnsignedShortType {
    }       
    
    public interface ReadableInteger extends ReadableNumber<Integer>, IntegerType {
    }
    
    public interface ReadableIntegerShort extends ReadableNumber<Integer>, UnsignedIntegerType {
    }   

    public interface ReadableLong extends ReadableNumber<Long>, LongType {
    }
    
    public interface ReadableUnsignedLong extends ReadableNumber<Long>, UnsignedLongType {
    }       
    
    public interface ReadableFloat extends ReadableNumber<Float>, FloatType {
    }
    
    public interface ReadableDouble extends ReadableNumber<Double>, DoubleType {
    }

    public interface ReadableArray<T> extends Readable<T> {

        public int getSize();
    }

    public interface ReadableCalibratedArray<T> extends ReadableArray<T> {

        public ArrayCalibration getCalibration();
    }

    public interface ReadableMatrix<T> extends Readable<T> {

        public int getWidth();

        public int getHeight();
    }

    public interface ReadableCalibratedMatrix<T> extends ReadableMatrix<T> {

        public MatrixCalibration getCalibration();
    }

    public abstract static class ReadableNumberDevice<T> extends DeviceBase implements ReadableNumber {

        protected ReadableNumberDevice(String name) {
            super(name);
        }
    }

    public abstract static class ReadableArrayDevice<T> extends DeviceBase implements ReadableArray {

        protected ReadableArrayDevice(String name) {
            super(name);
        }
    }

    public abstract static class ReadableCalibratedArrayDevice<T> extends DeviceBase implements ReadableCalibratedArray {

        protected ReadableCalibratedArrayDevice(String name) {
            super(name);
        }
    }

    public abstract static class ReadableMatrixDevice<T> extends DeviceBase implements ReadableMatrix {

        protected ReadableMatrixDevice(String name) {
            super(name);
        }
    }

    public abstract static class ReadableCalibratedMatrixDevice<T> extends DeviceBase implements ReadableCalibratedMatrix {

        protected ReadableCalibratedMatrixDevice(String name) {
            super(name);
        }
    }

    /**
     * Tags for anticipating types in scans.
     */
    public interface ReadableType {
        @Hidden
        default Class _getElementType() {
            return Object.class;
        }
        
        @Hidden
        default Boolean _isElementUnsigned() {
            return false;
        }

    }

    public interface BooleanType extends ReadableType {

        @Override
        default Class _getElementType() {
            return Boolean.class;
        }
    }

    public interface StringType extends ReadableType {

        @Override
        default Class _getElementType() {
            return String.class;
        }
    }

    public interface NumberType extends ReadableType {

        @Override
        default Class _getElementType() {
            return Number.class;
        }
    }

    public interface ByteType extends NumberType {

        @Override
        default Class _getElementType() {
            return Byte.class;
        }
    }

    public interface ShortType extends NumberType {

        @Override
        default Class _getElementType() {
            return Short.class;
        }
    }

    public interface IntegerType extends NumberType {

        @Override
        default Class _getElementType() {
            return Integer.class;
        }
    }

    public interface LongType extends NumberType {

        @Override
        default Class _getElementType() {
            return Long.class;
        }
    }

    public interface UnsignedByteType extends ByteType {

        @Override
        default Boolean _isElementUnsigned() {
            return true;
        }
    }

    public interface UnsignedShortType extends ShortType {

        @Override
        default Boolean _isElementUnsigned() {
            return true;
        }
    }

    public interface UnsignedIntegerType extends IntegerType {

        @Override
        default Boolean _isElementUnsigned() {
            return true;
        }
    }

    public interface UnsignedLongType extends LongType {

        @Override
        default Boolean _isElementUnsigned() {
            return true;
        }
    }

    public interface FloatType extends NumberType {

        @Override
        default Class _getElementType() {
            return Float.class;
        }
    }

    public interface DoubleType extends NumberType {

        @Override
        default Class _getElementType() {
            return Double.class;
        }
    }
}
