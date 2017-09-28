package ch.psi.pshell.plot;

import ch.psi.utils.Reflection.Hidden;

/**
 *
 */
abstract public class SlicePlotBase extends PlotBase<SlicePlotSeries> implements SlicePlot {

    protected SlicePlotBase() {
        this(null);
    }

    protected SlicePlotBase(String title) {
        super(SlicePlotSeries.class, title);
    }

    @Override
    protected void createChart() {
        super.createChart();
        createAxis(AxisId.X, "X");
        createAxis(AxisId.Y, "Y");
        createAxis(AxisId.Z, "Z");
    }

    //Only supporting one series for now so let's gain some speed    
    @Override
    public void addSeries(SlicePlotSeries series) {
        if (getAllSeries().length > 0) {
            removeSeries(getAllSeries()[0]);
        }
        super.addSeries(series);
        series.setSlicePlot(this);
        series.setPage(0);
    }

    @Override
    protected String getDataAsString() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void detach(String className) {
        throw new UnsupportedOperationException();
    }

    //Injecting specific entries in popup menu
    @Override
    protected void createPopupMenu() {
    }

    protected void onSeriesRangeZChanged(SlicePlotSeries s) {
    }

    // Known Implemenmtations
    @Hidden
    public static SlicePlot newPlot(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return (SlicePlot) PlotBase.newPlot(className);
    }

    @Override
    public String toString() {
        return SlicePlot.class.getSimpleName();
    }
}
