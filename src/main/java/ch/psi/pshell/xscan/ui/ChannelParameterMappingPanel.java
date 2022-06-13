package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ChannelParameterMapping;
import java.awt.Component;

/**
 *
 */
public class ChannelParameterMappingPanel extends EditablePanel<ChannelParameterMapping> {

    public ChannelParameterMappingPanel() {
        this(new ChannelParameterMapping());
    }

    /**
     * Creates new form ChannelParameterMappingPanel
     */
    public ChannelParameterMappingPanel(final ChannelParameterMapping mapping) {
        super(mapping);
        initComponents();

        setManagedFields(jButton1,
                new Component[]{jTextFieldVariable, jTextFieldName},
                new Component[]{jComboBoxType},
                new String[]{new ChannelParameterMapping().getType()}
        );

        // Establish bindings
        bindEditor(jTextFieldName, "channel");
        bindEditor(jTextFieldVariable, "variable");
        bindEditor(jComboBoxType, "type");
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jTextFieldVariable = new javax.swing.JTextField();
        jTextFieldName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jComboBoxType = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jTextFieldVariable.setText("Variable");
        jTextFieldVariable.setPreferredSize(new java.awt.Dimension(80, 28));
        jPanel1.add(jTextFieldVariable);

        jTextFieldName.setText("Channel Name");
        jTextFieldName.setPreferredSize(new java.awt.Dimension(180, 28));
        jPanel1.add(jTextFieldName);

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setLabelFor(jComboBoxType);
        jLabel1.setText("Type:");
        jPanel1.add(jLabel1);

        jComboBoxType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Double", "Integer", "String" }));
        jComboBoxType.setToolTipText("Id to map this variable");
        jComboBoxType.setName("Type"); // NOI18N
        jPanel1.add(jComboBoxType);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel1.add(jButton1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 474, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox jComboBoxType;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextFieldName;
    private javax.swing.JTextField jTextFieldVariable;
    // End of variables declaration//GEN-END:variables

}
