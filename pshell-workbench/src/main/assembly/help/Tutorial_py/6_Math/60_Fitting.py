################################################################################################### 
# Function fitting and peak search with mathutils.py
################################################################################################### 


from mathutils import fit_polynomial,fit_gaussian, fit_harmonic, calculate_peaks
from mathutils import PolynomialFunction, Gaussian, HarmonicOscillator
import math

start = 0
end = 10
step_size = 0.1

result= lscan(ao1,ai1,start,end,[step_size,],0.01)

readable = result.getReadable(0)
positions = result.getPositions(0)

pars_polynomial = (a0, a1, a2, a3, a4, a5, a6) = fit_polynomial(readable, positions, 6)
fitted_polynomial_function = PolynomialFunction(pars_polynomial)
print pars_polynomial 

(normalization, mean_val, sigma) = fit_gaussian(readable, positions)
fitted_gaussian_function = Gaussian(normalization, mean_val, sigma)
print (normalization, mean_val, sigma)

(amplitude, angular_frequency, phase) = fit_harmonic(readable, positions)
fitted_harmonic_function = HarmonicOscillator(amplitude, angular_frequency, phase)
print (amplitude, angular_frequency, phase) 


resolution = step_size/100
fit_polinomial = []
fit_gaussian = []
fit_harmonic = []
for x in frange(start,end,resolution, True):
    fit_polinomial.append(fitted_polynomial_function.value(x))
    fit_gaussian.append(fitted_gaussian_function.value(x))
    fit_harmonic.append(fitted_harmonic_function.value(x))
x = frange(start, end+resolution, resolution)


peaks = calculate_peaks(fitted_polynomial_function, start, end)

plots = plot([readable, fit_polinomial, fit_gaussian, fit_harmonic] , 
    ["data", "polinomial", "gaussian", "harmonic"], xdata = [positions,x,x,x], title="Data")

for p in peaks:    
    print "Max: " + str(p)
    plots[0].addMarker(p, None, "Max=" + str(round(p,2)), Color.LIGHT_GRAY)

plots[0].addMarker(mean_val, None, "Mean=" + str(round(mean_val,2)), Color.LIGHT_GRAY)