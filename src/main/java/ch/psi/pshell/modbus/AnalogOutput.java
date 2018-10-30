package ch.psi.pshell.modbus;

import ch.psi.pshell.device.Register.RegisterNumber;
import ch.psi.pshell.device.RegisterBase;
import java.io.IOException;
import ch.psi.pshell.device.Readable.IntegerType;

/**
 * Scalar register classes wrapping single AO in a master.
 */
public class AnalogOutput extends RegisterBase<Integer> implements RegisterNumber<Integer>, IntegerType {

    final int index;

    public AnalogOutput(String name, ModbusDevice master, int index) {
        super(name);
        setParent(master);
        this.index = index;
    }

    @Override
    protected Integer doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readRegister(index + ((ModbusDevice) getParent()).getConfig().offsetReadAnalogOutput);
    }

    @Override
    protected void doWrite(Integer value) throws IOException, InterruptedException {
        ((ModbusDevice) getParent()).writeRegister(index + ((ModbusDevice) getParent()).getConfig().offsetWriteAnalogOutput, value);
    }

}
