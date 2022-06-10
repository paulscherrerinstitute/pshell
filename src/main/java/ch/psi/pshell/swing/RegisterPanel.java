package ch.psi.pshell.swing;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import ch.psi.pshell.device.Register.RegisterString;
import ch.psi.pshell.device.ProcessVariableBase;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
import ch.psi.utils.Str;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.JTextField;

/**
 *
 */
public final class RegisterPanel extends DevicePanel {

    public RegisterPanel() {
        initComponents();
    }

    public JTextField getTextField(){
        return txtRegisterReadout;
    }
    
    public String getText(){
        return getTextField().getText();
    }
    
    public void setText(String text){
        getTextField().setText(text);
    }
    
    public Object getValue(){
        return getDevice().take();
    }
    
    @Override
    public ReadonlyRegisterBase getDevice() {
        return (ReadonlyRegisterBase) super.getDevice();
    }
    
    public boolean isReadonlyRegister(){
        return  !(getDevice() instanceof RegisterBase);
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
        update();
    }
    
    @Override
    public void setDevice(Device device) {        
        super.setDevice(device);
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
        txtRegisterReadout.setEditable(enabled);
        checkBackColor();
    }

    private boolean isEditing() {
        return editing;
    }

    protected void checkBackColor() {
        if (isReadOnly() || isReadonlyRegister() || !isEnabled()) {
            txtRegisterReadout.setBackground(TEXT_READONLY_BACKGROUND_COLOR);
        } else {
            if (isEditing()) {
                txtRegisterReadout.setBackground(TEXT_EDIT_BACKGROUND_COLOR);
            } else {
                txtRegisterReadout.setBackground(TEXT_DISPLAY_BACKGROUND_COLOR);
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
                txtRegisterReadout.setText("");
            } else if (isEditing() == false) {
                Object val = getValue();                
                if (val instanceof String){
                    txtRegisterReadout.setText((String)val);
                } else {
                    if (! (val instanceof Number)){
                        throw new Exception();
                    }
                    int decimals = Math.min(getDecimals(), getDevice().getPrecision());
                    decimals = Math.max(decimals, 0);
                    if ((val instanceof Float) || (val instanceof Double)){
                        Double position = Convert.roundDouble(((Double)val).doubleValue(), decimals);
                        txtRegisterReadout.setText((position == null) ? "" : String.format("%1." + decimals + "f", position));
                    } else {
                        txtRegisterReadout.setText(Str.toString(val));
                    }
                }
            }
        } catch (Exception ex) {
            txtRegisterReadout.setText("");
        }
    }

    public void write(Object value) throws IOException {
        write(value, true);
    }
    
    public void write(Object value, boolean showException) throws IOException {
        if (!isReadonlyRegister()){
            ((RegisterBase)getDevice()).writeAsync(value).handle((ok, ex) -> {
                if (showException){
                    if ((ex != null) && ((ex instanceof IOException) || (ex instanceof IllegalArgumentException))) {
                        showException((Exception) ex);
                    }
                }
                return ok;
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtRegisterReadout = new javax.swing.JTextField();

        txtRegisterReadout.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRegisterReadout.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtRegisterReadoutFocusLost(evt);
            }
        });
        txtRegisterReadout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                txtRegisterReadoutMouseEntered(evt);
            }
        });
        txtRegisterReadout.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtRegisterReadoutKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtRegisterReadoutKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(txtRegisterReadout, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(txtRegisterReadout)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtRegisterReadoutMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtRegisterReadoutMouseEntered
        if (getDevice() == null){
            txtRegisterReadout.setToolTipText(null);
        } else if (getDevice() instanceof ProcessVariableBase){
            ProcessVariableBase pv = (ProcessVariableBase)getDevice();
            txtRegisterReadout.setToolTipText(getDevice().getName() + " range: " + pv.getMinValue() + " to " + pv.getMaxValue() + " " + pv.getUnit());
        } else {
            txtRegisterReadout.setToolTipText(getDeviceTooltip());
        }
    }//GEN-LAST:event_txtRegisterReadoutMouseEntered

    private void txtRegisterReadoutFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtRegisterReadoutFocusLost
        // 
    }//GEN-LAST:event_txtRegisterReadoutFocusLost

    private void txtRegisterReadoutKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRegisterReadoutKeyPressed
        if (isReadOnly() || isReadonlyRegister()) {
            setEditing(false);
            evt.consume();
        }
    }//GEN-LAST:event_txtRegisterReadoutKeyPressed

    private void txtRegisterReadoutKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRegisterReadoutKeyTyped
        try {
            char c = evt.getKeyChar();

            if (isReadOnly() || isReadonlyRegister()) {
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
                    String text = txtRegisterReadout.getText();
                    if (getDevice() instanceof RegisterString){
                        write(text);
                    } else {
                        write(Double.valueOf(text));
                    }
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
    }//GEN-LAST:event_txtRegisterReadoutKeyTyped

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField txtRegisterReadout;
    // End of variables declaration//GEN-END:variables
}