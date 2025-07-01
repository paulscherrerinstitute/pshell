package ch.psi.pshell.devices;

import ch.psi.pshell.device.GenericDevice;

/**
 * The listener interface for receiving device pool events.
 */
public interface DevicePoolListener {

    default void onDeviceAdded(GenericDevice dev) {}

    default void onDeviceRemoved(GenericDevice dev) {}
    
    default void onClosing() {}
    
    default void onInitialized() {}
}
