package ch.psi.pshell.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * A panel which simplifies receiving events, registering listeners and forwarding the events to
 * derived classes as protected method calls.
 */
public class MonitoredPanel extends JPanel{

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
    
    protected void onLafChange() {
    }
    
    //Utils
    public void showMessage(String title, String message) {
        SwingUtils.showMessage(this, title, message);
    }      
    
    public void showMessage(String title, String msg, int messageType) {
        SwingUtils.showMessage(this, title, msg, -1, messageType);
    }

    public void showScrollableMessage(String title, String description, String message) {
        SwingUtils.showScrollableMessage(this, title, description, message);
    }
    
    public JDialog showSplash(String title,  Dimension size, String message){
        return SwingUtils.showSplash(this, title, size, message);
    }    
    
    public JDialog showSplash(String title,  Dimension size, JPanel panel){
        return SwingUtils.showSplash(this, title, size, panel);
    }     
    
    public void showException(Exception ex) {
        SwingUtils.showException(this, ex);
    }

    public SwingUtils.OptionResult showOption(String title, String msg, SwingUtils.OptionType type) {
        return SwingUtils.showOption(this, title, msg, type);
    }

    public SwingUtils.OptionResult showOption(String title, Component componment, SwingUtils.OptionType type) {
        return SwingUtils.showOption(this, title, componment, type);
    }

    public String getString(String msg, Object current) {
        return SwingUtils.getString(this, msg, current);
    }
    
    public String getString(String msg, String[] options, String current) {
        return SwingUtils.getString(this, msg, options, current);
    }    

    public String getPassword(Component parent, String title, String msg) {
        return SwingUtils.getPassword(this, title, msg);
    }

    public Object getEnum(Component parent, String msg, Class type, Object current) {
        return SwingUtils.getEnum(this, msg,  type, current);
    }
    
    public JDialog showDialog(String title, Dimension size, JComponent content) {
        return SwingUtils.showDialog(getWindow(), title, size, content);
    }
    
    public JFrame showFrame(String title, Dimension size, JComponent content) {
        return SwingUtils.showFrame(getWindow(), title, size, content);
    }        
    
    public Frame getFrame(){
        return SwingUtils.getFrame(this);
    }
    
    public Window getWindow() {
        return SwingUtils.getWindow(this);
    }
    
}
