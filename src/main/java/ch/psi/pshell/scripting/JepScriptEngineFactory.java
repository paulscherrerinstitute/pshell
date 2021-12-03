package ch.psi.pshell.scripting;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

public class JepScriptEngineFactory implements ScriptEngineFactory {


    String[] libraryPath;
    public JepScriptEngineFactory(String[] libraryPath) {
        this.libraryPath = libraryPath;
    }

    @Override
    public String getEngineName() {
        return "jep";
    }

    @Override
    public String getEngineVersion() {
        return "1";
    }

    @Override
    public List getExtensions() {
        return Arrays.asList("py");
    }

    @Override
    public String getLanguageName() {
        return "CPython";
    }

    @Override
    public String getLanguageVersion() {
        return "2 or 3";
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String[] args) {
        String ret = obj;
        ret += "." + m + "(";
        for (int i = 0; i < args.length; i++) {
            ret += args[i];
            if (i == args.length - 1) {
                ret += ")";
            } else {
                ret += ",";
            }
        }
        return ret;
    }

    @Override
    public List getMimeTypes() {
        return new ArrayList();
    }

    @Override
    public List getNames() {
        return new ArrayList();
    }

    @Override
    public String getOutputStatement(String o) {
        return "print(o)";
    }

    @Override
    public Object getParameter(String p) {
        if (p !=null) {
            if (p.equals(ScriptEngine.ENGINE)) {
                return getEngineName();
            }
            if (p.equals(ScriptEngine.ENGINE_VERSION)) {
                return getEngineVersion();
            }
            if (p.equals(ScriptEngine.NAME)) {
                return getEngineName();
            }
            if (p.equals(ScriptEngine.LANGUAGE)) {
                return getLanguageName();
            }
            if (p.equals(ScriptEngine.LANGUAGE_VERSION)) {
                return getLanguageVersion();
            }
        }
        return null;
    }

    @Override
    public String getProgram(String[] lines) {
        return String.join("\n", lines);
    }

    @Override
    public ScriptEngine getScriptEngine() {
        try {
            JepScriptEngine e = new JepScriptEngine();
            e.setFactory(this);
            e.setContext(new SimpleScriptContext() {
                @Override
                public void setWriter(Writer writer) {
                    super.setWriter(writer);
                    try {
                        e.put("jep_stdout", writer);
                        e.eval("sys.stdout=jep_stdout");
                    } catch (Exception ex) {
                        Logger.getLogger(ScriptManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                @Override
                public void setErrorWriter(Writer writer) {
                    super.setErrorWriter(writer);
                    try {
                        e.put("jep_stderr", writer);
                        e.eval("sys.stderr=jep_stderr");
                    } catch (Exception ex) {
                        Logger.getLogger(ScriptManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                @Override
                public void setReader(Reader reader) {
                    super.setReader(reader);
                    try {
                        e.put("jep_stdout", reader);
                        e.eval("sys.stdout=jep_stdout");
                    } catch (Exception ex) {
                        Logger.getLogger(ScriptManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            try {
                if (libraryPath!=null){
                    e.eval("import sys");
                    for (String path : libraryPath) {
                        path = (new File(path)).getCanonicalPath();
                        Logger.getLogger(ScriptManager.class.getName()).info("Adding to Python Path: " + path);
                        e.eval("sys.path.append('" + path + "')");
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(ScriptManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            return e;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}
