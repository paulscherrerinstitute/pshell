package ch.psi.pshell.imaging;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.imageio.ImageIO;

/**
 *
 */
public interface ImageBuffer {

    public BufferedImage getImage();

    public void waitNext(int timeout) throws InterruptedException, TimeoutException;

    default public BufferedImage getNext(int timeout) throws InterruptedException, TimeoutException {
        waitNext(timeout);
        return getImage();
    }

    //Saving
    default public void saveSnapshot(String fileName, String format) throws IOException {
        saveImage(getImage(), fileName, format);
    }

    static public void saveImage(BufferedImage image, String fileName, String format) throws IOException {
        if (image == null) {
            throw new IOException("No image available");
        }
        File file = new File(fileName);
        if (!ImageIO.write(image, format, file)) {
            throw new IOException("Image format not supported: " + format);
        }
    }
}
