package ch.psi.pshell.device;

/**
 * A Readonly Register with metadata: units, range, scale and offset. Metadata
 * may be static or persisted in configuration.
 */
public interface ReadonlyProcessVariable extends ReadonlyRegister<Double> {

    @Override
    public ReadonlyProcessVariableConfig getConfig();

    public double getOffset();

    public double getScale();

    public int getSignBit();

    public String getUnit();
   

}
