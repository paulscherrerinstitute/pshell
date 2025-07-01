package ch.psi.pshell.plot;

import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Time;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Dummy plot, used to manage tables together with plots
 */
public class LinePlotTable extends LinePlotBase {


    JTable table;
    JLabel title;
    DefaultTableModel model;    
    DecimalFormat decimalFormat;
    boolean scientificNotation;
    final DefaultTableCellRenderer doubleRenderer;
    final DefaultTableCellRenderer stringRenderer;
    JPopupMenu popupMenu;    
    final MouseAdapter tableMouseAdapter;
    int selectedDataCol;
    boolean timeString;
     
    public LinePlotTable() {
        super();
        BorderLayout layout = new BorderLayout();
        layout.setVgap(4);
        setLayout(layout);
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);        
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        title = new JLabel();
        title.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        scrollPane.setViewportView(table);
        add(scrollPane);        

        model =newModel();                
        setScientificNotation(false);
        doubleRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Double d) {
                    super.setValue(decimalFormat.format(d)); // Format the Double value
                } else {
                    super.setValue(value); // Default behavior for non-Double values
                }
            }
        };
        doubleRenderer.setHorizontalAlignment(SwingConstants.RIGHT); // Align numbers to the right
        
        stringRenderer = new DefaultTableCellRenderer();
        stringRenderer.setHorizontalAlignment(SwingConstants.RIGHT); // Align strings to the right
                
        table.setModel(model);
        setRenderers();
        
        JCheckBoxMenuItem menuShowTimeStr = new JCheckBoxMenuItem("Show Time as String");
        menuShowTimeStr.addActionListener((ActionEvent e) -> {
            setTimeString(menuShowTimeStr.isSelected());
        });                
        
        JCheckBoxMenuItem menuScientificNotation = new JCheckBoxMenuItem("Scientific Notation");
        menuScientificNotation.addActionListener((ActionEvent e) -> {
            setScientificNotation(menuScientificNotation.isSelected());
        });
        

        tableMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        menuShowTimeStr.setEnabled(timeString);
                        menuShowTimeStr.setSelected(timeString);
                        if (!timeString){
                            if (model.getRowCount()>0){
                                Double v = (Double)model.getValueAt(0, 0);
                                menuShowTimeStr.setEnabled((v>=TIMESTAMP_2000) && (v<=TIMESTAMP_2100));      
                            }                                                               
                        }
                        
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }

        };
        table.addMouseListener(tableMouseAdapter);      
        

        addPopupMenuItem(null);
        addPopupMenuItem(menuScientificNotation);
        addPopupMenuItem(menuShowTimeStr);        
        
        
        JMenuItem menuPlotCol = new JMenuItem("Plot column");
        menuPlotCol.addActionListener((ActionEvent e) -> {
            int col = selectedDataCol;
            if (col>=0){
                double[] y = getColumn(col);
                String columnName = table.getColumnName(col);
                if ((columnName==null) || (columnName.isBlank())){
                    columnName = "Column " + col;
                }
                if (y!=null){
                    double[] x = (col>=0) ? getColumn(0) : null;
                    LinePlotJFree.showDialog(getFrame(), columnName, x, y);
                }
            }
        });                

        JPopupMenu tableDataColPopupMenu = new JPopupMenu();
        tableDataColPopupMenu.add(menuPlotCol);                
        
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(MouseEvent e) {
                try {
                    if (e.isPopupTrigger()) {
                        int c = table.columnAtPoint(e.getPoint());
                        if (c >= 0 && c < table.getColumnCount()) {
                            selectedDataCol = c;                                                                                   
                            tableDataColPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }
        });
        
    }
    
    public void setTimeString(boolean value){
        if (timeString != value){
            timeString = value;
            if (model!=null){
                DefaultTableModel m = newModel();

                // Add columns to the new model, excluding the removed column
                for (int col = 0; col < model.getColumnCount(); col++) {
                    m.addColumn(model.getColumnName(col));
                }

                // Add rows to the new model
                for (int row = 0; row < model.getRowCount(); row++) {
                    Object[] rowData = new Object[m.getColumnCount()];
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        Object v =  model.getValueAt(row, col);
                        if (col==0){
                            if (timeString){
                                rowData[col] = Time.timestampToStr(((Double)v).longValue());
                            } else {
                                rowData[col] = ((Long)Time.getTimestamp((String)v)).doubleValue();
                            }
                        } else {
                            rowData[col] = v;
                        }
                    }
                    m.addRow(rowData);
                }
                // Set the new model to the table
                table.setModel(m);    
                model = m;
                setRenderers();        
            }
        }
    }       
    
    protected DefaultTableModel newModel(){
        return new DefaultTableModel() {
            @Override
            public Class getColumnClass(int columnIndex) {
                if (timeString && (columnIndex==0)){
                    return String.class;
                }
                return Double.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };        
    }
            
    
    protected static DefaultTableModel newModel(Object[][] data, Object[] columnNames){
        ArrayList<Class> classes= new ArrayList<>();
        if (data.length>0){
            for (Object o: data[0]){
                classes.add(o.getClass());
            }
        }
        return new DefaultTableModel(data, columnNames) {
            @Override
            public Class getColumnClass(int columnIndex) {
                if (classes.size()>columnIndex){
                    return classes.get(columnIndex);
                }
                return Double.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
    }

    public void setScientificNotation(boolean value){
        scientificNotation = value;
        if (value){
            decimalFormat = new DecimalFormat("0.0#######E0");
        } else {
            decimalFormat = new DecimalFormat("0.0#######");
        }        
        setRenderers();
        table.repaint();
    }
    
    public boolean isScientificNotation(){
        return scientificNotation;
    }
    
    
    
    @Override
    protected void onTitleChanged() {
        if (getTitle() != null) {
            title.setText(getTitle());
            title.setFont(getTitleFont());
            add(title, BorderLayout.NORTH);
        } else {
            remove(title);
        }
    }
    
    @Override
    protected void onAxisLabelChanged(AxisId axis) {
        switch (axis) {
            case X -> {
                if (table.getColumnCount()>0) {
                    SwingUtils.setColumTitle(table, 0, getDomainLabel());
                }
            }
        }
    }
    

    public void setData(String[] header, Object[][] data) {
        if ((header == null) && (data != null) && (data[0] != null)) {
            header = new String[data[0].length];
        }
        model = newModel(data, header);
        table.setModel(model);
        setRenderers();
    }
    
    protected void setRenderers(){
        for (int col = 0; col < table.getColumnCount(); col++) {            
            DefaultTableCellRenderer renderer = (timeString && (col ==0)) ? stringRenderer : doubleRenderer;
            if (col>0){
                LinePlotSeries series= getAllSeries()[col-1];
                DefaultTableCellRenderer colRenderer = seriesRenderers.getOrDefault(series, null);
                if (colRenderer!=null){
                    renderer = colRenderer;
                }
            }
            table.getColumnModel().getColumn(col).setCellRenderer(renderer);
        }                    
    }
    
    
    Map<LinePlotSeries, DefaultTableCellRenderer> seriesRenderers =  new HashMap<>();
    public void setSeriesRenderer(LinePlotSeries series, DefaultTableCellRenderer renderer){
        if (renderer==null){
            seriesRenderers.remove(series);
        } else {
            seriesRenderers.put(series, renderer);
        }
         setRenderers();
    }

    @Override
    protected Object onAddedSeries(LinePlotSeries series) {       
        if (model.getColumnCount()==0){
            model.addColumn(getDomainLabel());
        }
        model.addColumn(getSeriesLabel(series));
        setRenderers();        
        return series.getName();
    }
    
    String getDomainLabel(){
        String label = getAxis(AxisId.X).getLabel();
        if ((label==null) || label.isBlank()){
            label = "Domain";
        }        
        return label;
    }

    String getSeriesLabel(LinePlotSeries series){
        String label = series.getName();
        if ((label==null) || label.isBlank()){
            label = "Series " + series.id;
        }        
        return label;
    }
    
    
    
    @Override
    protected void onRemovedSeries(LinePlotSeries series) {
        int index = getSeriesIndex(series);
        if (index>=0){
            if (model.getColumnCount()<=2){
                model =newModel();    
                table.setModel(model);
            } else {
                int column= getSeriesIndex(series)+1;
                SwingUtils.removeColumn(model, table, column);
                model = (DefaultTableModel) table.getModel();                       
                setRenderers();        
            }                        
        }
    }
    
    protected double getDomainValue(int row){
        Object timestamp = model.getValueAt(row, 0);
        if (timeString){
            return ((Long)Time.getTimestamp((String)timestamp)).doubleValue();
        } else {
            return (Double)timestamp;
        }
        
    }

    protected void appendData(int column, double domain, double value) {
        if (column <= 0) {
            throw new IllegalArgumentException("Column index must be greater than 0 (domain column is 0).");
        }

        // Search for the row with the matching domain value
        int rowCount = model.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            Double existingTimestamp = getDomainValue(row);
            if (existingTimestamp != null && existingTimestamp.equals(domain)) {
                // domain exists, update the value in the specified column
                model.setValueAt(value, row, column);
                return;
            }
        }

        // domain not found, create a new row
        Vector<Object> newRow = new Vector<>();
        // Add domain to the first column
        if (timeString){
            newRow.add(Time.timestampToStr(((Double)domain).longValue()));        
        } else {
            newRow.add(domain); 
        }

        // Fill other columns with null values (sparse table)
        for (int i = 1; i < model.getColumnCount(); i++) {
            if (i == column) {
                newRow.add(value); // Add value to the specified column
            } else {
                newRow.add(null); // Add null to other columns
            }
        }

        // Add the new row to the table
        model.addRow(newRow);
    }

    @Override
    protected void onAppendData(LinePlotSeries series, double x, double y) {
        int column= getSeriesIndex(series)+1;
        appendData(column, x, y);
    }

    @Override
    protected void onSetData(LinePlotSeries series, double[] x, double[] y) {
        if ((x==null) && (y==null)){
            return;
        }
        if (x==null){
            x = Arr.indexesDouble(y.length);
        }
        int column= getSeriesIndex(series)+1;        
        for (int i=0; i< x.length; i++){
            appendData(column, x[i], y[i]);
        }
    }

    @Override
    public void updateSeries(LinePlotSeries series) {
    }

    @Override
    public double[][] getSeriesData(LinePlotSeries series) {
        int column= getSeriesIndex(series)+1;
        double[] x = new double[0];
        double[] y = new double[0];        
        if (column>0){
            int rows = model.getRowCount();
            x = new double[rows];
            y = new double[rows];
            for (int i =0; i<rows; i++){            
                Double domain = getDomainValue(i);
                Double value = (Double)model.getValueAt(i, column);
                x[i]= domain;
                y[i]=(value==null) ? Double.NaN : value;
            }            
        }
        return new double[][]{x, y};    
    }

    @Override
    public void addPopupMenuItem(JMenuItem item) {
        if (popupMenu==null){
           popupMenu  = new JPopupMenu();
           if (item==null){
               return;
           }
        }
        if (item == null) {
            popupMenu.addSeparator();
        } else {
            popupMenu.add(item);
        }
    }
    
    public double[] getColumn(int index) {        
        int rowCount = model.getRowCount();
        int columnCount = model.getColumnCount();
        
        if ((index<0) || (index>=columnCount)){
            return null;
        }
        double[] ret = new double[rowCount];

        // Get the data vector from the model
        Vector<Vector> dataVector = model.getDataVector();

        for (int i = 0; i<rowCount; i++) {
            Object value = dataVector.get(i).get(index);
            if (timeString && (index==0)){
                value = ((Long)Time.getTimestamp((String)value)).doubleValue();
            }
                    
            if (value == null) {
                ret[i] = Double.NaN; // Convert null to NaN
            } else if (value instanceof Number number) {
                ret[i] = number.doubleValue(); // Convert Number to double
            } else {
                throw new IllegalArgumentException("Invalid data type in table model: " + value);
            }
        }
        return ret;        
    }    

}
