package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * This actuator sets an Channel Access channel from a start to an end value by doing discrete steps.
 */
public class ChannelAccessLinearActuator<T> implements Actor {

    private static Logger logger = Logger.getLogger(ChannelAccessLinearActuator.class.getName());

    boolean asynchronous = false;

    double start;
    double end;
    double stepSize;
    int direction;

    /**
     * Execution count of actuator. This variable is used to minimize the floating point rounding errors for calculating
     * the next step.
     */
    int count;

    /**
     * Flag that indicates whether there is a next set value for the Actor
     */
    boolean next;

    /**
     * Value to set at next @see ch.psi.pshell.xscan.engine.Actor#set() call
     */
    double value;

    /**
     * Level of accuracy the positioner need to have (e.g. if a positioner is set to 1 the readback set value of the
     * positioner need to have at lease 1+/-accuracy) Default is stepSize/2
     */
    double accuracy;

    final T doneValue;
    final long doneDelay;

    final double originalStart;
    final double originalEnd;
    final int originalDirection;

    Long timeout; // Set timeout

    final Channel<Double> channel;
    final Channel<T> doneChannel;

    boolean checkActorSet = true;

    /**
     * Constructor
     *
     * @param channel
     * @param doneChannel	If null actor will not wait (for this channel) to continue
     * @param doneValue
     * @param doneDelay	Delay in seconds before checking the done channel
     * @param start
     * @param end
     * @param stepSize
     * @param timeout	Maximum move time (in milliseconds)
     */
    public ChannelAccessLinearActuator(Channel<Double> channel, Channel<T> doneChannel, T doneValue, double doneDelay, double start, double end, double stepSize, Long timeout) {

        this.doneValue = doneValue;
        this.doneDelay = (long) Math.floor((doneDelay * 1000));
        this.start = start;
        this.end = end;

        if (stepSize <= 0) {
            throw new IllegalArgumentException("Step size [" + stepSize + "] must be > 0");
        }
        this.stepSize = stepSize;

        this.accuracy = stepSize / 2;

        // Validate and save timeout parameter
        if (timeout != null && timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be >0 or null");
        } else {
            this.timeout = timeout;
        }

        init();

        // Save original settings
        this.originalStart = start;
        this.originalEnd = end;
        this.originalDirection = direction;

        this.channel = channel;
        this.doneChannel = doneChannel;
    }

    @Override
    public void set() throws InterruptedException {

        // Throw an IllegalStateException in the case that set is called although there is no next step.
        if (!next) {
            throw new IllegalStateException("The actuator does not have any next step.");
        }

        // Set actuator channel
        logger.finest("Set actuator channel " + channel.getName() + " to value: " + value);
        try {

            if (!asynchronous) {
                if (timeout == null) {
                    channel.setValue(value);
                } else {
                    channel.setValueAsync(value).get(timeout, TimeUnit.MILLISECONDS);
                }
            } else {
                channel.setValueNoWait(value);
            }

            if (doneChannel != null) {
                Thread.sleep(doneDelay);
                doneChannel.waitForValue(doneValue);
            }

            // Check whether the set value is really on the value that was set before.
            if (checkActorSet) {
                double c = channel.getValue(true);
                double a = Math.abs(c - value);
                if (a > accuracy) {
                    throw new RuntimeException("Actor could not be set to the value " + value + " The readback of the set value does not match the value that was set [value: " + c + " delta: " + a + " accuracy: " + accuracy + "]");
                }
            }

        } catch (ExecutionException | TimeoutException | ChannelException e) {
            throw new RuntimeException("Unable to move actuator [channel: " + channel.getName() + "] to value " + value, e);
        }

        count++;
        double nextValue = start + (count * stepSize * direction); // Done like this to keep floating point rounding errors minimal

        if ((direction == 1 && nextValue <= end) || (direction == -1 & nextValue >= end)) {
            logger.finer("Next actor value: " + nextValue);
            value = nextValue;
            this.next = true;
        } else {
            // There is no next set value
            this.next = false;
        }
    }

    @Override
    public boolean hasNext() {
        return next;
    }

    @Override
    public void init() {
        this.count = 0;

        // Determine move direction
        this.direction = 1;
        if (start > end) {
            direction = -1; // Move in negative direction
        }

        // Set first set value to the start value
        this.value = start;
        this.next = true;
    }

    @Override
    public synchronized void reverse() {
        double oldStart = start;
        this.start = this.end;
        this.end = oldStart;

        // Determine move direction
        this.direction = 1;
        if (this.start > this.end) {
            direction = -1; // Move in negative direction
        }

    }

    @Override
    public void reset() {
        this.start = this.originalStart;
        this.end = this.originalEnd;
        this.direction = this.originalDirection;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }
}
