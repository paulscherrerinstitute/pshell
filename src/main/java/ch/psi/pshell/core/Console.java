package ch.psi.pshell.core;

import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.scripting.JepScriptEngine;
import ch.psi.pshell.scripting.JythonUtils;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.scripting.ViewPreference;
import ch.psi.pshell.ui.App;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.Observable;
import ch.psi.utils.Reflection;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
import ch.psi.utils.Sys.OSFamily;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import javax.swing.JComponent;
import javax.swing.JPanel;
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
    
    static final boolean INCLUDE_IMMUTABLE_ATTRS_SIGNATURES = false;
    
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
        Context.getInstance().addListener(contextListener);
    }

    public void detachInterpreterOutput() {
        Context.getInstance().removeListener(contextListener);
    }
    
    //Pure Java, no history and headless: suited for server
    void runStandardConsole() throws IOException {
        Context context = Context.getInstance();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print(context.getCursor());
                String statement = null;
                try {
                    statement = reader.readLine();
                } catch (IOException ex) {
                }
                if (statement == null) {
                    break;
                }

                try {
                    Object ret = context.evalLine(CommandSource.console, statement);
                    if (ret != null) {
                        System.out.println(ret);
                    }
                } catch (Exception ex) {
                    System.err.println(getPrintableMessage(ex));
                }
            }
        }

    }
        

    //Uses JLine has JNI on Windows
    void runAdvancedConsole() throws IOException {
        Context context = Context.getInstance();
        ConsoleReader console = new ConsoleReader();
        MemoryHistory history = new MemoryHistory();
        history.setMaxSize(context.history.getSize());
        history.setIgnoreDuplicates(true);
        for (String line : context.getHistory()) {
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
                        List<String> methods = getSignatures(string, string.length() - 1, true);
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
            while ((statement = console.readLine(context.getCursor())) != null) {
                try {
                    CompletableFuture cf = context.evalLineAsync(CommandSource.console, statement);
                    while (!cf.isDone()) {
                        int key = ((NonBlockingInputStream) console.getInput()).read(10);
                        if (key>0){
                            char CTRL_P = '\u0010';
                            if (key == KeyMap.CTRL_X) {
                                System.out.println("\nControl command: abort");
                                context.abort(CommandSource.console);
                                break;
                            } else if (key == CTRL_P) {
                                System.out.println("\nControl command: pause");
                                context.pause(CommandSource.console);
                            } else if (key == KeyMap.CTRL_R) {
                                System.out.println("\nControl command: resume");
                                context.resume(CommandSource.console);
                            }
                        }
                    }
                    Object ret = cf.get();
                    if (ret != null) {
                        System.out.println(ret);
                    }
                } catch (ExecutionException ex) {
                    System.err.println(getPrintableMessage(ex.getCause()));
                } catch (Exception ex) {
                    System.err.println(getPrintableMessage(ex));
                }
            }
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception ex) {
                System.err.println(getPrintableMessage(ex));
            }
        }
    }

    ScanListener scanListener = new ScanListener() {
        @Override
        public void onScanStarted(Scan scan, String plotTitle) {
            if (!Context.getInstance().getExecutionPars().isScanDisplayed(scan)){
                return;
            }
            System.out.println(scan.getHeader("\t"));
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            if (!Context.getInstance().getExecutionPars().isScanDisplayed(scan)){
                return;
            }
            System.out.println(record.print("\t"));
        }

        @Override

        public void onScanEnded(Scan scan, Exception ex) {
        }
    };

    final ContextListener contextListener = new ContextAdapter() {
        @Override
        public void onShellCommand(CommandSource source, String command) {
            if (source.isRemote() && !Context.getInstance().getConfig().hideServerMessages) {
                System.out.println(command);
            }
        }

        @Override
        public void onShellResult(CommandSource source, Object result) {
            Context context = Context.getInstance();
            if (source.isRemote() && !context.getConfig().hideServerMessages) {
                if (result != null) {                    
                    System.out.println(context.interpreterVariableToString(result));
                }
                System.out.print(context.getCursor());
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
            Context.getInstance().addScanListener(scanListener);
        } else {
            Context.getInstance().removeScanListener(scanListener);
        }
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

    public static void main(String args[]) throws IOException {
        App.init(args);
        Context context = Context.createInstance();
        context.start();
        new Console().run(System.in, System.out, true);
    }
    
    
    static boolean canCallInterpreter(){
        Context context = Context.getInstance();
        return (context.getScriptManager().isThreaded() || context.getState().isReady());
    }

    public static List<String> getSignatures(String str, int position, boolean propagate) {
        if (!canCallInterpreter()){
            return null;
        }
        if (str.length() > 0) {
            String aux = str.toLowerCase();
            int i = position - 1;
            for (; i >= 0; i--) {
                char c = aux.charAt(i);
                char next = (i > 0) ? aux.charAt(i - 1) : 0;
                if (((c == ')') && (next == '('))) { //Closing parenthesis
                    i--;
                } else if (((c < 'a') || (c > 'z')) && ((c < '0') || (c > '9'))
                        && (c != '_') && (c != '.')) {
                    break;
                }
            }
            str = str.substring(i + 1, position);
            String var = str.trim();
            Object obj = null;
            if (var.equals("_")) {
                obj = Context.getInstance().getLastEvalResult();
            } else {
                if (propagate) {
                    String[] tokens = var.split("\\.");
                    try {
                        obj = Context.getInstance().getInterpreterVariable(tokens[0]);
                        for (int j = 1; j < tokens.length; j++) {
                            String method = tokens[j];
                            //Only process getters
                            if (method.endsWith("()") && method.startsWith("get")) {
                                method = method.substring(0, method.length() - 2);
                            } else {
                                method = "get" + Str.capitalizeFirst(tokens[j]);
                            }
                            obj = obj.getClass().getMethod(method, new Class[0]).invoke(obj, new Object[0]);
                        }
                    } catch (Exception ex) {
                        obj = null;
                    }
                } else {
                    obj = Context.getInstance().getInterpreterVariable(var);
                }
            }
            if ((obj != null) && (Convert.getPrimitiveClass(obj.getClass()) == null)) {
                return getSignatures(obj, var);
            }
        }
        return null;
    }
    
    static List<String> getSignatures(Object obj, String name) {
        if (obj instanceof org.python.core.PyObject) {
            //Not parsed as normal java objects, must "dir" them
            return JythonUtils.getSignatures((org.python.core.PyObject) obj, true);
        } else if ((Context.getInstance().getScriptType()==ScriptType.cpy)   &&  
                (!(obj.getClass().getName().startsWith("ch.psi")))) {
            //TODO: JepScriptEngine.getSignatures does not work for java objects, and 
            //wrapped objects as java.* and jep.* woll show different signatures then the python counbterpars.
            //Therefore decided to remove autocompletion of them (JepScriptEngine.getSignatures will fail). 
            //Use standard auto-complention for internal (ch.psi) objects because they match Python's.
            try {
                if (canCallInterpreter()){                                                 
                    return (List<String>)Context.getInstance().runInInterpreterThread(null,(Callable<List<String>>)() ->{
                        return ((JepScriptEngine)Context.getInstance().getScriptManager().getEngine()).getSignatures(
                                obj, (name.endsWith(".")) ? name.substring(0, name.length()-1)  : name);
                    });
                }
            } catch (Exception ex) {
                Logger.getLogger(Console.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        } else {            
            Class[] excludeClasses = new Class[]{AutoCloseable.class, Observable.class, JPanel.class, JComponent.class, Container.class, Component.class};
            String[] excludeNames = new String[]{};
            if (Context.getInstance().getSetup().getScriptType() == ScriptType.py) {
                excludeClasses = Arr.append(excludeClasses, JythonUtils.REFLECTION_EXCLUDE_CLASSES);
                excludeNames = Arr.append(excludeNames, JythonUtils.REFLECTION_EXCLUDE_NAMES);
            }
            List<String> ret = Reflection.getMethodsSignature(obj, excludeClasses, excludeNames, true, true, true);
            //Proxies: included methods defined in python
            if (obj instanceof org.python.core.PyProxy) {
                ret.addAll(JythonUtils.getSignatures(((org.python.core.PyProxy) obj)._getPyInstance(), false));
            }
            if (INCLUDE_IMMUTABLE_ATTRS_SIGNATURES){
                ret.addAll(Reflection.getAttributesSignature(obj, excludeClasses, excludeNames, true, false, true));
            }
            return ret;
        }
    }

    @Override
    public void close() {
         detachInterpreterOutput();
    }
}
