package ch.psi.pshell.csm;


import ch.psi.pshell.camserver.PipelineClient;
import ch.psi.pshell.camserver.ProxyClient;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Str;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class PanelUserScripts extends MonitoredPanel {

    ProxyClient proxy;
    List<String> scriptsNames = new ArrayList<>();
    final DefaultTableModel modelScripts;
    List<String> visibleScriptNames = new ArrayList<>();

    
    List<String> libsNames = new ArrayList<>();
    final DefaultTableModel modelLibs;
    List<String> visibleLibNames = new ArrayList<>();

    
    public void setUrl(String url){
        setProxy(new ProxyClient(url));
    }       

    public PanelUserScripts() {
        initComponents();

        modelScripts = (DefaultTableModel) tableUserScripts.getModel();

        tableUserScripts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (!e.isPopupTrigger())) {
                    buttonScriptEditActionPerformed(null);
                }
            }
        });
        
        modelLibs = (DefaultTableModel) tableLibs.getModel();
        updateButtons();
    }

    void updateButtons() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                updateButtons();
            });
            return;
        }
        buttonScriptEdit.setEnabled(tableUserScripts.getSelectedRow() >= 0);
        buttonScriptDel.setEnabled(tableUserScripts.getSelectedRow() >= 0);

        buttonLibDownload.setEnabled(tableLibs.getSelectedRow() >= 0);
        buttonLibDel.setEnabled(tableLibs.getSelectedRow() >= 0);
    }

    Thread updateScripts() {
        Thread t = new Thread(() -> {
            try {
                PipelineClient client = new PipelineClient(getUrl());
                scriptsNames = client.getScripts();
                Collections.sort(scriptsNames); //, String.CASE_INSENSITIVE_ORDER);
                
            
                visibleScriptNames = List.copyOf(scriptsNames);
                if ((scriptFilterName!=null) && (!scriptFilterName.isBlank())){
                    visibleScriptNames = visibleScriptNames
                        .stream()
                        .filter(c -> c.toLowerCase().contains(scriptFilterName))
                        .collect(Collectors.toList());                            
                }                
                
                modelScripts.setRowCount(0);
                for (String script : visibleScriptNames) {
                    modelScripts.addRow(new Object[]{script});
                }
                updateButtons();
            } catch (Exception ex) {
                Logger.getLogger(PanelUserScripts.class.getName()).log(Level.WARNING, null, ex);
            }
        }, "Task Update Scripts");
        t.start();
        return t;
    }

    Thread updateLibs() {
        Thread t = new Thread(() -> {
            try {
                PipelineClient client = new PipelineClient(getUrl());
                libsNames = client.getLibs();
                Collections.sort(libsNames); //, String.CASE_INSENSITIVE_ORDER);
                
            
                visibleLibNames = List.copyOf(libsNames);
                if ((libFilterName!=null) && (!libFilterName.isBlank())){
                    visibleLibNames = visibleLibNames
                        .stream()
                        .filter(c -> c.toLowerCase().contains(libFilterName))
                        .collect(Collectors.toList());                            
                }                
                
                modelLibs.setRowCount(0);
                for (String lib : visibleLibNames) {
                    modelLibs.addRow(new Object[]{lib});
                }
                updateButtons();
            } catch (Exception ex) {
                Logger.getLogger(PanelUserScripts.class.getName()).log(Level.WARNING, null, ex);
            }
        }, "Task Update Libs");
        t.start();
        return t;
    }
    
    @Override
    protected void onShow() {
        updateButtons();
        updateScripts();
        updateLibs();
    }

    public void setProxy(ProxyClient proxy) {
        this.proxy = proxy;
    }

    public ProxyClient getProxy() {
        return proxy;
    }

    public String getUrl() {
        if (proxy == null) {
            return null;
        }
        return proxy.getUrl();
    }
    
    String scriptFilterName;
    void setScriptFilter(String str){        
        if (str==null){
            str="";
        }
        if (!str.equals(scriptFilterName)){
            scriptFilterName = str;
            updateScripts();
        }
    }
        
    void onScriptFilter(){
        setScriptFilter(textScriptFilter.getText().trim().toLowerCase());
    }        
    
    
    String libFilterName;
    void setLibFilter(String str){        
        if (str==null){
            str="";
        }
        if (!str.equals(libFilterName)){
            libFilterName = str;
            updateLibs();
        }
    }
        
    void onLibFilter(){
        setLibFilter(textLibFilter.getText().trim().toLowerCase());
    }        

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        panelScripts = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        tableUserScripts = new javax.swing.JTable();
        buttonScriptNew = new javax.swing.JButton();
        buttonScriptDel = new javax.swing.JButton();
        buttonScriptEdit = new javax.swing.JButton();
        textScriptFilter = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        buttonScriptUpload = new javax.swing.JButton();
        panelScripts1 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        tableLibs = new javax.swing.JTable();
        buttonLibDel = new javax.swing.JButton();
        textLibFilter = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        buttonLibUpload = new javax.swing.JButton();
        buttonLibDownload = new javax.swing.JButton();

        panelScripts.setPreferredSize(new java.awt.Dimension(288, 250));

        tableUserScripts.setModel(new javax.swing.table.DefaultTableModel(
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
        tableUserScripts.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableUserScripts.getTableHeader().setReorderingAllowed(false);
        tableUserScripts.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableUserScriptsMouseReleased(evt);
            }
        });
        tableUserScripts.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableUserScriptsKeyReleased(evt);
            }
        });
        jScrollPane5.setViewportView(tableUserScripts);

        buttonScriptNew.setText("New");
        buttonScriptNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScriptNewActionPerformed(evt);
            }
        });

        buttonScriptDel.setText("Delete");
        buttonScriptDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScriptDelActionPerformed(evt);
            }
        });

        buttonScriptEdit.setText("Edit");
        buttonScriptEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScriptEditActionPerformed(evt);
            }
        });

        textScriptFilter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textScriptFilterKeyReleased(evt);
            }
        });

        jLabel5.setText("Filter:");

        buttonScriptUpload.setText("Upload");
        buttonScriptUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScriptUploadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelScriptsLayout = new javax.swing.GroupLayout(panelScripts);
        panelScripts.setLayout(panelScriptsLayout);
        panelScriptsLayout.setHorizontalGroup(
            panelScriptsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelScriptsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelScriptsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelScriptsLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonScriptNew)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonScriptUpload)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonScriptDel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonScriptEdit)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(panelScriptsLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textScriptFilter)))
                .addContainerGap())
        );

        panelScriptsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonScriptDel, buttonScriptEdit, buttonScriptNew, buttonScriptUpload});

        panelScriptsLayout.setVerticalGroup(
            panelScriptsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelScriptsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelScriptsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(textScriptFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelScriptsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonScriptEdit)
                    .addComponent(buttonScriptNew)
                    .addComponent(buttonScriptDel)
                    .addComponent(buttonScriptUpload))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Processing Functions", panelScripts);

        panelScripts1.setPreferredSize(new java.awt.Dimension(288, 250));

        tableLibs.setModel(new javax.swing.table.DefaultTableModel(
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
        tableLibs.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableLibs.getTableHeader().setReorderingAllowed(false);
        tableLibs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableLibsMouseReleased(evt);
            }
        });
        tableLibs.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableLibsKeyReleased(evt);
            }
        });
        jScrollPane6.setViewportView(tableLibs);

        buttonLibDel.setText("Delete");
        buttonLibDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLibDelActionPerformed(evt);
            }
        });

        textLibFilter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textLibFilterKeyReleased(evt);
            }
        });

        jLabel6.setText("Filter:");

        buttonLibUpload.setText("Upload");
        buttonLibUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLibUploadActionPerformed(evt);
            }
        });

        buttonLibDownload.setText("Download");
        buttonLibDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLibDownloadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelScripts1Layout = new javax.swing.GroupLayout(panelScripts1);
        panelScripts1.setLayout(panelScripts1Layout);
        panelScripts1Layout.setHorizontalGroup(
            panelScripts1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelScripts1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelScripts1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelScripts1Layout.createSequentialGroup()
                        .addGap(0, 34, Short.MAX_VALUE)
                        .addComponent(buttonLibUpload)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonLibDownload)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonLibDel)
                        .addGap(0, 34, Short.MAX_VALUE))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(panelScripts1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textLibFilter)))
                .addContainerGap())
        );

        panelScripts1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonLibDel, buttonLibDownload, buttonLibUpload});

        panelScripts1Layout.setVerticalGroup(
            panelScripts1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelScripts1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelScripts1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(textLibFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelScripts1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(buttonLibDownload, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonLibDel)
                    .addComponent(buttonLibUpload))
                .addContainerGap())
        );

        panelScripts1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonLibDel, buttonLibDownload, buttonLibUpload});

        jTabbedPane1.addTab("Libraries", panelScripts1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
    }// </editor-fold>//GEN-END:initComponents

    String templateProc   =   "from cam_server.pipeline.data_processing import functions, processor\n"
                            + "\n"
                            + "def process_image(image, pulse_id, timestamp, x_axis, y_axis, parameters, bsdata=None):\n"
                            + "    ret = processor.process_image(image, pulse_id, timestamp, x_axis, y_axis, parameters, bsdata)\n"
                            + "    return ret";

    String templateStream =   "\n"
                            + "def process(data, pulse_id, timestamp, params):\n"
                            + "    return data";

    String templateCustom =   "import time\n"
                            + "import numpy\n"         
                            + "from collections import OrderedDict\n"         
                            + "\n"
                            + "def process(parameters, init=False):\n"         
                            + "    data = OrderedDict()\n"         
                            + "    data['image'] = numpy.random.randint(1, 101, 200, \"uint16\").reshape((10, 20))\n"         
                            + "    timestamp = time.time()\n"         
                            + "    pulse_id = None\n"         
                            + "    rx_size = data['image'].size\n"         
                            + "    return data, timestamp, pulse_id, rx_size\n";       

            
    String templateScripted = "from cam_server.pipeline.utils import *\n"
                            + "from logging import getLogger\n"
                            + "import time\n"
                            + "from cam_server.utils import update_statistics, init_statistics\n"
                            + "\n"
                            + "_logger = getLogger(__name__)\n"
                            + "\n"
                            + "def run(stop_event, statistics, parameter_queue, logs_queue, cam_client, pipeline_config, output_stream_port,\n"
                            + "        background_manager, user_scripts_manager=None):\n"
                            + "    set_log_tag('custom_pipeline')\n"
                            + "    exit_code = 0\n"
                            + "    init_pipeline_parameters(pipeline_config)\n"
                            + "    try:\n"
                            + "        init_statistics(statistics)\n"
                            + "        set_log_tag(' [' + str(pipeline_config.get_name()) + ':' + str(output_stream_port) + ']')\n"
                            + "        sender = create_sender(output_stream_port, stop_event)\n"
                            + "        _logger.debug('Transceiver started. %s' % log_tag)\n"
                            + "        # Indicate that the startup was successful.\n"
                            + "        stop_event.clear()\n"
                            + "        \n"
                            + "        while not stop_event.is_set():\n"
                            + "            try:\n"
                            + "                data = {'status': 'ok'}\n"
                            + "                timestamp = time.time()\n"
                            + "                pulse_id = None\n"
                            + "                send(sender, data, timestamp, pulse_id)\n"
                            + "                update_statistics(sender)\n"
                            + "                time.sleep(0.1)\n"
                            + "            except Exception as e:\n"
                            + "                _logger.exception('Could not process message: ' + str(e) + '. %s' % log_tag)\n"
                            + "                if abort_on_error():\n"
                            + "                    stop_event.set()\n"
                            + "        _logger.info('Stopping transceiver. %s' % log_tag)\n"
                            + "    except:\n"
                            + "        _logger.exception('Exception while trying to start the receive and process thread. %s' % log_tag)\n"
                            + "        exit_code = 1\n"
                            + "        raise\n"
                            + "    finally:\n"
                            + "        _logger.info('Stopping transceiver. %s' % log_tag)\n"
                            + "        if sender:\n"
                            + "            try:\n"
                            + "                sender.close()\n"
                            + "            except:\n"
                            + "                pass\n"
                            + "        sys.exit(exit_code)\n";
    
    
    private void buttonScriptNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScriptNewActionPerformed
        try {
            String name = SwingUtils.getString(this, "Enter script name: ", "");
            if (name != null) {

                if (!name.endsWith(".py") && !name.endsWith(".c")) {
                    name = name + ".py";
                }

                if (scriptsNames.contains(name)) {
                    throw new Exception("Script name is already used: " + name);
                }
                                
                
                Object[] options = new Object[]{"Scripted", "Custom", "Stream", "Processing"};
                int type = SwingUtils.showOption(this, "New Script", "Which is the type pipeline for this script?", options, options[0]);
                String script;
                switch(type){
                    case 3:
                        script = templateProc;
                        break;
                    case 2:
                        script = templateStream;
                        break;
                    case 1:
                        script = templateCustom;
                        break;
                    case 0:
                        script = templateScripted;
                        break;
                    default:
                        return;
                }
                                               
                PipelineClient client = new PipelineClient(getUrl());
                client.setScript(name, script);
                updateScripts().join();
                if (!scriptsNames.contains(name)) {
                    throw new Exception("Error adding script: " + name);
                }
                int index = visibleScriptNames.indexOf(name);
                if (index>=0){
                    tableUserScripts.setRowSelectionInterval(index, index);
                    SwingUtils.scrollToVisible(tableUserScripts, index, 0);
                    buttonScriptEditActionPerformed(null);          
                }                 
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonScriptNewActionPerformed

    private void buttonScriptDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScriptDelActionPerformed
        try {
            int row = tableUserScripts.getSelectedRow();
            if (row >= 0) {
                String name = Str.toString(modelScripts.getValueAt(row, 0));
                Object[] options = new Object[]{"No", "Yes"};
                if (SwingUtils.showOption(this, "Delete Script", "Are you sure to delete the processing script: " + name + "?", options, options[0]) == 1) {
                    PipelineClient client = new PipelineClient(getUrl());
                    client.deleteScript(name);
                    updateScripts();
                }
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonScriptDelActionPerformed

    private void buttonScriptEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScriptEditActionPerformed
        try {
            int row = tableUserScripts.getSelectedRow();
            if (row >= 0) {
                String name = Str.toString(modelScripts.getValueAt(row, 0));
                PipelineClient client = new PipelineClient(getUrl());
                String script = client.getScript(name);
                String type = name.endsWith(".c") ? "c" : "py";
                ScriptEditor dlg = new ScriptEditor(SwingUtils.getFrame(this), true, name, script, type);
                dlg.setVisible(true);
                if (dlg.getResult()) {
                    client.setScript(name, dlg.ret);
                }
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }

    }//GEN-LAST:event_buttonScriptEditActionPerformed

    private void tableUserScriptsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableUserScriptsKeyReleased
        updateButtons();
    }//GEN-LAST:event_tableUserScriptsKeyReleased

    private void tableUserScriptsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableUserScriptsMouseReleased
        updateButtons();
    }//GEN-LAST:event_tableUserScriptsMouseReleased

    private void textScriptFilterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textScriptFilterKeyReleased
        try{
            onScriptFilter();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_textScriptFilterKeyReleased

    private void buttonScriptUploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScriptUploadActionPerformed
        try {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Script files", "py", "c");
            chooser.setFileFilter(filter);
            chooser.setAcceptAllFileFilterUsed(false);
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                String name = chooser.getSelectedFile().getName();
                if (scriptsNames.contains(name)) {
                    Object[] options = new Object[]{"No", "Yes"};
                    if (SwingUtils.showOption(this, "Upload Script", "Overwrite the processing script: " + name + "?", options, options[0]) != 1) {
                        return;
                    }
                }
                PipelineClient client = new PipelineClient(getUrl());
                client.setScriptFile(chooser.getSelectedFile().getAbsolutePath());
                updateScripts();
            }
            
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonScriptUploadActionPerformed

    private void tableLibsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableLibsMouseReleased
         updateButtons();
    }//GEN-LAST:event_tableLibsMouseReleased

    private void tableLibsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableLibsKeyReleased
         updateButtons();
    }//GEN-LAST:event_tableLibsKeyReleased

    private void buttonLibDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLibDelActionPerformed
        try {
            int row = tableLibs.getSelectedRow();
            if (row >= 0) {
                String name = Str.toString(modelLibs.getValueAt(row, 0));
                Object[] options = new Object[]{"No", "Yes"};
                if (SwingUtils.showOption(this, "Delete Library", "Are you sure to delete the library file: " + name + "?", options, options[0]) == 1) {
                    PipelineClient client = new PipelineClient(getUrl());
                    client.deleteLib(name);
                    updateLibs();
                }
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);        
        }    }//GEN-LAST:event_buttonLibDelActionPerformed

    private void textLibFilterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textLibFilterKeyReleased
        try{
            onLibFilter();
        } catch (Exception ex){
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_textLibFilterKeyReleased

    private void buttonLibUploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLibUploadActionPerformed
        try {
            JFileChooser chooser = new JFileChooser();
            int rVal = chooser.showOpenDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                String name = chooser.getSelectedFile().getName();
                if (libsNames.contains(name)) {
                    Object[] options = new Object[]{"No", "Yes"};
                    if (SwingUtils.showOption(this, "Upload Library", "Overwrite the library: " + name + "?", options, options[0]) != 1) {
                        return;
                    }
                }
                PipelineClient client = new PipelineClient(getUrl());
                client.setLibFile(chooser.getSelectedFile().getAbsolutePath());
                updateLibs();
            }
            
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonLibUploadActionPerformed

    private void buttonLibDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLibDownloadActionPerformed
        try {
            int row = tableLibs.getSelectedRow();
            if (row >= 0) {
                String name = Str.toString(modelLibs.getValueAt(row, 0));

                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int rVal = chooser.showSaveDialog(this);
                if (rVal == JFileChooser.APPROVE_OPTION) {
                    PipelineClient client = new PipelineClient(getUrl());
                    byte[] lib  = client.getLib(name);
                    Path path = Paths.get(chooser.getSelectedFile().toString(), name);
                    if (path.toFile().exists()){
                        Object[] options = new Object[]{"No", "Yes"};
                        if (SwingUtils.showOption(this, "Download Library", "Overwrite local file: " + path.toString() + "?", options, options[0]) != 1) {
                            return;
                        }                        
                    }
                    Files.write(path, lib);
                    SwingUtils.showMessage(this, "Download Library", "Success downloading library: " + name);
                }
            }
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonLibDownloadActionPerformed

    /*    */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonLibDel;
    private javax.swing.JButton buttonLibDownload;
    private javax.swing.JButton buttonLibUpload;
    private javax.swing.JButton buttonScriptDel;
    private javax.swing.JButton buttonScriptEdit;
    private javax.swing.JButton buttonScriptNew;
    private javax.swing.JButton buttonScriptUpload;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel panelScripts;
    private javax.swing.JPanel panelScripts1;
    private javax.swing.JTable tableLibs;
    private javax.swing.JTable tableUserScripts;
    private javax.swing.JTextField textLibFilter;
    private javax.swing.JTextField textScriptFilter;
    // End of variables declaration//GEN-END:variables
}
