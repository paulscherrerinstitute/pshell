package ch.psi.pshell.device;

/**
 * Entity class holding a motor configuration.
 */
public class MotorConfig extends PositionerConfig {

    public double defaultSpeed = Double.NaN;
    public double minSpeed = Double.NaN;
    public double maxSpeed = Double.NaN;
    public int estbilizationDelay = 0;
    public int startRetries = 1;
    //backlash ?
    //acceleration ?
    //deadband ?
}
