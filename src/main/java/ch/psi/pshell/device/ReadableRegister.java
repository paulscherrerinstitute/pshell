package ch.psi.pshell.device;

import java.io.IOException;

/**
 * Wrapping a readable into a Register
 */
public class ReadableRegister<T> extends ReadonlyRegisterBase<T>{
    final Readable<T> readable;
    
    
    public ReadableRegister(Readable<T> readable){
        this.readable = readable;
        try {
            initialize();
        } catch (Exception ignore) {
        }        
    }
    
    public Readable<T> getReadable(){
        return readable;
    }
    
    @Override
    protected T doRead() throws IOException, InterruptedException {
        return readable.read();
    }
    
    public static class ReadableRegisterNumber<T extends Number> extends ReadableRegister<T> implements Readable.ReadableNumber<T>, Cacheable.CacheableNumber<T>{
        public ReadableRegisterNumber(ReadableNumber<T> readable){
            super(readable);
        }        
    }

    public static class ReadableRegisterArray<T> extends ReadableRegister<T> implements ReadonlyRegisterArray<T>, Readable.ReadableArray<T>, Cacheable.CacheableArray<T>{
        public ReadableRegisterArray(ReadableArray<T> readable){
            super(readable);
        }        

        @Override
        public int getSize() {
            return ((ReadableArray)readable).getSize();
        }
    }

    public static class ReadableRegisterMatrix<T> extends ReadableRegister<T> implements ReadonlyRegisterMatrix<T>,Readable.ReadableMatrix<T>, Cacheable.CacheableMatrix<T>{
        public ReadableRegisterMatrix(ReadableMatrix<T> readable){
            super(readable);
        }        

        @Override
        public int getWidth() {
            return ((ReadableMatrix)readable).getWidth();
        }

        @Override
        public int getHeight() {
            return ((ReadableMatrix)readable).getHeight();
        }
    }
}
