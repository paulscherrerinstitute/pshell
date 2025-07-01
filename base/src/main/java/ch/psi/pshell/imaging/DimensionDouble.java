package ch.psi.pshell.imaging;

import java.awt.geom.Dimension2D;
import java.io.Serializable;

/**
 *
 */
public class DimensionDouble extends Dimension2D implements Serializable {

    private static final long serialVersionUID = 1L;

    double width;
    double height;

    public DimensionDouble() {
    }

    public DimensionDouble(double width, double height) {
        setSize(width, height);
    }

    public DimensionDouble(Dimension2D d) {
        this(d.getWidth(), d.getHeight());
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Dimension2D d2d) {
            return ((getWidth() == d2d.getWidth()) && (getHeight() == d2d.getHeight()));
        }
        return false;
    }

}
