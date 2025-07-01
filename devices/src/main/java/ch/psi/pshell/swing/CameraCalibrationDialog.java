package ch.psi.pshell.swing;

import ch.psi.pshell.archiver.Inventory;
import ch.psi.pshell.camserver.CameraSource;
import ch.psi.pshell.devices.App;
import ch.psi.pshell.devices.Setup;
import ch.psi.pshell.imaging.ImageRenderer;
import ch.psi.pshell.imaging.Overlay;
import ch.psi.pshell.imaging.Overlays;
import ch.psi.pshell.imaging.Pen;
import ch.psi.pshell.imaging.Renderer;
import ch.psi.pshell.imaging.RendererListener;
import ch.psi.pshell.imaging.RendererMode;
import ch.psi.pshell.utils.Convert;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;


/**
 *
 */
public class CameraCalibrationDialog extends StandardDialog {
    
    CameraSource server;
    Renderer renderer;
    String cameraName;
    Overlay[] calibrationOverlays;
    Pen pen = new Pen(new Color(128, 0, 128), 1, Pen.LineStyle.solid);                        
    Overlays.Crosshairs ovTop = new Overlays.Crosshairs(pen, new Dimension(-1, 1));
    Overlays.Crosshairs ovBottom = new Overlays.Crosshairs(pen, new Dimension(-1, 1));
    Overlays.Crosshairs ovLeft = new Overlays.Crosshairs(pen, new Dimension(1, -1));
    Overlays.Crosshairs ovRight = new Overlays.Crosshairs(pen, new Dimension(1, -1));
        
    public CameraCalibrationDialog(Window parent, String cameraServer, String cameraName, Renderer renderer) throws IOException {
        super(parent, cameraName, false);        
        
        initComponents();    
        server = new CameraSource("Camera Server", cameraServer);   
                        
        this.cameraName = cameraName;
        this.renderer = renderer;
        
        Map<String, Object> config = server.getConfig(cameraName);
        Map<String, Object> calibration = (Map<String, Object>) config.get("camera_calibration");
        List<Integer> refMarker = null;
        System.out.println("Current config: " + config);
        checkMirrorX.setSelected((config.get("mirror_x") ==null) ? Boolean.FALSE: (Boolean)config.get("mirror_x"));
        checkMirrorY.setSelected((config.get("mirror_y") ==null) ? Boolean.FALSE: (Boolean)config.get("mirror_y"));
        comboRotation.setSelectedIndex((config.get("rotate") == null)  ? 0 :(Integer) (config.get("rotate")));
        ckCalibrationEnabled.setSelected(calibration!=null);
        if (calibration!=null){
            spinnerRefWidth.setValue(((Number)calibration.get("reference_marker_width")).doubleValue());
            spinnerRefHeight.setValue(((Number)calibration.get("reference_marker_height")).doubleValue());
            spinnerAngleHor.setValue(((Number)calibration.get("angle_horizontal")).doubleValue());
            spinnerAngleVer.setValue(((Number)calibration.get("angle_vertical")).doubleValue());
            refMarker = ((List)calibration.get("reference_marker"));
            if (refMarker.size()==4){
                spinnerLeft.setValue(refMarker.get(0));
                spinnerTop.setValue(refMarker.get(1));
                spinnerRight.setValue(refMarker.get(2));
                spinnerBottom.setValue(refMarker.get(3));
            }
        }  
        updateResults();                
        update();
        
        renderer.setMode(RendererMode.Fit);
        calibrationOverlays = new Overlay[]{ovTop, ovBottom, ovLeft, ovRight};
        for (Overlay ov : calibrationOverlays){
            ov.setMovable(true);                    
        }           
        renderer.addOverlays(calibrationOverlays);                        

        try{
            ovLeft.update(new Point(Math.max(refMarker.get(0), 0), 0));                
            ovTop.update(new Point(0, Math.max(refMarker.get(1), 0)));
            ovRight.update(new Point(Math.max(refMarker.get(2), 0), 0));                
            ovBottom.update(new Point(0,  Math.max(refMarker.get(3),0)));
        } catch (Exception ex){
            SwingUtils.invokeDelayed(() -> {
                Dimension size = renderer.getImageSize();
                ovTop.update(new Point(0, size.height/8));
                ovBottom.update(new Point(0,  7*size.height/8));
                ovLeft.update(new Point(size.width/8, 0));                
                ovRight.update(new Point(7*size.width/8, 0));
            }, 500);                            
        }
        renderer.addListener(new RendererListener(){
            @Override
            public void onMoveFinished(ImageRenderer renderer, Overlay overlay) {
                if (overlay==ovTop){
                    spinnerTop.setValue(ovTop.getPosition().y);
                } else if (overlay==ovBottom){
                    spinnerBottom.setValue(ovBottom.getPosition().y);
                } else if (overlay==ovLeft){
                    spinnerLeft.setValue(ovLeft.getPosition().x);
                } else if (overlay==ovRight){
                    spinnerRight.setValue(ovRight.getPosition().x);
                }
            }
        });
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();
    }
    
    @Override
    protected void onClosed() {
        renderer.removeOverlays(calibrationOverlays);
        calibrationOverlays = null;    
        server.close();
    }
    
    void update(){
        boolean calibrationEnabled = ckCalibrationEnabled.isSelected();
        spinnerLeft.setEnabled(calibrationEnabled);
        spinnerTop.setEnabled(calibrationEnabled);
        spinnerRight.setEnabled(calibrationEnabled);
        spinnerBottom.setEnabled(calibrationEnabled);
    }
    
    void updateResults(){
        try{
            int left = (Integer)spinnerLeft.getValue();
            int right = (Integer)spinnerRight.getValue();
            int top = (Integer)spinnerTop.getValue();
            int bottom = (Integer)spinnerBottom.getValue();
            double width = (Double)spinnerRefWidth.getValue();
            double height = (Double)spinnerRefHeight.getValue();
            double angleHor = (Double)spinnerAngleHor.getValue();
            double angleVer = (Double)spinnerAngleVer.getValue();
            double pixelHor = width * Math.cos(Math.toRadians(angleHor)) /Math.abs(right-left);
            double pixelVer = height * Math.cos(Math.toRadians(angleVer)) /Math.abs(bottom-top);
            textOriginX.setText(String.valueOf((left+right)/2));
            textOriginY.setText(String.valueOf((top+bottom)/2));
            textPixelSizeX.setText(String.valueOf(Convert.roundDouble(pixelHor, 2)));
            textPixelSizeY.setText(String.valueOf(Convert.roundDouble(pixelVer, 2)));
            ovLeft.update(new Point(Math.max(left, 0), 0));                
            ovTop.update(new Point(0, Math.max(top, 0)));
            ovRight.update(new Point(Math.max(right, 0), 0));                
            ovBottom.update(new Point(0,  Math.max(bottom,0)));
            renderer.refresh();
            
        } catch (Exception ex){
            showException(ex);
        }
    }
            
    
    public static void main(String args[]) throws Exception {
        App.init(args);        
        ch.psi.pshell.devices.Options.addCamServer();
        String cameraName = App.getAditionalArgument();

        Renderer renderer = new Renderer();
        JDialog dlgRenderer = SwingUtils.showDialog(null, cameraName, new Dimension(600,400), renderer);
        CameraCalibrationDialog dialog = new CameraCalibrationDialog(null, Setup.getCameraServer(), cameraName, renderer);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dlgRenderer.setVisible(false);
                System.exit(0);
            }
        });            
        SwingUtils.centerComponent(null, dialog);
        dialog.setVisible(true);
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
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        spinnerAngleHor = new javax.swing.JSpinner();
        spinnerAngleVer = new javax.swing.JSpinner();
        spinnerRefWidth = new javax.swing.JSpinner();
        spinnerRefHeight = new javax.swing.JSpinner();
        spinnerTop = new javax.swing.JSpinner();
        spinnerBottom = new javax.swing.JSpinner();
        spinnerLeft = new javax.swing.JSpinner();
        spinnerRight = new javax.swing.JSpinner();
        buttonFetch = new javax.swing.JButton();
        ckCalibrationEnabled = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        checkMirrorX = new javax.swing.JCheckBox();
        checkMirrorY = new javax.swing.JCheckBox();
        comboRotation = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        textOriginX = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        textOriginY = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        textPixelSizeX = new javax.swing.JTextField();
        textPixelSizeY = new javax.swing.JTextField();
        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        buttonApply = new javax.swing.JButton();

        jLabel1.setText("Calibrate the camera moving the line overlays  to the reference marks.");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Calibration"));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Reference marker height (um):");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel5.setText("Horizontal camera angle (deg):");

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel6.setText("Vertical camera angle (deg):");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("Reference marker width (um):");

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("Top (px):");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel8.setText("Botton (px):");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Left (px):");

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel10.setText("Right (px):");

        spinnerAngleHor.setModel(new javax.swing.SpinnerNumberModel(0.0d, -360.0d, 360.0d, 1.0d));
        spinnerAngleHor.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                calibrationSpinnerChanged(evt);
            }
        });

        spinnerAngleVer.setModel(new javax.swing.SpinnerNumberModel(0.0d, -360.0d, 360.0d, 1.0d));
        spinnerAngleVer.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                calibrationSpinnerChanged(evt);
            }
        });

        spinnerRefWidth.setModel(new javax.swing.SpinnerNumberModel(30000.0d, 1.0d, 500000.0d, 1.0d));
        spinnerRefWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                calibrationSpinnerChanged(evt);
            }
        });

        spinnerRefHeight.setModel(new javax.swing.SpinnerNumberModel(30000.0d, 1.0d, 500000.0d, 1.0d));
        spinnerRefHeight.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                calibrationSpinnerChanged(evt);
            }
        });

        spinnerTop.setModel(new javax.swing.SpinnerNumberModel(0, -10000, 10000, 1));
        spinnerTop.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                calibrationSpinnerChanged(evt);
            }
        });

        spinnerBottom.setModel(new javax.swing.SpinnerNumberModel(0, -10000, 10000, 1));
        spinnerBottom.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                calibrationSpinnerChanged(evt);
            }
        });

        spinnerLeft.setModel(new javax.swing.SpinnerNumberModel(0, -10000, 10000, 1));
        spinnerLeft.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                calibrationSpinnerChanged(evt);
            }
        });

        spinnerRight.setModel(new javax.swing.SpinnerNumberModel(0, -10000, 10000, 1));
        spinnerRight.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                calibrationSpinnerChanged(evt);
            }
        });

        buttonFetch.setText("Fetch from Inventory");
        buttonFetch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFetchActionPerformed(evt);
            }
        });

        ckCalibrationEnabled.setText("Enabled");
        ckCalibrationEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckCalibrationEnabledActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerRefHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerRight, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerAngleVer, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerAngleHor, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(ckCalibrationEnabled)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(buttonFetch))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(spinnerTop, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerRefWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel2, jLabel8, jLabel9});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonFetch)
                    .addComponent(ckCalibrationEnabled))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(spinnerRefWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel8)
                    .addComponent(spinnerRefHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel9)
                    .addComponent(spinnerAngleHor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel10)
                    .addComponent(spinnerAngleVer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Transformations"));

        checkMirrorX.setText("Mirror X");

        checkMirrorY.setText("Mirror Y");

        comboRotation.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0", "270", "180", "90" }));

        jLabel7.setText("Rotation: ");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkMirrorX)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(comboRotation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkMirrorY)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(comboRotation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7))
                    .addComponent(checkMirrorX))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkMirrorY)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Results"));

        jLabel11.setText("Origin X (px):");

        textOriginX.setEditable(false);
        textOriginX.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel12.setText("Origin Y (px):");

        textOriginY.setEditable(false);
        textOriginY.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel13.setText("Y pixel size (um/px):");

        jLabel14.setText("X pixel size (um/px):");

        textPixelSizeX.setEditable(false);
        textPixelSizeX.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        textPixelSizeY.setEditable(false);
        textPixelSizeY.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textOriginY, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textOriginX, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textPixelSizeY, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textPixelSizeX, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel11, jLabel12});

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel13, jLabel14});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(textPixelSizeX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13)
                            .addComponent(textPixelSizeY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(textOriginX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12)
                            .addComponent(textOriginY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        buttonOk.setText("Ok");
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonApply.setText("Apply");
        buttonApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonCancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonApply)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonOk)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonApply, buttonCancel, buttonOk});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel)
                    .addComponent(buttonApply))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
     
        buttonApplyActionPerformed(evt);                
        accept();
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();        
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonFetchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFetchActionPerformed
        try{
             List<Double> calib = Inventory.getCalibFromInventory((String)null, cameraName);
             spinnerRefWidth.setValue(calib.get(0)*1000.0);
             spinnerRefHeight.setValue(calib.get(1)*1000.0);
             spinnerAngleHor.setValue(calib.get(2).doubleValue());
             spinnerAngleVer.setValue(calib.get(3).doubleValue());                          
        } catch (Exception ex){
            if (ex instanceof InvocationTargetException invocationTargetException){
                ex = (Exception) invocationTargetException.getCause();
            }
            showException(ex);
        }        
    }//GEN-LAST:event_buttonFetchActionPerformed

    private void calibrationSpinnerChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_calibrationSpinnerChanged
        updateResults();
    }//GEN-LAST:event_calibrationSpinnerChanged

    private void buttonApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonApplyActionPerformed
        try {
            int left = (Integer)spinnerLeft.getValue();         //left.getPosition().x
            int right = (Integer)spinnerRight.getValue();       //right.getPosition().x
            int top = (Integer)spinnerTop.getValue();           //top.getPosition().y
            int bottom = (Integer)spinnerBottom.getValue();     //bottom.getPosition().y
            double width = (Double)spinnerRefWidth.getValue();
            double height = (Double)spinnerRefHeight.getValue();
            double angleHor = (Double)spinnerAngleHor.getValue();
            double angleVer = (Double)spinnerAngleVer.getValue();
            
            int x1 = Math.min(left, right);
            int x2 = Math.max(left, right);
            int y1 = Math.min(top, bottom);
            int y2 = Math.max(top, bottom);
            System.out.println("Updating " + cameraName + " configuration");                                    
            HashMap calibration = null;
            if (ckCalibrationEnabled.isSelected()){
                calibration = new HashMap();
                calibration.put("reference_marker", Arrays.asList(new Integer[]{x1, y1, x2, y2}));
                calibration.put("reference_marker_width", width);
                calibration.put("reference_marker_height", height);
                calibration.put("angle_horizontal", angleHor);
                calibration.put("angle_vertical", angleVer);           
            }

            Map<String, Object> config = server.getConfig(cameraName);
            config.put("camera_calibration", calibration);
            config.put("mirror_x", checkMirrorX.isSelected());
            config.put("mirror_y", checkMirrorY.isSelected());
            config.put("rotate", comboRotation.getSelectedIndex());
            server.setConfig(cameraName, config);                

            System.out.println("New  config: " + config);
            boolean reticle = renderer.getShowReticle();
            if (reticle){
                renderer.setShowReticle(false);
            }
            showMessage("Success", "Updated " + cameraName + " configuration");

            if (reticle){
                renderer.setShowReticle(true);
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonApplyActionPerformed

    private void ckCalibrationEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckCalibrationEnabledActionPerformed
        try{
            update();
        } catch (Exception ex) {
            showException(ex);
        }        
    }//GEN-LAST:event_ckCalibrationEnabledActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonApply;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonFetch;
    private javax.swing.JButton buttonOk;
    private javax.swing.JCheckBox checkMirrorX;
    private javax.swing.JCheckBox checkMirrorY;
    private javax.swing.JCheckBox ckCalibrationEnabled;
    private javax.swing.JComboBox comboRotation;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
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
    private javax.swing.JSpinner spinnerAngleHor;
    private javax.swing.JSpinner spinnerAngleVer;
    private javax.swing.JSpinner spinnerBottom;
    private javax.swing.JSpinner spinnerLeft;
    private javax.swing.JSpinner spinnerRefHeight;
    private javax.swing.JSpinner spinnerRefWidth;
    private javax.swing.JSpinner spinnerRight;
    private javax.swing.JSpinner spinnerTop;
    private javax.swing.JTextField textOriginX;
    private javax.swing.JTextField textOriginY;
    private javax.swing.JTextField textPixelSizeX;
    private javax.swing.JTextField textPixelSizeY;
    // End of variables declaration//GEN-END:variables
}
