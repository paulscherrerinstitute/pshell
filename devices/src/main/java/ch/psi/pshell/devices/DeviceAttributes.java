package ch.psi.pshell.devices;

import ch.psi.pshell.device.AccessType;
import ch.psi.pshell.utils.Arr;

/**
 * Entity class holding the configuration attributes of a device in the
 * global device pool.
 */
public class DeviceAttributes {

    String name;
    Boolean enabled;
    String className;
    String[] arguments;
    Boolean simulated;
    AccessType accessType;
    Boolean monitored;
    Integer polling;

    public String[] getArguments() {
        //Remove  "" from strings
        return arguments;
    }

    public String[] getParameters() {
        return ((arguments == null) || (arguments.length == 0)) ? null : Arr.remove(arguments, 0);
    }

    public Boolean isSimulated() {
        return simulated;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public Boolean isMonitored() {
        return monitored;
    }

    public Integer getPolling() {
        return polling;
    }

    public String getName() {
        return name;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public String getClassName() {
        return className;
    }
    
}
