package ch.psi.pshell.plot;

import java.awt.Color;

/**
 *
 */
public class TimePlotSeries extends PlotSeries<TimePlot> {

    public Color color = Color.BLUE;
    public int axis = 1;

    public TimePlotSeries(String name) {
        super(name);
    }

    public TimePlotSeries(String name, int axis) {
        super(name);
        this.axis = axis;
    }

    public TimePlotSeries(String name, Color color) {
        super(name);
        this.color = color;
    }

    public TimePlotSeries(String name, Color color, int axis) {
        super(name);
        this.color = color;
        this.axis = axis;
    }

    public Color getColor() {
        if ((color == null) && (getPlot() != null)) {
            return ((TimePlotBase) getPlot()).getSeriesColor(this);
        }
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        if (getPlot() != null) {
            ((TimePlotBase) getPlot()).setSeriesColor(this, color);
        }
    }

    public int getAxisY() {
        return axis;
    }

    public int getIndex() {
        return ((TimePlotBase) getPlot()).getIndex(this);
    }

    public void appendTerminator() {
        ((TimePlotBase) getPlot()).addTerminator(getIndex());
    }

    public void appendData(double value) {
        ((TimePlotBase) getPlot()).add(getIndex(), value);
    }

    public void appendData(long time, double value) {
        ((TimePlotBase) getPlot()).add(getIndex(), time, value);
    }

}
