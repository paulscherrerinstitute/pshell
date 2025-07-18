package ch.psi.pshell.swing;

import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.IO.FilePermissions;
import ch.psi.pshell.utils.ObservableBase;
import java.io.IOException;

/**
 * Representation of documents manipulated by Editor class.
 */
public abstract class Document extends ObservableBase<DocumentListener> {

    abstract public void clear();

    abstract public void load(String fileName) throws IOException;

    abstract public void save(String fileName) throws IOException;

    public void save(String fileName, FilePermissions filePermissions) throws IOException {
        save(fileName);
        IO.setFilePermissions(fileName, filePermissions);
        for (DocumentListener listener : getListeners()) {
            listener.onDocumentSaved(this);
        }
    }

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
