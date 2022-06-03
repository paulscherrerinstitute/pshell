package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Movable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
            if (device instanceof ReadbackDevice) {
                readback = ((ReadbackDevice) device).getReadback();
            } else if (device instanceof Readable) {
                readback= ((Readable) device);
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
        logger.finest("Set actuator device " + device.getName() + " to value: " + value);
        try {
            double fvalue = function.calculate(value);

            if (asynchronous) {
                if (! (device instanceof Movable)){
                    ((Writable)device).writeAsync(fvalue);
                } else {
                     ((Movable)device).moveAsync(fvalue);
                }
             } else {    
                if (! (device instanceof Movable)){
                    ((Writable)device).write(fvalue);
                } else {
                    if (timeout == null){
                         ((Movable)device).move(fvalue);
                    } else {
                        ((Movable)device).move(fvalue, timeout.intValue());
                    }
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
                    throw new RuntimeException("Actor could not be set to the value " + fvalue + " The readback of the set value does not match the value that was set [value: " + c + " delta: " + a + " accuracy: " + accuracy + "]");
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
            logger.finer("Next actor value: " + nextValue);
            value = nextValue;
            this.next = true;
        } else {
            // There is no next set value
            this.next = false;
        }
    }
}
