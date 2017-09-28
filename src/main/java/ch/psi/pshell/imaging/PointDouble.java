package ch.psi.pshell.imaging;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 *
 */
public class PointDouble extends Point2D.Double implements Serializable {

    private static final long serialVersionUID = 1L;

    public PointDouble() {
        super();
    }

    public PointDouble(double x, double y) {
        super(x, y);
    }

    public PointDouble(Point2D p) {
        this(p.getX(), p.getY());
    }

    public double getDistance(Point2D p) {
        return Math.hypot((p.getX() - x), (p.getY() - y));
    }

    public double getDistance(Line2D l) {
        double x1 = l.getX1();
        double x2 = l.getX2();
        double y1 = l.getY1();
        double y2 = l.getY2();
        double normal = Math.hypot((x2 - x1), (y2 - y1));
        return Math.abs((x - x1) * (y2 - y1) - (y - y1) * (x2 - x1)) / normal;
    }

    public double getDistanceSegment(LineDouble l) {
        double distanceToLine = getDistance(l);
        double lineSize = l.getSize();
        double distanceToP1 = getDistance(l.getP1());
        double distanceToP2 = getDistance(l.getP2());
        if (Math.pow(distanceToP2, 2) > (Math.pow(lineSize, 2) + (Math.pow(distanceToLine, 2)))) {
            return distanceToP1;
        }
        if (Math.pow(distanceToP1, 2) > (Math.pow(lineSize, 2) + (Math.pow(distanceToLine, 2)))) {
            return distanceToP2;
        }
        return distanceToLine;
    }
}
