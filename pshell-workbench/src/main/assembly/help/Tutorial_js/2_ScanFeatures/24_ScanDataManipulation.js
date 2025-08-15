///////////////////////////////////////////////////////////////////////////////////////////////////
// Processing and plotting scan data.
/////////////////////////////////////////////////////////////////////////////////////////////////// 


ao1.write(0.0)
scan1= lscan(ao1, [ai1,ai2,wf1], 0, 40, 40, 0.01, false)
scan2= lscan(ao1, [ai1,ai2,wf1], 0, 40, 40, 0.01, false)


result=[]
for (var i=0; i<scan1.records.length; i++){
	result.push(scan1.records[i].values[0]+scan2.records[i].values[0])
}


plot(result)
print (result)
