package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;

/**
 * Mapping of a script parameter to a channel bean.
 */
public class JythonParameterMappingChannel<T> extends JythonParameterMapping {

    private Channel<T> channel;

    public JythonParameterMappingChannel(String variable, Channel<T> channel) {
        super(variable);
        this.channel = channel;
    }

    public Channel<T> getChannel() {
        return channel;
    }
}
