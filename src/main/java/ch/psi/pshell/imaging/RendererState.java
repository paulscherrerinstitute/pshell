package ch.psi.pshell.imaging;

import java.awt.Point;
import java.io.Serializable;

/**
 *
 */
public class RendererState implements Serializable {

    private static final long serialVersionUID = 1L;
    public boolean status;
    public boolean reticle;
    public double zoom;
    public double scaleX;
    public double scaleY;
    public Point imagePosition;
    public RendererMode mode;
    public RendererMode formerMode;
    public Overlay marker;
    Calibration calibration;
    public Renderer.Profile profile;
}
