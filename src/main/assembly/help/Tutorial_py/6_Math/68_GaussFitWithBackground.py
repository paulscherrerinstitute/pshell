################################################################################################### 
# Fit a gaussian with background  
################################################################################################### 

from mathutils import *
from plotutils import * 

xdata = [ 1.0,  2.0,   3.0,   4.0,   5.0,   6.0,   7.0,   8.0,   9.0,  10.0, 11.0, 12.0]
ydata = [24.0, 36.0, 66.0, 121.0, 474.0, 989.0, 357.0, 175.0,  50.0,  40.0,  30.0, 22.0]
weights = [ 1.0] * len(xdata)
p = plot(ydata, "Data" , xdata)[0]
p.setLegendVisible(True)


################################################################################################### 
#Fitting the gaussian function with offset f(x) = a + b * exp(-(pow((x - c), 2) / (2 * pow(d, 2))))
################################################################################################### 
(off, amp, com, sigma) = fit_gaussian_offset(ydata, xdata, None, weights)
f = Gaussian(amp, com, sigma)
gauss = [f.value(i)+off for i in xdata]
s = LinePlotSeries("Fit with offset")    
p.addSeries(s)
s.setData(xdata, gauss)
error=0
for i in range(len(ydata)) : error += abs(ydata[i]-gauss[i])
print "\nFit with offset:\off: ", off , "  amp: ",  amp, "  com: ",  com, "  sigma: ",  sigma, "  error: ",  error


################################################################################################### 
#Fitting the gaussian function with linear background f(x) = a * x +  b  + c * exp(-(pow((x - d), 2) / (2 * pow(e, 2)))) 
################################################################################################### 
(a, b, amp, com, sigma) = fit_gaussian_linear(ydata, xdata, None, weights)
f = Gaussian(amp, com, sigma)
gauss = [f.value(i) + a*i + b for i in xdata]
s = LinePlotSeries("Fit with lin back")    
p.addSeries(s)
s.setData(xdata, gauss)
error=0
for i in range(len(ydata)) : error += abs(ydata[i]-gauss[i])
print "\nFit with linear back:\a: ", a , "  b: ",  b , "  amp: ",  amp, "  com: ",  com, "  sigma: ",  sigma, "  error: ",  error


################################################################################################### 
#Fitting a normal  gaussian function f(x) = c * exp(-(pow((x - d), 2) / (2 * pow(e, 2)))) 
################################################################################################### 
(amp, com, sigma) = fit_gaussian(ydata, xdata, None, weights)
f = Gaussian(amp, com, sigma)
gauss = [f.value(i) for i in xdata]
s = LinePlotSeries("Normal Fit")    
p.addSeries(s)
s.setData(xdata, gauss)
error=0
for i in range(len(ydata)) : error += abs(ydata[i]-gauss[i])
print "\nNormal fit:\amp: ",  amp, "  com: ",  com,  sigma, "  error: ",  error
