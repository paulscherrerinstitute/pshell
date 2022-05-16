package ch.psi.pshell.xscan.plot;

import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotSeries;

public interface SeriesDataFilter {
	/**
	 * Get the plot for the filter
	 * @return Plot
	 */
	public Plot getPlot();
        public PlotSeries getSeries();
}
