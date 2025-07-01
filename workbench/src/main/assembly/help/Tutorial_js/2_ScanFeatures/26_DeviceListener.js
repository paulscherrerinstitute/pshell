///////////////////////////////////////////////////////////////////////////////////////////////////
// Create a device listener to interrupt the scan
/////////////////////////////////////////////////////////////////////////////////////////////////// 

InterruptedException = Java.type('java.lang.InterruptedException')

//Create a listener to the sensor, verifying the readback values.
      
var ListenerAI = Java.extend(DeviceListener)
var listenerAI = new ListenerAI() {	    
        onValueChanged: function (device, value, former) {
            if (value > 1.02) {
                print ("Value over limit-> aborting")
                abort()
            }
        },
    }
ai1.addListener(listenerAI)


var ListenerAO = Java.extend(DeviceListener)
var listenerAO = new ListenerAO() {	    
	    onStateChanged: function (device, state, former) {
	    	
	    },
        onValueChanged: function (device, value, former) {
            print ("Moved  to: " + value)
        },
        onValueChanginf: function (device, value, former) {
            if (value > 20) {
               throw "Forbidden move to " + value
            }
            print( "Moving to: " + value + " ... ")
        },
    }

ao1.addListener(listenerAO)




try{
    lscan(ao1, (ai1), 0, 40, 200, 0.01) 
} catch (ex){
    print ("Aborted")
}
finally{
    ai1.removeListener(listenerAI)
    ao1.removeListener(listenerAO)
}
