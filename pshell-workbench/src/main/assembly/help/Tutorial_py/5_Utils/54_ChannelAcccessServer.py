################################################################################################### 
# Creation of EPICS CA servers over register devices
################################################################################################### 

#The local CA port can be defined as
#CAS.setServerPort(5062)

#PVs can be created as:
waveform = PV.newWaveform("TEST:WAVEFORM", "double", 5)
scalar = PV.newScalar("TEST:SCALAR", "double")

#PVs are updated writing to the registers
def after_read(record, scan):
    waveform.write(scan.result[ai1])
    scalar.write(record.index)
    
r= tscan((ai1, ai1), 5, 0.3, after_read=after_read, save=False)
waveform.write(r[ai1])
scalar.write(len(r))

print caget("TEST:WAVEFORM")
print caget("TEST:SCALAR")


#Channels can be created to map existing Registers:

cas1 = CAS("TESTCAS:c1", ai1, 'double') 
cas2 = CAS("TESTCAS:c2", ai1, 'int') 
cas3 = CAS("TESTCAS:c3", ai1, 'string') 
cas4 = CAS("TESTCAS:c4", ao1, 'double')
cas5 = CAS("TESTCAS:c5", dp1, 'string')
cas6 = CAS("TESTCAS:c6", wf1, 'double')
cas7 = CAS("TESTCAS:c7", wf1, 'int')
cas8 = CAS("TESTCAS:c8", wf1, 'string')

print caget("TESTCAS:c1")
print caget("TESTCAS:c2")
print caget("TESTCAS:c3")
print caget("TESTCAS:c4")
print caget("TESTCAS:c5")
print caget("TESTCAS:c6").tolist()
print caget("TESTCAS:c7").tolist()
print caget("TESTCAS:c8","[s").tolist()


