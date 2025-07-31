package ch.psi.pshell.imaging;

import ch.psi.pshell.utils.Serializer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;

/**
 */
public abstract class OverlayBase implements Overlay {

    public static final Point UNDEFINED_POINT = new Point(-1, -1);
    public static final Dimension UNDEFINED_SIZE = new Dimension(0, 0);
    public static final int CONTOUR_TOLERANCE = 4;

    Point position;
    Dimension size;
    Color color;
    Color xor;
    Font font;
    boolean visible;
    String text;
    Pen pen;
    boolean solid;
    boolean filled;
    boolean fixed;
    int anchor;
    boolean passive;
    int zOrder;
    Double rotation;
    PointDouble absolutePosition;
    DimensionDouble absoluteSize;
    Calibration calibration;

    static double zoomScaleX = 1.0;
    static double zoomScaleY = 1.0;

    protected OverlayBase() {
        this(null);
    }

    protected OverlayBase(Pen pen) {
        position = new Point(UNDEFINED_POINT);
        size = new Dimension(UNDEFINED_SIZE);
        absolutePosition = new PointDouble(UNDEFINED_POINT);
        absoluteSize = new DimensionDouble(UNDEFINED_SIZE);
        passive = true;
        visible = true;
        setPen(pen);
    }

    //Geometry
    @Override
    public Dimension getSize() {
        if (!isCalibrated()) {
            return size;
        }
        return convertToDisplaySize(getAbsoluteSize());
    }

    @Override
    public void setSize(Dimension value) {
        if (isCalibrated()) {
            setAbsoluteSize(convertToAbsoluteSize(value));
        }
        size.setSize(value);
    }

    @Override
    public int getWidth() {
        return getSize().width;
    }

    @Override
    public void setWidth(int width) {
        setSize(new Dimension(width, getHeight()));
    }

    @Override
    public int getHeight() {
        return getSize().height;
    }

    @Override
    public void setHeight(int height) {
        setSize(new Dimension(getWidth(), height));
    }

    protected Dimension getOriginalSize() {
        return size;
    }

    @Override
    public Point getPosition() {
        Point ret = position;
        if (isCalibrated()) {
            PointDouble rp = getAbsolutePosition();
            ret = convertToDisplayPosition(rp);
        }
        if (offset != null) {
            return new Point(ret.x + offset.x, ret.y + offset.y);
        }
        return ret;
    }

    @Override
    public void setPosition(Point value) {
        if (isCalibrated()) {
            PointDouble rp = convertToAbsolutePosition(value);
            absolutePosition.setLocation(rp.getX(), rp.getY());
        }
        if (offset != null) {
            value = new Point(value.x - offset.x, value.y - offset.y);
        }
        position.setLocation(value);
    }

    Point offset;

    @Override
    public void setOffset(Point offset) {
        this.offset = offset;
    }

    @Override
    public Point getOffset() {
        return offset;
    }

    boolean manageReducedScale;

    @Override
    public boolean isManagingScaling() {
        return !passive && !fixed
                && (((zoomScaleX > 1) && (zoomScaleY > 1)) || (manageReducedScale));
    }

    protected Point getOriginalPosition() {
        return position;
    }

    @Override
    public Point getUtmost() {
        Point p = getPosition();
        Dimension s = getSize();
        return new Point(p.x + s.width, p.y + s.height);
    }

    @Override
    public void setUtmost(Point value) {
        Point p = getPosition();
        setSize(new Dimension(value.x - p.x, value.y - p.y));
    }

    @Override
    public double getLength() {
        Dimension s = getSize();
        return Math.hypot(s.getWidth(), s.getHeight());
    }

    @Override
    public Rectangle getBounds() {
        int w = getWidth();
        int h = getHeight();
        int x = (w >= 0) ? getPosition().x : getPosition().x + w;
        int y = (h >= 0) ? getPosition().y : getPosition().y + h;
        return new Rectangle(x, y, Math.abs(w), Math.abs(h));
    }

    @Override
    public Point getCenter() {
        Rectangle bounds = getBounds();
        return new Point(bounds.x + (bounds.width / 2), bounds.y + (bounds.height / 2));
    }

    @Override
    public double getRotation() {
        return (rotation == null) ? 0.0 : rotation;
    }

    @Override
    public void setRotation(double value) {
        rotation = value;
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }

    @Override
    public void setZOrder(int value) {
        zOrder = value;
    }

    //Calibrated geometry....    
    @Override
    public DimensionDouble getAbsoluteSize() {
        return absoluteSize;
    }

    @Override
    public void setAbsoluteSize(DimensionDouble value) {
        absoluteSize.setSize(value);
    }

    @Override
    public double getAbsoluteWidth() {
        return getAbsoluteSize().getWidth();
    }

    @Override
    public void setAbsoluteWidth(double width) {
        setAbsoluteSize(new DimensionDouble(width, getAbsoluteHeight()));
    }

    @Override
    public double getAbsoluteHeight() {
        return getAbsoluteSize().getHeight();
    }

    @Override
    public void setAbsoluteHeight(double height) {
        setAbsoluteSize(new DimensionDouble(getAbsoluteWidth(), height));
    }

    @Override
    public PointDouble getAbsolutePosition() {
        return absolutePosition;
    }

    @Override
    public void setAbsolutePosition(PointDouble value) {
        absolutePosition.setLocation(value);
    }

    @Override
    public double getAbsoluteLength() {
        DimensionDouble s = getAbsoluteSize();
        return Math.hypot(s.getWidth(), s.getHeight());
    }

    @Override
    public PointDouble getAbsoluteUtmost() {
        PointDouble p = getAbsolutePosition();
        DimensionDouble s = getAbsoluteSize();
        return new PointDouble(p.getX() + s.getWidth(), p.getY() + s.getHeight());
    }

    @Override
    public PointDouble getAbsoluteCenter() {
        PointDouble pos = getAbsolutePosition();
        DimensionDouble s = getAbsoluteSize();
        return new PointDouble(pos.getX() + s.getWidth() / 2, pos.getY() + s.getHeight() / 2);
    }

    //Attibutes
    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color value) {
        color = value;
    }

    @Override
    public Color getXOR() {
        return xor;
    }

    @Override
    public void setXOR(Color xor) {
        this.xor = xor;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void setFont(Font value) {
        font = value;
    }

    @Override
    public String getText() {
        if (text == null) {
            return "";
        }
        return text;
    }

    @Override
    public void setText(String value) {
        text = value;
    }

    @Override
    public Pen getPen() {
        return pen;
    }

    @Override
    public void setPen(Pen p) {
        pen = p;
        setColor((p == null) ? null : p.getColor());
        setXOR((p == null) ? null : p.getXOR());
    }

    //Configuration
    @Override
    public void setVisible(boolean value) {
        visible = value;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isFixed() {
        return fixed;
    }

    @Override
    public void setFixed(boolean value) {
        fixed = value;
    }

    @Override
    public int getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(int value) {
        anchor = value;
    }

    @Override
    public boolean isPassive() {
        return passive;
    }

    @Override
    public void setPassive(boolean value) {
        passive = value;
    }

    @Override
    public boolean isSolid() {
        return solid;
    }

    @Override
    public void setSolid(boolean value) {
        solid = value;
    }

    @Override
    public boolean isFilled() {
        return filled;
    }

    @Override
    public void setFilled(boolean value) {
        filled = value;
    }

    //Calibration
    public boolean isCalibrated() {
        return (calibration != null);
    }

    @Override
    public Calibration getCalibration() {
        return calibration;
    }

    @Override
    public void setCalibration(Calibration calibration) {
        if (calibration != this.calibration) {
            this.calibration = calibration;
            if (isCalibrated()) {
                if (absolutePosition.equals(UNDEFINED_POINT) && !position.equals(UNDEFINED_POINT)) {
                    absolutePosition = convertToAbsolutePosition(position);
                }
                if (absoluteSize.equals(UNDEFINED_SIZE) && !size.equals(UNDEFINED_SIZE)) {
                    absoluteSize = convertToAbsoluteSize(size);
                }
            }
        }
    }

    public PointDouble convertToAbsolutePosition(Point displayPosition) {
        if (calibration == null) {
            return null;
        }
        return calibration.convertToAbsolutePosition(displayPosition);
    }

    public Point convertToDisplayPosition(PointDouble absolutePosition) {
        if (calibration == null) {
            return null;
        }
        return calibration.convertToImagePosition(absolutePosition);
    }

    public DimensionDouble convertToAbsoluteSize(Dimension displaySize) {
        if (calibration == null) {
            return null;
        }
        return new DimensionDouble(displaySize.getWidth() * calibration.getScaleX(),
                displaySize.getHeight() * calibration.getScaleY());
    }

    public Dimension convertToDisplaySize(DimensionDouble absoluteSize) {
        if (calibration == null) {
            return null;
        }
        return new Dimension((int) Math.round(absoluteSize.getWidth() / calibration.getScaleX()),
                (int) Math.round(absoluteSize.getHeight() / calibration.getScaleY()));
    }

    //Utilities
    @Override
    public boolean isInside(Point p) {
        return getBounds().contains(p);
    }

    @Override
    public boolean isBorder(Point p) {
        Rectangle bounds = getBounds();
        Rectangle inner = new Rectangle(bounds.x + CONTOUR_TOLERANCE, bounds.y + CONTOUR_TOLERANCE, bounds.width - 2 * CONTOUR_TOLERANCE, bounds.height - 2 * CONTOUR_TOLERANCE);
        Rectangle outer = new Rectangle(bounds.x - CONTOUR_TOLERANCE, bounds.y - CONTOUR_TOLERANCE, bounds.width + 2 * CONTOUR_TOLERANCE, bounds.height + 2 * CONTOUR_TOLERANCE);
        return (outer.contains(p)) && (!inner.contains(p));
    }

    @Override
    public boolean contains(Point p) {
        return isBorder(p) || (isSolid() && isInside(p));
    }

    @Override
    public Overlay copy() throws IOException {
        return (Overlay) Serializer.copy(this);
    }

    //Mouse operations
    boolean movable;

    @Override
    public boolean isMovable() {
        return movable;
    }

    @Override
    public void setMovable(boolean value) {
        movable = value;
    }

    boolean selectable;

    @Override
    public boolean isSelectable() {
        return selectable;
    }

    @Override
    public void setSelectable(boolean value) {
        selectable = value;
    }

    public void drawCenteredString(Graphics g, String text, Point p) {
        FontMetrics fm = g.getFontMetrics(g.getFont());
        int x = p.x - fm.stringWidth(text) / 2;
        int y = p.y - (fm.getHeight()) / 2;
        g.drawString(text, x, y);
    }

    //Painting
    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform back = null;

        if (isVisible()) {
            if (color != null) {
                g.setColor(color);
            }
            if (xor != null) {
                g.setXORMode(xor);
            }
            if (font != null) {
                g.setFont(font);
            }
            if (pen.getStroke() != null) {
                g2d.setStroke(pen.getStroke());
            }
            if (rotation != null) {
                back = g2d.getTransform();
                g2d.transform(AffineTransform.getRotateInstance(-rotation, position.x, position.y));
            }
            Dimension size = this.size;
            Point position = this.position;
            if (isManagingScaling()) {
                this.size = new Dimension((int) (size.width * zoomScaleX), (int) (size.height * zoomScaleY));
                this.position = new Point((int) (position.x * zoomScaleX), (int) (position.y * zoomScaleY));
            }

            draw(g2d);

            this.size = size;
            this.position = position;
            if (xor != null) {
                g.setPaintMode();
            }
            if (pen.getStroke() != null) {
                g2d.setStroke(Pen.DEFAULT_STROKE);
            }
            if (rotation != null) {
                g2d.setTransform(back);
            }
        }
    }

    abstract protected void draw(Graphics2D g);
}
