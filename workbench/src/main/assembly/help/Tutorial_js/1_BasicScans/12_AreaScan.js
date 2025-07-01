///////////////////////////////////////////////////////////////////////////////////////////////////
// Area Scan: Multiple positioners, each one is one dimension.
///////////////////////////////////////////////////////////////////////////////////////////////////


//The second sensor is an array. In the plot window it is overwritten in every same x position.
//The data window never displays 3d data, but the 3d data can be accesses during the scan in the Data tab.
r1 = ascan([m1,m2], [ai1,wf1], [0.0,0.0], [2.0,1.0], [10,10])


