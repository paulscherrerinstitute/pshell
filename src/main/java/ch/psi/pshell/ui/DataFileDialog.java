package ch.psi.pshell.ui;

import ch.psi.pshell.core.Configuration;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.pshell.core.ContextListener;
import ch.psi.pshell.core.Setup;
import ch.psi.pshell.data.DataManager;
import ch.psi.pshell.data.Provider;
import ch.psi.pshell.data.RSync;
import ch.psi.utils.Config;
import ch.psi.utils.State;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class DataFileDialog extends StandardDialog {

    final Configuration config;
    final Setup setup;
    final String defaultPath;
    Provider provider;

    public DataFileDialog(java.awt.Frame parent, boolean modal) {
        super(parent, "Data Setup", modal);
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
        SwingUtils.setEnumCombo(comboTransferMode, Configuration.DataTransferMode.class);
        SwingUtils.setEnumCombo(comboNotification, Configuration.NotificationLevel.class);
        buttonUndoActionPerformed(null);
    }

    final ContextListener contextListener = new ContextAdapter() {
        @Override
        public void onContextStateChanged(State state, State former) {
            if ((dialogTokens != null) && (dialogTokens.isShowing())) {
                ((DefaultTableModel) tableTokens.getModel()).setDataVector(getTokenData(),
                        SwingUtils.getTableColumnNames(tableTokens));
                setTokensTableColumnSizes(tableTokens);
            }
            try {
                spinnerSeq.setValue(Context.getInstance().getFileSequentialNumber());
            } catch (Exception ex) {
            }

            updateButtons();
        }
    };

    void setTokensTableColumnSizes(JTable tableTokens) {
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

    Object[][] getTokenData() {
        Object[][] tokenData = new Object[][]{
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
            new Object[]{Setup.TOKEN_DAY_SEQUENTIAL_NUMBER, "", "Daily run index (formatter can be appended: " + Setup.TOKEN_DAY_SEQUENTIAL_NUMBER + "%02d )"},
            new Object[]{Setup.TOKEN_SYS_HOME, "", "System user name"},
            new Object[]{Setup.TOKEN_SYS_USER, "", "System user home folder"},
            new Object[]{Setup.TOKEN_SESSION_ID, "", "Session ID (formatter can be appended: " + Setup.TOKEN_SESSION_ID + "%02d )"},
            new Object[]{Setup.TOKEN_SESSION_NAME, "", "Session Name"},};

        for (Object[] row : tokenData) {
            row[1] = setup.expandPath(String.valueOf(row[0]));
        }

        return tokenData;
    }

    boolean changedSeq() {
        return !spinnerSeq.getValue().equals(Context.getInstance().getFileSequentialNumber());
    }

    boolean changedLocation() {
        return (!textPathConfig.getText().trim().equals(config.dataPath.trim()));

    }

    boolean changedProvider() {
        return !String.valueOf(comboProvider.getSelectedItem()).trim().equals(config.dataProvider.trim());
    }

    boolean changedLayout() {
        return !String.valueOf(comboLayout.getSelectedItem()).trim().equals(config.dataLayout.trim());
    }
    
    boolean changedDataLogs() {
        return  (checkDataLogs.isSelected() == config.disableDataFileLogs) ;
    }    

    boolean changedFormat() {
        return (changedProvider() || changedLayout() || changedDataLogs() 
                || (checkDataLogs.isSelected() == config.disableDataFileLogs) 
                || (checkEmbeddedAttrs.isSelected() == config.disableEmbeddedAttributes)                 
                || !spinnerDepthDim.getValue().equals(config.depthDimension));

    }

    boolean changedScans() {
        return (ckAutoSave.isSelected() != config.autoSaveScanData)
                || (ckFlush.isSelected() != config.dataScanFlushRecords)
                || (ckKeep.isSelected() == config.dataScanReleaseRecords)
                || (ckSaveConsole.isSelected() != config.dataScanSaveOutput)
                || (ckSaveScript.isSelected() != config.dataScanSaveScript)
                || (ckSaveSetpoints.isSelected() != config.dataScanSaveSetpoints)
                || (ckConvert.isSelected() == config.dataScanPreserveTypes);

    }

    boolean changedTransfer() {
        return (config.getDataTransferMode() != comboTransferMode.getSelectedItem())
                || !textTransferPath.getText().trim().equals(config.getDataTransferPath())
                || !textTransferUser.getText().trim().equals(config.getDataTransferUser());
    }

    boolean changedNotify() {
        return (config.notificationLevel != comboNotification.getSelectedItem())
                || !textTasks.getText().trim().equals(config.notifiedTasks);
    }

    boolean changedNotificationManager() {
        return !textRecipients.getText().trim().equals(Context.getInstance().getNotificationManager().getConfig().to);
    }

    boolean changedConfig() {
        return changedLocation() || changedFormat() || changedScans() || changedTransfer() || changedNotify();
    }

    boolean changed() {
        return changedSeq() || changedConfig() || changedNotificationManager();
    }

    void updateButtons() {
        buttonPathDefault.setEnabled(!textPathConfig.getText().trim().equals(defaultPath));
        buttonResetSeq.setEnabled(!spinnerSeq.getValue().equals(0));

        boolean change = changed();
        buttonUndo.setEnabled(change);
        buttonApply.setEnabled(change);
        textDayIndex.setText(String.valueOf(Context.getInstance().getDaySequentialNumber()));

    }

    boolean updatingControls;

    void update() {
        updateButtons();
        try {
            String path = setup.expandPath(textPathConfig.getText().trim());
            if (provider != null) {
                path = path + (provider.isPacked() ? ("." + provider.getFileType()) : "/");
            }
            textPathExpansion.setText(path);

        } catch (Exception ex) {
            textPathExpansion.setText("");
        }
    }

    void updateProvider() {
        try {
            provider = ((Provider) DataManager.getProviderClass(String.valueOf(comboProvider.getSelectedItem())).newInstance());
        } catch (Exception ex) {
            provider = null;
        }
    }

    void updateScans() {
        ckAutoSave.setSelected(config.autoSaveScanData);
        ckConvert.setSelected(!config.dataScanPreserveTypes);
        ckFlush.setSelected(config.dataScanFlushRecords);
        ckKeep.setSelected(!config.dataScanReleaseRecords);
        ckSaveConsole.setSelected(config.dataScanSaveOutput);
        ckSaveScript.setSelected(config.dataScanSaveScript);
        ckSaveSetpoints.setSelected(config.dataScanSaveSetpoints);
        spinnerDepthDim.setValue((config.depthDimension > 2) ? 0 : Math.max(config.depthDimension, 0));
        checkDataLogs.setSelected(!config.disableDataFileLogs);
        checkEmbeddedAttrs.setSelected(!config.disableEmbeddedAttributes);
    }

    void updateTransfer() {
        comboTransferMode.setSelectedItem(config.getDataTransferMode());
        textTransferPath.setText(config.getDataTransferPath());
        textTransferUser.setText(config.getDataTransferUser());
        new Thread(() -> {
            try {
                Context.getInstance().waitStateNot(State.Initializing, 120000);
                boolean authorized = isAuthorized();
                SwingUtilities.invokeLater(() -> {
                    checkRSyncAuthorized.setSelected(authorized);
                });
            } catch (Exception ex) {
                Logger.getLogger(DataFileDialog.class.getName()).log(Level.WARNING, null, ex);
            }
        }).start();

    }

    void updateNotify() {
        comboNotification.setSelectedItem(config.notificationLevel);
        textTasks.setText(config.notifiedTasks);
        textRecipients.setText(Context.getInstance().getNotificationManager().getConfig().to);
    }

    JTable tableTokens;
    JDialog dialogTokens;

    void showTokens() {
        if ((dialogTokens != null) && (dialogTokens.isShowing())) {
            dialogTokens.setVisible(false);
        }
        JScrollPane scrollTokens = new JScrollPane();
        tableTokens = new JTable();
        tableTokens.setModel(new DefaultTableModel(
                getTokenData(),
                new String[]{"Token", "Expansion", "Description"}
        ) {
            Class[] types = new Class[]{
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tableTokens.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableTokens.setCellSelectionEnabled(true);
        tableTokens.getTableHeader().setReorderingAllowed(false);
        scrollTokens.setViewportView(tableTokens);

        dialogTokens = SwingUtils.showDialog(this, "Expansion Tokens", new Dimension(600, 400), scrollTokens);
        setTokensTableColumnSizes(tableTokens);
    }

    boolean isAuthorized() {
        try {
            return RSync.isAuthorized();
        } catch (Exception e) {
            Logger.getLogger(DataFileDialog.class.getName()).log(Level.WARNING, null, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonCancel = new javax.swing.JButton();
        buttonApply = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        textPathConfig = new javax.swing.JTextField();
        textPathExpansion = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        buttonPathDefault = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        buttonResetSeq = new javax.swing.JButton();
        spinnerSeq = new javax.swing.JSpinner();
        buttonTokens = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        textDayIndex = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        comboProvider = new javax.swing.JComboBox<>();
        comboLayout = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        spinnerDepthDim = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        checkDataLogs = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        checkEmbeddedAttrs = new javax.swing.JCheckBox();
        jLabel12 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        ckAutoSave = new javax.swing.JCheckBox();
        ckConvert = new javax.swing.JCheckBox();
        ckKeep = new javax.swing.JCheckBox();
        ckSaveConsole = new javax.swing.JCheckBox();
        ckFlush = new javax.swing.JCheckBox();
        ckSaveSetpoints = new javax.swing.JCheckBox();
        ckSaveScript = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        textTransferPath = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        comboTransferMode = new javax.swing.JComboBox<>();
        jPanel1 = new javax.swing.JPanel();
        buttonRSyncRemove = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        textTransferUser = new javax.swing.JTextField();
        buttonRSyncAuthorize = new javax.swing.JButton();
        checkRSyncAuthorized = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        comboNotification = new javax.swing.JComboBox<>();
        jLabel14 = new javax.swing.JLabel();
        textRecipients = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        textTasks = new javax.swing.JTextField();
        buttonUndo = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

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

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Pattern");

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

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Day Index:");

        textDayIndex.setEditable(false);
        textDayIndex.setHorizontalAlignment(javax.swing.JTextField.TRAILING);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(buttonTokens)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 174, Short.MAX_VALUE)
                        .addComponent(buttonPathDefault))
                    .addComponent(textPathExpansion)
                    .addComponent(textPathConfig)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(textDayIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(spinnerSeq, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonResetSeq)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonPathDefault, buttonResetSeq});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel6});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonTokens, spinnerSeq, textDayIndex});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textPathConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonPathDefault)
                    .addComponent(buttonTokens))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textPathExpansion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(textDayIndex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(buttonResetSeq)
                    .addComponent(spinnerSeq, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Location", jPanel2);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("File format:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Data layout:");

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

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Depth dimension:");

        spinnerDepthDim.setModel(new javax.swing.SpinnerNumberModel(0, 0, 2, 1));
        spinnerDepthDim.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerDepthDimStateChanged(evt);
            }
        });

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Create data logs:");

        checkDataLogs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkDataLogsActionPerformed(evt);
            }
        });

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText("Embedded attibutes:");

        checkEmbeddedAttrs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkEmbeddedAttrsActionPerformed(evt);
            }
        });

        jLabel12.setText("(on text files)");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spinnerDepthDim, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboProvider, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkDataLogs)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(checkEmbeddedAttrs)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel12)))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel3, jLabel4, jLabel7});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel3)
                    .addComponent(comboProvider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel4)
                    .addComponent(comboLayout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel8)
                    .addComponent(checkDataLogs))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel10)
                    .addComponent(checkEmbeddedAttrs)
                    .addComponent(jLabel12))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinnerDepthDim, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {comboLayout, comboProvider});

        jTabbedPane1.addTab("Format", jPanel3);

        ckAutoSave.setText("Automatically save scan data files");
        ckAutoSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAutoSaveActionPerformed(evt);
            }
        });

        ckConvert.setText("Sensors with undefined type have datasets set to double");
        ckConvert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAutoSaveActionPerformed(evt);
            }
        });

        ckKeep.setText("Keep scan data in memory");
        ckKeep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAutoSaveActionPerformed(evt);
            }
        });

        ckSaveConsole.setText("Save console output to data file");
        ckSaveConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAutoSaveActionPerformed(evt);
            }
        });

        ckFlush.setText("Flush data on each record");
        ckFlush.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAutoSaveActionPerformed(evt);
            }
        });

        ckSaveSetpoints.setText("Save positioner setpoints (in addition to readbacks)");
        ckSaveSetpoints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAutoSaveActionPerformed(evt);
            }
        });

        ckSaveScript.setText("Save script to data file");
        ckSaveScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAutoSaveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ckSaveScript)
                    .addComponent(ckConvert)
                    .addComponent(ckKeep)
                    .addComponent(ckSaveConsole)
                    .addComponent(ckFlush)
                    .addComponent(ckSaveSetpoints)
                    .addComponent(ckAutoSave))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ckAutoSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ckKeep)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ckFlush)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ckConvert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ckSaveScript)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ckSaveConsole)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ckSaveSetpoints)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Scans", jPanel4);

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel18.setText("Path:");

        textTransferPath.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textTransferPathKeyReleased(evt);
            }
        });

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel19.setText("Mode:");

        comboTransferMode.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboTransferMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboTransferModeActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("RSync"));

        buttonRSyncRemove.setText("Remove");
        buttonRSyncRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRSyncRemoveActionPerformed(evt);
            }
        });

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("User:");

        textTransferUser.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textTransferUserKeyReleased(evt);
            }
        });

        buttonRSyncAuthorize.setText("Authorize");
        buttonRSyncAuthorize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRSyncAuthorizeActionPerformed(evt);
            }
        });

        checkRSyncAuthorized.setEnabled(false);

        jLabel9.setText("Authorized");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRSyncAuthorize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRSyncRemove)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(checkRSyncAuthorized)
                .addGap(0, 0, 0)
                .addComponent(jLabel9)
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(44, 44, 44)
                .addComponent(textTransferUser)
                .addGap(6, 6, 6))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonRSyncAuthorize, buttonRSyncRemove});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(textTransferUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel9)
                    .addComponent(checkRSyncAuthorized)
                    .addComponent(buttonRSyncAuthorize)
                    .addComponent(buttonRSyncRemove))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textTransferPath, javax.swing.GroupLayout.DEFAULT_SIZE, 404, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboTransferMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(comboTransferMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textTransferPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18))
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Transfer", jPanel5);

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel13.setText("Mode:");

        comboNotification.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboNotification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboNotificationActionPerformed(evt);
            }
        });

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel14.setText("Recipients:");

        textRecipients.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textRecipientsKeyReleased(evt);
            }
        });

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Notified Tasks:");

        textTasks.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textTasksKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(comboNotification, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 243, Short.MAX_VALUE))
                            .addComponent(textRecipients)))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textTasks)))
                .addContainerGap())
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel13, jLabel14, jLabel15});

        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(comboNotification, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(textRecipients, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(textTasks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(78, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Notification", jPanel6);

        buttonUndo.setText("Undo");
        buttonUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUndoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonCancel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonUndo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonApply)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonOk)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jTabbedPane1)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonApply, buttonCancel, buttonOk, buttonUndo});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel)
                    .addComponent(buttonApply)
                    .addComponent(buttonUndo))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonApplyActionPerformed
        try {
            if (changedSeq()) {
                Context.getInstance().setFileSequentialNumber((Integer) spinnerSeq.getValue());
            }
            if (changedConfig()) {
                boolean changedFormat = changedFormat();
                config.dataPath = textPathConfig.getText().trim();
                config.dataProvider = String.valueOf(comboProvider.getSelectedItem()).trim();
                config.dataLayout = String.valueOf(comboLayout.getSelectedItem()).trim();
                config.autoSaveScanData = ckAutoSave.isSelected();
                config.dataScanPreserveTypes = !ckConvert.isSelected();
                config.dataScanFlushRecords = ckFlush.isSelected();
                config.dataScanReleaseRecords = !ckKeep.isSelected();
                config.dataScanSaveOutput = ckSaveConsole.isSelected();
                config.dataScanSaveScript = ckSaveScript.isSelected();
                config.dataScanSaveSetpoints = ckSaveSetpoints.isSelected();
                config.depthDimension = (Integer) spinnerDepthDim.getValue();
                config.disableDataFileLogs = !checkDataLogs.isSelected();
                config.disableEmbeddedAttributes = !checkEmbeddedAttrs.isSelected();                
                config.dataTransferMode = (Configuration.DataTransferMode) comboTransferMode.getSelectedItem();
                config.dataTransferPath = textTransferPath.getText().trim();
                config.dataTransferUser = textTransferUser.getText().trim();
                config.notificationLevel = (Configuration.NotificationLevel) comboNotification.getSelectedItem();
                config.notifiedTasks = textTasks.getText().trim();
                config.save();
                if (changedFormat) {
                    Context.getInstance().getDataManager().initialize();
                } 
            }
            if (changedNotificationManager()) {
                String to = textRecipients.getText().trim();
                Context.getInstance().getNotificationManager().setRecipients(to);
            }
            updateTransfer();
            update();

        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonApplyActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        if (buttonApply.isEnabled()) {
            buttonApplyActionPerformed(evt);
        }
        accept();
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUndoActionPerformed
        boolean changedProvider = (provider == null) || (changedProvider());
        textPathConfig.setText(config.dataPath.trim());
        comboProvider.setSelectedItem(config.dataProvider.trim());
        comboLayout.setSelectedItem(config.dataLayout.trim());
        spinnerSeq.setValue(Context.getInstance().getFileSequentialNumber());
        updateScans();
        updateTransfer();
        updateNotify();
        if (changedProvider) {
            updateProvider();
        }
        update();
    }//GEN-LAST:event_buttonUndoActionPerformed

    private void textTransferPathKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textTransferPathKeyReleased
        update();
    }//GEN-LAST:event_textTransferPathKeyReleased

    private void ckAutoSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckAutoSaveActionPerformed
        update();
    }//GEN-LAST:event_ckAutoSaveActionPerformed

    private void spinnerDepthDimStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerDepthDimStateChanged
        update();
    }//GEN-LAST:event_spinnerDepthDimStateChanged

    private void comboLayoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboLayoutActionPerformed
        updateButtons();
    }//GEN-LAST:event_comboLayoutActionPerformed

    private void comboProviderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboProviderActionPerformed
        updateProvider();
        update();
    }//GEN-LAST:event_comboProviderActionPerformed

    private void buttonTokensActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTokensActionPerformed
        showTokens();
    }//GEN-LAST:event_buttonTokensActionPerformed

    private void spinnerSeqStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSeqStateChanged
        updateButtons();
    }//GEN-LAST:event_spinnerSeqStateChanged

    private void buttonResetSeqActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetSeqActionPerformed
        spinnerSeq.setValue(0);
        updateButtons();
    }//GEN-LAST:event_buttonResetSeqActionPerformed

    private void buttonPathDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPathDefaultActionPerformed
        textPathConfig.setText(defaultPath);
        update();
    }//GEN-LAST:event_buttonPathDefaultActionPerformed

    private void textPathConfigKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textPathConfigKeyReleased
        update();
    }//GEN-LAST:event_textPathConfigKeyReleased

    private void textPathConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textPathConfigActionPerformed
        update();
    }//GEN-LAST:event_textPathConfigActionPerformed

    private void comboTransferModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboTransferModeActionPerformed
        update();
    }//GEN-LAST:event_comboTransferModeActionPerformed

    private void textTransferUserKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textTransferUserKeyReleased
        update();
    }//GEN-LAST:event_textTransferUserKeyReleased

    private void buttonRSyncAuthorizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRSyncAuthorizeActionPerformed
        try {
            String user = textTransferUser.getText().trim();
            if (user.isEmpty()) {
                throw new Exception("invalid user name");
            }
            boolean fixPermissions = false;
            switch (SwingUtils.showOption(this, "Authorize", "Do you want to fix permissions for user " + user + "?\n"
                    + "Some system require no group write access in home folder \n"
                    + "for ssh to work with authorized keys.", OptionType.YesNoCancel)) {
                case Yes:
                    fixPermissions = true;
                    break;
                case No:
                    fixPermissions = false;
                    break;
                default:
                    return;
            }
            RSync.authorize(user, fixPermissions);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        } finally {
            updateTransfer();
        }
    }//GEN-LAST:event_buttonRSyncAuthorizeActionPerformed

    private void buttonRSyncRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRSyncRemoveActionPerformed
        try {
            RSync.removeAuthorization();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        } finally {
            updateTransfer();
        }
    }//GEN-LAST:event_buttonRSyncRemoveActionPerformed

    private void textRecipientsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textRecipientsKeyReleased
        update();
    }//GEN-LAST:event_textRecipientsKeyReleased

    private void textTasksKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textTasksKeyReleased
        update();
    }//GEN-LAST:event_textTasksKeyReleased

    private void comboNotificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboNotificationActionPerformed
        update();
    }//GEN-LAST:event_comboNotificationActionPerformed

    private void checkDataLogsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkDataLogsActionPerformed
       update();
    }//GEN-LAST:event_checkDataLogsActionPerformed

    private void checkEmbeddedAttrsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkEmbeddedAttrsActionPerformed
        update();
    }//GEN-LAST:event_checkEmbeddedAttrsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonApply;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonPathDefault;
    private javax.swing.JButton buttonRSyncAuthorize;
    private javax.swing.JButton buttonRSyncRemove;
    private javax.swing.JButton buttonResetSeq;
    private javax.swing.JButton buttonTokens;
    private javax.swing.JButton buttonUndo;
    private javax.swing.JCheckBox checkDataLogs;
    private javax.swing.JCheckBox checkEmbeddedAttrs;
    private javax.swing.JCheckBox checkRSyncAuthorized;
    private javax.swing.JCheckBox ckAutoSave;
    private javax.swing.JCheckBox ckConvert;
    private javax.swing.JCheckBox ckFlush;
    private javax.swing.JCheckBox ckKeep;
    private javax.swing.JCheckBox ckSaveConsole;
    private javax.swing.JCheckBox ckSaveScript;
    private javax.swing.JCheckBox ckSaveSetpoints;
    private javax.swing.JComboBox<String> comboLayout;
    private javax.swing.JComboBox<String> comboNotification;
    private javax.swing.JComboBox<String> comboProvider;
    private javax.swing.JComboBox<String> comboTransferMode;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JSpinner spinnerDepthDim;
    private javax.swing.JSpinner spinnerSeq;
    private javax.swing.JTextField textDayIndex;
    private javax.swing.JTextField textPathConfig;
    private javax.swing.JTextField textPathExpansion;
    private javax.swing.JTextField textRecipients;
    private javax.swing.JTextField textTasks;
    private javax.swing.JTextField textTransferPath;
    private javax.swing.JTextField textTransferUser;
    // End of variables declaration//GEN-END:variables
}
