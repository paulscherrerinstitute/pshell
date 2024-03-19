####################################################################################################
#  Facade to ImageJ functionality
####################################################################################################

#More information on:
#   Image:   https://imagej.nih.gov/ij/docs/guide/146-28.html#toc-Section-28
#   Process: https://imagej.nih.gov/ij/docs/guide/146-29.html#toc-Section-29
#   Analyze: https://imagej.nih.gov/ij/docs/guide/146-30.html#toc-Section-30

import ch.psi.utils.Convert as Convert
import ch.psi.pshell.imaging.Utils as ImagingUtils
from startup import get_context, expand_path, is_string
import java.awt.image.BufferedImage as BufferedImage
import org.python.core.PyArray as PyArray
import jarray
import os

import ij.IJ as IJ
import ij.ImageJ as ImageJ
import ij.WindowManager as WindowManager
import ij.ImagePlus as ImagePlus
import ij.ImageStack as ImageStack
import ij.Prefs as Prefs
import ij.io.FileSaver as FileSaver
import ij.io.Opener as Opener
from ij.gui import Roi

import ij.process.ImageProcessor as ImageProcessor
import ij.process.ByteProcessor as ByteProcessor
import ij.process.ShortProcessor as ShortProcessor
import ij.process.ColorProcessor as ColorProcessor
import ij.process.FloatProcessor as FloatProcessor
import ij.process.ImageConverter as ImageConverter
import ij.process.AutoThresholder as AutoThresholder
import ij.process.LUT as LUT
import ij.measure.Measurements as Measurements
import ij.measure.ResultsTable as ResultsTable
import ij.plugin.filter.Analyzer as Analyzer
import ij.plugin.filter.GaussianBlur as GaussianBlur
import ij.plugin.filter.Filters as Filters
import ij.plugin.filter.FFTFilter as FFTFilter
import ij.plugin.filter.BackgroundSubtracter as BackgroundSubtracter
import ij.plugin.filter.EDM as EDM
import ij.plugin.filter.Shadows as Shadows
import ij.plugin.filter.UnsharpMask as UnsharpMask
import ij.plugin.filter.MaximumFinder as MaximumFinder 
import ij.plugin.filter.EDM as EDM
import ij.plugin.filter.Shadows as Shadows
import ij.plugin.filter.UnsharpMask as UnsharpMask
import ij.plugin.filter.RankFilters as RankFilters
import ij.plugin.filter.Convolver as Convolver
import ij.plugin.filter.ParticleAnalyzer as ParticleAnalyzer

import ij.plugin.ContrastEnhancer as ContrastEnhancer
import ij.plugin.Thresholder as Thresholder
import ij.plugin.ImageCalculator as ImageCalculator
import ij.plugin.FFT as FFT
import ij.plugin.Concatenator as Concatenator

#ImageJ customizations
import ch.psi.pshell.imaging.ij.FFTMath as FFTMath
import ch.psi.pshell.imaging.ij.FFTFilter as FFTFilter
import ch.psi.pshell.imaging.ij.Binary as Binary
import ch.psi.pshell.imaging.ij.Slicer as Slicer


#This eliminates the error messages due to the bug on ij.gui.ImageWindow row 555 (ij is null)
if not "_image_j" in globals().keys():
    _image_j = ImageJ(None, ImageJ.NO_SHOW)
    
###################################################################################################
#Image creation, copying & saving
###################################################################################################
def load_image(image, title = None):    
    """
    image: file name or BufferedImage
    """    
    if is_string(image):
        try:
            file = expand_path(image)
        except:
            pass
        try:
            image = ImagingUtils.newImage(file)
        except:
            #try loading from assembly
            image = get_context().setup.getAssemblyImage(image)
        if title is None:
            title = os.path.basename(file)
    return ImagePlus("img" if title is None else title, image)

def load_array(array, width=None, height=None, title = "img"):    
    """
    array: 1d array if width and height defined , or else 2d array to be flattened.
    """    
    #2D
    if (width==None) and (height==None):
        if array.typecode == '[B': proc = ByteProcessor(len(array[0]), len(array), Convert.flatten(array))
        elif array.typecode == '[S': proc = ShortProcessor(len(array[0]), len(array), Convert.flatten(array), None)
        elif array.typecode in ['[I','[F', '[D']: proc = FloatProcessor(len(array[0]), len(array), Convert.flatten(array))        
        else: raise Exception("Invalid array type")   
    #1D
    else:
        if (len(array) > width*height):
            array = array[:(width*height)]
        if array.typecode == 'b': proc = ByteProcessor(width, height, array)
        elif array.typecode == 'h': proc = ShortProcessor(width, height, array, None)
        elif array.typecode in ['i','f','d']: proc = FloatProcessor(width, height, array)
        else: raise Exception("Invalid array type")   
    return ImagePlus(title, proc)

def to_ip(img):
    if isinstance (img, ImagePlus):
        return img
    if isinstance(img,PyArray):
        return load_array(img)
    return load_image(img)

def save_image(ip, path=None, format = None, metadata={}):
    """
    Saves image or stack
    If parameters omitted, saves image again in same location, with same format.
    """
    fs = FileSaver(ip)

    info = ""
    for key,val in metadata.items():
        info = info + ("\n" if len(info)>0 else "") + str(key) + ": " + str(val)        
    ip.setProperty("Info", info)

    if path == None: fs.save()
    else:
        try:
            path = expandPath(path)
        except:
            pass           
        if format == "bmp": fs.saveAsBmp(path) 
        elif format == "fits": fs.saveAsFits(path) 
        elif format == "gif": fs.saveAsGif(path) 
        elif format == "jpeg": fs.saveAsJpeg(path) 
        elif format == "lut": fs.saveAsLut(path) 
        elif format == "pgm": fs.saveAsPgm(path) 
        elif format == "png": fs.saveAsPng(path) 
        elif format == "raw" and ip.getImageStackSize()>1: fs.saveAsRawStack(path) 
        elif format == "raw": fs.saveAsRaw(path) 
        elif format == "txt": fs.saveAsText(path)
        elif format == "tiff" and ip.getImageStackSize()>1: fs.saveAsTiffStack(path)
        elif format == "tiff": fs.saveAsTiff(path)
        elif format == "zip": fs.saveAsZip(path)   


def open_image(path, index=1):
    """
    Open file using ij.io,Opener
    """
    try:
        path = expand_path(path)
    except:
        pass  
    opener = Opener()
    return opener.openImage(path, index)

def new_image(width, height, image_type="byte", title = "img", fill_color = None):
    """
    type = "byte", "short", "color" or "float"
    """
    if image_type == "byte": p=ByteProcessor(width, height)
    elif image_type == "short": p=ShortProcessor(width, height)
    elif image_type == "color": p=ColorProcessor(width, height)
    elif image_type == "float": p=FloatProcessor(width, height)
    else: raise Exception("Invalid image type " + str(image_type))
    ret = ImagePlus(title, p)    
    if fill_color is not None:
        p.setColor(fill_color)
        p.resetRoi()
        p.fill()
    return ret    

def get_ip_array(ip):
    """
    Returns data array of ImagePlus
    """
    if type(ip.getProcessor()) == FloatProcessor:
        return ip.getProcessor().getFloatArray()
    else:
        return ip.getProcessor().getIntArray()


def sub_image(ip, x, y, width, height):
    """
    Returns new ImagePlus
    """
    ip.setRoi(x, y, width, height)
    p=ip.getProcessor().crop()
    return ImagePlus(ip.getTitle() + " subimage", p)    

def copy_image(ip):
    return ip.duplicate()

def copy_image_to(ip_source, ip_dest, x, y):
    ip_source.deleteRoi()    
    ip_source.copy()
    ip_dest.setRoi(x, y, ip_source.getWidth(), ip_source.getHeight())
    ip_dest.paste()
    ip_dest.changes = False
    ip_dest.deleteRoi()
        
def pad_image(ip, left=0, right=0, top=0, bottom=0, fill_color = None):
    p=ip.getProcessor()
    width = p.getWidth() + left + right
    height =  p.getHeight() + top + bottom        
    image_type = get_image_type(ip)
    ret = new_image(width, height, image_type, ip.getTitle() + " padded", fill_color)
    ip.deleteRoi()
    ip.copy()
    ret.setRoi(left, top, p.getWidth(), p.getHeight())
    ret.paste()
    ret.changes = False
    ret.deleteRoi()
    return ret    

def get_image_type(ip):
    """
    Returns: "byte", "short", "color" or "float"
    """
    p=ip.getProcessor()
    if type(p) == ShortProcessor: return "short"
    elif type(p) == ColorProcessor: return "color"
    elif type(p) == FloatProcessor: return "float"
    return "byte"

###################################################################################################
#Image measurements
###################################################################################################

def get_measurement(ip, measurement):
    """
    Return image measurement:
    "Area", "Mean", "StdDev", "Mode", "Min", "Max", "X", "Y", "XM", "YM", "Perim.", "BX", "BY",
    "Width", "Height", "Major", "Minor", "Angle", "Circ.", "Feret", "IntDen", "Median", "Skew",
    "Kurt", "%Area", "RawIntDen", "Ch", "Slice", "Frame", "FeretX", "FeretY", "FeretAngle",
    "MinFeret", "AR", "Round", "Solidity", "MinThr" or "MaxThr"
    """
    return IJ.getValue(ip,measurement)

###################################################################################################
#Image type conversion
###################################################################################################
def grayscale(ip, do_scaling=None, in_place=True):
    ip = ip if in_place else ip.duplicate()
    ic = ImageConverter(ip)
    if do_scaling is not None:
        ic.setDoScaling(do_scaling)
    ic.convertToGray8()
    return ip

def get_channel(ip, channel):
    """
    Return a channel from a color image as a new  ImagePlus.
    channel: "red", "green","blue", "alpha", "brightness", 
    """
    proc = ip.getProcessor()    
    if channel ==   "red": ret = proc.getChannel(1, None)
    elif channel == "green": ret = proc.getChannel(2, None)
    elif channel == "blue": ret = proc.getChannel(3, None)
    elif channel == "alpha": ret = proc.getChannel(4, None)
    elif channel == "brightness": ret =	proc.getBrightness()
    else: raise Exception("Invalid channel " + str(channel))
    return ImagePlus(ip.getTitle() + " channel: " + channel, ret)

###################################################################################################
#Thresholder
###################################################################################################
def threshold(ip, min_threshold, max_threshold, in_place=True):
    ip = ip if in_place else ip.duplicate()
    ip.getProcessor().setThreshold(min_threshold, max_threshold, ImageProcessor.NO_LUT_UPDATE)    
    WindowManager.setTempCurrentImage(ip)
    Thresholder().run("mask")
    return ip

def auto_threshold(ip, dark_background = False, method = AutoThresholder.getMethods()[0], in_place=True):
    ip = ip if in_place else ip.duplicate()
    ip.getProcessor().setAutoThreshold(method, dark_background , ImageProcessor.NO_LUT_UPDATE)
    WindowManager.setTempCurrentImage(ip)
    thresholder=Thresholder().run("mask")
    return ip

###################################################################################################
#Binary functions    
###################################################################################################
def binary_op(ip, op, dark_background=False, iterations=1, count=1, in_place=True):
    """
    op = "erode","dilate", "open","close", "outline", "fill holes", "skeletonize"
    """
    ip = ip if in_place else ip.duplicate()
    binary = Binary(count, iterations, dark_background )        
    binary.setup(op, ip)
    binary.run(ip.getProcessor())
    return ip

def binary_erode(ip, dark_background=False, iterations=1, count=1, in_place=True):
    return binary_op(ip, "erode", dark_background, iterations, count, in_place)

def binary_dilate(ip, dark_background=False, iterations=1, count=1, in_place=True):
    return binary_op(ip, "dilate", dark_background, iterations, count, in_place)

def binary_open(ip, dark_background=False, iterations=1, count=1, in_place=True):
    return binary_op(ip, "open", dark_background, iterations, count, in_place)

def binary_close(ip, dark_background=False, iterations=1, count=1, in_place=True):
    return binary_op(ip, "close", dark_background, iterations, count)

def binary_outline(ip, dark_background=False, in_place=True):
    return binary_op(ip, "outline", dark_background, in_place=in_place)
    
def binary_fill_holes(ip, dark_background=False, in_place=True):
    return binary_op(ip, "fill holes", dark_background, in_place=in_place)

def binary_skeletonize(ip, dark_background=False, in_place=True):
    return binary_op(ip, "skeletonize", dark_background, in_place=in_place)

def  analyse_particles(ip, min_size, max_size, fill_holes = True, exclude_edges = True, extra_measurements = 0, \
                       print_table = False, output_image = "outlines", minCirc = 0.0, maxCirc = 1.0):
    """
    Returns: tuple (ResultsTable results_table, ImagePlus output_image)
    output_image = "outlines", "overlay_outlines", "masks", "overlay_masks", "roi_masks" or None
    extra_measurements = mask with Measurements.CENTROID, PERIMETER, RECT, MIN_MAX, ELLIPSE, CIRCULARITY, AREA_FRACTION, INTEGRATED_DENSITY, INVERT_Y, FERET, KURTOSIS, MEDIAN, MODE, SKEWNESS, STD_DEV 
    Measurements is a mask of flags: https://imagej.nih.gov/ij/developer/api/ij/measure/Measurements.html.
    Returned ResultsTable hold public fields: https://imagej.nih.gov/ij/developer/api/ij/measure/ResultsTable.html
    
    """
    rt = ResultsTable()
    show_summary = False 
    options = ParticleAnalyzer.SHOW_RESULTS | ParticleAnalyzer.CLEAR_WORKSHEET 
    """
        ParticleAnalyzer.SHOW_ROI_MASKS | \
        #ParticleAnalyzer.RECORD_STARTS | \
        #ParticleAnalyzer.ADD_TO_MANAGER | \
        #ParticleAnalyzer.FOUR_CONNECTED | \
        #ParticleAnalyzer.IN_SITU_SHOW | \
        #ParticleAnalyzer.SHOW_NONE | \
    """
    if show_summary:             options = options | ParticleAnalyzer.DISPLAY_SUMMARY 
    if output_image == "outlines": options = options | ParticleAnalyzer.SHOW_OUTLINES 
    elif output_image == "overlay_outlines": options = options | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES
    elif output_image == "masks":     options = options | ParticleAnalyzer.SHOW_MASKS
    elif output_image == "overlay_masks":     options = options | ParticleAnalyzer.SHOW_OVERLAY_MASKS
    elif output_image == "roi_masks":     options = options | ParticleAnalyzer.SHOW_ROI_MASKS
    #ParticleAnalyzer.SHOW_ROI_MASKS
    if exclude_edges:            options = options | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
    if fill_holes:               options = options | ParticleAnalyzer.INCLUDE_HOLES
    measurements = Measurements.AREA  | Measurements.MEAN  |  Measurements.CENTER_OF_MASS | Measurements.RECT
    pa = ParticleAnalyzer(options, measurements, rt, min_size, max_size, minCirc, maxCirc)
    pa.setHideOutputImage(True)
    pa.setResultsTable(rt)
    if pa.analyze(ip):
        if print_table:
            print rt.getColumnHeadings()
            for row in range (rt.counter):
                print rt.getRowAsString(row)    
        return (rt, pa.getOutputImage())

###################################################################################################
#Image operators
###################################################################################################
def op_image(ip1, ip2, op, float_result=False, in_place=True):
    """
    op = "add","subtract",  "multiply","divide", "and", "or", "xor", "min", "max", "average", "difference" or "copy"
    """
    ip1 = ip1 if in_place else ip1.duplicate()
    ic = ImageCalculator()
    pars = op
    if float_result:
        op = op + " float" 
    ic.run(pars, ip1, ip2)
    return ip1

def op_const(ip, op, val, in_place=True):
    """
    op = "add","subtract",  "multiply","divide", "and", "or", "xor", "min", "max", "gamma", "set" or "log", "exp", "sqr", "sqrt","abs"
    """
    ip = ip if in_place else ip.duplicate()
    pr = ip.getProcessor()
    if op == 'add': pr.add(val)
    elif op == 'sub': pr.subtract(val)
    elif op == 'multiply': pr.multiply(val)
    elif op == 'divide' and val!=0: pr.multiply(1.0/val)
    elif op == 'and': pr.and(val)
    elif op == 'or': pr.or(val)
    elif op == 'xor': pr.xor(val)
    elif op == 'min': pr.min(val);pr.resetMinAndMax()
    elif op == 'max': pr.max(val);pr.resetMinAndMax()
    elif op == 'gamma' and 0.05 < val < 5.0: pr.gamma(val)
    elif op == 'set': pr.set(val)
    elif op == 'log': pr.log()
    elif op == 'exp': pr.exp()        
    elif op == 'sqr': pr.sqr()        
    elif op == 'sqrt': pr.sqrt()        
    elif op == 'abs': pr.abs();pr.resetMinAndMax()
    else: raise Exception("Invalid operation " + str(op))    
    return ip

def op_fft(ip1, ip2, op, do_inverse = True) :
    """
    Images must have same sizes, and multiple of  2  height and width.
    op = "correlate" (complex conjugate multiply), "convolve" (Fourier domain multiply), "deconvolve" (Fourier domain divide)
    """
    if   op == "correlate": op_index = 0
    elif op == "convolve":  op_index = 1
    elif op == "deconvolve":  op_index = 2
    else: raise Exception("Invalid operation " + str(op))    
    return FFTMath().doMath(ip1, ip2, op_index, do_inverse)

def op_rank(ip, op, kernel_radius =1 ,  dark_outliers = False ,threshold = 50, in_place=True):
    """
    op = "mean", "min", "max", "variance", "median", "close_maxima", "open_maxima", "remove_outliers", "remove_nan", "despeckle"
    """
    if   op == "mean": filter_type = RankFilters.MEAN
    elif op == "min": filter_type = RankFilters.MIN
    elif op == "max": filter_type = RankFilters.MAX
    elif op == "variance": filter_type = RankFilters.VARIANCE
    elif op == "median": filter_type = RankFilters.MEDIAN
    elif op == "close_maxima": filter_type = RankFilters.CLOSE
    elif op == "open_maxima": filter_type = RankFilters.OPEN
    elif op == "remove_outliers": filter_type = RankFilters.OUTLIERS
    elif op == "remove_nan": filter_type = RankFilters.REMOVE_NAN
    elif op == "despeckle": filter_type, kernel_radius = RankFilters.MEDIAN, 1
    else: raise Exception("Invalid operation " + str(op))
    ip = ip if in_place else ip.duplicate()            
    RankFilters().rank(ip.getProcessor(), kernel_radius, filter_type, RankFilters.DARK_OUTLIERS if dark_outliers else RankFilters.BRIGHT_OUTLIERS ,threshold)
    return ip    

def op_edm(ip, op="edm", dark_background=False, in_place=True):
    """
    Euclidian distance map & derived  operations
    op ="edm", "watershed","points", "voronoi"
    """    
    ip = ip if in_place else ip.duplicate()        
    pr = ip.getProcessor()
    edm=EDM()
    Prefs.blackBackground=dark_background      
    if  op=="edm":        
        #pr.setPixels(0, edm.makeFloatEDM(pr, 0, False));
        #pr.resetMinAndMax();                
        if dark_background:
            pr.invert()
        edm.toEDM(pr)
    else:
        edm.setup(op, ip)
        edm.run(pr)
    return ip

def watershed(ip, dark_background=False, in_place=True):
    return op_edm(ip, "watershed", dark_background, in_place)

def ultimate_points(ip, dark_background=False, in_place=True):
    return op_edm(ip, "points", dark_background, in_place)

def veronoi(ip, dark_background=False, in_place=True):
    return op_edm(ip, "voronoi", dark_background, in_place)

def edm(ip, dark_background=False, in_place=True):
    return op_edm(ip, "edm", dark_background, in_place)

def op_filter(ip, op, in_place=True):    
    """
    This is redundant as just calls processor methods.
    op ="invert", "smooth", "sharpen", "edge", "add"
    """    
    ip = ip if in_place else ip.duplicate()
    f = Filters()
    f.setup(op, ip )
    f.run(ip.getProcessor())
    return ip

###################################################################################################
#Other operations
###################################################################################################
def gaussian_blur(ip, sigma_x=3.0, sigma_y=3.0, accuracy = 0.01, in_place=True):
    ip = ip if in_place else ip.duplicate()
    GaussianBlur().blurGaussian(ip.getProcessor(),  sigma_x, sigma_y, accuracy)
    return ip

def find_maxima(ip, tolerance=25, threshold = ImageProcessor.NO_THRESHOLD, output_type=MaximumFinder.IN_TOLERANCE, exclude_on_edges = False, is_edm = False):
    """
    Returns new ImagePlus
    tolerance:  maxima are accepted only if protruding more than this value  from the ridge to a higher maximum
    threshhold: minimum height of a maximum (uncalibrated);
    output_type = SINGLE_POINTS, IN_TOLERANCE or SEGMENTED.  No output image is created for output types POINT_SELECTION, LIST and COUNT.   
    """
    byte_processor =  MaximumFinder().findMaxima(ip.getProcessor(), tolerance, threshold, output_type, exclude_on_edges, is_edm)     
    return ImagePlus(ip.getTitle() + " maxima", byte_processor)


def get_maxima_points(ip, tolerance=25, exclude_on_edges = False):
    polygon =  MaximumFinder().getMaxima(ip.getProcessor(), tolerance, exclude_on_edges)
    return (polygon.xpoints, polygon.ypoints)
    
def enhance_contrast(ip, equalize_histo = True, saturated_pixels = 0.5, normalize = False, stack_histo = False, in_place=True):
    ip = ip if in_place else ip.duplicate()
    ce = ContrastEnhancer()
    if equalize_histo:
        ce.equalize(ip.getProcessor());
    else:
        ce.stretchHistogram(ip.getProcessor(), saturated_pixels)
        if normalize:
            ip.getProcessor().setMinAndMax(0,1.0 if (ip.getProcessor().getBitDepth()==32) else ip.getProcessor().maxValue())
    return ip   

def shadows(ip, op, in_place=True):
    """
    op ="north","northeast", "east", "southeast","south", "southwest", "west","northwest"
    """
    ip = ip if in_place else ip.duplicate()
    shadows= Shadows()
    shadows.setup(op, ip)
    shadows.run(ip.getProcessor())
    return ip

def unsharp_mask(ip, sigma, weight, in_place=True):
    """
    Float processor
    """    
    ip = ip if in_place else ip.duplicate()
    ip.getProcessor().snapshot()
    unsharp=UnsharpMask()
    USmask.setup("  ", ip)
    USmask.sharpenFloat( ip.getProcessor(),sigma, weight)
    return ip

def subtract_background(ip, radius = 50, create_background=False, dark_background=False, use_paraboloid =True, do_presmooth = True, correctCorners = True, rgb_brightness=False, in_place=True):
    ip = ip if in_place else ip.duplicate()
    if rgb_brightness:        
        BackgroundSubtracter().rollingBallBrightnessBackground(ip.getProcessor(), radius, create_background,not dark_background, use_paraboloid, do_presmooth, correctCorners)
    else:
        BackgroundSubtracter().rollingBallBackground(ip.getProcessor(), radius, create_background, not dark_background, use_paraboloid, do_presmooth, correctCorners)        
    return ip

###################################################################################################
#FFT
###################################################################################################
def image_fft(ip, show = True):   
    WindowManager.setTempCurrentImage(ip)
    fft = FFT()
    fft.run("fft")    
    #TODO: how to avoid it to be created?
    #ret =  ImagePlus("FHT of " + ip.getTitle(), WindowManager.getCurrentImage().getProcessor())
    ret = WindowManager.getCurrentImage()
    if not show:
        WindowManager.getCurrentImage().hide()
    return ret
    

def image_ffti(ip, show = True):   
    WindowManager.setTempCurrentImage(ip)
    fft = FFT()
    fft.run("inverse")
    #WindowManager.getCurrentImage().hide()
    #TODO: how to avoid it to be created?
    #ret = WindowManager.getCurrentImage()
    #WindowManager.getCurrentImage().hide()
    #ret =  ImagePlus(ip.getTitle() + " ffti", WindowManager.getCurrentImage().getProcessor())
    ret = WindowManager.getCurrentImage()
    if not show:
        WindowManager.getCurrentImage().hide()
    
    return ret

def bandpass_filter(ip, small_dia_px, large_dia_px, suppress_stripes = 0, stripes_tolerance_direction = 5.0, autoscale_after_filtering = False, saturate_if_autoscale = False, display_filter = False, in_place=True):
    """
    suppress_stripes = 0 for none, 1 for horizontal, 2 for vertical
    """
    ip = ip if in_place else ip.duplicate()
    filter=  FFTFilter();
    FFTFilter.filterLargeDia = large_dia_px
    FFTFilter.filterSmallDia = small_dia_px
    FFTFilter.choiceIndex = suppress_stripes
    FFTFilter.toleranceDia = stripes_tolerance_direction
    FFTFilter.doScalingDia = autoscale_after_filtering
    FFTFilter.saturateDia = saturate_if_autoscale
    FFTFilter.displayFilter =display_filter
    filter.setup(None, ip);
    filter.run(ip.getProcessor())
    return ip

###################################################################################################
#Convolution
###################################################################################################

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


def convolve(ip, kernel, in_place=True):
    """
    kernel: list of lists
    """
    ip = ip if in_place else ip.duplicate()
    kernel_width = len(kernel)
    kernel_height= len(kernel[0])
    kernel = [item for row in kernel for item in row]
    #Convolver().convolve(ip.getProcessor(), kernel, kernel_width, kernel_height) 
    ip.getProcessor().convolve(kernel, kernel_width, kernel_height)
    return ip

        
###################################################################################################
#Shortcut  to ImageProcessor methods
###################################################################################################
def invert(ip, in_place=True):
    ip = ip if in_place else ip.duplicate()
    ip.getProcessor().invert()
    return ip

def smooth(ip, in_place=True):
    ip = ip if in_place else ip.duplicate()
    ip.getProcessor().smooth()
    return ip
            
def sharpen(ip, in_place=True):
    ip = ip if in_place else ip.duplicate()
    ip.getProcessor().sharpen()
    return ip

def edges(ip, in_place=True): #Sobel
    ip = ip if in_place else ip.duplicate()
    ip.getProcessor().findEdges()
    return ip

def noise(ip, sigma = 25.0, in_place=True):
    ip = ip if in_place else ip.duplicate()
    ip.getProcessor().noise(sigma)
    return ip

def remap(ip, min=None, max=None, in_place=True):
    ip = ip if in_place else ip.duplicate()
    if min is None or max is None:
        stats = get_statistics(ip, Measurements.MIN_MAX)
        if min is None: min = stats.min
        if max is None: max = stats.max
    ip.getProcessor().setMinAndMax(min, max)
    return ip

def set_lut(ip, r, g, b):
    """
    r,g and b are lists of 256 integers
    """
    r = [x if x<128 else x-256 for x in r]
    g = [x if x<128 else x-256 for x in g]
    b = [x if x<128 else x-256 for x in b]    
    ip.setLut(LUT(jarray.array(r,'b'),jarray.array(g,'b'),jarray.array(b,'b')))

def resize(ip, width, height):
    """
    Returns new ImagePlus
    """
    p = ip.getProcessor().resize(width, height)
    return ImagePlus(ip.getTitle() + " resized", p)

def binning(ip, factor):
    p=ip.getProcessor().bin(factor)
    return ImagePlus(ip.getTitle() + " resized", p)

def get_histogram(ip, hist_min = 0, hist_max = 0, hist_bins = 256, roi=None):
    """
    hist_min, hist_max, hist_bins used only for float images (otherwise fixed to 0,255,256)
    roi is list  [x,y,w,h]
    """
    if roi == None: ip.deleteRoi()    
    else: ip.setRoi(roi[0],roi[1],roi[2],roi[3])
    image_statistics = ip.getStatistics(0, hist_bins, hist_min, hist_max)    
    return image_statistics.getHistogram()


def get_array(ip):
    return ip.getProcessor().getIntArray()

def get_line(ip, x1, y1, x2, y2):
    return ip.getProcessor().getLine(x1, y1, x2, y2)

def get_pixel_range(ip):
    return (ip.getProcessor().getMin(), ip.getProcessor().getMax())

def get_num_channels(ip):
    return ip.getProcessor().getNChannels()

def is_binary(ip):
    return ip.getProcessor().isBinary()    

def get_pixel(ip, x, y):
    return ip.getProcessor().getPixel(x,y)

def get_pixel_array(ip, x, y):
    a = [0]*get_num_channels(ip)
    return ip.getProcessor().getPixel(x,y,a)

def get_pixels(ip):
    return ip.getProcessor().getPixels()

def get_width(ip):
    return ip.getProcessor().getWidth()

def get_height(ip):
    return ip.getProcessor().getHeight()

def get_row(ip, y):
    a = [0]*get_width(ip)
    array = jarray.array(a,'i')
    ip.getProcessor().getRow(0, y, array, get_width(ip))
    return array

def get_col(ip, x):
    a = [0]*get_height(ip)
    array = jarray.array(a,'i')
    ip.getProcessor().getColumn(x, 0, array, get_height(ip))
    return array     

def get_statistics(ip, measurements = None):
    """
    Measurements is a mask of flags: https://imagej.nih.gov/ij/developer/api/ij/measure/Measurements.html.
    Statistics object hold public fields: https://imagej.nih.gov/ij/developer/api/ij/process/ImageStatistics.html
    """
    if measurements is None:
        return ip.getStatistics()
    else:
        return ip.getStatistics(measurements)

###################################################################################################
#Image stack functions
###################################################################################################
def create_stack(ip_list, duplicate=True, title = None):
    stack = Concatenator().concatenate(ip_list, duplicate)
    if title is not None:
        stack.setTitle(title)
    return stack

def open_stack(path_list, title=None):
    """
    Open list of files as a stack using ij.io,Opener
    """
    ip_list = []
    for path in path_list:
        ip_list.append(open_image(path))
    return  create_stack(ip_list, duplicate=False, title = "stack" if title is None else title)

def reslice(stack, start_at = "Top", vertically = True, flip = True, output_pixel_spacing=1.0, avoid_interpolation = True, title = None):
    ss = Slicer()
    ss.rotate = vertically
    ss.startAt = start_at 
    ss.flip = flip
    ss.nointerpolate =  avoid_interpolation
    ss.outputZSpacing = output_pixel_spacing
    stack =  ss.reslice(stack)
    if title is not None:
        stack.setTitle(title)
    return stack
    
    
 
###############################################################################
# ImagePlus list operations
###############################################################################

def integrate_ips(ips, as_float=True):    
    """
    Integrate list if ImagePlus with the same size.
    """ 
    aux = None
    for i in range(len(ips)):
        if i==0:        
            img_type = "float" if as_float else "short"
            aux = new_image(ips[i].width, ips[i].height, image_type=img_type, title = "sum", fill_color = None)
        op_image(aux, ips[i], "add", float_result=as_float, in_place=True)    
    return aux

def average_ips (ips, roi=None, as_float=True):   
    """
    Average list if ImagePlus with the same size.
    """ 
    aux = integrate_ips(ips, as_float)     
    op_const(aux, "divide", float(len(ips)), in_place=True)    
    return aux


###############################################################################
# Composite images
###############################################################################

def create_composite_image(ref, slots_x, slots_y, name="Composite"):
    ref = to_ip(ref)
    proc=ref.getProcessor()
    combined_image = ImagePlus(name, proc.createProcessor(proc.width*slots_x, proc.height*slots_y))
    return combined_image


def append_composite_image(composite, img, slot_x, slot_y):
    img = to_ip(img)
    proc=img.getProcessor()
    cproc = composite.getProcessor()
    if cproc.width < (slot_x+1) * proc.width or cproc.height < (slot_y+1) * proc.height:
        raise Exception("Invalid composite image slot: " + str ((slot_x, slot_y)))
    cproc.insert(proc, slot_x * proc.width, slot_y * proc.height)


def display_composite_image(composite_image, title=None):
    import ch.psi.pshell.plot.MatrixPlot as MatrixPlot
    from builtin_functions import get_plots, plot
    composite_image =  to_ip(composite_image)
    #title = composite_image.getTitle()
    data = Convert.toDouble(Convert.transpose(get_ip_array(composite_image)))
    plots = get_plots(title)
    if len(plots)==1:
       p = plots[0]
       if isinstance(p,MatrixPlot):
            s=p.getSeries(0)
            x,y = s.getNumberOfBinsX(), s.getNumberOfBinsY()
            if x == composite_image.getWidth() and y== composite_image.getHeight():
                s.setData(data)
                return 
    p=plot(data, title=title)[0]
