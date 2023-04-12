package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.SessionManager;
import ch.psi.pshell.core.SessionManager.MetadataType;
import ch.psi.pshell.core.SessionManager.SessionManagerListener;
import ch.psi.pshell.ui.App;
import ch.psi.pshell.ui.Task;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.IO;
import ch.psi.utils.SciCat;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
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
        manager = Context.getInstance().getSessionManagerIfHandling();
        modelSessions = (DefaultTableModel) tableSessions.getModel();
        modelMetadata = (DefaultTableModel) tableMetadata.getModel();
        modelRuns = (DefaultTableModel) tableRuns.getModel();
        modelFiles = (DefaultTableModel) tableFiles.getModel();
        
        
        int minColSize =  (UIManager.getLookAndFeel().getName().equalsIgnoreCase("nimbus"))? 68:60;
        tableRuns.getColumnModel().getColumn(0).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(0).setMaxWidth(minColSize);
        tableRuns.getColumnModel().getColumn(1).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(1).setMaxWidth(minColSize);
        tableRuns.getColumnModel().getColumn(2).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(2).setMaxWidth(minColSize);
        tableRuns.getColumnModel().getColumn(3).setPreferredWidth(minColSize);
        tableRuns.getColumnModel().getColumn(3).setMaxWidth(minColSize);
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
                        try{
                            selectSession(Integer.valueOf(tableSessions.getValueAt(row, 0).toString()));
                        } catch (Exception ex){
                            selectSession(-1);
                        }
                    }
                }
            }
        });
        
        tableRuns.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent lse) {
                if (!lse.getValueIsAdjusting() && !updating) {         
                    updateButtons();
                }
            }
        });        
        
        tableSessions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                        if (currentSession>0){
                            Map<String, Object> info = manager.getInfo(currentSession);                           
                            info.put("metadata", manager.getMetadata(currentSession));  
                            showScrollableMessage("Session Info",  
                                    "Session id: " + currentSession, EncoderJson.encode(info, true));
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(SessionPanel.class.getName()).log(Level.WARNING, null, ex);
                }
            }        
        });
        modelRuns.addTableModelListener((TableModelEvent e) -> {
            if ((!updating) & (currentSession>=0)){
                cancelMetadataEditing();
                if (e.getType() == TableModelEvent.UPDATE){                    
                    int col=e.getColumn();
                    if (e.getColumn()==0){
                        int runIndex = e.getFirstRow();                                                      
                        try {                            
                            Boolean value = (Boolean) modelRuns.getValueAt(runIndex, 0);
                            String data = (String) modelRuns.getValueAt(runIndex, 4);
                            
                            boolean editable = SessionManager.isSessionEditable(getSelectedSessionState());                                                   
                            if (editable){
                                //manager.setRunEnabled(currentSession, runIndex, value);
                                manager.setRunEnabled(currentSession, data, value);
                            } else {
                                updating = true;
                                try{
                                    modelRuns.setValueAt(manager.isRunEnabled(currentSession, data), runIndex, 0);
                                } finally{
                                    updating = false;
                                }
                            }
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
        textScicatPI.setText(sciCat.getConfig().principalInvestigator);   
        //SwingUtils.setEnumCombo(comboScicatEnv, SciCat.Environment.class);
        //comboScicatEnv.setSelectedItem(sciCat.getConfig().getEnvironment());   
        
        int sessions = tableSessions.getRowCount();
        if (sessions>0){
            tableSessions.setRowSelectionInterval(sessions-1, sessions-1);
            SwingUtils.scrollToVisible(tableSessions, sessions-1, 0);
        }
        
        modelMetadata.addTableModelListener((TableModelEvent e) -> {
            if (!updating) {
                if (currentSession>=0){
                    if (e.getType() == TableModelEvent.UPDATE) {
                        int row = e.getFirstRow();
                        String key = Str.toString(modelMetadata.getValueAt(row, 0));
                        Object value = modelMetadata.getValueAt(row, 1);                           
                        String current = null;
                        try{
                            current = Str.toString(manager.getMetadata(currentSession, true).get(key));
                        } catch (Exception ex){
                            
                        }                            
                        if (!Str.toString(value).equals(current)){
                            String state = getSelectedSessionState();
                            if (!SessionManager.isSessionEditable(state)){
                                showMessage( "Error", "Cannot change session metadata if state is " + state);
                                SwingUtilities.invokeLater(()->{ 
                                    selectSession(currentSession); 
                                });                            
                                return;
                            }
                            if (e.getColumn() == 1) {
                                addMetadata(currentSession, key, value);
                            }
                        }
                    }
                }
            }
        });    
        checkCurrentUser.setFont(UIManager.getFont("Table.font"));
        checkCompleted.setFont(checkCurrentUser.getFont());
    }
    
    void addMetadata(int session, String key, Object value) {
        try {
            MetadataType type = manager.getMetadataType(key);
            if (value instanceof String){
                String str = (String)value;
                if (type==MetadataType.List){
                    if (!str.startsWith("[")){
                        value = "[" + value;
                    }
                    if (!str.endsWith("]")){
                        value = value + "]";
                    }
                }
                else if (type==MetadataType.Map){
                    if (!str.startsWith("{")){
                        value = "{" + value;
                    }
                    if (!str.endsWith("}")){
                        value = value + "}";
                    }
                }
            }            
            manager.fromString(type, Str.toString(value));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                showException(ex);
                selectSession(currentSession); 
            });
            //Don't update values if cannot parse value according to type
            return;
            
        }
        try {
            manager.setMetadata(session, key, value);
        } catch (IOException ex) {
            Logger.getLogger(SessionPanel.class.getName()).log(Level.WARNING, null, ex);
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
    public void onChange(int id, SessionManager.ChangeType type) {
        SwingUtilities.invokeLater(() -> {
            int session = currentSession;
            switch(type){
                case STATE:
                    update();
                    if (session >=0) {
                        selectSession(session);                
                    }                    
                    break;
                case METADATA:
                    if (id==session){
                        updateMetadata();
                    }
                    break;
                case INFO:
                    if (id==session){
                        updateRuns();
                        updateFiles();
                    }                    
                    break;
            }
        });
    }    
    
    String getSelectedSessionState(){
        int sessionRow = tableSessions.getSelectedRow();
        return (sessionRow>=0) ? Str.toString(modelSessions.getValueAt(sessionRow, 5)) : "";
    }
        
    void updateButtons(){
        boolean archiveEnabled = true;
        boolean editEnabled = false;
        int sessionRow = tableSessions.getSelectedRow();
        if (panelSessions.isVisible()){            
            String sessionState = getSelectedSessionState();
            archiveEnabled = SessionManager.isSessionArchivable(sessionState);
            editEnabled = SessionManager.isSessionEditable(sessionState) && (currentUser.equals(Sys.getUserName()));
        } 
        buttonAddFile.setEnabled(editEnabled);
        buttonRemoveFile.setEnabled(editEnabled && tableFiles.getSelectedRow()>=0);
        
        buttonAddMetadata.setEnabled(editEnabled);
        int row =  tableMetadata.getSelectedRow();
        buttonRemoveMetadata.setEnabled(editEnabled && (row>=0) && (!manager.isMetadataDefinedKey(Str.toString(modelMetadata.getValueAt(row,0)))));

        buttonZIP.setEnabled(sessionRow>=0); //Allow zipping open sessions
        buttonScicatIngestion.setEnabled(archiveEnabled);
                
        buttonDetach.setEnabled(editEnabled && (tableRuns.getSelectedRows().length>0));
        buttonMove.setEnabled(editEnabled && (tableRuns.getSelectedRows().length>0));
    }
    
    void update(){
        updating = true;
        try{
            
            List<Integer> ids = manager.getIDs(
                    checkCompleted.isSelected() ? SessionManager.STATE_COMPLETED : null, 
                    checkCurrentUser.isSelected() ? Sys.getUserName() : null);            
            modelSessions.setNumRows(ids.size());
            for (int i=0; i<ids.size(); i++){
                try{
                    int id = ids.get(i);
                    Map<String, Object> info = manager.getInfo(id);
                    String name = (String)info.getOrDefault("name", "");
                    String start = SessionPanel.getDateTimeStr((Number)info.getOrDefault("start", 0));
                    String stop = SessionPanel.getDateTimeStr((Number)info.getOrDefault("stop", 0));
                    String root = (String)info.getOrDefault("root", "");
                    String user = (String)info.getOrDefault("user", "");
                    String state = (String)info.getOrDefault("state", "unknown");
                    modelSessions.setValueAt(String.valueOf(id), i, 0);
                    modelSessions.setValueAt(name, i, 1);
                    modelSessions.setValueAt(start, i, 2);
                    modelSessions.setValueAt(stop, i, 3);
                    modelSessions.setValueAt(user, i, 4);
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

    void cancelMetadataEditing(){
        try{
            TableCellEditor editor = tableMetadata.getCellEditor();
            if (editor!=null){
                editor.cancelCellEditing();
            }
        } catch (Exception ex) {
        }        
    }
    
    void updateMetadata(){
        updating = true;
        try {   
            List<ImmutablePair<String,Object>>  metadata= manager.getDisplayableMetadata(currentSession);
            modelMetadata.setNumRows(metadata.size());
            int index = 0;
            for (ImmutablePair<String,Object> entry : metadata) {
                modelMetadata.setValueAt(entry.getLeft(), index, 0);
                modelMetadata.setValueAt(entry.getRight(), index++, 1);
            }
        } catch (Exception ex) {
            modelMetadata.setNumRows(0);
        } finally {
            updating = false;
        }       }
    
    void updateRuns(){
        updating = true;
        try{
            String filter = testRunFilter.getText();
            List<Map<String, Object>> runs = manager.getRuns(currentSession, true, filter);            
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
        } finally {
            updating = false;
        }   
    }
    
    void updateFiles(){
        updating = true;
        try{
            List<String> files = manager.getAdditionalFiles(currentSession, true);            
            modelFiles.setNumRows(files.size());
            for (int i=0; i<files.size(); i++){
                modelFiles.setValueAt(files.get(i), i,0);
            }       
        } catch (Exception ex){
            modelFiles.setNumRows(0);
        } finally {
            updating = false;
        }   
    }
    String currentUser = "";
    int currentSession = -1;
    void selectSession(int session){
        try{        
            currentSession = session; 
            if (session<0){
                tableSessions.clearSelection();
            }
            currentUser = (session<0) ? "" : Str.toString(tableSessions.getValueAt(tableSessions.getSelectedRow(), 4));
            cancelMetadataEditing();
            testRunFilter.setText("");
            updateMetadata();
            updateRuns();
            updateFiles();
            tableRuns.clearSelection();
            tableMetadata.clearSelection();
            tableFiles.clearSelection();
            updateButtons();
        } finally {
            this.setTitle((currentSession>0)? "Session Archiver: " + currentSession : "Session Archiver");         
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
   
    
    
    
    /**
     * Task to ingest session data into SciCat.
     */
    public class ScicatIngest extends Task {

        @Override
        protected String doInBackground() throws Exception {
            String msg = "Ingesting session data to Scicat";
            setMessage(msg);
            setProgress(0);
            buttonScicatIngestion.setEnabled(false);
            try {
                App.getInstance().sendTaskInit(msg);
                
                SciCat.IngestOutput result = null;
                JDialog splash = SwingUtils.showSplash(App.getInstance().getMainFrame(), "Archive", new Dimension(400,200), "Ingesting SciCat dataset...");
                try{
                    result  = sciCat.ingest(currentSession, null);
                } finally{
                    splash.setVisible(false);
                }
                msg = result.success ?
                    "Success ingesting SciCat dataset " + result.datasetName + "\nId: " + result.datasetId :
                    "Error ingesting SciCat dataset " + result.datasetName;
                
                SwingUtils.showScrollableMessage(App.getInstance().getMainFrame(),"SciCat Ingestion", msg, result.output);
                Logger.getLogger(SessionPanel.class.getName()).info(msg + "\n" + result.output);
                                
                msg = "Success ingesting to Scicat";
                App.getInstance().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                setMessage("Error ingesting to Scicat");
                App.getInstance().sendError(ex.toString());
                throw ex;
            } finally {
                buttonScicatIngestion.setEnabled(true);
                App.getInstance().sendTaskFinish(msg);                
            }
        }
    }
    
    /**
     * Task to archive session data into ZIP file.
     */
    public class ZipSession extends Task {
        final File file;
        ZipSession(File file){
            this.file=file;
        }

        @Override
        protected String doInBackground() throws Exception {
            SessionManager manager = Context.getInstance().getSessionManager();
            String msg = "Archiving session data to zip file";
            setMessage(msg);
            setProgress(0);
            buttonZIP.setEnabled(false);
            try {
                App.getInstance().sendTaskInit(msg);
                
                JDialog splash = SwingUtils.showSplash(App.getInstance().getMainFrame(),"Archive", new Dimension(400,200), "Generating ZIP file...");
                try{
                    manager.createZipFile(currentSession, file, checkPreserveDirectoryStructure.isSelected());                    
                } finally{
                    splash.setVisible(false);
                }                                                
                msg = "Success creating ZIP file: " + file.getName();               
                SwingUtils.showMessage(App.getInstance().getMainFrame(),"Archive", msg);
                App.getInstance().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {
                setMessage("Error ingesting to Scicat");
                App.getInstance().sendError(ex.toString());
                throw ex;
            } finally {
                buttonZIP.setEnabled(true);
                App.getInstance().sendTaskFinish(msg);
            }
        }
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
        checkCurrentUser = new javax.swing.JCheckBox();
        checkCompleted = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        panelRuns = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableRuns = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        testRunFilter = new javax.swing.JTextField();
        buttonDetach = new javax.swing.JButton();
        buttonMove = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        panelMetadata = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableMetadata = new javax.swing.JTable();
        buttonAddMetadata = new javax.swing.JButton();
        buttonRemoveMetadata = new javax.swing.JButton();
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
        jLabel1 = new javax.swing.JLabel();
        textScicatLocation = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textScicatGroup = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        textScicatPI = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        buttonScicatIngestion = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        panelSessions.setBorder(javax.swing.BorderFactory.createTitledBorder("Sessions"));

        tableSessions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Id", "Name", "Start", "Stop", "User", "State"
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

        checkCurrentUser.setText("Current User");
        checkCurrentUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkCurrentUserActionPerformed(evt);
            }
        });

        checkCompleted.setText("Completed");
        checkCompleted.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkCompletedActionPerformed(evt);
            }
        });

        jLabel3.setText("Filter:");

        javax.swing.GroupLayout panelSessionsLayout = new javax.swing.GroupLayout(panelSessions);
        panelSessions.setLayout(panelSessionsLayout);
        panelSessionsLayout.setHorizontalGroup(
            panelSessionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSessionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSessionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSessionsLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(checkCurrentUser)
                        .addGap(18, 18, 18)
                        .addComponent(checkCompleted)))
                .addContainerGap())
        );
        panelSessionsLayout.setVerticalGroup(
            panelSessionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSessionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addGroup(panelSessionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkCurrentUser)
                    .addComponent(checkCompleted)
                    .addComponent(jLabel3)))
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

        jLabel4.setText("Filter:");

        testRunFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testRunFilterActionPerformed(evt);
            }
        });

        buttonDetach.setText("Detach");
        buttonDetach.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDetachActionPerformed(evt);
            }
        });

        buttonMove.setText("Move");
        buttonMove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMoveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelRunsLayout = new javax.swing.GroupLayout(panelRuns);
        panelRuns.setLayout(panelRunsLayout);
        panelRunsLayout.setHorizontalGroup(
            panelRunsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRunsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelRunsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelRunsLayout.createSequentialGroup()
                        .addComponent(jScrollPane3)
                        .addGap(7, 7, 7))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelRunsLayout.createSequentialGroup()
                        .addComponent(buttonDetach)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonMove)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(testRunFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        panelRunsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDetach, buttonMove});

        panelRunsLayout.setVerticalGroup(
            panelRunsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRunsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addGroup(panelRunsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(testRunFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonDetach)
                    .addComponent(buttonMove))
                .addGap(0, 0, 0))
        );

        jSplitPane1.setDividerLocation(300);
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
                false, true
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
        tableMetadata.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableMetadataMouseReleased(evt);
            }
        });
        tableMetadata.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableMetadataKeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(tableMetadata);

        buttonAddMetadata.setText("Add");
        buttonAddMetadata.setEnabled(false);
        buttonAddMetadata.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddMetadataActionPerformed(evt);
            }
        });

        buttonRemoveMetadata.setText("Remove");
        buttonRemoveMetadata.setEnabled(false);
        buttonRemoveMetadata.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveMetadataActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelMetadataLayout = new javax.swing.GroupLayout(panelMetadata);
        panelMetadata.setLayout(panelMetadataLayout);
        panelMetadataLayout.setHorizontalGroup(
            panelMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMetadataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                    .addGroup(panelMetadataLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonAddMetadata)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonRemoveMetadata)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelMetadataLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonAddMetadata, buttonRemoveMetadata});

        panelMetadataLayout.setVerticalGroup(
            panelMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMetadataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addGroup(panelMetadataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonRemoveMetadata)
                    .addComponent(buttonAddMetadata))
                .addGap(0, 0, 0))
        );

        jSplitPane1.setLeftComponent(panelMetadata);

        panelFiles.setBorder(javax.swing.BorderFactory.createTitledBorder("Files"));

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
        tableFiles.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
                        .addGap(0, 57, Short.MAX_VALUE)
                        .addComponent(buttonAddFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonRemoveFile)
                        .addGap(0, 57, Short.MAX_VALUE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        panelFilesLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonAddFile, buttonRemoveFile});

        panelFilesLayout.setVerticalGroup(
            panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFilesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addGroup(panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonRemoveFile)
                    .addComponent(buttonAddFile))
                .addGap(0, 0, 0))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 150, Short.MAX_VALUE)
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

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Principal Investigator:");

        textScicatPI.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textScicatPIKeyReleased(evt);
            }
        });

        buttonScicatIngestion.setText("Ingest");
        buttonScicatIngestion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScicatIngestionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonScicatIngestion)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonScicatIngestion)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textScicatLocation, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                    .addComponent(textScicatGroup)
                    .addComponent(textScicatPI))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel5});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {textScicatGroup, textScicatLocation});

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
            cancelMetadataEditing();
            JFileChooser chooser = new JFileChooser(Context.getInstance().getSetup().getDataPath());
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setMultiSelectionEnabled(true);
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                for (File file : chooser.getSelectedFiles()){
                    ((DefaultTableModel)tableFiles.getModel()).addRow(new Object[]{file.getCanonicalPath()});
                }
                setAdditionalFiles();
            }
        } catch (Exception ex) {
            showException(ex);
        }          
    }//GEN-LAST:event_buttonAddFileActionPerformed

    private void buttonRemoveFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveFileActionPerformed
        try {
            cancelMetadataEditing();
            int[] rows = tableFiles.getSelectedRows();            
            Arrays.sort(rows);
            for (int i = rows.length-1; i >= 0; i--) {
                int row = rows[i];
                if(row>=0){
                    modelFiles.removeRow(row);                    
                }                
            }
            setAdditionalFiles();
        } catch (Exception ex) {
            showException(ex);
        }            
    }//GEN-LAST:event_buttonRemoveFileActionPerformed

    private void buttonZIPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZIPActionPerformed
        try {
            App.getInstance().getState().assertReady();
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
                    if (showOption("Overwrite", "File " + file.getName() + " already exists.\nDo you want to overwrite it?", OptionType.YesNo) != OptionResult.Yes) {
                        return;
                    }
                }
                App.getInstance().startTask(new ZipSession(file));
            }
        } catch (Exception ex) {
            showException(ex);
        }     
    }//GEN-LAST:event_buttonZIPActionPerformed

    private void tableFilesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableFilesKeyReleased
        updateButtons();
    }//GEN-LAST:event_tableFilesKeyReleased

    private void tableFilesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableFilesMouseReleased
        updateButtons();
    }//GEN-LAST:event_tableFilesMouseReleased

    private void buttonScicatIngestionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScicatIngestionActionPerformed
        try {
            App.getInstance().getState().assertReady();
            App.getInstance().startTask(new ScicatIngest());
        } catch (Exception ex) {
            showException(ex);
            Logger.getLogger(SessionPanel.class.getName()).log(Level.WARNING, null, ex);
        }
    }//GEN-LAST:event_buttonScicatIngestionActionPerformed

    private void textScicatPIKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textScicatPIKeyReleased
        try {
            sciCat.setPrincipalInvestigator(textScicatPI.getText());
        } catch (IOException ex) {
            showException(ex);
        }
    }//GEN-LAST:event_textScicatPIKeyReleased

    private void textScicatGroupKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textScicatGroupKeyReleased
        try {
            sciCat.setOwnerGroup(textScicatGroup.getText());
        } catch (IOException ex) {
            showException(ex);
        }
    }//GEN-LAST:event_textScicatGroupKeyReleased

    private void textScicatLocationKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textScicatLocationKeyReleased
        try {
            sciCat.setCreationLocation(textScicatLocation.getText());
        } catch (IOException ex) {
            showException(ex);
        }
    }//GEN-LAST:event_textScicatLocationKeyReleased

    private void checkCurrentUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkCurrentUserActionPerformed
        try {
            update();
            selectSession(-1);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_checkCurrentUserActionPerformed

    private void checkCompletedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkCompletedActionPerformed
        try {
            update();
            selectSession(-1);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_checkCompletedActionPerformed

    private void testRunFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testRunFilterActionPerformed
        try {
            updateRuns();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_testRunFilterActionPerformed

    private void buttonDetachActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDetachActionPerformed
        try {
            List<String> data = new ArrayList<>();
            for (int i : tableRuns.getSelectedRows()){
                data.add(Str.toString(modelRuns.getValueAt(i, 4)));
            }
            String currentName = manager.getName(currentSession);
            String name =getString("Enter the new session name for detaching the selected data files", currentName);
            if (name != null) {
                int id = manager.detach(name, currentSession, data);
                showScrollableMessage("Success", "Success detaching to session "+ id + "-" + name + " the files:", String.join("\n", data));
            }            
        } catch (Exception ex) {
            showException(ex);
        } 
    }//GEN-LAST:event_buttonDetachActionPerformed

    private void buttonMoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMoveActionPerformed
        try {
            List<String> data = new ArrayList<>();
            for (int i : tableRuns.getSelectedRows()){
                data.add(Str.toString(modelRuns.getValueAt(i, 4)));
            }
            
            SessionReopenDialog dlg = new SessionReopenDialog(getFrame(),true, "Select Destination Session");
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);
            if (dlg.getResult()) {
                int id = dlg.getSelectedSession();
                String name = manager.getName(id);
                manager.move(currentSession, data, id);
                showScrollableMessage("Success", "Success moving to session " + id + "-" + name + " the files:", String.join("\n", data));
            }
        } catch (Exception ex) {
            showException(ex);
        } 
    }//GEN-LAST:event_buttonMoveActionPerformed

    private void buttonAddMetadataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddMetadataActionPerformed
        try {
            cancelMetadataEditing();
            String name = getString("Enter the name of the new metadata field:", "");
            if (name != null) {
                if (manager.getMetadata(currentSession,name)!=null){
                    throw new Exception("The field is already defined: " + name);
                } else {
                    manager.setMetadata(currentSession, name, manager.getMetadataDefault(name));
                    updateMetadata();
                }
            }            
        } catch (Exception ex) {
            showException(ex);
        } 
    }//GEN-LAST:event_buttonAddMetadataActionPerformed

    private void buttonRemoveMetadataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveMetadataActionPerformed
        try {
            cancelMetadataEditing();
            int row = tableMetadata.getSelectedRow();
            if(row>=0){
                manager.setMetadata(currentSession, Str.toString(modelMetadata.getValueAt(row, 0)), null);
                updateMetadata();
            }
        } catch (Exception ex) {
            showException(ex);
        } 
    }//GEN-LAST:event_buttonRemoveMetadataActionPerformed

    private void tableMetadataKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableMetadataKeyReleased
        updateButtons();
    }//GEN-LAST:event_tableMetadataKeyReleased

    private void tableMetadataMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMetadataMouseReleased
        updateButtons();
    }//GEN-LAST:event_tableMetadataMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddFile;
    private javax.swing.JButton buttonAddMetadata;
    private javax.swing.JButton buttonDetach;
    private javax.swing.JButton buttonMove;
    private javax.swing.JButton buttonRemoveFile;
    private javax.swing.JButton buttonRemoveMetadata;
    private javax.swing.JButton buttonScicatIngestion;
    private javax.swing.JButton buttonZIP;
    private javax.swing.JCheckBox checkCompleted;
    private javax.swing.JCheckBox checkCurrentUser;
    private javax.swing.JCheckBox checkPreserveDirectoryStructure;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
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
    private javax.swing.JTextField testRunFilter;
    private javax.swing.JTextField textScicatGroup;
    private javax.swing.JTextField textScicatLocation;
    private javax.swing.JTextField textScicatPI;
    // End of variables declaration//GEN-END:variables
}
