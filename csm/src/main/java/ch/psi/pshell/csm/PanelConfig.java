package ch.psi.pshell.csm;

import ch.psi.pshell.camserver.ProxyClient;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Str;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class PanelConfig extends MonitoredPanel {

    ProxyClient proxy;
    Map<String, Object> serverCfg = null;
    List<String> instanceCfgNames = new ArrayList<>();
    List<String> visibleNames = new ArrayList<>();
    List<String> scriptsNames = new ArrayList<>();
    Map<String, String> permanentInstances = new HashMap<>();
    String currentConfig = "" ;
    String currentServer = "" ;
    
    final DefaultTableModel modelConfigs;
    final DefaultTableModel modelServers;
    final DefaultTableModel modelFixed;
    final DefaultTableModel modelPermanent;
    
    boolean modelPermanentChanged;
    boolean modelFixedChanged;
    boolean modelServersChanged;
    boolean modelFixedCamsChanged;
    
    final int SMALL_COL_SZIE = 80;
    
    
    public PanelConfig() {
        initComponents();
        splitTop.setDividerLocation(0.5);
        
        buttonConfigEdit.setEnabled(false);
        
        modelConfigs = (DefaultTableModel)  tableConfigurations.getModel();
        modelServers = (DefaultTableModel)  tableServers.getModel();
        modelFixed = (DefaultTableModel)  tableFixedInstances.getModel();
        modelPermanent = (DefaultTableModel)  tablePermanentInstances.getModel();
        
        tableServers.getColumnModel().getColumn(0).setPreferredWidth(SMALL_COL_SZIE);
        tableServers.getColumnModel().getColumn(0).setMaxWidth(SMALL_COL_SZIE);
        tableServers.getColumnModel().getColumn(0).setResizable(false);
        tableServers.getColumnModel().getColumn(2).setPreferredWidth(SMALL_COL_SZIE);
        tableServers.getColumnModel().getColumn(2).setMaxWidth(SMALL_COL_SZIE);
        tableServers.getColumnModel().getColumn(2).setResizable(false);
        tableFixedInstances.getColumnModel().getColumn(0).setPreferredWidth(SMALL_COL_SZIE);
        tableFixedInstances.getColumnModel().getColumn(0).setMaxWidth(SMALL_COL_SZIE);
        tableFixedInstances.getColumnModel().getColumn(0).setResizable(false);
        tableFixedInstances.getColumnModel().getColumn(2).setPreferredWidth(SMALL_COL_SZIE);
        tableFixedInstances.getColumnModel().getColumn(2).setMaxWidth(SMALL_COL_SZIE);
        tableFixedInstances.getColumnModel().getColumn(2).setResizable(false);                
        tablePermanentInstances.getColumnModel().getColumn(0).setPreferredWidth(SMALL_COL_SZIE);
        tablePermanentInstances.getColumnModel().getColumn(0).setMaxWidth(SMALL_COL_SZIE);
        tablePermanentInstances.getColumnModel().getColumn(0).setResizable(false);
        
        modelPermanent.addTableModelListener((TableModelEvent e) -> {
            modelPermanentChanged=true;
            updateButtons();
        });
        
        modelFixed.addTableModelListener((TableModelEvent e) -> {
            modelFixedChanged=true;
            updateButtons();
        });
        
        modelServers.addTableModelListener((TableModelEvent e) -> {
            modelServersChanged=true;
            updateButtons();
        });  
        
        textFixedCameras.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent e) {
            changed();
          }
          public void removeUpdate(DocumentEvent e) {
            changed();
          }
          public void insertUpdate(DocumentEvent e) {
            changed();
          }
          public void changed() {
            if (!modelFixedCamsChanged){            
                modelFixedCamsChanged = true;
                updateButtons();
            }
          }
        });        
                

        tableConfigurations.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                    buttonConfigEditActionPerformed(null);
                }
            }
        });
                        
        tableConfigurations.setDragEnabled(true);
        tableConfigurations.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return DnDConstants.ACTION_COPY_OR_MOVE;
            }
            @Override
            public Transferable createTransferable(JComponent comp) {
                onTableInstancesSelection();
                return (currentConfig != null) ? new StringSelection(currentConfig) : null;
            }
        });
        
        
        tablePermanentInstances.setDropMode(DropMode.INSERT_ROWS);
        tablePermanentInstances.setFillsViewportHeight(true);
        tablePermanentInstances.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return DnDConstants.ACTION_COPY_OR_MOVE;
            }

            @Override
            public boolean canImport(TransferHandler.TransferSupport info) {
                return info.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                if ((!support.isDrop()) ||  (!canImport(support))) {
                    return false;
                }
                try {
                    JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();                    
                    String name = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);         
                    modelPermanent.insertRow(dl.getRow(), new Object[]{true, name,name});
                    updateButtons();
                } catch (Exception ex) {
                    return false;
                }                
                return true;
            }

        });

        tableFixedInstances.setDropMode(DropMode.INSERT_ROWS);
        tableFixedInstances.setFillsViewportHeight(true);
        tableFixedInstances.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return DnDConstants.ACTION_COPY_OR_MOVE;
            }

            @Override
            public boolean canImport(TransferHandler.TransferSupport info) {
                if ((currentServer==null) || (currentServer.isBlank())){
                    return false;
                }
                return info.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                if ((!support.isDrop()) ||  (!canImport(support))) {
                    return false;
                }
                try {
                    JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();                    
                    String name = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);         
                    modelFixed.insertRow(dl.getRow(), new Object[]{true,name,""});
                    updateButtons();
                } catch (Exception ex) {
                    return false;
                }                
                return true;
            }

        });
        
        updateButtons();
    }
    
    public void disableFixedCameras(){
        if (tabFixed.getTabCount()>1){
            tabFixed.removeTabAt(1);
        }
    }
    
    void updateButtons(){
        if (!SwingUtilities.isEventDispatchThread()){
            SwingUtilities.invokeLater(()->{updateButtons();});
            return;
        }
        
        boolean expert = App.isExpert();
        boolean serverSelected = (tableServers.getSelectedRow()>=0);
        buttonConfigDel.setEnabled(tableConfigurations.getSelectedRow()>=0);
        buttonConfigEdit.setEnabled(tableConfigurations.getSelectedRow()>=0);
        buttonFixedDel.setEnabled((tableFixedInstances.getSelectedRow()>=0) && serverSelected);
        buttonFixedApply.setEnabled(modelFixedChanged && serverSelected);
        buttonFixedUndo.setEnabled(modelFixedChanged && serverSelected);
        buttonPermDelete.setEnabled((tablePermanentInstances.getSelectedRow()>=0));
        buttonPermApply.setEnabled(modelPermanentChanged);
        buttonPermUndo.setEnabled(modelPermanentChanged);
        buttonServersDel.setEnabled(expert && serverSelected);
        buttonServersApply.setEnabled(modelServersChanged);
        buttonServersUndo.setEnabled(modelServersChanged);
        buttonFixedCamUndo.setEnabled(modelFixedCamsChanged && serverSelected);
        buttonFixedCamApply.setEnabled(modelFixedCamsChanged && serverSelected);    
        textFixedCameras.setEnabled((currentServer!=null) && (!currentServer.isBlank()));        
    }
    
    
    Thread updateConfigs(){
        Thread t = new Thread(()->{
            try {
                instanceCfgNames =proxy.getConfigNames();
                Collections.sort(instanceCfgNames); //, String.CASE_INSENSITIVE_ORDER);
                visibleNames = List.copyOf(instanceCfgNames);
                if ((filterName!=null) && (!filterName.isBlank())){
                    visibleNames = visibleNames
                        .stream()
                        .filter(c -> c.toLowerCase().contains(filterName))
                        .collect(Collectors.toList());                            
                }
                modelConfigs.setRowCount(0);
                for (String instance : visibleNames){
                    modelConfigs.addRow(new Object[]{instance});
                }                
                updateButtons();
            } catch (Exception ex) {
                Logger.getLogger(PanelConfig.class.getName()).log(Level.WARNING, null, ex);
            }             
        }, "PC Update Config");
        t.start();
        return t;
    }
    
    Thread updateServers(){
        if (tableServers.isEditing()){
            tableServers.getCellEditor().stopCellEditing();
        }
        
        Thread t = new Thread(()->{
            try {
                serverCfg =proxy.getConfig();
                modelServers.setRowCount(0);
                for (String server : serverCfg.keySet()){
                    Map serverCfg = (Map) this.serverCfg.get(server);
                    boolean expanding = (Boolean) serverCfg.getOrDefault("expanding", true);
                    boolean enabled = (Boolean) serverCfg.getOrDefault("enabled", true);
                    modelServers.addRow(new Object[]{enabled,server,expanding});
                }      
                modelServersChanged = false;      
                onTableServersSelection();
            } catch (Exception ex) {
                Logger.getLogger(PanelConfig.class.getName()).log(Level.WARNING, null, ex);
            }             
        }, "PC Update Servers");
        t.start();
        return t;  
    }
    
    Thread updatePermanent(){                
        modelPermanentChanged=false; //So that disable update button immediately
        updateButtons();
        if (tablePermanentInstances.isEditing()){
            tablePermanentInstances.getCellEditor().stopCellEditing();
        }
        Thread t = new Thread(()->{
            try {
                permanentInstances = proxy.getPemanentInstances();   
                modelPermanent.setRowCount(0);
                ArrayList<String> keys = new ArrayList<>(permanentInstances.keySet());
                Collections.sort(keys, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        o1 = o1.startsWith("#") ? o1.substring(1) : o1;
                        o2 = o2.startsWith("#") ? o2.substring(1) : o2;
                        return o1.compareTo(o2);
                    }
                });                
                for (String name : keys){

                    String config = Str.toString(permanentInstances.get(name));
                    boolean enabled = true;
                    if (name.startsWith("#")){
                        name = name.substring(1);
                        enabled = false;
                    }
                    modelPermanent.addRow(new Object[]{enabled, config, name});
                }    
                modelPermanentChanged=false;
                updateButtons();
            } catch (Exception ex) {
                Logger.getLogger(PanelConfig.class.getName()).log(Level.WARNING, null, ex);
            }             
        }, "PC Update Permanent");
        t.start();
        return t;  
    }    
    
    void updateFixed(){        
        if (tableFixedInstances.isEditing()){
            tableFixedInstances.getCellEditor().stopCellEditing();
        }
        try {            
            modelFixed.setRowCount(0);
            if((currentServer!=null) && (!currentServer.isBlank())){
                Map cfg = (Map) this.serverCfg.get(currentServer);
                List<String> fixed = (List<String>) cfg.getOrDefault("instances", new ArrayList());
                for (String instance : fixed){
                    boolean enabled = true;
                    String port = "";
                    if (instance.startsWith("#")){
                        instance = instance.substring(1);
                        enabled = false;
                    }
                    if (instance.contains(":")){
                        port = instance.substring(instance.lastIndexOf(":") + 1);
                        instance = instance.substring(0, instance.lastIndexOf(":"));                        
                    }
                    modelFixed.addRow(new Object[]{enabled, instance, port});
                }    
                modelFixedChanged=false;    
            }
            updateButtons();
        } catch (Exception ex) {
            Logger.getLogger(PanelConfig.class.getName()).log(Level.WARNING, null, ex);
        }                      
    }    

    void updateCamFixed(){        
        try {       
            if (tabFixed.getTabCount()>1){
                if((currentServer!=null) && (!currentServer.isBlank())){
                    textFixedCameras.setEnabled(true);
                    Map cfg = (Map) this.serverCfg.get(currentServer);
                    List<String> cameras = (List<String>) cfg.get("cameras");
                    if (cameras!=null){
                        textFixedCameras.setText(String.join("\n", cameras));
                    } else {
                        textFixedCameras.setText("");
                    }
                } else {
                    textFixedCameras.setText("");
                    textFixedCameras.setEnabled(false);
                }
                modelFixedCamsChanged = false;
                updateButtons();
            }
        } catch (Exception ex) {
            Logger.getLogger(PanelConfig.class.getName()).log(Level.WARNING, null, ex);
            textFixedCameras.setText("");
        }                      
    }    
        
    void selectServer(String name){
        for (int i=0; i<modelServers.getRowCount();i++){
            if (Str.toString(modelServers.getValueAt(i, 1)).equals(name)){
                tableServers.setRowSelectionInterval(i, i);
                SwingUtils.scrollToVisible(tableServers, i, 0);
                onTableServersSelection();
                return;
            }
        }        
        tableServers.clearSelection();
        onTableServersSelection();        
    }
    
           
    @Override
    protected void onShow() {   
        updateButtons();
        updateServers();
        updateConfigs();
        updatePermanent();
    }
    
        
    public void setProxy(ProxyClient proxy){
        this.proxy = proxy;        
    }
    
    public ProxyClient getProxy(){
       return proxy;
    }   
            
    public String getUrl(){
       if (proxy==null){
           return null;
       }
       return proxy.getUrl();
    }    
          
    
    void onTableInstancesSelection(){
        int row=tableConfigurations.getSelectedRow();
        if (row<0){
            currentConfig = "";
        } else {
            currentConfig = String.valueOf(tableConfigurations.getValueAt(row, 0));
        }
        updateButtons();
    }
    
    void onTableServersSelection(){
        int row=tableServers.getSelectedRow();
        if (row<0){
            currentServer = "";            
        } else {
            currentServer = String.valueOf(tableServers.getValueAt(row, 1));           
        }
        updateFixed();
        updateCamFixed();
        updateButtons();
    }
    
    void setFilter(String str){        
        if (str==null){
            str="";
        }
        if (!str.equals(filterName)){
            filterName = str;
            updateConfigs();
        }
    }
    
    String filterName;
    void onFilter(){
        setFilter(textFilter.getText().trim().toLowerCase());
    }    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        splitTop = new javax.swing.JSplitPane();
        splitLeft = new javax.swing.JSplitPane();
        panelServers = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tableServers = new javax.swing.JTable();
        buttonServersDel = new javax.swing.JButton();
        buttonServersUndo = new javax.swing.JButton();
        buttonServersApply = new javax.swing.JButton();
        tabFixed = new javax.swing.JTabbedPane();
        panelFixedInstances = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        tableFixedInstances = new javax.swing.JTable();
        buttonFixedDel = new javax.swing.JButton();
        buttonFixedUndo = new javax.swing.JButton();
        buttonFixedApply = new javax.swing.JButton();
        panelFixedCameras = new javax.swing.JPanel();
        buttonFixedCamUndo = new javax.swing.JButton();
        buttonFixedCamApply = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        textFixedCameras = new javax.swing.JTextArea();
        panelInstances = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablePermanentInstances = new javax.swing.JTable();
        buttonPermUndo = new javax.swing.JButton();
        buttonPermApply = new javax.swing.JButton();
        buttonPermDelete = new javax.swing.JButton();
        panelConfigurations = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableConfigurations = new javax.swing.JTable();
        buttonConfigEdit = new javax.swing.JButton();
        buttonConfigNew = new javax.swing.JButton();
        buttonConfigDel = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        textFilter = new javax.swing.JTextField();

        splitTop.setResizeWeight(1.0);

        splitLeft.setDividerLocation(350);
        splitLeft.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitLeft.setResizeWeight(0.5);

        panelServers.setBorder(javax.swing.BorderFactory.createTitledBorder("Servers"));
        panelServers.setPreferredSize(new java.awt.Dimension(320, 320));

        tableServers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Address", "Expanding"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableServers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableServers.getTableHeader().setReorderingAllowed(false);
        tableServers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableServersMouseReleased(evt);
            }
        });
        tableServers.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableServersKeyReleased(evt);
            }
        });
        jScrollPane4.setViewportView(tableServers);

        buttonServersDel.setText("Delete");
        buttonServersDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonServersDelActionPerformed(evt);
            }
        });

        buttonServersUndo.setText("Undo");
        buttonServersUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonServersUndoActionPerformed(evt);
            }
        });

        buttonServersApply.setText("Apply");
        buttonServersApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonServersApplyActionPerformed(evt);
            }
        });

        tableFixedInstances.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Instance", "Port"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableFixedInstances.setDropMode(javax.swing.DropMode.INSERT_ROWS);
        tableFixedInstances.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableFixedInstances.getTableHeader().setReorderingAllowed(false);
        tableFixedInstances.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableFixedInstancesMouseReleased(evt);
            }
        });
        tableFixedInstances.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableFixedInstancesKeyReleased(evt);
            }
        });
        jScrollPane6.setViewportView(tableFixedInstances);

        buttonFixedDel.setText("Delete");
        buttonFixedDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFixedDelActionPerformed(evt);
            }
        });

        buttonFixedUndo.setText("Undo");
        buttonFixedUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFixedUndoActionPerformed(evt);
            }
        });

        buttonFixedApply.setText("Apply");
        buttonFixedApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFixedApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelFixedInstancesLayout = new javax.swing.GroupLayout(panelFixedInstances);
        panelFixedInstances.setLayout(panelFixedInstancesLayout);
        panelFixedInstancesLayout.setHorizontalGroup(
            panelFixedInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFixedInstancesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelFixedInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFixedInstancesLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonFixedDel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonFixedUndo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonFixedApply)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        panelFixedInstancesLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonFixedApply, buttonFixedDel, buttonFixedUndo});

        panelFixedInstancesLayout.setVerticalGroup(
            panelFixedInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFixedInstancesLayout.createSequentialGroup()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFixedInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonFixedUndo)
                    .addComponent(buttonFixedApply)
                    .addComponent(buttonFixedDel)))
        );

        tabFixed.addTab("Fixed Instances", panelFixedInstances);

        buttonFixedCamUndo.setText("Undo");
        buttonFixedCamUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFixedCamUndoActionPerformed(evt);
            }
        });

        buttonFixedCamApply.setText("Apply");
        buttonFixedCamApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFixedCamApplyActionPerformed(evt);
            }
        });

        textFixedCameras.setColumns(20);
        textFixedCameras.setRows(5);
        jScrollPane3.setViewportView(textFixedCameras);

        javax.swing.GroupLayout panelFixedCamerasLayout = new javax.swing.GroupLayout(panelFixedCameras);
        panelFixedCameras.setLayout(panelFixedCamerasLayout);
        panelFixedCamerasLayout.setHorizontalGroup(
            panelFixedCamerasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFixedCamerasLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelFixedCamerasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFixedCamerasLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonFixedCamUndo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonFixedCamApply)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelFixedCamerasLayout.setVerticalGroup(
            panelFixedCamerasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFixedCamerasLayout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFixedCamerasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonFixedCamUndo)
                    .addComponent(buttonFixedCamApply)))
        );

        tabFixed.addTab("Fixed Cameras", panelFixedCameras);

        javax.swing.GroupLayout panelServersLayout = new javax.swing.GroupLayout(panelServers);
        panelServers.setLayout(panelServersLayout);
        panelServersLayout.setHorizontalGroup(
            panelServersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelServersLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonServersDel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonServersUndo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonServersApply)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(tabFixed)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelServersLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        panelServersLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonServersApply, buttonServersDel, buttonServersUndo});

        panelServersLayout.setVerticalGroup(
            panelServersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelServersLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelServersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonServersUndo)
                    .addComponent(buttonServersApply)
                    .addComponent(buttonServersDel))
                .addGap(0, 0, 0)
                .addComponent(tabFixed))
        );

        splitLeft.setLeftComponent(panelServers);

        panelInstances.setBorder(javax.swing.BorderFactory.createTitledBorder("Permanent Instances"));
        panelInstances.setPreferredSize(new java.awt.Dimension(288, 250));

        tablePermanentInstances.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Instance", "Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablePermanentInstances.setDropMode(javax.swing.DropMode.INSERT_ROWS);
        tablePermanentInstances.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablePermanentInstances.getTableHeader().setReorderingAllowed(false);
        tablePermanentInstances.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tablePermanentInstancesMouseReleased(evt);
            }
        });
        tablePermanentInstances.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tablePermanentInstancesKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(tablePermanentInstances);

        buttonPermUndo.setText("Undo");
        buttonPermUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPermUndoActionPerformed(evt);
            }
        });

        buttonPermApply.setText("Apply");
        buttonPermApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPermApplyActionPerformed(evt);
            }
        });

        buttonPermDelete.setText("Delete");
        buttonPermDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPermDeleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelInstancesLayout = new javax.swing.GroupLayout(panelInstances);
        panelInstances.setLayout(panelInstancesLayout);
        panelInstancesLayout.setHorizontalGroup(
            panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInstancesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelInstancesLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonPermDelete)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonPermUndo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonPermApply)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelInstancesLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonPermApply, buttonPermDelete, buttonPermUndo});

        panelInstancesLayout.setVerticalGroup(
            panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInstancesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelInstancesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonPermApply)
                    .addComponent(buttonPermDelete)
                    .addComponent(buttonPermUndo))
                .addContainerGap())
        );

        splitLeft.setRightComponent(panelInstances);

        splitTop.setLeftComponent(splitLeft);

        panelConfigurations.setBorder(javax.swing.BorderFactory.createTitledBorder("Saved Configurations"));
        panelConfigurations.setPreferredSize(new java.awt.Dimension(320, 320));

        tableConfigurations.setModel(new javax.swing.table.DefaultTableModel(
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
        tableConfigurations.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableConfigurations.getTableHeader().setReorderingAllowed(false);
        tableConfigurations.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableConfigurationsMouseReleased(evt);
            }
        });
        tableConfigurations.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableConfigurationsKeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(tableConfigurations);

        buttonConfigEdit.setText("Edit");
        buttonConfigEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonConfigEditActionPerformed(evt);
            }
        });

        buttonConfigNew.setText("New");
        buttonConfigNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonConfigNewActionPerformed(evt);
            }
        });

        buttonConfigDel.setText("Delete");
        buttonConfigDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonConfigDelActionPerformed(evt);
            }
        });

        jLabel1.setText("Filter Name:");

        textFilter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textFilterKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout panelConfigurationsLayout = new javax.swing.GroupLayout(panelConfigurations);
        panelConfigurations.setLayout(panelConfigurationsLayout);
        panelConfigurationsLayout.setHorizontalGroup(
            panelConfigurationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelConfigurationsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelConfigurationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(panelConfigurationsLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFilter))
                    .addGroup(panelConfigurationsLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonConfigNew)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonConfigDel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonConfigEdit)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelConfigurationsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonConfigDel, buttonConfigEdit, buttonConfigNew});

        panelConfigurationsLayout.setVerticalGroup(
            panelConfigurationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelConfigurationsLayout.createSequentialGroup()
                .addGroup(panelConfigurationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 489, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelConfigurationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonConfigEdit)
                    .addComponent(buttonConfigNew)
                    .addComponent(buttonConfigDel))
                .addContainerGap())
        );

        splitTop.setRightComponent(panelConfigurations);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitTop, javax.swing.GroupLayout.DEFAULT_SIZE, 622, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitTop, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tableConfigurationsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableConfigurationsKeyReleased
        try{
            onTableInstancesSelection();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_tableConfigurationsKeyReleased

    private void tableConfigurationsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableConfigurationsMouseReleased
        try{
            onTableInstancesSelection();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_tableConfigurationsMouseReleased

    private void buttonConfigEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonConfigEditActionPerformed
        try{
            if (currentConfig!=null){
                String config = proxy.getNamedConfig(currentConfig);
                ScriptEditor dlg = new ScriptEditor(SwingUtils.getFrame(this), true, currentConfig, config, "json");
                dlg.setVisible(true);
                if (dlg.getResult()){
                    proxy.setNamedConfig(currentConfig, dlg.ret);
                }    
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }        
    }//GEN-LAST:event_buttonConfigEditActionPerformed

    private void buttonConfigNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonConfigNewActionPerformed
        try{
            String name = SwingUtils.getString(this, "Enter configuration name: ", "");
            if (name !=null){
                if (instanceCfgNames.contains(name)){
                    throw new Exception("Configuration name is already used: " + name);
                }
                proxy.setNamedConfig(name, "{}");
                
                textFilter.setText("");
                filterName = null;
                updateConfigs().join();
                
                if (!instanceCfgNames.contains(name)){
                    throw new Exception("Error adding configuration: " + name);
                }
                int index = visibleNames.indexOf(name);
                if (index>=0){
                    tableConfigurations.setRowSelectionInterval(index, index);
                    SwingUtils.scrollToVisible(tableConfigurations, index, 0);
                    currentConfig = name;
                    buttonConfigEditActionPerformed(null);                
                } 
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex); 
        }   
    }//GEN-LAST:event_buttonConfigNewActionPerformed

    private void buttonConfigDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonConfigDelActionPerformed
        try{
            if (currentConfig!=null){
                Object[] options = new Object[]{"No", "Yes"};
                if (SwingUtils.showOption(this, "Delete Configuration", "Are you sure to delete the configuration: " + currentConfig  + "?", options, options[0]) == 1){
                    proxy.deleteNamedConfig(currentConfig);
                    updateConfigs();
                }
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        } 
    }//GEN-LAST:event_buttonConfigDelActionPerformed

    private void buttonPermDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPermDeleteActionPerformed
        try{
            modelPermanent.removeRow(tablePermanentInstances.getSelectedRow());
            updateButtons();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonPermDeleteActionPerformed

    private void buttonFixedDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFixedDelActionPerformed
        try{
            modelFixed.removeRow(tableFixedInstances.getSelectedRow());
            updateButtons();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonFixedDelActionPerformed

    private void tableServersKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableServersKeyReleased
        try{
            onTableServersSelection();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_tableServersKeyReleased

    private void tableServersMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableServersMouseReleased
        try{
            onTableServersSelection();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_tableServersMouseReleased

    private void tableFixedInstancesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableFixedInstancesKeyReleased
        updateButtons();
    }//GEN-LAST:event_tableFixedInstancesKeyReleased

    private void tableFixedInstancesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableFixedInstancesMouseReleased
        updateButtons();
    }//GEN-LAST:event_tableFixedInstancesMouseReleased

    private void tablePermanentInstancesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tablePermanentInstancesKeyReleased
        updateButtons();
    }//GEN-LAST:event_tablePermanentInstancesKeyReleased

    private void tablePermanentInstancesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tablePermanentInstancesMouseReleased
        updateButtons();
    }//GEN-LAST:event_tablePermanentInstancesMouseReleased

    private void buttonPermUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPermUndoActionPerformed
        try{
            updatePermanent();
            tablePermanentInstances.requestFocus();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonPermUndoActionPerformed

    private void buttonPermApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPermApplyActionPerformed
        try{
            if (modelPermanentChanged){
                Map<String,String> map = new HashMap<>();
                for (int i=0; i<modelPermanent.getRowCount();i++){
                    Boolean enabled = (Boolean)modelPermanent.getValueAt(i, 0);
                    String instance = (String)modelPermanent.getValueAt(i, 1);
                    String name = (String)modelPermanent.getValueAt(i, 2);
                    if (!enabled){
                        name="#"+name;
                    }
                    map.put(name, instance);
                }
                proxy.setPemanentInstances(map);
                updatePermanent();
                tablePermanentInstances.requestFocus();
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonPermApplyActionPerformed

    private void buttonFixedUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFixedUndoActionPerformed
        updateFixed();
        tableFixedInstances.requestFocus();
    }//GEN-LAST:event_buttonFixedUndoActionPerformed

    private void buttonServersUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonServersUndoActionPerformed
        updateServers();
        tableServers.requestFocus();
    }//GEN-LAST:event_buttonServersUndoActionPerformed

    private void buttonFixedApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFixedApplyActionPerformed
        try{
            List<String> fixed = new ArrayList<>();
            for (int i=0; i<modelFixed.getRowCount();i++){
                Boolean enabled = (Boolean)modelFixed.getValueAt(i, 0);
                String instance = (String)modelFixed.getValueAt(i, 1);
                String port = ((String)modelFixed.getValueAt(i, 2));
                if (!enabled){
                    instance="#"+instance;
                }
                if (!port.isBlank()){
                    instance = instance + ":" + port;
                }                
                fixed.add(instance);
            }
                        
            Map cfg = (Map) serverCfg.get(currentServer);                       
            cfg.put("instances", fixed);            
            proxy.setConfig(serverCfg);
            updateServers().join();
            selectServer(currentServer);
            tableFixedInstances.requestFocus();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonFixedApplyActionPerformed

    private void buttonServersApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonServersApplyActionPerformed
        try{
            for (int i=0; i<modelServers.getRowCount();i++){
                Boolean enabled = (Boolean)modelServers.getValueAt(i, 0);
                String server = (String)modelServers.getValueAt(i, 1);
                Boolean expanding = ((Boolean)modelServers.getValueAt(i, 2));      
                Map cfg = (Map) serverCfg.get(server);
                cfg.put("enabled", enabled);
                cfg.put("expanding", expanding);
                proxy.setConfig(serverCfg);
                updateServers();
                tableServers.requestFocus();        
            }
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }        
    }//GEN-LAST:event_buttonServersApplyActionPerformed

    private void buttonServersDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonServersDelActionPerformed
        try{
            modelServers.removeRow(tableServers.getSelectedRow());
            updateButtons();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonServersDelActionPerformed

    private void buttonFixedCamUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFixedCamUndoActionPerformed
        try{
            updateCamFixed();
            textFixedCameras.requestFocus();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }        
    }//GEN-LAST:event_buttonFixedCamUndoActionPerformed

    private void buttonFixedCamApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFixedCamApplyActionPerformed
        try{
            List<String> cameras = new ArrayList<>();
            for (String cam : textFixedCameras.getText().split("\n")){
                if(!cam.isBlank()){
                    cameras.add(cam);
                }
            }
                        
            Map cfg = (Map) serverCfg.get(currentServer);                       
            cfg.put("cameras", cameras);            
            proxy.setConfig(serverCfg);
            updateServers().join();
            selectServer(currentServer);
            textFixedCameras.requestFocus();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonFixedCamApplyActionPerformed

    private void textFilterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFilterKeyReleased
        try{
            onFilter();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_textFilterKeyReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonConfigDel;
    private javax.swing.JButton buttonConfigEdit;
    private javax.swing.JButton buttonConfigNew;
    private javax.swing.JButton buttonFixedApply;
    private javax.swing.JButton buttonFixedCamApply;
    private javax.swing.JButton buttonFixedCamUndo;
    private javax.swing.JButton buttonFixedDel;
    private javax.swing.JButton buttonFixedUndo;
    private javax.swing.JButton buttonPermApply;
    private javax.swing.JButton buttonPermDelete;
    private javax.swing.JButton buttonPermUndo;
    private javax.swing.JButton buttonServersApply;
    private javax.swing.JButton buttonServersDel;
    private javax.swing.JButton buttonServersUndo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JPanel panelConfigurations;
    private javax.swing.JPanel panelFixedCameras;
    private javax.swing.JPanel panelFixedInstances;
    private javax.swing.JPanel panelInstances;
    private javax.swing.JPanel panelServers;
    private javax.swing.JSplitPane splitLeft;
    private javax.swing.JSplitPane splitTop;
    private javax.swing.JTabbedPane tabFixed;
    private javax.swing.JTable tableConfigurations;
    private javax.swing.JTable tableFixedInstances;
    private javax.swing.JTable tablePermanentInstances;
    private javax.swing.JTable tableServers;
    private javax.swing.JTextField textFilter;
    private javax.swing.JTextArea textFixedCameras;
    // End of variables declaration//GEN-END:variables
}
