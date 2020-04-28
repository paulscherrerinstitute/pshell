package ch.psi.pshell.swing;

import ch.psi.pshell.core.JsonSerializer;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.ui.App.ExecutionStage;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class NextStagesPanel extends MonitoredPanel{

    final DefaultTableModel model;

    public NextStagesPanel() {
        initComponents();
        model = (DefaultTableModel) table.getModel();
        setupMenu();
    }
    JPopupMenu popupMenu;
    JMenuItem menuCancel;
    JMenuItem menuCancelAll;

    final void setupMenu() {
        popupMenu = new JPopupMenu();
        menuCancel = new JMenuItem("Cancel");
        menuCancelAll = new JMenuItem("Cancel All");

        menuCancel.addActionListener((ActionEvent e) -> {
            
            try {
                int index = table.getSelectedRow();
                App.getInstance().cancelExecutionStage(index);
            } catch (Exception ex) {
                SwingUtils.showException(NextStagesPanel.this, ex);
            }
        });

        menuCancelAll.addActionListener((ActionEvent e) -> {
            
            try {
                App.getInstance().cancelExecutionQueue();
            } catch (Exception ex) {
                SwingUtils.showException(NextStagesPanel.this, ex);
            }
        });

        popupMenu.add(menuCancel);
        popupMenu.addSeparator();
        popupMenu.add(menuCancelAll);

        
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        int r = table.rowAtPoint(e.getPoint());
                        if (r >= 0 && r < table.getRowCount()) {
                            table.setRowSelectionInterval(r, r);
                        } else {
                            table.clearSelection();
                        }
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }                    
                } catch (Exception ex) {
                    SwingUtils.showException(NextStagesPanel.this, ex);
                }
            }
        }
        );

    }

    public void setExecutionQueue(ExecutionStage[] queue){
        model.setRowCount(0);
        for (ExecutionStage stage : queue){      
            String file = stage.file == null ? "" : stage.file.toString();
            String args = stage.file == null ? stage.statement : stage.getArgsStr();
            model.addRow(new Object[]{file, args});
        }
    }
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "File name", "Arguments"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables

}
