package ch.psi.pshell.scripting;

import ch.psi.utils.Chrono;
import ch.psi.utils.IO;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.Str;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *
 */
public class ScriptManager implements AutoCloseable {

    public final static String PROPERTY_PYTHON_HOME = "python.home";
    public final static String PROPERTY_PYTHON_PATH = "python.path";

    public static final String JYTHON_OBJ_CLASS = "org.python.proxies";

    public static final String LAST_RESULT_VARIABLE = "_";

    final ScriptType type;
    final String[] libraryPath;
    final HashMap<String, Object> injections;
    final ScriptEngine engine;
    final Interpreter interpreter;
    final static Logger logger = Logger.getLogger(ScriptManager.class.getName());
    final boolean threaded;
    final Library lib;
    String sessionFilePath;
    PrintWriter sessionOut;
    Object lastResult;

    public ScriptManager(ScriptType type, String[] libraryPath, HashMap<String, Object> injections) {
        logger.info("Initializing " + getClass().getSimpleName());
        this.type = type;
        this.libraryPath = libraryPath;
        this.injections = injections;

        if (type == ScriptType.py) {
            setPythonPath(libraryPath);
            //TODO: This is a workaroung to a bug in Jython 2.7.b3 (http://sourceforge.net/p/jython/mailman/message/32935831/)
            org.python.core.Options.importSite = false;
        }

        engine = new ScriptEngineManager().getEngineByExtension(type.toString());
        if (engine == null) {
            throw new RuntimeException("Error instantiating script engine");
        }

        threaded = ((engine.getFactory().getParameter("THREADING")) != null)
                || // TODO: Nashorn is returning null. Even if it is not explicitly thread safe, 
                // blocking background calls will remove much functionality. Didn't found an issue so far.
                (type == ScriptType.js);

        lib = new Library(engine);
        lib.setPath(libraryPath);
        injections.put("lib", lib);

        injectVars();

        interpreter = new Interpreter(engine, type, null);
        logger.info("Finished " + getClass().getSimpleName() + " initialization");
    }

    public void setSessionFilePath(String sessionFilePath) {
        sessionFilePath = sessionFilePath;
        if (sessionOut != null) {
            sessionOut.close();
        }
        if ((sessionFilePath != null) && (!sessionFilePath.isEmpty())) {
            try {
                File file = new File(sessionFilePath, Chrono.getTimeStr(System.currentTimeMillis(), "YYMMdd_HHmmss") + "." + type);
                sessionOut = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            } catch (IOException e) {
                logger.log(Level.WARNING, null, e);
            }
        }
    }

    public void injectVars() {
        for (String var : injections.keySet()) {
            engine.put(var, injections.get(var));
        }
    }

    public void addInjection(String name, Object value) {
        injections.put(name, value);
        engine.put(name, value);
    }

    public void removeInjection(String name) {
        injections.remove(name);
    }

    public Map<String, Object> getInjections() {
        HashMap<String, Object> ret = new HashMap<>();
        for (String var : injections.keySet()) {
            ret.put(var, injections.get(var));
        }
        return ret;
    }

    public void setVar(String name, Object value) {
        engine.put(name, value);
    }

    public Object getVar(String name) {
        try {
            return engine.get(name);
        } catch (Exception ex) {
            return null;
        }
    }

    private void setPythonPath(String[] folders) {
        Properties props = new Properties();
        props.setProperty(PROPERTY_PYTHON_PATH, String.join(File.pathSeparator, folders));
        org.python.util.PythonInterpreter.initialize(System.getProperties(), props, new String[]{""});
    }

    public void setWriter(Writer writer) {
        engine.getContext().setWriter(writer);
    }

    public void setErrorWriter(Writer writer) {
        engine.getContext().setErrorWriter(writer);
    }

    public void setReader(Reader reader) {
        engine.getContext().setReader(reader);
    }

    public Object evalFile(String script) throws ScriptException, IOException {
        String fileName = lib.resolveFile(script);
        if (fileName == null) {
            throw new FileNotFoundException(script);
        }

        try {
            evalThread = Thread.currentThread();
            setVar(LAST_RESULT_VARIABLE, null);
            Object ret = interpreter.evalFile(fileName);
            //In Jython, the output of last statement is not returned so we have to use a return variable
            if (ret == null) {
                ret = getVar(LAST_RESULT_VARIABLE);
            }
            saveStatement("\n#Eval file:  " + script + "\n");
            return ret;
        } finally {
            evalThread = null;
        }
    }

    @Hidden
    public void resetLineNumber() {
        interpreter.lineNumber = 1;
    }

    volatile boolean runningStatementList = false;

    public boolean isRunningStatementList() {
        return runningStatementList;
    }

    volatile boolean statementListExecutionPaused = false;

    public boolean isStatementListExecutionPaused() {
        return statementListExecutionPaused;
    }

    volatile Statement runningStatement = null;

    public Statement getRunningStatement() {
        return runningStatement;
    }

    public void pauseStatementListExecution() {
        statementListExecutionPaused = true;
    }

    public void resumeStatementListExecution() {
        statementListExecutionPaused = false;
        synchronized (debugStepLock) {
            debugStepLock.notifyAll();
        }
    }

    volatile boolean stepStatementListExecutionFlag = false;

    public void stepStatementListExecution() {
        if (isStatementListExecutionPaused()) {
            stepStatementListExecutionFlag = true;
            synchronized (debugStepLock) {
                debugStepLock.notifyAll();
            }
        }
    }

    /**
     * The listener interface for receiving statement evaluation events.
     */
    public interface StatementsEvalListener {

        void onNewStatement(Statement statement);

        void onStartingStatement(Statement statement);

        void onFinishedStatement(Statement statement, InterpreterResult result);
    }

    public Object eval(Statement[] statements) throws ScriptException, InterruptedException {
        return eval(statements, null);
    }

    final Object debugStepLock = new Object();

    public Object eval(Statement[] statements, StatementsEvalListener listener) throws ScriptException, InterruptedException {
        InterpreterResult result = null;
        evalThread = Thread.currentThread();
        runningStatementList = true;
        interpreter.lineNumber++;
        try {
            for (Statement statement : statements) {
                runningStatement = statement;
                if (listener != null) {
                    listener.onNewStatement(statement);
                }
                while (isStatementListExecutionPaused() && (!stepStatementListExecutionFlag)) {
                    synchronized (debugStepLock) {
                        debugStepLock.wait();
                    }
                }
                stepStatementListExecutionFlag = false;
                if (listener != null) {
                    listener.onStartingStatement(statement);
                }

                result = interpreter.interpret(statement.text, true);
                if (result.exception != null) {
                    result.exception = new StatementException(result.exception, statement);
                } else {
                    if (result.result != null) {
                        lastResult = result.result;
                        setVar(LAST_RESULT_VARIABLE, lastResult);
                    }
                    saveStatement(statement.text);
                }

                if (listener != null) {
                    listener.onFinishedStatement(statement, result);
                }
                if ((result != null) && (result.exception != null)) {
                    throw result.exception;
                }
            }
            if (result == null) {
                return null;
            }
            return result.result;
        } finally {
            evalThread = null;
            runningStatementList = false;
            stepStatementListExecutionFlag = false;
            statementListExecutionPaused = false;
            runningStatement = null;
        }
    }

    /*
     Object lastResult;
     public Object getLastResult(){
     return lastResult;
     }
     */
    public InterpreterResult eval(String line) {
        evalThread = Thread.currentThread();
        try {
            InterpreterResult ret = interpreter.interpret(line);
//            lastResult = ret.result;
            //We don't care about the line script name cause it is interactive
            if (ret.exception != null) {
                ret.exception = new StatementException(ret.exception, null, -1);
            } else {
                if (ret.result != null) {
                    lastResult = ret.result;
                    setVar(LAST_RESULT_VARIABLE, lastResult);
                }
                saveStatement(line);
            }
            return ret;
        } finally {
            evalThread = null;
        }
    }

    public InterpreterResult eval(Statement statement) {

        if (statement == null) {
            return eval((String) null);
        }
        evalThread = Thread.currentThread();
        try {
            //TODO: could execute the compiled script directly
            InterpreterResult ret = interpreter.interpret(statement.text);
            if (ret.exception != null) {
                ret.exception = new StatementException(ret.exception, statement);
            } else {
                if (ret.result != null) {
                    lastResult = ret.result;
                    setVar(LAST_RESULT_VARIABLE, lastResult);
                }
                saveStatement(statement.text);
            }
            return ret;
        } finally {
            evalThread = null;
        }
    }

    public InterpreterResult evalBackground(String statement) {
        if (!threaded) {
            return null;
        }
        InterpreterResult ret = new InterpreterResult();

        try {
            ret.result = engine.eval(statement);
            ret.correct = true;
            ret.complete = true;
        } catch (ScriptException ex) {
            ret.exception = ex;
        }
        return ret;
    }

    private Thread evalThread;

    public Thread getEvalThread() {
        return evalThread;
    }

    public void abort() {
        logger.info("Aborting");
        try {
            if ((evalThread != null) && (evalThread != Thread.currentThread())) {
                evalThread.interrupt();
            }
            resetInterpreter();
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
    }

    public int getLineNumber() {
        return interpreter.getLineNumber();
    }

    public int getStatementLineCount() {
        return interpreter.getStatementLineCount();
    }

    public void resetInterpreter() {
        interpreter.reset();
    }

    public Object getLastResult() {
        return lastResult;
    }

    public Statement[] parse(String fileName) throws FileNotFoundException, ScriptException, IOException {
        fileName = lib.resolveFile(fileName);
        IO.assertExistsFile(fileName);
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName))) {
            return parse(reader, fileName);
        }
    }

    public Statement[] parse(String string, String fileName) throws FileNotFoundException, ScriptException, IOException {
        try (StringReader reader = new StringReader(string)) {
            return parse(reader, fileName);
        }//Implicit close
    }

    public Statement[] parse(Reader reader, String fileName) throws FileNotFoundException, ScriptException, IOException {
        ArrayList<String> lines = new ArrayList<>();
        ArrayList<Statement> ret = new ArrayList<>();

        String line;
        int statementNumber = 1;
        int statementLineNumber = 1;
        boolean inBlockStatement = false;

        try (BufferedReader br = new BufferedReader(reader)) {
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            StringBuilder sb = new StringBuilder();
            outerloop:
            for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                line = lines.get(lineNumber);
                if (sb.length() == 0) {
                    statementLineNumber = lineNumber + 1;
                } else {
                    sb.append("\n");
                }

                sb.append(line);
                String aux = line.trim();
                if (type == ScriptType.py) {
                    //Check if in commented line                    
                    //This is simplistic, not supporting block comment nesting
                    if (aux.contains("\"\"\"")) {
                        if (Str.count(aux, "\"\"\"") != 2) {
                            inBlockStatement = !inBlockStatement;
                        }
                    } else if (aux.contains("'''")) {
                        if (Str.count(aux, "'''") != 2) {
                            inBlockStatement = !inBlockStatement;
                        }
                    }

                    if (lineNumber < (lines.size() - 1)) {
                        if (inBlockStatement || aux.startsWith("#")) {
                            continue;
                        }
                    }
                    if (lineNumber < (lines.size() - 1)) {
                        String next = lines.get(lineNumber + 1);
                        if (next.trim().isEmpty() || hasIndentation(next)) {
                            continue;
                        }
                        //So that non-aligned comments are ok:
                        //def F():
                        //    ...
                        //#    Comment
                        //  #    Comment 2
                        //    ...                        
                        if (next.trim().startsWith("#")) {
                            if (lineNumber < (lines.size() - 2)) {
                                String following = lines.get(lineNumber + 2);
                                if (hasIndentation(following) || following.trim().startsWith("#")) {
                                    if (hasIndentation(line) || aux.startsWith("#")) {
                                        continue;
                                    }
                                }
                            }
                        }
                        for (String str : new String[]{"else", "except", "elif", "finally"}) {
                            if (next.startsWith(str)) {
                                char c = next.charAt(str.length());
                                if ((c == ' ') || (c == ':') || (c == '(')) {
                                    continue outerloop;
                                }
                            }
                        }
                    }
                } else if (type == ScriptType.js) {
                    if (!inBlockStatement && aux.contains("/*") && Str.count(aux, "/*") != Str.count(aux, "*/")) {
                        inBlockStatement = true;
                    } else if (inBlockStatement && aux.contains("*/") && Str.count(aux, "/*") != Str.count(aux, "*/")) {
                        inBlockStatement = false;
                    }
                    if (lineNumber < (lines.size() - 1)) {
                        if (inBlockStatement /*|| aux.startsWith("//")*/) {
                            continue;
                        }
                    }
                    if (lineNumber < (lines.size() - 1)) {
                        String next = lines.get(lineNumber + 1).trim();
                        if (next.isEmpty()) {
                            continue;
                        }
                        if (next.startsWith("//")) {
                            continue;
                        }
                        for (String str : new String[]{"else", "catch", "finally"}) {
                            if (next.startsWith(str)) {
                                continue outerloop;
                            }
                        }
                    }
                }

                CompiledScript cs = interpreter.tryCompiling(sb.toString(), true);

                //Check if next line is indented  
                if (cs != null) {
                    //We have a valid, complete statement!                
                    Statement statement = new Statement();
                    statement.fileName = fileName;
                    statement.text = sb.toString();
                    statement.number = statementNumber++;
                    statement.lineNumber = statementLineNumber;
                    statement.compiledScript = cs;
                    statement.finalLineNumber = lineNumber + 1;
                    ret.add(statement);

                    sb = new StringBuilder();
                }
            }
        } catch (ScriptException ex) {
            throw new StatementException(ex, fileName, statementLineNumber);
        }
        return ret.toArray(new Statement[0]);
    }

    boolean hasIndentation(String str) {
        if (str.length() > 1) {
            if ((str.charAt(0) == ' ') || (str.charAt(0) == '\t')) {
                return true;
            }
        }
        return false;
    }

    boolean isSameIndentation(String str1, String str2) {
        try {
            for (int i = 0; i < str1.length(); i++) {
                if ((str1.charAt(i) == ' ') || (str1.charAt(i) == '\t')) {
                    if (str2.charAt(i) != str1.charAt(i)) {
                        return false;
                    }
                } else if ((str2.charAt(i) == ' ') || (str2.charAt(i) == '\t')) {
                    return false;
                } else {
                    break;
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    void saveStatement(String statement) {
        //Save statement to history
        try {
            if (sessionOut != null) {
                sessionOut.println(statement);
                sessionOut.flush();
            }
        } catch (Exception e) {
            sessionOut = null;
            logger.warning("Error writing to session file");
        }
    }

    public Library getLibrary() {
        return lib;
    }

    @Override
    public void close() {
        try {
            if (sessionOut != null) {
                sessionOut.close();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }
}
