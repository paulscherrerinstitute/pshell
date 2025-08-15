package ch.psi.pshell.framework;

import ch.psi.pshell.app.Stdio;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.sequencer.CommandSource;
import ch.psi.pshell.sequencer.ControlCommand;
import ch.psi.pshell.sequencer.InterpreterListener;
import ch.psi.pshell.sequencer.InterpreterUtils;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Sys.OSFamily;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.KeyMap;
import jline.console.completer.Completer;
import jline.console.history.MemoryHistory;
import jline.internal.NonBlockingInputStream;

/**
 * Implement PShell command-line interface. If Console.run() is called with the advanced parameter
 * set to true, uses JLine to provide advanced capabilities as command history.
 */
public class Console implements AutoCloseable{
    
    public Console(){
        attachInterpreterOutput();
    }

    public void run(InputStream in, PrintStream out, boolean advanced) throws IOException {        
        if (advanced) {
            runAdvancedConsole();
        } else {
            runStandardConsole();
        }
    }

    public void attachInterpreterOutput() {
        Context.getInterpreter().addListener(interpreterListener);
    }

    public void detachInterpreterOutput() {
        Context.getInterpreter().removeListener(interpreterListener);
    }
    
    //Pure Java, no history and headless: suited for server
    void runStandardConsole() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print(Context.getInterpreter().getCursor());
                String statement = null;
                try {
                    statement = reader.readLine();
                } catch (IOException ex) {
                }
                if (statement == null) {
                    break;
                }

                try {
                    Object ret = Context.getInterpreter().evalLine(CommandSource.console, statement);
                    if (ret != null) {
                        System.out.println(ret);
                    }
                } catch (Exception ex) {
                    System.err.println(Stdio.getPrintableMessage(ex));
                }
            }
        }

    }
        

    //Uses JLine has JNI on Windows
    void runAdvancedConsole() throws IOException {
        ConsoleReader console = new ConsoleReader();
        MemoryHistory history = new MemoryHistory();
        history.setMaxSize(Context.getInterpreter().getHistory().getSize());
        history.setIgnoreDuplicates(true);
        for (String line : Context.getInterpreter().getHistoryEntries()) {
            history.add(line);
        }
        console.setHistory(history);
        console.addCompleter(new Completer() {
            @Override
            public int complete(String string, int i, List<CharSequence> list) {
                if (string.equals(":")) {
                    for (ControlCommand cmd : ControlCommand.values()) {
                        list.add(cmd.toString());
                    }
                    return i;
                } else if (string.endsWith(".")) {
                        List<String> methods = InterpreterUtils.getSignatures(string, string.length() - 1, true);
                        if (methods != null) {
                            list.addAll(methods);
                            return i;
                        }
                    }
                list.add(string + "    ");
                return 0;
            }
        });
        //TODO: JLine bug: ctrl+c not Working on windows : https://github.com/jline/jline2/issues/undefined
        if (Sys.getOSFamily() == OSFamily.Windows) {
            console.addTriggeredAction('\u0003', new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }

            });
        }
        try {
            String statement = null;
            while ((statement = console.readLine(Context.getInterpreter().getCursor())) != null) {
                try {
                    CompletableFuture cf = Context.getInterpreter().evalLineAsync(CommandSource.console, statement);
                    while (!cf.isDone()) {
                        int key = ((NonBlockingInputStream) console.getInput()).read(10);
                        if (key>0){
                            char CTRL_P = '\u0010';
                            if (key == KeyMap.CTRL_X) {
                                System.out.println("\nControl command: abort");
                                Context.getInterpreter().abort(CommandSource.console);
                                break;
                            } else if (key == CTRL_P) {
                                System.out.println("\nControl command: pause");
                                Context.getInterpreter().pause(CommandSource.console);
                            } else if (key == KeyMap.CTRL_R) {
                                System.out.println("\nControl command: resume");
                                Context.getInterpreter().resume(CommandSource.console);
                            }
                        }
                    }
                    Object ret = cf.get();
                    if (ret != null) {
                        System.out.println(ret);
                    }
                } catch (ExecutionException ex) {
                    System.err.println(Stdio.getPrintableMessage(ex.getCause()));
                } catch (Exception ex) {
                    System.err.println(Stdio.getPrintableMessage(ex));
                }
            }
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception ex) {
                System.err.println(Stdio.getPrintableMessage(ex));
            }
        }
    }

    ScanListener scanListener = new ScanListener() {
        @Override
        public void onScanStarted(Scan scan, String plotTitle) {
            if (!Context.getExecutionPars().isScanDisplayed(scan)){
                return;
            }
            System.out.println(scan.getHeader("\t"));
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            if (!Context.getExecutionPars().isScanDisplayed(scan)){
                return;
            }
            System.out.println(record.print("\t"));
        }

        @Override

        public void onScanEnded(Scan scan, Exception ex) {
        }
    };

    final InterpreterListener interpreterListener = new InterpreterListener() {
        @Override
        public void onShellCommand(CommandSource source, String command) {
            if (source.isRemote() && !Context.isServerCommandsHidden()) {
                System.out.println(command);
            }
        }

        @Override
        public void onShellResult(CommandSource source, Object result) {
            if (source.isRemote() && !Context.isServerCommandsHidden()) {
                if (result != null) {                    
                    System.out.println(Context.getInterpreter().interpreterVariableToString(result));
                }
                System.out.print(Context.getInterpreter().getCursor());
            }
        }

        @Override
        public void onShellStdout(String str) {
            System.out.println(str);
        }

        @Override
        public void onShellStderr(String str) {
            System.err.println(str);
        }

        @Override
        public void onShellStdin(String str) {

        }

        @Override
        public void onPreferenceChange(ViewPreference preference, Object value) {
            if (preference == ViewPreference.PRINT_SCAN) {
                setPrintScan((value == null) ? defaultPrintScan : (Boolean) value);
            }
        }
    };

    static boolean defaultPrintScan;
    public static void setDefaultPrintScan(boolean value) {
        defaultPrintScan = value;
    }

    boolean printScan=defaultPrintScan;
    public boolean getPrintScan(boolean value) {
        return printScan;
    }

    public void setPrintScan(boolean value) {
        printScan = value;
        if (value) {
            Context.getInterpreter().addScanListener(scanListener);
        } else {
            Context.getInterpreter().removeScanListener(scanListener);
        }
    }

    public static void main(String args[]) throws IOException {
        App.init(args);
        new Console().run(System.in, System.out, true);
    }
    
    @Override
    public void close() {
         detachInterpreterOutput();
    }
}
