package ch.psi.pshell.core;

import ch.psi.pshell.device.GenericDevice;

/**
 * The listener interface for receiving device pool events.
 */
public interface DevicePoolListener {

    void onDeviceAdded(GenericDevice dev);

    void onDeviceRemoved(GenericDevice dev);
}
