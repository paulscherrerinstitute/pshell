################################################################################################### 
# Demonstrate the use of Line Scan: one or multiple positioners move together linearly.
###################################################################################################


#Execute the scan: 100 steps, a1 from 0 to 40
r1 = lscan(ao1, (ai1,ai2,wf1), 0, 40, 100, 0.01)                    

#Steps of size 1.0, a1 from 0 to 40
r2 = lscan(ao1, (ai1,ai2,wf1), 0, 40, 1.0, 0.01)                   

#2 positioners moving together in 10 steps. Also sampling an image:
r3 = lscan((ao1,ao2), (ai1,ai2,wf1,im1),  (0, 0), (40, 100), 4, 0.01)                    

