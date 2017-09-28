package ch.psi.pshell.epics;

/**
 * Wraps an EPICS PV as an integer register.
 */
public class ChannelInteger extends EpicsRegisterNumber<Integer> {

    public ChannelInteger(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelInteger(String name, String channelName, boolean timestamped) {
        super(name, channelName, -1, timestamped);
    }

    public ChannelInteger(String name, String channelName, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, -1, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.IntegerTimestamp.class;
        } else {
            return Integer.class;
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(0);
    }
}
