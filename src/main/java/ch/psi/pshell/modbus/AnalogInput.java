package ch.psi.pshell.modbus;

import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterNumber;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import java.io.IOException;

/**
 * Scalar register classes wrapping single AI in a master.
 */
public class AnalogInput extends ReadonlyRegisterBase<Integer> implements ReadonlyRegisterNumber<Integer> {

    final int index;

    public AnalogInput(String name, ModbusDevice master, int index) {
        super(name);
        setParent(master);
        this.index = index;
    }

    @Override
    protected Integer doRead() throws IOException, InterruptedException {
        return ((ModbusDevice) getParent()).readInputRegister(index + ((ModbusDevice) getParent()).getConfig().offsetReadAnalogInput);
    }

}
