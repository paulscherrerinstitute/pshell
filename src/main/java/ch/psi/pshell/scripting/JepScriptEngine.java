package ch.psi.pshell.scripting;

import ch.psi.utils.Threading;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import jep.Jep;
import jep.SharedInterpreter;
import jep.python.PyCallable;

public class JepScriptEngine implements ScriptEngine, AutoCloseable, Compilable {     
    
    private Jep jep = null;

    private Bindings bindings = new SimpleBindings();

    private Bindings globalBindings = new SimpleBindings();

    private ScriptContext context = null;

    private ScriptEngineFactory factory = null;

    Thread interpreterThread;

    /**
     * Make a new JepScriptEngineF
     *
     * @throws ScriptException if an error occurs
     */
    public JepScriptEngine() throws ScriptException {
        try {
            // make interactive because javax.script sucks           
            interpreterExecutor = Executors.newSingleThreadExecutor((Runnable runnable) -> {
                interpreterThread = new Thread(Thread.currentThread().getThreadGroup(), runnable, "MainThread");
                return interpreterThread;
            });            
            runInInterpreterThread(() -> {
                //this.jep = new Jep(new JepConfig().setInteractive(false));
                //this.jep.setClassLoader(Thread.currentThread().getContextClassLoader());
                this.jep = new SharedInterpreter();    
                try {
                    //Reloading threading to set the main thread
                    eval("__aux__=sys.stdout", this.context, this.bindings); //No reload messages
                    eval("sys.stdout=None", this.context, this.bindings);
                    eval("import threading", this.context, this.bindings);
                    eval("import importlib", this.context, this.bindings);
                    eval("importlib.reload(threading)", this.context, this.bindings);
                    eval("sys.stdout=__aux__", this.context, this.bindings);
                } catch (Exception ex) {
                    Logger.getLogger(ScriptManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            });
        } catch (Exception e) {
            throw (ScriptException) new ScriptException(e.getMessage()).initCause(e);
        }
    }

    public boolean isInterpreterThread() {
        return (interpreterThread == Thread.currentThread());
    }

    ExecutorService interpreterExecutor;

    Object runInInterpreterThread(Callable callable) throws ScriptException {
        Object result;
        try {
            if (isInterpreterThread()) {
                result = callable.call();
            } else {
                try {
                    synchronized (interpreterExecutor) {
                        result = interpreterExecutor.submit(callable).get();
                    }
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof ScriptException) {
                        throw (ScriptException) ex.getCause();
                    }
                    if (ex.getCause() instanceof InterruptedException) {
                        throw (InterruptedException) ex.getCause();
                    }
                    if (ex.getCause() instanceof IOException) {
                        throw (IOException) ex.getCause();
                    } else {
                        throw ex;
                    }
                }
            }
            return result;
        } catch (ScriptException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ScriptException(ex);
        } catch (Throwable t) {
            Logger.getLogger(JepScriptEngine.class.getName()).log(Level.SEVERE, null, t); //Should never happen;
            return null;
        }
    }

    /**
     * Describe createBindings method here.
     *
     * @see javax.script.ScriptEngine#createBindings()
     *
     * @return a Bindings value
     */
    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    // me: lazy
    private void _setContext(ScriptContext c) throws ScriptException {
        try {
            this.jep.set("context", c);
        } catch (Exception e) {
            throw (ScriptException) new ScriptException(e.getMessage()).initCause(e);
        }
    }

    private String toString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(1024);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();        
    }    
    
    private Object eval(Reader reader, ScriptContext context, Bindings bindings) throws ScriptException {
        try {
            _setContext(context);
            String str = toString(reader);
            this.jep.eval(str);
            return null;
        } catch (IOException e) {
            throw new ScriptException("Error writing to file: " + e.getMessage());
        } catch (Exception e) {
            throw (ScriptException) new ScriptException(e.getMessage()).initCause(e);
        }
    }

    
    private Object eval(File file, ScriptContext context, Bindings bindings) throws ScriptException {
        try {
            _setContext(context);
            this.jep.runScript(file.getAbsolutePath());
            return null;
        } catch (Exception e) {
            throw (ScriptException) new ScriptException(e.getMessage()).initCause(e);
        }
    }
    

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return runInInterpreterThread(() -> eval(reader, this.context, this.bindings));
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return runInInterpreterThread(() -> eval(reader, context, this.bindings));
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        return runInInterpreterThread(() -> eval(reader, context, bindings));
    }
    
    public Object eval(File file) throws ScriptException {
        return runInInterpreterThread(() -> eval(file, this.context, this.bindings));
    }

    @Override
    public Object eval(String line) throws ScriptException {
        return runInInterpreterThread(() -> eval(line, this.context, this.bindings));
    }

    @Override
    public Object eval(String line, ScriptContext context) throws ScriptException {
        return runInInterpreterThread(() -> eval(line, context, this.bindings));
    }

    @Override
    public Object eval(String line, Bindings b) throws ScriptException {
        return runInInterpreterThread(() -> eval(line, this.context, b));
    }

    private Object eval(String line, ScriptContext context, Bindings b) throws ScriptException {
        return runInInterpreterThread(() -> {
            try {
                _setContext(context);
                jep.eval(line);
                return null;
            } catch (Exception e) {
                throw (ScriptException) new ScriptException(e.getMessage()).initCause(e);
            }
        });
    }

    @Override
    public ScriptEngineFactory getFactory() {
        try {
            return (ScriptEngineFactory) runInInterpreterThread((Callable<ScriptEngineFactory>) () -> {
                if (this.factory == null) {
                    this.factory = new JepScriptEngineFactory(new String[0]);
                }
                return this.factory;
            });
        } catch (Exception e) {
            return null;
        }            
    }

    protected void setFactory(ScriptEngineFactory fact) {
        try {
            runInInterpreterThread(() -> {this.factory = fact; return null;});
        } catch (Exception e) {
        }              
    }

    @Override
    public Object get(String name) {
        try {
            return runInInterpreterThread(() -> this.jep.getValue(name));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void put(String name, Object val) throws IllegalArgumentException {
        try {
            runInInterpreterThread(() -> {
                this.jep.set(name, val);
                return null;
            });
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Bindings getBindings(int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            return this.bindings;
        }

        return this.globalBindings;
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            this.bindings = bindings;
        }

        this.globalBindings = bindings;
    }

    @Override
    public ScriptContext getContext() {        
        return this.context;
    }

    @Override
    public void setContext(ScriptContext c) {
        try {
            runInInterpreterThread(() -> {
            this.context = c;
            _setContext(c);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }        
    }

    @Override
    public void close() {
        try {
            if (interpreterExecutor != null) {
                interpreterExecutor.shutdownNow();
                Threading.stop(interpreterThread, true, 3000);
            }
            this.jep.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompiledScript compile(final String string) throws ScriptException {
        return (CompiledScript) runInInterpreterThread((Callable<CompiledScript>)() -> {
            boolean eval = false;
            put("__code__", string);
            //eval("__comp__=compile(__code__, '<string>', 'single')");
            try{
                eval("__comp__=compile(__code__, '<string>', 'eval')");
                eval = true;
            } catch(Exception ex){
                eval("__comp__=compile(__code__, '<string>', 'exec')");
            }
            final boolean isEval = eval;
            return new CompiledScript() {
                @Override
                public Object eval(ScriptContext sc) throws ScriptException {
                    if (isEval){
                        getEngine().eval("__ret__=eval(__comp__, globals(), globals())");
                        Object ret = getEngine().get("__ret__");
                        return ret;
                    } else {
                        getEngine().eval("eval(__comp__, globals(), globals())");
                        return null;
                    }
                    /*
                    try{
                        getEngine().eval("__ret__=eval(__code__, globals(), globals())");
                        Object ret = getEngine().get("__ret__");
                        return ret;
                    } catch (ScriptException ex){
                        getEngine().eval("eval(__comp__, globals(), globals())");
                        return null;
                    }
                    */
                }
                @Override
                public ScriptEngine getEngine() {
                    return JepScriptEngine.this;
                }
            };
        });        
    }
    
    public List<String> getSignatures(Object obj, String name){        
        try {      
            return (List) runInInterpreterThread((Callable<List>)() -> {          
                List<String> ret = new ArrayList<>();
                //jep.python.PyCallable dir = (jep.python.PyCallable) ((jep.python.PyObject)obj).getAttr("__dir__");
                //List<String> attrs = (List<String>) dir.call();
                List<String> attrs = (List<String>) this.jep.getValue("dir("+name+")");
                for (String s: attrs){
                    try{
                        if (!s.startsWith("_")){
                            //jep.python.PyCallable f = (jep.python.PyCallable) ((jep.python.PyObject)obj).getAttr(s);
                            //String signature = getSignature(f);
                            String signature = getSignature(name, s);
                            if ((signature!=null) && !signature.startsWith("_")){
                                ret.add(signature);
                            }
                        }
                    } catch (Exception ex) {                        
                    }
                }
                Collections.sort(ret);
                return ret;
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(JepScriptEngine.class.getName()).log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }
    
    String getSignature(String object, String method){
        String ret = method;
        try {         
            return ret + this.jep.getValue("str(inspect.signature("+object+"."+method+"))");
        } catch (Exception ex) {
            return null;
        }                    
    }
    
    String getSignature(PyCallable f){
        try {            
                String ret = "";
                ret = (String) f.getAttr("__name__");
                put("__obj__", f);
                try {         
                    ret = ret + this.jep.getValue("str(inspect.signature(__obj__))");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }                    
                return ret;
        } catch (Exception e) {
            return null;
        }        
    }

    @Override
    public CompiledScript compile(Reader reader) throws ScriptException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
