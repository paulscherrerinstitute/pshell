package ch.psi.pshell.imaging;

import ch.psi.pshell.utils.Reflection.Hidden;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;

/**
 *
 */
public interface Overlay extends Serializable {

    static int ANCHOR_IMAGE_TOP_LEFT = 0;
    static int ANCHOR_IMAGE_TOP_RIGHT = 1;
    static int ANCHOR_IMAGE_BOTTOM_LEFT = 2;
    static int ANCHOR_IMAGE_BOTTOM_RIGHT = 3;
    static int ANCHOR_VIEWPORT_TOP_LEFT = 4;
    static int ANCHOR_VIEWPORT_TOP_RIGHT = 5;
    static int ANCHOR_VIEWPORT_BOTTOM_LEFT = 6;
    static int ANCHOR_VIEWPORT_BOTTOM_RIGHT = 7;
    static int ANCHOR_VIEWPORT_OR_IMAGE_TOP_RIGHT = 8;
    static int ANCHOR_VIEWPORT_OR_IMAGE_BOTTOM_LEFT = 9;
    static int ANCHOR_VIEWPORT_OR_IMAGE_BOTTOM_RIGHT = 10;
    static int ANCHOR_VIEWPORT_TOP = 11;
    static int ANCHOR_VIEWPORT_RIGHT = 12;
    static int ANCHOR_VIEWPORT_LEFT = 13;
    static int ANCHOR_VIEWPORT_BOTTOM = 14;
    static int ANCHOR_VIEWPORT_OR_IMAGE_RIGHT = 15;
    static int ANCHOR_VIEWPORT_OR_IMAGE_BOTTOM = 17;

    public class ZOrderComparator implements Comparator<Overlay> {

        @Override
        public int compare(Overlay o1, Overlay o2) {
            if ((o1 == null) || (o2 == null)) {
                return 0;
            }
            return Integer.valueOf(o1.getZOrder()).compareTo(o2.getZOrder());
        }
    }

    //Geometry
    public Dimension getSize();

    public void setSize(Dimension value);

    public int getWidth();

    public void setWidth(int width);

    public int getHeight();

    public void setHeight(int height);

    public Point getPosition();

    public void setPosition(Point value);

    public Point getUtmost();

    public void setUtmost(Point value);

    public double getLength();

    public Rectangle getBounds();

    public Point getCenter();

    public double getRotation();

    public void setRotation(double value);

    public int getZOrder();

    public void setZOrder(int value);

    //Calibrated geometry....
    public DimensionDouble getAbsoluteSize();

    public void setAbsoluteSize(DimensionDouble value);

    public double getAbsoluteWidth();

    public void setAbsoluteWidth(double width);

    public double getAbsoluteHeight();

    public void setAbsoluteHeight(double height);

    public PointDouble getAbsolutePosition();

    public void setAbsolutePosition(PointDouble value);

    public double getAbsoluteLength();

    public PointDouble getAbsoluteUtmost();

    public PointDouble getAbsoluteCenter();

    //Attributes
    public Color getColor();

    public void setColor(Color value);

    public void setXOR(Color value);    //null for no XOR, Color.WHITE for standard XOR op

    public Color getXOR();

    public Font getFont();

    public void setFont(Font value);

    public void setText(String value);

    public String getText();

    public Pen getPen();

    public void setPen(Pen p);

    //Configuration
    public boolean isVisible();

    public void setVisible(boolean value);

    public boolean isFixed();

    public void setFixed(boolean value);

    public int getAnchor();

    public void setAnchor(int value);

    public boolean isPassive();

    /**
     * If passive the overlay is zoom scaled together with the raster. Otherwise it must draw itself
     * scaled.
     */
    public void setPassive(boolean value);

    public boolean isSolid();

    public void setSolid(boolean value);

    public boolean isFilled();

    public void setFilled(boolean value);

    public Calibration getCalibration();

    public void setCalibration(Calibration calibration);

    //Utilities
    public boolean isInside(Point p);

    public boolean isBorder(Point p);

    public boolean contains(Point p);

    public Overlay copy() throws IOException;

    //Mouse operations
    public boolean isMovable();

    public void setMovable(boolean value);

    public boolean isSelectable();

    public void setSelectable(boolean value);

    //Internals
    @Hidden
    public void setOffset(Point offset);

    @Hidden
    public Point getOffset();

    @Hidden
    public boolean isManagingScaling();

    //Painting
    public void paint(Graphics g);
}
