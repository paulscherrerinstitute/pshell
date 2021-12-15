################################################################################################### 
# Example of convex hull plot
################################################################################################### 

import random

def add_convex_hull_plot(title, x,y, name=None, clear = False):
    plots = get_plots(title = title)
    p = None
    if len(plots)==0:
        p = plot(None,name=name, title = title)[0]        
        p.getAxis(AxisId.X).setRange(-50,150)
        p.getAxis(AxisId.Y).setRange(-50,150)
        p.setLegendVisible(True)        
    else:
        p = plots[0]    
        if clear:
            p.clear()
        p.addSeries(LinePlotSeries(name))            
    s = p.getSeries(name)        
    s.setLinesVisible(False)
    s.setPointSize(3)
    s.setData(x, y)         

    #In the first time the plot shows, it takes some time for the color to be assigned
    while s.color is None:
        time.sleep(0.001)

    hull = LinePlotSeries(name + " Hull", s.color)
        
    #Bounding box
    #x1,x2,y1,y2 = min(x), max(x), min(y), max(y)    
    #(hx,hy) = ([x1,x2, x2, x1, x1], [y1, y1, y2, y2, y1])     
    
    #Convex Hull
    (hx,hy) = convex_hull(x=x, y=y)
    hx.append(hx[0])
    hy.append(hy[0])
                
    p.addSeries(hull)                   
    hull.setLineWidth(2)
    hull.setData(hx,hy)     
    hull.setColor(s.color)    
        
title = "Convex Hull"
if len(get_plots(title))>0:
    get_plots(title)[0].clear()

for step in range(1,5):
    x,y=[],[]
    for i in range(50):
        x.append(random.random() * 100 / step)
        y.append(random.random() * 100/ step)        
    add_convex_hull_plot (title, x,y,"Scan " + str(step))

