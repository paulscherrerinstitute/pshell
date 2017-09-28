package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.scan.ScanListener;
import ch.psi.pshell.scan.ScanRecord;
import ch.psi.pshell.swing.OutputPanel;
import ch.psi.pshell.swing.Shell;
import ch.psi.utils.Chrono;
import ch.psi.utils.ControlChar;
import ch.psi.utils.State;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.TextEditor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker.StateValue;
import javax.swing.Timer;

/**
 * The status bar of Workbench. In dettached mode, it can be added to the executing Panel with the
 * '-sbar'option. Methods to set status label, state iconand progress bar are thread safe (invoke
 * event loop if needed). Icons and some code are taken from Netbeans SAF template.
 */
public class StatusBar extends MonitoredPanel implements PropertyChangeListener {

    private Timer timerStateIcon;
    private Icon iconIdle;
    private Icon[] iconsBusy = new Icon[15];
    private int busyIconIndex = 0;

    /**
     */
    public StatusBar() {
        initComponents();

        progressBar.setModel(new DefaultBoundedRangeModel(0, 0, 0, 1000));
        progressBar.setVisible(false);
        int animationRate = 30;

        String name = "busyicons/idle.png";
        Image image = Toolkit.getDefaultToolkit().getImage(App.getResourceUrl(name));
        if (MainFrame.isDark()) {
            image = SwingUtils.invert(image);
        }
        iconIdle = new ImageIcon(image);

        for (int i = 0; i < iconsBusy.length; i++) {
            name = "busyicons/" + String.format("%02d", i) + ".png";
            image = Toolkit.getDefaultToolkit().getImage(App.getResourceUrl(name));
            if (MainFrame.isDark()) {
                image = SwingUtils.invert(image);
            }
            iconsBusy[i] = new ImageIcon(image);
        }

        timerStateIcon = new Timer(animationRate, (ActionEvent e) -> {
            busyIconIndex = (busyIconIndex + 1) % iconsBusy.length;
            stateIconLabel.setIcon(iconsBusy[busyIconIndex]);
        });
        stateIconLabel.setIcon(iconIdle);
        stateIconLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        ((javax.swing.GroupLayout) getLayout()).setHonorsVisibility(false); //In order not to change if progress bar is shown

        if (App.getInstance().isDetachedAppendStatusBar()) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem openLogs = new JMenuItem("Open logs");
            openLogs.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Context context = Context.getInstance();
                        App app = App.getInstance();
                        TextEditor editor = new TextEditor();
                        editor.load(context.getLogFileName());
                        editor.setReadOnly(true);
                        SwingUtils.showDialog((Frame) getTopLevelAncestor(), context.getLogFileName(), new Dimension(800, 400), editor);
                    } catch (Exception ex) {
                    }
                }
            });

            OutputPanel outputPanel = new OutputPanel();
            outputPanel.initialize();
            if (App.isOutputRedirected()) {
                class ConsoleStream extends OutputStream {

                    final boolean is_err;
                    StringBuilder sb;

                    ConsoleStream(boolean is_err) {
                        this.is_err = is_err;
                        sb = new StringBuilder();
                    }

                    @Override
                    public void write(int b) throws IOException {
                        if (b == ControlChar.LF) {
                            //if (outputPanel.isDisplayable()) {
                            if (is_err) {
                                outputPanel.putError(sb.toString());
                            } else {
                                outputPanel.putOutput(sb.toString());
                            }
                            //}
                            sb = new StringBuilder();
                        } else {
                            sb.append((char) b);
                        }
                    }
                }
                System.setOut(new PrintStream(new ConsoleStream(false)));
                System.setErr(new PrintStream(new ConsoleStream(true)));
            }

            JMenuItem showOutput = new JMenuItem("Show output");

            showOutput.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (!outputPanel.isDisplayable()) {
                            SwingUtils.showDialog((Frame) getTopLevelAncestor(), "Output", new Dimension(800, 400), outputPanel);
                        }
                    } catch (Exception ex) {
                    }
                }
            });

            JMenuItem showConsole = new JMenuItem("Show console");
            Shell shell = new Shell();
            shell.initialize();
            showConsole.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (!shell.isDisplayable()) {
                            SwingUtils.showDialog((Frame) getTopLevelAncestor(), "Console", new Dimension(800, 400), shell);
                        }
                    } catch (Exception ex) {
                    }
                }
            });

            popupMenu.add(openLogs);
            popupMenu.addSeparator();
            popupMenu.add(showOutput);
            popupMenu.add(showConsole);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    checkPopupMenu(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    checkPopupMenu(e);
                }

                void checkPopupMenu(MouseEvent e) {
                    try {
                        if (e.isPopupTrigger()) {
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    } catch (Exception ex) {
                    }
                }
            };

            addMouseListener(mouseAdapter);
        }
    }

    public JLabel getStatusLabel() {
        return statusMessage;
    }

    public JLabel getAuxLabel() {
        return auxMessage;
    }

    public JLabel getStateIconLabel() {
        return stateIconLabel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    @Override
    protected void onShow() {
        App app = App.getInstance();
        if (app != null) {
            setApplicationState(app.getState());
            app.getSwingPropertyChangeSupport().addPropertyChangeListener(this);
        }
        Context context = Context.getInstance();
        if (context != null) {
            context.addScanListener(scanListener);
        }
    }

    @Override
    protected void onHide() {
        App app = App.getInstance();
        if (app != null) {
            app.getSwingPropertyChangeSupport().removePropertyChangeListener(this);
        }
        Context context = Context.getInstance();
        if (context != null) {
            Context.getInstance().removeScanListener(scanListener);
        }
    }

    void setApplicationStateIcon(final State state) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                setApplicationStateIcon(state);
            });
            return;
        }
        switch (state) {
            case Ready:
                if (timerStateIcon.isRunning()) {
                    timerStateIcon.stop();
                }
                stateIconLabel.setIcon(iconsBusy[0]);
                break;
            case Busy:
            case Paused:
            case Initializing:
                if (!timerStateIcon.isRunning()) {
                    stateIconLabel.setIcon(iconsBusy[0]);
                    busyIconIndex = 0;
                    timerStateIcon.start();
                }
                break;
            default:
                if (timerStateIcon.isRunning()) {
                    timerStateIcon.stop();
                }
                stateIconLabel.setIcon(iconIdle);
                break;
        }
    }

    void setApplicationStatusMessage(final State state) {
        setStatusMessage(state.toString());
    }

    public void setApplicationState(final State state) {
        setApplicationStateIcon(state);
        setApplicationStatusMessage(state);
        progressBar.setVisible(state.isProcessing());
        progressBar.setIndeterminate(state == State.Initializing);
        if (!state.isProcessing()) {
            setStatusIconLabelMessage("");
        }
    }

    double progressStep = 0.1;
    double progress = -1;

    public void setProgressStep(double step) {
        progressStep = step;
    }

    public void resetProgress() {
        setProgress(-1);
    }

    public double getProgress() {
        return progress;
    }

    public void stepProgress() {
        double current = getProgress();
        if (current >= 0) {
            setProgress(Math.min(current + progressStep, 1.0));
        } else {
            setProgress(progressStep);
        }
    }

    public void setProgress(final double val) {
        if (val < 0) {
            progress = -1;
        } else {
            progress = Math.min(val, 1.0);
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                updateProgressBar();
            });
        } else {
            updateProgressBar();
        }
    }

    void updateProgressBar() {
        if (progress < 0) {
            progressBar.setValue(0);
        } else {
            int i = (int) (progress * 1000);
            progressBar.setValue(i);
        }
    }

    public void setStatusMessage(final String str) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                setStatusMessage(str);
            });
            return;
        }
        statusMessage.setText(str);
    }

    public void setAuxMessage(final String str) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                setAuxMessage(str);
            });
            return;
        }
        auxMessage.setText(str);
    }

    public void setStatusIconLabelMessage(final String str) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                setStatusIconLabelMessage(str);
            });
            return;
        }
        stateIconLabel.setText(str);
    }

    Timer timerDataFileName;

    public void setShowDataFileName(boolean value) {
        if ((timerDataFileName != null) != value) {
            if (value) {
                final Context context = Context.getInstance();
                getAuxLabel().setForeground(new java.awt.Color(100, 100, 100));
                timerDataFileName = new Timer(2000, (ActionEvent e) -> {
                    try {
                        //Limioting the size of aux message, otherwise will expand window.
                        int maxFilenameLenght = Math.max((getVisibleRect().width - 200) / 8, 10);
                        String file = context.getDataManager().getLastOutput();
                        if (context.getState().isNormal() && (file != null)) {
                            if (file.length() > maxFilenameLenght) {
                                file = file.substring(0, maxFilenameLenght - 3) + "...";
                            }
                            setAuxMessage(file);
                        } else {
                            setAuxMessage("");
                        }
                    } catch (Exception ex) {
                        setAuxMessage(ex.getMessage());
                    }
                });
                timerDataFileName.start();

            } else {
                timerDataFileName.stop();
                timerDataFileName = null;
                setAuxMessage("");
            }
        }
    }

    ScanListener scanListener = new ScanListener() {
        Scan progressScan;

        @Override
        public void onScanStarted(Scan scan, final String plotTitle) {
            setProgress(0);
            int records = scan.getNumberOfRecords();
            if (records > 0) {
                setProgressStep(1.0 / (double) records);
            } else {
                setProgressStep(0);
            }
            progressScan = scan;
        }

        @Override
        public void onNewRecord(Scan scan, ScanRecord record) {
            if (scan == progressScan) {
                stepProgress();
            }
        }

        @Override
        public void onScanEnded(Scan scan, Exception ex) {
            if (scan == progressScan) {
                if (App.getInstance().getRunningTask() == null) {
                    setProgress(-1);
                }
            }
        }
    };

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
        if ("state".equals(propertyName)) { //Task state
            StateValue state = (StateValue) (e.getNewValue());
            switch (state) {
                case STARTED:
                    progressBar.setValue(0);
                    //progressBar.setIndeterminate(true);                    
                    break;
                case DONE:
                    //progressBar.setIndeterminate(false);
                    progressBar.setValue(1000);
                    break;
            }
        } else if ("message".equals(propertyName)) {
            String text = (String) (e.getNewValue());
            setStatusMessage(text);
        } else if ("aux".equals(propertyName)) {
            String text = (String) (e.getNewValue());
            setAuxMessage(text);
        } else if ("timer".equals(propertyName)) {
            long timer = (Long) (e.getNewValue());
            setStatusIconLabelMessage(timer >= 0 ? Chrono.getEllapsedStr((int) timer, "HH:mm:ss") : "");
        } else if ("progress".equals(propertyName)) {
            int value = (Integer) (e.getNewValue()) * 10;
            //progressBar.setIndeterminate(false);
            progressBar.setValue(value);
        } else if ("appstate".equals(propertyName)) {
            State state = (State) (e.getNewValue());
            State former = (State) (e.getOldValue());

            //In the end of taks keep final message and progress for some more time
            if ((state == State.Ready) && (former == State.Busy)) {
                SwingUtils.invokeDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (state == App.getInstance().getState()) {
                            setApplicationState(state);
                        }
                    }
                }, 100);
            } else {
                setApplicationState(state);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessage = new javax.swing.JLabel();
        stateIconLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        auxMessage = new javax.swing.JLabel();

        stateIconLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        stateIconLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        stateIconLabel.setMinimumSize(new java.awt.Dimension(0, 16));

        auxMessage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        auxMessage.setMaximumSize(new java.awt.Dimension(300, 0));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(statusMessage)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(230, 230, 230)
                        .addComponent(auxMessage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stateIconLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(statusPanelSeparator)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(statusMessage)
                    .addComponent(stateIconLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(auxMessage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(2, 2, 2))
        );

        statusMessage.getAccessibleContext().setAccessibleDescription("");
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel auxMessage;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel stateIconLabel;
    private javax.swing.JLabel statusMessage;
    // End of variables declaration//GEN-END:variables

}
