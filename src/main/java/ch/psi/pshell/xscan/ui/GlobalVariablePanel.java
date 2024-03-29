package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Variable;
import java.awt.Component;
import javax.swing.event.DocumentEvent;

/**
 *
 */
public class GlobalVariablePanel extends EditablePanel<Variable> {

    public GlobalVariablePanel() {
        this(new Variable());
    }

    /**
     * Creates new form FunctionPanel
     */
    public GlobalVariablePanel(Variable var) {
        super(var);

        initComponents();

        setManagedFields(jButton1,
                new Component[]{jTextFieldName, jTextFieldValue},
                new Component[]{jTextFieldDescription}
        );

        bindEditor(jTextFieldName, "name", true);
        bindEditor(jTextFieldDescription, "description", true);
        bindEditor(jTextFieldValue, "value");

        jTextFieldName.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void valueChange(DocumentEvent de) {
                ModelUtil.getInstance().refreshIds();
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextFieldName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldValue = new javax.swing.JTextField();
        jTextFieldDescription = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();

        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setLabelFor(jTextFieldName);
        jLabel1.setText("Name:");
        add(jLabel1);

        jTextFieldName.setPreferredSize(new java.awt.Dimension(120, 28));
        add(jTextFieldName);

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel2.setLabelFor(jTextFieldValue);
        jLabel2.setText("Value:");
        add(jLabel2);

        jTextFieldValue.setPreferredSize(new java.awt.Dimension(120, 28));
        add(jTextFieldValue);

        jTextFieldDescription.setText("Description");
        jTextFieldDescription.setPreferredSize(new java.awt.Dimension(200, 28));
        add(jTextFieldDescription);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        add(jButton1);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField jTextFieldDescription;
    private javax.swing.JTextField jTextFieldName;
    private javax.swing.JTextField jTextFieldValue;
    // End of variables declaration//GEN-END:variables

}
