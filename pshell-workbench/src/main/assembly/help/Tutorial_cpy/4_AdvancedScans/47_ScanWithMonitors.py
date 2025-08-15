################################################################################################### 
# Example of saving monitors during a scan 
################################################################################################### 

#Make simulated devices generate events on change (every 100ms)
m1.moveAsync(3.0)

ret = tscan(ai2, 10, 0.2, monitors=[m1.getReadback(), m2.getReadback()])
#plot the scan readable agains the scan timestamps
plot(ret.getReadable("ai2"), xdata=ret.getTimestamps())
#plot the scan monitors against their timestamps
plot(ret.getMonitor(m1.getReadback())[1], xdata=ret.getMonitor(m1.getReadback())[0])

