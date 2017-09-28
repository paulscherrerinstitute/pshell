///////////////////////////////////////////////////////////////////////////////////////////////////
// Demonstrate configuring scan plot options
///////////////////////////////////////////////////////////////////////////////////////////////////


//This optional preference limits the displayed plots
set_preference(Preference.ENABLED_PLOTS, [ai1, wf1])

//This optional preference displays wf1 as a 1d plot at each scan point, instead of a matrix plot
set_preference(Preference.PLOT_TYPES, {wf1:1})


//This optional preference disables printing the scan table
//set_preference(Preference.SCAN_PLOT_DISABLED, true) 

//This optional preference disable all scan plotting
//set_preference( Preference.SCAN_TABLE_DISABLED, true) 

//Execute the scan: 200 steps, a1 from 0 to 40
a= lscan(ao1, [ai1,ai2,wf1], 0, 40, 100, 0.01)                    




//This optional preference displays wf1 as a 1d plot at each scan point, instead of a matrix plot
set_preference(Preference.PLOT_TYPES, {wf1:1})

ascan([m1,m2], [ai1,wf1], [0.0,0.0], [2.0,1.0], [4,4])


