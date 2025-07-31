package ch.psi.pshell.utils;

import java.util.HashMap;

/**
 * Interface implemented by globally recognizable objects such as devices and image sources.
 */
public interface Nameable {
    //Default implementation not great,  keeping object references in the map
    final HashMap<Nameable, String> aliases = new HashMap<>();
    public static void clear(){
        aliases.clear();
    }

    public static String getShortClassName(Class cls) {
        String ret = cls.getSimpleName();
        //Inner classes...
        if (ret.isEmpty()) {
            ret = cls.getName();
            ret = ret.substring(ret.lastIndexOf(".") + 1);
        } else if (ret.startsWith("__main__") || ret.startsWith("builtin_classes$")) {
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

    default void setAlias(String alias) {
        if (alias != null) {
            aliases.put(this, alias);
        } else {
            aliases.remove(this);
        }
    }

    default String getAlias() {
        if (aliases.containsKey(this)) {
            return aliases.get(this);
        }
        return getName();
    }

    /**
     * Default returns short name of the class. Implementation should provide a more specific name.
     */
    default String getName() {
        return getShortClassName(getClass());
    }
}
