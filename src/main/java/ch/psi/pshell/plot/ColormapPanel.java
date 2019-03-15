package ch.psi.pshell.plot;

import ch.psi.pshell.imaging.Colormap;
import static ch.psi.pshell.plot.LinePlotJFree.AUTO_RANGE_MINIMUM_SIZE;
import ch.psi.utils.Range;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnitSource;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

/**
 * Panel displaying a colormap
 */
public class ColormapPanel extends MonitoredPanel {

    PaintScaleLegend legend;
    NumberAxis axis;

    public enum Position {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    public ColormapPanel() {
        super();
        setPreferredSize(new Dimension(70, 400));
    }

    Position position = Position.RIGHT;

    public void setPosition(Position pos) {
        this.position = position;
        if (legend != null) {
            switch (position) {
                case LEFT:
                    legend.setPosition(RectangleEdge.LEFT);
                    break;
                case RIGHT:
                    legend.setPosition(RectangleEdge.RIGHT);
                    break;
                case TOP:
                    legend.setPosition(RectangleEdge.TOP);
                    break;
                case BOTTOM:
                    legend.setPosition(RectangleEdge.BOTTOM);
                    break;
            }
        }
    }

    public Position getPosition() {
        return position;
    }

    Integer stripWidth = 20;

    public void setStripWidth(Integer stripWidth) {
        this.stripWidth = stripWidth;
        if (legend != null) {
            legend.setStripWidth(stripWidth);
            updateUI();
        }
    }

    public Integer getStripWidth() {
        return stripWidth;
    }

    Boolean logarithmic = Boolean.FALSE;

    public void setLogarithmic(Boolean logarithmic) {
        this.logarithmic = logarithmic;
        update();
    }

    public Boolean isLogarithmic() {
        return logarithmic;
    }

    Double scaleMin = 0.0;

    public void setScaleMin(Double scaleMin) {
        this.scaleMin = scaleMin;
        update();
    }

    public Double getScaleMin() {
        return scaleMin;
    }

    Double scaleMax = 100.0;

    public void setScaleMax(Double scaleMax) {
        this.scaleMax = scaleMax;
        update();
    }

    public Double getScaleMax() {
        return scaleMax;
    }

    public void setScale(Range scale) {
        this.scaleMin = scale.min;
        this.scaleMax = scale.max;
        update();
    }

    public Range getScale() {
        return new Range(getScaleMin(), getScaleMax());
    }

    Colormap colormap = Colormap.Temperature;

    public void setColormap(Colormap colormap) {
        this.colormap = colormap;
        update();
    }

    public Colormap getColormap() {
        return colormap;
    }

    public void update(Colormap colormap, Range scale, Boolean logarithmic) {
        boolean changed = false;
        if ((colormap != null) && (colormap != this.colormap)) {
            this.colormap = colormap;
            changed = true;
        }
        if ((scale != null) && ((this.scaleMin != scale.min) || (this.scaleMin != scale.min))) {
            this.scaleMin = scale.min;
            this.scaleMax = scale.max;
            changed = true;
        }
        if ((logarithmic != null) && (logarithmic != this.logarithmic)) {
            this.logarithmic = logarithmic;
            changed = true;
        }
        if (changed) {
            update();
        }
    }

    void update() {
        if (isShowing()) {
            double min = Math.min(this.scaleMin, this.scaleMax);
            double max = Math.max(this.scaleMin, this.scaleMax);
            if (min >= max) {
                max = min + 0.01;
            }            
            LookupPaintScale legendScale = new LookupPaintScale(min, max, Color.GRAY);
            if (isLogarithmic()) {
                for (int i = 0; i < 256; i++) {
                    double value = min + (i / 255.0) * (max - min);
                    legendScale.add(value, colormap.getColorLogarithmic(value, min, max));
                }
            } else {
                for (int i = 0; i < 256; i++) {
                    double value = min + (i / 255.0) * (max - min);
                    legendScale.add(value, colormap.getColor(value, min, max));
                }
            }
            if (legend == null) {
                axis = new NumberAxis();
                axis.setAutoRangeMinimumSize(AUTO_RANGE_MINIMUM_SIZE);
                //Fix https://stackoverflow.com/questions/24210665/jfreechart-large-values
                axis.setStandardTickUnits(new NumberTickUnitSource() {
                    @Override
                    public TickUnit getCeilingTickUnit(double size) {
                        if (Double.isInfinite(size)) {
                            return super.getCeilingTickUnit(AUTO_RANGE_MINIMUM_SIZE);
                        }
                        return super.getCeilingTickUnit(size);
                    }
                });
                legend = new PaintScaleLegend(legendScale, axis);
                legend.setPadding(new RectangleInsets(5, 5, 5, 5));
                legend.setStripWidth(getStripWidth());
                setPosition(position);
                legend.setBackgroundPaint(getBackground());
                legend.getAxis().setTickLabelPaint(PlotBase.getAxisTextColor());
                legend.setVisible(true);
            } else {
                legend.setScale(legendScale);
                axis.setRange(legendScale.getLowerBound(), legendScale.getUpperBound());
            }
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (legend != null) {
            legend.draw((Graphics2D) g, new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        }
    }

    @Override
    protected void onShow() {
        update();
    }

    public static void main(String[] args) throws InterruptedException {
        ColormapPanel cp = new ColormapPanel();
        cp.colormap = Colormap.Flame;
        cp.setScaleMin(0.0);
        cp.setScaleMax(1000.0);
        cp.setLogarithmic(true);
        cp.setPosition(Position.BOTTOM);

        // Create legend
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(cp, BorderLayout.EAST);

        SwingUtils.showDialog(null, "", new Dimension(800, 600), panel);

        Thread.sleep(3000);
        cp.setLogarithmic(false);

    }

}
