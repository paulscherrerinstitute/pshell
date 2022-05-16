package ch.psi.pshell.xscan.ui;

import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * TransferHandler
 */
public class ListItemTransferHandler extends TransferHandler {

    @Override()
    public Transferable createTransferable(JComponent c) {
        if (c instanceof ListItem) {
            return((Transferable) c); // RandomDragAndDropPanel implements Transferable
        }

        // Not found
        return null;
    }

    @Override()
    public int getSourceActions(JComponent c) {
        if (c instanceof ListItem) {
            return TransferHandler.COPY;
        }

        return TransferHandler.NONE;
    }
}