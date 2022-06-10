package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ParameterMapping;
import ch.psi.pshell.xscan.model.ScriptManipulation;
import java.awt.Component;
import javax.swing.text.JTextComponent;

/**
 *
 */
public class ScriptManipulationPanel extends EditablePanel<ScriptManipulation> {

    final ScriptManipulation manipulation;

    public ScriptManipulationPanel() {
        this(new ScriptManipulation());
    }

    /**
     * Creates new form ScriptManipulationPanel
     */
    public ScriptManipulationPanel(final ScriptManipulation manipulation) {
        super(manipulation);
        this.manipulation = manipulation;
        // Generate id if not available
        if (manipulation.getId() == null || manipulation.getId().equals("")) {
            manipulation.setId(IdGenerator.generateId());
        }
        // Cleanup script
        String script = manipulation.getScript();
        if (script != null) {
            // Remove empty lines at the begining and end
            script = script.replaceAll("^[ ,\t,\n]*", "");
            script = script.replaceAll("[ ,\t,\n]*$", "");
        } else {
            script = "def process():\n    return 0.0";
        }
        manipulation.setScript(script);

        initComponents();
        
        JTextComponent textScript = formatScriptEditor(jTextAreaScript);
        jScrollPane1.setViewportView(textScript);    
        
        setManagedFields(jButton1,
                new Component[]{jTextFieldId, textScript},
                new Component[]{jCheckBoxArray, collapsibleListContainerMapping},
                new String[]{"false", null}
        );

        collapsibleListContainerMapping.setHeader("Mappings");
        collapsibleListContainerMapping.setName("Mappings");

        // Update view
        bindIdEditor(jTextFieldId);
        bindEditor(textScript, "script");
        bindEditor(jCheckBoxArray, "returnArray");
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        collapsibleListContainerMapping = new CollapsibleListContainer<ParameterMapping>(new ParameterMappingListItemProvider(manipulation.getMapping()));
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaScript = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldId = new javax.swing.JTextField();
        jCheckBoxArray = new javax.swing.JCheckBox();

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 255, 204)));

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel1.add(jButton1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jTextAreaScript.setColumns(20);
        jTextAreaScript.setRows(5);
        jScrollPane1.setViewportView(jTextAreaScript);

        jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setLabelFor(jTextFieldId);
        jLabel1.setText("Id:");
        jPanel3.add(jLabel1);

        jTextFieldId.setBackground(getBackground());
        jTextFieldId.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jTextFieldId.setForeground(new java.awt.Color(102, 102, 102));
        jTextFieldId.setText("Id");
        jTextFieldId.setBorder(null);
        jTextFieldId.setInputVerifier(new IdInputVerifier());
        jTextFieldId.setPreferredSize(new java.awt.Dimension(80, 16));
        jPanel3.add(jTextFieldId);

        jCheckBoxArray.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jCheckBoxArray.setText("Array");
        jCheckBoxArray.setName("Returns Array"); // NOI18N
        jPanel3.add(jCheckBoxArray);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
            .addComponent(collapsibleListContainerMapping, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(collapsibleListContainerMapping, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainerMapping;
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBoxArray;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaScript;
    private javax.swing.JTextField jTextFieldId;
    // End of variables declaration//GEN-END:variables

}