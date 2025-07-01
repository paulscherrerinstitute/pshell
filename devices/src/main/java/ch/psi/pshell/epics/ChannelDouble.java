package ch.psi.pshell.epics;

import ch.psi.pshell.device.Readable.DoubleType;

/**
 * Wraps an EPICS PV as a double register.
 */
public class ChannelDouble extends EpicsRegisterNumber<Double> implements DoubleType{

    public ChannelDouble(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelDouble(String name, String channelName, int precision) {
        super(name, channelName, precision);
    }

    public ChannelDouble(String name, String channelName, int precision, boolean timestamped) {
        super(name, channelName, precision, timestamped);
    }

    public ChannelDouble(String name, String channelName, int precision, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, precision, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.DoubleTimestamp.class;
        } else {
            return Double.class;
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(0.0);
    }
}
