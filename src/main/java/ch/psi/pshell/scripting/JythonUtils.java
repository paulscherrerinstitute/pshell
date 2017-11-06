/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */
package ch.psi.pshell.scripting;

import java.util.ArrayList;
import java.util.List;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyType;

/**
 *
 */
public class JythonUtils {

    public static String PY_METHOD_UNK_RET = "<py>";
    public static String PY_METHOD_UNK_PARS = "...";

    public static Class[] REFLECTION_EXCLUDE_CLASSES = new Class[]{org.python.core.PyObject.class, org.python.core.PyInstance.class};
    public static String[] REFLECTION_EXCLUDE_NAMES = new String[]{"classDictInit", "clone", "finalize"}; //new String[]{ScriptManager.JYTHON_OBJ_CLASS}

    public static String getPyMethodSignature(PyObject obj, String method) {
        try {
            Object f = ((PyObject) obj).__findattr__(method);
            if ((f != null) && (f instanceof PyMethod)) {
                return getPyMethodSignature((PyMethod) f);
            } else if (f instanceof PyBuiltinMethod) {
                PyBuiltinMethod m = ((PyBuiltinMethod) f);
                String doc = (m.getDoc() == null) ? "" : m.getDoc().trim();
                if (doc.isEmpty()) {
                    return (method + "(" + PY_METHOD_UNK_PARS + ") " + PY_METHOD_UNK_RET);
                } else {
                    doc = doc.substring(doc.indexOf(method));
                    doc = doc.substring(0, doc.indexOf(")") + 1);
                    return (doc + " " + PY_METHOD_UNK_RET);
                }
            }
        } catch (Exception ex) {
        }
        return null;
    }

    public static String getPyMethodSignature(PyMethod m) {
        if ((m.__func__ != null) && (m.__func__.getType().getClass() == PyType.class)) {
            String name = null;
            List<String> pars = null;
            try {
                name = m.__func__.__findattr__("func_code").__findattr__("co_name").asString();
            } catch (Exception ex) {
                return null;
            }

            try {
                pars = (List) m.__func__.__findattr__("func_code").__findattr__("co_varnames");
                if (pars.size() > 0) {
                    //Remove self
                    pars = pars.subList(1, pars.size());
                    //Include defaults                                
                    Object def = m.__func__.__findattr__("__defaults__");
                    if ((def != null) && (def instanceof List) && (((List) def).size() > 0)) {
                        int named_args = m.__findattr__("func_code").__findattr__("co_argcount").asInt() - 1;
                        pars = new ArrayList(pars);
                        List defaults = (List) def;
                        int start_defaults = named_args - defaults.size();
                        for (int i = 0; i < defaults.size(); i++) {
                            pars.set(start_defaults + i, pars.get(start_defaults + i) + "=" + defaults.get(i));
                        }
                    }
                }
            } catch (Exception ex) {
                pars = new ArrayList();
                pars.add(PY_METHOD_UNK_PARS);
            }
            return name + "(" + String.join(", ", pars) + ") " + PY_METHOD_UNK_RET;
        }
        return null;
    }

    public static List<String> getSignatures(PyObject obj) {
        List<String> ret = new ArrayList<>();
        List<String> pythonMethods = (List) ((org.python.core.PyObject) obj).__dir__();
        for (String s : pythonMethods) {
            if (!s.startsWith("_")) {
                String signature = getPyMethodSignature(((org.python.core.PyObject) obj), s);
                if (signature != null) {
                    ret.add(signature);
                }
            }
        }
        return ret;
    }

}
