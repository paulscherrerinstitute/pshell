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
    
    public abstract static class ReadableNumberDevice<T> extends DeviceBase implements ReadableNumber{
        protected ReadableNumberDevice(String name){
            super(name);
        }
    }
    
    public abstract static class ReadableArrayDevice<T> extends DeviceBase implements ReadableArray{
        protected ReadableArrayDevice(String name){
            super(name);
        }        
    }
    
    public abstract static class ReadableCalibratedArrayDevice<T> extends DeviceBase implements ReadableCalibratedArray{
        protected ReadableCalibratedArrayDevice(String name){
            super(name);
        }        
    }
    
    public abstract static class ReadableMatrixDevice<T> extends DeviceBase implements ReadableMatrix{
         protected ReadableMatrixDevice(String name){
            super(name);
         }       
    }
    
    public abstract static class ReadableCalibratedMatrixDevice<T> extends DeviceBase implements ReadableCalibratedMatrix{
         protected ReadableCalibratedMatrixDevice(String name){
             super(name);
        }       
    }    

}
