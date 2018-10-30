package ch.psi.pshell.epics;

import ch.psi.pshell.device.Register.RegisterString;

/**
 * Wraps an EPICS PV as a string register.
 */
public class ChannelString extends EpicsRegister<String> implements RegisterString{

    public ChannelString(String name, String channelName) {
        super(name, channelName);
    }

    public ChannelString(String name, String channelName, boolean timestamped) {
        super(name, channelName, -1, timestamped);
    }

    public ChannelString(String name, String channelName, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, -1, timestamped, invalidAction);
    }

    @Override
    protected Class getType() {
        if (requestMetadata) {
            return ch.psi.jcae.impl.type.StringTimestamp.class;
        } else {
            return String.class;
        }
    }

}
