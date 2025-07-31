package ch.psi.pshell.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.LayoutManager;
import javax.swing.JButton;
import javax.swing.JSpinner;

public class HorizontalSpinner extends JSpinner {
    @Override
    public void setLayout(LayoutManager mgr) {
        super.setLayout(new HorizontalSpinnerLayout(this));
    }

    public static class HorizontalSpinnerLayout extends BorderLayout {

        final JSpinner spinner;

        public HorizontalSpinnerLayout(JSpinner spinner) {
            this.spinner = spinner;
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
            if ("Editor".equals(constraints)) {
                constraints = "Center";
            } else if ("Next".equals(constraints)) {
                constraints = "East";
                JButton button = new JButton(">");
                button.addActionListener((e) -> {
                    if (spinner.getModel().getNextValue()!=null){
                        spinner.getModel().setValue(spinner.getModel().getNextValue());
                    }
                });                    
                spinner.add(button);
                comp = button;
                
            } else if ("Previous".equals(constraints)) {
                constraints = "West";
                JButton button = new JButton("<");
                button.addActionListener((e) -> {
                    if (spinner.getModel().getPreviousValue()!=null){
                        spinner.getModel().setValue(spinner.getModel().getPreviousValue());
                    }
                });
                spinner.add(button);
                comp = button;                    
                
            } else  {
                return;
            }
            super.addLayoutComponent(comp, constraints);
        }
    }
}
