package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.ProcessorXScan;
import ch.psi.utils.swing.MonitoredPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseListener;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 *
 */
public class CollapsibleContainer extends MonitoredPanel {
    String icon;
    /**
     * Collapse status of the container
     */
    boolean collapsed = false;
    /**
     * Variable to store the default border of the container
     */
    private Border defaultBorder = null;

    private ListContainer<?> childContainer;

    /**
     * Default constructor
     *
     * @param component
     */
    public CollapsibleContainer() {
        initComponents();
    }

    /**
     * Creates new CollapsiblePanel containing the passed component
     *
     * @param component Panel that should be collapsible
     */
    public CollapsibleContainer(Component component) {
        this(component.getName(), "", component);
    }

    /**
     * Creates new CollapsiblePanel containing the passed component
     *
     * @param header Header of the collapsible panel
     * @param icon Name of the icon to dislay for the group. The icon files are located in ch.psi.pshell.xscan.ui.icons.
     * The name of the icon is just the icon file name withou the .png suffix. For example "photo" will load the
     * photo.png in the ch.psi.pshell.xscan.ui.icons package.
     * @param component Panel that should be collapsible
     */
    public CollapsibleContainer(String header, String icon, Component component) {
        initComponents();

        this.jButton2.setVisible(false);

        // Set header
        if (header != null) {
            setHeader(header);
        }

        this.icon=icon;
        onLafChange();       

        // Add component to collapsible panel
        this.jPanel2.add(component, BorderLayout.CENTER);
    }    

    @Override
    protected void onLafChange() {
        ProcessorXScan.setIcon(jLabel2, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/wrench.png"));
        ProcessorXScan.setIcon(jButton1, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/sq_up.png"));
        ProcessorXScan.setIcon(jButton2, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"));
        if ((icon != null) && !icon.isEmpty()) {
            ProcessorXScan.setIcon(jLabel2, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/" + icon + ".png"));
        }             
    }  
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabelAdditionalHeader = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        setMinimumSize(new java.awt.Dimension(0, 0));
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel1.setPreferredSize(new java.awt.Dimension(487, 28));

        jPanel3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jPanel3MouseExited(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jPanel3MouseEntered(evt);
            }
        });
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButton1.setBackground(getBackground());
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/sq_up.png"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jButton1.setIconTextGap(0);
        jButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton1.setPreferredSize(new java.awt.Dimension(16, 16));
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton1MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton1MouseExited(evt);
            }
        });
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton1);

        jPanel4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jPanel4MouseExited(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jPanel4MouseEntered(evt);
            }
        });
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/wrench.png"))); // NOI18N
        jLabel2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jLabel2MouseExited(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jLabel2MouseEntered(evt);
            }
        });
        jPanel4.add(jLabel2);

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setText("Header");
        jLabel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel1MouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jLabel1MouseExited(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jLabel1MouseEntered(evt);
            }
        });
        jPanel4.add(jLabel1);

        jLabelAdditionalHeader.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jPanel4.add(jLabelAdditionalHeader);

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/plus.png"))); // NOI18N
        jButton2.setBorderPainted(false);
        jButton2.setContentAreaFilled(false);
        jButton2.setPreferredSize(new java.awt.Dimension(16, 16));
        jPanel4.add(jButton2);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        add(jPanel1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jLabel3.setPreferredSize(new java.awt.Dimension(20, 0));
        jPanel2.add(jLabel3, java.awt.BorderLayout.LINE_START);

        add(jPanel2);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        collapse();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jPanel3MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel3MouseEntered
        highlight(true);
    }//GEN-LAST:event_jPanel3MouseEntered

    private void jPanel3MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel3MouseExited
        highlight(false);
    }//GEN-LAST:event_jPanel3MouseExited

    private void jButton1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseEntered
        highlight(true);
    }//GEN-LAST:event_jButton1MouseEntered

    private void jButton1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseExited
        highlight(false);
    }//GEN-LAST:event_jButton1MouseExited

    private void jPanel4MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel4MouseEntered
        highlight(true);
    }//GEN-LAST:event_jPanel4MouseEntered

    private void jPanel4MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel4MouseExited
        highlight(false);
    }//GEN-LAST:event_jPanel4MouseExited

    private void jLabel1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel1MouseEntered
        highlight(true);
    }//GEN-LAST:event_jLabel1MouseEntered

    private void jLabel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel1MouseExited
        highlight(false);
    }//GEN-LAST:event_jLabel1MouseExited

    private void jLabel2MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel2MouseEntered
        highlight(true);
    }//GEN-LAST:event_jLabel2MouseEntered

    private void jLabel2MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel2MouseExited
        highlight(false);
    }//GEN-LAST:event_jLabel2MouseExited

    private void jLabel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel1MouseClicked
        if (evt.getClickCount() == 2) {
            collapse();
        }
    }//GEN-LAST:event_jLabel1MouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabelAdditionalHeader;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    // End of variables declaration//GEN-END:variables

    /**
     * Collapse of open container
     */
    private void collapse() {
        if (collapsed) {
            jPanel2.setVisible(true);
            ProcessorXScan.setIcon(jButton1, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/sq_up.png"));

            jLabelAdditionalHeader.setText("");
            collapsed = false;

        } else {
            jPanel2.setVisible(false);
            ProcessorXScan.setIcon(jButton1, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/sq_down.png"));

            if (childContainer != null) {
                int size = childContainer.getListSize();
                StringBuilder b = new StringBuilder();
                b.append("[");
                if (size <= 4) {
                    for (int i = 1; i <= size; i++) {
                        b.append(i);
                        b.append(",");
                    }
                } else {
                    b.append("1,2,...]");
                }

                // If the length is 1 we only have the opening bracket ([) in the builder
                // Therefor just append the closing bracket (]) if this is the case.
                // Else we replace the last character with ]
                if (b.length() > 1) {
                    b.replace(b.length() - 1, b.length(), "]");
                } else {
                    b.append("]");
                }
                jLabelAdditionalHeader.setText(b.toString());
            }

            collapsed = true;
        }

        this.validate();
        this.repaint();
    }

    /**
     * Highlight container
     *
     * @param flag
     */
    private void highlight(boolean flag) {
        if (flag) {
            this.defaultBorder = this.getBorder();
            this.setBorder(new LineBorder(Color.yellow));
        } else {
            if (defaultBorder != null) {
                this.setBorder(defaultBorder);
            }
        }
    }

    /**
     * Set header of the container
     *
     * @param header
     */
    public void setHeader(String header) {
        this.jLabel1.setText(header);
    }

    /**
     * Get header of the container
     *
     * @return
     */
    public String getHeader() {
        return (this.jLabel1.getText());
    }

    public void setIcon(String icon) {
        if ((icon != null) && !icon.isEmpty()) {
            ProcessorXScan.setIcon(jLabel2, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/" + icon + ".png"));
        }
    }

    /**
     * Collapse/open container
     *
     * @param collapsed
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = !collapsed;
        collapse();
    }

    /**
     * Activate the add button of this component
     *
     * @param listener
     */
    public void activateAddButton(MouseListener listener) {
        this.jButton2.setVisible(true);
        this.jButton2.addMouseListener(listener);
        validate();
    }

    /**
     * Deactivate the add button
     */
    public void deactivateAddButton() {
        this.jButton2.setVisible(false);
        for (MouseListener l : this.jButton2.getMouseListeners()) {
            this.jButton2.removeMouseListener(l);
        }
        validate();
    }

    /**
     * Set the additional header for this component This field is used to e.g. show how many items are inside the
     * container [1,2,3,...]
     *
     * @param text
     */
    public void setAdditionalHeader(String text) {
        jLabelAdditionalHeader.setText(text);
    }

    /**
     * Clear the additional header
     */
    public void clearAdditionalHeader() {
        jLabelAdditionalHeader.setText("");
    }

    public void setChildContainer(ListContainer<?> childContainer) {
        this.childContainer = childContainer;
    }

    public void setAddEnabled(boolean enabled) {
        jButton2.setEnabled(enabled);
    }
}
