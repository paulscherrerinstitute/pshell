package ch.psi.pshell.epics;

import ch.psi.pshell.imaging.RegisterArraySource;

/**
 * Image source based on an EPICS waveform channel.
 */
public class ArraySource extends RegisterArraySource {

    @Override
    public GenericArray getDevice() {
        return (GenericArray) super.getDevice();
    }

    public ArraySource(String name, String channelName) {
        this(name, channelName, null);
    }

    public ArraySource(String name, String channelName, int size) {
        this(name, channelName, size, null, null);
    }

    public ArraySource(String name, String channelName, int size, String type) {
        this(name, channelName, size, type, null);
    }

    protected ArraySource(String name, String channelName, RegisterArraySourceConfig config) {
        this(name, channelName, SIZE_MAX, config);
    }

    protected ArraySource(String name, String channelName, int size, RegisterArraySourceConfig config) {
        this(name, channelName, size, null, config);
    }

    protected ArraySource(String name, String channelName, int size, String type, RegisterArraySourceConfig config) {
        super(name, new GenericArray(name + " data", channelName, size, false, null, type), config);
    }

}
