package ch.psi.pshell.workbench;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.framework.Task;
import ch.psi.pshell.scripting.JepUtils;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.SwingUtils.OptionResult;
import ch.psi.pshell.swing.SwingUtils.OptionType;
import ch.psi.pshell.utils.Miniconda;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

/**
 *
 */
public class MinicondaDialog extends StandardDialog {
    public static String DEFAULT_PYTHON_HOME = "{home}/cpython";

    public MinicondaDialog(java.awt.Window parent, boolean modal) {
        super(parent, modal);
        setTitle("CPython");
        initComponents();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setDefaults();
        updateVersion();
        
        String java_home = System.getenv().get("JAVA_HOME");            
        if ((java_home==null) || (java_home.isBlank())) {
            String jdk = System.getProperty("java.home");
            java.util.logging.Logger.getLogger(MinicondaDialog.class.getName()).info("Setting JAVA_HOME to: " + jdk) ;
            try{
                Sys.setEnvironmentVariable("JAVA_HOME", jdk);
            } catch (Exception ex){                    
                java.util.logging.Logger.getLogger(MinicondaDialog.class.getName()).warning("Cannot set  JAVA_HOME") ;
            }
        }        
        java_home = System.getenv().get("JAVA_HOME");
        textJavaHome.setText((java_home==null)? "" : java_home);        
    }

    void setDefaults() {
        try {
            String pythonHome = JepUtils.getPythonHome();
            String installPath = Setup.expandPath((pythonHome!=null) ? pythonHome : DEFAULT_PYTHON_HOME);
            textPythonHome.setText(pythonHome);
            textInstaller.setText(Miniconda.getStandardInstaller());
            textLink.setText(Miniconda.MINICONDA_DOWNLOAD_LINK);
            textPath.setText(installPath);
            textPackages.setText(String.join("\n", Miniconda.getDefaultPackages()));       
        } catch (Exception ex) {
            showException(ex);
        }

    }

    Path getPath() {
        return Paths.get(textPath.getText().trim());
    }

    void updateVersion() {
        textVersion.setText(getVersion());
    }

    void install() {
        try {            
            if ((textJavaHome.getText().isBlank())) {
                if (showOption("JAVA_HOME is undefined and cannot be set.\nJEP compilation may fail and manual installation be required.\nDo you want to continue?", this, OptionType.YesNo) == OptionResult.No) {
                    return;
                }
            }                        
            Context.getApp().getState().assertReady();
            if (JepUtils.getPythonHome()==null){
                if (App.getInstance().getConfig().getPythonHome()==null){
                    //User DEFAULT_PYTHON_HOME, so save it.
                    App.getInstance().getConfig().pythonHome = DEFAULT_PYTHON_HOME;
                    App.getInstance().getConfig().save();
                }
            }            
            Context.getApp().startTask(new CPythonInstall());

        } catch (Exception ex) {
            showException(ex);
            Logger.getLogger(MinicondaDialog.class.getName()).log(Level.WARNING, null, ex);
        }

    }

    String getVersion() {
        try {
            String version = Miniconda.getVersion(getPath());
            return (version == null) ? "" : version;
        } catch (Exception ex) {
            return "";
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
            String msg = "Installing CPython";
            Path path = getPath();
            String version = getVersion();
            String r1 = "", r2, r3;
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
                        r1 = Miniconda.install(textLink.getText().trim(), textInstaller.getText().trim(), path);
                        msg = "Installing packages";
                    } finally {
                        splash.setVisible(false);
                    }
                }
                setMessage(msg);
                setProgress(50);
                splash = SwingUtils.showSplash(Context.getView(), "Install", new Dimension(400, 200), "Installing Packages...");
                try {
                    r2 = Miniconda.installPackages(path, Str.trim(textPackages.getText().trim().split("\n")));
                } finally {
                    splash.setVisible(false);
                }

                setMessage("Installing JEP");
                setProgress(90);

                splash = SwingUtils.showSplash(Context.getView(), "Install", new Dimension(400, 200), "Installing Packages...");
                try {
                    r3 = Miniconda.pipInstallPackage(path, "jep");
                } finally {
                    splash.setVisible(false);
                }

                SwingUtils.showScrollableMessage(Context.getView(), "CPython", "Success installing Python", String.join("\n\n", new String[]{r1, r2, r3}));

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
                updateVersion();
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

        jLabel1 = new javax.swing.JLabel();
        textPath = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textVersion = new javax.swing.JTextField();
        butonInstall = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        textLink = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        textInstaller = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        buttonDefaults = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        textPackages = new javax.swing.JTextPane();
        jLabel6 = new javax.swing.JLabel();
        textPythonHome = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        textJavaHome = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Installation Path:");

        textPath.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textPathKeyReleased(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Version:");

        textVersion.setEditable(false);

        butonInstall.setText("Install");
        butonInstall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butonInstallActionPerformed(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Download Link:");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Installer:");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Default Packages:");

        buttonDefaults.setText("Set Defaults");
        buttonDefaults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultsActionPerformed(evt);
            }
        });

        jScrollPane1.setViewportView(textPackages);

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Python Home:");

        textPythonHome.setEditable(false);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Java Home:");

        textJavaHome.setEditable(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 99, Short.MAX_VALUE)
                        .addComponent(buttonDefaults)
                        .addGap(18, 18, 18)
                        .addComponent(butonInstall)
                        .addGap(0, 99, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel6)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addComponent(jLabel2)
                                .addComponent(jLabel3)
                                .addComponent(jLabel4)
                                .addComponent(jLabel5))
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(textJavaHome)
                            .addComponent(textPath)
                            .addComponent(textVersion)
                            .addComponent(textLink)
                            .addComponent(textInstaller)
                            .addComponent(jScrollPane1)
                            .addComponent(textPythonHome))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4, jLabel5});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {butonInstall, buttonDefaults});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(textPythonHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(textJavaHome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textLink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textInstaller, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(butonInstall)
                            .addComponent(buttonDefaults))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void textPathKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textPathKeyReleased
        updateVersion();
    }//GEN-LAST:event_textPathKeyReleased

    private void buttonDefaultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultsActionPerformed
        setDefaults();
    }//GEN-LAST:event_buttonDefaultsActionPerformed

    private void butonInstallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butonInstallActionPerformed
        install();
    }//GEN-LAST:event_butonInstallActionPerformed

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
            java.util.logging.Logger.getLogger(MinicondaDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MinicondaDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MinicondaDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MinicondaDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                MinicondaDialog dialog = new MinicondaDialog(new javax.swing.JFrame(), true);
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
    private javax.swing.JButton buttonDefaults;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField textInstaller;
    private javax.swing.JTextField textJavaHome;
    private javax.swing.JTextField textLink;
    private javax.swing.JTextPane textPackages;
    private javax.swing.JTextField textPath;
    private javax.swing.JTextField textPythonHome;
    private javax.swing.JTextField textVersion;
    // End of variables declaration//GEN-END:variables
}
