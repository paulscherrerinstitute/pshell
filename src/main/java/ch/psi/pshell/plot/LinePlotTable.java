package ch.psi.pshell.plot;

import ch.psi.utils.Arr;
import ch.psi.utils.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
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
    JPopupMenu popupMenu;    
    JCheckBoxMenuItem menuScientificNotation;
    final MouseAdapter tableMouseAdapter;
    int selectedDataCol;
    
    public LinePlotTable() {
        super();
        BorderLayout layout = new BorderLayout();
        layout.setVgap(4);
        setLayout(layout);
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        
        title = new JLabel();
        title.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        scrollPane.setViewportView(table);
        model =newModel();
        add(scrollPane);
        
        setScientificNotation(false);
        doubleRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Double) {
                    setText(decimalFormat.format((Double) value)); // Format the Double value
                } else {
                    super.setValue(value); // Default behavior for non-Double values
                }
            }
        };
        doubleRenderer.setHorizontalAlignment(SwingConstants.RIGHT); // Align numbers to the right
        
        model.addColumn("domain");
        table.setModel(model);
        setRenderers();
        
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
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } catch (Exception ex) {
                    showException(ex);
                }
            }

        };
        table.addMouseListener(tableMouseAdapter);      
        
        menuScientificNotation = new JCheckBoxMenuItem("Scientific Notation");
        menuScientificNotation.addActionListener((ActionEvent e) -> {
            setScientificNotation(menuScientificNotation.isSelected());
        });

        addPopupMenuItem(null);
        addPopupMenuItem(menuScientificNotation);
        
        
        JMenuItem menuPlotCol = new JMenuItem("Plot col");
        menuPlotCol.addActionListener((ActionEvent e) -> {
            int col = selectedDataCol;
            if (col>=0){
                double[] y = getColumn(col);
                if (y!=null){
                    double[] x = (col>=0) ? getColumn(0) : null;
                    //plotData("Column " + col, x, y);
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
    
    protected static DefaultTableModel newModel(){
        return new DefaultTableModel() {
            @Override
            public Class getColumnClass(int columnIndex) {
                return Double.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };        
    }
    
    protected static DefaultTableModel newModel(Object[][] data, Object[] columnNames){
        return new DefaultTableModel(data, columnNames) {
            @Override
            public Class getColumnClass(int columnIndex) {
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

    public void setData(String[] header, String[][] data) {
        if ((header == null) && (data != null) && (data[0] != null)) {
            header = new String[data[0].length];
        }
        model = newModel(data, header);
        table.setModel(model);
        setRenderers();
    }
    
    protected void setRenderers(){
        for (int col = 0; col < table.getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(doubleRenderer);
        }                    
    }

    @Override
    protected Object onAddedSeries(LinePlotSeries series) {        
        model.addColumn(series.getName());
        setRenderers();
        return series.getName();
    }
    
    @Override
    protected void onRemovedSeries(LinePlotSeries series) {
        int index = getSeriesIndex(series);
        if (index>=0){
            int column= getSeriesIndex(series)+1;
            SwingUtils.removeColumn(model, table, column);
            model = (DefaultTableModel) table.getModel();            
            setRenderers();        
        }
    }
    

    protected void appendData(int column, double domain, double value) {
        if (column <= 0) {
            throw new IllegalArgumentException("Column index must be greater than 0 (domain column is 0).");
        }

        // Search for the row with the matching domain value
        int rowCount = model.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            Double existingTimestamp = (Double) model.getValueAt(row, 0); // Get domain from column 0
            if (existingTimestamp != null && existingTimestamp.equals(domain)) {
                // domain exists, update the value in the specified column
                model.setValueAt(value, row, column);
                return;
            }
        }

        // domain not found, create a new row
        Vector<Object> newRow = new Vector<>();
        newRow.add(domain); // Add domain to the first column

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
                Double domain = (Double)model.getValueAt(i, 0);
                Double value = (Double)model.getValueAt(i, column);
                x[i]=domain;
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
            if (value == null) {
                ret[i] = Double.NaN; // Convert null to NaN
            } else if (value instanceof Number) {
                ret[i] = ((Number) value).doubleValue(); // Convert Number to double
            } else {
                throw new IllegalArgumentException("Invalid data type in table model: " + value);
            }
        }
        return ret;        
    }    

}
