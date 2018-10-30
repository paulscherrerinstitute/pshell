package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.BooleanType;
import ch.psi.pshell.device.Readable.StringType;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * Interface for devices containing a readonly numeric or array value.
 */
public interface ReadonlyRegister<T> extends Device, Readable<T>, Cacheable<T> {

    //public boolean isArray();
    //public int getMaximumSize();
    int getPrecision();

    public interface ReadonlyRegisterNumber<T extends Number> extends ReadonlyRegister<T>, Readable.ReadableNumber<T>, Cacheable.CacheableNumber<T> {
    }

    public interface ReadonlyRegisterArray<T> extends ReadonlyRegister<T>, Readable.ReadableArray<T>, Cacheable.CacheableArray<T> {

        default public int getMaximumSize() {
            return -1;
        }

        default public void setSize(int size) throws IOException {
            throw new IOException("Cannot change array size");
        }
    }

    public interface ReadonlyRegisterMatrix<T> extends ReadonlyRegister<T>, Readable.ReadableMatrix<T>, Cacheable.CacheableMatrix<T> {

    }
    
    public interface ReadonlyRegisterBoolean extends ReadonlyRegister<Boolean>, BooleanType, Cacheable.CacheableBoolean {
    }

    public interface ReadonlyRegisterString extends ReadonlyRegister<String>, StringType, Cacheable.CacheableString {
    }    

    //TODO: Not implemented in ReadonlyRegisterArray so Jython classes can override getSize in ReadonlyRegisterArray (http://bugs.jython.org/issue2403)
    public interface DefaultReadonlyRegisterArray<T> extends ReadonlyRegisterArray<T> {

        @Override
        default int getSize() {
            Object cache = take();
            if ((cache == null) || (!cache.getClass().isArray())) {
                return 0;
            }
            return Array.getLength(cache);
        }
    }

    /**
     * Performs a take if monitored, otherwise a read.
     */
    T getValue() throws IOException, InterruptedException;

    void waitValueInRange(T value, T range, int timeout) throws IOException, InterruptedException;
}
