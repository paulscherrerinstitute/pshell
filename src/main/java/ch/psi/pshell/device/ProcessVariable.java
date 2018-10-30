package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.DoubleType;

/**
 * A Register with metadata: units, range, resolution, scale and offset.
 * Metadata may be static or persisted in configuration.
 */
public interface ProcessVariable extends Register<Double>, Resolved, DoubleType {

    @Override
    public ProcessVariableConfig getConfig();

    public double getOffset();

    public double getScale();
    
    public int getSignBit();

    public String getUnit();

    public double getMinValue();

    public double getMaxValue();
}
