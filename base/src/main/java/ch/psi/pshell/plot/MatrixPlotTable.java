package ch.psi.pshell.plot;

import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.Vector;
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
public class MatrixPlotTable extends MatrixPlotBase {


    JTable table;
    JLabel title;
    DefaultTableModel model;    
    DecimalFormat decimalFormat;
    boolean scientificNotation;    
    final DefaultTableCellRenderer doubleRenderer;
    JPopupMenu popupMenu;
    final MouseAdapter tableMouseAdapter;
    int selectedDataCol;
    
    public MatrixPlotTable() {
        super();
        BorderLayout layout = new BorderLayout();
        layout.setVgap(4);
        setLayout(layout);
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);        
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        title = new JLabel();
        title.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        scrollPane.setViewportView(table);                
        //scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        //scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane);
        
        model = newModel(); 
        
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
        
        table.setModel(model);
        setRenderers();
        JMenuItem menuPlotRow = new JMenuItem("Plot row");
        
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
                menuPlotRow.setEnabled(table.getSelectedRow()>=0);
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
                
        menuPlotRow.addActionListener((ActionEvent e) -> {
            int row = table.getSelectedRow();
            double[] data = getRow(row);
            if (data!=null){
                LinePlotJFree.showDialog(getFrame(), "Row "+ row, null, data);
            }
        });

        JMenuItem menuPlotCol = new JMenuItem("Plot column");
        menuPlotCol.addActionListener((ActionEvent e) -> {
            int col = selectedDataCol;
            double[] data = getColumn(col);
            if (data!=null){
                LinePlotJFree.showDialog(getFrame(), "Column " + col, null, data);
            }
        });
                
        addPopupMenuItem(null);
        addPopupMenuItem(menuPlotRow);
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
    
    protected DefaultTableModel newModel(){
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

    public void setData(String[] header, Object[][] data) {
        if ((header == null) && (data != null) && (data[0] != null)) {
            header = new String[data[0].length];
        }
        model = LinePlotTable.newModel(data, header);
        table.setModel(model);
        setRenderers();
    }
    
    protected void setRenderers(){
        for (int col = 0; col < table.getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(doubleRenderer);
        }                    
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

    @Override
    protected Object onAddedSeries(MatrixPlotSeries series) {
        model = newModel();
        model.setColumnCount(series.getNumberOfBinsX());
        model.setRowCount(series.getNumberOfBinsY());
        table.setModel(model);        
        model.setColumnIdentifiers(Convert.toObjectArray(Arr.indexesInt(series.getNumberOfBinsX())));
        return series.name;
    }

    @Override
    protected void onRemovedSeries(MatrixPlotSeries series) {
        model = newModel();
        table.setModel(model);        
    }
    
    @Override
    protected void onAppendData(MatrixPlotSeries series, int indexX, int indexY, double x, double y, double z) {
        model.setValueAt(z, indexY, indexX);
    }

    @Override
    protected void onSetData(MatrixPlotSeries series, double[][] data, double[][] x, double[][] y) {
        for (int i=0; i<data.length; i++){
            double[] row = data[i];
            for (int j =0; j<row.length; j++){
                double val = row[j];
                model.setValueAt(Double.isNaN(val) ? null : val, i, j);
            }
        }        
    }

    @Override
    public void updateSeries(MatrixPlotSeries series) {
    }

    @Override
    public double[][] getSeriesData(MatrixPlotSeries series) {        
        int rowCount = model.getRowCount();
        int columnCount = model.getColumnCount();
        double[][] ret = new double[rowCount][columnCount];

        // Get the data vector from the model
        Vector<Vector> dataVector = model.getDataVector();

        for (int i = 0; i < rowCount; i++) {
            Vector<Object> row = dataVector.get(i);
            for (int j = 0; j < columnCount; j++) {
                Object value = row.get(j);
                if (value == null) {
                    ret[i][j] = Double.NaN; // Convert null to NaN
                } else if (value instanceof Number number) {
                    ret[i][j] = number.doubleValue(); // Convert Number to double
                } else {
                    throw new IllegalArgumentException("Invalid data type in table model: " + value);
                }
            }
        }
        return ret;        
    }

    public double[] getRow(int index) {        
        int rowCount = model.getRowCount();
        int columnCount = model.getColumnCount();
        
        if ((index<0) || (index>=rowCount)){
            return null;
        }
        double[] ret = new double[columnCount];

        // Get the data vector from the model
        Vector<Vector> dataVector = model.getDataVector();

        Vector<Object> row = dataVector.get(index);
        for (int i = 0; i<columnCount; i++) {
            Object value = row.get(i);
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
            } else if (value instanceof Number number) {
                ret[i] = number.doubleValue(); // Convert Number to double
            } else {
                throw new IllegalArgumentException("Invalid data type in table model: " + value);
            }
        }
        return ret;        
    }

}
