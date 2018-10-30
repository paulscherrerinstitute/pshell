package ch.psi.pshell.modbus;

import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import java.io.IOException;
import ch.psi.pshell.device.Readable.IntegerType;

/**
 * Array register classes wrapping sequence of AIs in a master.
 */
public class AnalogInputArray extends ReadonlyRegisterBase<int[]> implements ReadonlyRegisterArray<int[]> , IntegerType{

    final int index;
    int size;

    public AnalogInputArray(String name, ModbusDevice master, int index, int size) {
        super(name);
        setParent(master);
        this.index = index;
        this.size = size;
    }

    @Override
    protected int[] doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readInputRegisters(index + ((ModbusDevice) getParent()).getConfig().offsetReadAnalogInput, size);
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
