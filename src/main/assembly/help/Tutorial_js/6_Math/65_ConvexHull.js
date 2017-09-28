///////////////////////////////////////////////////////////////////////////////////////////////////
// Example of convex hull plot
//
//////////////////////////////////////////////////////////////////////////////////////////////////


function add_convex_hull_plot(title, x,y, name, clear){
	if (!is_defined(name))    name = null
	if (!is_defined(clear))    clear = false
    plots = get_plots(title = title)
    p = null
    if (plots.length==0){
        p = plot(null,name=name, undefined, undefined, title = title)[0]        
        p.getAxis(AxisId.X).setRange(-50,150)
        p.getAxis(AxisId.Y).setRange(-50,150)
        p.setLegendVisible(true)      
    } else {
        p = plots[0]    
        if (clear){
            p.clear()
        }
        p.addSeries(new LinePlotSeries(name))            
    }
    s = p.getSeries(name)        
    s.setLinesVisible(false)
    s.setPointSize(3)
    s.setData(x, y)         

    //In the first time the plot shows, it takes some time for the color to be assigned
    while (s.color == null){
        sleep(0.001)
    }        

    hull = new LinePlotSeries(name + " Hull", s.color)
        
    //Bounding box
    //x1 = Math.min.apply(null, x) 
    //x2 = Math.max.apply(null, x) 
    //y1 = Math.min.apply(null, y) 
    //y2 = Math.max.apply(null, y) 
    //(hx,hy) = ([x1,x2, x2, x1, x1], [y1, y1, y2, y2, y1])     
    
    //Convex Hull
    ret = convex_hull(point_list=undefined, x=x, y=y)
    hx=ret[0]
    hy=ret[1]
    hx.push(hx[0])
    hy.push(hy[0])
                
    p.addSeries(hull)                   
    hull.setLineWidth(2)
    hull.setData(hx,hy)     
    hull.setColor(s.color)    
}    
        
title = "Convex Hull"
if (get_plots(title).length>0){
    get_plots(title)[0].clear()
}    

for (var step=1; step<=5; step++){
    var fx=[]
    var fy=[]
    for (var i=0; i<=50 ; i++){
        fx.push(Math.random() * 100 / step)
        fy.push(Math.random() * 100/ step)        
    }
    add_convex_hull_plot (title, fx,fy,"Scan " + step)
}    

