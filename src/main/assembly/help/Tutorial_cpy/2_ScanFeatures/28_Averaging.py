################################################################################################### 
# Scan averaging sensor value, storing all values, mean and variance
###################################################################################################


#First option: using get_averager builtin and Averager class.
av = create_averager(ai2, 5, 0.05)
#set_preference(Preference.PLOT_TYPES, {av.name:'minmax'})  #This is to display min/max instead of sigma.
res= lscan(ao1, (av, av.samples), 0, 40, 20, 0.1)

#If the averager is set to monitored, than if samples in the background, not blocking when read in scan.
av.setMonitored(True)
sleep(0.5) #Give some time for the averager to fill its buffer, before first use
res= lscan(ao1, (av, av.samples), 0, 40, 20, 0.1)

#Second option: creating pseudo-devices
class Mean(Readable):
    def __init__(self, dev):
        self.dev =dev    

    def read(self):  
        return mean(self.dev.take())

class Variance(Readable):
    def __init__(self, dev):
        self.dev =dev    

    def read(self):  
        return variance(self.dev.take())

class Measures(ReadonlyRegisterBase, ReadonlyRegisterArray):
    def __init__(self, dev, count, interval=0):        
        self.dev = dev
        self.count = count
        self.interval = interval        
        self.initialize()
        
    def getSize(self):
        return self.count
                
    def doRead(self):      
        measures = []
        for i in range(self.count):
            measures.append(self.dev.read())
            if self.interval>0 and i<(self.count-1):
                time.sleep(self.interval)
        return measures  

def average(dev, count, interval=0):
    av = Measures(dev, count, interval)
    return [av, Mean(av), Variance(av)]                  

res= lscan(ao1, average(ai2, 5, 0.05), 0, 40, 20, 0.1)