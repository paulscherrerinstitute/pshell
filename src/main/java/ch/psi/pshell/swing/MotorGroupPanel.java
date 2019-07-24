package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Motor;
import ch.psi.pshell.device.MotorGroup;
import ch.psi.utils.State;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

/**
 */
public class MotorGroupPanel extends DevicePanel {

    javax.swing.ImageIcon iconEmpty;
    javax.swing.ImageIcon iconSet;

    public MotorGroupPanel() {
        initComponents();
    }

    @Override
    public MotorGroup getDevice() {
        return (MotorGroup) super.getDevice();
    }
    
    boolean showTweak = true;
    public boolean getShowTweak() {
        return showTweak;
    }

    public void setShowTweak(boolean value) {
        showTweak = value;
    }      
    
    void updateStep(){
        for (MotorPanel motorPanel : motorPanels){
            motorPanel.setStepSize((Double)spinnerStep.getValue());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelMotors = new javax.swing.JPanel();
        statePanel = new ch.psi.pshell.swing.DeviceStatePanel();
        buttonStop = new javax.swing.JButton();
        panelTweak = new javax.swing.JPanel();
        spinnerStep = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();

        setName("Form"); // NOI18N

        panelMotors.setBorder(javax.swing.BorderFactory.createTitledBorder("Motors"));
        panelMotors.setName("panelMotors"); // NOI18N

        javax.swing.GroupLayout panelMotorsLayout = new javax.swing.GroupLayout(panelMotors);
        panelMotors.setLayout(panelMotorsLayout);
        panelMotorsLayout.setHorizontalGroup(
            panelMotorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 190, Short.MAX_VALUE)
        );
        panelMotorsLayout.setVerticalGroup(
            panelMotorsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        statePanel.setName("statePanel"); // NOI18N

        buttonStop.setText("Stop");
        buttonStop.setName("buttonStop"); // NOI18N
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        panelTweak.setBorder(javax.swing.BorderFactory.createTitledBorder("Tweak"));
        panelTweak.setName("panelTweak"); // NOI18N

        spinnerStep.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, 10000.0d, 1.0d));
        spinnerStep.setName("spinnerStep"); // NOI18N
        spinnerStep.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerStepStateChanged(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel1.setText("Step:");
        jLabel1.setName("jLabel1"); // NOI18N

        javax.swing.GroupLayout panelTweakLayout = new javax.swing.GroupLayout(panelTweak);
        panelTweak.setLayout(panelTweakLayout);
        panelTweakLayout.setHorizontalGroup(
            panelTweakLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTweakLayout.createSequentialGroup()
                .addContainerGap(47, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panelTweakLayout.setVerticalGroup(
            panelTweakLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTweakLayout.createSequentialGroup()
                .addGroup(panelTweakLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(spinnerStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelMotors, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(statePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonStop)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(panelTweak, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelMotors, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(panelTweak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(statePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonStop)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        try {
            getDevice().stop();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStopActionPerformed

    private void spinnerStepStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerStepStateChanged
        updateStep();
    }//GEN-LAST:event_spinnerStepStateChanged

    JLabel[] motorNames = new JLabel[0];
    MotorPanel[] motorPanels = new MotorPanel[0];

    @Override
    public void setDevice(Device device) {
        //Positions Panel        
        panelMotors.removeAll();
        motorNames = new JLabel[0];
        motorPanels = new MotorPanel[0];

        statePanel.setDevice(device);
        panelTweak.setVisible(showTweak);

        if (device != null) {
            try {
                Motor[] motors = ((MotorGroup) device).getMotors();
                motorNames = new JLabel[motors.length];
                motorPanels = new MotorPanel[motors.length];

                if (motors.length > 0) {
                    //FlowLayout top = new FlowLayout(FlowLayout.LEFT);
                    //top.setVgap(0);
                    //top.setHgap(0);
                    BorderLayout top = new BorderLayout();
                    panelMotors.setLayout(top);

                    JPanel pn_cmd = new JPanel();

                    panelMotors.add(pn_cmd);

                    SpringLayout cmd_lm = new SpringLayout();
                    pn_cmd.setLayout(cmd_lm);

                    for (int i = 0; i < motors.length; i++) {
                        motorNames[i] = new JLabel(motors[i].getName() + ":");
                        motorNames[i].setHorizontalAlignment(SwingConstants.TRAILING);
                        pn_cmd.add(motorNames[i]);

                        motorPanels[i] = new MotorPanel();
                        motorPanels[i].setShowAdvanced(false);
                        motorPanels[i].setShowJog(false);
                        motorPanels[i].setShowStatus(false);
                        motorPanels[i].setShowTweak(showTweak);
                        motorPanels[i].setDevice(motors[i]);
                        motorPanels[i].setBorder(null);
                        pn_cmd.add(motorPanels[i]);
                    }

                    SwingUtils.makeGridSpringLayout(pn_cmd, motors.length, 2, 4, 0, 4, 2);
                    for (int i = 0; i < motors.length; i++) {
                        motorPanels[i].setPreferredSize(new Dimension(Math.max(200, motorPanels[i].getPreferredSize().width), motorPanels[i].getPreferredSize().height));
                    }
                }
            } catch (Exception ex) {
                showException(ex);
            }

            if (isVisible()) {
                paintAll(getGraphics());
            }
        }
        super.setDevice(device);
    }

    void clear() {
    }

    void update() {
        try {
            MotorGroup dev = getDevice();
            if (dev != null) {
                //boolean canMove = isEnabled() && device.isReady(); //Assuming motors can change destination on the fly
                boolean canMove = isEnabled();
                for (DevicePanel motorPanel : motorPanels) {
                    motorPanel.setEnabled(canMove);
                }
                return;
            }
        } catch (Exception ex) {
        }
        clear();
    }

    @Override
    protected void onDeviceStateChanged(State state, State former) {
        update();
    }

    @Override
    protected void onDeviceValueChanged(Object value, Object former) {
        update();
    }

    @Override
    protected void onDeviceReadbackChanged(Object value) {
        update();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonStop;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel panelMotors;
    private javax.swing.JPanel panelTweak;
    private javax.swing.JSpinner spinnerStep;
    private ch.psi.pshell.swing.DeviceStatePanel statePanel;
    // End of variables declaration//GEN-END:variables

}
