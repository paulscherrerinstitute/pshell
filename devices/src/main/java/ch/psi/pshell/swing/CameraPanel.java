package ch.psi.pshell.swing;



import ch.psi.pshell.app.MainFrame;
import ch.psi.pshell.device.Camera;
import ch.psi.pshell.device.Camera.ColorMode;
import ch.psi.pshell.device.Camera.DataType;
import ch.psi.pshell.device.Camera.GrabMode;
import ch.psi.pshell.device.Camera.TriggerMode;
import ch.psi.pshell.device.CameraImageDescriptor;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceBase.DeviceInvalidParameterException;
import ch.psi.pshell.swing.ValueSelection.ValueSelectionListener;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JComboBox;

/**
 *
 */
public class CameraPanel extends DevicePanel {

    Camera device;

    public CameraPanel() {
        initComponents();
        onLafChange();

        SwingUtils.setEnumCombo(comboColorMode, ColorMode.class);
        SwingUtils.setEnumCombo(comboDataType, DataType.class);
        SwingUtils.setEnumCombo(comboTriggerMode, TriggerMode.class);
        SwingUtils.setEnumCombo(comboGrabMode, GrabMode.class);

        ValueSelectionListener valueListener = (ValueSelection origin, double value, boolean editing) -> {
            if (editing) {
                try {
                    Camera camera = getDevice();
                    if (origin == valueGain) {
                        camera.setGain(value);
                    } else if (origin == valueBinX) {
                        camera.setBinningX((int) value);
                    } else if (origin == valueBinY) {
                        camera.setBinningY((int) value);
                    } else if ((origin == valueX) || (origin == valueY)
                            || (origin == valueW) || (origin == valueH)) {
                        camera.setROI((int) valueX.getValue(), (int) valueY.getValue(),
                                (int) valueW.getValue(), (int) valueH.getValue());
                    } else if (origin == valueExposure) {
                        camera.setExposure(value);
                    } else if (origin == valuePeriod) {
                        camera.setAcquirePeriod(value);
                    } else if (origin == valueImages) {
                        camera.setNumImages((int) value);
                    } else if (origin == valueIterations) {
                        camera.setIterations((int) value);
                    }
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
    }
    
    @Override
    protected void onLafChange() {
        boolean dark = MainFrame.isDark();
        textSensorSize.setEnabled(dark);
        textImageSize.setEnabled(dark);
        textImageCount.setEnabled(dark);
        textInfo.setEnabled(dark);
    }       

    @Override
    public Camera getDevice() {
        return (Camera) super.getDevice();
    }

    @Override
    public void setDevice(Device device) {
        statePanel.setDevice(device);
        super.setDevice(device);
        if (device == null) {
            stopTimer();
        } else {
            startTimer(2000,0);
        }
    }

    @Override
    protected void onShow() {
        super.onShow();
    }

    @Override
    protected void onTimer() {
        startBackgroundUpdate();
    }

    @Override
    public void setEnabled(boolean value) {
    }

    //Callbacks
    @Override
    protected void onDeviceStateChanged(State state, State former) {
        buttonStart.setEnabled(state == State.Ready);
        buttonStop.setEnabled(state == State.Busy);
    }

    @Override
    protected void onDeviceValueChanged(Object state, Object former) {
    }

    //Backgroud update
    public class DeviceData {

        //read-once
        String info;
        int[] sensorSize;

        boolean started;
        int imageCount;

        CameraImageDescriptor desc;
        double gain;
        int[] roi;
        int binX;
        int binY;

        double exposure;
        double period;
        int images;
        int iterations;
        GrabMode grabMode;
        TriggerMode triggerMode;
    }

    @Override
    protected DeviceData doBackgroundUpdate() throws IOException, InterruptedException {
        DeviceData dd = new DeviceData();
        dd.info = getDevice().getInfo();
        dd.sensorSize = getDevice().getSensorSize();
        dd.started = getDevice().isStarted(); //Updates state
        dd.desc = getDevice().readImageDescriptor(); //Updates value
        dd.imageCount = getDevice().getImagesComplete();
        dd.gain = getDevice().getGain();
        dd.binX = getDevice().getBinningX();
        dd.binY = getDevice().getBinningY();
        dd.roi = getDevice().getROI();
        try {
            dd.grabMode = getDevice().getGrabMode();
        } catch (DeviceInvalidParameterException ex) {
            dd.grabMode = null;
        }
        try {
            dd.triggerMode = getDevice().getTriggerMode();
        } catch (DeviceInvalidParameterException ex) {
            dd.triggerMode = null;
        }
        dd.exposure = getDevice().getExposure();
        dd.period = getDevice().getAcquirePeriod();
        dd.images = getDevice().getNumImages();
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
        //read-once
        if (textInfo.getText().isEmpty() && (dd.info != null)) {
            textInfo.setText(dd.info);
        }
        if (textSensorSize.getText().isEmpty()) {
            textSensorSize.setText(dd.sensorSize[0] + " x " + dd.sensorSize[1]);
        }

        textImageSize.setText(dd.desc.width + " x " + dd.desc.height);
        textImageCount.setText(String.valueOf(dd.imageCount));

        updateValueField(valueGain, dd.gain);
        updateValueField(valueBinX, dd.binX);
        updateValueField(valueBinY, dd.binY);
        updateValueField(valueX, dd.roi[0]);
        updateValueField(valueY, dd.roi[1]);
        updateValueField(valueW, dd.roi[2]);
        updateValueField(valueH, dd.roi[3]);
        updateValueField(valueExposure, dd.exposure);
        updateValueField(valuePeriod, dd.period);
        updateValueField(valueImages, dd.images);
        updateValueField(valueIterations, dd.iterations);
        updateComboField(comboColorMode, dd.desc.colorMode);
        updateComboField(comboDataType, dd.desc.dataType);
        updateComboField(comboGrabMode, dd.grabMode);
        updateComboField(comboTriggerMode, dd.triggerMode);
    }

    /*
     JDialog rendererDialog;
     void showRenderer() {
     JDialog dlg = (JDialog) getTopLevelAncestor();
     if ((rendererDialog!=null)&&(rendererDialog.isDisplayable())) {
     SwingUtils.centerComponent(dlg, rendererDialog);
     rendererDialog.requestFocus();
     return;
     }
        
     final CameraSource source = new CameraSource(getDevice().getName(), getDevice());
     final Renderer renderer = new Renderer(){
     protected void onHide() {
     source.removeListener(this); 
     }            
     };
     source.addListener(renderer);            
     rendererDialog = SwingUtils.showDialog(dlg, getDevice().getName(), new Dimension(600,400), renderer);
     rendererDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
     }   
    
     @Override
     protected void onHide() {
     super.onHide();
     if ((rendererDialog!=null)&&(rendererDialog.isDisplayable())) {
     rendererDialog.setVisible(false);
     rendererDialog=null;
     }
     }    
    
     */
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToggleButton1 = new javax.swing.JToggleButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        valueGain = new ch.psi.pshell.swing.ValueSelection();
        valueBinY = new ch.psi.pshell.swing.ValueSelection();
        valueBinX = new ch.psi.pshell.swing.ValueSelection();
        valueX = new ch.psi.pshell.swing.ValueSelection();
        valueY = new ch.psi.pshell.swing.ValueSelection();
        valueW = new ch.psi.pshell.swing.ValueSelection();
        valueH = new ch.psi.pshell.swing.ValueSelection();
        jLabel11 = new javax.swing.JLabel();
        comboColorMode = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        comboDataType = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        valueExposure = new ch.psi.pshell.swing.ValueSelection();
        jLabel14 = new javax.swing.JLabel();
        valuePeriod = new ch.psi.pshell.swing.ValueSelection();
        jLabel15 = new javax.swing.JLabel();
        valueImages = new ch.psi.pshell.swing.ValueSelection();
        jLabel16 = new javax.swing.JLabel();
        comboGrabMode = new javax.swing.JComboBox();
        comboTriggerMode = new javax.swing.JComboBox();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        valueIterations = new ch.psi.pshell.swing.ValueSelection();
        jPanel3 = new javax.swing.JPanel();
        buttonStart = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        textSensorSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textImageSize = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        textImageCount = new javax.swing.JTextField();
        textInfo = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        statePanel = new ch.psi.pshell.swing.DeviceStatePanel();

        jToggleButton1.setText("jToggleButton1");

        setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Readout"));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Binning X:");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Binning Y:");

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("ROI X:");

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("ROI Y:");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Width:");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Height:");

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText("Gain:");

        valueGain.setDecimals(3);
        valueGain.setMaxValue(1000.0);
        valueGain.setMinValue(0.0);
        valueGain.setShowButtons(false);

        valueBinY.setDecimals(0);
        valueBinY.setMaxValue(16.0);
        valueBinY.setMinValue(0.0);
        valueBinY.setShowButtons(false);

        valueBinX.setDecimals(0);
        valueBinX.setMaxValue(16.0);
        valueBinX.setMinValue(0.0);
        valueBinX.setShowButtons(false);

        valueX.setDecimals(0);
        valueX.setMaxValue(10000.0);
        valueX.setMinValue(0.0);
        valueX.setShowButtons(false);

        valueY.setDecimals(0);
        valueY.setMaxValue(10000.0);
        valueY.setMinValue(0.0);
        valueY.setShowButtons(false);

        valueW.setDecimals(0);
        valueW.setMaxValue(10000.0);
        valueW.setMinValue(0.0);
        valueW.setShowButtons(false);

        valueH.setDecimals(0);
        valueH.setMaxValue(10000.0);
        valueH.setMinValue(0.0);
        valueH.setShowButtons(false);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Color Mode:");

        comboColorMode.setPrototypeDisplayValue("XXXXXXX");
        comboColorMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboColorModeActionPerformed(evt);
            }
        });

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel12.setText("Data Type:");

        comboDataType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboDataTypeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel8)
                    .addComponent(jLabel7)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueX, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueY, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueW, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueH, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueBinX, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueBinY, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueGain, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboColorMode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboDataType, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel6, jLabel7, jLabel8, jLabel9});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {comboColorMode, comboDataType, valueBinX, valueBinY, valueGain, valueH, valueW, valueX, valueY});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel11, jLabel12, jLabel4, jLabel5});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel6)
                    .addComponent(jLabel4)
                    .addComponent(valueBinX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7)
                    .addComponent(valueBinY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel10)
                    .addComponent(jLabel8)
                    .addComponent(valueGain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel9)
                    .addComponent(valueH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel11)
                        .addComponent(comboColorMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(comboDataType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Collect"));

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel13.setText("Exposure:");

        valueExposure.setDecimals(3);
        valueExposure.setMaxValue(10000.0);
        valueExposure.setMinValue(0.0);
        valueExposure.setShowButtons(false);

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel14.setText("Period:");

        valuePeriod.setDecimals(3);
        valuePeriod.setMaxValue(10000.0);
        valuePeriod.setMinValue(0.0);
        valuePeriod.setShowButtons(false);

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Images:");

        valueImages.setDecimals(0);
        valueImages.setMaxValue(10000.0);
        valueImages.setMinValue(0.0);
        valueImages.setShowButtons(false);

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel16.setText("Grab:");

        comboGrabMode.setPrototypeDisplayValue("XXXXXXXXXX");
        comboGrabMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboGrabModeActionPerformed(evt);
            }
        });

        comboTriggerMode.setPrototypeDisplayValue("XXXXXXXXXX");
        comboTriggerMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboTriggerModeActionPerformed(evt);
            }
        });

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel17.setText("Trigger:");

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel18.setText("Iterations:");

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
                    .addComponent(jLabel13)
                    .addComponent(jLabel14)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueImages, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valuePeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueExposure, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueIterations, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboGrabMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboTriggerMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel13, jLabel14, jLabel15});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {comboGrabMode, comboTriggerMode});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {valueExposure, valueImages, valueIterations, valuePeriod});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel16, jLabel17, jLabel18});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel13)
                    .addComponent(valueExposure, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueIterations, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel14)
                    .addComponent(valuePeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel16)
                        .addComponent(comboGrabMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel15)
                    .addComponent(valueImages, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel17)
                        .addComponent(comboTriggerMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Control"));

        buttonStart.setText("Start");
        buttonStart.setMinimumSize(new java.awt.Dimension(80, 23));
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });

        buttonStop.setText("Stop");
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addComponent(buttonStart, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                .addGap(48, 48, 48)
                .addComponent(buttonStop, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                .addGap(48, 48, 48))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonStop))
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Properties"));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Sensor Size:");

        textSensorSize.setEditable(false);
        textSensorSize.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        textSensorSize.setEnabled(false);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Image Size:");

        textImageSize.setEditable(false);
        textImageSize.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        textImageSize.setEnabled(false);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Image Count:");

        textImageCount.setEditable(false);
        textImageCount.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        textImageCount.setEnabled(false);

        textInfo.setEditable(false);
        textInfo.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        textInfo.setEnabled(false);

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel19.setText("Info:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel19, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textInfo, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textImageCount)
                    .addComponent(textSensorSize, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(textImageSize))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textInfo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textSensorSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textImageSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textImageCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(statePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(statePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartActionPerformed
        try {
            getDevice().start();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStartActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        try {
            getDevice().stop();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStopActionPerformed

    private void comboColorModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboColorModeActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                ColorMode mode = (ColorMode) comboColorMode.getSelectedItem();
                if (mode != null) {
                    getDevice().setColorMode(mode);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboColorModeActionPerformed

    private void comboDataTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboDataTypeActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                DataType type = (DataType) comboDataType.getSelectedItem();
                if (type != null) {
                    getDevice().setDataType(type);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboDataTypeActionPerformed

    private void comboGrabModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboGrabModeActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                GrabMode mode = (GrabMode) comboGrabMode.getSelectedItem();
                if (mode != null) {
                    getDevice().setGrabMode(mode);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboGrabModeActionPerformed

    private void comboTriggerModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboTriggerModeActionPerformed
        try {
            if (!isBackgroundUpdate()) {
                TriggerMode mode = (TriggerMode) comboTriggerMode.getSelectedItem();
                if (mode != null) {
                    getDevice().setTriggerMode(mode);
                }
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_comboTriggerModeActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonStart;
    private javax.swing.JButton buttonStop;
    private javax.swing.JComboBox comboColorMode;
    private javax.swing.JComboBox comboDataType;
    private javax.swing.JComboBox comboGrabMode;
    private javax.swing.JComboBox comboTriggerMode;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JToggleButton jToggleButton1;
    private ch.psi.pshell.swing.DeviceStatePanel statePanel;
    private javax.swing.JTextField textImageCount;
    private javax.swing.JTextField textImageSize;
    private javax.swing.JTextField textInfo;
    private javax.swing.JTextField textSensorSize;
    private ch.psi.pshell.swing.ValueSelection valueBinX;
    private ch.psi.pshell.swing.ValueSelection valueBinY;
    private ch.psi.pshell.swing.ValueSelection valueExposure;
    private ch.psi.pshell.swing.ValueSelection valueGain;
    private ch.psi.pshell.swing.ValueSelection valueH;
    private ch.psi.pshell.swing.ValueSelection valueImages;
    private ch.psi.pshell.swing.ValueSelection valueIterations;
    private ch.psi.pshell.swing.ValueSelection valuePeriod;
    private ch.psi.pshell.swing.ValueSelection valueW;
    private ch.psi.pshell.swing.ValueSelection valueX;
    private ch.psi.pshell.swing.ValueSelection valueY;
    // End of variables declaration//GEN-END:variables
}
