package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.DoubleType;

/**
 * A Readonly Register with metadata: units, range, scale and offset. Metadata
 * may be static or persisted in configuration.
 */
public interface ReadonlyProcessVariable extends ReadonlyRegister<Double>, DoubleType {

    @Override
    public ReadonlyProcessVariableConfig getConfig();

    public double getOffset();

    public double getScale();

    public int getSignBit();

    public String getUnit();
   

}
