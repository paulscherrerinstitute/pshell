package ch.psi.pshell.imaging;

import ch.psi.pshell.device.Camera;
import ch.psi.pshell.device.Camera.ColorMode;
import ch.psi.pshell.device.CameraImageDescriptor;

/**
 * Image source connecting to a Camera device, such as an EPICS area detector.
 */
public class CameraSource extends DeviceSource {

    final Camera camera;

    public CameraSource(String name, Camera device) {
        super(name, device.getDataArray());
        this.camera = device;
    }

    @Override
    protected void onDataReceived(Object value) throws Exception {
        CameraImageDescriptor desc = (CameraImageDescriptor) camera.take();
        if (desc == null) {
            if (camera.isInitialized()) {
                camera.request();
            }
        } else if (desc.getPixels() == 0) {
            if (!camera.getState().isRunning()) {
                throw new Exception("Camera has not been not started");
            }
        } else {
            Data data = new Data(value, desc.width, desc.height, desc.dataType.isUnsigned(), desc.colorMode.getDepth());
            if (desc.calibration != null) {
                data.setCalibration(new Calibration(desc.calibration));
            }

            if ((desc.colorMode == ColorMode.RGB2)
                    || (desc.colorMode == ColorMode.RGB3)
                    || ((desc.colorMode == ColorMode.RGB1) && (desc.dataType.getSize() > 1))) {
                throw new Exception("Invalid image format");
            }

            pushData(data);
        }
    }
}
