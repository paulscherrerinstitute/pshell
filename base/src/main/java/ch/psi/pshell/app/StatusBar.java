package ch.psi.pshell.app;

import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.State;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker.StateValue;
import javax.swing.Timer;

/**
 * The status bar of Workbench. In detached mode, it can be added to the executing Panel with the
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

        onLafChange();

        timerStateIcon = new Timer(animationRate, (ActionEvent e) -> {
            busyIconIndex = (busyIconIndex + 1) % iconsBusy.length;
            stateIconLabel.setIcon(iconsBusy[busyIconIndex]);
        });
        stateIconLabel.setIcon(iconIdle);
        stateIconLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        ((javax.swing.GroupLayout) getLayout()).setHonorsVisibility(false); //In order not to change if progress bar is shown
    }
    
    @Override
    protected void onLafChange(){
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
        if (App.hasInstance()){
            setApplicationStateIcon(App.getInstance().getState());
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
        if (App.hasInstance()) {
            setApplicationState(App.getInstance().getState());
            App.getInstance().getPropertyChangeSupport().addPropertyChangeListener(this);
        }       
    }

    @Override
    protected void onHide() {
        if (App.hasInstance()) {
            App.getInstance().getPropertyChangeSupport().removePropertyChangeListener(this);
        }
    }

    void setApplicationStateIcon(final State state) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                setApplicationStateIcon(state);
            });
            return;
        }
        if (timerStateIcon!=null){
            switch (state) {
                case Ready -> {
                    if (timerStateIcon.isRunning()) {
                        timerStateIcon.stop();
                    }
                    stateIconLabel.setIcon(iconsBusy[0]);
                }
                case Busy, Paused, Initializing -> {
                    if (!timerStateIcon.isRunning()) {
                        stateIconLabel.setIcon(iconsBusy[0]);
                        busyIconIndex = 0;
                        timerStateIcon.start();
                    }
                }
                case Invalid -> {
                }
                default -> {
                    if (timerStateIcon.isRunning()) {
                        timerStateIcon.stop();
                    }
                    stateIconLabel.setIcon(iconIdle);
                }
            }
        }
    }

    void setApplicationStatusMessage(final State state) {
        setStatusMessage(state.toString());
    }

    public void setApplicationState(final State state) {
        setApplicationStateIcon(state);
        if ((state!=State.Busy) || (command==null)){
            setApplicationStatusMessage(state);
        }
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

    String currentMessage;
    public void setStatusMessage(final String str) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                setStatusMessage(str);
            });
            return;
        }
        if (timerTransitoryStatusMessage!=null){
            timerTransitoryStatusMessage.stop();            
            timerTransitoryStatusMessage = null;
        }
        currentMessage = str;
        statusMessage.setText(currentMessage);
    }
    
    public String getStatusMessage(){
        return currentMessage;
    }
    
    Timer timerTransitoryStatusMessage;
    
    String formerMessage;
    public void setTransitoryStatusMessage(final String str, int timer) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {
                setTransitoryStatusMessage(str, timer);
            });
            return;
        }
        if (timerTransitoryStatusMessage==null){
            formerMessage = currentMessage;
        }
        setStatusMessage(str);
        timerTransitoryStatusMessage = new Timer(timer, (ActionEvent ae) -> {
            setStatusMessage(formerMessage);
        });
        timerTransitoryStatusMessage.start();
        
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

    String command;
    
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
        if ("state".equals(propertyName)) { //Task state
            StateValue state = (StateValue) (e.getNewValue());
            switch (state) {
                case STARTED -> progressBar.setValue(0); //progressBar.setIndeterminate(true);
                case DONE ->  progressBar.setValue(1000); //progressBar.setIndeterminate(false);
            }
        } else if ("message".equals(propertyName)) {
            String text = (String) (e.getNewValue());
            setStatusMessage(text);
        } else if ("command".equals(propertyName)) {
            command = (String) (e.getNewValue());
            setStatusMessage("Running: " + command);
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
            
            if (former == State.Busy){
                command = null;
            }
            //In the end of taks keep final message and progress for some more time
            if ((state == State.Ready) && (former == State.Busy)) {
                SwingUtils.invokeDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (App.hasInstance()){
                            if (state ==App.getInstance().getState()) {
                                setApplicationState(state);
                            }
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
