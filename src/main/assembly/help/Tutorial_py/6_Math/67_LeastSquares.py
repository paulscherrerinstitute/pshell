################################################################################################### 
# Example of least squares optimization
# http://commons.apache.org/proper/commons-math/userguide/leastsquares.html
################################################################################################### 

from mathutils import *
from plotutils import * 

[p1,p2] = plot([None, None], [None, None])

################################################################################################### 
#Fitting the quadratic function f(x) = a x2 + b x + c  
################################################################################################### 
x = [ 1.0,  2.0,   3.0,   4.0,   5.0,   6.0,   7.0,   8.0,   9.0,  10.0]
y = [36.0, 66.0, 121.0, 183.0, 263.0, 365.0, 473.0, 603.0, 753.0, 917.0]
num_samples = len(x)
weigths = [ 1.0] * num_samples

p1.getSeries(0).setData(x, y)
p1.getSeries(0)

class Model(MultivariateJacobianFunction):
    def value(self, variables):
        value = ArrayRealVector(num_samples)
        jacobian = Array2DRowRealMatrix(num_samples, 3)        
        for i in range(num_samples):
            (a,b,c) = (variables.getEntry(0), variables.getEntry(1), variables.getEntry(2))   
            model = a*x[i]*x[i] + b*x[i] + c
            value.setEntry(i, model)                                    
            jacobian.setEntry(i, 0,  x[i]*x[i])  # derivative with respect to p0 = a            
            jacobian.setEntry(i, 1,  x[i])       # derivative with respect to p1 = b            
            jacobian.setEntry(i, 2,  1.0)        # derivative with respect to p2 = c        
        return Pair(value, jacobian)

model = Model()
initial = [1.0, 1.0, 1.0]    #parameters = a, b, c
target = [v for v in y]      #the target is to have all points at the positios

(parameters, residuals, rms, evals, iters) = optimize_least_squares(model, target, initial, weigths)

(a,b,c) = parameters

print "A: ",  a , "  B: ",  b, "  C: ",  c
print "RMS: " , rms,  "  evals: "  , evals,  "  iters: "   , iters
for i in range (num_samples):
    print x[i], y[i], poly(x[i], [c,b,a])

plot_function(p1, PolynomialFunction((c,b,a)), "Fit", x)
print "----------------------------------------------------------------------------\n"


################################################################################################### 
#Fitting center of circle of known radius to observed points
################################################################################################### 

radius = 70.0
x = [30.0,  50.0, 110.0, 35.0, 45.0]
y = [68.0,  -6.0, -20.0, 15.0, 97.0]
num_samples = len(x)
weigths = [ 1.0] * num_samples
weigths = [0.1,  0.1, 1.0, 0.1, 1.0]

p2.getSeries(0).setData(x, y)
p2.getSeries(0).setLinesVisible(False)
p2.getSeries(0).setPointSize(4)

# the model function components are the distances to current estimated center,
# they should be as close as possible to the specified radius
class Model(MultivariateJacobianFunction):
    def value(self, variables):
        (cx,cy) = (variables.getEntry(0), variables.getEntry(1))  
        value = ArrayRealVector(num_samples)
        jacobian = Array2DRowRealMatrix(num_samples, 2)
        for i in range(num_samples):
            model = math.hypot(cx-x[i], cy-y[i]) 
            value.setEntry(i, model)            
            jacobian.setEntry(i, 0, (cx - x[i]) / model)  # derivative with respect to p0 = x center                
            jacobian.setEntry(i, 1, (cy - y[i]) / model)  # derivative with respect to p1 = y center
        return Pair(value, jacobian)

model = Model()                    #modeled radius should be close to target radius
initial = [mean(x), mean(y)]       #parameters = cx, cy
target = [radius,] * num_samples   #the target is to have all points at the specified radius from the center

(parameters, residuals, rms, evals, iters) = optimize_least_squares(model, target, initial, weigths)

(cx, cy) = parameters

print "CX: ",  cx , "  CY: ",  cy
print "RMS: " , rms,  "  evals: "  , evals,  "  iters: "   , iters
       
plot_point(p2, cx, cy, size = 5, name = "Fit Center")
plot_circle(p2, cx, cy, radius, width = 1, name = "Fit")
