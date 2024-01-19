package ch.psi.pshell.swing;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.Plugin;
import ch.psi.utils.Arr;
import ch.psi.utils.IO;
import ch.psi.utils.swing.Document;
import ch.psi.utils.swing.Editor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class PluginsEditor extends Editor {

    final DefaultTableModel model;

    public PluginsEditor() {
        super(new PluginsEditorDocument());
        ((PluginsEditorDocument) getDocument()).editor = this;
        initComponents();

        model = (DefaultTableModel) table.getModel();
        model.addTableModelListener((TableModelEvent e) -> {
            getDocument().setChanged(true);
        });

        ((JComponent) (loaded.getDefaultRenderer(Boolean.class))).setOpaque(true);
        loaded.getColumnModel().getColumn(0).setPreferredWidth(120);
        loaded.getColumnModel().getColumn(1).setPreferredWidth(165);
        loaded.getColumnModel().getColumn(2).setPreferredWidth(30);
        updateLoaded();
        updateButtons();

        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            updateButtons();
        });

        loaded.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            updateLoadedButtons();
        });

    }

    PluginsEditorListener listener;

    /**
     * The listener interface for receiving plugins editor events.
     */
    public interface PluginsEditorListener {

        void onPluginEdit(File file);
    }

    public void setListener(PluginsEditorListener listener) {
        this.listener = listener;
    }

    final List<Plugin> loadedPlugins = new ArrayList<>();

    private void updateLoaded() {
        loadedPlugins.clear();
        ((DefaultTableModel) loaded.getModel()).setNumRows(0);
        for (ch.psi.pshell.core.Plugin p : Context.getInstance().getPlugins()) {
            ((DefaultTableModel) loaded.getModel()).addRow(new Object[]{p.getPluginName(), p.getClass().getName(), Boolean.valueOf(p.isStarted())});
            loadedPlugins.add(p);
        }
        for (Class cls : Context.getInstance().getPluginManager().getDynamicClasses()) {
            ((DefaultTableModel) loaded.getModel()).addRow(new Object[]{"", cls.getName(), Boolean.FALSE});
        }
        updateLoadedButtons();
    }

    Plugin getLoadedTableSelected() {
        if ((loadedPlugins.size() > 0) && (loaded.getSelectedRow() >= 0) && (loaded.getSelectedRow() < loadedPlugins.size())) {
            return loadedPlugins.get(loaded.getSelectedRow());
        }
        return null;
    }

    private void updateLoadedButtons() {
        Plugin p = getLoadedTableSelected();
        boolean enabled = (!isReadOnly()) && (p != null);
        buttonReload.setEnabled(enabled);
        buttonUnload.setEnabled(enabled);
        buttonStart.setEnabled(enabled && !p.isStarted());
        buttonStop.setEnabled(enabled && p.isStarted());
    }

    private void updateButtons() {
        int rows = model.getRowCount();
        int cur = table.getSelectedRow();
        buttonUp.setEnabled((!isReadOnly()) && (rows > 0) && (cur > 0));
        buttonDown.setEnabled((!isReadOnly()) && (rows > 0) && (cur >= 0) && (cur < (rows - 1)));
        String ext = IO.getExtension(getSelectedPlugin());
        buttonLoad.setEnabled((!isReadOnly()) && (listener != null) && (rows > 0) && (cur >= 0) && !Context.getInstance().getPluginManager().isLoaded(String.valueOf(table.getValueAt(cur, 1))));
        buttonEdit.setEnabled((!isReadOnly()) && (listener != null) && (rows > 0) && (cur >= 0) && (!"jar".equals(ext)));
    }

    public static class PluginsEditorDocument extends Document {

        PluginsEditor editor;

        @Override
        public void clear() {
            editor.model.setNumRows(0);
            //Fix bug of nimbus rendering Boolean in table
            ((JComponent) editor.table.getDefaultRenderer(Boolean.class)).setOpaque(true);
            editor.table.getColumnModel().getColumn(0).setResizable(true);
            editor.table.getColumnModel().getColumn(0).setPreferredWidth(70);
            editor.table.getColumnModel().getColumn(1).setPreferredWidth(280);
            setChanged(false);
            editor.updateButtons();
        }

        @Override
        public void load(String fileName) throws IOException {
            clear();
            String pluginsConfigFile = Context.getInstance().getSetup().getPluginsConfigurationFile();
            String pluginsFolder = Context.getInstance().getSetup().getPluginsPath();
            Properties properties = new Properties();
            ArrayList<String> orderedPlugins = new ArrayList<String>();
            try (FileInputStream in = new FileInputStream(pluginsConfigFile)) {
                properties.load(in);
                orderedPlugins = IO.getOrderedPropertyKeys(pluginsConfigFile);
            } catch (Exception ex) {
            }

            File[] pluginFiles = Context.getInstance().getPluginManager().getPluginFolderContents();
            //New files
            for (File file : pluginFiles) {
                String name = file.getName();
                if (!properties.containsKey(name)) {
                    editor.model.addRow(new Object[]{false, name});
                }
            }
            //Files in peoperty files
            for (String path : orderedPlugins) {
                if (Arr.containsEqual(pluginFiles, Paths.get(pluginsFolder, path).toFile())) {
                    Boolean enabled = false;
                    if (properties.containsKey(path)) {
                        if (properties.getProperty(path).equals("enabled")) {
                            enabled = true;
                        }
                    }
                    editor.model.addRow(new Object[]{enabled, path});
                }
            }
            editor.updateButtons();
            setChanged(false);
        }

        @Override
        public void save(String fileName) throws IOException {
            ArrayList<String> lines = new ArrayList<String>();

            //Saving to file in order            
            for (int i = 0; i < editor.model.getRowCount(); i++) {
                Boolean enabled = (Boolean) editor.model.getValueAt(i, 0);
                String name = ((String) editor.model.getValueAt(i, 1)).trim();
                if (!name.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    //Properties duplicate '\' character.
                    //Assumiong the '\' character, in file names, is only used for path separation on Windows
                    //Other scape char sequences will fail
                    name = name.replace("\\", "\\\\");
                    name = name.replace(":", "\\:");
                    name = name.replace(" ", "\\ ");
                    sb.append(name).append("=").append(enabled ? "enabled" : "disabled");
                    lines.add(sb.toString());
                }
            }
            Files.write(Paths.get(fileName), lines);
            IO.setFilePermissions(fileName, Context.getInstance().getConfig().filePermissionsConfig);

            /*
             Properties properties = new Properties();
             for (int i = 0; i < editor.model.getRowCount(); i++) {
             Boolean enabled = (Boolean) editor.model.getValueAt(i, 0);
             String name = ((String) editor.model.getValueAt(i, 1)).trim();
             properties.put(name, enabled ? "enabled" : "disabled");
             }
             try (FileOutputStream out = new FileOutputStream(fileName);) {
             properties.store(out, null);
            IO.setFilePermissions(fileName, Context.getInstance().getConfig().filePermissionsConfig);                
             }
             */
            setChanged(false);
            editor.updateButtons();
        }
    }

    enum Type{
        Standard,
        Panel,
        Processor,
        ScriptPanel
    }
    
    void create(Type type) {
        try {
            String name = getString("Enter Plugin name:", "");
            if ((name == null) || (name.isEmpty())) {
                return;
            }
            File file = Paths.get(Context.getInstance().getSetup().getPluginsPath(), name + ".java").toFile();
            if (file.exists()) {
                throw new Exception("File already exists: " + file.getName());
            }

            String jar = Context.getInstance().getSetup().getJarFile();
            File jarFile = null;
            File templatesFolder = null;
            if (jar == null) {
                if (Context.getInstance().getSetup().isRunningInIde()) {
                    templatesFolder = Paths.get(Context.getInstance().getSetup().getSourceAssemblyFolder(), "templates", "plugin").toFile();
                } else {
                    throw new Exception("Not executing from jar file");
                }
            } else {
                jarFile = new File(jar);
            }
            String path = file.getCanonicalPath();
            switch (type) {
                case Standard:
                    if (jarFile != null) {
                        IO.extractZipFileContent(jarFile, "templates/plugin/DefaultPlugin.java", path);
                    } else {
                        IO.copy(Paths.get(templatesFolder.getPath(), "DefaultPlugin.java").toString(), path);
                    }
                    IO.replace(path, "DefaultPlugin", name);                
                    break;
                case Panel:
                    if (jarFile != null) {
                        IO.extractZipFileContent(jarFile, "templates/plugin/PanelPlugin.java", path);
                    } else {
                        IO.copy(Paths.get(templatesFolder.getPath(), "PanelPlugin.java").toString(), path);
                    }
                    String formFilePath = path.substring(0, path.length() - 4) + "form";
                    if (jarFile != null) {
                        IO.extractZipFileContent(jarFile, "templates/plugin/PanelPlugin.form", formFilePath);
                    } else {
                        IO.copy(Paths.get(templatesFolder.getPath(), "PanelPlugin.form").toString(), formFilePath);
                    }
                    IO.replace(path, "PanelPlugin", name);
                    break;
                case Processor:
                    if (jarFile != null) {
                        IO.extractZipFileContent(jarFile, "templates/plugin/ProcessorPlugin.java", path);
                    } else {
                        IO.copy(Paths.get(templatesFolder.getPath(), "ProcessorPlugin.java").toString(), path);
                    }
                    String formPath = path.substring(0, path.length() - 4) + "form";
                    if (jarFile != null) {
                        IO.extractZipFileContent(jarFile, "templates/plugin/ProcessorPlugin.form", formPath);
                    } else {
                        IO.copy(Paths.get(templatesFolder.getPath(), "ProcessorPlugin.form").toString(), formPath);
                    }
                    IO.replace(path, "ProcessorPlugin", name);
                    break;
                case ScriptPanel:
                    if (jarFile != null) {
                        IO.extractZipFileContent(jarFile, "templates/plugin/ScriptPanel.java", path);
                    } else {
                        IO.copy(Paths.get(templatesFolder.getPath(), "ScriptPanel.java").toString(), path);
                    }
                    String scriptFormPath = path.substring(0, path.length() - 4) + "form";
                    if (jarFile != null) {
                        IO.extractZipFileContent(jarFile, "templates/plugin/ScriptPanel.form", scriptFormPath);
                    } else {
                        IO.copy(Paths.get(templatesFolder.getPath(), "ScriptPanel.form").toString(), scriptFormPath);
                    }
                    IO.replace(path, "ScriptPanel", name);
                    break;                                        
            }
            

            IO.setFilePermissions(path, Context.getInstance().getConfig().filePermissionsScripts);
            showMessage("Plugin Creation", "Success creating plugin: " + name);
            //Reload table
            ((PluginsEditorDocument) getDocument()).load(Context.getInstance().getSetup().getPluginsConfigurationFile());
            for (int i = 0; i < model.getRowCount(); i++) {
                if (file.toString().equals(((String) model.getValueAt(i, 1)).trim())) {
                    model.setValueAt(true, i, 0);
                    break;
                }
            }
            updateButtons();

        } catch (Exception ex) {
            showException(ex);
        }
    }

    @Override
    public void setReadOnly(boolean value) {
        super.setReadOnly(value);
        table.setEnabled(!value);
        updateButtons();
        updateLoadedButtons();
        buttonSave.setEnabled(!value);
        buttonCreateStandard.setEnabled(!value);
        buttonCreatePanel.setEnabled(!value);
    }

    @Override
    public boolean isReadOnly() {
        return !table.isEnabled();
    }

    String getSelectedPlugin() {
        int cur = table.getSelectedRow();
        if (cur < 0) {
            return null;
        }
        return ((String) model.getValueAt(cur, 1)).trim();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonUp = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        buttonEdit = new javax.swing.JButton();
        buttonLoad = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        loaded = new javax.swing.JTable();
        buttonReloadAll = new javax.swing.JButton();
        buttonReload = new javax.swing.JButton();
        buttonUnload = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();
        buttonStart = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        buttonCreateStandard = new javax.swing.JButton();
        buttonCreatePanel = new javax.swing.JButton();
        buttonCreatePanel1 = new javax.swing.JButton();
        buttonCreateScript = new javax.swing.JButton();

        jPanel3.setPreferredSize(new java.awt.Dimension(452, 300));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Plugin"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false
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
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setResizable(false);
        }

        buttonUp.setText("Move Up");
        buttonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpActionPerformed(evt);
            }
        });

        buttonDown.setText("Move Down");
        buttonDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDownActionPerformed(evt);
            }
        });

        buttonSave.setText("Save");
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        buttonEdit.setText("Edit");
        buttonEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonEditActionPerformed(evt);
            }
        });

        buttonLoad.setText("Load");
        buttonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(buttonUp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonDown, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonSave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonEdit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonLoad, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDown, buttonUp});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonDown)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonEdit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonLoad)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 75, Short.MAX_VALUE)
                .addComponent(buttonSave)
                .addContainerGap())
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Enabling", jPanel3);

        loaded.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Plugin", "Class", "Started"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Boolean.class
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
        loaded.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(loaded);
        if (loaded.getColumnModel().getColumnCount() > 0) {
            loaded.getColumnModel().getColumn(0).setResizable(false);
        }

        buttonReloadAll.setText("Reload All");
        buttonReloadAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReloadAllActionPerformed(evt);
            }
        });

        buttonReload.setText("Reload");
        buttonReload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReloadActionPerformed(evt);
            }
        });

        buttonUnload.setText("Unload");
        buttonUnload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUnloadActionPerformed(evt);
            }
        });

        buttonStop.setText("Stop");
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        buttonStart.setText("Start");
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(buttonReloadAll)
                                .addComponent(buttonReload, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addComponent(buttonUnload, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addComponent(buttonStop, javax.swing.GroupLayout.Alignment.TRAILING))
                    .addComponent(buttonStart, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonReload, buttonReloadAll, buttonStart, buttonStop, buttonUnload});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonReload)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonUnload)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonStop)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonStart)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 75, Short.MAX_VALUE)
                .addComponent(buttonReloadAll)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Loaded", jPanel4);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Creation of Dynamic Plugins"));

        buttonCreateStandard.setText("Create Standard Plugin");
        buttonCreateStandard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCreateStandardActionPerformed(evt);
            }
        });

        buttonCreatePanel.setText("Create Panel Plugin");
        buttonCreatePanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCreatePanelActionPerformed(evt);
            }
        });

        buttonCreatePanel1.setText("Create Processor Panel");
        buttonCreatePanel1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCreatePanel1ActionPerformed(evt);
            }
        });

        buttonCreateScript.setText("Create Script Panel");
        buttonCreateScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCreateScriptActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(144, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(buttonCreateStandard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonCreatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonCreatePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonCreateScript, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(145, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCreatePanel, buttonCreatePanel1, buttonCreateStandard});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonCreateStandard)
                .addGap(18, 18, 18)
                .addComponent(buttonCreatePanel)
                .addGap(18, 18, 18)
                .addComponent(buttonCreatePanel1)
                .addGap(18, 18, 18)
                .addComponent(buttonCreateScript)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Creation", jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCreateStandardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCreateStandardActionPerformed
        create(Type.Standard);
    }//GEN-LAST:event_buttonCreateStandardActionPerformed

    private void buttonCreatePanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCreatePanelActionPerformed
        create(Type.Panel);
    }//GEN-LAST:event_buttonCreatePanelActionPerformed

    private void buttonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpActionPerformed
        try {
            int rows = model.getRowCount();
            int cur = table.getSelectedRow();
            model.moveRow(cur, cur, cur - 1);
            table.setRowSelectionInterval(cur - 1, cur - 1);
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonUpActionPerformed

    private void buttonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownActionPerformed
        try {
            int rows = model.getRowCount();
            int cur = table.getSelectedRow();
            model.moveRow(cur, cur, cur + 1);
            table.setRowSelectionInterval(cur + 1, cur + 1);
            updateButtons();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDownActionPerformed

    private void buttonReloadAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReloadAllActionPerformed
        buttonReloadAll.setEnabled(false);
        buttonReloadAll.repaint();
        new Thread(() -> {
            try {
                Context.getInstance().reloadPlugins();
            } catch (Exception ex) {
                showException(ex);
            }
            SwingUtilities.invokeLater(() -> {
                try {
                    updateLoaded();
                    updateButtons();
                } catch (Exception ex) {
                    showException(ex);
                }
                buttonReloadAll.setEnabled(true);
            });
        }).start();
    }//GEN-LAST:event_buttonReloadAllActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try {
            save();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonEditActionPerformed
        try {
            if (listener != null) {
                String pluginsFolder = Context.getInstance().getSetup().getPluginsPath();
                listener.onPluginEdit(Paths.get(pluginsFolder, getSelectedPlugin()).toFile());
            }

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonEditActionPerformed

    private void buttonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadActionPerformed
        try {
            String pluginsFolder = Context.getInstance().getSetup().getPluginsPath();
            File file = Paths.get(pluginsFolder, getSelectedPlugin()).toFile();
            Context.getInstance().getPluginManager().loadInitializePlugin(file);
        } catch (Exception ex) {
            showException(ex);
        } finally {
            updateButtons();
            updateLoaded();
        }
    }//GEN-LAST:event_buttonLoadActionPerformed

    private void buttonReloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReloadActionPerformed
        try {
            Plugin p = getLoadedTableSelected();
            if (p != null) {
                int row = loaded.getSelectedRow();
                Plugin plugin = Context.getInstance().getPluginManager().reloadPlugin(p);
                if (plugin != null) {
                    loadedPlugins.set(row, plugin);
                    loaded.setValueAt(Boolean.valueOf(p.isStarted()), row, 2);
                } else {
                    throw new Exception("Error reloading plugin");
                }
            }
        } catch (Exception ex) {
            updateLoaded();
            showException(ex);
        }
        updateLoadedButtons();
        updateButtons();
    }//GEN-LAST:event_buttonReloadActionPerformed

    private void buttonUnloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUnloadActionPerformed
        try {
            Plugin p = getLoadedTableSelected();
            if (p != null) {
                Context.getInstance().getPluginManager().unloadPlugin(p);
                updateLoaded();
                updateButtons();
            }

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonUnloadActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        try {
            Plugin p = getLoadedTableSelected();
            if (p != null) {
                Context.getInstance().getPluginManager().stopPlugin(p);
                updateLoadedButtons();
                loaded.setValueAt(Boolean.valueOf(p.isStarted()), loaded.getSelectedRow(), 2);
            }

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStopActionPerformed

    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartActionPerformed
        try {
            Plugin p = getLoadedTableSelected();
            if (p != null) {
                Context.getInstance().getPluginManager().startPlugin(p);
                updateLoadedButtons();
                loaded.setValueAt(Boolean.valueOf(p.isStarted()), loaded.getSelectedRow(), 2);
            }

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStartActionPerformed

    private void buttonCreatePanel1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCreatePanel1ActionPerformed
        create(Type.Processor);
    }//GEN-LAST:event_buttonCreatePanel1ActionPerformed

    private void buttonCreateScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCreateScriptActionPerformed
        create(Type.ScriptPanel);
    }//GEN-LAST:event_buttonCreateScriptActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCreatePanel;
    private javax.swing.JButton buttonCreatePanel1;
    private javax.swing.JButton buttonCreateScript;
    private javax.swing.JButton buttonCreateStandard;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonEdit;
    private javax.swing.JButton buttonLoad;
    private javax.swing.JButton buttonReload;
    private javax.swing.JButton buttonReloadAll;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonStart;
    private javax.swing.JButton buttonStop;
    private javax.swing.JButton buttonUnload;
    private javax.swing.JButton buttonUp;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable loaded;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
