package ch.psi.pshell.plot;

import ch.psi.pshell.imaging.Colormap;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import ch.psi.utils.IO;
import ch.psi.utils.NamedThreadFactory;
import ch.psi.utils.Range;
import ch.psi.utils.swing.ExtensionFileFilter;
import ch.psi.utils.swing.ImageTransferHandler;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.Color;
import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
abstract public class PlotBase<T extends PlotSeries> extends MonitoredPanel implements Plot<T>, Printable {

    public static final String PROPERTY_PLOT_MARKER_SIZE = "ch.psi.pshell.plot.marker.size";

    protected static final String LINE_SEPARATOR = System.lineSeparator();
    protected static final String FIELD_SEPARATOR = "\t";

    protected static final int PREFERRED_WIDTH = 300;
    protected static final int PREFERRED_HEIGHT = 240;

    protected static final int DETACHED_WIDTH = 600;
    protected static final int DETACHED_HEIGHT = 400;

    protected static int SNAPSHOT_WIDTH = 1200;
    protected static int SNAPSHOT_HEIGHT = 1000;
    
    protected static Font TICK_LABEL_FONT = new Font(Font.SANS_SERIF, 0, 10);
    protected static Font LABEL_FONT = new Font(Font.SANS_SERIF, 0, 11);
    

    final Class seriesType;

    protected PlotBase(Class<T> seriesType) {
        this(seriesType, null);
    }

    volatile boolean instantiated;
    final ExecutorService executor;

    protected PlotBase(Class<T> seriesType, String title) {
        super();
        if (title == null) {
            title = "Plot";
        }
        setTitle(title);
        quality = Quality.High;
        this.seriesType = seriesType;
        try {
            createChart();
            if (!offscreen) {
                createPopupMenu();
            }
        } catch (Exception ex) {
            Logger.getLogger(PlotBase.class.getName()).log(Level.INFO, null, ex);
        }
        updating = new AtomicBoolean(false);
        instantiated = true;
        executor = offscreen ? Executors.newSingleThreadExecutor(new NamedThreadFactory("Offscreen plot update task")) : null;
    }

    static String imagesFolderName;

    public static void setImageFileFolder(String folderName) {
        imagesFolderName = folderName;
    }

    public static String getImageFileFolder() {
        return imagesFolderName;
    }

    String title;

    @Override
    public void setTitle(String title) {
        this.title = title;
        if (instantiated) {
            onTitleChanged();
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    Quality quality;

    @Override
    public void setQuality(Quality quality) {
        if (quality != null) {
            this.quality = quality;
        }
    }

    @Override
    public Quality getQuality() {
        return quality;
    }

    Font titleFont;

    @Override
    public void setTitleFont(Font font) {
        this.titleFont = font;
        if (instantiated) {
            onTitleChanged();
        }
    }

    @Override
    public Font getTitleFont() {
        return titleFont;
    }

    private final AtomicBoolean updating;

    @Override
    public final void update(boolean deferred) {
        if ((!deferred) && (offscreen || SwingUtilities.isEventDispatchThread())) {
            updating.set(false);
            doUpdate();
        } else {
            if (updating.compareAndSet(false, true)) {
                invokeLater(() -> {
                    if (updating.compareAndSet(true, false)) {
                        try {
                            doUpdate();
                        } catch (Exception ex) {
                            //Logger.getLogger(PlotBase.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
        }
    }

    boolean requireUpdateOnAppend = true;

    protected void setRequireUpdateOnAppend(boolean value) {
        requireUpdateOnAppend = value;
    }

    protected boolean getRequireUpdateOnAppend() {
        return requireUpdateOnAppend;
    }

    boolean updatesEnabled = true;

    @Override
    public void setUpdatesEnabled(boolean value) {
        updatesEnabled = value;
    }

    @Override
    public boolean isUpdatesEnabled() {
        return updatesEnabled;
    }

    /**
     * Should be improved in implementations to make it independent of the
     * window state (and to consider the size parameter);
     */
    @Override
    public BufferedImage getSnapshot(Dimension size) {
        if (!offscreen) {
            if (!SwingUtilities.isEventDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        //So plot will be updated with data set in other threads.
                    });
                } catch (Exception ex) {
                }
            }
        }
        //So plot will be updated with data set in other threads.
        return SwingUtils.createImage(this);
    }

    @Override
    public void saveSnapshot(String filename, String format, Dimension size) throws IOException {
        ImageIO.write(getSnapshot(size), format, new File(filename));
    }

    @Override
    public void copy() {
        String data = null;
        //Try copy data
        try {
            data = getDataAsString();
        } catch (Exception ex) {
            Logger.getLogger(PlotBase.class.getName()).log(Level.INFO, null, ex);
        }

        try {
            BufferedImage img = getSnapshot(null);
            if (img != null) {
                ImageTransferHandler imageSelection = new ImageTransferHandler(img, data);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(imageSelection, (Clipboard clipboard1, Transferable contents) -> {
                });
            }
        } catch (Exception ex) {
            Logger.getLogger(PlotBase.class.getName()).log(Level.INFO, null, ex);
        }
    }

    //Printable interface
    @Override
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {

        if (page > 0) {
            return NO_SUCH_PAGE;
        }

        BufferedImage img = getSnapshot(null);

        //Schrinks the image if too big but does not expand it
        double scaleX = img.getWidth() > pf.getImageableWidth() ? ((double) pf.getImageableWidth()) / img.getWidth() : 1.0;
        double scaleY = img.getHeight() > pf.getImageableHeight() ? ((double) pf.getImageableHeight()) / img.getHeight() : 1.0;
        double scale = Math.min(scaleX, scaleY);    //Keep aspect ratio
        AffineTransform transf = new AffineTransform();
        transf.scale(scale, scale);

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());
        g2d.drawImage(img, transf, null);
        //g2d.drawImage(img,0,0,null);
        return PAGE_EXISTS;
    }

    @Override
    public void saveData(String filename) throws IOException {
        String data = getDataAsString();
        if (data != null) {
            Files.write(Paths.get(filename), data.getBytes());
        }
    }

    protected void createChart() {

    }

    protected void createPopupMenu() {
        addPopupMenuItem(null);//Separator

        JMenuItem saveSnapshot = new JMenuItem("Save Image As...");
        saveSnapshot.addActionListener((ActionEvent e) -> {
            try {

                JFileChooser chooser = new JFileChooser(imagesFolderName);
                chooser.addChoosableFileFilter(new ExtensionFileFilter("PNG files (*.png)", new String[]{"png"}));
                chooser.addChoosableFileFilter(new ExtensionFileFilter("Bitmap files (*.bmp)", new String[]{"bmp"}));
                chooser.addChoosableFileFilter(new ExtensionFileFilter("GIF files (*.gif)", new String[]{"gif"}));
                chooser.addChoosableFileFilter(new ExtensionFileFilter("TIFF files (*.tif)", new String[]{"tif", "tiff"}));
                chooser.addChoosableFileFilter(new ExtensionFileFilter("JPEG files (*.jpg)", new String[]{"jpg", "jpeg"}));
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showSaveDialog(PlotBase.this) == JFileChooser.APPROVE_OPTION) {

                    String filename = chooser.getSelectedFile().getAbsolutePath();
                    String type = "png";
                    String ext = IO.getExtension(chooser.getSelectedFile());
                    for (String fe : new String[]{"bmp", "jpg"}) {
                        if ((chooser.getFileFilter().getDescription().contains(fe))
                                || (fe.equals(ext))) {
                            type = fe;
                            break;
                        }
                    }
                    if ((ext == null) || (ext.isEmpty())) {
                        filename += "." + type;
                    }
                    if (new File(filename).exists()) {
                        if (SwingUtils.showOption(PlotBase.this, "Overwrite", "File " + filename + " already exists.\nDo you want to overwrite it?", OptionType.YesNo) == OptionResult.No) {
                            return;
                        }
                    }
                    saveSnapshot(filename, type, null);
                }
            } catch (Exception ex) {
                SwingUtils.showException(PlotBase.this, ex);
            }
        });
        addPopupMenuItem(saveSnapshot);

        JMenuItem saveData = new JMenuItem("Save Data As...");
        saveData.addActionListener((ActionEvent e) -> {
            try {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Text data files", "txt", "dat");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showSaveDialog(PlotBase.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String filename = chooser.getSelectedFile().getAbsolutePath();
                    String type = IO.getExtension(chooser.getSelectedFile());
                    if ((type == null) || (type.isEmpty())) {
                        type = "txt";
                        filename += "." + type;
                    }
                    saveData(filename);
                }
            } catch (Exception ex) {
                SwingUtils.showException(PlotBase.this, ex);
            }
        });
        addPopupMenuItem(saveData);

        JMenuItem print = new JMenuItem("Print...");
        print.addActionListener((ActionEvent e) -> {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintable(PlotBase.this);

                PrinterJob pj = PrinterJob.getPrinterJob();
                //PageFormat pdef=pj.defaultPage();
                //PageFormat pf = pj.pageDialog(pdef);
                //if (pf!=pdef){
                if (job.printDialog()) {
                    job.print();
                }
                //}
            } catch (Exception ex) {
                SwingUtils.showException(PlotBase.this, ex);
            }
        });
        addPopupMenuItem(print);

        JMenuItem copy = new JMenuItem("Copy");
        copy.addActionListener((ActionEvent e) -> {
            try {
                copy();
            } catch (Exception ex) {
                SwingUtils.showException(PlotBase.this, ex);
            }
        });
        addPopupMenuItem(copy);

        addPopupMenuItem(null);//Separator

        JMenuItem detachPlotMenuItem = new JMenuItem("Detach");
        detachPlotMenuItem.addActionListener((ActionEvent e) -> {
            detach(PlotBase.this.getClass().getName());
        });
        addPopupMenuItem(detachPlotMenuItem);

    }

    abstract protected String getDataAsString();

    abstract public void detach(String className);

    public void detach() {
        detach(getClass().getName());
    }

    /**
     * null for separator
     */
    abstract public void addPopupMenuItem(JMenuItem item);

    //Series list support
    final HashMap<String, T> seriesList = new HashMap<>();
    volatile int seriesID = 1;

    @Override
    public void addSeries(T series) {
        if (series.name == null) {
            return;
        }
        T existingSeries = getSeries(series.name);
        if (existingSeries == series) {
            return;
        }
        if (existingSeries != null) {
            removeSeries(existingSeries);
        }
        synchronized (seriesList) {
            seriesList.put(series.name, series);
        }
        series.setPlot(this);
        series.id = seriesID++;
        series.setToken(onAddedSeries(series));
    }

    @Override
    public void addSeries(T[] series) {
        for (T s : series) {
            addSeries(s);
        }
    }

    @Override
    public void removeSeries(T series) {
        if (series != null) {
            if (series.getPlot() == this) {
                series.setPlot(null);
            }
            onRemovedSeries(series);
            synchronized (seriesList) {
                seriesList.remove(series.name);
            }
        }
    }

    @Override
    public T getSeries(String name) {
        synchronized (seriesList) {
            return seriesList.get(name);
        }
    }

    @Override
    public T getSeries(int index) {
        synchronized (seriesList) {
            if ((index < 0) || (index >= seriesList.size())) {
                return null;
            }
            return getAllSeries()[index];
        }
    }

    @Override
    public String[] getSeriesNames() {
        T[] series = getAllSeries();
        ArrayList<String> ret = new ArrayList<>();
        for (T s : series) {
            ret.add(s.name);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public int getNumberOfSeries() {
        return seriesList.size();
    }

    public class SeriesComparator implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            return Integer.valueOf(o1.id).compareTo(Integer.valueOf(o2.id));
        }
    }

    @Override
    public T[] getAllSeries() {
        synchronized (seriesList) {
            ArrayList<T> list = new ArrayList();
            for (T series : seriesList.values()) {
                list.add(series);
            }
            Collections.sort(list, new SeriesComparator());
            return list.toArray((T[]) Array.newInstance(seriesType, 0));
        }
    }

    @Override
    public void requestSeriesUpdate(final T series) {
        if (series.updating.compareAndSet(false, true)) {
            invokeLater(() -> {
                series.updating.set(false);
                try {
                    updateSeries(series);
                } catch (Exception ex) {
                    //Logger.getLogger(LinePlotBase.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    protected void invokeLater(Runnable r) {
        if (offscreen) {
            executor.submit(r);
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    abstract protected Object onAddedSeries(T series);

    abstract protected void onRemovedSeries(T series);

    /**
     * These overridables can be optimised in implementations
     */
    protected void onRemovedAllSeries() {
        for (T s : getAllSeries()) {
            removeSeries(s);
        }
    }

    protected void doUpdate() {
        for (T s : getAllSeries()) {
            updateSeries(s);
        }
    }

    @Override
    public void clear() {
        onRemovedAllSeries();
        synchronized (seriesList) {
            seriesList.clear();
        }
        update(true);
    }

    //Axis
    final HashMap<AxisId, Axis> axisList = new HashMap<>();

    protected void createAxis(AxisId axis) {
        createAxis(axis, null);
    }

    protected void createAxis(AxisId axis, String label) {
        axisList.put(axis, new Axis(label, this, axis));
    }

    @Override
    public Axis getAxis(AxisId id) {
        return axisList.get(id);
    }

    /**
     * Implementations should override and return axis actually displayed (if
     * auto-range, this will return 0,0).
     */
    @Override
    public Range getAxisRange(AxisId axis) {
        return new Range(getAxis(axis).min, getAxis(axis).max);
    }

    //Axis Callbacks
    protected void onTitleChanged() {
        if (isUpdatesEnabled()) {
            doUpdate();
        }
    }

    protected void onAxisLabelChanged(AxisId axis) {
        if (isUpdatesEnabled()) {
            doUpdate();
        }
    }

    protected void onAxisRangeChanged(AxisId axis) {
        if (isUpdatesEnabled()) {
            doUpdate();
        }
    }

    //Markers are optional
    @Override
    public Object addMarker(double val, AxisId axis, String label, Color color) {
        return null;
    }

    @Override
    public Object addIntervalMarker(double start, double end, final AxisId axis, String label, Color color) {
        return null;
    }

    @Override
    public void removeMarker(final Object marker) {
    }

    @Override
    public List getMarkers() {
        return new ArrayList();
    }

    @Override
    public Object addText(double x, double y, String label, Color color) {
        return null;
    }

    @Override
    public void removeText(Object text) {
    }

    @Override
    public List getTexts() {
        return new ArrayList();
    }

    //Static Configuration
    //Hardware Acceleration
    static boolean hardwareAccelerated = true;

    public static void setHardwareAccelerated(boolean value) {
        hardwareAccelerated = value;
    }

    public static boolean getHardwareAccelerated() {
        return hardwareAccelerated;
    }

    static boolean lighweightPopups = true;

    public static void setLighweightPopups(boolean value) {
        lighweightPopups = value;
        if (javax.swing.JPopupMenu.getDefaultLightWeightPopupEnabled() != value) {
            javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(lighweightPopups);
        }
    }

    public static boolean getLighweightPopups() {
        return lighweightPopups;
    }

    //Preferred Colors (not all implementations may enable changing these colors)
    static Color plotBackground;  //Default is null: transparent

    public static void setPlotBackground(Color color) {
        plotBackground = color;
    }

    public static Color getPlotBackground() {
        return plotBackground;
    }

    static final Color DEFAULT_GRID_COLOR = MainFrame.isDark() ? new Color(187, 187, 187) : Color.LIGHT_GRAY;
    static Color gridColor = DEFAULT_GRID_COLOR;

    public static void setGridColor(Color color) {
        gridColor = (color == null) ? DEFAULT_GRID_COLOR : color;
    }

    public static Color getGridColor() {

        return gridColor;
    }

    static final Color DEFAULT_OUTLINE_COLOR = Color.GRAY;
    static Color outlineColor = DEFAULT_OUTLINE_COLOR;

    public static void setOutlineColor(Color color) {
        outlineColor = (color == null) ? DEFAULT_OUTLINE_COLOR : color;
    }

    public static Color getOutlineColor() {
        return outlineColor;
    }

    static Color axisTextColor = MainFrame.isDark() ? new Color(187, 187, 187) : Color.DARK_GRAY;

    public static void setAxisTextColor(Color color) {
        axisTextColor = color;
    }

    public static Color getAxisTextColor() {
        return axisTextColor;
    }

    static Colormap defaultColormap = Colormap.Temperature;

    public static void setDefaultColormap(Colormap color) {
        defaultColormap = color;
    }

    public static Colormap getDefaultColormap() {

        return defaultColormap;
    }
        
    public static void setDefaultLabelFont(Font font) {
        LABEL_FONT = font;
    }

    public static Font getDefaultLabelFont() {

        return LABEL_FONT;
    }

    public static void setDefaultTickFont(Font font) {
        TICK_LABEL_FONT = font;
    }

    public static Font getDefaultTickFont() {
        return TICK_LABEL_FONT;
    }

    static public int getMarkerSize() {
        String str = System.getProperty(PROPERTY_PLOT_MARKER_SIZE);
        if (str != null) {
            try {
                int size = Integer.valueOf(str);
                if (size > 0) {
                    return size;
                }
            } catch (Exception ex) {
            }
        }
        return 2;
    }

    @Override
    public String toString() {
        return Plot.class.getSimpleName();
    }

}
