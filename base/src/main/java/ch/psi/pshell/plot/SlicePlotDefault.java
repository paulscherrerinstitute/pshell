package ch.psi.pshell.plot;

import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.swing.ValueSelection;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Range;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;

/**
 *
 * |
 */
public class SlicePlotDefault extends SlicePlotBase {

    MatrixPlotBase matrixPlot;


    static MatrixPlotBase getDefaultMatrixPlot(){
        try {
            return (MatrixPlotBase) Plot.newPlot(PlotPanel.getMatrixPlotImpl());            
        } catch (Exception ex) {
            Logger.getLogger(SlicePlotDefault.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }        
    }
    
    public SlicePlotDefault() {
        this(getDefaultMatrixPlot());
    }
    
    public SlicePlotDefault(MatrixPlotBase matrixPlot) {
        initComponents();

        pageSelection.setMinValue(0);
        pageSelection.setMaxValue(0);
        pageSelection.setStep(1);
        pageSelection.setDecimals(0);
        pageSelection.setValue(0);
        pageSelection.addListener((ValueSelection origin, double value, boolean editing) -> {
            if (editing) {
                setPage((int) value);
            }
        });
        this.matrixPlot = matrixPlot;
        if (matrixPlot != null){
            if (matrixPlot instanceof MatrixPlotRenderer matrixPlotRenderer){
                matrixPlotRenderer.getRenderer().getStatusBar().setShowFrameRate(false);
            }
            panelPlot.add(matrixPlot);
        }        
    }

    public MatrixPlotBase getMatrixPlot() {
        return matrixPlot;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        setPageTitle();
    }

    @Override
    public void setQuality(Quality quality) {
        super.setQuality(quality);
        if (matrixPlot != null) {
            matrixPlot.setQuality(quality);
        }
    }

    public void setColormap(Colormap colormap) {
        if (matrixPlot != null) {
            matrixPlot.setColormap(colormap);
        }
    }

    public Colormap getColormap() {
        if (matrixPlot != null) {
            return matrixPlot.getColormap();
        }
        return getDefaultColormap();
    }
    
    public void setColormapLogarithmic(boolean value) {
        if (matrixPlot != null) {
            matrixPlot.setColormapLogarithmic(value);
        }
    }
    
    public boolean isColormapLogarithmic() {
        if (matrixPlot != null) {
            return matrixPlot.isColormapLogarithmic();
        }
        return false;
    }    
    
    
    @Override
    public void setTitleFont(Font font) {
        super.setTitleFont(font);
        if (matrixPlot != null) {
            matrixPlot.setTitleFont(font);
        }
    }
    
    @Override
    public void setPlotOutlineColor(Color c) {
        if (matrixPlot != null) {
            matrixPlot.setPlotOutlineColor(c);
        }
    }
    
    @Override
    public void setPlotOutlineWidth(int width) {
        if (matrixPlot != null) {
            matrixPlot.setPlotOutlineWidth(width);
        }   
    }       

    void setData(double[][] data) {
        if (data == null) {
            matrixPlot.getSeries(0).clear();
        } else {
            matrixPlot.getSeries(0).setData(data);
        }
    }

    public void setPage(int page) {
        try {
            if (getSeries(0) != null) {
                getSeries(0).setPage(page);
                setPageTitle();
                pageSelection.setValue(page);
            }
        } catch (Exception ex) {
            if (!offscreen){
                showException(ex);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pageSelection = new ch.psi.pshell.swing.ValueSelection();
        panelPlot = new javax.swing.JPanel();

        panelPlot.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pageSelection, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(panelPlot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelPlot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pageSelection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ch.psi.pshell.swing.ValueSelection pageSelection;
    private javax.swing.JPanel panelPlot;
    // End of variables declaration//GEN-END:variables

    @Override
    public void addPopupMenuItem(JMenuItem item) {
    }

    @Override
    protected Object onAddedSeries(SlicePlotSeries series) {
        matrixPlot.addSeries(series);
        pageSelection.setValue(0);
        int max = Math.max(getSeries(0).getNumberOfBinsZ() - 1, 0);
        pageSelection.setMaxValue(max);
        pageSelection.setEnabled(true);
        setPageTitle();
        return null;
    }
    
    protected String getPageSubtitle(SlicePlotSeries series, int page){
        if ((series.hasRangeZ())) {
            return " z=" + String.valueOf(Convert.roundDouble(series.getZ(page), 6));
        }
        return "";
    }

    void setPageTitle() {
        if (matrixPlot != null) {
            String title = getTitle();                        

            SlicePlotSeries series = getSeries(0);
            if (series != null) {
                int page = (int) pageSelection.getValue();
                title += getPageSubtitle(series, page);                
            }
            matrixPlot.setTitle(title);
        }
    }

    @Override
    protected void onRemovedSeries(SlicePlotSeries series) {
        matrixPlot.clear();
        pageSelection.setValue(0);
        pageSelection.setMinValue(0);
        pageSelection.setMaxValue(0);
        pageSelection.setEnabled(false);

    }

    @Override
    public void updateSeries(SlicePlotSeries series) {
        matrixPlot.update(true);
    }

    @Override
    public double[][] getSeriesData(SlicePlotSeries series) {
        return matrixPlot.getSeriesData(series);
    }

    @Override
    public void onSeriesRangeZChanged(SlicePlotSeries s) {
        int max = Math.max(getSeries(0).getNumberOfBinsZ() - 1, 0);
        pageSelection.setMaxValue(max);
        setPageTitle();
    }
    
    @Override
    public Range getAxisRange(AxisId axisId){

        return switch (axisId) {
            case Z -> (getSeries(0)==null) ? null :new Range(0,   Math.max(getSeries(0).getNumberOfBinsZ() - 1, 0));
            default -> matrixPlot.getAxisRange(axisId);
        };
    }    
    
    @Override
    public BufferedImage getSnapshot(Dimension size) {
        return matrixPlot.getSnapshot(size);
    }     
}
