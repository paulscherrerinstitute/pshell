///////////////////////////////////////////////////////////////////////////////////////////////////
//  Plot utilities
///////////////////////////////////////////////////////////////////////////////////////////////////


function plot_function(plot, func, name, range, show_points, show_lines, color){
    /*
     Plots a function to a plot.

    Args:
        plot(LinePlot)
        func(UnivariateFunction): Gaussian, PolynomialFunction, HarmonicOscillator...
        name(str): name of the series
        range(list or array of floats): x values to plot
    Returns:
        Tuples of harmonic parameters: (amplitude, angular_frequency, phase)
    */    
    if (!is_defined(show_points))    show_points =true
    if (!is_defined(show_lines))    show_lines = true
    if (!is_defined(color))    color = null
    
    if (plot.getStyle().isError()){
        s = new LinePlotErrorSeries(name, color)
    }
    else{
        s = new LinePlotSeries(name, color)    
    }
    plot.addSeries(s)
    s.setPointsVisible(show_points)
    s.setLinesVisible(show_lines)
    for (var x in range){
        s.appendData(range[x], func.value(range[x]))   
    }
    return s
}        

function plot_point(plot, x, y, size, color, name){
    if (!is_defined(size))    size =3
    if (!is_defined(name))    name = "Point"
    if (!is_defined(color))    color = null
    s = new LinePlotSeries(name, color)
    plot.addSeries(s)
    s.setPointSize(size)
    s.appendData(x, y)
    return s
}    

function plot_line(plot, x1, y1, x2, y2, width, color, name){
    if (!is_defined(width))    width = 1
    if (!is_defined(name))    name = "Line"
    if (!is_defined(color))    color = null
    s = new LinePlotSeries(name, color)    
    plot.addSeries(s)
    s.setLineWidth(width)
    s.setPointsVisible(false)
    s.appendData(x1, y1)        
    s.appendData(x2, y2)        
    return s
}    

function plot_cross(plot, x, y, size, width, color, name){
    if (!is_defined(size))    size =1
    if (!is_defined(width))    width = 1
    if (!is_defined(color))    color = null
    if (!is_defined(name))    name = "Cross"
    s = new LinePlotSeries(name, color)    
    plot.addSeries(s)   
    s.setLineWidth(width)
    s.setPointsVisible(false)
    s.appendData(NaN, NaN)        
    s.appendData(x-size/2, y)        
    s.appendData(x+size/2, y)        
    s.appendData(NaN, NaN)        
    s.appendData(x, y-size/2)     
    s.appendData(x, y+size/2)     
    return s    
 }

function plot_rectangle(plot, x1, y1, x2, y2, width, color, name){
    if (!is_defined(width))    width = 1
    if (!is_defined(name))    name = "Rectangle"
    if (!is_defined(color))    color = null
    s = new LinePlotSeries(name, color)    
    plot.addSeries(s)
    s.setLineWidth(width)
    s.setPointsVisible(false)
    s.appendData(x1, y1)        
    s.appendData(x1, y2)        
    s.appendData(x2, y2)        
    s.appendData(x2, y1)        
    s.appendData(x1, y1)  
    return s
}    

function plot_circle(plot, cx, cy, radius, width, color, name){
    if (!is_defined(width))    width = 1
    if (!is_defined(name))    name = "Circle"
    if (!is_defined(color))    color = null	
    s = new LinePlotSeries(name, color)    
    plot.addSeries(s)
    s.setLineWidth(width)
    s.setPointsVisible(false) 
    res=radius / 100.0
    epson = 1e-12
    for (var xp = cx+radius-epson ; xp >= ( cx-radius+epson) ; xp-=res){
        yp = Math.sqrt(Math.pow(radius, 2) - Math.pow(xp - cx, 2)) + cy
        s.appendData(xp, yp)        
    }
    for (var xp = cx-radius+epson ; xp <= ( cx+radius-epson) ; xp+=res){
        yp = -Math.sqrt(Math.pow(radius, 2) - Math.pow(xp - cx, 2)) + cy
        s.appendData(xp, yp)  
    }     
    if (s.getCount()>0)
        s.appendData(s.getX()[0], s.getY()[0])  
    return s
}        