package ch.psi.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
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
     * Methods with this annotation are not reported in the public method
     * parsing methods.
     */
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Hidden {
    }       //Methods hidden from user in online help

    /**
     * Parse all signatures of the public interface of an object - excluding
     * methods annotated with Hidden.
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
                            for (Class cls : excludeClasses) {
                                if ((declaringClass == cls)) {
                                    hideMethod = true;
                                    break;
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

    /**
     * Returns a string with the representation of the signature of an attribute
     */
    public static String getAttributeSignature(Field f, boolean umlNotation) {
        StringBuilder sb = new StringBuilder();
        if (!umlNotation) {
            sb.append(f.getType().getSimpleName()).append(" ");
        }
        sb.append(f.getName());
        if (umlNotation) {
            sb.append(" :").append(f.getType().getSimpleName());
        }
        return sb.toString();
    }

    /**
     * Parse all public attributes of an object - attributes methods annotated
     * with Hidden.
     */
    public static ArrayList<String> getAttributesSignature(Object obj, Class[] excludeClasses,
            String[] excludeNames, boolean immutable, boolean classVariables, boolean umlNotation) {
        ArrayList<String> attibutesSignatures = new ArrayList<>();

        ArrayList<Class> superClasses = new ArrayList<>();
        Class type = obj.getClass();

        ArrayList<Field> fields = new ArrayList<>();
        for (Field field : type.getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && ((!immutable) || Modifier.isFinal(modifiers)) && ((classVariables) || !Modifier.isStatic(modifiers))) {
                try {
                    Hidden annotation = field.getAnnotation(Hidden.class);
                    if (annotation != null) {
                        continue;
                    }
                } catch (Exception ex) {
                }

                boolean hideField = false;
                Class declaringClass = field.getDeclaringClass();
                if (field.getName().startsWith("_")) {
                    hideField = true;
                } else {
                    if (excludeClasses != null) {
                        for (Class cls : excludeClasses) {
                            if ((declaringClass == cls)) {
                                hideField = true;
                                break;
                            }
                        }
                    }
                    if (!hideField) {
                        if (excludeNames != null) {
                            for (String s : excludeNames) {
                                hideField = declaringClass.getName().startsWith(s) || field.getName().contains(s);
                                if (hideField) {
                                    break;
                                }
                            }
                        }
                    }
                }

                if (hideField == false) {
                    fields.add(field);
                }
            }
        }

        for (Field f : fields) {
            attibutesSignatures.add(getAttributeSignature(f, umlNotation));
        }
        Collections.sort(attibutesSignatures, Collator.getInstance());

        return attibutesSignatures;
    }

    public static Class getDeclaredClass(Class cls, String name){
        for (Class c: cls.getDeclaredClasses()){
            if (c.getSimpleName().equals(name)){
                return c;
            }
        }
        return null;
    }
}
