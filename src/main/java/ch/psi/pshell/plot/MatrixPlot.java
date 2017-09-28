package ch.psi.pshell.plot;

/**
 *
 */
public interface MatrixPlot extends Plot<MatrixPlotSeries> {

    public double[][] getSeriesX(final MatrixPlotSeries series);

    public double[][] getSeriesY(final MatrixPlotSeries series);

    public enum Style {
        Normal,
        Mesh,
        Image,
    }

}
