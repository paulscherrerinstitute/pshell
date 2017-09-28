package ch.psi.pshell.device;

/**
 * A convenience abstract adapter class for receiving readback device events, with empty methods.
 * This Adapter class only exists because of Jython bug extending default methods. Python scripts
 * cannot extend the Listener directly.
 */
public abstract class ReadbackDeviceAdapter extends DeviceAdapter implements ReadbackDeviceListener {

    @Override
    public void onReadbackChanged(Device device, Object value) {

    }

}
