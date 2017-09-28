package ch.psi.pshell.modbus;

import ch.psi.pshell.device.Register.RegisterArray;
import ch.psi.pshell.device.RegisterBase;
import java.io.IOException;

/**
 * Array register classes wrapping sequence of AOs in a master.
 */
public class AnalogOutputArray extends RegisterBase<int[]> implements RegisterArray<int[]> {

    final int index;
    int size;

    public AnalogOutputArray(String name, ModbusDevice master, int index, int size) {
        super(name);
        setParent(master);
        this.index = index;
        this.size = size;
    }

    @Override
    protected int[] doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readRegisters(index + ((ModbusDevice) getParent()).getConfig().offsetReadAnalogOutput, size);
    }

    @Override
    protected void doWrite(int[] value) throws IOException, InterruptedException {
        if ((value == null) || (value.length != getSize())) {
            throw new IOException("Invalid array size");
        }
        ((ModbusDevice) getParent()).writeRegisters(index + ((ModbusDevice) getParent()).getConfig().offsetWriteAnalogOutput, value);
    }

    @Override
    public void setSize(int size) throws IOException {
        this.size = size;
    }

    @Override
    public int getSize() {
        return size;
    }
}
