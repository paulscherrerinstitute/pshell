package ch.psi.pshell.epics;

import ch.psi.jcae.Channel;
import ch.psi.jcae.impl.type.TimestampValue;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.pshell.device.RegisterConfig;
import ch.psi.pshell.device.TimestampedValue;
import ch.psi.utils.Reflection.Hidden;
import java.util.logging.Level;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * Base for all EPICS register classes.
 */
public abstract class EpicsRegister<T> extends RegisterBase<T> {    
    Channel<T> channel;
    int maximumSize = UNDEFINED;
    final String channelName;
    final protected Boolean timestamped;
    final protected Boolean requestMetadata;
    final protected InvalidValueAction invalidAction;
    volatile Severity severity;

    public static class EpicsRegisterConfig extends RegisterConfig {

        public boolean timestamped;
        public boolean status;
        public InvalidValueAction invalidValueAction;
    }

    /**
     * Persisted configuration
     */
    protected EpicsRegister(String name, String channelName, EpicsRegisterConfig config) {
        super(name, config);
        this.channelName = channelName;
        timestamped = getConfig().timestamped;
        requestMetadata = getConfig().status || timestamped;
        invalidAction = getConfig().status ? getConfig().invalidValueAction : null;
    }

    /*
     * Volatile configuration
     */
    protected EpicsRegister(String name, String channelName) {
        this(name, channelName, UNDEFINED_PRECISION);
    }

    protected EpicsRegister(String name, String channelName, int precision) {
        this(name, channelName, precision, true);
    }

    protected EpicsRegister(String name, String channelName, int precision, boolean timestamped) {
        this(name, channelName, precision, timestamped, timestamped ? Epics.getDefaultInvalidValueAction() : null); //By default, if not timestamped, request only value data
    }

    protected EpicsRegister(String name, String channelName, int precision, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, precision);
        this.channelName = channelName;
        this.timestamped = timestamped;
        this.invalidAction = invalidAction;
        this.requestMetadata = (this.invalidAction != null) || timestamped;
    }

    @Override
    public EpicsRegisterConfig getConfig() {
        return (EpicsRegisterConfig) super.getConfig();
    }

    public String getChannelName() {
        return channelName;
    }

    @Override
    protected T doRead() throws IOException, InterruptedException {
        try {
            if ((channel == null) || (!channel.isConnected())) {
                doInitialize();
            }
            if (invalidAction != null) {
                T ret = channel.getValue(isForcedRead());
                if ((((TimestampValue<T>) ret).getSeverity()) == Severity.Invalid.ordinal()) {
                    if (invalidAction == InvalidValueAction.Nullify) {
                        ((TimestampValue<T>) ret).setValue(null);
                    }
                    if (invalidAction == InvalidValueAction.Exception) {
                        throw new IOException("Value is invalid");
                    }
                }
                return ret;
            }
            return channel.getValue(isForcedRead());
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DeviceException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void doWrite(T value) throws IOException, InterruptedException {
        try {
            if ((channel == null) || (!channel.isConnected())) {
                doInitialize();
            }
            if (requestMetadata) {
                TimestampValue val = (TimestampValue) getType().newInstance();
                val.setTimestampPrimitive(System.currentTimeMillis());
                val.setValue(value);
                value = (T) val;    //value is actually a Number or array, but this "fake" cast won't rise exception.
            }
            if (blockingWrite) {
                channel.setValue(value);
            } else {
                channel.setValueNoWait(value);
            }

        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DeviceException(ex.getMessage(), ex);
        }
    }

    //final Object monitorLock=new Object();
    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (channel != null) {
            try {
                channel.setMonitored(value);
                if (value) {
                    channel.addPropertyChangeListener(changeListener);
                    request();
                } else {
                    channel.removePropertyChangeListener(changeListener);
                }
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
    }

    PropertyChangeListener changeListener = (PropertyChangeEvent pce) -> {
        if (pce.getPropertyName().equals(Channel.PROPERTY_VALUE)) {
            onChannelValueChange((T) pce.getNewValue(), (T) pce.getOldValue());
        }
    };

    void onChannelValueChange(T value, T former) {
        try {
            if (invalidAction != null) {
                if ((((TimestampValue<T>) value).getSeverity()) == Severity.Invalid.ordinal()) {
                    if (invalidAction == InvalidValueAction.Nullify) {
                        ((TimestampValue<T>) value).setValue(null);
                    }
                    if (invalidAction == InvalidValueAction.Exception) {
                        return;
                    }
                }
            }
            onReadout(value);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }

    @Override
    protected void onReadout(Object value) {
        if (requestMetadata && !isSimulated()) {
            try {
                TimestampValue<T> tv = ((TimestampValue<T>) value);
                T val = tv.getValue();
                int severityIndex = tv.getSeverity();
                synchronized (requestMetadata) {
                    severity = ((severityIndex >= 0) && (severityIndex < Severity.values().length)) ? Severity.values()[tv.getSeverity()] : null;
                    if (timestamped) {
                        setCache(convertFromRead(val), tv.getTimestampPrimitive(), tv.getNanosecondOffset());
                    } else {
                        setCache(convertFromRead(val));
                    }
                }
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        } else {
            super.onReadout(value);
        }
    }

    @Override
    public TimestampedValue takeTimestamped() {
        if (requestMetadata) {
            synchronized (requestMetadata) {
                TimestampedValue val = super.takeTimestamped();
                return (val == null) ? null : new EpicsTimestampedValue(val.getValue(), val.getTimestamp(), val.getNanosOffset(), severity);
            }
        } else {
            return super.takeTimestamped();
        }
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        try {
            if (channel != null) {
                Epics.closeChannel(channel);
                channel = null;
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        super.doInitialize();

        try {
            if (!isSimulated()) {
                channel = (Channel<T>) Epics.newChannel(getChannelName(), getType());
                maximumSize = channel.getSize();
            }            
            if (isMonitored()) {
                //If this is an array then sets monitor after the size is initialized.            
                if (! (this instanceof EpicsRegisterArray)) {
                    doSetMonitored(true);
                }
            }            
        } catch (InterruptedException ex) {
            throw ex;
        } catch (java.util.concurrent.TimeoutException ex) {
            throw new DeviceTimeoutException("Timeout on channel creation:" + getChannelName());
        } catch (Exception ex) {
            throw new DeviceException("Error creating channel: " + getChannelName(), ex);
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        maximumSize = (Epics.getMaxArrayBytes()) != null ? Epics.getMaxArrayBytes() : 1000000;
    }

    abstract protected Class getType();

    public int getMaximumSize() {
        return maximumSize;
    }

    volatile boolean blockingWrite = false;

    /**
     * If true write operation waits actual position change
     */
    public boolean isBlockingWrite() {
        return blockingWrite;
    }

    public void setBlockingWrite(boolean value) {
        blockingWrite = value;
    }

    private volatile boolean forceRead = true;

    /**
     * Force device access if device is monitored.
     */
    @Hidden
    public boolean isForcedRead() {
        return forceRead;
    }

    @Hidden
    public void setForcedRead(boolean value) {
        forceRead = value;
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        if (channel != null) {
            Epics.closeChannel(channel);
            channel = null;
        }
    }

}
