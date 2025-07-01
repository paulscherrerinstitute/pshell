package ch.psi.pshell.plot;

import java.util.logging.Logger;

/**
 * Data representation for MatrixPlot.
 */
public class SlicePlotSeries extends MatrixPlotSeries {

    private static final Logger logger = Logger.getLogger(SlicePlotSeries.class.getName());

    private int numberOfBinsZ;
    private double minZ = Double.NaN;
    private double maxZ = Double.NaN;
    private double binWidthZ;
    private SlicePlot plot;

    /**
     * The listener interface for receiving slice plot series events.
     */
    public interface SlicePlotSeriesListener {

        void onPageChanged(SlicePlotSeries series, int page);

    }

    SlicePlotSeriesListener listener;

    public void setListener(SlicePlotSeriesListener pageChangeListener) {
        this.listener = pageChangeListener;
        setPage(0);
    }

    public SlicePlotSeries(String name) {
        super(name);
    }

    public SlicePlotSeries(String name, double minX, double maxX, int nX, double minY, double maxY, int nY, double minZ, double maxZ, int nZ) {
        super(name, minX, maxX, nX, minY, maxY, nY);
        this.numberOfBinsZ = nZ;
        setRangeZ(minZ, maxZ);
    }

    void updateBinWidth() {
        if (numberOfBinsZ == 0) {
            binWidthZ = 0;
        } else if (numberOfBinsZ == 1) {
            binWidthZ = 1;
        } else if (!Double.isNaN(minZ) && !Double.isNaN(maxZ)) {
            binWidthZ = (this.maxZ - this.minZ) / (double) (numberOfBinsZ - 1);
        } else {
            binWidthZ = 1;
        }
    }

    public void setRangeZ(double minZ, double maxZ) {
        if (maxZ < minZ) {
            this.minZ = maxZ;
            this.maxZ = minZ;
        } else {
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
        updateBinWidth();
        if (plot != null) {
            ((SlicePlotBase) plot).onSeriesRangeZChanged(this);
        }
    }

    void setSlicePlot(SlicePlot plot) {
        this.plot = plot;
    }

    SlicePlot getSlicePlot() {
        return plot;
    }

    public double getBinWidthZ() {
        return binWidthZ;
    }

    public double getMinZ() {
        return Double.isNaN(minZ) ? 0 : minZ;
    }

    public double getMaxZ() {
        return Double.isNaN(maxZ) ? Math.max(numberOfBinsZ - 1, 1) : maxZ;
    }

    public boolean hasRangeZ() {
        return !Double.isNaN(minZ) && !Double.isNaN(maxZ);
    }

    public void setNumberOfBinsZ(int bins) {
        this.numberOfBinsZ = bins;
        updateBinWidth();
        if (plot != null) {
            ((SlicePlotBase) plot).onSeriesRangeZChanged(this);
        }
    }

    public int getNumberOfBinsZ() {
        return numberOfBinsZ;
    }

    int currentPage = -1;

    public void setPage(int page) {
        currentPage = page;
        if ((page < 0) || (page >= numberOfBinsZ) || (listener == null)) {
            setData(null);
        } else {
            listener.onPageChanged(this, page);
        }
    }

    public int getPage() {
        return currentPage;
    }

    public double getZ(int page) {
        if ((page < 0) || (page >= numberOfBinsZ)) {
            return Double.NaN;
        }
        return getMinZ() + getBinWidthZ() * page;
    }

}
