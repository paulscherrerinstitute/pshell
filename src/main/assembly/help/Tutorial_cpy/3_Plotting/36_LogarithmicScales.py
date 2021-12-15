################################################################################################### 
# Displaying logarithmic scales
################################################################################################### 

p = plot([10,500,1000,5000,1000], xdata = [0.1,1,10,100,1000, 10000])[0]
p.getAxis(AxisId.X).setLogarithmic(True)
p.getAxis(AxisId.Y).setRange(1, 100000)
p.getAxis(AxisId.Y).setLogarithmic(True)




