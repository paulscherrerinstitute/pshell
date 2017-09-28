package ch.psi.pshell.imaging;

import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Image source connecting to a __ReadableMatrix__, pushing a frame every time the register contents
 * change.
 */
public class RegisterMatrixSource extends DeviceSource {

    public RegisterMatrixSource(String name, ReadonlyRegisterMatrix device) {
        super(name, device);
    }

    //Wrapper constructor to readable
    public RegisterMatrixSource(String name, ReadableMatrix readable) {
        this(name, readable instanceof ReadableCalibratedMatrix
                ? new CalibratedWrapperRegister((ReadableCalibratedMatrix) readable)
                : new WrapperRegister(readable));
        try {
            device.initialize();
        } catch (Exception ex) {
            Logger.getLogger(RegisterMatrixSource.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    static class WrapperRegister extends ReadonlyRegisterBase implements ReadonlyRegisterMatrix {

        final ReadableMatrix readable;

        WrapperRegister(ReadableMatrix readable) {
            super(readable.getName());
            this.readable = readable;
        }

        @Override
        protected Object doRead() throws IOException, InterruptedException {
            return readable.read();
        }

        @Override
        public int getWidth() {
            return readable.getWidth();
        }

        @Override
        public int getHeight() {
            return readable.getHeight();
        }
    }

    static class CalibratedWrapperRegister extends WrapperRegister implements ReadableCalibratedMatrix {

        public CalibratedWrapperRegister(ReadableCalibratedMatrix readable) {
            super(readable);
        }

        @Override
        public MatrixCalibration getCalibration() {
            return ((CalibratedWrapperRegister) readable).getCalibration();
        }
    }

    @Override
    protected void onDataReceived(Object value) throws IOException {
        setCalibration((getDevice() instanceof ReadableCalibratedMatrix)
                ? new Calibration(((ReadableCalibratedMatrix) getDevice()).getCalibration())
                : null);
        pushData(value);
    }
}
