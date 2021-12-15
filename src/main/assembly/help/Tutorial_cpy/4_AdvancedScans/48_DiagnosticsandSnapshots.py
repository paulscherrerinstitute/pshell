################################################################################################### 
# Example of saving diagostics and snapshots during a scan 
# Snapshots devices are Readables that are sampled before the start of the scan.
# Diagnostic devices are Readables that are sampled for each scan step, similar to normal sensors.
# The goal is to factorize the acquisition of auxiliary data that is collected for many scans.
################################################################################################### 

DIAGS = [ai2, wf1, wf2]
SNAPS = [im1, wf1, wf2]

ret = lscan(m1, ai1, 0.0, 1.0, 4,  diags=DIAGS, snaps=SNAPS)

plot(ret.getDiag('ai2'))
plot(ret.getDiag('wf1'))
plot(ret.getSnap('wf2'))
