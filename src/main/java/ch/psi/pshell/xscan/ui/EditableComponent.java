package ch.psi.pshell.xscan.ui;

/**
 *
 */
public interface EditableComponent {

    /**
     * Returns whether the component was modified and clears the components
     * modified state
     */
    public boolean modified();

    public void clearModified();


}
