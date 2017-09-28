package ch.psi.utils.swing;

import ch.psi.utils.ObservableBase;
import java.io.IOException;

/**
 * Representation of documents manipulated by Editor class.
 */
public abstract class Document extends ObservableBase<DocumentListener> {

    abstract public void clear();

    abstract public void load(String fileName) throws IOException;

    abstract public void save(String fileName) throws IOException;

    boolean changed;

    public void setChanged(boolean changed) {
        if (changed != this.changed) {
            this.changed = changed;
            for (DocumentListener listener : getListeners()) {
                listener.onDocumentChanged(this);
            }
        }
    }

    public boolean hasChanged() {
        return changed;
    }

    /**
     * Return string to be copied to clipboard
     */
    public String getContents() {
        return "";
    }
}
