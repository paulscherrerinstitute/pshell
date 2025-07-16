package ch.psi.pshell.plotter;


import static org.junit.Assert.*;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.utils.Arr;
import org.junit.Test;

/**
 *
 */
public class ViewTest {
    
    public ViewTest() {
    }

    @Test
    public void test() {                                         
        View view = new View();        
        view.setVisible(true);
        Plotter pm = view.pm;
        String context = "Test";
        pm.clearPlots(context);
        pm.setProgress(context, 0.2);
        pm.addLinePlot(context, "Plot1", null);            
        pm.addLinePlot(context, "Plot2", LinePlot.Style.ErrorY);
        pm.addMatrixPlot(context, "Plot3", null, null);
        pm.addMatrixPlot(context, "Plot4", null, null);
        pm.addTable(context, "Table 1");
        pm.addLineSeries(context+"/0", "Series1", "red", 1, null, null, null);
        pm.addLineSeries(context+"/1", "Series2", "blue", 1, null, null, null);
        byte[] arr = pm.getPlotSnapshot(context + "/0", "png", null, null);
        pm.addMatrixSeries(context+"/2", "Series3", 0.0, 100.0, 20, 10.0, 20.0, 10);
        pm.addMatrixSeries(context+"/Plot4", "Series4", null, null, null, null, null, null);
        pm.setProgress(context, 0.4);
        double[]  x = Arr.indexesDouble(100);
        double[]  y = Arr.indexesDouble(100); y[10] = 40; y[20] = 80;
        pm.setLineSeriesData(context+"/0/0", x, y, null, null);
        double[]  e = Arr.onesDouble(100);
        pm.setLineSeriesData(context+"/1/0", x, y, e, null);
        double[][] data = new double[10][20]; data[2][1] = 10; data[5][4] = 20; data[8][7] = 30;
        pm.setMatrixSeriesData(context+"/2/0", data, null, null);            
        pm.setMatrixSeriesData(context+"/Plot4/Series4", data, null, null);            
        pm.setTableData(context + "/Table 1", null, new String[][]{
            new String[]{"Var1", "10.0"},
            new String[]{"Var2", "20.0"},
        });

        pm.addMarker(context+"/0/X",50.0,"Test Marker",  "128,0,0");
        pm.addText(context+"/1",20.0, 30.0,"Test Text",  "0,0,128");
        pm.setProgress(context, null);
    }       
    
    
}
