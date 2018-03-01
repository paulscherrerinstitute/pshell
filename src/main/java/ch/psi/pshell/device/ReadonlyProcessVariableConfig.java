package ch.psi.pshell.device;

/**
 * Entity class holding the configuration of ProcessVariable devices.
 */
public class ReadonlyProcessVariableConfig extends RegisterConfig {

    public double offset = 0;
    public double scale = 1;
    public String unit;
    
    public boolean hasDefinedUnit() {
        return isStringDefined(unit);
    }
}
