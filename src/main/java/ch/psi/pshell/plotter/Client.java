package ch.psi.pshell.plotter;

import ch.psi.pshell.core.JsonSerializer;
import ch.psi.utils.Str;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 */
public class Client implements AutoCloseable {

    final org.zeromq.ZMQ.Context context;
    final String url;
    final int timeout;

    org.zeromq.ZMQ.Socket socket;

    public Client(String url, int timeout) {
        if (!url.contains(":")) {
            url = "tcp://" + url + ":" + PlotServer.DEFAULT_PORT;
        }
        this.url = url;
        this.timeout = timeout;
        context = org.zeromq.ZMQ.context(1);
        createSocket();
    }

    private void createSocket() {
        socket = context.socket(org.zeromq.ZMQ.REQ);
        if (timeout > 0) {
            socket.setLinger(0);
            socket.setReceiveTimeOut(timeout);
        }
        socket.connect(url);
    }

    Object execute(Command command) throws Exception {
        String commandType = command.getClass().getSimpleName();
        String commandData = JsonSerializer.encode(command);
        socket.sendMore(commandType);
        socket.send(commandData);

        String rx = socket.recvStr();
        if (rx == null) {
            socket.close();
            createSocket();
            throw new TimeoutException();
        }
        Response response = (Response) JsonSerializer.decode(rx, Response.class);
        if ((response.error != null) && (!response.error.isEmpty())) {
            throw new Exception(response.error);
        }
        return command.translate(response.ret);
    }

    @Override
    public void close() throws Exception {
        socket.close();
        context.term();
    }

    public static class TimeoutException extends RuntimeException {

        public TimeoutException() {
            super("Communication timeout");
        }
    }

    protected class ProxyListener implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            try {
                Class commandClass = Class.forName(ch.psi.pshell.plotter.Command.class.getName() + "$" + Str.capitalizeFirst(method.getName()));
                for (Constructor c : commandClass.getConstructors()) {
                    if (((args == null) && (c.getParameterCount() == 0)) || ((args != null) && (c.getParameterCount() == args.length))) {
                        Command command = (Command) c.newInstance(args);
                        return execute(command);
                    }
                }

            } catch (Exception ex) {
                throw ex;
            }
            throw new RuntimeException("Invalid proxy method arguments");
        }
    }

    public Plotter getProxy() {
        ProxyListener proxyListener = new ProxyListener();
        return (Plotter) Proxy.newProxyInstance(Plotter.class.getClassLoader(), new Class[]{Plotter.class}, proxyListener);
    }

}
