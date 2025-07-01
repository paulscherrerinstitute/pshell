package ch.psi.pshell.modbus;

import ch.psi.pshell.device.DeviceConfig;

/**
 * Configuration of a Modbus device: contains offsets to different read blocks (added to IO
 * indexes).
 */
public class ModbusDeviceConfig extends DeviceConfig {

    public int offsetReadDigitalOutput;   //FC1    
    public int offsetReadDigitalInput;   //FC2
    public int offsetWriteDigitalOutput;  //FC5 and FC15        
    public int offsetReadAnalogOutput;   //FC3
    public int offsetReadAnalogInput;   //FC4    
    public int offsetWriteAnalogOutput;  //FC6 and FC16    
    public int timeout = 1000;      //TODO
}
