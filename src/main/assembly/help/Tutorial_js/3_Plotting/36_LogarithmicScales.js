///////////////////////////////////////////////////////////////////////////////////////////////////
// Displaying logarithmic scales
///////////////////////////////////////////////////////////////////////////////////////////////////

p = plot([10,500,1000,5000,1000], name = undefined, xdata = [0.1,1,10,100,1000, 10000])[0]
p.getAxis(AxisId.X).logarithmic = true
p.getAxis(AxisId.Y).setRange(1, 100000)
p.getAxis(AxisId.Y).logarithmic = true




