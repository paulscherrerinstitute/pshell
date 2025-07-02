package ch.psi.pshell.scan;

import ch.psi.pshell.device.DeviceBase;

/**
 * 
 */
public abstract class Otf extends DeviceBase {    
    public Otf(String name){
        super(name);
    }
    abstract public void start();
    abstract public void abort();    
}
