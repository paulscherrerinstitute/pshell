///////////////////////////////////////////////////////////////////////////////////////////////////
// Example of least squares optimization
// http://commons.apache.org/proper/commons-math/userguide/leastsquares.html
/////////////////////////////////////////////////////////////////////////////////////////////////// 

run("mathutils")
run("plotutils")


plots = plot([null, null], [null, null])
p1=plots[0]
p2=plots[1]

/////////////////////////////////////////////////////////////////////////////////////////////////// 
//Fitting the quadratic function f(x) = a x2 + b x + c  
///////////////////////////////////////////////////////////////////////////////////////////////////
var fx = [ 1.0,  2.0,   3.0,   4.0,   5.0,   6.0,   7.0,   8.0,   9.0,  10.0]
var fy = [36.0, 66.0, 121.0, 183.0, 263.0, 365.0, 473.0, 603.0, 753.0, 917.0]
num_samples = fx.length
weigths = []; size=num_samples; while(size--) weigths.push(1.0)    

p1.getSeries(0).setData(fx, fy)
p1.getSeries(0)


var Model = Java.extend(MultivariateJacobianFunction)
var model = new Model() {	    
        value: function (variables) {
            value = new ArrayRealVector(num_samples)
            jacobian = new Array2DRowRealMatrix(num_samples, 3)        
            for (var i=0; i< num_samples; i++){
            	a=variables.getEntry(0)
            	b=variables.getEntry(1)
            	c=variables.getEntry(2)
                model = a*fx[i]*fx[i] + b*fx[i] + c
                value.setEntry(i, model)                                    
                jacobian.setEntry(i, 0,  fx[i]*fx[i])  // derivative with respect to p0 = a            
                jacobian.setEntry(i, 1,  fx[i])       // derivative with respect to p1 = b            
                jacobian.setEntry(i, 2,  1.0)        // derivative with respect to p2 = c        
            }
            return new Pair(value, jacobian)
        },
    }

initial = [1.0, 1.0, 1.0]    //parameters = a, b, c
target = fy.slice(0)      //the target is to have all points at the positios

ret = optimize_least_squares(model, target, initial, weigths)

parameters = ret[0]
residuals = ret[1]
rms = ret[2]
evals = ret[3]
iters = ret[4]

a = parameters[0]
b = parameters[1]
c = parameters[2]

print ("A: " +  a + "  B: "+  b, "  C: "+  c)
print ("RMS: " + rms+  "  evals: "  + evals+  "  iters: "   + iters)
for (var i=0; i<num_samples; i++){
    print (fx[i] + " "  + fy[i] + " " +  poly(fx[i] + " "  +  [c,b,a]))
}

plot_function(p1, new PolynomialFunction([c,b,a]), "Fit", fx)
print ("----------------------------------------------------------------------------\n")


///////////////////////////////////////////////////////////////////////////////////////////////////
//Fitting center of circle of known radius to observed points
/////////////////////////////////////////////////////////////////////////////////////////////////// 

radius = 70.0
fx = [30.0,  50.0, 110.0, 35.0, 45.0]
fy = [68.0,  -6.0, -20.0, 15.0, 97.0]
num_samples = fx.length
weigths = [0.1,  0.1, 1.0, 0.1, 1.0]

p2.getSeries(0).setData(fx, fy)
p2.getSeries(0).setLinesVisible(false)
p2.getSeries(0).setPointSize(4)

// the model function components are the distances to current estimated center,
// they should be as close as possible to the specified radius
var Model = Java.extend(MultivariateJacobianFunction)
var model = new Model() {	    
        value: function (variables) {
        	cx=variables.getEntry(0)
        	cy=variables.getEntry(1)
            value = new ArrayRealVector(num_samples)
            jacobian = new Array2DRowRealMatrix(num_samples, 2)            
            for (var i=0; i< num_samples; i++){
                var mod = hypot(cx-fx[i], cy-fy[i])                 
                value.setEntry(i, mod)            
                jacobian.setEntry(i, 0, (cx - fx[i]) / mod)  // derivative with respect to p0 = x center                
                jacobian.setEntry(i, 1, (cy - fy[i]) / mod)  // derivative with respect to p1 = y center
            }
            return new Pair(value, jacobian)
        },
    }        

initial = [mean(fx), mean(fy)]       //parameters = cx, cy
target = []; size=num_samples; while(size--) target.push(radius); //the target is to have all points at the specified radius from the center

ret = optimize_least_squares(model, target, initial, weigths)
parameters = ret[0]
residuals = ret[1]
rms = ret[2]
evals = ret[3]
iters = ret[4]
cx = parameters[0]
cy = parameters[1]

print ("CX: " + cx + "  CY: "+  cy)
print ("RMS: " + rms+  "  evals: "  + evals,  "  iters: "   + iters)       
plot_point(p2, cx, cy, size = 5, color = null, name = "Fit Center")
plot_circle(p2, cx, cy, radius, width = 1, color = null, name = "Fit")
