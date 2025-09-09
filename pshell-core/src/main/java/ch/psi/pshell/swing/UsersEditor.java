package ch.psi.pshell.swing;

import ch.psi.pshell.security.AccessLevel;
import ch.psi.pshell.security.Rights;
import ch.psi.pshell.security.User;
import ch.psi.pshell.security.Security;
import ch.psi.pshell.swing.ConfigDialog;
import ch.psi.pshell.swing.ConfigDialog;
import ch.psi.pshell.swing.Document;
import ch.psi.pshell.swing.Document;
import ch.psi.pshell.swing.Editor;
import ch.psi.pshell.swing.Editor;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Convert;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.WindowConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import ch.psi.pshell.security.SecurityListener;

/**
 *
 */
public class UsersEditor extends Editor {

    final DefaultTableModel model;
    final Security securityManager;

    public UsersEditor(Security securityManager) {
        super(new UsersEditorDocument());
        this.securityManager = securityManager;
        ((UsersEditorDocument) getDocument()).editor = this;
        initComponents();

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "Name", "Access Level", "Authentication", "Auto Logon"
                }
        ) {
            Class[] types = new Class[]{
                java.lang.String.class, AccessLevel.class, java.lang.Boolean.class, java.lang.Boolean.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });

        TableColumn column = table.getColumnModel().getColumn(1);
        JComboBox combo = new JComboBox();
        SwingUtils.setEnumCombo(combo, AccessLevel.class);
        column.setCellEditor(new DefaultCellEditor(combo));

        model = (DefaultTableModel) table.getModel();
        model.addTableModelListener((TableModelEvent e) -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (col == 3) {
                boolean autoLogon = (Boolean) model.getValueAt(row, 3);
                if (autoLogon) {
                    for (int i = 0; i < model.getRowCount(); i++) {
                        if (i != row) {
                            model.setValueAt(Boolean.FALSE, i, 3);
                        } else {
                            model.setValueAt(Boolean.FALSE, i, 2);   //Auto-logon must not be authenticated
                        }
                    }
                }
            }
            getDocument().setChanged(true);
        });
        update();

        securityManager.addListener(securityManagerListener);
    }

    SecurityListener securityManagerListener = new SecurityListener() {
        @Override
        public void onUserChange(User user, User former) {
            closeWindow(true);
            securityManager.removeListener(this);
        }
    };

    @Override
    protected void update() {
        buttonDelete.setEnabled(model.getRowCount() > 0);
    }

    public static class UsersEditorDocument extends Document {

        UsersEditor editor;

        @Override
        public void clear() {
            editor.model.setNumRows(0);
            //Fix bug of nimbus rendering Boolean in table
            ((JComponent) editor.table.getDefaultRenderer(Boolean.class)).setOpaque(true);
            setChanged(false);
        }

        @Override
        public void load(String fileName) throws IOException {
            clear();
            editor.setFileName(editor.securityManager.getUsersFile().toString());
            for (User user : editor.securityManager.getUsers()) {
                editor.model.addRow(new Object[]{user.name, user.accessLevel, user.authentication, user.autoLogon});
            }
            setChanged(false);
        }

        @Override
        public void save(String fileName) throws IOException {
            ArrayList<User> users = new ArrayList<>();
            boolean hasAdmin = false;
            boolean emptyName = false;
            int autoLogon = 0;
            boolean autoLogonAuthenticated = false;

            for (int i = 0; i < editor.model.getRowCount(); i++) {
                User user = new User();
                user.name = ((String) editor.model.getValueAt(i, 0)).trim();
                if (user.name.trim().isEmpty()) {
                    emptyName = true;
                }
                user.accessLevel = ((AccessLevel) editor.model.getValueAt(i, 1));
                user.authentication = ((Boolean) editor.model.getValueAt(i, 2));
                user.autoLogon = (Boolean) editor.model.getValueAt(i, 3);
                users.add(user);

                if (user.autoLogon) {
                    autoLogon++;
                    autoLogonAuthenticated = user.authentication;
                }
                if (user.accessLevel == AccessLevel.administrator) {
                    hasAdmin = true;
                }
            }
            if (emptyName) {
                throw new IOException("User name must not be empty");
            }
            if (!hasAdmin) {
                throw new IOException("At least one administrative user must be defined");
            }
            if (autoLogon != 1) {
                throw new IOException("One user must be set as Auto Logon");
            }
            if (autoLogonAuthenticated) {
                throw new IOException("Auto Logon user must not be authenticated");
            }

            editor.securityManager.setUsers(users.toArray(new User[0]));
            setChanged(false);
        }
    }

    @Override
    public void setReadOnly(boolean value) {
        table.setEnabled(!value);
        buttonInsert.setEnabled(!value);
        buttonDelete.setEnabled(!value);
    }

    @Override
    public boolean isReadOnly() {
        return !table.isEnabled();
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
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();
        buttonRigths = new javax.swing.JButton();

        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setResizable(false);
        }

        buttonInsert.setText("Insert");
        buttonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });

        buttonDelete.setText("Delete");
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        buttonRigths.setText("Rights");
        buttonRigths.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRigthsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonRigths)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonInsert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDelete)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonInsert, buttonRigths});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonRigths))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        model.insertRow(table.getSelectedRow() + 1, new Object[]{"", AccessLevel.standard, Boolean.FALSE, Boolean.FALSE});
        update();
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (model.getRowCount() > 0) {
            model.removeRow(Math.max(table.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonRigthsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRigthsActionPerformed
        try {
            String ret = getString("Select Level", Convert.toStringArray(AccessLevel.values()), null);
            //if (!current.equals(branch)) {
            if (ret != null) {
                AccessLevel level = AccessLevel.valueOf(ret);
                final ConfigDialog dlg = new ConfigDialog(null, true);
                dlg.setTitle("Rights Configuration: " + level);
                final Rights rights = new Rights();
                rights.load(Security.getInstance().getRightsFile(level).toString());
                dlg.setConfig(rights);
                dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                dlg.setListener((StandardDialog sd, boolean accepted) -> {
                    if (sd.getResult()) {
                        try {
                            rights.save();
                        } catch (IOException ex) {
                            Logger.getLogger(UsersEditor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                dlg.setSize(360, 480);
                SwingUtils.centerComponent(this, dlg);
                dlg.setVisible(true);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonRigthsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonRigths;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
