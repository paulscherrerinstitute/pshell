/////////////////////////////////////////////////////////////////////////////////////////////////// 
// EPICS direct channel access.
// EPICS devices implemented are included in PShell, package ch.psi.pshell.epics.
// However direct channel access builtin functions are available.
/////////////////////////////////////////////////////////////////////////////////////////////////// 


channel_name = "TESTIOC:TESTCALCOUT:Output"

//reading/writing to a channel
print (caget(channel_name))
caput(channel_name, 0.0)
//Put with no wait
caput(channel_name, 0.0)
print (caget(channel_name))

//waiting for a channel value
cawait(channel_name, 0.0, timeout = 10.0)

//If many IO it is better to keep the same CA connection
channel = create_channel(channel_name, 'd')
for (var i=0; i<10; i++){
    print (channel.get())
}    
channel.close()


//The create_channel_device method return a device implements Readable and Writable and therefore can be used in scans
channel = create_channel_device(channel_name, 'd', null, "My Channel")
lscan(channel, ai2, 0, 10, 0.1)
channel.close()


//Or else we can use a Device
ChannelDouble= Java.type('ch.psi.pshell.epics.ChannelDouble')
channel = new ChannelDouble("My Channel", channel_name)
channel.initialize()
lscan(channel, ai2, 0, 10, 0.1)
channel.close()



//Creating a table reading set of channels for each scan step.
attrs_dataset = null
attrs_names = ["TESTIOC:TESTCALCOUT:Input",
    "TESTIOC:TESTCALCOUT:Output", 
    "TESTIOC:TESTSINUS:SinCalc",
    "TESTIOC:TESTWF2:MyWF"]
attrs_types = ["d", "d", "d", "[d"]
attrs_lenghts = [0,0,0,10]


function AfterReadout(rec){
    if (attrs_dataset == null){
        attrs_dataset = get_exec_pars().group + "attributes"
        create_table(attrs_dataset, attrs_names, attrs_types, attrs_lenghts)    
    }
    record = []
    for (var i=0; i<attrs_names.length; i++){
    	val = caget(attrs_names[i], attrs_types[i])
        record.push(val)
    }
    //print record
    append_table(attrs_dataset, record)   
}    

a = lscan(m1, [ai1, ai2], 0, 0.1, 20, 0.01, undefined, undefined, undefined, undefined, after_read=AfterReadout)