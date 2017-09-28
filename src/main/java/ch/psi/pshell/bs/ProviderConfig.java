package ch.psi.pshell.bs;

import ch.psi.pshell.device.DeviceConfig;

/**
 * Configuration for Provider objects.
 */
public class ProviderConfig extends DeviceConfig {

    public enum SocketType {
        SUB,
        PULL,
        DEFAULT
    }

    public SocketType socketType = SocketType.DEFAULT;
    public boolean keepListeningOnStop = false;
    public boolean parallelHandlerProcessing = true;
    public boolean disableCompression = false;
    public boolean dropIncomplete = false;
}
