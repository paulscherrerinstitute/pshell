package ch.psi.pshell.serial;

import ch.psi.pshell.serial.SerialPortDeviceConfig.DataBits;
import ch.psi.pshell.serial.SerialPortDeviceConfig.Parity;
import ch.psi.pshell.serial.SerialPortDeviceConfig.StopBits;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * Communication through a PC serial port. The port name and parameters are defined in the device
 * configuration
 */
public class SerialPortDevice extends StreamDevice {

    String port;

    int baudRate;
    DataBits dataBits;
    StopBits stopBits;
    Parity parity;

    SerialPort serialPort;

    //Constructors
    public SerialPortDevice(String name) {
        super(name, new SerialPortDeviceConfig());
    }

    @Override
    public SerialPortDeviceConfig getConfig() {
        return (SerialPortDeviceConfig) super.getConfig();
    }

    public SerialPortDevice(String name, String port, int baudRate, DataBits dataBits, StopBits stopBits, Parity parity) {
        super(name);
        this.port = port;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        if (getConfig() != null) {
            this.port = getConfig().port;
            this.baudRate = getConfig().baudRate;
            this.dataBits = getConfig().dataBits;
            this.stopBits = getConfig().stopBits;
            this.parity = getConfig().parity;
        }
        closePort();
        super.doInitialize();
        if (isSimulated()) {
            return;
        }
        try {
            CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(port);
            CommPort port = ((CommPortIdentifier) portId).open(this.getClass().getSimpleName(), 2000);
            if (!(port instanceof SerialPort)) {
                throw new DeviceException("No such port: " + port);
            }
            serialPort = (SerialPort) port;
            setStreams(serialPort.getInputStream(), serialPort.getOutputStream());
            serialPort.addEventListener((SerialPortEvent event) -> {
                if (getMode() == Mode.HalfDuplex) {
                    return;
                }
                try {
                    switch (event.getEventType()) {
                        case SerialPortEvent.DATA_AVAILABLE:
                            int rx;
                            while ((rx = readByte()) >= 0) {
                                onByte(rx);
                            }
                    }
                } catch (Exception e) {
                    onByte(-1);
                }

            });
            serialPort.notifyOnDataAvailable(getMode() == Mode.FullDuplex);
            serialPort.setSerialPortParams(baudRate, dataBits.get(), stopBits.get(), parity.get());
        } catch (Exception ex) {
            close();
            throw new DeviceException(ex);
        }
    }
    
    void closePort(){
        if (isSimulated()) {
            return;
        }
        if (serialPort != null) {
            try {
                serialPort.notifyOnDataAvailable(false);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
            try {
                serialPort.removeEventListener();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
            try {
                serialPort.enableReceiveTimeout(0);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
            try {
                serialPort.close();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
            serialPort = null;
            System.gc();
        }        
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        closePort();
    }

    public static ArrayList<String> getPortList() {
        ArrayList<String> ret = new ArrayList<>();
        try {
            Enumeration ports = CommPortIdentifier.getPortIdentifiers();
            while (ports.hasMoreElements()) {
                CommPortIdentifier port_id = (CommPortIdentifier) ports.nextElement();
                if (port_id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                    ret.add(port_id.getName());
                }
            }
        } catch (Throwable t) {
        }
        return ret;
    }
}
