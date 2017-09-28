package ch.psi.pshell.core;

/**
 * Interface implemented by globally recognizable objects such as devices and image sources.
 */
public interface Nameable {

    public static String getShortClassName(Class cls) {
        String ret = cls.getSimpleName();
        //Inner classes...
        if (ret.isEmpty()) {
            ret = cls.getName();
            ret = ret.substring(ret.lastIndexOf(".") + 1);
        } else if (ret.startsWith("__builtin__")) {
            //Jython class            
            String[] tokens = ret.split("\\$");
            if (tokens.length > 1) {
                ret = tokens[1];
            }
        } else if (ret.contains("$$")) {
            ret = ret.substring(0, ret.indexOf("$$"));
        }
        return ret;
    }

    /**
     * Default returns short name of the class. Implementation should provide a more specific name.
     */
    default String getName() {
        return getShortClassName(getClass());
    }
}
