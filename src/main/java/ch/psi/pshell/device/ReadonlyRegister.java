package ch.psi.pshell.device;

import java.io.IOException;
import java.lang.reflect.Array;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.Arr;

/**
 * Interface for devices containing a readonly numeric or array value.
 */
public interface ReadonlyRegister<T> extends Device, Cacheable<T> {

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
                return Array.getLength(take(-1));
            } catch (Exception ex) {
                return 0;
            }
        }        
    }

    public interface ReadonlyRegisterMatrix<T> extends ReadonlyRegister<T>, Readable.ReadableMatrix<T>, Cacheable.CacheableMatrix<T> {
        /**
         * Derived classes may define width and height by overriding this method.
         * Default implementation verifies cache.
         */
        
        default public int getWidth(){
            try {
                return Array.getLength(Array.get(take(-1),0)); 
            } catch (Exception ex) {
                return 0;
            }
        }

        default public int getHeight(){
            try {
                return Array.getLength(take(-1)); 
            } catch (Exception ex) {
                return 0;
            }
        }

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
    
    default int[] getShape(){
        return UNDEFINED_SHAPE;
    }
}
