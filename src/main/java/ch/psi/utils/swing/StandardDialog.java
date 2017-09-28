package ch.psi.utils.swing;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

/**
 * Dialog implementing: - Ok/Cancel commands. - Canceling when hitting "Esc". - Listener for
 * closing. - Forward dialog events to derived classes as protected method calls.
 */
public class StandardDialog extends JDialog {

    boolean showing = false;

    public StandardDialog() {
        super();
    }

    public StandardDialog(Dialog owner) {
        super(owner);
    }

    public StandardDialog(Dialog owner, boolean modal) {
        super(owner, modal);
    }

    public StandardDialog(Dialog owner, String title) {
        super(owner, title);
    }

    public StandardDialog(Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
    }

    public StandardDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    public StandardDialog(Frame owner) {
        super(owner);
    }

    public StandardDialog(Frame owner, boolean modal) {
        super(owner, modal);
    }

    public StandardDialog(Frame owner, String title) {
        super(owner, title);
    }

    public StandardDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
    }

    public StandardDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    private void dialogOpened() {
        onOpened();
        triggerWindowOpened();
    }

    private void dialogClosed() {
        onClosed();
        triggerWindowClosed();
    }

    private void triggerWindowOpened() {
        if ((listener != null) && (listener instanceof StandardDialogListener)) {
            ((StandardDialogListener) listener).onWindowOpened(this);
        }

    }

    private void triggerWindowClosed() {
        if (listener != null) {
            listener.onWindowClosed(this, mResult);
        }
    }

    public boolean isDisposableOnClose() {
        return (getDefaultCloseOperation() == WindowConstants.DISPOSE_ON_CLOSE);
    }

    boolean cancelledOnEscape = true;

    public boolean isCancelledOnEscape() {
        return cancelledOnEscape;
    }

    public void setCancelledOnEscape(boolean value) {
        cancelledOnEscape = value;
    }

    @Override
    protected JRootPane createRootPane() {
        JRootPane rootPane = new JRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        Action actionListener = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (isCancelledOnEscape()) {
                    cancel();
                }
            }
        };
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", actionListener);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                dialogOpened();
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                dialogClosed();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                onActive();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                onDesactive();
            }
        });

        addHierarchyListener((HierarchyEvent e) -> {
            boolean parentChanged = (e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0;
            boolean showingChanged = (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0;
            boolean displayabilityChanged = (e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0;

            if (showingChanged /*|| displayabilityChanged*/) {
                if (isShowing() && !showing) {
                    try {
                        onShow();
                    } catch (Exception ex) {
                        Logger.getLogger(MonitoredPanel.class.getName()).log(Level.FINE, null, ex);
                    }
                    showing = true;
                } else if (!isShowing() && showing) {
                    {
                        try {
                            onHide();

                            //If not disposable, notify listeners has been closed with no acceptance
                            if (!isDisposableOnClose()) {
                                triggerWindowClosed();
                            }

                        } catch (Exception ex) {
                            Logger.getLogger(MonitoredPanel.class.getName()).log(Level.FINE, null, ex);
                        }
                        showing = false;
                    }
                }
            }
        });

        return rootPane;
    }

    /**
     * The listener interface for receiving standard dialog close event. Declared separated from
     * StandardDialogListener in order setListener to support a functional interface, which is
     * convenient for checking the dialog result.
     */
    public interface StandardDialogCloseListener {

        public void onWindowClosed(StandardDialog dlg, boolean accepted);
    }

    /**
     * The listener interface for receiving standard dialog events.
     */
    public interface StandardDialogListener extends StandardDialogCloseListener {

        public void onWindowOpened(StandardDialog dlg);
    }

    StandardDialogCloseListener listener = null;

    public void setListener(StandardDialogCloseListener listener) {
        this.listener = listener;
    }

    //Overridables
    protected void onOpened() {
    }

    protected void onClosed() {
    }

    protected void onShow() {
    }

    protected void onHide() {
    }

    protected void onActive() {
    }

    protected void onDesactive() {
    }

    //Result values
    private boolean mResult = false;

    protected void cancel() {
        mResult = false;
        setVisible(false);
        if (isDisposableOnClose()) {
            dispose();
        } else {
            triggerWindowClosed();
        }
    }

    protected void accept() {
        mResult = true;
        setVisible(false);
        if (isDisposableOnClose()) {
            dispose();
        } else {
            triggerWindowClosed();
        }
    }

    public boolean getResult() {
        return mResult;
    }

}
