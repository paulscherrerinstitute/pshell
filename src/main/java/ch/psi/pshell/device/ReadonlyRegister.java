package ch.psi.pshell.device;

import java.io.IOException;
import java.lang.reflect.Array;
import ch.psi.utils.Reflection.Hidden;

/**
 * Interface for devices containing a readonly numeric or array value.
 */
public interface ReadonlyRegister<T> extends Device, Readable<T>, Cacheable<T> {

    //public boolean isArray();
    //public int getMaximumSize();

    @Hidden
    default boolean isReadonlyRegister() { return true; }

    public interface ReadonlyRegisterNumber<T extends Number> extends ReadonlyRegister<T>, Readable.ReadableNumber<T>, Cacheable.CacheableNumber<T> {
    }

    public interface ReadonlyRegisterArray<T> extends ReadonlyRegister<T>, Readable.ReadableArray<T>, Cacheable.CacheableArray<T> {

        default public int getMaximumSize() {
            return UNDEFINED;
        }

        default public void setSize(int size) throws IOException {
            throw new IOException("Cannot change array size");
        }
        
        @Override
        /**
         * Derived classes may define size by overriding this method.
         * Default implementation verifies size of cache.
         */
        default public int getSize() {
            Object cache;
            try {
                cache = take(-1);
            } catch (Exception ex) {
                cache = null;
            }
            if ((cache == null) || (!cache.getClass().isArray())) {
                return 0;
            }
            return Array.getLength(cache);
        }        
    }

    public interface ReadonlyRegisterMatrix<T> extends ReadonlyRegister<T>, Readable.ReadableMatrix<T>, Cacheable.CacheableMatrix<T> {

    }
    
    public interface ReadonlyRegisterBoolean extends ReadonlyRegister<Boolean>, Readable.ReadableBoolean, Cacheable.CacheableBoolean {
    }

    public interface ReadonlyRegisterString extends ReadonlyRegister<String>, Readable.ReadableString, Cacheable.CacheableString {
    }    

    /**
     * Performs a take if monitored, otherwise a read.
     */
    T getValue() throws IOException, InterruptedException;

    void waitValueInRange(T value, T range, int timeout) throws IOException, InterruptedException;
    
    
    default String getUnit(){
        return "";
    }    
    
    default int getPrecision(){
        return UNDEFINED_PRECISION;
    }
}
