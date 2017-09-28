################################################################################################### 
# Calibrating array and matrix pseudo-devices
################################################################################################### 


class ArrayCalibrated(ReadableArray, ReadableCalibratedArray):
    def read(self):
        return wf1.read()
        
    def getSize(self):
        return wf1.size
        
    def getCalibration(self):
        return ArrayCalibration(5,1000)
        

class MatrixCalibrated(ReadableMatrix, ReadableCalibratedMatrix):
    def read(self):
        return im1.read()
        
    def getWidth(self):
        return im1.width

    def getHeight(self):
        return im1.height

    def getCalibration(self):
        return MatrixCalibration(2,4,100,200)       

ac1 = ArrayCalibrated()
ac2 = ArrayCalibrated()
mc1 = MatrixCalibrated()

set_device_alias(ac1, "wf1_calib")
set_device_alias(ac2, "wf1_calib_1d")
set_device_alias(mc1, "im1_calib")
setup_plotting(line_plots = (ac2,))

a= lscan(ao1, [wf1, ac1, im1, mc1, ac2], 0, 40, 50, 0.02)         