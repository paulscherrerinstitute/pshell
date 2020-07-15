import random

####################################################################################################
# Simulated Devices
####################################################################################################

class AnalogOutput(RegisterBase):    
    def doRead(self):
        return self.val if hasattr(self, 'val') else 0.0 

    def doWrite(self, val):
        self.val = val

class AnalogInput(ReadonlyRegisterBase):
    def doRead(self):
        time.sleep(0.001)
        self.val = to_array(self.calc(), 'd')
        return self.val

class Waveform(ReadonlyRegisterBase, ReadonlyRegisterArray):
    def doRead(self):
        time.sleep(0.001)
        self.val = to_array(self.calc(), 'd')
        return self.val

class Image(ReadonlyRegisterBase, ReadonlyRegisterMatrix):
    def doRead(self):
        time.sleep(0.001)
        self.val = to_array(self.calc(), 'd')
        return self.val

    def getWidth(self):
        return len(self.take(-1)[0])

    def getHeight(self):
        return len(self.take(-1))



class Random(AnalogInput):
    def calc(self):
        return random.random()


class SinusoidSample(AnalogInput):
    def calc(self):
        self.x = self.x + 0.1 if hasattr(self, 'x') else 0.0
        noise = (random.random() - 0.5) / 10.0
        return math.sin(self.x)  + noise

class SinusoidTime(AnalogInput):
    def calc(self):
        noise = (random.random() - 0.5) / 10.0
        return math.sin(time.time())  + noise


class SinusoidWaveform(Waveform):
    def calc(self):
        ret = []
        x = random.random()
        for i in range (20):
            ret.append(math.sin(x))
            x = x + 0.1
        return ret

class SinusoidImage(Image):
    def calc(self):     
        (width, height) = (200, 100)
        ret = []
        x = random.random();
        base = []        
        for i in range (width):
            base.append( math.sin(x))
            x = x + 0.05
        for i in range (height):
            noise = (random.random() - 0.5)/5.0 
            ret.append([x+noise for x in base])
        return ret
        
        
#Defintion
add_device(DummyMotor("m1"), True)
add_device(DummyMotor("m2"), True)
add_device(DummyRegister("reg1",3), True)
add_device(AnalogOutput("ao1"), True)
add_device(AnalogOutput("ao2"), True)
add_device(SinusoidSample("ai1"), True)
add_device(SinusoidTime("ai2"), True)
add_device(Random("ai3"), True)
add_device(SinusoidWaveform("wf1"), True)
add_device(SinusoidImage("im1"), True)
add_device(DummyPositioner("p1"),True)
add_device(MotorGroupBase("mg1", m1, m2), True)
add_device(MotorGroupDiscretePositioner("dp1", mg1), True)



#Initial Configuration
if p1.config.unit is None:
    p1.config.minValue = 0.0 #Not persisted
    p1.config.maxValue = 1000.0
    p1.config.unit = "mm"
    p1.config.save()
    p1.initialize()

if dp1.config.positions is None:
    dp1.config.positions = ["Park","Ready","Out","Clear"]
    dp1.config.motor1 = ["0.0","4.0","8.0" ,"0.0"]
    dp1.config.motor2 = ["0.0","5.0","3.0" ,"NaN"]
    dp1.config.save()
    dp1.initialize()



#Update
m1.setMonitored(True)
m2.setMonitored(True)




####################################################################################################
# Simple Readable / Writable objects can be created and used in scans
####################################################################################################
class WritableScalar(Writable):
    def write(self, value):
        pass

class ReadableScalar(Readable):
    def read(self):        
        return random.random()


class ReadableWaveform(ReadableArray):
    def getSize(self):
        return 20

    def read(self):
        ret = []
        for i in range (self.getSize()):
            ret.append(random.random())
        return ret

class ReadableImage(ReadableMatrix):
    def read(self):
        ret = []
        for i in range (self.getHeight()):
            ret.append([random.random()] * self.getWidth())
        return to_array(ret, 'd')

    def getWidth(self):
        return 80

    def getHeight(self):
        return 40



ws1 = WritableScalar()
rs1 = ReadableScalar()
rw1 = ReadableWaveform()
ri1 = ReadableImage()


####################################################################################################
# Imaging
####################################################################################################

configured = os.path.exists(Device.getConfigFileName("src1"))

add_device(RegisterMatrixSource("src1", im1), True)
add_device(RegisterMatrixSource("src2", ri1), True)

src1.polling = 100
src2.polling = 100

#Some configuration for so the imaging will work out of the box
if not configured:
    import ch.psi.pshell.imaging.Colormap
    src1.config.colormapAutomatic = True
    src1.config.colormap = ch.psi.pshell.imaging.Colormap.Temperature
    src1.config.save()
    src2.config.colormapAutomatic = True
    src2.config.save()


