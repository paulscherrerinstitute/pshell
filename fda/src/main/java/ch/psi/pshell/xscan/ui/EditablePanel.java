package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.swing.CodeEditor;
import ch.psi.pshell.framework.ScriptEditor;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.xscan.ProcessorXScan;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
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
    private boolean updating = false;

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
    
    @Override
    public void setModified() {
        modified = true;
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
                
                Method _getterVar,_setterVar;
                String var = null;
                try {
                    _getterVar = obj.getClass().getMethod("get" + Str.capitalizeFirst(property)+"Var", new Class[0]);
                    _setterVar = obj.getClass().getMethod("set" + Str.capitalizeFirst(property)+"Var", new Class[]{String.class});
                    var =  (String) _getterVar.invoke(obj);
                } catch (Exception ex) {
                    _getterVar = null;
                    _setterVar = null;
                }                
                Method getterVar = _getterVar;
                Method setterVar = _setterVar;
                
                Object value = getter.invoke(obj);
                String id = ModelUtil.getInstance().getId(value);
                if (id != null) {
                    value = id;
                }
                if (editor instanceof JComboBox combo) {
                    combo.setSelectedItem(value);
                    combo.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent ie) {
                            try {
                                Object sel = combo.getSelectedItem();
                                if (sel instanceof String string) {
                                    Object modelObject = ModelUtil.getInstance().getObject(string);
                                    if (modelObject != null) {
                                        sel = modelObject;
                                    }
                                }
                                if (hasChanged(sel, getter.invoke(obj))) {
                                    setter.invoke(obj, sel);
                                    setModified();
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(EditableComponent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                } else if (editor instanceof JCheckBox check) {
                    check.setSelected((boolean) value);
                    check.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent ce) {
                            try {
                                if (hasChanged(check.isSelected(), getter.invoke(obj))){
                                    setter.invoke(obj, check.isSelected());
                                    setModified();
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(EditableComponent.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                } else {
                    JTextComponent textComponent = (JTextComponent) editor;    
                    if (textComponent instanceof JFormattedTextField jFormattedTextField){
                        JFormattedTextField.AbstractFormatterFactory formatFactory=jFormattedTextField.getFormatterFactory();
                        textComponent.addKeyListener(new KeyAdapter() {
                            @Override
                            public void keyReleased(KeyEvent arg0) {
                                String str = textComponent.getText();
                                if ((getterVar!=null) && ((getVariable(str)!=null) || (getInterpreterVariable(str)!=null))){
                                    if (jFormattedTextField.getFormatterFactory() != null){
                                        jFormattedTextField.setFormatterFactory(null);
                                    }
                                } else {                                
                                    if (formatFactory != jFormattedTextField.getFormatterFactory()){
                                        String text = textComponent.getText();
                                        jFormattedTextField.setFormatterFactory(formatFactory);
                                        textComponent.setText(text);
                                    }
                                }                                
                            }                         
                        });
                    }
                    
                    if ((var!=null) && (!var.isBlank())){
                        ((JFormattedTextField)textComponent).setFormatterFactory(null);
                        textComponent.setText(var);
                    } else {
                        textComponent.setText((value == null) ? "" : (value + ""));
                    }
                    
                    textComponent.getDocument().addDocumentListener(new DocumentAdapter() {
                        @Override
                        public void valueChange(DocumentEvent de) {
                            if (!updating && textComponent.isVisible()){
                                updating=true;
                                //Must invoke because doc updating to current value generate 1 remove + 1 insert, and doc is marked as changed if checked in between
                                SwingUtilities.invokeLater(()->{
                                    updating=false;
                                    boolean changed;
                                    try {
                                        Object cur = getter.invoke(obj);                                
                                        String str = realFieldValue ? getPanelSupport(editor).getValue(editor) : textComponent.getText();                                
                                        String var=(getterVar!=null) ? (String)getterVar.invoke(obj): null;
                                        Object edited=null;
                                        Object editedVar=null;
                                        if ((str != null) || ((type == String.class))) {     
                                            if ((getterVar!=null) && ((getVariable(str)!=null) || (getInterpreterVariable(str)!=null))){
                                                editedVar = str;
                                            } else {                                
                                                if ((type == String.class)||(type == Object.class)) {
                                                    edited = str;
                                                } else if ((type == Boolean.class) || (type == boolean.class)) {
                                                    edited = Boolean.valueOf(str.trim());
                                                } else {
                                                    Double editedDouble;
                                                    editedDouble = Double.valueOf(str.trim());
                                                    edited = Convert.toType(editedDouble, type);
                                                }
                                                //if (var!=null){
                                                //    setModified();
                                                //}                                                             
                                            }
                                            if (editedVar!=null){
                                                if (hasChanged(editedVar, var)) {
                                                    setterVar.invoke(obj, editedVar);
                                                    setModified();
                                                }                                        
                                            } else {
                                                if (hasChanged(edited, cur)) {
                                                    setter.invoke(obj, edited);
                                                    if (setterVar!=null){
                                                        setterVar.invoke(obj, (String)null);
                                                    }
                                                    setModified();
                                                }
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
                                });
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
        
    public ProcessorXScan getProcessor(){
        Component c = this;
        while ((c=c.getParent()) != null) {
            if (c instanceof ProcessorXScan processorXScan) {
                 return processorXScan;
            }
        }        
        return null;
    }      
    
    public Map<String, Object> getVariables(){
        try{
            return getProcessor().getVariables();
        } catch (Exception ex){
            return new HashMap<>();
        }
    }
    
    public Object getVariable(String name){
        return getVariables().get(name);
    }        
    
    public Object getInterpreterVariable(String name){
        try{
            return getProcessor().getInterpreterVariable(name);
        } catch (Exception ex){
            return null;
        }
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
            if (ret instanceof JTextArea jTextArea){
                jTextArea.setRows(text.getRows());
                jTextArea.setColumns(text.getColumns());
            }
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
