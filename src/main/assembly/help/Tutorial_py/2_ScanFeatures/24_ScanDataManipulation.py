################################################################################################### 
# Processing and plotting scan data.
################################################################################################### 


ao1.write(0.0)
scan1= lscan(ao1, (ai1,ai2,wf1), 0, 40, 40, 0.01, False, title="Scan 1")
scan2= lscan(ao1, (ai1,ai2,wf1), 0, 40, 40, 0.01, False, title="Scan 2")


from operator import add
result = map(add, scan1.getReadable(0), scan2.getReadable(0))

#Alternative:
#result=[]
#for i in range(len(scan1.records)):
#	result.append(scan1.records[i].values[0]+scan2.records[i].values[0])


plot(result)
print result

