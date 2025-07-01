package ch.psi.pshell.app;

import ch.psi.pshell.utils.Arr;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * An optional interface for the workbench to exchange data through the stdio.
 */
abstract public class Stdio {

    static final Logger logger = Logger.getLogger(Stdio.class.getName());

    public Stdio() {
        input = System.in;
    }

    String cursor;

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    InputStream input;

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public void start() {
        stop();
        thread = new Thread(runnable, "Console");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    Thread thread;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            logger.info("Enter console manager");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

                while (!Thread.currentThread().isInterrupted()) {
                    if (cursor != null) {
                        System.out.print(cursor);
                    }
                    String command = reader.readLine();

                    if (command == null) {
                        logger.info("End of input stream");
                        break;
                    }
                    command = command.trim();
                    if (!command.isEmpty()) {
                        logger.log(Level.INFO, command);
                        try {
                            onConsoleCommand(command);
                        } catch (Exception ex) {
                            if (ex instanceof InterruptedException) {
                                throw ex;
                            }
                            logger.log(Level.INFO, null, ex);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            } finally {
                logger.info("Quit console manager");
            }
        }
    };

    //For logging
    @Override
    public String toString() {
        return "Console";
    }
    
    public static String getPrintableMessage(Throwable ex) {
        try{
            if (ex instanceof ScriptException) {
                String ret = ex.getMessage();
                if (ret != null) {
                    //Filtering duplicate exception class name 
                    String[] tokens = ret.split(":");
                    if ((tokens.length >= 3) && (tokens[0].trim().equals(tokens[1].trim()))) {
                        ret = ret.substring(ret.indexOf(":") + 1).trim();
                    }
                    return ret;
                }
            }
            return ex.toString();
        } catch (Throwable f){
            return "Error getting error message: " + f.getMessage();
        }
    }
    

    protected void onConsoleCommand(String command) throws Exception {
        command = command.trim();
        String[] tokens = command.split(" ");
        String name = tokens[0].trim();
        String trimming = command.contains(" ") ? command.substring(command.indexOf(" ")).trim() : "";
        String[] pars = {};

        for (int i = 1; i < tokens.length; i++) {
            if (!tokens[i].trim().isEmpty()) {
                pars = Arr.append(pars, tokens[i].trim());
            }
        }
        try {
            onConsoleCommand(name, pars, trimming);
        } catch (Exception ex) {
            System.err.println(getPrintableMessage(ex));
            throw ex;
        }
    }

    /**
     * Override to implement the console behaviour
     */
    protected void onConsoleCommand(String name, String[] pars, String trimming) throws Exception {}
}
