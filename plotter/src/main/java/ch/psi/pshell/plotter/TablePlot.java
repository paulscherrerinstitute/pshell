package ch.psi.pshell.plotter;

import ch.psi.pshell.plot.PlotBase;
import ch.psi.pshell.plot.PlotSeries;
import ch.psi.pshell.plotter.TablePlot.TableSeries;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.Writer;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Dummy plot, used to manage tables together with plots
 */
public class TablePlot extends PlotBase<TableSeries> {

    public static class TableSeries extends PlotSeries<TablePlot> {
    }

    JTable table;
    JLabel title;

    public TablePlot() {
        super(TableSeries.class);
        BorderLayout layout = new BorderLayout();
        layout.setVgap(4);
        setLayout(layout);
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        title = new JLabel();
        title.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new java.awt.Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        scrollPane.setViewportView(table);

        add(scrollPane);
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
            //Arrays.fill(header, "");
        }
        DefaultTableModel model = new DefaultTableModel(data, header) {
            @Override
            public Class getColumnClass(int columnIndex) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        table.setModel(model);
    }

    @Override
    protected void writeData(Writer writer) throws IOException {      
        throw new UnsupportedOperationException();
    }

    @Override
    public void detach(String className) {
    }

    @Override
    public void addPopupMenuItem(JMenuItem item) {
    }

    @Override
    protected Object onAddedSeries(TableSeries series) {
        return null;
    }

    @Override
    protected void onRemovedSeries(TableSeries series) {
    }

    @Override
    public void updateSeries(TableSeries series) {
    }

    @Override
    public double[][] getSeriesData(TableSeries series) {
        return null;
    }

}
