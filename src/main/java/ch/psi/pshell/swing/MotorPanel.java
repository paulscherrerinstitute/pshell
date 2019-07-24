package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Motor;
import ch.psi.utils.State;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.SpinnerNumberModel;

/**
 *
 */
public class MotorPanel extends DevicePanel {

    public MotorPanel() {
        initComponents();
        FocusListener jogFocusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if ((e.isTemporary()) && (e.getSource() instanceof JButton)) {
                    if (jogging) {
                        stop();
                    }
                }
            }
        };
        buttonJogPos.addFocusListener(jogFocusListener);
        buttonJogNeg.addFocusListener(jogFocusListener);
        if (!defaultShowHoming){
            setShowHoming(false);
        }
        if (!defaultShowJog){
            setShowJog(false);
        }        
        //textState.setBackground(TEXT_READONLY_BACKGROUND_COLOR);        
    }

    private static boolean defaultShowHoming = true;
    
    public static void setDefaultShowHoming(boolean value){
        defaultShowHoming = value;
    }

    public static boolean getDefaultShowHoming(){
        return defaultShowHoming;
    }    
    
    private static boolean defaultShowJog = true;
    
    public static void setDefaultShowJog(boolean value){
        defaultShowJog = value;
    }

    public static boolean getDefaultShowJog(){
        return defaultShowJog;
    }       
    
    public boolean getShowButtons() {
        return valueSelection.isVisible();
    }

    public void setShowButtons(boolean value) {
        valueSelection.setVisible(value);
    }

    public boolean getShowStatus() {
        return panelStatus.isVisible();
    }

    public void setShowStatus(boolean value) {
        panelStatus.setVisible(value);
    }

    public boolean getShowAdvanced() {
        return panelAdvanced.isVisible();
    }

    public void setShowAdvanced(boolean value) {
        panelAdvanced.setVisible(value);
    }

    public boolean getShowJog() {
        return buttonJogNeg.isVisible();
    }

    public void setShowJog(boolean value) {
        buttonJogNeg.setVisible(value);
        buttonJogPos.setVisible(value);
    }
    
    public boolean getShowTweak() {
        return buttonBack.isVisible();
    }

    public void setShowTweak(boolean value) {
        buttonBack.setVisible(value);
        buttonFor.setVisible(value);
    }    

    public boolean getShowHoming() {
        return buttonReset.isVisible();
    }

    public void setShowHoming(boolean value) {
        buttonReset.setVisible(value);
    }

    double stepSize = Double.NaN;

    public double getStepSize() {
        return stepSize;
    }

    public void setStepSize(double value) {
        stepSize = value;
        if (device != null) {
            updateSpinnerStep();
        }
    }

    double stepIncrement = Double.NaN;

    public double getStepIncrement() {
        return stepIncrement;
    }

    public void setStepIncrement(double value) {
        stepIncrement = value;
    }

    void updateSpinners() {
        int width = Math.max(spinnerSpeed.getSize().width, spinnerStep.getSize().width);
        width = Math.max(width, spinnerSpeed.getPreferredSize().width);
        width = Math.max(width, spinnerStep.getPreferredSize().width);
        if (width > 0) {
            spinnerSpeed.setPreferredSize(new Dimension(width, spinnerSpeed.getPreferredSize().height));
            spinnerStep.setPreferredSize(new Dimension(width, spinnerStep.getPreferredSize().height));
        }
    }

    @Override
    public Motor getDevice() {
        return (Motor) super.getDevice();
    }
    
    void updateSpinnerStep(){
        SpinnerNumberModel model = (SpinnerNumberModel) spinnerStep.getModel();
        if (!Double.isNaN(stepSize)) {
            model.setValue(stepSize);
        } else {
            double min = getDevice().getMinSpeed();
            double max = getDevice().getMaxSpeed();            
            min = getDevice().getMinValue();
            max = getDevice().getMaxValue();
            Double step = ProcessVariablePanel.getIdealStep(min, max);
            if (step != null) {
                step = Math.max(step, (Double) model.getMinimum());
                step = Math.min(step, (Double) model.getMaximum());
                model.setValue(step);
                model.setStepSize(ProcessVariablePanel.getIdealStepIncrement(step));
            }
        }
        if (!Double.isNaN(stepIncrement)) {
            model.setStepSize(stepIncrement);
        }
    }

    @Override
    public void setDevice(Device device) {
        super.setDevice(device);
        readout.setDevice(device);
        if (device != null) {
            onDeviceStateChanged(device.getState(), null);
            onDeviceStatusChanged(((Motor) device).takeStatus());
            double min = getDevice().getMinSpeed();
            double max = getDevice().getMaxSpeed();
            double def = getDevice().getDefaultSpeed();
            def = Math.max(min, def);
            def = Math.min(max, def);
            spinnerSpeed.setModel(new SpinnerNumberModel(def, min, max, 1.0));


            updateSpinnerStep();
            updateSpinners();
            //In a different thread because can be blocking
            new Thread(() -> {
                try {
                    updatingSpeed = true;
                    spinnerSpeed.setValue(getDevice().getSpeed());
                } catch (Exception ex) {
                } finally {
                    updatingSpeed = false;
                }
            }).start();

        } else {
            textState.setText("");
        }
    }

    boolean updatingSpeed;

    protected void setSpeed(double speed) throws IOException {
        Double current = getDevice().getVelocity().take();
        if ((current == null) || (current != speed)) {
            getDevice().getVelocity().writeAsync(speed);
        }
    }

    protected void moveRel(double offset) throws IOException {
        Double destination = getDevice().take();
        if (destination != null) {
            destination += offset;
            getDevice().writeAsync(destination).handle((ok, ex) -> {
                if ((ex != null) && (ex instanceof IOException)) {
                    SwingUtils.showException(this, (Exception) ex);
                }
                return ok;
            });

        }
    }

    boolean jogging;

    protected void jog(boolean positive) throws IOException, InterruptedException {
        getDevice().startJog(positive);
        jogging = true;
    }

    protected void reference() {
        new Thread(() -> {
            try {
                getDevice().reference();
            } catch (Exception ex) {
            }
        }).start();
    }

    protected void stop() {
        jogging = false;
        new Thread(() -> {
            try {
                getDevice().stop();
            } catch (Exception ex) {
            }
        }).start();
    }

    @Override
    protected void onDeviceSpeedChanged(Double value) {
        updatingSpeed = true;
        try {
            spinnerSpeed.setValue(value);
        } finally {
            updatingSpeed = false;
        }
    }

    @Override
    protected void onDeviceStateChanged(State state, State former) {
        if ((state != State.Ready) || (state != State.Busy)) {
            textState.setText(state.toString());
        }
    }

    @Override
    protected void onDeviceStatusChanged(Object value) {
        textState.setText((value == null) ? "" : value.toString());
    }

    public void setDecimals(int decimals) {
        readout.setDecimals(decimals);
    }

    public int getDecimals() {
        return readout.getDecimals();
    }

    @Override
    public void setEnabled(boolean value) {
        buttonJogNeg.setEnabled(value);
        buttonJogPos.setEnabled(value);
        buttonBack.setEnabled(value);
        buttonFor.setEnabled(value);
        buttonReset.setEnabled(value);
        buttonStop.setEnabled(value);
        spinnerStep.setEnabled(value);
        spinnerSpeed.setEnabled(value);
        readout.setEnabled(value);
        if (!value) {
            textState.setText("");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        valueSelection = new javax.swing.JPanel();
        readout = new ch.psi.pshell.swing.MotorReadoutPanel();
        buttonJogPos = new javax.swing.JButton();
        buttonFor = new javax.swing.JButton();
        buttonBack = new javax.swing.JButton();
        buttonJogNeg = new javax.swing.JButton();
        panelAdvanced = new javax.swing.JPanel();
        buttonReset = new javax.swing.JButton();
        spinnerStep = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        buttonStop = new javax.swing.JButton();
        panelStatus = new javax.swing.JPanel();
        textState = new javax.swing.JTextField();
        spinnerSpeed = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        buttonJogPos.setText(">>");
        buttonJogPos.setToolTipText("Jog Forward");
        buttonJogPos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                buttonJogPosMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonJogPosMouseReleased(evt);
            }
        });

        buttonFor.setText(">|");
        buttonFor.setToolTipText("Step Forward");
        buttonFor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonForActionPerformed(evt);
            }
        });

        buttonBack.setText("|<");
        buttonBack.setToolTipText("Step Backwad");
        buttonBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBackActionPerformed(evt);
            }
        });

        buttonJogNeg.setText("<<");
        buttonJogNeg.setToolTipText("Jog Backward");
        buttonJogNeg.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                buttonJogNegMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                buttonJogNegMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout valueSelectionLayout = new javax.swing.GroupLayout(valueSelection);
        valueSelection.setLayout(valueSelectionLayout);
        valueSelectionLayout.setHorizontalGroup(
            valueSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(valueSelectionLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(buttonJogNeg)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonBack)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(readout, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonFor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonJogPos)
                .addGap(0, 0, 0))
        );

        valueSelectionLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonBack, buttonFor, buttonJogNeg, buttonJogPos});

        valueSelectionLayout.setVerticalGroup(
            valueSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(valueSelectionLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(valueSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(valueSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(buttonBack)
                        .addComponent(buttonJogNeg))
                    .addComponent(readout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(valueSelectionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(buttonFor)
                        .addComponent(buttonJogPos)))
                .addGap(0, 0, 0))
        );

        valueSelectionLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonBack, buttonFor, buttonJogNeg, buttonJogPos, readout});

        buttonReset.setText("Home");
        buttonReset.setToolTipText("Start Homing");
        buttonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetActionPerformed(evt);
            }
        });

        spinnerStep.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, 10000.0d, 1.0d));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Step:");

        buttonStop.setText("Stop");
        buttonStop.setToolTipText("Stop Movement");
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelAdvancedLayout = new javax.swing.GroupLayout(panelAdvanced);
        panelAdvanced.setLayout(panelAdvancedLayout);
        panelAdvancedLayout.setHorizontalGroup(
            panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAdvancedLayout.createSequentialGroup()
                .addComponent(buttonReset)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonStop)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        panelAdvancedLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonReset, buttonStop});

        panelAdvancedLayout.setVerticalGroup(
            panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAdvancedLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(buttonReset)
                    .addComponent(buttonStop)
                    .addComponent(spinnerStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(0, 0, 0))
        );

        textState.setEditable(false);
        textState.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        textState.setDisabledTextColor(javax.swing.UIManager.getDefaults().getColor("TextPane.foreground"));
        textState.setEnabled(false);

        spinnerSpeed.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, 10000.0d, 1.0d));
        spinnerSpeed.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerSpeedStateChanged(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Speed:");

        javax.swing.GroupLayout panelStatusLayout = new javax.swing.GroupLayout(panelStatus);
        panelStatus.setLayout(panelStatusLayout);
        panelStatusLayout.setHorizontalGroup(
            panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStatusLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(textState, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
        panelStatusLayout.setVerticalGroup(
            panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStatusLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(textState, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(spinnerSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueSelection, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelAdvanced, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(valueSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelAdvanced, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBackActionPerformed
        try {
            double step = (Double) spinnerStep.getValue();
            moveRel(-step);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonBackActionPerformed

    private void buttonForActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonForActionPerformed
        try {
            double step = (Double) spinnerStep.getValue();
            moveRel(step);
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_buttonForActionPerformed

    private void buttonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetActionPerformed
        try {
            reference();
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_buttonResetActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        try {
            stop();
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_buttonStopActionPerformed

    private void buttonJogPosMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonJogPosMousePressed
        try {
            jog(true);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonJogPosMousePressed

    private void buttonJogPosMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonJogPosMouseReleased
        stop();
    }//GEN-LAST:event_buttonJogPosMouseReleased

    private void buttonJogNegMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonJogNegMousePressed
        try {
            jog(false);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonJogNegMousePressed

    private void buttonJogNegMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buttonJogNegMouseReleased
        stop();
    }//GEN-LAST:event_buttonJogNegMouseReleased

    private void spinnerSpeedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSpeedStateChanged
        try {
            if (!updatingSpeed) {
                setSpeed((Double) spinnerSpeed.getValue());
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_spinnerSpeedStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonBack;
    private javax.swing.JButton buttonFor;
    private javax.swing.JButton buttonJogNeg;
    private javax.swing.JButton buttonJogPos;
    private javax.swing.JButton buttonReset;
    private javax.swing.JButton buttonStop;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel panelAdvanced;
    private javax.swing.JPanel panelStatus;
    private ch.psi.pshell.swing.MotorReadoutPanel readout;
    private javax.swing.JSpinner spinnerSpeed;
    private javax.swing.JSpinner spinnerStep;
    private javax.swing.JTextField textState;
    private javax.swing.JPanel valueSelection;
    // End of variables declaration//GEN-END:variables
}
