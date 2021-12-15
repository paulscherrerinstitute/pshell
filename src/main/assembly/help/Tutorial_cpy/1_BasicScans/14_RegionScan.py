################################################################################################### 
# Demonstrate the use of Region Scan: one positioner move linearly in multiple regions.
###################################################################################################


#Execute the scan: 3 regions with different number of steps
r1 = rscan(ao1, (ai1,ai2), [(0,5,5), (10,15,20), (20,25,5)] , 0.01)                    

#Execute the scan: 3 regions with different step size
r2 = rscan(ao1, (ai1,ai2), [(0,5,1.0), (10,15,0.2), (20,25,1.0)] , 0.01)             