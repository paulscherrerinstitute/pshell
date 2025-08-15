################################################################################################### 
# Example creating error plots
################################################################################################### 


[py, px, pxy]=plot([None,None,None],["y","x", "xy"])

#Y error plot
py.setStyle(LinePlotStyle.ErrorY)
py.setLegendVisible(True)

sy1 = LinePlotErrorSeries("F1")
py.addSeries(sy1)
sy1.appendData(1.0, 10.0, 9.0, 11.0)
sy1.appendData(10.0, 6.1, 4.34, 7.54)
sy1.appendData(17.8, 4.5, 3.1, 5.8)
#One can define error instead of min/max (appendData(x, y, error))
sy2 = LinePlotErrorSeries("F2")
py.addSeries(sy2)
sy2.setLinesVisible(False)
sy2.appendData(3.0, 7.0, 2.0);
sy2.appendData(13.0, 13.0, 2.0);
sy2.appendData(24.0, 16.1, 1.0);

#X error plot
px.setStyle(LinePlotStyle.ErrorX)
px.setLegendVisible(True)
sx = LinePlotErrorSeries("F3")
px.addSeries(sx)
sx.appendData(1.0, 10.0, 0.5, 1.5)
sx.appendData(10.0, 6.1, 9.0, 10.3)
sx.appendData(17.8, 4.5, 17.0, 18.0)
#One can define error instead of min/max (appendData(x, y, error))
sx2 = LinePlotErrorSeries("F4")
px.addSeries(sx2)
sx2.setLinesVisible(False)
sx2.appendData(1.0, 3.0, 1.0)
sx2.appendData(10.0, 5.1, 1.0)
sx2.appendData(17.8, 7.5, 0.5)

#XY error plot
pxy.setStyle(LinePlotStyle.ErrorXY)
pxy.setLegendVisible(True)
sxy = LinePlotErrorSeries("F5")
pxy.addSeries(sxy)
sxy.appendData(1.0,0.5,1.5, 10.0, 9.0, 11.0)
sxy.appendData(10.0,9.0, 11.0, 6.1, 4.34, 7.54)
sxy.appendData(17.8, 17.0, 18.0, 4.5, 3.1, 5.8)
#One can define error instead of min/max (appendData(x, y, error))
sxy2 = LinePlotErrorSeries("F6")
pxy.addSeries(sxy2)
sxy2.appendData(3.0, 7.0, 0.5);
sxy2.appendData(13.0, 13.0, 0.5);
sxy2.appendData(24.0, 16.1, 0.5);