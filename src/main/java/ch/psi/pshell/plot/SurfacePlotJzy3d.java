package ch.psi.pshell.plot;

import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.Plot.Quality;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import com.jogamp.opengl.GLAnimatorControl;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.Settings;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapGrayscale;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.contour.DefaultContourColoringPolicy;
import org.jzy3d.contour.IContourColoringPolicy;
import org.jzy3d.contour.MapperContourPictureGenerator;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.SurfaceBuilder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Drawable;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.ScatterMultiColor;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.primitives.axis.ContourAxisBox;
import org.jzy3d.plot3d.rendering.canvas.IScreenCanvas;
import org.jzy3d.plot3d.rendering.legends.colorbars.AWTColorbarLegend;
import org.jzy3d.plot3d.rendering.view.modes.ViewBoundMode;
import ch.psi.utils.swing.SwingUtils;
import com.jogamp.opengl.util.texture.TextureData;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.jzy3d.chart.NativeAnimator;
import org.jzy3d.chart.factories.ContourChartFactory;

/**
 *
 */
public class SurfacePlotJzy3d extends SurfacePlotBase {
    private Chart chart;
    GLAnimatorControl animatorControl;
    IScreenCanvas screenCanvas;
    private Mapper mapper;
    private Shape surface;
    private MatrixPlotSeries series;
    private double[][] data;
    JPopupMenu menuPopup;
    JLabel title;

    public SurfacePlotJzy3d() {
        super();
    }

    
    @Override
    protected void onShow() {
        /*
        if (series != null) {
            SwingUtilities.invokeLater(new Runnable() { //Invoking later because was not rendering when setting date with FDA
                @Override
                public void run() {
                    if (chart == null) {            
                        createGraph();
                    } else {
                        updateGraph(false);
                    }
                }
            });
        } else {
            checkBounds(true);
        }*/
    }

    protected void onHidden() {
        /*
        if (chart != null) {
            chart.stopAnimation();
            removeAll();
            chart.dispose();
            chart = null; 
        }
        */
    }

    @Override
    protected void createChart() {
        super.createChart();
        setLayout(new BorderLayout());        
        title = new JLabel();
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);
        
        setColormap(Colormap.Temperature);
        if (getHardwareAccelerated() != Settings.getInstance().isHardwareAccelerated()) {
            Settings.getInstance().setHardwareAccelerated(getHardwareAccelerated());
        }
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        menuPopup = new JPopupMenu();
    }
    
    @Override
    protected void onTitleChanged() {
        if (getTitleFont()!=null){
            title.setFont(getTitleFont());
        }
        title.setText((getTitle()==null) ? "" : getTitle());
    }    

    @Override
    protected Object onAddedSeries(final MatrixPlotSeries s) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        onAddedSeries(s);
                    }
                });
            } catch (Exception ex) {
                Logger.getLogger(SurfacePlotJzy3d.class.getName()).log(Level.SEVERE, null, ex);
            }
            return this;
        }

        synchronized (chartLock) {
            if (s != null) {
                if (chart != null) {
                    //chart clear ###
                    chart.dispose();
                    chart = null;
                }
            }

            if (s == null) {
                series = null;
                data = null;
            } else {
                rangeX = new Range((float) s.getMinX(), (float) s.getMaxX());
                rangeY = new Range((float) s.getMinY(), (float) s.getMaxY());

                //If same series & same dimensions then preserve data
                if ((data == null) || (series != s) || (s.getNumberOfBinsY() != data.length) || (s.getNumberOfBinsX() != data[0].length)) {
                    data = new double[s.getNumberOfBinsY()][s.getNumberOfBinsX()];
                    for (int i = 0; i < data.length; i++) {
                        Arrays.fill(data[i], Double.NaN);
                    }
                }
                series = s;
            }
        }
        //if (isShowing()) {
            createGraph();
        //}
        return this;
    }

    @Override
    protected void onAxisRangeChanged(AxisId axisId) {
        if (series != null) {
            updateGraph(true);
        }
    }

    @Override
    protected void onRemovedSeries(MatrixPlotSeries s) {
        series = null;
        if (chart != null) {
            //chart clear ###
            chart.dispose();
            chart = null;
        }
    }

    @Override
    protected void onAppendData(MatrixPlotSeries s, int indexX, int indexY, double x, double y, double z) {
        data[indexY][indexX] = z;
    }

    @Override
    protected void onSetData(MatrixPlotSeries s, double[][] data, double[][] xdata, double[][] ydata) {
        this.data = data;
    }

    @Override
    public void doUpdate() {
        //createGraph();
        updateGraph(true);
    }

    public void updateSeries(MatrixPlotSeries s) {
        updateGraph(false);
    }

    @Override
    public double[][] getSeriesData(MatrixPlotSeries s) {
        return data;
    }

    private Range rangeX;
    private Range rangeY;
    double[] rangeZ = null;

    Boolean showLegend;

    public void setShowLegend(boolean value) {
        if (value != getShowLegend()) {
            showLegend = value;
            //if (isShowing()) {
                updateGraph(true);
            //}
        }
    }

    public boolean getShowLegend() {
        if (showLegend == null) {
            return true;
        }
        return showLegend;
    }

    Boolean showFace;

    public void setShowFace(boolean value) {
        if (value != getShowFace()) {
            showFace = value;
            //if (isShowing()) {
                updateGraph(true);
            //}
        }
    }

    public boolean getShowFace() {
        if (showFace == null) {
            return true;
        }
        return showFace;
    }

    Boolean showFrame;

    public void setShowFrame(boolean value) {
        if (value != getShowFrame()) {
            showFrame = value;
            //if (isShowing()) {
                updateGraph(true);
            //}
        }
    }

    public boolean getShowFrame() {
        if (showFrame == null) {
            return false;
        }
        return showFrame;
    }

    Boolean showAxis;

    public void setShowAxis(boolean value) {
        if (value != getShowAxis()) {
            showAxis = value;
            //if (isShowing()) {
                updateGraph(true);
            //}
        }
    }

    public boolean getShowAxis() {
        if (showAxis == null) {
            return true;
        }
        return showAxis;
    }


    public void setColormap(Colormap value) {
        if ((value != Colormap.Grayscale) && (value != Colormap.Temperature)) {
            value = Colormap.Temperature;
        }
        if (value != getColormap()) {
            super.setColormap(value);
            //if (isShowing()) {
                updateGraph(true);
            //}
        }
    }

    org.jzy3d.plot3d.rendering.canvas.Quality getJzy3dQuality() {
        switch (getQuality()) {
            case Low:
                return org.jzy3d.plot3d.rendering.canvas.Quality.Fastest();
            case Medium:
                return org.jzy3d.plot3d.rendering.canvas.Quality.Intermediate();
            case High:
                return org.jzy3d.plot3d.rendering.canvas.Quality.Advanced();
            case Maximum:
                return org.jzy3d.plot3d.rendering.canvas.Quality.Nicest();
        }
        return null;
    }

    public void setQuality(Quality quality) {
        super.setQuality(quality);
        for (Component c : menuQuality.getMenuComponents()) {
            JMenuItem menuItem = (JMenuItem) c;
            menuItem.setSelected(String.valueOf(quality).equals(menuItem.getText()));
        }
        if (series != null) {
            //if (isShowing()) {
                createGraph();
            //}
        }
    }

    public enum Mode {

        Top,
        Profile,
        Free;

        private org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode toJzy3dMode() {
            switch (this) {
                case Top:
                    return org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode.TOP;
                case Profile:
                    return org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode.PROFILE;
                case Free:
                    return org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode.FREE;
            }
            return null;
        }
    }
    Mode mode;

    public void setMode(Mode mode) {
        this.mode = mode;
        if (series != null) {
            updateGraph(true);
        }
    }

    public Mode getMode() {
        if (mode == null) {
            return Mode.Free;
        }
        return mode;

    }

    public enum Contour {

        None,
        Normal,
        Filled,
        HeightMap,
        Contour3D

    }
    Contour contour;

    public void setContour(Contour contour) {
        this.contour = contour;
        if (series != null) {
            //if (isShowing()) {
                createGraph();
            //}
        }
    }

    public Contour getContour() {
        if (contour == null) {
            return Contour.None;
        }
        return contour;
    }

    Integer contourLevels;

    public void setContourLevels(int value) {
        contourLevels = value;
        updateGraph(true);
    }

    public int getContourLevels() {
        if (contourLevels == null) {
            return 10;
        }
        return contourLevels;
    }

    Integer contourDensity;

    public void setContourDensity(int value) {
        contourDensity = value;
        updateGraph(true);
    }

    public int getContourDensity() {
        if (contourDensity == null) {
            return 400;
        }
        return contourDensity;
    }

    java.awt.Color frameColor;

    public void setFrameColor(java.awt.Color value) {
        frameColor = value;
        updateGraph(true);
    }

    public java.awt.Color getFrameColor() {
        if (frameColor == null) {
            return new java.awt.Color(getAxisTextColor().getRed(), getAxisTextColor().getGreen(), getAxisTextColor().getBlue());
        }
        return frameColor;
    }

    Boolean hideEmptyRows;

    /**
     * By default empty lines (containing only NaN) are not plotted.
     */
    public void setHideEmptyRows(boolean value) {
        hideEmptyRows = value;
        updateGraph(true);
    }

    public boolean getHideEmptyRows() {
        if (hideEmptyRows == null) {
            return true;
        }
        return hideEmptyRows;
    }

    protected static void remap(Shape shape, Mapper mapper) {
        List<Drawable> polygons = shape.getDrawables();
        for (Drawable d : polygons) {
            if (d instanceof Polygon) {
                Polygon p = (Polygon) d;
                for (int i = 0; i < p.size(); i++) {
                    Point pt = p.get(i);
                    Coord3d c = pt.xyz;
                    c.z = (float) mapper.f(c.x, c.y);
                }
            }
        }
    }

    final Object chartLock = new Object();

    void createGraph() {
        synchronized (chartLock) {
            Chart formerChart = chart;
            if (series != null) {
                if ((getContour() == Contour.None) || (getContour() == Contour.Contour3D)) {
                    chart = ContourChartFactory.chart(getJzy3dQuality());
                } else {
                    chart = new ContourChartFactory().newChart(getJzy3dQuality());
                }       
                java.awt.Color frameColor = getFrameColor();
                chart.getAxisLayout().setMainColor(new Color(frameColor.getRed(), frameColor.getGreen(), frameColor.getBlue()));
                java.awt.Color background = getBackground();
                if (background == null) {
                    background = getParent().getBackground();
                }
                chart.getView().setBackgroundColor(new Color(background.getRed(), background.getGreen(), background.getBlue()));


                if (rangeY == null) {
                    rangeY = new Range(0, data.length - 1);
                }
                if (rangeX == null) {
                    rangeX = new Range(0, data[0].length - 1);
                }
                mapper = new Mapper() {
                    @Override
                    public double f(double x, double y) {
                        if ((data == null) || (data.length == 0)) {
                            return Double.NaN;
                        }
                        int indexY = (int) Math.round((y - rangeY.getMin()) / ((rangeY.getMax() - rangeY.getMin()) / (data.length - 1)));
                        int indexX = (int) Math.round((x - rangeX.getMin()) / ((rangeX.getMax() - rangeX.getMin()) / (data[0].length - 1)));
                        double ret = data[indexY][indexX];
                        if (Double.isInfinite(ret)) {
                            ret = Double.NaN;
                        }
                        return ret;
                    }
                };
                animatorControl = null;
                screenCanvas = null;
                Component canvas = (Component) chart.getCanvas();
                if (canvas instanceof IScreenCanvas) {
                    screenCanvas = (IScreenCanvas) canvas;
                    NativeAnimator animation = (NativeAnimator) screenCanvas.getAnimation();                    
                    animatorControl = (GLAnimatorControl) animation.getAnimator();
                }

                if (formerChart!=null){
                    SurfacePlotJzy3d.this.remove((Component)  formerChart.getCanvas());
                }                
                SurfacePlotJzy3d.this.add(canvas, BorderLayout.CENTER);

                //Todo: why it is not displyed if I don'r pack the Window?
                if (SurfacePlotJzy3d.this.isVisible()) {
                    SurfacePlotJzy3d.this.validate();
                }

                chart.addMouseCameraController();
                chart.getAxisLayout().setXAxisLabel(getAxis(AxisId.X).getLabel());
                chart.getAxisLayout().setYAxisLabel(getAxis(AxisId.Y).getLabel());
                chart.getAxisLayout().setZAxisLabel(getAxis(AxisId.Z).getLabel());

                
                Object listener =  new MouseAdapter() {

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        checkPopup(e);
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        checkPopup(e);
                    }

                    private void checkPopup(MouseEvent e) {
                        if (e.isPopupTrigger()
                                && (!e.isAltDown()) && (!e.isControlDown()) && (!e.isShiftDown())) {
                            menuPopup.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                };

                chart.getCanvas().addMouseController(listener);
                

                updateGraph(true);

                if (animatorControl != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            animatorControl.pause();
                        }
                    });
                }

            }
            if (formerChart != null) {
                formerChart.dispose();
            }

        }
    }

    JMenu menuQuality;

    @Override
    protected void createPopupMenu() {
        JMenuItem menuUpdate = new JMenuItem("Update");
        final JCheckBoxMenuItem menuShowAxis = new JCheckBoxMenuItem("Show Axis");
        final JCheckBoxMenuItem menuShowLegend = new JCheckBoxMenuItem("Show Legend");
        final JCheckBoxMenuItem menuShowFace = new JCheckBoxMenuItem("Show Face");
        final JCheckBoxMenuItem menuShowFrame = new JCheckBoxMenuItem("Show Frame");
        JMenuItem frameColor = new JMenuItem("Set Frame Color");

        final JMenu menuColormap = new JMenu("Colormap");
        final JMenu menuMode = new JMenu("Mode");
        final JMenu menuContour = new JMenu("Contour");
        menuQuality = new JMenu("Quality");
        menuPopup.add(menuShowAxis);
        menuPopup.add(menuShowLegend);
        menuPopup.add(menuShowFace);
        menuPopup.add(menuShowFrame);
        menuPopup.add(frameColor);
        menuPopup.add(menuColormap);
        menuPopup.add(menuMode);
        menuPopup.add(menuContour);
        menuPopup.add(menuQuality);
        menuPopup.add(menuUpdate);
        menuUpdate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    update(true);
                } catch (Exception ex) {
                }
            }
        });

        frameColor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String ret = SwingUtils.getString(SurfacePlotJzy3d.this, "Enter frame color (name or R,G,B):", getFrameColor().getRed() + "," + getFrameColor().getGreen() + "," + getFrameColor().getBlue());
                    if (ret != null) {
                        java.awt.Color color = null;
                        if (ret.contains(",")) {
                            String[] tokens = ret.split(",");
                            color = new java.awt.Color(Integer.valueOf(tokens[0]), Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]));
                        } else {
                            Field field = java.awt.Color.class.getField(ret);
                            color = (java.awt.Color) field.get(null);
                        }
                        setFrameColor(color);
                        updateGraph(true);
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(SurfacePlotJzy3d.this, ex);
                }
            }
        });

        menuShowFrame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setShowFrame(!getShowFrame());
                menuShowFrame.setSelected(getShowFrame());
            }
        });
        menuShowFrame.setSelected(getShowFrame());

        menuShowLegend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setShowLegend(!getShowLegend());
                menuShowLegend.setSelected(getShowLegend());
            }
        });
        menuShowLegend.setSelected(getShowLegend());

        menuShowFace.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setShowFace(!getShowFace());
                menuShowFace.setSelected(getShowFace());
            }
        });
        menuShowFace.setSelected(getShowFace());

        menuShowAxis.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setShowAxis(!getShowAxis());
                menuShowAxis.setSelected(getShowAxis());
            }
        });
        menuShowAxis.setSelected(getShowAxis());

        ButtonGroup colormapGroup = new ButtonGroup();
        for (Colormap c : new Colormap[]{Colormap.Temperature, Colormap.Grayscale}) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(c.toString());
            colormapGroup.add(menuItem);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setColormap(Colormap.valueOf(e.getActionCommand()));
                }
            });
            menuItem.setSelected(getColormap().toString().equals(menuItem.getText()));
            menuColormap.add(menuItem);
        }

        ButtonGroup modeGroup = new ButtonGroup();
        for (Mode q : Mode.values()) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(q.toString());
            modeGroup.add(menuItem);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setMode(Mode.valueOf(e.getActionCommand()));
                }
            });
            menuItem.setSelected(getMode().toString().equals(menuItem.getText()));
            menuMode.add(menuItem);
        }

        ButtonGroup contourGroup = new ButtonGroup();
        for (Contour q : Contour.values()) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(q.toString());
            contourGroup.add(menuItem);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setContour(Contour.valueOf(e.getActionCommand()));
                }
            });
            menuItem.setSelected(getContour().toString().equals(menuItem.getText()));
            menuContour.add(menuItem);
        }
        menuContour.addSeparator();
        JMenuItem levels = new JMenuItem("Set Levels");
        levels.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String ret = SwingUtils.getString(SurfacePlotJzy3d.this, "Enter number of contour levels:", getContourLevels());
                    if (ret != null) {
                        Integer levels = Integer.valueOf(ret);
                        setContourLevels(levels);
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(SurfacePlotJzy3d.this, ex);
                }
            }
        });
        menuContour.add(levels);

        JMenuItem density = new JMenuItem("Set Density");
        density.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String ret = SwingUtils.getString(SurfacePlotJzy3d.this, "Enter contour density:", getContourDensity());
                    if (ret != null) {
                        Integer density = Integer.valueOf(ret);
                        setContourDensity(density);
                    }
                } catch (Exception ex) {
                    SwingUtils.showException(SurfacePlotJzy3d.this, ex);
                }
            }
        });
        menuContour.add(density);

        ButtonGroup qualityGroup = new ButtonGroup();
        for (Quality q : Quality.values()) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(q.toString());
            qualityGroup.add(menuItem);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setQuality(Quality.valueOf(e.getActionCommand()));
                }
            });
            menuItem.setSelected(getQuality().toString().equals(menuItem.getText()));
            menuQuality.add(menuItem);
        }
        super.createPopupMenu();
    }

    private void updateGraph(final boolean newSeries) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateGraph(newSeries);
                }
            });
            return;
        }
        synchronized (chartLock) {
            if (chart == null) {
                return;
            }
            Shape former = surface;
            if (series == null) {
                if (surface != null) {
                    chart.getScene().getGraph().remove(former, false);
                }
                surface = null;
            } else {

                //TODO: Not succeding having the partial updates - and they are not performing much better neither
                boolean changedBoundsZ = checkBounds(false);
                if ((surface == null) || (newSeries) || getAxis(AxisId.Z).isAutoRange() || changedBoundsZ) {
                    //if (true) {
                    surface = new SurfaceBuilder().orthonormal(new OrthonormalGrid(rangeX, series.getNumberOfBinsX(), rangeY, series.getNumberOfBinsY()), mapper);
                    ColorMapper colorMapper = new ColorMapper((getColormap() == Colormap.Temperature) ? new ColorMapRainbow() : new ColorMapGrayscale(),
                            (float) ((getAxis(AxisId.Z).isAutoRange()) ? surface.getBounds().getZmin() : getAxis(AxisId.Z).getMin()),
                            (float) ((getAxis(AxisId.Z).isAutoRange()) ? surface.getBounds().getZmax() : getAxis(AxisId.Z).getMax()),
                            new Color(1, 1, 1, .5f));
                    surface.setColorMapper(colorMapper);
                    surface.setFaceDisplayed(true);
                    surface.setWireframeDisplayed(getShowFrame());

                    surface.setWireframeColor(new Color(getFrameColor().getRed(), getFrameColor().getGreen(), getFrameColor().getBlue(), getFrameColor().getAlpha()));
                    surface.setFaceDisplayed(getShowFace());
                    if (getShowLegend()) {
                        AWTColorbarLegend cbar = new AWTColorbarLegend(surface, chart.getView().getAxis().getLayout());
                        cbar.setBackground(chart.getView().getBackgroundColor());
                        surface.setLegend(cbar);
                    }

                    if (getContour() != Contour.None) {
                        int xRes = getContourDensity();
                        int yRes = getContourDensity();

                        MapperContourPictureGenerator contour = new MapperContourPictureGenerator(mapper, rangeX, rangeY);
                        IContourColoringPolicy policy = new DefaultContourColoringPolicy(colorMapper);
                        ContourAxisBox cab = (getContour() == Contour.Contour3D) ? null : (ContourAxisBox) chart.getView().getAxis();

                        switch (getContour()) {
                            case Normal:
                                cab.setContourImg(contour.getContourImage(policy, xRes, yRes, getContourLevels()), rangeX, rangeY);
                                break;
                            case Filled:
                                cab.setContourImg(contour.getFilledContourImage(policy, xRes, yRes, getContourLevels()), rangeX, rangeY);
                                break;
                            case HeightMap:
                                cab.setContourImg(contour.getHeightMap(policy, xRes, yRes, getContourLevels()), rangeX, rangeY);
                                break;
                            case Contour3D:
                                double dx = ((double) series.getNumberOfBinsX()-1) * series.getBinWidthX() / xRes;
                                double dy = ((double) series.getNumberOfBinsY()-1) * series.getBinWidthY() / yRes;
                                double[][] contours = contour.getContourMatrix(xRes, yRes, getContourLevels());
                               
                                // Create the dot cloud scene and fill with data
                                int size = xRes * yRes;
                                Coord3d[] points = new Coord3d[size];
                                for (int x = 0; x < xRes; x++) {
                                    for (int y = 0; y < yRes; y++) {
                                        double px = ((double) x * dx + series.getMinX());
                                        //double py = (double) (series.getMaxY() - (double) (y+1) * dy);
                                        double py = ((double) (yRes-y) * dy + series.getMinY());
                                        double z = contours[x][y];
                                        if (z > -Double.MAX_VALUE) { // Non contours points are -Double.MAX_VALUE and are not painted
                                            points[x * yRes + y] = new Coord3d(px, py, z);
                                        } else {
                                            points[x * yRes + y] = new Coord3d(px, py, Double.NaN);
                                        }
                                    }
                                }
                                ScatterMultiColor scatter = new ScatterMultiColor(points, colorMapper);
                                surface.add(scatter);
                                break;                                                                                            
                        }
                    }
                    chart.setViewMode(getMode().toJzy3dMode());
                    chart.setAxeDisplayed(getShowAxis());
                }

                if (surface == former) {
                    remap(surface, mapper);
                    chart.render();
                } else {

                    chart.getScene().getGraph().add(surface, false);
                    if (former != null) {
                        chart.getScene().getGraph().remove(former, false);
                    }
                    boolean changed = checkBounds(true);
                    if (!changed)
                        chart.getView().updateBounds();
                }
            }
        }
        if (newSeries) {
            if (animatorControl != null) {
                if (animatorControl.isPaused()) {
                    screenCanvas.display();
                }
            }
            
        }

    }

    protected boolean checkBounds(boolean updateBounds) {
        if (chart == null) {
            return false;
        }
        boolean changed = false;
        boolean hasContour = (getContour() != Contour.None);
        boolean autoRange = getAxis(AxisId.X).isAutoRange() && getAxis(AxisId.Y).isAutoRange() && getAxis(AxisId.Z).isAutoRange();
        boolean forceSeriesRange = getAxis(AxisId.X).isAutoRange() && getAxis(AxisId.Y).isAutoRange() && (!getHideEmptyRows());
        boolean manualBounds = (!autoRange) || hasContour || forceSeriesRange;

        //If manual bounds
        if (manualBounds) {
            //Deferring setting bounds untiul the panel is displayed
            if (/*isShowing() &&*/ (chart != null)) {
                if (chart.getView().getBoundsMode() != ViewBoundMode.MANUAL) {
                    changed = true;
                }
                BoundingBox3d bounds = chart.getView().getBounds();
                if (hasContour || forceSeriesRange) {
                    //TODO: Auto-range will not plot points=NaN, and bounds can be smaller. It will break the contour plot.
                    //Cant I find a way to plot only the visible contour instead of foercing the full range?
                    if (bounds.getXmin() != rangeX.getMin()) {
                        bounds.setXmin((float) rangeX.getMin());
                        changed = true;
                    }
                    if (bounds.getXmax() != rangeX.getMax()) {
                        bounds.setXmax((float) rangeX.getMax());
                        changed = true;
                    }
                    if (bounds.getYmin() != rangeY.getMin()) {
                        bounds.setYmin((float) rangeY.getMin());
                        changed = true;
                    }
                    if (bounds.getYmax() != rangeY.getMax()) {
                        bounds.setYmax((float) rangeY.getMax());
                        changed = true;
                    }
                } else {
                    if (!getAxis(AxisId.X).isAutoRange()) {
                        if (bounds.getXmin() != getAxis(AxisId.X).getMin()) {
                            bounds.setXmin((float) getAxis(AxisId.X).getMin());
                            changed = true;
                        }
                        if (bounds.getXmax() != getAxis(AxisId.X).getMax()) {
                            bounds.setXmax((float) getAxis(AxisId.X).getMax());
                            changed = true;
                        }
                    }
                    if (!getAxis(AxisId.Y).isAutoRange()) {
                        if (bounds.getYmin() != getAxis(AxisId.Y).getMin()) {
                            bounds.setYmin((float) getAxis(AxisId.Y).getMin());
                            changed = true;
                        }
                        if (bounds.getYmax() != getAxis(AxisId.Y).getMax()) {
                            bounds.setYmax((float) getAxis(AxisId.Y).getMax());
                            changed = true;
                        }
                    }
                }
                if (!getAxis(AxisId.Z).isAutoRange()) {
                    if (bounds.getZmin() != getAxis(AxisId.Z).getMin()) {
                        bounds.setZmin((float) getAxis(AxisId.Z).getMin());
                        changed = true;
                    }
                    if (bounds.getZmax() != getAxis(AxisId.Z).getMax()) {
                        bounds.setZmax((float) getAxis(AxisId.Z).getMax());
                        changed = true;
                    }
                }
                if (changed && updateBounds) {
                    chart.getView().setBoundManual(bounds);
                }
            }           
        } else if (chart.getView().getBoundsMode() != ViewBoundMode.AUTO_FIT) {
            if (updateBounds) {
                chart.getView().setBoundMode(ViewBoundMode.AUTO_FIT);
            }
            changed = true;
        }

        return changed;
    }

    @Override
    public void addPopupMenuItem(final JMenuItem item) {
        if (item == null) {
            menuPopup.addSeparator();
        } else {
            menuPopup.add(item);
        }
    }

    @Override
    public BufferedImage getSnapshot(Dimension size) {

        File temp = null;
        try {
            temp = File.createTempFile("snapshot", ".png");
            chart.screenshot(temp);
            return ImageIO.read(new File(temp.getAbsolutePath()));
        } catch (Exception ex) {
            return null;
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    @Override
    public void saveSnapshot(String filename, String format, Dimension size) throws IOException {
        if (chart != null) {
            chart.screenshot(new File(filename));
        }
    }

}
