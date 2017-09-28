###################################################################################################
#  Plot utilities
###################################################################################################

import ch.psi.pshell.plot.LinePlotSeries as LinePlotSeries
import ch.psi.pshell.plot.LinePlotErrorSeries as LinePlotErrorSeries
import math
from startup import frange, to_array

def plot_function(plot, function, name, range, show_points = True, show_lines = True, color = None):
    """Plots a function to a plot.

    Args:
        plot(LinePlot)
        function(UnivariateFunction): Gaussian, PolynomialFunction, HarmonicOscillator...
        name(str): name of the series
        range(list or array of floats): x values to plot
    Returns:
        Tuples of harmonic parameters: (amplitude, angular_frequency, phase)
    """
    if plot.style.isError():
        s = LinePlotErrorSeries(name, color)
    else:
        s = LinePlotSeries(name, color)    
    plot.addSeries(s)
    s.setPointsVisible(show_points)
    s.setLinesVisible(show_lines)
    for x in range:
        s.appendData(x, function.value(x))   
    return s

def plot_data(plot, data, name, xdata = None,  error = None, show_points = True, show_lines = True, color = None):
    """Plots a subscriptable object to a plot.

    Args:
        plot(LinePlot)
        data(subscriptable): Y data
        name(str): name of the series
        xdata(subscriptable): X data
        error(subscriptable): Error data (only for error plots)
    Returns:
        Tuples of harmonic parameters: (amplitude, angular_frequency, phase)
    """
    if plot.style.isError():
        s = LinePlotErrorSeries(name, color)
    else:
        s = LinePlotSeries(name, color)    
    plot.addSeries(s)
    s.setPointsVisible(show_points)
    s.setLinesVisible(show_lines)
    if xdata is None: 
        xdata = range(len(data))
    xdata = to_array(xdata, 'd')
    data = to_array(data, 'd')
    if plot.style.isError():
        error = to_array(error, 'd')
        s.setData(xdata, data, error)
    else:
        s.setData(xdata, data)
    return s

def plot_point(plot, x, y, size = 3, color = None, name = "Point"):
    s = LinePlotSeries(name, color)
    plot.addSeries(s)
    s.setPointSize(size)
    s.appendData(x, y)
    return s

def plot_line(plot, x1, y1, x2, y2, width = 1, color = None, name = "Line"):
    s = LinePlotSeries(name, color)    
    plot.addSeries(s)
    s.setLineWidth(width)
    s.setPointsVisible(False)
    s.appendData(x1, y1)        
    s.appendData(x2, y2)     
    return s

def plot_cross(plot, x, y, size = 1.0, width = 1, color = None, name = "Cross"):
    size = float(size)
    s = LinePlotSeries(name, color)     
    plot.addSeries(s)
    s.setLineWidth(width)
    s.setPointsVisible(False)
    s.appendData(float('nan'), float('nan'))        
    s.appendData(x-size/2, y)        
    s.appendData(x+size/2, y)        
    s.appendData(float('nan'), float('nan'))        
    s.appendData(x, y-size/2)     
    s.appendData(x, y+size/2)     
    return s    

def plot_rectangle(plot, x1, y1, x2, y2, width = 1, color = None, name = "Rectangle"):
    s = LinePlotSeries(name, color)    
    plot.addSeries(s)
    s.setLineWidth(width)
    s.setPointsVisible(False)
    s.appendData(x1, y1)        
    s.appendData(x1, y2)        
    s.appendData(x2, y2)        
    s.appendData(x2, y1)        
    s.appendData(x1, y1)      
    return s

def plot_circle(plot, cx, cy, radius, width = 1, color = None, name = "Circle"):
    s = LinePlotSeries(name, color)    
    plot.addSeries(s)
    s.setLineWidth(width)
    s.setPointsVisible(False) 
    res=float(radius) / 100.0
    epson = 1e-12
    for xp in frange (cx+radius-epson , cx-radius+epson , -res):
        yp = math.sqrt(math.pow(radius, 2) - math.pow(xp - cx, 2)) + cy
        s.appendData(xp, yp)        
    for xp in frange (cx-radius+epson , cx+radius-epson, res):
        yp = -math.sqrt(math.pow(radius, 2) - math.pow(xp - cx, 2)) + cy
        s.appendData(xp, yp)       
    if s.getCount()>0:
        s.appendData(s.getX()[0], s.getY()[0])  
    return s