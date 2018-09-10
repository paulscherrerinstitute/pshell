package ch.psi.pshell.epics;

import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.Register.RegisterArray;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base for all EPICS array register classes.
 */
public abstract class EpicsRegisterArray<T> extends EpicsRegister<T> implements RegisterArray<T> {

    Integer size;

    public static class EpicsRegisterArrayConfig extends EpicsRegister.EpicsRegisterConfig {

        public int size;
    }

    /**
     * Persisted configuration
     */
    protected EpicsRegisterArray(String name, String channelName, EpicsRegisterArrayConfig config) {
        super(name, channelName, config);
        size = getConfig().size;
    }

    /*
     * Volatile configuration
     */
    protected EpicsRegisterArray(String name, String channelName) {
        this(name, channelName, -1);
    }

    protected EpicsRegisterArray(String name, String channelName, int precision) {
        this(name, channelName, precision, -1);
    }

    protected EpicsRegisterArray(String name, String channelName, int precision, int size) {
        this(name, channelName, precision, size, true);
    }

    protected EpicsRegisterArray(String name, String channelName, int precision, int size, boolean timestamped) {
        super(name, channelName, precision, timestamped);
        this.size = size;
    }

    protected EpicsRegisterArray(String name, String channelName, int precision, int size, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, precision, timestamped, invalidAction);
        this.size = size;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        if (size > 0) {
            updateSize();
        } else {
            if (isSimulated()) {
                Object val = take();
                if ((val != null) && (val.getClass().isArray())) {
                    size = Array.getLength(val);
                    return;
                }
            }
            size = getMaximumSize();
        }
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void setSize(int size) throws IOException {
        if (size < 0) {
            size = getMaximumSize();
        }
        size = Math.min(size, getMaximumSize());
        size = Math.max(size, 1);   //Error creating array of size 0
        if (size != this.size) {
            this.size = size;
            updateSize();
        }
    }

    void updateSize() throws IOException {
        if (channel != null) {
            try {
                channel.setSize(size);
            } catch (Exception ex) {
                throw new DeviceException(ex);
            }
        }
    }

    public void setSizeToValidElements() throws IOException, InterruptedException {
        setSize(getValidElemets());
    }

    public int getValidElemets() throws IOException, InterruptedException {
        try {
            return Epics.get(channelName + ".NORD", Integer.class);
            } catch (ChannelException | java.util.concurrent.TimeoutException | ExecutionException ex) {
                throw new DeviceException(ex);
            }
    }

    @Override
    public EpicsRegisterArrayConfig getConfig() {
        return (EpicsRegisterArrayConfig) super.getConfig();
    }

    abstract public int getComponentSize();
}
