# Plot

The __plot__ builtin function return a list of __Plot__ objects.The __Plot__  and __PlotSeries__ class 
methods enable more advanced plotting features. 

As an example the following code produces a plot with a second series, sets fixed ranges to both axis
and adds a marker:

```
    x = [10,20,30,40,50,60]
    p = plot(None,name="Data 1")[0]
    p.getAxis(p.AxisId.X).setRange(0.0,80.0)
    p.getAxis(p.AxisId.Y).setRange(0.0,1.0)
    p.addSeries(LinePlotSeries("Data2"))
    p.getSeries(0).setData(x, [0.1,0.2,0.3,0.4,0.5,0.6])    
    for i in range(len(x)):
        p.getSeries(1).appendData(x[i], 1.0/(i+1))    
    p.addMarker(40.0, None, "This is the answer", None)
```

<br>

Methods:
 * void update(boolean deferred)
 * void setUpdatesEnabled(boolean value)
 * boolean isUpdatesEnabled()
 * void setTitle(String title)
 * String getTitle()
 * void setTitleFont(Font font)
 * Font getTitleFont()
 * setQuality(Quality quality)
 * Quality getQuality()
 * void saveData(String filename)
 * BufferedImage getSnapshot()
 * void saveSnapshot(String filename, String format)
 * void copy()
 * void addSeries(PlotSeries series)
 * void addSeries(PlotSeries[] series)
 * removeSeries(PlotSeries series)
 * PlotSeries getSeries(String name)
 * PlotSeries getSeries(int index)
 * int getNumberOfSeries()
 * void updateSeries(PlotSeries series)
 * void requestSeriesUpdate(PlotSeries series)
 * double[][] getSeriesData(PlotSeries series)
 * PlotSeries[] getAllSeries()
 * void clear(): remove all series
 * Axis getAxis(AxisId id)
 * Object addMarker(double val, AxisId axis, String label, Color color)
 * Object addIntervalMarker(double start, double end, AxisId axis, String label, Color color)
 * void removeMarker(Object marker)
 * Object addText(double x, double y, String label)
 * void removeText(Object text)



Enums:

 * Quality
    - Low
    - Medium
    - High
    - Maximum

 * AxisId:
    - X
    - Y
    - Y2
    - Z

<br>


# PlotSeries

Methods:
 * Plot getPlot()
 * String getName()
 * void setName(String name) 
 * void clear() 

<br>

# LinePlotSeries extends PlotSeries

Methods:
 * LinePlotSeries(String name)
 * LinePlotSeries(String name, Color color)
 * LinePlotSeries(String name, Color color, int axisY)
 * Color getColor()
 * void setColor(Color color)
 * void setLinesVisible(boolean value)
 * boolean getLinesVisible()
 * void setLineWidth(int value)
 * int getLineWidth() 
 * void setPointsVisible(boolean value)
 * boolean getPointsVisible()
 * public void setPointSize(int value)
 * public int getPointSize()
 * int getAxisY()
 * void setData(double[] y)
 * void setData(double[] x, double[] y)
 * void appendData(double x, double y)
 * double[] getX()
 * double[] getY()
 * int getCount()
 * int getMaxItemCount()
 * void setMaxItemCount(int value)
 * double getAverage() 
 * double[] getMinimum() 
 * double[] getMaximum() 
 * double[][] getDerivative()
 * double[][] getIntegral()


# MatrixPlotSeries extends PlotSeries

Methods:
 * MatrixPlotSeries(String name, double minX, double maxX, int nX, double minY, double maxY, int nY) 
 * void setData(double[][] data) 
 * setData(double[][] data, double[][] x, double[][] y)
 * void getNumberOfBinsX(int bins)
 * int getNumberOfBinsX()
 * void setNumberOfBinsY(int bins)
 * int getNumberOfBinsY()
 * double getBinWidthX()
 * void setBinWidthX(double value)
 * double getBinWidthY()
 * void setBinWidthY(double value)
 * void setRangeX(double minX, double maxX) 
 * void setRangeY(double minY, double maxY) 
 * double getMinX()
 * double getMaxX()
 * double getMinY()
 * double getMaxY()
 * void appendData(double x, double y, double z) 
 * double[][] getData() 
 * double[][] getX() 
 * double[][] getY() 
 * boolean contains(int indexX, int indexY)
 * double[] minMaxZValue() 