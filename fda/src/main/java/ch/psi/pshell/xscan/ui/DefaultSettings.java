package ch.psi.pshell.xscan.ui;

/**
 * Settings object, affecting following behavior: Collapsed status collapsible panels
 *
 */
public class DefaultSettings {

    private final static DefaultSettings instance = new DefaultSettings();

    private boolean collapsed = false;

    private DefaultSettings() {
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public static DefaultSettings getInstance() {
        return (instance);
    }
}
