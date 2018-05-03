package ch.psi.pshell.device;

/**
 * Entity class holding the configuration of ProcessVariable devices.
 */
public class ProcessVariableConfig extends ReadonlyProcessVariableConfig {

    public double resolution = Double.NaN;  //In-position deadband: if undefined equals to Math.pow(10.0, -precision);
    public double minValue = Double.NaN;
    public double maxValue = Double.NaN;

}
