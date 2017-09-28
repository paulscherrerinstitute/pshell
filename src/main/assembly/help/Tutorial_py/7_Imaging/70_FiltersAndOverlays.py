################################################################################################### 
# Demonstrate creation of an image filters and new source based on filter.
# Also demonstrate creation of image histogram and use of overlays.
################################################################################################### 


import ch.psi.pshell.imaging.Filter as Filter
from ch.psi.pshell.imaging.Utils import *
from ch.psi.pshell.imaging.Overlays import *
import ch.psi.pshell.imaging.Pen as Pen


class MyFilter(Filter):
    def process(self, image, data):
        image = grayscale(image)
        image = blur(image)
        image = sobel(image)
        return image

#Setting the filter to a source
src2.setFilter(MyFilter())

#Creating a new source with the filter
src1.setFilter(None)
add_device(MyFilter("f1"), True)
#f1.passive = True
src1.addListener(f1)


#Open renderers
renderer_filter = show_panel(f1)
renderer = show_panel(src1)


#Overlays
plots = None
size = renderer.getImageSize() ; 
ov_text = Text(Pen(java.awt.Color.GREEN.darker()), "Ploting Histogram", 
    java.awt.Font("Verdana", java.awt.Font.PLAIN, 12), java.awt.Point(20,20))
ov_line = Line(Pen(java.awt.Color.DARK_GRAY), java.awt.Point(0,size.height/2), java.awt.Point( size.width ,size.height/2))
ov_rect = Rect(Pen(java.awt.Color.DARK_GRAY), java.awt.Point(size.width/2 -15,size.height/2+10), java.awt.Dimension(30,30))
ov_cross = Crosshairs(Pen(java.awt.Color.DARK_GRAY),  java.awt.Point(size.width/2 ,size.height/3), java.awt.Dimension(15,15))
ov_rect.setSolid(True)
ov_rect.setMovable(True)

#Histogram
try:
    renderer.addOverlays([ov_text,ov_line, ov_rect, ov_cross])
    while(True):       
        (hd,xd) = histogram(im1.read(), bin=0.1)     
        
        image = ch.psi.pshell.imaging.Utils.grayscale(src1.getOutput())   
        data = Convert.toUnsigned(image.getData().getDataBuffer().getData())
        (hi,xi) = histogram(data, range_min=0, range_max=255)     
    
        if plots is None:
            plots = plot((hd,hi), ("Data", "Image"), (xd, xi), title = "Histo")
        else:
            plots[0].getSeries(0).setData(xd,hd)
            plots[1].getSeries(0).setData(xi,hi)    
    
        if plots[0].displayable == False: 
            break
        ov_cross.update(java.awt.Point((ov_cross.position.x+1) % size.width ,(ov_cross.position.y+1) % size.height))
        time.sleep(0.1)
finally:      
    renderer.removeOverlays([ov_text,ov_line, ov_rect, ov_cross])