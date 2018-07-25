package ch.psi.pshell.swing;

import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class StripChartAlarmEditor extends StandardDialog {
    
    public static class StripChartAlarmConfig extends HashMap{
        public StripChartAlarmConfig(){
            this.put("enabled", false);
            this.put("lowLimit", Double.NaN);
            this.put("highLimit", Double.NaN);
            this.put("alarmInvalid", false);
        } 
        
        public StripChartAlarmConfig(Map map){
            this.putAll(map);
        }
        
        public boolean isEnabled(){
            return (Boolean)get("enabled");
        }
        
        public double getLowLimit(){
            return (Double)get("lowLimit");
        }

        public double getHighLimit(){
            return (Double)get("highLimit");
        }
        
        public boolean getAlarmInvalid(){
            return (Boolean)get("alarmInvalid");
        }
        
        public boolean isAlarm(Number value){
            double lowLimit = getLowLimit();
            double highLimit = getHighLimit();
            
            if (!isEnabled()){
                return false;
            }
            if ((value == null) || (Double.isNaN(value.doubleValue()))){
                return getAlarmInvalid();
            }
            if (!Double.isNaN(lowLimit) && (value.doubleValue() < lowLimit)){
                return true;
            }
            if (!Double.isNaN(highLimit) && (value.doubleValue() > highLimit)){
                return true;
            }       
            return false;
        }
    }
    
    public StripChartAlarmConfig config;

    /**
     * Creates new form StripChartAlarmEditor
     */
    public StripChartAlarmEditor(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setAlwaysOnTop(true);
    }
    
    @Override
    protected void onOpened() {
        if (config!=null){
            enabled.setSelected(config.isEnabled());
            alarmInvalid.setSelected(config.getAlarmInvalid());
            textLow.setText(Double.isNaN(config.getLowLimit()) ? "" : String.valueOf(config.getLowLimit()));
            textHigh.setText(Double.isNaN(config.getHighLimit()) ? "" : String.valueOf(config.getHighLimit()));
        }
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Alarm Editor");
        setCancelledOnEscape(false);

        jLabel1.setText("Low limit:");

        jLabel2.setText("High limit:");

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
                    .addComponent(textHigh)
                    .addComponent(textLow)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(enabled)
                            .addComponent(alarmInvalid))
                        .addGap(0, 0, Short.MAX_VALUE)))
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textLow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textHigh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
            config = new StripChartAlarmConfig();
            config.put("enabled", enabled.isSelected());
            config.put("alarmInvalid", alarmInvalid.isSelected());
            config.put("lowLimit", textLow.getText().trim().isEmpty() ? Double.NaN : Double.valueOf(textLow.getText()));            
            config.put("highLimit", textHigh.getText().trim().isEmpty() ? Double.NaN : Double.valueOf(textHigh.getText()));
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
                StripChartAlarmEditor dialog = new StripChartAlarmEditor(new javax.swing.JFrame(), true);
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
    private javax.swing.JCheckBox enabled;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    public javax.swing.JTextField textHigh;
    public javax.swing.JTextField textLow;
    // End of variables declaration//GEN-END:variables
}
