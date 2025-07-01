package ch.psi.pshell.xscan.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 */
public abstract class DocumentAdapter implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent de) {
        valueChange(de);
    }

    @Override
    public void removeUpdate(DocumentEvent de) {
        valueChange(de);
    }

    @Override
    public void changedUpdate(DocumentEvent de) {
    }

    public abstract void valueChange(DocumentEvent de);

}
