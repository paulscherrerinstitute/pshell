package ch.psi.pshell.bs;

import ch.psi.pshell.device.RegisterConfig;

/**
 * Configuration for Scalar objects.
 */
public class StreamChannelConfig extends RegisterConfig {

    public String id;
    public int modulo = StreamChannel.DEFAULT_MODULO;
    public int offset = StreamChannel.DEFAULT_OFFSET;
}
