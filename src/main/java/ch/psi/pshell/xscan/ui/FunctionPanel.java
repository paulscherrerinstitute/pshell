package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Function;
import ch.psi.pshell.xscan.model.ParameterMapping;
import java.awt.Component;

/**
 *
 */
public class FunctionPanel extends EditablePanel<Function> {
    final Function function;
    public FunctionPanel(){
        this(new Function());
    }

    /** Creates new form FunctionPanel */
    public FunctionPanel(Function f) {
        super(f);
        function=f;
        if(f.getScript()== null){
            f.setScript("def calculate(parameter):\n    return parameter");
        }

        initComponents();
        
        setManagedFields(jButton1, 
            new Component[]{jTextAreaScript},
            new Component[]{collapsibleListContainerMapping}
        );          

        collapsibleListContainerMapping.setHeader("Mappings");
        collapsibleListContainerMapping.setName("Mappings");

        // Establish bindings
        bindEditor(jTextAreaScript, "script");

        // Update view
        String p = f.getScript();
        p = p.replaceAll("^[ ,\t,\n]*", "");
        p = p.replaceAll("[ ,\t,\n]*$", "");
        jTextAreaScript.setText(p);

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        collapsibleListContainerMapping = new CollapsibleListContainer<ParameterMapping>(new ParameterMappingVariableListItemProvider(function.getMapping()));
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaScript = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();

        jPanel1.setLayout(new java.awt.BorderLayout());

        jTextAreaScript.setColumns(20);
        jTextAreaScript.setRows(5);
        jScrollPane1.setViewportView(jTextAreaScript);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel2.add(jButton1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(collapsibleListContainerMapping, javax.swing.GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(collapsibleListContainerMapping, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainerMapping;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaScript;
    // End of variables declaration//GEN-END:variables

}
