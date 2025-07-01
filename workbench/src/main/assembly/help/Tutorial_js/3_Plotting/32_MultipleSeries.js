///////////////////////////////////////////////////////////////////////////////////////////////////
// Advanced plotting examples
///////////////////////////////////////////////////////////////////////////////////////////////////


//2 series in the same plot
var xdata = [10,20,30,40,50,60]
var p = plot(null, name="Data 1")[0]
p.addSeries(new LinePlotSeries("Data2"))
p.getSeries(0).setData(xdata, [1,2,3,4,5,6])    
p.getSeries(1).setData(xdata, [6,5,4,3,2,1])    
p.addMarker(35.0, null, "This is the answer", Color.BLACK)


//2 series in the same plot, setting range & appending point by point
var p = plot(null,name="Data 1", title = "Plot 2")[0]
p.getAxis(AxisId.X).setRange(0.0,80.0)
p.getAxis(AxisId.Y).setRange(0.0,7.0)
p.addSeries(new LinePlotSeries("Data2"))
for (var i = 0; i< xdata.length; i++){
    p.getSeries(0).appendData(xdata[i], i)    
    p.getSeries(1).appendData(xdata[i], 1.0/(i+1))    
}






