package ch.psi.pshell.device;

import ch.psi.pshell.core.Nameable;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface implemented by sensors in scans.
 */
public interface Readable<T> extends Nameable {

    T read() throws IOException, InterruptedException;

    default CompletableFuture<T> readAsync() {
        return (CompletableFuture<T>) Threading.getFuture(() -> read());
    }

    public interface ReadableNumber<T extends Number> extends Readable<T> {
    }

    public interface ReadableBoolean extends Readable<Boolean> {
    }

    public interface ReadableString extends Readable<String> {
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

        default Class getElementType() {
            return Object.class;
        }

    }
    public interface BooleanType extends ReadableType {

        @Override
        default Class getElementType() {
            return Boolean.class;
        }
    }

    public interface StringType extends ReadableType {

        @Override
        default Class getElementType() {
            return String.class;
        }
    }

    public interface NumberType extends ReadableType {

        @Override
        default Class getElementType() {
            return Number.class;
        }
    }

    public interface IntegerType extends ReadableType {

        @Override
        default Class getElementType() {
            return Integer.class;
        }
    }

    public interface ByteType extends ReadableType {

        @Override
        default Class getElementType() {
            return Byte.class;
        }
    }

    public interface ShortType extends ReadableType {

        @Override
        default Class getElementType() {
            return Short.class;
        }
    }

    public interface LongType extends ReadableType {

        @Override
        default Class getElementType() {
            return Long.class;
        }
    }

    public interface FloatType extends ReadableType {

        @Override
        default Class getElementType() {
            return Float.class;
        }
    }

    public interface DoubleType extends ReadableType {

        @Override
        default Class getElementType() {
            return Double.class;
        }
    }
}
