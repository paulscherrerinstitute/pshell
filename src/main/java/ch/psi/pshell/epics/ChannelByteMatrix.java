package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.IntegerType;

/**
 * Wraps an EPICS PV as an integer register matrix.
 */
public class ChannelByteMatrix extends GenericMatrix implements IntegerType{

    public ChannelByteMatrix(String name, String channelName, int width, int height) {
        this(name, channelName, width, height, true);
    }

    public ChannelByteMatrix(String name, String channelName, int width, int height, boolean timestamped) {
        this(name, channelName, width, height, timestamped, timestamped ? Epics.getDefaultInvalidValueAction(): null);
    }

    public ChannelByteMatrix(String name, String channelName, int width, int height, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, width, height, timestamped, invalidAction, int[].class.getName());
    }
}
