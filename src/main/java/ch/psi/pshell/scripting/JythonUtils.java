package ch.psi.pshell.scripting;

import java.util.ArrayList;
import java.util.List;
import org.python.core.ClassDictInit;
import org.python.core.PyArray;
import org.python.core.PyBuiltinCallable;
import org.python.core.PyFunction;
import org.python.core.PyJavaType;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyReflectedFunction;
import org.python.core.PyType;
import org.python.core.ReflectedArgs;

/**
 *
 */
public class JythonUtils {

    public static String PY_METHOD_UNK_RET = "*";
    public static String PY_METHOD_UNK_PARS = "...";

    public static Class[] REFLECTION_EXCLUDE_CLASSES = new Class[]{org.python.core.PyObject.class, org.python.core.PyInstance.class};
    //public static String[] REFLECTION_EXCLUDE_NAMES = new String[]{"classDictInit", "clone", "finalize"}; //new String[]{ScriptManager.JYTHON_OBJ_CLASS}
    public static String[] REFLECTION_EXCLUDE_NAMES = new String[]{ScriptManager.JYTHON_OBJ_CLASS};

    public static String getPyMethodSignature(PyObject obj, String method, boolean includeJavaMethods) {
        try {
            for (String exclude : REFLECTION_EXCLUDE_NAMES) {
                if (method.contains(exclude)) {
                    return null;
                }
            }
            Object f = ((PyObject) obj).__findattr__(method);
            if (f != null) {
                if (f instanceof PyMethod) {
                    return getPyMethodSignature((PyMethod) f, includeJavaMethods);
                } else if (f instanceof PyFunction) {
                    return getFunctionSignature((PyFunction) f, includeJavaMethods);
                } else if (f instanceof PyBuiltinCallable) {
                    return getPyBuiltinCallableSignature(method, ((PyBuiltinCallable) f));
                } else if (f instanceof PyReflectedFunction) {
                    return getFunctionSignature((PyReflectedFunction) f, includeJavaMethods);
                    //return getReflectedFunctionSignature((PyReflectedFunction) f);
                }
            }
        } catch (Exception ex) {
        }
        return null;
    }

    static String getPyMethodSignature(PyMethod m, boolean includeJavaMethods) {
        return getFunctionSignature(m.__func__, includeJavaMethods);
    }

    static String getPyBuiltinCallableSignature(String method, PyBuiltinCallable m) {
        String doc = (m.getDoc() == null) ? "" : m.getDoc().trim();
        if (doc.isEmpty()) {
            return (method + "(" + PY_METHOD_UNK_PARS + ") " + PY_METHOD_UNK_RET);
        } else {
            doc = doc.substring(doc.indexOf(method));
            doc = doc.substring(0, doc.indexOf(")") + 1);
            return (doc + " " + PY_METHOD_UNK_RET);
        }
    }

    static String getFunctionSignature(PyObject f, boolean includeJavaMethods) {
        if (f != null) {
            String name = null;
            List<String> pars = new ArrayList();
            try {
                if (f.getType().getClass() == PyType.class) {
                    PyObject code = f.__findattr__("func_code");
                    int flags;
                    int named_args;
                    try {
                        name = code.__findattr__("co_name").asString();
                        pars = (List) code.__findattr__("co_varnames");
                        flags = code.__findattr__("co_flags").asInt();
                        named_args = code.__findattr__("co_argcount").asInt();
                    } catch (Exception ex) {
                        return null;
                    }
                    if (pars.size() > 0) {
                        int args = named_args;
                        //*arg 
                        if ((flags & 0x04) > 0) {
                            args++;
                        }
                        //**arg 
                        if ((flags & 0x08) > 0) {
                            args++;
                        }
                        args = Math.min(args, pars.size());

                        //Remove self
                        pars = pars.subList(1, args);
                        args--;
                        
                        //Include defaults                                
                        Object def = f.__findattr__("__defaults__");
                        if ((def != null) && (def instanceof List) && (((List) def).size() > 0)) {
                            pars = new ArrayList(pars);
                            List defaults = (List) def;
                            int start_defaults = named_args - 1 - defaults.size();
                            for (int i = 0; i < defaults.size(); i++) {
                                Object defaultValue = defaults.get(i);
                                if (defaultValue == null){
                                    defaultValue = "None";
                                } else if (defaultValue instanceof String){
                                    defaultValue = "'" + defaultValue + "'";
                                }                                
                                pars.set(start_defaults + i, pars.get(start_defaults + i) + "=" + defaultValue);
                            }
                        }
                    }
                } else if (f.getType().getClass() == PyJavaType.class) {
                    if (!includeJavaMethods) {
                        return null;
                    }
                    try {
                        name = f.__findattr__("__name__").asString();
                    } catch (Exception ex) {
                        return null;
                    }
                    for (Class cls : new Class[]{Object.class, AutoCloseable.class, ClassDictInit.class}) {
                        for (java.lang.reflect.Method m : cls.getMethods()) {
                            if (m.getName().equals(name)) {
                                return null;
                            }
                        }
                    }
                    List args = (List) (((PyArray) f.__findattr__("argslist")).tolist());
                    for (int i = 0; i < args.size(); i++) {
                        ReflectedArgs ra = (ReflectedArgs) args.get(i);
                        if ((ra != null) && (ra.args != null) && (ra.args.length > 0)) {
                            for (int j = 0; j < ra.args.length; j++) {
                                pars.add((ra.args)[j].getSimpleName());
                            }
                        }
                        break;//Only showing first overload
                    }                    
                    if (f instanceof PyReflectedFunction){
                        //TODO: Didn't manage to resolve the return type for PyReflectedFunction
                    } 
                    return name + "(" + String.join(", ", pars) + ") " + PY_METHOD_UNK_RET;
                } else {
                    return null;
                }

            } catch (Exception ex) {
                pars.clear();
                pars.add(PY_METHOD_UNK_PARS);
            }
            return name + "(" + String.join(", ", pars) + ") " + PY_METHOD_UNK_RET;
        }
        return null;
    }

    public static List<String> getSignatures(PyObject obj, boolean includeJavaMethods) {
        List<String> ret = new ArrayList<>();
        List<String> pythonMethods = (List) obj.__dir__();
        for (String s : pythonMethods) {
            if (!s.startsWith("_")) {
                String signature = getPyMethodSignature(obj, s, includeJavaMethods);
                if (signature != null) {
                    ret.add(signature);
                }
            }
        }
        return ret;
    }

}
