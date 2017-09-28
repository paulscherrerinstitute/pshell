///////////////////////////////////////////////////////////////////////////////////////////////////
// Demonstrate creation of an image filters and new source based on filter.
// Also demonstrate creation of image histogram and use of overlays.
///////////////////////////////////////////////////////////////////////////////////////////////////


importClass(java.awt.Point)
importClass(java.awt.Dimension)
importClass(java.awt.Font)

Filter = Java.type('ch.psi.pshell.imaging.Filter')

Pen = Java.type('ch.psi.pshell.imaging.Pen')
Line = Java.type('ch.psi.pshell.imaging.Overlays.Line')
Rect = Java.type('ch.psi.pshell.imaging.Overlays.Rect')
Crosshairs = Java.type('ch.psi.pshell.imaging.Overlays.Crosshairs')
Text = Java.type('ch.psi.pshell.imaging.Overlays.Text')
Utils = Java.type('ch.psi.pshell.imaging.Utils')


var MyFilter = Java.extend(Filter)
var filter = new MyFilter() {	    
        process: function (image, data) {
        image = Utils.grayscale(image)
        image = Utils.blur(image)
        image = Utils.sobel(image)
        return image
        },
    }

//Setting the filter to a source
src2.setFilter(filter)

//Creating a new source with the filter
src1.setFilter(null)


add_device(new MyFilter("f1") {	    
        process: function (image, data) {
        image = Utils.grayscale(image)
        image = Utils.blur(image)
        image = Utils.sobel(image)
        return image
        },
    }, true)
//f1.passive = true
src1.addListener(f1)


//Open renderers
var renderer_filter = show_panel(f1)
var renderer = show_panel(src1)


//Overlays
var plots = null
var size = renderer.getImageSize()
var ov_text = new Text(new Pen(Color.GREEN.darker()), "Ploting Histogram", new Font("Verdana", Font.PLAIN, 12), new Point(20,20))
var ov_line = new Line(new Pen(Color.DARK_GRAY), new Point(0,size.height/2), new Point( size.width ,size.height/2))
var ov_rect = new Rect(new Pen(Color.DARK_GRAY), new Point(size.width/2 -15,size.height/2+10), new Dimension(30,30))
var ov_cross = new Crosshairs(new Pen(Color.DARK_GRAY),  new Point(size.width/2 ,size.height/3), new Dimension(15,15))
ov_rect.setSolid(true)
ov_rect.setMovable(true)

//Histogram
try{
    renderer.addOverlays([ov_text,ov_line, ov_rect, ov_cross])
    
    while(true){    
    	
        var h = histogram(im1.read(), undefined, undefined, bin=0.1)     
        var hd=h[0]; var xd = h[1]
        
        var image = Utils.grayscale(src1.getOutput())   
        var data = Convert.toUnsigned(image.getData().getDataBuffer().getData())
        h = histogram(data, range_min=0, range_max=255)     
        var hi=h[0]; var xi = h[1]
    
        if (plots == null){
            plots = plot([hd,hi], ["Data", "Image"],[xd, xi], title = "Histo")
        } else{
            plots[0].getSeries(0).setData(xd,hd)
            plots[1].getSeries(0).setData(xi,hi)    
        }
        if (plots[0].displayable == false){
            break
        }
        
        ov_cross.update(new Point((ov_cross.position.x+1) % size.width ,(ov_cross.position.y+1) % size.height))
        
        sleep(0.1)
    }
} finally {
    renderer.removeOverlays([ov_text,ov_line, ov_rect, ov_cross])
}