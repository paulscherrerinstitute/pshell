################################################################################################### 
# Example if fitting with weights
################################################################################################### 

from mathutils import *
from plotutils import * 


x = [ 1.0,  2.0,   3.0,   4.0,   5.0,   6.0,   7.0,   8.0,   9.0,  10.0]
y = [36.0, 66.0, 121.0, 183.0, 263.0, 365.0, 473.0, 603.0, 753.0, 917.0]
num_samples = len(x)

p=plot(y, "Observed Data", xdata=x)[0]
p.setLegendVisible(True)

pars_polynomial=  fit_polynomial(y, x, 1, None)
function = PolynomialFunction(pars_polynomial)
plot_function(p, function, "1st order", x)


w = [ 1.0] * num_samples
w[5]=20.0 
pars_polynomial=  fit_polynomial(y, x, 1, None, w)
function = PolynomialFunction(pars_polynomial)
plot_function(p, function, "1st order Weighted: " + str(w) , x)
    
pars_polynomial=  fit_polynomial(y, x, 2, None, w)
function = PolynomialFunction(pars_polynomial)
plot_function(p, function, "2nd order" + str(w) , x)
for xp in x:
    print xp, poly(xp, pars_polynomial)

print ""
w = [ 10.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 10.0] 
pars_polynomial=  fit_polynomial(y, x, 2, None, w)
function = PolynomialFunction(pars_polynomial)
plot_function(p, function, "2nd order Weighted: " + str(w) , x)
for xp in x:
    print xp, poly(xp, pars_polynomial)

