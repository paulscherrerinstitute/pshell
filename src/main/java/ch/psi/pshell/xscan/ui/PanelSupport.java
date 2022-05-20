package ch.psi.pshell.xscan.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * PanelSupport class that adds the dynamic behaviour of text fields, optional fields and the add button of the panel.
 *
 * If an optional field has a label (JLabel), the <em>labelFor</em> attribute of the JLable need to be set to "inherit"
 * the dynamic behaviour of the optional text field to the label.
 *
 */
public class PanelSupport {

    /**
     * List of mandatory fields
     */
    private List<Component> mandatoryFields;
    /**
     * List of optional fields
     */
    private HashMap<Component, ComponentMetadata> optionalFields;

    private HashMap<Component, String> fieldLabels = new HashMap<Component, String>();

    /**
     * Map holding original border of the TextFields Map will be filled when manageTextFields() is called
     */
    private HashMap<Component, Border> bordermap = new HashMap<Component, Border>();

    /**
     * Button to "add" optional fields
     */
    private JButton button;
    private JPopupMenu popup;

    /**
     * Panel this instance belongs to
     */
    private JPanel panel;

    public PanelSupport() {

    }

    /**
     * Constructor
     *
     * @param panel Panel this support class belongs to
     * @param fields Map of fields that this support class manages. Specify true in the map if the field is mandatory
     * and false if the field is optional.
     * @param button Button to use to add additional components
     */
    public void manage(JPanel panel, HashMap<Component, ComponentMetadata> fields, JButton button) {

        // Save panel this support is attached to
        this.panel = panel;
        this.button = button;

        // Determine optional and mandatory fields
        this.mandatoryFields = new ArrayList<Component>();
        this.optionalFields = new HashMap<Component, ComponentMetadata>();

        for (Component f : fields.keySet()) {
            if (fields.get(f).isMandatory()) {
                this.mandatoryFields.add(f);
            } else {
                this.optionalFields.put(f, fields.get(f));
            }
        }

        addTextFieldNameSupport(fieldLabels);

        // Add activate/deactivate support to fields
        addActivateDeactivateSupport(fields);

        // Add popup to button
        if (button != null) {
            this.popup = new JPopupMenu();
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent evt) {
                    showPopup();
                }
            });
        }
    }

    public void analyze(HashMap<Component, ComponentMetadata> fields) {
        for (Component field : fields.keySet()) {
            if (field instanceof JTextField) {
                JTextField f = ((JTextField) field);
                String label = f.getText();
                if (label != null && !label.trim().equals("")) {
                    fieldLabels.put(field, label);
                }
            } else if (field instanceof JFormattedTextField) {
                JFormattedTextField f = ((JFormattedTextField) field);
                String label = f.getText();
                if (label != null && !label.trim().equals("")) {
                    fieldLabels.put(field, label);
                }
            }
        }
    }

    /**
     * Add text field gray label support to a (formatted) text fields
     *
     * @param fields
     */
    private void addTextFieldNameSupport(HashMap<Component, String> fields) {
        for (Component field : fields.keySet()) {
            addTextFieldNameSupport(field, fields.get(field));
        }
    }

    /**
     * Add text field gray label support to a (formatted) text field
     *
     * @param field
     */
    private void addTextFieldNameSupport(final Component field, final String label) {
        final Color baseColor = ch.psi.utils.swing.TextEditor.FOREGROUND_COLOR;
        final Color colorGray = new Color(160, 160, 160);

        if (field instanceof JTextField) {
            final JTextField f = ((JTextField) field);

            // Set label if current text is empty
            if (f.getText().trim().equals("")) {
                f.setText(label);
            }

            if (f.getText().equals(label)) {
                f.setForeground(colorGray);
            } else {
                f.setForeground(baseColor);
            }

            f.addFocusListener(new java.awt.event.FocusAdapter() {

                @Override
                public void focusGained(java.awt.event.FocusEvent evt) {
                    if (f.getText().equals(label)) {
                        f.setText("");
                    }
                    f.setForeground(baseColor);
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent evt) {
                    String t = f.getText().trim();
                    if (t.equals("")) {
                        f.setText(label);
                        f.setForeground(colorGray);
                    }
                }
            });

        } else if (field instanceof JFormattedTextField) {
            final JFormattedTextField f = ((JFormattedTextField) field);

            // Set label if current text is empty
            if (f.getText().trim().equals("")) {
                f.setText(label);
            }

            if (f.getText().equals(label)) {
                f.setForeground(colorGray);
            } else {
                f.setForeground(baseColor);
            }

            f.addFocusListener(new java.awt.event.FocusAdapter() {

                @Override
                public void focusGained(java.awt.event.FocusEvent evt) {
                    if (f.getText().equals(label)) {
                        f.setText("");
                    }
                    f.setForeground(baseColor);
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent evt) {
                    String t = f.getText().trim();
                    if (t.equals("")) {
                        f.setText(label);
                        f.setForeground(colorGray);
                    }
                }
            });

        }
    }

    private void addActivateDeactivateSupport(final Component field, final ComponentMetadata metadata) {
        // Save original border for text fields
        if (field instanceof JTextField) {
            JTextField f = (JTextField) field;
            bordermap.put(f, f.getBorder());
        } else if (field instanceof JFormattedTextField) {
            JFormattedTextField f = (JFormattedTextField) field;
            bordermap.put(f, f.getBorder());
        }

        if (metadata.isMandatory()) {
            // Mandatory field
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent evt) {
                    activateComponent(field);
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent evt) {
                    deactivateComponent(field);
                }
            });
        } else {
            // Optional field
            if (CollapsibleListContainer.class.isInstance(field)) {
                field.addMouseListener(new MouseListener() {

                    @Override
                    public void mouseClicked(MouseEvent me) {
                    }

                    @Override
                    public void mousePressed(MouseEvent me) {
                    }

                    @Override
                    public void mouseReleased(MouseEvent me) {
                    }

                    @Override
                    public void mouseEntered(MouseEvent me) {
                        activateComponent(field);
                    }

                    @Override
                    public void mouseExited(MouseEvent me) {
                        deactivateComponent(field);
                        // Check visibility
                        checkVisibility(field, metadata.getDefaultValue());
                    }
                });
            } else {
                field.addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override
                    public void focusGained(java.awt.event.FocusEvent evt) {
                        activateComponent(field);
                    }

                    @Override
                    public void focusLost(java.awt.event.FocusEvent evt) {
                        deactivateComponent(field);
                        // Check visibility
                        checkVisibility(field, metadata.getDefaultValue());
                    }
                });
            }

            // Check visibility (do not show up optional fields at startup)
            checkVisibility(field, metadata.getDefaultValue());
        }

        // Deactivate field - default
        deactivateComponent(field);
    }

    /**
     * Add activate/deactivate support to passed text fields
     *
     * @param fields
     */
    private void addActivateDeactivateSupport(HashMap<Component, ComponentMetadata> fields) {
        for (final Component field : fields.keySet()) {
            addActivateDeactivateSupport(field, fields.get(field));
        }
    }

    /**
     * Deactivate a text field.
     *
     * @param field Text field to deactivate
     */
    private void deactivateComponent(Component field) {
        if (field instanceof JTextField) {
//            JTextField f =(JTextField) field;
//
////          // Resize text element
////          int nco = field.getText().length();
////          field.setColumns(nco);
//////        field.setColumns((int) (nco * 0.6));
//
//            // Set background color to panel color
//            f.setBackground(f.getParent().getBackground());
//
//            // Remove border
//            f.setBorder(null);
        } else if (field instanceof JFormattedTextField) {

        } else if (field instanceof JComboBox) {
//            JComboBox b = (JComboBox) field;
        } else if (field instanceof JTextArea) {
//            JTextArea f =(JTextArea) field;
        }
    }

    /**
     * Activate text field.
     *
     * @param field Text field to deactivate
     */
    private void activateComponent(Component field) {
        if (field instanceof JTextField) {
//            JTextField f =(JTextField) field;
//
//            // Set background color to white
//            f.setBackground(Color.white);
//
//            // Restore border
//            if (bordermap.get(f) != null) {
//                f.setBorder(bordermap.get(f));
//            }
        } else if (field instanceof JFormattedTextField) {

        } else if (field instanceof JComboBox) {
//            JComboBox b = (JComboBox) field;
        } else if (field instanceof JTextArea) {
//            JTextArea f =(JTextArea) field;
        }
    }

    /**
     * Determine whether an optional field should be visible or not
     *
     * @param component
     */
    private void checkVisibility(Component field, String defaultValue) {
        boolean hide = false;

        if (field instanceof JTextField) {
            JTextField f = (JTextField) field;
            if (f.getText().trim().equals(defaultValue) || f.getText().trim().equals("")) {
                hide = true;
            }
        } else if (field instanceof JFormattedTextField) {
            JFormattedTextField f = (JFormattedTextField) field;
            if (f.getText().trim().equals(defaultValue) || f.getText().trim().equals("")) {
                hide = true;
            }
        } else if (field instanceof JTextArea) {
            JTextArea f = (JTextArea) field;
            if (f.getText().trim().equals(defaultValue) || f.getText().trim().equals("")) {
                hide = true;
            }
        } else if (field instanceof JComboBox) {
            JComboBox b = (JComboBox) field;
            if (b.getSelectedItem().equals(defaultValue)) {
                hide = true;
            }
        } else if (CollapsibleListContainer.class.isInstance(field)) {
            CollapsibleListContainer<?> c = (CollapsibleListContainer<?>) field;
            if (c.isEmpty()) {
                hide = true;
            }
        } else if (field instanceof JCheckBox) {
            JCheckBox box = (JCheckBox) field;
            if (!box.isSelected()) {
                hide = true;
            }
        }

        if (hide) {
            // Disable it
            field.setVisible(false);

            // Hide all related lable fields as well
            for (Component comp : field.getParent().getComponents()) {
                if (comp instanceof JLabel) {
                    JLabel l = (JLabel) comp;
                    if (field.equals(l.getLabelFor())) {
                        l.setVisible(false);
                    }
                }
            }
        } else {
            if (!field.isVisible()) {
                field.setVisible(true);
            }
            // Show all related lable fields as well
            for (Component comp : field.getParent().getComponents()) {
                if (comp instanceof JLabel) {
                    JLabel l = (JLabel) comp;
                    if (field.equals(l.getLabelFor())) {
                        l.setVisible(true);
                    }
                }
            }
        }

        // Validate whether add button need to be shown
        validateAddButton();
    }

    /**
     * Validate whether the add button should be shown
     */
    private void validateAddButton() {

        if (button != null) {
            boolean visible = false;
            if (optionalFields != null) {
                for (Component c : this.optionalFields.keySet()) {
                    if (!c.isVisible()) {
                        visible = true;
                        break;
                    }
                }
            }

            // Set button visible/invisible
            if (visible) {
                button.setVisible(true);
            } else {
                button.setVisible(false);
            }
        }

        // Revalidate and repaint parent panel
        updatePanel();
    }

    /**
     * Show popup for add Button
     */
    private void showPopup() {
        popup.removeAll();
        int cnt = 0;
        if (optionalFields == null) {
            return;
        }
        for (final Component c : this.optionalFields.keySet()) {
            if (!c.isVisible()) {

                if (CollapsibleListContainer.class.isInstance(c)) {

                    JMenu menu = new JMenu(c.getName());
                    final CollapsibleListContainer<?> clc = (CollapsibleListContainer<?>) c;

                    for (final String k : clc.getItemKeys()) {

//                        String itemName = c.getName()+"-"+k;
                        String itemName = k;

                        JMenuItem item = new JMenuItem(itemName);
                        item.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                clc.newItem(k);
                                clc.setCollapsed(false);

                                c.setVisible(true);

                                // Show all related label fields as well
                                for (Component comp : c.getParent().getComponents()) {

                                    if (comp instanceof JLabel) {
                                        JLabel l = (JLabel) comp;
                                        if (c.equals(l.getLabelFor())) {
                                            l.setVisible(true);
                                        }
                                    }
                                }

                                // Revalidate and repaint parent panel
                                updatePanel();

                                // Request focus for this element
                                c.requestFocus();

                                validateAddButton();
                            }
                        });

                        menu.add(item);
                        cnt++;
                    }

                    popup.add(menu);
                } else {
                    String itemName = c.getName();

                    JMenuItem item = new JMenuItem(itemName);
                    item.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            c.setVisible(true);

                            // If the component is a checkbox also activate or deactivate checkbox
                            // depending on its default value (always the opposite of the default)
                            if (c instanceof JCheckBox) {
                                if ("true".equalsIgnoreCase(optionalFields.get(c).getDefaultValue())) {
                                    ((JCheckBox) c).setSelected(false);
                                } else if ("false".equalsIgnoreCase(optionalFields.get(c).getDefaultValue())) {
                                    ((JCheckBox) c).setSelected(true);
                                }
                            }

                            // Show all related label fields as well
                            for (Component comp : c.getParent().getComponents()) {

                                if (comp instanceof JLabel) {
                                    JLabel l = (JLabel) comp;
                                    if (c.equals(l.getLabelFor())) {
                                        l.setVisible(true);
                                    }
                                }
                            }

                            // Revalidate and repaint parent panel
                            updatePanel();

                            // Request focus for this element
                            c.requestFocus();

                            validateAddButton();
                        }
                    });
                    popup.add(item);
                    cnt++;
                }

            }
        }

        // Show popup menu for button
        if (cnt > 0) {
//            popup.show(button, button.getWidth()/2, button.getHeight()/2);
            popup.show(button, button.getWidth(), button.getHeight() / 2);
        }
    }

    /**
     * Revalidate and repaint the parent panel
     */
    public void updatePanel() {
//        Component c = panel;
//        while (c != null){
//            if(c.getParent()!=null){
//                c = c.getParent();
//            }
//            else{
//                break;
//            }
//
//        }
//        c.validate();

        panel.revalidate();
        panel.repaint();

//        if(panel.getParent()!=null){
//            panel.getParent().validate();
//        }
    }

    /**
     * Add an additional component to this panel support object
     *
     * @param component
     * @param mandatory
     */
    public void addComponent(Component component, ComponentMetadata metadata) {

        if (metadata.isMandatory()) {
            if (mandatoryFields != null) {
                this.mandatoryFields.add(component);
            }
        } else {
            if (optionalFields != null) {
                this.optionalFields.put(component, metadata);
            }
        }

        // Add activate/deactivate support to fields
        addActivateDeactivateSupport(component, metadata);

    }

    public boolean isOptionalField(Component component) {
        if (optionalFields != null) {
            return optionalFields.containsKey(component);
        }
        return false;
    }

    public boolean isMandatoryField(Component component) {
        if (mandatoryFields != null) {
            return mandatoryFields.contains(component);
        }
        return false;
    }

    public boolean isField(Component component) {
        return isOptionalField(component) || isMandatoryField(component);
    }

    /**
     * Check the visibility of the the optional fields
     */
    public void checkVisibility() {
        if (optionalFields != null) {
            for (Component c : optionalFields.keySet()) {
                checkVisibility(c, optionalFields.get(c).getDefaultValue());
            }
        }
    }

    /**
     * For fields with in field labels use this method to get the real value of the field The method checks whether the
     * field had/has an in field label and then checks whether the actual value is is the label. If this is the case,
     * the method will return an empty String.
     *
     * @param field
     * @return
     */
    public String getRealFieldValue(Component field) {
        String value = null;

        if (field instanceof JTextField) {
            JTextField f = ((JTextField) field);
            value = f.getText();
        } else if (field instanceof JFormattedTextField) {
            JFormattedTextField f = ((JFormattedTextField) field);
            value = f.getText();
        }

        if (fieldLabels.containsKey(field)) {
            if (value.equals(fieldLabels.get(field))) {
                value = "";
            }
        }

        return (value);
    }

    public String getValue(Component field) {
        String s = getRealFieldValue(field);

        if ((optionalFields != null) && (optionalFields.containsKey(field))) {
            if (s.trim().equals("")) {
                return null;
            } else if (s.equals(optionalFields.get(field).getDefaultValue())) {
                return null;
            }
        }

        return s;
    }

}
