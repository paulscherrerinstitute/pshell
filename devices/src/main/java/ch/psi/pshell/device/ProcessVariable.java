package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable.DoubleType;
import java.io.IOException;

/**
 * A Register with metadata: units, range, deadband, scale and offset.
 * Metadata may be static or persisted in configuration.
 */
public interface ProcessVariable extends ReadonlyProcessVariable, Register.RegisterNumber<Double>, ContinuousPositionable, DoubleType {

    @Override
    public ProcessVariableConfig getConfig();

    public double getMinValue();

    public double getMaxValue();
    
    @Override
    default Double getPosition() throws IOException, InterruptedException{
        return read();
    }
    
    @Override
    default public double getDeadband() {   
        return ReadonlyProcessVariable.super.getDeadband();
    }
}
