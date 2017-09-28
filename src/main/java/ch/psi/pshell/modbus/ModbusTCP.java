package ch.psi.pshell.modbus;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.facade.ModbusTCPMaster;
import java.io.IOException;

/**
 * Implements a Modbus TCP device controller.
 */
public class ModbusTCP extends ModbusDevice {

    String address = null;

    public ModbusTCP(String name, String address) {
        super(name);
        this.address = address;
    }

    static class ModbusMasterTCP extends ModbusTCPMaster implements ModbusMaster {

        ModbusMasterTCP(String addr) {
            super(addr);
        }

        ModbusMasterTCP(String addr, int port) {
            super(addr, port);
        }
    }

    public String getAddress() {
        return address;
    }

    @Override
    protected synchronized void doInitialize() throws IOException, InterruptedException {
        doClose();
        if ((address == null) || (address.isEmpty())) {
            throw new DeviceException("Address is not defined");
        }

        if (address.contains(":")) {
            try {
                String[] tokens = address.split(":");
                String ip = tokens[0].trim();
                int port = Integer.valueOf(tokens[1]);
                master = new ModbusMasterTCP(ip, port);
            } catch (Exception ex) {
                throw new DeviceException("Invalid address: " + address);
            }

        } else {
            int port = Modbus.DEFAULT_PORT;
            master = new ModbusMasterTCP(address);
        }

        //TODO: define master timeout
        super.doInitialize();
    }
}
