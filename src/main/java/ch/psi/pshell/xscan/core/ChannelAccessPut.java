package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Perform a put on the specified Channel Access channel. The put can be done synchronous or asynchronously.
 */
public class ChannelAccessPut<E> implements Action {

    private static Logger logger = Logger.getLogger(ChannelAccessPut.class.getName());

    private final Channel<E> channel;
    private final E value;
    private final boolean asynchronous;

    private final Long timeout;

    /**
     * @param channel
     * @param value	Value to set
     * @param asynchronous	Flag whether to set the value synchronous (wait for response) or asynchronously (fire and
     * forget)
     * @param timeout	Timeout used for set operation (time that set need to come back)
     */
    public ChannelAccessPut(Channel<E> channel, E value, boolean asynchronous, Long timeout) {

        this.channel = channel;
        this.value = value;
        this.asynchronous = asynchronous;
        this.timeout = timeout;
    }

    /**
     * Additional constructor for convenience. This constructor defaults the operation type to synchronous put.
     *
     * @param channel
     * @param value	Value to set
     */
    public ChannelAccessPut(Channel<E> channel, E value) {
        this(channel, value, false, null);
    }

    /**
     * @throws RuntimeException	Cannot set value on channel
     */
    @Override
    public void execute() throws InterruptedException {
        logger.finest("Put to channel: " + channel.getName() + " asynchronous: " + asynchronous);
        try {
            if (asynchronous) {
                channel.setValueNoWait(value);
            } else {
                if (timeout == null) {
                    channel.setValue(value);
                } else {
                    channel.setValueAsync(value).get(timeout, TimeUnit.MILLISECONDS);
                }
            }
        } catch (ExecutionException | TimeoutException | ChannelException e) {
            throw new RuntimeException("Unable to set channel [name:" + channel.getName() + "] to value " + value, e);
        }
    }
}
