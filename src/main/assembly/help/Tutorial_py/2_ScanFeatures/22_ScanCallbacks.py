################################################################################################### 
# Demonstrate use of scan callbacks to trigger a detector at falling edge.
################################################################################################### 


def before_read(position, scan):
    ao1.write(1)
    ao1.write(0)
    print "In position: " + str(position[0]) + ", " + str(position[1])
    
    #Example with an epics direct channel access
    #caput("CHANNEL_NAME", 1)
    #caput("CHANNEL_NAME", 0)    


def after_read(record, scan):
    print "Aquired frame: " + str(record.index) + " at " + \
        str(record[m1])  + ", " + str(record[m2]) + ": " + \
        str(record[ai1]) + ", " + str(record[ai2])

a= lscan((m1,m2), (ai1, ai2), (0,0), (4,8), steps=20, latency = 0.01, before_read=before_read, after_read=after_read)