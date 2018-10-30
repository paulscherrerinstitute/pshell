package ch.psi.pshell.modbus;

import ch.psi.pshell.device.Readable.BooleanType;
import ch.psi.pshell.device.RegisterBase;
import java.io.IOException;

/**
 * Scalar register classes wrapping single DO in a master.
 */
public class DigitalOutput extends RegisterBase<Boolean> implements  BooleanType {

    final int index;

    public DigitalOutput(String name, ModbusDevice master, int index) {
        super(name);
        setParent(master);
        this.index = index;
    }

    @Override
    protected Boolean doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readCoil(index + ((ModbusDevice) getParent()).getConfig().offsetReadDigitalOutput);
    }

    @Override
    protected void doWrite(Boolean value) throws IOException, InterruptedException {
        ((ModbusDevice) getParent()).writeCoil(index + ((ModbusDevice) getParent()).getConfig().offsetWriteDigitalOutput, value);
    }

}
