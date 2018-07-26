package ch.psi.pshell.swing;

import ch.psi.pshell.epics.Epics;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class StripChartAlarmEditor extends StandardDialog {

    final String channel;

    public static class StripChartAlarmConfig extends HashMap {

        final String channel;
        Double channelLowLimit, channelHighLimit;

        public StripChartAlarmConfig(String channel) {
            this.put("enabled", false);
            this.put("lowLimit", Double.NaN);
            this.put("highLimit", Double.NaN);
            this.put("alarmInvalid", false);
            this.channel = channel;
            updateChannelLimits();
        }

        public StripChartAlarmConfig(Map map, String channel) {
            this.putAll(map);
            this.channel = channel;
            updateChannelLimits();
        }

        final void updateChannelLimits() {
            if (String.valueOf(Double.NaN).equals(get("lowLimit"))) {
                put("lowLimit", Double.NaN);
            }
            if (String.valueOf(Double.NaN).equals(get("highLimit"))) {
                put("highLimit", Double.NaN);
            }
            if ((get("lowLimit") != null) && Double.isNaN((Double) get("lowLimit")) && (channel != null)) {
                try {
                    channelLowLimit = getChannelLowLimit(channel);
                } catch (Exception ex) {
                    Logger.getLogger(StripChartAlarmEditor.class.getName()).log(Level.WARNING, null, ex);
                }

            }
            if ((get("highLimit") != null) && Double.isNaN((Double) get("highLimit")) && (channel != null)) {
                try {
                    channelHighLimit = getChannelHighLimit(channel);
                } catch (Exception ex) {
                    Logger.getLogger(StripChartAlarmEditor.class.getName()).log(Level.WARNING, null, ex);
                }

            }
        }

        public Boolean isEnabled() {
            return (Boolean) get("enabled");
        }

        public Double getLowLimit() {
            Double lowLimit = (Double) get("lowLimit");
            if ((lowLimit != null) && (Double.isNaN(lowLimit))) {
                return (channelLowLimit != null) ? channelLowLimit : Double.NaN;
            }
            return lowLimit;
        }

        public Double getHighLimit() {
            Double highLimit = (Double) get("highLimit");
            if ((highLimit != null) && (Double.isNaN(highLimit))) {
                return (channelHighLimit != null) ? channelHighLimit : Double.NaN;
            }
            return highLimit;
        }

        public Boolean getAlarmInvalid() {
            return (Boolean) get("alarmInvalid");
        }

        public boolean isAlarm(Number value) {
            Double lowLimit = getLowLimit();
            Double highLimit = getHighLimit();

            if (!isEnabled()) {
                return false;
            }
            if ((value == null) || (Double.isNaN(value.doubleValue()))) {
                return getAlarmInvalid();
            }
            if ((lowLimit != null) && !Double.isNaN(lowLimit) && (value.doubleValue() < lowLimit)) {
                return true;
            }
            if ((highLimit != null) && !Double.isNaN(highLimit) && (value.doubleValue() > highLimit)) {
                return true;
            }
            return false;
        }
    }

    public StripChartAlarmConfig config;

    /**
     * Creates new form StripChartAlarmEditor
     */
    public StripChartAlarmEditor(java.awt.Frame parent, boolean modal, StripChartAlarmConfig config, String channel) {
        super(parent, modal);
        initComponents();
        this.setAlwaysOnTop(true);
        this.config = (config == null) ? new StripChartAlarmConfig(channel) : config;
        this.channel = channel;
        if (config != null) {
            config.updateChannelLimits();
            Double lowLimit = (Double) config.get("lowLimit");
            Double highLimit = (Double) config.get("highLimit");

            enabled.setSelected(config.isEnabled());
            alarmInvalid.setSelected(config.getAlarmInvalid());
            if (lowLimit == null) {
                comboLowLimit.setSelectedIndex(0);
            } else if (Double.isNaN(lowLimit)) {
                comboLowLimit.setSelectedIndex(2);
            } else {
                comboLowLimit.setSelectedIndex(1);
            }
            try {
                lowLimit = config.getLowLimit();
                textLow.setText(((lowLimit == null) || (Double.isNaN(lowLimit))) ? "" : String.valueOf(lowLimit));
            } catch (Exception ex) {
                textLow.setText("");
            }

            if (highLimit == null) {
                comboHighLimit.setSelectedIndex(0);
            } else if (Double.isNaN(highLimit)) {
                comboHighLimit.setSelectedIndex(2);
            } else {
                comboHighLimit.setSelectedIndex(1);
            }
            try {
                highLimit = config.getHighLimit();
                textHigh.setText(((highLimit == null) || (Double.isNaN(highLimit))) ? "" : String.valueOf(highLimit));
            } catch (Exception ex) {
                textHigh.setText("");
            }
            comboLowLimitActionPerformed(null);
            comboHighLimitActionPerformed(null);
        }        
    }

    static double getChannelLowLimit(String channel) throws Exception {
        if (channel == null) {
            throw new Exception("Not a channel type");
        }
        return Epics.get(channel + ".LOLO", Double.class);
    }

    static double getChannelHighLimit(String channel) throws Exception {
        if (channel == null) {
            throw new Exception("Not a channel type");
        }
        return Epics.get(channel + ".HIHI", Double.class);
    }

    double getChannelLowLimit() throws Exception {
        return getChannelLowLimit(channel);
    }

    double getChannelHighLimit() throws Exception {
        return getChannelHighLimit(channel);
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
        jLabel2 = new javax.swing.JLabel();
        textLow = new javax.swing.JTextField();
        textHigh = new javax.swing.JTextField();
        buttonOk = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        enabled = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        alarmInvalid = new javax.swing.JCheckBox();
        buttonRemove = new javax.swing.JButton();
        comboLowLimit = new javax.swing.JComboBox<>();
        comboHighLimit = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Alarm Editor");
        setCancelledOnEscape(false);

        jLabel1.setText("Low limit:");

        jLabel2.setText("High limit:");

        textLow.setEditable(false);

        textHigh.setEditable(false);

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

        enabled.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jLabel3.setText("Enabled:");

        jLabel4.setText("Alarm on Invalid:");

        alarmInvalid.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        buttonRemove.setText("Remove");
        buttonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveActionPerformed(evt);
            }
        });

        comboLowLimit.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Disabled", "User", "Channel" }));
        comboLowLimit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboLowLimitActionPerformed(evt);
            }
        });

        comboHighLimit.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Disabled", "User", "Channel" }));
        comboHighLimit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboHighLimitActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(alarmInvalid)
                    .addComponent(comboHighLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboLowLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(enabled))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(textHigh, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                    .addComponent(textLow, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(buttonOk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRemove)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCancel)
                .addGap(18, 18, 18))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel4});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOk, buttonRemove});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(enabled)
                    .addComponent(jLabel3))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(textLow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(comboLowLimit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textHigh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboHighLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(alarmInvalid)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOk)
                    .addComponent(buttonCancel)
                    .addComponent(buttonRemove))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        try {
            config = new StripChartAlarmConfig(channel);
            config.put("enabled", enabled.isSelected());
            config.put("alarmInvalid", alarmInvalid.isSelected());
            Double lowLimit = null, highLimit = null;
            switch (comboLowLimit.getSelectedIndex()) {
                case 0:
                    lowLimit = null;
                    break;
                case 1:
                    lowLimit = Double.valueOf(textLow.getText());
                    break;
                case 2:
                    lowLimit = Double.NaN;
                    break;
            }
            switch (comboHighLimit.getSelectedIndex()) {
                case 0:
                    highLimit = null;
                    break;
                case 1:
                    highLimit = Double.valueOf(textHigh.getText());
                    break;
                case 2:
                    highLimit = Double.NaN;
                    break;
            }
            config.put("lowLimit", lowLimit);
            config.put("highLimit", highLimit);
            accept();
        } catch (Exception ex) {
            SwingUtils.showException(this, ex);
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        accept();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveActionPerformed
        cancel();
    }//GEN-LAST:event_buttonRemoveActionPerformed

    private void comboLowLimitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboLowLimitActionPerformed
        textLow.setEditable(comboLowLimit.getSelectedIndex() == 1);
        if (comboLowLimit.getSelectedIndex() == 0){
            textLow.setText("");
        } else if ((comboLowLimit.getSelectedIndex() == 2) && (channel != null)) {
            try {
                textLow.setText(String.valueOf(getChannelLowLimit(channel)));
            } catch (Exception ex) {
                Logger.getLogger(StripChartAlarmEditor.class.getName()).log(Level.WARNING, null, ex);
                textLow.setText("");
            }
        }
    }//GEN-LAST:event_comboLowLimitActionPerformed

    private void comboHighLimitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboHighLimitActionPerformed
        textHigh.setEditable(comboHighLimit.getSelectedIndex() == 1);
        if (comboHighLimit.getSelectedIndex() == 0){
            textHigh.setText("");
        } else if ((comboHighLimit.getSelectedIndex() == 2) && (channel != null)) {
            try {
                textHigh.setText(String.valueOf(getChannelHighLimit(channel)));
            } catch (Exception ex) {
                Logger.getLogger(StripChartAlarmEditor.class.getName()).log(Level.WARNING, null, ex);
                textHigh.setText("");
            }
        }
    }//GEN-LAST:event_comboHighLimitActionPerformed

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
            java.util.logging.Logger.getLogger(StripChartAlarmEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(StripChartAlarmEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(StripChartAlarmEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(StripChartAlarmEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                StripChartAlarmEditor dialog = new StripChartAlarmEditor(new javax.swing.JFrame(), true, null, null);
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
    private javax.swing.JCheckBox alarmInvalid;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonRemove;
    private javax.swing.JComboBox<String> comboHighLimit;
    private javax.swing.JComboBox<String> comboLowLimit;
    private javax.swing.JCheckBox enabled;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    public javax.swing.JTextField textHigh;
    public javax.swing.JTextField textLow;
    // End of variables declaration//GEN-END:variables
}
