///////////////////////////////////////////////////////////////////////////////////////////////////
// Example of using ImageJ functionalities through ijutils.
/////////////////////////////////////////////////////////////////////////////////////////////////// 

run("ijutils")

//Image Loading
ip = load_image("images/img.png", title="Image")

//Basic image manipulation: creation, copying, padding, saving 
resized = resize(ip, 300,300)
save_image(resized, get_context().setup.expandPath("{images}/resized.tiff") ,"tiff")
crop=sub_image(ip,10,20,50,30)    
bin_im = binning(ip,2)
new_im = new_image(256, 256, "color")
copy_image_to(bin_im, new_im, 20, 20)
pad_im = pad_image(ip, 1, 2, 3, 4, Color.RED)
stack=create_stack([ip,resized,crop, bin_im, new_im, pad_im], undefined, title = "Basic Functions")
save_image(stack, get_context().setup.expandPath("{images}/stack.tiff") ,"tiff")
stack.show()


//Decomposing color channels

create_stack([  get_channel(ip, "red"), 
                get_channel(ip, "green"), 
                get_channel(ip, "blue"), 
                get_channel(ip, "alpha"), 
                grayscale(get_channel(ip, "brightness"), false)], undefined, title = "Color Decomposition").show()


//Basic functions (in_place)
aux = ip.duplicate()
aux.show()
grayscale(aux)
gaussian_blur(aux); aux.repaintWindow()
invert(aux); aux.repaintWindow()
smooth(aux); aux.repaintWindow()
sharpen(aux); aux.repaintWindow()
noise(aux, 100); aux.repaintWindow()


//Changing LUT
aux = ip.duplicate()
aux = grayscale(aux, in_place=false)
r=[]; g=[]; b=[]
for (var i=0; i<256; i++){
    r.push(0)
    g.push(0)
    b.push(i)
}    
set_lut(aux, r, g, b)
aux.show()


//Histogram
plot(get_histogram(ip))


aux = grayscale(ip, in_place = false)
bin = ip.duplicate()
ip_bin = auto_threshold(aux, undefined, undefined, in_place=false)
create_stack([ ip_bin, 
               binary_fill_holes(ip_bin, dark_background = false, in_place=false),
               binary_outline(ip_bin, dark_background = false, in_place=false),
               binary_outline(binary_fill_holes(ip_bin, dark_background = false, in_place=false), dark_background = false),
               binary_dilate(ip_bin, undefined, undefined, undefined, in_place=false),
               binary_erode(ip_bin, undefined, undefined, undefined, in_place=false),
               binary_open(ip_bin, undefined, undefined, undefined, in_place=false),
               binary_close(ip_bin, undefined, undefined, undefined, in_place=false),
               binary_skeletonize(ip_bin, in_place=false)], undefined, title = "Binarization").show()              


//EDM, const & image operations
aux = grayscale(ip, in_place = false)
ip_bin = auto_threshold(aux, undefined, undefined, in_place=false)
binary_fill_holes(ip_bin)

edm = edm(ip_bin, dark_background=false, in_place=false)
ws = watershed(ip_bin, dark_background=false, in_place=false)
up = ultimate_points(ip_bin, dark_background=false, in_place=false)
vr = veronoi(ip_bin, dark_background=false, in_place=false)
edm_disp = remap(edm, min=null, max=null, in_place=false)
ws_disp = grayscale(ws, in_place=false)
up_disp = enhance_contrast(up, undefined, undefined, undefined, undefined, in_place=false)
vr_disp = enhance_contrast(vr, undefined, undefined, undefined, undefined, in_place=false)
create_stack([edm_disp, aux, ip_bin, ws_disp, up_disp, vr_disp], undefined, title = "EDM Operations").show()
final_img = grayscale(ip_bin, in_place = false)
op_const(final_img,"add", -200)
op_image(final_img, vr_disp, 'or')
op_image(final_img, up_disp, 'or')
final_img.show()

aux = grayscale(ip, in_place = false)

create_stack([ aux,                
               subtract_background(aux, undefined, undefined, undefined, undefined, undefined, undefined, undefined, in_place=false),
               smooth(aux, false),
               sharpen(aux, false),
               edges(aux, false),
               bandpass_filter(aux,0, 5, undefined, undefined, undefined, undefined, undefined, in_place=false),
               bandpass_filter(aux,5, 100, undefined, undefined, undefined, undefined, undefined, in_place=false),
               op_const(aux,"and", 127, false), 
               convolve(aux, KERNEL_BLUR, false),
               convolve(aux, KERNEL_SHARPEN, false),
               convolve(aux, KERNEL_SHARPEN_2, false),

               
               convolve(aux, KERNEL_LIGHT, false),
               convolve(aux, KERNEL_DARK, false),
               convolve(aux, KERNEL_EDGE_DETECT, false),
               convolve(aux, KERNEL_EDGE_DETECT_2, false),
               convolve(aux, KERNEL_DIFFERENTIAL_EDGE_DETECT, false),
               convolve(aux, KERNEL_PREWITT, false),
               convolve(aux, KERNEL_SOBEL, false)
               ], undefined, title = "General Operations").show()


//Rank operators
rank_opers = []
oplist = ["mean", "min", "max", "variance", "median", "close_maxima", "open_maxima", "remove_outliers", "remove_nan", "despeckle"]
for (var op in oplist){
    rank_opers.push(op_rank(aux,oplist[op],kernel_radius=1, undefined, undefined, in_place=false))
}    
create_stack(rank_opers, undefined, title = "Rank Operations").show()


//Reslicing
//orig = load_image("{data}/img/img2.png")
orig = resize(ip, 300,200)
grayscale(orig)
images=[]
for (var i=0; i<20; i++){
    images.push(orig.duplicate())    
    op_const(orig, "multiply", 0.9)
}    
stack=create_stack(images, undefined, title = "Original Stack")
//stack.show()    
r1 = reslice(stack, start_at="Left", undefined, undefined, undefined, undefined, title="Reslice Horizontally")
r2 = reslice(stack, start_at="Top", undefined, undefined, undefined, undefined, title="Reslice Vertically")
r1.show()
r2.show()


//Particle Analysis
aux = grayscale(ip, in_place = false)
auto_threshold(aux)
//binary_fill_holes(aux)
//aux.show()
var ret=analyse_particles(aux, 100,1000, true, true, 0, print_table=true)
var results=ret[0]
var output_img=ret[1]
output_img.show()

