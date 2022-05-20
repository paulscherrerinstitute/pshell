package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.ProcessorXscan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 *
 */
public class ListItem extends javax.swing.JPanel implements Transferable {

    private DataFlavor flavor;

    /**
     * Parent container
     */
    private ListContainer<?> container;
    /**
     * Component of this item
     */
    private Component component;

    /**
     * Variable to store the default border of the container
     */
    private final Border defaultBorder;

    /**
     * Creates new form ListElement
     */
    public ListItem(ListContainer<?> container, Component component) {

        // BEGIN Drad and Drop
        // Create supported flavor
        try {
            flavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + ListItem.class.getCanonicalName());
        } catch (ClassNotFoundException ex) {
//            throw new RuntimeException(ex);

        }

        // Add the listener which will export this panel for dragging
        this.addMouseListener(new MouseAdapter() {
            @Override()
            public void mousePressed(MouseEvent e) {
                JComponent c = (JComponent) e.getSource();
                TransferHandler handler = c.getTransferHandler();

                // Copies the actual panel into the transfer handler
                handler.exportAsDrag(c, e, TransferHandler.COPY);
            }
        });

        // Add the handler, which negotiates between drop target and this
        // draggable panel
        setTransferHandler(new ListItemTransferHandler());

        // END Drag and Drop
        initComponents();

        ProcessorXscan.setIcon(jButton2, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/up.png"));
        ProcessorXscan.setIcon(jButton3, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/down.png"));

        this.component = component;
        this.container = container;
        this.jPanel1.add(component, BorderLayout.CENTER);

        defaultBorder = this.getBorder();

//        jButton4.setVisible(false);
//        jButton4.setEnabled(false);
        jButton4.setIcon(null);
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
        jButton4 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jPanel1.setLayout(new java.awt.BorderLayout());

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/close-gray.png"))); // NOI18N
        jButton4.setBorderPainted(false);
        jButton4.setContentAreaFilled(false);
        jButton4.setPreferredSize(new java.awt.Dimension(16, 16));
        jButton4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton4MouseExited(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton4MouseEntered(evt);
            }
        });
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton4);

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(102, 102, 102));
        jLabel1.setText("Label");
        jLabel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jLabel1MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jLabel1MouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jLabel1MousePressed(evt);
            }
        });
        jPanel3.add(jLabel1);

        jPanel2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jPanel2MouseExited(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jPanel2MouseEntered(evt);
            }
        });
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 1, 5));

        jButton2.setBackground(getBackground());
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/up.png"))); // NOI18N
        jButton2.setBorderPainted(false);
        jButton2.setContentAreaFilled(false);
        jButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton2.setPreferredSize(new java.awt.Dimension(16, 16));
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton2MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton2MouseExited(evt);
            }
        });
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton2);

        jButton3.setBackground(getBackground());
        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/xscan/ui/icons/down.png"))); // NOI18N
        jButton3.setBorderPainted(false);
        jButton3.setContentAreaFilled(false);
        jButton3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton3.setPreferredSize(new java.awt.Dimension(16, 16));
        jButton3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton3MouseExited(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton3MouseEntered(evt);
            }
        });
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseEntered
        highlight(true);
    }//GEN-LAST:event_jButton2MouseEntered

    private void jButton2MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseExited
        highlight(false);
    }//GEN-LAST:event_jButton2MouseExited

    private void jButton3MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton3MouseEntered
        highlight(true);
    }//GEN-LAST:event_jButton3MouseEntered

    private void jButton3MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton3MouseExited
        highlight(false);
    }//GEN-LAST:event_jButton3MouseExited

    private void jPanel2MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MouseExited
        highlight(false);
}//GEN-LAST:event_jPanel2MouseExited

    private void jPanel2MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MouseEntered
        highlight(true);
    }//GEN-LAST:event_jPanel2MouseEntered

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        this.container.moveUp(this);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        this.container.moveDown(this);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jLabel1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel1MouseEntered
        highlight(true);
        out = false;
//        jButton4.setVisible(true);
//        jButton4.setEnabled(true);
        ProcessorXscan.setIcon(jButton4, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/close-gray.png"));
        Timer timer = new Timer(5000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {

                Timer t = (Timer) ae.getSource();
                if (out) {
//                    jButton4.setVisible(false);
//                    jButton4.setEnabled(false);
                    jButton4.setIcon(null); // NOI18N
                } else {
                    t.restart();
                }
            }
        });
        timer.start();

        validate();
        repaint();
    }//GEN-LAST:event_jLabel1MouseEntered

    private void jLabel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel1MouseExited
        highlight(false);
        out = true;
    }//GEN-LAST:event_jLabel1MouseExited

    private void jLabel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel1MousePressed
        TransferHandler handler = this.getTransferHandler();
        // Copies the actual panel into the transfer handler
        handler.exportAsDrag(this, evt, TransferHandler.COPY);
    }//GEN-LAST:event_jLabel1MousePressed

    private void jButton4MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton4MouseEntered
        ProcessorXscan.setIcon(jButton4, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/close.png"));
        out = false;
    }//GEN-LAST:event_jButton4MouseEntered

    private void jButton4MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton4MouseExited
        ProcessorXscan.setIcon(jButton4, getClass().getResource("/ch/psi/pshell/xscan/ui/icons/close-gray.png"));
        out = true;
    }//GEN-LAST:event_jButton4MouseExited

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        this.container.remove(this);
    }//GEN-LAST:event_jButton4ActionPerformed

    volatile boolean out = true;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    // End of variables declaration//GEN-END:variables

    /**
     * Highlight container
     *
     * @param flag
     */
    private void highlight(boolean flag) {
        if (flag) {
            this.setBorder(new LineBorder(Color.yellow));
        } else {
            this.setBorder(defaultBorder);
        }
    }

    /**
     * Set label of item
     *
     * @param label
     */
    public void setLabel(String label) {
        this.jLabel1.setText(label);
    }

    /**
     * @return Component of this item
     */
    public Component getComponent() {
        return (component);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) {
        if (flavor.equals(this.flavor)) {
            return (this);
        }
        return null;

    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{this.flavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (flavor.equals(this.flavor)) {
            return true;
        }
        return false;
    }
}
