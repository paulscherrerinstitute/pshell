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

public class DeviceFunctionActuator<T> extends ChannelAccessFunctionActuator{

    private static Logger logger = Logger.getLogger(DeviceFunctionActuator.class.getName());
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
    public DeviceFunctionActuator(Device dev, Channel<T> doneChannel, T doneValue, double doneDelay, Function function, double start, double end, double stepSize, Long timeout) {
        super(null, doneChannel, doneValue, doneDelay, function, start, end, stepSize, timeout);
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

        // Set actuator channel
        logger.log(Level.FINER, "Set actuator device {0} to value: {1}", new Object[]{device.getName(), value});
        try {
            double fvalue = function.calculate(value);

            if (asynchronous) {
                if (device instanceof Movable movable){
                    movable.moveAsync(fvalue);
                } else if (device instanceof Writable writable){
                    writable.writeAsync(fvalue);
                }
             } else {    
                if (device instanceof Movable movable){
                    if (timeout == null){
                         movable.move(fvalue);
                    } else {
                        movable.move(fvalue, timeout.intValue());
                    }
                } else if (device instanceof Writable writable){
                    writable.write(fvalue);
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
                double a = Math.abs(c - fvalue);
                if (a > accuracy) {
                    throw new RuntimeException("Actuator [device: " + device.getName() + "] could not be set to the value " + fvalue + " The readback of the set value does not match the value that was set [value: " + c + " delta: " + a + " accuracy: " + accuracy + "]");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Unable to move actuator [device: " + device.getName() + "] to value " + value, e);
        }

        count++;
        double nextValue = start + (count * stepSize * direction); // Done like this to keep floating point rounding errors minimal

        if ((direction == 1 && nextValue <= end) || (direction == -1 & nextValue >= end)) {

            // Apply function
//			nextValue = function.calculate(nextValue);
            logger.log(Level.FINER, "Next actor value: {0}", nextValue);
            value = nextValue;
            this.next = true;
        } else {
            // There is no next set value
            this.next = false;
        }
    }
}
