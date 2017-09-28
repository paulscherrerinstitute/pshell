package ch.psi.pshell.modbus;

import ch.psi.pshell.device.Register.RegisterArray;
import ch.psi.pshell.device.RegisterBase;
import java.io.IOException;

/**
 * Array register classes wrapping sequence of DOs in a master.
 */
public class DigitalOutputArray extends RegisterBase<boolean[]> implements RegisterArray<boolean[]> {

    final int index;
    int size;

    public DigitalOutputArray(String name, ModbusDevice master, int index, int size) {
        super(name);
        setParent(master);
        this.index = index;
        this.size = size;
    }

    @Override
    protected boolean[] doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readCoils(index + ((ModbusDevice) getParent()).getConfig().offsetReadDigitalOutput, size);
    }

    @Override
    protected void doWrite(boolean[] value) throws IOException, InterruptedException {
        if ((value == null) || (value.length != getSize())) {
            throw new IOException("Invalid array size");
        }
        ((ModbusDevice) getParent()).writeCoils(index + ((ModbusDevice) getParent()).getConfig().offsetWriteDigitalOutput, value);
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
