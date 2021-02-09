package ch.psi.pshell.epics;

import ch.psi.jcae.Channel;
import ch.psi.pshell.device.Register.RegisterArray;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;

/**
 * Base for all EPICS array register classes.
 */
public abstract class EpicsRegisterArray<T> extends EpicsRegister<T> implements RegisterArray<T> {    
    
    Integer configSize;
    Channel validElements;
    boolean keepToValidElements;
    int size;

    public static class EpicsRegisterArrayConfig extends EpicsRegister.EpicsRegisterConfig {
        public int size;
    }

    /**
     * Persisted configuration
     */
    protected EpicsRegisterArray(String name, String channelName, EpicsRegisterArrayConfig config) {
        super(name, channelName, config);
        configSize = getConfig().size;
        size = 0;
    }

    /*
     * Volatile configuration
     */
    protected EpicsRegisterArray(String name, String channelName) {
        this(name, channelName, UNDEFINED_PRECISION);
    }

    protected EpicsRegisterArray(String name, String channelName, int precision) {
        this(name, channelName, precision, SIZE_MAX);
    }

    protected EpicsRegisterArray(String name, String channelName, int precision, int size) {
        this(name, channelName, precision, size, true);
    }

    protected EpicsRegisterArray(String name, String channelName, int precision, int size, boolean timestamped) {
        super(name, channelName, precision, timestamped);
        this.configSize = size;
        size =  configSize;
    }

    protected EpicsRegisterArray(String name, String channelName, int precision, int size, boolean timestamped, InvalidValueAction invalidAction) {
        super(name, channelName, precision, timestamped, invalidAction);
        this.configSize = size;
        size =  configSize;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        try {
            if (validElements != null) {
                Epics.closeChannel(validElements);
                validElements = null;
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        keepToValidElements = false;
        size = 0;
        super.doInitialize();
        
        if (!isSimulated()) {            
            Integer maxArrayBytes = Epics.getMaxArrayBytes();
            if (maxArrayBytes != null) {
                int maxArraySize = maxArrayBytes / ((EpicsRegisterArray) this).getComponentSize();
                if (maximumSize > maxArraySize) {
                    maximumSize = maxArraySize;
                }
            }
            try {
                validElements = (Channel<T>) Epics.newChannel(channelName + ".NORD", Integer.class);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
        if (isSimulated() && (configSize <= 0)) {
            Object val = take();
            if ((val != null) && (val.getClass().isArray())) {
                configSize = Array.getLength(val);
            }            
        }
        setSize(configSize);
        if (isMonitored()) {
            doSetMonitored(true);
        }        
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        if (validElements != null) {
            Epics.closeChannel(validElements);
            validElements = null;
        }
    }

    @Override
    public int getSize() {
        return (isInitialized()) ? size : configSize;
    }
    
    @Override
    public void setSize(int size) throws IOException {
        boolean keepToValid = (size == KEEP_TO_VALID);
        if (size == 0) {
            //Error creating array of size 0
            size = getMaximumSize();
        } else if (size == SIZE_MAX) {
            size = getMaximumSize();
        } else if (size < 0) {
            size = getValidElements();
        }         
        size = Math.min(size, getMaximumSize());
        size = Math.max(size, 1);   
        if  (size != this.size) {
            this.size = size;        
            if (channel != null) {
                try {
                    channel.setSize(size);
                } catch (Exception ex) {
                    throw new DeviceException(ex);
                }
            }
        }
        setKeepToValidElements(keepToValid);
    }

    public void setSizeToValidElements() throws IOException, InterruptedException {
        setSize(SIZE_VALID);
    }
    
    PropertyChangeListener validElementsChangeListener = (PropertyChangeEvent pce) -> {
        if (pce.getPropertyName().equals(Channel.PROPERTY_VALUE)) {
            try{
                setSize(KEEP_TO_VALID);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }                
        }
    };
    
    public void setKeepToValidElements(boolean value) throws IOException {        
        try {
            if (value!=keepToValidElements){                
                keepToValidElements = value;
                 if (validElements!=null){
                    validElements.setMonitored(value);
                    if (value){
                        validElements.addPropertyChangeListener(validElementsChangeListener);
                        setSize(KEEP_TO_VALID);
                    } else {
                        validElements.removePropertyChangeListener(validElementsChangeListener);
                    }
                }
            }                     
        } catch (Exception ex) {
            throw new DeviceException(ex);
        }
    }    
    
    public boolean getKeepToValidElements(){
        return keepToValidElements;
    }

    public int getValidElements() throws IOException {
        try {
            if (validElements==null){
                return getMaximumSize();
            }
            return (Integer) validElements.getValue(false);
        } catch (Exception ex) {
            throw new DeviceException(ex);
        }
    }

    @Override
    public EpicsRegisterArrayConfig getConfig() {
        return (EpicsRegisterArrayConfig) super.getConfig();
    }

    abstract public int getComponentSize();
}
