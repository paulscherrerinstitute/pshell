package ch.psi.pshell.plot;

import ch.psi.pshell.plot.Plot.AxisId;

/**
 *
 */
public class Axis {

    PlotBase plot;
    AxisId id;
    boolean logarithmic;

    Axis(String label, PlotBase plot, AxisId id) {
        this.plot = plot;
        this.id = id;
        if (label == null) {
            label = "";
        }
        this.label = label;
    }

    String label = "";

    public String getLabel() {
        return label;
    }

    public AxisId getId() {
        return id;
    }

    public Plot getPlot() {
        return plot;
    }

    public void setLabel(String value) {
        label = value;
        plot.onAxisLabelChanged(id);
    }

    double min;

    public double getMin() {
        return min;
    }

    public void setMin(double value) {
        min = value;
        plot.onAxisRangeChanged(id);
    }

    double max;

    public double getMax() {
        return max;
    }

    public void setMax(double value) {
        max = value;
        plot.onAxisRangeChanged(id);
    }

    public void setRange(double min, double max) {
        this.min = min;
        this.max = max;
        plot.onAxisRangeChanged(id);
    }

    public boolean isAutoRange() {
        return (max <= min);
    }

    public void setAutoRange() {
        setRange(0, 0);
    }

    public boolean isLogarithmic() {
        return logarithmic;
    }

    public void setLogarithmic(boolean logarithmic) {
        if (this.logarithmic != logarithmic) {
            this.logarithmic = logarithmic;
            plot.onAxisRangeChanged(id);
        }
    }

    boolean inverted;

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean value) {
        inverted = value;
        plot.onAxisRangeChanged(id);
    }
}
