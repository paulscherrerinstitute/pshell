package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.utils.Arr;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An optional console interface ("t" option) for the workbench. Not to be
 * confused with ch.psi.pshell.core.Console.
 */
public class Console {

    static final Logger logger = Logger.getLogger(Console.class.getName());

    public Console() {
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
            System.err.println(ch.psi.pshell.core.Console.getPrintableMessage(ex));
            throw ex;
        }
    }

    /**
     * Override to implement the console behavior
     */
    protected void onConsoleCommand(String name, String[] pars, String trimming) throws Exception {
        App app = App.getInstance();
        Context context = Context.getInstance();
        StringBuilder sb;

        switch (name) {
            case "exit":
                app.exit(this);
                break;
            case "run":
                if (pars.length > 0) {
                    String file = pars[0];
                    app.startTask(new Task.ScriptExecution(file, null, null, false, false));
                }
                break;
            case "eval":
                if (!trimming.isEmpty()) {
                    System.out.println(context.evalLine(trimming));
                }
                break;
            case "evalb":
                if (!trimming.isEmpty()) {
                    System.out.println(context.evalLineBackground(trimming));
                }
                break;
            case "show":
                if (app.getMainFrame() != null) {
                    app.getMainFrame().setVisible(true);
                }
                break;
            case "state":
                System.out.println(app.getState());
                break;
            case "hide":
                if (app.getMainFrame() != null) {
                    app.getMainFrame().setVisible(false);
                }

                break;
            case "about":
                sb = new StringBuilder();
                sb.append("Name: ").append(App.getApplicationName()).append(" ").append(App.getResourceBundleValue("Application.copyright"));
                sb.append("\nVersion: ").append(App.getApplicationBuildInfo());
                sb.append("\nFeedback: ").append(App.getResourceBundleValue("Application.feedback"))
                        .append("@")
                        .append(App.getResourceBundleValue("Application.vendor"));
                sb.append("\nDescription: ").append(App.getResourceBundleValue("Application.description"));
                sb.append("\n");
                System.out.println(sb.toString());
                break;
            case "plugins":
                sb = new StringBuilder();
                for (ch.psi.pshell.core.Plugin p : context.getPlugins()) {
                    sb.append("Name: ").append(p.toString()).append(" Started: ").append(p.isStarted()).append(" Class: ").append(p.getClass().getName());
                }
                System.out.println(sb.toString());
                break;
            default:
                System.out.println("Invalid Command");
                break;
        }
    }
}
