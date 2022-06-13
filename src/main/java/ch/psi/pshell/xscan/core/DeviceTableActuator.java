package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Movable;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.Writable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * This actuator sets an Channel Access channel by using the positions from the given table.
 */
public class DeviceTableActuator<T> extends ChannelAccessTableActuator {

    private static Logger logger = Logger.getLogger(DeviceTableActuator.class.getName());

    Device device;
    ch.psi.pshell.device.Readable readback;

    /**
     * Constructor
     *
     * @param device
     * @param doneChannel
     * @param doneValue
     * @param doneDelay
     * @param table
     * @param timeout	Maximum move time (in milliseconds)
     */
    public DeviceTableActuator(Device device, Channel<T> doneChannel, T doneValue, double doneDelay, double[] table, Long timeout) {
        super(null, doneChannel, doneValue, doneDelay, table, timeout);
        if (! (device instanceof Writable)){
            throw new IllegalStateException("Device " + device.getName() +  " is not Writable");
        }
        if (checkActorSet) {
            if (device instanceof ReadbackDevice) {
                readback = ((ReadbackDevice) device).getReadback();
            } else if (device instanceof ch.psi.pshell.device.Readable) {
                readback= ((ch.psi.pshell.device.Readable) device);
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

        double fvalue = table[count];
        // Set actuator channel
        logger.finest("Set device " + channel.getName() + " to value: " + fvalue);
        
        
        try {
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
            throw new RuntimeException("Move actuator [device: " + device.getName() + "] to value " + fvalue, e);
        }

        if (positiveDirection) {
            count++;
            if (count < table.length) {
                this.next = true;
            } else {
                // There is no next set value
                this.next = false;
            }
        } else {
            count--;
            if (count >= 0) {
                this.next = true;
            } else {
                // There is no next set value
                this.next = false;
            }
        }
    }

}
