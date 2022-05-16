package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.ui.App;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * Input verifier for JTextFields holding ids
 * If the id contains wrong characters a dialog box will be shown
 */
public class IdInputVerifier extends InputVerifier {

    @Override
    public boolean verify(JComponent jc) {
        JTextField tf = (JTextField) jc;
        boolean v = tf.getText().matches("^[a-zA-Z]+[a-zA-Z0-9-_]*$");

        if (!v) {
            /*      
            NotifyDescriptor d;
            if(!tf.getText().matches("^[a-zA-Z].*")){
                d = new NotifyDescriptor.Message("Id need to start with an character", NotifyDescriptor.WARNING_MESSAGE);
            }
            else{
                d = new NotifyDescriptor.Message("Invalid characters in Id - only accepting a-z,A-Z,0-9,-,_", NotifyDescriptor.WARNING_MESSAGE);
            }
            DialogDisplayer.getDefault().notify(d);
            */
            String message = null;
            if(!tf.getText().matches("^[a-zA-Z].*")){
                message = "Id need to start with an character";
            }
            else{
                message = "Invalid characters in Id - only accepting a-z,A-Z,0-9,-,_";
            }
            App.getInstance().getMainFrame().showMessage("Error", message);
        }

        return (v);
    }
}
