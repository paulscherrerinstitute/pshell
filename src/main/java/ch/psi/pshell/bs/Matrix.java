package ch.psi.pshell.bs;

import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.utils.Convert;
import java.io.IOException;

/**
 * Represents a 2-dimensional array element in a BS stream.
 */
public class Matrix<T> extends Scalar<T> implements ReadonlyRegisterMatrix<T> {

    Integer width;
    Integer height;

    public Matrix(String name, Stream stream) {
        super(name, stream, new MatrixConfig());
    }

    public Matrix(String name, Stream stream, String id, int width, int height) {
        super(name, stream, id);
        this.width = width;
        this.height = height;
    }

    public Matrix(String name, Stream stream, String id, int modulo, int width, int height) {
        super(name, stream, id, modulo);
        this.width = width;
        this.height = height;
    }

    public Matrix(String name, Stream stream, String id, int modulo, int offset, int width, int height) {
        super(name, stream, id, modulo, offset);
        this.width = width;
        this.height = height;
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
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    void set(long pulseId, long timestamp, long nanosOffset, T value) {
        super.set(pulseId, timestamp, nanosOffset, (T) Convert.reshape(value, height, width));
    }
}
