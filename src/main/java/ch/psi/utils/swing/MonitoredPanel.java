package ch.psi.utils.swing;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 * A panel which simplifies receiving events, registering listeners and forwarding the events to
 * derived classes as protected method calls.
 */
public class MonitoredPanel extends JPanel {

    boolean showing = false;
    boolean displayable = false;

    public MonitoredPanel() {
        super();
        addHierarchyListener((HierarchyEvent e) -> {
            boolean parentChanged = (e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0;
            boolean showingChanged = (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0;
            boolean displayabilityChanged = (e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0;

            if (showingChanged) {
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
                        } catch (Exception ex) {
                            Logger.getLogger(MonitoredPanel.class.getName()).log(Level.FINE, null, ex);
                        }
                        showing = false;
                    }
                }
            }
            if (displayabilityChanged) {
                if (isDisplayable() && !displayable) {
                    try {
                        onActive();
                    } catch (Exception ex) {
                        Logger.getLogger(MonitoredPanel.class.getName()).log(Level.FINE, null, ex);
                    }
                    displayable = true;
                } else if (!isShowing() && displayable) {
                    {
                        try {
                            onDesactive();
                        } catch (Exception ex) {
                            Logger.getLogger(MonitoredPanel.class.getName()).log(Level.FINE, null, ex);
                        }
                        displayable = false;
                    }
                }
            }
            if (parentChanged) {
                onParentChange();
            }
        });
    }

    protected void onShow() {
    }

    protected void onHide() {
    }

    protected void onActive() {
    }

    protected void onDesactive() {
    }

    protected void onParentChange() {
    }
    
    //Utils
    public void showException(Exception ex) {                                           
        SwingUtils.showException(SwingUtils.getWindow(this), ex);
    }        

    public void showMessage(String title, String message) {
        SwingUtils.showMessage(SwingUtils.getWindow(this), title, message);
    }      
    
    public Frame getFrame(){
        return SwingUtils.getFrame(this);
    }
    
    public Window getWindow() {
        return SwingUtils.getWindow(this);
    }
}
