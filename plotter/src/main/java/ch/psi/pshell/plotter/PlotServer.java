package ch.psi.pshell.plotter;

import ch.psi.pshell.utils.EncoderJson;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ;

/**
 *
 */
public class PlotServer implements AutoCloseable {

    final Thread thread;
    final int port;
    ZMQ.Socket socket;
    ZMQ.Context context;
    final Plotter pm;
    public static final int DEFAULT_PORT = 7777;
    //public static final String ERROR_PREFIX = "ERROR: "; 
    //final InvokingProducer<Command> commandProducer;

    public static boolean debug = false;

    public PlotServer(Plotter pm) {
        this(DEFAULT_PORT, pm);
    }

    public PlotServer(int port, Plotter pm) {
        this.port = port;
        this.pm = pm;
        /*
        commandProducer = new InvokingProducer<Command>() {
            @Override
            protected void consume(Command command) {
                process(command);
            }
        };
         */
        thread = new Thread(() -> {
            context = ZMQ.context(1);
            socket = context.socket(zmq.ZMQ.ZMQ_REP);

            try {
                socket.bind(getAddress());
            } catch (Exception ex) {
                socket.close();
                context.term();
                Logger.getLogger(PlotServer.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String commandType;
                    String commandData = null;
                    try {
                        commandType = socket.recvStr();
                        if (socket.hasReceiveMore()) {
                            commandData = socket.recvStr();
                        }
                        if (debug) {
                            System.out.println("RX: " + commandType + " - " + commandData);
                        }
                    } catch (Throwable ex) {
                        Logger.getLogger(PlotServer.class.getName()).log(Level.FINE, null, ex);
                        break;
                    }
                    String ret = null;
                    String error = null;
                    try {
                        Class type = Class.forName(ch.psi.pshell.plotter.Command.class.getName() + "$" + commandType);
                        Command command = (Command) EncoderJson.decode(commandData, type);
                        //commandProducer.post(command);                                                
                        ret = process(command);
                        //socket.send(String.valueOf(response));
                        //ret = response;
                    } catch (Throwable ex) {
                        if (debug){
                            ex.printStackTrace();
                        }
                        if (ex instanceof InterruptedException) {
                            Logger.getLogger(PlotServer.class.getName()).fine("Thread interrupted");
                            break;
                        }
                        Logger.getLogger(PlotServer.class.getName()).log(Level.WARNING, null, ex);
                        //socket.send("ERROR: " + c);
                        error = ex.getMessage();
                    }
                    String tx = null;
                    try {
                        tx = EncoderJson.encode(new Response(ret, error), false);
                    } catch (Exception ex) {
                        Logger.getLogger(PlotServer.class.getName()).log(Level.WARNING, null, ex);                            
                        //continue; Must return something otherwise break send/rec
                        tx = Response.UNKNOWN_ERROR;
                        
                   }
                    if (debug) {
                        System.out.println("TX: " + commandType + " - " + tx);
                    }
                    socket.send(tx);
                }
            } catch (Throwable t) {
                if (debug){
                    t.printStackTrace();
                }
            } finally {
                if (debug){
                    System.out.println("Quitting server thread. Interrupted: " + Thread.currentThread().isInterrupted());    
                }
                socket.close();
                context.term();
                Logger.getLogger(PlotServer.class.getName()).log(Level.INFO, "Quitting");
            }

        }, "Plot Server - port: " + port);

        thread.start();
    }
    
    public int getPort(){
        return port;
    }
    
    public String getAddress(){
        return "tcp://*:" + port;
    }

    public boolean isRunning() {
        return thread.isAlive();
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }

    String process(Command command) throws Exception {
        return command.invoke(pm);
    }

}
