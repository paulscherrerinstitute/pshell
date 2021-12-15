import random

####################################################################################################
# Simulated devices
####################################################################################################

add_device(DummyMotor("m1"), True)
add_device(DummyMotor("m2"), True)
add_device(DummyRegister("reg1",3), True)
add_device(DummyPositioner("p1"),True)
add_device(MotorGroupBase("mg1", m1, m2), True)
add_device(MotorGroupDiscretePositioner("dp1", mg1), True)


#Initial Configuration
if p1.getConfig().unit is None:
    p1.getConfig().minValue = 0.0 #Not persisted
    p1.getConfig().maxValue = 1000.0
    p1.getConfig().unit = "mm"
    p1.getConfig().save()
    p1.initialize()

if dp1.getConfig().positions is None:
    dp1.getConfig().positions = ["Park","Ready","Out","Clear"]
    dp1.getConfig().motor1 = ["0.0","4.0","8.0" ,"0.0"]
    dp1.getConfig().motor2 = ["0.0","5.0","3.0" ,"NaN"]
    dp1.getConfig().save()
    dp1.initialize()



#Update
m1.setMonitored(True)
m2.setMonitored(True)

####################################################################################################
# Readable / Writable objects can be created and used in scans
####################################################################################################

class MyWritable(Writable):
    def write(self, value):
        #print ("Write: ",value)
        pass
        
class MyReadable(Readable):
    def read(self):   
        return random.random()       

class MyReadableArray(ReadableArray):   
    def read(self):
        ret = []
        for i in range (self.getSize()):
            ret.append(random.random())
        return to_array(ret,'d')
         
    def getSize(self):
        return 20

class MyReadableArrayNumpy(ReadableArray):   
    def read(self):
        ret = numpy.ones(self.getSize(),'d')
        return ret
         
    def getSize(self):
        return 20        
                
class MyReadableMatrix(ReadableMatrix):     
    def read(self):
        ret = []
        for i in range (self.getHeight()):
            ret.append([random.random()] * self.getWidth())
        return to_array(ret, 'd')
    
    def getWidth(self):
        return 80

    def getHeight(self):
        return 40

class MyReadableMatrixNumpy(ReadableMatrix):     
    def read(self):
        ret = numpy.ones((self.getHeight(), self.getWidth()),'d')
        for i in range(self.getHeight()):
            ret[i]=i
        return to_array(ret, 'd')
    
    def getWidth(self):
        return 80

    def getHeight(self):
        return 40        
   

ao1 = MyWritable("ao1")
ao2 = MyWritable("ao2")
ai1 = MyReadable("ai1")
ai2 = MyReadable("ai2")
wf1 = MyReadableArray("wf1")
wf2 = MyReadableArrayNumpy("wf2")
im1 = MyReadableMatrix("im1")
im2 = MyReadableMatrixNumpy("im2")
