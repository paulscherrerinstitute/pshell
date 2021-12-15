################################################################################################### 
# Calibrating array and matrix pseudo-devices
################################################################################################### 


class MyReadableCalibratedArray(ReadableCalibratedArray):
    def read(self):
        return wf1.read()
        
    def getSize(self):
        return wf1.getSize()
        
    def getCalibration(self):
        return ArrayCalibration(5,1000)
        

class MyReadableCalibratedMatrix(ReadableCalibratedMatrix):
    def read(self):
        return im1.read()
        
    def getWidth(self):
        return im1.getWidth()

    def getHeight(self):
        return im1.getHeight()

    def getCalibration(self):
        return MatrixCalibration(2,4,100,200)     


wfc1 = MyReadableCalibratedArray("wfc1")
imc1 = MyReadableCalibratedMatrix("imc1")

a= lscan(ao1, [wf1, wfc1, im1, imc1, wfc2], 0, 40, 50, 0.02)    