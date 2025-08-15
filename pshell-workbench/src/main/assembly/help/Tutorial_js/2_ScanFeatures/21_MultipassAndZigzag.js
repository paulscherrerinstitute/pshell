///////////////////////////////////////////////////////////////////////////////////////////////////
// Multi-pass and zigzag scans
/////////////////////////////////////////////////////////////////////////////////////////////////// 


//Linear with 2 passes
r= lscan(m1, [ai1,wf1], -0.2, 0.2, 20, latency = 0.0, relative = true, passes = 4) 

//Linear with 4 passes and zigzag
r= lscan(m1, [ai1,wf1], -0.2, 0.2, 20, latency = 0.0, relative = true, passes = 4, zigzag = true) 

//Multi-dimentional zigzag
r = ascan ([m1,m2], ai1, [0,0], [1, 1], [0.25,0.25], latency=0.01, relative = undefined, passes = undefined, zigzag=true)
