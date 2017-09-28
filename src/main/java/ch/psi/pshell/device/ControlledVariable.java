package ch.psi.pshell.device;

/**
 * ProcessVariable with readback (implementing ReadbackDevice) and position checking (implementing
 * Positionable)
 */
public interface ControlledVariable extends ProcessVariable, Positionable<Double>, ReadbackDevice<Double> {
}
