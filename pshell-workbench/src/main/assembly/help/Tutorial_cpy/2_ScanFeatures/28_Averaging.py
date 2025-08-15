################################################################################################### 
# Scan averaging sensor value, storing all values, mean and variance
###################################################################################################


av = create_averager(ai2, 5, 0.05)
#set_preference(Preference.PLOT_TYPES, {av.name:'minmax'})  #This is to display min/max instead of sigma.
res= lscan(ao1, (av, av.getSamples()), 0, 40, 20, 0.1)

#If the averager is set to monitored, then it samples in the background, not blocking in scan read.
m1.setPolling(100)
m1.moveRelAsync(2.0)
av = create_averager(m1.getReadback(), 3, 0.05)
av.setMonitored(True)
sleep(0.2) #Give some time for the averager to fill its buffer, before first use
res= lscan(ao1, (av, av.getSamples()), 0, 40, 20, 0.1)
