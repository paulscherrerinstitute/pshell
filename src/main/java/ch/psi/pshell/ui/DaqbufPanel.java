package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.ProviderCSV;
import ch.psi.pshell.data.ProviderText;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.LinePlotErrorSeries;
import ch.psi.pshell.plot.LinePlotJFree;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlotJFree;
import ch.psi.pshell.plot.MatrixPlotRenderer;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.Plot.AxisId;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.plot.SlicePlotDefault;
import ch.psi.pshell.plot.SlicePlotSeries;
import ch.psi.pshell.plot.SlicePlotSeries.SlicePlotSeriesListener;
import ch.psi.pshell.plot.TimePlotBase;
import ch.psi.pshell.plotter.Preferences;
import ch.psi.pshell.swing.ChannelSelector;
import ch.psi.pshell.swing.DataPanel;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.Daqbuf;
import ch.psi.utils.Daqbuf.Query;
import ch.psi.utils.Daqbuf.QueryListener;
import ch.psi.utils.Daqbuf.QueryRecordListener;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.IO;
import ch.psi.utils.Range;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.utils.swing.SwingUtils.OptionResult;
import ch.psi.utils.swing.SwingUtils.OptionType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

/**
 *
 */
public class DaqbufPanel extends StandardDialog {

    public static final String PLOT_PRIVATE = "Private";
    public static final String PLOT_SHARED = "Shared";

    Color defaultBackgroundColor = null;
    Color defaultGridColor = null;
    Font tickLabelFont = null;
    boolean pulse;

    final DefaultTableModel modelSeries;

    final ArrayList<PlotBase> plots = new ArrayList<>();
    final HashMap<Device, Long> appendTimestamps = new HashMap<>();
    volatile boolean started = false;

    Color backgroundColor;
    Color gridColor;
    final Daqbuf daqbuf;
    int numPlots = 0;
    
    final Color GRAYED_COLOR =new Color(0xA0, 0xA0, 0xA0, 0x40);
    
    class ChartElement {

        ChartElement(TimePlotBase plot, int seriesIndex, long time, double value) {
            this.plot = plot;
            this.seriesIndex = seriesIndex;
            this.time = time;
            this.value = value;
        }
        int seriesIndex;
        long time;
        double value;
        TimePlotBase plot;
    }
    public DaqbufPanel(Window parent, String url, String title, boolean modal) {
        super(parent, title, modal);        
        initComponents();
        daqbuf = new Daqbuf(url);
        if (getTitle()==null){
            setTitle(daqbuf.getUrl());
        }
        if (App.hasArgument("background_color")) {
            try {
                defaultBackgroundColor = Preferences.getColorFromString(App.getArgumentValue("background_color"));
                backgroundColor = defaultBackgroundColor;
            } catch (Exception ex) {
                Logger.getLogger(DaqbufPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (App.hasArgument("grid_color")) {
            try {
                defaultGridColor = Preferences.getColorFromString(App.getArgumentValue("grid_color"));
                gridColor = defaultGridColor;
            } catch (Exception ex) {
                Logger.getLogger(DaqbufPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (App.hasArgument("tick_label_font")) {
            try {
                String[] tokens = App.getArgumentValue("tick_label_font").split(":");
                tickLabelFont = new Font(tokens[0], Font.PLAIN, Integer.valueOf(tokens[1]));
            } catch (Exception ex) {
                Logger.getLogger(DaqbufPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }

        buttonPlot.setEnabled(false);
        setCancelledOnEscape(false);

        //pnGraphs.setLayout(new GridBagLayout());
        pnGraphs.setLayout(new BoxLayout(pnGraphs, BoxLayout.Y_AXIS));

        modelSeries = (DefaultTableModel) tableSeries.getModel();
        modelSeries.addTableModelListener(modelSeriesListener);

        initializeTable();
        clear();

        onLafChange();
    }

    //Access functions
    public JTabbedPane getTabbedPane() {
        return this.tabPane;
    }

    public JPanel getPlotPanel() {
        return panelPlots;
    }

    public JPanel getConfigPanel() {
        return panelSeries;
    }
    
    
    public void initializeTable() {
        //Fix bug of nimbus rendering Boolean in table
        ((JComponent) tableSeries.getDefaultRenderer(Boolean.class)).setOpaque(true);
        tableSeries.getColumnModel().getColumn(0).setResizable(true);

        TableColumn colEnabled = tableSeries.getColumnModel().getColumn(0);
        colEnabled.setPreferredWidth(60);

        TableColumn colName = tableSeries.getColumnModel().getColumn(1);
        JTextField textNameEditor = new JTextField();
        ChannelSelector selector = new ChannelSelector();
        //selector.configure(ChannelSelector.Type.Daqbuf, null,  null, 1000);
        selector.setHistorySize(0);
        selector.setListMode(ChannelSelector.ListMode.Popup);
        
        
        colName.setPreferredWidth(320);
        colName.setCellEditor(new ChannelSelector.ChannelSelectorCellEditor(selector));    
        
        colName.getCellEditor().addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                int row = tableSeries.getSelectedRow();
                if (row>=0){
                    String channel = (String) modelSeries.getValueAt(row, 1);
                    String backend = (String) modelSeries.getValueAt(row, 2);
                    try {
                        daqbuf.startSearch(backend, channel, null, 1).handle((ret,ex)->{
                            if (ex!=null){
                                showException((Exception)ex);
                            } else {
                                List<Map<String, Object>> list = (List<Map<String, Object>>) ret;
                                SwingUtilities.invokeLater(()->{
                                    if (channel.equals(modelSeries.getValueAt(row, 1)) && backend.equals(modelSeries.getValueAt(row, 2))){
                                        String shape = (list.size()>0) ?  Str.toString(list.get(0).getOrDefault("shape", "")) : "";
                                        modelSeries.setValueAt(shape, row, 3);
                                    }
                                });
                            }
                            return ret;
                        });
                    } catch (Exception ex) {
                        showException(ex);
                    }
                }
            }

            @Override
            public void editingCanceled(ChangeEvent e) {
            }
        });
        
        
        TableColumn colType = tableSeries.getColumnModel().getColumn(2);
        colType.setPreferredWidth(120);
        DefaultComboBoxModel modelType = new DefaultComboBoxModel();
        for (String backend: daqbuf.getBackends()) {
            modelType.addElement(backend);
        }        
        JComboBox comboBackend = new JComboBox();      
        tableSeries.setRowHeight(Math.max(tableSeries.getRowHeight(), comboBackend.getPreferredSize().height - 3));
        comboBackend.setModel(modelType);
        DefaultCellEditor cellEditor = new DefaultCellEditor(comboBackend);
        cellEditor.setClickCountToStart(2);
        colType.setCellEditor(cellEditor);                         
        comboBackend.addActionListener((e)->{
            selector.configure(ChannelSelector.Type.Daqbuf, null,  comboBackend.getSelectedItem().toString(), 1000);
        });   
        comboBackend.setSelectedIndex(0);
        
        TableColumn colShape = tableSeries.getColumnModel().getColumn(3);
        colShape.setPreferredWidth(60);
        
        TableColumn colPlot = tableSeries.getColumnModel().getColumn(4);
        colPlot.setPreferredWidth(60);
        JComboBox comboPlot = new JComboBox();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(PLOT_PRIVATE);
        model.addElement(PLOT_SHARED);
        comboPlot.setModel(model);
        cellEditor = new DefaultCellEditor(comboPlot);
        cellEditor.setClickCountToStart(2);
        colPlot.setCellEditor(cellEditor);

        TableColumn colY = tableSeries.getColumnModel().getColumn(5);
        colY.setPreferredWidth(60);
        JComboBox comboY = new JComboBox();
        model = new DefaultComboBoxModel();
        model.addElement(1);
        model.addElement(2);
        comboY.setModel(model);
        cellEditor = new DefaultCellEditor(comboY);
        cellEditor.setClickCountToStart(2);
        colY.setCellEditor(cellEditor);

        TableColumn colColors = tableSeries.getColumnModel().getColumn(6);
        colColors.setPreferredWidth(60);

        class ColorEditor extends AbstractCellEditor implements TableCellEditor {

            private final JTextField field = new JTextField();
            private Color color;

            ColorEditor() {
                field.setBorder(null);
                field.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            color = JColorChooser.showDialog(DaqbufPanel.this, "Choose a Color - Click 'Cancel for default", color);
                            field.setBackground(color);
                            stopCellEditing();
                        }
                    }
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                color = Preferences.getColorFromString((String) value);
                field.setBackground(color);
                field.setEditable(false);
                return field;
            }

            @Override
            public Object getCellEditorValue() {
                return (color == null) ? "" : color.getRed() + "," + color.getGreen() + "," + color.getBlue();
            }

            @Override
            public boolean isCellEditable(EventObject ev) {
                if (ev instanceof MouseEvent) {
                    return ((MouseEvent) ev).getClickCount() >= 2;
                }
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject ev) {
                return false;
            }
        }
        colColors.setCellEditor(new ColorEditor());

        colColors.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                Color color = Preferences.getColorFromString((String) value);
                ((JLabel) comp).setBackground(color);
                ((JLabel) comp).setEnabled(false);
                return comp;
            }
        });

        update();
    }

    TableModelListener modelSeriesListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (started) {
                try {
                    int index = e.getFirstRow();
                    if (e.getColumn() == 6) {
                        final Color color = Preferences.getColorFromString((String) modelSeries.getValueAt(index, 6));
                        PlotSeries series = (PlotSeries) getPlotSeries(index);
                        if (series instanceof LinePlotErrorSeries){                         
                            ((LinePlotErrorSeries)series).setColor(color);
                            updateSeriesPaint((LinePlotErrorSeries)series);
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        }
    };

    public void initializePlots() {
        numPlots=0;
        pnGraphs.removeAll();
        plots.clear();
    }

    JButton saveButton;

    protected void update() {
        boolean editing = true;
        int rows = modelSeries.getRowCount();
        int cur = tableSeries.getSelectedRow();
        buttonUp.setEnabled((rows > 0) && (cur > 0) && editing);
        buttonDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)) && editing);
        buttonDelete.setEnabled((rows > 0) && (cur >= 0) && editing);
        buttonInsert.setEnabled(editing);
        buttonPlot.setEnabled(modelSeries.getRowCount() > 0);
    }

    boolean isSeriesTableRowEditable(int row, int column) {
        return true;
    }

    boolean isChartTableRowEditable(int row, int column) {
        return (column > 0);
    }


    public void clear() {
        Logger.getLogger(DaqbufPanel.class.getName()).info("Init");
        reset();
        backgroundColor = defaultBackgroundColor;
        gridColor = defaultGridColor;
        modelSeries.setRowCount(0);
modelSeries.addRow(new Object[]{true,"S10BC01-DBPM010:Q1", "sf-databuffer", "[]", PLOT_SHARED,1,null});
modelSeries.addRow(new Object[]{true,"S10BC01-DBPM010:X1", "sf-databuffer", "[]", PLOT_SHARED,2,null});
modelSeries.addRow(new Object[]{true,"SARFE10-PSSS059:FIT-COM", "sf-databuffer", "[]", PLOT_PRIVATE,1,null});
modelSeries.addRow(new Object[]{true,"SARFE10-PSSS059:SPECTRUM_X", "sf-databuffer", "[2560]", PLOT_PRIVATE,1,null});
modelSeries.addRow(new Object[]{false,"SARES11-SPEC125-M1:FPICTURE", "sf-imagebuffer", "[2048, 2048]", PLOT_PRIVATE,1,null});


textFrom.setText("2024-05-02 09:00:00");
textTo.setText("2024-05-02 10:00:00");
        
        file = null;
        initializeTable();
        update();
    }

    String getInitFormat() {
        if (Context.getInstance() != null) {
            if (Context.getInstance().getDataManager().getProvider() instanceof ProviderCSV) {
                return "csv";
            }
            if (Context.getInstance().getDataManager().getProvider() instanceof ProviderText) {
                return "txt";
            }
        }
        return "h5";
    }

    String getInitLayout() {
        return "table";
    }

    File file;
    
    final ArrayList<Device> devices = new ArrayList<>();
    final ArrayList<PlotSeries> plotSeries = new ArrayList<>();

    PlotSeries getPlotSeries(int row) {
        int index = 0;
        for (int i = 0; i < modelSeries.getRowCount(); i++) {
            if (modelSeries.getValueAt(i, 0).equals(Boolean.TRUE)) {
                if (row == i) {
                    return plotSeries.get(index);
                }
                index++;
            }
        }
        return null;
    }
    
    String removeAlias(String str){
        str=str.trim();
        if (str.endsWith(">")) {
            int start = str.lastIndexOf('<');
            if (start>=0){
                str=str.substring(0, start);
            }
        }    
        return str;
    }
    
    String getChannelName(String str){
        str=removeAlias(str);
        if (str.contains(" ")) {
            String[] tokens = str.split(" ");
            str = tokens[0];
        }
        return str.trim();        
    }

    String[] getChannelArgs(String str){
        str=removeAlias(str);
        if (str.contains(" ")) {
            String[] tokens = str.split(" ");
            String[] args = Arr.remove(tokens, 0);
            args=Arr.removeEquals(args, "");
            return args;
        }
        return new String[0];        
    }

    String getChannelAlias(String str){
        str=str.trim();
        if (str.endsWith(">")) {
            int end = str.lastIndexOf('>');            
            int start = str.lastIndexOf('<');
            if (start>=0){
                String alias = str.substring(start + 1, end);
                if (!alias.isBlank()){
                    return alias;
                }
            }
        }
        return getChannelName(str);               
    }
    
    void addPlot(PlotBase plot){
        if (backgroundColor != null) {
            plot.setPlotBackgroundColor(backgroundColor);
        }
        if (gridColor != null) {
            plot.setPlotGridColor(gridColor);
        }
        if (tickLabelFont != null) {
            plot.setLabelFont(tickLabelFont);
            plot.setTickLabelFont(tickLabelFont);
        }                               
        plot.setTitle(null);    
        plot.setQuality(PlotPanel.getQuality());
        plots.add(plot);
        pnGraphs.add(plot);
        numPlots++;        
    }
    
    LinePlotJFree addLinePlot(boolean binned){
        Double y1min = null;
        Double y1max = null;
        Double y2min = null;
        Double y2max = null;

        LinePlotJFree plot = new LinePlotJFree();
        if (binned){
            plot.setStyle(LinePlot.Style.ErrorY);
        }
        DateAxis axis = new DateAxis(null); //("Time");
        axis.setLabelFont(plot.getLabelFont());
        axis.setLabelPaint(plot.getAxisTextColor());
        axis.setTickLabelPaint(plot.getAxisTextColor());
        plot.getChart().getXYPlot().setDomainAxis(axis);            
        plot.getAxis(AxisId.Y).setLabel(null);
        //plot.setTimeAxisLabel(null);
        plot.setLegendVisible(true);
        //plot.setMarkersVisible(Boolean.TRUE.equals(markers));
        //plot.setDurationMillis((duration == null) ? 60000 : (int) (duration * 1000));
        if ((y1min != null) && (y1max != null)) {
            plot.getAxis(AxisId.Y).setRange(y1min, y1max);
        }
        if ((y2min != null) && (y2max != null)) {
            plot.getAxis(AxisId.Y2).setRange(y2min, y2max);
        }
        //if (numPlots > 1) {
        //    plot.setAxisSize(50);
        //}

        // Add an axis change listener to dynamically update the cap length
        axis.addChangeListener(new AxisChangeListener() {
            @Override
            public void axisChanged(AxisChangeEvent event) {
                updateCapLength(plot);
            }
        });           
        plot.getChartPanel().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateCapLength(plot);
            }
        });

        plot.addPopupMenuItem(null);
        
        JCheckBoxMenuItem menuMarkers = new JCheckBoxMenuItem("Show Markers");
        menuMarkers.setSelected(true);
        menuMarkers.addActionListener((ActionEvent e) -> {
            boolean show = menuMarkers.isSelected();
            for (LinePlotSeries s : plot.getAllSeries()){
                s.setPointsVisible(show);
            }            
        });
        plot.addPopupMenuItem(menuMarkers);
        
        
        JCheckBoxMenuItem menuRanges = new JCheckBoxMenuItem("Show Ranges");
        menuRanges.setSelected(binned);
        menuRanges.addActionListener((ActionEvent e) -> {
            boolean show = menuRanges.isSelected();
            for (XYItemRenderer r : plot.getChart().getXYPlot().getRenderers().values()){                        
               ((XYErrorRenderer)r).setDrawYError(show);
            }            
        });
        plot.addPopupMenuItem(menuRanges);        
        menuRanges.setEnabled(binned);

        JMenuItem menuBackgroundColor = new JMenuItem("Set Background Color...");
        menuBackgroundColor.addActionListener((ActionEvent e) -> {
            Color color = JColorChooser.showDialog(this, "Choose a Color", plot.getPlotBackgroundColor());            
            if (color != null) {
                plot.setPlotBackgroundColor(color);
            } else {
                plot.setPlotBackgroundColor(PlotBase.getPlotBackground());
            }           
        });
        plot.addPopupMenuItem(menuBackgroundColor);        

        JMenuItem menuGridColor = new JMenuItem("Set Grid Color...");
        menuGridColor.addActionListener((ActionEvent e) -> {                   
            Color color = JColorChooser.showDialog(this, "Choose a Color", plot.getPlotGridColor());
            if (color != null) {
                plot.setPlotGridColor(color);
            } else {
                plot.setPlotGridColor(PlotBase.getGridColor());
            }
        });
        plot.addPopupMenuItem(menuGridColor);        
        
        JMenuItem menuRangeY1 = new JMenuItem("Set Y1 Range...");
        menuRangeY1.addActionListener((ActionEvent e) -> {                   
            Range init = plot.getAxis(AxisId.Y).isAutoRange() ? null : new Range(plot.getAxis(AxisId.Y).getMin(), plot.getAxis(AxisId.Y).getMax());
            Range r = getRange(init, "Enter Y1 Range");            
            if (r!=null){
                plot.getAxis(AxisId.Y).setRange(r.min, r.max);                       
            } else {
                plot.getAxis(AxisId.Y).setAutoRange();
            }
        });
        plot.addPopupMenuItem(menuRangeY1);        
        
        JMenuItem menuRangeY2 = new JMenuItem("Set Y2 Range...");
        menuRangeY2.addActionListener((ActionEvent e) -> {                   
            Range init = plot.getAxis(AxisId.Y2).isAutoRange() ? null : new Range(plot.getAxis(AxisId.Y2).getMin(), plot.getAxis(AxisId.Y2).getMax());
            Range r = getRange(init, "Enter Y2 Range");            
            if (r!=null){
                plot.getAxis(AxisId.Y2).setRange(r.min, r.max);                       
            } else {
                plot.getAxis(AxisId.Y2).setAutoRange();
            }
        });
        plot.addPopupMenuItem(menuRangeY2);        

        addPlot(plot);
        return plot;
    }
           
    
    Range getRange(Range init, String title){
        Range ret = null;
        try {
            JPanel panel = new JPanel();            
            GridBagLayout layout = new GridBagLayout();
            layout.columnWidths = new int[]{0, 180};   //Minimum width
            panel.setLayout(layout);
            JTextField min = new JTextField((init==null) ? "" : String.valueOf(init.min));
            JTextField max = new JTextField((init==null) ? "" : String.valueOf(init.max));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            panel.add(new JLabel("Minimum:"), c);
            c.gridy = 1;
            panel.add(new JLabel("Maximum:"), c);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            panel.add(max, c);
            c.gridy = 0;
            panel.add(min, c);

            if (showOption((title==null) ? "Enter Range" : title, panel, OptionType.OkCancel) == OptionResult.Yes) {
                return new Range(Double.valueOf(min.getText().trim()), Double.valueOf(max.getText().trim()));
            }
        } catch (Exception ex) {
            ret = null;
        }
        return ret;
    }
    
    
    LinePlotErrorSeries addBinnedSeries(LinePlotJFree plot, String name, String backend, String start, String end, int bins, int axis, Color color){        
        LinePlotErrorSeries series = new LinePlotErrorSeries(name, color, axis);
        plotSeries.add(series);
        plot.addSeries(series);

        XYPlot xyplot = plot.getChart().getXYPlot();
        XYErrorRenderer renderer = (XYErrorRenderer) plot.getSeriesRenderer(series);            
        renderer.setDrawYError(true);
        renderer.setErrorStroke(new BasicStroke());

        try {
            daqbuf.startFetchQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, bins).handle((ret,ex)->{
                if (ex!=null){
                    showException((Exception)ex);
                } else {
                    Map<String, List>  map = (Map<String, List>) ret;                                
                    List<Number> average   = map.get(Daqbuf.FIELD_AVERAGE);
                    List<Number> min   = map.get(Daqbuf.FIELD_MIN);
                    List<Number> max   = map.get(Daqbuf.FIELD_MAX);
                    List<Long> t1   = map.get(Daqbuf.FIELD_START);
                    List<Long> t2   = map.get(Daqbuf.FIELD_END);
                    //SwingUtilities.invokeLater(()->{
                    long now = System.currentTimeMillis();                                    

                    try{
                        plot.setUpdatesEnabled(false);
                        updateSeriesPaint(series);
                        updateCapLength(plot);                                    
                        for (int j=0; j< average.size(); j++){
                            double timestamp = (t1.get(j) + t2.get(j))/2.0/1e6;
                            series.appendData( timestamp,  average.get(j).floatValue(), min.get(j).floatValue(), max.get(j).floatValue());      
                        }
                    } finally{
                        plot.update(true);
                        plot.setUpdatesEnabled(true);
                    }

                    //});
                }
                return ret;
            });
        } catch (Exception ex) {
            showException(ex);
        }                
        return series;
    }

    LinePlotSeries addLineSeries(LinePlotJFree plot, String name, String backend,String start, String end, int axis, Color color){        
        LinePlotSeries series = new LinePlotSeries(name, color, axis);
        //LinePlotErrorSeries series = new LinePlotErrorSeries(name, color, axis);
        plotSeries.add(series);
        plot.addSeries(series);
        series.setMaxItemCount((Integer)spinnerSize.getValue());
                
        //((XYErrorRenderer)plot.getSeriesRenderer(series)).setDrawYError(false);
               
        try {
            daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, new QueryListener(){                
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {
                    try{
                        plot.setUpdatesEnabled(false);
                        List<Number> aux = (List<Number>)values;                                                
                        if (series.getCount() >= series.getMaxItemCount()){
                            throw new RuntimeException ("Series too big for plotting: " + name);
                        }
                        for (int j=0; j< values.size(); j++){
                            series.appendData( timestamps.get(j).doubleValue()/1e6,  aux.get(j).doubleValue());      
                        }
                    //} catch (Exception ex){
                    //    showException(ex);
                    } finally{
                        plot.update(true);
                        plot.setUpdatesEnabled(true);
                    }                    
               }
            }).handle((ret,ex)->{
                if (ex!=null){
                    showException((Exception)ex);
                };
                return ret;
            });                                        
        } catch (Exception ex) {
            showException(ex);
        }                
        return series;
    }
    
    //MatrixPlotRenderer
    MatrixPlotJFree addMatrixPlot(boolean binned){
        MatrixPlotJFree plot = new MatrixPlotJFree();
        if (binned){
            DateAxis axis = new DateAxis(null); //("Time");
            axis.setLabelFont(PlotBase.getDefaultLabelFont());
            axis.setLabelPaint(plot.getAxisTextColor());
            axis.setTickLabelPaint(plot.getAxisTextColor());
            plot.getChart().getXYPlot().setDomainAxis(axis);            
        }
        
        // Add a chart mouse listener to intercept double-click events
        plot.getChartPanel().addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {                
                if (event.getTrigger().getClickCount() == 2) { // Check if it's a double-click
                    XYPlot xyplot = plot.getChart().getXYPlot();
                    ChartPanel chartPanel = plot.getChartPanel();
                    Point p = SwingUtilities.convertPoint(event.getTrigger().getComponent(), event.getTrigger().getPoint(), chartPanel);                   
                    Rectangle2D dataArea = plot.getChartPanel().getScreenDataArea();
                    double chartX = xyplot.getDomainAxis().java2DToValue(p.getX(), dataArea, xyplot.getDomainAxisEdge());
                    double chartY = xyplot.getRangeAxis().java2DToValue(p.getY(), dataArea, xyplot.getRangeAxisEdge());
                    
                    if ((chartX < xyplot.getDomainAxis().getRange().getLowerBound()) ||
                        (chartX > xyplot.getDomainAxis().getRange().getUpperBound()) ||
                        (chartY < xyplot.getRangeAxis().getRange().getLowerBound()) ||
                        (chartY > xyplot.getRangeAxis().getRange().getUpperBound())  ){
                        return;
                    }
                                        
                    // Calculate the bin indices
                    // Find the closest X value
                    DefaultXYZDataset xyDataset = (DefaultXYZDataset) xyplot.getDataset();
                    int seriesIndex = 0;
                    int itemCount = xyDataset.getItemCount(seriesIndex);
                    int closestIndex = -1;
                    double minDistance = Double.MAX_VALUE;
                    for (int i = 0; i < itemCount; i++) {
                        double x = xyDataset.getXValue(seriesIndex, i);
                        double distance = Math.abs(x - chartX);
                        if (distance < minDistance) {
                            minDistance = distance;
                            closestIndex = i;
                        }
                    }
                    MatrixPlotSeries series = plot.getSeries(seriesIndex);
                    //double time = series.getMinX() + closestIndex * series.getBinWidthX();
                    onMatrixSeriesClicked(plot, series, chartX, closestIndex);                    
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }
        });        
        addPlot(plot);
        return plot;
    }


 
                        
    void onMatrixSeriesClicked(MatrixPlotJFree plot, MatrixPlotSeries series, double timestamp, int index){
        try {
            boolean binned = (series instanceof MatrixPlotBinnedSeries);            
            double time  = series.getX()[index][index];
            String timestr = getTimeString(time, false);
            String name = series.getName() + " " + timestr  + " ["+ index +"]";
            LinePlotJFree lplot = new LinePlotJFree();
            LinePlotSeries lseries;
            if (binned){
                lplot.setStyle(LinePlot.Style.ErrorY);
                lseries = new LinePlotErrorSeries(name, Color.RED);                
            }else {
                lseries = new  LinePlotSeries(name, Color.RED);
            }
            lplot.addSeries(lseries);
            lplot.setTitle("");
            lplot.setLegendVisible(true);            

            try{
                plot.setUpdatesEnabled(false);
                if (binned){
                    updateSeriesPaint(lseries);
                    double[] min = ((MatrixPlotBinnedSeries)series).min[index];
                    double[] max = ((MatrixPlotBinnedSeries)series).max[index];
                    double[] average = ((MatrixPlotBinnedSeries)series).average[index];                    
                    for (int i=0; i< average.length; i++){
                        ((LinePlotErrorSeries)lseries).appendData( i,  average[i], min[i], max[i]);      
                    }                
                    updateSeriesPaint(lseries);
                } else {
                    double[] row = Convert.transpose(series.getData())[index];
                    lseries.setData(row);
                }                                
            } finally{
                plot.update(true);
                plot.setUpdatesEnabled(true);
            }
            showDialog(series.getName(), new Dimension(800, 600), lplot);            
        } catch (Exception ex) {
            showException((Exception)ex);
        }                
                    
    }
    
    MatrixPlotSeries addMatrixSeries(MatrixPlotJFree plot, String name, String backend,String start, String end){          
        List value = new ArrayList();
        List<Long> id = new ArrayList<>();
        List<Long> timestamp = new ArrayList<>();
        long maxSize = (Integer)spinnerSize.getValue();
        MatrixPlotSeries series = new MatrixPlotSeries(name);
        plot.addSeries(series);        
        plotSeries.add(series);          
        
        try {
            daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, new QueryListener(){                
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {                                        
                    if ((value.size()>0) && (((List)value.get(0)).size() * value.size()) > maxSize){
                        throw new RuntimeException ("Series too big for plotting: " + name);
                    }                                        
                    value.addAll(values);
                    id.addAll(ids);
                    timestamp.addAll(timestamps);
               }
            }).handle((ret,ex)->{
                if (ex!=null){
                    showException((Exception)ex);
                };                                
                if (value.size()>0) {        
                    double[][] data = (double[][])Convert.toPrimitiveArray(value, Double.class);        
                    series.setData(Convert.transpose(data));
                }
                plot.getChartPanel().restoreAutoDomainBounds(); //Needed because changed domain axis to time                
                return ret;
            });   ;            
        } catch (Exception ex) {
            showException((Exception)ex);
        }                
        return series;
    }
    
    static class MatrixPlotBinnedSeries extends MatrixPlotSeries{
        MatrixPlotBinnedSeries(String name){
            super(name);
        }
        double[][] average;
        double[][] min;
        double[][] max;
    }
        
    
    void setMatrixBinnedSeries(MatrixPlotJFree plot, MatrixPlotBinnedSeries series, List<List> average, List<List> min, List<List> max, List<Long> t1, List<Long> t2){
        int bins = average.size();
        if (bins>0) {                                
            double[] timestamps = new double[bins];
            for (int j=0; j< bins; j++){
                timestamps[j]= (t1.get(j) + t2.get(j))/2.0/1e6;
            }
            double maxTime = timestamps[timestamps.length-1];
            double minTime = timestamps[0];            
            plot.getAxis(AxisId.X).setRange(minTime, maxTime);        
            series.average = (double[][])Convert.toPrimitiveArray(average, Double.class);
            series.min = (double[][])Convert.toPrimitiveArray(min, Double.class);
            series.max = (double[][])Convert.toPrimitiveArray(max, Double.class);            
            series.setNumberOfBinsX(series.average.length);
            series.setNumberOfBinsY(series.average[0].length);
            series.setRangeX(minTime, maxTime);            
            series.setRangeY(0, series.average[0].length-1);                    
            series.setData(Convert.transpose(series.average));                
        }
        plot.getChartPanel().restoreAutoDomainBounds(); //Needed because changed domain axis to time                
    }
    
    
    MatrixPlotBinnedSeries addMatrixSeriesBinned(MatrixPlotJFree plot, String name, String backend,String start, String end, int bins){  
        long maxSize = (Integer)spinnerSize.getValue();
        MatrixPlotBinnedSeries series = new MatrixPlotBinnedSeries(name);
        plot.addSeries(series);        
        plotSeries.add(series);        
        
        try {
            /*
            daqbuf.startFetchQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, bins).handle((ret,ex)->{
                if (ex!=null){
                    showException((Exception)ex);
                } else {
                    Map<String, List>  map = (Map<String, List>) ret;                                
                    List average   = map.get(Daqbuf.FIELD_AVERAGE);
                    List min   = map.get(Daqbuf.FIELD_MIN);
                    List max   = map.get(Daqbuf.FIELD_MAX);
                    List<Long> t1   = map.get(Daqbuf.FIELD_START);
                    List<Long> t2   = map.get(Daqbuf.FIELD_END);
                    long now = System.currentTimeMillis();                                    

                    try{
                        plot.setUpdatesEnabled(false);
                    } finally{
                        plot.update(true);
                        plot.setUpdatesEnabled(true);
                    }
                }
                return ret;
            });
            */            
           
            List value = new ArrayList();
            List<Long> id = new ArrayList<>();
            List<Long> timestamp = new ArrayList<>();            
            daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, new QueryListener(){                
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {                                        
                    if ((value.size()>0) && (((List)value.get(0)).size() * value.size()) > maxSize){
                        throw new RuntimeException ("Series too big for plotting: " + name);
                    }                                        
                    value.addAll(values);
                    id.addAll(ids);
                    timestamp.addAll(timestamps);
               }
            }/*,bins*/).handle((ret,ex)->{
                if (ex!=null){
                    //showException((Exception)ex);
                };            
                List<List> average = value;
                List<List> min = new ArrayList<>();
                List<List> max = new ArrayList<>();
                List<Long> t1 = new ArrayList<>();
                List<Long> t2 = new ArrayList<>();
                for (Long t: timestamp){
                    t1.add(t);
                    t2.add(t + 1000000);
                }
                for (List<Number> l: average){
                    List lmin = new ArrayList();
                    List lmax = new ArrayList();
                    for (Number n : l){
                        lmin.add(n.doubleValue()-1.0);
                        lmax.add(n.doubleValue()+1.0);
                    }
                    min.add(lmin);
                    max.add(lmax);
                }                
                setMatrixBinnedSeries(plot, series, average, min,  max, t1, t2);
                
                return ret;
            });    
        } catch (Exception ex) {
            showException((Exception)ex);
        }                
                        
        
        return series;
    }

    
    SlicePlotDefault addSlicePlot(){
        SlicePlotDefault plot = new SlicePlotDefault(new MatrixPlotRenderer()) {
            @Override
            protected String getPageSubtitle(SlicePlotSeries series, int page){            
                return " - " + page;
            }
        };     

        addPlot(plot);
        return plot;
    }
    
    //TODO
    int[] getShape(String name, String backend, String start, String end){
        return  new int[]{200,100,10};
    }

    //TODO
    double[][] getFrame(String name, String backend, String start, int index, int[]shape){
        try {
            Thread.sleep(500);
            double[][]data = new  double[shape[1]][shape[0]];
            for (int j=0;j<data.length; j++){
                for (int k=0;k<data[0].length; k++){
                    data[j][k] = index*10000 + j*100 + k;
                }
            }
            return data;
        } catch (Exception ex) {
            return null;
        }
    }

    
    SlicePlotSeries addImageSeries(SlicePlotDefault plot, String name, String backend, String start, String end){        
        SlicePlotSeries series = new SlicePlotSeries(name);        
        plotSeries.add(series);
        plot.addSeries(series);
        plot.setTitle(name);
        plot.setUpdatesEnabled(true);
        
        
        final int[] shape = getShape(name, backend, start, end);

        series.setNumberOfBinsX(shape[0]);
        series.setNumberOfBinsY(shape[1]);
        series.setRangeX(0, shape[0]-1);
        series.setRangeY(0, shape[1]-1);
        series.setNumberOfBinsZ(shape[2]);
        series.setRangeZ(0, shape[2]-1);                
        
        series.setListener((SlicePlotSeries s, int index) -> {
            try{
                double[][] page_data = getFrame(name, backend, start, index, shape);
                s.setData(page_data);                                
            } catch (Exception ex){
                 s.clear();
            }
        });

        
        try {
            daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, new QueryRecordListener() {
                 public void onRecord(Query query, Object value, Long id, Long timestamp){
                     System.out.println(timestamp + " - " + value);
                 }
            });
        } catch (Exception ex) {
            showException(ex);
        }                
        return series;
    }

    public void plotQuery() throws Exception {
        reset();
        
        if (modelSeries.getRowCount() == 0) {
            return;
        }
        numPlots = 0;

        final String start = textFrom.getText();
        final String end = textTo.getText();        
                
        started = true;
        update();

        Plot currentPlot = null;
        Vector vector = modelSeries.getDataVector();
        Vector[] rows = (Vector[]) vector.toArray(new Vector[0]);
        for (int i = 0; i < rows.length; i++) {
            Vector info = rows[i];
            if (info.get(0).equals(true)) {
                final Integer bins = checkBins.isSelected() ? (Integer)spinnerBins.getValue() : null;
                final String name = getChannelAlias(((String) info.get(1)).trim());
                final String backend = info.get(2).toString();
                final boolean  shared = info.get(4).equals(PLOT_SHARED);
                final int axis = (Integer) info.get(5);                
                final Color color = Preferences.getColorFromString((String) info.get(6));
                List<Integer> shape = new ArrayList<>();
                try{
                    shape = (List<Integer>) EncoderJson.decode(info.get(3).toString(), List.class);
                } catch (Exception ex){                    
                }
                                
                Plot plot = null;
                PlotSeries series= null;
                switch (shape.size()){
                    case 0:
                        if (shared && (currentPlot!=null) && (currentPlot instanceof LinePlotJFree)){
                            plot = currentPlot;
                        } else {
                            plot = addLinePlot(bins!=null);
                        }
                        if (bins==null){
                            series = addLineSeries((LinePlotJFree)plot, name, backend, start, end, axis, color);                            
                        } else {
                            series = addBinnedSeries((LinePlotJFree)plot, name, backend, start, end, bins, axis, color);
                        }
                        break;
                    case 1:                        
                        if (bins==null){
                            plot = addMatrixPlot(false);
                            series = addMatrixSeries((MatrixPlotJFree)plot, name, backend, start, end);                            
                        } else {
                            plot = addMatrixPlot(true);
                            series = addMatrixSeriesBinned((MatrixPlotJFree)plot, name, backend, start, end, bins);
                        }
                        break;
                    case 2:
                        plot = addSlicePlot();
                        series = addImageSeries((SlicePlotDefault)plot, name, backend, start, end);
                }                
                
                
                currentPlot = plot;                                                
                
            }
        }

    }
    
    void updateCapLength(LinePlotJFree plot){
        try{
            DateAxis axis = (DateAxis) plot.getChart().getXYPlot().getDomainAxis();        
            for (int i=0; i<plot.getChart().getXYPlot().getRendererCount(); i++){
                XYErrorRenderer renderer = (XYErrorRenderer) plot.getChart().getXYPlot().getRenderer(i);      
                try{
                    int bins = plot.getChart().getXYPlot().getDataset().getItemCount(0);
                    double start = plot.getChart().getXYPlot().getDataset().getXValue(0, 0);
                    double end = plot.getChart().getXYPlot().getDataset().getXValue(0, bins-1);
                    double capLenghtMs = (end - start) / bins;
                    double domainLenghtPixels = plot.getChartPanel().getScreenDataArea().getWidth();                
                    domainLenghtPixels /= plot.getChartPanel().getScaleX();
                    double domainLengthMs = axis.getRange().getLength();
                    double capLength = (capLenghtMs * domainLenghtPixels ) / domainLengthMs    ;    
                    renderer.setCapLength(capLength);      
                } catch (Exception ex) {
                    renderer.setCapLength(4.0);     
                }
            }
        } catch (Exception ex) {
        }
            
    }
    
    
    
    void updateSeriesPaint(LinePlotSeries series){
        LinePlotJFree plot = (LinePlotJFree) series.getPlot();
        XYErrorRenderer renderer = (XYErrorRenderer) plot.getSeriesRenderer(series);            
        Paint paint = renderer.getSeriesPaint(plot.getSeriesIndex(series));                          
        YIntervalSeriesCollection dataset= (YIntervalSeriesCollection) plot.getDataset(series.getAxisY());
        YIntervalSeries yseries = (YIntervalSeries) series.getToken();
        if ((paint instanceof Color) && (dataset.getSeriesCount() ==1)){
            Color c = (Color)paint;
            paint = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0x40); 
        } else {
            paint = GRAYED_COLOR; 
        }
       
        renderer.setErrorPaint(paint);        
    }

    String[] getNames() throws IOException {
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < modelSeries.getRowCount(); i++) {
            String id = getId(i);
            if (id != null) {
                names.add(id);
            }
        }
        return names.toArray(new String[0]);
    }

    String getBaseId(Vector[] rows, int index) throws IOException {
        if ((index < 0) || (index >= rows.length)) {
            throw new IOException("Invalid name index");
        }
        Vector info = rows[index];
        if (info.get(0).equals(false)) {
            return null;
        }
        String id = ((String) info.get(1)).trim();
        String backend = ((String) info.get(2)).trim();
        
        id = id.replace("/", "_");
        return id;
    }

    String getId(int index) throws IOException {
        Vector[] rows = (Vector[]) modelSeries.getDataVector().toArray(new Vector[0]);
        String id = getBaseId(rows, index);
        if (id != null) {
            int existing = 0;
            for (int i = 0; i < index; i++) {
                String otherId = getBaseId(rows, i);
                if (otherId != null) {
                    if (otherId.equals(id)) {
                        existing++;
                    }
                }
            }
            if (existing > 0) {
                id = id + "-" + (existing + 1);
            }
        }
        return id;
    }


    final Object lock = new Object();
    
    void reset() {
        update();
        synchronized (lock) {
            lock.notifyAll();
        }
        plotSeries.clear();
        initializePlots();
        appendTimestamps.clear();
    }

    @Override
    protected void onClosed() {
    }


    public static void create(String url, boolean modal, String title) {
        java.awt.EventQueue.invokeLater(() -> {
            DaqbufPanel dialog = new DaqbufPanel(null, url, title, modal);
            dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    dialog.onClosed();
                    if (modal) {
                        System.exit(0);
                    } 
                }
            });
            SwingUtils.centerComponent(null, dialog);
            if (dialog.getOwner()!=null){
                dialog.getOwner().setIconImage(Toolkit.getDefaultToolkit().getImage(App.getResourceUrl("IconSmall.png")));
            }
            dialog.setVisible(true);
            dialog.requestFocus();
        });
    }
    
    void openFile(String fileName) {        
        try {
            DataPanel panel = new DataPanel();            
            panel.load(fileName);            
            panel.setDefaultDataPanelListener();            
            panel.showFileProps();
            showDialog(fileName, new Dimension(800, 600), panel);    
        } catch (Exception ex) {
            showException(ex);
        }        
    }
    
    void saveQuery(String filename) throws IOException, InterruptedException {
        List<String> channels = new ArrayList<>();
        Vector vector = modelSeries.getDataVector();
        Vector[] rows = (Vector[]) vector.toArray(new Vector[0]);
        for (int i = 0; i < rows.length; i++) {
            Vector info = rows[i];
            if (info.get(0).equals(true)) {
                String name = getChannelAlias(((String) info.get(1)).trim());
                String backend = info.get(2).toString();
                channels.add(name + Daqbuf.BACKEND_SEPARATOR + backend);
            }
        }
        if (channels.isEmpty()){
            throw new IOException("No channel selected");
        }
        final Integer bins = checkBins.isSelected() ? (Integer)spinnerBins.getValue() : null;
        final String start = textFrom.getText();
        final String end = textTo.getText();        

        buttonSaveData.setEnabled(false);
        JDialog splash = SwingUtils.showSplash(this, "Save", new Dimension(400,200), "Saving data to " + filename);
        daqbuf.startSaveQuery(filename, channels.toArray(new String[0]), start, end, bins).handle((Object ret, Object ex)->{
            splash.setVisible(false);
            if (ex!=null){
                showException((Exception)ex);
            } else {
                if (SwingUtils.showOption(this, "Save", "Success saving data to " + filename + ".\nDo you want to open the file?" , OptionType.YesNo) == OptionResult.Yes) {
                    openFile(filename);
                }
            }
            buttonSaveData.setEnabled(true);
            return ret;
        });
    }
    
    void saveQuery() throws IOException {
        try{
            String path = (Context.getInstance() != null) ? Context.getInstance().getConfig().dataPath : Sys.getUserHome();
            JFileChooser chooser = new JFileChooser(path);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("HDF5 files", "h5");
            chooser.setFileFilter(filter);
            int rVal = chooser.showSaveDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                String fileName = chooser.getSelectedFile().getAbsolutePath();
                if (IO.getExtension(fileName).isEmpty()){
                    fileName += ".h5";
                }
                saveQuery(fileName);                
            }
        } catch (Exception ex) {
            showException(ex);
        }        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabPane = new javax.swing.JTabbedPane();
        panelSeries = new javax.swing.JPanel();
        panelSerie = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSeries = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return isSeriesTableRowEditable(row, column);
            };
        };
        buttonDelete = new javax.swing.JButton();
        buttonUp = new javax.swing.JButton();
        buttonInsert = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        panelFile = new javax.swing.JPanel();
        buttonPlot = new javax.swing.JButton();
        buttonSaveData = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        textFrom = new javax.swing.JTextField();
        textTo = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        comboTime = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        checkUTC = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        checkBins = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        spinnerBins = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        spinnerSize = new javax.swing.JSpinner();
        panelPlots = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        pnGraphs = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        tabPane.setName("tabPane"); // NOI18N
        tabPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabPaneStateChanged(evt);
            }
        });

        panelSeries.setName("panelSeries"); // NOI18N

        panelSerie.setBorder(javax.swing.BorderFactory.createTitledBorder("Series"));

        tableSeries.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Name", "Backend", "Shape", "Plot", "Y Axis", "Color"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tableSeries.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableSeries.getTableHeader().setReorderingAllowed(false);
        tableSeries.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableSeriesMouseReleased(evt);
            }
        });
        tableSeries.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tableSeriesKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(tableSeries);

        buttonDelete.setText("Delete");
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        buttonUp.setText("Move Up");
        buttonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpActionPerformed(evt);
            }
        });

        buttonInsert.setText("Insert");
        buttonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });

        buttonDown.setText("Move Down");
        buttonDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDownActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelSerieLayout = new javax.swing.GroupLayout(panelSerie);
        panelSerie.setLayout(panelSerieLayout);
        panelSerieLayout.setHorizontalGroup(
            panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSerieLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 563, Short.MAX_VALUE)
                    .addGroup(panelSerieLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDown)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonInsert)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDelete)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        panelSerieLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonUp});

        panelSerieLayout.setVerticalGroup(
            panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSerieLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                .addGap(4, 4, 4)
                .addGroup(panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonDown)
                    .addComponent(buttonUp))
                .addContainerGap())
        );

        panelSerieLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonDelete, buttonDown, buttonInsert, buttonUp});

        buttonPlot.setText("Plot");
        buttonPlot.setPreferredSize(new java.awt.Dimension(89, 23));
        buttonPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPlotActionPerformed(evt);
            }
        });

        buttonSaveData.setText("Save");
        buttonSaveData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveDataActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelFileLayout = new javax.swing.GroupLayout(panelFile);
        panelFile.setLayout(panelFileLayout);
        panelFileLayout.setHorizontalGroup(
            panelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFileLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonSaveData)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(buttonPlot, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelFileLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonPlot, buttonSaveData});

        panelFileLayout.setVerticalGroup(
            panelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFileLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(panelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonPlot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSaveData))
                .addGap(0, 0, 0))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Range"));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel2.setText("From:");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("To:");

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);

        comboTime.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { " ", "Last 1min", "Last 10min", "Last 1h", "Last 12h", "Last 24h", "Last 7d", "Yesterday", "Today", "Last Week", "This Week", "Last Month", "This Month" }));
        comboTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboTimeActionPerformed(evt);
            }
        });

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("Set:");

        checkUTC.setText("UTC");
        checkUTC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkUTCActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel9))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textTo, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(comboTime, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkUTC))
                .addContainerGap())
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addGap(19, 19, 19)
                    .addComponent(jLabel6)
                    .addContainerGap(544, Short.MAX_VALUE)))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel2, jLabel3});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(textFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(textTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkUTC))
                .addContainerGap())
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                    .addContainerGap(15, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(15, 15, 15)))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Bins"));

        checkBins.setSelected(true);
        checkBins.setText("Binned Data");
        checkBins.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBinsActionPerformed(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Bins:");

        spinnerBins.setModel(new javax.swing.SpinnerNumberModel(500, 1, 10000, 10));

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Max size: ");

        spinnerSize.setModel(new javax.swing.SpinnerNumberModel(10000, 1, 200000, 10000));
        spinnerSize.setEnabled(false);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkBins)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7)
                        .addComponent(spinnerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(checkBins)
                        .addComponent(spinnerBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout panelSeriesLayout = new javax.swing.GroupLayout(panelSeries);
        panelSeries.setLayout(panelSeriesLayout);
        panelSeriesLayout.setHorizontalGroup(
            panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelSerie, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panelFile, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSeriesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelSeriesLayout.setVerticalGroup(
            panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSeriesLayout.createSequentialGroup()
                .addComponent(panelSerie, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panelFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabPane.addTab("Series", panelSeries);

        panelPlots.setName("panelPlots"); // NOI18N
        panelPlots.setLayout(new java.awt.BorderLayout());

        scrollPane.setPreferredSize(new java.awt.Dimension(356, 303));

        pnGraphs.setPreferredSize(new java.awt.Dimension(354, 301));

        javax.swing.GroupLayout pnGraphsLayout = new javax.swing.GroupLayout(pnGraphs);
        pnGraphs.setLayout(pnGraphsLayout);
        pnGraphsLayout.setHorizontalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 583, Short.MAX_VALUE)
        );
        pnGraphsLayout.setVerticalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 423, Short.MAX_VALUE)
        );

        scrollPane.setViewportView(pnGraphs);

        panelPlots.add(scrollPane, java.awt.BorderLayout.CENTER);

        tabPane.addTab("Plots", panelPlots);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabPane, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tabPane)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpActionPerformed
        try {
            int rows = modelSeries.getRowCount();
            int cur = tableSeries.getSelectedRow();
            modelSeries.moveRow(cur, cur, cur - 1);
            tableSeries.setRowSelectionInterval(cur - 1, cur - 1);
            update();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonUpActionPerformed

    private void buttonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownActionPerformed
        try {
            int rows = modelSeries.getRowCount();
            int cur = tableSeries.getSelectedRow();
            modelSeries.moveRow(cur, cur, cur + 1);
            tableSeries.setRowSelectionInterval(cur + 1, cur + 1);
            update();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDownActionPerformed

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        Object[] data = new Object[]{Boolean.TRUE, "", Daqbuf.getDefaultBackend(), "", PLOT_PRIVATE, 1};
        if (tableSeries.getSelectedRow() >= 0) {
            modelSeries.insertRow(tableSeries.getSelectedRow() + 1, data);
        } else {
            modelSeries.addRow(data);
        }
        modelSeries.fireTableDataChanged();
        update();
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        if (modelSeries.getRowCount() > 0) {
            modelSeries.removeRow(Math.max(tableSeries.getSelectedRow(), 0));
            update();
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void tabPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabPaneStateChanged
        try {
        } catch (Exception ex) {
            showException(ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_tabPaneStateChanged

    private void tableSeriesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSeriesMouseReleased
        update();
    }//GEN-LAST:event_tableSeriesMouseReleased

    private void tableSeriesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableSeriesKeyReleased
        update();
    }//GEN-LAST:event_tableSeriesKeyReleased

    private void buttonPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPlotActionPerformed
        if (tableSeries.isEditing()) {
            tableSeries.getCellEditor().stopCellEditing();
        }        
        try {
            if (modelSeries.getRowCount() > 0) {
                plotQuery();
                tabPane.setSelectedComponent(panelPlots);
            }
        } catch (Exception ex) {
            showException(ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_buttonPlotActionPerformed

    private void buttonSaveDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveDataActionPerformed
        try {
            saveQuery();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSaveDataActionPerformed

    private void checkBinsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBinsActionPerformed
        spinnerBins.setEnabled(checkBins.isSelected());
        spinnerSize.setEnabled(!checkBins.isSelected());
    }//GEN-LAST:event_checkBinsActionPerformed

    String getTimeString(Number timestamp, boolean utc){
        DateTimeFormatter formatter = (checkUTC.isSelected()) ?
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") :
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");    
        
        Instant instant = Instant.ofEpochMilli(timestamp.longValue());
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, utc ? ZoneOffset.UTC : ZoneOffset.systemDefault());
        return localDateTime.format(formatter);
    }
    private void comboTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboTimeActionPerformed
        DateTimeFormatter formatter = (checkUTC.isSelected()) ?
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") :
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");    
        LocalDateTime now = (checkUTC.isSelected()) ? 
                LocalDateTime.now(ZoneOffset.UTC) :
                LocalDateTime.now();
        LocalDateTime to = now;
        LocalDateTime from = null;
        
        switch (comboTime.getSelectedIndex()){
            case 1:
                from = now.minusMinutes(1);
                break;
            case 2:
                from = now.minusMinutes(10);
                break;
            case 3:
                from = now.minusHours(1);
                break;
            case 4:
                from = now.minusHours(12);
                break;
            case 5:
                from = now.minusHours(24);
                break;
            case 6:
                from = now.minusDays(7);
                break;
            case 7:                
                LocalDate yesterdayDate = now.toLocalDate().minusDays(1);
                from = LocalDateTime.of(yesterdayDate, LocalTime.MIN);           
                to = LocalDateTime.of(yesterdayDate, LocalTime.MAX);
                break;
            case 8:
                from = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);           
                break;
            case 9:
                LocalDate startOfCurrentWeek = now.toLocalDate().with(DayOfWeek.MONDAY);
                LocalDate endOfLastWeek = startOfCurrentWeek.minusDays(1);
                LocalDate startOfLastWeek = endOfLastWeek.minusDays(6);
                from = LocalDateTime.of(startOfLastWeek, LocalTime.MIN);
                to = LocalDateTime.of(endOfLastWeek, LocalTime.MAX);
                break;
            case 10:
                from = LocalDateTime.of(now.toLocalDate().with(DayOfWeek.MONDAY), LocalTime.MIN);
                break;
            case 11:
                YearMonth previousMonth = YearMonth.from(now.minusMonths(1));
                LocalDate firstDayOfPreviousMonth = previousMonth.atDay(1);
                LocalDate lastDayOfPreviousMonth = previousMonth.atEndOfMonth();
                from = LocalDateTime.of(firstDayOfPreviousMonth, LocalTime.MIN);
                to = LocalDateTime.of(lastDayOfPreviousMonth, LocalTime.MAX);
                break;
            case 12:
                from = LocalDateTime.of(YearMonth.from(now).atDay(1), LocalTime.MIN);
                break;
        }

        textFrom.setText(from.format(formatter));
        textTo.setText(to.format(formatter));        
    }//GEN-LAST:event_comboTimeActionPerformed

    private void checkUTCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkUTCActionPerformed
        comboTimeActionPerformed(null);
    }//GEN-LAST:event_checkUTCActionPerformed

    /**
     */
    public static void main(String args[]) {
        App.init(args);
        create(null, true, null);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonPlot;
    private javax.swing.JButton buttonSaveData;
    private javax.swing.JButton buttonUp;
    private javax.swing.JCheckBox checkBins;
    private javax.swing.JCheckBox checkUTC;
    private javax.swing.JComboBox<String> comboTime;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel panelFile;
    private javax.swing.JPanel panelPlots;
    private javax.swing.JPanel panelSerie;
    private javax.swing.JPanel panelSeries;
    private javax.swing.JPanel pnGraphs;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JSpinner spinnerBins;
    private javax.swing.JSpinner spinnerSize;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JTable tableSeries;
    private javax.swing.JTextField textFrom;
    private javax.swing.JTextField textTo;
    // End of variables declaration//GEN-END:variables
}
