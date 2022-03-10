package ch.psi.pshell.swing;

import ch.psi.utils.Convert;
import ch.psi.utils.Observable;
import ch.psi.utils.swing.MonitoredPanel;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextField;

/**
 *
 */
public class ValueSelection extends MonitoredPanel implements Observable<ValueSelection.ValueSelectionListener> {

    public ValueSelection() {
        initComponents();
        showButtons = true;
        showLimitButtons = true;
        showText = true;
        acceptEmpty = false;
        setMinValue(1);
        setMaxValue(1);
        setUnit("");
    }

    boolean showButtons;

    public boolean getShowButtons() {
        return showButtons;
    }

    public void setShowButtons(boolean value) {
        showButtons = value;
        panelRight.setVisible(value);
        panelLeft.setVisible(value);
    }

    boolean showLimitButtons;

    public boolean getShowLimitButtons() {
        return showLimitButtons;
    }

    public void setShowLimitButtons(boolean value) {
        showLimitButtons = value;
        buttonStart.setVisible(value);
        buttonEnd.setVisible(value);
    }

    boolean showText;

    public boolean getShowText() {
        return showText;
    }

    public void setShowText(boolean value) {
        showText = value;
        text.setVisible(value);
    }
    
    public JTextField getTextField(){
        return text;
    }
    
    public String getText(){
        return getTextField().getText();
    }
        
    boolean acceptEmpty;

    public boolean getAcceptEmpty() {
        return acceptEmpty;
    }

    public void setAcceptEmpty(boolean value) {
        acceptEmpty = value;
    }

    /**
     * The listener interface for receiving value selection events.
     */
    public interface ValueSelectionListener {

        void onValueChanged(ValueSelection origin, double value, boolean editing);
    }

    final List<ValueSelectionListener> listeners = new ArrayList<>();

    @Override
    public final void addListener(ValueSelectionListener listener) {
        if ((listener != null) && (!listeners.contains(listener))) {
            listeners.add(listener);
        }
    }

    @Override
    public final void removeListener(ValueSelectionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public final List<ValueSelectionListener> getListeners() {
        synchronized (listeners) {
            ArrayList<ValueSelectionListener> ret = new ArrayList<>();
            ret.addAll(listeners);
            return ret;
        }
    }

    @Override
    protected void onShow() {
        super.onShow();
        setEditing(false);
    }

    private boolean editing;

    private void setEditing(boolean value) {
        editing = value;
        updateText();
        checkBackColor();
    }

    public boolean isEditing() {
        return editing;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        text.setEditable(enabled);
        checkBackColor();
    }

    protected void checkBackColor() {
        if ((!isEnabled())) {
            text.setBackground(DevicePanel.TEXT_READONLY_BACKGROUND_COLOR);
        } else {
            if (isEditing()) {
                text.setBackground(DevicePanel.TEXT_EDIT_BACKGROUND_COLOR);
            } else {
                text.setBackground(DevicePanel.TEXT_DISPLAY_BACKGROUND_COLOR);
            }
        }
    }

    double step = 1;

    public double getStep() {
        return step;
    }

    public void setStep(double step) {
        this.step = step;
    }

    double minValue = 1;

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;

        buttonStart.setToolTipText("Minimum = " + minValue);
    }

    double maxValue = 1;

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        buttonEnd.setToolTipText("Maximum = " + maxValue);
    }

    String unit = "";

    public String getUnit() {
        return unit;
    }

    public void setUnit(String value) {
        this.unit = value;
    }

    int decimals = 1;

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    boolean isValidValue(double value) {
        double min = getMinValue();
        if (!Double.isNaN(min)) {
            if (value < min) {
                return false;
            }
        }
        double max = getMaxValue();
        if (!Double.isNaN(max)) {
            if (value > max) {
                return false;
            }
        }
        return true;
    }

    public class IllegalValueException extends IllegalArgumentException {

        IllegalValueException(double value) {
            super(value + " is out of range [" + getMinValue() + "," + getMaxValue() + "]");
        }
    }

    void assertValidValue(double value) throws IllegalArgumentException {
        if (!isValidValue(value)) {
            throw new IllegalValueException(value);
        }
    }

    double value = Double.NaN;

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        setValue(value, false);
    }

    protected void setValue(double value, boolean edition) {
        assertValidValue(value);
        if (value != this.value) {
            this.value = value;
            updateText();
            for (ValueSelectionListener listener : getListeners()) {
                listener.onValueChanged(this, value, isEditing() || edition);
            }
        }
    }

    protected void updateText() {
        if (Double.isNaN(this.value)) {
            text.setText("");
        } else {
            double value = Convert.roundDouble(this.value, getDecimals());
            String val;
            if (decimals == 0) {
                val = String.valueOf((int) value);
            } else {
                //val = String.valueOf(value);
                val = String.format("%1." + decimals + "f", value);
            }
            if (!isEditing()) {
                text.setText(val);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        text = new javax.swing.JTextField();
        panelLeft = new javax.swing.JPanel();
        buttonBack = new javax.swing.JButton();
        buttonStart = new javax.swing.JButton();
        panelRight = new javax.swing.JPanel();
        buttonNext = new javax.swing.JButton();
        buttonEnd = new javax.swing.JButton();

        text.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        text.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                textFocusLost(evt);
            }
        });
        text.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                textMouseEntered(evt);
            }
        });
        text.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                textKeyTyped(evt);
            }
        });

        buttonBack.setText("<");
        buttonBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBackActionPerformed(evt);
            }
        });

        buttonStart.setText("|<");
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelLeftLayout = new javax.swing.GroupLayout(panelLeft);
        panelLeft.setLayout(panelLeftLayout);
        panelLeftLayout.setHorizontalGroup(
            panelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLeftLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(buttonStart)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonBack)
                .addContainerGap())
        );

        panelLeftLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonBack, buttonStart});

        panelLeftLayout.setVerticalGroup(
            panelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLeftLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(panelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonStart)
                    .addComponent(buttonBack))
                .addGap(0, 0, 0))
        );

        panelLeftLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonBack, buttonStart});

        buttonNext.setText(">");
        buttonNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNextActionPerformed(evt);
            }
        });

        buttonEnd.setText(">|");
        buttonEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonEndActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelRightLayout = new javax.swing.GroupLayout(panelRight);
        panelRight.setLayout(panelRightLayout);
        panelRightLayout.setHorizontalGroup(
            panelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRightLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonNext)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonEnd)
                .addGap(0, 0, 0))
        );

        panelRightLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonEnd, buttonNext});

        panelRightLayout.setVerticalGroup(
            panelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelRightLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(panelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonNext)
                    .addComponent(buttonEnd))
                .addGap(0, 0, 0))
        );

        panelRightLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonEnd, buttonNext});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(panelLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(text, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(panelRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {panelLeft, panelRight});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(panelLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(panelRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {panelLeft, panelRight});

    }// </editor-fold>//GEN-END:initComponents

    private void buttonBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBackActionPerformed
        try {
            setValue(Math.max(getValue() - getStep(), getMinValue()), true);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonBackActionPerformed

    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartActionPerformed
        try {
            setValue(getMinValue(), true);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonStartActionPerformed

    private void buttonNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNextActionPerformed
        try {
            setValue(Math.min(getValue() + getStep(), getMaxValue()), true);
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonNextActionPerformed

    private void buttonEndActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonEndActionPerformed
        try {
            setValue(getMaxValue(), true);
        } catch (Exception ex) {
            showException(ex);
        }

    }//GEN-LAST:event_buttonEndActionPerformed

    private void textKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textKeyTyped
        try {
            char c = evt.getKeyChar();
            if (!isEnabled()) {
                setEditing(false);
            } else if ((Character.isDigit(c)) || (c == KeyEvent.VK_MINUS) || (c == KeyEvent.VK_PERIOD) || (c == KeyEvent.VK_COMMA) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
                setEditing(true);
            } else if ((c == KeyEvent.VK_ESCAPE) || (c == KeyEvent.VK_TAB)) {
                setEditing(false);
            } else if (c == KeyEvent.VK_ENTER) {
                try {
                    Double val;
                    if (text.getText().isEmpty() && getAcceptEmpty()) {
                        val = Double.NaN;
                    } else {
                        val = Double.valueOf(text.getText());
                    }
                    setValue(val);
                } catch (Exception ex) {
                    setValue(value);
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

    }//GEN-LAST:event_textKeyTyped

    private void textFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_textFocusLost
        setEditing(false);
    }//GEN-LAST:event_textFocusLost

    private void textMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textMouseEntered
        text.setToolTipText("Range: " + minValue + " to " + maxValue + " " + unit);
    }//GEN-LAST:event_textMouseEntered

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonBack;
    private javax.swing.JButton buttonEnd;
    private javax.swing.JButton buttonNext;
    private javax.swing.JButton buttonStart;
    private javax.swing.JPanel panelLeft;
    private javax.swing.JPanel panelRight;
    private javax.swing.JTextField text;
    // End of variables declaration//GEN-END:variables
}
