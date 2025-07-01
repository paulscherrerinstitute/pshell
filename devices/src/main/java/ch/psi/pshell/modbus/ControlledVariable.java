package ch.psi.pshell.modbus;

import ch.psi.pshell.device.ControlledVariableBase;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ProcessVariableConfig;
import java.io.IOException;

/**
 *
 */
public class ControlledVariable extends ControlledVariableBase {

    final int index;
    final ProcessVariable readback;

    public ControlledVariable(String name, ModbusDevice master, int index, int readbackIndex) {
        this(name, master, index, new ProcessVariable(name + " readback", master, readbackIndex));
    }

    public ControlledVariable(String name, ModbusDevice master, int index, ProcessVariable readback) {
        super(name, new ProcessVariableConfig());
        setParent(master);
        this.index = index;
        this.readback = readback;
        setChildren(new Device[]{readback});
        setTrackChildren(true);
        setReadback(readback);
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
