package ch.psi.pshell.xscan.core;

import ch.psi.pshell.device.DescStatsDouble;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.utils.Str;
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
            if (device instanceof ReadbackDevice readbackDevice) {
                readback = readbackDevice.getReadback();
            } else if (device instanceof Readable readable) {
                readback= readable;
            } else {
                throw  new Exception("Not Readable");
            }
                
            v = readback.read();
            if (v instanceof DescStatsDouble descStatsDouble){
                v=descStatsDouble.doubleValue();
            }
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Read device [{0}]: {1}", new Object[]{device.getName(), Str.toString(v, 10)});
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
