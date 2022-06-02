package ch.psi.pshell.xscan.core;

import ch.psi.utils.Str;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.Readable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Device sensor 
 */
public class DeviceSensor implements Sensor {

    private static Logger logger = Logger.getLogger(DeviceSensor.class.getName());

    private Device device;
    private final String id;
    private boolean failOnSensorError = true;

    public DeviceSensor(String id, Device device) {
        this.id = id;
        this.device = device;
    }

    public DeviceSensor(String id, Device device, boolean failOnSensorError) {
        this.id = id;
        this.device = device;
        this.failOnSensorError = failOnSensorError;
    }

    @Override
    public Object read() throws InterruptedException {
        Object v;
        try {
            Readable readback;
            if (device instanceof ReadbackDevice) {
                readback = ((ReadbackDevice) device).getReadback();
            } else if (device instanceof Readable) {
                readback= ((Readable) device);
            } else {
                throw  new Exception("Not Readable");
            }
                
            v = readback.read();
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Read device [" + device.getName() + "]: " + Str.toString(v, 10));
            }
        } catch (Exception e) {
            String errMsg = "Unable to get value from device [" + device.getName() + "]: " + e.getMessage();
            logger.fine(errMsg);
            if (failOnSensorError) {
                throw new RuntimeException(errMsg, e);
            }
            v = null;
        }
        return (v);
    }

    @Override
    public String getId() {
        return id;
    }
}
