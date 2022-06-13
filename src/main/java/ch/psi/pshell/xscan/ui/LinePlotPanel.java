package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.ProcessorXScan;
import ch.psi.pshell.xscan.model.LinePlot;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;

/**
 *
 */
public class LinePlotPanel extends EditablePanel<LinePlot> {

    private List<JComboBox> comboboxesY = new ArrayList<JComboBox>();
    private LinePlot plot;

    public LinePlotPanel() {
        this(new LinePlot());
    }

    /**
     * Creates new form LinePlotPanel
     */
    public LinePlotPanel(final LinePlot plot) {
        super(plot);
        this.plot = plot;
        initComponents();

        ProcessorXScan.setIcon(jButton2, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus-gray.png"));

        setManagedFields(jButton1,
                new Component[]{jComboBoxX, jComboBoxY},
                new Component[]{jTextFieldTitle}
        );

        // Update view
        jComboBoxX.setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getX())}));
        if (plot.getY().size() > 0) {
            jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getY().get(0))}));
            for (int i = 1; i < plot.getY().size(); i++) {
                // Add additional combo boxes
                addAdditionalComboBoxY(ModelUtil.getInstance().getId(plot.getY().get(i)));
            }
        } else {
            jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[]{" "}));
        }

        // Establish bindings
        bindEditor(jTextFieldTitle, "title");
        bindEditor(jComboBoxX, "x");

        jComboBoxY.addItemListener(itemListenerY);

    }

    ItemListener itemListenerY = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent ie) {
            System.out.println(ie.getStateChange());
            JComboBox box = (JComboBox) ie.getItemSelectable();
            if (((String) box.getSelectedItem()).equals("<remove>")) {
                // Remove combobox
                Logger.getLogger(LinePlotPanel.class.getName()).log(Level.INFO, "Remove combo box");

                jPanel1.remove(box);
                jPanel1.validate();

                comboboxesY.remove(box);
            }

            modified = true;
            plot.getY().clear();

            // Update references
            plot.getY().add(ModelUtil.getInstance().getObject((String) jComboBoxY.getSelectedItem()));
            for (JComboBox b : comboboxesY) {
                plot.getY().add(ModelUtil.getInstance().getObject((String) b.getSelectedItem()));
            }

        }
    };

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldTitle = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jComboBoxX = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jComboBoxY = new javax.swing.JComboBox();
        jButton2 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setLabelFor(jTextFieldTitle);
        jLabel1.setText("Title:");
        jPanel1.add(jLabel1);

        jTextFieldTitle.setToolTipText("Title of the plot");
        jTextFieldTitle.setName("Title"); // NOI18N
        jTextFieldTitle.setPreferredSize(new java.awt.Dimension(120, 28));
        jPanel1.add(jTextFieldTitle);

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel2.setLabelFor(jComboBoxX);
        jLabel2.setText("X:");
        jPanel1.add(jLabel2);

        jComboBoxX.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "none" }));
        jComboBoxX.setToolTipText("x-Axis");
        jComboBoxX.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                jComboBoxXPopupMenuWillBecomeVisible(evt);
            }
        });
        jPanel1.add(jComboBoxX);

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel3.setLabelFor(jComboBoxY);
        jLabel3.setText("Y:");
        jPanel1.add(jLabel3);

        jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "none" }));
        jComboBoxY.setToolTipText("y-Axis");
        jComboBoxY.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                jComboBoxYPopupMenuWillBecomeVisible(evt);
            }
        });
        jPanel1.add(jComboBoxY);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus-gray.png"))); // NOI18N
        jButton2.setBorderPainted(false);
        jButton2.setContentAreaFilled(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton2);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jPanel1.add(jButton1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBoxXPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_jComboBoxXPopupMenuWillBecomeVisible
        // Get selected item
        Object o = jComboBoxX.getSelectedItem();
        // Update the possible options
        List<String> li = ModelUtil.getInstance().getIds();
        li.add(0, " ");
        jComboBoxX.setModel(new javax.swing.DefaultComboBoxModel(li.toArray(new String[li.size()])));
        // Set the item selected that was selected before
        jComboBoxX.setSelectedItem(o);
    }//GEN-LAST:event_jComboBoxXPopupMenuWillBecomeVisible

    private void jComboBoxYPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_jComboBoxYPopupMenuWillBecomeVisible
        // Get selected item
        Object o = jComboBoxY.getSelectedItem();
        // Update the possible options
        List<String> li = ModelUtil.getInstance().getIds();
        li.add(0, " ");
        jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(li.toArray(new String[li.size()])));
        // Set the item selected that was selected before
        jComboBoxY.setSelectedItem(o);
    }//GEN-LAST:event_jComboBoxYPopupMenuWillBecomeVisible

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // Add an addional combo box for selecting series
        addAdditionalComboBoxY(" ");
    }//GEN-LAST:event_jButton2ActionPerformed

    private void addAdditionalComboBoxY(String selectedValue) {
        // Add a new Combo box to the panel
        final JComboBox box = new JComboBox();
        box.setModel(new javax.swing.DefaultComboBoxModel(new String[]{selectedValue}));
        box.setToolTipText("y-Axis");
        box.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }

            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                // Get selected item
                Object o = box.getSelectedItem();
                // Update the possible options
                List<String> li = ModelUtil.getInstance().getIds();
                li.add(0, " ");
                li.add("<remove>");
                box.setModel(new javax.swing.DefaultComboBoxModel(li.toArray(new String[li.size()])));
                // Set the item selected that was selected before
                box.setSelectedItem(o);
            }
        });
        box.addItemListener(itemListenerY);

        // Add item to comboboxes list
        comboboxesY.add(box);

        // Add item in front of the plus sign
        jPanel1.add(box, jPanel1.getComponents().length - 2);
        jPanel1.validate();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox jComboBoxX;
    private javax.swing.JComboBox jComboBoxY;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextFieldTitle;
    // End of variables declaration//GEN-END:variables

    /**
     * Update ID's shown in the combo box
     */
    public void updateIds() {
        jComboBoxX.setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getX())}));
        //
        if (plot.getY().size() > 0) {
            jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getY().get(0))}));
            for (int i = 1; i < plot.getY().size(); i++) {
                comboboxesY.get(i - 1).setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getY().get(i))}));
            }
        }
//        jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getY()) }));
    }
}
