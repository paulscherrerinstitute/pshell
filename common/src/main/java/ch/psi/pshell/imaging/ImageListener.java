package ch.psi.pshell.imaging;

import java.awt.image.BufferedImage;

/**
 * The listener interface for receiving image source events.
 */
public interface ImageListener {

    /**
     * Data structure is optional
     */
    void onImage(Object origin, BufferedImage image, Data data);

    void onError(Object origin, Exception ex);

}
