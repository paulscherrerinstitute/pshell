package ch.psi.pshell.scripting;

import ch.psi.pshell.logging.LogManager;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.FileSystemWatch;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.ProcessFactory;
import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
    static  ScriptManager INSTANCE;    
    public static  ScriptManager getInstance(){  
        return INSTANCE;
    }

    public final static String PROPERTY_PYTHON_HOME = "python.home";
    public final static String PROPERTY_PYTHON_PATH = "python.path";

    public static final String JYTHON_OBJ_CLASS = "org.python.proxies";

    public static final String LAST_RESULT_VARIABLE = "_";
    public static final String THREAD_RESULT_VARIABLE = "__THREAD_EXEC_RESULT__";

    final ScriptType type;
    final Map<String, Object> injections;
    final ScriptEngine engine;
    final Interpreter interpreter;
    final static Logger logger = Logger.getLogger(ScriptManager.class.getName());
    final boolean threaded;
    final Library lib;
    final FilePermissions scriptFilePermissions;
    final boolean dontWriteBytecode;
    String[] libraryPath;
    String sessionFilePath;
    PrintWriter sessionOut;
    Object lastResult;
    Object lastScriptResult;
    final Map<Thread, Object> results;

    final int CLASS_FILE_CHECK_INTERVAL = 5000;
    FileSystemWatch jythonClassFilePermissionMonitor;

    public ScriptManager(ScriptType type, String[] libraryPath, HashMap<String, Object> injections, FilePermissions scriptFilePermissions, boolean dontWriteBytecode) {
        INSTANCE  = this;
        logger.log(Level.INFO, "Initializing {0}", getClass().getSimpleName());
        this.type = type;
        this.libraryPath = (libraryPath==null) ? new String[0]: libraryPath;
        this.injections = injections;
        this.scriptFilePermissions = scriptFilePermissions;
        this.dontWriteBytecode = dontWriteBytecode;
        results = new HashMap<>();

        if (type == ScriptType.py) {
            //TODO: This is a workaround to a bug in Jython 2.7.b3 (http://sourceforge.net/p/jython/mailman/message/32935831/)
            //TODO: The problem is solved in Jython 2.7.1, but this option makes startup 0.5s faster. Are there consequences?
            org.python.core.Options.importSite = false;
            org.python.core.Options.dont_write_bytecode = dontWriteBytecode;
            setPythonPath(this.libraryPath);
        }

        //engine = new ScriptEngineManager().getEngineByExtension(type.toString());)
        ScriptEngineManager manager = new ScriptEngineManager(Sys.getDynamicClassLoader());
        if (type==ScriptType.cpy){
            try {
                String jar = IO.getExecutingJar(ScriptManager.class);
                if (jar!=null){
                    Sys.addToClassPath(new File(jar).getCanonicalFile()); 
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }            
            manager.registerEngineName("jep", new JepScriptEngineFactory(libraryPath));
            engine =  manager.getEngineByName("jep");
        } else {
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(Sys.getDynamicClassLoader());
            ScriptEngine engine = manager.getEngineByExtension(type.toString());            
            if (engine==null){
                Thread.currentThread().setContextClassLoader(originalClassLoader);
                manager = new ScriptEngineManager(originalClassLoader);
                engine = manager.getEngineByExtension(type.toString());
            }
            this.engine = engine;
        }        

        if (engine == null) {
            throw new RuntimeException("Error instantiating script engine");
        }

        boolean threaded = false;
        try {
            threaded = ((engine.getFactory().getParameter("THREADING")) != null) || (type == ScriptType.js);
            // TODO: Nashorn is returning null. Even if it is not explicitly thread safe, 
            // blocking background calls will remove much functionality. Didn't found an issue so far.               
            
            //TODO: JEP is single-threaded, mut all access are executed in private thread: functionality will be limited
        } catch (Exception ex) {
            //graaljs raises excerption and don't accept threaded
            threaded = false;
        }

        this.threaded = threaded;
        lib = new Library(engine);
        lib.setPath(libraryPath);
        injections.put("lib", lib);
        if (threaded) {
            injections.put(THREAD_RESULT_VARIABLE, results);
        }

        injectVars();
        
        interpreter = new Interpreter(engine, type, null);
        logger.log(Level.INFO, "Finished {0} initialization", getClass().getSimpleName());
    }
    
    public ScriptType  getType(){
        return type;
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public Object getResult(Thread thread) {
        return results.get(thread);
    }

    public void setSessionFilePath(String sessionFilePath) {
        sessionFilePath = sessionFilePath;
        if (sessionOut != null) {
            sessionOut.close();
        }
        if ((sessionFilePath != null) && (!sessionFilePath.isEmpty())) {
            try {
                File file = new File(sessionFilePath, Chrono.getTimeStr(System.currentTimeMillis(), "YYMMdd_HHmmss") + "." + type.getExtension());
                sessionOut = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                if (LogManager.hasInstance()){
                    IO.setFilePermissions(file, LogManager.getInstance().getFilePermissions());
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, null, e);
            }
        }
    }

    public void injectVars() {
        for (String var : injections.keySet()) {
            setVar(var, injections.get(var));
        }
    }

    public void addInjection(String name, Object value) {
        injections.put(name, value);
        try{
            setVar(name, value);
        } catch (Exception ex){
            logger.warning("Cannot set variable: " + name);
        }        
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
        if (type == ScriptType.py) {
            props.setProperty(PROPERTY_PYTHON_PATH, String.join(File.pathSeparator, folders));
            org.python.util.PythonInterpreter.initialize(System.getProperties(), props, new String[]{""});        
        }
    }

    void beforeEval(boolean background) {
        if (!background) {
            evalThread = Thread.currentThread();
        }
    }

    void afterEval(boolean background) {
        if (!background) {
            evalThread = null;
        }
        if (jythonClassFilePermissionMonitor != null) {
            jythonClassFilePermissionMonitor.check(CLASS_FILE_CHECK_INTERVAL);
        }
    }

    public void addLibraryPath(String path) {
        try {
            if (!Arr.containsEqual(libraryPath, path)) {
                libraryPath = Arr.append(libraryPath, path); 
                if (type == ScriptType.py) { //!!! Only managing Jython?  
                    if (engine!=null){
                        engine.eval("import sys; sys.path.append('" + path + "')");
                    } else {
                        setPythonPath(libraryPath);               
                    }
                }
                lib.setPath(libraryPath);
            }
            } catch (ScriptException ex) {
            Logger.getLogger(ScriptManager.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        lastScriptResult= null;
        String fileName = lib.resolveFile(script);
        if (fileName == null) {
            throw new FileNotFoundException(script);
        }
        beforeEval(false);
        try {
            setVar(LAST_RESULT_VARIABLE, null);
            results.put(evalThread, null);
            Object ret = interpreter.evalFile(fileName);
            //In Jython, the output of last statement is not returned so we have to use a return variable
            if (ret == null) {
                ret = results.get(evalThread);
            }
            lastScriptResult = ret;
            saveStatement("\n#Eval file:  " + script + "\n");
            return ret;
        } catch (Exception ex) {
            lastScriptResult = ex;
            throw ex;
        } finally {
            afterEval(false);
        }
    }

    public Object evalFileBackground(String script) throws ScriptException, IOException {
        assertThreaded();
        String fileName = lib.resolveFile(script);
        if (fileName == null) {
            throw new FileNotFoundException(script);
        }
        results.put(Thread.currentThread(), null);
        beforeEval(true);
        try {
            Object ret = interpreter.evalFile(fileName);
            if (ret == null) {
                ret = results.get(Thread.currentThread());
                results.remove(Thread.currentThread());
            }
            //In Jython, the output of last statement is not returned so we have to use a return variable
            saveStatement("\n#Eval file background:  " + script + "\n");
            return ret;
        } finally {
            afterEval(true);
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
    
    volatile boolean executingStatement = false;

    public boolean isExecutingStatement() {
        return executingStatement;
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
        beforeEval(false);
        try {
            executingStatement = false;
            runningStatementList = true;
            interpreter.lineNumber++;
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
                executingStatement = true;
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
                        results.put(evalThread, lastResult);
                    }
                    saveStatement(statement.text);
                }

                if (listener != null) {
                    listener.onFinishedStatement(statement, result);
                }
                executingStatement = false;
                if ((result != null) && (result.exception != null)) {
                    throw result.exception;
                }
            }
            if (result == null) {
                return null;
            }
            return result.result;
        } finally {
            afterEval(false);
            runningStatementList = false;
            stepStatementListExecutionFlag = false;
            statementListExecutionPaused = false;
            runningStatement = null;
            executingStatement = false;
        }
    }

    /*
     Object lastResult;
     public Object getLastResult(){
     return lastResult;
     }
     */
    public InterpreterResult eval(String line) {
        beforeEval(false);
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
                    results.put(evalThread, lastResult);
                }
                saveStatement(line);
            }
            return ret;
        } finally {
            afterEval(false);
        }
    }

    public InterpreterResult eval(Statement statement) {

        if (statement == null) {
            return eval((String) null);
        }
        beforeEval(false);
        try {
            //TODO: could execute the compiled script directly
            InterpreterResult ret = interpreter.interpret(statement.text);
            if (ret.exception != null) {
                ret.exception = new StatementException(ret.exception, statement);
            } else {
                if (ret.result != null) {
                    lastResult = ret.result;
                    setVar(LAST_RESULT_VARIABLE, lastResult);
                    results.put(evalThread, lastResult);
                }
                saveStatement(statement.text);
            }
            return ret;
        } finally {
            afterEval(false);
        }
    }

    public boolean isThreaded() {
        return threaded;
    }
    
    public void assertThreaded() throws ScriptException{   
        if (!threaded) {
            throw new ScriptException("Interpreter is single-threaded: background execution is disabled");
        }
    }

    public InterpreterResult evalBackground(String statement) throws ScriptException {
        assertThreaded();
        
        InterpreterResult ret = new InterpreterResult();
        beforeEval(true);
        try {
            ret.result = engine.eval(statement);
            ret.correct = true;
            ret.complete = true;
        } catch (ScriptException ex) {
            ret.exception = ex;
        }
        saveStatement("\n#Eval background:  " + statement + "\n");
        afterEval(true);
        return ret;
    }

    private Thread evalThread;

    public Thread getEvalThread() {
        return evalThread;
    }

    public void abort() {
        logger.fine("Aborting");
        try {
            if (engine instanceof JepScriptEngine jse){
                jse.abort();
            } 
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

    public Object getScriptResult() {
        return lastScriptResult;
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
                if (type.isPython()) {
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
            if (engine instanceof JepScriptEngine jse){
                jse.close();
            }   
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }            
        for (AutoCloseable ac : new AutoCloseable[]{sessionOut, jythonClassFilePermissionMonitor}) {
            try {
                if (ac != null) {
                    ac.close();
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////
    //Class files permission handling
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void fixClassFilesPermissions() {
        fixClassFilesPermissions(libraryPath, scriptFilePermissions);
    }
    
    public static void  fixClassFilesPermissions(String[] path, FilePermissions permissions){
        Set<String> fixedPath = new HashSet<>();        
        for (String p: path){
            boolean subpath = false;
            for (String o: path){
                if ((o!=p) && (IO.isSubPath(p, o))){
                    subpath = true;
                }
            }
            if (!subpath){
                fixedPath.add(new File(p).getAbsolutePath());
            }
        }
                        
        Logger.getLogger(ScriptManager.class.getName()).warning("Fixing class files permissions to " + permissions + ": " + Str.toString(fixedPath));
        for (String p : fixedPath) {
            for (File file : IO.listFilesRecursive(p, new String[]{"class"})) {
                fixClassFilePermissions(file, permissions);
            }
        }
    }    

    public void startClassFilesPermissionsMonitoring() {
        Logger.getLogger(ScriptManager.class.getName()).warning("Starting class files permissions monitoring");
        if (jythonClassFilePermissionMonitor != null) {
            try {
                jythonClassFilePermissionMonitor.close();
            } catch (Exception ex) {
                Logger.getLogger(ScriptManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        jythonClassFilePermissionMonitor = new FileSystemWatch(libraryPath, new String[]{"class"}, true, false, false, -1 /*CLASS_FILE_CHECK_INTERVAL*/, (path, kind) -> {
                    logger.log(Level.INFO, "Setting class file readable to all: {0}", path);
                    fixClassFilePermissions(path.toFile());
                });
    }
    
    public void fixClassFilePermissions(File file) {
        fixClassFilePermissions(file, scriptFilePermissions);
    }

    public static void fixClassFilePermissions(File file, FilePermissions permissions) {
        try {
            if (permissions == FilePermissions.Default) {
                Logger.getLogger(ScriptManager.class.getName()).fine("Setting file readable: " + file.getAbsolutePath());
                file.setReadable(true, false);
            } else {
                Logger.getLogger(ScriptManager.class.getName()).fine("Setting file permissions to " + permissions + ": " + file.getAbsolutePath());
                IO.setFilePermissions( file, permissions);
            }
        } catch (Exception ex) {
            Logger.getLogger(ScriptManager.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    /**
     * Executes in different process so can be called when shutting down.
     */
    public void startFixClassFilesPermissions() {
        Process p = ProcessFactory.createProcess(ScriptManager.class, Arr.insert(libraryPath, scriptFilePermissions.toString(),0));
    }

    /**
     * This is for executing a push in a different process with ProcessFactory
     */
    public static void main(String[] args) {
        //ProcessFactory
        try {
            /*
            LogManager logManager = new LogManager(FilePermissions.Default);
            String logFileName = Sys.getUserHome() + "/ScriptManager.log";
            logManager.start(logFileName,-1);
            logManager.setLevel(Level.FINE);
            */            
            FilePermissions permissions = FilePermissions.valueOf(args[0]);
            String[] path = Arr.getSubArray(args, 1, args.length-1);
            fixClassFilesPermissions(path, permissions);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

}
