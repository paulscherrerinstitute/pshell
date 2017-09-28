package ch.psi.pshell.device;

/**
 * A Register with metadata: units, range, resolution, scale and offset. Metadata may be static or
 * persisted in configuration.
 */
public interface ProcessVariable extends Register<Double> {

    @Override
    public ProcessVariableConfig getConfig();

    public double getOffset();

    public double getScale();

    /**
     * In-position deadband
     */
    public double getResolution();

    public String getUnit();

    public double getMinValue();

    public double getMaxValue();
}
