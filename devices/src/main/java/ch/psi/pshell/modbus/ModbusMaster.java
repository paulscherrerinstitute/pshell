package ch.psi.pshell.modbus;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.util.BitVector;

//To compensate the fact modbus master classes don't implement common interface
interface ModbusMaster {

    public void connect() throws Exception;

    public void disconnect();

    default BitVector readCoils(int ref, int count) throws ModbusException {
        return null;
    }

    //defined with unitif in TCP/UDP Master
    //boolean writeCoil(int ref, boolean state) throws ModbusException;
    default void writeMultipleCoils(int ref, BitVector coils) throws ModbusException {
    }

    default BitVector readInputDiscretes(int ref, int count) throws ModbusException {
        return null;
    }

    default InputRegister[] readInputRegisters(int ref, int count) throws ModbusException {
        return null;
    }

    default Register[] readMultipleRegisters(int ref, int count) throws ModbusException {
        return null;
    }

    default void writeSingleRegister(int ref, Register register) throws ModbusException {
    }

    default void writeMultipleRegisters(int ref, Register[] registers) throws ModbusException {
    }

    default BitVector readCoils(int unitid, int ref, int count) throws ModbusException {
        return readCoils(ref, count);
    }

    //Same signature for all
    boolean writeCoil(int unitid, int ref, boolean state) throws ModbusException;

    default void writeMultipleCoils(int unitid, int ref, BitVector coils) throws ModbusException {
        writeMultipleCoils(ref, coils);
    }

    default BitVector readInputDiscretes(int unitid, int ref, int count) throws ModbusException {
        return readInputDiscretes(ref, count);
    }

    default InputRegister[] readInputRegisters(int unitid, int ref, int count) throws ModbusException {
        return readInputRegisters(ref, count);
    }

    default Register[] readMultipleRegisters(int unitid, int ref, int count) throws ModbusException {
        return readMultipleRegisters(ref, count);
    }

    default void writeSingleRegister(int unitid, int ref, Register register) throws ModbusException {
        writeSingleRegister(ref, register);
    }

    default void writeMultipleRegisters(int unitid, int ref, Register[] registers) throws ModbusException {
        writeMultipleRegisters(ref, registers);
    }

}
