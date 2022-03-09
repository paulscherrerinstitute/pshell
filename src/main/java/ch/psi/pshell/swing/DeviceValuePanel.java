package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.utils.Str;
import javax.swing.JLabel;

/**
 *
 */
public final class DeviceValuePanel extends DevicePanel {

    public DeviceValuePanel() {
        initComponents();
    }

    @Override
    protected void onDeviceValueChanged(Object value, Object former) {
        if (!(device instanceof ReadbackDevice)) {
            setText(value);
        }
    }

    @Override
    protected void onDeviceReadbackChanged(Object value) {
        if (device instanceof ReadbackDevice) {
            setText(value);
        }
    }

    protected void setText(Object value) {
        if (value == null) {
            labelValue.setText(" ");
        } else {
            labelValue.setText(Str.toString(value, 5));
        }
    }

    @Override
    public void setDevice(Device device) {
        super.setDevice(device);
        if (device == null) {
            setText(null);
        }
        labelValue.setToolTipText((device==null) ? null : device.getName());
    }

    public JLabel getLabel() {
        return labelValue;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        labelValue = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        labelValue.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelValue.setText(" ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelValue, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelValue, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel labelValue;
    // End of variables declaration//GEN-END:variables
}
