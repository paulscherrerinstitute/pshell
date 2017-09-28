################################################################################################### 
# Example on running simultaneous scans. They should not manipulate same writables
################################################################################################### 


def scan1():
    print "scan1"
    return lscan(ao1, ai1, 0, 40, 20, 0.1, title = "scan1")

def scan2():
    print "scan2"    
    return lscan(ao2, wf1, 0, 40, 20, 0.1, title = "scan2")


parallelize(scan1, scan2)
