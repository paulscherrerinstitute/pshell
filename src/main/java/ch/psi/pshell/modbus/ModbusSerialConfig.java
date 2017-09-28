package ch.psi.pshell.modbus;

import ch.psi.pshell.serial.SerialPortDeviceConfig.DataBits;
import ch.psi.pshell.serial.SerialPortDeviceConfig.FlowControl;
import ch.psi.pshell.serial.SerialPortDeviceConfig.Parity;
import ch.psi.pshell.serial.SerialPortDeviceConfig.StopBits;

/**
 * Configuration of ModbusSerialConfig includes port name and parameters.
 */
public class ModbusSerialConfig extends ModbusDeviceConfig {

    public enum Encoding {

        ascii,
        rtu,
        bin
    }

    public String port;
    public int baudRate = 9600;
    public DataBits dataBits = DataBits.DB_8;
    public StopBits stopBits = StopBits.SB_1;
    public Parity parity = Parity.None;
    public Encoding encoding = Encoding.rtu;
    public FlowControl flowControlIn = FlowControl.None;
    public FlowControl flowControlOut = FlowControl.None;
    boolean echo;

    public int unitId = 1;
}
