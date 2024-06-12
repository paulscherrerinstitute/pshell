package ch.psi.pshell.plot;

import java.awt.Color;
import java.util.List;

/**
 * Data representation for LinePlot.
 */
public class LinePlotSeries extends PlotSeries<LinePlot> {

    /**
     * The listener interface for receiving line plot series events.
     */
    public interface LinePlotSeriesListener {

        void onSeriesSetData(LinePlotSeries series, double[] x, double[] y);

        void onSeriesAppendData(LinePlotSeries series, double x, double y);
        
        default void onSeriesAppendData(LinePlotSeries series, double[] x, double[] y){
            for (int i=0; i< x.length; i++){
                onSeriesAppendData(series, x, y);
            }
        }
        
        default void onSeriesAppendData(LinePlotSeries series, List<? extends Number> x, List<? extends Number> y){
            for (int i=0; i<x.size(); i++){
                onSeriesAppendData(series, x.get(i).doubleValue(), y.get(i).doubleValue());
            }
        }
        
    }

    public LinePlotSeries(String name) {
        this(name, /*SwingUtils.generateRandomColor()*/ null);
    }

    public LinePlotSeries(String name, Color color) {
        this(name, color, 1);
    }

    public LinePlotSeries(String name, Color color, int axisY) {
        super(name);
        this.color = color;
        this.showLines = true;
        this.showPoints = true;
        this.axisY = axisY;
    }

    private Color color;
    private boolean showLines;
    private boolean showPoints;
    private int pointSize;
    private int lineWidth;
    final private int axisY;

    public Color getColor() {
        if ((color == null) && (getPlot() != null)) {
            return ((LinePlotBase) getPlot()).getSeriesColor(this);
        }
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        if (getPlot() != null) {
            ((LinePlotBase) getPlot()).setSeriesColor(this, color);
        }
    }

    public void setLinesVisible(boolean value) {
        showLines = value;
        if (getPlot() != null) {
            ((LinePlotBase) getPlot()).setLinesVisible(this, value);
        }
    }

    public boolean getLinesVisible() {
        return showLines;
    }

    public void setLineWidth(int value) {
        lineWidth = value;
        if (getPlot() != null) {
            ((LinePlotBase) getPlot()).setLineWidth(this, value);
        }
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setPointsVisible(boolean value) {
        showPoints = value;
        if (getPlot() != null) {
            ((LinePlotBase) getPlot()).setPointsVisible(this, value);
        }
    }

    public boolean getPointsVisible() {
        return showPoints;
    }

    public void setPointSize(int value) {
        pointSize = value;
        if (getPlot() != null) {
            ((LinePlotBase) getPlot()).setPointSize(this, value);
        }
    }

    public int getPointSize() {
        if (pointSize <= 0) {
            return PlotBase.getMarkerSize();
        }
        return pointSize;
    }

    public int getAxisY() {
        return axisY;
    }

    public void setData(double[] y) {
        //double[] x = new double[y.length];
        //for (int i=0;i<y.length;i++){
        //    x[i]=i;
        //}
        setData(null, y);
    }

    public void setData(double[] x, double[] y) {
        Plot plot = getPlot();
        if (plot != null) {
            ((LinePlotBase) plot).seriesListener.onSeriesSetData(this, x, y);
        }
    }

    public void appendData(double x, double y) {
        Plot plot = getPlot();
        if (plot != null) {
            ((LinePlotBase) plot).seriesListener.onSeriesAppendData(this, x, y);          
        }
    }

    public void appendData(double[] x, double[] y) {
        Plot plot = getPlot();
        if (plot != null) {
            ((LinePlotBase) plot).seriesListener.onSeriesAppendData(this, x, y);          
        }
    }
    
    public void appendData(List<? extends Number> x, List<? extends Number> y) {
        Plot plot = getPlot();
        if (plot != null) {
            ((LinePlotBase) plot).seriesListener.onSeriesAppendData(this, x, y);          
        }
    }

    @Override
    public void clear() {
        setData(new double[0], new double[0]);
    }

    public double[] getX() {
        if (getPlot() == null) {
            return new double[0];
        }
        return getPlot().getSeriesData(this)[0];
    }

    public double[] getY() {
        if (getPlot() == null) {
            return new double[0];
        }
        return getPlot().getSeriesData(this)[1];
    }

    public int getCount() {
        if (getPlot() == null) {
            return 0;
        }
        return getPlot().getSeriesData(this)[0].length;
    }

    private int maxItemCount = -1;

    public int getMaxItemCount() {
        return maxItemCount;
    }

    public void setMaxItemCount(int value) {
        maxItemCount = value;
        if (getPlot() != null) {
            ((LinePlotBase) getPlot()).setMaxItemCount(this, value);
        }        
    }

    //Tools
    public double getAverage() {
        double average = 0.0;
        for (double y : getY()) {
            average = average + y;
        }
        return average / getCount();
    }

    public double[] getMinimum() {
        double min = Double.NaN;
        double xmin = Double.NaN;
        double[][] data = getPlot().getSeriesData(this);
        double[] x = data[0];
        double[] y = data[1];

        for (int i = 0; i < x.length; i++) {
            if (Double.isNaN(min) || (y[i] < min)) {
                min = y[i];
                xmin = x[i];
            }
        }
        return new double[]{xmin, min};
    }

    public double[] getMaximum() {
        double max = Double.NaN;
        double xmax = Double.NaN;
        double[][] data = getPlot().getSeriesData(this);
        double[] x = data[0];
        double[] y = data[1];

        for (int i = 0; i < x.length; i++) {
            if (Double.isNaN(max) || (y[i] > max)) {
                max = y[i];
                xmax = x[i];
            }
        }
        return new double[]{xmax, max};
    }

    public double[][] getDerivative() {
        if (getCount() <= 2) {
            return new double[][]{new double[0], new double[0]};
        }
        double[][] data = getPlot().getSeriesData(this);
        double[] x = data[0];
        double[] y = data[1];

        double[] dx = new double[getCount() - 2];
        double[] dy = new double[getCount() - 2];

        for (int i = 1; i < getCount() - 1; i++) { // do not start at 0 but 1 - stop 1 before end
            double xi = x[i];
            double ximinus1 = x[i - 1];
            double xiplus1 = x[i + 1];
            double yiminus1 = y[i - 1];
            double yiplus1 = y[i + 1];

            if (xiplus1 - ximinus1 != 0.0) {
                double di = (yiplus1 - yiminus1) / (xiplus1 - ximinus1);
                dx[i - 1] = xi;
                dy[i - 1] = di;
            } else {
                dx[i - 1] = xi;
                dy[i - 1] = Double.NaN;
            }
        }

        return new double[][]{dx, dy};
    }

    public double[][] getIntegral() {
        if (getCount() <= 1) {
            return new double[][]{new double[0], new double[0]};
        }
        double[][] data = getPlot().getSeriesData(this);
        double[] x = data[0];
        double[] y = data[1];

        double[] ix = new double[getCount() - 1];
        double[] iy = new double[getCount() - 1];
        double value = 0.0;

        for (int i = 1; i < getCount(); i++) { // Leave out 1. point 
            value += (x[i] - x[i - 1]) * (y[i] + y[i - 1]) / 2.0;
            ix[i - 1] = x[i];
            iy[i - 1] = value;
        }
        return new double[][]{ix, iy};
    }

}
