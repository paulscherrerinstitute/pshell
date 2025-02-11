package ch.psi.pshell.plot;

import javax.swing.JPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.pshell.plot.LinePlotSeries.LinePlotSeriesListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.List;
import javax.swing.JDialog;

/**
 *
 */
abstract public class LinePlotBase extends PlotBase<LinePlotSeries> implements LinePlot {

    Style style;

    protected LinePlotBase() {
        this(null);
    }

    protected LinePlotBase(String title) {
        super(LinePlotSeries.class, title);

    }

    @Override
    protected void createChart() {
        super.createChart();
        createAxis(AxisId.X, "X");
        createAxis(AxisId.Y, "Y");
        createAxis(AxisId.Y2, "");
    }

    //TODO: Improve it to share the same X rows
    @Override
    protected String getDataAsString() {
        StringBuilder str = new StringBuilder(1024);

        int numberSeries = getAllSeries().length;

        String labelX = getAxis(AxisId.X).getLabel();
        if ((labelX == null) || (labelX.isEmpty())) {
            labelX = "X";
        }
        str.append(labelX).append(FIELD_SEPARATOR);
        for (LinePlotSeries series : getAllSeries()) {
            str.append(series.getName()).append(FIELD_SEPARATOR);
        }
        str.append(LINE_SEPARATOR);

        int seriesIndex = 0;
        for (LinePlotSeries series : getAllSeries()) {
            double[][] data = getSeriesData(series);
            for (int i = 0; i < data[0].length; i++) {
                double x = data[0][i];
                double y = data[1][i];
                str.append(getPersistenceValue(x)).append(FIELD_SEPARATOR);
                for (int j = 0; j < seriesIndex; j++) {
                    str.append(String.format("NaN", x)).append(FIELD_SEPARATOR);
                }
                str.append(getPersistenceValue(y)).append(FIELD_SEPARATOR);
                for (int j = seriesIndex + 1; j < numberSeries; j++) {
                    str.append(String.format("NaN", x)).append(FIELD_SEPARATOR);
                }
                str.append(LINE_SEPARATOR);
            }
            seriesIndex++;
        }
        return str.toString();
    }

   final LinePlotSeriesListener seriesListener = new LinePlotSeriesListener() {
        @Override
        public void onSeriesAppendData(LinePlotSeries series, double x, double y) {
            onAppendData(series, x, y);
            if (getRequireUpdateOnAppend() && isUpdatesEnabled()) {
                requestSeriesUpdate(series);
            }
        }
        
        @Override
        public void onSeriesAppendData(LinePlotSeries series, double[] x, double[] y){
            int length = Math.min(x.length, y.length);
            for (int i=0; i<length; i++){
                 onAppendData(series, x[i], y[i]);
            }
            if (getRequireUpdateOnAppend() && isUpdatesEnabled()) {
                requestSeriesUpdate(series);
            }            
        }
        
        @Override
        public void onSeriesAppendData(LinePlotSeries series, List<? extends Number> x, List<? extends Number> y){
            int length = Math.min(x.size(), y.size());
            for (int i=0; i<length; i++){
                Number vx = x.get(i);
                Number vy = y.get(i);
                if ((vx!=null) && (vy!=null)){
                    onAppendData(series, vx.doubleValue(), vy.doubleValue());
                }
            }
            if (getRequireUpdateOnAppend() && isUpdatesEnabled()) {
                requestSeriesUpdate(series);
            }          
        }
        
        
        @Override
        public void onSeriesSetData(LinePlotSeries series, double[] x, double[] y) {
            if (y == null) {
                y = new double[0];
            }
            //if ((x == null) || (y.length != x.length)) {
            //    x = new double[y.length];
            //    for (int i = 0; i < x.length; i++) {
            //        x[i] = i;
            //    }
            //}
            onSetData(series, x, y);
            if (isUpdatesEnabled()) {
                requestSeriesUpdate(series);
            }
        }
    };

    protected Color getSeriesColor(LinePlotSeries series) {
        return null;
    }

    protected void setSeriesColor(LinePlotSeries series, Color color) {
    }

    protected void setLinesVisible(LinePlotSeries series, boolean value) {
    }

    protected void setLineWidth(LinePlotSeries series, int value) {
    }

    protected void setPointsVisible(LinePlotSeries series, boolean value) {
    }

    protected void setPointSize(LinePlotSeries series, int value) {
    }

    protected void setMaxItemCount(LinePlotSeries series, int value) {
    }

    /**
     * Returns a token to reference underlying data from LinePlotSeries
     */
    abstract protected void onAppendData(LinePlotSeries series, double x, double y);

    /**
     * Implementations must take care of automatic x axis generation (x==null)
     */
    abstract protected void onSetData(LinePlotSeries series, double[] x, double[] y);

    @Override
    public void detach(String className) {
        try {
            LinePlot p = (LinePlot) Plot.newPlot(className);
            p.setTitle(getTitle());
            if (getTitleFont() != null) {
                p.setTitleFont(getTitleFont());
            }
            p.getAxis(AxisId.X).setLabel(getAxis(AxisId.X).getLabel());
            p.getAxis(AxisId.Y).setLabel(getAxis(AxisId.Y).getLabel());
            if ((getAxis(AxisId.Y2).getLabel() != null) && (!getAxis(AxisId.Y2).getLabel().isEmpty())) {
                p.getAxis(AxisId.Y2).setLabel(getAxis(AxisId.Y2).getLabel());
            }
            for (LinePlotSeries series : getAllSeries()) {
                LinePlotSeries detachedSeries = new LinePlotSeries(series.getName(), series.getColor(), series.getAxisY());
                p.addSeries(detachedSeries);
                detachedSeries.setData(series.getX(), series.getY());
            }
            ((JPanel) p).setPreferredSize(new Dimension(DETACHED_WIDTH, DETACHED_HEIGHT));
            Frame frame = getFrame();
            JDialog dlg = new JDialog(frame, getTitle(), false);
            dlg.setContentPane((JPanel) p);
            dlg.pack();
            SwingUtils.centerComponent(frame, dlg);
            dlg.setVisible(true);
            dlg.requestFocus();
        } catch (Exception ex) {
            showException(ex);
        }
    }

    //Injecting specific entries in popup menu
    @Override
    protected void createPopupMenu() {
        super.createPopupMenu();
    }

    @Override
    public String toString() {
        return LinePlot.class.getSimpleName();
    }
}
