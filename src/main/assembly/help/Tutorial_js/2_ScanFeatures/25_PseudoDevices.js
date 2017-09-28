///////////////////////////////////////////////////////////////////////////////////////////////////
// Using pseudo-device to : 
//    - Add calculations to scan data.
//    - Execute logic during scan
/////////////////////////////////////////////////////////////////////////////////////////////////// 
      
var Clock = Java.extend(Readable)
var clock = new Clock() {	    
        read: function (value) {
        	return Date.now()
        },
    }
set_device_alias(clock, "Clock")    

var PseudoSensor = Java.extend(Readable)
var averager = new PseudoSensor() {	    
        read: function (value) {
            arr = wf1.take()  //Gets the CACHED waveform
            arr = to_array(arr) //Converts to a javascript array
            return  arr.reduce(function(a, b) { return a + b; }) / arr.length
        },
    }    
set_device_alias(averager, "Averager")        

var PseudoPositioner = Java.extend(Writable)
var positioner = new PseudoPositioner() {	    
        write: function (value) {
        	print ("Step = " + value)
        },
    }
set_device_alias(positioner, "Positioner")            


a= lscan([ao1,positioner],[ai2,wf1,averager,clock],[0,0],[40,20],20,0.1)


