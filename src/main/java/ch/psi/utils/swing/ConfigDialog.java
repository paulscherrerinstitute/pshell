package ch.psi.utils.swing;

import ch.psi.utils.Config;
import ch.psi.utils.Config.Defaults;
import ch.psi.utils.Convert;
import ch.psi.utils.Str;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A dialog for editing ch.psi.utils.Config structures.
 */
public class ConfigDialog extends PropertiesDialog {

    Config config;
    Config original;

    public ConfigDialog(Frame parent, boolean modal) {
        super(parent, modal);
    }

    public void setConfig(Config config) {
        try {
            this.config = config.copy();
            this.original = config;
        } catch (Exception ex) {
            //If cannot copy then work in place
            this.config = config;
            this.original = null;
            Logger.getLogger(ConfigDialog.class.getName()).log(Level.WARNING, "Editing config in place: " + config.getClass().getSimpleName());
        }
        super.setProperties(this.config.getProperties());
    }

    public Config getConfig() {
        return config;
    }

    public void setDisplayNames(HashMap<String, String> names) {
        ((ConfigPanel) propertiesPanel).setDisplayNames(names);
    }

    public void setDisplayName(String fieldName, String displayName) {
        ((ConfigPanel) propertiesPanel).setDisplayName(fieldName, displayName);
    }

    @Override
    protected PropertiesPanel newPropertiesPanel() {
        ConfigPanel panel = new ConfigPanel();
        panel.config = config;
        panel.setDefaultDisplayNames();
        return panel;
    }

    /**
     * The panel performing the elements edition. Can be added to other dialogs than
     * PropertiesDialog.
     */
    public static class ConfigPanel extends PropertiesPanel {

        Config config;
        final HashMap<String, String> displayNames = new HashMap<>();

        public void setConfig(Config config) {
            this.config = config;
            this.setProperties(config.getProperties());
            setDefaultDisplayNames();
        }

        void setDefaultDisplayNames() {
            displayNames.clear();
            for (String key : config.getKeys()) {
                String displayText = Str.toTitleCase(key);
                displayNames.put(key, displayText);
            }
        }

        public void setDisplayNames(HashMap<String, String> names) {
            displayNames.clear();
            displayNames.putAll(names);
        }

        public void setDisplayName(String fieldName, String displayName) {
            displayNames.put(fieldName, displayName);
        }

        @Override
        protected Object getPropertyValue(String key) {
            String value = String.valueOf(super.getPropertyValue(key));
            try {
                Field f = config.getField(key);
                if (f != null) {
                    Class type = f.getType();
                    for (Class c : new Class[]{Boolean.class, Double.class, Float.class, Long.class, Integer.class, Short.class, Byte.class}) {
                        if ((type == c) || (type == Convert.getPrimitiveClass(c))) {
                            return c.getMethod("valueOf", new Class[]{String.class}).invoke(null, new Object[]{value});
                        }
                    }
                    if (type.isEnum()) {
                        return Enum.valueOf(type, value);
                    }
                }
            } catch (Exception ex) {
            }
            return value;
        }

        @Override
        protected String getPropertyText(int row) {
            String key = getKey((String) table.getValueAt(row, 0));
            Object value = table.getValueAt(row, 1);
            return (value == null) ? "" : String.valueOf(value);
        }

        @Override
        protected String getEditorValue(Component c) {
            if (c instanceof JTextField) {
                return ((JTextField) c).getText();
            }
            if (c instanceof JComboBox) {
                return String.valueOf(((JComboBox) c).getSelectedItem());
            }
            if (c instanceof JCheckBox) {
                return String.valueOf(((JCheckBox) c).isSelected());
            }
            return null;
        }

        @Override
        protected DefaultCellEditor getPropertyEditor(String key) {
            DefaultCellEditor ret = super.getPropertyEditor(key);
            try {
                Field f = config.getClass().getField(key);
                Defaults defaults = f.getAnnotation(Defaults.class);
                if (defaults != null) {
                    Object value = getPropertyValue(key);
                    String[] values = defaults.values();
                    JComboBox editor = new JComboBox();
                    editor.setModel(new DefaultComboBoxModel(values));
                    editor.setEditable(true);
                    editor.setSelectedItem(value);
                    editor.setPreferredSize(new Dimension(editor.getPreferredSize().width, ret.getComponent().getPreferredSize().height));
                    ret = new DefaultCellEditor(editor);
                }

            } catch (Exception ex) {

            }
            return ret;
        }

        @Override
        protected Component getPropertyRenderer(String key, Object value, boolean changed, Color backColor, Component defaultComponent) {
            DefaultCellEditor ret = super.getPropertyEditor(key);
            try {
                Field f = config.getClass().getField(key);
                Defaults defaults = f.getAnnotation(Defaults.class);
                if (defaults != null) {
                    String[] values = defaults.values();
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    JComboBox cb = new JComboBox();
                    cb.setModel(new DefaultComboBoxModel(values));
                    cb.setEditable(true);
                    cb.setSelectedItem(value);
                    if (changed) {
                        cb.setFont(cb.getFont().deriveFont(Font.BOLD));
                    }
                    panel.add(cb);
                    return panel;
                }

            } catch (Exception ex) {

            }
            return super.getPropertyRenderer(key, value, changed, backColor, defaultComponent);
        }

        @Override
        protected String getDisplayName(String key) {
            String ret = displayNames.get(key);
            if (ret != null) {
                return ret;
            }
            return key;
        }

        @Override
        protected String getKey(String displayName) {
            for (String key : displayNames.keySet()) {
                if (displayNames.get(key).equals(displayName)) {
                    return key;
                }
            }
            return displayName;
        }
    }

    @Override
    public void accept() {
        config.updateFields(); //Because save is based on the field values, not properties   
        if (original != null) {
            if (config.equals(original)) {
                cancel();
                return;
            } else {
                original.copyFrom(config);
            }
        }
        super.accept();
    }
}
