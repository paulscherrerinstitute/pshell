package ch.psi.pshell.swing;

import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.TransferHandler;

/**
 * Handles the transfer of files to the clipboard.
 */
public class FileTransferHandler extends TransferHandler implements Transferable {

    private DataFlavor[] dataFlavors = {DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor};
    private final List<File>  files;

    public FileTransferHandler(String file) {
        this(new File(file));
    }

    public FileTransferHandler(File file) {
        this(List.of(file));
    }

    public FileTransferHandler(List<File> files) {
        this.files = files;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) {
        if (isDataFlavorSupported(flavor)) {
            if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                return files;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                return String.join(", ", Convert.toStringArray(files));
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
