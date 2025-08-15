///////////////////////////////////////////////////////////////////////////////////////////////////
// Using bsearch(Binary Search) and hsearch(Hill Climbing Search) to find optimum
///////////////////////////////////////////////////////////////////////////////////////////////////


var FitnessFunction = Java.extend(ReadonlyRegisterBase)
add_device(new FitnessFunction("fitness") {	    
    doRead: function () {
    	return 1000.0 - (Math.pow(ao1.take()-18, 2) + Math.pow(ao2.take()-6, 2))
    },
}, true)                
    


//Plot Fitness Function
r = ascan([ao1, ao2], fitness, [0.0,0.0], [21.0,26.0], [1.0, 1.0], title = "Fitness")

//Binary Search
strategy = "Normal" // or "Boundary" or "FullNeighborhood"
r = bsearch([ao1, ao2], fitness, [0.0,0.0], [21.0,26.0], [0.1, 0.1], maximum=true, strategy = strategy, latency = 0.01, relative=false, before_read=undefined, after_read=undefined, title = "Binary Search")
//Relative search:
//ao1.write(10.5); ao2.write(13.0)
//r = bsearch([ao1, ao2], fitness, [-10.5,-13.0], [10.5,13.0], [0.1, 0.1],maximum=true, strategy = strategy, latency = 0.01, relative = true, before_read=undefined, after_read=undefined, title = "Binary Search")

print ("---------------  Binary Search  -----------------")
print (r)
print (r.print())
print (r.getRecords().length)


//Hill Climbing Search
ao1.write(10.5)
ao2.write(13.0)
r = hsearch([ao1, ao2], fitness,[0.0,0.0], [21.0,26.0], [1.0, 1.0], [0.1, 0.1], 1, maximum=true, latency = 0.01, relative=false, before_read=undefined, after_read=undefined, title = "Hill Climbing")
print ("---------------  Hill Climbing Search  -----------------")
print (r)
print (r.print())
print (r.getRecords().length)
