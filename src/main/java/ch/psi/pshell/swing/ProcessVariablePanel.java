package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ProcessVariable;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.Stoppable;
import ch.psi.utils.Config;
import ch.psi.utils.Config.ConfigListener;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.SpinnerNumberModel;

/**
 *
 */
public final class ProcessVariablePanel extends DevicePanel {

    public ProcessVariablePanel() {
        initComponents();
        textReadback.setVisible(false);
        buttonStop.setVisible(false);
        valueSelection.addListener((ValueSelection origin, double value, boolean editing) -> {
            try {
                if (editing) {
                    write(value);
                }
            } catch (Exception ex) {
                showException(ex);
            }
        });
    }

    void write(double value) {
        getDevice().writeAsync(value).handle((ok, ex) -> {
            if ((ex != null) && ((ex instanceof IOException) || (ex instanceof IllegalArgumentException))) {
                showException((Exception) ex);
                onDeviceValueChanged(getDevice().take(), getDevice().take());
            }
            return ok;
        });
    }

    @Override
    public ProcessVariable getDevice() {
        return (ProcessVariable) super.getDevice();
    }

    public boolean getShowButtons() {
        return valueSelection.isVisible();
    }

    public void setShowButtons(boolean value) {
        valueSelection.setVisible(value);
    }

    public boolean getShowLimitButtons() {
        return valueSelection.getShowLimitButtons();
    }

    public void setShowLimitButtons(boolean value) {
        valueSelection.setShowLimitButtons(value);
    }

    public boolean getShowSliders() {
        return slider.isVisible();
    }

    public void setShowSlider(boolean value) {
        slider.setVisible(value);
    }

    public boolean getShowAdvanced() {
        return panelAdvanced.isVisible();
    }

    public void setShowAdvanced(boolean value) {
        panelAdvanced.setVisible(value);
    }

    boolean showStop = true;

    public boolean getShowStop() {
        return showStop;
    }

    public void setShowStop(boolean value) {
        showStop = value;
    }

    int decimals = 8;

    public void setDecimals(int decimals) {
        this.decimals = decimals;
        if (getDevice() != null) {
            decimals = Math.max(Math.min(decimals, getDevice().getPrecision()), 0);
        }
        valueSelection.setDecimals(decimals);
    }

    public int getDecimals() {
        return decimals;
    }

    double stepSize = Double.NaN;

    public double getStepSize() {
        return stepSize;
    }

    public void setStepSize(double value) {
        stepSize = value;
    }

    double stepIncrement = Double.NaN;

    public double getStepIncrement() {
        return stepIncrement;
    }

    public void setStepIncrement(double value) {
        stepIncrement = value;
    }

    public static Double getIdealStep(double min, double max) {
        if (!Double.isNaN(min) && !Double.isNaN(max)) {
            double range = max - min;
            double idealStep = range / 100;
            //Round to nearest power of ten
            return Math.pow(10, Math.round(Math.log10(idealStep) - Math.log10(5.5) + 0.5));
        }
        return null;
    }

    public static Double getIdealStepIncrement(double step) {
        return step / 10;
    }           

    @Override
    public void setDevice(Device device) {
        if (device != null) {
            ProcessVariable pv = (ProcessVariable) device;
            double min = pv.getMinValue();
            double max = pv.getMaxValue();

            slider.setEnabled(!Double.isNaN(max) && !Double.isNaN(min));

            SpinnerNumberModel model = (SpinnerNumberModel) spinnerStep.getModel();
            if (!Double.isNaN(stepSize)) {
                model.setValue(stepSize);
            } else {
                Double step = getIdealStep(min, max);
                if (step != null) {

                    step = Math.max(step, (Double) model.getMinimum());
                    step = Math.min(step, (Double) model.getMaximum());
                    model.setValue(step);
                    model.setStepSize(getIdealStepIncrement(step));
                }
            }
            if (!Double.isNaN(stepIncrement)) {
                model.setStepSize(stepIncrement);
            }            
            device.getConfig().addListener(new ConfigListener() {
                @Override
                public void onSave(Config config) {
                     setupValueSelection();
                }                
            });
        }        
        super.setDevice(device);
        setupValueSelection();
        updatingSlider = true;
        try {
            if (isRangeDefined()) {
                slider.setMaximum(0);
                slider.setMaximum(1000);
                slider.setEnabled(true);
            } else {
                slider.setEnabled(false);
            }
        } finally {
            updatingSlider = false;
        }

        textReadback.setToolTipText(getDeviceTooltip());
        textReadback.setVisible((device != null) && (device instanceof ReadbackDevice));
        buttonStop.setVisible(showStop && (device != null) && (device instanceof Stoppable));
    }
    
    public void setupValueSelection(){        
        ProcessVariable pv =  getDevice();
        if (pv!=null){
            valueSelection.setMinValue(pv.getMinValue());
            valueSelection.setMaxValue(pv.getMaxValue());
            valueSelection.setStep((Double) spinnerStep.getValue());
            int decimals = getDisplayDecimals(getDecimals());
            valueSelection.setDecimals(decimals);
            valueSelection.setUnit(((ProcessVariable) device).getUnit());        
        }
    }

    boolean isRangeDefined() {
        ProcessVariable pv = (ProcessVariable) getDevice();
        return (pv != null) && !Double.isNaN(pv.getMinValue()) && !Double.isNaN(pv.getMaxValue());
    }

    @Override
    protected void onDeviceStateChanged(State state, State former) {
        if (!state.isInitialized()) {
            clear();
        } else {
            if (former == State.Initializing){
                onDeviceValueChanged(getDevice().take(), null);
            }
        }
    }

    volatile boolean updatingSlider;

    @Override
    protected void onDeviceValueChanged(Object value, Object former) {
        if ((value != null) && (value instanceof Double) && (!Double.isNaN((Double) value))) {
            try {
                ProcessVariable pv = (ProcessVariable) getDevice();
                int decimals = Math.min(getDecimals(), getDevice().getPrecision());
                decimals = Math.max(decimals, 0);
                Double displayValue = Convert.roundDouble((Double) value, decimals);
                valueSelection.setValue(displayValue);
                updatingSlider = true;
                if (isRangeDefined()) {
                    double range = pv.getMaxValue() - pv.getMinValue();
                    double pos = Math.min(Math.max(((Double) value - pv.getMinValue()) / range, 0.0), 1.0);
                    slider.setValue((int) (pos * 1000));
                }
                return;
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
            } finally {
                updatingSlider = false;
            }
        }
        clear();
    }

    @Override
    protected void onDeviceReadbackChanged(Object value) {
        if ((value != null) && (value instanceof Double)) {
            ProcessVariable pv = (ProcessVariable) getDevice();
            int decimals = Math.min(getDecimals(), pv.getPrecision());
            decimals = Math.max(decimals, 0);
            Double displayValue = Convert.roundDouble((Double) value, decimals);
            textReadback.setText(String.format("%1." + decimals + "f", (Double) value));
        } else {
            textReadback.setText("");
        }
    }

    void clear() {
        valueSelection.setValue(Double.NaN);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        valueSelection = new ch.psi.pshell.swing.ValueSelection();
        slider = new javax.swing.JSlider();
        panelAdvanced = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        spinnerStep = new javax.swing.JSpinner();
        textReadback = new javax.swing.JTextField();
        buttonStop = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        slider.setMajorTickSpacing(100);
        slider.setMinorTickSpacing(20);
        slider.setPaintTicks(true);
        slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderStateChanged(evt);
            }
        });

        jLabel1.setText("Step:");

        spinnerStep.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, 1000.0d, 1.0d));
        spinnerStep.setMinimumSize(new java.awt.Dimension(30, 20));
        spinnerStep.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerStepStateChanged(evt);
            }
        });

        textReadback.setEditable(false);
        textReadback.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        textReadback.setDisabledTextColor(javax.swing.UIManager.getDefaults().getColor("TextField.foreground"));
        textReadback.setEnabled(false);

        buttonStop.setText("Stop");
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelAdvancedLayout = new javax.swing.GroupLayout(panelAdvanced);
        panelAdvanced.setLayout(panelAdvancedLayout);
        panelAdvancedLayout.setHorizontalGroup(
            panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAdvancedLayout.createSequentialGroup()
                .addComponent(textReadback)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonStop)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        panelAdvancedLayout.setVerticalGroup(
            panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAdvancedLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(panelAdvancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(spinnerStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(textReadback, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonStop)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueSelection, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(slider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelAdvanced, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(valueSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelAdvanced, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void spinnerStepStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerStepStateChanged
        try {
            valueSelection.setStep((Double) spinnerStep.getValue());
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_spinnerStepStateChanged

    private void sliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderStateChanged

        try {
            if ((!updatingSlider) && (!slider.getValueIsAdjusting())) {
                ProcessVariable pv = (ProcessVariable) getDevice();
                double range = pv.getMaxValue() - pv.getMinValue();
                double value = ((((double) slider.getValue()) / 1000.0) * range) + pv.getMinValue();
                write(value);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_sliderStateChanged

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        try {
            Device device = getDevice();
            if ((device != null) && (device instanceof Stoppable)){
                ((Stoppable)device).stop();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStopActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonStop;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel panelAdvanced;
    private javax.swing.JSlider slider;
    private javax.swing.JSpinner spinnerStep;
    private javax.swing.JTextField textReadback;
    private ch.psi.pshell.swing.ValueSelection valueSelection;
    // End of variables declaration//GEN-END:variables
}
