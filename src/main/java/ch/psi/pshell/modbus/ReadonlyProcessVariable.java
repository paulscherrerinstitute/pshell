package ch.psi.pshell.modbus;

import ch.psi.pshell.device.ReadonlyProcessVariableBase;
import ch.psi.pshell.device.ReadonlyProcessVariableConfig;
import java.io.IOException;

/**
 *
 */
public class ReadonlyProcessVariable extends ReadonlyProcessVariableBase {

    final int index;

    public ReadonlyProcessVariable(String name, ModbusDevice master, int index) {
        super(name, new ReadonlyProcessVariableConfig());
        setParent(master);
        this.index = index;
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        return (double) ((ModbusDevice) getParent()).readInputRegister(index + ((ModbusDevice) getParent()).getConfig().offsetReadAnalogInput);
    }
}
