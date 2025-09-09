package ch.psi.pshell.plot;

import ch.psi.pshell.data.DataSlice;
import ch.psi.pshell.data.DataStore;
import ch.psi.pshell.data.PlotDescriptor;
import ch.psi.pshell.plot.LinePlot.Style;
import ch.psi.pshell.plot.Plot.Quality;
import ch.psi.pshell.extension.Extensions;
import ch.psi.pshell.scripting.ViewPreference.PlotPreferences;
import ch.psi.pshell.swing.MonitoredPanel;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;

/**
 *
 */
public class PlotPanel extends MonitoredPanel {

    public static final String PROPERTY_PLOT_IMPL_LINE = "ch.psi.pshell.plot.impl.line";
    public static final String PROPERTY_PLOT_IMPL_MATRIX = "ch.psi.pshell.plot.impl.matrix";
    public static final String PROPERTY_PLOT_IMPL_SURFACE = "ch.psi.pshell.plot.impl.surface";
    public static final String PROPERTY_PLOT_IMPL_TIME = "ch.psi.pshell.plot.impl.time";
    public static final String PROPERTY_PLOT_QUALITY = "ch.psi.pshell.plot.quality";
    public static final String PROPERTY_PLOT_LAYOUT = "ch.psi.pshell.plot.layout";

    static public Class DEFAULT_PLOT_IMPL_LINE = ch.psi.pshell.plot.LinePlotJFree.class;
    static public Class DEFAULT_PLOT_IMPL_MATRIX = ch.psi.pshell.plot.MatrixPlotJFree.class;
    static public Class DEFAULT_PLOT_IMPL_SLICE = ch.psi.pshell.plot.SlicePlotDefault.class;
    static public Quality DEFAULT_PLOT_QUALITY = Quality.High;
    static public PlotLayout DEFAULT_PLOT_LAYOUT = PlotLayout.Vertical;

    static Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final int DEFAULT_RANGE_STEPS = 199;

    String plotTitle;

    protected PlotPreferences prefs;
    protected boolean changedScaleX;
    final protected ArrayList<Plot> plots;
    DataStore dataManager;

    public PlotPanel() {
        initComponents();
        LayoutManager layout = new GridBagLayout();
        pnGraphs.setLayout(layout);
        plots = new ArrayList<>();
        //scanRecordBuffer = new ArrayList<>();
        //readableIndexes = new ArrayList<>();
        //writableIndexes = new ArrayList<>();        
        prefs = new PlotPreferences();
    }
    
    public static void setTitleFont(Font font) {
        TITLE_FONT = font;
    }

    public static  Font getTitleFont() {
        return TITLE_FONT;
    }    

    static public String getLinePlotImpl() {
        String impl = System.getProperty(PROPERTY_PLOT_IMPL_LINE);
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? DEFAULT_PLOT_IMPL_LINE.getName() : impl;
    }

    static public String getMatrixPlotImpl() {
        String impl = System.getProperty(PROPERTY_PLOT_IMPL_MATRIX);
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? DEFAULT_PLOT_IMPL_MATRIX.getName() : impl;
    }

    static public String getSurfacePlotImpl() {
        String impl = System.getProperty(PROPERTY_PLOT_IMPL_SURFACE);
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? null : impl;
    }

    static public String getSlicePlotImpl() {
        String impl = System.getProperty("ch.psi.pshell.plot.impl.slice");
        return ((impl == null) || (impl.isEmpty()) || (impl.equals(String.valueOf((Object) null))))
                ? DEFAULT_PLOT_IMPL_SLICE.getName() : impl;
    }

    static public Quality getQuality() {
        try {
            return Quality.valueOf(System.getProperty(PROPERTY_PLOT_QUALITY));
        } catch (Exception ex) {
            return DEFAULT_PLOT_QUALITY;
        }
    }

    static public PlotLayout getDefaultLayout() {
        try{
            return PlotLayout.valueOf(System.getProperty(PROPERTY_PLOT_LAYOUT));
        } catch (Exception ex) {
            return DEFAULT_PLOT_LAYOUT;
        }
    }    
    
    public PlotLayout getPlotLayout() {
        if (prefs.plotLayout != null) {
            return prefs.plotLayout;
        }
        return getDefaultLayout();
    }    

    public void setPlotTitle(String plotTitle) {
        this.plotTitle = plotTitle;
    }
    
    public String getPlotTitle() {
        return plotTitle;
    }

    public List<Plot> getPlots() {
        return (List<Plot>) plots.clone();
    }
   
    public void initialize() {
        //Redoing layout because it may have been changed by View's initComponents()
        removeAll();
        setLayout(new BorderLayout());
        add(scrollPane);

        //setActive(false);
    }

    public PlotPreferences getPreferences() {
        return prefs;
    }

    public void setPreferences(PlotPreferences preferences) {
        prefs = preferences.clone();
    }
     
    public void clear() {
        plots.clear();
        if (!Plot.isOffscreen()) {
            pnGraphs.removeAll();
            panelIndexY = 0;
            panelIndexX = 0;
            validate();
            repaint();
        }
    }

    int panelIndexX = 0;
    int panelIndexY = 0;

    public void addPlot(PlotBase plot) {
        plot.setBackground(getBackground());
        plot.setTitleFont(TITLE_FONT);
        plot.setQuality(getQuality());
        plots.add(plot);

        if (!Plot.isOffscreen()) {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.gridx = panelIndexX;
            c.gridy = panelIndexY;

            switch (getPlotLayout()) {
                case Horizontal -> panelIndexX++;
                case Vertical -> panelIndexY++;
                default -> {
                    panelIndexX++;
                    if (panelIndexX > 1) {
                        panelIndexX = 0;
                        panelIndexY++;
                    }
                }
            }
            plot.setVisible(true);
            pnGraphs.add(plot, c);
        }
    }
    
    protected Class getPlotClass(Object plotType) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (plotType != null) {
            if (plotType instanceof Number number) {
                switch (number.intValue()) {
                    case 1 -> plotType = PlotPanel.getLinePlotImpl();
                    case 2 -> plotType = PlotPanel.getMatrixPlotImpl();
                    case 3 -> plotType = PlotPanel.getSlicePlotImpl();
                }
            }
            if (plotType instanceof String str) {                  
                plotType = Extensions.getClass(str);
            }
            if (plotType instanceof Class cls) {
                return cls;
            }
        }
        return null;
    }


    Plot newPlot(String name, boolean isScan, int dim, boolean allowLowerDim) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Plot requestedPlot = null;
        try {
            if (isScan && (prefs.plotTypes != null)) {
                Class type = getPlotClass(prefs.plotTypes.get(name));
                //If device name matches a Cacheable cache name, use the rule for the parent  
                if (type != null) {
                    requestedPlot = (Plot) type.newInstance();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
        }

        if (dim == 3) {
            return Plot.newPlot(getSlicePlotImpl());
        }

        if (dim == 2) {
            if ((requestedPlot != null)) {
                if (allowLowerDim && (requestedPlot instanceof LinePlot)) {
                    return requestedPlot;
                } else if (requestedPlot instanceof MatrixPlot) {
                    return requestedPlot;
                }
            }
            return Plot.newPlot(getMatrixPlotImpl());
        }
        if (requestedPlot instanceof LinePlot) {
            return requestedPlot;
        }
        return Plot.newPlot(getLinePlotImpl());
    }

    protected Plot addPlot(String name, boolean isScan, String labelX, int rank, int[] recordSize, double[] start, double[] end, int[] steps, boolean hasStats) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Plot plot = null;
        int recordDimensions = (recordSize == null) ? 0 : recordSize.length;

        if (rank == 3) {
            //Don't use slice plot during scan
            if (isScan) {
                plot = newPlot(name, isScan, 2, true);
                if (plot instanceof LinePlotBase) {
                    if ((start != null) && (start.length > 0) && (end != null) && (end.length > 0)) {
                        if (prefs.range != null) {
                            plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        } else if ((prefs.autoRange == null) || (!prefs.autoRange)) {
                            plot.getAxis(Plot.AxisId.X).setRange(Math.min(start[0], end[0]), Math.max(start[0], end[0]));
                        }
                    }
                    plot.getAxis(Plot.AxisId.X).setLabel(labelX);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    plot.addSeries(new LinePlotSeries(name));
                } else {
                    MatrixPlotSeries series = null;
                    if (recordDimensions == 0) {
                        series = new MatrixPlotSeries(name, start[2], end[2], steps[2] + 1, start[1], end[1], steps[1] + 1);
                    } else if (recordDimensions == 1) {
                        int ySize = recordSize[0];
                        series = new MatrixPlotSeries(name, start[0], end[0], steps[0] + 1, 0, ySize - 1, ySize);
                    } else if (recordDimensions == 2) {
                        series = new MatrixPlotSeries(name, recordSize[0], recordSize[1]);
                    } else {
                        return null;
                    }
                    
                    if (prefs.range != null) {
                        if (series.getNumberOfBinsX()==0){
                            series.setNumberOfBinsX(DEFAULT_RANGE_STEPS);
                        }
                        plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        series.setRangeX(prefs.range.min, prefs.range.max);                        
                    }
                    if (prefs.rangeY != null) {
                        if (series.getNumberOfBinsY()==0){
                            series.setNumberOfBinsY(DEFAULT_RANGE_STEPS);
                        }
                        plot.getAxis(Plot.AxisId.Y).setRange(prefs.rangeY.min, prefs.rangeY.max);                        
                        series.setRangeY(prefs.rangeY.min, prefs.rangeY.max);
                    }                    
                        //}                
                    plot.addSeries(series);
                
                }
            } else {
                if ((recordSize == null) || (recordSize[0] == 1)) {

                    plot = newPlot(name, isScan, 3, false);
                    SlicePlotSeries series = new SlicePlotSeries(name, start[1], end[1], steps[1] + 1, start[2], end[2], steps[2] + 1, start[0], end[0], steps[0] + 1);
                    plot.addSeries(series);
                }
            }
        } else if (rank == 2) {
            if (recordDimensions == 2) {
                int xSize = recordSize[0];
                int ySize = recordSize[1];
                if ((start == null) || (start.length != 2)) {
                    start = new double[]{0, 0};
                }
                if ((end == null) || (end.length != 2)) {
                    end = new double[]{xSize - 1, ySize - 1};
                }

                MatrixPlotSeries series = new MatrixPlotSeries(name, start[0], end[0], xSize, start[1], end[1], ySize);
                plot = newPlot(name, isScan, 2, false);
                plot.addSeries(series);
            } else if (recordDimensions == 1) {
                int ySize = recordSize[0];
                plot = newPlot(name, isScan, 2, true);
                if (plot instanceof LinePlotBase) {
                    plot.getAxis(Plot.AxisId.X).setLabel(labelX);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    LinePlotSeries series = new LinePlotSeries(name);
                    plot.addSeries(series);
                } else {
                    //TODO: should be start[2] /end[2] in some cases?
                    double y_start = (start.length > 2) ? start[1] : 0;
                    double y_end = (end.length > 2) ? end[1] : (ySize - 1);
                    int nX = ((steps[0] < 0)||(steps[0] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : steps[0] + 1;
                    MatrixPlotSeries series = new MatrixPlotSeries(name, (prefs.range != null) ? prefs.range.min : start[0], (prefs.range != null) ? prefs.range.max : end[0], nX,
                            y_start, y_end, ySize);
                    plot.addSeries(series);
                }
            } else {
                plot = newPlot(name, isScan, 2, true);
                if (plot instanceof LinePlotBase) {
                    if ((start != null) && (start.length > 0) && (end != null) && (end.length > 0)) {
                        if (prefs.range != null) {
                            plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        } else if ((prefs.autoRange == null) || (!prefs.autoRange)) {
                            plot.getAxis(Plot.AxisId.X).setRange(Math.min(start[0], end[0]), Math.max(start[0], end[0]));
                        }
                    }
                    plot.getAxis(Plot.AxisId.X).setLabel(labelX);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    double step_size = (steps[1]!=0) ? ((end[1] - start[1]) / (steps[1])) : 0;
                    for (int i = 0; i <= steps[1]; i++) {
                        double y = start[1] + (i * step_size);
                        String seriesName = String.valueOf(Convert.roundDouble(y, 8));
                        while (Arr.containsEqual(plot.getSeriesNames(), seriesName)){
                            seriesName = seriesName + "'";
                        }                
                        LinePlotSeries series = new LinePlotSeries(seriesName);
                        plot.addSeries(series);
                    }
                } else {
                    MatrixPlotSeries series = null;
                    int nX = ((steps[0] < 0)|| (steps[0] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : (steps[0] + 1);
                    int nY = ((steps[1] < 0)|| (steps[1] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : (steps[1] + 1);
                    series = new MatrixPlotSeries(name, start[0], end[0], nX, start[1], end[1], nY);
                    if (prefs.range != null) {
                        plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        if (changedScaleX || ((steps[0] < 0))) {
                            series.setRangeX(prefs.range.min, prefs.range.max);
                        }
                    }
                    if (prefs.rangeY != null) {
                        plot.getAxis(Plot.AxisId.Y).setRange(prefs.rangeY.min, prefs.rangeY.max);
                        if ((steps[1] < 0)) {
                            series.setRangeY(prefs.rangeY.min, prefs.rangeY.max);
                        }
                    }
                    //}
                    plot.addSeries(series);
                }
            }
        }
        if (rank <= 1) {
            if (recordDimensions == 2) {
                plot = newPlot(name, isScan, 2, false);
                int xSize = recordSize[0];
                int ySize = recordSize[1];
                if ((start == null) || (start.length != 2)) {
                    start = new double[]{0, 0};
                }
                if ((end == null) || (end.length != 2)) {
                    end = new double[]{xSize - 1, ySize - 1};
                }

                MatrixPlotSeries series = new MatrixPlotSeries(name, start[0], end[0], xSize, start[1], end[1], ySize);
                plot.addSeries(series);
            } else if (recordDimensions == 1) {
                plot = newPlot(name, isScan, 2, true);
                if (plot instanceof LinePlotBase) {
                    plot.getAxis(Plot.AxisId.X).setLabel(null);
                    plot.getAxis(Plot.AxisId.Y).setLabel(null);
                    LinePlotSeries series = new LinePlotSeries(name);
                    plot.addSeries(series);

                } else {
                    int ySize = recordSize[0];
                    double y_start = (start.length > 1) ? start[1] : 0;
                    double y_end = (end.length > 1) ? end[1] : (ySize - 1);
                    MatrixPlotSeries series = null;
                    int nX = ((steps[0] < 0)||(steps[0] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : steps[0] + 1;
                    series = new MatrixPlotSeries(name, start[0], end[0], nX, y_start, y_end, ySize);
                    if (prefs.range != null) {
                        plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                        if (changedScaleX) {
                            series.setRangeX(prefs.range.min, prefs.range.max);
                        }
                    }
                    plot.addSeries(series);
                }
            } else {
                
                plot = newPlot(name, isScan, 1, true);
                if (hasStats && (plot instanceof LinePlotJFree linePlotJFree)) {
                    linePlotJFree.setStyle(Style.ErrorY);
                    linePlotJFree.addSeries(new LinePlotErrorSeries(name));
                } else {
                    plot.addSeries(new LinePlotSeries(name));
                }
                if (prefs.range != null) {
                    plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                } else if ((prefs.autoRange == null) || (!prefs.autoRange)) {
                    if ((start != null) && (start.length > 0) && (end != null) && (end.length > 0)) {
                        plot.getAxis(Plot.AxisId.X).setRange(Math.min(start[0], end[0]), Math.max(start[0], end[0]));
                    }
                }
                if (prefs.rangeY != null) {
                    plot.getAxis(Plot.AxisId.Y).setRange(prefs.rangeY.min, prefs.rangeY.max);
                }                
                plot.getAxis(Plot.AxisId.X).setLabel(labelX);
                plot.getAxis(Plot.AxisId.Y).setLabel(null);
            }
        }

        if (plot != null) {
            if (plot instanceof MatrixPlotBase matrixPlot) {
                addSurfacePlotMenu(matrixPlot);
            } else if (plot instanceof SlicePlotDefault slicePlot) {
                addSurfacePlotMenu(slicePlot.getMatrixPlot());
            }
            plot.setTitle(name);
            addPlot((PlotBase) plot);
            plot.setUpdatesEnabled(Plot.isOffscreen());
        }
        return plot;
    }

    public static void addSurfacePlotMenu(final MatrixPlotBase plot) {
        if (getSurfacePlotImpl() != null) {
            JMenuItem detachPlotMenuItem = new JMenuItem("Surface Plot");
            detachPlotMenuItem.addActionListener((ActionEvent e) -> {
                plot.detach(getSurfacePlotImpl());
            });
            plot.addPopupMenuItem(detachPlotMenuItem);
        }
    }

    Object validatePlotDataType(Object data) {
        try {
            data = Convert.toDouble(data);
            if (data != null) {
                return data;
            }
        } catch (Exception ex) {
        }
        throw new IllegalArgumentException("Invalid array type: " + data.getClass());
    }

    public Plot addPlot(PlotDescriptor descriptor) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Object data = descriptor.data;
        if (data == null) {
            data = new double[0];
        }
        if (descriptor.unsigned) {
            data = Convert.toUnsigned(data);
        }
        data = validatePlotDataType(data);

        int[] shape = Arr.getShape(data);
        int dimensions = shape.length;
        int rank = descriptor.rank;
        double[] x = descriptor.x;
        double[] y = descriptor.y;
        double[] z = descriptor.z;
        int[] numberSteps = descriptor.steps;
        boolean multidimentional1dDataset = descriptor.isMultidimentional1dArray();
        boolean forceMatrixPlot=false;
        boolean sparseArrayData = false;

        if (rank < 0) {
            rank = dimensions;
        }

        double[] start = null;
        double[] end = null;
        int[] steps = null;

        if (dimensions == 1) {
            //2D-scan with 1d datasets
            if ((numberSteps != null) && (numberSteps.length == 2) && (y != null) && (x != null)
                    && (x.length == y.length) && (((double[]) data).length == x.length)) {
                start = new double[]{(Double) Arr.getMin(x), (Double) Arr.getMin(y)};
                end = new double[]{(Double) Arr.getMax(x), (Double) Arr.getMax(y)};
                steps = numberSteps;
                rank = 2;
                multidimentional1dDataset = true;
            }
        } else if (dimensions == 2) {
            if (rank == 3) {
                //z must be set
                double[][] array = (double[][]) data;
                start = new double[]{z[0], 0, 0};
                end = new double[]{z[z.length - 1], shape[1] - 1, shape[0] - 1};
                steps = new int[]{z.length - 1, array[0].length - 1, array.length - 1};
                if ((x != null) && (x.length > 0)) {
                    start[1] = x[0];
                    end[1] = x[x.length - 1];
                    steps[1] = x.length - 1;
                }
                if ((y != null) && (y.length > 0)) {
                    start[2] = y[0];
                    end[2] = y[y.length - 1];
                    steps[2] = y.length - 1;
                }
            } else {
                //2D-scan with 1d datasets of arrays 
                if (multidimentional1dDataset) {
                    sparseArrayData = (dimensions==2) && (numberSteps[0] < 0) && (numberSteps[1] < 0);
                    // Sparse table: don't try to make 3d plots of array data
                    if (sparseArrayData){
                        //data = Convert.transpose(data);
                        start = new double[]{(Double) Arr.getMin(x), 0};
                        end = new double[]{(Double) Arr.getMax(x), shape[1] - 1};
                        steps = new int[]{-1, shape[1] - 1};
                        rank = 2;
                    } else {
                        start = new double[]{(Double) Arr.getMin(x), (Double) Arr.getMin(y), 0};
                        end = new double[]{(Double) Arr.getMax(x), (Double) Arr.getMax(y), shape[1] - 1};
                        steps = new int[]{numberSteps[0], numberSteps[1], shape[1] - 1};
                        if (numberSteps.length==2){
                            if ((z != null) && (z.length > 0)) {
                                start[2] = z[0];
                                end[2] = z[z.length - 1];
                                steps[2] = z.length - 1;
                            }
                            rank = 3;
                        } else if (numberSteps.length>2){
                            start = new double[]{0, 0};
                            end = new double[]{z.length-1, shape[1] -1};
                            steps = new int[]{z.length-1, shape[1] -1};
                            rank = 2;
                            forceMatrixPlot = true;
                        }
                    }
                } else {
                    start = new double[2];
                    end = new double[2];
                    steps = new int[2];
                    if ((x == null) || (x.length == 0)) {
                        start[0] = 0;
                        end[0] = shape[1] - 1;
                        steps[0] = shape[1] - 1;
                    } else {
                        //TODO:Should create a 3d plot to support multipass scans(overlapping samples)?
                        int length = x.length;
                        if (descriptor.passes > 1) {
                            length /= descriptor.passes;
                        }
                        start[0] = x[0];
                        end[0] = x[length - 1];
                        steps[0] = length - 1;
                    }
                    if ((y == null) || (y.length == 0)) {
                        start[1] = 0;
                        end[1] = shape[0] - 1;
                        steps[1] = shape[0] - 1;
                    } else {
                        start[1] = y[0];
                        end[1] = y[y.length - 1];
                        steps[1] = y.length - 1;
                    }
                }
            }
        } else if (dimensions == 3) {
            double[][][] array = (double[][][]) data;

            start = new double[3];
            start[0] = 0;
            end = new double[3];
            end[0] = array.length - 1;
            steps = new int[3];
            steps[0] = array.length - 1;

            if ((y == null) || (y.length == 0)) {
                start[2] = 0;
                end[2] = shape[1] - 1;
                steps[2] = shape[1] - 1;
            } else {
                start[2] = y[0];
                end[2] = y[y.length - 1];
                steps[2] = y.length - 1;
            }

            if ((x == null) || (x.length == 0)) {
                start[1] = 0;
                end[1] = shape[2] - 1;
                steps[1] = shape[2] - 1;
            } else {
                start[1] = x[0];
                end[1] = x[x.length - 1];
                steps[1] = x.length - 1;
            }
        }

        Plot plot = addPlot(descriptor.name, false, descriptor.labelX, rank, null, start, end, steps, false);
        if (plot != null) {
            if (plot instanceof LinePlotBase linePlot) {
                if ((descriptor.error != null) && (descriptor.error.length == ((double[]) data).length) && (plot instanceof LinePlotJFree)) {
                    linePlot.setStyle(Style.ErrorY);
                    LinePlotErrorSeries series = new LinePlotErrorSeries(descriptor.name);
                    linePlot.addSeries(series);
                    series.setData(x, (double[]) data, descriptor.error);
                } else {
                    if (data instanceof Number){
                        data = new double[]{((Number)data).doubleValue()};
                    }
                    ((LinePlotSeries) plot.getSeries(0)).setData(x, (double[]) data);
                }
            } else if (plot instanceof MatrixPlot matrixPlot) {
                MatrixPlotSeries series = matrixPlot.getSeries(0);
                if (multidimentional1dDataset){
                    if (sparseArrayData){
                        double[][] array = (double[][]) data;
                        //Already checked array sizes                   
                        for (int i = 0; i < array.length; i++) {
                            for (int j = 0; j < array[i].length; j++) {
                                series.appendData(x[i], j, array[i][j]);
                            }
                        }                        
                    } else {
                        if (forceMatrixPlot){
                            double[][] array = (double[][]) data;
                            //Already checked array sizes                   
                            for (int i = 0; i < array.length; i++) {
                                for (int j = 0; j < array[i].length; j++) {
                                    series.appendData(i, j, array[i][j]);
                                }
                            }
                        } else {
                            double[] array = (double[]) data;
                            //Already checked array sizes                   
                            for (int i = 0; i < array.length; i++) {
                                series.appendData(x[i], y[i], array[i]);
                            }
                        }
                    }
                } else {
                    series.setData((double[][]) data);
                }
            } else if (plot instanceof SlicePlot slicePlot) {
                final SlicePlotSeries series = slicePlot.getSeries(0);
                if (multidimentional1dDataset) {
                    double[][] array = (double[][]) data;
                    final int[] _steps = steps;
                    ((SlicePlotSeries) plot.getSeries(0)).setListener((SlicePlotSeries series1, int page) -> {
                        try {
                            int begin = page * (_steps[1] + 1);
                            series1.clear();
                            for (int i = begin; i < begin + (_steps[1] + 1); i++) {
                                for (int j = 0; j < (_steps[2] + 1); j++) {
                                    series1.appendData(y[i], (z == null) ? j : z[j], array[i][j]);
                                }
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    });
                } else if (dimensions == 3) {
                    double[][][] array = (double[][][]) data;
                    series.setListener((SlicePlotSeries series1, int page) -> {
                        try {
                            series1.setData(array[page]);
                        } catch (Exception ex) {
                            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    });
                } else {
                    series.setListener((SlicePlotSeries series1, int page) -> {
                        try {
                            DataStore dm = new DataStore("h5"); //Test!!! Assume only h5 can cope with 4d data
                            DataSlice slice = dm.getData(descriptor.root, descriptor.path, page);   
                            Object data1 = slice.sliceData;
                            if (slice.unsigned) {
                                data1 = Convert.toUnsigned(data1);
                            }
                            series1.setData((double[][]) Convert.toDouble(data1));
                        } catch (Exception ex) {
                            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    });
                }
            }

            plot.update(true);
            plot.setUpdatesEnabled(true);//This plot is user general-purpose so disable scan optimization
            if (!Plot.isOffscreen()) {                
                validate();
                repaint();
            }
        }
        return plot;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        pnGraphs = new javax.swing.JPanel();

        setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout pnGraphsLayout = new javax.swing.GroupLayout(pnGraphs);
        pnGraphs.setLayout(pnGraphsLayout);
        pnGraphsLayout.setHorizontalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 354, Short.MAX_VALUE)
        );
        pnGraphsLayout.setVerticalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 301, Short.MAX_VALUE)
        );

        scrollPane.setViewportView(pnGraphs);

        add(scrollPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel pnGraphs;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
