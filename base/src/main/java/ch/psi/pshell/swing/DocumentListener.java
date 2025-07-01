package ch.psi.pshell.swing;

/**
 * The listener interface for receiving document events.
 */
public interface DocumentListener {
    void onDocumentChanged(Document doc);
    void onDocumentSaved(Document doc);
}
