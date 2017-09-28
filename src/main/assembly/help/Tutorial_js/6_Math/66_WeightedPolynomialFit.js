///////////////////////////////////////////////////////////////////////////////////////////////////
// Example if fitting with weights
/////////////////////////////////////////////////////////////////////////////////////////////////// 


run("mathutils")
run("plotutils")


fx = [ 1.0,  2.0,   3.0,   4.0,   5.0,   6.0,   7.0,   8.0,   9.0,  10.0]
fy = [36.0, 66.0, 121.0, 183.0, 263.0, 365.0, 473.0, 603.0, 753.0, 917.0]
num_samples = fx.length

p=plot(fy, "Observed Data", xdata=fx)[0]
p.setLegendVisible(true)

pars_polynomial=  fit_polynomial(fy, fx, 1, null)
func = new PolynomialFunction(pars_polynomial)
plot_function(p, func, "1st order", fx)

var w = []; size=num_samples; while(size--) w.push(1.0);
w[5]=20.0 
pars_polynomial=  fit_polynomial(fy, fx, 1, null, w)
func = new PolynomialFunction(pars_polynomial)
plot_function(p, func, "1st order Weighted: " + w , fx)
    
pars_polynomial=  fit_polynomial(fy, fx, 2, null, w)
func = new PolynomialFunction(pars_polynomial)
plot_function(p, func, "2nd order" + w , fx)
for (var xp in fx){
    print (fx[xp] + " " + poly(fx[xp], pars_polynomial))
}

print ("")
w = [ 10.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 10.0] 
pars_polynomial=  fit_polynomial(fy, fx, 2, null, w)
func = new PolynomialFunction(pars_polynomial)
plot_function(p, func, "2n order Weighted: " + w , fx)
for (var xp in fx){
    print (fx[xp] + " " + poly(fx[xp], pars_polynomial))
}
