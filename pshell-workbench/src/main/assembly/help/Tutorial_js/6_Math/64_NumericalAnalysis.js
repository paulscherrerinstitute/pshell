///////////////////////////////////////////////////////////////////////////////////////////////////
// Use of numerical analysis functions in mathutils
///////////////////////////////////////////////////////////////////////////////////////////////////

run("mathutils")

//interpolation
var fy = [0, 1, 4,10,50,25,12, 5, 3, 0] 
var fx = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
types = ["linear", "cubic",  "akima", "loess", "neville", "newton"]
functions = types.map(function(t) {return interpolate(fy,fx,t)})    

plot_x = range (0,9,0.01)
values = functions.map(function(f) {return get_values(f,plot_x)})    
plots=plot(values,types,plot_x)

//derivative
for (var i=0; i<types.length; i++){
    try{
        d = deriv(functions[i])
        plots[i].addSeries(new LinePlotSeries("derivative"))
        plots[i].getSeries(1).setData(plot_x, get_values(d,plot_x))    
    } catch(ex){
        //not differentiable
    }
}

//integration
for (var i=0; i<types.length; i++){
    s = integrate(functions[i],fx)
    plots[i].addMarker(fx[fx.length-1]+0.1, null, "Integral=" + Math.round(s,4), plots[i].background).setLabelPaint(Color.BLACK)
}

//Direct calculation on arrays:
print ("Deriv (linear interpolation): ", deriv(fy,fx))
print ("Deriv (cubic interpolation): ", deriv(fy, fx, "cubic"))
print ("Integral (linear interpolation): ", integrate(fy, null, fx, "cubic"))
print ("Integral (cubic interpolation): ", integrate(fy, null, fx, "cubic"))
print ("Integral (linear interpolation in range [1,5]): ", integrate(fy, [1,5], fx))