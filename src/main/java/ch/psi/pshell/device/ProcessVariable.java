package ch.psi.pshell.device;

/**
 * A Register with metadata: units, range, resolution, scale and offset.
 * Metadata may be static or persisted in configuration.
 */
public interface ProcessVariable extends Register<Double>, Resolved {

    @Override
    public ProcessVariableConfig getConfig();

    public double getOffset();

    public double getScale();

    public String getUnit();

    public double getMinValue();

    public double getMaxValue();
}
