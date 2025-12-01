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
        if (!PlotBase.getOffscreen()) {
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

        if (!plot.isOffscreen()) {
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
            if (plotType instanceof String str) {                  
                try {
                   plotType = Integer.parseInt(str);                  
               } catch (NumberFormatException e) {
               }
            }
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
            if (prefs.plotTypes != null) { 
                Class type = getPlotClass(prefs.plotTypes.get(name));
                if (isScan || LinePlot.class.isAssignableFrom(type)){ 
                    //If device name matches a Cacheable cache name, use the rule for the parent  
                    if (type != null) {
                        requestedPlot = (Plot) type.newInstance();
                    }
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
        int records = -1;
        if ((steps!=null) && (steps.length>0)){
            records = 1;
            for (int i=0; i<steps.length; i++){
                records = records * (steps[i]+1);
            }
        }

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
                    //SlicePlotSeries series = new SlicePlotSeries(name, start[2], end[2], steps[2] + 1, start[1], end[1], steps[1] + 1, start[0], end[0], steps[0] + 1);
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
                        String seriesName = getDispVal(y);
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
        } else {
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
                    if (rank <= 1){                        
                        int nX = ((steps[0] < 0)||(steps[0] == Integer.MAX_VALUE)) ? DEFAULT_RANGE_STEPS : steps[0] + 1;
                        series = new MatrixPlotSeries(name, start[0], end[0], nX, y_start, y_end, ySize);
                        if (prefs.range != null) {
                            plot.getAxis(Plot.AxisId.X).setRange(prefs.range.min, prefs.range.max);
                            if (changedScaleX) {
                                series.setRangeX(prefs.range.min, prefs.range.max);
                            }
                        }
                    } else {
                        series = new MatrixPlotSeries(name, 0, records-1, records, 0, recordSize[0]-1, recordSize[0]);
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
                if (rank <= 1){    
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
                } else {
                    plot.getAxis(Plot.AxisId.X).setRange(0, records-1);
                    plot.getAxis(Plot.AxisId.X).setLabel("Index");
                }
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
            plot.setUpdatesEnabled(plot.isOffscreen());
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
            Object ret = Convert.toDouble(data);
            if (ret != null) {
                return ret;
            }
        } catch (Exception ex) {
        }
        throw new IllegalArgumentException("Invalid array type: " + data.getClass());
    }

    //Open offline pots (scan=False)
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
 
        double[] start = (dimensions>1) || multidimentional1dDataset ? descriptor.start : null;
        double[] end =  (dimensions>1) || multidimentional1dDataset ? descriptor.end : null;
        int[] steps = null;
         if (dimensions == 1) {            
            if (multidimentional1dDataset) {
                start = descriptor.start;
                end = descriptor.end;                               
                if (descriptor.dimensions>3){
                    int records = descriptor.getRecords();
                    steps = new int[] {records * descriptor.passes -1};                    
                    start = new double[]{0.0};
                    end = new double[]{steps[0]};          
                    rank = 1;                                        
                } else if (descriptor.dimensions==3){
                    //3D-scan with 1d datasets of scalar
                    if (start==null){
                        start = new double[]{(Double) Arr.getMin(x), (Double) Arr.getMin(y), (Double) Arr.getMin(z)};
                    }
                    if (end==null){
                        end = new double[]{(Double) Arr.getMax(x), (Double) Arr.getMax(y), (Double) Arr.getMax(z)};                    
                    }
                    steps = numberSteps;
                    if (descriptor.passes>1){
                        steps[0]=steps[0]*2+1;
                    }
                    rank = 3;
                } else {
                    //2D-scan with 1d datasets of scalar
                    if (start==null){
                        start = new double[]{(Double) Arr.getMin(x), (Double) Arr.getMin(y)};
                    }
                    if (end==null){
                        end = new double[]{(Double) Arr.getMax(x), (Double) Arr.getMax(y)};                    
                    }
                    steps = numberSteps;
                    rank = 2;                    
                }
            } else {
                //Single line plot or 1d scan of scalar
            }
        } else if (dimensions == 2) {
            if (rank == 3) {
                //Single slice plot as list of images, or 1d scan of images, or 2d scan of images  , or 3d scan of images 
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
                if (multidimentional1dDataset) {
                    //2D-scan with 1d datasets of arrays  or 3D-scan with 1d datasets of arrays
                    sparseArrayData = (dimensions==2) && (numberSteps[0] < 0) && (numberSteps[1] < 0);
                    // Sparse table: don't try to make 3d plots of array data
                    double startX = ((start==null)||(start.length<1)) ? (Double) Arr.getMin(x) : start[0];
                    double startY = ((start==null)||(start.length<2)) ? (Double) Arr.getMin(y) : start[1];
                    double endX = ((end==null)||(end.length<1)) ? (Double) Arr.getMax(x) : end[0];
                    double endY = ((end==null)||(end.length<2)) ? (Double) Arr.getMax(y) : end[1];
                    if (sparseArrayData){
                        start = new double[]{startX, 0};
                        end = new double[]{endX, shape[1] - 1};
                        steps = new int[]{-1, shape[1] - 1};
                        rank = 2;
                    } else {
                        start = new double[]{startX, startY, 0};
                        end = new double[]{endX, endY, shape[1] - 1};
                        steps = new int[]{numberSteps[0], numberSteps[1], shape[1] - 1};
                        if (numberSteps.length==2){
                            if ((z != null) && (z.length > 0)) {
                                start[2] = z[0];
                                end[2] = z[z.length - 1];
                                steps[2] = z.length - 1;
                            } else {
                                if (descriptor.passes>1){
                                    steps[0]=steps[0]*2+1;
                                }                                
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
                    //Single matrix plot or 1d scan of waveform
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
            //Single slice plot as 3d dataset
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

        int[] size = (steps==null) ? new int[0] : new int[steps.length];
        for (int i=0; i< size.length;i++){
            size[i] = steps[i]+1;
        }
                
        Plot plot = addPlot(descriptor.name, false, descriptor.getLabelX(), rank, null, start, end, steps, false);
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
                   
                    if ((rank == 2) && (steps.length==2) ){                            
                        if (data instanceof double[][] d){
                            //1d scan of array  as lineplot.
                            //TODO: data had been transposed because arrays are plotted vertically to match scalars against x.
                            descriptor.transpose();
                            d = (double[][])descriptor.data;                            
                            linePlot.clear();
                            linePlot.getAxis(Plot.AxisId.X).setLabel(descriptor.name);
                            linePlot.getAxis(Plot.AxisId.X).setAutoRange(); 
                            for (int i = 0; i <  d.length; i++) {
                                String seriesName = (x!=null) ? getDispVal(x[i]) : String.valueOf(i);
                                LinePlotSeries series = new LinePlotSeries(seriesName);
                                linePlot.addSeries(series);
                                series.setData(d[i]);
                            }
                        } else if (data instanceof double[] d){
                            //2d scan of scalar  as lineplot.
                            for (int p=0; p< descriptor.passes; p++){
                                boolean newPass= p>0;
                                int offset = p * size[0] * size[1];
                                for (int j=0; j<=steps[1];j++){  
                                    double[] series = new double[size[0]];
                                    double[] xseries = (x!=null) ? new double[size[0]] : null;
                                    for (int i=0; i<=steps[0]; i++){
                                        boolean backwards = descriptor.zigzag && (i%2)==1;
                                        int index = offset + i*size[1] + (backwards? steps[1]-j : j);
                                        //int sindex = i ? steps[0]-i: i;
                                        series[i] = (index<d.length) ? d[index] : Double.NaN;
                                        if (xseries!=null){
                                            xseries[i] = (index<x.length) ? x[index] : Double.NaN;
                                        }
                                    }
                                    if (newPass){
                                        ((LinePlotSeries) plot.getSeries(j)).appendData(xseries[0], Double.NaN);
                                    }
                                    ((LinePlotSeries) plot.getSeries(j)).appendData(xseries, series);
                                }                        
                            }
                        }
                    }
                    else {
                        //Single line plot or 1d scan of scalar, or multidimentinsl scan (>3)
                        ((LinePlotSeries) plot.getSeries(0)).setData((descriptor.dimensions>3) ? null : x, (double[]) data);
                    }
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
                            //3D or multidimentional scan with 1d datasets of arrays 
                            double[][] array = (double[][]) data;
                            //Already checked array sizes                   
                            for (int i = 0; i < array.length; i++) {
                                for (int j = 0; j < array[i].length; j++) {
                                    series.appendData(i, j, array[i][j]);
                                }
                            }
                        } else {
                            //2D-scan with 1d datasets (scalar)
                            double[] array = (double[]) data;
                            //Already checked array sizes                   
                            for (int i = 0; i < array.length; i++) {
                                series.appendData(x[i], y[i], array[i]);
                            }
                        }
                    }
                    if (plot instanceof MatrixPlotJFree mpjf){
                        mpjf.resetZoom();
                    }
                } else {
                    //Single matrix plot or  1d scan of waveform
                    series.setData((double[][]) data);
                }
            } else if (plot instanceof SlicePlot slicePlot) {
                final SlicePlotSeries series = slicePlot.getSeries(0);
                if (multidimentional1dDataset) {
                    //3D-scan with 1d datasets of scalar
                    double step_y = descriptor.getStepSizeY();
                    if (data instanceof double[] array){
                        int pageSize = size[2] * size[1];
                        ((SlicePlotSeries) plot.getSeries(0)).setListener((SlicePlotSeries series1, int page) -> {
                            try {
                                int begin = page * pageSize;
                                series1.clear();
                                for (int i = begin; i <begin+pageSize; i++) {
                                    series1.appendData(y[i], z[i], array[i]);
                                }
                                //double vy = descriptor.start[0] + step_y * page;
                                if (x!=null){
                                    series1.getSlicePlot().setPageSubtitle( " " + descriptor.getLabelZ() + "=" + getDispVal(x[begin]));
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                            }
                        });                        
                    } else {
                        //2D-scan with 1d datasets of arrays  
                        double[][] array = (double[][]) data;
                        ((SlicePlotSeries) plot.getSeries(0)).setListener((SlicePlotSeries series1, int page) -> {
                            try {
                                int begin = page * size[1];
                                series1.clear();
                                for (int i = begin; i < begin + size[1]; i++) {
                                    for (int j = 0; j < size[2]; j++) {
                                        series1.appendData(y[i], (z == null) ? j : z[j], array[i][j]);
                                    }
                                }
                                double vy = x[page * size[1]];
                                if ((descriptor.steps.length==2) &&  !Double.isNaN(step_y)){
                                    series1.getSlicePlot().setPageSubtitle( " " + descriptor.getLabelY() + "=" + getDispVal(vy));
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                            }
                        });
                    }
                } else if (dimensions == 3) {
                    //Single slice plot as 3d datasety
                    double[][][] array = (double[][][]) data;
                    series.setListener((SlicePlotSeries series1, int page) -> {
                        try {
                            series1.setData(array[page]);
                        } catch (Exception ex) {
                            Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                        }
                    });
                } else {
                    //Single slice plot as list of images, or 1d scan of images, or multi-dimentional scan of image
                    series.setListener((SlicePlotSeries series1, int page) -> {
                        for (String fmt: DataStore.getFormatIds()){
                            try {                            
                                double step_z = descriptor.getStepSizeZ();
                                double step_y = descriptor.getStepSizeY();
                                double step_x = descriptor.getStepSizeX();
                                DataSlice slice = new DataStore(fmt).getData(descriptor.root, descriptor.path, page);   
                                try{
                                    Object data1 = slice.sliceData;
                                    if (slice.unsigned) {
                                        data1 = Convert.toUnsigned(data1);
                                    }
                                    series1.setData((double[][]) Convert.toDouble(data1));
                                    if ((descriptor.steps.length==2) && !Double.isNaN(step_x) && !Double.isNaN(step_y)){
                                        int index_x;
                                        int index_y;
                                        if (descriptor.dimensions<=1){
                                            index_y = page % (descriptor.steps[0]+1);
                                            index_x = index_y;
                                        } else {
                                            if (descriptor.passes>1){
                                                page = page % ((descriptor.steps[0]+1) * (descriptor.steps[1]+1));
                                            }                                            
                                            index_y = page / (descriptor.steps[1]+1);
                                            boolean backwards = descriptor.zigzag && (index_y%2)==1;
                                            index_x = backwards ? descriptor.steps[1] - page % (descriptor.steps[1]+1) : page % (descriptor.steps[1]+1);
                                        }
                                        double vy = descriptor.start[0] + step_y * index_y;
                                        double vx = descriptor.start[1] + step_x * index_x;
                                        series1.getSlicePlot().setPageSubtitle( " " +
                                                descriptor.getLabelY() + "=" + getDispVal(vy) + " " +
                                                descriptor.getLabelX() + "=" + getDispVal(vx));
                                    } else if ((descriptor.steps.length==3) && !Double.isNaN(step_x) && !Double.isNaN(step_y)&& !Double.isNaN(step_z)){
                                        int sz[]=new int[]{(descriptor.steps[0]+1), (descriptor.steps[1]+1), (descriptor.steps[2]+1)};
                                        int index_x = page % sz[2];
                                        int index_y = (page/sz[2]) % (sz[1]);
                                        int index_z = (page/sz[2]/sz[1]) % (sz[0]);
                                        
                                        double vz = descriptor.start[0] + step_z * index_z;
                                        double vy = descriptor.start[2] + step_y * index_y;
                                        double vx = descriptor.start[1] + step_x * index_x;
                                        series1.getSlicePlot().setPageSubtitle( " " +
                                                descriptor.getLabelZ() + "=" + getDispVal(vz) + " " +
                                                descriptor.getLabelY() + "=" + getDispVal(vy) + " " +
                                                descriptor.getLabelX() + "=" + getDispVal(vx));
                                    }
                                } catch (Exception ex) {
                                    Logger.getLogger(PlotPanel.class.getName()).log(Level.WARNING, null, ex);
                                }
                                return;
                            }catch (Exception ex){                                    
                            }                        
                        }
                    });
                }
            }

            plot.update(true);
            plot.setUpdatesEnabled(true);//This plot is user general-purpose so disable scan optimization
            if (!plot.isOffscreen()) {                
                validate();
                repaint();
            }
        }
        return plot;
    }

    static String getDispVal(Double d){
        return String.valueOf(Convert.roundDouble(d, 6));
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
