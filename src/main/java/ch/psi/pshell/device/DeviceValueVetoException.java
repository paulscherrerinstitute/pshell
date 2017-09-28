package ch.psi.pshell.device;

import ch.psi.utils.Str;

/**
 * This exception means a given write command is not allowed at the time it was called. It is thrown
 * by a device if a write command violates an Interlock containing that device, or if any device
 * listener throws and exception on the onValueChanging callback.
 */
public class DeviceValueVetoException extends Exception {

    public DeviceValueVetoException(Device dev, Object value) {
        super("Cannot set " + dev.getName() + " value to " + Str.toString(value, 10));
    }
}
