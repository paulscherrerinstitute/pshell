package ch.psi.pshell.plotter;

import ch.psi.pshell.imaging.Colormap;
import ch.psi.pshell.plot.LinePlot;
import ch.psi.pshell.plot.MatrixPlot;
import ch.psi.pshell.plot.Plot;
import ch.psi.utils.Arr;
import ch.psi.utils.ProcessFactory;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 */
public class ClientTest {
    
    public ClientTest() {
    }
    
    static Process serverProcess;
            
    @BeforeClass
    public static void setUpClass() throws InterruptedException {
       serverProcess = ProcessFactory.createProcess(View.class);
       Thread.sleep(1000);
    }
    
    @AfterClass
    public static void tearDownClass() {
        serverProcess.destroy();
    }
    
    @Test
    public void test() throws InterruptedException {
        
        Client pc = new Client("localhost", 3000);
        String context = "Remote";
        Plotter pm = pc.getProxy();
        pm.clearPlots(context);
        String[] contexts = pm.getContexts();
        pm.setStatus(context, "Running remote");
        pm.setProgress(context, 0.2);
        pm.setContextAttrs(context, Plot.Quality.High, ch.psi.pshell.plotter.PlotLayout.Vertical);
        pm.addLinePlot(context, "Plot1", null);
        pm.addLinePlot(context, "Plot2", LinePlot.Style.ErrorY);
        pm.addMatrixPlot(context, "Plot3",  MatrixPlot.Style.Normal, null);
        pm.addMatrixPlot(context, "Plot4", null, null);        
        pm.addTable(context, "Table 1");
        pm.addLineSeries(context + "/0", "Series1", "red", 1, null, null, null);
        pm.addLineSeries(context + "/1", "Series2", "blue", 1, null, null, null);
        pm.addMatrixSeries(context + "/2", "Series3", 0.0, 100.0, 20, 10.0, 20.0, 10);
        pm.addMatrixSeries(context + "/Plot4", "Series4", null, null, null, null, null, null);
        pm.setProgress(context, 0.4);
        double[] x = Arr.indexesDouble(100);
        double[] y = Arr.indexesDouble(100);
        y[10] = 40;
        y[20] = 80;
        //pm.setLineSeriesData(context+"/0/0", x, y, null, null);
        for (int i = 0; i < x.length; i++) {
            pm.appendLineSeriesData(context + "/0/0", x[i], y[i], 0);
            Thread.sleep(1);
        }
        double[] e = Arr.onesDouble(100);
        pm.setLineSeriesData(context + "/1/0", x, y, e, null);
        double[][] data = new double[10][20];
        data[2][1] = 10;
        data[5][4] = 20;
        data[8][7] = 30;
        //pm.setMatrixSeriesData(context+"/2/0", data, null, null);            
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                pm.appendMatrixSeriesData(context + "/2/0", j * 5.263, i * 1.111 + 10, data[i][j]);
                Thread.sleep(1);
            }
        }
        pm.setMatrixSeriesData(context + "/Plot4/Series4", data, null, null);
        
        pm.addMarker(context + "/0/X", 50.0, "Test Marker", "128,0,0");
        pm.addText(context + "/1", 20.0, 30.0, "Test Text", "0, 0, 128");
        pm.setTableData(context + "/Table 1", new String[]{"Variable", "Value"}, new String[][]{
            new String[]{"Var1", "10.0"},
            new String[]{"Var2", "20.0"},});
        pm.setProgress(context, 1.0);
        pm.setProgress(context, null);

        byte[] arr = pm.getPlotSnapshot(context + "/0", "png", 200, 100);

        //Files.write(Paths.get("test.png"), arr);
        
        
        pm.addTimePlot(context, "Plot 5", true, 60000, true);
        pm.addTimeSeries(context + "/5", "Series5", "green", 1);
        pm.addTimeSeries(context + "/5", "Series6", "blue", 2);
        for (int i=0; i<100; i++){
            pm.appendTimeSeriesData(context + "/5/0" , null, (double)i);
            pm.appendTimeSeriesData(context + "/5/1" , null, 10000.0-i);
            Thread.sleep(1);
        }
        pm.setTimePlotAttrs(context + "/5", true, 1500, true);
        pm.setTimeSeriesAttrs(context + "/5/0", "orange");                
        
        pm.add3dPlot(context, "Plot6", Colormap.Flame);
        pm.add3dSeries(context + "/6", "Series7", null, null, null, null, null, null, null, null, null );
        //pm.add3dSeries(context + "/6", "Series7", 0.0, 100.0, 20, 10.0, 20.0, 10, null,null, null);
        //pm.add3dSeries(context + "/6", "Series7", 0.0, 100.0, 20, 10.0, 20.0, 10, null,null, 20);
        //pm.add3dSeries(context + "/6", "Series7", 0.0, 100.0, 20, 10.0, 20.0, 10, 10.0, 100.0, 20);
        for (int i=0; i<20; i++){
            data[0][i] = i+1;
            pm.append3dSeriesData(context + "/6/0", data);            
            Thread.sleep(1);
        }                
                
        
    }
    
}
