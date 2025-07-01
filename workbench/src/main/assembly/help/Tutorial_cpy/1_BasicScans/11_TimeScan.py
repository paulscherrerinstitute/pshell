################################################################################################### 
# Demonstrate the use of Time Scan: time-based sensor sampling
###################################################################################################


#Execute the scan: 100 samples, 10ms sampling interval
r1 = tscan((ai1,ai2,wf1), 100, 0.01)
