package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Action;
import ch.psi.pshell.xscan.model.ArrayDetector;
import java.awt.Component;
import javax.swing.JFormattedTextField;

/**
 *
 */
public class ArrayDetectorPanel extends EditablePanel<ArrayDetector> {

    final ArrayDetector detector;

    public ArrayDetectorPanel() {
        this(new ArrayDetector());
    }

    /**
     * Creates new form ArrayDetectorPanel
     */
    public ArrayDetectorPanel(final ArrayDetector detector) {
        super(detector);
        this.detector = detector;
        if (detector.getId() == null || detector.getId().equals("")) {
            detector.setId(IdGenerator.generateId());
        }

        initComponents();

        setManagedFields(jButton1, 
                new Component[]{jTextFieldName}, 
                new Component[]{jFormattedTextFieldArraySize, collapsibleListContainer1,});

        // Establish bindings
        bindIdEditor(jTextFieldId);
        bindEditor(jTextFieldName, "name");
        bindEditor(jFormattedTextFieldArraySize, "arraySize", true);

        collapsibleListContainer1.setHeader("Pre Actions");
        collapsibleListContainer1.setName("Pre Actions");

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        collapsibleListContainer1 = new CollapsibleListContainer<Action>(new ActionListItemProvider(detector.getPreAction()));
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldId = new javax.swing.JTextField();
        jTextFieldName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jFormattedTextFieldArraySize = new JFormattedTextField(FieldUtilities.getIntegerFormat());
        jButton1 = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 204, 204)));

        collapsibleListContainer1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 0, 204)));

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

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel2.setLabelFor(jFormattedTextFieldArraySize);
        jLabel2.setText("Array Size:");
        jPanel1.add(jLabel2);

        jFormattedTextFieldArraySize.setToolTipText("Array Size");
        jFormattedTextFieldArraySize.setPreferredSize(new java.awt.Dimension(40, 28));
        jPanel1.add(jFormattedTextFieldArraySize);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel1.add(jButton1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(collapsibleListContainer1, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(collapsibleListContainer1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainer1;
    private javax.swing.JButton jButton1;
    private javax.swing.JFormattedTextField jFormattedTextFieldArraySize;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextFieldId;
    private javax.swing.JTextField jTextFieldName;
    // End of variables declaration//GEN-END:variables

}
