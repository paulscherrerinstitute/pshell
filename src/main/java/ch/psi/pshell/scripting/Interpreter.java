package ch.psi.pshell.scripting;

import ch.psi.utils.Str;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class Interpreter {

    final ScriptEngine engine;
    final Bindings bindings;
    final StringBuilder sb;
    final ScriptType type;
    int lineNumber;
    int statementNumber;
    int statementLineCount;
    ScriptException statementException;

    public Interpreter(ScriptEngine engine, ScriptType type, Bindings bindings) {
        this.engine = engine;
        this.type = type;
        this.bindings = bindings;
        sb = new StringBuilder();
        lineNumber = 0;
        statementNumber = 1;
        reset();
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public void reset() {
        sb.setLength(0);
        statementLineCount = 0;
        statementException = null;
    }

    public int getStatementLineCount() {
        return statementLineCount;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getStatementNumber() {
        return statementNumber;
    }

    public ScriptException getStatementException() {
        return statementException;
    }

    public InterpreterResult interpret(String line) {
        return interpret(line, false);
    }

    public InterpreterResult interpret(String line, boolean batch) {
        InterpreterResult ret = new InterpreterResult();
        if (line != null) {
            boolean firstLine = (statementLineCount == 0);
            boolean empty = line.isEmpty();
            boolean finishedCompositeStatement = false;

            if (line.trim().isEmpty()) {
                //An empty line marks the final of a composite statement
                if ((firstLine) || (!line.isEmpty())) {
                    reset();
                    ret.statement = line;
                    return ret;
                }
            }

            if (!firstLine) {
                finishedCompositeStatement = empty;
                if (!empty) {
                    sb.append("\n");
                }
            }

            if (!finishedCompositeStatement) {
                if (!batch) {
                    lineNumber++;
                }
                statementLineCount++;
                sb.append(line);
            }

            String statement = sb.toString();
            ret.statement = statement;

            //Only try to compile composite statement again after the empty string
            if ((!firstLine) && (!finishedCompositeStatement)) {
                ret.correct = true; //Maybe not...                
            } else {
                CompiledScript cs = null;
                try {
                    cs = tryCompiling(statement, firstLine);
                    ret.correct = true;
                    if (cs == null) {
                        //A correct partial statement
                        ret.complete = false;
                    } else {
                        try {
                            statementNumber++;
                            ret.result = cs.eval(bindings);
                            return ret;
                        } finally {
                            reset();
                        }
                    }
                } catch (ScriptException ex) {
                    statementNumber++;
                    ret.exception = ex;
                } catch (Exception e) {
                    statementNumber++;
                    ret.exception = new ScriptException(e);
                }
            }
        }
        return ret;
    }
    
    public Object evalFile(String fileName) throws ScriptException, IOException {
        reset();
        lineNumber++;
        if (type==ScriptType.cpy){
            return ((JepScriptEngine)engine).eval(new File(fileName));
        }
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName))) {
            return engine.eval(reader);
        }
    }
        
    public boolean isValidCompleteStatement(String string) {
        try {
            return (tryCompiling(string, true) != null);
        } catch (ScriptException ex) {
            return false;
        }
    }

    public boolean isValidPartialStatement(String string) {
        try {
            return (tryCompiling(string, true) == null);
        } catch (ScriptException ex) {
            return false;
        }
    }

    public CompiledScript tryCompiling(String string, boolean tryFilterException) throws ScriptException {
        CompiledScript result = null;
        try {
            Compilable c = (Compilable) engine;
            result = c.compile(string);
            statementException = null;
        } catch (ScriptException se) {

            boolean rethrow = true;
            if ((tryFilterException) && (se.getCause() != null)) {
                String[] lines = string.split("\n", -1); //limit=-1 to add final empty string, if present
                int lineCount = lines.length;
                int line = se.getLineNumber();
                int col = se.getColumnNumber();
                
                switch (type) {
                    case js:
                        Throwable cause = se.getCause();
                        if (cause != null) {
                            if (cause.getClass().getSimpleName().equals("ParserException")) {
                                try{
                                    Integer pos = (Integer) cause.getClass().getMethod("getPosition").invoke(cause);
                                    String type = cause.getClass().getMethod("getErrorType").invoke(cause).toString();
                                    if (type.equals("SYNTAX_ERROR")) {
                                         if (pos == string.length()) {
                                             rethrow = false;
                                         } else if (string.contains("/*") && !string.contains("*/")) {
                                             rethrow = false;
                                         }
                                    }
                                } catch (Exception e){                                    
                                }                            
                            }
                        }
                        break;
                    case py:
                        if (se.getCause() instanceof org.python.core.PySyntaxError) {                           
                            org.python.core.PySyntaxError pex = (org.python.core.PySyntaxError) se.getCause();
                            int lastUsefulLine = 1;
                            for (int i = lines.length; i > 0; i--) {
                                String aux = lines[i - 1].trim();
                                if ((!aux.isEmpty()) && (!aux.startsWith("#"))) {
                                    lastUsefulLine = i;
                                    break;
                                }
                            }        
                            if ((line == lastUsefulLine) || ((line == lines.length))) {
                                String lastLine = Str.trimRight(lines[line - 1]);
                                int indexComment = lastLine.indexOf("#");
                                if (indexComment >= 0) {
                                    lastLine = Str.trimRight(lastLine.substring(0, indexComment));
                                }

                                if (lastLine.endsWith("\\")) {       //Remove \ character if present
                                    lastLine = Str.trimRight(lastLine.substring(0, lastLine.length() - 1));
                                }
                                //When find """ in the beggining of the line, then ignoring.
                                String aux = lastLine.trim();
                                if (aux.contains("\"\"\"") || aux.contains("'''") || aux.startsWith("@")) {
                                    rethrow = false;
                                }

                                //Partial parsing exception will have col = length of last line of statement
                                if (col >= (lastLine.length() - 1)) {
                                    rethrow = false;
                                } //An Exception: in some assignments such as
                                //class c:    
                                //    def reshape(self, newshape):
                                //        try:
                                //            self.out=self  
                                //        except AttributeError:
                                ////           self.out = self                                
                                else if (col > 1) {
                                    if (lastLine.substring(0, col).trim().endsWith("=")) {
                                        rethrow = false;
                                    }
                                }
                            }
                        }
                        break;
                    case cpy:
                      if (se.getCause() instanceof jep.JepException) {   
                            jep.JepException jex = (jep.JepException)se.getCause();                            
                            String exm=se.getMessage();                           
                            String m="'<stdin>',";
                            String[] tokens = exm.substring(exm.indexOf(m)+m.length()).split(", ");
                            
                            try{
                                //line = Integer.valueOf(tokens[0].trim());
                                col = Integer.valueOf(tokens[1].trim());
                            }catch (Exception ex){            
                            }
                            
                                                     
                         int lastUsefulLine = 1;
                            for (int i = lines.length; i > 0; i--) {
                                String aux = lines[i - 1].trim();
                                if ((!aux.isEmpty()) && (!aux.startsWith("#"))) {
                                    lastUsefulLine = i;
                                    break;
                                }
                            }    
                            if ((line == lastUsefulLine) || ((line == lines.length))|| ((line ==-1))) {
                                if (line==-1){
                                    line = lines.length;
                                }
                                String lastLine = Str.trimRight(lines[line - 1]);
                                int indexComment = lastLine.indexOf("#");
                                if (indexComment >= 0) {
                                    lastLine = Str.trimRight(lastLine.substring(0, indexComment));
                                }

                                if (lastLine.endsWith("\\")) {       //Remove \ character if present
                                    lastLine = Str.trimRight(lastLine.substring(0, lastLine.length() - 1));
                                }
                                //When find """ in the beggining of the line, then ignoring.
                                String aux = lastLine.trim();
                                if (aux.contains("\"\"\"") || aux.contains("'''") || aux.startsWith("@")) {
                                    rethrow = false;
                                }

                                //Partial parsing exception will have col = length of last line of statement
                                if (col >= (lastLine.length() - 1)) {
                                    rethrow = false;
                                } 
                                else if (col > 1) {
                                    if (lastLine.substring(0, col).trim().endsWith("=")) {
                                        rethrow = false;
                                    }
                                }
                                
                                if (exm.contains("IndentationError")){
                                    rethrow = false;
                                }
                            }
                        }
                        break;
                    case groovy:
                        if (se.getCause() instanceof org.codehaus.groovy.control.CompilationFailedException) {
                            org.codehaus.groovy.control.CompilationFailedException pex = (org.codehaus.groovy.control.CompilationFailedException) se.getCause();
                            String str = pex.toString();
                            line = Integer.valueOf(str.substring(str.indexOf("@ line") + 6).split(",")[0].trim());
                            if (line == lineCount) {
                                rethrow = false;
                            }
                        }
                        break;
                }
            }
            if (rethrow) {
                reset();
                throw se;
            } else {
                if (statementException != null) {
                    statementException = se;
                }
            }
        }
        return result;
    }    
}
