package ch.psi.pshell.imaging;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 *
 */
public class DirectSource extends SourceBase {

    BufferedImage img;

    public DirectSource(BufferedImage img) {
        this(null, img);
    }

    public DirectSource(String name, BufferedImage img) {
        super(name, null);
        this.img = img;
    }

    @Override
    public void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        doUpdate();

    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        pushImage(img);
    }

    public void set(BufferedImage img) throws IOException, InterruptedException {
        this.img = img;
        pushImage(img);
    }
}
