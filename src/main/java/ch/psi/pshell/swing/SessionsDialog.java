package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.SessionManager;
import static ch.psi.pshell.core.SessionManager.STATE_COMPLETED;
import ch.psi.pshell.core.SessionManager.SessionManagerListener;
import ch.psi.utils.IO;
import ch.psi.utils.SciCat;
import ch.psi.utils.Str;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.lang3.tuple.ImmutablePair;


public class SessionsDialog extends StandardDialog implements SessionManagerListener{

    final SessionManager manager;    
    final DefaultTableModel modelSessions;
    final DefaultTableModel modelMetadata;
    final DefaultTableModel modelRuns;
    final DefaultTableModel modelFiles;
    
    final SciCat sciCat;
    
    volatile boolean updating;
    

    public SessionsDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        manager = Context.getInstance().getSessionManager();
        modelSessions = (DefaultTableModel) tableSessions.getModel();
        modelMetadata = (DefaultTableModel) tableMetadata.getModel();
        modelRuns = (DefaultTableModel) tableRuns.getModel();
        modelFiles = (DefaultTableModel) tableFiles.getModel();
        
        tableRuns.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(0).setMaxWidth(60);
        tableRuns.getColumnModel().getColumn(1).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(1).setMaxWidth(60);
        tableRuns.getColumnModel().getColumn(2).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(2).setMaxWidth(60);
        tableRuns.getColumnModel().getColumn(3).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(3).setMaxWidth(60);
        tableRuns.getColumnModel().getColumn(0).setResizable(false);                
        tableRuns.getColumnModel().getColumn(1).setResizable(false);                
        tableRuns.getColumnModel().getColumn(2).setResizable(false);                
        tableRuns.getColumnModel().getColumn(3).setResizable(false);                
        
        tableRuns.getColumnModel().getColumn(5).setPreferredWidth(60);
        tableRuns.getColumnModel().getColumn(5).setResizable(false);
        
        
        update();        
        tableSessions.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent lse) {
                if (!lse.getValueIsAdjusting()) {         
                    int row = tableSessions.getSelectedRow();
                    if (row >=0){
                        selectSession(Integer.valueOf(tableSessions.getValueAt(row, 0).toString()));
                    }
                }
            }
        });
        
        modelRuns.addTableModelListener((TableModelEvent e) -> {
            if ((!updating) & (currentSession>=0)){
                if (e.getType() == TableModelEvent.UPDATE){
                    int col=e.getColumn();
                    if (e.getColumn()==0){
                        int runIndex = e.getFirstRow();                              
                        try {
                            Boolean value = (Boolean) modelRuns.getValueAt(runIndex, 0);
                            manager.setRunEnabled(currentSession, runIndex, value);
                        } catch (IOException ex) {
                            Logger.getLogger(SessionPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    }
                }
            }
        });     
        
        sciCat= new SciCat();
        textScicatLocation.setText(sciCat.getConfig().creationLocation);
        textScicatGroup.setText(sciCat.getConfig().ownerGroup);        
        textScicatParameters.setText(sciCat.getConfig().parameters);   
        textScicatPI.setText(sciCat.getConfig().principalInvestigator);   
        
        int sessions = tableSessions.getRowCount();
        if (sessions>0){
            tableSessions.setRowSelectionInterval(sessions-1, sessions-1);
            SwingUtils.scrollToVisible(tableSessions, sessions-1, 0);
        }
    }
    
    @Override
    protected void onOpened() {
        manager.addListener(this);
    }

    @Override
    protected void onClosed() {
        manager.removeListener(this);
    }   
    
    @Override
    public void onChange(SessionManager.ChangeType type) {
        SwingUtilities.invokeLater(() -> {
            int session = currentSession;
            int current = manager.getCurrentId();
            if (type==SessionManager.ChangeType.STATE){
                update();
            }
            if ((session >=0) && (session==current)) {
                selectSession(session);                
            }
        });
    }    
        
    void updateButtons(){
        boolean archiveEnabled = true;
        int sessionRow = tableSessions.getSelectedRow();
        if (panelSessions.isVisible()){            
            String sessionState = (sessionRow>=0) ? Str.toString(modelSessions.getValueAt(sessionRow, 5)) : "";
            archiveEnabled = sessionState.equals(STATE_COMPLETED);
        } 
        buttonAddFile.setEnabled(currentSession>=0);
        buttonRemoveFile.setEnabled(buttonAddFile.isEnabled() && tableFiles.getSelectedRow()>=0);
        
        buttonZIP.setEnabled(sessionRow>=0); //Allow zipping open sessions
        buttonScicatIngestion.setEnabled(archiveEnabled);
    }
    
    void update(){
        updating = true;
        try{
            List<Integer> ids = manager.getIDs();
            modelSessions.setNumRows(ids.size());
            for (int i=0; i<ids.size(); i++){
                try{
                    int id = ids.get(i);
                    Map<String, Object> info = manager.getInfo(id);
                    String name = (String)info.getOrDefault("name", "");
                    String start = SessionPanel.getTimeStr((Number)info.getOrDefault("start", 0));
                    String stop = SessionPanel.getTimeStr((Number)info.getOrDefault("stop", 0));
                    String root = (String)info.getOrDefault("root", "");
                    String state = (String)info.getOrDefault("state", "unknown");
                    modelSessions.setValueAt(String.valueOf(id), i, 0);
                    modelSessions.setValueAt(name, i, 1);
                    modelSessions.setValueAt(start, i, 2);
                    modelSessions.setValueAt(stop, i, 3);
                    modelSessions.setValueAt(root, i, 4);
                    modelSessions.setValueAt(state, i, 5);
                    //modelSessions.addRow(new Object[]{String.valueOf(id), name, start, stop, root, state});
                } catch (Exception ex){
                    Logger.getLogger(SessionsDialog.class.getName()).log(Level.WARNING, null, ex);
                }
            }
            updateButtons();
        } finally {
            updating = false;
        }
    }
    

    int currentSession = -1;
    void selectSession(int session){
        updating = true;
        try{        
           currentSession = session; 
            try {            
                List<ImmutablePair<String,Object>>  metadata= manager.getDisplayableMetadata(session);
                modelMetadata.setNumRows(metadata.size());
                int index = 0;
                for (ImmutablePair<String,Object> entry : metadata) {
                    modelMetadata.setValueAt(entry.getLeft(), index, 0);
                    modelMetadata.setValueAt(entry.getRight(), index++, 1);
                }
            } catch (Exception ex) {
                modelMetadata.setNumRows(0);
            }

            try{
                List<Map<String, Object>> runs = manager.getRuns(session, true);            
                modelRuns.setNumRows(runs.size());
                String dataHome = Context.getInstance().getSetup().getDataPath();
                int index=0;
                for(int i=0; i<runs.size(); i++ ){
                    Map<String, Object> run = runs.get(i);                
                    modelRuns.setValueAt(run.getOrDefault("enabled", true), index, 0);
                    modelRuns.setValueAt(SessionPanel.getDateStr((Number)run.getOrDefault("start", 0)), index, 1);
                    modelRuns.setValueAt(SessionPanel.getTimeStr((Number)run.getOrDefault("start", 0)), index, 2);
                    modelRuns.setValueAt(SessionPanel.getTimeStr((Number)run.getOrDefault("stop", 0)), index, 3);
                    modelRuns.setValueAt(Str.toString(run.getOrDefault("data", "")), index, 4);
                    modelRuns.setValueAt(run.getOrDefault("state", ""), index++, 5);
                }
            } catch (Exception ex){
                modelRuns.setNumRows(0);
            }        

            try{
                List<String> files = manager.getAdditionalFiles(session, true);            
                modelFiles.setNumRows(files.size());
                for (int i=0; i<files.size(); i++){
                    modelFiles.setValueAt(files.get(i), i,0);
                }       
            } catch (Exception ex){
                modelFiles.setNumRows(0);
            }  
            updateButtons();
        } finally {
            updating = false;
        }
    }
    
    void setAdditionalFiles() throws IOException{
        if (currentSession>=0){
            List<String> files = new ArrayList<>();         
            for (int i=0; i<modelFiles.getRowCount(); i++){
                files.add(modelFiles.getValueAt(i,0).toString());
            }       
            manager.setAdditionalFiles(currentSession, files);          
        }
        updateButtons();
    }   
    
    public void setSingleSession(int session){
        selectSession(session);
        panelSessions.setVisible(false);
        updateButtons();
    }
    
    JDialog showMessageDialog(String message){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel label = new JLabel(message);        
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);        
        JDialog dialogMessage = SwingUtils.showDialog(this, "Archive",new Dimension(400,200), panel);
        panel.paintImmediately(0,0,panel.getWidth(),panel.getHeight());
        return dialogMessage;
    }
   
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelSessions = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSessions = new javax.swing.JTable();
        panelRuns = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableRuns = new javax.swing.JTable();
        jSplitPane1 = new javax.swing.JSplitPane();
        panelMetadata = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableMetadata = new javax.swing.JTable();
        panelFiles = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tableFiles = new javax.swing.JTable();
        buttonAddFile = new javax.swing.JButton();
        buttonRemoveFile = new javax.swing.JButton();
        panelArchive = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        buttonZIP = new javax.swing.JButton();
        checkPreserveDirectoryStructure = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        buttonScicatIngestion = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        textScicatLocation = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textScicatGroup = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        textScicatParameters = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        textScicatPI = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        panelSessions.setBorder(javax.swing.BorderFactory.createTitledBorder("Sessions"));

        tableSessions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Id", "Name", "Start", "Finish", "Root", "State"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableSessions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableSessions.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tableSessions);

        javax.swing.GroupLayout panelSessionsLayout = new javax.swing.GroupLayout(panelSessions);
        panelSessions.setLayout(panelSessionsLayout);
        panelSessionsLayout.setHorizontalGroup(
            panelSessionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSessionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        panelSessionsLayout.setVerticalGroup(
            panelSessionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSessionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
                .addContainerGap())
        );

        panelRuns.setBorder(javax.swing.BorderFactory.createTitledBorder("Runs"));

        tableRuns.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Date", "Start", "Stop", "Data", "State"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableRuns.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(tableRuns);

        javax.swing.GroupLayout panelRunsLayout = new javax.swing.GroupLayout(panelRuns);
        panelRuns.setLayout(panelRunsLayout);
        panelRunsLayout.setHorizontalGroup(
            panelRunsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRunsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addGap(7, 7, 7))
        );
        panelRunsLayout.setVerticalGroup(
            panelRunsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelRunsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerLocation(300);
        jSplitPane1.setDividerSize(5);
        jSplitPane1.setResizeWeight(0.5);

        panelMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("Metadata"));

        tableMetadata.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class
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
        tableMetadata.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        tableMetadata.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tableMetadata);

        javax.swing.GroupLayout panelMetadataLayout = new javax.swing.GroupLayout(panelMetadata);
        panelMetadata.setLayout(panelMetadataLayout);
        panelMetadataLayout.setHorizontalGroup(
            panelMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMetadataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelMetadataLayout.setVerticalGroup(
            panelMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMetadataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(panelMetadata);

        panelFiles.setBorder(javax.swing.BorderFactory.createTitledBorder("Additional Files"));

        tableFiles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableFiles.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        tableFiles.getTableHeader().setReorderingAllowed(false);
        tableFiles.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableFilesMouseReleased(evt);
            }
        });
        tableFiles.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableFilesKeyReleased(evt);
            }
        });
        jScrollPane4.setViewportView(tableFiles);

        buttonAddFile.setText("Add");
        buttonAddFile.setEnabled(false);
        buttonAddFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddFileActionPerformed(evt);
            }
        });

        buttonRemoveFile.setText("Remove");
        buttonRemoveFile.setEnabled(false);
        buttonRemoveFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveFileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelFilesLayout = new javax.swing.GroupLayout(panelFiles);
        panelFiles.setLayout(panelFilesLayout);
        panelFilesLayout.setHorizontalGroup(
            panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFilesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFilesLayout.createSequentialGroup()
                        .addGap(0, 40, Short.MAX_VALUE)
                        .addComponent(buttonAddFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonRemoveFile)
                        .addGap(0, 40, Short.MAX_VALUE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        panelFilesLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonAddFile, buttonRemoveFile});

        panelFilesLayout.setVerticalGroup(
            panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFilesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonRemoveFile)
                    .addComponent(buttonAddFile))
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(panelFiles);

        panelArchive.setBorder(javax.swing.BorderFactory.createTitledBorder("Archive"));

        buttonZIP.setText("Genarate ZIP File");
        buttonZIP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonZIPActionPerformed(evt);
            }
        });

        checkPreserveDirectoryStructure.setText("Preserve root directory structure");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addComponent(checkPreserveDirectoryStructure)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 91, Short.MAX_VALUE)
                .addComponent(buttonZIP)
                .addGap(50, 50, 50))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonZIP)
                    .addComponent(checkPreserveDirectoryStructure))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("ZIP", jPanel1);

        buttonScicatIngestion.setText("Ingest");
        buttonScicatIngestion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScicatIngestionActionPerformed(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Creation Location:");

        textScicatLocation.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textScicatLocationKeyReleased(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Owner Group:");

        textScicatGroup.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textScicatGroupKeyReleased(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Parameters:");

        textScicatParameters.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textScicatParametersKeyReleased(evt);
            }
        });

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Principal Investigator:");

        textScicatPI.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textScicatPIKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textScicatParameters, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                    .addComponent(textScicatGroup)
                    .addComponent(textScicatLocation)
                    .addComponent(textScicatPI))
                .addGap(18, 18, 18)
                .addComponent(buttonScicatIngestion)
                .addGap(20, 20, 20))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel5});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonScicatIngestion)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(textScicatLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, 0)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(textScicatGroup, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, 0)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(textScicatPI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, 0)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(textScicatParameters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {textScicatGroup, textScicatLocation, textScicatParameters});

        jTabbedPane1.addTab("SciCat", jPanel2);

        javax.swing.GroupLayout panelArchiveLayout = new javax.swing.GroupLayout(panelArchive);
        panelArchive.setLayout(panelArchiveLayout);
        panelArchiveLayout.setHorizontalGroup(
            panelArchiveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        panelArchiveLayout.setVerticalGroup(
            panelArchiveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelSessions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jSplitPane1)
            .addComponent(panelRuns, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelArchive, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelSessions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jSplitPane1)
                .addGap(0, 0, 0)
                .addComponent(panelRuns, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(panelArchive, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonAddFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddFileActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(Context.getInstance().getSetup().getDataPath());
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setAcceptAllFileFilterUsed(true);
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                ((DefaultTableModel)tableFiles.getModel()).addRow(new Object[]{chooser.getSelectedFile().getCanonicalPath()});
                setAdditionalFiles();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }          
    }//GEN-LAST:event_buttonAddFileActionPerformed

    private void buttonRemoveFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveFileActionPerformed
        try {
            int row = tableFiles.getSelectedRow();
            if(row>=0){
                modelFiles.removeRow(row);
                setAdditionalFiles();
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }            
    }//GEN-LAST:event_buttonRemoveFileActionPerformed

    private void buttonZIPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZIPActionPerformed
        try {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("ZIP files", "zip");
            String name = manager.getName(currentSession);
            String filename = (name.isBlank()) ? String.valueOf(currentSession) : currentSession+"_"+name;
            chooser.setSelectedFile(new File(filename + ".zip"));
            chooser.setFileFilter(filter);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setAcceptAllFileFilterUsed(true);            
            int rVal = chooser.showSaveDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                File file =  chooser.getSelectedFile();
                if (IO.getExtension(file).isBlank()){
                    file = new File(file.getCanonicalPath()+".zip");
                }     
                if (file.exists()){
                    if (SwingUtils.showOption(this, "Overwrite", "File " + file.getName() + " already exists.\nDo you want to overwrite it?", OptionType.YesNo) != OptionResult.Yes) {
                        return;
                    }
                }
                JDialog dialogMessage = showMessageDialog("Generating ZIP file...");
                try{
                    manager.createZipFile(currentSession, file, checkPreserveDirectoryStructure.isSelected());                    
                } finally{
                    dialogMessage.setVisible(false);
                }
                SwingUtils.showMessage(this, "Archive", "Success creating ZIP file: " + file.getName());
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }     
    }//GEN-LAST:event_buttonZIPActionPerformed

    private void buttonScicatIngestionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScicatIngestionActionPerformed
        try {    
            SciCat.IngestOutput result = null;
            JDialog dialogMessage = showMessageDialog("Ingesting SciCat dataset...");
            try{                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("creationLocation", textScicatLocation.getText());
                metadata.put("principalInvestigator", textScicatPI.getText());
                metadata.put("ownerGroup", textScicatGroup.getText());                
                result  = sciCat.ingest(currentSession, metadata, textScicatParameters.getText());
            } finally{
                dialogMessage.setVisible(false);
            }
            String msg = result.success ? "Success ingesting SciCat dataset " + result.datasetId : "Success ingesting SciCat dataset";
            SwingUtils.showScrollableMessage(this, "SciCat Ingestion", msg, result.output);
            Logger.getLogger(SessionPanel.class.getName()).info(msg + "\n" + result);
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
            Logger.getLogger(SessionPanel.class.getName()).log(Level.WARNING, null, ex);
        }          
    }//GEN-LAST:event_buttonScicatIngestionActionPerformed

    private void tableFilesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableFilesKeyReleased
        updateButtons();
    }//GEN-LAST:event_tableFilesKeyReleased

    private void tableFilesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableFilesMouseReleased
        updateButtons();
    }//GEN-LAST:event_tableFilesMouseReleased

    private void textScicatLocationKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textScicatLocationKeyReleased
        try {
            sciCat.setCreationLocation(textScicatLocation.getText());
        } catch (IOException ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_textScicatLocationKeyReleased

    private void textScicatGroupKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textScicatGroupKeyReleased
        try {
            sciCat.setOwnerGroup(textScicatGroup.getText());
        } catch (IOException ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_textScicatGroupKeyReleased

    private void textScicatParametersKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textScicatParametersKeyReleased
                        
        try {
            sciCat.setParameters(textScicatParameters.getText());
        } catch (IOException ex) {
            SwingUtils.showException(this, ex);
        }                    
    }//GEN-LAST:event_textScicatParametersKeyReleased

    private void textScicatPIKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textScicatPIKeyReleased
        try {
            sciCat.setPrincipalInvestigator(textScicatPI.getText());
        } catch (IOException ex) {
            SwingUtils.showException(this, ex);
        } 
    }//GEN-LAST:event_textScicatPIKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddFile;
    private javax.swing.JButton buttonRemoveFile;
    private javax.swing.JButton buttonScicatIngestion;
    private javax.swing.JButton buttonZIP;
    private javax.swing.JCheckBox checkPreserveDirectoryStructure;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel panelArchive;
    private javax.swing.JPanel panelFiles;
    private javax.swing.JPanel panelMetadata;
    private javax.swing.JPanel panelRuns;
    private javax.swing.JPanel panelSessions;
    private javax.swing.JTable tableFiles;
    private javax.swing.JTable tableMetadata;
    private javax.swing.JTable tableRuns;
    private javax.swing.JTable tableSessions;
    private javax.swing.JTextField textScicatGroup;
    private javax.swing.JTextField textScicatLocation;
    private javax.swing.JTextField textScicatPI;
    private javax.swing.JTextField textScicatParameters;
    // End of variables declaration//GEN-END:variables
}
