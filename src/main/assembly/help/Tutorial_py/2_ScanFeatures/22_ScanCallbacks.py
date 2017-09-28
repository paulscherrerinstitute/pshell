################################################################################################### 
# Demonstrate use of scan callbacks to trigger a detector at falling edge.
################################################################################################### 


def BeforeReadout(position, scan):
    ao1.write(1)
    ao1.write(0)
    print "In position: " + str(position[0]) + ", " + str(position[1])
    
    #Example with an epics direct channel access
    #caput("CHANNEL_NAME", 1)
    #caput("CHANNEL_NAME", 0)    


def AfterReadout(record, scan):
    print "Aquired frame: " + str(record.index)

a= lscan((m1,m2), (ai1, ai2), (0,0), (4,8), steps=20, latency = 0.01, before_read=BeforeReadout, after_read=AfterReadout)