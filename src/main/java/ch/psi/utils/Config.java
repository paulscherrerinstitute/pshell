package ch.psi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Implements simple persistence to property files of Entity classes. Persists all public fields
 * defined in implementations, if translatable into String.
 */
public class Config extends ObservableBase<Config.ConfigListener> {

    /**
     * The listener interface for receiving configuration events.
     */
    public static interface ConfigListener {

        default void onLoad(Config config) {
        }

        default void onSaving(Config config) {
        }

        default void onSave(Config config) {
        }
    }

    public static final String ARRAY_SEPARATOR = "|";
    final Properties properties = new SortedProperties();
    String fileName;
    boolean fileSync;

    public Properties getProperties() {
        ArrayList<String> fieldNames = new ArrayList<>();
        for (Field f : getFields()) {
            fieldNames.add(f.getName());
        }
        ArrayList<String> removed = new ArrayList<>();
        String[] keys = properties.keySet().toArray(new String[0]);
        for (String key : keys) {
            if (!fieldNames.contains(key)) {
                removed.add(key);
            }
        }
        for (String key : removed) {
            properties.remove(key);
        }
        return properties;
    }

    public String getFileName() {
        return fileName;
    }

    protected Object convertStringToField(Class type, String str) {
        return fromString(type, str);
    }

    protected String convertFieldToString(Object val) {
        return toString(val);
    }

    public void updateFields() {
        for (Field f : getFields()) {
            try {
                String strVal = properties.getProperty(f.getName());
                Class type = f.getType();
                Object val = convertStringToField(type, strVal);
                if ((val != null) || (!type.isPrimitive())) {
                    f.set(this, val);
                } else {
                    Logger.getLogger(Config.class.getName()).warning("Config value for '" + f.getName() + "' was invalid: " + strVal + " [" + getFileName() + "]");
                }
            } catch (Exception ex) {
                Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean updateProperties() {
        boolean changed = false;
        for (Field f : getFields()) {
            try {
                String name = f.getName();
                Object val = f.get(this);

                String curStr = properties.getProperty(name);

                if ((val != null) && (curStr != null) && (Number.class.isAssignableFrom(val.getClass()))) {
                    if (curStr.startsWith("0x")) {
                        val = "0x" + Long.toHexString(((Number) val).longValue()).toUpperCase();
                    }
                }

                String valStr = convertFieldToString(val);
                if (!valStr.equals(properties.get(name))) {
                    properties.put(name, valStr);
                    changed = true;
                }
            } catch (Exception ex) {
                Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return changed;
    }

    public boolean changed() {
        for (Field f : getFields()) {
            try {
                String name = f.getName();
                String valStr = toString(f.get(this));
                if (!valStr.equals(properties.get(name))) {
                    return true;
                }
            } catch (Exception ex) {
                Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public List<Field> getFields() {
        ArrayList<Field> ret = new ArrayList<>();
        for (Field f : getClass().getFields()) {
            if (f.getDeclaringClass() != Config.class) {
                if (Modifier.isPublic(f.getModifiers())) {
                    if (!Modifier.isTransient(f.getModifiers())) {
                        ret.add(f);
                    }
                }
            }
        }
        return ret;
    }

    public Field getField(String name) {
        for (Field f : getFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    public void load(String fileName) throws IOException {
        backup = null;
        properties.clear();
        this.fileName = fileName;
        if (fileName != null) {
            File folder = new File(fileName).getParentFile();
            if (folder != null) {
                folder.mkdirs();
            }
            try (FileInputStream in = new FileInputStream(fileName)) {
                properties.load(in);
                updateFields();
                fileSync = true;
            } catch (Exception ex) {
                Logger.getLogger(Config.class.getName()).log(Level.INFO, null, ex);
            }
            for (ConfigListener listener : getListeners()) {
                listener.onLoad(this);
            }
            try {
                save();
            } catch (IOException ex) {
                this.fileName = null;
                throw ex;
            }
        }
    }

    public void save() throws IOException {
        if (fileName != null) {
            //To avoid changing the property file timestamp if no change has been made
            boolean changed = updateProperties() || !fileSync;
            if (changed) {
                for (ConfigListener listener : getListeners()) {
                    listener.onSaving(this);
                }
                try (FileOutputStream out = new FileOutputStream(fileName);) {
                    properties.store(out, null);
                }
                for (ConfigListener listener : getListeners()) {
                    listener.onSave(this);
                }
            }
            if (backup != null) {
                backup();
            }
        }
    }

    public void save(String fileName) throws IOException {
        this.fileName = fileName;
        save();
    }

    public String[] getKeys() {
        return properties.keySet().toArray(new String[0]);
    }

    public Object getValue(String key) {
        return properties.getProperty(key);
    }

    public static Object fromString(Class type, String str) {
        Method method = null;
        if (type != null) {
            if (type.isArray()) {
                try {
                    char aux = (char) ControlChar.US;
                    //TODO: "|" should convert to a array of 1 empty string, but actually is converted to an empty string
                    String[] tokens = str.split(Pattern.quote(ARRAY_SEPARATOR));
                    Object ret = Array.newInstance(type.getComponentType(), tokens.length);
                    for (int i = 0; i < tokens.length; i++) {
                        Array.set(ret, i, fromString(type.getComponentType(), tokens[i]));
                    }
                    return ret;
                } catch (Exception ex) {
                    return null;
                }

            }
            if (type == String.class) {
                return str;
            }
            if (type.isPrimitive()) {
                type = Convert.getWrapperClass(type);
            }

            try {
                if (Number.class.isAssignableFrom(type)) {
                    if (str.startsWith("0x")) {
                        str = Integer.valueOf(str.substring(2), 16).toString();
                    }
                }
                method = String.class.getMethod("valueOf", type);
                if (Modifier.isStatic(method.getModifiers())) {
                    return method.invoke(null, str);
                }
            } catch (Exception ex) {

            }

            for (String methodName : new String[]{"fromString", "valueOf"}) {
                try {
                    method = type.getMethod(methodName, String.class);
                    if (Modifier.isStatic(method.getModifiers())) {
                        return method.invoke(null, str);
                    }
                } catch (Exception ex) {
                }
            }
        }
        return null;
    }

    public static String toString(Object val) {
        if ((val != null) && (val.getClass().isArray())) {
            StringJoiner sj = new StringJoiner(ARRAY_SEPARATOR);
            int len = Array.getLength(val);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(val, i);
                String str = toString(item);
                sj.add(str);
            }
            return sj.toString();
        } else {
            return String.valueOf(val);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof Config)) {
            return false;
        }
        Config config = ((Config) obj);
        if ((config.getClass() != this.getClass())) {
            return false;
        }

        String[] myKeys = getKeys();
        String[] hisKeys = config.getKeys();
        if ((myKeys == null) || (hisKeys == null) || (myKeys.length != hisKeys.length)) {
            return false;
        }
        for (int i = 0; i < myKeys.length; i++) {
            String key = myKeys[i];
            if ((!key.equals(hisKeys[i]))
                    || (!getValue(key).equals(config.getValue(key)))) {
                return false;
            }
        }
        return true;
    }

    public Config copy() throws InstantiationException, IllegalAccessException {
        Config ret = getClass().newInstance();
        ret.fileName = this.fileName;
        ret.properties.putAll(this.properties);
        ret.fileSync = false;
        return ret;
    }

    public void copyFrom(Config config) {
        if ((config == null) || (config.getClass() != this.getClass())) {
            return;
        }
        properties.putAll(config.getProperties());
        updateFields();
        fileSync = false;
    }

    Properties backup;

    public void backup() {
        backup = new SortedProperties();
        backup.putAll(this.properties);
    }

    public void restore() throws InstantiationException, IllegalAccessException {
        if (backup != null) {
            properties.clear();
            properties.putAll(backup);
            updateFields();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field f : getFields()) {
            try {
                sb.append(f.getName()).append(" = ");
                sb.append(f.get(this)).append("\n");
            } catch (Exception ex) {
            }
        }
        return sb.toString();
    }

    static public boolean isStringDefined(String str) {
        return (str != null) && (!str.equals(String.valueOf((String) null)));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Defaults {

        public String[] values();
    }
}
