package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.DoubleType;

/**
 * A Readonly Register with metadata: units, range, scale and offset. Metadata
 * may be static or persisted in configuration.
 */
public interface ReadonlyProcessVariable extends ReadonlyRegister.ReadonlyRegisterNumber<Double>, DoubleType {

    @Override
    public ReadonlyProcessVariableConfig getConfig();

    public double getOffset();

    public double getScale();

    public int getSignBit();

    @Override
    public String getUnit();
   
    default public double getDeadband() {
        double precision = getPrecision();
        if (precision < 0) {
            return Math.pow(10.0, -6);
        }
        return Math.pow(10.0, -precision);
    }
    

}
