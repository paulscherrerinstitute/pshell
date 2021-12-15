################################################################################################### 
# Spiral scan using a generator
################################################################################################### 

def spiral_gen(radius, step, resolution=.1, angle=0.0, origin=[0.0, 0.0]):
    d = 0.0
    while d * math.hypot(math.cos(angle), math.sin(angle)) < radius:
        pos=[ origin[0] + d * math.cos(angle), \
              origin[1] + d * math.sin(angle) ]
        yield(pos)
        d+=step
        angle+=resolution

    
r2 = vscan((m1,m2),(ai1,ai2),spiral_gen(10, 0.1), latency=0.1, range = [-10.0,10.0, -10.0, 10.0])       
