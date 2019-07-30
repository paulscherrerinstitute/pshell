package ch.psi.pshell.device;

import java.io.IOException;

/**
 * Positioner with speed, speed range, jog commands, and reference method.
 */
public interface Motor extends ControlledVariable, ControlledSpeedable, Positioner {

    public Register<Double> getVelocity();

    void reference() throws IOException, InterruptedException;

    MotorStatus takeStatus();

    MotorStatus readStatus() throws IOException, InterruptedException;

    void startJog(boolean positive) throws IOException, InterruptedException;
    
    void setCurrentPosition(double value) throws IOException, InterruptedException;

}
