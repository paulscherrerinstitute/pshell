################################################################################################### 
# Use of Snapshot class to read and restore the state of devices
################################################################################################### 

#Initial state
ao1.write(1.0)
ao2.write(2.0)

#Snapshot creation: list of read-writable devices and an optional name.
#The name is only needed for saving/loading from files, so multiple different snapshots can be saved at a time. 
#If ommited then name is set to "default", and it will overrite other snapshotws when saved.
s=Snapshot([ao1, ao2], "snapshot1") 

#Take snapshot
errors = s.take(Snapshot.Mode.PARALLEL)
if len(errors)>0: #take() returns return errors as a dict device -> exception. If empty then all devices were successfully read.
    device=errors.keys()[0]; e=errors[device]
    raise Exception("Error taking " +  device.name + " - " + str(e.message))
#Do stuff
ao1.write(4.0)
ao2.write(5.0)
time.sleep(2.0)

#Restore
errors = s.restore(Snapshot.Mode.PARALLEL)
if len(errors)>0: #restore() returns errors as a dict device -> exception. If empty then all devices were successfully restored.
    device=errors.keys()[0]; e=errors[device]
    raise Exception("Error restoring " +  device.name + " - " + str(e))

#Mode for take and restore can be: 
#Snapshot.Mode.PARALLEL: all the devices are accessed in parallel, returning all errors.
#Snapshot.Mode.SERIES: all the devices are accessed sequentially, returning all errors.
#Snapshot.Mode.STOP_ON_ERROR: all the devices are access sequentially, but stops upon the first error.

#Saving and loading from files 
errors = s.take(Snapshot.Mode.PARALLEL)
if len(errors)>0:
    raise errors[0]
timestamp = s.save()

#Do stuff
ao1.write(8.0)
ao2.write(9.0)
time.sleep(2.0)

#If you had still the reference to s when loading, you could use it to load
s.load(timestamp) #Loads the snapshot from that timestamp, or the most recent if timestamp is ommited
s.restore()
if len(errors)>0:
    raise errors.values(0)

ao1.write(6.0)
ao2.write(4.0)
time.sleep(2.0)

#If you must load from file in a different script, then you must create a snapshot having the same name and set of devices.

s2=Snapshot([ao1, ao2], "snapshot1")
s2.load() #Loads the last snapshot sved
s2.restore()
if len(errors)>0:
    raise errors.values(0)


