################################################################################################### 
# Example of using ImageJ functionalities through ijutils.
################################################################################################### 

from ijutils import *
import java.awt.Color as Color

#Image Loading
ip = load_image("images/img.png", title="Image")

#Basic image manipulation: creation, copying, padding, saving 
resized = resize(ip, 300,300)
save_image(resized, expand_path("{images}/resized.tiff") ,"tiff")
crop=sub_image(ip,10,20,50,30)    
bin_im = binning(ip,2)
new_im = new_image(256, 256, "color")
copy_image_to(bin_im, new_im, 20, 20)
pad_im = pad_image(ip, 1, 2, 3, 4, Color.RED)
stack=create_stack([ip,resized,crop, bin_im, new_im, pad_im], title = "Basic Functions")
save_image(stack,expand_path("{images}/stack.tiff") ,"tiff")
stack.show()


#Decomposing color channels

create_stack([  get_channel(ip, "red"), 
                get_channel(ip, "green"), 
                get_channel(ip, "blue"), 
                get_channel(ip, "alpha"), 
                grayscale(get_channel(ip, "brightness"), False)], title = "Color Decomposition").show()


#Basic functions (in_place)
aux = ip.duplicate()
aux.show()
grayscale(aux)
gaussian_blur(aux); aux.repaintWindow()
invert(aux); aux.repaintWindow()
smooth(aux); aux.repaintWindow()
sharpen(aux); aux.repaintWindow()
noise(aux, 100); aux.repaintWindow()


#Changing LUT
aux = ip.duplicate()
aux = grayscale(aux, in_place=False)
r,g,b = [],[],[]
for i in range(256):
    r.append(0)
    g.append(0)
    b.append(i)
set_lut(aux, r, g, b)
aux.show()


#Histogram
plot(get_histogram(ip))


aux = grayscale(ip, in_place = False)
bin = ip.duplicate()
ip_bin = auto_threshold(aux, in_place=False)
create_stack([ ip_bin, 
               binary_fill_holes(ip_bin, in_place=False),
               binary_outline(ip_bin, in_place=False),
               binary_outline(binary_fill_holes(ip_bin, in_place=False)),
               binary_dilate(ip_bin, in_place=False),
               binary_erode(ip_bin, in_place=False),
               binary_open(ip_bin, in_place=False),
               binary_close(ip_bin, in_place=False),
               binary_skeletonize(ip_bin, in_place=False)], title = "Binarization").show()            


#EDM, const & image operations
aux = grayscale(ip, in_place = False)
ip_bin = auto_threshold(aux, in_place=False)
binary_fill_holes(ip_bin)

edm = edm(ip_bin, in_place=False)
ws = watershed(ip_bin, in_place=False)
up = ultimate_points(ip_bin, in_place=False)
vr = veronoi(ip_bin, in_place=False)
edm_disp = remap(edm, in_place=False)
ws_disp = grayscale(ws, False)
up_disp = enhance_contrast(up, in_place=False)
vr_disp = enhance_contrast(vr, in_place=False)
create_stack([edm_disp, aux, ip_bin, ws_disp, up_disp, vr_disp], title = "EDM Operations").show()
final = grayscale(ip_bin, in_place = False)
op_const(final,"add", -200)
op_image(final, vr_disp, 'or')
op_image(final, up_disp, 'or')
final.show()

aux = grayscale(ip, in_place = False)

create_stack([ aux,                
               subtract_background(aux, in_place=False),
               smooth(aux, False),
               sharpen(aux, False),
               edges(aux, False),
               bandpass_filter(aux,0, 5, in_place=False),
               bandpass_filter(aux,5, 100, in_place=False),
               op_const(aux,"and", 127, False), 
               convolve(aux, KERNEL_BLUR, False),
               convolve(aux, KERNEL_SHARPEN, False),
               convolve(aux, KERNEL_SHARPEN_2, False),
               convolve(aux, KERNEL_LIGHT, False),
               convolve(aux, KERNEL_DARK, False),
               convolve(aux, KERNEL_EDGE_DETECT, False),
               convolve(aux, KERNEL_EDGE_DETECT_2, False),
               convolve(aux, KERNEL_DIFFERENTIAL_EDGE_DETECT, False),
               convolve(aux, KERNEL_PREWITT, False),
               convolve(aux, KERNEL_SOBEL, False)
               ], title = "General Operations").show()


#Rank operators
rank_opers = []
for op in "mean", "min", "max", "variance", "median", "close_maxima", "open_maxima", "remove_outliers", "remove_nan", "despeckle":
    rank_opers.append(op_rank(aux,op, in_place=False, kernel_radius=1))
create_stack(rank_opers, title = "Rank Operations").show()


#Reslicing
#orig = load_image("{data}/img/img2.png")
orig = resize(ip, 300,200)
grayscale(orig)
images=[]
for i in range (20):        
    images.append(orig.duplicate())    
    op_const(orig, "multiply", 0.9)
stack=create_stack(images, title = "Original Stack")
#stack.show()    
r1 = reslice(stack, start_at="Left", title="Reslice Horizontally")
r2 = reslice(stack, start_at="Top", title="Reslice Vertically")
r1.show()
r2.show()


#Particle Analysis
aux = grayscale(ip, in_place = False)
auto_threshold(aux)
#binary_fill_holes(aux)
#aux.show()
(results,output_img)=analyse_particles(aux, 100,1000, print_table=True)
output_img.show()

