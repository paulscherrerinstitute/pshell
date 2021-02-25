package ch.psi.pshell.epics;

import ch.psi.pshell.device.Register;
import java.io.IOException;

/**
 * Wraps an EPICS PV as register array with variable type.
 */
public class GenericArray extends GenericChannel implements Register.RegisterArray {                    
    RegisterArray register;
    
    public GenericArray(String name, final String channelName) {
         this(name, channelName, SIZE_MAX);
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
        super(name, channelName, timestamped, invalidAction, className, size);
        defaultValue = byte[].class;
    }
  
    @Override
    public int getMaximumSize() {
        if (register == null) {
            return UNDEFINED;
        }
        return register.getMaximumSize();
    }

    @Override
    public int getSize() {
        if (register == null) {
            return UNDEFINED;
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
        setSize(SIZE_VALID);
    }
    
    
    public int getValidElements() throws IOException, InterruptedException{
        if ((register != null) && (register instanceof EpicsRegisterArray)){
            return ((EpicsRegisterArray)register).getValidElements();
        }
        return SIZE_MAX;
    }    
    
    public void setKeepToValidElements(boolean value) throws IOException {   
        if ((register != null) && (register instanceof EpicsRegisterArray)){
            ((EpicsRegisterArray)register).setKeepToValidElements(value);
        }        
    }    
    
    public boolean getKeepToValidElements(){    
        if ((register != null) && (register instanceof EpicsRegisterArray)){
            return ((EpicsRegisterArray)register).getKeepToValidElements();
        }        
        return false;
    }
}
