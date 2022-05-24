package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.swing.CodeEditor;
import ch.psi.pshell.swing.ScriptEditor;
import ch.psi.pshell.xscan.ProcessorXScan;
import ch.psi.utils.Convert;
import ch.psi.utils.Str;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;

/**
 *
 */
public class EditablePanel<T> extends javax.swing.JPanel implements EditableComponent, ObjectProvider<T> {

    final T target;
    protected boolean modified = false;
    private final List<PanelSupport> panelSupportList = new ArrayList<>();
    private static Object DEFAULT_VALUE_ON_ERROR = new Object();

    protected EditablePanel() {
        this(null);
    }

    protected EditablePanel(T target) {
        super();
        this.target = target;
    }

    public PanelSupport getPanelSupport() {
        return getPanelSupport(false);
    }

    private PanelSupport getPanelSupport(boolean create) {
        if (create || (panelSupportList.size() == 0)) {
            PanelSupport panelSupport = new PanelSupport();
            panelSupportList.add(panelSupport);
            return panelSupport;
        }
        return panelSupportList.get(panelSupportList.size() - 1);
    }

    private PanelSupport getPanelSupport(Component component) {
        for (PanelSupport panelSupport : panelSupportList) {
            if (panelSupport.isField(component)) {
                return panelSupport;
            }
        }
        return getPanelSupport();
    }

    @Override
    public boolean modified() {
        boolean m = modified;
        modified = false;
        return m;
    }

    @Override
    public void clearModified() {
        modified = false;
    }

    protected void setManagedFields(JButton button, Component[] mandatory) {
        setManagedFields(button, mandatory, new Component[0]);
    }

    protected void setManagedFields(JButton button, Component[] mandatory, Component[] optional) {
        setManagedFields(button, mandatory, optional, null);
    }

    protected void setManagedFields(JButton button, Component[] mandatory, Component[] optional, String[] defaultOptionalValues) {
        if (button != null) {
            ProcessorXScan.setIcon(button, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"));
        }
        PanelSupport panelSupport = getPanelSupport(true); //If already had called it, creates a new
        HashMap<Component, ComponentMetadata> managedFields = new HashMap<>();
        if (mandatory != null) {
            for (Component c : mandatory) {
                managedFields.put(c, new ComponentMetadata(true));
            }
        }
        if (optional != null) {
            for (int i = 0; i < optional.length; i++) {
                Component c = optional[i];
                String defaulValue = (defaultOptionalValues == null) ? null : defaultOptionalValues[i];
                if (defaulValue == null) {
                    managedFields.put(c, new ComponentMetadata(false));
                } else {
                    managedFields.put(c, new ComponentMetadata(false, defaulValue));
                }
            }
        }
        panelSupport.analyze(managedFields);
        SwingUtilities.invokeLater(() -> {
            panelSupport.manage(this, managedFields, button);
        });
    }

    protected boolean bindIdEditor(Component editor) {
        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                // Workaround to update shown ids in visualizations and manipulations
                ModelUtil.getInstance().refreshIds();
            }
        });
        return bindEditor(editor, "id");
    }

    protected boolean bindEditor(Component editor, String property) {
        return bindEditor(editor, property, false);
    }

    protected boolean bindEditor(Component editor, String property, boolean realFieldValue) {
        return bindEditor(editor, property, realFieldValue, DEFAULT_VALUE_ON_ERROR);
    }

    protected boolean bindEditor(Component editor, String property, boolean realFieldValue, Object valueOnError) {
        return bindEditor(editor, target, property, realFieldValue, valueOnError);
    }

    protected boolean bindEditor(Component editor, Object obj, String property) {
        return bindEditor(editor, obj, property, false);
    }

    protected boolean bindEditor(Component editor, Object obj, String property, boolean realFieldValue) {
        return bindEditor(editor, obj, property, realFieldValue, DEFAULT_VALUE_ON_ERROR);
    }

    protected boolean bindEditor(Component editor, Object obj, String property, boolean realFieldValue, Object valueOnError) {
        if (obj != null) {
            try {
                Method _getter;
                try {
                    _getter = obj.getClass().getMethod("get" + Str.capitalizeFirst(property), new Class[0]);
                } catch (NoSuchMethodException ex) {
                    _getter = obj.getClass().getMethod("is" + Str.capitalizeFirst(property), new Class[0]);
                }
                Method getter = _getter;
                Class type = getter.getReturnType();
                Method _setter;
                try {
                    _setter = obj.getClass().getMethod("set" + Str.capitalizeFirst(property), new Class[]{type});
                } catch (NoSuchMethodException ex) {
                    Class altType = type.isPrimitive() ? Convert.getWrapperClass(type) : Convert.getPrimitiveClass(type);
                    _setter = obj.getClass().getMethod("set" + Str.capitalizeFirst(property), new Class[]{altType});
                }
                Method setter = _setter;
                Object value = getter.invoke(obj);
                String id = ModelUtil.getInstance().getId(value);
                if (id != null) {
                    value = id;
                }
                if (editor instanceof JComboBox) {
                    JComboBox combo = (JComboBox) editor;
                    combo.setSelectedItem(value);

                    combo.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent ie) {
                            try {
                                Object sel = combo.getSelectedItem();
                                if (sel instanceof String) {
                                    Object modelObject = ModelUtil.getInstance().getObject((String) sel);
                                    if (modelObject != null) {
                                        sel = modelObject;
                                    }
                                }
                                if (hasChanged(sel, getter.invoke(obj))) {
                                    setter.invoke(obj, sel);
                                    modified = true;
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(EditableComponent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                } else if (editor instanceof JCheckBox) {
                    JCheckBox check = (JCheckBox) editor;
                    check.setSelected((boolean) value);
                    check.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent ce) {
                            try {
                                if (hasChanged(check.isSelected(), getter.invoke(obj))){
                                    setter.invoke(obj, check.isSelected());
                                    modified = true;
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(EditableComponent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                } else {
                    JTextComponent textComponent = (JTextComponent) editor;
                    textComponent.setText((value == null) ? "" : (value + ""));
                    textComponent.getDocument().addDocumentListener(new DocumentAdapter() {
                        @Override
                        public void valueChange(DocumentEvent de) {
                            boolean changed;
                            if (de.getType() == DocumentEvent.EventType.REMOVE) {
                                return;
                            }
                            try {
                                Object cur = getter.invoke(obj);
                                String str = realFieldValue ? getPanelSupport(editor).getValue(editor) : textComponent.getText();
                                Object edited;
                                if (str != null) {
                                    if (type == String.class) {
                                        edited = str;
                                    } else if ((type == Boolean.class) || (type == boolean.class)) {
                                        edited = Boolean.valueOf(str.trim());
                                    } else {
                                        Double editedDouble = Double.valueOf(str.trim());
                                        edited = Convert.toType(editedDouble, type);
                                    }
                                    if (hasChanged(edited, cur)) {
                                        setter.invoke(obj, edited);
                                        modified = true;
                                    }
                                }
                            } catch (Exception e) {
                                try {
                                    if (type == String.class) {
                                        setter.invoke(obj, (valueOnError == DEFAULT_VALUE_ON_ERROR) ? "" : valueOnError);
                                    } else if ((type == Boolean.class) || (type == boolean.class)) {
                                        setter.invoke(obj, (valueOnError == DEFAULT_VALUE_ON_ERROR) ? false : valueOnError);
                                    } else {
                                        setter.invoke(obj, Convert.toType((valueOnError == DEFAULT_VALUE_ON_ERROR) ? 0.0 : (Number) valueOnError, type));
                                    }
                                } catch (Exception ex) {
                                    Logger.getLogger(EditableComponent.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    });
                }
            } catch (Exception ex) {
                Logger.getLogger(EditableComponent.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return true;
    }

    private boolean hasChanged(Object value, Object former) {
        if (former == null) {
            return (value != null);
        }
        if (value == former) {
            return false;
        }
        return !former.equals(value);
    }
    
    
    protected JTextComponent formatScriptEditor(JTextArea text){
        JTextComponent ret = text;
        boolean simpleEditor = ProcessorXScan.isSimpleCodeEditor();
        if (!simpleEditor){
            ScriptEditor editor= new ScriptEditor(true, false, true);
            editor.setTabSize(text.getTabSize());
            editor.setTextPaneFont(text.getFont());
            editor.setEditorBackground(text.getBackground());
            ret = editor.getTextEditor().getEditor();
            ((CodeEditor)editor.getTextEditor()).setHighlightCurrentLine(false);
            ret.setSelectionColor(text.getSelectionColor());
        }
        return ret;
    }

    @Override
    public T getObject() {
        return target;
    }

}
