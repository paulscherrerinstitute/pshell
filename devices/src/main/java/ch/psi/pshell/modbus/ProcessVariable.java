package ch.psi.pshell.modbus;

import ch.psi.pshell.device.ProcessVariableBase;
import ch.psi.pshell.device.ProcessVariableConfig;
import java.io.IOException;

/**
 *
 */
public class ProcessVariable extends ProcessVariableBase {

    final int index;

    public ProcessVariable(String name, ModbusDevice master, int index) {
        super(name, new ProcessVariableConfig());
        setParent(master);
        this.index = index;
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        return (double) ((ModbusDevice) getParent()).readRegister(index + ((ModbusDevice) getParent()).getConfig().offsetReadAnalogOutput);
    }

    @Override
    protected void doWrite(Double value) throws IOException, InterruptedException {
        ((ModbusDevice) getParent()).writeRegister(index + ((ModbusDevice) getParent()).getConfig().offsetWriteAnalogOutput, value.intValue());
    }

}
