################################################################################################### 
# Example changing plot styles
################################################################################################### 


data = [0,3,5,1,3,5,3,4,3,2,1,0]
[p1, p2, p3] = plot([data, data, data], ["normal", "step", "spline"])
p2.setStyle(LinePlotStyle.Step)
p3.setStyle(LinePlotStyle.Spline)