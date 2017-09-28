///////////////////////////////////////////////////////////////////////////////////////////////////
// Function fitting and peak search with mathutils.py
///////////////////////////////////////////////////////////////////////////////////////////////////


run("mathutils")


start = 0
end = 10
step_size = 0.1

result= lscan(ao1,ai1,start,end,[step_size,],0.01)

readable = result.getReadable(0)
positions = result.getPositions(0)

function get_function_data(func, start, end, resolution){
    ret = []
    for (var x=start; x<=end; x+=resolution){
        fit_polinomial.push(func.value(x))
    }
}


pars_polynomial = fit_polynomial(readable, positions, 6)
     //(a0, a1, a2, a3, a4, a5, a6) 
fitted_polynomial_function = new PolynomialFunction(pars_polynomial)
print (pars_polynomial)

pars_gaussian = fit_gaussian(readable, positions)
normalization = pars_gaussian[0]
mean_val = pars_gaussian[1]
sigma = pars_gaussian [2]
fitted_gaussian_function = new Gaussian(normalization, mean_val, sigma)
print (normalization + " " +  mean_val + " " + sigma)

pars_harmonic= fit_harmonic(readable, positions)
amplitude = pars_harmonic[0]
angular_frequency = pars_harmonic[1]
phase = pars_harmonic[2]
fitted_harmonic_function = new HarmonicOscillator(amplitude, angular_frequency, phase)
print (amplitude+ " " + angular_frequency+ " " + phase) 


resolution = step_size/100
fit_polinomial = []
fit_gaussian = []
fit_harmonic = []
for (var x=start; x<=end; x+=resolution){
    fit_polinomial.push(fitted_polynomial_function.value(x))
    fit_gaussian.push(fitted_gaussian_function.value(x))
    fit_harmonic.push(fitted_harmonic_function.value(x))
}
fx = range(start, end+resolution, resolution)

peaks = calculate_peaks(fitted_polynomial_function, start, end)

plots = plot([readable, fit_polinomial, fit_gaussian, fit_harmonic] , 
    ["data", "polinomial", "gaussian", "harmonic"], xdata = [positions,fx,fx,fx], ydata=undefined, title="Data")

for (var p in peaks){    
    print ("Max: " + p)
    plots[0].addMarker(p, null, "Max=" + Math.round(p,2), Color.LIGHT_GRAY)
}    

plots[0].addMarker(mean_val, null, "Mean=" + Math.round(mean_val,2), Color.LIGHT_GRAY)