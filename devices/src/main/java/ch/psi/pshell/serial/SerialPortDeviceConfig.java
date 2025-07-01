package ch.psi.pshell.serial;

import ch.psi.pshell.device.DeviceConfig;
import gnu.io.SerialPort;

/**
 *
 */
public class SerialPortDeviceConfig extends DeviceConfig {

    public enum DataBits {

        DB_5,
        DB_6,
        DB_7,
        DB_8;

        public int get() {
            switch (this) {
                case DB_5:
                    return SerialPort.DATABITS_5;
                case DB_6:
                    return SerialPort.DATABITS_6;
                case DB_7:
                    return SerialPort.DATABITS_7;
            }
            return SerialPort.DATABITS_8;
        }
    }

    public enum StopBits {

        SB_1,
        SB_1_5,
        SB_2;

        public int get() {
            switch (this) {
                case SB_1_5:
                    return SerialPort.STOPBITS_1_5;
                case SB_2:
                    return SerialPort.STOPBITS_2;
            }
            return SerialPort.STOPBITS_1;
        }
    }

    public enum Parity {

        None,
        Odd,
        Even,
        Mark,
        Space;

        public int get() {
            switch (this) {
                case Odd:
                    return SerialPort.PARITY_ODD;
                case Even:
                    return SerialPort.PARITY_EVEN;
                case Mark:
                    return SerialPort.PARITY_MARK;
                case Space:
                    return SerialPort.PARITY_SPACE;
            }
            return SerialPort.PARITY_NONE;
        }
    }

    public enum FlowControl {

        None,
        RtsCtsIn,
        RtsCtsOut,
        XonXoffIn,
        XonXoffOut;

        public int get() {
            switch (this) {
                case RtsCtsIn:
                    return SerialPort.FLOWCONTROL_RTSCTS_IN;
                case RtsCtsOut:
                    return SerialPort.FLOWCONTROL_RTSCTS_OUT;
                case XonXoffIn:
                    return SerialPort.FLOWCONTROL_XONXOFF_IN;
                case XonXoffOut:
                    return SerialPort.FLOWCONTROL_XONXOFF_OUT;
            }
            return SerialPort.FLOWCONTROL_NONE;
        }
    }

    public String port;
    public int baudRate = 9600;
    public DataBits dataBits = DataBits.DB_8;
    public StopBits stopBits = StopBits.SB_1;
    public Parity parity = Parity.None;
}
