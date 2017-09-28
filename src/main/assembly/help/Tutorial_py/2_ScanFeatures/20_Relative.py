################################################################################################### 
# Demonstrate use of Relative Line Scan.
# The arguments start and end are relative to the current position.
# After the scan the positioner(s) move back to the initial position.
################################################################################################### 


print "Initial position = " + str(m1.position)

r1 = lscan(m1, (ai1,ai2,wf1), start = -2, end =2, steps = 20, relative = True)


print "Final position = " +  str(m1.position)