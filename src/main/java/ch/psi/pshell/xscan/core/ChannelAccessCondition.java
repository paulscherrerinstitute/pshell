package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * Perform a put on the specified Channel Access channel. The put can be done synchronous or asynchronously.
 */
public class ChannelAccessCondition<E> implements Action {

    private static Logger logger = Logger.getLogger(ChannelAccessCondition.class.getName());

    private final Channel<E> channel;
    private final E expectedValue;
    private final Comparator<E> comparator;
    private final Long timeout;

    private volatile boolean abort = false;
    private volatile Thread waitT = null;

    /**
     * @param channel	Channel to wait value for
     * @param expectedValue	Value to wait for
     * @param timeout	Timeout of the condition in milliseconds (null accepted - will take default wait timeout for
     * channels ch.psi.jcae.ChannelBeanFactory.waitTimeout)
     *
     * @throws	IllegalArgumentException	Timeout specified is not >=0
     */
    public ChannelAccessCondition(Channel<E> channel, E expectedValue, Long timeout) {

        if (timeout != null && timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be > 0");
        }

        this.channel = channel;
        this.expectedValue = expectedValue;
        this.comparator = null;
        this.timeout = timeout;
    }

    public ChannelAccessCondition(Channel<E> channel, E expectedValue, Comparator<E> comparator, Long timeout) {

        if (timeout != null && timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be > 0");
        }

        this.channel = channel;
        this.expectedValue = expectedValue;
        this.comparator = comparator;
        this.timeout = timeout;
    }

    /**
     * @throws RuntimeException	Channel value did not reach expected value (within the specified timeout period)
     */
    @Override
    public void execute() throws InterruptedException {
        abort = false;
        logger.finest("Checking channel " + channel.getName() + " for value " + expectedValue + " [timeout: " + timeout + "]");
        try {
            waitT = Thread.currentThread();
            try {
                if (comparator == null) {
                    if (timeout == null) {
                        channel.waitForValue(expectedValue);
                    } else {
                        channel.waitForValue(expectedValue, timeout);
                    }
                } else {
                    if (timeout == null) {
                        channel.waitForValue(expectedValue, comparator);
                    } else {
                        channel.waitForValue(expectedValue, comparator, timeout);
                    }
                }
            } catch (Exception e) {
                if ( e instanceof InterruptedException) {
                    if (abort){
                        return;
                    }
                    throw (InterruptedException)e;
                }
                throw new RuntimeException("Channel [name:" + channel.getName() + "] did not reach expected value " + expectedValue + " ", e);
            }
        } finally {
            waitT = null;
        }
    }

    @Override
    public void abort() {
        abort = true;
        if (waitT != null) {
            waitT.interrupt();
        }
    }
}
