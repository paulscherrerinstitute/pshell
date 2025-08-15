################################################################################################### 
# Example of saving diagostics and snapshots during a scan 
# Snapshots devices are Readables that are sampled before the start of the scan.
# Diagnostic devices are Readables that are sampled for each scan step, similar to normal sensors.
# The goal is to factorize the acquisition of auxiliary data that is collected for many scans.
################################################################################################### 

DIAGS = [ai2]
SNAPS = [ai3, wf1]

ret = lscan(m1, ai1, 0.0, 1.0, 4,  diags=DIAGS, snaps=SNAPS)

plot(ret.getSnap(wf1))
plot(ret.getDiag(ai2))

#All devices can be directly indexd
for dev in [m1, ai1, ai2, wf1, ai3]:
    print dev.name, " -> ",  ret[dev]
