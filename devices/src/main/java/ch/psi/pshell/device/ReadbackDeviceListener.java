package ch.psi.pshell.device;

/**
 * The listener interface for receiving readback device events.
 */
public interface ReadbackDeviceListener extends DeviceListener {

    default void onReadbackChanged(Device device, Object value) {}
}
