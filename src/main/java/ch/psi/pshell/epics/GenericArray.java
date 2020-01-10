package ch.psi.pshell.epics;

import ch.psi.pshell.device.Camera.DataType;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Register;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps an EPICS PV as register array with variable type.
 */
public class GenericArray extends RegisterBase implements Register.RegisterArray {

    final String channelName;        
    final String typeName;    
    final boolean timestamped;
    final InvalidValueAction invalidAction;
    int size = -1;
    RegisterArray register;
    Class type;
    boolean autoResolveType = true;    
    
    

    public GenericArray(String name, final String channelName) {
         this(name, channelName, -1);
    }

    public GenericArray(String name, final String channelName, final int size) {
        this(name, channelName, size, true);
    }
    
    public GenericArray(String name, final String channelName, final int size, boolean timestamped) {
        this(name, channelName, size, timestamped, timestamped ? Epics.getDefaultInvalidValueAction(): null);
    }

    public GenericArray(String name, final String channelName, final int size, boolean timestamped, InvalidValueAction invalidAction) {
        this(name, channelName, size, timestamped, invalidAction, null);
    }
    
    public GenericArray(String name, final String channelName, final int size, boolean timestamped, InvalidValueAction invalidAction, final String className) {
        super(name);
        this.channelName = channelName;
        this.setTrackChildren(true);
        this.typeName = className;
        this.timestamped = timestamped;
        this.invalidAction = invalidAction;
        this.size = size;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public RegisterArray getRegister(){
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
            setRegister((EpicsRegisterArray) Epics.newChannelDevice(name, channelName, type, timestamped, -1, -1, invalidAction));
            if (initialized) {
                register.initialize();
            }
            if (size != -1) {
                register.setSize(size);
            }
        }
    }

    public void resolveType() throws IOException, InterruptedException {
        try {
            Object val = Epics.get(channelName, null, 1);
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
                setType(DataType.valueOf(typeName).getArrayType());
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
            if (size != -1) {
                register.setSize(size);
            }
        }
    }

    void setRegister(EpicsRegisterArray register) throws IOException, IOException, InterruptedException {
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
        if (this.register == null) {
            throw new IOException("Type not set");
        }
        return register.read();
    }

    @Override
    protected void doWrite(Object value) throws IOException, InterruptedException {
    }

    @Override
    public int getMaximumSize() {
        if (register == null) {
            return -1;
        }
        return register.getMaximumSize();
    }

    @Override
    public int getSize() {
        if (register == null) {
            return -1;
        }
        return register.getSize();
    }

    @Override
    public void setSize(int size) throws IOException {
        this.size = size;
        if (register != null) {
            register.setSize(size);
        }
    }
    
    public void setSizeToValidElements() throws IOException, InterruptedException{
        setSize(getValidElemets());
    }
    
    
    public int getValidElemets() throws IOException, InterruptedException{
        if ((register != null) && (register instanceof EpicsRegisterArray)){
            return ((EpicsRegisterArray)register).getValidElemets();
        }
        return -1;
    }    
}
