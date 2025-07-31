package ch.psi.pshell.plot;

import ch.psi.pshell.app.MainFrame;
import ch.psi.pshell.utils.Convert;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;

/**
 *
 */
public class RangeSelectionPlot extends LinePlotJFree {

    public static class RangeSelection {

        public static final double EPSILON = 1e-8;

        public RangeSelection(double min, double max) {
            update(min, max);
        }
        double min;
        double max;
        double center;
        Object[] vars;

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getCenter() {
            return center;
        }

        public double getLength() {
            return max - min;
        }

        public Object[] getVars() {
            return vars;
        }

        public void setVars(Object[] vars) {
            this.vars = vars;
        }

        void update(double min, double max) {
            this.min = min;
            this.max = max;
            center = (min + max) / 2;
        }

        public boolean overlaps(RangeSelection r) {
            return overlaps(r.min, r.max);
        }

        public boolean overlaps(double min, double max) {
            return !((this.max < min) || (this.min > max));
        }

        public boolean equals(double min, double max) {
            return ((Math.abs(this.max - max) < EPSILON) && (Math.abs(this.min - min) < EPSILON));
        }

        public boolean equals(RangeSelection r) {
            return equals(r.min, r.max);
        }

        @Override
        public String toString() {
            return min + " to " + max;
        }

        public boolean contains(double value) {
            return (min <= value) && (value <= max);
        }

    }

    /**
     * The listener interface for receiving range selection plot events.
     */
    public interface RangeSelectionPlotListener {

        void onRangeAdded(RangeSelection range);

        void onRangeChanged(RangeSelection range);

        void onRangeRemoved(RangeSelection range);

        void onRangeSelected(RangeSelection range);

        void onDataChanged();
    }

    public RangeSelectionPlot() {
        super();
        notifying = new AtomicBoolean(false);
    }

    JMenuItem menuDeleteAll;

    @Override
    protected void createPopupMenu() {
        getChartPanel().getPopupMenu().addSeparator();
        menuDeleteAll = new JMenuItem("Delete all selections");
        menuDeleteAll.addActionListener((ActionEvent e) -> {
            removeAllRanges();
        });
        getChartPanel().getPopupMenu().add(menuDeleteAll);
    }

    RangeSelectionPlotListener listener;

    public void setListener(RangeSelectionPlotListener listener) {
        this.listener = listener;
    }

    public RangeSelectionPlotListener getListener() {
        return listener;
    }

    public RangeSelection addRange(double min, double max) throws IllegalArgumentException {
        min = Convert.roundDouble(min, getPrecision());
        max = Convert.roundDouble(max, getPrecision());
        RangeSelection ret = new RangeSelection(min, max);
        addRange(ret);
        return ret;
    }

    void addRange(RangeSelection range) throws IllegalArgumentException {
        if (range != null) {
            if (!ranges.containsKey(range)) {
                if (range.getLength() < getMinimumLength()) {
                    throw new IllegalArgumentException("Minimum range length is " + getMinimumLength());
                }
                if (!isOverlapAllowed()) {
                    for (RangeSelection r : getSelectedRanges()) {
                        if (r.overlaps(range)) {
                            throw new IllegalArgumentException("Selection overlaps existing range");
                        }
                    }
                }
                if (!isDuplicateAllowed()) {
                    for (RangeSelection r : getSelectedRanges()) {
                        if (r.equals(range)) {
                            throw new IllegalArgumentException("Selection matches existing range");
                        }
                    }
                }
                Object rangeMarker = RangeSelectionPlot.this.addIntervalMarker(range.min, range.max, AxisId.X, range.toString(), getSelectionColor());
                ranges.put(range, rangeMarker);
                if (listener != null) {
                    try {
                        listener.onRangeAdded(range);
                    } catch (Exception ex) {
                        Logger.getLogger(RangeSelectionPlot.class.getName()).log(Level.WARNING, title, ex);
                    }
                }
            }
        }
    }

    public void removeRange(RangeSelection r) {
        if (ranges.containsKey(r)) {
            Object rangeMarker = ranges.get(r);
            if (rangeMarker == selectedMarker) {
                deselectMarker();
            }
            removeMarker(rangeMarker);
            ranges.remove(r);
            if (listener != null) {
                try {
                    listener.onRangeRemoved(r);
                } catch (Exception ex) {
                    Logger.getLogger(RangeSelectionPlot.class.getName()).log(Level.WARNING, title, ex);
                }
            }
        }
    }

    public void updateRange(RangeSelection range, Double min, Double max) {
        if (ranges.containsKey(range)) {
            min = Convert.roundDouble(min, getPrecision());
            max = Convert.roundDouble(max, getPrecision());

            if ((max - min) < getMinimumLength()) {
                throw new IllegalArgumentException("Minimum range length is " + getMinimumLength());
            }
            if (!isOverlapAllowed()) {
                for (RangeSelection r : getSelectedRanges()) {
                    if (r != range) {
                        if (r.overlaps(min, max)) {
                            throw new IllegalArgumentException("Selection overlaps existing range");
                        }
                    }
                }
            }

            Object rangeMarker = ranges.get(range);
            removeMarker(rangeMarker);
            range.update(min, max);
            rangeMarker = RangeSelectionPlot.this.addIntervalMarker(range.min, range.max, AxisId.X, range.toString(), getSelectionColor());
            ranges.put(range, rangeMarker);
            if (listener != null) {
                try {
                    listener.onRangeChanged(range);
                } catch (Exception ex) {
                    Logger.getLogger(RangeSelectionPlot.class.getName()).log(Level.WARNING, title, ex);
                }
            }
        }
    }

    public void removeAllRanges() {
        for (RangeSelection range : getSelectedRanges()) {
            removeRange(range);
        }
        deselectMarker();
    }

    boolean manualSelectionEnabled = true;

    public boolean isManualSelectionEnabled() {
        return manualSelectionEnabled;
    }

    public void setManualSelectionEnabled(boolean value) {
        manualSelectionEnabled = value;
        menuDeleteAll.setEnabled(value);
        if (!value) {
            stopSelection();
            deselectMarker();
        }
    }

    public boolean hasData() {
        for (LinePlotSeries series : getAllSeries()) {
            XYSeries xySeries = getXYSeries(series);
            if (!xySeries.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSetData(LinePlotSeries series, double[] x, double[] y) {
        super.onSetData(series, x, y);
        SwingUtilities.invokeLater(() -> {
            if (!hasData()) {
                removeAllRanges();
            }
        });
        notifyDataChange();
    }

    @Override
    protected void onAppendData(LinePlotSeries series, double x, double y) {
        super.onAppendData(series, x, y);
        notifyDataChange();
    }

    private final AtomicBoolean notifying;

    void notifyDataChange() {
        if (listener != null) {
            if (notifying.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(() -> {
                    if (listener != null) {
                        notifying.set(false);
                        try {
                            listener.onDataChanged();
                        } catch (Exception ex) {
                            Logger.getLogger(RangeSelectionPlot.class.getName()).log(Level.WARNING, title, ex);
                        }
                    }
                });
            }
        }
    }

    final HashMap<RangeSelection, Object> ranges = new LinkedHashMap<>();

    public RangeSelection[] getSelectedRanges() {
        return ranges.keySet().toArray(new RangeSelection[0]);
    }

    public RangeSelection[] getSelectedRangesOrdered() {
        ArrayList<RangeSelection> list = new ArrayList<>();
        list.addAll(ranges.keySet());
        if (autoReorder) {
            Collections.sort(list, (RangeSelection o1, RangeSelection o2) -> Double.valueOf(o1.center).compareTo(Double.valueOf(o2.center)));
        }
        return list.toArray(new RangeSelection[0]);
    }

    int precision = 3;

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int value) {
        precision = value;
    }

    double minimumLength = Double.MIN_VALUE;

    public double getMinimumLength() {
        return minimumLength;
    }

    public void setMinimumLength(double value) {
        minimumLength = value;
    }

    Color selectionColor;

    public Color getSelectionColor() {
        return (selectionColor==null) ? MainFrame.isDark() ? new Color(65, 81, 109) : new Color(240, 240, 240) : selectionColor;
    }

    public void setSelectionColor(Color value) {
        selectionColor = value;
    }

    boolean multipleSelection;

    public boolean isMultipleSelection() {
        return multipleSelection;
    }

    public void setMultipleSelection(boolean value) {
        multipleSelection = value;
    }

    boolean overlapAllowed;

    public boolean isOverlapAllowed() {
        return overlapAllowed;
    }

    public void setOverlapAllowed(boolean value) {
        overlapAllowed = value;
    }

    boolean allowDuplicate;

    public void setDuplicateAllowed(boolean value) {
        allowDuplicate = value;
    }

    public boolean isDuplicateAllowed() {
        return allowDuplicate;
    }

    boolean autoReorder;

    public void setAutoReorder(boolean value) {
        autoReorder = value;
    }

    public boolean isAutoReorder() {
        return autoReorder;
    }

    @Override
    protected ChartPanel newChartPanel(JFreeChart chart) {
        return new ChartPanel(chart) {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                drawSelectionRectangle(g2);
                g2.dispose();
            }
        };
    }

    IntervalMarker selectedMarker;
    Stroke backStroke;
    Color backColor;

    @Override
    protected void createChart() {
        super.createChart();
        getChartPanel().setMouseZoomable(false, true);

        // Up arrow
        final String escKey = "cancel";
        getChartPanel().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), escKey);
        getChartPanel().getActionMap().put(escKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSelection();
                deselectMarker();
            }
        });
        final String delKey = "delete";
        getChartPanel().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), delKey);
        getChartPanel().getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), delKey);
        getChartPanel().getActionMap().put(delKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        });

        getChartPanel().addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isManualSelectionEnabled() && hasData()) {
                    if (e.isPopupTrigger()) {
                        stopSelection();
                    } else {
                        double v = getDomainValue(e.getX());
                        for (RangeSelection range : getSelectedRanges()) {
                            if (range.contains(v)) {
                                if (ranges.get(range) == selectedMarker) {
                                    deselectMarker();
                                } else {
                                    selectMarker(range);
                                }
                                return;
                            }
                        }

                        startSelection(e.getPoint());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isManualSelectionEnabled() && hasData()) {
                    try {
                        double[] range = getSelectionRange();
                        stopSelection();
                        if (range != null) {
                            addRange(range[0], range[1]);
                        }
                    } catch (Exception ex) {
                        showException(ex);
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        getChartPanel().addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isManualSelectionEnabled() && hasData()) {
                    updateSelection(e.getPoint());
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }

        });
    }

    void stopSelection() {
        selectionRectangle = null;
        startSelection = null;
        if (selectionAnno != null) {
            getChartPanel().getChart().getXYPlot().removeAnnotation(selectionAnno);
        }
        getChartPanel().repaint();

    }

    XYTextAnnotation selectionAnno;
    Point startSelection;

    void startSelection(Point p) {
        if (!isMultipleSelection()) {
            removeAllRanges();
        }

        Rectangle2D screenDataArea = getChartPanel().getScreenDataArea(p.x, p.y);
        if (screenDataArea != null) {
            XYPlot plot = getChartPanel().getChart().getXYPlot();
            startSelection = getPointInRectangle(new Point(p.x, p.y), screenDataArea);
            selectionAnno = new XYTextAnnotation("", plot.getDomainAxis().getRange().getCentralValue(), plot.getRangeAxis().getRange().getCentralValue());
            selectionAnno.setPaint(Color.DARK_GRAY);
            selectionAnno.setTextAnchor(TextAnchor.CENTER);
            selectionAnno.setFont(new Font(Font.SANS_SERIF, 0, 9));
            //SansSerif 9
            plot.addAnnotation(selectionAnno);
        }
    }

    void updateSelection(Point p) {
        if (startSelection != null) {
            Rectangle2D scaledDataArea = getChartPanel().getScreenDataArea((int) startSelection.getX(), (int) startSelection.getY());

            Point p1 = getPointInRectangle(startSelection, scaledDataArea);
            Point p2 = getPointInRectangle(p, scaledDataArea);
            selectionRectangle = new Rectangle(new Point(p1.x, (int) scaledDataArea.getMinY()));
            selectionRectangle.add(new Point(p2.x, (int) scaledDataArea.getMaxY()));

            //Graphics2D g2 = (Graphics2D) getChartPanel().getGraphics();
            //drawSelectionRectangle(g2);
            //g2.dispose();
            double[] range = getSelectionRange();
            if (range != null) {
                double center = (range[0] + range[1]) / 2;
                selectionAnno.setX(center);
                selectionAnno.setText(range[0] + " to " + range[1]);
            }
        }
    }

    double[] getSelectionRange() {
        if ((startSelection != null) && (selectionRectangle != null)) {
            Rectangle2D scaledDataArea = getChartPanel().getScreenDataArea((int) startSelection.getX(), (int) startSelection.getY());
            double v1 = (selectionRectangle.getMinX() - scaledDataArea.getMinX()) / scaledDataArea.getWidth();
            double v2 = (selectionRectangle.getMaxX() - scaledDataArea.getMinX()) / scaledDataArea.getWidth();

            Range r = getChartPanel().getChart().getXYPlot().getDomainAxis().getRange();
            v1 = v1 * r.getLength() + r.getLowerBound();
            v2 = v2 * r.getLength() + r.getLowerBound();
            v1 = Convert.roundDouble(v1, getPrecision());
            v2 = Convert.roundDouble(v2, getPrecision());
            if (v1 != v2) {
                return new double[]{v1, v2};
            }
        }
        return null;
    }

    double getDomainValue(double x) {
        Rectangle2D scaledDataArea = getChartPanel().getScreenDataArea();
        double v = (x - scaledDataArea.getMinX()) / scaledDataArea.getWidth();
        Range r = getChartPanel().getChart().getXYPlot().getDomainAxis().getRange();
        v = v * r.getLength() + r.getLowerBound();
        v = Convert.roundDouble(v, precision);
        return v;
    }

    Point getPointInRectangle(Point p, Rectangle2D area) {
        int x = (int) Math.max(Math.ceil(area.getMinX()), Math.min(p.x, Math.floor(area.getMaxX())));
        int y = (int) Math.max(Math.ceil(area.getMinY()), Math.min(p.y, Math.floor(area.getMaxY())));
        return new Point(x, y);
    }

    Rectangle2D selectionRectangle;

    void drawSelectionRectangle(Graphics2D g2) {
        // Set XOR mode to draw the zoom rectangle         
        if (this.selectionRectangle != null) {
            g2.setColor(Color.DARK_GRAY);
            g2.setXORMode(MainFrame.isDark() ? Color.WHITE : Color.BLACK);
            g2.fill(this.selectionRectangle);
            g2.setPaintMode();
        }
        // Reset to the default 'overwrite' mode

    }

    public void deselectMarker() {
        if (selectedMarker != null) {
            selectedMarker.setOutlineStroke(backStroke);
            selectedMarker.setOutlinePaint(backColor);
            selectedMarker.setLabelPaint(MainFrame.isDark() ? backColor.brighter() : backColor);
            selectedMarker = null;
            if (listener != null) {
                try {
                    listener.onRangeSelected(null);
                } catch (Exception ex) {
                    Logger.getLogger(RangeSelectionPlot.class.getName()).log(Level.WARNING, title, ex);
                }
            }
        }
    }

    public void selectMarker(RangeSelection range) {
        deselectMarker();
        if (range == null) {
            return;
        }
        selectedMarker = (IntervalMarker) ranges.get(range);
        if (selectedMarker != null) {
            backStroke = selectedMarker.getOutlineStroke();
            backColor = (Color) selectedMarker.getOutlinePaint();
            selectedMarker.setOutlineStroke(new BasicStroke(2f));
            selectedMarker.setOutlinePaint(MainFrame.isDark() ? backColor.brighter() : backColor.darker());
            selectedMarker.setLabelPaint(MainFrame.isDark() ? backColor.brighter().brighter() : selectedMarker.getOutlinePaint());
            if (listener != null) {
                try {
                    listener.onRangeSelected(range);
                } catch (Exception ex) {
                    Logger.getLogger(RangeSelectionPlot.class.getName()).log(Level.WARNING, title, ex);
                }
            }
        }
    }

    void deleteSelected() {
        if (selectedMarker != null) {
            for (RangeSelection range : getSelectedRanges()) {
                if (ranges.get(range) == selectedMarker) {
                    removeRange(range);
                }
            }
        }
    }
}
