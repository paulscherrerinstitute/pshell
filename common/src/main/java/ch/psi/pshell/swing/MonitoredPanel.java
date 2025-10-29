package ch.psi.pshell.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 * A panel which simplifies receiving events, registering listeners and forwarding the events to
 * derived classes as protected method calls.
 */
public class MonitoredPanel extends JPanel{

    boolean showing = false;
    boolean displayable = false;
    private final Map<JTabbedPane, ChangeListener> attachedTabPanes = new IdentityHashMap<>();
    boolean handleTabEvents;

    public MonitoredPanel() {
        super();
        addHierarchyListener((HierarchyEvent e) -> {
            boolean parentChanged = (e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0;
            boolean showingChanged = (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0;
            boolean displayabilityChanged = (e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0;

            if (showingChanged) {                                
                if (getHandleTabEvents() && isInsideTabbedPane())  {
                    //Only handle onShow/onHide if NOT inside a tabbed pane
                    updateTabbedVisibility();
                } else {
                    if (isShowing() && !showing) {
                        safeCall(thisPanel -> thisPanel.onShow());
                        showing = true;
                    } else if (!isShowing() && showing) {
                        safeCall(thisPanel -> thisPanel.onHide());
                        showing = false;
                    }
                }
            }
            if (displayabilityChanged) {
                if (isDisplayable() && !displayable) {
                    safeCall(thisPanel -> thisPanel.onActive());
                    displayable = true;
                } else if (!isShowing() && displayable) {
                    safeCall(thisPanel -> thisPanel.onDesactive());
                    displayable = false;
                }
            }
            if (parentChanged) {
                safeCall(thisPanel -> thisPanel.onParentChange());
                if (getHandleTabEvents()){
                    attachTabbedPaneListeners();
                    updateTabbedVisibility();
                }
            }
        });
    }
    
    private void safeCall(java.util.function.Consumer<MonitoredPanel> action) {
        try {
            action.accept(this);
        } catch (Exception ex) {
            Logger.getLogger(MonitoredPanel.class.getName()).log(Level.FINE, null, ex);
        }
    }

    //If set to Truie handle onShow/onHide based on JTabbedPane ChangeListener (in case owned by a tab)
    //May be necessary if tabs visibility is set before first being displayed, to avoid an earlyu call to onShow.
    //Not set by default to avoid the overhead.
    public void setHandleTabEvents(boolean value){
        handleTabEvents = value;
    }
    
    public boolean getHandleTabEvents(){
        return handleTabEvents;
    }
    
    
    /**
     * Attach listeners to all parent tabbed panes 
     */
    private void attachTabbedPaneListeners() {
        // Remove listeners from panes no longer in the hierarchy
        Set<JTabbedPane> currentParents = findAllParentTabbedPanes();
        for (JTabbedPane tp : attachedTabPanes.keySet().toArray(new JTabbedPane[0])) {
            if (!currentParents.contains(tp)) {
                tp.removeChangeListener(attachedTabPanes.get(tp));
                attachedTabPanes.remove(tp);
            }
        }

        // Add listeners for any new tabbed panes
        for (JTabbedPane tp : currentParents) {
            if (!attachedTabPanes.containsKey(tp)) {
                ChangeListener listener = e -> updateTabbedVisibility();
                tp.addChangeListener(listener);
                attachedTabPanes.put(tp, listener);
            }
        }
    }
    /**
     * Returns true if this panel and all of its ancestor tabs are currently selected.
     */
    private boolean isEffectivelyVisibleInTabs() {
        Container current = this;
        while (current != null) {
            Container parent = current.getParent();
            if (parent instanceof JTabbedPane tp) {
                if (tp.getSelectedComponent() != current) {
                    return false;
                }
            }
            current = parent;
        }
        return isShowing(); // also require Swing-visible
    }

    private void updateTabbedVisibility() {
        boolean visible = isEffectivelyVisibleInTabs();
        if (visible && !showing) {
            safeCall(MonitoredPanel::onShow);
            showing = true;
        } else if (!visible && showing) {
            safeCall(MonitoredPanel::onHide);
            showing = false;
        }
    }

    private boolean isInsideTabbedPane() {
        return findParentTabbedPane() != null;
    }

    private JTabbedPane findParentTabbedPane() {
        Container p = getParent();
        while (p != null) {
            if (p instanceof JTabbedPane) return (JTabbedPane) p;
            p = p.getParent();
        }
        return null;
    }

    private Set<JTabbedPane> findAllParentTabbedPanes() {
        Set<JTabbedPane> set = new HashSet<>();
        Container p = getParent();
        while (p != null) {
            if (p instanceof JTabbedPane tp) set.add(tp);
            p = p.getParent();
        }
        return set;
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
