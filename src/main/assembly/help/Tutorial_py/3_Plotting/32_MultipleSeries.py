################################################################################################### 
# Advanced plotting examples
################################################################################################### 


#2 series in the same plot
x = [10,20,30,40,50,60]
p = plot(None,name="Data 1")[0]
p.addSeries(LinePlotSeries("Data2"))
p.getSeries(0).setData(x, [1,2,3,4,5,6])    
p.getSeries(1).setData(x, [6,5,4,3,2,1])    
p.addMarker(35.0, None, "This is the answer", Color.BLACK)


#2 series in the same plot, setting range & appending point by point
p = plot(None,name="Data 1", title = "Plot 2")[0]
p.getAxis(p.AxisId.X).setRange(0.0,80.0)
p.getAxis(p.AxisId.Y).setRange(0.0,7.0)
p.addSeries(LinePlotSeries("Data2"))
for i in range(len(x)):
    p.getSeries(0).appendData(x[i], i)    
    p.getSeries(1).appendData(x[i], 1.0/(i+1))    






