package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.ProcessorXScan;
import ch.psi.pshell.xscan.model.LinePlotArray;
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
public class LinePlotArrayPanel extends EditablePanel<LinePlotArray> {

    LinePlotArray plot;

    private List<JComboBox> comboboxesY = new ArrayList<JComboBox>();

    public LinePlotArrayPanel() {
        this(new LinePlotArray());
    }

    /**
     * Creates new form LinePlotPanel
     */
    public LinePlotArrayPanel(final LinePlotArray plot) {
        super(plot);
        this.plot = plot;

        initComponents();

        ProcessorXScan.setIcon(jButton2, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus-gray.png"));

        setManagedFields(jButton1,
                new Component[]{jComboBoxY},
                new Component[]{jTextFieldTitle, jTextFieldMaxSeries, jTextFieldOffset, jTextFieldSize},
                new String[]{null, null, "0", "0"}
        );

        // Update view
        if (plot.getY().size() > 0) {
            jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getY().get(0))}));
            for (int i = 1; i < plot.getY().size(); i++) {
                // Add additional combo boxes
                addAdditionalComboBoxY(ModelUtil.getInstance().getId(plot.getY().get(i)));
            }
        } else {
            jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[]{" "}));
        }
        jTextFieldMaxSeries.setText(plot.getMaxSeries() + "");
        jTextFieldOffset.setText(plot.getOffset() + "");
        jTextFieldSize.setText(plot.getSize() + "");

        // Establish bindings 
        bindEditor(jTextFieldTitle, "maxSeries");
        bindEditor(jTextFieldMaxSeries, "maxSeries", false, 1);
        bindEditor(jTextFieldOffset, "offset");
        bindEditor(jTextFieldSize, "size");

        jComboBoxY.addItemListener(itemListenerY);
    }

    ItemListener itemListenerY = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent ie) {
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
        jLabel3 = new javax.swing.JLabel();
        jComboBoxY = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldMaxSeries = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldOffset = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldSize = new javax.swing.JTextField();
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

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel2.setLabelFor(jTextFieldMaxSeries);
        jLabel2.setText("Max Series:");
        jLabel2.setToolTipText("Maximum number of series in plot");
        jPanel1.add(jLabel2);

        jTextFieldMaxSeries.setToolTipText("Maximum number of series in plot");
        jTextFieldMaxSeries.setName("Max Series"); // NOI18N
        jTextFieldMaxSeries.setPreferredSize(new java.awt.Dimension(40, 28));
        jPanel1.add(jTextFieldMaxSeries);

        jLabel4.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel4.setLabelFor(jTextFieldOffset);
        jLabel4.setText("Offset:");
        jPanel1.add(jLabel4);

        jTextFieldOffset.setName("Offset"); // NOI18N
        jTextFieldOffset.setPreferredSize(new java.awt.Dimension(40, 28));
        jPanel1.add(jTextFieldOffset);

        jLabel5.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel5.setLabelFor(jTextFieldSize);
        jLabel5.setText("Size:");
        jPanel1.add(jLabel5);

        jTextFieldSize.setName("Size"); // NOI18N
        jTextFieldSize.setPreferredSize(new java.awt.Dimension(40, 28));
        jPanel1.add(jTextFieldSize);

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
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 729, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBoxYPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_jComboBoxYPopupMenuWillBecomeVisible
        // Get selected item
        Object o = jComboBoxY.getSelectedItem();
        // Update the possible options
        List<String> li = ModelUtil.getInstance().getIds();
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
        box.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent ie) {
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
        });

        // Add item to comboboxes list
        comboboxesY.add(box);

        // Add item in front of the plus sign
        jPanel1.add(box, jPanel1.getComponents().length - 8);
        jPanel1.validate();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox jComboBoxY;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextFieldMaxSeries;
    private javax.swing.JTextField jTextFieldOffset;
    private javax.swing.JTextField jTextFieldSize;
    private javax.swing.JTextField jTextFieldTitle;
    // End of variables declaration//GEN-END:variables

    /**
     * Update ID's shown in the combo box
     */
    public void updateIds() {
//        jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getY()) }));
        if (plot.getY().size() > 0) {
            jComboBoxY.setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getY().get(0))}));
            for (int i = 1; i < plot.getY().size(); i++) {
                comboboxesY.get(i - 1).setModel(new javax.swing.DefaultComboBoxModel(new String[]{ModelUtil.getInstance().getId(plot.getY().get(i))}));
            }
        }
    }
}