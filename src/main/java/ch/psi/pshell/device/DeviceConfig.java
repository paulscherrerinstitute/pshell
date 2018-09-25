package ch.psi.pshell.device;

import ch.psi.utils.Config;
import ch.psi.pshell.scripting.JythonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity class storing the persisted device configuration. 
 * Handles config defined as a Map and config classes defined in Jython.
 */
public class DeviceConfig extends Config {

    private Map mapProperties;

    public DeviceConfig() {
        super();
        mapProperties = null;
    }

    public DeviceConfig(Map properties) {
        this.mapProperties = properties;
    }

    boolean isJythonObj() {
        return this instanceof org.python.core.PyProxy;
    }

    @Override
    public Defaults getDefaults(String key) throws NoSuchFieldException {
        if (isJythonObj() || (mapProperties != null)) {
            return null;
        }
        return super.getDefaults(key);
    }

    Map<String, Object> getJythonFieldDict() {
        if (this instanceof org.python.core.PyProxy) {
            return JythonUtils.getFields(((org.python.core.PyProxy) this)._getPyInstance());
        }
        return null;
    }

    @Override
    public List<String> getFieldNames() {
        if (isJythonObj()) {
            Map<String, Object> fields = getJythonFieldDict();
            return fields == null ? new ArrayList<>() : new ArrayList<>(fields.keySet());
        } else if (mapProperties != null) {
            List<String> ret = new ArrayList<>();
            ret.addAll(mapProperties.keySet());            
            return ret;
        }
        return super.getFieldNames();
    }

    @Override
    public Object getFieldValue(String name) {
        if (isJythonObj()) {
            Map<String, Object> fields = getJythonFieldDict();
            return fields == null ? null : fields.get(name);
        } else if (mapProperties != null) {
            return mapProperties.get(name);
        }
        return super.getFieldValue(name);
    }

    @Override
    public Class getFieldType(String name) {
        if (isJythonObj() || (mapProperties != null)) {
            Object val = getFieldValue(name);
            return (val == null) ? null : val.getClass();
        }
        return super.getFieldType(name);
    }

    @Override
    public void updateFields() {
        if (isJythonObj() || (mapProperties != null)) {
            for (String name : getFieldNames()) {
                try {
                    String strVal = _getProperties().getProperty(name);
                    Object val = getFieldValue(name);
                    Class type = val.getClass();
                    val = convertStringToField(type, strVal);
                    if ((val != null) || (!type.isPrimitive())) {
                        if (mapProperties == null) {
                            Map<String, Object> fields = getJythonFieldDict();
                            JythonUtils.setField(((org.python.core.PyProxy) this)._getPyInstance(), name, val);
                        } else {
                            mapProperties.put(name, val);
                        }
                    } else {
                        Logger.getLogger(Config.class.getName()).warning("Config value for '" + name + "' was invalid: " + strVal + " [" + getFileName() + "]");
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return;
        }
        super.updateFields();
    }

    @Override
    public boolean updateProperties() {
        if (isJythonObj() || (mapProperties != null)) {
            boolean changed = false;
            for (String name : getFieldNames()) {
                try {
                    Object val = getFieldValue(name);
                    String curStr = _getProperties().getProperty(name);

                    if ((val != null) && (curStr != null) && (Number.class.isAssignableFrom(val.getClass()))) {
                        if (curStr.startsWith("0x")) {
                            val = "0x" + Long.toHexString(((Number) val).longValue()).toUpperCase();
                        }
                    }

                    String valStr = convertFieldToString(val);
                    if (!valStr.equals(_getProperties().get(name))) {
                        _getProperties().put(name, valStr);
                        changed = true;
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return changed || isJythonObj(); //Always flag as changed, because cannot copy it  (in-place editing)
        }
        return super.updateProperties();
    }

    @Override
    public boolean changed() {
        if (isJythonObj() || (mapProperties != null)) {
            for (String name : getFieldNames()) {
                try {
                    String valStr = toString(getFieldValue(name));
                    if (!valStr.equals(_getProperties().get(name))) {
                        return true;
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return false;
        }
        return super.changed();
    }

    @Override
    public Config copy() throws InstantiationException, IllegalAccessException {
        Config ret = super.copy();
        ((DeviceConfig) ret).mapProperties = this.mapProperties;
        return ret;
    }

    @Override
    public void copyFrom(Config config) {
        super.copyFrom(config);
        if (config instanceof DeviceConfig) {
            mapProperties = ((DeviceConfig) config).mapProperties;
        }
    }
}
