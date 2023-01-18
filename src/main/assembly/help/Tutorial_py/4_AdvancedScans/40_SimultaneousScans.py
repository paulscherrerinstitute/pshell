################################################################################################### 
# Example on running simultaneous scans. 
################################################################################################### 

#Simple example running 2 simultaneous scans, not manipulating same writables.
#Each scan plots to a different context.
def scan1():
    print "scan1"
    return lscan(ao1, ai1, 0, 40, 20, 0.1, title = "scan1", tag = "scan_1")

def scan2():
    print "scan2"    
    return lscan(ao2, wf1, 0, 40, 20, 0.1, title = "scan2", tag = "scan_2")

parallelize(scan1, scan2)



#Example of simultaneous scans, running one at a time in passes, manipulating same writable.
#All scans plot to default context.

REGIONS = [("R1",(1.0, 2.0, 0.1)), ("R2",(2.0, 8.0, 0.5)), ("R3",(8.0, 9.0, 0.2)), ]
PASSES = 3

scanning=False
current_index=-1
def scan(region):
    name, pars=REGIONS[region]
    def before_pass(pass_num, scan):        
        global scanning, current_index
        while (current_index != (region-1)) or scanning:
            time.sleep(0.1)
        scanning = True
        current_index = current_index+1
        App.getInstance().mainFrame.setScanDisplays(scan, None)        
    def after_pass(pass_num):
        global scanning, current_index
        if current_index>=(len(REGIONS)-1):
            current_index=-1
        scanning = False
    lscan(ao1, [ai1, ai2], pars[0], pars[1], pars[2], 0.1, tag=name, passes=PASSES,
                  before_pass = before_pass, after_pass=after_pass, initial_move=False)

parallelize(* [[scan,[i,]] for i in range(len(REGIONS))])



