package ch.psi.pshell.bs;

import ch.psi.bsread.message.ChannelConfig;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.utils.Convert;
import java.io.IOException;

/**
 * Represents a 2-dimensional array element in a BS stream.
 */

public class Matrix<T> extends StreamChannel<T> implements ReadonlyRegisterMatrix<T> {

    Integer width;
    Integer height;
    int[] shape;

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
            return shape[1];
        }
        return width;
    }

    @Override
    public int getHeight() {
        if (height==null){
            return shape[0];
        }
        return height;
    }

    @Override
    void set(long pulseId, long timestamp, long nanosOffset, T value, ChannelConfig config) {
        shape = config.getShape();
        super.set(pulseId, timestamp, nanosOffset, (T) Convert.reshape(value, getWidth(), getHeight()), config);
    }
}
