package ch.psi.pshell.imaging;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Polls a file, pushing a new frame every time the file changes.
 */
public class FileSource extends SourceBase {

    final String url;
    File file;
    long lastModified;

    public FileSource(String name, String url) {
        super(name, new SourceConfig());
        this.url = url;
    }

    @Override
    public void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        lastModified = 0;
        doUpdate();

    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        //Try not to rescan unchanged local files
        try {
            File file = new File(url);
            if ((file.exists()) && (file.isFile())) {
                if (lastModified == file.lastModified()) {
                    return;
                }
                lastModified = file.lastModified();
            }
        } catch (Exception ex) {
        }

        try (InputStream file = new BufferedInputStream(new FileInputStream(url))) {
            BufferedImage img = ImageIO.read(file);
            pushImage(img);
        } catch (IOException ex) {
            pushError(ex);
        }
    }

}
