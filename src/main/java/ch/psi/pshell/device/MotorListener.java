package ch.psi.pshell.device;

/**
 * The listener interface for receiving motor events.
 */
public interface MotorListener extends ReadbackDeviceListener {

    void onSpeedChanged(Motor device, Double value);

    void onMotorStatusChanged(Motor device, MotorStatus value);
}
