package ch.psi.pshell.device;

/**
 * A convenience abstract adapter class for receiving motor events, with empty methods. This Adapter
 * class only exists because of Jython bug extending default methods. Python scripts cannot extend
 * the Listener directly.
 */
public abstract class MotorAdapter extends ReadbackDeviceAdapter implements MotorListener {

    @Override
    public void onSpeedChanged(Motor device, Double value) {
    }

    @Override
    public void onMotorStatusChanged(Motor device, MotorStatus value) {
    }

}
