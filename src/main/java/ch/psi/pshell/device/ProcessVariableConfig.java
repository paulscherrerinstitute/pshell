package ch.psi.pshell.device;

/**
 * Entity class holding the configuration of ProcessVariable devices.
 */
public class ProcessVariableConfig extends RegisterConfig {

    public double offset = 0;
    public double scale = 1;
    public String unit;
    public double resolution = Double.NaN;  //In-position deadband: if undefined equals to Math.pow(10.0, -precision);
    public double minValue = Double.NaN;
    public double maxValue = Double.NaN;

    public boolean hasDefinedUnit() {
        return isStringDefined(unit);
    }
}
