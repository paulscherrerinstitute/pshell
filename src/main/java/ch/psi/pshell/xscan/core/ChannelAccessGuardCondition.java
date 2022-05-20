package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;

public class ChannelAccessGuardCondition<T> {

    private final Channel<T> channel;
    private final T value; // Value of the channel to meet condition

    public ChannelAccessGuardCondition(Channel<T> channel, T value) {
        this.channel = channel;
        this.value = value;
    }

    public Channel<T> getChannel() {
        return channel;
    }

    public T getValue() {
        return value;
    }
}
