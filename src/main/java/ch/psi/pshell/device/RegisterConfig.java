package ch.psi.pshell.device;

/**
 * Entity class holding the configuration of Register devices.
 */
public class RegisterConfig extends DeviceConfig {

    public int precision = Record.UNDEFINED_PRECISION;      //Number of decimals
    public String description;
    
    public boolean hasDefinedDescription() {
        return isStringDefined(description);
    }      
    
    public boolean isUndefined(){
        return !hasDefinedDescription();
    }
}
