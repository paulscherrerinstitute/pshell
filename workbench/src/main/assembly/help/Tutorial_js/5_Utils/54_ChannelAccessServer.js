/////////////////////////////////////////////////////////////////////////////////////////////////// 
// Creation of EPICS CA servers over register devices
/////////////////////////////////////////////////////////////////////////////////////////////////// 

CAS = Java.type('ch.psi.pshell.epics.CAS')

cas = []

//CAS.setServerPort(5062)

cas1 = new CAS("TESTCAS:c1", ai1, 'double') 
cas2 = new CAS("TESTCAS:c2", ai1, 'int') 
cas3 = new CAS("TESTCAS:c3", ai1, 'string') 
cas4 = new CAS("TESTCAS:c4", ao1, 'double')
cas5 = new CAS("TESTCAS:c5", dp1, 'string')
cas6 = new CAS("TESTCAS:c6", wf1, 'double')
cas7 = new CAS("TESTCAS:c7", wf1, 'int')
cas8 = new CAS("TESTCAS:c8", wf1, 'string')

print (caget("TESTCAS:c1"))
print (caget("TESTCAS:c2"))
print (caget("TESTCAS:c3"))
print (caget("TESTCAS:c4"))
print (caget("TESTCAS:c5"))
print (to_array(caget("TESTCAS:c6")))
print (to_array(caget("TESTCAS:c7")))
print (to_array(caget("TESTCAS:c8","[s")))


