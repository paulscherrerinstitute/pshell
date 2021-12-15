################################################################################################### 
# Use of numerical analysis functions in mathutils
###################################################################################################

from mathutils import *
import java.awt.Color as Color

#interpolation
y = [0, 1, 4,10,50,25,12, 5, 3, 0] 
x = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
interp_types = "linear", "cubic",  "akima", "loess", "neville", "newton"
functions = [interpolate(y,x,t) for t in interp_types]

plot_x = frange (0,9,0.01)
values = [get_values(f,plot_x) for f in functions]
plots=plot(values,interp_types,plot_x)

#derivative
for i in range(len(interp_types)):
    try:
        d = deriv(functions[i])
        plots[i].addSeries(LinePlotSeries("derivative"))
        plots[i].getSeries(1).setData(plot_x, get_values(d,plot_x))    
    except:
        #not differentiable
        pass

#integration
for i in range(len(interp_types)):
    s = integrate(functions[i],x)
    plots[i].addMarker(x[-1]+0.1, None, "Integral=" + str(round(s,4)), plots[i].background).setLabelPaint(Color.BLACK)


#Direct calculation on arrays:
print "Deriv (linear interpolation): ", deriv(y,x)
print "Deriv (cubic interpolation): ", deriv(y, x, "cubic")
print "Integral (linear interpolation): ", integrate(y, None, x, "cubic")
print "Integral (cubic interpolation): ", integrate(y, None, x, "cubic")
print "Integral (linear interpolation in range [1,5]): ", integrate(y, [1,5], x)
