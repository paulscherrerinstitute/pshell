package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.utils.TcpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;

/**
 * A simple console interface to the interpreter through a raw TCP connection.
 */
public class TerminalServer extends TcpServer {

    public TerminalServer(int port) throws IOException {
        super(port);
    }

    ByteArrayOutputStream buffer;

    @Override
    protected void onReceivedData(byte[] data, SocketAddress address) throws IOException {
        for (byte b : data) {
            if (b == 0x0A) {
                if (buffer != null) {
                    try {
                        String statement = new String((buffer.toByteArray()));
                        Object ret = Context.getInterpreter().evalLine(CommandSource.terminal, statement.equals("&nbsp") ? "" : statement); //&nbsp is token for empty string
                        if (ret != null) {
                            send(String.valueOf(ret).getBytes(), address);
                            send("\n".getBytes(), address);
                        }

                    } catch (Exception ex) {
                        send(ex.toString().getBytes(), address);
                        send("\n".getBytes(), address);
                    } finally {
                        buffer = null;
                    }
                }
            } else {
                if (b != 0x0D) {
                    if (buffer == null) {
                        buffer = new ByteArrayOutputStream(256);
                    }
                    buffer.write(b);
                }
            }
        }
    }
}