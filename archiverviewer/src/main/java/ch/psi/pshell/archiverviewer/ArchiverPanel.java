package ch.psi.pshell.archiverviewer;


import ch.psi.pshell.app.MainFrame;
import ch.psi.pshell.archiver.Daqbuf;
import ch.psi.pshell.archiver.Daqbuf.Query;
import ch.psi.pshell.archiver.Daqbuf.QueryBinnedListener;
import ch.psi.pshell.archiver.Daqbuf.QueryListener;
import ch.psi.pshell.archiver.Daqbuf.QueryRecordListener;
import ch.psi.pshell.data.FormatCSV;
import ch.psi.pshell.data.FormatText;
import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.LinePlotBase;
import ch.psi.pshell.plot.LinePlotErrorSeries;
import ch.psi.pshell.plot.LinePlotJFree;
import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.LinePlotTable;
import ch.psi.pshell.plot.MatrixPlotBase;
import ch.psi.pshell.plot.MatrixPlotJFree;
import ch.psi.pshell.plot.MatrixPlotRenderer;
import ch.psi.pshell.plot.MatrixPlotSeries;
import ch.psi.pshell.plot.MatrixPlotTable;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.Plot.AxisId;
import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.PlotBase.ZoomListener;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.plot.SlicePlotDefault;
import ch.psi.pshell.plot.SlicePlotSeries;
import ch.psi.pshell.swing.ChannelSelector;
import ch.psi.pshell.swing.PlotPanel;
import ch.psi.pshell.swing.StandardDialog;
import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.swing.SwingUtils.OptionResult;
import ch.psi.pshell.swing.SwingUtils.OptionType;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.EncoderJson;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Range;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
import ch.psi.pshell.utils.Threading;
import ch.psi.pshell.utils.Time;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYZDataset;

/**
 *
 */
public class ArchiverPanel extends StandardDialog {
    public static final String FILE_EXTENSION = "arc";
    
    public static final String[] CHANNEL_SEARCH_EXCLUDES = new String[]{"*.EGU", "*.DESC"};

    public static final String PLOT_NEW = "New";
    public static final String PLOT_SAME = "Same";
    public static final String PLOT_TABLE = "Table";
    
    public static final int AXIS_NONE = 0;
    public static final int AXIS_1 = 1;
    public static final int AXIS_2 = 2;
    public static final int AXIS_X = -1;
    
    public static final int DEFAULT_BINS = 200;

    Color defaultBackgroundColor = null;
    Color defaultGridColor = null;
    Font tickLabelFont = null;

    final DefaultTableModel modelSeries;
    final File defaultFolder;

    final ArrayList<PlotBase> plots = new ArrayList<>();
    final Map<Plot, List<SeriesInfo>> plotInfo = new HashMap<>();
    
    volatile boolean started = false;
    volatile boolean updating = false;
    DefaultComboBoxModel modelComboY = new DefaultComboBoxModel();
    ChannelSelector selector = new ChannelSelector();

    Color backgroundColor;
    Color gridColor;
    final Daqbuf daqbuf;
    int numPlots = 0;
    Integer plotHeight = null;
    
    final List<HashMap> plotProperties = new ArrayList<>();
    List requestedProperties = new ArrayList();

    final int ERROR_RANGE_ALPHA= 0x60;        
    Range queryRange;
    volatile boolean lockZoomLevels;
    JDialog channelSearchDialog; 

    public ArchiverPanel(Window parent, String url, String backend, String title, boolean modal, File defaultFolder) {
        super(parent, null, modal);        
        initComponents();        
        defaultFolder = (defaultFolder==null) ? getDaqbufFolderArg() : defaultFolder;
        this.defaultFolder = (defaultFolder == null) ?  new File(Sys.getUserHome()) : defaultFolder;        
        
        if ("default".equals(url) || "".equals(url)){
            url = null;
        }

        if ("default".equals(backend) || "".equals(backend)){
            backend = null;
        }
                
        daqbuf = new Daqbuf(url, backend);
        daqbuf.setTimestampMillis(true);
        
        if (Options.BACKGROUND_COLOR.hasValue()) {
            try {
                defaultBackgroundColor = SwingUtils.readableStringToColor(Options.BACKGROUND_COLOR.getString(null));
                backgroundColor = defaultBackgroundColor;
            } catch (Exception ex) {
                Logger.getLogger(ArchiverPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        
        if (Options.GRID_COLOR.hasValue()) {
            try {
                defaultGridColor = SwingUtils.readableStringToColor(Options.GRID_COLOR.getString(null));
                gridColor = defaultGridColor;
            } catch (Exception ex) {
                Logger.getLogger(ArchiverPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        if (Options.PLOT_HEIGHT.hasValue()) {
            try {
                plotHeight = Options.PLOT_HEIGHT.getInt(null);
            } catch (Exception ex) {
                Logger.getLogger(ArchiverPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        
        if (Options.LABEL_FONT.hasValue()) {
            try {
                String[] tokens = Options.LABEL_FONT.getString(null).split(":");
                tickLabelFont = new Font(tokens[0], Font.PLAIN, Integer.valueOf(tokens[1]));
            } catch (Exception ex) {
                Logger.getLogger(ArchiverPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        
        if (Options.AUTO_RANGE.hasValue()) {
            try {
                ckAutoRange.setSelected(Options.AUTO_RANGE.getBool(false));
            } catch (Exception ex) {
                Logger.getLogger(ArchiverPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        
        if (Options.LOCK_ZOOMS.hasValue()) {
            try {
                lockZoomLevels = Options.LOCK_ZOOMS.getBool(false);
                buttonLockZooms.setSelected(lockZoomLevels);                        
            } catch (Exception ex) {
                Logger.getLogger(ArchiverPanel.class.getName()).log(Level.WARNING, null, ex);
            }
        }        

        buttonPlotData.setEnabled(false);
        setCancelledOnEscape(false);

        //pnGraphs.setLayout(new GridBagLayout());
        pnGraphs.setLayout(new BoxLayout(pnGraphs, BoxLayout.Y_AXIS));

        modelSeries = (DefaultTableModel) tableSeries.getModel();
        modelSeries.addTableModelListener(modelSeriesListener);

        toolBar.setRollover(true);
        toolBar.setFloatable(false); //By default true in nimbus        
        
        onLafChange();                
        
        setTitle ("Connecting to " + daqbuf.getUrl() + "...");
        daqbuf.getInitialization().handle((ret, ex) ->{            
            setTitle((title==null) ? daqbuf.getUrl() : title);   
            if (daqbuf.getAvailableBackends()==null){
                showMessage("Error", "Cannot retrieve known backends from server \n" + daqbuf.getUrl());
            } else {
                setComboBackends();            
            }                    
            return ret;
        });
        clear();
    }
    
    public static File getDaqbufFolderArg() {
        return Options.ARCHIVER_VIEWER_HOME.getPath();
    }
    
    
    @Override
    protected void onLafChange() {
        String prefix = MainFrame.isDark() ? "dark/" : "";
        for (AbstractButton b : SwingUtils.getComponentsByType(toolBar, AbstractButton.class)) {
            try{
                b.setIcon(new ImageIcon(App.getResourceUrl(prefix + new File(((AbstractButton) b).getIcon().toString()).getName())));
            } catch (Exception ex){                    
            }
        }        
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
    
    void setComboBackends(){   
        String[] knownBackends = daqbuf.getAvailableBackends();
        if (knownBackends != null){
            TableColumn colType = tableSeries.getColumnModel().getColumn(2);
            DefaultComboBoxModel modelType = new DefaultComboBoxModel();
            for (String backend : knownBackends) {
                modelType.addElement(backend);
            }
            JComboBox comboBackend = new JComboBox();
            tableSeries.setRowHeight(Math.max(tableSeries.getRowHeight(), comboBackend.getPreferredSize().height - 3));
            comboBackend.setModel(modelType);
            DefaultCellEditor cellEditor = new DefaultCellEditor(comboBackend);
            cellEditor.setClickCountToStart(2);
            colType.setCellEditor(cellEditor);
            comboBackend.setSelectedIndex(0);
        }
        
    }

    public void initializeTable() {
        //Fix bug of nimbus rendering Boolean in table
        ((JComponent) tableSeries.getDefaultRenderer(Boolean.class)).setOpaque(true);
        tableSeries.getColumnModel().getColumn(0).setResizable(true);

        DefaultCellEditor disabledEditor = new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return false;
            }
        };

        TableColumn colEnabled = tableSeries.getColumnModel().getColumn(0);
        colEnabled.setPreferredWidth(60);

        TableColumn colName = tableSeries.getColumnModel().getColumn(1);
        JTextField textNameEditor = new JTextField();
        selector = new ChannelSelector();
        //selector.configure(ChannelSelector.Type.Daqbuf, null,  null, 1000);
        selector.setHistorySize(0);
        selector.setListMode(ChannelSelector.ListMode.Popup);

        colName.setPreferredWidth(320);
        colName.setCellEditor(new ChannelSelector.ChannelSelectorCellEditor(selector));

        colName.getCellEditor().addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                int row = tableSeries.getSelectedRow();
                if (row >= 0) {
                    try {
                        updateShape(row);
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
        setComboBackends();

        TableColumn colShape = tableSeries.getColumnModel().getColumn(3);
        colShape.setCellEditor(disabledEditor);
        colShape.setPreferredWidth(60);

        TableColumn colPlot = tableSeries.getColumnModel().getColumn(4);
        colPlot.setPreferredWidth(60);
        JComboBox comboPlot = new JComboBox();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(PLOT_NEW);
        model.addElement(PLOT_SAME);
        model.addElement(PLOT_TABLE);
        comboPlot.setModel(model);
        DefaultCellEditor cellEditor = new DefaultCellEditor(comboPlot);
        cellEditor.setClickCountToStart(2);
        colPlot.setCellEditor(cellEditor);

        TableColumn colY = tableSeries.getColumnModel().getColumn(5);
        colY.setPreferredWidth(60);
        JComboBox comboY = new JComboBox();
        configureModelComboY(0, false);
        comboY.setModel(modelComboY);
        cellEditor = new DefaultCellEditor(comboY);
        cellEditor.setClickCountToStart(2);
        colY.setCellEditor(cellEditor);

        TableColumn colColors = tableSeries.getColumnModel().getColumn(6);
        colColors.setPreferredWidth(60);

        class ColorEditor extends AbstractCellEditor implements TableCellEditor {

            private final JTextField field = new JTextField();
            private Color color;
            private Colormap colormap;

            ColorEditor() {
                field.setBorder(null);
                field.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            if (getRowRank(tableSeries.getSelectedRow()) == 0) {
                                colormap = null;
                                field.setText("");
                                Color c = SwingUtils.getColorWithDefault(ArchiverPanel.this, "Choose a Color", color);
                                if (c!=null){
                                    color = (c == SwingUtils.DEFAULT_COLOR) ? null : c;
                                    field.setBackground(color);
                                }                                
                            } else {
                                color = null;
                                //field.setBackground(null);                            
                                JPanel panel = new JPanel();
                                panel.setLayout(new BorderLayout(0, 30));
                                JLabel label = new JLabel("Select the Colormap:");
                                panel.add(label, BorderLayout.NORTH);
                                JComboBox combo = new JComboBox();
                                SwingUtils.setEnumCombo(combo, Colormap.class);
                                combo.setSelectedItem(colormap);
                                panel.add(combo, BorderLayout.CENTER);
                                if (showOption("Colormap", panel, OptionType.OkCancel) == OptionResult.Yes) {
                                    colormap = (Colormap) combo.getSelectedItem();
                                    field.setText(colormap.toString());
                                } else {
                                    colormap = null;
                                    field.setText("");
                                }
                                field.setBackground(null);
                            }
                            stopCellEditing();
                        }
                    }
                });
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                try {
                    colormap = Colormap.valueOf((String) value);
                    color = null;
                    field.setText(colormap.toString());
                    field.setBackground(null);
                } catch (Exception ex) {
                    colormap = null;
                    color = SwingUtils.readableStringToColor((String) value);
                    field.setText("");
                    field.setBackground(color);
                }
                field.setEditable(false);
                return field;
            }

            @Override
            public Object getCellEditorValue() {
                if (color != null) {
                    return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
                }
                if (colormap != null) {
                    return colormap.toString();
                }
                return "";
            }

            @Override
            public boolean isCellEditable(EventObject ev) {
                if (ev instanceof MouseEvent mouseEvent) {
                    return mouseEvent.getClickCount() >= 2;
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
                try {
                    Colormap colormap = Colormap.valueOf((String) value);
                    ((JLabel) comp).setBackground(null);
                    ((JLabel) comp).setText(colormap.toString());
                    ((JLabel) comp).setEnabled(true);
                } catch (Exception ex) {
                    Color color = SwingUtils.readableStringToColor((String) value);
                    ((JLabel) comp).setBackground(color);
                    ((JLabel) comp).setText("");
                    ((JLabel) comp).setEnabled(false);
                }
                comp.setForeground(table.getForeground());
                return comp;
            }
        });

        update();
    }
    
    void configureModelComboY(int rank, boolean x) {
        int curRank = modelComboY.getSize() >= 2 ? 0 : 1;
        boolean curX = modelComboY.getSize() == 3;
        if ((rank != curRank) || (x != curX)) {
            modelComboY.removeAllElements();
            if (rank == 0) {
                modelComboY.addElement("Y1");
                modelComboY.addElement("Y2");
                if (x) {
                    modelComboY.addElement("X");
                }
            } else {
                modelComboY.addElement("");
            }
        }

    }
    
    void updateSelector(){
        if (selector!=null){
            int row = tableSeries.getSelectedRow();
            if (row>=0){
                Object value = modelSeries.getValueAt(row, 2);
                selector.configure(ChannelSelector.Type.Daqbuf, daqbuf.getUrl(), value.toString(), 1000, CHANNEL_SEARCH_EXCLUDES);
            }
        }
    }

    TableModelListener modelSeriesListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (started) {
                try {
                    int index = e.getFirstRow();                    
                    switch (e.getColumn()){
                        case 2 -> updateSelector();
                        case 6 -> {
                            final Color color = SwingUtils.readableStringToColor((String) modelSeries.getValueAt(index, 6));
                            PlotSeries series = (PlotSeries) getPlotSeries(index);
                            if (series instanceof LinePlotErrorSeries linePlotErrorSeries) {
                                linePlotErrorSeries.setColor(color);
                            }
                        }

                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        }
    };

    public void initializePlots() {
        numPlots = 0;
        pnGraphs.removeAll();
        plots.clear();
    }

    int getRowRank(int row) {
        try {
            String shape = modelSeries.getValueAt(row, 3).toString().trim();
            if (shape.contains("[")){
                shape = shape.substring(shape.lastIndexOf("["));
            } else {
                shape = "[]";
            }
            if ((shape.isBlank() || shape.equals("[]"))) {
                return 0;
            }
            if (shape.indexOf(",") >= 0) {
                return 2;
            }
            return 1;
        } catch (Exception ex) {
            return 0;
        }
    }
    
    String getRowType(int row) {
        try {
            String shape = modelSeries.getValueAt(row, 3).toString().trim();
            if (shape.contains("[")){
                shape = shape.substring(0,shape.lastIndexOf("["));
            } 
            return shape.trim();
        } catch (Exception ex) {            
        }        
        return null;
    }

    JButton saveButton;

    protected void update() {
        boolean editing = true; //(knownBackends!=null);
        int rows = modelSeries.getRowCount();
        int cur = tableSeries.getSelectedRow();
        buttonRowUp.setEnabled((rows > 0) && (cur > 0) && editing);
        buttonRowDown.setEnabled((rows > 0) && (cur >= 0) && (cur < (rows - 1)) && editing);
        buttonRowDelete.setEnabled((rows > 0) && (cur >= 0) && editing);
        buttonRowInsert.setEnabled(editing);
        buttonPlotData.setEnabled(modelSeries.getRowCount() > 0);
        buttonDumpData.setEnabled(buttonPlotData.isEnabled() && !dumping);
        buttonSave.setEnabled(rows > 0);
        
        textFrom.setEnabled(editing);
        textTo.setEnabled(editing);
        comboTime.setEnabled(editing);
        checkBins.setEnabled(editing);
        spinnerBins.setEnabled(editing);
        spinnerSize.setEnabled(editing);
        spinnerBins.setEnabled(checkBins.isSelected() && editing);
        spinnerSize.setEnabled(!checkBins.isSelected() & editing);
        

        int row = tableSeries.getSelectedRow();
        if (row >= 0) {
            //boolean shared = PLOT_SHARED.equals(modelSeries.getValueAt(row, 4));
            //boolean first = (row == 0) || (!PLOT_SHARED.equals(modelSeries.getValueAt(row - 1, 4)));
            
            boolean first = PLOT_NEW.equals(modelSeries.getValueAt(row, 4));
            boolean shared = (modelSeries.getRowCount() > (row+1))  &&  (PLOT_SAME.equals(modelSeries.getValueAt(row +1, 4)));
            
            configureModelComboY(getRowRank(row), shared && first);
        }        
        updateSelector();
    }

    CompletableFuture updateShape(int row) {
        String channel = (String) modelSeries.getValueAt(row, 1);
        String exactName = "^" + channel + "$";
        String backend = (String) modelSeries.getValueAt(row, 2);
        CompletableFuture cf = daqbuf.startSearch(backend, exactName, null, 1).handle((ret, ex) -> {
            if (ex != null) {
                //showException((Exception) ex);
                modelSeries.setValueAt("", row, 3);
            } else {
                List<Map<String, Object>> list = (List<Map<String, Object>>) ret;
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        if (channel.equals(modelSeries.getValueAt(row, 1)) && backend.equals(modelSeries.getValueAt(row, 2))) {
                            String shape = (list.size() > 0) ? Str.toString(list.get(0).getOrDefault("shape", "[]")) : "[]";
                            String type = (list.size() > 0) ? Str.toString(list.get(0).getOrDefault("type", "")) : "";
                            boolean is_enum = (list.size() > 0) ? "enum".equalsIgnoreCase(type) : false;
                            if ((type!=null) & (!type.isBlank())){
                                shape = type + shape;
                            }
                            modelSeries.setValueAt(shape, row, 3);
                        }
                    });
                } catch (Exception e) {
                    showException((Exception) e);
                }
            }
            return ret;
        });
        return cf;
    }


    boolean isSeriesTableRowEditable(int row, int column) {
        return true;
    }

    boolean isChartTableRowEditable(int row, int column) {
        return (column > 0);
    }

    public void clear() {
        Logger.getLogger(ArchiverPanel.class.getName()).info("Init");
        reset();
        updating = true;
        try {
            backgroundColor = defaultBackgroundColor;
            gridColor = defaultGridColor;

            modelSeries.setRowCount(0);
            textFrom.setText("");
            textTo.setText("");
            checkBins.setSelected(true);
            spinnerBins.setValue(DEFAULT_BINS);
            setMaxSeriesSize(DEFAULT_MAX_SERIES_SIZE);
            comboTime.setSelectedIndex(0);
        } finally {
            updating = false;
            file = null;
            initializeTable();
            update();
        }
    }

    String getInitFormat() {
        if (Context.hasDataManager()) {
            if (Context.getDataManager().getFormat() instanceof FormatCSV) {
                return "csv";
            }
            if (Context.getDataManager().getFormat() instanceof FormatText) {
                return "txt";
            }
        }
        return "h5";
    }

    String getInitLayout() {
        return "table";
    }

    File file;

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

    String removeAlias(String str) {
        str = str.trim();
        if (str.endsWith(">")) {
            int start = str.lastIndexOf('<');
            if (start >= 0) {
                str = str.substring(0, start);
            }
        }
        return str;
    }

    String getChannelName(String str) {
        str = removeAlias(str);
        if (str.contains(" ")) {
            String[] tokens = str.split(" ");
            str = tokens[0];
        }
        return str.trim();
    }

    String[] getChannelArgs(String str) {
        str = removeAlias(str);
        if (str.contains(" ")) {
            String[] tokens = str.split(" ");
            String[] args = Arr.remove(tokens, 0);
            args = Arr.removeEquals(args, "");
            return args;
        }
        return new String[0];
    }

    String getChannelAlias(String str) {
        str = str.trim();
        if (str.endsWith(">")) {
            int end = str.lastIndexOf('>');
            int start = str.lastIndexOf('<');
            if (start >= 0) {
                String alias = str.substring(start + 1, end);
                if (!alias.isBlank()) {
                    return alias;
                }
            }
        }
        return getChannelName(str);
    }
    
    public final static int DEFAULT_MAX_SERIES_SIZE = 250_000;
    
    int getMaxSeriesSize(){
        return ((Integer) spinnerSize.getValue() * 1000);
    }

    void setMaxSeriesSize(Integer bytes){
        spinnerSize.setValue(bytes / 1000);
    }    

    void addPlot(PlotBase plot) {
        addPlot(plot, ckAutoRange.isSelected());
    }
    
    void addPlot(PlotBase plot, boolean autoRange) {
        if (backgroundColor != null) {
            plot.setPlotBackgroundColor(backgroundColor);
        }
        if (gridColor != null) {
            plot.setPlotGridColor(gridColor);
        }
        if ((plotHeight!=null) && (plotHeight>=0)){
            plot.setPreferredSize(new Dimension (plot.getPreferredSize().width, plotHeight));
        }
        if (tickLabelFont != null) {
            plot.setLabelFont(tickLabelFont);
            plot.setTickLabelFont(tickLabelFont);
        }
        plot.setTitle(null);
        plot.setQuality(PlotPanel.getQuality());
        if ((!autoRange) && (queryRange!=null)){
            plot.getAxis(AxisId.X).setRange(queryRange.min, queryRange.max);
        }
        plots.add(plot);
        pnGraphs.add(plot);
        List<SeriesInfo> series = new ArrayList<>();
        plotInfo.put(plot, series);
        plotProperties.add(new HashMap());
        numPlots++;
    }
    
    int getPlotIndex(PlotBase plot){
        for (int i=0; i< plots.size(); i++){
            if (plots.get(i) == plot){
                return i;
            }
        }
        return -1;
    }
    
    DateAxis setDateDomainAxis(PlotBase plot){
        DateAxis axis = new DateAxis(null); //("Time");
        axis.setLabelFont((plot instanceof LinePlotJFree linePlot) ? linePlot.getLabelFont() : PlotBase.getDefaultLabelFont());
        axis.setLabelPaint(plot.getAxisTextColor());
        axis.setTickLabelPaint(plot.getAxisTextColor());
        axis.setAutoRange(plot.getAxis(AxisId.X).isAutoRange());
        if (plot instanceof LinePlotJFree linePlotJFree){
            linePlotJFree.getChart().getXYPlot().setDomainAxis(axis);        
        } else if (plot instanceof MatrixPlotJFree matrixPlotJFree){
            matrixPlotJFree.getChart().getXYPlot().setDomainAxis(axis);        
        }
        return axis;
    }

    NumberAxis setNumberDomainAxis(PlotBase plot){
        NumberAxis axis = new NumberAxis(null); 
        axis.setLabelFont((plot instanceof LinePlotJFree linePlot) ? linePlot.getLabelFont() : PlotBase.getDefaultLabelFont());
        axis.setLabelPaint(plot.getAxisTextColor());
        axis.setTickLabelPaint(plot.getAxisTextColor());
        axis.setAutoRange(plot.getAxis(AxisId.X).isAutoRange());
        if (plot instanceof LinePlotJFree linePlotJFree){
            linePlotJFree.getChart().getXYPlot().setDomainAxis(axis);        
        } else if (plot instanceof MatrixPlotJFree matrixPlotJFree){
            matrixPlotJFree.getChart().getXYPlot().setDomainAxis(axis);        
        }
        return axis;
    }
    
    
    boolean isDateDomainAxis(PlotBase plot){
        if (plot instanceof LinePlotJFree linePlotJFree){
            return linePlotJFree.getChart().getXYPlot().getDomainAxis() instanceof DateAxis;        
        } else if (plot instanceof MatrixPlotJFree matrixPlotJFree){
            return matrixPlotJFree.getChart().getXYPlot().getDomainAxis() instanceof DateAxis;  
        }
        return false;
    }

    void setPlotArg(PlotBase plot, String name, Object value){
        int index = getPlotIndex(plot);
        if ((index>=0) && (index<plotProperties.size())){
            ((HashMap)plotProperties.get(index)).put(name, value);
        }                            
    }
    
    void showMarkers(LinePlotJFree plot, boolean value){
        for (LinePlotSeries s : plot.getAllSeries()) {
            s.setPointsVisible(value);
        }
        setPlotArg(plot, "show_markers", value);
    }
    
    
    void showRanges(LinePlotJFree plot, boolean value){
        for (XYItemRenderer r : plot.getChart().getXYPlot().getRenderers().values()) {
            ((XYErrorRenderer) r).setDrawYError(value);
        }
        setPlotArg(plot, "show_ranges", value);                   
    }
    
    void setBackgroundColor(LinePlotJFree plot, Color c){
        plot.setPlotBackgroundColor((c == SwingUtils.DEFAULT_COLOR) ? PlotBase.getPlotBackground() : c);
        setPlotArg(plot, "background_color",  SwingUtils.colorToString(c));
    }

    void setGridColor(LinePlotJFree plot, Color c){
        plot.setPlotGridColor((c == SwingUtils.DEFAULT_COLOR) ? PlotBase.getGridColor() : c);
        setPlotArg(plot, "grid_color",  SwingUtils.colorToString(c));
    }

    void setRangeY1(LinePlotJFree plot, Range r){
        if (r != null) {
            plot.getAxis(AxisId.Y).setRange(r.min, r.max);
            setPlotArg(plot, "y1_range",  Arrays.asList(new Double[]{r.min, r.max}));
        } else {
            plot.getAxis(AxisId.Y).setAutoRange();
            setPlotArg(plot, "y1_range",  null);
        }                    
    }
    
    void setRangeY2(LinePlotJFree plot, Range r){
        if (r != null) {
            plot.getAxis(AxisId.Y2).setRange(r.min, r.max);
            setPlotArg(plot, "y2_range",  Arrays.asList(new Double[]{r.min, r.max}));
        } else {
            plot.getAxis(AxisId.Y2).setAutoRange();
            setPlotArg(plot, "y2_range",  null);
        }        
    }
    
    void setLogY1(LinePlotJFree plot, boolean log){
        plot.getAxis(AxisId.Y).setLogarithmic(log);
        setPlotArg(plot, "y1_log",  log);
    }

    void setLogY2(LinePlotJFree plot, boolean log){
        plot.getAxis(AxisId.Y2).setLogarithmic(log);
        setPlotArg(plot, "y2_log",  log);
    }
    
    
    void applyPlotPreferences(List plotPrefs){
        try{
            if (plotPrefs.size()==plots.size()){
                for (int i=0; i< plotPrefs.size();i++){                                
                    if (plots.get(i) instanceof LinePlotJFree plot){                
                        Map prefs = (Map)plotPrefs.get(i);
                        Boolean show_ranges =  (Boolean)prefs.getOrDefault("show_ranges", null);
                        Boolean show_markers =  (Boolean)prefs.getOrDefault("show_markers", null);
                        String background_color =  (String)prefs.getOrDefault("background_color", null);
                        String grid_color =  (String)prefs.getOrDefault("grid_color", null);
                        List y1_range =  (List)prefs.getOrDefault("y1_range",null);
                        List y2_range =  (List)prefs.getOrDefault("y2_range", null);
                        Boolean y1_log =  (Boolean)prefs.getOrDefault("y1_log", null);
                        Boolean y2_log =  (Boolean)prefs.getOrDefault("y2_log", null);

                        if (show_ranges!=null){
                            showRanges(plot, show_ranges);
                        }
                        if (show_markers!=null){
                            showMarkers(plot, show_markers);
                        }
                        if (background_color!=null){
                            setBackgroundColor(plot, SwingUtils.stringToColor(background_color));
                        }
                        if (grid_color!=null){
                            setGridColor(plot, SwingUtils.stringToColor(grid_color));
                        }
                        if ((y1_range!=null) && (y1_range.size()==2)){
                            setRangeY1(plot, new Range(((Number)y1_range.get(0)).doubleValue(), ((Number)y1_range.get(1)).doubleValue()));
                        }
                        if ((y2_range!=null) && (y2_range.size()==2)){
                            setRangeY2(plot, new Range(((Number)y2_range.get(0)).doubleValue(), ((Number)y2_range.get(1)).doubleValue()));
                        }
                        if (y1_log!=null){
                            setLogY1(plot, y1_log);
                        }
                        if (y2_log!=null){
                            setLogY2(plot, y2_log);
                        }                        
                    }                
                }
             }
        } catch (Exception ex){
            showException(ex);
        }
    }    

    
    void setLinePlotPopupMenu(LinePlotJFree plot, boolean binned){
        plot.addPopupMenuItem(null);        

        JCheckBoxMenuItem menuMarkers = new JCheckBoxMenuItem("Show Markers");
        menuMarkers.setSelected(true);
        menuMarkers.addActionListener((ActionEvent e) -> {
            boolean show = menuMarkers.isSelected();
            showMarkers(plot, show);
        });
        plot.addPopupMenuItem(menuMarkers);

        JCheckBoxMenuItem menuRanges = new JCheckBoxMenuItem("Show Ranges");
        menuRanges.setSelected(binned);
        menuRanges.addActionListener((ActionEvent e) -> {
            boolean show = menuRanges.isSelected();
            showRanges(plot, show);
        });
        plot.addPopupMenuItem(menuRanges);
        menuRanges.setVisible(binned);

        JMenuItem menuBackgroundColor = new JMenuItem("Set Background Color...");
        menuBackgroundColor.addActionListener((ActionEvent e) -> {
            Color c = SwingUtils.getColorWithDefault(ArchiverPanel.this, "Choose a Color", plot.getPlotBackgroundColor());
            if (c!=null){
                setBackgroundColor(plot, c);
            }                                            
        });
        plot.addPopupMenuItem(menuBackgroundColor);

        JMenuItem menuGridColor = new JMenuItem("Set Grid Color...");
        menuGridColor.addActionListener((ActionEvent e) -> {
            Color c = SwingUtils.getColorWithDefault(ArchiverPanel.this, "Choose a Color", plot.getPlotGridColor());
            if (c!=null){
                setGridColor(plot, c);
            }                                
        });
        plot.addPopupMenuItem(menuGridColor);

        JMenuItem menuRangeY1 = new JMenuItem("Set Y1 Range...");
        menuRangeY1.addActionListener((ActionEvent e) -> {
            Range init = plot.getAxis(AxisId.Y).isAutoRange() ? null : new Range(plot.getAxis(AxisId.Y).getMin(), plot.getAxis(AxisId.Y).getMax());
            boolean log =  plot.getAxis(AxisId.Y).isLogarithmic();
            RangeLog ret = getRange(init, log, "Enter Y1 Range");
            if (ret!=null){
                setRangeY1(plot, ret.range);
                setLogY1(plot, ret.log);
            }
            
        });
        plot.addPopupMenuItem(menuRangeY1);

        JMenuItem menuRangeY2 = new JMenuItem("Set Y2 Range...");
        menuRangeY2.addActionListener((ActionEvent e) -> {
            Range init = plot.getAxis(AxisId.Y2).isAutoRange() ? null : new Range(plot.getAxis(AxisId.Y2).getMin(), plot.getAxis(AxisId.Y2).getMax());
            boolean log =  plot.getAxis(AxisId.Y2).isLogarithmic();
            RangeLog ret = getRange(init, log, "Enter Y2 Range");
            if (ret!=null){
                setRangeY2(plot, ret.range);
                setLogY2(plot, ret.log);
            }            
        });
        plot.addPopupMenuItem(menuRangeY2);

        JMenuItem menuUnbin = new JMenuItem("Unbinned");
        menuUnbin.addActionListener((ActionEvent e) -> {
            unbin(plot);
            if (lockZoomLevels){
                for (PlotBase p: plots){
                    if (p!=plot){
                        if (isBinnedPlot(p)){
                            unbin(p);
                        }
                    }
                }
            }            
        });
        plot.addPopupMenuItem(menuUnbin);
        menuUnbin.setVisible(binned);
        

        JMenuItem menuRequery = new JMenuItem("Requery");
        menuRequery.addActionListener((ActionEvent e) -> {
            requery(plot);
            if (lockZoomLevels){
                for (PlotBase p: plots){
                    if (p!=plot){
                        if (isBinnedPlot(p)){
                            requery(p);
                        }
                    }
                }
            }
        });
        plot.addPopupMenuItem(menuRequery);
        menuRequery.setVisible(binned);
        //plot.removePopupMenuItem("Logarithmic Scale");
    }

    boolean updatingZoom;
    void lockZooms(PlotBase source, Range rangeX, Range rangeY){
            if (!updatingZoom){
                updatingZoom=true;
                //Invoke later to update first plot that was zoomed
                SwingUtilities.invokeLater(()->{
                    try{
                        for (PlotBase p: plots){
                            if ((source!=p) && (p.getZoomListener() == zoomListener)){
                                if (isDateDomainAxis(p)){
                                    if (p instanceof LinePlotJFree linePlotJFree){
                                        linePlotJFree.zoomDomain(rangeX);
                                    }  else if (p instanceof MatrixPlotJFree matrixPlotJFree){
                                        matrixPlotJFree.zoomDomain(rangeX);
                                    }
                                }
                            }
                        }
                    } finally {
                        updatingZoom = false;
                    }
                });
            }        
    }
            
    final ZoomListener zoomListener = (PlotBase source, Range rangeX, Range rangeY) -> {      
        if (lockZoomLevels){
            if (isDateDomainAxis(source)){
                lockZooms(source, rangeX, rangeY);
            }
        }
    };
       
        
    LinePlotJFree addLinePlot(boolean binned) {
        Double y1min = null;
        Double y1max = null;
        Double y2min = null;
        Double y2max = null;

        LinePlotJFree plot = new LinePlotJFree();
        plot.setLineExtension(!binned);
        plot.setExtensionThreshold(queryRange.max);
        plot.setZoomListener(zoomListener);
        if (binned) {
            plot.setStyle(LinePlot.Style.ErrorY);
            plot.setErrorRangeAlpha(ERROR_RANGE_ALPHA);
        }
        DateAxis axis = setDateDomainAxis(plot);        
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

        setLinePlotPopupMenu(plot, binned);                
        addPlot(plot);
        return plot;
    }

    LinePlotTable addLineTablePlot(boolean binned) {
        LinePlotTable plot = new LinePlotTable();
        plot.getAxis(AxisId.X).setLabel("Timestamp");
        plot.setZoomListener(zoomListener);
        plot.setTimeString(true);
        addPlot(plot);
        return plot;
    }
    
    
    LinePlotJFree addCorrelationPlot(boolean binned) {
        LinePlotJFree plot = new LinePlotJFree();
        plot.setLegendVisible(true);
        plot.addPopupMenuItem(null);
        plot.getAxis(AxisId.X).setLabel(null);
        plot.getAxis(AxisId.Y).setLabel(null);

        JCheckBoxMenuItem menuMarkers = new JCheckBoxMenuItem("Show Markers");
        menuMarkers.setSelected(true);
        menuMarkers.addActionListener((ActionEvent e) -> {
            boolean show = menuMarkers.isSelected();
            for (LinePlotSeries s : plot.getAllSeries()) {
                s.setPointsVisible(show);
            }
        });
        plot.addPopupMenuItem(menuMarkers);

        JMenuItem menuBackgroundColor = new JMenuItem("Set Background Color...");
        menuBackgroundColor.addActionListener((ActionEvent e) -> {
            Color c = SwingUtils.getColorWithDefault(ArchiverPanel.this, "Choose a Color", plot.getPlotBackgroundColor());
            if (c!=null){
                plot.setPlotBackgroundColor((c == SwingUtils.DEFAULT_COLOR) ? PlotBase.getPlotBackground() : c);
            }   
        });
        plot.addPopupMenuItem(menuBackgroundColor);

        JMenuItem menuGridColor = new JMenuItem("Set Grid Color...");
        menuGridColor.addActionListener((ActionEvent e) -> {
            Color c = SwingUtils.getColorWithDefault(ArchiverPanel.this, "Choose a Color", plot.getPlotGridColor());
            if (c!=null){
                plot.setPlotGridColor((c == SwingUtils.DEFAULT_COLOR) ? PlotBase.getGridColor() : c);
            }   
            
        });
        plot.addPopupMenuItem(menuGridColor);
        addPlot(plot, true);
        return plot;
    }

    void requery(PlotBase plot) {
        XYPlot xyplot = (plot instanceof LinePlotJFree linePlot)
                ? linePlot.getChart().getXYPlot()
                : ((MatrixPlotJFree) plot).getChart().getXYPlot();
        if (!(xyplot.getDomainAxis() instanceof DateAxis)) {
            return;
        }
        double xmin = xyplot.getDomainAxis().getRange().getLowerBound();
        double xmax = xyplot.getDomainAxis().getRange().getUpperBound();
        String start = Time.millisToStr((long) xmin, false);
        String end = Time.millisToStr((long) xmax, false);
        SeriesInfo[] formerInfo = plotInfo.get(plot).toArray(new SeriesInfo[0]);
        plotInfo.get(plot).clear();
        plot.clear();
        if (!plot.getAxis(AxisId.X).isAutoRange()){
            plot.getAxis(AxisId.X).setRange(xmin, xmax);
        }
        for (SeriesInfo seriesInfo : formerInfo) {
            seriesInfo = new SeriesInfo(seriesInfo, start, end, seriesInfo.bins);
            PlotSeries series = switch (seriesInfo.getRank()){
                case 0 -> {
                    if (seriesInfo.bins == null) {
                        yield addLineSeries((LinePlotJFree) plot, seriesInfo);
                    } else {
                        yield addBinnedSeries((LinePlotJFree) plot, seriesInfo);
                    }
                }
                case 1 -> {
                    if (seriesInfo.bins == null) {
                        yield addMatrixSeries((MatrixPlotBase) plot, seriesInfo);
                    } else {
                        yield addMatrixSeriesBinned((MatrixPlotBase) plot, seriesInfo);
                    }
                }
                default -> null;
            };
            if (series != null) {
                plotInfo.get(plot).add(seriesInfo);
            }
        }
    }            
    
    void unbin(PlotBase plot) {
        XYPlot xyplot = (plot instanceof LinePlotJFree linePlot)
                ? linePlot.getChart().getXYPlot()
                : ((MatrixPlotJFree) plot).getChart().getXYPlot();
        if (!(xyplot.getDomainAxis() instanceof DateAxis)) {
            return;
        }
        double xmin = xyplot.getDomainAxis().getRange().getLowerBound();
        double xmax = xyplot.getDomainAxis().getRange().getUpperBound();
        String start = Time.millisToStr((long) xmin, false);
        String end = Time.millisToStr((long) xmax, false);
        SeriesInfo[] formerInfo = plotInfo.get(plot).toArray(new SeriesInfo[0]);
        plotInfo.get(plot).clear();
        plot.clear();
        if ( plot instanceof LinePlotJFree linePlotJFree){
            linePlotJFree.setStyle(LinePlot.Style.Normal);
            setDateDomainAxis(plot);
            setLinePlotPopupMenu(linePlotJFree, false); 
            if (!plot.getAxis(AxisId.X).isAutoRange()){
                plot.getAxis(AxisId.X).setRange(xmin, xmax);
            }        
        } else {
            JPopupMenu menu = ((MatrixPlotJFree)plot).getChartPanel().getPopupMenu();
            for (Component c : List.of(menu.getComponents())){                
                if (c instanceof JMenuItem menuItem){
                    if (("Unbinned".equals(menuItem.getText())) || ("Requery".equals(menuItem.getText()))){ 
                        menu.remove(c);
                    }
                }
            }
            setNumberDomainAxis(plot);
        }
        for (SeriesInfo seriesInfo : formerInfo) {
            seriesInfo = new SeriesInfo(seriesInfo, start, end, null);
            PlotSeries series = null;
            switch (seriesInfo.getRank()) {
                case 0 -> series = addLineSeries((LinePlotJFree) plot, seriesInfo);
                case 1 -> series = addMatrixSeries((MatrixPlotJFree) plot, seriesInfo);
            }
            if (series != null) {
                plotInfo.get(plot).add(seriesInfo);
            }
        }
    }
    
    class RangeLog{
        final Range range;
        final boolean log;
        RangeLog(Range range, boolean log){
            this.range = range;
            this.log=log;
        }
    }
    
    RangeLog getRange(Range init, boolean logarithmic, String title) {
        try {
            JPanel panel = new JPanel();
            GridBagLayout layout = new GridBagLayout();
            layout.columnWidths = new int[]{0, 180};   //Minimum width
            panel.setLayout(layout);
            JTextField min = new JTextField((init == null) ? "" : String.valueOf(init.min));
            JTextField max = new JTextField((init == null) ? "" : String.valueOf(init.max));
            JCheckBox log = new JCheckBox("", logarithmic);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            panel.add(new JLabel("Minimum:"), c);
            c.gridy = 1;
            panel.add(new JLabel("Maximum:"), c);
            c.gridy = 2;
            c.insets = new Insets(8,0,0,0);
            panel.add(new JLabel("Logarithmic:"), c);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            panel.add(log, c);
            c.insets = new Insets(0,0,0,0);
            c.gridy = 1;
            panel.add(max, c);
            c.gridy = 0;            
            panel.add(min, c);

            if (showOption((title == null) ? "Enter Range" : title, panel, OptionType.OkCancel) == OptionResult.Yes) {
                Range range = null;
                try{
                    range = new Range(Double.valueOf(min.getText().trim()), Double.valueOf(max.getText().trim()));
                } catch (Exception ex) {            
                }                
                return new RangeLog(range, log.isSelected());
            }
        } catch (Exception ex) {            
        }
        return null;
    }
    
    LinePlotSeries addBinnedSeries(LinePlotBase plot, SeriesInfo si) {
        return addBinnedSeries(plot, si.name, si.backend, si.start, si.end, si.bins, si.axis, si.color);
    }

    LinePlotSeries addBinnedSeries(LinePlotBase plot, String name, String backend, String start, String end, int bins, int axis, Color color) {
        LinePlotSeries series;                
        LinePlotJFree linePlot  = plot instanceof LinePlotTable ? null : (LinePlotJFree) plot;        
        LinePlotErrorSeries errorSeries;

        if (linePlot != null){
            series = new LinePlotErrorSeries(name, color, axis);
            errorSeries = (LinePlotErrorSeries)series;
            plot.addSeries(series);
            XYErrorRenderer renderer = (XYErrorRenderer) linePlot.getSeriesRenderer(series);
            renderer.setDrawYError(true);
            renderer.setErrorStroke(new BasicStroke());            
        } else {            
            series = new LinePlotSeries(name, color, axis);
            plot.addSeries(series);
            errorSeries = null;
        }
        plotSeries.add(series);
        
       

        try {
            if (daqbuf.isStreamed()){
                daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, new QueryBinnedListener() {
                    public void onMessage(Query query, List average, List min, List max, List<Integer> count, List<Long> t1, List<Long> t2) {                                                
                        if (average.size()>0){
                            List<Long> t = IntStream.range(0, t1.size()).mapToObj(i -> (t1.get(i) + t2.get(i)) / 2).collect(Collectors.toList());    
                            synchronized(plot){
                                try {
                                    plot.disableUpdates();                                    
                                    if (errorSeries!=null){
                                        errorSeries.appendData(t, average, min, max);   
                                        updateCapLength(linePlot, series.getAxisY()-1);
                                        //plot.getChartPanel().restoreAutoBounds();
                                    } else {
                                        series.appendData(t, average);
                                    }
                                } finally {
                                     plot.reenableUpdates();
                                }
                            }
                        }                                    
                    }
                }, bins).handle((ret, ex) -> {
                    if (ex != null) {
                        showException((Exception) ex);
                    } else {
                        if (linePlot!=null){
                            synchronized(plot){
                                if (plot.getAxis(series).isLogarithmic()){
                                    linePlot.getChartPanel().restoreAutoBounds();
                                }
                            }
                        }
                    }                    
                    return ret;
                });                
                
            } else {
                daqbuf.startFetchQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, bins).handle((ret, ex) -> {
                    if (ex != null) {
                        showException((Exception) ex);
                    } else {
                        Map<String, List> map = (Map<String, List>) ret;
                        List<Number> average = map.get(Daqbuf.FIELD_AVERAGE);
                        List<Number> min = map.get(Daqbuf.FIELD_MIN);
                        List<Number> max = map.get(Daqbuf.FIELD_MAX);
                        List<Long> t1 = map.get(Daqbuf.FIELD_START);
                        List<Long> t2 = map.get(Daqbuf.FIELD_END);
                        List<Long> t = IntStream.range(0, t1.size()).mapToObj(i -> (t1.get(i) + t2.get(i)) / 2).collect(Collectors.toList());                                        
                        synchronized(plot){
                            try {
                                plot.disableUpdates();                                
                                if (errorSeries!=null){
                                    errorSeries.appendData(t, average, min, max);
                                    updateCapLength(linePlot);
                                    linePlot.getChartPanel().restoreAutoBounds();
                                } else {
                                    series.appendData(t, average);
                                }
                            } finally {
                                plot.reenableUpdates();                        
                            }
                            if (linePlot!=null) {
                                if (plot.getAxis(series).isLogarithmic()){
                                    linePlot.getChartPanel().restoreAutoBounds();
                                }                            
                            }
                        }
                    }
                    return ret;
                });            
            }
            if (linePlot!=null){
                checkEgu(linePlot, series, backend, start);
            }
        } catch (Exception ex) {
            showException(ex);
        }
        return series;
    }
    
    void checkEgu(LinePlotJFree plot, LinePlotSeries series, String backend, String start){
            String name = series.getName();
            //Strip field if present.
            if (name.contains(".")){
                name = name.substring(0, name.lastIndexOf("."));
            }
            daqbuf.startFetchQuery(name + ".EGU" + Daqbuf.BACKEND_SEPARATOR + backend, start, null).handle((ret, ex) -> {
                if (ex != null) {
                } else {
                    Map<String, List> map = (Map<String, List>) ret;
                    List<String> values = map.getOrDefault(Daqbuf.FIELD_VALUE, new ArrayList());
                    if (values.size()>0){
                        String egu = Str.toString(values.get(values.size()-1));                        
                        if (!egu.isBlank()){
                            plot.renameSeries(series, series.getName() + " [" + egu + "]");
                            plot.getSeries(series).setDescription(egu);
                        }
                    }
                }
                return ret;
            });            
        
    }
    
    LinePlotSeries addLineSeries(LinePlotBase plot, SeriesInfo si) {
        return addLineSeries(plot, si.name, si.backend, si.start, si.end, si.axis, si.color, si.type);
    }

    LinePlotSeries addLineSeries(LinePlotBase plot, String name, String backend, String start, String end, int axis, Color color, String type) {
        LinePlotSeries series = new LinePlotSeries(name, color, axis);
        plotSeries.add(series);
        plot.addSeries(series);
        series.setMaxItemCount(getMaxSeriesSize());
        final Map<Number,String> valueStrings =  (plot instanceof LinePlotTable) && type.equals("enum") ? new HashMap<>() : null;
        LinePlotJFree linePlot  = plot instanceof LinePlotTable ? null : (LinePlotJFree) plot;        
                        
        try {
            daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start.startsWith(Daqbuf.ADD_LAST_PREFIX) ? start : Daqbuf.ADD_LAST_PREFIX+start, end, new QueryListener() {
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {
                    List<Number> aux = (List<Number>) values;
                    boolean fits = true;
                    if ((series.getCount()+ aux.size()) > series.getMaxItemCount()) {
                        fits = false;
                        int size = series.getMaxItemCount()-series.getCount();
                        aux=aux.subList(0, size);
                        timestamps=timestamps.subList(0, size);
                    }
                    if (aux.size()>0){
                        if  (valueStrings!=null){
                            for (Object value : values){
                                if (value instanceof Daqbuf.EnumValue enumValue){
                                    valueStrings.put(enumValue.intValue(), enumValue.stringValue());
                                } else {
                                    break;
                                }
                            }
                        }
                        synchronized(plot){
                            try {
                                plot.disableUpdates();
                                series.appendData(timestamps, aux);
                            } finally {
                                 plot.reenableUpdates();
                            }
                        }
                    }
                    
                    if (!fits) {
                        throw new RuntimeException("Series too big for plotting: " + name);
                    }                                       
                }                
            }).handle((ret, ex) -> {
                if (ex != null) {
                    showException((Exception) ex);
                } else {
                    if (linePlot!=null){
                        synchronized(plot){
                            if (plot.getAxis(series).isLogarithmic()){
                                linePlot.getChartPanel().restoreAutoBounds();
                            }
                        }
                    }
                }
                return ret;
            });
            if (plot instanceof LinePlotJFree linePlotJFree){
                checkEgu(linePlotJFree, series, backend, start);
            } else if (plot instanceof LinePlotTable linePlotTable){
                if (type.equals("enum")){
                    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
                        @Override
                        protected void setValue(Object value) {
                            String string = "";
                            if (value instanceof Number number){
                                int intval = number.intValue();
                                String valueString = valueStrings.getOrDefault(intval, null);
                                if ((valueString!=null) && !valueString.isBlank()){
                                    string = intval + ":" + valueString;
                                } else {
                                    string = String.valueOf(intval);
                                }
                                
                            }
                            super.setValue(string);
                        };
                    };
                    renderer.setHorizontalAlignment(SwingConstants.RIGHT);
                    linePlotTable.setSeriesRenderer(series, renderer);
                }
            }
                    
        } catch (Exception ex) {
            showException(ex);
        }
        return series;
    }

    CompletableFuture setDomainAxis(LinePlotJFree plot, SeriesInfo si) {
        return setDomainAxis(plot, si.name, si.backend, si.start, si.end, si.bins, si.axis, si.color);
    }

    CompletableFuture setDomainAxis(LinePlotJFree plot, String name, String backend, String start, String end, Integer bins, int axis, Color color) {
        try {
            plot.getAxis(AxisId.X).setLabel(name);
            return Threading.getPrivateThreadFuture(() -> {
                try {
                    Map<String, List> map = daqbuf.fetchQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, bins);
                    List<Number> values = map.keySet().contains(Daqbuf.FIELD_AVERAGE) ? map.get(Daqbuf.FIELD_AVERAGE) : map.get(Daqbuf.FIELD_VALUE);
                    return values;
                } catch (Exception ex) {
                    return null;
                }

            });
        } catch (Exception ex) {
            showException(ex);
            return null;
        }
    }

    LinePlotSeries addCorrelationSeries(LinePlotBase plot, SeriesInfo si, CompletableFuture domainAxisFuture) {
        return addCorrelationSeries(plot, si.name, si.backend, si.start, si.end, si.bins, si.axis, si.color, domainAxisFuture);
    }

    LinePlotSeries addCorrelationSeries(LinePlotBase plot, String name, String backend, String start, String end, Integer bins, int axis, Color color, CompletableFuture domainAxisFuture) {
        LinePlotSeries series = new LinePlotSeries(name, color, axis);
        plotSeries.add(series);
        plot.addSeries(series);
        series.setMaxItemCount(getMaxSeriesSize());
        series.setLinesVisible(false);
        try {
            daqbuf.startFetchQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, bins).handle((ret, ex) -> {
                if (ex != null) {
                    showException((Exception) ex);
                } else {
                    Map<String, List> map = (Map<String, List>) ret;
                    List<Number> y = map.keySet().contains(Daqbuf.FIELD_AVERAGE) ? map.get(Daqbuf.FIELD_AVERAGE) : map.get(Daqbuf.FIELD_VALUE);
                    List<Number> count =  map.keySet().contains(Daqbuf.FIELD_COUNT) ? map.get(Daqbuf.FIELD_COUNT) : null;
                    List<Number> x = null;
                    try {
                        x = (List<Number>) domainAxisFuture.get();
                    } catch (Exception e) {
                        showException(e);
                    }
                    if (series.getCount() >= series.getMaxItemCount()) {
                        throw new RuntimeException("Series too big for plotting: " + name);
                    }
                    if ((x != null) && (x.size() == y.size())) {
                        synchronized(plot){
                            try {
                                plot.disableUpdates();
                                if (count!=null){
                                    Convert.removeElements(count, 0, x, y);
                                }
                                series.appendData(x,y);
                            } finally {
                                 plot.reenableUpdates();
                            }
                        }
                    }
                }
                return ret;
            });
        } catch (Exception ex) {
            showException(ex);
        }
        return series;
    }
    
    MatrixPlotJFree addMatrixPlot(boolean binned) {
        MatrixPlotJFree plot = new MatrixPlotJFree();
        if (binned) {
            setDateDomainAxis(plot);
            plot.setZoomListener(zoomListener);
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

                    if ((chartX < xyplot.getDomainAxis().getRange().getLowerBound())
                            || (chartX > xyplot.getDomainAxis().getRange().getUpperBound())
                            || (chartY < xyplot.getRangeAxis().getRange().getLowerBound())
                            || (chartY > xyplot.getRangeAxis().getRange().getUpperBound())) {
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
        setMatrixPlotPopupMenu(plot, binned);
        addPlot(plot);
        return plot;
    }
    
    MatrixPlotTable addMatrixTablePlot(boolean binned) {
        MatrixPlotTable plot = new MatrixPlotTable();
        addPlot(plot);
        return plot;
    }
    
    void setMatrixPlotPopupMenu(MatrixPlotJFree plot, boolean binned){
        
        plot.addPopupMenuItem(null);
        JMenuItem menuUnbin = new JMenuItem("Unbinned");
        menuUnbin.addActionListener((ActionEvent e) -> {
            unbin(plot);
        });
        plot.addPopupMenuItem(menuUnbin);
        menuUnbin.setVisible(binned);
        
        JMenuItem menuRequery = new JMenuItem("Requery");
        menuRequery.addActionListener((ActionEvent e) -> {
            requery(plot);                        
        });
        plot.addPopupMenuItem(menuRequery);    
    }
    
    boolean isBinnedPlot(PlotBase plot){
        if (plot!=null){
            if (plot instanceof LinePlotJFree linePlotJFree){
                if (linePlotJFree.getStyle() == LinePlot.Style.ErrorY){
                    XYPlot xyplot =  linePlotJFree.getChart().getXYPlot();
                    if (xyplot.getDomainAxis() instanceof DateAxis){
                        return true;
                    }
                }
            } else if (plot instanceof MatrixPlotJFree matrixPlotJFree){
                 XYPlot xyplot =  matrixPlotJFree.getChart().getXYPlot();
                 if (xyplot.getDomainAxis() instanceof DateAxis){
                     return true;
                 }
            } 
        }
        return false;
    }

    void onMatrixSeriesClicked(MatrixPlotJFree plot, MatrixPlotSeries series, double timestamp, int index) {
        try {
            boolean binned = (series instanceof MatrixPlotBinnedSeries);
            double time = series.getX()[index][0];
            String timestr = Time.getTimeString(time, false, false);
            String name = series.getName() + " " + timestr + " [" + index + "]";
            LinePlotJFree lplot = new LinePlotJFree();
            LinePlotSeries lseries;
            if (binned) {
                lplot.setStyle(LinePlot.Style.ErrorY);
                lplot.setErrorRangeAlpha(ERROR_RANGE_ALPHA);
                lseries = new LinePlotErrorSeries(name, Color.RED);
            } else {
                lseries = new LinePlotSeries(name, Color.RED);
            }
            lplot.addSeries(lseries);
            lplot.setTitle("");
            lplot.setLegendVisible(true);

            synchronized(plot){
                try {
                    plot.disableUpdates();
                    if (binned) {
                        double[] min = ((MatrixPlotBinnedSeries) series).min[index];
                        double[] max = ((MatrixPlotBinnedSeries) series).max[index];
                        double[] average = ((MatrixPlotBinnedSeries) series).average[index];
                        double[] indexes = Arr.indexesDouble(average.length);
                        ((LinePlotErrorSeries) lseries).appendData(indexes, average, min, max);
                        plot.getChartPanel().restoreAutoBounds();
                    } else {
                        double[] row = Convert.transpose(series.getData())[index];
                        lseries.setData(row);
                    }
                } finally {
                    plot.reenableUpdates();
                }
            }
            showDialog(series.getName(), new Dimension(800, 600), lplot);
        } catch (Exception ex) {
            showException((Exception) ex);
        }

    }

    MatrixPlotSeries addMatrixSeries(MatrixPlotBase plot, SeriesInfo si) {
        return addMatrixSeries(plot, si.name, si.backend, si.start, si.end, si.colormap);
    }

    MatrixPlotSeries addMatrixSeries(MatrixPlotBase plot, String name, String backend, String start, String end, Colormap colormap) {
        List value = new ArrayList();
        List<Long> timestamp = new ArrayList<>();
        long maxSize = getMaxSeriesSize();
        MatrixPlotSeries series = new MatrixPlotSeries(name);
        if (colormap != null) {
            plot.setColormap(colormap);
        }        
        plot.getAxis(AxisId.X).setAutoRange();
        plot.addSeries(series);                
        plotSeries.add(series);

        try {
            daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, new QueryListener() {
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {
                    if ((value.size() > 0) && (((List) value.get(0)).size() * value.size()) > maxSize) {
                        throw new RuntimeException("Series too big for plotting: " + name);
                    }
                    value.addAll(values);
                    timestamp.addAll(timestamps);
                }
            }).handle((ret, ex) -> {
                if (ex != null) {
                    showException((Exception) ex);
                };
                if (value.size() > 0) {
                    double[][] data = (double[][]) Convert.toPrimitiveArray(value, Double.class);
                                        
                    synchronized(plot){
                        try{
                            plot.disableUpdates();
                            series.setData(Convert.transpose(data));
                        } finally {
                             plot.reenableUpdates();
                        }                    
                    }
                }
                if (plot instanceof MatrixPlotJFree matrixPlotJFree){
                     matrixPlotJFree.getChartPanel().restoreAutoDomainBounds(); //Needed because changed domain axis to time       
                }
                return ret;
            });
        } catch (Exception ex) {
            showException((Exception) ex);
        }   
        return series;
    }

    static class MatrixPlotBinnedSeries extends MatrixPlotSeries {

        MatrixPlotBinnedSeries(String name) {
            super(name);
        }
        double[][] average;
        double[][] min;
        double[][] max;
    }

    void setMatrixBinnedSeries(MatrixPlotBase plot, MatrixPlotBinnedSeries series, List<List> average, List<List> min, List<List> max, List<Long> t1, List<Long> t2) {
        int bins = average.size();
        if (bins > 0) {
            double[] timestamps = new double[bins];
            for (int j = 0; j < bins; j++) {
                timestamps[j] = (t1.get(j) + t2.get(j)) / 2.0;
            }                        
            double maxTime = timestamps[timestamps.length - 1];
            double minTime = timestamps[0];
            synchronized(plot){
                try{
                    plot.disableUpdates();
                    series.average = (double[][]) Convert.toPrimitiveArray(average, Double.class);
                    series.min = (double[][]) Convert.toPrimitiveArray(min, Double.class);
                    series.max = (double[][]) Convert.toPrimitiveArray(max, Double.class);
                    series.setNumberOfBinsX(series.average.length);
                    series.setNumberOfBinsY(series.average[0].length);
                    if (!plot.getAxis(AxisId.X).isAutoRange() && (queryRange!=null)){
                        plot.getAxis(AxisId.X).setRange(queryRange.min, queryRange.max);
                        series.setRangeX(queryRange.min, queryRange.max);
                    } else {
                        plot.getAxis(AxisId.X).setRange(minTime, maxTime);                        
                        series.setRangeX(minTime, maxTime);
                    }                                        
                    series.setRangeY(0, series.average[0].length - 1);
                    series.setData(Convert.transpose(series.average));                    
                } finally {
                     plot.reenableUpdates();
                }
            }
        }        
        if (plot instanceof MatrixPlotJFree matrixPlotJFree){
            if (!plot.getAxis(AxisId.X).isAutoRange() && (queryRange!=null)){
                matrixPlotJFree.zoomDomain(new Range(queryRange.min, queryRange.max));
            } else{
                matrixPlotJFree.getChartPanel().restoreAutoDomainBounds(); //Needed because changed domain axis to time       
            }                        
        }
    }

    MatrixPlotBinnedSeries addMatrixSeriesBinned(MatrixPlotBase plot, SeriesInfo si) {
        return addMatrixSeriesBinned(plot, si.name, si.backend, si.start, si.end, si.bins, si.colormap);
    }

    MatrixPlotBinnedSeries addMatrixSeriesBinned(MatrixPlotBase plot, String name, String backend, String start, String end, int bins, Colormap colormap) {
        long maxSize = getMaxSeriesSize();
        MatrixPlotBinnedSeries series = new MatrixPlotBinnedSeries(name);
        if (colormap != null) {
            plot.setColormap(colormap);
        }
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
                        plot.disableUpdates();
                    } finally{
                        plot.reenableUpdates();
                    }
                }
                return ret;
            });
             */

            List value = new ArrayList();
            List<Long> timestamp = new ArrayList<>();
            daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, new QueryListener() {
                public void onMessage(Query query, List values, List<Long> ids, List<Long> timestamps) {
                    if ((value.size() > 0) && (((List) value.get(0)).size() * value.size()) > maxSize) {
                        throw new RuntimeException("Series too big for plotting: " + name);
                    }
                    value.addAll(values);
                    timestamp.addAll(timestamps);
                }
            }/*,bins*/).handle((ret, ex) -> {
                if (ex != null) {
                    //showException((Exception)ex);
                };                
                //List<List> average = value;
                List<List> average = new ArrayList<>();
                List<List> min = new ArrayList<>();
                List<List> max = new ArrayList<>();
                List<Long> t1 = new ArrayList<>();
                List<Long> t2 = new ArrayList<>();
                long s = Time.getTimestamp(start);
                long e = Time.getTimestamp(end);  
                double w = ((double)e-s)/bins;
                for (int i=0; i<bins; i++){
                    average.add(((List)value.get(i % value.size())));
                    t1.add(s+(long)(i * w));
                    t2.add(s+(long)((i+1) * w));
                }
                double off = 20.0;
                for (List l : average) {
                    List lmin = new ArrayList();
                    List lmax = new ArrayList();
                    for (double n : (double[])Convert.toPrimitiveArray(l, Double.class)) {
                        lmin.add(n - off);
                        lmax.add(n + off);
                    }
                    min.add(lmin);
                    max.add(lmax);
                }
                setMatrixBinnedSeries(plot, series, average, min, max, t1, t2);

                return ret;
            });
        } catch (Exception ex) {
            showException((Exception) ex);
        }

        return series;
    }

    SlicePlotDefault addSlicePlot() {
        SlicePlotDefault plot = new SlicePlotDefault(new MatrixPlotRenderer()) {
            @Override
            protected String getPageSubtitle(SlicePlotSeries series, int page) {
                return " - " + page;
            }
        };

        addPlot(plot, true);
        return plot;
    }

    //TODO
    int[] getShape(String name, String backend, String start, String end) {
        return new int[]{200, 100, 10};
    }

    //TODO
    double[][] getFrame(String name, String backend, String start, int index, int[] shape) {
        try {
            double[][] data = new double[shape[1]][shape[0]];
            for (int j = 0; j < data.length; j++) {
                for (int k = 0; k < data[0].length; k++) {
                    data[j][k] = index * 10000 + j * 100 + k;
                }
            }
            return data;
        } catch (Exception ex) {
            return null;
        }
    }

    SlicePlotSeries addImageSeries(SlicePlotDefault plot, SeriesInfo si) {
        return addImageSeries((SlicePlotDefault) plot, si.name, si.backend, si.start, si.end, si.colormap);
    }

    SlicePlotSeries addImageSeries(SlicePlotDefault plot, String name, String backend, String start, String end, Colormap colormap) {
        SlicePlotSeries series = new SlicePlotSeries(name);
        if (colormap != null) {
            plot.setColormap(colormap);
        }
        plotSeries.add(series);
        plot.addSeries(series);
        plot.setTitle(name);
        plot.setUpdatesEnabled(true);

        final int[] shape = getShape(name, backend, start, end);

        series.setNumberOfBinsX(shape[0]);
        series.setNumberOfBinsY(shape[1]);
        series.setRangeX(0, shape[0] - 1);
        series.setRangeY(0, shape[1] - 1);
        series.setNumberOfBinsZ(shape[2]);
        series.setRangeZ(0, shape[2] - 1);

        series.setListener((SlicePlotSeries s, int index) -> {
            try {
                double[][] page_data = getFrame(name, backend, start, index, shape);
                s.setData(page_data);
            } catch (Exception ex) {
                s.clear();
            }
        });

        try {
            daqbuf.startQuery(name + Daqbuf.BACKEND_SEPARATOR + backend, start, end, new QueryRecordListener() {
                public void onRecord(Query query, Object value, Long id, Long timestamp) {
                    System.out.println(timestamp + " - " + value);
                }
            });
        } catch (Exception ex) {
            showException(ex);
        }
        return series;
    }

    String expandTime(String text){
        text = text.trim();
        if (Pattern.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:$", text)){
            return text + "00";
        }
        if (Pattern.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:$", text)){
            return text + "00";
        }
        if (Pattern.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}$", text)){
            return text + ":00";
        }
        if (Pattern.matches("^\\d{4}-\\d{2}-\\d{2}", text)){
            return text + " 00:00";
        }
        if (Pattern.matches("^\\d{4}-\\d{2}", text)){
            return text + "-01 00:00";
        }
        if (Pattern.matches("^\\d{4}-\\d{2}-", text)){
            return text + "01 00:00";
        }
        if (Pattern.matches("^\\d{4}", text)){
            return text + "-01-01 00:00";
        }
        if (Pattern.matches("^\\d{4}-", text)){
            return text + "01-01 00:00";
        }
        return text;
    }

    class SeriesInfo {

        final String start;
        final String end;
        final Integer bins;
        final String name;
        final String backend;
        //final boolean shared;
        final boolean create;
        final boolean table;
        final int axis;
        final Color color;
        final Colormap colormap;
        final List<Integer> shape;
        final String type;
        final SeriesInfo parent;

        SeriesInfo(Vector info) {
            parent = null;
            start = expandTime(textFrom.getText());
            end = expandTime(textTo.getText());
            bins = checkBins.isSelected() ? (Integer) spinnerBins.getValue() : null;
            name = getChannelAlias(((String) info.get(1)).trim());
            backend = info.get(2).toString();
            create = !PLOT_SAME.equals(info.get(4));
            table = PLOT_TABLE.equals(info.get(4));
            
            axis = switch (Str.toString(info.get(5))) {
                case "X" -> AXIS_X;
                case "Y1", "1" -> AXIS_1;
                case "Y2", "2" -> AXIS_2;
                default -> AXIS_NONE;
            };
            
            Color cl;
            Colormap cm;
            try {
                cm = Colormap.valueOf((String) info.get(6));
                cl = null;
            } catch (Exception ex) {
                cl = SwingUtils.readableStringToColor((String) info.get(6));
                cm = null;
            }
            colormap = cm;
            color = cl;
            List<Integer> s = new ArrayList<>();            
            String t=null;
            try {
                String sh = info.get(3).toString();                
                if (sh.contains("[")){
                    t =  sh.substring(0,sh.lastIndexOf("["));
                    sh = sh.substring(sh.lastIndexOf("["));                    
                } else {
                    t = sh.trim();
                }
                s = (List<Integer>) EncoderJson.decode(sh, List.class);
            } catch (Exception ex) {
            }
            type = t;
            shape = s;
        }

        SeriesInfo(SeriesInfo parent, String start, String end, Integer bins) {
            this.parent = parent;
            this.start = start;
            this.end = end;
            this.bins = bins;
            name = parent.name;
            backend = parent.backend;
            table = parent.table;
            create = parent.create;
            axis = parent.axis;
            color = parent.color;
            colormap = parent.colormap;
            shape = parent.shape;
            type = parent.type;
        }

        int getRank() {
            return shape.size();
        }

    }
    
    void checkTimeRange() throws IOException{
        queryRange = null;
        if (textFrom.getText().isBlank() && textTo.getText().isBlank()){
            comboTime.setSelectedIndex(1);
        }
        
        String start = expandTime(textFrom.getText());
        String to = expandTime(textTo.getText());
        textFrom.setText(start);
        textTo.setText(to);                
        
        if (Time.compare(to, start)<=0){
            throw new IOException ("Invalid time range");
        }
        
        queryRange = new Range(Time.getTimestamp(start), Time.getTimestamp(to));
    }

    public void plotQuery() throws Exception {
        reset();

        if (modelSeries.getRowCount() == 0) {
            return;
        }
        numPlots = 0;
        started = true;
        checkTimeRange();                    
        update();       

        Plot currentPlot = null;
        plotInfo.clear();
        Vector vector = modelSeries.getDataVector();
        Vector[] rows = (Vector[]) vector.toArray(new Vector[0]);
        CompletableFuture cf = null;

        for (int i = 0; i < rows.length; i++) {
            Vector info = rows[i];
            Vector next = (i < rows.length-1) ? rows[i+1] : null;
            if (info.get(0).equals(true)) {
                SeriesInfo seriesInfo = new SeriesInfo(info);
                Plot plot = null;
                PlotSeries series = null;
                switch (seriesInfo.getRank()) {
                    case 0:
                        if (!seriesInfo.create && (currentPlot != null) && (currentPlot instanceof LinePlotBase)) {
                            plot = currentPlot;
                        } else {
                            cf = null;
                            if (seriesInfo.axis < 0) {
                                plot = addCorrelationPlot(seriesInfo.bins != null);
                            } else {
                                if (seriesInfo.table){
                                    plot = addLineTablePlot(seriesInfo.bins != null);
                                } else {
                                    plot = addLinePlot(seriesInfo.bins != null);
                                }
                            }
                            currentPlot = plot;
                        }
                        if (seriesInfo.axis < 0) {
                            cf = setDomainAxis((LinePlotJFree) plot, seriesInfo);
                        } else if (cf != null) {
                            series = addCorrelationSeries((LinePlotBase) plot, seriesInfo, cf);
                        } else if (seriesInfo.bins == null) {
                            series = addLineSeries((LinePlotBase) plot, seriesInfo);
                        } else {
                            series = addBinnedSeries((LinePlotBase) plot, seriesInfo);
                        }
                        break;
                    case 1:
                        if (seriesInfo.bins == null) {
                            if (seriesInfo.table){
                                plot = addMatrixTablePlot(true);
                            } else {
                                plot = addMatrixPlot(false);
                            }
                            series = addMatrixSeries((MatrixPlotBase) plot, seriesInfo);
                        } else {
                            if (seriesInfo.table){
                                plot = addMatrixTablePlot(true);
                            } else {
                                plot = addMatrixPlot(true);
                            }
                            series = addMatrixSeriesBinned((MatrixPlotBase) plot, seriesInfo);
                        }
                        break;
                    case 2:
                        plot = addSlicePlot();
                        series = addImageSeries((SlicePlotDefault) plot, seriesInfo);
                }
                if ((plot != null) && (series != null)) {
                    plotInfo.get(plot).add(seriesInfo);
                }
            }
        }
        if ((requestedProperties!=null) && (requestedProperties.size()>0)){
            applyPlotPreferences(requestedProperties);           
            modelSeries.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    requestedProperties=null;
                    modelSeries.removeTableModelListener(this);
                }
            });
        }
    }

    void updateCapLength(LinePlotJFree plot) {
        for (int i = 0; i < plot.getChart().getXYPlot().getRendererCount(); i++) {
            updateCapLength(plot,i);
        }
    }
    
    void updateCapLength(LinePlotJFree plot, int yxis) {
        try {
            DateAxis axis = (DateAxis) plot.getChart().getXYPlot().getDomainAxis();            
            XYErrorRenderer renderer = (XYErrorRenderer) plot.getChart().getXYPlot().getRenderer(yxis);
            double capLength = renderer.getCapLength();
            try {
                int bins = plot.getChart().getXYPlot().getDataset().getItemCount(0);
                double start = plot.getChart().getXYPlot().getDataset().getXValue(0, 0);
                double end = plot.getChart().getXYPlot().getDataset().getXValue(0, bins - 1);
                double capLenghtMs = (end - start) / bins;
                double domainLenghtPixels = plot.getChartPanel().getScreenDataArea().getWidth();
                if (domainLenghtPixels>0){
                    domainLenghtPixels /= plot.getChartPanel().getScaleX();
                    double domainLengthMs = axis.getRange().getLength();
                    capLength = (capLenghtMs * domainLenghtPixels) / domainLengthMs;
                    if (capLength>1000){
                        throw new Exception("Invalid cap size");
                    }
                    capLength =Math.max(capLength, 1);
                }
            } catch (Exception ex) {
                capLength = 4.0;
            }
            double change = (renderer.getCapLength()<=0) ? 0.0 : capLength/renderer.getCapLength();
            if ((change<0.99) | (change>1.01)){
                renderer.setCapLength(capLength);
            }
        } catch (Exception ex) {
        }

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
        plotProperties.clear();
        plotSeries.clear();
        initializePlots();
    }

    @Override
    protected void onClosed() {
    }
    
    volatile boolean dumping = false;
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
        if (channels.isEmpty()) {
            throw new IOException("No channel selected");
        }
        final Integer bins = checkBins.isSelected() ? (Integer) spinnerBins.getValue() : null;
        checkTimeRange(); 
        final String start = textFrom.getText();
        final String end = textTo.getText();

        String finalFilename = IO.getExtension(filename).isEmpty() ? filename + ".h5" : filename;
        JDialog splash = SwingUtils.showSplash(this, "Save", new Dimension(400, 200), "Saving data to " + finalFilename);
        dumping = true;
        update();
        daqbuf.startSaveQuery(finalFilename, channels.toArray(new String[0]), start, end, bins).handle((Object ret, Object ex) -> {
            splash.setVisible(false);
            if (ex != null) {
                showException((Exception) ex);
            } else {
                if (SwingUtils.showOption(this, "Save", "Success saving data to " + finalFilename + ".\nDo you want to open the file?", OptionType.YesNo) == OptionResult.Yes) {
                    //DataPanel.createDialog(this, finalFilename, null, null);
                    ch.psi.pshell.dataviewer.App.create(this, null, false, new File(finalFilename), null, "h5");
                }
            }
            dumping = false;
            update();
            return ret;
        });
    }
    
    void saveQuery() throws IOException {
        try {
            String dataHome = Setup.getDataPath();
            JFileChooser chooser = new JFileChooser(dataHome);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("HDF5 files", "h5");
            chooser.setFileFilter(filter);
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setDialogTitle("Dump Data");            
            chooser.setCurrentDirectory(new File(Context.hasDataManager() ? Setup.expandPath("{data}") : Sys.getUserHome()));

            int rVal = chooser.showSaveDialog(this);
            if (rVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (file!=null) {
                    saveQuery(file.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            showException(ex);
        } catch (Throwable t) {
           t.printStackTrace();
        }
    }

    void searchChannels() throws Exception {
        if((channelSearchDialog!=null) && channelSearchDialog.isShowing()){
            channelSearchDialog.setLocationRelativeTo(this);
            channelSearchDialog.requestFocus();
            return;
        }
        selector.setHistorySize(0);
        selector.setListMode(ChannelSelector.ListMode.Popup);                                
        ChannelSelector selector = new ChannelSelector();
        selector.setHistorySize(0);
        selector.setListMode(ChannelSelector.ListMode.Visible);
        selector.setMultipleSelection(true);
               
        JPanel panel = new JPanel(new BorderLayout());                    
        ((BorderLayout) panel.getLayout()).setHgap(10);       
        
        JPanel p1 = new JPanel(new FlowLayout());
        //((BorderLayout) p1.getLayout()).setHgap(5);       
        JComboBox comboBackend = new JComboBox();
        DefaultComboBoxModel modelBackend  = (DefaultComboBoxModel) comboBackend.getModel();
        modelBackend.addAll(Arrays.asList(daqbuf.getAvailableBackends()));                
        modelBackend.setSelectedItem(daqbuf.getAvailableDefaultBackend());
        comboBackend.addActionListener((e)->{
            selector.update();
        });
        p1.add(new JLabel("Backend:"));
        selector.setBorder(new EmptyBorder(0, 8, 0, 8));
        p1.add(comboBackend);
        panel.add(p1, BorderLayout.NORTH);
        panel.add(selector, BorderLayout.CENTER);
        JPanel p2 = new JPanel(new FlowLayout());
        JButton buttonCancel = new JButton("Cancel");
        JButton buttonOk = new JButton("Ok");
        p2.add(buttonCancel);
        p2.add(buttonOk);
        panel.add(p2, BorderLayout.SOUTH);
               
        Runnable configureSelector = () -> selector.configure(ChannelSelector.Type.Daqbuf, daqbuf.getUrl(), (String) comboBackend.getSelectedItem(), 1000, CHANNEL_SEARCH_EXCLUDES);
        modelBackend.addListDataListener(comboTime);
        comboBackend.addActionListener((e) -> {
            configureSelector.run();
        }); 
        configureSelector.run();
        
        channelSearchDialog  = SwingUtils.showDialog(this, "Channel Search", new Dimension(350, 500), panel);
        SwingUtils.centerComponent(null, channelSearchDialog);        
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                channelSearchDialog.dispose();
            }
        });                

        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try{
                    
                    List<String> channels = selector.getSelection();
                    String backend = (String) modelBackend.getSelectedItem();
                    if ((backend!=null) && !channels.isEmpty()){
                        for (String ch : channels){
                            if (!isChannelPresent(ch, backend)){
                                Object[] data = getEmptyRow();
                                data[1] = ch;
                                data[2] = backend;
                                modelSeries.addRow(data);
                                updateShape(modelSeries.getRowCount()-1);
                            }
                        }
                        modelSeries.fireTableDataChanged();                        
                        update();                    
                    }                    
                } catch (Exception ex){                    
                    showException(ex);
                }
                channelSearchDialog.dispose();
            }
        });                  
    }
    
    public boolean isChannelPresent(String channel, String backend) {
        for (int row = 0; row < modelSeries.getRowCount(); row++) {
            if (channel.equals(modelSeries.getValueAt(row, 1)) && backend.equals(modelSeries.getValueAt(row, 2))){
                return true; 
            }
        }
        return false; 
    }    
    
    void plotData() throws Exception {
        if (tableSeries.isEditing()) {
            tableSeries.getCellEditor().stopCellEditing();
        }
        if (modelSeries.getRowCount() > 0) {
            plotQuery();
            tabPane.setSelectedComponent(panelPlots);
        }
    }
    
    String getDefaultFolder() {
        return defaultFolder.getAbsolutePath();
    }

    void saveConfig() throws Exception  {
        JFileChooser chooser = new JFileChooser(getDefaultFolder());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Config files", FILE_EXTENSION);
        chooser.setFileFilter(filter);
        if (file != null) {
            try {
                chooser.setSelectedFile(file);
            } catch (Exception ex) {
            }
        }                
        chooser.setDialogTitle("Save Config");
        int rVal = chooser.showSaveDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            String filename = chooser.getSelectedFile().getAbsolutePath();
            if (IO.getExtension(filename).isEmpty()) {
                filename += ".arc";
            }
            saveConfig(new File(filename)); 
        }
    }

    void openConfig() throws Exception  {
        JFileChooser chooser = new JFileChooser(getDefaultFolder());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Config files", FILE_EXTENSION, "dbuf");
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Open Config");
        
        int rVal = chooser.showOpenDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            String filename = chooser.getSelectedFile().getAbsolutePath();
            File file = new File(filename);
            if (!file.exists()){
                if (IO.getExtension(filename).isEmpty()) {
                    filename += "." + FILE_EXTENSION;
                }   
                file = new File(filename);
            }
            if (file.exists()){
                openConfig(file);
            }
        }
    }

    void saveConfig(File file) throws Exception {
        Map<String, Object> data = new HashMap();
        data.put("series", modelSeries.getDataVector());
        if (comboTime.getSelectedIndex()==0){
            data.put("from", textFrom.getText());
            data.put("to", textTo.getText());
            data.put("range", null);
        } else {
            data.put("from", null);
            data.put("to", null);
            data.put("range", comboTime.getSelectedItem());
        }
        data.put("autorange", ckAutoRange.isSelected());
        data.put("binned", checkBins.isSelected());
        data.put("bins",  spinnerBins.getValue() );            
        data.put("maxsize", getMaxSeriesSize());
        data.put("plots", plotProperties);               
        String json = EncoderJson.encode(data, true);
        Files.write(file.toPath(), json.getBytes());
        this.file = file;
    }
    void openConfig(File file) throws Exception  {
       String json = new String(Files.readAllBytes(file.toPath()));
       openConfig(json);
    }
    
    void openConfig(String json) throws Exception  {        
        Map<String, Object> data = (Map<String, Object>) EncoderJson.decode(json, Map.class);
        String from = (String) data.getOrDefault("from", null);
        String to = (String) data.getOrDefault("to", null);
        String range = (String) data.getOrDefault("range", null);
        Boolean binned = (Boolean) data.getOrDefault("binned", null);        
        Integer bins = (Integer) data.getOrDefault("bins", null);
        Integer maxsize = (Integer) data.getOrDefault("maxsize", null);
        List<List> series = (List<List>) data.getOrDefault("series", new ArrayList());
        Boolean autoRange = (Boolean) data.getOrDefault("autorange", null);
        List<Map> plotProperties = (List<Map>) data.getOrDefault("plots", new ArrayList());
        
        requestedProperties = plotProperties;
        openConfig(series, from, to, range, binned, bins, maxsize, autoRange);        
        this.file = file;
    }

    void openConfig(List<List> series, String from, String to, String range, Boolean binned, Integer bins, Integer maxsize, Boolean autoRange) {
        clear();
        textFrom.setText((from==null) ? "" : from);
        textTo.setText((to==null) ? "" : to);
        if (range!=null){
            comboTime.setSelectedItem(range);
        }
        if (bins!=null){
            spinnerBins.setValue(bins);
        }
        if (maxsize!=null){
            setMaxSeriesSize(maxsize);
        }
        if (binned!=null){
            checkBins.setSelected(binned);
        }
        ckAutoRange.setSelected(Boolean.TRUE.equals(autoRange));
        checkBinsActionPerformed(null);
        Object[][]dataVector =  Convert.to2dArray(Convert.toArray(series));        
        modelSeries.setDataVector(dataVector, SwingUtils.getTableColumnNames(tableSeries));
        initializeTable();

    }
    
    public File getFileArg() {
        File file = App.getFileArg();
        if (((file==null) || !file.isFile()) && (Setup.getFileArg()!=null)){
            File f = new File(Setup.expandPath(Paths.get(getDefaultFolder(), Setup.getFileArg()).toString()));               
            if (f.exists()){
                file = f;
            }
        }
        return file;
    }    

    void openArgs() {
        File file = getFileArg();
        String config = Setup.getConfigArg();
        List<CompletableFuture> futures = new ArrayList<>();
        if (file!=null){ 
           try{
               openConfig(file);
           } catch (Exception ex) {               
               showException(ex);
           }                
        } else if (config!=null){        
           try{
               openConfig(config);
           } catch (Exception ex) {
               showException(ex);
           }                
        }
        else {
            String from = Options.FROM.getString(null);
            String to = Options.TO.getString(null);
            String range =Options.RANGE.getString(null);
            Integer bins = Options.BINS.getInt(DEFAULT_BINS);
            bins = (bins<1) ? null : bins;
            Boolean binned  = (bins != null);;
            Integer maxsize = Options.MAXSIZE.getInt(null);
            Boolean autoRange = Options.AUTO_RANGE.getBool(false);
            openConfig(new ArrayList<List>(), from, to, range, binned, bins, maxsize, autoRange);
            
            if (Options.CHANNEL.hasValue()){                
                BiFunction channelTask = (ret, ex) ->{
                    if (ex==null){
                        for (String s: Options.CHANNEL.getStringList()){
                            String name = daqbuf.getChannelName(s);
                            String backend = daqbuf.getChannelBackend(s);
                            Object[]row = getEmptyRow();
                            row[1] = name;
                            row[2] = backend;
                            modelSeries.addRow(row);
                            futures.add(updateShape(modelSeries.getRowCount()-1));
                        }
                        checkPlotArg(futures);
                    }
                    update();
                    return ret;
                };

                boolean backendsDefined = true;                
                for (String s: Options.CHANNEL.getStringList()){
                    if (!daqbuf.isBackendDefined(daqbuf.getChannelBackend(s))){
                        backendsDefined=false;
                    }
                }
                                
                if (backendsDefined){ //(daqbuf.isBackendDefined()){
                    channelTask.apply(null, null);                    
                } else {                    
                    daqbuf.getInitialization().handle(channelTask);                    
                }
                return;
            }
            update();
        } 
        checkPlotArg(futures);
    }
    
    void checkPlotArg(List<CompletableFuture> futures){
        if (Setup.getStartArg()){ 
            new Thread(()->{
                try {
                    for (CompletableFuture cf : futures){
                        cf.get();
                    }
                    SwingUtilities.invokeLater(()->{
                        try {
                            plotData();
                        } catch (Exception ex) {
                            showException(ex);
                        }
                    });
                } catch (Exception ex) {
                    showException(ex);
                }
            }).start();
        }        
    }

    Object[] getEmptyRow(){
        return new Object[]{Boolean.TRUE, "", daqbuf.getAvailableDefaultBackend(), "", PLOT_NEW, "Y1"};
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
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        textFrom = new javax.swing.JTextField();
        textTo = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        comboTime = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        ckAutoRange = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        checkBins = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        spinnerBins = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        spinnerSize = new javax.swing.JSpinner();
        toolBar = new javax.swing.JToolBar();
        buttonNew = new javax.swing.JButton();
        buttonOpen = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        buttonRowDelete = new javax.swing.JButton();
        buttonRowDown = new javax.swing.JButton();
        buttonRowUp = new javax.swing.JButton();
        buttonRowInsert = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        buttonSearchChannels = new javax.swing.JButton();
        buttonLockZooms = new javax.swing.JToggleButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        buttonDumpData = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        buttonPlotData = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        labelUser = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
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
                "Enabled", "Name", "Backend", "Format", "Plot", "Axis", "Color"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, true, false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
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

        javax.swing.GroupLayout panelSerieLayout = new javax.swing.GroupLayout(panelSerie);
        panelSerie.setLayout(panelSerieLayout);
        panelSerieLayout.setHorizontalGroup(
            panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSerieLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelSerieLayout.setVerticalGroup(
            panelSerieLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSerieLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
                .addContainerGap())
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

        ckAutoRange.setText("Auto Range");
        ckAutoRange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ckAutoRangeActionPerformed(evt);
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
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(jLabel9))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textTo, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(comboTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ckAutoRange))
                .addContainerGap())
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addGap(19, 19, 19)
                    .addComponent(jLabel6)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel2, jLabel3});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(comboTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel9)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(textTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(ckAutoRange))
                .addContainerGap(9, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(15, 15, 15)))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {comboTime, textFrom, textTo});

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Data"));

        checkBins.setSelected(true);
        checkBins.setText("Binned ");
        checkBins.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBinsActionPerformed(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText("Bins:");

        spinnerBins.setModel(new javax.swing.SpinnerNumberModel(200, 1, 10000, 10));

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel7.setText("Max size (K): ");

        spinnerSize.setModel(new javax.swing.SpinnerNumberModel(250, 50, 2500, 50));
        spinnerSize.setEnabled(false);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkBins)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerBins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {spinnerBins, spinnerSize});

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

        buttonNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/New.png"))); // NOI18N
        buttonNew.setToolTipText("Clear config");
        buttonNew.setFocusable(false);
        buttonNew.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonNew.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNewActionPerformed(evt);
            }
        });
        toolBar.add(buttonNew);

        buttonOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Open.png"))); // NOI18N
        buttonOpen.setToolTipText("Open config");
        buttonOpen.setFocusable(false);
        buttonOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOpenActionPerformed(evt);
            }
        });
        toolBar.add(buttonOpen);

        buttonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Save.png"))); // NOI18N
        buttonSave.setToolTipText("Save config");
        buttonSave.setFocusable(false);
        buttonSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });
        toolBar.add(buttonSave);

        jSeparator1.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator1.setPreferredSize(new java.awt.Dimension(20, 0));
        jSeparator1.setRequestFocusEnabled(false);
        toolBar.add(jSeparator1);

        buttonRowDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Abort.png"))); // NOI18N
        buttonRowDelete.setToolTipText("Remove row");
        buttonRowDelete.setFocusable(false);
        buttonRowDelete.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRowDelete.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonRowDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });
        toolBar.add(buttonRowDelete);

        buttonRowDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/arrows/Down.png"))); // NOI18N
        buttonRowDown.setToolTipText("Move row down");
        buttonRowDown.setFocusable(false);
        buttonRowDown.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRowDown.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonRowDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDownActionPerformed(evt);
            }
        });
        toolBar.add(buttonRowDown);

        buttonRowUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/arrows/Up.png"))); // NOI18N
        buttonRowUp.setToolTipText("Move row up");
        buttonRowUp.setFocusable(false);
        buttonRowUp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRowUp.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonRowUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpActionPerformed(evt);
            }
        });
        toolBar.add(buttonRowUp);

        buttonRowInsert.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Insert.png"))); // NOI18N
        buttonRowInsert.setToolTipText("Add new row");
        buttonRowInsert.setFocusable(false);
        buttonRowInsert.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRowInsert.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonRowInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });
        toolBar.add(buttonRowInsert);

        jSeparator3.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator3.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(jSeparator3);

        buttonSearchChannels.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Search.png"))); // NOI18N
        buttonSearchChannels.setToolTipText("Multiple channel search");
        buttonSearchChannels.setFocusable(false);
        buttonSearchChannels.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSearchChannels.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonSearchChannels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSearchChannelsActionPerformed(evt);
            }
        });
        toolBar.add(buttonSearchChannels);

        buttonLockZooms.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Chain.png"))); // NOI18N
        buttonLockZooms.setToolTipText("Lock plot zoom levels");
        buttonLockZooms.setFocusable(false);
        buttonLockZooms.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonLockZooms.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonLockZooms.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLockZoomsActionPerformed(evt);
            }
        });
        toolBar.add(buttonLockZooms);

        jSeparator4.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator4.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(jSeparator4);

        buttonDumpData.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Rec.png"))); // NOI18N
        buttonDumpData.setToolTipText("Dump data to file");
        buttonDumpData.setFocusable(false);
        buttonDumpData.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonDumpData.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonDumpData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDumpDataActionPerformed(evt);
            }
        });
        toolBar.add(buttonDumpData);

        jSeparator2.setMaximumSize(new java.awt.Dimension(20, 32767));
        jSeparator2.setPreferredSize(new java.awt.Dimension(20, 0));
        toolBar.add(jSeparator2);

        buttonPlotData.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/psi/pshell/ui/Play.png"))); // NOI18N
        buttonPlotData.setToolTipText("Plot data");
        buttonPlotData.setFocusable(false);
        buttonPlotData.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonPlotData.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonPlotData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPlotDataActionPerformed(evt);
            }
        });
        toolBar.add(buttonPlotData);
        toolBar.add(filler1);
        toolBar.add(labelUser);
        toolBar.add(filler2);

        javax.swing.GroupLayout panelSeriesLayout = new javax.swing.GroupLayout(panelSeries);
        panelSeries.setLayout(panelSeriesLayout);
        panelSeriesLayout.setHorizontalGroup(
            panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelSerie, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panelSeriesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelSeriesLayout.setVerticalGroup(
            panelSeriesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSeriesLayout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(panelSerie, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tabPane.addTab("Config", panelSeries);

        panelPlots.setMinimumSize(new java.awt.Dimension(0, 0));
        panelPlots.setName("panelPlots"); // NOI18N
        panelPlots.setLayout(new java.awt.BorderLayout());

        scrollPane.setPreferredSize(new java.awt.Dimension(356, 303));

        javax.swing.GroupLayout pnGraphsLayout = new javax.swing.GroupLayout(pnGraphs);
        pnGraphs.setLayout(pnGraphsLayout);
        pnGraphsLayout.setHorizontalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 459, Short.MAX_VALUE)
        );
        pnGraphsLayout.setVerticalGroup(
            pnGraphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 359, Short.MAX_VALUE)
        );

        scrollPane.setViewportView(pnGraphs);

        panelPlots.add(scrollPane, java.awt.BorderLayout.CENTER);

        tabPane.addTab("Plots", panelPlots);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(tabPane))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tabPane)
                .addGap(0, 0, 0))
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
        Object[] data = getEmptyRow();
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

    private void checkBinsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBinsActionPerformed
        update();
    }//GEN-LAST:event_checkBinsActionPerformed

    private void comboTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboTimeActionPerformed
        if (updating){
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now;
        LocalDateTime from = null;

        switch (comboTime.getSelectedIndex()) {
            case 0 -> {
                return;
            }
            case 1 -> from = now.minusMinutes(1);
            case 2 -> from = now.minusMinutes(10);
            case 3 -> from = now.minusHours(1);
            case 4 -> from = now.minusHours(12);
            case 5 -> from = now.minusHours(24);
            case 6 -> from = now.minusDays(7);
            case 7 -> {
                LocalDate yesterdayDate = now.toLocalDate().minusDays(1);
                from = LocalDateTime.of(yesterdayDate, LocalTime.MIN);
                to = LocalDateTime.of(yesterdayDate, LocalTime.MAX);
            }
            case 8 -> from = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);
            case 9 -> {
                LocalDate startOfCurrentWeek = now.toLocalDate().with(DayOfWeek.MONDAY);
                LocalDate endOfLastWeek = startOfCurrentWeek.minusDays(1);
                LocalDate startOfLastWeek = endOfLastWeek.minusDays(6);
                from = LocalDateTime.of(startOfLastWeek, LocalTime.MIN);
                to = LocalDateTime.of(endOfLastWeek, LocalTime.MAX);
            }
            case 10 -> from = LocalDateTime.of(now.toLocalDate().with(DayOfWeek.MONDAY), LocalTime.MIN);
            case 11 -> {
                YearMonth previousMonth = YearMonth.from(now.minusMonths(1));
                LocalDate firstDayOfPreviousMonth = previousMonth.atDay(1);
                LocalDate lastDayOfPreviousMonth = previousMonth.atEndOfMonth();
                from = LocalDateTime.of(firstDayOfPreviousMonth, LocalTime.MIN);
                to = LocalDateTime.of(lastDayOfPreviousMonth, LocalTime.MAX);
            }
            case 12 -> from = LocalDateTime.of(YearMonth.from(now).atDay(1), LocalTime.MIN);
        }

        textFrom.setText(from.format(formatter));
        textTo.setText(to.format(formatter));
    }//GEN-LAST:event_comboTimeActionPerformed

    private void buttonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNewActionPerformed
        try{
            clear();
        } catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_buttonNewActionPerformed

    private void buttonOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOpenActionPerformed
        try{
            openConfig();
        } catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_buttonOpenActionPerformed

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        try{
            saveConfig();
        } catch (Exception ex){
            showException(ex);
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonPlotDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPlotDataActionPerformed
        try {
            plotData();
        } catch (Exception ex) {
            showException(ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_buttonPlotDataActionPerformed

    private void buttonDumpDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDumpDataActionPerformed
        try {
            saveQuery();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDumpDataActionPerformed

    private void ckAutoRangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ckAutoRangeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ckAutoRangeActionPerformed

    private void buttonSearchChannelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSearchChannelsActionPerformed
        try {
            searchChannels();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonSearchChannelsActionPerformed

    private void buttonLockZoomsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLockZoomsActionPerformed
        lockZoomLevels=buttonLockZooms.isSelected();
    }//GEN-LAST:event_buttonLockZoomsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDumpData;
    private javax.swing.JToggleButton buttonLockZooms;
    private javax.swing.JButton buttonNew;
    private javax.swing.JButton buttonOpen;
    private javax.swing.JButton buttonPlotData;
    private javax.swing.JButton buttonRowDelete;
    private javax.swing.JButton buttonRowDown;
    private javax.swing.JButton buttonRowInsert;
    private javax.swing.JButton buttonRowUp;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonSearchChannels;
    private javax.swing.JCheckBox checkBins;
    private javax.swing.JCheckBox ckAutoRange;
    private javax.swing.JComboBox<String> comboTime;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JLabel labelUser;
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
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables
}
