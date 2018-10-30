package ch.psi.pshell.modbus;

import ch.psi.pshell.device.RegisterBase;
import java.io.IOException;
import ch.psi.pshell.device.Readable.IntegerType;

/**
 *
 */
public class Register extends RegisterBase<Integer> implements ch.psi.pshell.device.Register.RegisterNumber<Integer>, IntegerType {

    final int index;

    public Register(String name, ModbusDevice master, int index) {
        super(name);
        setParent(master);
        this.index = index;
    }

    @Override
    protected Integer doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readRegister(index);
    }

    @Override
    protected void doWrite(Integer value) throws IOException, InterruptedException {
        ((ModbusDevice) getParent()).writeRegister(index, value);
    }

}
