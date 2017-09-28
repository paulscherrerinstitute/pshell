package ch.psi.pshell.serial;

import ch.psi.pshell.device.DeviceConfig;

/**
 *
 */
public class SocketDeviceConfig extends DeviceConfig {

    public String address;
    public int port;
    public int timeout = 3000;
}
