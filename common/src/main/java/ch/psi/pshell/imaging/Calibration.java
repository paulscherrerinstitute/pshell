package ch.psi.pshell.imaging;

import ch.psi.pshell.utils.Convert;
import java.awt.Point;
import java.io.Serializable;
import java.lang.reflect.Array;

/**
 *
 */
public class Calibration implements Serializable {

    private static final long serialVersionUID = 1L;

    final double scaleX, scaleY;
    final double offsetX, offsetY;

    public Calibration(double scaleX, double scaleY, double offsetX, double offsetY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public Calibration(double scaleX, double scaleY, Point center) {
        this(scaleX, scaleY, -center.x, -center.y);
    }

    //Only supporting linear arryas
    public Calibration(Object xAxis, Object yAxis) {
        int lenght;
        if ((xAxis != null) && (xAxis.getClass().isArray()) && ((lenght = Array.getLength(xAxis)) > 1)) {
            Double first = (Double) Convert.toDouble(Array.get(xAxis, 0));
            Double last = (Double) Convert.toDouble(Array.get(xAxis, lenght - 1));
            offsetX = (first / (last - first)) *  (lenght - 1);
            scaleX = (last - first) / (lenght - 1);
        } else {
            offsetX = 0.0;
            scaleX = 1.0;
        }
        if ((yAxis != null) && (yAxis.getClass().isArray()) && ((lenght = Array.getLength(yAxis)) > 1)) {
            Double first = (Double) Convert.toDouble(Array.get(yAxis, 0));
            Double last = (Double) Convert.toDouble(Array.get(yAxis, lenght - 1));
            offsetY = (first / (last - first)) *  (lenght - 1);
            scaleY = (last - first) / (lenght - 1);
        } else {
            offsetY = 0.0;
            scaleY = 1.0;
        }
    }

    public double getScaleX() {
        return scaleX;
    }

    public double getScaleY() {
        return scaleY;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public Point getCenter() {
        Point ret = new Point();
        ret.setLocation(-offsetX, -offsetY);
        return ret;
    }

    public PointDouble convertToAbsolutePosition(Point imagePosition) {
        double x = ((double) imagePosition.x) + offsetX;
        double y = ((double) imagePosition.y) + offsetY;

        x *= getScaleX();
        y *= getScaleY();

        return new PointDouble(x, y);
    }

    public Point convertToImagePosition(PointDouble absolutePosition) {
        double x = absolutePosition.getX() / getScaleX();
        double y = absolutePosition.getY() / getScaleY();

        x -= offsetX;
        y -= offsetY;

        Point ret = new Point();
        ret.setLocation(x, y);
        return ret;
    }

    public double convertToAbsoluteX(int imageX) {
        return (((double) imageX) + offsetX) * getScaleX();
    }

    public int convertToImageX(double absoluteX) {
        return (int) Math.floor(((absoluteX / getScaleX()) - offsetX) + 0.5);
    }

    public double convertToAbsoluteY(int imageY) {
        return (((double) imageY) + offsetY) * getScaleY();
    }

    public int convertToImageY(double absoluteY) {
        return (int) Math.floor(((absoluteY / getScaleY()) - offsetY) + 0.5);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Calibration c) {
            return ((scaleX == c.scaleX)
                    && (scaleY == c.scaleY)
                    && (offsetX == c.offsetX)
                    && (offsetY == c.offsetY));
        }
        return false;
    }

    public double[] getAxisX(int size) {
        double[] ret = new double[size];
        for (int i = 0; i < size; i++) {
            ret[i] = convertToAbsoluteX(i);
        }
        return ret;
    }

    public double[] getAxisY(int size) {
        double[] ret = new double[size];
        for (int i = 0; i < size; i++) {
            ret[i] = convertToAbsoluteY(i);
        }
        return ret;
    }
}
