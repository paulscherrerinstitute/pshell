package ch.psi.pshell.imaging;

import ch.psi.pshell.app.MainFrame;
import ch.psi.pshell.app.Setup;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Threading;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

/**
 *
 */
public class SnapshotDialog extends StandardDialog {

    final Renderer renderer;

    public SnapshotDialog(Renderer renderer) {
        super(SwingUtils.getFrame(renderer), "Snapshot Dialog", false);
        initComponents();
        this.renderer = renderer;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        SwingUtils.centerComponent(renderer, this);
        onLafChange();
    }
    volatile int index = 0;
    volatile Exception backgroungException;

    @Override
    protected void onLafChange() {
         textFolder.setEnabled(MainFrame.isDark());
    }    
    
    @Override
    protected void onClosed() {
        abort();
    }

    void grab(Path file, String format, boolean overwrite, boolean overlays) throws IOException {
        if (!checkOverwrite.isSelected()) {
            if (file.toFile().exists()) {
                throw new IOException("File already exists: " + file.toFile().getName());
            }
        }
        renderer.saveSnapshot(file.toString(), format, checkOverlays.isSelected());
        frameCount++;
        index++;
        synchronized (waitLock) {
            waitLock.notifyAll();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                spIndex.setValue(index);
            }
        });
    }

    void grab() throws IOException {
        String folder = Setup.expandPath(textFolder.getText());
        String format = comboFormat.getSelectedItem().toString();
        index = (Integer) spIndex.getValue();
        Path path = Paths.get(folder, textPrefix.getText() + String.format("_%04d", index) + "." + format);
        grab(path, format, checkOverwrite.isSelected(), checkOverlays.isSelected());
    }

    int frameCount;
    final Object waitLock = new Object();

    void execute(String folder, String prefix, String format, boolean overwrite, boolean overlays) throws IOException, InterruptedException {
        try {
            final boolean everyFrame = cbEveryFrame.isSelected();
            final int interval = (int) (((Double) spInterval.getValue()) * 1000);
            final boolean fixedTime = cbTime.isSelected();
            final int time = (int) (((Double) spTime.getValue()) * 1000);
            final int frames = (Integer) spFrames.getValue();
            final long start = System.currentTimeMillis();
            frameCount = 0;

            if (everyFrame) {
                rendererListener = new RendererListener() {
                    @Override
                    public void onImage(Renderer renderer, Object origin, BufferedImage image, Data data) {
                        try {
                            Path path = Paths.get(folder, prefix + String.format("_%04d", index) + "." + format);
                            grab(path, format, overwrite, overlays);
                        } catch (IOException ex) {
                            backgroungException = ex;
                            abort();
                        }
                    }
                };
                renderer.addListener(rendererListener);
            } else {
                timer = Threading.scheduleAtFixedRateNotRetriggerable(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Path path = Paths.get(folder, prefix + String.format("_%04d", index) + "." + format);
                            grab(path, format, overwrite, overlays);
                        } catch (IOException ex) {
                            backgroungException = ex;
                            abort();
                        }
                    }
                }, 0, interval, TimeUnit.MILLISECONDS, "Snapshot Dialog Task");
            }

            while (true) {
                if (fixedTime) {
                    if ((System.currentTimeMillis() - start) >= time) {
                        return;
                    }
                    Thread.sleep(10);
                } else {
                    synchronized (waitLock) {
                        waitLock.wait();
                    }
                    if (frameCount >= frames) {
                        return;
                    }
                }
            }
        } finally {
            if (rendererListener != null) {
                renderer.removeListener(rendererListener);
                rendererListener = null;
            }
            if (timer != null) {
                timer.shutdownNow();
                timer = null;
            }
        }
    }

    RendererListener rendererListener;

    ScheduledExecutorService timer;

    volatile boolean running;
    Thread grabTask;

    void start() throws IOException {
        backgroungException = null;
        index = (Integer) spIndex.getValue();
        final String folder = Setup.expandPath(textFolder.getText());
        final String prefix = textPrefix.getText();
        final String format = comboFormat.getSelectedItem().toString();
        final boolean overwrite = checkOverwrite.isSelected();
        final boolean overlays = checkOverlays.isSelected();

        running = true;
        updateState();
        grabTask = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    execute(folder, prefix, format, overwrite, overlays);
                } catch (InterruptedException ex) {
                    if (backgroungException != null) {
                        showException(backgroungException);
                    }
                } catch (Exception ex) {
                    showException(ex);
                } finally {
                    running = false;
                    renderer.removeListener(rendererListener);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateState();
                        }
                    });
                }
            }
        });
        grabTask.start();
    }

    void abort() {
        if ((grabTask != null) && (grabTask.isAlive())) {
            grabTask.interrupt();
        }
    }

    void updateState() {
        boolean enabled = !running;
        buttonStart.setText(enabled ? "Start" : "Abort");
        buttonSet.setEnabled(enabled);
        comboFormat.setEnabled(enabled);
        spIndex.setEnabled(enabled);
        textPrefix.setEnabled(enabled);
        cbInterval.setEnabled(enabled);
        cbEveryFrame.setEnabled(enabled);
        cbTime.setEnabled(enabled);
        cbFrames.setEnabled(enabled);
        checkOverwrite.setEnabled(enabled);
        checkOverlays.setEnabled(enabled);
        buttonGrabSingle.setEnabled(enabled);
        spInterval.setEnabled(enabled && cbInterval.isSelected());
        spTime.setEnabled(enabled && cbTime.isSelected());
        spFrames.setEnabled(enabled && cbFrames.isSelected());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        spIndex = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        textPrefix = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        buttonSet = new javax.swing.JButton();
        comboFormat = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        textFolder = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        buttonBrowse = new javax.swing.JButton();
        panelControl = new javax.swing.JPanel();
        buttonGrabSingle = new javax.swing.JButton();
        buttonStart = new javax.swing.JButton();
        cbFrames = new javax.swing.JRadioButton();
        cbTime = new javax.swing.JRadioButton();
        spTime = new javax.swing.JSpinner();
        spFrames = new javax.swing.JSpinner();
        cbInterval = new javax.swing.JRadioButton();
        spInterval = new javax.swing.JSpinner();
        cbEveryFrame = new javax.swing.JRadioButton();
        checkOverwrite = new javax.swing.JCheckBox();
        checkOverlays = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameters"));

        spIndex.setModel(new javax.swing.SpinnerNumberModel(0, 0, 99999, 1));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Prefix:");

        textPrefix.setText("img");

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Folder:");

        buttonSet.setText("Set");
        buttonSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetActionPerformed(evt);
            }
        });

        comboFormat.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "png", "bmp", "jpg", "tif", "gif" }));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Index:");

        textFolder.setEditable(false);
        textFolder.setText("{images}");
        textFolder.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        textFolder.setEnabled(false);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Format:");

        buttonBrowse.setText("Browse");
        buttonBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBrowseActionPerformed(evt);
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
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textPrefix)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spIndex, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFolder)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSet)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonBrowse)))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel3, jLabel4});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonBrowse, buttonSet, comboFormat, jLabel2, spIndex});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSet)
                    .addComponent(buttonBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textPrefix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(spIndex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(comboFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        panelControl.setBorder(javax.swing.BorderFactory.createTitledBorder("Grab Control"));

        buttonGrabSingle.setText("Save Single");
        buttonGrabSingle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonGrabSingleActionPerformed(evt);
            }
        });

        buttonStart.setText("Start");
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });

        buttonGroup2.add(cbFrames);
        cbFrames.setText("Frames:");
        cbFrames.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioActionPerformed(evt);
            }
        });

        buttonGroup2.add(cbTime);
        cbTime.setSelected(true);
        cbTime.setText("Time (s):");
        cbTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioActionPerformed(evt);
            }
        });

        spTime.setModel(new javax.swing.SpinnerNumberModel(10.0d, 0.01d, null, 1.0d));

        spFrames.setModel(new javax.swing.SpinnerNumberModel(10, 1, null, 1));
        spFrames.setEnabled(false);

        buttonGroup1.add(cbInterval);
        cbInterval.setSelected(true);
        cbInterval.setText("Interval (s):");
        cbInterval.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioActionPerformed(evt);
            }
        });

        spInterval.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.01d, null, 1.0d));

        buttonGroup1.add(cbEveryFrame);
        cbEveryFrame.setText("Every frame");
        cbEveryFrame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioActionPerformed(evt);
            }
        });

        checkOverwrite.setText("Overwrite");

        checkOverlays.setText("Overlays");

        javax.swing.GroupLayout panelControlLayout = new javax.swing.GroupLayout(panelControl);
        panelControl.setLayout(panelControlLayout);
        panelControlLayout.setHorizontalGroup(
            panelControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelControlLayout.createSequentialGroup()
                        .addComponent(cbInterval)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(cbEveryFrame))
                .addGap(18, 18, 18)
                .addGroup(panelControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelControlLayout.createSequentialGroup()
                        .addComponent(cbTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelControlLayout.createSequentialGroup()
                        .addComponent(cbFrames)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spFrames, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(panelControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkOverwrite)
                    .addComponent(checkOverlays))
                .addGap(18, 18, 18)
                .addGroup(panelControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonGrabSingle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonStart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        panelControlLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spFrames, spTime});

        panelControlLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cbFrames, cbTime});

        panelControlLayout.setVerticalGroup(
            panelControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(spTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbInterval)
                    .addComponent(spInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbTime)
                    .addComponent(checkOverwrite)
                    .addComponent(buttonGrabSingle))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(spFrames, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbFrames)
                    .addComponent(cbEveryFrame)
                    .addComponent(checkOverlays)
                    .addComponent(buttonStart))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelControl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(panelControl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioActionPerformed
        updateState();
    }//GEN-LAST:event_radioActionPerformed

    private void buttonGrabSingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonGrabSingleActionPerformed
        try {
            grab();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonGrabSingleActionPerformed

    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartActionPerformed
        try {
            if (running) {
                abort();
            } else {
                start();
            }
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_buttonStartActionPerformed

    private void buttonSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetActionPerformed
        try {

            JFileChooser chooser = new JFileChooser(Setup.expandPath(textFolder.getText()));
            chooser.setDialogTitle("Select folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showSaveDialog(renderer) == JFileChooser.APPROVE_OPTION) {
                textFolder.setText(chooser.getSelectedFile().toString());
            }

        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSetActionPerformed

    private void buttonBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBrowseActionPerformed
        try {

            File file = new File(Setup.expandPath(textFolder.getText()));
            Logger.getLogger(SnapshotDialog.class.getName()).fine("Opening desktop for: " + String.valueOf(file));
            Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonBrowseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonBrowse;
    private javax.swing.JButton buttonGrabSingle;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton buttonSet;
    private javax.swing.JButton buttonStart;
    private javax.swing.JRadioButton cbEveryFrame;
    private javax.swing.JRadioButton cbFrames;
    private javax.swing.JRadioButton cbInterval;
    private javax.swing.JRadioButton cbTime;
    private javax.swing.JCheckBox checkOverlays;
    private javax.swing.JCheckBox checkOverwrite;
    private javax.swing.JComboBox comboFormat;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel panelControl;
    private javax.swing.JSpinner spFrames;
    private javax.swing.JSpinner spIndex;
    private javax.swing.JSpinner spInterval;
    private javax.swing.JSpinner spTime;
    private javax.swing.JTextField textFolder;
    private javax.swing.JTextField textPrefix;
    // End of variables declaration//GEN-END:variables
}
