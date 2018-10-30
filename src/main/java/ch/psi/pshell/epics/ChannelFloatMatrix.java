package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.FloatType;

/**
 * Wraps an EPICS PV as an float register matrix.
 */
public class ChannelFloatMatrix extends GenericMatrix implements FloatType{

    public ChannelFloatMatrix(String name, String channelName, int width, int height) {
        this(name, channelName, width, height, true);
    }

    public ChannelFloatMatrix(String name, String channelName, int width, int height, boolean timestamped) {
        this(name, channelName, width, height, timestamped, timestamped ? Epics.getDefaultInvalidValueAction(): null);
    }

    public ChannelFloatMatrix(String name, String channelName, int width, int height, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, width, height, timestamped, invalidAction, float[].class.getName());
    }
}
