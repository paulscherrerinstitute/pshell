################################################################################################### 
# Creation of EPICS CA servers over register devices
################################################################################################### 

cas = []

#CAS.setServerPort(5062)

cas1 = CAS("TESTCAS:M1:POSITION", m1.getReadback(), 'double') 

print (caget("TESTCAS:M1:POSITION"))


