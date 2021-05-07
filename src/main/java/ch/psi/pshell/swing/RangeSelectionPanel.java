package ch.psi.pshell.swing;

import ch.psi.pshell.plot.LinePlotSeries;
import ch.psi.pshell.plot.Plot;
import ch.psi.pshell.plot.RangeSelectionPlot;
import ch.psi.pshell.plot.RangeSelectionPlot.RangeSelection;
import ch.psi.pshell.plot.RangeSelectionPlot.RangeSelectionPlotListener;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.swing.MainFrame;
import ch.psi.utils.swing.MonitoredPanel;
import ch.psi.utils.swing.SwingUtils;
import java.awt.Color;
import java.util.ArrayList;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class RangeSelectionPanel extends MonitoredPanel {

    boolean tableSelection;
    boolean inserting;
    boolean showMiddle = true;
    boolean tableInitialized;
    
    int indexLower = 0;
    int indexMiddle = 1;
    int indexUpper = 2;
    
    public RangeSelectionPanel() {
        initComponents();
        //table.setColumnModel(new HideColumnModel());                
        model = (DefaultTableModel) table.getModel();
        initPlot();
        initModel();
        initTable();
        plot.setSelectionColor(MainFrame.isDark() ? table.getSelectionBackground() : brighter(table.getSelectionBackground()));        
    }
    
    
    public boolean getShowMiddle(){
        return showMiddle;
    }

    public void setShowMiddle(boolean value){
        if (showMiddle!=value){
            showMiddle=value;
            indexMiddle = showMiddle? 1 : -1;
            indexUpper = showMiddle? 2 : 1;            
            if (tableInitialized){
                this.setAditionalColumns(new String[0], new Class[0]);
            }
        }
    }
    

    void initTable() {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() >= 0) {
                    tableSelection = true;
                    try {
                        int index = table.convertRowIndexToModel(table.getSelectedRow());
                        if (index >= 0) {
                            RangeSelection range = getPlotRange((Double) model.getValueAt(index, indexLower), (Double) model.getValueAt(index, indexUpper));
                            plot.selectMarker(range);
                        } else {
                            plot.selectMarker(null);
                        }
                    } finally {
                        tableSelection = false;
                    }
                }
            }
        });

    }

    void initModel() {        
        model.addTableModelListener(new TableModelListener() {
            boolean processingUpdate;

            @Override
            public void tableChanged(TableModelEvent e) {
                if (table.isEditing() && (e.getType() == TableModelEvent.UPDATE) && !processingUpdate) {
                    processingUpdate = true;
                    boolean adding = false;
                    int index = -1;
                    RangeSelection range = null;
                    try {
                        if ((table.getSelectedRow() >= 0) && (table.getSelectedColumn() <= indexUpper)) {
                            index = table.convertRowIndexToModel(table.getSelectedRow());
                            Double min = (Double) model.getValueAt(index, indexLower);
                            Double center =  showMiddle ? (Double) model.getValueAt(index, indexMiddle) : null;
                            Double max = (Double) model.getValueAt(index, indexUpper);
                            min = (min == null) ? null : Convert.roundDouble(min, getPrecision());
                            center = (center == null) ? null : Convert.roundDouble(center, getPrecision());
                            max = (max == null) ? null : Convert.roundDouble(max, getPrecision());

                            Double[] vals = showMiddle ? new Double[]{min, center, max} : new Double[]{min, max};
                            vals[e.getColumn()] = null;
                            range = getPlotRange(vals[0], showMiddle ? vals[1] : null, vals[vals.length-1]);

                            if ((min == null) || (max == null)) {
                                if ((min != null) && (center != null)) {
                                    max = (center - min) + center;
                                }
                                if ((max != null) && (center != null)) {
                                    min = center - (max - center);
                                }
                            }
                                
                            if (range != null) {
                                //Editing existing

                                if ((showMiddle) && (e.getColumn() == 1)) {
                                    //If changed center
                                    double offset = center - range.getCenter();
                                    plot.updateRange(range, min + offset, max + offset);
                                } else {
                                    plot.updateRange(range, min, max);
                                    if (showMiddle){
                                        model.setValueAt(range.getCenter(), index, 1);
                                    }
                                }
                                //Setting those to enforce precision
                                model.setValueAt(range.getMin(), index, indexLower);
                                model.setValueAt(range.getMax(), index, indexUpper);
                            } else {
                                //Adding new
                                adding = true;
                                if ((min != null) && (max != null)) {
                                    plot.addRange(min, max);

                                    for (int j = (indexUpper+1); j < model.getColumnCount(); j++) {
                                        model.setValueAt(model.getValueAt(index, j), model.getRowCount() - 1, j);
                                    }
                                    model.removeRow(index);
                                    inserting = false;
                                    updateTablePanels();
                                    onSelectionChanged();
                                }
                            }
                        }
                    } catch (Exception ex) {
                        showException(ex);
                        if (!adding) {
                            //Restore values
                            if (index >= 0) {
                                if (range != null) {
                                    model.setValueAt(range.getMin(), index, indexLower);
                                    if (showMiddle){
                                        model.setValueAt(range.getCenter(), index, indexMiddle);
                                    }
                                    model.setValueAt(range.getMax(), index, indexUpper);
                                }
                            }
                        }
                    } finally {
                        processingUpdate = false;
                    }
                }
            }
        });
        tableInitialized = true;
    }

    void initPlot() {
        plot.setTitle("");
        plot.getAxis(Plot.AxisId.X).setLabel("");
        plot.getAxis(Plot.AxisId.Y).setLabel("");
        plot.setMultipleSelection(true);
        updateTablePanels();
        plot.setListener(new RangeSelectionPlotListener() {
            @Override
            public void onRangeAdded(RangeSelectionPlot.RangeSelection range) {
                model.addRow(showMiddle ? 
                        new Object[]{range.getMin(), range.getCenter(), range.getMax()}:
                        new Object[]{range.getMin(), range.getMax()}
                );
                updateTablePanels();
                onSelectionChanged();
            }

            @Override
            public void onRangeRemoved(RangeSelectionPlot.RangeSelection range) {
                int index = getModelIndex(range.getMin(), null, range.getMax());
                if (index >= 0) {
                    model.removeRow(index);
                    updateTablePanels();
                    onSelectionChanged();
                }
            }

            @Override
            public void onRangeChanged(RangeSelection range) {
            }

            @Override
            public void onRangeSelected(RangeSelection range) {
                if (tableSelection) {
                    return;
                }
                if (range == null) {
                    table.clearSelection();
                } else {
                    int index = getTableIndex(range.getMin(), null, range.getMax());
                    if (index >= 0) {
                        table.setRowSelectionInterval(index, index);
                    }
                }
            }

            @Override
            public void onDataChanged() {
                updateTablePanels();
            }
        });

    }

    public void setAditionalColumns(String[] names, final Class[] types) {
        //model
        model = new javax.swing.table.DefaultTableModel(new Object[][]{}, 
                Arr.append(showMiddle ? new String[]{"Lower", "Middle", "Upper"}: new String[]{"Lower", "Upper"}, names)) {
            public Class getColumnClass(int columnIndex) {
                return (columnIndex < (indexUpper+1)) ? Double.class : types[columnIndex - (indexUpper+1)];
            }
        };
        table.setModel(model);
        initModel();
    }

    public Color brighter(Color c) {
        Color ret = c;
        do {
            ret = new Color((ret.getRed() + 255) / 2,
                    (ret.getGreen() + 255) / 2,
                    (ret.getBlue() + 255) / 2);
        } while (SwingUtils.getPerceivedLuminance(ret) < 224);
        return ret;
    }

    public RangeSelection getPlotRange(Double min, Double max) {
        return getPlotRange(min, null, max);
    }
    
    public RangeSelection getPlotRange(Double min, Double center, Double max) {
        double res = getResolution();
        for (RangeSelection range : plot.getSelectedRanges()) {
            if ((min != null) && (max != null)) {
                if ((Math.abs(min - range.getMin()) <= res) && (Math.abs(max - range.getMax()) <= res)) {
                    return range;
                }
            } else if (!showMiddle){
                if (min != null) {
                    if (Math.abs(min - range.getMin()) <= res) {
                        return range;
                    }
                } else if (max != null) {
                    if (Math.abs(max - range.getMax()) <= res) {
                        return range;
                    }
                }                
            } else if ((min != null) && (center != null)) {
                if ((Math.abs(min - range.getMin()) <= res) && (Math.abs(center - range.getCenter()) <= res)) {
                    return range;
                }
            } else if ((max != null) && (center != null)) {
                if ((Math.abs(center - range.getCenter()) <= res) && (Math.abs(max - range.getMax()) <= res)) {
                    return range;
                }
            }
        }
        return null;
    }

    int getModelIndex(Double min, Double center, Double max) {
        int index = getTableIndex(min, center, max);
        if (index >= 0) {
            return table.convertRowIndexToModel(index);
        }
        return -1;
    }

    int getTableIndex(Double min, Double center, Double max) {
        if ((min == null) || (max == null)) {
            if ((min != null) && (center != null)) {
                max = (center - min) + center;
            }
            if ((max != null) && (center != null)) {
                min = center - (max - center);
            }
        }
        double res = getResolution();
        for (int i = 0; i < table.getRowCount(); i++) {
            if ((table.getValueAt(i, indexLower) != null) && (Math.abs((Double) table.getValueAt(i, indexLower) - min) <= res)) {
                if ((table.getValueAt(i, indexUpper) != null) && (Math.abs((Double) table.getValueAt(i, indexUpper) - max) <= res)) {
                    return i;
                }
            }
        }
        return -1;
    }

    DefaultTableModel model;

    public enum TablePosition {

        right,
        left,
        top,
        bottom
    }

    public TablePosition getTablePosition() {
        if (splitter.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            if (splitter.getRightComponent() == panelTable) {
                return TablePosition.right;
            } else {
                return TablePosition.left;
            }
        } else {
            if (splitter.getRightComponent() == panelTable) {
                return TablePosition.bottom;
            } else {
                return TablePosition.top;
            }
        }
    }

    public void setTablePosition(TablePosition position) {
        if (position != getTablePosition()) {
            splitter.setLeftComponent(null);
            splitter.setRightComponent(null);
            switch (position) {
                case right:
                    splitter.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                    splitter.setLeftComponent(plot);
                    splitter.setRightComponent(panelTable);
                    break;
                case left:
                    splitter.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                    splitter.setLeftComponent(panelTable);
                    splitter.setRightComponent(plot);
                    break;
                case top:
                    splitter.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitter.setLeftComponent(panelTable);
                    splitter.setRightComponent(plot);
                    break;
                case bottom:
                    splitter.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitter.setLeftComponent(plot);
                    splitter.setRightComponent(panelTable);
                    break;
            }
        }
    }

    public void setSplitterPosition(int position) {
        splitter.setDividerLocation(position);
    }

    public int getSplitterPosition() {
        return splitter.getDividerLocation();
    }

    public void setSplitterEnabled(boolean enabled) {
        splitter.setEnabled(enabled);
    }

    public boolean getSplitterEnabled() {
        return splitter.isEnabled();
    }

    public void setShowTable(boolean value) {
        panelTable.setVisible(value);
    }

    public boolean getShowTable() {
        return panelTable.isVisible();
    }

    public int getPrecision() {
        return plot.getPrecision();
    }

    public void setPrecision(int value) {
        plot.setPrecision(value);
    }

    public double getResolution() {
        return Math.pow(10.0, -getPrecision());
    }

    public double getMinimumLength() {
        return plot.getMinimumLength();
    }

    public void setMinimumLength(double value) {
        plot.setMinimumLength(value);
    }

    public boolean isOverlapAllowed() {
        return plot.isOverlapAllowed();
    }

    public void setOverlapAllowed(boolean value) {
        plot.setOverlapAllowed(value);
    }

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        plot.setManualSelectionEnabled(value);
        table.setEnabled(value);
        buttonClear.setEnabled(value);
        updateTablePanels();
    }

    private void updateTablePanels() {
        buttonDelete.setEnabled((model.getRowCount() > 0) && isEnabled());
        buttonInsert.setEnabled(plot.hasData() && !inserting && isEnabled());
    }

    public RangeSelectionPlot getPlot() {
        return plot;
    }

    public JTable getTable() {
        return table;
    }

    public void setSeries(LinePlotSeries series) {
        if (plot.getNumberOfSeries() > 0) {
            if (plot.getSeries(0) == series) {
                return;
            }
        }
        plot.clear();
        model.setNumRows(0);
        if (series != null) {
            plot.addSeries(series);
        }
        updateTablePanels();
        onSeriesChanged();
    }

    Object[] getVars(RangeSelection range) {
        for (int i = 0; i < model.getRowCount(); i++) {
            Double min = (Double) model.getValueAt(i, indexLower);
            Double max = (Double) model.getValueAt(i, indexUpper);
            if (range.equals(min, max)) {
                ArrayList ret = new ArrayList();
                for (int j = (indexUpper+1); j < model.getColumnCount(); j++) {
                    ret.add(model.getValueAt(i, j));
                }
                return ret.toArray();
            }
        }
        return null;
    }

    public void selectRange(int index) {
        RangeSelection[] ranges = plot.getSelectedRanges();
        if ((index >= 0) && (index < ranges.length)) {
            selectRange(ranges[index]);
        } else {
            selectRange(null);
        }
    }

    public void selectRange(RangeSelection range) {
        plot.selectMarker(range);
    }

    public RangeSelection[] getRanges() {
        RangeSelection[] ret = plot.getSelectedRanges();
        for (RangeSelection r : ret) {
            r.setVars(getVars(r));
        }
        return ret;
    }

    public RangeSelection[] getRangesOrdered() {
        RangeSelection[] ret = plot.getSelectedRangesOrdered();
        for (RangeSelection r : ret) {
            r.setVars(getVars(r));
        }
        return ret;
    }

    public void removeAllRanges() {
        plot.removeAllRanges();
        model.setNumRows(0);
        inserting = false;
        updateTablePanels();
        onSelectionChanged();
    }

    public void clear() {
        removeAllRanges();
        setSeries(null);
        onSeriesChanged();
    }

    protected void onSeriesChanged() {

    }

    protected void onSelectionChanged() {

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        splitter = new javax.swing.JSplitPane();
        plot = new ch.psi.pshell.plot.RangeSelectionPlot();
        panelTable = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        buttonInsert = new javax.swing.JButton();
        buttonDelete = new javax.swing.JButton();
        buttonClear = new javax.swing.JButton();

        splitter.setResizeWeight(0.5);
        splitter.setLeftComponent(plot);

        table.setAutoCreateRowSorter(true);
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Lower", "Middle", "Upper"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(table);

        buttonInsert.setText("Insert");
        buttonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInsertActionPerformed(evt);
            }
        });

        buttonDelete.setText("Delete");
        buttonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDeleteActionPerformed(evt);
            }
        });

        buttonClear.setText("Clear");
        buttonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelTableLayout = new javax.swing.GroupLayout(panelTable);
        panelTable.setLayout(panelTableLayout);
        panelTableLayout.setHorizontalGroup(
            panelTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTableLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonInsert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonDelete)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonClear)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelTableLayout.setVerticalGroup(
            panelTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTableLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonDelete)
                    .addComponent(buttonInsert)
                    .addComponent(buttonClear))
                .addContainerGap())
        );

        splitter.setRightComponent(panelTable);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitter, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitter)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInsertActionPerformed
        try {
            model.insertRow(table.getRowCount(), new Object[]{null, null, null});
            inserting = true;
            updateTablePanels();
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonInsertActionPerformed

    private void buttonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDeleteActionPerformed
        try {
            if (table.getSelectedRow() >= 0) {
                int index = table.convertRowIndexToModel(table.getSelectedRow());
                RangeSelection range = getPlotRange((Double) model.getValueAt(index, indexLower), (Double) model.getValueAt(index, indexUpper));
                if (range != null) {
                    plot.removeRange(range);
                } else {
                    inserting = false;
                    model.removeRow(index);
                }
                updateTablePanels();
            }
        } catch (Exception ex) {
            showException(ex);
        }
    }//GEN-LAST:event_buttonDeleteActionPerformed

    private void buttonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearActionPerformed
        removeAllRanges();
    }//GEN-LAST:event_buttonClearActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonClear;
    private javax.swing.JButton buttonDelete;
    private javax.swing.JButton buttonInsert;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel panelTable;
    private ch.psi.pshell.plot.RangeSelectionPlot plot;
    private javax.swing.JSplitPane splitter;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
