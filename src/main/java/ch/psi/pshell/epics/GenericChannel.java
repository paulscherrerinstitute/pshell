package ch.psi.pshell.epics;

import ch.psi.pshell.device.Camera;
import ch.psi.pshell.device.Device;
import static ch.psi.pshell.device.Record.UNDEFINED_PRECISION;
import ch.psi.pshell.device.Register;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps an EPICS PV scalar with variable type.
 */
public class GenericChannel extends RegisterBase {

    final String channelName;        
    final String typeName;    
    final boolean timestamped;
    final InvalidValueAction invalidAction;
    Register register;
    Class type;
    boolean autoResolveType = true;    
    int size=1;
    Object defaultValue;

    public GenericChannel(String name, final String channelName) {
         this(name, channelName, true);
    }
    
    public GenericChannel(String name, final String channelName, boolean timestamped) {
        this(name, channelName, timestamped, timestamped ? Epics.getDefaultInvalidValueAction(): null);
    }

    public GenericChannel(String name, final String channelName, boolean timestamped, InvalidValueAction invalidAction) {
        this(name, channelName, timestamped, invalidAction, null);
    }
    
    public GenericChannel(String name, final String channelName, boolean timestamped, InvalidValueAction invalidAction, final String className) {
        this(name, channelName, timestamped, invalidAction, className, 1);
    }
    
    protected GenericChannel(String name, final String channelName, boolean timestamped, InvalidValueAction invalidAction, final String className, int size) {
        super(name);
        this.channelName = channelName;
        this.setTrackChildren(true);
        this.typeName = className;
        this.timestamped = timestamped;
        this.invalidAction = invalidAction;
        this.size = size;
        this.defaultValue = Double.class;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public Register getRegister(){
        return register;
    } 

    public boolean getAutoResolveType() {
        return autoResolveType;
    }

    public void setAutoResolveType(boolean value) {
        autoResolveType = value;
    }

    public void setType(Class type) throws IOException, InterruptedException {
        if ((type != null) && (type != this.type)) {
            boolean initialized = isInitialized();
            closeRegister();
            String name = getName() + " channel";
            setRegister(Epics.newChannelDevice(name, channelName, type, timestamped, UNDEFINED_PRECISION, SIZE_MAX, invalidAction));
            if (initialized) {
                register.initialize();
            }
            if (register instanceof RegisterArray){
                ((RegisterArray)register).setSize(size);
            }
        }
    }
    public void resolveType() throws IOException, InterruptedException {
        try {
            Object val = isSimulated() ?  defaultValue : Epics.get(channelName, null, 1);
            setType(val.getClass());
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void closeRegister() {
        if (register != null) {
            try {
                register.close();
            } catch (Exception ex) {
                Logger.getLogger(AreaDetector.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        type = null;
        register = null;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        if (typeName != null) {
            try {
                setType(Camera.DataType.valueOf(typeName).getArrayType());
            } catch (Exception ex) {
                try {
                    Class cls = Epics.getChannelType(typeName);
                    if (cls==null){
                        throw new Exception("Invalid array type: " + typeName);
                    }
                    setType(cls);
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }
        } else if (autoResolveType) {
            resolveType();
        }
        if (register != null) {
            if (!register.isInitialized()) {
                register.initialize();
            }
            if (register instanceof RegisterArray){
                ((RegisterArray)register).setSize(size);
            }
        }
    }

    void setRegister(EpicsRegister register) throws IOException, IOException, InterruptedException {
        if (isSimulated()) {
            register.setSimulated();
        }
        this.register = register;
        this.type = register.getType();
        this.setChildren(new Device[]{register});
    }

    @Override
    protected void onChildValueChange(Device child, Object value, Object former) {
        if (child == register) {
            setCache(value);
        }
    }

    @Override
    protected void onChildStateChange(Device child, State state, State former) {
        if (child == register) {
            setState(state);
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        if (this.register != null) {
            register.update();
        }
    }

    @Override
    protected Object doRead() throws IOException, InterruptedException {
        assertTypeSet();
        return register.read();
    }

    @Override
    protected void doWrite(Object value) throws IOException, InterruptedException {
        assertTypeSet();
        register.write(value);
    }
    
    void assertTypeSet() throws IOException{
        if (this.register == null) {
            throw new IOException("Type not set");
        }        
    }
}
