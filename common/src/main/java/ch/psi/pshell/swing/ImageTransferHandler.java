package ch.psi.pshell.swing;

import ch.psi.pshell.utils.Arr;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.TransferHandler;

/**
 * Handles the transfer of image and text to the clipboard.
 */
public class ImageTransferHandler extends TransferHandler implements Transferable {

    private DataFlavor[] dataFlavors = {DataFlavor.imageFlavor};
    private final Image image;
    private final String text;

    public ImageTransferHandler(Image data) {
        this.image = data;
        this.text = null;
    }

    public ImageTransferHandler(Image data, String text) {
        this.image = data;
        this.text = text;
        dataFlavors = new DataFlavor[]{DataFlavor.imageFlavor, DataFlavor.stringFlavor};
    }

    @Override
    public Object getTransferData(DataFlavor flavor) {
        if (isDataFlavorSupported(flavor)) {
            if (flavor.equals(DataFlavor.imageFlavor)) {
                return image;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                return text;
            }
        }
        return null;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return dataFlavors;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor flavor[]) {
        if (!(comp instanceof JLabel)) {
            return false;
        }
        for (DataFlavor f : flavor) {
            if (isDataFlavorSupported(f)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Arr.containsEqual(dataFlavors, flavor);
    }
}
