################################################################################################### 
# Using bsearch(Binary Search) and hsearch(Hill Climbing Search) to find optimum
################################################################################################### 


class FitnessFunction(ReadonlyRegisterBase):
    def doRead(self):
        return 1000.0 - (math.pow(ao1.take()-18, 2) + math.pow(ao2.take()-6, 2))

add_device(FitnessFunction("fitness"), True)            

#Plot Fitness Function
r = ascan([ao1, ao2], fitness, [0.0,0.0], [21.0,26.0], [1.0, 1.0], title = "Fitness")

#Binary Search
strategy = "Normal" # or "Boundary" or "FullNeighborhood"
r = bsearch([ao1, ao2], fitness, [0.0,0.0], [21.0,26.0], [0.1, 0.1], maximum=True, strategy = strategy, latency = 0.01, title = "Binary Search")
#Relative search:
#ao1.write(10.5); ao2.write(13.0)
#r = bsearch([ao1, ao2], fitness, [-10.5,-13.0], [10.5,13.0], [0.1, 0.1],relative = True, maximum=True, strategy = "Normal", title = "Binary Search")

print "---------------  Binary Search  -----------------"
print r
print r.print()
print len(r.getRecords())


#Hill Climbing Search
ao1.write(10.5); ao2.write(13.0)
r = hsearch([ao1, ao2], fitness,[0.0,0.0], [21.0,26.0], [1.0, 1.0], [0.1, 0.1], 1, relative = False, maximum=True, latency = 0.01, title = "Hill Climbing")
print "---------------  Hill Climbing Search  -----------------"
print r
print r.print()
print len(r.getRecords())