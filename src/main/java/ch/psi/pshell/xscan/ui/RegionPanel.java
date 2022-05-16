package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Action;
import ch.psi.pshell.xscan.model.Function;
import ch.psi.pshell.xscan.model.Region;
import java.awt.Component;
import javax.swing.JFormattedTextField;

/**
 *
 */
public class RegionPanel extends EditablePanel<Region> {

    final Region region;

    public RegionPanel() {
        this(new Region());
    }

    /** Creates new form RegionPanel */
    public RegionPanel(final Region region) {
        super(region);
        this.region=region;
        initComponents();
        
        setManagedFields(jButton1, new Component[]{jFormattedTextFieldStart, jFormattedTextFieldEnd, jFormattedTextFieldStepSize},
                                    new Component[]{collapsibleListContainer1, collapsibleListContainerFunction});

        // Establish bindings
        bindEditor(jFormattedTextFieldStart, "start");
        bindEditor(jFormattedTextFieldEnd, "end");
        bindEditor(jFormattedTextFieldStepSize, "stepSize");


        collapsibleListContainer1.setHeader("Pre Actions");
        collapsibleListContainer1.setName("Pre Actions");

        collapsibleListContainerFunction.setHeader("Function");
        collapsibleListContainerFunction.setName("Function");
        collapsibleListContainerFunction.setCollapsed(true);
        collapsibleListContainerFunction.deactivateAddButton(); // Disable add button
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        collapsibleListContainer1 = new CollapsibleListContainer<Action>(new ActionListItemProvider(region.getPreAction()));
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jFormattedTextFieldStart = new JFormattedTextField(FieldUtilities.getDecimalFormat());
        jLabel2 = new javax.swing.JLabel();
        jFormattedTextFieldEnd = new JFormattedTextField(FieldUtilities.getDecimalFormat());
        jLabel3 = new javax.swing.JLabel();
        jFormattedTextFieldStepSize = new JFormattedTextField(FieldUtilities.getDecimalFormat());
        jButton1 = new javax.swing.JButton();
        collapsibleListContainerFunction = new CollapsibleListContainer<Function>(new FunctionListItemProvider(region));

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 255)));

        collapsibleListContainer1.setName("Pre-Actions"); // NOI18N

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setLabelFor(jFormattedTextFieldStart);
        jLabel1.setText("Start:");
        jPanel1.add(jLabel1);

        jFormattedTextFieldStart.setToolTipText("Start");
        jFormattedTextFieldStart.setName("Start"); // NOI18N
        jFormattedTextFieldStart.setPreferredSize(new java.awt.Dimension(60, 28));
        jPanel1.add(jFormattedTextFieldStart);

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel2.setLabelFor(jFormattedTextFieldEnd);
        jLabel2.setText("End:\n");
        jPanel1.add(jLabel2);

        jFormattedTextFieldEnd.setToolTipText("End");
        jFormattedTextFieldEnd.setName("End"); // NOI18N
        jFormattedTextFieldEnd.setPreferredSize(new java.awt.Dimension(60, 28));
        jPanel1.add(jFormattedTextFieldEnd);

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel3.setLabelFor(jFormattedTextFieldStepSize);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("ch/psi/pshell/xscan/ui/Bundle"); // NOI18N
        jLabel3.setText(bundle.getString("RegionPanel.jLabel3.text")); // NOI18N
        jPanel1.add(jLabel3);

        jFormattedTextFieldStepSize.setToolTipText("Step size");
        jFormattedTextFieldStepSize.setName("Step Size"); // NOI18N
        jFormattedTextFieldStepSize.setPreferredSize(new java.awt.Dimension(60, 28));
        jPanel1.add(jFormattedTextFieldStepSize);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel1.add(jButton1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
            .addComponent(collapsibleListContainer1, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
            .addComponent(collapsibleListContainerFunction, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(collapsibleListContainer1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(collapsibleListContainerFunction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainer1;
    private ch.psi.pshell.xscan.ui.CollapsibleListContainer collapsibleListContainerFunction;
    private javax.swing.JButton jButton1;
    private javax.swing.JFormattedTextField jFormattedTextFieldEnd;
    private javax.swing.JFormattedTextField jFormattedTextFieldStart;
    private javax.swing.JFormattedTextField jFormattedTextFieldStepSize;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables


}
