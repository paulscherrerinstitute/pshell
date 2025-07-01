package ch.psi.pshell.epics;

import ch.psi.pshell.device.Register.RegisterNumber;
import java.io.IOException;

/**
 * Base for all EPICS scalar register classes.
 */
public abstract class EpicsRegisterNumber<T extends Number> extends EpicsRegister<T> implements RegisterNumber<T> {

    /**
     * Persisted configuration
     */
    protected EpicsRegisterNumber(String name, String channelName, EpicsRegister.EpicsRegisterConfig config) {
        super(name, channelName, config);
    }

    /*
     * Volatile configuration
     */
    protected EpicsRegisterNumber(String name, String channelName) {
        super(name, channelName);
    }

    protected EpicsRegisterNumber(String name, String channelName, int precision) {
        super(name, channelName, precision);
    }

    protected EpicsRegisterNumber(String name, String channelName, int precision, boolean timestamped) {
        super(name, channelName, precision, timestamped);
    }

    protected EpicsRegisterNumber(String name, String channelName, int precision, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, precision, timestamped, invalidAction);
    }

    @Override
    protected void doWrite(Number value) throws IOException, InterruptedException {
        Class type = getType();
        if (type != value.getClass()) {
            if (type == Double.class) {
                value = value.doubleValue();
            } else if (type == Float.class) {
                value = value.floatValue();
            } else if (type == Long.class) {
                value = value.longValue();
            } else if (type == Integer.class) {
                value = value.intValue();
            } else if (type == Short.class) {
                value = value.shortValue();
            } else if (type == Byte.class) {
                value = value.byteValue();
            }
        }
        super.doWrite((T) value);
    }

}
