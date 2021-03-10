package ch.psi.pshell.scripting;

/**
 * Script utilities.
 */
public class ScriptUtils {

    public static Class getType(String typeId) throws ClassNotFoundException {
        if (typeId == null) {
            return null;
        }
        switch (typeId) {
            case "b":
                return Byte.class;
            case "h":
                return Short.class;
            case "u":
                return Integer.class;
            case "i":
                return Integer.class;
            case "l":
                return Long.class;
            case "c":
                return Character.class;
            case "f":
                return Float.class;
            case "d":
                return Double.class;
            case "z":
                return Boolean.class;
            case "s":
                return String.class;
            case "o":
                return Object.class;
            case "[b":
                return byte[].class;
            case "[h":
                return short[].class;
            case "[u":
                return int[].class;
            case "[i":
                return int[].class;
            case "[l":
                return long[].class;
            case "[c":
                return char[].class;
            case "[f":
                return float[].class;
            case "[d":
                return double[].class;
            case "[z":
                return boolean.class;
            case "[s":
                return String[].class;
            case "[o":
                return Object[].class;
        }
        return Class.forName(typeId);
    }

    public static Class getPrimitiveType(String typeId) throws ClassNotFoundException {
        if (typeId == null) {
            return null;
        }
        switch (typeId) {
            case "b":
                return byte.class;
            case "h":
                return short.class;
            case "u":
                return int.class;
            case "i":
                return int.class;
            case "l":
                return long.class;
            case "c":
                return char.class;
            case "f":
                return float.class;
            case "d":
                return double.class;
            case "z":
                return boolean.class;
            case "s":
                return String.class;
            case "o":
                return Object.class;
            case "[b":
                return byte[].class;
            case "[h":
                return short[].class;
            case "[u":
                return int[].class;
            case "[i":
                return int[].class;
            case "[l":
                return long[].class;
            case "[c":
                return char[].class;
            case "[f":
                return float[].class;
            case "[d":
                return double[].class;
            case "[z":
                return boolean.class;
            case "[s":
                return String[].class;
            case "[o":
                return Object[].class;
        }
        return Class.forName(typeId);
    }
    
    public static boolean isStandardType(String typeId) {
        if (typeId != null) {
            try {
                Class type = getType(typeId);
                return (!type.getName().contains(typeId));
            } catch (Exception ex) {
            }
        }
        return false;
    }

}
