package ch.psi.pshell.modbus;

import net.wimpi.modbus.facade.ModbusSerialMaster;
import net.wimpi.modbus.util.SerialParameters;
import java.io.IOException;

/**
 * Implements a Modbus Serial device controller.
 */
public class ModbusSerial extends ModbusDevice {

    public static final int BROADCAST = 0;
    boolean mEcho = false;

    public ModbusSerial(String name) {
        super(name, new ModbusSerialConfig());
    }

    @Override
    public ModbusSerialConfig getConfig() {
        return (ModbusSerialConfig) super.getConfig();
    }

    static class ModbusMasterSerial extends ModbusSerialMaster implements ModbusMaster {

        ModbusMasterSerial(SerialParameters pars) {
            super(pars);
        }
    }

    @Override
    protected synchronized void doInitialize() throws IOException, InterruptedException {
        ModbusSerialConfig cfg = getConfig();
        SerialParameters pars = new SerialParameters(cfg.port, cfg.baudRate, cfg.flowControlIn.get(), cfg.flowControlOut.get(), cfg.dataBits.get(), cfg.stopBits.get(), cfg.parity.get(), cfg.echo, cfg.timeout);
        pars.setEncoding(String.valueOf(cfg.encoding));
        setUnitId(getConfig().unitId);
        master = new ModbusMasterSerial(pars);

        //TODO: define master timeout
        super.doInitialize();
    }

}
