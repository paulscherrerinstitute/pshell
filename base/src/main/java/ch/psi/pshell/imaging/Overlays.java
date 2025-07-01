package ch.psi.pshell.imaging;

import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

/**
 */
public class Overlays {

    public static class Dot extends OverlayBase {

        public Dot() {
            this(null);
        }

        public Dot(Pen pen) {
            this(pen, UNDEFINED_POINT);
        }

        public Dot(Pen pen, Point position) {
            super(pen);
            update(position);
        }

        @Override
        protected void draw(Graphics2D g) {
            Point position = getPosition();
            g.drawRect(position.x, position.y, 1, 1);
        }

        public void update(Point position) {
            setPosition(position);
        }
    }

    public static class Line extends OverlayBase {

        public Line() {
            this(null);
        }

        public Line(Pen pen) {
            this(pen, UNDEFINED_POINT, UNDEFINED_POINT);
        }

        public Line(Pen pen, Point start, Point end) {
            super(pen);
            update(start, end);
        }

        public void update(Point start, Point end) {
            setPosition(start);
            setSize(new Dimension(end.x - start.x, end.y - start.y));
        }

        public void update(Point end) {
            Point p = getPosition();
            update(p, end);
        }

        @Override
        public boolean isInside(Point p) {
            return isBorder(p);
        }

        @Override
        public boolean isBorder(Point p) {
            LineDouble l = new LineDouble(getPosition(), getUtmost());
            double distance = new PointDouble(p).getDistanceSegment(l);
            return distance < CONTOUR_TOLERANCE;
        }

        @Override
        protected void draw(Graphics2D g) {
            Point start = getPosition();
            Point end = getUtmost();
            g.drawLine(start.x, start.y, end.x, end.y);
        }

    }

    public static class Rect extends OverlayBase {

        public Rect() {
            this(null);
        }

        public Rect(Pen pen) {
            this(pen, UNDEFINED_POINT, UNDEFINED_POINT);
        }

        public Rect(Pen pen, Point position, Dimension size) {
            super(pen);
            update(position, size);
        }

        public Rect(Pen pen, Point start, Point end) {
            super(pen);
            update(start, end);
        }

        public void update(Point position, Dimension size) {
            setPosition(position);
            setSize(size);
        }

        public void update(Point start, Point end) {
            setPosition(start);
            setSize(new Dimension(end.x - start.x, end.y - start.y));
        }

        public void update(Point end) {
            Point start = getPosition();
            update(start, end);
        }

        @Override
        protected void draw(Graphics2D g) {
            Rectangle rect = getBounds();
            if (isFilled()) {
                g.fillRect(rect.x, rect.y, rect.width, rect.height);
            } else {
                g.drawRect(rect.x, rect.y, rect.width, rect.height);
            }
        }
    }

    public static class Ellipse extends Rect {

        public Ellipse() {
            this(null);
        }

        public Ellipse(Pen pen) {
            this(pen, UNDEFINED_POINT, UNDEFINED_POINT);
        }

        public Ellipse(Pen pen, Point position, Dimension size) {
            super(pen, position, size);
        }

        public Ellipse(Pen pen, Point start, Point end) {
            super(pen, start, end);
        }

        @Override
        public boolean isInside(Point p) {
            Dimension s = getSize();
            Point pos = getCenter();
            double ellipse = ((Math.pow((double) (p.x - pos.x), 2) / Math.pow((double) (s.width / 2), 2))
                    + (Math.pow((double) (p.y - pos.y), 2) / Math.pow((double) (s.height / 2), 2)));
            return ellipse <= 1;
        }

        @Override
        public boolean isBorder(Point p) {
            Dimension s = getSize();
            Point pos = getCenter();
            double ellipse = ((Math.pow((double) (p.x - pos.x), 2) / Math.pow((double) (s.width / 2), 2))
                    + (Math.pow((double) (p.y - pos.y), 2) / Math.pow((double) (s.height / 2), 2)));
            return (ellipse > 0.95) && (ellipse < 1.05);
        }

        @Override
        protected void draw(Graphics2D g) {
            Rectangle rect = getBounds();
            if (isFilled()) {
                g.fillOval(rect.x, rect.y, rect.width, rect.height);
            } else {
                g.drawOval(rect.x, rect.y, rect.width, rect.height);
            }
        }

    }

    public static class Text extends OverlayBase {

        Color backgroundColor = null;

        public Text() {
            this(null, "", null);
        }

        public Text(Pen pen, String text, Font font) {
            this(pen, text, font, UNDEFINED_POINT);
        }

        public Text(Pen pen, String text, Font font, Point position) {
            super(pen);
            setPosition(position);
            setFont(font);
            setText(text);
            manageReducedScale = true;
        }

        public void update(String str) {
            setText(str);
        }

        public void update(Point p) {
            setPosition(p);
        }

        public void setBackgroundColor(Color color) {
            backgroundColor = color;
        }

        @Override
        protected void draw(Graphics2D g) {
            Point p = getPosition();
            String[] tokens = getText().split("\n");
            for (int i = 0; i < tokens.length; i++) {
                String str = tokens[i].trim();
                if (backgroundColor != null) {
                    Dimension dim = SwingUtils.getTextSize(str, g.getFontMetrics());
                    g.setColor(backgroundColor);
                    g.fillRect(p.x, p.y + (getFont().getSize() - 1) * (i - 1), dim.width, dim.height + 2);
                    g.setColor(getColor());
                }
                g.drawString(str, p.x, p.y + (getFont().getSize() + 2) * i);
            }
        }
    }

    public static class Image extends Rect {

        BufferedImage image;

        public Image() {
            this(null, null, null);
            manageReducedScale = true;
        }

        public Image(Pen pen, Point p, BufferedImage image) {
            super(pen, p, new Dimension(image.getWidth(), image.getHeight()));
            this.image = image;
        }

        public void update(BufferedImage image) {
            this.image = image;
        }

        @Override
        protected void draw(Graphics2D g) {
            Point p = getPosition();
            g.drawImage(image, null, p.x, p.y);
        }
    }

    public static class Crosshairs extends Dot {

        public Crosshairs() {
            this(null);
        }

        public Crosshairs(Pen pen) {
            this(pen, UNDEFINED_POINT);
        }

        public Crosshairs(Pen pen, Point p) {
            this(pen, p, new Dimension(-1, -1));
        }

        public Crosshairs(Pen pen, Dimension size) {
            super(pen);
            setSize(size);
        }

        public Crosshairs(Pen pen, Point p, Dimension size) {
            super(pen, p);
            setSize(size);
        }

        @Override
        protected void draw(Graphics2D g) {
            Point p = getPosition();
            Dimension s = getSize();
            Rectangle rect = g.getClipBounds(new java.awt.Rectangle(0, 0, 4000, 2000));
            if (getOriginalSize().width < 0) {
                g.drawLine(rect.x, p.y, rect.x + rect.width, p.y);
            } else {
                g.drawLine(p.x - s.width / 2, p.y, p.x + s.width / 2, p.y);
            }

            if (getOriginalSize().height < 0) {
                g.drawLine(p.x, rect.y, p.x, rect.y + rect.height);
            } else {
                g.drawLine(p.x, p.y - s.height / 2, p.x, p.y + s.height / 2);
            }
        }

        @Override
        public boolean isBorder(Point p) {
            Point pos = getPosition();
            if (Math.abs(p.x - pos.x) <= CONTOUR_TOLERANCE) {
                if ((getOriginalSize().height < 0) || (Math.abs(p.y - pos.y) <= CONTOUR_TOLERANCE)) {
                    return true;
                }
            }
            if (Math.abs(p.y - pos.y) <= CONTOUR_TOLERANCE) {
                if ((getOriginalSize().width < 0) || (Math.abs(p.x - pos.x) <= CONTOUR_TOLERANCE)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class Arrow extends Line {

        ArrowType type = ArrowType.end;
        double arrowSize = 3.0;

        public enum ArrowType {

            begin,
            end,
            both
        }

        public Arrow() {
            this(null);
        }

        public Arrow(Pen pen) {
            super(pen);
        }

        public Arrow(Pen pen, Point p1, Point p2) {
            super(pen, p1, p2);
        }

        public void setArrowType(ArrowType value) {
            type = value;
        }

        public ArrowType getArrowType() {
            return type;
        }

        public void setArrowSize(double value) {
            arrowSize = value;
        }

        public double getArrowSize() {
            return arrowSize;
        }

        @Override
        protected void draw(Graphics2D g) {
            Point p1 = getPosition();
            Point p2 = getUtmost();
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
            double f1 = getArrowSize() / getLength();
            double f2 = 1 - f1;
            Dimension s = getSize();
            int w = s.width;
            int h = s.height;
            if (getArrowType() != ArrowType.begin) {
                g.drawLine(p1.x + (int) (f2 * w + f1 * h), p1.y + (int) (f2 * h - f1 * w), p1.x + w, p1.y + h);
                g.drawLine(p1.x + (int) (f2 * w - f1 * h), p1.y + (int) (f2 * h + f1 * w), p1.x + w, p1.y + h);
            }
            if (getArrowType() != ArrowType.end) {
                g.drawLine(p2.x - (int) (f2 * w + f1 * h), p2.y - (int) (f2 * h - f1 * w), p2.x - w, p2.y - h);
                g.drawLine(p2.x - (int) (f2 * w - f1 * h), p2.y - (int) (f2 * h + f1 * w), p2.x - w, p2.y - h);

            }
        }
    }

    public static class Polyline extends OverlayBase {

        int[] positionX;
        int[] positionY;
        double[] absolutePositionX = null;
        double[] absolutePositionY = null;

        public Polyline() {
            this(null);
        }

        public Polyline(Pen pen) {
            this(pen, null, null);
        }

        public Polyline(Pen pen, int[] x, int[] y) {
            super(pen);
            update(x, y);
        }

        public int getPoints() {
            if (positionX == null) {
                return 0;
            }
            return positionX.length;
        }

        public Point getPoint(int index) {
            if ((index < 0) || (index >= getPoints())) {
                return null;
            }
            Point pos;
            if (isCalibrated()) {
                pos = convertToDisplayPosition(new PointDouble(absolutePositionX[index], absolutePositionY[index]));
            } else {
                pos = new Point(positionX[index], positionY[index]);
            }
            return pos;
        }

        public int[] getX() {
            return positionX;
        }

        public int[] getY() {
            return positionY;
        }

        public double[] getAbsoluteX() {
            return absolutePositionX;
        }

        public double[] getAbsoluteY() {
            return absolutePositionY;
        }

        public void append(Point point) {
            append(point.x, point.y);
        }

        public void append(int x, int y) {
            if (positionX == null) {
                positionX = new int[0];
            }
            if (positionY == null) {
                positionY = new int[0];
            }
            int[] newX = new int[positionX.length + 1];
            int[] newY = new int[positionY.length + 1];
            System.arraycopy(positionX, 0, newX, 0, positionX.length);
            newX[positionX.length] = x;
            System.arraycopy(positionY, 0, newY, 0, positionY.length);
            newY[positionY.length] = y;
            update(newX, newY);
        }

        public void remove(int index) {
            if ((index < 0) || (index >= getPoints())) {
                return;
            }
            positionX = ArrayUtils.remove(positionX, index);
            positionY = ArrayUtils.remove(positionY, index);
            update(positionX, positionY);
        }

        protected void calibrate() {
            if ((!isCalibrated())
                    || (absolutePositionX == null) || (absolutePositionY == null)
                    || (absolutePositionX.length != absolutePositionY.length)) {
                absolutePositionX = null;
                absolutePositionY = null;
            } else {
                if ((positionX == null) || (absolutePositionX.length != positionX.length)) {
                    positionX = new int[absolutePositionX.length];
                }
                if ((positionY == null) || (absolutePositionY.length != positionY.length)) {
                    positionY = new int[absolutePositionY.length];
                }
                for (int i = 0; i < positionX.length; i++) {
                    Point pos = convertToDisplayPosition(new PointDouble(absolutePositionX[i], absolutePositionY[i]));
                    positionX[i] = pos.x;
                    positionY[i] = pos.y;
                }
            }
            if (getPoints() == 0) {
                super.setPosition(new Point(0, 0));
                super.setUtmost(new Point(0, 0));
            } else {
                super.setPosition(new Point((Integer) Arr.getMin(positionX), (Integer) Arr.getMin(positionY)));
                super.setUtmost(new Point((Integer) Arr.getMax(positionX), (Integer) Arr.getMax(positionY)));
            }
        }

        @Override
        public boolean isInside(Point p) {
            return isBorder(p);
        }

        @Override
        public boolean isBorder(Point p) {
            for (int i = 0; i < getPoints() - 1; i++) {
                LineDouble l = new LineDouble(new Point(positionX[i], positionY[i]),
                        new Point(positionX[i + 1], positionY[i + 1]));
                double distance = new PointDouble(p).getDistanceSegment(l);
                if (distance < CONTOUR_TOLERANCE) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setPosition(Point value) {
            if (getPoints() > 0) {
                Point cur = getPosition();
                if (cur != null) {
                    int offsetX = value.x - cur.x;
                    int offsetY = value.y - cur.y;
                    for (int i = 0; i < getPoints(); i++) {
                        positionX[i] += offsetX;
                        positionY[i] += offsetY;
                    }
                    update(positionX, positionY);
                }
            }
        }

        @Override
        public void setAbsolutePosition(PointDouble value) {
            if (isCalibrated()) {
                setPosition(convertToDisplayPosition(value));
            } else {
                setPosition(new Point((int) value.getX(), (int) value.getY()));
            }
        }

        public void update(int index, int x, int y) {
            if ((index < 0) || (index >= getPoints())) {
                return;
            }
            positionX[index] = x;
            positionY[index] = y;
            if (isCalibrated()) {
                PointDouble absolute = convertToAbsolutePosition(new Point(positionX[index], positionY[index]));
                absolutePositionX[index] = absolute.getX();
                absolutePositionY[index] = absolute.getY();
            }
            calibrate();
        }

        public void update(int[] x, int[] y) {
            positionX = x;
            positionY = y;
            if (isCalibrated()) {
                absolutePositionX = new double[x.length];
                absolutePositionY = new double[y.length];
                for (int i = 0; i < x.length; i++) {
                    PointDouble absolute = convertToAbsolutePosition(new Point(x[i], y[i]));
                    absolutePositionX[i] = absolute.getX();
                    absolutePositionY[i] = absolute.getY();
                }
            }
            calibrate();
        }

        public void updateAbsolute(int index, double x, double y) {
            if ((index < 0) || (index >= getPoints())) {
                return;
            }
            if (!isCalibrated()) {
                update(index, (int) x, (int) y);
                return;
            }
            absolutePositionX[index] = x;
            absolutePositionY[index] = y;
            calibrate();
        }

        public void updateAbsolute(double[] x, double[] y) {
            if (!isCalibrated()) {
                update((int[]) Convert.toPrimitiveArray(x, int.class), (int[]) Convert.toPrimitiveArray(y, int.class));
                return;
            }
            absolutePositionX = x;
            absolutePositionY = y;
            calibrate();
        }

        @Override
        protected void draw(Graphics2D g) {
            if ((positionX != null) && (positionY != null)) {
                int[] x = positionX;
                int[] y = positionY;
                boolean managingScaling = isManagingScaling();
                if (managingScaling || (offset != null)) {
                    x = Arrays.copyOf(positionX, positionX.length);
                    y = Arrays.copyOf(positionY, positionY.length);
                    if (managingScaling) {
                        for (int i = 0; i < x.length; i++) {
                            x[i] = (int) (x[i] * zoomScaleX);
                            y[i] = (int) (y[i] * zoomScaleY);
                        }
                    }
                    if (offset != null) {
                        for (int i = 0; i < x.length; i++) {
                            x[i] = x[i] + offset.x;
                            y[i] = y[i] + offset.y;
                        }
                    }
                }
                if (isFilled()) {
                    g.fillPolygon(x, y, x.length);
                } else {
                    g.drawPolyline(x, y, x.length);
                }
            }
        }
    }

    public static class Reticle extends Dot {

        public Reticle() {
            this(null);
        }

        Reticle(Pen pen) {
            super(pen);
            setAbsolutePosition(new PointDouble(0.0, 0.0));
        }

        private double tickUnits = 1;

        public void setTickUnits(double value) {
            tickUnits = value;
        }

        public double getTickUnits() {
            return tickUnits;
        }

        @Override
        protected void draw(Graphics2D g) {
            int ticksX = Math.abs((int) (((getOriginalSize().width < 0) ? 4000 : isCalibrated() ? getAbsoluteWidth() : getWidth()) / tickUnits));
            int ticksY = Math.abs((int) (((getOriginalSize().height < 0) ? 4000 : isCalibrated() ? getAbsoluteHeight() : getHeight()) / tickUnits));
            double scaleX = isCalibrated() ? getCalibration().scaleX : 1;
            double scaleY = isCalibrated() ? getCalibration().scaleY : 1;
            Point pos = getPosition();
            if (isManagingScaling()) {
                pos = new Point((int) (pos.x * zoomScaleX), (int) (pos.y * zoomScaleY));
                scaleX /= zoomScaleX;
                scaleY /= zoomScaleY;
            }

            int bigTickInterval = 10;
            double scale = Math.abs(scaleX);
            //Minimum distance of 50px between big ticks
            while ((bigTickInterval * tickUnits) < (50 * scale)) {
                bigTickInterval *= 10;
            }
            int mediumTickInterval = bigTickInterval / 2;
            int textInterval = ((getWidth() / (tickUnits / scale)) > 25) ? bigTickInterval : mediumTickInterval;

            int crossLength = 8;
            int smallTickLength = 2;
            int mediumTickLength = 6;
            int bigTickLength = 10;
            int textOffsetX = 16;
            int textOffsetY = 10;
            g.setFont(new Font("Verdana", Font.PLAIN, 10));
            for (int i = -ticksX / 2; i <= ticksX / 2; i++) {
                int tickLength = smallTickLength;
                int posX = (int) (pos.x + i * tickUnits / scaleX);
                if (i == 0) {
                    tickLength = crossLength;
                } else {
                    if ((i % mediumTickInterval) == 0) {
                        tickLength = mediumTickLength;
                    }
                    if ((i % bigTickInterval) == 0) {
                        tickLength = bigTickLength;
                    }
                    if ((i % textInterval) == 0) {
                        drawCenteredString(g, String.valueOf(i * tickUnits), new Point(posX, pos.y - textOffsetY));
                    }
                }
                g.drawLine(posX, pos.y - tickLength, posX, pos.y + tickLength);
            }

            for (int i = -ticksY / 2; i <= ticksY / 2; i++) {
                int tickLength = smallTickLength;
                int posY = (int) (pos.y + i * tickUnits / scaleY);
                if (i == 0) {
                    tickLength = crossLength;
                } else {
                    if ((i % mediumTickInterval) == 0) {
                        tickLength = mediumTickLength;
                    }
                    if ((i % (bigTickInterval)) == 0) {
                        tickLength = bigTickLength;
                    }
                    if ((i % textInterval) == 0) {
                        g.drawString(String.valueOf(i * tickUnits), pos.x + textOffsetX, posY + 4);
                    }
                }
                g.drawLine(pos.x - tickLength, posY, pos.x + tickLength, posY);
            }
        }
    }

}
