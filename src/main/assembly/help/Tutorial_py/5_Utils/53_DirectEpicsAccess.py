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

#waiting for a channel valur
cawait(channel_name, 0.0, timeout = 10.0)

#If many IO it is better to keep the same CA connection
channel = Channel(channel_name, 'd')
for i in range(100):
    print channel.get()

#The channel class implements Readable and Writable and therefore can be used in scans
lscan(channel, ai2, 0, 10, 0.1)
channel.close()


#Or else we can use a Device
import ch.psi.pshell.epics.ChannelDouble as ChannelDouble
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

def AfterReadout(rec):
    global attrs_dataset, attrs_names, attrs_type, attrs_lenghts
    if attrs_dataset is None:
        attrs_dataset = get_exec_pars().group + "attributes"
        create_table(attrs_dataset, attrs_names, attrs_types, attrs_lenghts)    
    record = []
    for i in range(len(attrs_names)):
        record.append(caget(attrs_names[i], attrs_types[i]))
    #print record
    append_table(attrs_dataset, record)   

a = lscan(m1, (ai1, ai2), 0, 0.1, 20, 0.01, after_read=AfterReadout)