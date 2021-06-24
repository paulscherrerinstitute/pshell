package ch.psi.pshell.imaging;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ContextAdapter;
import ch.psi.utils.Arr;
import ch.psi.utils.ArrayProperties;
import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import ch.psi.utils.Convert;
import ch.psi.utils.Observable;
import ch.psi.utils.ObservableBase;
import ch.psi.utils.Serializer;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import ch.psi.pshell.core.ContextListener;
import ch.psi.pshell.imaging.Overlays.Arrow;
import ch.psi.pshell.imaging.Overlays.Text;
import ch.psi.pshell.plot.ColormapPanel;
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.swing.PlotPanel;
import static ch.psi.pshell.swing.PlotPanel.getLinePlotImpl;
import static ch.psi.pshell.swing.PlotPanel.getMatrixPlotImpl;
import ch.psi.utils.Config;
import ch.psi.utils.Config.ConfigListener;
import ch.psi.utils.Range;
import ch.psi.utils.Sys;
import ch.psi.utils.Sys.OSFamily;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

/**
 */
public class Renderer extends MonitoredPanel implements ImageListener, ImageBuffer, Observable<RendererListener> {

    final JLabel painter;
    JScrollPane scrollPane;
    RendererStatusBar statusBar;
    boolean autoScroll;
    JPopupMenu popupMenu;
    Object currentOrigin;
    BufferedImage currentImage;
    Data currentData;
    Calibration calibration;
    boolean overlaysEnabled;
    final List<Overlay> overlays;
    Point currentMouseLocation;
    SnapshotDialog snapshotDialog;
    Overlay selectionOverlay;    
    SelectionType selectionType;   
    
    public enum SelectionType {
        Line,
        Horizontal,
        Vertical,
        Rectangle
    }    

    final Object imageLock = new Object();
    final Object overlaysLock = new Object();

    class Painter extends JLabel {

        Image doubleBufferImage = null;

        @Override
        protected void paintComponent(Graphics g) {
            BufferedImage image = getImage();
            Graphics gr = g;
            Rectangle visible = scrollPane.getViewport().getViewRect();
            double scaleX = getScaleX();
            double scaleY = getScaleY();

            if (backgroundRendering) {
                Rectangle rect = (Rectangle) Renderer.this.clip;
                if (rect != null) {
                    g.clipRect(rect.x, rect.y, rect.width, rect.height);
                }
                int width = getWidth();
                int height = getHeight();
                if (mode == RendererMode.Zoom) {
                    width = visible.width;
                    height = visible.height;
                }
                if ((doubleBufferImage == null) || (doubleBufferImage.getWidth(null) != width) || (doubleBufferImage.getHeight(null) != height)) {
                    doubleBufferImage = createImage(width, height);
                }
                g = doubleBufferImage.getGraphics();
            }

            Graphics originalGraphics = null;
            if (mode != RendererMode.Fixed) {
                originalGraphics = ((Graphics2D) g).create();
                ((Graphics2D) g).scale(scaleX, scaleY);
            }

            if (image != null) {
                if (mode == RendererMode.Zoom) {
                    int x = (int) (visible.x / scaleX);
                    int y = (int) (visible.y / scaleY);
                    int w = (int) Math.ceil(visible.width / scaleX) + 1; //+1 in order to fillout the sub-pixel empty space after the visuble ROI
                    int h = (int) Math.ceil(visible.height / scaleY) + 1;

                    //Limit to the data size
                    if ((x + w) > image.getWidth()) {
                        w = image.getWidth() - x;
                    }
                    if ((y + h) > image.getHeight()) {

                        h = image.getHeight() - y;
                    }
                    Image subImage = image.getSubimage(x, y, w, h);
                    if (backgroundRendering) {
                        g.drawImage(subImage, 0, 0, null);
                        //Position for following overlay drawing
                        g.translate(-x, -y);
                        originalGraphics.translate(-visible.x, -visible.y);
                    } else {
                        g.drawImage(subImage, x, y, null);
                    }

                } else {
                    g.drawImage(image, 0, 0, null);
                }
            }

            Overlay[] overlays = getOverlays();
            if ((overlays != null) && (overlaysEnabled)) {
                OverlayBase.zoomScaleX = scaleX;
                OverlayBase.zoomScaleY = scaleY;
                double imageWidth = image == null ? 0.0 : image.getWidth() * scaleX;
                double imageHeight = image == null ? 0.0 : image.getHeight() * scaleY;
                for (Overlay overlay : overlays) {
                    try {
                        manageOverlayOffset(overlay, imageWidth, imageHeight, visible);
                        if ((mode != RendererMode.Fixed) && ((overlay.isFixed()) || overlay.isManagingScaling())) {
                            overlay.paint(originalGraphics);
                        } else {
                            overlay.paint(g);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
            if (backgroundRendering) {
                if (mode == RendererMode.Zoom) {
                    int x = visible.x; //(int) (visible.x / scaleX);
                    int y = visible.y; //(int) (visible.y / scaleY);
                    gr.drawImage(doubleBufferImage, visible.x, visible.y, null);
                } else {
                    gr.drawImage(doubleBufferImage, 0, 0, null);
                }
            }
            statusBar.updateInfo();
            statusBar.updatePosition();
        }

    }

    void manageOverlayOffset(Overlay overlay, double imageWidth, double imageHeight, Rectangle visible) {
        if (overlay.isFixed() && (overlay.getAnchor() != Overlay.ANCHOR_IMAGE_TOP_LEFT)) {
            Point offset = new Point(0, 0);
            switch (overlay.getAnchor()) {
                case Overlay.ANCHOR_IMAGE_TOP_RIGHT:
                    offset.setLocation(imageWidth, 0);
                    break;
                case Overlay.ANCHOR_IMAGE_BOTTOM_LEFT:
                    offset.setLocation(0, imageHeight);
                    break;
                case Overlay.ANCHOR_IMAGE_BOTTOM_RIGHT:
                    offset.setLocation(imageWidth, imageHeight);
                    break;
                case Overlay.ANCHOR_VIEWPORT_TOP_LEFT:
                    offset.setLocation(visible.getLocation());
                    break;
                case Overlay.ANCHOR_VIEWPORT_TOP_RIGHT:
                    offset.setLocation(visible.x + visible.width, visible.y);
                    break;
                case Overlay.ANCHOR_VIEWPORT_BOTTOM_LEFT:
                    offset.setLocation(visible.x, visible.y + visible.height);
                    break;
                case Overlay.ANCHOR_VIEWPORT_BOTTOM_RIGHT:
                    offset.setLocation(visible.x + visible.width, visible.y + visible.height);
                    break;
                case Overlay.ANCHOR_VIEWPORT_OR_IMAGE_TOP_RIGHT:
                    offset.setLocation(Math.min(visible.x + visible.width, imageWidth), visible.y);
                    break;
                case Overlay.ANCHOR_VIEWPORT_OR_IMAGE_BOTTOM_LEFT:
                    offset.setLocation(visible.x, Math.min(visible.y + visible.height, imageHeight));
                    break;
                case Overlay.ANCHOR_VIEWPORT_OR_IMAGE_BOTTOM_RIGHT:
                    offset.setLocation(Math.min(visible.x + visible.width, imageWidth), Math.min(visible.y + visible.height, imageHeight));
                    break;
                case Overlay.ANCHOR_VIEWPORT_TOP:
                    offset.setLocation(0, visible.y);
                    break;
                case Overlay.ANCHOR_VIEWPORT_RIGHT:
                    offset.setLocation(visible.x + visible.width, 0);
                    break;
                case Overlay.ANCHOR_VIEWPORT_LEFT:
                    offset.setLocation(visible.x, 0);
                    break;
                case Overlay.ANCHOR_VIEWPORT_BOTTOM:
                    offset.setLocation(0, visible.y + visible.height);
                    break;
                case Overlay.ANCHOR_VIEWPORT_OR_IMAGE_RIGHT:
                    offset.setLocation(Math.min(visible.x + visible.width, imageWidth), 0);
                    break;
                case Overlay.ANCHOR_VIEWPORT_OR_IMAGE_BOTTOM:
                    offset.setLocation(0, Math.min(visible.y + visible.height, imageHeight));
                    break;
            }
            overlay.setOffset(offset);
        }
    }

    public static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

    public Renderer() {
        this.observableBridge = new ObservableBase<>();
        autoScroll = true;
        painter = new Painter();
        overlays = new ArrayList<>();
        overlaysEnabled = true;
        setLayout(new BorderLayout());
        painter.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        painter.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        painter.setBackground(null);
        painter.setCursor(DEFAULT_CURSOR);
        painter.setFocusable(true);
        statusBar = new RendererStatusBar(this);
        scrollPane = new JScrollPane();
        add(scrollPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        scrollPane.setViewportView(painter);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            boolean buttonPressed;

            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                    checkPopupMenu(e);
                    if (!e.isPopupTrigger() && e.getButton() == 1) {
                        buttonPressed = false;

                        onMouseUp(toImageCoord(e.getPoint()));
                        triggerMouseReleased(toImageCoord(e.getPoint()));
                    }
                } catch (Exception ex) {
                     Logger.getLogger(Renderer.class.getName()).log(Level.WARNING, null, ex);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    painter.requestFocus();
                    if (!e.isPopupTrigger() && e.getButton() == 1) {
                        buttonPressed = true;
                        if (e.getClickCount() % 2 == 0) {
                            lastDoubleClick = toImageCoord(e.getPoint());
                            onDoubleClick(new Point(lastDoubleClick));
                            triggerMouseDoubleClick(new Point(lastDoubleClick));
                        } else {
                            lastClick = toImageCoord(e.getPoint());
                            onMouseDown(new Point(lastClick));
                            triggerMousePressed(new Point(lastClick));
                        }
                    }
                    checkPopupMenu(e);
                } catch (Exception ex) {
                     Logger.getLogger(Renderer.class.getName()).log(Level.WARNING, null, ex);
                }
            }

            void checkPopupMenu(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    lastPopupClick = toImageCoord(e.getPoint());
                    onPopupMenu(e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                currentMouseLocation = e.getPoint();
                statusBar.updatePosition();
                try {
                    onMouseMove(toImageCoord(e.getPoint()));
                } catch (Exception ex) {
                }

                if (!e.isPopupTrigger() && buttonPressed) {
                    try {
                        onMouseDrag(toImageCoord(e.getPoint()));
                    } catch (Exception ex) {
                    }
                    triggerMouseDragged(toImageCoord(e.getPoint()));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                currentMouseLocation = e.getPoint();
                statusBar.updatePosition();
                try {
                    onMouseMove(toImageCoord(e.getPoint()));
                } catch (Exception ex) {
                }
                triggerMouseMoved(toImageCoord(e.getPoint()));
            }

        };

        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKeyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                onKeyReleased(e);
            }
        };

        painter.addMouseListener(mouseAdapter);
        painter.addMouseMotionListener(mouseAdapter);
        painter.addKeyListener(keyListener);

        setZoom(defaultZoom);
        setMode(RendererMode.Zoom);
    }

    public JPopupMenu getPopupMenu() {
        if (popupMenu == null) {
            popupMenu = new RendererMenu(this);        
        }
        return popupMenu;
    }

    //Popup menu
    protected void onPopupMenu(MouseEvent e) {
        getPopupMenu().setLightWeightPopupEnabled(false); //Not to be erased by rendering
        getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
    }

    public void setAutoScroll(boolean value) {
        if (autoScroll != value) {
            this.autoScroll = value;
            if (autoScroll) {
                if ((mode == RendererMode.Fixed) || (mode == RendererMode.Zoom)) {
                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                }
            } else {
                painter.setPreferredSize(getPreferredSize());
                painter.setSize(getCanvasVisibleSize());
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            }
        }
    }

    @Override
    public void requestFocus() {
        painter.requestFocus();
    }

    public JLabel getPainter() {
        return painter;
    }

    Dimension getCanvasVisibleSize() {
        return scrollPane.getViewport().getSize();
    }

    Dimension getCanvasExtentSize() {
        return scrollPane.getViewport().getExtentSize();
    }

    volatile Point lastClick;
    volatile Point lastDoubleClick;
    volatile Point lastPopupClick;

    /**
     * In image coordinates
     */
    public Point getLastClick() {
        return lastClick;
    }

    /**
     * In image coordinates
     */
    public Point getLastDoubleClick() {
        return lastDoubleClick;
    }

    /**
     * In image coordinates
     */
    public Point getLastPopupClick() {
        return lastPopupClick;
    }

    public Point waitClick(int timeout) throws TimeoutException, InterruptedException {
        lastClick = null;
        Chrono chrono = new Chrono();
        chrono.waitCondition(new Condition() {
            @Override
            public boolean evaluate() throws InterruptedException {
                return (lastClick != null);
            }
        }, timeout, 10);
        return lastClick;
    }

    public Point waitDoubleClick(int timeout) throws TimeoutException, InterruptedException {
        lastDoubleClick = null;
        Chrono chrono = new Chrono();
        chrono.waitCondition(new Condition() {
            @Override
            public boolean evaluate() throws InterruptedException {
                return (lastDoubleClick != null);
            }
        }, timeout, 10);
        return lastDoubleClick;
    }

    //Properties
    public boolean getAutoScroll() {
        return autoScroll;

    }

    RendererMode mode;

    public void setMode(RendererMode mode) {
        if (mode == null) {
            mode = RendererMode.Zoom;
        }
        if (mode != this.mode) {
            this.mode = mode;
            if (autoScroll) {
                if ((mode == RendererMode.Fixed) || (mode == RendererMode.Zoom)) {
                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                } else {
                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                }
            }
            refresh();  //To redefine the viewport area
            if ((mode == RendererMode.Fit) || (mode == RendererMode.Stretch)) {
                formerMode = null;
            }
            checkPersistence();
        }
    }

    public RendererMode getMode() {
        return mode;
    }

    boolean backgroundRendering;

    public boolean getBackgroundRendering() {
        return backgroundRendering;
    }

    public void setBackgroundRendering(boolean value) {
        if (value != backgroundRendering) {
            clip = null;
            backgroundRendering = value;
        }
    }

    public boolean getShowStatus() {
        return statusBar.isVisible();
    }

    public void setShowStatus(boolean value) {
        statusBar.setVisible(value);
        statusBar.update();
        checkPersistence();
    }

    public ColormapPanel getColormapPanel(){
        return colormapPanel;
    }
    
    ColormapPanel colormapPanel;
    ColormapSource colormapSource;
    final ConfigListener colormapSourceListener = new ConfigListener() {
        @Override
        public void onSave(Config config) {
            updateColormapScale();
        }
    };

    public boolean getShowColormapScale() {
        return colormapPanel != null;
    }

    public void setShowColormapScale(boolean value) {
        boolean current = colormapPanel != null;
        if (current != value) {
            if (value) {
                Object origin = getOrigin();
                colormapPanel = new ColormapPanel();
                setColormapSource(((origin == null) || (origin instanceof ColormapSource)) ? (ColormapSource) origin : null);
                add(colormapPanel, BorderLayout.EAST);
            } else {
                setColormapSource(null);
                if (colormapPanel != null) {
                    remove(colormapPanel);
                    colormapPanel = null;
                }
            }
            updateUI();
            checkPersistence();
        }
    }

    void setColormapSource(ColormapSource source) {
        if (colormapSource != null) {
            colormapSource.getConfig().removeListener(colormapSourceListener);
        }
        colormapSource = source;
        if (colormapSource != null) {
            updateColormapScale();
            colormapSource.getConfig().addListener(colormapSourceListener);
        } else {
            updateColormapScale(Colormap.Grayscale, new Range(0,1), false);
        }
    }

    void updateColormapScale() {
        Object origin = getOrigin();
        if (((origin != null) && (origin instanceof ColormapSource))) {
            ColormapSource source = (ColormapSource) origin;
            updateColormapScale(source.getConfig().colormap, source.getCurrentColormapRange(), source.getConfig().colormapLogarithmic);
        }
    }

    void updateColormapScale(Colormap colormap, Range range, Boolean log) {
        if (colormapPanel != null) {
            colormapPanel.update(colormap, range, log);
        }
    }
    
    volatile boolean updatingColormapScale;
    void requestColormapScaleUpdate(){
        if (updatingColormapScale==false){
            updatingColormapScale = true;
            SwingUtilities.invokeLater(()->{
                if (colormapSource!=null){
                    updatingColormapScale = false;
                    updateColormapScale(null, colormapSource.getCurrentColormapRange(), null);
                }
            });
        }
    }    

    Overlays.Reticle reticle;

    public Overlays.Reticle getReticle() {
        return reticle;
    }

    public boolean getShowReticle() {
        return (reticle != null);
    }

    public Calibration getCalibration() {
        if (calibration != null) {
            return calibration;
        }
        if ((currentData != null) && (currentData.calibration != null)) {
            return currentData.calibration;
        } else if ((currentOrigin != null) && (currentOrigin instanceof Source)) {
            return ((Source) currentOrigin).getCalibration();
        }
        return null;
    }

    public void setCalibration(Calibration calibration) {
        this.calibration = calibration;
        if (reticle != null) {
            reticle.setCalibration(calibration);
        }
    }

    Dimension reticleSize;
    double reticleTickUnits = 1.0;

    public void configureReticle(Dimension size, double tickUnits) {
        reticleSize = size;
        reticleTickUnits = tickUnits;
        if (reticle != null) {
            reticle.setSize(reticleSize);
            reticle.setTickUnits(reticleTickUnits);
        }
    }

    public void setShowReticle(boolean value) {
        if (value != getShowReticle()) {
            if (value) {
                if (getCalibration() != null) {
                    reticle = new Overlays.Reticle(penReticle);
                    reticle.setPassive(false);
                    reticle.setCalibration(getCalibration());
                    reticle.setSize((reticleSize == null) ? new Dimension(400, 200) : reticleSize);
                    reticle.setTickUnits(reticleTickUnits); //units
                    reticle.setZOrder(zOrderReticle); //Always below other overlays
                    addOverlay(reticle);
                }
            } else {
                removeOverlay(reticle);
                reticle = null;
            }
            checkPersistence();
        }
    }

    Arrow measureTool;
    Text measureToolText;

    public void setMeasureToolVisible(boolean value) {
        stopMeasureTool();
        if (value) {
            measureTool = new Arrow(penMeasureTool);
            measureTool.setArrowType(Arrow.ArrowType.both);
            measureTool.setCalibration(getCalibration());
            measureToolText = new Text(penMeasureTool, "", new Font("Monospaced", java.awt.Font.BOLD, 12));
            measureToolText.setPassive(false);
            addOverlay(measureToolText);
            startSelection(measureTool);
        }
    }

    public boolean isMeasureToolVisible() {
        return (measureTool != null) && (hasOverlay(measureTool));
    }

    void stopMeasureTool() {
        removeOverlay(measureTool);
        measureTool = null;
        removeOverlay(measureToolText);
        measureToolText = null;
    }

    double zoom;

    public void setZoom(Double zoom) {
        if (zoom == null) {
            zoom = defaultZoom;
        }
        if (zoom != this.zoom) {
            Point pos = null;
            try {
                pos = toImageCoord(scrollPane.getViewport().getViewPosition());
            } catch (Exception ex) {
            }
            this.zoom = zoom;
            if (getMode() == RendererMode.Zoom) {
                refresh();  //To redefine the viewport area
                if (pos != null) {
                    try {
                        setViewPosition(pos);
                    } catch (Exception ex) {
                    }
                }
            }
            repaint();
            checkPersistence();
        }
    }

    public void setViewPosition(Point imagePoint) {
        scrollPane.getViewport().setViewPosition(toViewCoord(imagePoint));
        checkPersistence();
    }

    public double getZoom() {
        return zoom;
    }

    public Dimension getImageSize() {
        BufferedImage image = getImage();
        if (image == null) {
            return null;
        }
        return new Dimension(image.getWidth(), image.getHeight());
    }

    public Dimension getViewSize() {
        BufferedImage image = getImage();
        if (image == null) {
            return null;
        }
        int width, height;

        switch (mode) {
            case Zoom:
                double z = getZoom();
                width = (int) (image.getWidth() * z);
                height = (int) (image.getHeight() * z);
                break;
            case Fit:
                double scale = getScaleX();  //Identical to ScaleY
                width = (int) Math.floor((image.getWidth()) * scale);
                height = (int) Math.floor((image.getHeight()) * scale);
                break;
            case Stretch:
                width = getCanvasVisibleSize().width;
                height = getCanvasVisibleSize().height;
                break;
            default:
                width = image.getWidth();
                height = image.getHeight();
                break;
        }
        return new Dimension(width, height);
    }

    RendererMode formerMode;

    public void zoomTo(Rectangle zoomImage) {
        if ((mode == RendererMode.Fit) || (mode == RendererMode.Stretch)) {
            formerMode = mode;
        }
        BufferedImage image = getImage();
        if ((image != null)) {
            Rectangle2D zoomImageClipped = zoomImage.createIntersection(new Rectangle(image.getWidth(), image.getHeight()));
            if ((zoomImageClipped.getWidth() > 0) && (zoomImageClipped.getHeight() > 0)) {
                Rectangle viewRect = scrollPane.getViewport().getViewRect();
                double scaleX = (double) viewRect.width / zoomImageClipped.getWidth();
                double scaleY = (double) viewRect.height / zoomImageClipped.getHeight();
                setZoom(Math.min(Math.min(scaleX, scaleY), 32.0));
                setMode(RendererMode.Zoom);
                refresh();  //To redefine the viewport area                
                try {
                    setViewPosition(zoomImageClipped.getBounds().getLocation());
                } catch (Exception ex) {
                }
                repaint();
            }
        }
    }

    double defaultZoom = 1.0;

    public void setDefaultZoom(double zoom) {
        defaultZoom = zoom;
    }

    public double getDefaultZoom() {
        return defaultZoom;
    }

    public void resetZoom() {
        if (zoom != defaultZoom) {
            if (formerMode != null) {
                setMode(formerMode);
            } else {
                setZoom(defaultZoom);
                setViewPosition(new Point(0, 0));
            }
        }
        formerMode = null;
    }

    //Scale
    public double getScaleX() {
        switch (mode) {
            case Zoom:
                return getZoom();
            case Fit:
                BufferedImage img = getImage();
                if ((img != null) && (img.getWidth() > 0) && (img.getHeight() > 0)) {
                    return Math.min(((double) getCanvasVisibleSize().width) / img.getWidth(),
                            ((double) getCanvasVisibleSize().height) / img.getHeight());
                }
                break;
            case Stretch:
                BufferedImage image = getImage();
                if ((image != null) && (image.getWidth() > 0)) {
                    return ((double) getCanvasVisibleSize().width) / image.getWidth();
                }
                break;
        }
        return 1.0;
    }

    public double getScaleY() {
        switch (mode) {
            case Zoom:
                return getZoom();
            case Fit:
                BufferedImage img = getImage();
                if ((img != null) && (img.getWidth() > 0) && (img.getHeight() > 0)) {
                    return Math.min(((double) getCanvasVisibleSize().width) / img.getWidth(),
                            ((double) getCanvasVisibleSize().height) / img.getHeight());
                }
                break;
            case Stretch:
                BufferedImage image = getImage();
                if ((image != null) && (image.getHeight() > 0)) {
                    return ((double) getCanvasVisibleSize().height) / image.getHeight();
                }
                break;
        }
        return 1.0;
    }

    //Coordinate conversion 
    public Point toImageCoord(Point viewCoord) {
        if ((mode) != RendererMode.Fixed) {
            return new Point(((int) (viewCoord.x / getScaleX())), ((int) (viewCoord.y / getScaleY())));
        }
        return viewCoord;
    }

    public Rectangle toImageCoord(Rectangle viewCoord) {
        Point p1 = toImageCoord(new Point(viewCoord.x, viewCoord.y));
        Point p2 = toImageCoord(new Point(viewCoord.x + viewCoord.width, viewCoord.y + viewCoord.height));
        return new Rectangle(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
    }

    public Point toViewCoord(Point imageCoord) {
        if ((mode) != RendererMode.Fixed) {
            return new Point(((int) (imageCoord.x * getScaleX())), ((int) (imageCoord.y * getScaleY())));
        }
        return imageCoord;
    }

    public Rectangle toViewCoord(Rectangle imageCoord) {
        Point p1 = toViewCoord(new Point(imageCoord.x, imageCoord.y));
        Point p2 = toViewCoord(new Point(imageCoord.x + imageCoord.width, imageCoord.y + imageCoord.height));
        return new Rectangle(p1.x, p1.y, p2.x, p2.y);
    }

    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
        BufferedImage image = getImage();
        if (pageIndex >= 1) {
            return Printable.NO_SUCH_PAGE;
        } else {
            double W, H, L, T;
            double scale = 0.8;
            W = (double) image.getWidth() * scale;
            H = (double) image.getHeight() * scale;
            L = pageFormat.getImageableX() + (pageFormat.getImageableWidth() / 2.0) - (W / 2.0);
            T = pageFormat.getImageableY() + (pageFormat.getImageableHeight() / 2.0) - (H / 2.0);
            g.drawImage(image, (int) L, (int) T, (int) (L + W), (int) (T + H), 0, 0, image.getWidth(), image.getHeight(), null);
            return Printable.PAGE_EXISTS;
        }
    }

    Overlay errorOverlay;

    @Override
    public void onError(Object origin, Exception ex) {
        clear();
        errorOverlay = new Overlays.Text(penErrorText, ex.toString(), new Font("Verdana", Font.PLAIN, 12), new Point(20, 20));
        addOverlay(errorOverlay);
        repaint();
    }

    volatile Dimension invokedSize;

    @Override
    public void onImage(Object origin, BufferedImage image, Data data) {
        if (paused) {
            return;
        }
        setImage(origin, image, data);
        triggerNewImage(origin, image, data);

    }

    volatile long frameCount;

    public long getFrameCount() {
        return frameCount;
    }

    long lastFrameRateTime;
    long lastFrameCount;

    public double getFrameRate() {
        if (isPaused() || (getImage() == null)) {
            return 0;
        }
        long now = System.currentTimeMillis();
        if (lastFrameRateTime == 0) {
            lastFrameRateTime = now;
            lastFrameCount = frameCount;
            return 0;
        }
        long timespan = now - lastFrameRateTime;
        long frames = frameCount - lastFrameCount;
        if (frames == 0) {
            return 1000.0 / timespan;
        } else {
            lastFrameRateTime = now;
            lastFrameCount = frameCount;
            if (lastFrameRateTime == 0) {
                return 0;
            } else {
                return 1000.0 * frames / timespan;
            }
        }
    }

    public void setImage(Object origin, BufferedImage image, Data data) {
        if (image != null) {
            if (errorOverlay != null) {
                removeOverlay(errorOverlay);
                errorOverlay = null;
            }
            if (autoScroll) {
                final Dimension d;
                switch (mode) {
                    case Fixed:
                        d = new Dimension(image.getWidth(), image.getHeight());
                        break;
                    case Zoom:
                        double z = getZoom();
                        d = new Dimension((int) (image.getWidth() * z), (int) (image.getHeight() * z));
                        break;
                    default:
                        d = getCanvasVisibleSize();
                }

                if (!d.equals(painter.getPreferredSize())) {
                    if ((invokedSize == null) || (!d.equals(invokedSize))) {
                        Runnable runnable = () -> {
                            invokedSize = null;
                            try {
                                painter.setPreferredSize(d);
                                painter.setSize(d);
                            } catch (Exception ex) {
                                Logger.getLogger(Renderer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        };
                        if (SwingUtilities.isEventDispatchThread()) {
                            runnable.run();
                        } else {
                            SwingUtilities.invokeLater(runnable);
                        }
                    }
                }
            }
        }
        synchronized (imageLock) {
            currentImage = image;
            currentData = data;
            currentOrigin = origin;
        }

        if (backgroundRendering) {
            JViewport viewport = scrollPane.getViewport();
            Graphics g = viewport.getGraphics();
            if (g != null) {
                if (image != null) {
                    Dimension viewSize = getViewSize();
                    Rectangle rect = (viewSize == null) ? null : new Rectangle(0, 0, viewSize.width, viewSize.height);
                    Shape c = clip;
                    if ((rect == null) || (c == null) || (!c.equals(rect))) {
                        clip = rect;
                        viewport.paint(viewport.getGraphics());
                    } else {
                        painter.paint(painter.getGraphics());
                    }
                } else {
                    clip = null;
                    viewport.paint(viewport.getGraphics());
                    lastFrameRateTime = 0;
                }
            }
        } else {
            repaint();
        }
        if (image != null) {
            frameCount++;
            synchronized (waitLock) {
                waitLock.notifyAll();
            }
        }
        if (getShowColormapScale() && (origin != null) && (origin != this)) {
            if ((origin instanceof ColormapSource)) {
                if (origin != colormapSource) {
                    setColormapSource((ColormapSource)origin);
                }
                if (colormapSource.getConfig().colormapAutomatic){
                    requestColormapScaleUpdate();
                }                
            } else {
                setShowColormapScale(false);
            }
        }
        checkProfile();
    }

    volatile Shape clip = null;

    public void clear() {
        clearOverlays();
        clearImage();
    }

    public void clearImage() {
        onImage(this, null, null);
    }
    
    public void refresh() {      
        try{
            if ((selectedOverlayMarker != null) && (selectedOverlay!=null)){
                selectedOverlayMarker.setPosition(selectedOverlay.getPosition());
                selectedOverlayMarker.setSize(selectedOverlay.getSize());
            }           
        } catch (Exception ex){            
        }
        synchronized (imageLock) {
            setImage(getOrigin(), getImage(), getData());
        }
        updateDataSelectionDialog();
    }

    boolean paused = false;

    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
        lastFrameRateTime = 0;
    }

    @Override
    public BufferedImage getImage() {
        synchronized (imageLock) {
            return currentImage;
        }
    }

    public Data getData() {
        synchronized (imageLock) {
            if ((currentData == null) && (currentImage != null)) {
                currentData = new Data(currentImage);
            }
            return currentData;
        }
    }

    public Object getOrigin() {
        synchronized (imageLock) {
            return currentOrigin;
        }
    }

    public BufferedImage getImage(boolean withOverlays) {
        BufferedImage ret = getImage();
        if (ret == null) {
            return null;
        }

        if (withOverlays) {
            Overlay[] ovs = getOverlays();
            if ((ovs != null) && (ovs.length > 0)) {
                if ((Utils.isGrayscale(ret)) && (getOverlaysEnabled())) {
                    ret = Utils.copy(ret, BufferedImage.TYPE_INT_RGB, null);
                } else {
                    ret = Utils.copy(ret, null, null);
                }
                Graphics g = ret.createGraphics();

                //TODO: This should be synchronized if if not in event loop (or a better solution used)              
                OverlayBase.zoomScaleX = 1.0;
                OverlayBase.zoomScaleY = 1.0;
                Rectangle visible = new Rectangle(ret.getWidth(), ret.getHeight());
                for (Overlay overlay : ovs) {
                    manageOverlayOffset(overlay, ret.getWidth(), ret.getHeight(), visible);
                    try {
                        overlay.paint(g);
                    } catch (Exception ex) {
                    }
                }
                g.dispose();
            }
        }
        return ret;
    }

    public void saveSnapshot(String fileName, String format, boolean overlays) throws IOException {
        ImageBuffer.saveImage(getImage(overlays), fileName, format);
    }

    public BufferedImage getDisplayedImage() {
        BufferedImage image = getImage();
        if (image != null) {
            if (mode != RendererMode.Fixed) {
                Dimension d = getViewSize();
                try {
                    image = Utils.stretch(image, d.width, d.height);
                } catch (Throwable ex) {   //Heap erros are disregarded here
                }
            }
        }
        return image;
    }

    final Object waitLock = new Object();

    @Override
    public void waitNext(int timeout) throws InterruptedException, TimeoutException {
        Chrono chrono = new Chrono();
        int wait = Math.max(timeout, 0);
        long current = frameCount;
        while (frameCount == current) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            synchronized (waitLock) {
                waitLock.wait(wait);
            }
            if (wait > 0) {
                wait = timeout - chrono.getEllapsed();
                if (wait <= 0) {
                    throw new java.util.concurrent.TimeoutException();
                }
            }
        }
    }

    public BufferedImage getNext(boolean withOverlays, int timeout) throws InterruptedException, TimeoutException {
        waitNext(timeout);
        return getImage(withOverlays);
    }

    //Overlays
    public void setOverlaysEnabled(boolean value) {
        synchronized (overlaysLock) {
            overlaysEnabled = value;
        }
    }

    public boolean getOverlaysEnabled() {
        return overlaysEnabled;
    }

    public void setOverlays(Overlay[] overlays) {
        synchronized (overlays) {
            Overlay[] remove = this.overlays.toArray(new Overlay[0]);
            updateOverlays(overlays, remove);
        }
    }

    public void clearOverlays() {
        synchronized (overlays) {
            Overlay[] remove = overlays.toArray(new Overlay[0]);
            removeOverlays(remove);
        }
    }

    public Overlay[] getOverlays() {
        synchronized (overlaysLock) {
            return overlays.toArray(new Overlay[0]);
        }
    }

    public boolean hasOverlay(Overlay ov) {
        synchronized (overlaysLock) {
            return overlays.contains(ov);
        }
    }

    public void addOverlay(Overlay overlay) {
        updateOverlays(overlay, null);
    }

    public void removeOverlay(Overlay overlay) {
        updateOverlays(null, overlay);
    }

    public void addOverlays(Overlay[] overlays) {
        updateOverlays(overlays, null);
    }

    public void removeOverlays(Overlay[] overlays) {
        updateOverlays(null, overlays);
    }

    public void updateOverlays(Overlay add, Overlay remove) {
        updateOverlays((add == null) ? null : new Overlay[]{add}, (remove == null) ? null : new Overlay[]{remove});
    }

    public void updateOverlays(Overlay[] add, Overlay[] remove) {
        boolean removedSelection = false;
        synchronized (overlays) {
            if (remove != null) {
                for (Overlay overlay : remove) {
                    overlays.remove(overlay);
                    if (overlay == selectedOverlay) {
                        removedSelection = true;
                    }
                }
            }
            if (add != null) {
                add = Arr.removeNulls(add);
                //this.overlays.addAll(Arrays.asList(add));
                for (Overlay overlay : add) {
                    if (!overlays.contains(overlay)) {
                        overlays.add(overlay);
                    }
                }
            }
            Collections.sort(overlays, new Overlay.ZOrderComparator());
            repaint();
        }

        if (removedSelection) {
            setSelectedOverlay(null);
        }
        if (remove != null) {
            for (Overlay overlay : remove) {
                onOverlayDeleted(overlay);
            }
        }
        if (add != null) {
            for (Overlay overlay : add) {
                onOverlayAdded(overlay);
            }
        }
    }

    protected void onOverlayAdded(Overlay inserted) {
    }

    protected void onOverlayDeleted(Overlay deleted) {
    }

    //Fixed overlay coordinates adjustment
    Point toOverlayCoord(Overlay ov, Point p) {
        return ov.isFixed() ? toViewCoord(p) : p;
    }

    boolean contains(Overlay ov, Point p) {
        return ov.contains(toOverlayCoord(ov, p));
    }

    //Default overlay pens
    Pen penProfile = new Pen(new Color(0, 128, 0), 0);

    public void setPenProfile(Pen value) {
        penProfile = value;
    }

    public Pen getPenProfile() {
        return penProfile;
    }

    Pen penErrorText = new Pen(Color.GREEN);

    public void setPenErrorText(Pen value) {
        penErrorText = value;
    }

    public Pen getPenErrorText() {
        return penErrorText;
    }

    Pen penMovingOverlay = new Pen(new Color(0x80, 0x80, 0x40));

    public void setPenMovingOverlay(Pen value) {
        penMovingOverlay = value;
    }

    public Pen getPenMovingOverlay() {
        return penMovingOverlay;
    }

    Pen penSelectedOverlay = new Pen(new Color(0x80, 0x80, 0x40), 2, Pen.LineStyle.dashed);

    public void setPenSelectedOverlay(Pen value) {
        penSelectedOverlay = value;
    }

    public Pen getPenSelectedOverlay() {
        return penSelectedOverlay;
    }

    Pen penMarker = new Pen(new Color(0x00, 0x00, 0x80));

    public void setPenMarker(Pen value) {
        penMarker = value;
        if (marker != null) {
            marker.setPen(value);
        }
    }

    public Pen getPenMarker() {
        return penMarker;
    }

    int zOrderReticle = -1;
    Pen penReticle = new Pen(new Color(0, 128, 0));

    public void setPenReticle(Pen value) {
        penReticle = value;
        if (reticle != null) {
            reticle.setPen(value);
        }
    }

    public Pen getPenReticle() {
        return penReticle;
    }

    Pen penMeasureTool = new Pen(new Color(10, 96, 10), 0, Pen.LineStyle.solid);

    public void setPenMeasureTool(Pen value) {
        penMeasureTool = value;
    }

    public Pen getPenMeasureTool() {
        return penMeasureTool;
    }
    
    public Pen getPenMouseSelecting() {
        return new Pen(penSelectedOverlay.getColor(), 1, Pen.LineStyle.dotted);
    }
    
    Pen penDataSelection = new Pen(penSelectedOverlay.getColor());
            
    public void setPenDataSelection(Pen pen) {
        penDataSelection = pen;
    }    
    
    public Pen getPenDataSelection() {
        return penDataSelection;
    }    
    

    /**
     * Sets vertical/horizontal image profile (integration)
     */
    public enum Profile {

        None,
        Vertical,
        Horizontal,
        Both;

        public boolean hasVertical() {
            return (this == Vertical) || (this == Both);
        }

        public boolean hasHorizontal() {
            return (this == Horizontal) || (this == Both);
        }
    }

    Profile profile = Profile.None;

    public void setProfile(Profile profile) {
        if (profile==null){
            profile = Profile.None;
        }
        this.profile = profile;
        checkProfile();
    }

    public Profile getProfile() {
        return profile;
    }

    public Overlay[] getProfileOverlays() {
        return profileOverlays;
    }

    Overlay[] profileOverlays = null;
    int profileSize = -1;
    int zOrderProfile = -2;

    public void setProfileSize(int value) {
        profileSize = value;
    }

    public int getProfileSize() {
        return profileSize;
    }

    boolean showProfileLimits = true;

    public void setShowProfileLimits(boolean value) {
        showProfileLimits = value;
    }

    public boolean getShowProfileLimits() {
        return showProfileLimits;
    }

    boolean profileNormalized = false;

    public void setProfileNormalized(boolean value) {
        profileNormalized = value;
    }

    public boolean getProfileNormalized() {
        return profileNormalized;
    }

    private void checkProfile() {
        BufferedImage img = currentImage;
        Overlays.Line hline = null, vline = null;
        Overlays.Polyline hpoly = null, vpoly = null;
        if ((img != null) || (profileOverlays != null)) {
            Data data = getData();
            Overlay[] overlays = null;
            if ((profile != Profile.None) && (img != null)) {
                int profileSize = (this.profileSize > 0) ? this.profileSize : Math.min(img.getWidth(), img.getHeight()) / 4;
                //Int and double images are always normalized
                double maxPlot = 255;
                double minPlot = 0;
                if (profileNormalized || ((data.getType() != byte.class) && (data.getType() != short.class))) {
                    ArrayProperties properties = data.getProperties();
                    maxPlot = properties.max;
                    minPlot = properties.min;
                } else {
                    if (data.getType() == byte.class) {
                        maxPlot = data.isUnsigned() ? 0xFF : 127;
                        minPlot = data.isUnsigned() ? 0 : -128;
                    } else if (data.getType() == short.class) {
                        maxPlot = data.isUnsigned() ? 0xFFFF : 2047;
                        minPlot = data.isUnsigned() ? 0 : -2048;
                    }
                }
                double rangePlot = maxPlot - minPlot;

                if (profile.hasVertical()) {
                    double[] sum = (data == null) ? (double[]) Convert.toDouble(Utils.integrateVertically(img)) : data.integrateVertically(true);
                    int[] y = new int[img.getWidth()];
                    if (rangePlot > 0) {
                        for (int i = 0; i < img.getWidth(); i++) {
                            y[i] = (int) (img.getHeight() - 1 - (((sum[i] / img.getHeight() - minPlot) / rangePlot) * profileSize));
                        }
                    }
                    vpoly = new Overlays.Polyline(penProfile, Arr.indexesInt(img.getWidth()), y);
                    vpoly.setPassive(false);
                    if (showProfileLimits) {
                        int max = img.getHeight() - 1 - profileSize;
                        vline = new Overlays.Line(new Pen(penProfile.getColor(), 0, Pen.LineStyle.dotted), new Point(0, max), new Point(img.getWidth() - 1, max));
                        vline.setPassive(false);
                    }
                }
                if (profile.hasHorizontal()) {
                    double[] sum = (data == null) ? (double[]) Convert.toDouble(Utils.integrateHorizontally(img)) : data.integrateHorizontally(true);
                    int[] x = new int[img.getHeight()];
                    if (rangePlot > 0) {
                        for (int i = 0; i < img.getHeight(); i++) {
                            x[i] = (int) (((sum[i] / img.getWidth() - minPlot) / rangePlot) * profileSize);
                        }
                    }
                    hpoly = new Overlays.Polyline(penProfile, x, Arr.indexesInt(img.getHeight()));
                    hpoly.setPassive(false);
                    if (showProfileLimits) {
                        int max = profileSize;
                        hline = new Overlays.Line(new Pen(penProfile.getColor(), 0, Pen.LineStyle.dotted), new Point(max, 0), new Point(max, img.getHeight() - 1));
                        hline.setPassive(false);
                    }
                }
                overlays = new Overlay[]{hpoly, hline, vpoly, vline};
                for (Overlay o : overlays) {
                    if (o != null) {
                        o.setZOrder(zOrderProfile);
                    }
                }
            }
            updateOverlays(overlays, profileOverlays);
            profileOverlays = overlays;
        } else {
            profileOverlays = null;
        }
    }

    Overlay marker;

    /**
     * Sets a special overlay that is persisted within the renderer state
     */
    public void setMarker(Overlay marker) {
        if (this.marker != null) {
            removeOverlay(this.marker);
        }
        this.marker = marker;
        if (!hasOverlay(marker)) {
            addOverlay(marker);
        }
        checkPersistence();
    }

    public Overlay getMarker() {
        return marker;
    }

    //Overlay selection
    Overlay selectedOverlay = null;
    Overlay selectedOverlayMarker = null;

    public void setSelectedOverlay(Overlay overlay) {
        try {
            if (selectedOverlay != overlay) {
                if (selectedOverlayMarker != null) {
                    removeOverlay(selectedOverlayMarker);
                }
                selectedOverlay = overlay;
                if (selectedOverlay != null) {
                    selectedOverlayMarker = selectedOverlay.copy();
                    selectedOverlayMarker.setPen(new Pen(selectedOverlay.getColor(), penSelectedOverlay.lineWidth, penSelectedOverlay.lineStyle));
                    addOverlay(selectedOverlayMarker);
                }
                triggerSelectedOverlayChanged(selectedOverlay);
            }
        } catch (Exception ex) {
        }
    }
    
    public Overlay getSelectedOverlay(){
        return selectedOverlay;
    }

    //Mouse operations: Overlay mouse definition, selections & movement
    Overlay mouseSelectionOverlay = null;
    Overlay movingOverlay = null;
    Overlay movingOriginalOverlay = null;
    Point movingStartPosition = null;
    Point movingReferencePosition = null;

    protected void onMouseDown(Point p) throws IOException {
        if ((controlKey) && (mouseSelectionOverlay == null) && (zoomToSelection == null)) {
            onZoomTo();
            if (zoomToSelection != null) {
                onMouseDown(p);
            }
            return;
        }

        setSelectedOverlay(null);
        if (mouseSelectionOverlay != null) {
            Point pov = toOverlayCoord(mouseSelectionOverlay, p);
            if (mouseSelectionOverlay instanceof Overlays.Rect) {
                ((Overlays.Rect) mouseSelectionOverlay).update(pov, pov);
            } else if (mouseSelectionOverlay instanceof Overlays.Line) {
                ((Overlays.Line) mouseSelectionOverlay).update(pov, pov);
            } else if (mouseSelectionOverlay instanceof Overlays.Dot) {
                ((Overlays.Dot) mouseSelectionOverlay).update(pov);
                repaint();
                stopSelection(false);
                return;
            }
            triggerSelecting(mouseSelectionOverlay);
            repaint();
        } else {
            for (Overlay overlay : getOverlays()) {
                if ((overlay.isMovable()) && contains(overlay, p)) {
                    movingStartPosition = overlay.getPosition();
                    movingOverlay = overlay.copy();
                    movingOverlay.setPen(penMovingOverlay);
                    movingOverlay.setPosition(movingStartPosition);
                    movingReferencePosition = toOverlayCoord(overlay, p);
                    movingOriginalOverlay = overlay;
                    addOverlay(movingOverlay);
                    if ((overlay == selectedOverlay) && (selectedOverlayMarker != null)) {
                        removeOverlay(selectedOverlayMarker);
                        selectedOverlayMarker = null;
                    }
                    triggerMoveStarted(overlay);
                    break;
                }
            }
        }
        if (measureToolText != null) {
            measureToolText.update("");
        }

        checkCursorType(p);
    }

    protected void onDoubleClick(Point p) {
        if (mouseSelectionOverlay != null) {
        }
    }

    protected void onMouseDrag(Point p) {
        if (mouseSelectionOverlay != null) {
            Point pov = toOverlayCoord(mouseSelectionOverlay, p);
            if (mouseSelectionOverlay instanceof Overlays.Rect) {
                if (mouseSelectionOverlay.getPosition().equals(OverlayBase.UNDEFINED_POINT)) {
                    ((Overlays.Rect) mouseSelectionOverlay).update(pov, pov);
                } else {
                    ((Overlays.Rect) mouseSelectionOverlay).update(pov);
                }
                triggerSelecting(mouseSelectionOverlay);
                repaint();
            } else if (mouseSelectionOverlay instanceof Overlays.Line) {
                if (mouseSelectionOverlay.getPosition().equals(OverlayBase.UNDEFINED_POINT)) {
                    ((Overlays.Line) mouseSelectionOverlay).update(pov, pov);
                } else {
                    ((Overlays.Line) mouseSelectionOverlay).update(pov);
                }
                triggerSelecting(mouseSelectionOverlay);
                if (mouseSelectionOverlay == measureTool) {
                    String str = String.format("%.8G", (getCalibration() != null) ? measureTool.getAbsoluteLength() : measureTool.getLength());
                    Dimension textSize = SwingUtils.getTextSize(str, getGraphics().getFontMetrics());
                    textSize = new Dimension((int) (textSize.width / getScaleX()), (int) (textSize.height / getScaleY()));
                    measureToolText.update(new Point(measureTool.getCenter().x - textSize.width / 2, measureTool.getCenter().y - textSize.height / 2));
                    measureToolText.update(str);
                }
                repaint();
            }
        }
        if (movingOverlay != null) {
            Point start = movingOriginalOverlay.getPosition();
            Point position = movingOverlay.getPosition();
            p = toOverlayCoord(movingOverlay, p);
            movingOverlay.setPosition(new Point(start.x + (p.x - movingReferencePosition.x), start.y + (p.y - movingReferencePosition.y)));
            triggerMoving(movingOverlay);
            repaint();
        }
    }

    protected void onMouseUp(Point p) {
        if (mouseSelectionOverlay != null) {
            Point pov = toOverlayCoord(mouseSelectionOverlay, p);
            if (mouseSelectionOverlay instanceof Overlays.Rect) {
                ((Overlays.Rect) mouseSelectionOverlay).update(pov);
            } else if (mouseSelectionOverlay instanceof Overlays.Line) {
                ((Overlays.Line) mouseSelectionOverlay).update(pov);
            }
            repaint();
            stopSelection(false);
        }
        if (movingOverlay != null) {
            Point position = movingOriginalOverlay.getPosition();
            Point restore = new Point(position);
            Point end = toOverlayCoord(movingOverlay, p);
            movingOriginalOverlay.setPosition(new Point(position.x + (end.x - movingReferencePosition.x), position.y += (end.y - movingReferencePosition.y)));
            endMove(false);
        }

        Overlay selected = null;
        for (Overlay overlay : getOverlays()) {
            if ((overlay.isSelectable()) && contains(overlay, p)) {
                selected = overlay;
                break;
            }
        }
        setSelectedOverlay(selected);
        checkCursorType(p);
    }

    protected void onMouseMove(Point p) {
        if (mouseSelectionOverlay != null) {
            Point pov = toOverlayCoord(mouseSelectionOverlay, p);
            if (mouseSelectionOverlay instanceof Overlays.Dot) {
                ((Overlays.Dot) mouseSelectionOverlay).update(pov);
                triggerSelecting(mouseSelectionOverlay);
                repaint();
            }
        }
        checkCursorType(p);
    }

    boolean controlKey;

    public void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        boolean is_mac = (Sys.getOSFamily() == OSFamily.Mac);
        if ((!is_mac && (key == KeyEvent.VK_CONTROL)) || (is_mac && (key == KeyEvent.VK_SHIFT))) {
            controlKey = true;
            if ((mouseSelectionOverlay == null) && (zoomToSelection == null)) {
                onZoomTo();
            }
            e.consume();
            return;
        }
        if (key == KeyEvent.VK_ESCAPE) {
            setSelectedOverlay(null);
        }
        if (mouseSelectionOverlay != null) {
            abortSelection();
        } else if (movingOverlay != null) {
            abortMove();
        } else if (selectedOverlay != null) {
            if (key == KeyEvent.VK_DELETE) {
                Overlay overlay = selectedOverlay;
                removeOverlay(selectedOverlay);
                triggerDeleted(overlay);
            } else if (selectedOverlay.isMovable()) {
                Point p = selectedOverlay.getPosition();
                Point position = null;
                ///TODO: limit to renderer?
                switch (key) {
                    case KeyEvent.VK_UP:
                        position = new Point(p.x, p.y - 1);
                        break;
                    case KeyEvent.VK_DOWN:
                        position = new Point(p.x, p.y + 1);
                        break;
                    case KeyEvent.VK_LEFT:
                        position = new Point(p.x - 1, p.y);
                        break;
                    case KeyEvent.VK_RIGHT:
                        position = new Point(p.x + 1, p.y);
                        break;
                }
                if (position != null) {
                    selectedOverlay.setPosition(position);
                    if (selectedOverlayMarker != null) {
                        selectedOverlayMarker.setPosition(position);
                    }
                    repaint();
                    triggerMoveFinished(selectedOverlay);
                    e.consume();
                }
            }
        }
    }

    public void onKeyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        try {
            boolean is_mac = (Sys.getOSFamily() == OSFamily.Mac);
            if ((!is_mac && (key == KeyEvent.VK_CONTROL)) || (is_mac && (key == KeyEvent.VK_SHIFT))) {
                controlKey = false;
                if ((mouseSelectionOverlay != null) && (mouseSelectionOverlay == zoomToSelection)) {
                    abortSelection();
                }
            }
        } catch (Exception ex) {
        }
    }

    Overlays.Rect zoomToSelection;

    protected void onZoomTo() {
        zoomToSelection = new Overlays.Rect(new Pen(penSelectedOverlay.getColor(), 1, Pen.LineStyle.dotted));
        addListener(new RendererListener() {
            @Override
            public void onSelectionFinished(Renderer renderer, Overlay overlay) {
                if (overlay == zoomToSelection) {
                    try {
                        if ((overlay.getUtmost().x < overlay.getPosition().x) && (overlay.getUtmost().y < overlay.getPosition().y)) {
                            resetZoom();
                        } else {
                            zoomTo(overlay.isFixed() ? toImageCoord(overlay.getBounds()) : overlay.getBounds());
                        }
                    } catch (Exception ex) {
                    } finally {
                        zoomToSelection = null;
                    }
                    removeListener(this);
                }
            }

            @Override
            public void onSelectionAborted(Renderer renderer, Overlay overlay) {
                if (overlay == zoomToSelection) {
                    zoomToSelection = null;
                    renderer.removeListener(this);
                }
            }
        });
        zoomToSelection.setFixed(true);
        startSelection(zoomToSelection);
    }

    //Mouse overlay creation start/stop
    public void startSelection(Overlay overlay) {
        stopSelection(true);
        mouseSelectionOverlay = overlay;
        if ((measureTool != null) && (measureTool != overlay)) {
            stopMeasureTool();
        }
        if (overlay != null) {
            addOverlay(mouseSelectionOverlay);
            triggerSelectionStarted(overlay);
        }
    }

    public void abortSelection() {
        stopSelection(true);
    }

    void stopSelection(boolean abort) {
        if (mouseSelectionOverlay != null) {
            Overlay overlay = mouseSelectionOverlay;
            removeOverlay(mouseSelectionOverlay);
            mouseSelectionOverlay = null;
            if (abort) {
                stopMeasureTool();
                triggerSelectionAborted(overlay);
            } else {
                triggerSelectionFinished(overlay);
                if (measureTool == overlay) {
                    addOverlay(measureTool);
                    startSelection(measureTool);
                }
            }
        }
    }

    public Point getMovingOverlayOffset() {
        if ((movingStartPosition == null) || (movingReferencePosition == null)) {
            return new Point(0, 0);
        }
        return new Point(movingReferencePosition.x - movingStartPosition.x, movingReferencePosition.y - movingStartPosition.y);
    }

    public void abortMove() {
        endMove(true);
    }

    void endMove(boolean abort) {
        if (movingOverlay != null) {
            removeOverlay(movingOverlay);
            movingOverlay = null;
            movingReferencePosition = null;
            if (abort) {
                triggerMoveAborted(movingOriginalOverlay);
            } else {
                triggerMoveFinished(movingOriginalOverlay);
            }
        }
    }

    private Cursor customCursor = Cursor.getDefaultCursor();

    private void checkCursorType(Point p) {
        Cursor cursor = DEFAULT_CURSOR;
        if (mouseSelectionOverlay != null) {
            cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
            //} else if (getShowStatus()) {
            //    cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        } else if (movingOverlay != null) {
            cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        } else {
            for (Overlay overlay : getOverlays()) {
                if (overlay.isMovable() && contains(overlay, p)) {
                    cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                }
            }
        }
        painter.setCursor(cursor);
    }
    
    
    //Data selections
    
    final RendererListener selectionListener = new RendererListener() {
        @Override
        public void onImage(Renderer renderer, Object origin, BufferedImage image, Data data) {
            if (selectionOverlay != null) {
                updateDataSelectionDialogPlot();
            }
        }

        @Override
        public void onError(Renderer renderer, Object origin, Exception ex) {
            if (selectionOverlay != null) {
                cleanPlot();
            }
        }

        @Override
        public void onMoveFinished(Renderer renderer, Overlay overlay) {
            if (selectionOverlay != null) {
                if (overlay == selectionOverlay) {
                    updateDataSelectionDialog();
                }
            }
        }

        @Override
        public void onDeleted(Renderer renderer, Overlay overlay) {
            if (selectionOverlay != null) {
                if (overlay == selectionOverlay) {
                    removeDataSelection();
                }
            }
        }

    };    

    protected void addDataSelectionOverlay(Overlay overlay) {
        removeDataSelectionOverlay();
        selectionOverlay = overlay;
        selectionOverlay.setSelectable(true);
        selectionOverlay.setMovable(true);
        Pen pen = getPenDataSelection();
        if (pen!=null){
            selectionOverlay.setPen(getPenDataSelection());
        }
        addOverlay(selectionOverlay);
        addListener(selectionListener);
        
    }

    protected void removeDataSelectionOverlay() {
        abortSelection();
        removeListener(selectionListener);
        if (selectionOverlay != null) {
            removeOverlay(selectionOverlay);
            selectionOverlay = null;
        }
    }
    
    protected void addDataSelectionDialog() {
        if (dialogPlot == null) {
            dialogPlot = new JDialog((Window) getTopLevelAncestor(), "Data Selection");
            dialogPlot.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialogPlot.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    removeDataSelectionOverlay();
                    removeIntegration();
                }
            });
            dialogPlot.getContentPane().setLayout(new BorderLayout());
            dialogPlot.setSize(480, 320);
            SwingUtils.centerComponent(getTopLevelAncestor(), dialogPlot);
        }
        dialogPlot.getContentPane().removeAll();
        try {
            if (selectionType != null) {
                Rectangle bounds = selectionOverlay.getBounds();
                if (selectionOverlay.isFixed()) {
                    bounds = toImageCoord(bounds);
                }
                double minX = bounds.x;
                double maxX = bounds.x + bounds.width - 1;
                int nX = bounds.width;
                double minY = bounds.y;
                double maxY = bounds.y + bounds.height - 1;
                int nY = bounds.height;

                switch (selectionType) {
                    case Line:
                    case Horizontal:
                    case Vertical:
                        plot = (PlotBase) Plot.newPlot(getLinePlotImpl());
                        series = new LinePlotSeries("Selection");
                        plot.getAxis(Plot.AxisId.X).setLabel(null);
                        plot.getAxis(Plot.AxisId.Y).setLabel(null);
                        break;
                    case Rectangle:
                        plot = (PlotBase) Plot.newPlot(getMatrixPlotImpl());
                        plot.getAxis(Plot.AxisId.Y).setInverted(true);
                        //Rectangle dataRect = getData().getInverseRect(rect);                        
                        //series = new MatrixPlotSeries("Selection", minX, maxX, dataRect.width, minY, maxY, dataRect.height );
                        series = new MatrixPlotSeries("Selection", minX, maxX, nX, minY, maxY, nY);
                        PlotPanel.addSurfacePlotMenu((MatrixPlotBase) plot);
                        break;
                }
            } else if (integration != null) {
                plot = (PlotBase) Plot.newPlot(getLinePlotImpl());
                series = new LinePlotSeries("Integration");
                plot.getAxis(Plot.AxisId.X).setLabel(null);
                plot.getAxis(Plot.AxisId.Y).setLabel(null);
            }

            plot.setTitle(null);
            plot.addSeries(series);
            plot.setQuality(PlotPanel.getQuality());
            dialogPlot.getContentPane().add(plot);
            dialogPlot.setVisible(true);
            updateDataSelectionDialog();
            painter.requestFocus();
        } catch (Exception ex) {
            showException(ex);
            removeDataSelectionDialog();
        }
    }

    protected void removeDataSelectionDialog() {
        selectionType = null;
        removeIntegration();
        if (dialogPlot != null) {
            dialogPlot.setVisible(false);
        }
    }
     
    

    JDialog dialogPlot;
    PlotBase plot;
    PlotSeries series;
    
    protected void updateDataSelectionDialog() {
        try{
            updateDataSelectionDialogTitle();
            updateDataSelectionDialogPlot();
        } catch (Exception ex){            
        } 
    }

    protected void updateDataSelectionDialogPlot() {
        if ((selectionType != null) && (selectionOverlay != null) && (series != null)) {
            Data data = getData();
            BufferedImage img = getImage();
            double[] x = null;
            double[] y = null;

            Rectangle bounds = selectionOverlay.getBounds();
            if (selectionOverlay.isFixed()) {
                bounds = toImageCoord(bounds);
            }

            switch (selectionType) {
                case Horizontal:
                    x = data.getRowSelectionX(true);
                    ((LinePlotBase) plot).getAxis(Plot.AxisId.X).setRange(data.getX(0), data.getX(img.getWidth() - 1));
                    ((LinePlotSeries) series).setData(x, data.getRowSelection(bounds.y, true));
                    break;
                case Vertical:
                    x = data.getColSelectionX(true);
                    ((LinePlotBase) plot).getAxis(Plot.AxisId.X).setRange(data.getY(0), data.getY(img.getHeight() - 1));
                    ((LinePlotSeries) series).setData(x, data.getColSelection(bounds.x, true));
                    break;
                case Line:
                    ((LinePlotSeries) series).setData(data.getLineSelection(bounds.getLocation(), new Point(bounds.x + bounds.width, bounds.y + bounds.height), true));
                    break;
                case Rectangle:
                    MatrixPlotSeries mps = ((MatrixPlotSeries) series);
                    if (bounds.width!= mps.getNumberOfBinsX()){
                        mps.setNumberOfBinsX(bounds.width);                              
                    }
                    if (bounds.height!= mps.getNumberOfBinsY()){
                        mps.setNumberOfBinsY(bounds.height);                              
                    }                    
                    double minBoundsX = data.getX(bounds.x);
                    double maxBoundsX = data.getX(bounds.x + bounds.width);
                    double minBoundsY = data.getY(bounds.y);
                    double maxBoundsY = data.getY(bounds.y + bounds.height);
                    mps.setRangeX(minBoundsX, maxBoundsX);
                    mps.setRangeY(minBoundsY, maxBoundsY);
                    plot.getAxis(Plot.AxisId.X).setRange(minBoundsX, maxBoundsX);
                    plot.getAxis(Plot.AxisId.Y).setRange(minBoundsY, maxBoundsY);
                    mps.setData(data.getRectSelection(bounds, true));
                    break;
            }
        } else if (integration != null) {
            Data data = getData();
            double[] x = null;
            double[] y = null;
            
            if (integration){
                //vertical
                x = data.getRowSelectionX(true);
                double[] iv = (double[]) Convert.toDouble(data.integrateVertically(true));
                ((LinePlotBase) plot).getAxis(Plot.AxisId.X).setRange(data.getX(0), data.getX(iv.length - 1));
                ((LinePlotSeries) series).setData(x, iv);
            } else {
                //horizontal
                x = data.getColSelectionX(true);
                double[] ih = (double[]) Convert.toDouble(data.integrateHorizontally(true));
                ((LinePlotBase) plot).getAxis(Plot.AxisId.X).setRange(data.getY(0), data.getY(ih.length - 1));
                ((LinePlotSeries) series).setData(x, ih);
            }
        }
    }

    protected void updateDataSelectionDialogTitle() {
        if ((selectionType != null) && (selectionOverlay != null) && (series != null)) {
            String title = null;
            Point p = selectionOverlay.getPosition();
            Point u = selectionOverlay.getUtmost();
            switch (selectionType) {
                case Horizontal:
                    title = "row: " + p.y;
                    break;
                case Vertical:
                    title = "column: " + p.x;
                    break;
                case Line:
                    title = "line: " + "[" + p.x + "," + p.y + "]" + " to " + "[" + u.x + "," + u.y + "]";
                    break;
                case Rectangle:
                    Rectangle r = selectionOverlay.getBounds();
                    title = "rectangle: " + "[" + r.x + "," + r.y + "]" + " to " + "[" + (r.x + r.width) + "," + (r.y + r.height) + "]";
                    break;
            }
            if ((dialogPlot != null) && (title != null)) {
                title = "Data Selection  (" + title + ")";
            }
            if (!title.equals(dialogPlot.getTitle())) {
                dialogPlot.setTitle(title);
            }
        }
    }

    protected void cleanPlot() {
        if (series != null) {
            series.clear();
        }
    }
            
    RendererListener integrationListener = new RendererListener() {
        @Override
        public void onImage(Renderer renderer, Object origin, BufferedImage image, Data data) {
            updateDataSelectionDialogPlot();
        }
    };
    
    //Starting data selection
    public void startDataSelection(SelectionType type) {
        Overlay selection = null;
        selectionType = type;
        removeIntegration();
        switch (selectionType) {
            case Line:
                selection = new Overlays.Arrow(getPenMouseSelecting());
                ((Overlays.Arrow) selection).setArrowType(Overlays.Arrow.ArrowType.end);
                //selection = new Overlays.Line(PEN_MOVING_OVERLAY);
                break;
            case Horizontal:
                selection = new Overlays.Crosshairs(getPenMouseSelecting(), new Dimension(-1, 1));
                break;
            case Vertical:
                selection = new Overlays.Crosshairs(getPenMouseSelecting(), new Dimension(1, -1));
                break;
            case Rectangle:
                selection = new Overlays.Rect(getPenMouseSelecting());
                //selection.setSolid(true);
                break;
        }
        if (selection != null) {
            removeDataSelectionOverlay();
            addListener(new RendererListener() {
                @Override
                public void onSelectionFinished(Renderer renderer, Overlay overlay) {
                    try {
                        if (overlay.getLength() > 0) {
                            Overlay dataSelection = overlay.copy();                        
                            addDataSelection(dataSelection);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(RendererMenu.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        renderer.removeListener(this);
                    }
                }

                @Override
                public void onSelectionAborted(Renderer renderer, Overlay overlay) {
                    renderer.removeListener(this);
                    removeDataSelectionDialog();
                }
            });
            selection.setPassive(false);
            startSelection(selection);
        }        
    }    
    
    public void addDataSelection(Overlay dataSelection){
        if (dataSelection!=null){
            if (selectionType == null) {
                if (dataSelection instanceof Overlays.Rect){
                    selectionType = SelectionType.Rectangle;
                } else if (dataSelection instanceof Overlays.Line){
                    selectionType = SelectionType.Line;
                } else if (dataSelection instanceof Overlays.Crosshairs){
                    if (dataSelection.getSize().width<0){
                        selectionType = SelectionType.Horizontal;
                    }
                    if (dataSelection.getSize().height<0){
                        selectionType = SelectionType.Vertical;
                    }
                }  
            }
            if (selectionType!=null){
                addDataSelectionOverlay(dataSelection);
                addDataSelectionDialog();        
                return;
            }
        }
        removeDataSelection();        
    }
    
    public void removeDataSelection(){
        removeDataSelectionOverlay();
        removeDataSelectionDialog();           
    }
    
    public Overlay getDataSelection(){
        return selectionOverlay;
    }    
    
    //Integration
    Boolean integration;
    public void addIntegration(boolean vertical) {
        selectionType = null;
        removeDataSelectionOverlay();
        integration = vertical;
        addDataSelectionDialog();
        addListener(integrationListener);
        dialogPlot.setTitle((vertical ? "Vertical" : "Horizontal") + " Integration");
    }

    public void removeIntegration() {
        removeListener(integrationListener);
        integration = null;
    }    

    //Callbacks & Listeners
    final ObservableBase<RendererListener> observableBridge;

    @Override
    public void addListener(RendererListener listener) {
        observableBridge.addListener(listener);
    }

    @Override
    public List<RendererListener> getListeners() {
        return observableBridge.getListeners();
    }

    @Override
    public void removeListener(RendererListener listener) {
        observableBridge.removeListener(listener);
    }

    @Override
    public void removeAllListeners() {
        observableBridge.removeAllListeners();
    }

    protected void triggerNewImage(Object origin, BufferedImage image, Data data) {
        for (RendererListener listener : getListeners()) {
            try {
                listener.onImage(this, origin, image, data);
            } catch (Exception ex) {
            }
        }
    }

    protected void triggerError(Object origin, Exception ex) {
        for (RendererListener listener : getListeners()) {
            try {
                listener.onError(this, origin, ex);
            } catch (Exception e) {
            }
        }
    }

    protected void triggerMoveStarted(Overlay overlay) {
        if (overlay != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMoveStarted(this, overlay);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerMoving(Overlay overlay) {
        if (overlay != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMoving(this, overlay);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerMoveFinished(Overlay overlay) {
        if (overlay != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMoveFinished(this, overlay);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerMoveAborted(Overlay overlay) {
        if (overlay != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMoveAborted(this, overlay);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerSelectionStarted(Overlay overlay) {
        for (RendererListener listener : getListeners()) {
            try {
                listener.onSelectionStarted(this, overlay);
            } catch (Exception ex) {
            }
        }
    }

    protected void triggerSelecting(Overlay overlay) {
        if (overlay != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onSelecting(this, overlay);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerSelectionFinished(Overlay overlay) {
        for (RendererListener listener : getListeners()) {
            try {
                listener.onSelectionFinished(this, overlay);
            } catch (Exception ex) {
            }
        }
    }

    protected void triggerSelectionAborted(Overlay overlay) {
        for (RendererListener listener : getListeners()) {
            try {
                listener.onSelectionAborted(this, overlay);
            } catch (Exception ex) {
            }
        }
    }

    protected void triggerSelectedOverlayChanged(Overlay overlay) {
        for (RendererListener listener : getListeners()) {
            try {
                listener.onSelectedOverlayChanged(this, overlay);
            } catch (Exception ex) {
            }
        }
    }

    protected void triggerDeleted(Overlay overlay) {
        if (overlay != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onDeleted(this, overlay);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerInserted(Overlay overlay) {
        if (overlay != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onInserted(this, overlay);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerMousePressed(Point p) {
        if (p != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMousePressed(this, p);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerMouseReleased(Point p) {
        if (p != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMouseReleased(this, p);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerMouseDoubleClick(Point p) {
        if (p != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMouseDoubleClick(this, p);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerMouseMoved(Point p) {
        if (p != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMouseMoved(this, p);
                } catch (Exception ex) {
                }
            }
        }
    }

    protected void triggerMouseDragged(Point p) {
        if (p != null) {
            for (RendererListener listener : getListeners()) {
                try {
                    listener.onMouseDragged(this, p);
                } catch (Exception ex) {
                }
            }
        }
    }

    //Fixed source name
    private String deviceName;

    public void setDeviceName(String value) {
        deviceName = value;
        Context context = Context.getInstance();
        if (context != null) {
            if (contextListener != null) {
                context.removeListener(contextListener);
                contextListener = null;
            }
            if (context.getState().isInitialized()) {
                setDevice(context.getDevicePool().getByName(deviceName, Source.class));
            }
            if ((value != null) && (!value.isEmpty())) {
                contextListener = new ContextAdapter() {
                    @Override
                    public void onContextInitialized(int runCount) {
                        setDevice(context.getDevicePool().getByName(deviceName, Source.class));
                    }
                };
                context.addListener(contextListener);
            }
        }
    }

    ContextListener contextListener;

    @Override
    protected void onDesactive() {
        if (contextListener != null) {
            Context.getInstance().removeListener(contextListener);
            contextListener = null;
        }
    }

    public String getDeviceName() {
        if (deviceName == null) {
            return "";
        }
        return deviceName;
    }

    Source device;

    public void setDevice(final Source device) {
        if (this.device != null) {
            this.device.removeListener(this);
        }
        this.device = device;
        if (device != null) {
            if (this.isShowing()) {
                device.addListener(this);
            }
            device.refresh();
        }
    }

    public Source getDevice() {
        return device;
    }

    @Override
    protected void onShow() {
        if (device != null) {
            device.addListener(this);
            device.refresh();
        }
    }

    @Override
    protected void onHide() {
        if (device != null) {
            device.removeListener(this);
        }
        checkPersistence();
    }

    public void persistState(Path file) {
        RendererState state = new RendererState();
        state.mode = getMode();
        state.scale = getShowColormapScale();
        state.zoom = getZoom();
        state.scaleX = getScaleX();
        state.scaleY = getScaleY();
        state.reticle = getShowReticle();
        state.imagePosition = toImageCoord(scrollPane.getViewport().getViewPosition());
        state.status = getShowStatus();
        state.marker = marker;
        state.profile = profile;
        state.calibration = calibration;
        state.formerMode = formerMode;
        try {
            Files.write(file, Serializer.encode(state, Serializer.EncoderType.bin));
        } catch (Exception ex) {
            Logger.getLogger(Renderer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    boolean restoringState;

    public void restoreState(Path file) {
        try {
            restoringState = true;
            RendererState state = (RendererState) Serializer.decode(Files.readAllBytes(file));
            setShowStatus(state.status);
            setShowColormapScale(state.scale);
            setCalibration(state.calibration);
            setMode(state.mode);
            setZoom(state.zoom);
            formerMode = state.formerMode;
            if (state.marker != null) {
                setMarker(state.marker);

            }
            if (state.profile != null) {
                setProfile(state.profile);
            }
            new Thread(() -> {
                try {
                    waitNext(10000);
                    SwingUtils.invokeDelayed(() -> {
                        setViewPosition(state.imagePosition);
                        //If state had no reticule, but a reticule has been enabled, do nothing.
                        if (state.reticle) {
                            setShowReticle(true);
                        }
                    }, 100);
                } catch (Exception ex) {
                }
            }).start();
        } catch (NoSuchFileException ex) {
        } catch (Exception ex) {
            Logger.getLogger(Renderer.class.getName()).log(Level.WARNING, null, ex);
        }
        restoringState = false;
    }

    Path persistenceFile;

    public void setPersistenceFile(Path file) {
        persistenceFile = file;
        if (persistenceFile != null) {
            restoreState(persistenceFile);
        }
    }

    public Path getPersistenceFile() {
        return persistenceFile;
    }

    void checkPersistence() {
        if (persistenceFile != null) {
            if (!restoringState) {
                persistState(persistenceFile);
            }
        }
    }

    //Snapshot dialog
    public void setSnapshotDialogVisible(boolean value) {
        if (value) {
            snapshotDialog = new SnapshotDialog(this);
            snapshotDialog.setVisible(true);
        } else {
            if (snapshotDialog != null) {
                snapshotDialog.setVisible(false);
            }
            snapshotDialog = null;
        }
    }

    public boolean isSnapshotDialogVisible() {
        return ((snapshotDialog != null) && (snapshotDialog.isShowing()));
    }

    //Internal source management
    Source source;

    public void setSource(Source source) {
        if (source != this.source) {
            this.source = source;
        }
    }

    public Source getSource() {
        return source;
    }

}
