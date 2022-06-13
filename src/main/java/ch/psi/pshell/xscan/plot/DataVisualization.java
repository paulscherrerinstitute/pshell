package ch.psi.pshell.xscan.plot;

import ch.psi.pshell.plot.Plot;
import java.util.ArrayList;
import java.util.List;

public class DataVisualization {

    private final Plot plot;
    private final List<XYSeriesDataFilter> series;

    public DataVisualization(Plot plot) {
        this.plot = plot;
        this.series = new ArrayList<XYSeriesDataFilter>();
    }

    /**
     * @return the plot
     */
    public Plot getPlot() {
        return plot;
    }

    /**
     * @return the series
     */
    public List<XYSeriesDataFilter> getSeries() {
        return series;
    }

}
