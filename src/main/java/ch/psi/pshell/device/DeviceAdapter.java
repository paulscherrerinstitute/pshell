package ch.psi.pshell.device;

import ch.psi.utils.State;

/**
 * A convenience abstract adapter class for receiving device events, with empty methods. This
 * Adapter class only exists because of Jython bug extending default methods. Python scripts cannot
 * extend the Listener directly.
 */
//TODO: When Jython fixes bug with extending default methods,  remove 
//DeviceAdapter, ReadbackDeviceAdapter, MotorAdapter and ContextAdapter.
//Jython throws "Illegal use of nonvirtual function call" when trying to override a default method.
public abstract class DeviceAdapter implements DeviceListener {

    @Override
    public void onStateChanged(Device device, State state, State former) {
    }

    @Override
    public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
    }

    @Override
    public void onValueChanged(Device device, Object value, Object former) {
    }

    @Override
    public void onValueChanging(Device device, Object value, Object former) throws Exception {

    }
}
