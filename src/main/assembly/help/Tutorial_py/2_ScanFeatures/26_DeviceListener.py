################################################################################################### 
# Create a device listener to interrupt the scan
################################################################################################### 

import java.lang.InterruptedException

#Create a listener to the sensor, verifying the readback values.
class ListenerAI (DeviceListener):
    def onValueChanged(self, device, value, former):
        if value > 1.02:
            print "Value over limit-> aborting"
            abort()
listenerAI = ListenerAI()
ai1.addListener(listenerAI)



#Create a listener to the positioner checking the setpoint before each command is sent.
class ListenerAO (DeviceListener):
    def onStateChanged(self, device, state, former):
        pass
    def onValueChanged(self, device, value, former):
        print "Moved  to: " + str(value)
    def onValueChanging(self, device, value, former):
        if value > 20:
            #Vetoing the change will abort the scan
            raise Exception("Forbidden move to " + str(value))
        print "Moving to: " + str(value) + " ... " , 
listenerAO = ListenerAO()
ao1.addListener(listenerAO)


try:
    lscan(ao1, (ai1), 0, 40, 200, 0.01) 
except java.lang.InterruptedException:
    print "Aborted"
finally:
    ai1.removeListener(listenerAI)
    ao1.removeListener(listenerAO)
