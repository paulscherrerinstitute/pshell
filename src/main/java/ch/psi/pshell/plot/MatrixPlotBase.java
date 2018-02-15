package ch.psi.pshell.plot;

import ch.psi.pshell.imaging.Colormap;
import javax.swing.JPanel;
import ch.psi.utils.swing.SwingUtils;
import ch.psi.pshell.plot.MatrixPlotSeries.MatrixPlotSeriesListener;
import static ch.psi.pshell.plot.PlotBase.getDefaultColormap;
import ch.psi.utils.Reflection.Hidden;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 *
 */
abstract public class MatrixPlotBase extends PlotBase<MatrixPlotSeries> implements MatrixPlot {

    private Colormap colormap;

    protected MatrixPlotBase() {
        this(null);
    }

    protected MatrixPlotBase(String title) {
        super(MatrixPlotSeries.class, title);
    }

    @Override
    protected void createChart() {
        super.createChart();
        createAxis(AxisId.X, "X");
        createAxis(AxisId.Y, "Y");
        createAxis(AxisId.Z);
    }

    //Only supporting one series for now so let's gain some speed    
    @Override
    public void addSeries(MatrixPlotSeries series) {
        if (getAllSeries().length > 0) {
            removeSeries(getAllSeries()[0]);
        }
        super.addSeries(series);

    }
   
    final MatrixPlotSeriesListener seriesListener = new MatrixPlotSeriesListener() {
        @Override
        public void onSeriesAppendData(MatrixPlotSeries series, double x, double y, double z) {

            if ((series == null) || (getNumberOfSeries() == 0)) {
                return;
            }
            final int indexX = (int) Math.round((x - series.getMinX()) / series.getBinWidthX());
            final int indexY = (int) Math.round((y - series.getMinY()) / series.getBinWidthY());

            if (!series.contains(indexX, indexY)) {
                return;
            }
            onAppendData(series, indexX, indexY, x, y, z);
            if (getRequireUpdateOnAppend() && isUpdatesEnabled()) {
                requestSeriesUpdate(series);
            }
        }

        @Override
        public void onSeriesSetData(MatrixPlotSeries series, double[][] data, double[][] x, double[][] y) {
            if ((series == null) || (getNumberOfSeries() == 0)) {
                return;
            }
            //TODO: Check consistency of z
            onSetData(series, data, x, y);
            if (isUpdatesEnabled()) {
                requestSeriesUpdate(series);
            }
        }
    };

    @Override
    public double[][] getSeriesX(final MatrixPlotSeries series) {
        return null; //Given by the position only
    }

    @Override
    public double[][] getSeriesY(final MatrixPlotSeries series) {
        return null; //Given by the position only
    }

    @Override
    protected String getDataAsString() {
        if (getAllSeries().length == 0) {
            return null;
        }
        MatrixPlotSeries series = getAllSeries()[0];
        StringBuilder str = new StringBuilder(1024);

        str.append(getAxis(AxisId.X).getLabel().isEmpty() ? "X" : getAxis(AxisId.X).getLabel()).append(FIELD_SEPARATOR);
        str.append(getAxis(AxisId.Y).getLabel().isEmpty() ? "Y" : getAxis(AxisId.Y).getLabel()).append(FIELD_SEPARATOR);
        str.append(getAxis(AxisId.Z).getLabel().isEmpty() ? "Z" : getAxis(AxisId.Z).getLabel()).append(LINE_SEPARATOR);

        double[][] data = getSeriesData(getAllSeries()[0]);
        double[][] xdata = getSeriesX(getAllSeries()[0]);
        double[][] ydata = getSeriesY(getAllSeries()[0]);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                double z = data[i][j];
                double y = (ydata == null) ? series.getMinY() + i * series.getBinWidthY() : ydata[i][j];
                double x = (xdata == null) ? series.getMinX() + j * series.getBinWidthX() : xdata[i][j];
                str.append(String.format("%1.6f", x)).append(FIELD_SEPARATOR);
                str.append(String.format("%1.6f", y)).append(FIELD_SEPARATOR);
                str.append(String.format("%1.6f", z)).append(LINE_SEPARATOR);
            }
        }
        return str.toString();
    }

    @Override
    public void detach(String className) {
        try {
            if (getAllSeries().length == 0) {
                return;
            }
            MatrixPlotSeries s = getAllSeries()[0];
            MatrixPlot p = (MatrixPlot) Plot.newPlot(className);
            p.setTitle(getTitle());
            p.getAxis(AxisId.X).setLabel(getAxis(AxisId.X).getLabel());
            p.getAxis(AxisId.Y).setLabel(getAxis(AxisId.Y).getLabel());
            p.getAxis(AxisId.Z).setLabel(getAxis(AxisId.Z).getLabel());
            MatrixPlotSeries detachedSeries = new MatrixPlotSeries(null, s.getMinX(), s.getMaxX(), s.getNumberOfBinsX(), s.getMinY(), s.getMaxY(), s.getNumberOfBinsY());
            p.addSeries(detachedSeries);
            detachedSeries.setData(s.getData(), s.getX(), s.getY());

            //TODO: Detaching always in HIGH quality?
            //p.setQuality(quality);        
            Frame frame = SwingUtils.getFrame(this);
            JDialog dlg = new JDialog(frame, getTitle(), false);
            ((JPanel) p).setPreferredSize(new Dimension(DETACHED_WIDTH, DETACHED_HEIGHT));
            dlg.setContentPane((JPanel) p);
            dlg.pack();
            SwingUtils.centerComponent(frame, dlg);
            dlg.setVisible(true);
        } catch (Exception ex) {
            SwingUtils.showException(MatrixPlotBase.this, ex);
        }
    }

    //Injecting specific entries in popup menu
    @Override
    protected void createPopupMenu() {
        super.createPopupMenu();
    }

    public static class ManualScaleDialog extends JDialog {

        // Default serial id
        private static final long serialVersionUID = 1L;

        private final JPanel contentPanel = new JPanel();
        private final JTextField textFieldHighValue;
        private final JTextField textFieldLowValue;

        private MatrixPlotBase plot;

        private int selectedOption;

        public ManualScaleDialog() {
            setModal(true); // Block until dialog is disposed.
            setBounds(100, 100, 450, 300);
            getContentPane().setLayout(new BorderLayout());
            contentPanel.setLayout(new FlowLayout());
            contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            getContentPane().add(contentPanel, BorderLayout.CENTER);
            {
                JLabel lblLowValue = new JLabel("Low Value:");
                lblLowValue.setFont(new Font("Lucida Grande", Font.BOLD, 13));
                contentPanel.add(lblLowValue);
            }
            {
                textFieldLowValue = new JTextField();
                contentPanel.add(textFieldLowValue);
                textFieldLowValue.setColumns(10);
            }
            {
                JLabel lblHighValue = new JLabel("High Value:");
                lblHighValue.setFont(new Font("Lucida Grande", Font.BOLD, 13));
                contentPanel.add(lblHighValue);
            }
            {
                textFieldHighValue = new JTextField();
                contentPanel.add(textFieldHighValue);
                textFieldHighValue.setColumns(10);
            }
            {
                JPanel buttonPane = new JPanel();
                buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
                getContentPane().add(buttonPane, BorderLayout.SOUTH);
                {
                    JButton okButton = new JButton("OK");
                    okButton.addActionListener((ActionEvent arg0) -> {
                        // set OK
                        selectedOption = JOptionPane.OK_OPTION;
                        dispose();
                    });
                    {
                        JButton btnApply = new JButton("Apply");
                        btnApply.addActionListener((ActionEvent arg0) -> {
                            // TODO apply values
                            if (plot != null) {
                                plot.setScale(getLow(), getHigh());
                            }
                        });
                        buttonPane.add(btnApply);
                    }
                    okButton.setActionCommand("OK");
                    buttonPane.add(okButton);
                    getRootPane().setDefaultButton(okButton);
                }
                {
                    JButton cancelButton = new JButton("Cancel");
                    cancelButton.addActionListener((ActionEvent arg0) -> {
                        // set Cancel
                        selectedOption = JOptionPane.CANCEL_OPTION;
                        dispose();
                    });
                    cancelButton.setActionCommand("Cancel");
                    buttonPane.add(cancelButton);
                }
            }

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
        }

        public void showDialog() {
            setVisible(true);
        }

        public void setMatrixPlot(MatrixPlotBase p) {
            plot = p;
        }

        public void setLow(double low) {
            textFieldLowValue.setText(String.format("%.4f", low));
        }

        public void setHigh(double high) {
            textFieldHighValue.setText(String.format("%.4f", high));
        }

        public double getLow() {
            double low = Double.NaN;
            try {
                low = Double.parseDouble(textFieldLowValue.getText());
            } catch (NumberFormatException e) {
            }
            return low;
        }

        public double getHigh() {
            double high = Double.NaN;
            try {
                high = Double.parseDouble(textFieldHighValue.getText());
            } catch (NumberFormatException e) {
            }
            return high;
        }

        /**
         * @return the selectedOption
         */
        public int getSelectedOption() {
            return selectedOption;
        }
    }

    boolean autoScale = true;
    double scaleMin = Double.NaN;
    double scaleMax = Double.NaN;
    double scaleRange = Double.NaN;

    public void setScale(double scaleMin, double scaleMax) {
        autoScale = false;
        updateScale(scaleMin, scaleMax);
    }

    protected void updateScale(double scaleMin, double scaleMax) {
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.scaleRange = scaleMax - scaleMin;
    }

    public void setAutoScale() {
        autoScale = true;
    }

    public boolean isAutoScale() {
        return autoScale;
    }

    public double getScaleMin() {
        return scaleMin;
    }

    public double getScaleMax() {
        return scaleMax;
    }

    public void setColormap(Colormap value) {
        if (value != getColormap()) {
            colormap = value;
        }
    }

    public Colormap getColormap() {
        if (colormap == null) {
            colormap = getDefaultColormap();
        }
        return colormap;
    }

    //abstract protected Object onSetSeries(MatrixPlotSeries series);
    abstract protected void onAppendData(MatrixPlotSeries series, int indexX, int indexY, double x, double y, double z);

    abstract protected void onSetData(MatrixPlotSeries series, double[][] data, double[][] x, double[][] y);


    @Hidden
    public static MatrixPlot newPlot(Style style) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        style = (style == null) ? MatrixPlot.Style.Normal : style;
        switch (style) {
            case Mesh:
                return (MatrixPlot) Plot.newPlot("ch.psi.pshell.plot.SurfacePlotJzy3d");
            case Image:
                return (MatrixPlot) Plot.newPlot(ch.psi.pshell.plot.MatrixPlotRenderer.class.getName());
            default:
                return (MatrixPlot) Plot.newPlot(ch.psi.pshell.plot.MatrixPlotJFree.class.getName());
        }
    }

    @Override
    public String toString() {
        return MatrixPlot.class.getSimpleName();
    }
}
