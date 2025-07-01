package ch.psi.pshell.device;

/**
 * Entity class holding the configuration of ReadonlyProcessVariable devices.
 */
public class ReadonlyProcessVariableConfig extends RegisterConfig {

    public double offset = 0;
    public double scale = 1;
    public String unit;
    public int sign_bit;

    public boolean hasDefinedUnit() {
        return isStringDefined(unit);
    }
      
    public double applySign(Double value) {
        if (sign_bit <= 0) {
            return value;
        }
        long l = value.longValue();
        long mask = Long.rotateLeft(1, sign_bit);
        if ((l & mask) > 0) {
            return l - (mask << 1);
        }
        return l;
    }
    
    @Override
    public boolean isUndefined(){
        return !hasDefinedUnit();
    }    
      
}
