package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Movable;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.Writable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This actuator sets an Channel Access channel from a start to an end value by doing discrete steps.
 */
public class DeviceLinearActuator<T> extends ChannelAccessLinearActuator {
    private static Logger logger = Logger.getLogger(DeviceLinearActuator.class.getName());
    Device device;
    Readable readback;
    
    /**
     * Constructor
     *
     * @param device
     * @param doneChannel	If null actor will not wait (for this channel) to continue
     * @param doneValue
     * @param doneDelay	Delay in seconds before checking the done channel
     * @param start
     * @param end
     * @param stepSize
     * @param timeout	Maximum move time (in milliseconds)
     */
    public DeviceLinearActuator(Device device, Channel<T> doneChannel, T doneValue, double doneDelay, double start, double end, double stepSize, Long timeout) {
        super(null, doneChannel, doneValue, doneDelay, start, end, stepSize, timeout);
        if (! (device instanceof Writable)){
            throw new IllegalStateException("Device " + device.getName() +  " is not Writable");
        }
        if (checkActorSet) {
            if (device instanceof ReadbackDevice readbackDevice) {
                readback = readbackDevice.getReadback();
            } else if (device instanceof Readable readable) {
                readback= readable;
            } else {
                throw new IllegalStateException("Device " + device.getName() +  " is not readable for checking");
            }
        }
        this.device = device;
    }

    @Override
    public void set() throws InterruptedException {

        // Throw an IllegalStateException in the case that set is called although there is no next step.
        if (!next) {
            throw new IllegalStateException("The actuator does not have any next step.");
        }

        // Set device
        logger.log(Level.FINER, "Set device {0} to value: {1}", new Object[]{device.getName(), value});
        try {

            if (asynchronous) {
                switch (device) {
                    case Movable movable -> movable.moveAsync(value);
                    case Writable writable -> writable.writeAsync(value);
                    default -> {}
                }
             } else {    
                switch (device) {
                    case Movable movable -> {
                        if (timeout == null){
                            movable.move(value);
                        } else {
                            movable.move(value, timeout.intValue());
                        }
                    }
                    case Writable writable -> writable.write(value);
                    default -> {}
                }
            }

            if (doneChannel != null) {
                Thread.sleep(doneDelay);
                doneChannel.waitForValue(doneValue);
            }

            // Check whether the set value is really on the value that was set before.
            if (checkActorSet) {
                double c;
                try {
                    c = ((Number)readback.read()).doubleValue();
                } catch (IOException ex) {
                     throw new RuntimeException(ex);
                }
                double a = Math.abs(c - value);
                if (a > accuracy) {
                    throw new RuntimeException("Device [" + device.getName() + "] could not be set to the value " + value + " The readback of the set value does not match the value that was set [value: " + c + " delta: " + a + " accuracy: " + accuracy + "]");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Unable to move device [" + device.getName() + "] to value " + value, e);
        }

        count++;
        double nextValue = start + (count * stepSize * direction); // Done like this to keep floating point rounding errors minimal

        if ((direction == 1 && nextValue <= end) || (direction == -1 & nextValue >= end)) {
            logger.log(Level.FINER, "Next actor value: {0}", nextValue);
            value = nextValue;
            this.next = true;
        } else {
            // There is no next set value
            this.next = false;
        }
    }
}
