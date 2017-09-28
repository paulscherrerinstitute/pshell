package ch.psi.pshell.epics;

/**
 * Wraps an EPICS PV as a short register.
 */
public class ChannelShort extends EpicsRegisterNumber<Short> {

    public ChannelShort(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelShort(String name, String channelName, boolean timestamped) {
        super(name, channelName, -1, timestamped);
    }

    public ChannelShort(String name, String channelName, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, -1, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.ShortTimestamp.class;
        } else {
            return Short.class;
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(0);
    }
}
