package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ChannelParameterMapping;
import ch.psi.pshell.xscan.model.ScriptAction;
import java.awt.Component;
import javax.swing.text.JTextComponent;

/**
 *
 */
public class ScriptActionPanel extends EditablePanel<ScriptAction> {

    final ScriptAction action;

    public ScriptActionPanel() {
        this(new ScriptAction());
    }

    /**
     * Creates new form ScriptActionPanel
     */
    public ScriptActionPanel(final ScriptAction action) {
        super(action);
        this.action = action;
        initComponents();
        
        JTextComponent textScript = formatScriptEditor(jTextArea1);
        jScrollPane1.setViewportView(textScript);
        
        setManagedFields(jButton1,
                new Component[]{textScript},
                new Component[]{collapsibleListContainerMapping}
        );

        // Establish bindings
        bindEditor(jTextArea1, "script");

        collapsibleListContainerMapping.setHeader("Mappings");
        collapsibleListContainerMapping.setName("Mappings");
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        collapsibleListContainerMapping = new CollapsibleListContainer<ChannelParameterMapping>(new ChannelParameterMappingListItemProvider(action.getMapping()));
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 3));

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setToolTipText("Add optional items");
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel1.add(jButton1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setToolTipText("Jython Script");
        jScrollPane1.setViewportView(jTextArea1);

        jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(collapsibleListContainerMapping, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(collapsibleListContainerMapping, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainerMapping;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables

}