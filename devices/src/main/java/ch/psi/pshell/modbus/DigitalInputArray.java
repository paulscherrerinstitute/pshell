package ch.psi.pshell.modbus;

import ch.psi.pshell.device.Readable.BooleanType;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import java.io.IOException;

/**
 * Array register classes wrapping sequence of DIs in a master.
 */
public class DigitalInputArray extends ReadonlyRegisterBase<boolean[]> implements ReadonlyRegisterArray<boolean[]>, BooleanType {

    final int index;
    int size;

    public DigitalInputArray(String name, ModbusDevice master, int index, int size) {
        super(name);
        setParent(master);
        this.index = index;
        this.size = size;
    }

    @Override
    protected boolean[] doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readInputDiscretes(index + ((ModbusDevice) getParent()).getConfig().offsetReadDigitalInput, size);
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
