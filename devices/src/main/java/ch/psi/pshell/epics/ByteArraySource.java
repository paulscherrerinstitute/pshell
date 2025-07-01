package ch.psi.pshell.epics;

import ch.psi.pshell.imaging.RegisterArraySource;

/**
 * Image source based on an EPICS byte waveform channel.
 */
public class ByteArraySource extends RegisterArraySource {

    public ByteArraySource(String name, String channelName) {
        this(name, channelName, SIZE_MAX);
    }

    public ByteArraySource(String name, String channelName, int size) {
        super(name, new ChannelByteArray(name + " data", channelName, size, false));
    }
}
