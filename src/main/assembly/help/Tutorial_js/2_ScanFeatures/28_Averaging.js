///////////////////////////////////////////////////////////////////////////////////////////////////
// Scan averaging 
///////////////////////////////////////////////////////////////////////////////////////////////////


av = create_averager(ai2, 5, 0.05)
//set_preference(Preference.PLOT_TYPES, {av.name:'minmax'})  //This is to display min/max instead of sigma.
res= lscan(ao1, [av, av.samples], 0, 40, 20, 0.1)


//If the averager is set to monitored, then it samples in the background, not blocking in scan read.
av.setMonitored(true)

//Give some time for the averager to fill its buffer, before first use
sleep(0.5)
res= lscan(ao1, [av, av.samples], 0, 40, 20, 0.1)
