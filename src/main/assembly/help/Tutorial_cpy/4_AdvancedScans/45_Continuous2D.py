################################################################################################### 
# This is an option to implement a 2D continuous scan
################################################################################################### 

STEPS_M1 = 10
STEPS_M2 = 5
        
class Sensor(ReadableArray):
    def read(self):
        r1 = cscan(m1, (ai1), 0.0, 1.0 , steps=STEPS_M1, save=False, display=False) 
        return r1.getReadable("ai1")
        
    def getSize(self):
        return STEPS_M1+1        

lscan(m2, Sensor(), 0, 1.0, STEPS_M2)