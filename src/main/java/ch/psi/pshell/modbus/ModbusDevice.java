package ch.psi.pshell.modbus;

import ch.psi.pshell.device.DeviceBase;
import ch.psi.utils.Chrono;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;
import net.wimpi.modbus.util.BitVector;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Interface to a Modbus master
 */
public abstract class ModbusDevice extends DeviceBase {

    //Set by implementation
    ModbusMaster master;
    boolean connected;

    protected ModbusDevice(String name) {
        this(name, new ModbusDeviceConfig());
    }

    protected ModbusDevice(String name, ModbusDeviceConfig config) {
        super(name, new ModbusDeviceConfig());
    }

    private static String getMessage(Throwable cause) {
        if (cause instanceof ModbusSlaveException) {
            ModbusSlaveException ex = (ModbusSlaveException) cause;
            switch (ex.getType()) {
                case 1:
                    return "Illegal function";
                case 2:
                    return "Illegal address";
                case 3:
                    return "Illegal value";
            }
        }
        return cause.getMessage();

    }

    public class ModbusDeviceException extends DeviceException {

        public ModbusDeviceException(String message) {
            super(message);
        }

        public ModbusDeviceException(Throwable cause) {
            super(ModbusDevice.this.getMessage(cause));
        }
    }

    @Override
    public ModbusDeviceConfig getConfig() {
        return (ModbusDeviceConfig) super.getConfig();
    }

    private int unitId;

    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int value) {
        unitId = value;
    }

    public int getTimeout() {
        return getConfig().timeout;
    }

    public boolean isConnected() {
        return connected;
    }

    private int latencty;

    public int getLatency() {
        return latencty;
    }

    public void setLatency(int value) {
        latencty = value;
    }

    private Chrono latencyChrono = new Chrono();

    synchronized Object execute(int index, Callable callable) throws IOException, InterruptedException {
        if (!isInitialized()) {
            throw new DeviceStateException();
        }
        if (index < 0) {
            throw new DeviceException("Invalid index: " + index);
        }

        if (!isConnected()) {
            //try reconnect
            getLogger().fine("Trying reconnection");
            disconnect();
            connect(); //doInitialize();
        }
        if (latencty > 0) {
            latencyChrono.waitTimeout(latencty);
        }

        try {
            return callable.call();
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                throw (InterruptedException) ex;
            }
            if (ex instanceof ModbusIOException) {
                connected = false;
            }
            throw new ModbusDeviceException(ex);
        } finally {
            latencyChrono = new Chrono();
        }

    }

    //FC1
    public boolean[] readCoils(int index, int count) throws IOException, InterruptedException {
        if (isSimulated()) {
            return new boolean[count];
        }
        return (boolean[]) execute(index, new Callable() {
            @Override
            public Object call() throws Exception {
                BitVector res = master.readCoils(unitId, index, count);
                boolean[] ret = new boolean[count];
                for (int i = 0; i < count; i++) {
                    ret[i] = res.getBit(i);
                }
                return ret;
            }
        });
    }

    public boolean readCoil(int index) throws IOException, InterruptedException {
        return readCoils(index, 1)[0];
    }

    //FC2
    public boolean[] readInputDiscretes(int index, int count) throws IOException, InterruptedException {
        if (isSimulated()) {
            return new boolean[count];
        }
        return (boolean[]) execute(index, new Callable() {
            @Override
            public Object call() throws Exception {
                BitVector res = master.readInputDiscretes(unitId, index, count);
                boolean[] ret = new boolean[count];
                for (int i = 0; i < count; i++) {
                    ret[i] = res.getBit(i);
                }
                return ret;
            }
        });
    }

    public boolean readInputDiscrete(int index) throws IOException, InterruptedException {
        return readInputDiscretes(index, 1)[0];
    }

    //FC3
    public int[] readRegisters(int index, int count) throws IOException, InterruptedException {
        if (isSimulated()) {
            return new int[count];
        }
        return (int[]) execute(index, new Callable() {
            @Override
            public Object call() throws Exception {
                Register[] res = master.readMultipleRegisters(unitId, index, count);
                int[] ret = new int[count];
                for (int i = 0; i < count; i++) {
                    ret[i] = res[i].getValue();
                }
                return ret;
            }
        });
    }

    public int readRegister(int index) throws IOException, InterruptedException {
        return readRegisters(index, 1)[0];
    }

    //FC4
    public int[] readInputRegisters(int index, int count) throws IOException, InterruptedException {
        if (isSimulated()) {
            return new int[count];
        }
        return (int[]) execute(index, new Callable() {
            @Override
            public Object call() throws Exception {
                InputRegister[] res = master.readInputRegisters(unitId, index, count);
                int[] ret = new int[count];
                for (int i = 0; i < count; i++) {
                    ret[i] = res[i].getValue();
                }
                return ret;
            }
        });
    }

    public int readInputRegister(int index) throws IOException, InterruptedException {
        return readInputRegisters(index, 1)[0];
    }

    //FC5
    public void writeCoil(int index, boolean state) throws IOException, InterruptedException {
        if (isSimulated()) {
            return;
        }
        execute(index, new Callable() {
            @Override
            public Object call() throws Exception {
                boolean ret = master.writeCoil(unitId, index, state);
                return null;
            }
        });
    }

    //FC6
    public void writeRegister(int index, int value) throws IOException, InterruptedException {
        if (isSimulated()) {
            return;
        }
        execute(index, new Callable() {
            @Override
            public Object call() throws Exception {
                SimpleRegister reg = new SimpleRegister(value);
                master.writeSingleRegister(unitId, index, reg);
                return null;
            }
        });
    }

    //FC15
    public void writeCoils(int index, boolean[] state) throws IOException, InterruptedException {
        if (isSimulated()) {
            return;
        }
        execute(index, new Callable() {
            @Override
            public Object call() throws Exception {
                BitVector coils = new BitVector(state.length);
                for (int i = 0; i < state.length; i++) {
                    coils.setBit(i, state[i]);
                }
                master.writeMultipleCoils(unitId, index, coils);
                return null;
            }
        });
    }

    //FC16
    public void writeRegisters(int index, int[] values) throws IOException, InterruptedException {
        if (isSimulated()) {
            return;
        }
        execute(index, new Callable() {
            @Override
            public Object call() throws Exception {
                SimpleRegister[] regs = new SimpleRegister[values.length];
                for (int i = 0; i < values.length; i++) {
                    regs[i] = new SimpleRegister(values[i]);
                }
                master.writeMultipleRegisters(unitId, index, regs);
                return null;
            }
        });
    }

    @Override
    protected synchronized void doInitialize() throws IOException, InterruptedException {
        connect();
        super.doInitialize();
    }

    public void connect() throws IOException {
        try {
            if (!isSimulated()) {
                master.connect();
            }
            connected = true;
        } catch (Exception ex) {
            throw new DeviceException("Cannot connect to device", ex);
        }
    }

    public void disconnect() {
        try {
            connected = false;
            if (!isSimulated()) {
                master.disconnect();
            }
        } catch (Exception ex) {
        }
    }

    @Override
    protected synchronized void doClose() throws IOException {
        super.doClose();
        if (master != null) {
            disconnect();
            master = null;
        }
    }

}
