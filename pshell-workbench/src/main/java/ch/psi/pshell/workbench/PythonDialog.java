package ch.psi.pshell.workbench;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.framework.Task;
import ch.psi.pshell.scripting.ScriptType;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.SwingUtils.OptionResult;
import ch.psi.pshell.swing.SwingUtils.OptionType;
import ch.psi.pshell.utils.Miniconda;
import ch.psi.pshell.utils.Python;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
import java.awt.Dimension;
import java.awt.Font;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

/**
 *
 */
public class PythonDialog extends StandardDialog {
    public static String getPythonHome(String version){
        return Setup.expandPath("{home}/python-"+version);        
    }

    public PythonDialog(java.awt.Window parent, boolean modal) {
        super(parent, modal);
        setTitle("CPython");
        initComponents();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        updateCurrent();
        setDefaults();    
        updateInstallSource();
        Font f = Preferences.getDefaultEditorFont().toFont();
        textInstalledPackages.setFont(f.deriveFont(f.getSize()-2));
        
        String java_home = System.getenv().get("JAVA_HOME");            
        if ((java_home==null) || (java_home.isBlank())) {
            String jdk = System.getProperty("java.home");
            java.util.logging.Logger.getLogger(PythonDialog.class.getName()).info("Setting JAVA_HOME to: " + jdk) ;
            try{
                Sys.setEnvironmentVariable("JAVA_HOME", jdk);
            } catch (Exception ex){                    
                java.util.logging.Logger.getLogger(PythonDialog.class.getName()).warning("Cannot set  JAVA_HOME") ;
            }
        }        
        java_home = System.getenv().get("JAVA_HOME");
        textJavaHome.setText((java_home==null)? "" : java_home);    
    }

    void setDefaults() {
        try {            
            textDownloadURL.setText(Python.BASE_URL);
            textDownloadVersion.setText(Python.DEFAULT_VERSION);
            textInstallationPackages.setText(String.join("\n", Python.getDefaultPackages()));                                           
            String installPath = getPythonHome(textDownloadVersion.getText());
            textInstallationPath.setText(installPath);                        
            textInstallerMiniconda.setText(Miniconda.getStandardInstaller());
            textDownloadURLMiniconda.setText(Miniconda.MINICONDA_DOWNLOAD_LINK);
      
            
            updateInstallation();
        } catch (Exception ex) {
            showException(ex);
        }

    }
    
    void updateInstallSource(){
        boolean miniconda = radioMiniconda.isSelected();
        labelDownloadURLMiniconda.setVisible(miniconda);        
        labelInstallerMiniconda.setVisible(miniconda);
        textDownloadURLMiniconda.setVisible(miniconda);
        textInstallerMiniconda.setVisible(miniconda);
        labelDownloadURL.setVisible(!miniconda);        
        labelDownloadVersion.setVisible(!miniconda);
        textDownloadURL.setVisible(!miniconda);
        textDownloadVersion.setVisible(!miniconda);
    }
            
    Path getCurrentPath() {
        String home = Setup.getPythonHome();
        if (home == null){
            return null;
        }
        return Paths.get(Setup.expandPath(home));
    }
            
    
    Path getInstallationPath() {
        return Paths.get(textInstallationPath.getText().trim());
    }

    void updateCurrent() {
        String pythonHome = Setup.getPythonHome();                        
        textPythonHome.setText(pythonHome);
        textInstalledVersion.setText(getInstalledVersion());
        textInstalledPackages.setText(String.join("\n", getInstalledPackages()));   
        buttonCheckConnection.setEnabled(!textInstalledVersion.getText().isBlank());
    }
    void updateInstallation(){
        textInstallationVersion.setText(getInstallationVersion());        
    }
    

    void install() {
        try {            
            if ((textJavaHome.getText().isBlank())) {
                if (showOption("Error", "JAVA_HOME is undefined and cannot be set.\nJEP compilation may fail and manual installation be required.\nDo you want to continue?", OptionType.YesNo) == OptionResult.No) {
                    return;
                }
            }            
            Context.getApp().getState().assertReady();         
            Context.getApp().startTask(new CPythonInstall());

        } catch (Exception ex) {
            showException(ex);
            Logger.getLogger(PythonDialog.class.getName()).log(Level.WARNING, null, ex);
        }
    }
    
    void checkInstall() {
        try {                        
            String prefix = null;
            if (Context.getInterpreter().getScriptType() == ScriptType.cpy){
                prefix = (String )Context.getInterpreter().evalLine("sys.prefix");     
            } else {
                Context.getInterpreter().evalLine("from jeputils import get_jep");
                prefix = (String )Context.getInterpreter().evalLine("get_jep('sys.prefix')");                                
            }
            if (textPythonHome.getText().equals(prefix)){
                showMessage("Success", "CPython correctly installed on path: " + prefix);
            } else {
                showMessage("Success", "Check your system, active CPython path is wrong: " + prefix);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }

    String getInstalledVersion() {
        try {
            String version = Python.getVersion(getCurrentPath());
            return (version == null) ? "" : version;
        } catch (Exception ex) {
            return "";
        }
    }
    
    String getInstallationVersion() {
        try {
            String version = Python.getVersion(getInstallationPath());
            return (version == null) ? "" : version;
        } catch (Exception ex) {
            return "";
        }
    }
    
    
    String[] getInstalledPackages(){
        try {
            return Python.getInstalledPackages(getCurrentPath());                                           
        } catch (Exception ex) {
            return new String[0];
        }  
    }
        
    public void setReadOnly(boolean value){
        butonInstall.setEnabled(!value);
    }

    /**
     * Task to ingest session data into SciCat.
     */
    public class CPythonInstall extends Task {

        @Override
        protected String doInBackground() throws Exception {            
            boolean miniconda = radioMiniconda.isSelected();
            String msg = "Installing CPython with " + (miniconda? "Miniconda" : "Python.org");            
            Path path = getInstallationPath();
            String version = getInstallationVersion();
            var ret  = new ArrayList<String>();
            setMessage(msg);
            setProgress(0);
            butonInstall.setEnabled(false);
            JDialog splash;            
            
            try {
                Context.getApp().sendTaskInit(msg);
                                                
                if (!version.isBlank()) {
                    msg = "CPython is already installed: updating packages";
                } else {
                    splash = SwingUtils.showSplash(Context.getView(), "Install", new Dimension(400, 200), "Installing CPython...");
                    try {
                        if (miniconda){
                            ret.add(Miniconda.install(textDownloadURLMiniconda.getText().trim(), textInstallerMiniconda.getText().trim(), path));
                        } else {
                            Python.install(textDownloadVersion.getText().trim(), path);
                        }
                        msg = "Installing packages";
                    } finally {
                        splash.setVisible(false);
                    }
                }
                if (Setup.getPythonHome()==null){
                    if (App.getInstance().getConfig().getPythonHome()==null){
                        App.getInstance().getConfig().pythonHome = path.toString();
                        App.getInstance().getConfig().save();
                    }
                }                   
                
                setMessage(msg);
                setProgress(50);
                String pkgText = textInstallationPackages.getText().trim();
                if (!pkgText.isBlank()){
                    String[] packages = Str.trim(pkgText.split("\n"));
                    if (packages.length>0){
                        splash = SwingUtils.showSplash(Context.getView(), "Install", new Dimension(400, 200), "Installing Packages...");
                        try {
                            if (miniconda){
                                ret.add(Miniconda.installPackages(path, packages));
                            } else {
                                ret.add(Python.installPackages(path, packages));
                            }
                        } finally {
                            splash.setVisible(false);
                        }
                    }
                }

                setMessage("Installing JEP");
                setProgress(90);

                splash = SwingUtils.showSplash(Context.getView(), "Install", new Dimension(400, 200), "Installing JEP...");
                try {
                    if (miniconda){
                        ret.add(Miniconda.pipInstallPackage(path, "jep"));
                    } else {
                        ret.add(Python.installJep(path, textJavaHome.getText()));
                    }
                } finally {
                    splash.setVisible(false);
                }

                SwingUtils.showScrollableMessage(Context.getView(), "CPython", "Success installing Python", String.join("\n\n", ret));

                msg = "Success installing CPython";
                Context.getApp().sendOutput(msg);
                setMessage(msg);
                setProgress(100);
                return msg;
            } catch (Exception ex) {                
                msg = "Error installing CPython";
                setMessage(msg);
                Context.getApp().sendError(ex.toString());
                showException(ex);
                throw ex;
            } finally {
                updateCurrent();
                butonInstall.setEnabled(true);
                Context.getApp().sendTaskFinish(msg);
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        textInstalledVersion = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        textInstalledPackages = new javax.swing.JTextPane();
        jLabel2 = new javax.swing.JLabel();
        textPythonHome = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        buttonCheckConnection = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        textJavaHome = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        textInstallationPackages = new javax.swing.JTextPane();
        textDownloadVersion = new javax.swing.JTextField();
        labelDownloadURL = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        labelDownloadVersion = new javax.swing.JLabel();
        textDownloadURL = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        textInstallationPath = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        buttonDefaults = new javax.swing.JButton();
        butonInstall = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        textInstallationVersion = new javax.swing.JTextField();
        labelDownloadURLMiniconda = new javax.swing.JLabel();
        textDownloadURLMiniconda = new javax.swing.JTextField();
        textInstallerMiniconda = new javax.swing.JTextField();
        labelInstallerMiniconda = new javax.swing.JLabel();
        radioPyhtonOrg = new javax.swing.JRadioButton();
        radioMiniconda = new javax.swing.JRadioButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Python Home:");

        textInstalledVersion.setEditable(false);

        textInstalledPackages.setEditable(false);
        jScrollPane2.setViewportView(textInstalledPackages);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Installed Version:");

        textPythonHome.setEditable(false);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Installed Packages:");

        buttonCheckConnection.setText("Check Connection");
        buttonCheckConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCheckConnectionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel4)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 290, Short.MAX_VALUE)
                            .addComponent(textInstalledVersion)
                            .addComponent(textPythonHome)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonCheckConnection)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(textPythonHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textInstalledVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCheckConnection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 268, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Active", jPanel1);

        textJavaHome.setEditable(false);

        jScrollPane1.setViewportView(textInstallationPackages);

        labelDownloadURL.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        labelDownloadURL.setText("Download URL:");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Packages:");

        labelDownloadVersion.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        labelDownloadVersion.setText("Download Version:");

        textDownloadURL.setEditable(false);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Installation Path:");

        textInstallationPath.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textInstallationPathKeyReleased(evt);
            }
        });

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Java Home:");

        buttonDefaults.setText("Set Defaults");
        buttonDefaults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultsActionPerformed(evt);
            }
        });

        butonInstall.setText("Install");
        butonInstall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butonInstallActionPerformed(evt);
            }
        });

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Version:");

        textInstallationVersion.setEditable(false);

        labelDownloadURLMiniconda.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        labelDownloadURLMiniconda.setText("Download URL:");

        textDownloadURLMiniconda.setEditable(false);

        labelInstallerMiniconda.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        labelInstallerMiniconda.setText("Installer:");

        buttonGroup1.add(radioPyhtonOrg);
        radioPyhtonOrg.setSelected(true);
        radioPyhtonOrg.setText("Python.org");
        radioPyhtonOrg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioPyhtonOrgActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioMiniconda);
        radioMiniconda.setText("Miniconda");
        radioMiniconda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioMinicondaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(labelDownloadURL, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(labelDownloadVersion, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(labelDownloadURLMiniconda, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(labelInstallerMiniconda, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(radioPyhtonOrg)
                        .addGap(18, 18, 18)
                        .addComponent(radioMiniconda)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(textJavaHome)
                    .addComponent(textDownloadURL)
                    .addComponent(jScrollPane1)
                    .addComponent(textDownloadVersion)
                    .addComponent(textInstallationPath, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textInstallationVersion, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textInstallerMiniconda)
                    .addComponent(textDownloadURLMiniconda))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(107, Short.MAX_VALUE)
                .addComponent(buttonDefaults)
                .addGap(18, 18, 18)
                .addComponent(butonInstall)
                .addGap(95, 95, 95))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {butonInstall, buttonDefaults});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textInstallationPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(textInstallationVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(textJavaHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioPyhtonOrg)
                    .addComponent(radioMiniconda))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelDownloadURL)
                    .addComponent(textDownloadURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelDownloadVersion)
                    .addComponent(textDownloadVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelDownloadURLMiniconda)
                    .addComponent(textDownloadURLMiniconda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelInstallerMiniconda)
                    .addComponent(textInstallerMiniconda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(butonInstall)
                    .addComponent(buttonDefaults))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Installation", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void textInstallationPathKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textInstallationPathKeyReleased
        updateInstallation();
    }//GEN-LAST:event_textInstallationPathKeyReleased

    private void buttonDefaultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultsActionPerformed
        setDefaults();
    }//GEN-LAST:event_buttonDefaultsActionPerformed

    private void butonInstallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butonInstallActionPerformed
        install();
    }//GEN-LAST:event_butonInstallActionPerformed

    private void radioPyhtonOrgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioPyhtonOrgActionPerformed
        updateInstallSource();
    }//GEN-LAST:event_radioPyhtonOrgActionPerformed

    private void radioMinicondaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioMinicondaActionPerformed
        updateInstallSource();
    }//GEN-LAST:event_radioMinicondaActionPerformed

    private void buttonCheckConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCheckConnectionActionPerformed
       checkInstall();
    }//GEN-LAST:event_buttonCheckConnectionActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PythonDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PythonDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PythonDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PythonDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                PythonDialog dialog = new PythonDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton butonInstall;
    private javax.swing.JButton buttonCheckConnection;
    private javax.swing.JButton buttonDefaults;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelDownloadURL;
    private javax.swing.JLabel labelDownloadURLMiniconda;
    private javax.swing.JLabel labelDownloadVersion;
    private javax.swing.JLabel labelInstallerMiniconda;
    private javax.swing.JRadioButton radioMiniconda;
    private javax.swing.JRadioButton radioPyhtonOrg;
    private javax.swing.JTextField textDownloadURL;
    private javax.swing.JTextField textDownloadURLMiniconda;
    private javax.swing.JTextField textDownloadVersion;
    private javax.swing.JTextPane textInstallationPackages;
    private javax.swing.JTextField textInstallationPath;
    private javax.swing.JTextField textInstallationVersion;
    private javax.swing.JTextPane textInstalledPackages;
    private javax.swing.JTextField textInstalledVersion;
    private javax.swing.JTextField textInstallerMiniconda;
    private javax.swing.JTextField textJavaHome;
    private javax.swing.JTextField textPythonHome;
    // End of variables declaration//GEN-END:variables
}
