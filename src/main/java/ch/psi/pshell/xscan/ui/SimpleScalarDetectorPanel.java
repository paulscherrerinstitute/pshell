package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.SimpleScalarDetector;
import java.awt.Component;

/**
 *
 */
public class SimpleScalarDetectorPanel extends EditablePanel<SimpleScalarDetector> {

    public SimpleScalarDetectorPanel() {
        this(new SimpleScalarDetector());
    }

    /**
     * Creates new form SimpleScalarDetectorPanel
     */
    public SimpleScalarDetectorPanel(final SimpleScalarDetector detector) {
        super(detector);
        if (detector.getId() == null || detector.getId().equals("")) {
            detector.setId(IdGenerator.generateId());
        }

        initComponents();

        setManagedFields(jButton1,
                new Component[]{jTextFieldName},
                new Component[]{jCheckBoxSCR},
                new String[]{"false"}
        );

        // Establish bindings
        bindIdEditor(jTextFieldId);
        bindEditor(jTextFieldName, "name");
        bindEditor(jCheckBoxSCR, "scr");
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldId = new javax.swing.JTextField();
        jTextFieldName = new javax.swing.JTextField();
        jCheckBoxSCR = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setText("Id:");
        jPanel1.add(jLabel1);

        jTextFieldId.setBackground(getBackground());
        jTextFieldId.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jTextFieldId.setForeground(new java.awt.Color(102, 102, 102));
        jTextFieldId.setText("Id");
        jTextFieldId.setBorder(null);
        jTextFieldId.setInputVerifier(new IdInputVerifier());
        jTextFieldId.setPreferredSize(new java.awt.Dimension(80, 16));
        jPanel1.add(jTextFieldId);

        jTextFieldName.setText("Channel Name");
        jTextFieldName.setToolTipText("Channel Name");
        jTextFieldName.setPreferredSize(new java.awt.Dimension(180, 28));
        jPanel1.add(jTextFieldName);

        jCheckBoxSCR.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jCheckBoxSCR.setText("SCR");
        jCheckBoxSCR.setName("SCR"); // NOI18N
        jPanel1.add(jCheckBoxSCR);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel1.add(jButton1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBoxSCR;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextFieldId;
    private javax.swing.JTextField jTextFieldName;
    // End of variables declaration//GEN-END:variables

}
