################################################################################################### 
# Processing and plotting scan data.
################################################################################################### 



ao1.write(0.0)
scan1= lscan(ao1, (ai1,ai2,wf1), 0, 40, 40, 0.01, False, title="Scan 1")
scan2= lscan(ao1, (ai1,ai2,wf1), 0, 40, 40, 0.01, False, title="Scan 2")


result=[]
for i in range(len(scan1.getRecords())):
    result.append(scan1.getReadable("ai1")[0]+scan2.getReadable("ai1")[0])


plot(result)
print (result)



