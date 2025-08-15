################################################################################################### 
# Using pseudo-device to : 
#    - Add calculations to scan data.
#    - Execute logic during scan
################################################################################################### 
      
       
class Clock(Readable):
    def read(self):
        return time.clock()

class PseudoSensor(Readable):
    def read(self):
        arr = wf1.take()  #Gets the CACHED waveform
        return reduce(lambda x, y: x + y, arr) / len(arr) 

class PseudoPositioner(Writable):
    def write(self,pos):
        print "Step = " + str(pos)

clock=Clock()
averager=PseudoSensor()
positioner=PseudoPositioner()

a= lscan((ao1,positioner),(ai2,wf1,averager,clock),(0,0),(40,20),20,0.1)


