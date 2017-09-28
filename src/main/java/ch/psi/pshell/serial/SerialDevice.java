package ch.psi.pshell.serial;

import ch.psi.pshell.device.Device;
import java.io.IOException;

/**
 * Abstraction for serial communications, both in mode half duplex (synchronous) and full duplex
 * (asynchronous). Contains method for sending messages and waiting answers, both string and byte
 * array oriented. Some methods can manage simple protocols, having as parameters the header,
 * trailer and/or number of bytes to wait for.
 */
public interface SerialDevice extends Device {

    //Mode
    public enum Mode {

        FullDuplex,
        HalfDuplex;
    }

    Mode getMode();

    //Writing     
    void write(byte tx) throws IOException;

    void write(byte tx[]) throws IOException;

    public void write(String str) throws IOException;

    //Half-duplex reading (default. For reading in full-duplex mode override onByte)
    //Reading single byte
    int read() throws IOException;

    //Waiting answer 
    byte waitByte(int timeout) throws IOException, InterruptedException;

    byte waitByte(int timeout, Byte value) throws IOException, InterruptedException;

    byte[] waitBytes(int bytes, int timeout) throws IOException, InterruptedException;

    byte[] waitBytes(byte[] header, byte[] trailer, int messageSize, int timeout) throws IOException, InterruptedException;

    byte[] waitBytes(byte[] header, byte[] trailer, int messageSize, int timeout, boolean payloadOnly) throws IOException, InterruptedException;

    String waitString(int timeout) throws IOException, InterruptedException;

    String waitString(String trailer, int timeout) throws IOException, InterruptedException;

    String waitString(String header, String trailer, int bytes, int timeout) throws IOException, InterruptedException;

    //Send/receive pairs with retries
    byte[] sendReceive(byte[] tx, byte[] header, byte[] trailer, int bytes, int timeout, int retries) throws IOException, InterruptedException;

    String sendReceive(String tx, int timeout, int retries) throws IOException, InterruptedException;

    String sendReceive(String tx, String header, String trailer, int bytes, int timeout, int retries) throws IOException, InterruptedException;

    public void flush() throws IOException;
}
