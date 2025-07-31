package ch.psi.pshell.imaging;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 *
 */
public class LineDouble extends Line2D.Double implements Serializable {

    private static final long serialVersionUID = 1L;

    public LineDouble() {
        super();
    }

    public LineDouble(Point2D p1, Point2D p2) {
        super(p1, p2);
    }

    public LineDouble(Line2D line) {
        this(line.getP1(), line.getP2());
    }

    public double getSize() {
        return new PointDouble(getP1()).getDistance(getP2());
    }
}
