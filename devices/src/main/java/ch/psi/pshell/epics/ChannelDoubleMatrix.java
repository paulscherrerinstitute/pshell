package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.DoubleType;

/**
 * Wraps an EPICS PV as an double register matrix.
 */
public class ChannelDoubleMatrix extends GenericMatrix implements DoubleType{

    public ChannelDoubleMatrix(String name, String channelName, int width, int height) {
        this(name, channelName, width, height, true);
    }

    public ChannelDoubleMatrix(String name, String channelName, int width, int height, boolean timestamped) {
        this(name, channelName, width, height, timestamped, timestamped ? Epics.getDefaultInvalidValueAction(): null);
    }

    public ChannelDoubleMatrix(String name, String channelName, int width, int height, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, width, height, timestamped, invalidAction, double[].class.getName());
    }
}
