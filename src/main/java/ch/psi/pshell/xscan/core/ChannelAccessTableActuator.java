package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This actuator sets an Channel Access channel by using the positions from the given table.
 */
public class ChannelAccessTableActuator<T> implements Actor {

    private static Logger logger = Logger.getLogger(ChannelAccessTableActuator.class.getName());

    boolean asynchronous = false;

    /**
     * Position table
     */
    final double[] table;

    /**
     * Level of accuracy the positioner need to have (e.g. if a positioner is set to 1 the readback set value of the
     * positioner need to have at lease 1+/-accuracy)
     */
    double accuracy = 0.1;

    /**
     * Execution count of actuator. This variable is used to minimize the floating point rounding errors for calculating
     * the next step.
     */
    int count;

    /**
     * Flag that indicates whether there is a next set value for the Actor
     */
    boolean next;

    Channel<Double> channel;
    Channel<T> doneChannel = null;

    final T doneValue;
    final long doneDelay;

    /**
     * Flag that indicates whether the actor moves in the positive direction
     */
    boolean positiveDirection = true;
    final boolean originalPositiveDirection;

    /**
     * Maximum move time (in milliseconds)
     */
    Long timeout = null;

    boolean checkActorSet = true;

    /**
     * Constructor - Initialize actor
     *
     * @param channelName	Name of the channel to set
     * @param table	Position table with the explicit positions for each step
     * @param timeout	Maximum move time (in milliseconds)
     */
    public ChannelAccessTableActuator(Channel<Double> channel, double[] table, Long timeout) {
        this(channel, null, null, 0, table, timeout);
    }

    /**
     * Constructor
     *
     * @param channel
     * @param doneChannel
     * @param doneValue
     * @param doneDelay
     * @param table
     * @param timeout	Maximum move time (in milliseconds)
     */
    public ChannelAccessTableActuator(Channel<Double> channel, Channel<T> doneChannel, T doneValue, double doneDelay, double[] table, Long timeout) {

        this.doneValue = doneValue;
        this.doneDelay = (long) Math.floor((doneDelay * 1000));

        if (table == null) {
            throw new IllegalArgumentException("Null table is not accepted");
        }
        if (table.length == 0) {
            throw new IllegalArgumentException("Position table need to have at least one position");
        }

        this.table = table;

        // Validate and save timeout parameter
        if (timeout != null && timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be >0 or null");
        } else {
            this.timeout = timeout;
        }

        init();

        // Save the initial direction
        this.originalPositiveDirection = positiveDirection;

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
        logger.finer("Set actuator channel " + channel.getName() + " to value: " + table[count]);
        try {
            if (!asynchronous) {
                if (timeout == null) {
                    channel.setValue(table[count]);
                } else {
                    channel.setValueAsync(table[count]).get(timeout, TimeUnit.MILLISECONDS);
                }
            } else {
                channel.setValueNoWait(table[count]);
            }

            if (doneChannel != null) {
                Thread.sleep(doneDelay);
                doneChannel.waitForValue(doneValue);
            }

            // Check whether the set value is really on the value that was set before.
            if (doneChannel == null && !asynchronous && checkActorSet) {
                if (Math.abs(channel.getValue() - table[count]) > accuracy) {
                    throw new RuntimeException("Actuator [channel: " + channel.getName() + "] could not be set to the value " + table[count] + " The readback of the set value does not match the value that was set");
                }
            }

        } catch (InterruptedException e) {
            throw e; 
        } catch (Exception e) {
            throw new RuntimeException("Move actuator [channel: " + channel.getName() + "] to value " + table[count], e);
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

    @Override
    public boolean hasNext() {
        return next;
    }

    @Override
    public void init() {

        // Set first set value to the start value
        if (positiveDirection) {
            this.count = 0; // Set count to the first element
        } else {
            this.count = table.length - 1; // Set count to the last element
        }

        if (table.length > 0) {
            this.next = true;
        }
    }

    @Override
    public void reverse() {
        if (positiveDirection) {
            positiveDirection = false;
        } else {
            positiveDirection = true;
        }
    }

    @Override
    public void reset() {
        this.positiveDirection = this.originalPositiveDirection;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }
}
