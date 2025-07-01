# Math Utilities

In order to cope with lack of NumPy on Jython, PShell includes Apache's Commons Math library 
(http://commons.apache.org/proper/commons-math/).

All functionality available in org.apache.commons.math3 can be accessed from the scripts.
The direct access to this library is not straightforward in Python though.
The module mathutils.py implements a facade to this library, in order to facilitate the use of 
its most common features.

The functions in  mathutils.py are not built-in functions: the module must be imported such as:


```
from mathutils import *

```

The tutorial provides examples on the use of mathutils.


The main functions available are:

 * Derivative and interpolation:
    - __deriv__: Calculate derivative of UnivariateFunction, array or list.
    - __integrate__: Integrate UnivariateFunction, array or list.
    - __trapz__: Integrate an array or list using the composite trapezoidal rule.
    - __interpolate__: Interpolate data array or list to a UnivariateFunction.    

 * Fitting and peak search:
    - __fit_polynomial__: Fits data vector as a polynomial.
    - __fit_gaussian__: Fits data vector as a gaussian.
    - __fit_gaussian_offset__:Fits data vector as a gaussian with constant background.
    - __fit_gaussian_linear__:Fits data vector as a gaussian with linear background.
    - __fit_harmonic__: Fits data vector as an harmonic.
    - __estimate_peak_indexes__: Estimation of peaks in an array by ordering local maxima according to given criteria.
    - __fit_gaussians__: Fits data on multiple gaussians on the given peak indexes.
    - __calculate_peaks__: Calculate peaks of a DifferentiableUnivariateFunction in a given range by finding the roots of the derivative.

 * Least squares problem:
    - __optimize_least_squares__: Fits a parametric model to a set of observed values by minimizing a cost function.

 * Transforms:
    - __fft__: Calculate the FFT of a vector, padding to the next power of 2 elements.
    - __ffti__: Calculates the Inverse FFT of a vector, padding to the next power of 2 elements. 
    - __get_real__: Calculate real part of a complex vector. 
    - __get_imag__: Calculate imaginary part of a complex vector. 
    - __get_modulus__: Calculate modulus of a complex vector. 
    - __get_phase__: Calculate phase of a complex vector. 

 * Tools:
    - __get_values__: Computes list of values of a UnivariateFunction

__Note__: Check the PyDocs of the methods for parameters and return types:

```
        help(function_name)

```