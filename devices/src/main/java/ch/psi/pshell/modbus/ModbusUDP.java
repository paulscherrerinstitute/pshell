package ch.psi.pshell.modbus;

import java.io.IOException;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.facade.ModbusUDPMaster;

/**
 * Implements a Modbus UDP device controller.
 */
public class ModbusUDP extends ModbusDevice {

    String address = null;

    public ModbusUDP(String name, String address) {
        super(name);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    static class ModbusMasterUDP extends ModbusUDPMaster implements ModbusMaster {

        ModbusMasterUDP(String addr) {
            super(addr);
        }

        ModbusMasterUDP(String addr, int port) {
            super(addr, port);
        }
    }

    @Override
    protected synchronized void doInitialize() throws IOException, InterruptedException {
        doClose(); //If do not want to close children, call closeMaster() instead.
        if ((address == null) || (address.isEmpty())) {
            throw new DeviceException("Address is not defined");
        }

        if (address.contains(":")) {
            try {
                String[] tokens = address.split(":");
                String ip = tokens[0].trim();
                int port = Integer.valueOf(tokens[1]);
                master = new ModbusMasterUDP(ip, port);
            } catch (Exception ex) {
                throw new DeviceException("Invalid address: " + address);
            }

        } else {
            int port = Modbus.DEFAULT_PORT;
            master = new ModbusMasterUDP(address);
        }
        super.doInitialize();
    }

}
