///////////////////////////////////////////////////////////////////////////////////////////////////
//  Facade to Apache Commons Math
///////////////////////////////////////////////////////////////////////////////////////////////////

importClass(java.util.List)
importClass(java.lang.Class)

FastMath = Java.type('org.apache.commons.math3.util.FastMath')
Pair = Java.type('org.apache.commons.math3.util.Pair')
Complex = Java.type('org.apache.commons.math3.complex.Complex')

DifferentiableUnivariateFunction = Java.type('org.apache.commons.math3.analysis.DifferentiableUnivariateFunction')
Gaussian = Java.type('org.apache.commons.math3.analysis.function.Gaussian')
HarmonicOscillator = Java.type('org.apache.commons.math3.analysis.function.HarmonicOscillator')
DerivativeStructure = Java.type('org.apache.commons.math3.analysis.differentiation.DerivativeStructure')
FiniteDifferencesDifferentiator = Java.type('org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator')
SimpsonIntegrator = Java.type('org.apache.commons.math3.analysis.integration.SimpsonIntegrator')
TrapezoidIntegrator = Java.type('org.apache.commons.math3.analysis.integration.TrapezoidIntegrator')
RombergIntegrator = Java.type('org.apache.commons.math3.analysis.integration.RombergIntegrator')
MidPointIntegrator = Java.type('org.apache.commons.math3.analysis.integration.MidPointIntegrator')
PolynomialFunction = Java.type('org.apache.commons.math3.analysis.polynomials.PolynomialFunction')
PolynomialFunctionLagrangeForm = Java.type('org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm')
LaguerreSolver = Java.type('org.apache.commons.math3.analysis.solvers.LaguerreSolver')
UnivariateFunction = Java.type('org.apache.commons.math3.analysis.UnivariateFunction')
SplineInterpolator = Java.type('org.apache.commons.math3.analysis.interpolation.SplineInterpolator')
LinearInterpolator = Java.type('org.apache.commons.math3.analysis.interpolation.LinearInterpolator')
NevilleInterpolator = Java.type('org.apache.commons.math3.analysis.interpolation.NevilleInterpolator')
LoessInterpolator = Java.type('org.apache.commons.math3.analysis.interpolation.LoessInterpolator')
DividedDifferenceInterpolator = Java.type('org.apache.commons.math3.analysis.interpolation.DividedDifferenceInterpolator')
AkimaSplineInterpolator = Java.type('org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator')

GaussianCurveFitter = Java.type('org.apache.commons.math3.fitting.GaussianCurveFitter')
PolynomialCurveFitter = Java.type('org.apache.commons.math3.fitting.PolynomialCurveFitter')
HarmonicCurveFitter = Java.type('org.apache.commons.math3.fitting.HarmonicCurveFitter')
WeightedObservedPoint = Java.type('org.apache.commons.math3.fitting.WeightedObservedPoint')
MultivariateJacobianFunction = Java.type('org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction')
LeastSquaresBuilder = Java.type('org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder')
LevenbergMarquardtOptimizer = Java.type('org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer')
GaussNewtonOptimizer = Java.type('org.apache.commons.math3.fitting.leastsquares.GaussNewtonOptimizer')

SimpleRegression = Java.type('org.apache.commons.math3.stat.regression.SimpleRegression')

FastFourierTransformer = Java.type('org.apache.commons.math3.transform.FastFourierTransformer')
DftNormalization = Java.type('org.apache.commons.math3.transform.DftNormalization')
TransformType = Java.type('org.apache.commons.math3.transform.TransformType')

ArrayRealVector = Java.type('org.apache.commons.math3.linear.ArrayRealVector')
Array2DRowRealMatrix = Java.type('org.apache.commons.math3.linear.Array2DRowRealMatrix')
MatrixUtils = Java.type('org.apache.commons.math3.linear.MatrixUtils')
 
///////////////////////////////////////////////////////////////////////////////////////////////////
//Derivative and interpolation
///////////////////////////////////////////////////////////////////////////////////////////////////

function get_values(f, xdata){
    /*
    Return list of values of a function

    Args:
        f(UnivariateFunction): function
        xdata(float array or list): Domain values
    Returns:
        List of doubles

    */ 
    v = []
    for (var x in xdata){
        v.push(f.value(xdata[x]))
    }
    return v
}    

function interpolate(data, xdata, interpolation_type){
    /*
    Interpolate data array or list to a UnivariateFunction

    Args:
        data(float array or list): The values to interpolate
        xdata(float array or list, optional): Domain values
        interpolation_type(str , optional): "linear", "cubic", "akima", "neville", "loess", "newton"
    Returns:
        UnivariateDifferentiableFunction object

    */ 
    if (!is_defined(xdata))    xdata =null
    if (!is_defined(interpolation_type))    interpolation_type ="linear"
    if (xdata == null){
        xdata = range(0, data.length, 1.0)      
    }
    if ((data.length != xdata.length) || (data.length<2)){
        throw "Dimension mismatch"
    }
    if (interpolation_type == "cubic"){
        i = new SplineInterpolator()
    } else if (interpolation_type == "linear"){
        i = new LinearInterpolator()
    } else if (interpolation_type == "akima"){
        i = new AkimaSplineInterpolator()
    } else if (interpolation_type == "neville"){
        i = new NevilleInterpolator()
    } else if (interpolation_type == "loess"){
        i = new LoessInterpolator()
    } else if (interpolation_type == "newton"){
        i = new DividedDifferenceInterpolator()
    }else{
        throw "Invalid interpolation type"
    }
    return i.interpolate(to_array(xdata,'d'), to_array(data,'d'))
}
       
function deriv(f, xdata, interpolation_type){
    /*
    Calculate derivative of UnivariateFunction, array or list.

    Args:
        f(UnivariateFunction or array): The function object. If array it is interpolated.
        xdata(float array or list, optional): Domain values to process.
        interpolation_type(str , optional): "linear", "cubic", "akima", "neville", "loess", "newton"
    Returns:
        List with the derivative values for xdata

    */ 
    if (!is_defined(xdata))    xdata =null
    if (!is_defined(interpolation_type))    interpolation_type ="linear"

    if (! (f instanceof UnivariateFunction)){
        if (xdata == null){
            xdata = range(0, f.length, 1.0)              
        }
        f = interpolate(f, xdata, interpolation_type)
    }
    if (xdata == null){
        if (f instanceof DifferentiableUnivariateFunction){
            return f.derivative() 
        }
        throw "Domain range not defined"
    }
    d = []    
    for (var x in xdata){
        var xds = new DerivativeStructure(1, 2, 0, x)
        var yds = f.value(xds)
        d.push( yds.getPartialDerivative(1))
    }
    return d
}
        
function integrate(f, range, xdata , interpolation_type , integrator_type){
    /*
    Integrate UnivariateFunction, array or list in an interval.

    Args:
        f(UnivariateFunction or array): The function object. If array it is interpolated.
        range(list, optional): integration range ([min, max]).
        xdata(float array or list, optional): disregarded if f is UnivariateFunction.
        interpolation_type(str , optional): "linear", "cubic", "akima", "neville", "loess", "newton"
        integrator_type(str , optional): "simpson", "trapezoid", "romberg" or "midpoint"
    Returns:
        Integrated value (Float)

    */     
    if (!is_defined(range))    range =null
    if (!is_defined(xdata))    xdata =null
    if (!is_defined(interpolation_type))    interpolation_type ="linear"
    if (!is_defined(integrator_type))    integrator_type ="simpson"
    
    if (! (f instanceof UnivariateFunction)){
        if (xdata == null){
            xdata = range(0, f.length, 1.0) 
        }
        if (range == null){ 
            range = xdata
        }
        f = interpolate(f, xdata, interpolation_type)
    }
    if (range == null){
        throw "Domain range not defined"
    }
    d = []    
    if (integrator_type == "simpson"){
        integrator = new SimpsonIntegrator()
    } else if (integrator_type == "trapezoid"){
        integrator = new TrapezoidIntegrator()
    } else if (integrator_type == "romberg"){
        integrator = new RombergIntegrator()
    } else if (integrator_type == "midpoint"){
        integrator = new MidPointIntegrator()
        throw "Invalid integrator type"
    }
    max_eval = 1000000
    lower = Math.min.apply(null, range)
    upper = Math.max.apply(null, range)
    return integrator.integrate(max_eval,  f, lower, upper)     
}

///////////////////////////////////////////////////////////////////////////////////////////////////
//Fitting and peak search
///////////////////////////////////////////////////////////////////////////////////////////////////

MAX_FLOAT = 1.7976931348623157e+308 

MAX_ITERATIONS = 1000
MAX_EVALUATIONS = 1000

function calculate_peaks(func, start_value, end_value, positive){
    /*
    Calculate peaks of a DifferentiableUnivariateFunction in a given range by finding the roots of the derivative

    Args:
        function(DifferentiableUnivariateFunction): The function object.
        start_value(float): start of range
        end_value(float, optional): end of range
        positive (boolean, optional): True for searching positive peaks, False for negative.
    Returns:
        List of peaks in the interval

    */ 
    if (!is_defined(end_value))    end_value =MAX_FLOAT
    if (!is_defined(positive))    positive =true
    derivative = func.derivative() 
    derivative2 = derivative.derivative() 
    var peaks = []
    solver = new LaguerreSolver()
    var ret = solver.solveAllComplex(derivative.coefficients, start_value)
    for (var complex in ret){
        var r = ret[complex].getReal()
        if ((start_value < r) && (r < end_value)){
            if ((positive && (derivative2.value(r) < 0)) || ( (!positive) && (derivative2.value(r) > 0)))
                peaks.push(r)
        }
    }
    return peaks
}

function estimate_peak_indexes(data, xdata, threshold, min_peak_distance, positive){
    /*
    Estimation of peaks in an array by ordering local maxima according to given criteria.

    Args:
        data(float array or list)
        xdata(float array or list, optional): if not null must have the same length as data.
        threshold(float, optional): if specified filter peaks below this value
        min_peak_distance(float, optional): if specified defines minimum distance between two peaks.
                              if xdata == null, it represents index counts, otherwise in xdata units.
        positive (boolean, optional): True for searching positive peaks, False for negative.
    Returns:
        List of peaks indexes.
    */ 
    if (!is_defined(xdata))    xdata =null
    if (!is_defined(threshold))    threshold =null
    if (!is_defined(min_peak_distance))    min_peak_distance =null
    if (!is_defined(positive))    positive =true
    peaks = []
    indexes = sort_indexes(data, positive)
    for (var index in indexes){
        first = (indexes[index] == 0)
        last = (indexes[index] == (data.length-1))
        val=data[indexes[index]]
        prev = first ? Number.NaN : data[indexes[index]-1]
        next = last ? Number.NaN : data[indexes[index]+1]

        if (threshold != null){
            if ((positive && (val<threshold)) || ((!positive) && (val>threshold)))
                break
        }
        if  ( ( (positive) && (first || val>prev ) && (last || val>=next ) )  || (  
               (!positive) && (first || val<prev ) && (last || val<=next ) ) ) { 
                var append = true
                if (min_peak_distance != null){
                    for (var peak in peaks){
                        if  (   ((xdata == null) && (Math.abs(peaks[peak]-indexes[index]) < min_peak_distance)) || 
                                ((xdata != null) && (Math.abs(xdata[peaks[peak]]-xdata[indexes[index]]) < min_peak_distance)) ){
                            append = false    
                            break
                        }
                    }
                }
                if (append)   peaks.push(indexes[index])
        }
    }
    return peaks
}
        
function _assert_valid_for_fit(fy,fx){
    if ((fy.length<2) || ((fx != null) && (fx.length>fy.length)))
        throw "Invalid data for fit"
}
        
function fit_gaussians(fy, fx, peak_indexes){
    /*
    Fits data on multiple gaussians on the given peak indexes.

    Args:
        x(float array or list)
        y(float array or list)
        peak_indexes(list of int)
    Returns:
        List of tuples of gaussian parameters: (normalization, mean, sigma)
    */ 
    fx = to_array(fx)
    fy = to_array(fy)
    _assert_valid_for_fit(fy,fx)
    ret = []    

    minimum = Math.min.apply(null, fy)
    for (var peak in peak_indexes){
        //Copy data
        data = fy.slice(0)
        //Remover data from other peaks
        for (var p in peak_indexes){
            limit = Math.floor(Math.round((peak_indexes[p]+peak_indexes[peak])/2))
            if (peak_indexes[p] > peak_indexes[peak]){
            	for (var x = limit; x< fy.length; x++){
            		data[x] = minimum
            	}
            } else if (peak_indexes[p] < peak_indexes[peak]){
            	for (var x = 0; x< limit; x++){
            		data[x] = minimum
            	}
            }
        }        
        //Build fit point list
        values = create_fit_point_list(data, fx)                        
        maximum = Math.max.apply(null, data)        
        gaussian_fitter = GaussianCurveFitter.create().withStartPoint([(maximum-minimum)/2,fx[peak_indexes[peak]],1.0]).withMaxIterations(MAX_ITERATIONS)
        //Fit return parameters: (normalization, mean, sigma) 
        try{
            ret.push(to_array(gaussian_fitter.fit(values)))         
        } catch(ex) {
            ret.push(null) //Fitting error
        }
    }
    return ret
}
        
function create_fit_point_list(fy, fx, weights){
	if (!is_defined(weights))    weights =null
    values = []
    for (var i = 0; i< fx.length; i++)
        if (weights == null){
            values.push(new WeightedObservedPoint(1.0, fx[i], fy[i]))
        } else {
            values.push(new WeightedObservedPoint(weights[i], fx[i], fy[i]))
        }
    return values
}
        
function fit_polynomial(fy, fx, order, start_point, weights){
    /*
    Fits data into a polynomial.

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        order(int): if start_point is provided order parameter is disregarded - set to  len(start_point)-1.
        start_point(optional tuple of float): initial parameters (a0, a1, a2, ...)
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of polynomial parameters: (a0, a1, a2, ...)
    */ 
    if (!is_defined(start_point))    start_point =null
    if (!is_defined(weights))    weights =null
    _assert_valid_for_fit(fy,fx)
    fit_point_list = create_fit_point_list(fy, fx, weights)
    if (start_point == null){
        polynomial_fitter = PolynomialCurveFitter.create(order).withMaxIterations(MAX_ITERATIONS)
    } else {
        polynomial_fitter =  PolynomialCurveFitter.create(0).withStartPoint(start_point).withMaxIterations(MAX_ITERATIONS)
    }
    try{
        return to_array(polynomial_fitter.fit(fit_point_list))
    } catch(ex) {
        throw "Fitting failure"
    }
}
        
function fit_gaussian(fy, fx, start_point, weights){
    /*
    Fits data into a gaussian.

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        start_point(optional tuple of float): initial parameters (normalization, mean, sigma)
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of gaussian parameters: (normalization, mean, sigma)
    */ 
    if (!is_defined(start_point))    start_point =null
    if (!is_defined(weights))    weights =null
    _assert_valid_for_fit(fy,fx)
    fit_point_list = create_fit_point_list(fy, fx, weights)

    //If start point not provided, start on peak
    if (start_point == null){
        peaks = estimate_peak_indexes(fy, fx)
        minimum = Math.min.apply(null, fy)
        maximum = Math.max.apply(null, fy)        
        start_point = [(maximum-minimum)/2,fx[peaks[0]],1.0]
    }
    gaussian_fitter = GaussianCurveFitter.create().withStartPoint(start_point).withMaxIterations(MAX_ITERATIONS)
    try{
        return to_array(gaussian_fitter.fit(fit_point_list))  //     (normalization, mean, sigma)
    } catch(ex) {
        throw "Fitting failure"
    }
}
        
function fit_harmonic(fy, fx, start_point, weights){
    /*
    Fits data into an harmonic.

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        start_point(optional tuple of float): initial parameters (amplitude, angular_frequency, phase)
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of harmonic parameters: (amplitude, angular_frequency, phase)
    */
    if (!is_defined(start_point))    start_point =null
    if (!is_defined(weights))    weights =null
    _assert_valid_for_fit(fy,fx)
    fit_point_list = create_fit_point_list(fy, fx, weights)
    if (start_point == null){
        harmonic_fitter = HarmonicCurveFitter.create().withMaxIterations(MAX_ITERATIONS)
    } else { 
        harmonic_fitter = HarmonicCurveFitter.create().withStartPoint(start_point).withMaxIterations(MAX_ITERATIONS)
    }
    try{
        return to_array(harmonic_fitter.fit(fit_point_list))  // (amplitude, angular_frequency, phase)
    } catch(ex) {
        throw "Fitting failure"
    }
}
///////////////////////////////////////////////////////////////////////////////////////////////////
//Least squares problem
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function  optimize_least_squares(model, target, initial, weights){
    if (is_array(weights)){
        weights = MatrixUtils.createRealDiagonalMatrix(weights)
    }
    problem = new LeastSquaresBuilder().start(initial).model(model).target(target).lazyEvaluation(false).maxEvaluations(MAX_EVALUATIONS).maxIterations(MAX_ITERATIONS).weight(weights).build()
    optimizer = new LevenbergMarquardtOptimizer()
    optimum = optimizer.optimize(problem)    

    parameters=to_array(optimum.getPoint().toArray())
    residuals = to_array(optimum.getResiduals().toArray())
    rms = optimum.getRMS()
    evals = optimum.getEvaluations()
    iters = optimum.getIterations()
    return [parameters, residuals, rms, evals, iters]
}

///////////////////////////////////////////////////////////////////////////////////////////////////
//FFT
///////////////////////////////////////////////////////////////////////////////////////////////////

function is_power_of_2(n){
    return n && (n & (n - 1)) === 0;
}

function bit_length(num) {
    return num.toString(2).length
}


function is_complex(v) {
    return v instanceof Complex
}

  
function pad_to_power_of_two(data){
    if (is_power_of_2(data.length)){
        return data
    }
    pad =(1 << bit_length(data.length)) -  data.length
    elem = is_complex(data[0])  ? new Complex(0,0) : [0.0,]
    for (var i=0; i<pad; i++){
    	data.push(elem)
    }
    return data
}

        
function get_real(values){
    /*
    Returns real part of a complex numbers vector.
    Args:
        values: List of complex.
    Returns:
        List of float
    */
    var ret = []
    for (var c in values){
        ret.push(values[c].getReal())
    }
    return ret
}
        
function get_imag(values){
    /*
    Returns imaginary part of a complex numbers vector.
    Args:
        values: List of complex.
    Returns:
        List of float
    */
    var ret = []
    for (var c in values){
        ret.push(values[c].getImaginary())
    }
    return ret
}
        
function get_modulus(values){
    /*
    Returns the modulus of a complex numbers vector.
    Args:
        values: List of complex.
    Returns:
        List of float
    */
    var ret = []
    for (var c in values){
        ret.push(hypot(values[c].getImaginary(),values[c].getReal()))
    }
    return ret
}
        
function get_phase(values){
    /*
    Returns the phase of a complex numbers vector.
    Args:
        values: List of complex.
    Returns:
        List of float
    */
    var ret = []
    for (var c in values){
        ret.push(Math.atan(values[c].getImaginary()/values[c].getReal()))
    }
    return ret
}

function fft(f){
    /*
    Calculates the Fast Fourrier Transform of a vector, padding to the next power of 2 elements.
    Args:
        values(): List of float or complex 
    Returns:
        List of complex
    */        
    f = pad_to_power_of_two(f)    
    if (is_complex(f[0])){
        aux = []
        for (var c in f){
            aux.append(Complex(f[c].getReal(), f[c].getImaginary()))
        }
        f = aux               
    } else {
        f = to_array(f,'d')  
    }
    fftt = new FastFourierTransformer(DftNormalization.STANDARD)
    var ret = []
    transform = fftt.transform(f,TransformType.FORWARD)
    for (var c in transform){
        ret.push(new Complex(transform[c].getReal(),transform[c].getImaginary()))
    }
    return ret
}

        
function  ffti(f){
    /*
    Calculates the Inverse Fast Fourrier Transform of a vector, padding to the next power of 2 elements.
    Args:
        values(): List of float or complex 
    Returns:
        List of complex
    */
    f = pad_to_power_of_two(f)
    if (is_complex(f[0])){
        aux = []
        for (var c in f){
            aux.append(Complex(f[c].getReal(), f[c].getImaginary()))
        }
        f = aux               
    }  else {
        f = to_array(f,'d')  
    }       
    fftt = new FastFourierTransformer(DftNormalization.STANDARD)
    var ret = []
    transform = fftt.transform(f,TransformType.INVERSE )
    for (var c in transform){
        ret.push(new Complex(transform[c].getReal(),transform[c].getImaginary()))
    }
    return ret
}    