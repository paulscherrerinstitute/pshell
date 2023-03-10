package ch.psi.pshell.swing;

import ch.psi.pshell.device.Motor;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.JTextField;

/**
 *
 */
public final class MotorReadoutPanel extends DevicePanel {

    public MotorReadoutPanel() {
        initComponents();
    }
    
    public JTextField getTextField(){
        return txtMotorReadout;
    }    
    
    public String getText(){
        return getTextField().getText();
    }    

    @Override
    public Motor getDevice() {
        return (Motor) super.getDevice();
    }

    @Override
    protected void onShow() {
        super.onShow();
        setEditing(false);
    }

    @Override
    protected void onDeviceStateChanged(State state, State former) {
        update();
    }

    @Override
    protected void onDeviceValueChanged(Object value, Object former) {
    }

    @Override
    protected void onDeviceReadbackChanged(Object value) {
        update();
    }

    private boolean editing;

    private void setEditing(boolean value) {
        editing = value;
        if (!editing) {
            update();
        }
        checkBackColor();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        txtMotorReadout.setEditable(enabled);
        checkBackColor();
    }

    private boolean isEditing() {
        return editing;
    }

    @Override
    protected void checkBackColor() {
        if ((isReadOnly()) || (!isEnabled())) {
            txtMotorReadout.setBackground(getTextDisplayBackgroundColor());
        } else {
            if (isEditing()) {
                txtMotorReadout.setBackground(getTextEditBackgroundColor());
            } else {
                txtMotorReadout.setBackground(getTextReadonlyBackgroundColor());
            }
        }
    }

    int decimals = 8;

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public int getDecimals() {
        return decimals;
    }

    protected void update() {
        try {
            if (getDevice() == null) {
                txtMotorReadout.setText("");
            } else if (isEditing() == false) {
                Double position = getDevice().getReadback().take();
                int decimals = getDisplayDecimals(getDecimals());                
                position = Convert.roundDouble(position, decimals);
                txtMotorReadout.setText((position == null) ? "" : String.format("%1." + decimals + "f", position));
            }
        } catch (Exception ex) {
            txtMotorReadout.setText("");
        }
    }

    protected void move(double value) throws IOException {
        getDevice().writeAsync(value).handle((ok, ex) -> {
            if ((ex != null) && ((ex instanceof IOException) || (ex instanceof IllegalArgumentException))) {
                showException((Exception) ex);
            }
            return ok;
        });
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtMotorReadout = new javax.swing.JTextField();

        txtMotorReadout.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtMotorReadout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                txtMotorReadoutMouseEntered(evt);
            }
        });
        txtMotorReadout.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtMotorReadoutFocusLost(evt);
            }
        });
        txtMotorReadout.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtMotorReadoutKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtMotorReadoutKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtMotorReadout, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtMotorReadout)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtMotorReadoutMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtMotorReadoutMouseEntered
        if (getDevice() == null) {
            txtMotorReadout.setToolTipText(null);
        } else {
            txtMotorReadout.setToolTipText(getDevice().getName()+ " range: " + getDevice().getMinValue() + " to " + getDevice().getMaxValue() + " " + getDevice().getUnit());
        }
    }//GEN-LAST:event_txtMotorReadoutMouseEntered

    private void txtMotorReadoutFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtMotorReadoutFocusLost
        // 
    }//GEN-LAST:event_txtMotorReadoutFocusLost

    private void txtMotorReadoutKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtMotorReadoutKeyPressed
        if (isReadOnly()) {
            setEditing(false);
            evt.consume();
        }
    }//GEN-LAST:event_txtMotorReadoutKeyPressed

    private void txtMotorReadoutKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtMotorReadoutKeyTyped
        try {
            char c = evt.getKeyChar();

            if (isReadOnly()) {
                setEditing(false);
                evt.consume();
            } else if (!isEnabled()) {
                setEditing(false);
            } else if ((Character.isDigit(c)) || (c == KeyEvent.VK_MINUS) || (c == KeyEvent.VK_PERIOD) || (c == KeyEvent.VK_COMMA) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
                setEditing(true);
            } else if ((c == KeyEvent.VK_ESCAPE) || (c == KeyEvent.VK_TAB)) {
                setEditing(false);
            } else if (c == KeyEvent.VK_ENTER) {
                try {
                    Double destination = Double.valueOf(txtMotorReadout.getText());
                    move(destination);
                } catch (Exception ex) {
                    throw ex;
                } finally {
                    setEditing(false);
                }
            } else {
                evt.consume();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_txtMotorReadoutKeyTyped

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField txtMotorReadout;
    // End of variables declaration//GEN-END:variables
}
