################################################################################################### 
# Demonstrate the use of Continuous Scan Scan: a Linear Scan with continuous motor move and 
# sampling on the fly.
################################################################################################### 


m1.move(0.0)

#A single motor at current speed
r1 = cscan(m1, (ai1,ai2), -2, 3 , steps=10, relative=True) 

#A single motor in a given time 
r2 = cscan(m1, (ai1,ai2), -2.0, 3.0, steps=100 ,time = 4.0, relative=True) 

#Multiple motors in a given time 
r3 = cscan((m1, m2), (ai1,ai2), (-2.0, -3), (3.0, 5.0), steps=100,time = 4.0, relative=True) 

