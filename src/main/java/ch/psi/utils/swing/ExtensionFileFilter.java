package ch.psi.utils.swing;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 * Create FileFilter based on extension(s).
 */
public class ExtensionFileFilter extends FileFilter {

    String description;
    String extensions[];

    public ExtensionFileFilter(String description, String extension) {
        this(description, new String[]{extension});
    }

    public ExtensionFileFilter(String description, String extensions[]) {
        if (description == null) {
            this.description = extensions[0];
        } else {
            this.description = description;
        }
        this.extensions = (String[]) extensions.clone();
        for (int i = 0, n = extensions.length; i < n; i++) {
            extensions[i] = extensions[i].toLowerCase();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        } else {
            String path = file.getAbsolutePath().toLowerCase();
            for (int i = 0, n = extensions.length; i < n; i++) {
                String extension = extensions[i];
                if ((path.endsWith(extension) && (path.charAt(path.length() - extension.length() - 1)) == '.')) {
                    return true;
                }
            }
        }
        return false;
    }

}
