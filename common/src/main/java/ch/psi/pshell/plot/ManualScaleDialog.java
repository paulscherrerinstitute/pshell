package ch.psi.pshell.plot;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 *
 */
public class ManualScaleDialog extends JDialog {
    
    // Default serial id
    private static final long serialVersionUID = 1L;
    private final JPanel contentPanel = new JPanel();
    private final JTextField textFieldHighValue;
    private final JTextField textFieldLowValue;
    private ScaleChangeListener scaleChangeListener;
    private int selectedOption;
    
    
    public interface ScaleChangeListener{
        void setScale(double scaleMin, double scaleMax);
    }

    public ManualScaleDialog() {
        setModal(true); // Block until dialog is disposed.
        setBounds(100, 100, 450, 300);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setLayout(new FlowLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        {
            JLabel lblLowValue = new JLabel("Low Value:");
            lblLowValue.setFont(new Font("Lucida Grande", Font.BOLD, 13));
            contentPanel.add(lblLowValue);
        }
        {
            textFieldLowValue = new JTextField();
            contentPanel.add(textFieldLowValue);
            textFieldLowValue.setColumns(10);
        }
        {
            JLabel lblHighValue = new JLabel("High Value:");
            lblHighValue.setFont(new Font("Lucida Grande", Font.BOLD, 13));
            contentPanel.add(lblHighValue);
        }
        {
            textFieldHighValue = new JTextField();
            contentPanel.add(textFieldHighValue);
            textFieldHighValue.setColumns(10);
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.addActionListener((ActionEvent arg0) -> {
                    // set OK
                    selectedOption = JOptionPane.OK_OPTION;
                    dispose();
                });
                {
                    JButton btnApply = new JButton("Apply");
                    btnApply.addActionListener((ActionEvent arg0) -> {
                        // TODO apply values
                        if (scaleChangeListener != null) {
                            scaleChangeListener.setScale(getLow(), getHigh());
                        }
                    });
                    buttonPane.add(btnApply);
                }
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener((ActionEvent arg0) -> {
                    // set Cancel
                    selectedOption = JOptionPane.CANCEL_OPTION;
                    dispose();
                });
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    } // Block until dialog is disposed.
    // set OK
    // TODO apply values
    // set Cancel

    public void showDialog() {
        setVisible(true);
    }

    public void setScaleChangeListener(ScaleChangeListener scaleChangeListener) {
        this.scaleChangeListener = scaleChangeListener;
    }

    public void setLow(double low) {
        textFieldLowValue.setText(String.format("%.4f", low));
    }

    public void setHigh(double high) {
        textFieldHighValue.setText(String.format("%.4f", high));
    }

    public double getLow() {
        double low = Double.NaN;
        try {
            low = Double.parseDouble(textFieldLowValue.getText());
        } catch (NumberFormatException e) {
        }
        return low;
    }

    public double getHigh() {
        double high = Double.NaN;
        try {
            high = Double.parseDouble(textFieldHighValue.getText());
        } catch (NumberFormatException e) {
        }
        return high;
    }

    /**
     * @return the selectedOption
     */
    public int getSelectedOption() {
        return selectedOption;
    }
    
}
