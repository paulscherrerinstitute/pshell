package ch.psi.pshell.modbus;

import ch.psi.pshell.device.Readable.BooleanType;
import ch.psi.pshell.device.ReadonlyRegister;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import java.io.IOException;

/**
 * Scalar register classes wrapping single DI in a master.
 */
public class DigitalInput extends ReadonlyRegisterBase<Boolean> implements BooleanType {

    final int index;

    public DigitalInput(String name, ModbusDevice master, int index) {
        super(name);
        setParent(master);
        this.index = index;
    }

    @Override
    protected Boolean doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readInputDiscrete(index + ((ModbusDevice) getParent()).getConfig().offsetReadDigitalInput);
    }

}
