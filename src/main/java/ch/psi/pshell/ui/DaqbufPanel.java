package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.ProviderCSV;
import ch.psi.pshell.data.ProviderText;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.LinePlotErrorSeries;
import ch.psi.pshell.plot.LinePlotJFree;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.MatrixPlotRenderer;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.Plot.AxisId;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.plot.SlicePlotDefault;
import ch.psi.pshell.plot.SlicePlotSeries;
import ch.psi.pshell.plot.SlicePlotSeries.SlicePlotSeriesListener;
import ch.psi.pshell.plot.TimePlotBase;
import ch.psi.pshell.plot.TimePlotJFree;
import ch.psi.pshell.plot.TimePlotSeries;
import ch.psi.pshell.plotter.Preferences;
import ch.psi.pshell.swing.ChannelSelector;
import ch.psi.pshell.swing.HistoryChart;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.swing.ScriptEditor;
import static ch.psi.pshell.ui.Preferences.PanelLocation.Plot;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.Daqbuf;
import ch.psi.utils.Daqbuf.Query;
import ch.psi.utils.Daqbuf.QueryListener;
import ch.psi.utils.Daqbuf.QueryRecordListener;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
import ch.psi.utils.swing.StandardDialog;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellEditor;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeEventType;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
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
    final DefaultTableModel modelCharts;

    final ArrayList<PlotBase> plots = new ArrayList<>();
    final HashMap<Device, Long> appendTimestamps = new HashMap<>();
    volatile boolean started = false;

    Color backgroundColor;
    Color gridColor;
    final Daqbuf daqbuf;
    int numPlots = 0;
    
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
                panelColorBackground.setBackground(defaultBackgroundColor);
                backgroundColor = defaultBackgroundColor;
            } catch (Exception ex) {
                Logger.getLogger(DaqbufPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (App.hasArgument("grid_color")) {
            try {
                defaultGridColor = Preferences.getColorFromString(App.getArgumentValue("grid_color"));
                panelColorGrid.setBackground(defaultGridColor);
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
        modelCharts = (DefaultTableModel) tableCharts.getModel();
        modelCharts.addTableModelListener(modelChartsListener);

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

        tableCharts.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableCharts.getColumnModel().getColumn(0).setCellRenderer((TableCellRenderer) new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) comp).setHorizontalAlignment(SwingConstants.CENTER);
                return comp;
            }
        });
        final DecimalFormat formatter = new DecimalFormat("#.############");
        for (int i = 1; i <= 4; i++) {
            tableCharts.getColumnModel().getColumn(i).setPreferredWidth(90);
            tableCharts.getColumnModel().getColumn(i).setCellRenderer((TableCellRenderer) new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    if (value != null) {
                        value = formatter.format((Double) value);
                    }
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            });
        }

        TableColumn colMarkers = tableCharts.getColumnModel().getColumn(5);
        colMarkers.setPreferredWidth(80);

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

    TableModelListener modelChartsListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (started) {
                if (e.getType() == TableModelEvent.UPDATE){
                    int i = e.getFirstRow();
                    Double y1min = (Double) modelCharts.getValueAt(i, 1);
                    Double y1max = (Double) modelCharts.getValueAt(i, 2);
                    Double y2min = (Double) modelCharts.getValueAt(i, 3);
                    Double y2max = (Double) modelCharts.getValueAt(i, 4);
                    Boolean markers = (Boolean) modelCharts.getValueAt(i, 5);
                    Boolean range = (Boolean) modelCharts.getValueAt(i, 6);
                    PlotBase plotBase = plots.get(i);
                    if (plotBase instanceof LinePlotJFree){
                        LinePlotJFree plot = (LinePlotJFree)plotBase;
                        //plot.setMarkersVisible(Boolean.TRUE.equals(markers));
                        if ((y1min != null) && (y1max != null)) {
                           plot.getAxis(AxisId.Y).setRange(y1min, y1max);                       
                        }
                        if ((y2min != null) && (y2max != null)) {
                            plot.getAxis(AxisId.Y2).setRange(y2min, y2max);
                        }                                 
                        for (LinePlotSeries s : plot.getAllSeries()){
                            s.setPointsVisible(markers);
                        }

                        for (XYItemRenderer r : plot.getChart().getXYPlot().getRenderers().values()){                        
                           ((XYErrorRenderer)r).setDrawYError(range);
                        }
                    }
                }
            }
        }
    };

    public void initializePlots() {
        numPlots=0;
        modelCharts.setRowCount(0);
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
        panelColorBackground.setBackground(backgroundColor);
        panelColorGrid.setBackground(gridColor);
        modelSeries.setRowCount(0);
modelSeries.addRow(new Object[]{true,"S10BC01-DBPM010:Q1", "sf-databuffer", "[]", PLOT_SHARED,1,null});
modelSeries.addRow(new Object[]{true,"S10BC01-DBPM010:X1", "sf-databuffer", "[]", PLOT_SHARED,2,null});
modelSeries.addRow(new Object[]{true,"SARFE10-PSSS059:FIT-COM", "sf-databuffer", "[]", PLOT_PRIVATE,1,null});
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
        modelCharts.addRow(new Object[]{numPlots+1, null, null, null, null, true, true});
        numPlots++;        
    }
    
    LinePlotJFree addLinePlot(boolean range){
        Double y1min = null;
        Double y1max = null;
        Double y2min = null;
        Double y2max = null;

        LinePlotJFree plot = new LinePlotJFree();
        if (range){
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

        addPlot(plot);
        return plot;
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
                    List<Double> average   = map.get(Daqbuf.FIELD_AVERAGE);
                    List<Double> min   = map.get(Daqbuf.FIELD_MIN);
                    List<Double> max   = map.get(Daqbuf.FIELD_MAX);
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
                                series.appendData( timestamp,  average.get(j),min.get(j), max.get(j));      
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
    
    
    SlicePlotDefault addSlicePlot(){
        SlicePlotDefault plot = new SlicePlotDefault(new MatrixPlotRenderer());        

        addPlot(plot);
        return plot;
    }
    
    SlicePlotSeries addImageSeries(SlicePlotDefault plot, String name, String backend, String start, String end){        
        SlicePlotSeries series = new SlicePlotSeries(name);
        plotSeries.add(series);
        plot.addSeries(series);
        
        series.setListener(new SlicePlotSeriesListener() {
            @Override
            public void onPageChanged(SlicePlotSeries series, int page) {
                System.out.println(page);
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
                    case 2:
                        plot = addSlicePlot();
                        series = addImageSeries((SlicePlotDefault)plot, name, start, end, backend);
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
            paint = new Color(0xA0, 0xA0, 0xA0, 0x40); 
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
        daqbuf.startSaveQuery(filename, channels.toArray(new String[0]), start, end, bins).handle((ret, ex)->{
            splash.setVisible(false);
            if (ex!=null){
                showException((Exception)ex);
            } else {
                this.showMessage("Save","Success saving data to " + filename);
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
        panelCharts = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableCharts = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return isChartTableRowEditable(row, column);
            };
        };
        jPanel1 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        panelColorBackground = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        panelColorGrid = new javax.swing.JPanel();
        buttonDefaultColors = new javax.swing.JButton();
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

        comboTime.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Last 1min", "Last 10min", "Last 1h", "Last 12h", "Last 24h", "Last 7d", "Yesterday", "Today", "Last Week", "This Week", "Last Month", "This Month" }));
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
                .addGap(12, 12, 12)
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

        spinnerBins.setModel(new javax.swing.SpinnerNumberModel(500, 1, 10000, 1));

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Max size: ");

        spinnerSize.setModel(new javax.swing.SpinnerNumberModel(10000, 1, 200000, 1));
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
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(checkBins)
                    .addComponent(spinnerBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7)
                        .addComponent(spinnerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
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

        tableCharts.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Chart", "Y1min", "Y1max", "Y2min", "Y2max", "Markers", "Range"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableCharts.setRowSelectionAllowed(false);
        tableCharts.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableCharts.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(tableCharts);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Colors"));

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel15.setText("Background:");

        panelColorBackground.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelColorBackground.setPreferredSize(new java.awt.Dimension(44, 23));
        panelColorBackground.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelColorBackgroundMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout panelColorBackgroundLayout = new javax.swing.GroupLayout(panelColorBackground);
        panelColorBackground.setLayout(panelColorBackgroundLayout);
        panelColorBackgroundLayout.setHorizontalGroup(
            panelColorBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 44, Short.MAX_VALUE)
        );
        panelColorBackgroundLayout.setVerticalGroup(
            panelColorBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel17.setText("Grid:");

        panelColorGrid.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelColorGrid.setPreferredSize(new java.awt.Dimension(44, 23));
        panelColorGrid.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelColorGridMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout panelColorGridLayout = new javax.swing.GroupLayout(panelColorGrid);
        panelColorGrid.setLayout(panelColorGridLayout);
        panelColorGridLayout.setHorizontalGroup(
            panelColorGridLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 44, Short.MAX_VALUE)
        );
        panelColorGridLayout.setVerticalGroup(
            panelColorGridLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        buttonDefaultColors.setText("Defaults");
        buttonDefaultColors.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDefaultColorsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelColorBackground, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelColorGrid, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonDefaultColors)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelColorGrid, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelColorBackground, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonDefaultColors, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {buttonDefaultColors, jLabel15, jLabel17, panelColorBackground, panelColorGrid});

        javax.swing.GroupLayout panelChartsLayout = new javax.swing.GroupLayout(panelCharts);
        panelCharts.setLayout(panelChartsLayout);
        panelChartsLayout.setHorizontalGroup(
            panelChartsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelChartsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelChartsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelChartsLayout.setVerticalGroup(
            panelChartsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelChartsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13))
        );

        tabPane.addTab("Charts", panelCharts);

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

    private void panelColorBackgroundMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelColorBackgroundMouseClicked
        Color c = JColorChooser.showDialog(this, "Choose a Color", backgroundColor);
        if (c != null) {
            backgroundColor = c;
            panelColorBackground.setBackground(backgroundColor);
            for (Plot plot : plots) {
                plot.setPlotBackgroundColor(backgroundColor);
            }
        }
    }//GEN-LAST:event_panelColorBackgroundMouseClicked

    private void panelColorGridMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelColorGridMouseClicked
        Color c = JColorChooser.showDialog(this, "Choose a Color", gridColor);
        if (c != null) {
            gridColor = c;
            panelColorGrid.setBackground(gridColor);
            for (Plot plot : plots) {
                plot.setPlotGridColor(gridColor);
            }
        }
    }//GEN-LAST:event_panelColorGridMouseClicked

    private void buttonDefaultColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDefaultColorsActionPerformed
        backgroundColor = defaultBackgroundColor;
        gridColor = defaultGridColor;
        panelColorBackground.setBackground(backgroundColor);
        panelColorGrid.setBackground(gridColor);
        for (Plot plot : plots) {
            plot.setPlotGridColor(PlotBase.getGridColor());
            plot.setPlotBackgroundColor(PlotBase.getPlotBackground());
        }
    }//GEN-LAST:event_buttonDefaultColorsActionPerformed

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
    private javax.swing.JButton buttonDefaultColors;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JButton buttonPlot;
    private javax.swing.JButton buttonSaveData;
    private javax.swing.JButton buttonUp;
    private javax.swing.JCheckBox checkBins;
    private javax.swing.JCheckBox checkUTC;
    private javax.swing.JComboBox<String> comboTime;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPanel panelCharts;
    private javax.swing.JPanel panelColorBackground;
    private javax.swing.JPanel panelColorGrid;
    private javax.swing.JPanel panelFile;
    private javax.swing.JPanel panelPlots;
    private javax.swing.JPanel panelSerie;
    private javax.swing.JPanel panelSeries;
    private javax.swing.JPanel pnGraphs;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JSpinner spinnerBins;
    private javax.swing.JSpinner spinnerSize;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JTable tableCharts;
    private javax.swing.JTable tableSeries;
    private javax.swing.JTextField textFrom;
    private javax.swing.JTextField textTo;
    // End of variables declaration//GEN-END:variables
}
