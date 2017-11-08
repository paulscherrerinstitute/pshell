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
    print channel.get(),
print ""

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



#Epics Registers can be configured with a ChannelSettlingCondition object to 
#wait for a condition after a write operation.
#The following scan is performed waiting "TESTIOC:TESTCALCOUT:Input" value to be 0 after every write.
#A readback is used with with no dependency on the setpoint (the in-position band is set to infinity).

import ch.psi.pshell.epics.ControlledVariable as ControlledVariable
import ch.psi.pshell.epics.ChannelSettlingCondition as ChannelSettlingCondition
caput("TESTIOC:TESTCALCOUT:Input", 0)
positioner = ControlledVariable("positioner", "TESTIOC:TESTCALCOUT:Output", "TESTIOC:TESTSINUS:SinCalc")
positioner.config.resolution = float('inf') 
positioner.initialize()
positioner.setSettlingCondition(ChannelSettlingCondition("TESTIOC:TESTCALCOUT:Input", 0))
positioner.settlingCondition.latency = 100

lscan(positioner, [ai1], 1.0, 1.5, 0.05 , latency= 0.1, range="auto")


#A custom SettlingCondition:
class MySettlingCondition(SettlingCondition):
    def doWait(self):
            time.sleep(0.1)
            cawait('TESTIOC:TESTCALCOUT:Output', self.getValue(), timeout = 3600.0)    
positioner.setSettlingCondition(MySettlingCondition())
lscan(positioner, [ai1], 1.0, 1.5, 0.05 , latency= 0.1, range="auto")
