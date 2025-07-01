///////////////////////////////////////////////////////////////////////////////////////////////////
// Demonstrate use of scan callbacks to trigger a detector at falling edge.
/////////////////////////////////////////////////////////////////////////////////////////////////// 


function BeforeReadout(position){
    ao1.write(1)
    ao1.write(0)
    //Example with an epics direct channel access
    //caput("CHANNEL_NAME", 1)
    //caput("CHANNEL_NAME", 0)    
    print ("In position: " + position[0] + ", " + position[1])      
}
    



function AfterReadout(record, scan){
    print ("Aquired frame: " + record.index)
}

a= lscan([m1,m2], [ai1, ai2], [0,0], [4,8], steps=20, latency = 0.01, relative = undefined, 
         passes = undefined, zigzag = undefined,before_read=BeforeReadout, after_read=AfterReadout)