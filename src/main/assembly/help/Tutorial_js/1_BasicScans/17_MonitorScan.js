///////////////////////////////////////////////////////////////////////////////////////////////////
// Demonstrate the use of Monitor Scan: sampling based on device change event
///////////////////////////////////////////////////////////////////////////////////////////////////


//Simulating async devices:
ai1.setPolling(50)
ai2.setPolling(50)


//Execute the scan: 25 samples
r1 = mscan(ai1, [ai1], 25)

//Execute the scan: 50 samples, with a timeout of 2s, including a second device, which cache is sampled
r2 = mscan(ai1, [ai1, ai2], 50, 2.0)

//Execute the scan: sampling for 5s, an undefined number of sample
r3 = mscan(ai1, [ai1, ai2], -1, 5.0)

//If a non-cached sensor access is needed, scan must be started in sync mode, but records may then be lost.
//In this example ai1 is cached (the trigger always is), wf1 is not, and ai2 is.
r4 = mscan(ai1, [ai1, wf1, ai2.cache], -1, 5.0, async = false)


// Scanning a set of sensors based on a software trigger, using mscan
var Trigger = Java.extend(ReadonlyRegisterBase)
trigger = new Trigger("trigger") {	    
    doRead: function () {
    	return null;
    },
}     
trigger.initialize()

function scan(){
    mscan(trigger, [ai1, ai2], 10)
}

scan_task = fork(scan)

sleep(0.5)
for (i =0; i<10; i++){
    trigger.update()
    sleep(Math.random()/5)
}

ret = join(scan_task)

//Restoring the device state
ai1.setPolling(0)
ai2.setPolling(0)