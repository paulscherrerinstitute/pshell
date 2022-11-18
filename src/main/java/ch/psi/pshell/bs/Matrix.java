package ch.psi.pshell.bs;

import ch.psi.bsread.message.ChannelConfig;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * Represents a 2-dimensional array element in a BS stream.
 */

public class Matrix<T> extends StreamChannel<T> implements ReadonlyRegisterMatrix<T> {

    Integer width;
    Integer height;

    public Matrix(String name, Stream stream) {
        super(name, stream, new MatrixConfig());
    }

    public Matrix(String name, Stream stream, String id){
         super(name, stream, id);
    }
    public Matrix(String name, Stream stream, String id, int width, int height) {
        super(name, stream, id);
        this.width = (width<=0) ? null : width;
        this.height = (height<=0) ? null : height;
    }

    public Matrix(String name, Stream stream, String id, int modulo, int width, int height) {
        super(name, stream, id, modulo);
        this.width = (width<=0) ? null : width;
        this.height = (height<=0) ? null : height;
    }

    public Matrix(String name, Stream stream, String id, int modulo, int offset, int width, int height) {
        super(name, stream, id, modulo, offset);
        this.width = (width<=0) ? null : width;
        this.height = (height<=0) ? null : height;
    }

    @Override
    public MatrixConfig getConfig() {
        return (MatrixConfig) super.getConfig();
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        if (getConfig() != null) {
            width = getConfig().width;
            height = getConfig().height;
        }
    }

    @Override
    public int getWidth() {
        if (width==null){
            int[] shape = getShape();
            if ((shape==null) ||  (shape.length!=2)){
                try{
                    int[] ashape = Arr.getShape(take());
                    if  (shape.length==2){
                        return ashape[0];
                    }                 
                } catch (Exception ex){                    
                }
                throw new RuntimeException("Indefined matrix size");
            }
            return shape[1];
        }
        return width;
    }

    @Override
    public int getHeight() {
        if (height==null){
            int[] shape = getShape();
            if ((shape==null) || (shape.length!=2)){
                try{
                    int[] ashape = Arr.getShape(take());
                    if  (shape.length==2){
                        return ashape[1];
                    }                 
                } catch (Exception ex){                    
                }
                throw new RuntimeException("Indefined matrix size");
            }
            return shape[0];
        }
        return height;
    }

    @Override
    protected void set(long pulseId, long timestamp, long nanosOffset, T value, ChannelConfig config) {
        this.config = config; //Done before so getWidth and getHeight get updated value
        super.set(pulseId, timestamp, nanosOffset, (T) Convert.reshape(value, getHeight(), getWidth()), config);
    }
}
