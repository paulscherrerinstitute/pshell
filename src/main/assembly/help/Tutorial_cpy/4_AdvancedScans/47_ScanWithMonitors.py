################################################################################################### 
# Example of saving monitors during a scan 
################################################################################################### 

#Make simulated devices generate events on change (every 100ms)
ai1.polling=100
wf1.polling=100

ret = tscan(ai2, 10, 0.2, monitors=[ai1, wf1])
#plot the scan readable agains the scan timestamps
plot(ret[ai2], xdata=ret.timestamps)
#plot the scan monitors against their timestamps
plot(ret[ai1][1], xdata=ret[ai1][0])




################################################################################################### 
# Example of using OTF object with an unique value (create a single monitor)
################################################################################################### 

class OTF(Otf):
    def __init__(self, name):
        Otf.__init__(self, name)
        self.setCache(float("NaN"), long(time.time()*10e6))
        
    def task(self):
        try:
            #Simulate some processing
            time.sleep(5.0)            
            #Sends data after the processing
            for i in range(20):
                self.setCache(float(i), long((time.time()+i/10.0)*10e6))
            self.getLogger().info("Finished OTF")    
        except:
            self.getLogger().warning("Exceptin in OTF: " + str(sys.exc_info()[1]))     
        finally:
            self.getLogger().info("Exit OTF thread")       
            self.state=State.Ready

    def start(self):       
        self.state=State.Busy
        self.thread = fork(self.task)[0]
        
    def abort(self):
        self.getLogger().info("Interrupting OTF thread")
        self.thread.cancel(True)
        join(self.thread)
        
add_device(OTF("otf"), True)
        
ret = tscan(None, 0, 2.0, monitors=[otf], display=False)
#plot the otf value against its timestamp
plot(ret[otf][1], xdata=ret[otf][0])


################################################################################################### 
# Example of using OTF object with multiple values
################################################################################################### 


class OtfValue(ReadonlyAsyncRegisterBase):
    def __init__(self, name):
        ReadonlyAsyncRegisterBase.__init__(self, name)
        
    def setValue(self, value,timestamp):
        self.setCache(value,timestamp)

class OTF(Otf):
    def __init__(self, name):
        Otf.__init__(self, name)
        self.c1 = OtfValue("Channel1")
        self.c2 = OtfValue("Channel2")
        self.addComponents([self.c1, self.c2])
        self.c1.setValue(float("NaN"), long(time.time()*10e6))
        self.c2.setValue(-1, long(time.time()*10e6))
        self.initialize()

    def task(self):
        start = time.time()
        try:
            #Simulate streaming during a process
            for i in range(40):
                time.sleep(0.1)
                self.c1.setValue(time.time(), long(time.time()*10e6))
                finished = (time.time()-start)> 5.0       
            #Simulate streaming after a process  
            for i in range(20):
                self.c2.setValue(i,long((time.time()+i/10.0)*10e6))
            self.getLogger().info("Finished OTF")    
        except:
            self.getLogger().warning("Exceptin in OTF: " + str(sys.exc_info()[1]))     
        finally:
            self.getLogger().info("Exit OTF thread")       
            self.state=State.Ready

    def start(self):
        self.state=State.Busy
        self.interrupted = False
        self.thread = fork(self.task)
        
    def abort(self):
        self.getLogger().info("Interrupting OTF thread")
        self.thread[0].cancel(True)
        join(self.thread)

add_device(OTF("otf"), True)
        
ret = tscan(None, 0, 2.0, monitors=[otf], display=False)

#plot the otf values against their timestamps
plot([ret[otf.c1][1],ret[otf.c2][1]], [otf.c1.name, otf.c2.name], xdata=[ret[otf.c1][0], ret[otf.c2][0]])

