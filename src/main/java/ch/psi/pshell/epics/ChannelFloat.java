package ch.psi.pshell.epics;

/**
 * Wraps an EPICS PV as a float register.
 */
public class ChannelFloat extends EpicsRegisterNumber<Float> {

    public ChannelFloat(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelFloat(String name, String channelName, int precision) {
        super(name, channelName, precision);
    }

    public ChannelFloat(String name, String channelName, int precision, boolean timestamped) {
        super(name, channelName, precision, timestamped);
    }

    public ChannelFloat(String name, String channelName, int precision, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, precision, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.FloatTimestamp.class;
        } else {
            return Float.class;
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(0.0f);
    }

}
