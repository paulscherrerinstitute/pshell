################################################################################################### 
# EPICS direct channel access.
# EPICS devices implemented are included in PShell, package ch.psi.pshell.epics.
# However direct channel access builtin functions are available.
################################################################################################### 


channel_name = "TESTIOC:TESTCALCOUT:Output"

#reading/writing to a channel
print (caget(channel_name))
caput(channel_name, 0.0)
#Put with no wait
caput(channel_name, 0.0)
print (caget(channel_name))

#waiting for a channel value
cawait(channel_name, 0.0, timeout = 10.0)


#If many IO it is better to use a Device
ch1=create_channel_device(channel_name, type='d', size=None, device_name="ch1", monitored=False)
add_device(ch1, True)
lscan(ch1, ai2, 0, 10, 0.1)


#Or:
channel = ChannelDouble("My Channel", channel_name)
channel.initialize()
lscan(channel, ai2, 0, 10, 0.1)
channel.close()



#Creating a table reading set of channels for each scan step.
attrs_dataset = None
attrs_names = ["TESTIOC:TESTCALCOUT:Input",
    "TESTIOC:TESTCALCOUT:Output", 
    "TESTIOC:TESTSINUS:SinCalc",
    "TESTIOC:TESTWF2:MyWF"]
attrs_types = ["d", "d", "d", "[d"]
attrs_lenghts = [0,0,0,10]

def after_read(rec):
    global attrs_dataset, attrs_names, attrs_type, attrs_lenghts
    if attrs_dataset is None:
        attrs_dataset = get_exec_pars().getGroup() + "attributes"
        create_table(attrs_dataset, attrs_names, attrs_types, attrs_lenghts)    
    record = []
    for i in range(len(attrs_names)):
        record.append(caget(attrs_names[i], attrs_types[i]))
    #print record
    append_table(attrs_dataset, record)   

a = lscan(m1, (ai1, ai2), 0, 0.1, 20, 0.01, after_read=after_read)



#Epics Registers can be configured with a ChannelSettlingCondition object to 
#wait for a condition after a write operation.
#The following scan is performed waiting "TESTIOC:TESTCALCOUT:Input" value to be 0 after every write.
#A readback is used with with no dependency on the setpoint (the in-position band is set to infinity).

caput("TESTIOC:TESTCALCOUT:Input", 0.0)

positioner = ControlledVariable("positioner", "TESTIOC:TESTCALCOUT:Output", "TESTIOC:TESTSINUS:SinCalc")
positioner.getConfig().resolution=float('inf') 
positioner.initialize()
positioner.setSettlingCondition(ChannelSettlingCondition("TESTIOC:TESTCALCOUT:Input", 0.0))
positioner.getSettlingCondition().setLatency(100)

lscan(positioner, [ai1], 1.0, 1.5, 0.05 , latency= 0.1, range="auto")

