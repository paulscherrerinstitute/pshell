package ch.psi.pshell.imaging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.Serializable;

/**
 *
 */
public class Pen implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static Stroke DEFAULT_STROKE = new BasicStroke(1f);

    public enum LineStyle {

        solid, dotted, dashed
    }

    final LineStyle lineStyle;
    final float lineWidth;
    final Color color;
    Color xor = null;

    transient Stroke stroke;

    public Pen(Color color) {
        this(color, 1, LineStyle.solid);
    }

    public Pen(Color color, float lineWidth) {
        this(color, lineWidth, LineStyle.solid);
    }

    public Pen(Color color, float lineWidth, LineStyle lineStyle) {
        this.lineStyle = lineStyle;
        this.lineWidth = lineWidth;
        this.color = color;
    }

    public LineStyle getLineStyle() {
        return lineStyle;
    }

    public float getWidth() {
        return lineWidth;
    }

    public Color getColor() {
        return color;
    }

    public void setXOR(Color color) {
        xor = color;
    }

    public Color getXOR() {
        return xor;
    }

    public Stroke getStroke() {
        if (stroke == null) {
            stroke = switch (lineStyle) {
                case solid -> new BasicStroke(this.lineWidth);
                case dotted -> new BasicStroke(this.lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{2f}, 0f);
                case dashed -> new BasicStroke(this.lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{5f}, 0f);
                default -> null;
            };
        }
        return stroke;
    }

}
