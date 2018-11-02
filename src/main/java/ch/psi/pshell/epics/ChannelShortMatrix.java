package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.UnsignedShortType;

/**
 * Wraps an EPICS PV as an short register matrix.
 */
public class ChannelShortMatrix extends GenericMatrix implements UnsignedShortType{

    public ChannelShortMatrix(String name, String channelName, int width, int height) {
        this(name, channelName, width, height, true);
    }

    public ChannelShortMatrix(String name, String channelName, int width, int height, boolean timestamped) {
        this(name, channelName, width, height, timestamped, timestamped ? Epics.getDefaultInvalidValueAction(): null);
    }

    public ChannelShortMatrix(String name, String channelName, int width, int height, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, width, height, timestamped, invalidAction, short[].class.getName());
    }
}
