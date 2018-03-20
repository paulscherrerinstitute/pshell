package ch.psi.pshell.epics;

import ch.psi.pshell.imaging.RegisterArraySource;

/**
 * Wraps an EPICS PV as a byte register array.
 */
public class ByteArraySource extends RegisterArraySource {

    public ByteArraySource(String name, String channelName) {
        this(name, channelName, -1);
    }

    public ByteArraySource(String name, String channelName, int size) {
        super(name, new ChannelByteArray(name + " data", channelName, size, false));
    }
}
