package ch.psi.pshell.sequencer;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.scripting.JepScriptEngine;
import ch.psi.pshell.scripting.JythonUtils;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Observable;
import ch.psi.pshell.utils.Reflection;
import ch.psi.pshell.utils.Str;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author gobbo_a
 */
public class InterpreterUtils {
    static final boolean INCLUDE_IMMUTABLE_ATTRS_SIGNATURES = false;
    
    static boolean canCallInterpreter(){
        return (Context.getInterpreter().isThreaded() || Context.getState().isReady());
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
                obj = Context.getSequencer().getLastEvalResult();
            } else {
                if (propagate) {
                    String[] tokens = var.split("\\.");
                    try {
                        obj = Context.getSequencer().getInterpreterVariable(tokens[0]);
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
                    obj = Context.getSequencer().getInterpreterVariable(var);
                }
            }
            if ((obj != null) && (Convert.getPrimitiveClass(obj.getClass()) == null)) {
                return getSignatures(obj, var);
            }
        }
        return null;
    }
    
    static List<String> getSignatures(Object obj, String name) {
        if (obj instanceof org.python.core.PyObject po) {
            //Not parsed as normal java objects, must "dir" them
            return JythonUtils.getSignatures(po, true);
        } else if ((Context.getSequencer().getScriptType()==ScriptType.cpy)   &&  
                (!(obj.getClass().getName().startsWith("ch.psi")))) {
            //TODO: JepScriptEngine.getSignatures does not work for java objects, and 
            //wrapped objects as java.* and jep.* woll show different signatures then the python counbterpars.
            //Therefore decided to remove autocompletion of them (JepScriptEngine.getSignatures will fail). 
            //Use standard auto-complention for internal (ch.psi) objects because they match Python's.
            try {
                if (canCallInterpreter()){                                                 
                    return (List<String>)Context.getSequencer().runInInterpreterThread(null,(Callable<List<String>>)() ->{
                        return ((JepScriptEngine)Context.getInterpreter().getEngine()).getSignatures(
                                obj, (name.endsWith(".")) ? name.substring(0, name.length()-1)  : name);
                    });
                }
            } catch (Exception ex) {
                Logger.getLogger(InterpreterUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        } else {            
            Class[] excludeClasses = new Class[]{AutoCloseable.class, Observable.class, JPanel.class, JComponent.class, Container.class, Component.class};
            String[] excludeNames = new String[]{};
            if (Context.getSequencer().getScriptType() == ScriptType.py) {
                excludeClasses = Arr.append(excludeClasses, JythonUtils.REFLECTION_EXCLUDE_CLASSES);
                excludeNames = Arr.append(excludeNames, JythonUtils.REFLECTION_EXCLUDE_NAMES);
            }
            List<String> ret = Reflection.getMethodsSignature(obj, excludeClasses, excludeNames, true, true, true);
            //Proxies: included methods defined in python
            if (obj instanceof org.python.core.PyProxy pp) {
                ret.addAll(JythonUtils.getSignatures(pp._getPyInstance(), false));
            }
            if (INCLUDE_IMMUTABLE_ATTRS_SIGNATURES){
                ret.addAll(Reflection.getAttributesSignature(obj, excludeClasses, excludeNames, true, false, true));
            }
            return ret;
        }
    }
    
}
