package ch.psi.pshell.device;

import java.io.IOException;
import ch.psi.pshell.device.Readable.ReadableCalibratedMatrix;
import ch.psi.pshell.device.Readable.ReadableMatrix;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.utils.Convert;
import ch.psi.utils.State;

/**
 * Base class for camera devices.
 */
public abstract class CameraBase extends DeviceBase implements Camera {

    BidimentionalImageRegister matrix;

    public CameraBase(String name) {
        super(name);
        matrix = new BidimentionalImageRegister(name + " image");
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        ReadonlyRegisterArray dataArray = getDataArray();
        if (dataArray != null) {
            dataArray.addListener(new DeviceAdapter() {
                @Override
                public void onValueChanged(Device device, Object value, Object former) {
                    onData(value);
                }
            });
        }
        matrix.initialize();
    }

    @Override
    public ReadableMatrix getDataMatrix() {
        return matrix;
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        isStarted();    //Update state
        //getData().update();  //Data is independent
        readImageDescriptor();//Update value            
    }

    @Override
    public void start() throws IOException, InterruptedException {
        if (!isSimulated()) {
            doStart();
        }
        setState(State.Busy);
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        if (!isSimulated()) {
            doStop();
        }
        setState(State.Ready);
    }

    @Override
    public boolean isStarted() throws IOException, InterruptedException {
        if (!isInitialized()) {
            return false;
        }
        if (isSimulated()) {
            return getState() == State.Busy;
        }
        boolean started = getStarted();
        updateState(started);
        return started;
    }

    protected void updateState(boolean started) {
        if (isInitialized()) {
            setState(started ? State.Busy : State.Ready);
        }
    }

    abstract protected void doStart() throws IOException, InterruptedException;

    abstract protected void doStop() throws IOException, InterruptedException;

    abstract protected boolean getStarted() throws IOException, InterruptedException;

    @Override
    final public CameraImageDescriptor readImageDescriptor() throws IOException, InterruptedException {
        if (updatingCache){
            return (CameraImageDescriptor) take();
        }          
        CameraImageDescriptor ret = doReadImageDescriptor();
        setCache(ret);
        return ret;
    }

    public CameraImageDescriptor takeImageDescriptor() {
        return (CameraImageDescriptor) take();
    }

    //Overridables
    protected void onData(Object data) {

    }

    protected CameraImageDescriptor doReadImageDescriptor() throws IOException, InterruptedException {
        CameraImageDescriptor ret = new CameraImageDescriptor();
        ret.dataType = getDataType();
        ret.colorMode = getColorMode(); //Must be before image size, because image size register changes according to color mode
        int[] size = getImageSize();
        ret.width = size[0];
        ret.height = size[1];
        return ret;
    }

    class BidimentionalImageRegister extends ReadonlyRegisterBase implements ReadonlyRegisterMatrix, ReadableCalibratedMatrix {

        public BidimentionalImageRegister(String name) {
            super(name);
        }

        @Override
        public Object take() {
            try {
                Object data = getDataArray().take();
                return Convert.reshape(data, getHeight(), getWidth());
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        protected Object doRead() throws IOException, InterruptedException {
            CameraBase.this.update();
            ReadonlyRegisterArray dataArray = getDataArray();
            if (dataArray != null) {
                getDataArray().update();
            }
            return take();
        }

        @Override
        public int getWidth() {
            CameraImageDescriptor desc = takeImageDescriptor();
            if (desc == null) {
                return -1;
            }
            return desc.width;
        }

        @Override
        public int getHeight() {
            CameraImageDescriptor desc = takeImageDescriptor();
            if (desc == null) {
                return -1;
            }
            return desc.height;
        }

        @Override
        public MatrixCalibration getCalibration() {
            CameraImageDescriptor desc = takeImageDescriptor();
            if (desc == null) {
                return null;
            }
            return desc.calibration;
        }
    }

}
