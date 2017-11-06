package ch.psi.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Reflection utilities
 */
public class Reflection {

    /**
     * Returns a string with the representation of the signature of a method
     */
    public static String getMethodSignature(Method m, boolean showTypes, boolean showNames, boolean umlNotation) {
        StringBuilder sb = new StringBuilder();
        if (!umlNotation) {
            sb.append(m.getReturnType().getSimpleName()).append(" ");
        }
        sb.append(m.getName());
        sb.append("(");
        for (int j = 0; j < m.getParameterCount(); j++) {
            Parameter p = m.getParameters()[j];
            if (umlNotation) {
                if (showNames /*&& p.isNamePresent()*/) {
                    sb.append(p.getName());
                    if (showTypes) {
                        sb.append(":");
                    }
                }
                if (showTypes) {
                    sb.append(p.getType().getSimpleName());
                }
            } else {
                if (showTypes) {
                    sb.append(p.getType().getSimpleName());
                }
                if (showNames /*&& p.isNamePresent()*/) {
                    if (showTypes) {
                        sb.append(" ");
                    }
                    sb.append(p.getName());
                }
            }
            if (j < (m.getParameterCount() - 1)) {
                sb.append(", ");
            }
        }
        sb.append(")");
        if (umlNotation) {
            sb.append(" ").append(m.getReturnType().getSimpleName());
        }
        return sb.toString();
    }

    /**
     * Methods with this annotation are not reported in the public method parsing methods.
     */
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Hidden {
    }       //Methods hidden from user in online help

    /**
     * Parse all signatures of the public inerface of an object - excluding methods annotated with
     * Hidden.
     */
    public static ArrayList<String> getMethodsSignature(Object obj) {
        return getMethodsSignature(obj, null, null, true, true, true);
    }

    public static ArrayList<String> getMethodsSignature(Object obj, Class[] excludeClasses,
            String[] excludeNames, boolean showTypes, boolean showNames, boolean umlNotation) {
        ArrayList<String> methodsSignatures = new ArrayList<>();

        ArrayList<Class> superClasses = new ArrayList<>();
        Class parent = obj.getClass();

        while (parent != Object.class) {
            superClasses.add(parent);
            parent = parent.getSuperclass();
        }

        ArrayList<Method> methods = new ArrayList<>();
        for (Class type : superClasses) {
            for (Method method : type.getMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers)) {
                    try {
                        Hidden annotation = method.getAnnotation(Hidden.class);
                        if (annotation != null) {
                            continue;
                        }
                    } catch (Exception ex) {
                    }

                    boolean hideMethod = false;
                    Class declaringClass = method.getDeclaringClass();
                    if (method.getName().startsWith("_")) {
                        hideMethod = true;
                    } else {
                        if (excludeClasses != null) {
                            for (Class c : excludeClasses) {
                                try {
                                    hideMethod = (c.getMethod(method.getName(), method.getParameterTypes()) != null)
                                            || (declaringClass == c);
                                    if (hideMethod) {
                                        break;
                                    }
                                } catch (NoSuchMethodException ex) {
                                }
                            }
                        }
                        if (!hideMethod) {
                            if (excludeNames != null) {
                                for (String s : excludeNames) {
                                    hideMethod = declaringClass.getName().startsWith(s) || method.getName().contains(s);
                                    if (hideMethod) {
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (hideMethod == false) {
                        if ((declaringClass == type) || (declaringClass.isInterface())) {
                            boolean overriden = false;
                            for (Method m : methods) {
                                if (m.getName().equals(method.getName())) {
                                    if (m.getParameterCount() == method.getParameterCount()) {
                                        boolean different = false;
                                        for (int j = 0; j < m.getParameterCount(); j++) {
                                            if (m.getParameterTypes()[j] != method.getParameterTypes()[j]) {
                                                if (!method.getParameterTypes()[j].isAssignableFrom(m.getParameterTypes()[j])) {
                                                    different = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (different == false) {
                                            overriden = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!overriden) {
                                methods.add(method);
                            }
                        }
                    }
                }
            }
        }

        for (Method m : methods) {
            methodsSignatures.add(getMethodSignature(m, showTypes, showNames, umlNotation));
        }
        Collections.sort(methodsSignatures, Collator.getInstance());

        return methodsSignatures;
    }
}
