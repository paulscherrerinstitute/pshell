package ch.psi.pshell.device;

/**
 * The listener interface for receiving motor events.
 */
public interface MotorListener extends ReadbackDeviceListener {

    default void onSpeedChanged(Motor device, Double value) {}

    default void onMotorStatusChanged(Motor device, MotorStatus value) {}
}
