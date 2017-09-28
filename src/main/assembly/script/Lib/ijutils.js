///////////////////////////////////////////////////////////////////////////////////////////////////
//  Facade to ImageJ functionality
///////////////////////////////////////////////////////////////////////////////////////////////////

//More information on:
//   Image:   https://imagej.nih.gov/ij/docs/guide/146-28.htmltoc-Section-28
//   Process: https://imagej.nih.gov/ij/docs/guide/146-29.html#toc-Section-29
//   Analyze: https://imagej.nih.gov/ij/docs/guide/146-30.html#toc-Section-30

Utils = Java.type('ch.psi.pshell.imaging.Utils')
Pair = Java.type('org.apache.commons.math3.util.Pair')


IJ = Java.type('ij.IJ')
ImageJ = Java.type('ij.ImageJ')
WindowManager = Java.type('ij.WindowManager')
ImagePlus = Java.type('ij.ImagePlus')
Prefs = Java.type('ij.Prefs')
FileSaver = Java.type('ij.io.FileSaver')

ImageProcessor = Java.type('ij.process.ImageProcessor')
ByteProcessor = Java.type('ij.process.ByteProcessor')
ShortProcessor = Java.type('ij.process.ShortProcessor')
ColorProcessor = Java.type('ij.process.ColorProcessor')
FloatProcessor = Java.type('ij.process.FloatProcessor')
ImageConverter = Java.type('ij.process.ImageConverter')
AutoThresholder = Java.type('ij.process.AutoThresholder')
LUT = Java.type('ij.process.LUT')
Measurements = Java.type('ij.measure.Measurements')

ResultsTable = Java.type('ij.measure.ResultsTable')
Analyzer = Java.type('ij.plugin.filter.Analyzer')
GaussianBlur = Java.type('ij.plugin.filter.GaussianBlur')
Filters = Java.type('ij.plugin.filter.Filters')
FFTFilter = Java.type('ij.plugin.filter.FFTFilter')
BackgroundSubtracter = Java.type('ij.plugin.filter.BackgroundSubtracter')
EDM = Java.type('ij.plugin.filter.EDM')
Shadows = Java.type('ij.plugin.filter.Shadows')
UnsharpMask = Java.type('ij.plugin.filter.UnsharpMask')
MaximumFinder = Java.type('ij.plugin.filter.MaximumFinder') 
EDM = Java.type('ij.plugin.filter.EDM')
Shadows = Java.type('ij.plugin.filter.Shadows')
UnsharpMask = Java.type('ij.plugin.filter.UnsharpMask')
RankFilters = Java.type('ij.plugin.filter.RankFilters')
Convolver = Java.type('ij.plugin.filter.Convolver')
ParticleAnalyzer = Java.type('ij.plugin.filter.ParticleAnalyzer')

ContrastEnhancer = Java.type('ij.plugin.ContrastEnhancer')
Thresholder = Java.type('ij.plugin.Thresholder')
ImageCalculator = Java.type('ij.plugin.ImageCalculator')
FFT = Java.type('ij.plugin.FFT')
Concatenator = Java.type('ij.plugin.Concatenator')

//ImageJ customizations
FFTMath = Java.type('ch.psi.pshell.imaging.ij.FFTMath')
FFTFilter = Java.type('ch.psi.pshell.imaging.ij.FFTFilter')
Binary = Java.type('ch.psi.pshell.imaging.ij.Binary')
Slicer = Java.type('ch.psi.pshell.imaging.ij.Slicer')


//This eliminates the error messages due to the bug on ij.gui.ImageWindow row 555 (ij is null)
try{
    _image_j
} catch(ex) {   	
    _image_j = new ImageJ(null, ImageJ.NO_SHOW)
}    
    
///////////////////////////////////////////////////////////////////////////////////////////////////
//Image creation, copying & saving
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function  load_image(image, title){
    /*
    image: file name or BufferedImage
    */    
    if (!is_defined(title))    title ="img"
    
    if (typeof image == 'string') 
        try{
            file = get_context().setup.expandPath(image)
        } catch(ex) {
        }
        try{
            image = Utils.newImage(file)
        } catch(ex) {
            //try loading from assembly
            image = get_context().setup.getAssemblyImage(image)
        }
    return new ImagePlus(title, image)
}
        
function load_array(array, width, height, title){
    /*
    array: 1d array if width and height defined , or else 2d array to be flattened.
    */  
    if (!is_defined(width))    width = null
    if (!is_defined(height))    height = null
    if (!is_defined(title))    title ="img"  
    //2D
    if ((width==null) && (height==null)){
        if (array.typecode == '[B') proc = new ByteProcessor(array[0].length, array.length, Convert.toUnidimensional(array))
        else if (array.typecode == '[S') proc = new ShortProcessor(array[0].length, array.length, Convert.toUnidimensional(array), null)
        else if (array.typecode == '[I') proc = new FloatProcessor(array[0].length, array.length, Convert.toUnidimensional(array))        
        else if (array.typecode == '[F') proc = new FloatProcessor(array[0].length, array.length, Convert.toUnidimensional(array))        
        else if (array.typecode == '[D') proc = new FloatProcessor(array[0].length, array.length, Convert.toUnidimensional(array))        
        else throw "Invalid array type"
    //1D
    }else{
        if (array.length > width*height)
            array = array.slice[0, width*height]
        if (array.typecode == 'b') proc = new yteProcessor(width, height, array)
        else if (array.typecode == 'h') proc = new ShortProcessor(width, height, array, null)
        else if (array.typecode == 'i') proc = new FloatProcessor(width, height, array)
        else if (array.typecode == 'f') proc = new FloatProcessor(width, height, array)
        else if (array.typecode == 'd') proc = new FloatProcessor(width, height, array)
        else throw "Invalid array type"
    }
    return new ImagePlus(title, proc)
}
        
function save_image(ip, path, format){
    /*
    Saves image or stack
    If parameters omitted, saves image again in same location, with same format.
    */
    if (!is_defined(path))    path = null
    if (!is_defined(format))    format = null
    var fs = new FileSaver(ip)
    if (path == null)fs.save()
    else{
        try{
            path = get_context().setup.expandPath(path)
        } catch(ex) {
        }      
        if (format == "bmp") fs.saveAsBmp(path) 
        else if (format == "fits") fs.saveAsFits(path) 
        else if (format == "gif") fs.saveAsGif(path) 
        else if (format == "jpeg") fs.saveAsJpeg(path) 
        else if (format == "lut") fs.saveAsLut(path) 
        else if (format == "pgm") fs.saveAsPgm(path) 
        else if (format == "png") fs.saveAsPng(path) 
        else if (format == "raw" && (ip.getImageStackSize()>1)) fs.saveAsRawStack(path) 
        else if (format == "raw") fs.saveAsRaw(path) 
        else if (format == "txt") fs.saveAsText(path)
        else if (format == "tiff" && (ip.getImageStackSize()>1)) fs.saveAsTiffStack(path)
        else if (format == "tiff") fs.saveAsTiff(path)
        else if (format == "zip") fs.saveAsZip(path)   
    }
}
        
function new_image(width, height, image_type, title, fill_color){
    /*
    type = "byte", "short", "color" or "float"
    */
    if (!is_defined(image_type))    image_type = "byte"
    if (!is_defined(title))    title = "img"
    if (!is_defined(fill_color))    fill_color = null
    if (image_type == "byte")  p= new ByteProcessor(width, height)
    else if (image_type == "short") p= new ShortProcessor(width, height)
    else if (image_type == "color") p= new ColorProcessor(width, height)
    else if (image_type == "float") p= new FloatProcessor(width, height)
    else  throw ("Invalid image type " + str(image_type))
    ret = new ImagePlus(title, p)    
    if (fill_color != null){
        p.setColor(fill_color)
        p.resetRoi()
        p.fill()
    }
    return ret    
}
        
function sub_image(ip, x, y, width, height){
    /*
    Returns new ImagePlus
    */
    ip.setRoi(x, y, width, height)
    p=ip.getProcessor().crop()
    return new ImagePlus(ip.getTitle() + " subimage", p)    
}
        
function copy_image(ip){
    return ip.duplicate()
}
        
function copy_image_to(ip_source, ip_dest, x, y){
    ip_source.deleteRoi()    
    ip_source.copy()
    ip_dest.setRoi(x, y, ip_source.getWidth(), ip_source.getHeight())
    ip_dest.paste()
    ip_dest.changes = false
    ip_dest.deleteRoi()
}
        
function pad_image(ip, left, right, top, bottom, fill_color){
	if (!is_defined(left))    left = 0
	if (!is_defined(right))    right = 0
	if (!is_defined(top))    top = 0
	if (!is_defined(bottom))    bottom = 0
	if (!is_defined(fill_color))    fill_color = null
    p=ip.getProcessor()
    width = p.getWidth() + left + right
    height =  p.getHeight() + top + bottom        
    image_type = get_image_type(ip)
    ret = new_image(width, height, image_type, ip.getTitle() + " padded", fill_color)
    ip.deleteRoi()
    ip.copy()
    ret.setRoi(left, top, p.getWidth(), p.getHeight())
    ret.paste()
    ret.changes = false
    ret.deleteRoi()
    return ret    
}
        
function get_image_type(ip){
    /*
    Returns: "byte", "short", "color" or "float"
    */
    p=ip.getProcessor()
    if (p instanceof ShortProcessor) return "short"
    else if (p instanceof ColorProcessor) return "color"
    else if (p instanceof FloatProcessor) return "float"
    return "byte"
}
///////////////////////////////////////////////////////////////////////////////////////////////////
//Image type conversion
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function grayscale(ip, in_place){
	if (!is_defined(in_place))    in_place = true
    ip = (in_place==true) ? ip : ip.duplicate()
    ic = new ImageConverter(ip)
    ic.convertToGray8()
    return ip
}
        
function  get_channel(ip, channel){
    /*
    Return a channel from a color image as a new  ImagePlus.
    channel: "red", "green","blue", "alpha", "brightness", 
    */
    proc = ip.getProcessor()    
    if (channel ==   "red") ret = proc.getChannel(1, null)
    else if (channel == "green") ret = proc.getChannel(2, null)
    else if (channel == "blue") ret = proc.getChannel(3, null)
    else if (channel == "alpha") ret = proc.getChannel(4, null)
    else if (channel == "brightness") ret =	proc.getBrightness()
    else throw ("Invalid channel " + channel)
    return new ImagePlus(ip.getTitle() + " channel: " + channel, ret)
}
///////////////////////////////////////////////////////////////////////////////////////////////////
//Thresholder
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function threshold(ip, min_threshold, max_threshold, in_place){
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ip.getProcessor().setThreshold(min_threshold, max_threshold, ImageProcessor.NO_LUT_UPDATE)    
    WindowManager.setTempCurrentImage(ip)
    new Thresholder().run("mask")
    return ip
}
        
function auto_threshold(ip, dark_background, method, in_place){
	if (!is_defined(dark_background))    dark_background = false
	if (!is_defined(method))    method = AutoThresholder.getMethods()[0]
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ip.getProcessor().setAutoThreshold(method, dark_background , ImageProcessor.NO_LUT_UPDATE)
    WindowManager.setTempCurrentImage(ip)
    thresholder=new Thresholder().run("mask")
    return ip
}
///////////////////////////////////////////////////////////////////////////////////////////////////
//Binary functions    
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function binary_op(ip, op, dark_background, iterations, count, in_place){
    /*
    op = "erode","dilate", "open","close", "outline", "fill holes", "skeletonize"
    */
    if (!is_defined(dark_background))    dark_background = false
    if (!is_defined(iterations))    iterations = 1
    if (!is_defined(count))    count = 1
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    binary = new Binary()    
    Binary.count = count
    Binary.iterations = iterations
    Prefs.blackBackground=dark_background      
    binary.setup(op, ip)
    binary.run(ip.getProcessor())
    return ip
}
        
function binary_erode(ip, dark_background, iterations, count, in_place){
    return binary_op(ip, "erode", dark_background, iterations, count, in_place)
}
        
function binary_dilate(ip, dark_background, iterations, count, in_place){
    return binary_op(ip, "dilate", dark_background, iterations, count, in_place)
}
        
function binary_open(ip, dark_background, iterations, count, in_place){
    return binary_op(ip, "open", dark_background, iterations, count, in_place)
}
        
function binary_close(ip, dark_background, iterations, count, in_place){
    return binary_op(ip, "close", dark_background, iterations, count, in_place)
}
        
function binary_outline(ip, dark_background, in_place){
    return binary_op(ip, "outline", dark_background, 1, 1, in_place)
}
        
function binary_fill_holes(ip, dark_background, in_place){
    return binary_op(ip, "fill holes", dark_background, 1, 1, in_place)
}
        
function  binary_skeletonize(ip, dark_background, in_place){
    return binary_op(ip, "skeletonize", dark_background, 1, 1, in_place)
}    
        
function analyse_particles(ip, min_size, max_size, fill_holes, exclude_edges, extra_measurements,print_table, output_image, minCirc, maxCirc){
    /*
    Returns: tuple (ResultsTable results_table, ImagePlus output_image)
    output_image = "outlines", "overlay_outlines", "masks", "overlay_masks", "roi_masks" or null
    extra_measurements = mask with Measurements.CENTROID, PERIMETER, RECT, MIN_MAX, ELLIPSE, CIRCULARITY, AREA_FRACTION, INTEGRATED_DENSITY, INVERT_Y, FERET, KURTOSIS, MEDIAN, MODE, SKEWNESS, STD_DEV 
    Measurements is a mask of flags: https://imagej.nih.gov/ij/developer/api/ij/measure/Measurements.html.
    Returned ResultsTable hold public fields: https://imagej.nih.gov/ij/developer/api/ij/measure/ResultsTable.html
    
    */
    if (!is_defined(fill_holes))    fill_holes = true
    if (!is_defined(exclude_edges))    exclude_edges = true
    if (!is_defined(extra_measurements))    extra_measurements = 0
    if (!is_defined(print_table))    print_table = false
    if (!is_defined(output_image))    output_image = "outlines"
    if (!is_defined(minCirc))    minCirc = 0.0
    if (!is_defined(maxCirc))    maxCirc = 1.0
    rt = new ResultsTable()
    show_summary = false 
    var options = ParticleAnalyzer.SHOW_RESULTS | ParticleAnalyzer.CLEAR_WORKSHEET 
    /*
        ParticleAnalyzer.SHOW_ROI_MASKS | \
        //ParticleAnalyzer.RECORD_STARTS | \
        //ParticleAnalyzer.ADD_TO_MANAGER | \
        //ParticleAnalyzer.FOUR_CONNECTED | \
        //ParticleAnalyzer.IN_SITU_SHOW | \
        //ParticleAnalyzer.SHOW_NONE | \
    */
    if (show_summary)             options = options | ParticleAnalyzer.DISPLAY_SUMMARY 
    if (output_image == "outlines") options = options | ParticleAnalyzer.SHOW_OUTLINES 
    else if (output_image == "overlay_outlines") options = options | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES
    else if (output_image == "masks")     options = options | ParticleAnalyzer.SHOW_MASKS
    else if (output_image == "overlay_masks")     options = options | ParticleAnalyzer.SHOW_OVERLAY_MASKS
    else if (output_image == "roi_masks")     options = options | ParticleAnalyzer.SHOW_ROI_MASKS
    //ParticleAnalyzer.SHOW_ROI_MASKS
    if (exclude_edges)            options = options | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
    if (fill_holes)               options = options | ParticleAnalyzer.INCLUDE_HOLES
    measurements = Measurements.AREA  | Measurements.MEAN  |  Measurements.CENTER_OF_MASS | Measurements.RECT
    pa = new ParticleAnalyzer(options, measurements, rt, min_size, max_size, minCirc, maxCirc)
    pa.setHideOutputImage(true)
    ParticleAnalyzer.setResultsTable(rt)
    if (pa.analyze(ip)){
        if (print_table){
            print (rt.getColumnHeadings())
            for (var row= 0; row<rt.counter; row++){
                print (rt.getRowAsString(row))
            }
        }
        return [rt, pa.getOutputImage()]
    }
}        

///////////////////////////////////////////////////////////////////////////////////////////////////
//Image operators
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function op_image(ip1, ip2, op, float_result, in_place){
    /*
    op = "add","subtract",  "multiply","divide", "and", "or", "xor", "min", "max", "average", "difference" or "copy"
    */
    if (!is_defined(float_result))    float_result = false
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ic = new ImageCalculator()
    pars = op
    if (float_result) op = op + " float" 
    ic.run(pars, ip1, ip2)
    return ip1
}
        
function  op_const(ip, op, val, in_place){
    /*
    op = "add","subtract",  "multiply","divide", "and", "or", "xor", "min", "max", "gamma", "set" or "log", "exp", "sqr", "sqrt","abs"
    */
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    pr = ip.getProcessor()
    if (op == 'add') pr.add(val)
    else if (op == 'sub') pr.subtract(val)
    else if (op == 'multiply') pr.multiply(val)
    else if (op == 'divide' && val!=0) pr.multiply(1.0/val)
    else if (op == 'and') pr.and(val)
    else if (op == 'or') pr.or(val)
    else if (op == 'xor') pr.xor(val)
    else if (op == 'min') {pr.min(val);pr.resetMinAndMax()}
    else if (op == 'max') {pr.max(val);pr.resetMinAndMax()}
    else if (op == 'gamma' && (0.05 < val) && (val < 5.0)) pr.gamma(val)
    else if (op == 'set') pr.set(val)
    else if (op == 'log') pr.log()
    else if (op == 'exp') pr.exp()        
    else if (op == 'sqr') pr.sqr()        
    else if (op == 'sqrt') pr.sqrt()        
    else if (op == 'abs') {pr.abs();pr.resetMinAndMax()}
    else throw "Invalid operation " + op
    return ip
}
        
function op_fft(ip1, ip2, op, do_inverse){
    /*
    Images must have same sizes, and multiple of  2  height and width.
    op = "correlate" (complex conjugate multiply), "convolve" (Fourier domain multiply), "deconvolve" (Fourier domain divide)
    */
    if (!is_defined(do_inverse))    do_inverse = true
    var op_index
    if   (op == "correlate") op_index = 0
    else if (op == "convolve") op_index = 1
    else if (op == "deconvolve")  op_index = 2
    else throw "Invalid operation " + op  
    return new FFTMath().doMath(ip1, ip2, op_index, do_inverse)
}
        
function op_rank(ip, op, kernel_radius ,  dark_outliers ,threshold, in_place){
    /*
    op = "mean", "min", "max", "variance", "median", "close_maxima", "open_maxima", "remove_outliers", "remove_nan", "despeckle"
    */
    if (!is_defined(kernel_radius))    kernel_radius = 1
    if (!is_defined(dark_outliers))    dark_outliers = false
    if (!is_defined(threshold))    threshold = 50
    if (!is_defined(in_place))    in_place = true
    var filter_type 
    if   (op == "mean") filter_type = RankFilters.MEAN
    else if (op == "min") filter_type = RankFilters.MIN
    else if (op == "max") filter_type = RankFilters.MAX
    else if (op == "variance") filter_type = RankFilters.VARIANCE
    else if (op == "median") filter_type = RankFilters.MEDIAN
    else if (op == "close_maxima") filter_type = RankFilters.CLOSE
    else if (op == "open_maxima") filter_type = RankFilters.OPEN
    else if (op == "remove_outliers") filter_type = RankFilters.OUTLIERS
    else if (op == "remove_nan") filter_type = RankFilters.REMOVE_NAN
    else if (op == "despeckle") {filter_type = RankFilters.MEDIAN, kernel_radius = 1}
    else throw "Invalid operation " + op
    ip = in_place ? ip : ip.duplicate()
    new RankFilters().rank(ip.getProcessor(), kernel_radius, filter_type, dark_outliers ? RankFilters.DARK_OUTLIERS : RankFilters.BRIGHT_OUTLIERS ,threshold)
    return ip    
}
        
function op_edm(ip, op, dark_background, in_place){
    /*
    Euclidian distance map & derived  operations
    op ="edm", "watershed","points", "voronoi"
    */    
    if (!is_defined(op))    op = "edm"
    if (!is_defined(dark_background))    dark_background = false
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    pr = ip.getProcessor()
    var edm=new EDM()
    Prefs.blackBackground=dark_background      
    if  (op=="edm"){      
        //pr.setPixels(0, edm.makeFloatEDM(pr, 0, false));
        //pr.resetMinAndMax();                
        if (dark_background)  pr.invert()
        edm.toEDM(pr)
    }else{
        edm.setup(op, ip)
        edm.run(pr)
    }
    return ip
}
        
function watershed(ip, dark_background, in_place){
	if (!is_defined(dark_background))    dark_background = false
	if (!is_defined(in_place))    in_place = true
    return op_edm(ip, "watershed", dark_background, in_place)
}
        
function ultimate_points(ip, dark_background, in_place){
	if (!is_defined(dark_background))    dark_background = false
	if (!is_defined(in_place))    in_place = true
    return op_edm(ip, "points", dark_background, in_place)
}
        
function veronoi(ip, dark_background, in_place){
	if (!is_defined(dark_background))    dark_background = false
	if (!is_defined(in_place))    in_place = true
    return op_edm(ip, "voronoi", dark_background, in_place)
}
        
function edm(ip, dark_background, in_place){
	if (!is_defined(dark_background))    dark_background = false
	if (!is_defined(in_place))    in_place = true
    return op_edm(ip, "edm", dark_background, in_place)
}
        
function op_filter(ip, op, in_place){

    /*
    This is redundant as just calls processor methods.
    op ="invert", "smooth", "sharpen", "edge", "add"
    */    
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    f = new Filters()
    f.setup(op, ip )
    f.run(ip.getProcessor())
    return ip
}
///////////////////////////////////////////////////////////////////////////////////////////////////
//Other operations
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function gaussian_blur(ip, sigma_x, sigma_y, accuracy, in_place){
	if (!is_defined(sigma_x))    sigma_x = 3.0
	if (!is_defined(sigma_y))    sigma_y = 3.0
	if (!is_defined(accuracy))    accuracy = 0.01
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    new GaussianBlur().blurGaussian(ip.getProcessor(),  sigma_x, sigma_y, accuracy)
    return ip
}
        
function find_maxima(ip, tolerance, threshold, output_type, exclude_on_edges, is_edm ){
    /*
    Returns new ImagePlus
    tolerance:  maxima are accepted only if protruding more than this value  from the ridge to a higher maximum
    threshhold: minimum height of a maximum (uncalibrated);
    output_type = SINGLE_POINTS, IN_TOLERANCE or SEGMENTED.  No output image is created for output types POINT_SELECTION, LIST and COUNT.   
    */
    if (!is_defined(tolerance))    tolerance = 25
    if (!is_defined(threshold))    threshold =  ImageProcessor.NO_THRESHOLD
    if (!is_defined(output_type))    output_type = MaximumFinder.IN_TOLERANCE
    if (!is_defined(exclude_on_edges))    exclude_on_edges = false
    if (!is_defined(is_edm))    is_edm = false
    byte_processor =  new MaximumFinder().findMaxima(ip.getProcessor(), tolerance, threshold, output_type, exclude_on_edges, is_edm)     
    return new ImagePlus(ip.getTitle() + " maxima", byte_processor)
}
        
function get_maxima_points(ip, tolerance, exclude_on_edges){
    if (!is_defined(tolerance))    tolerance = 25
    if (!is_defined(exclude_on_edges))    exclude_on_edges = false
    polygon =  new MaximumFinder().getMaxima(ip.getProcessor(), tolerance, exclude_on_edges)
    return (polygon.xpoints, polygon.ypoints)
}
        
function enhance_contrast(ip, equalize_histo, saturated_pixels, normalize, stack_histo, in_place){
	if (!is_defined(equalize_histo))    equalize_histo = true
	if (!is_defined(saturated_pixels))    saturated_pixels =  0.5
	if (!is_defined(normalize))    normalize = false
	if (!is_defined(stack_histo))    stack_histo = false
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ce = new ContrastEnhancer()
    if (equalize_histo){
        ce.equalize(ip.getProcessor());
    } else{
        ce.stretchHistogram(ip.getProcessor(), saturated_pixels)
        if (normalize){
            ip.getProcessor().setMinAndMax(0,(ip.getProcessor().getBitDepth()==32) ? 1.0 : ip.getProcessor().maxValue())
        }
    }
    return ip   
}
        
function shadows(ip, op, in_place){
    /*
    op ="north","northeast", "east", "southeast","south", "southwest", "west","northwest"
    */
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    shadows= new Shadows()
    shadows.setup(op, ip)
    shadows.run(ip.getProcessor())
    return ip
}
        
function unsharp_mask(ip, sigma, weight, in_place){
    /*
    Float processor
    */    
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ip.getProcessor().snapshot()
    unsharp=new UnsharpMask()
    USmask.setup("  ", ip)
    USmask.sharpenFloat( ip.getProcessor(),sigma, weight)
    return ip
}
        
function subtract_background(ip, radius, create_background, dark_background, use_paraboloid, do_presmooth, correctCorners, rgb_brightness, in_place){
	if (!is_defined(radius))    radius = 50
	if (!is_defined(create_background))    create_background = false
	if (!is_defined(dark_background))    dark_background = false
	if (!is_defined(use_paraboloid))    use_paraboloid = true
	if (!is_defined(do_presmooth))    do_presmooth = true
	if (!is_defined(correctCorners))    correctCorners = true
    if (!is_defined(rgb_brightness))    rgb_brightness = false
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    if (rgb_brightness)
        new BackgroundSubtracter().rollingBallBrightnessBackground(ip.getProcessor(), radius, create_background,! dark_background, use_paraboloid, do_presmooth, correctCorners)
    else
        new BackgroundSubtracter().rollingBallBackground(ip.getProcessor(), radius, create_background, !dark_background, use_paraboloid, do_presmooth, correctCorners)        
    return ip
}
///////////////////////////////////////////////////////////////////////////////////////////////////
//FFT
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function image_fft(ip, show){
	if (!is_defined(show))    show = true
    WindowManager.setTempCurrentImage(ip)
    fft = new FFT()
    fft.run("fft")    
    //TODO: how to avoid it to be created?
    //ret =  ImagePlus("FHT of " + ip.getTitle(), WindowManager.getCurrentImage().getProcessor())
    ret = WindowManager.getCurrentImage()
    if (show == false)
        WindowManager.getCurrentImage().hide()
    return ret
}
        
function image_ffti(ip, show){
	if (!is_defined(show))    show = true
    WindowManager.setTempCurrentImage(ip)
    fft = new FFT()
    fft.run("inverse")
    //WindowManager.getCurrentImage().hide()
    //TODO: how to avoid it to be created?
    //ret = WindowManager.getCurrentImage()
    //WindowManager.getCurrentImage().hide()
    //ret =  ImagePlus(ip.getTitle() + " ffti", WindowManager.getCurrentImage().getProcessor())
    ret = WindowManager.getCurrentImage()
    if (show == false)
        WindowManager.getCurrentImage().hide()
    
    return ret
}
        
function bandpass_filter(ip, small_dia_px, large_dia_px, suppress_stripes, stripes_tolerance_direction, autoscale_after_filtering, saturate_if_autoscale, display_filter, in_place){
    /*
    suppress_stripes = 0 for none, 1 for horizontal, 2 for vertical
    */
    if (!is_defined(suppress_stripes))    suppress_stripes = 0
    if (!is_defined(stripes_tolerance_direction))    stripes_tolerance_direction = 5.0
    if (!is_defined(autoscale_after_filtering))    autoscale_after_filtering = false
    if (!is_defined(saturate_if_autoscale))    saturate_if_autoscale = false
    if (!is_defined(display_filter))    display_filter = false
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    filter=  new FFTFilter();
    FFTFilter.filterLargeDia = large_dia_px
    FFTFilter.filterSmallDia = small_dia_px
    FFTFilter.choiceIndex = suppress_stripes
    FFTFilter.toleranceDia = stripes_tolerance_direction
    FFTFilter.doScalingDia = autoscale_after_filtering
    FFTFilter.saturateDia = saturate_if_autoscale
    FFTFilter.displayFilter =display_filter
    filter.setup(null, ip);
    filter.run(ip.getProcessor())
    return ip
}
///////////////////////////////////////////////////////////////////////////////////////////////////
//Convolution
///////////////////////////////////////////////////////////////////////////////////////////////////

KERNEL_BLUR = [[0.1111, 0.1111, 0.1111], [0.1111, 0.1111, 0.1111], [0.1111, 0.1111, 0.1111]]
KERNEL_SHARPEN = [[0.0, -0.75, 0.0], [-0.75, 4.0, -0.75], [0.0, -0.75, 0.0]]
KERNEL_SHARPEN_2 = [[-1.0, -1.0, -1.0], [-1.0, 9.0, -1.0], [-1.0, -1.0, -1.0]]
KERNEL_LIGHT = [[0.1, 0.1, 0.1], [0.1, 1.0, 0.1],[0.1, 0.1, 0.1]]
KERNEL_DARK = [[0.01, 0.01, 0.01],[0.01, 0.5, 0.01],[0.01, 0.01, 0.01]]
KERNEL_EDGE_DETECT = [[0.0, -0.75, 0.0], [-0.75, 3.0, -0.75], [0.0, -0.75, 0.0]]
KERNEL_EDGE_DETECT_2 = [[-0.5, -0.5, -0.5], [-0.5, 4.0, -0.5], [-0.5, -0.5, -0.5]]
KERNEL_DIFFERENTIAL_EDGE_DETECT = [[-1.0, 0.0, 1.0], [0.0, 0.0, 0.0], [1.0, 0.0, -1.0]]
KERNEL_PREWITT = [[-2.0, -1.0, 0.0], [-1.0,  0.0,  1.0 ], [0.0, 1.0, 2.0]]
KERNEL_SOBEL = [[2.0, 2.0, 0.0], [2.0,  0.0,  -2.0 ],  [0.0, -2.0, -2.0]]
        
function  convolve(ip, kernel, in_place){
    /*
    kernel: list of lists
    */
    if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    kernel_width = kernel.length
    kernel_height= kernel[0].length
    kernel =flatten(kernel)
    
    //Convolver().convolve(ip.getProcessor(), kernel, kernel_width, kernel_height) 
    //ip.getProcessor().convolve(to_array(kernel, '[f'), kernel_width, kernel_height)
    ip.getProcessor().convolve(kernel, kernel_width, kernel_height)
    return ip
}
        
///////////////////////////////////////////////////////////////////////////////////////////////////
//Shortcut  to ImageProcessor methods
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function invert(ip, in_place){
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ip.getProcessor().invert()
    return ip
}
        
function smooth(ip, in_place){
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ip.getProcessor().smooth()
    return ip
}
        
function sharpen(ip, in_place){
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ip.getProcessor().sharpen()
    return ip
}
        
function edges(ip, in_place){ //Sobel
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ip.getProcessor().findEdges()
    return ip
}
        
function noise(ip, sigma, in_place){
	if (!is_defined(sigma))    sigma =  25.0
	if (!is_defined(in_place))    in_place = true
    ip = in_place ? ip : ip.duplicate()
    ip.getProcessor().noise(sigma)
    return ip
}
        
function remap(ip, min, max, in_place){
	if (!is_defined(min))    min = null
	if (!is_defined(max))    max = null
	if (!is_defined(in_place))    in_place = true	
    ip = in_place ? ip : ip.duplicate()
    if ((min ==null) || (max == null)){
        stats = get_statistics(ip, Measurements.MIN_MAX)
        if (min== null) min = stats.min
        if (max == null) max = stats.max
    }
    ip.getProcessor().setMinAndMax(min, max)
    return ip
}
        
function set_lut(ip, r, g, b){
    /*
    r,g and b are lists of 256 integers
    */
    r =  r.map(function(x) {return (x<128) ? x : (x-256 )})
    g =  g.map(function(x) {return (x<128) ? x : (x-256 )})
    b =  b.map(function(x) {return (x<128) ? x : (x-256 )})    
    ip.setLut(new LUT(to_array(r,'b'),to_array(g,'b'),to_array(b,'b')))
}
        
function resize(ip, width, height){
    /*
    Returns new ImagePlus
    */
    p = ip.getProcessor().resize(width, height)
    return new ImagePlus(ip.getTitle() + " resized", p)
}
        
function binning(ip, factor){
    p=ip.getProcessor().bin(factor)
    return new ImagePlus(ip.getTitle() + " resized", p)
}
        
function get_histogram(ip, hist_min, hist_max, hist_bins, roi){
    /*
    hist_min, hist_max, hist_bins used only for float images (otherwise fixed to 0,255,256)
    roi is list  [x,y,w,h]
    */
    if (!is_defined(hist_min))    hist_min = 0
    if (!is_defined(hist_max))    hist_max = 0
    if (!is_defined(hist_bins))    hist_bins = 256
    if (!is_defined(roi))    roi = null
    if (roi == null) {
    	ip.deleteRoi()    
    } else {
    	ip.setRoi(roi[0],roi[1],roi[2],roi[3])
    }
    image_statistics = ip.getStatistics(0, hist_bins, hist_min, hist_max)    
    return to_array(image_statistics.getHistogram())
}
        
function get_array(ip){
    return ip.getProcessor().getIntArray()
}
        
function get_line(ip, x1, y1, x2, y2){
    return ip.getProcessor().getLine(x1, y1, x2, y2)
}
        
function get_pixel_range(ip){
    return (ip.getProcessor().getMin(), ip.getProcessor().getMax())
}
        
function get_num_channels(ip){
    return ip.getProcessor().getNChannels()
}
        
function is_binary(ip){
    return ip.getProcessor().isBinary()    
}
        
function get_pixel(ip, x, y){
    return ip.getProcessor().getPixel(x,y)
}
        
function get_pixel_array(ip, x, y){
    a = [0]*get_num_channels(ip)
    return ip.getProcessor().getPixel(x,y,a)
}
        
function get_pixels(ip){
    return ip.getProcessor().getPixels()
}
        
function get_width(ip){
    return ip.getProcessor().getWidth()
}
        
function get_height(ip){
    return ip.getProcessor().getHeight()
}
        
function get_row(ip, y){
    a = [0]*get_width(ip)
    array = jarray.array(a,'i')
    ip.getProcessor().getRow(0, y, array, get_width(ip))
    return array
}
        
function get_col(ip, x){
    a = [0]*get_height(ip)
    array = jarray.array(a,'i')
    ip.getProcessor().getColumn(x, 0, array, get_height(ip))
    return array     
}
        
function get_statistics(ip, measurements){
    /*
    Measurements is a mask of flags: https://imagej.nih.gov/ij/developer/api/ij/measure/Measurements.html.
    Statistics object hold public fields: https://imagej.nih.gov/ij/developer/api/ij/process/ImageStatistics.html
    */
    if (!is_defined(measurements))    measurements = null
    if (measurements == null){
        return ip.getStatistics()
    } else {
        return ip.getStatistics(measurements)
    }
}
///////////////////////////////////////////////////////////////////////////////////////////////////
//Image stack functions
///////////////////////////////////////////////////////////////////////////////////////////////////
        
function create_stack(ip_list, keep, title){
    if (!is_defined(keep))    keep = true
    if (!is_defined(title))    title = null
    stack = new Concatenator().concatenate(ip_list, keep)
    if (title != null)
        stack.setTitle(title)
    return stack
}
        
function reslice(stack, start_at, vertically, flip, output_pixel_spacing, avoid_interpolation, title){
	if (!is_defined(start_at))    start_at = "Top"
	if (!is_defined(vertically))    vertically = true
	if (!is_defined(flip))    flip = true
	if (!is_defined(output_pixel_spacing))    output_pixel_spacing = 1.0
	if (!is_defined(avoid_interpolation))    avoid_interpolation = true
	if (!is_defined(title))    title = null
    ss = new Slicer()
    ss.rotate = vertically
    ss.startAt = start_at 
    ss.flip = flip
    ss.nointerpolate =  avoid_interpolation
    ss.outputZSpacing = output_pixel_spacing
    stack =  ss.reslice(stack)
    if (title != null)
        stack.setTitle(title)
    return stack
}