package ch.psi.pshell.plot;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Data representation for MatrixPlot.
 */
public class MatrixPlotSeries extends PlotSeries<MatrixPlot> {

    /**
     * The listener interface for receiving matrix plot series events.
     */
    public interface MatrixPlotSeriesListener {

        void onSeriesSetData(MatrixPlotSeries series, double[][] data, double[][] x, double[][] y);

        void onSeriesAppendData(MatrixPlotSeries series, double x, double y, double z);

    }

    private static final Logger logger = Logger.getLogger(MatrixPlotSeries.class.getName());

    private int numberOfBinsX;
    private double minX;
    private double maxX;
    private double binWidthX;

    private int numberOfBinsY;
    private double minY;
    private double maxY;
    private double binWidthY;

    public MatrixPlotSeries(String name) {
        super(name);
    }
    
    public MatrixPlotSeries(String name, Dimension size) {
        this(name, size.width, size.height);
    }
    
    public MatrixPlotSeries(String name, int width, int height) {
        this(name, 0, width-1, width,0, height-1, height);
    }

    public MatrixPlotSeries(String name, double minX, double maxX, int nX, double minY, double maxY, int nY) {
        this(name);
        setNumberOfBinsX(nX);
        setNumberOfBinsY(nY);
        setRangeX(minX, maxX);
        setRangeY(minY, maxY);
    }

    public void setData(double[][] data) {
        setData(data, null, null);
    }

    public void setData(double[][] data, double[][] x, double[][] y) {
        MatrixPlot plot = getPlot();
        if ((data != null) && (data.length > 0)) {
            //Late initialization
            if ((numberOfBinsX == 0) || (numberOfBinsY == 0)) {
                setNumberOfBinsX(data[0].length);
                setRangeX(0, data[0].length);
                setNumberOfBinsY(data.length);
                setRangeY(0, data.length);
            }
            if (plot != null) {
                setToken(((MatrixPlotBase) plot).onAddedSeries(this));
            }
        }

        if ((data == null) || (data.length != numberOfBinsY) || (data[0].length != numberOfBinsX)) {
            data = new double[numberOfBinsY][numberOfBinsX];
            for (int i = 0; i < numberOfBinsY; i++) {
                Arrays.fill(data[i], Double.NaN);
            }
        }
        if (plot != null) {
            ((MatrixPlotBase) plot).seriesListener.onSeriesSetData(this, data, x, y);
        }
    }

    public void setNumberOfBinsX(int bins) {
        this.numberOfBinsX = bins;
    }

    public void setNumberOfBinsY(int bins) {
        this.numberOfBinsY = bins;
    }

    public void setRangeX(double minX, double maxX) {
        if (maxX < minX) {
            this.minX = maxX;
            this.maxX = minX;
        } else {
            this.minX = minX;
            this.maxX = maxX;
        }
        if (numberOfBinsX == 0) {
            binWidthX = 0;
        } else if (numberOfBinsX == 1) {
            binWidthX = 1;
        } else {
            binWidthX = (this.maxX - this.minX) / (double) (numberOfBinsX - 1);
        }
    }

    public void setRangeY(double minY, double maxY) {
        if (maxY < minY) {
            this.minY = maxY;
            this.maxY = minY;
        } else {
            this.minY = minY;
            this.maxY = maxY;
        }
        if (numberOfBinsY == 0) {
            binWidthY = 0;
        } else if (numberOfBinsY == 1) {
            binWidthY = 1;
        } else {
            binWidthY = (this.maxY - this.minY) / (double) (numberOfBinsY - 1);
        }
    }

    public void appendData(double x, double y, double z) {
        if (getPlot() != null) {
            ((MatrixPlotBase) getPlot()).seriesListener.onSeriesAppendData(this, x, y, z);
        }

    }

    public double[][] getData() {
        if (getPlot() == null) {
            return new double[0][0];
        }
        return getPlot().getSeriesData(this);
    }

    public double[][] getX() {
        if (getPlot() == null) {
            return new double[0][0];
        }
        return getPlot().getSeriesX(this);
    }

    public double[][] getY() {
        if (getPlot() == null) {
            return new double[0][0];
        }
        return getPlot().getSeriesY(this);
    }

    @Override
    public void clear() {
        setData(null);
    }

    /**
     * @return the binWidthX
     */
    public double getBinWidthX() {
        return binWidthX;
    }

    public void setBinWidthX(double value) {
        binWidthX = value;
    }

    /**
     * @return the binWidthY
     */
    public double getBinWidthY() {
        return binWidthY;
    }

    public void setBinWidthY(double value) {
        binWidthY = value;
    }

    /**
     * @return the minX
     */
    public double getMinX() {
        return minX;
    }

    /**
     * @return the maxX
     */
    public double getMaxX() {
        return maxX;
    }

    /**
     * @return the minY
     */
    public double getMinY() {
        return minY;
    }

    /**
     * @return the maxY
     */
    public double getMaxY() {
        return maxY;
    }

    /**
     * @return the numberOfBinsX
     */
    public int getNumberOfBinsX() {
        return numberOfBinsX;
    }

    /**
     * @return the numberOfBinsY
     */
    public int getNumberOfBinsY() {
        return numberOfBinsY;
    }

    //Utilities
    public boolean contains(int indexX, int indexY) {
        return ((indexX < numberOfBinsX) && (indexX >= 0) && (indexY < numberOfBinsY) && (indexY >= 0));

    }

    public double[] minMaxZValue() {

        double min = Double.NaN;
        double max = Double.NaN;

        double[][] data = getData();
        for (double[] row : data) {
            for (double val : row) {
                if ((!Double.isNaN(val)) && (!Double.isInfinite(val))) {
                    if (Double.isNaN(min) || (val < min)) {
                        min = val;
                    }
                    if (Double.isNaN(max) || (val > max)) {
                        max = val;
                    }
                }
            }
        }
        return new double[]{min, max};
    }

}
