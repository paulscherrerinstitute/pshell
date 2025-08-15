################################################################################################### 
# Multi-pass and zigzag scans
################################################################################################### 


#Linear with 2 passes
a= lscan(m1, (ai1,wf1), -0.2, 0.2, 20, relative = True, passes = 4) 

#Linear with 4 passes and zigzag
a= lscan(m1, (ai1,wf1), -0.2, 0.2, 20, relative = True, passes = 4, zigzag = True) 

#Multi-dimentional zigzag
x = ascan ([m1,m2], ai1, [0,0], [1, 1], [0.25,0.25], latency=0.01, zigzag=True)
