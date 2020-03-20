package ch.psi.pshell.ui;

import ch.psi.pshell.core.Configuration;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.core.ContextListener;
import ch.psi.pshell.core.Setup;
import ch.psi.utils.Config;
import ch.psi.utils.State;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class DataFileDialog extends StandardDialog {

    final Configuration config;
    final Setup setup;
    final String defaultPath;    
    int sequential;
            
    public DataFileDialog(java.awt.Frame parent, boolean modal) {
        super(parent, "Data File Configuration", modal);
        initComponents();        
        config = Context.getInstance().getConfig();
        setup = Context.getInstance().getSetup();
        try {
            Config.Defaults defaults = config.getDefaults("dataProvider");
            comboProvider.setModel(new javax.swing.DefaultComboBoxModel<>(defaults.values()));
        } catch (Exception ex) {
            Logger.getLogger(DataFileDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Config.Defaults defaults = config.getDefaults("dataLayout");
            comboLayout.setModel(new javax.swing.DefaultComboBoxModel<>(defaults.values()));

        } catch (Exception ex) {
            Logger.getLogger(DataFileDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        defaultPath = new Configuration().dataPath; 
        textPathConfig.setText(config.dataPath.trim());
        comboProvider.setSelectedItem(config.dataProvider.trim());
        comboLayout.setSelectedItem(config.dataLayout.trim());
        sequential = Context.getInstance().getFileSequentialNumber();
        update();                
    }
    
    final ContextListener contextListener = new ContextAdapter() {
        @Override
        public void onContextStateChanged(State state, State former) {
            if ((dialogTokens!=null) && (dialogTokens.isShowing())){
                ((DefaultTableModel)tableTokens.getModel()).setDataVector(getTokenData(), 
                        SwingUtils.getTableColumnNames(tableTokens));
                setTokensTableColumnSizes(tableTokens);
            }    
            sequential = Context.getInstance().getFileSequentialNumber();
            try {
                spinnerSeq.setValue(sequential);
            } catch (Exception ex) {        
            }        
            
            updateButtons();
        }
    };
    
    void setTokensTableColumnSizes(JTable tableTokens){
        tableTokens.getColumnModel().getColumn(0).setPreferredWidth(75);
        tableTokens.getColumnModel().getColumn(1).setPreferredWidth(125);
        tableTokens.getColumnModel().getColumn(2).setPreferredWidth(400);                  
    }
    
    @Override
    protected void onOpened() {
        Context.getInstance().addListener(contextListener);
        contextListener.onContextStateChanged(Context.getInstance().getState(), null);
    }

    @Override
    protected void onClosed() {
        Context.getInstance().removeListener(contextListener);
    }

    
    
    Object[][] getTokenData(){
        Object[][] tokenData= new Object[][]{
            new Object[]{Setup.TOKEN_DATA, "", "Data root folder"},
            new Object[]{Setup.TOKEN_USER, "", "Application user name"},
            new Object[]{Setup.TOKEN_DATE, "", "Current date (format: YYYYMMDD)"},
            new Object[]{Setup.TOKEN_TIME, "", "Current time (format: HHMMSS)"},
            new Object[]{Setup.TOKEN_YEAR, "", "Current year"},
            new Object[]{Setup.TOKEN_MONTH, "", "Current month"},
            new Object[]{Setup.TOKEN_DAY, "", "Current day"},
            new Object[]{Setup.TOKEN_HOUR, "", "Current hours"},
            new Object[]{Setup.TOKEN_MIN, "", "Current minutes"},
            new Object[]{Setup.TOKEN_SEC, "", "Current seconds"},
            new Object[]{Setup.TOKEN_EXEC_NAME, "", "Running script name or, if set, the 'name' parameter"},
            new Object[]{Setup.TOKEN_EXEC_TYPE, "", "'type' parameter if set, otherwise empty string"},
            new Object[]{Setup.TOKEN_EXEC_COUNT, "", "Scan index since 'reset' parameter was set  (formatter can be appended: " + Setup.TOKEN_EXEC_COUNT + "%02d )"},
            new Object[]{Setup.TOKEN_EXEC_INDEX, "", "Scan index  (formatter can be appended: " + Setup.TOKEN_EXEC_INDEX + "%02d )"},
            new Object[]{Setup.TOKEN_FILE_SEQUENTIAL_NUMBER, "", "Execution sequential number (formatter can be appended: " + Setup.TOKEN_FILE_SEQUENTIAL_NUMBER + "%02d )"},
            new Object[]{Setup.TOKEN_SYS_HOME, "", "System user name"},
            new Object[]{Setup.TOKEN_SYS_USER, "", "System user home folder"},
            
        };
        
        for (Object[] row: tokenData){
            row[1] = setup.expandPath(String.valueOf(row[0]));
        }
        
        return tokenData;        
    }
    
    void updateButtons(){
        buttonPathDefault.setEnabled(!textPathConfig.getText().trim().equals(defaultPath));
        buttonPathUndo.setEnabled(!textPathConfig.getText().trim().equals(config.dataPath.trim()));
        buttonProviderUndo.setEnabled(!String.valueOf(comboProvider.getSelectedItem()).trim().equals(config.dataProvider.trim()));
        buttonLayoutUndo.setEnabled(!String.valueOf(comboLayout.getSelectedItem()).trim().equals(config.dataLayout.trim()));
        buttonSeqUndo.setEnabled(!spinnerSeq.getValue().equals(sequential));
        buttonResetSeq.setEnabled(!spinnerSeq.getValue().equals(0));
        buttonApply.setEnabled(buttonPathUndo.isEnabled() || buttonProviderUndo.isEnabled() || buttonLayoutUndo.isEnabled() || buttonSeqUndo.isEnabled());        
    }
        
    void update(){
        updateButtons();
        try{
            textPathExpansion.setText(setup.expandPath(textPathConfig.getText().trim()));
        } catch (Exception ex){
             textPathExpansion.setText("");
        }        
    }
    
    JTable tableTokens;
    JDialog dialogTokens;
    void showTokens(){
        if ((dialogTokens!=null) && (dialogTokens.isShowing())){
            dialogTokens.setVisible(false);
        }
        JScrollPane scrollTokens = new JScrollPane();
        tableTokens = new JTable();        
        tableTokens.setModel(new DefaultTableModel(
            getTokenData(),
            new String [] {"Token", "Expansion", "Description"}
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableTokens.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableTokens.setCellSelectionEnabled(true);
        tableTokens.getTableHeader().setReorderingAllowed(false);
        scrollTokens.setViewportView(tableTokens);
        
        dialogTokens = SwingUtils.showDialog(this, "Expansion Tokens", new Dimension(600, 400), scrollTokens);
        setTokensTableColumnSizes(tableTokens);        
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        textPathConfig = new javax.swing.JTextField();
        textPathExpansion = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        buttonPathDefault = new javax.swing.JButton();
        buttonPathUndo = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        buttonResetSeq = new javax.swing.JButton();
        spinnerSeq = new javax.swing.JSpinner();
        buttonTokens = new javax.swing.JButton();
        buttonSeqUndo = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        buttonProviderUndo = new javax.swing.JButton();
        comboProvider = new javax.swing.JComboBox<>();
        comboLayout = new javax.swing.JComboBox<>();
        buttonLayoutUndo = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        buttonApply = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Data Path"));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Configuration:");

        textPathConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textPathConfigActionPerformed(evt);
            }
        });
        textPathConfig.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textPathConfigKeyReleased(evt);
            }
        });

        textPathExpansion.setDisabledTextColor(javax.swing.UIManager.getDefaults().getColor("TextField.foreground"));
        textPathExpansion.setEnabled(false);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Expansion:");

        buttonPathDefault.setText("Default");
        buttonPathDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPathDefaultActionPerformed(evt);
            }
        });

        buttonPathUndo.setText("Undo");
        buttonPathUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPathUndoActionPerformed(evt);
            }
        });

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Sequential:");

        buttonResetSeq.setText("Reset");
        buttonResetSeq.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetSeqActionPerformed(evt);
            }
        });

        spinnerSeq.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000000000, 1));
        spinnerSeq.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerSeqStateChanged(evt);
            }
        });

        buttonTokens.setText("Tokens...");
        buttonTokens.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonTokensActionPerformed(evt);
            }
        });

        buttonSeqUndo.setText("Undo");
        buttonSeqUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSeqUndoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(buttonTokens)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 191, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(buttonResetSeq)
                                .addGap(5, 5, 5))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(buttonPathDefault)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buttonPathUndo)
                            .addComponent(buttonSeqUndo, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addComponent(textPathConfig)
                    .addComponent(textPathExpansion))
                .addContainerGap())
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerSeq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonPathDefault, buttonPathUndo, buttonResetSeq, buttonSeqUndo});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel5});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textPathConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textPathExpansion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonPathDefault)
                    .addComponent(buttonPathUndo)
                    .addComponent(buttonTokens))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(buttonResetSeq)
                    .addComponent(spinnerSeq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSeqUndo))
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Data Format"));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Format Provider:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Data Layout:");

        buttonProviderUndo.setText("Undo");
        buttonProviderUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonProviderUndoActionPerformed(evt);
            }
        });

        comboProvider.setEditable(true);
        comboProvider.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboProviderActionPerformed(evt);
            }
        });

        comboLayout.setEditable(true);
        comboLayout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboLayoutActionPerformed(evt);
            }
        });

        buttonLayoutUndo.setText("Undo");
        buttonLayoutUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLayoutUndoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(comboProvider, 0, 245, Short.MAX_VALUE)
                    .addComponent(comboLayout, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonProviderUndo, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonLayoutUndo))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel3, jLabel4});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(comboProvider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(buttonProviderUndo)
                    .addComponent(jLabel3))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(buttonLayoutUndo))
                .addGap(7, 7, 7))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {comboLayout, comboProvider});

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonApply.setText("Apply");
        buttonApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonApplyActionPerformed(evt);
            }
        });

        buttonOk.setText("Ok");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonCancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonApply)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonOk)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel)
                    .addComponent(buttonApply))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonApplyActionPerformed
        try {
            if (buttonPathUndo.isEnabled() || buttonProviderUndo.isEnabled() || buttonLayoutUndo.isEnabled()){
                config.dataPath =textPathConfig.getText().trim();
                config.dataProvider = String.valueOf(comboProvider.getSelectedItem()).trim();
                config.dataLayout = String.valueOf(comboLayout.getSelectedItem()).trim();
                config.save();
            }
            if (buttonSeqUndo.isEnabled()){
                Context.getInstance().setFileSequentialNumber((Integer)spinnerSeq.getValue());
            }
            update();
            
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonApplyActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        if (buttonApply.isEnabled()){
            buttonApplyActionPerformed(evt);
        }        
        accept();
    }//GEN-LAST:event_buttonOkActionPerformed

    private void comboProviderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboProviderActionPerformed
        updateButtons();
    }//GEN-LAST:event_comboProviderActionPerformed

    private void buttonPathDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPathDefaultActionPerformed
        textPathConfig.setText(defaultPath);
        update();
    }//GEN-LAST:event_buttonPathDefaultActionPerformed

    private void buttonPathUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPathUndoActionPerformed
        textPathConfig.setText(config.dataPath.trim());
        update();
    }//GEN-LAST:event_buttonPathUndoActionPerformed

    private void buttonProviderUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonProviderUndoActionPerformed
        comboProvider.setSelectedItem(config.dataProvider.trim());
        updateButtons();
    }//GEN-LAST:event_buttonProviderUndoActionPerformed

    private void buttonLayoutUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLayoutUndoActionPerformed
        comboLayout.setSelectedItem(config.dataLayout.trim());
        updateButtons();
    }//GEN-LAST:event_buttonLayoutUndoActionPerformed

    private void textPathConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textPathConfigActionPerformed
        update();
    }//GEN-LAST:event_textPathConfigActionPerformed

    private void textPathConfigKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textPathConfigKeyReleased
        update();
    }//GEN-LAST:event_textPathConfigKeyReleased

    private void buttonResetSeqActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetSeqActionPerformed
        spinnerSeq.setValue(0);
        updateButtons();
    }//GEN-LAST:event_buttonResetSeqActionPerformed

    private void spinnerSeqStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSeqStateChanged
        updateButtons();
    }//GEN-LAST:event_spinnerSeqStateChanged

    private void buttonTokensActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTokensActionPerformed
        showTokens();
    }//GEN-LAST:event_buttonTokensActionPerformed

    private void comboLayoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboLayoutActionPerformed
        updateButtons();
    }//GEN-LAST:event_comboLayoutActionPerformed

    private void buttonSeqUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSeqUndoActionPerformed
        spinnerSeq.setValue(sequential);
        updateButtons();
    }//GEN-LAST:event_buttonSeqUndoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonApply;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonLayoutUndo;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonPathDefault;
    private javax.swing.JButton buttonPathUndo;
    private javax.swing.JButton buttonProviderUndo;
    private javax.swing.JButton buttonResetSeq;
    private javax.swing.JButton buttonSeqUndo;
    private javax.swing.JButton buttonTokens;
    private javax.swing.JComboBox<String> comboLayout;
    private javax.swing.JComboBox<String> comboProvider;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSpinner spinnerSeq;
    private javax.swing.JTextField textPathConfig;
    private javax.swing.JTextField textPathExpansion;
    // End of variables declaration//GEN-END:variables
}
