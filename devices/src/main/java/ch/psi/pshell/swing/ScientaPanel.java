package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.epics.Scienta;
import ch.psi.pshell.epics.Scienta.AcquisitionMode;
import ch.psi.pshell.epics.Scienta.DetectorMode;
import ch.psi.pshell.epics.Scienta.ElementSet;
import ch.psi.pshell.epics.Scienta.EnergyMode;
import ch.psi.pshell.epics.Scienta.LensMode;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

/**
 *
 */
public final class ScientaPanel extends DevicePanel {

    LinePlotSeries spectrumSeries;

    public ScientaPanel() {
        initComponents();
        spectrumSeries = new LinePlotSeries("intensity");
        plot.getAxis(Plot.AxisId.X).setLabel("Channels");
        plot.getAxis(Plot.AxisId.Y).setLabel("Intensity");
        plot.addSeries(spectrumSeries);
        plot.setUpdatesEnabled(false);
        //plot.getAxis(Plot.AxisId.X).setRange(-0.1, 50000.1);
        //plot.getAxis(Plot.AxisId.Y).setRange(-0.6, 0.6);
        plot.setQuality(Plot.Quality.Low);

        ValueSelection.ValueSelectionListener valueListener = (ValueSelection origin, double value, boolean editing) -> {
            if (editing) {
                try {
                    if (origin == valueLow) {
                        getDevice().getLowEnergy().writeAsync(value);
                    } else if (origin == valueCenter) {
                        getDevice().getCenterEnergy().writeAsync(value);
                    } else if (origin == valueHigh) {
                        getDevice().getHighEnergy().writeAsync(value);
                    } else if (origin == valueTime) {
                        getDevice().getStepTime().writeAsync(value);
                    } else if (origin == valueSize) {
                        getDevice().getStepSize().writeAsync(value);
                    } else if (origin == valueSlices) {
                        getDevice().getSlices().writeAsync((int) value);
                    } else if (origin == valueIterations) {
                        getDevice().setIterations((int) value);
                    }/*else if (origin == valueFrames) {
                     getDevice().getFrames().writeAsync((int) value);
                     }*/

                } catch (Exception ex) {
                    showException(ex);
                }
            }
        };

        for (ValueSelection vs : SwingUtils.getComponentsByType(this, ValueSelection.class)) {
            if (vs.isEnabled()) {
                vs.addListener(valueListener);
            }
        }
        SwingUtils.setEnumCombo(comboLens, LensMode.class);
        SwingUtils.setEnumCombo(comboElement, ElementSet.class);
        SwingUtils.setEnumCombo(comboAcquisition, AcquisitionMode.class);
        SwingUtils.setEnumCombo(comboEnergy, EnergyMode.class);
        SwingUtils.setEnumCombo(comboDetector, DetectorMode.class);

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (Integer energy : Scienta.PASS_ENERGY_VALUES) {
            model.addElement(energy);
        }
        comboPass.setModel(model);
    }

    public boolean getShowCameraPanel() {
        return cameraPanel.isVisible();
    }

    public void setShowCameraPanel(boolean value) {
        cameraPanel.setVisible(value);
    }

    public boolean getShowSpectrum() {
        return plotSpectrum.isVisible();
    }

    public void setShowSpectrum(boolean value) {
        plotSpectrum.setVisible(value);
    }

    @Override
    public Scienta getDevice() {
        return (Scienta) super.getDevice();
    }

    @Override
    public void setDevice(Device device) {
        if (getDevice() != null) {
            getDevice().getCurrentChannel().removeListener(progressListener);
        }
        super.setDevice(device);
        cameraPanel.setDevice(device);
        if (device == null) {
            stopTimer();
        } else {
            getDevice().getCurrentChannel().addListener(progressListener);
            //cameraPanel.startTimer(3000, 0);
            startTimer(3000, 0);
        }
    }

    DeviceListener progressListener = new DeviceListener() {
        @Override
        public void onValueChanged(final Device device, final Object value, final Object former) {
            SwingUtilities.invokeLater(() -> {
                valueCurrent.setValue((Double) value);
                progress.setValue((int) (getDevice().getProgress() * 1000));
            });
        }
    };

    @Override
    protected void onHide() {
        super.onHide();
    }

    @Override
    protected void onTimer() {
        startBackgroundUpdate();
    }

    //Callbacks
    @Override
    protected void onDeviceStateChanged(State state, State former) {
    }

    @Override
    protected void onDeviceValueChanged(Object state, Object former) {
    }

    public class DeviceData {

        double[] spectrum;
        double[] spectrumX;
        LensMode lensMode;
        ElementSet elementSet;
        AcquisitionMode acquisitionMode;
        EnergyMode energyMode;
        DetectorMode detectorMode;
        double low;
        double center;
        double high;
        double width;
        //int frames;
        double time;
        double size;
        int slices;
        int pass;
        int iterations;

        int current;
        int total;
    }

    @Override
    protected DeviceData doBackgroundUpdate() throws IOException, InterruptedException {
        DeviceData dd = new DeviceData();
        //read-once
        try{
            dd.spectrum = getDevice().getSpectrum().read();
            dd.spectrumX = getDevice().getSpectrumX();
        } catch (Exception ex){
            dd.spectrum = null;
            dd.spectrumX = null;
        }

        try{
            dd.lensMode = getDevice().getLensMode();
        } catch (Exception ex){
            dd.lensMode = null;
        }
        try{
            dd.elementSet = getDevice().getElementSet();
        } catch (Exception ex){
            dd.elementSet = null;
        }
        try{
            dd.acquisitionMode = getDevice().getAcquisitionMode();
        } catch (Exception ex){
            dd.acquisitionMode = null;
        }
        try{
            dd.energyMode = getDevice().getEnergyMode();
        } catch (Exception ex){
            dd.energyMode = null;
        }
        try{
            dd.detectorMode = getDevice().getDetectorMode();
        } catch (Exception ex){
            dd.detectorMode = null;
        }
        try{
            dd.pass = getDevice().getPassEnergy();
        } catch (Exception ex){
            dd.pass = 0;
        }

        dd.low = getDevice().getLowEnergy().getValue();
        dd.center = getDevice().getCenterEnergy().getValue();
        dd.high = getDevice().getHighEnergy().getValue();
        dd.width = getDevice().getEnergyWidth().getValue();
        //dd.frames = getDevice().getFrames().getValue();
        dd.time = getDevice().getStepTime().getValue();
        dd.size = getDevice().getStepSize().getValue();
        dd.slices = getDevice().getSlices().getValue();

        dd.current = getDevice().getCurrentChannel().getValue().intValue();
        dd.total = getDevice().getTotalChannels().getValue().intValue();
        dd.iterations = getDevice().getIterations();
        return dd;
    }

    void updateValueField(ValueSelection field, double value) {
        try {
            field.setValue(value);
        } catch (Exception ex) {
            getLogger().log(Level.FINE, null, ex);
        }
    }

    void updateComboField(JComboBox field, Object value) {
        try {
            if (field.getSelectedItem() != value) {
                field.setSelectedItem(value);
            }
        } catch (Exception ex) {
            getLogger().log(Level.FINE, null, ex);
        }
    }

    @Override
    protected void onBackgroundUpdateFinished(Object data) {
        DeviceData dd = (DeviceData) data;

        updateValueField(valueLow, dd.low);
        updateValueField(valueCenter, dd.center);
        updateValueField(valueHigh, dd.high);
        updateValueField(valueWidth, dd.width);
        //updateValue(valueFrames, dd.frames);
        updateValueField(valueTime, dd.time);
        updateValueField(valueSize, dd.size);
        updateValueField(valueSlices, dd.slices);
        updateValueField(valueIterations, dd.iterations);
        updateValueField(valueTotal, dd.total);
        updateValueField(valueCurrent, dd.current);

        updateComboField(comboLens, dd.lensMode);
        updateComboField(comboAcquisition, dd.acquisitionMode);
        updateComboField(comboDetector, dd.detectorMode);
        updateComboField(comboEnergy, dd.energyMode);
        updateComboField(comboElement, dd.elementSet);
        updateComboField(comboPass, dd.pass);
        
        try {            
            if ((dd.spectrum == null)||(dd.spectrumX==null)) {
                throw new Exception("Invalid spectrum");
            }
            int length = Math.min(dd.spectrumX.length, dd.spectrum.length);
            spectrumSeries.setData( Arrays.copyOfRange(dd.spectrumX, 0, length),Arrays.copyOfRange(dd.spectrum, 0, length));
            plot.updateSeries(spectrumSeries);
        } catch (Exception ex) {
            getLogger().log(Level.FINE, null, ex);
            spectrumSeries.clear();
        }
                

        try {
            progress.setValue((int) (getDevice().getProgress() * 1000));
        } catch (Exception ex) {
            getLogger().log(Level.FINE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cameraPanel = new ch.psi.pshell.swing.CameraPanel();
        scientaPanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        progress = new javax.swing.JProgressBar();
        jLabel6 = new javax.swing.JLabel();
        valueCurrent = new ch.psi.pshell.swing.ValueSelection();
        jLabel16 = new javax.swing.JLabel();
        valueTotal = new ch.psi.pshell.swing.ValueSelection();
        plotSpectrum = new javax.swing.JPanel();
        plot = new ch.psi.pshell.plot.LinePlotJFree();
        jPanel6 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        comboLens = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        comboAcquisition = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        comboEnergy = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        comboDetector = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        comboElement = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        comboPass = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        valueLow = new ch.psi.pshell.swing.ValueSelection();
        jLabel13 = new javax.swing.JLabel();
        valueCenter = new ch.psi.pshell.swing.ValueSelection();
        jLabel14 = new javax.swing.JLabel();
        valueHigh = new ch.psi.pshell.swing.ValueSelection();
        jLabel15 = new javax.swing.JLabel();
        valueWidth = new ch.psi.pshell.swing.ValueSelection();
        jPanel2 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        valueTime = new ch.psi.pshell.swing.ValueSelection();
        jLabel9 = new javax.swing.JLabel();
        valueSize = new ch.psi.pshell.swing.ValueSelection();
        jLabel10 = new javax.swing.JLabel();
        valueSlices = new ch.psi.pshell.swing.ValueSelection();
        jLabel17 = new javax.swing.JLabel();
        valueIterations = new ch.psi.pshell.swing.ValueSelection();
        buttonZeroSupplies = new javax.swing.JButton();

        cameraPanel.setBorder(null);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Progress"));

        progress.setMaximum(1000);

        jLabel6.setText("Current point:");

        valueCurrent.setDecimals(0);
        valueCurrent.setEnabled(false);
        valueCurrent.setMaxValue(100000.0);
        valueCurrent.setMinValue(0.0);
        valueCurrent.setShowButtons(false);

        jLabel16.setText("Total:");

        valueTotal.setDecimals(0);
        valueTotal.setEnabled(false);
        valueTotal.setMaxValue(100000.0);
        valueTotal.setMinValue(0.0);
        valueTotal.setShowButtons(false);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueTotal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel6)
                    .addComponent(valueCurrent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueTotal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addGap(4, 4, 4))
        );

        plotSpectrum.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum"));

        plot.setTitle("");

        javax.swing.GroupLayout plotSpectrumLayout = new javax.swing.GroupLayout(plotSpectrum);
        plotSpectrum.setLayout(plotSpectrumLayout);
        plotSpectrumLayout.setHorizontalGroup(
            plotSpectrumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotSpectrumLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(plot, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
        );
        plotSpectrumLayout.setVerticalGroup(
            plotSpectrumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotSpectrumLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(plot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Modes"));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Lens:");

        comboLens.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboLens.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboLensActionPerformed(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Acquisition:");

        comboAcquisition.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboAcquisition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboAcquisitionActionPerformed(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Energy:");

        comboEnergy.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboEnergy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboEnergyActionPerformed(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Detector:");

        comboDetector.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboDetector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboDetectorActionPerformed(evt);
            }
        });

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Element:");

        comboElement.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboElement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboElementActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(comboLens, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboAcquisition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboElement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboDetector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboEnergy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4, jLabel5});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {comboAcquisition, comboDetector, comboElement, comboEnergy, comboLens});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(comboLens, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboAcquisition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(comboEnergy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(comboDetector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(comboElement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Energy"));

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Pass:");

        comboPass.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2", "5", "10", "20", "50", "100", "200" }));
        comboPass.setToolTipText("");
        comboPass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboPassActionPerformed(evt);
            }
        });

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel12.setText("Low:");

        valueLow.setDecimals(2);
        valueLow.setMaxValue(2000.0);
        valueLow.setMinValue(0.0);
        valueLow.setShowButtons(false);

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel13.setText("Center:");

        valueCenter.setDecimals(2);
        valueCenter.setMaxValue(2000.0);
        valueCenter.setMinValue(0.0);
        valueCenter.setShowButtons(false);

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel14.setText("High:");

        valueHigh.setDecimals(2);
        valueHigh.setMaxValue(2000.0);
        valueHigh.setMinValue(0.0);
        valueHigh.setShowButtons(false);

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Width:");

        valueWidth.setDecimals(2);
        valueWidth.setEnabled(false);
        valueWidth.setMaxValue(2000.0);
        valueWidth.setMinValue(0.0);
        valueWidth.setShowButtons(false);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel15)
                    .addComponent(jLabel14)
                    .addComponent(jLabel13)
                    .addComponent(jLabel12)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueHigh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueCenter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueLow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {comboPass, valueCenter, valueHigh, valueLow, valueWidth});

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel11, jLabel12, jLabel13, jLabel14, jLabel15});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel11)
                    .addComponent(comboPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel12)
                    .addComponent(valueLow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel13)
                    .addComponent(valueCenter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel14)
                    .addComponent(valueHigh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel15)
                    .addComponent(valueWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Step"));

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Time:");

        valueTime.setDecimals(2);
        valueTime.setMaxValue(1000.0);
        valueTime.setMinValue(0.0);
        valueTime.setShowButtons(false);

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Size:");

        valueSize.setDecimals(2);
        valueSize.setMaxValue(1000.0);
        valueSize.setMinValue(0.0);
        valueSize.setShowButtons(false);

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText("Slices:");

        valueSlices.setDecimals(0);
        valueSlices.setMaxValue(1000.0);
        valueSlices.setMinValue(0.0);
        valueSlices.setShowButtons(false);

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel17.setText("Iter:");

        valueIterations.setDecimals(0);
        valueIterations.setMaxValue(1000.0);
        valueIterations.setMinValue(0.0);
        valueIterations.setShowButtons(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(valueSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(valueTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(valueSlices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(valueIterations, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel17, jLabel8, jLabel9});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {valueIterations, valueSize, valueSlices, valueTime});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(valueSlices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(valueTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(valueSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17)
                    .addComponent(valueIterations, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        buttonZeroSupplies.setText("Zero Supplies");
        buttonZeroSupplies.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonZeroSuppliesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(buttonZeroSupplies, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonZeroSupplies)))
                .addGap(0, 0, 0))
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jPanel1, jPanel4});

        javax.swing.GroupLayout scientaPanelLayout = new javax.swing.GroupLayout(scientaPanel);
        scientaPanel.setLayout(scientaPanelLayout);
        scientaPanelLayout.setHorizontalGroup(
            scientaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scientaPanelLayout.createSequentialGroup()
                .addGroup(scientaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(plotSpectrum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        scientaPanelLayout.setVerticalGroup(
            scientaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scientaPanelLayout.createSequentialGroup()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(plotSpectrum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(cameraPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scientaPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(cameraPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(scientaPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void comboLensActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboLensActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                LensMode mode = (LensMode) comboLens.getSelectedItem();
                if (mode != null) {
                    getDevice().setLensMode(mode);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_comboLensActionPerformed

    private void comboAcquisitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboAcquisitionActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                AcquisitionMode mode = (AcquisitionMode) comboAcquisition.getSelectedItem();
                if (mode != null) {
                    getDevice().setAcquisitionMode(mode);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboAcquisitionActionPerformed

    private void comboEnergyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboEnergyActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                EnergyMode mode = (EnergyMode) comboEnergy.getSelectedItem();
                if (mode != null) {
                    getDevice().setEnergyMode(mode);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboEnergyActionPerformed

    private void comboDetectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboDetectorActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                DetectorMode mode = (DetectorMode) comboDetector.getSelectedItem();
                if (mode != null) {
                    getDevice().setDetectorMode(mode);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboDetectorActionPerformed

    private void comboElementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboElementActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                ElementSet mode = (ElementSet) comboElement.getSelectedItem();
                if (mode != null) {
                    getDevice().setElementSet(mode);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboElementActionPerformed

    private void comboPassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboPassActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                int val = (int) comboPass.getSelectedItem();
                getDevice().setPassEnergy(val);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboPassActionPerformed

    private void buttonZeroSuppliesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZeroSuppliesActionPerformed
        try {
            getDevice().zeroSupplies();
        } catch (Exception ex) {
            showException(ex);
        }
        }//GEN-LAST:event_buttonZeroSuppliesActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonZeroSupplies;
    private ch.psi.pshell.swing.CameraPanel cameraPanel;
    private javax.swing.JComboBox comboAcquisition;
    private javax.swing.JComboBox comboDetector;
    private javax.swing.JComboBox comboElement;
    private javax.swing.JComboBox comboEnergy;
    private javax.swing.JComboBox comboLens;
    private javax.swing.JComboBox comboPass;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private ch.psi.pshell.plot.LinePlotJFree plot;
    private javax.swing.JPanel plotSpectrum;
    private javax.swing.JProgressBar progress;
    private javax.swing.JPanel scientaPanel;
    private ch.psi.pshell.swing.ValueSelection valueCenter;
    private ch.psi.pshell.swing.ValueSelection valueCurrent;
    private ch.psi.pshell.swing.ValueSelection valueHigh;
    private ch.psi.pshell.swing.ValueSelection valueIterations;
    private ch.psi.pshell.swing.ValueSelection valueLow;
    private ch.psi.pshell.swing.ValueSelection valueSize;
    private ch.psi.pshell.swing.ValueSelection valueSlices;
    private ch.psi.pshell.swing.ValueSelection valueTime;
    private ch.psi.pshell.swing.ValueSelection valueTotal;
    private ch.psi.pshell.swing.ValueSelection valueWidth;
    // End of variables declaration//GEN-END:variables
}
