###################################################################################################
#  Facade to Apache Commons Math
###################################################################################################

import sys
import math
import operator

import java.util.List
import java.lang.reflect.Array
import java.lang.Class as Class
import jarray
import org.python.core.PyArray as PyArray
import ch.psi.utils.Convert as Convert

import org.apache.commons.math3.util.FastMath as FastMath
import org.apache.commons.math3.util.Pair as Pair
import org.apache.commons.math3.complex.Complex as Complex

import org.apache.commons.math3.analysis.DifferentiableUnivariateFunction as DifferentiableUnivariateFunction
import org.apache.commons.math3.analysis.function.Gaussian as Gaussian
import org.apache.commons.math3.analysis.function.HarmonicOscillator as HarmonicOscillator 
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure as DerivativeStructure
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator as FiniteDifferencesDifferentiator
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator as SimpsonIntegrator
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator as TrapezoidIntegrator
import org.apache.commons.math3.analysis.integration.RombergIntegrator as RombergIntegrator
import org.apache.commons.math3.analysis.integration.MidPointIntegrator as MidPointIntegrator
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction as PolynomialFunction
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm as PolynomialFunctionLagrangeForm
import org.apache.commons.math3.analysis.solvers.LaguerreSolver as LaguerreSolver
import org.apache.commons.math3.analysis.UnivariateFunction as UnivariateFunction
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator as SplineInterpolator
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator as LinearInterpolator
import org.apache.commons.math3.analysis.interpolation.NevilleInterpolator as NevilleInterpolator
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator as LoessInterpolator
import org.apache.commons.math3.analysis.interpolation.DividedDifferenceInterpolator as DividedDifferenceInterpolator
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator as AkimaSplineInterpolator

import org.apache.commons.math3.fitting.GaussianCurveFitter as GaussianCurveFitter
import org.apache.commons.math3.fitting.PolynomialCurveFitter as PolynomialCurveFitter
import org.apache.commons.math3.fitting.HarmonicCurveFitter as HarmonicCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoint as WeightedObservedPoint
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction as MultivariateJacobianFunction
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder as LeastSquaresBuilder
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer as LevenbergMarquardtOptimizer
import org.apache.commons.math3.fitting.leastsquares.GaussNewtonOptimizer as GaussNewtonOptimizer

import org.apache.commons.math3.stat.regression.SimpleRegression as SimpleRegression

import org.apache.commons.math3.transform.FastFourierTransformer as FastFourierTransformer
import org.apache.commons.math3.transform.DftNormalization as DftNormalization
import org.apache.commons.math3.transform.TransformType as TransformType

import org.apache.commons.math3.linear.ArrayRealVector as ArrayRealVector
import org.apache.commons.math3.linear.Array2DRowRealMatrix as Array2DRowRealMatrix
import org.apache.commons.math3.linear.MatrixUtils as MatrixUtils


 
###################################################################################################
#Derivative and interpolation
###################################################################################################

def get_values(f, xdata):
    """Return list of values of a function

    Args:
        f(UnivariateFunction): function
        xdata(float array or list): Domain values
    Returns:
        List of doubles

    """ 
    v = []
    for x in xdata:
        v.append(f.value(x))
    return v

def interpolate(data, xdata = None, interpolation_type = "linear"): 
    """Interpolate data array or list to a UnivariateFunction

    Args:
        data(float array or list): The values to interpolate
        xdata(float array or list, optional): Domain values
        interpolation_type(str , optional): "linear", "cubic", "akima", "neville", "loess", "newton"
    Returns:
        UnivariateDifferentiableFunction object

    """ 
    if xdata is None:
        from startup import frange
        xdata = frange(0, len(data), 1.0)      
    else:
        #X must be ordered
        xy = sorted(zip(xdata,data), key=operator.itemgetter(0))
        xdata, data = zip(*xy)        
    if len(data) != len(xdata) or len(data)<2:
        raise Exception("Dimension mismatch")   
    
    if interpolation_type == "cubic":
        i = SplineInterpolator()
    elif interpolation_type == "linear":
        i = LinearInterpolator()
    elif interpolation_type == "akima":
        i = AkimaSplineInterpolator()
    elif interpolation_type == "neville":
        i = NevilleInterpolator()
    elif interpolation_type == "loess":
        i = LoessInterpolator()
    elif interpolation_type == "newton":
        i = DividedDifferenceInterpolator()
    else:
        raise Exception("Invalid interpolation type")
    from startup import to_array
    return i.interpolate(to_array(xdata,'d'), to_array(data,'d'))
        
def deriv(f, xdata = None, interpolation_type = "linear"):
    """Calculate derivative of UnivariateFunction, array or list.

    Args:
        f(UnivariateFunction or array): The function object. If array it is interpolated.
        xdata(float array or list, optional): Domain values to process.
        interpolation_type(str , optional): "linear", "cubic", "akima", "neville", "loess", "newton"
    Returns:
        List with the derivative values for xdata

    """ 
    if not isinstance(f,UnivariateFunction):
        if xdata is None:
            from startup import frange
            xdata = frange(0, len(f), 1.0)              
        f = interpolate(f, xdata, interpolation_type)
    if xdata is None:
        if isinstance(f,DifferentiableUnivariateFunction):
            return f.derivative() 
        raise Exception("Domain range not defined")  
    d = []    
    for x in xdata:
        xds = DerivativeStructure(1, 2, 0, x)
        yds = f.value(xds)
        d.append( yds.getPartialDerivative(1))
    return d

def integrate(f, range = None, xdata = None, interpolation_type = "linear", integrator_type = "simpson"):
    """Integrate UnivariateFunction, array or list in an interval.

    Args:
        f(UnivariateFunction or array): The function object. If array it is interpolated.
        range(list, optional): integration range ([min, max]).
        xdata(float array or list, optional): disregarded if f is UnivariateFunction.
        interpolation_type(str , optional): "linear", "cubic", "akima", "neville", "loess", "newton"
        integrator_type(str , optional): "simpson", "trapezoid", "romberg" or "midpoint"
    Returns:
        Integrated value (Float)

    """     
    if not isinstance(f, UnivariateFunction):
        from startup import frange
        if xdata is None:            
            xdata = frange(0, len(f), 1.0) 
        if range is None: 
            range = xdata
        f = interpolate(f, xdata, interpolation_type)
    if range is None:
        raise Exception("Domain range not defined")  
    d = []    
    if integrator_type == "simpson":
        integrator = SimpsonIntegrator()
    elif integrator_type == "trapezoid":
        integrator = TrapezoidIntegrator()
    elif integrator_type == "romberg":
        integrator = RombergIntegrator()
    elif integrator_type == "midpoint":
        integrator = MidPointIntegrator()
        raise Exception("Invalid integrator type")        
    lower = min(range)
    upper = max(range)
    return integrator.integrate(MAX_EVALUATIONS,  f, lower, upper)     

def trapz(y, xdata=None):
    """Integrate an array or list using the composite trapezoidal rule.

    Args:
        y(array or list)
        xdata(float array or list, optional)
    """
    return integrate(y, range = None, xdata = xdata, interpolation_type = "linear", integrator_type = "trapezoid")

###################################################################################################
#Fitting and peak search
###################################################################################################

try:
    MAX_FLOAT = sys.float_info.max 
except: # Python 2.5
    MAX_FLOAT = 1.7976931348623157e+308 

MAX_ITERATIONS = 1000
MAX_EVALUATIONS = 1000000

def calculate_peaks(function, start_value, end_value = MAX_FLOAT, positive=True):
    """Calculate peaks of a DifferentiableUnivariateFunction in a given range by finding the roots of the derivative

    Args:
        function(DifferentiableUnivariateFunction): The function object.
        start_value(float): start of range
        end_value(float, optional): end of range
        positive (boolean, optional): True for searching positive peaks, False for negative.
    Returns:
        List of peaks in the interval

    """ 
    derivative = function.derivative() 
    derivative2 = derivative.derivative() 
    ret = []
    solver = LaguerreSolver()
    for complex in solver.solveAllComplex(derivative.coefficients, start_value):
        r = complex.real
        if start_value < r < end_value:
            if (positive and (derivative2.value(r) < 0)) or ( (not positive) and (derivative2.value(r) > 0)):         
                ret.append(r)
    return ret


def estimate_peak_indexes(data, xdata = None, threshold = None, min_peak_distance = None, positive = True):
    """Estimation of peaks in an array by ordering local maxima according to given criteria.

    Args:
        data(float array or list)
        xdata(float array or list, optional): if not None must have the same length as data.
        threshold(float, optional): if specified filter peaks below this value
        min_peak_distance(float, optional): if specified defines minimum distance between two peaks.
                              if xdata == None, it represents index counts, otherwise in xdata units.
        positive (boolean, optional): True for searching positive peaks, False for negative.
    Returns:
        List of peaks indexes.
    """ 
    peaks = []
    indexes = sorted(range(len(data)),key=lambda x:data[x])
    if positive:
        indexes = reversed(indexes)
    for index in indexes:
        first = (index == 0)
        last = (index == (len(data)-1))
        val=data[index]
        prev = float('NaN') if first else data[index-1]
        next = float('NaN') if last else data[index+1]

        if threshold is not None:
            if (positive and (val<threshold)) or ((not positive) and (val>threshold)):
                break
        if  (  positive       and (first or val>prev ) and (last or val>=next ) )  or (  
               (not positive) and (first or val<prev ) and (last or val<=next ) ):            
                append = True
                if min_peak_distance is not None:
                    for peak in peaks:
                        if  ((xdata is None) and (abs(peak-index) < min_peak_distance)) or (
                                (xdata is not None) and (abs(xdata[peak]-xdata[index]) < min_peak_distance)):
                            append = False    
                            break
                if append:
                    peaks.append(index)
    return peaks

def _assert_valid_for_fit(y,x):
    if len(y)<2 or ((x is not None) and (len(x)>len(y))):
        raise Exception("Invalid data for fit")

def fit_gaussians(y, x, peak_indexes):
    """Fits data on multiple gaussians on the given peak indexes.

    Args:
        x(float array or list)
        y(float array or list)
        peak_indexes(list of int)
    Returns:
        List of tuples of gaussian parameters: (normalization, mean, sigma)
    """ 
    _assert_valid_for_fit(y,x)
    ret = []    

    minimum = min(y)
    for peak in peak_indexes:
        #Copy data
        data = y[:]
        #Remover data from other peaks
        for p in peak_indexes:
            limit = int(round((p+peak)/2))
            if (p > peak):
                data[limit : len(y)] =[minimum] * (len(y)-limit)
            elif (p < peak):
                data[0:limit] = [minimum] *limit
        #Build fit point list
        values = create_fit_point_list(data, x)
        maximum = max(data)
        gaussian_fitter = GaussianCurveFitter.create().withStartPoint([(maximum-minimum)/2,x[peak],1.0]).withMaxIterations(MAX_ITERATIONS)
        #Fit return parameters: (normalization, mean, sigma) 
        try:
            ret.append(gaussian_fitter.fit(values).tolist())         
        except:
            ret.append(None) #Fitting error
    return ret


def create_fit_point_list(y, x, weights = None):
    values = []
    for i in sorted(range(len(x)),key=lambda v:x[v]):  #Creating list ordered by x, needed for gauss fit
        if weights is None:
            values.append(WeightedObservedPoint(1.0, x[i], y[i]))
        else:
            values.append(WeightedObservedPoint(weights[i], x[i], y[i]))
    return values

def fit_polynomial(y, x, order, start_point = None, weights = None):
    """Fits data into a polynomial.

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        order(int): if start_point is provided order parameter is disregarded - set to  len(start_point)-1.
        start_point(optional tuple of float): initial parameters (a0, a1, a2, ...)
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of polynomial parameters: (a0, a1, a2, ...)
    """ 
    _assert_valid_for_fit(y,x)
    fit_point_list = create_fit_point_list(y, x, weights)
    if start_point is None:
        polynomial_fitter = PolynomialCurveFitter.create(order).withMaxIterations(MAX_ITERATIONS)
    else:
        polynomial_fitter =  PolynomialCurveFitter.create(0).withStartPoint(start_point).withMaxIterations(MAX_ITERATIONS)
    try:
        return polynomial_fitter.fit(fit_point_list).tolist()
    except: 
        raise Exception("Fitting failure")

def fit_gaussian(y, x, start_point = None, weights = None):
    """Fits data into a gaussian.

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        start_point(optional tuple of float): initial parameters (normalization, mean, sigma)
                If None, use a custom initial estimation.
                Set to "default" to force  Commons.Math the default (GaussianCurveFitter.ParameterGuesser).     
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of gaussian parameters: (normalization, mean, sigma)
    """ 
    _assert_valid_for_fit(y,x)
    fit_point_list = create_fit_point_list(y, x, weights)

    #If start point not provided, start on peak
    if start_point is None:
        maximum, minimum = max(y), min(y)
        norm = maximum - minimum
        mean = x[y.index(maximum)]         
        sigma =  trapz([v-minimum for v in y], x)  / (norm*math.sqrt(2*math.pi))
        start_point = (norm, mean, sigma)
    elif start_point == "simple":   
        start_point = [(max(y)-min(y))/2, x[y.index(max(y))], 1.0]
    elif start_point == "default":
        start_point = GaussianCurveFitter.ParameterGuesser(fit_point_list).guess().tolist()
    gaussian_fitter = GaussianCurveFitter.create().withStartPoint(start_point).withMaxIterations(MAX_ITERATIONS)
    try:
        return gaussian_fitter.fit(fit_point_list).tolist()  #     (normalization, mean, sigma)
    except: 
        raise Exception("Fitting failure")

def fit_harmonic(y, x, start_point = None, weights = None):
    """Fits data into an harmonic.

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        start_point(optional tuple of float): initial parameters (amplitude, angular_frequency, phase)
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of harmonic parameters: (amplitude, angular_frequency, phase)
    """
    _assert_valid_for_fit(y,x)
    fit_point_list = create_fit_point_list(y, x, weights)
    if start_point is None:
        harmonic_fitter = HarmonicCurveFitter.create().withMaxIterations(MAX_ITERATIONS)
    else:
        harmonic_fitter =  HarmonicCurveFitter.create().withStartPoint(start_point).withMaxIterations(MAX_ITERATIONS)
    try:
        return harmonic_fitter.fit(fit_point_list).tolist()  # (amplitude, angular_frequency, phase)
    except: 
        raise Exception("Fitting failure")


def fit_gaussian_offset(y, x, start_point = None, weights = None):
    """Fits data into a gaussian with offset (constant background).
       f(x) = a + b * exp(-(pow((x - c), 2) / (2 * pow(d, 2))))

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        start_point(optional tuple of float): initial parameters (normalization, mean, sigma)
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of gaussian parameters: (offset, normalization, mean, sigma)
    """     

    # For normalised gauss curve sigma=1/(amp*sqrt(2*pi))
    if start_point is None:
        off = min(y)  # good enough starting point for offset
        com = x[y.index(max(y))]
        amp = max(y) - off
        sigma = trapz([v-off for v in y], x) / (amp*math.sqrt(2*math.pi))                 
        start_point = [off, amp, com , sigma]                                       
        
    class Model(MultivariateJacobianFunction):
        def value(self, variables):
            value = ArrayRealVector(len(x))
            jacobian = Array2DRowRealMatrix(len(x), 4)        
            for i in range(len(x)):
                (a,b,c,d) = (variables.getEntry(0), variables.getEntry(1), variables.getEntry(2), variables.getEntry(3))   
                v = math.exp(-(math.pow((x[i] - c), 2) / (2 * math.pow(d, 2))))
                model = a + b * v
                value.setEntry(i, model)                                    
                jacobian.setEntry(i, 0, 1)                  # derivative with respect to p0 = a                        
                jacobian.setEntry(i, 1, v)                  # derivative with respect to p1 = b            
                v2 = b*v*((x[i] - c)/math.pow(d, 2))
                jacobian.setEntry(i, 2, v2)                 # derivative with respect to p2 = c        
                jacobian.setEntry(i, 3, v2*(x[i] - c)/d )   # derivative with respect to p3 = d       
            return Pair(value, jacobian)
    
    model = Model()
    target = [v for v in y]      #the target is to have all points at the positios    
    (parameters, residuals, rms, evals, iters) = optimize_least_squares(model, target, start_point, weights)        
    return parameters


def fit_gaussian_linear(y, x, start_point = None, weights = None):
    """Fits data into a gaussian with linear background.
       f(x) = a * x +  b  + c * exp(-(pow((x - d), 2) / (2 * pow(e, 2)))) 

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        start_point(optional tuple of float): initial parameters (normalization, mean, sigma)
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of gaussian parameters: (a, b, normalization, mean, sigma)
    """     

    # For normalised gauss curve sigma=1/(amp*sqrt(2*pi))
    if start_point is None:
        off = min(y)  # good enough starting point for offset
        com = x[y.index(max(y))]
        amp = max(y) - off
        sigma = trapz([v-off for v in y], x) / (amp*math.sqrt(2*math.pi))                 
        start_point = [0, off, amp, com, sigma]                                       
        
    class Model(MultivariateJacobianFunction):
        def value(self, variables):
            value = ArrayRealVector(len(x))
            jacobian = Array2DRowRealMatrix(len(x), 5)        
            for i in range(len(x)):
                (a,b,c,d,e) = (variables.getEntry(0), variables.getEntry(1), variables.getEntry(2), variables.getEntry(3), variables.getEntry(4))   
                v = math.exp(-(math.pow((x[i] - d), 2) / (2 * math.pow(e, 2))))
                model = a*x[i] + b + c * v 
                value.setEntry(i, model)                                    
                jacobian.setEntry(i, 0, x[i])               # derivative with respect to p0 = a                        
                jacobian.setEntry(i, 1, 1)                  # derivative with respect to p1 = b
                jacobian.setEntry(i, 2, v)                  # derivative with respect to p2 = c            
                v2 = c*v*((x[i] - d)/math.pow(e, 2))
                jacobian.setEntry(i, 3, v2)                 # derivative with respect to p3 = d        
                jacobian.setEntry(i, 4, v2*(x[i] - d)/e )   # derivative with respect to p4 = e       
            return Pair(value, jacobian)
    
    model = Model()
    target = [v for v in y]      #the target is to have all points at the positios    
    (parameters, residuals, rms, evals, iters) = optimize_least_squares(model, target, start_point, weights)        
    return parameters

def fit_gaussian_exp_bkg(y, x, start_point = None, weights = None):
    """Fits data into a gaussian with exponential background.
       f(x) = a * math.exp(-(x/b)) + c * exp(-(pow((x - d), 2) / (2 * pow(e, 2)))) 

    Args:
        x(float array or list): observed points x 
        y(float array or list): observed points y 
        start_point(optional tuple of float): initial parameters (normalization, mean, sigma)
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of gaussian parameters: (a,b , normalization, mean, sigma)
    """     

    # For normalised gauss curve sigma=1/(amp*sqrt(2*pi))
    if start_point is None:
        off = min(y)  # good enough starting point for offset
        com = x[len(x)/2]
        #com = 11.9
        amp = max(y) - off
        sigma = trapz([v-off for v in y], x) / (amp*math.sqrt(2*math.pi))                 
        start_point = [1, 1, amp, com, sigma]                                       
        
    class Model(MultivariateJacobianFunction):
        def value(self, variables):
            value = ArrayRealVector(len(x))
            jacobian = Array2DRowRealMatrix(len(x), 5)        
            for i in range(len(x)):
                (a,b,c,d,e) = (variables.getEntry(0), variables.getEntry(1), variables.getEntry(2), variables.getEntry(3), variables.getEntry(4))   
                v = math.exp(-(math.pow((x[i] - d), 2) / (2 * math.pow(e, 2))))
                bkg=math.exp(-(x[i]/b))
                model = a*bkg + c * v 
                value.setEntry(i, model)                                    
                jacobian.setEntry(i, 0, bkg)               # derivative with respect to p0 = a                        
                jacobian.setEntry(i, 1, a*x[i]*bkg/math.pow(b, 2))    # derivative with respect to p1 = b
                jacobian.setEntry(i, 2, v)                  # derivative with respect to p2 = c            
                v2 = c*v*((x[i] - d)/math.pow(e, 2))
                jacobian.setEntry(i, 3, v2)                 # derivative with respect to p3 = d        
                jacobian.setEntry(i, 4, v2*(x[i] - d)/e )   # derivative with respect to p4 = e       
            return Pair(value, jacobian)
    
    model = Model()
    target = [v for v in y]      #the target is to have all points at the positios    
    (parameters, residuals, rms, evals, iters) = optimize_least_squares(model, target, start_point, weights)        
    return parameters

###################################################################################################
#Functions
###################################################################################################

class GaussianOffset(UnivariateFunction):
    def __init__(self, offset, normalization, mean_value, sigma):
        self.gaussian = Gaussian(normalization, mean_value, sigma)
        self.offset = offset
    def value(self,x):
        return self.gaussian.value(x) + self.offset

class GaussianLinear(UnivariateFunction):
    def __init__(self, a,b, normalization, mean_value, sigma):
        self.gaussian = Gaussian(normalization, mean_value, sigma)
        self.a = a
        self.b = b
    def value(self,x):
        return self.gaussian.value(x) + self.a * x + self.b
        
class GaussianExpBkg(UnivariateFunction):
    def __init__(self, a, b, normalization, mean_value, sigma):
        self.gaussian = Gaussian(normalization, mean_value, sigma)
        self.a = a
        self.b = b
    def value(self,x):
        return self.gaussian.value(x) + self.a * math.exp(-(x/self.b))

###################################################################################################
#Least squares
###################################################################################################

def optimize_least_squares(model, target, initial, weights):   
    """Fits a parametric model to a set of observed values by minimizing a cost function.

    Args:
        model(MultivariateJacobianFunction): observed points x 
        target(float array or list): observed data
        initial(optional tuple of float): initial guess
        weights(optional float array or list): weight for each observed point
    Returns:
        Tuples of harmonic parameters: (amplitude, angular_frequency, phase)
    """
    if isinstance(weights,tuple) or isinstance(weights,list):
        weights = MatrixUtils.createRealDiagonalMatrix(weights)
    problem = LeastSquaresBuilder().start(initial).model(model).target(target).lazyEvaluation(False).maxEvaluations(MAX_EVALUATIONS).maxIterations(MAX_ITERATIONS).weight(weights).build()
    optimizer = LevenbergMarquardtOptimizer()
    optimum = optimizer.optimize(problem)    

    parameters=optimum.getPoint().toArray().tolist()
    residuals = optimum.getResiduals().toArray().tolist()
    rms = optimum.getRMS()
    evals = optimum.getEvaluations()
    iters = optimum.getIterations()
    return (parameters, residuals, rms, evals, iters)


###################################################################################################
#FFT
###################################################################################################

def is_power(num, base):
    if base<=1: return num == 1
    power = int (math.log (num, base) + 0.5)
    return base ** power == num
  
def pad_to_power_of_two(data):
    if is_power(len(data),2):
        return data
    pad =(1 << len(data).bit_length()) -  len(data)
    elem = complex(0,0) if type(data[0]) is complex else [0.0,]
    return data + elem * pad  

def get_real(values):
    """Returns real part of a complex numbers vector.
    Args:
        values: List of complex.
    Returns:
        List of float
    """
    ret = []
    for c in values:
        ret.append(c.real)
    return ret

def get_imag(values):
    """Returns imaginary part of a complex numbers vector.
    Args:
        values: List of complex.
    Returns:
        List of float
    """
    ret = []
    for c in values:
        ret.append(c.imag)
    return ret

def get_modulus(values):
    """Returns the modulus of a complex numbers vector.
    Args:
        values: List of complex.
    Returns:
        List of float
    """
    ret = []
    for c in values:
        ret.append(math.hypot(c.imag,c.real))
    return ret

def get_phase(values):
    """Returns the phase of a complex numbers vector.
    Args:
        values: List of complex.
    Returns:
        List of float
    """
    ret = []
    for c in values:
        ret.append(math.atan(c.imag/c.real))
    return ret

def fft(f):
    """Calculates the Fast Fourrier Transform of a vector, padding to the next power of 2 elements.
    Args:
        values(): List of float or complex 
    Returns:
        List of complex
    """
    f = pad_to_power_of_two(f)
    if type(f[0]) is complex:
        aux = []
        for c in f:
            aux.append(Complex(c.real, c.imag))
        f = aux                        
    fftt = FastFourierTransformer(DftNormalization.STANDARD)
    ret = []
    for c in fftt.transform(f,TransformType.FORWARD ):
        ret.append(complex(c.getReal(),c.getImaginary()))
    return ret

def ffti(f):
    """Calculates the Inverse Fast Fourrier Transform of a vector, padding to the next power of 2 elements.
    Args:
        values(): List of float or complex 
    Returns:
        List of complex
    """
    f = pad_to_power_of_two(f)
    if type(f[0]) is complex:
        aux = []
        for c in f:
            aux.append(Complex(c.real, c.imag))
        f = aux
    fftt = FastFourierTransformer(DftNormalization.STANDARD)
    ret = []
    for c in fftt.transform(f,TransformType.INVERSE ):
        ret.append(complex(c.getReal(),c.getImaginary()))
    return ret