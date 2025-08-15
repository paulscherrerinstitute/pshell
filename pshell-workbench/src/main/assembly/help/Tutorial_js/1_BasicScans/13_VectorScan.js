///////////////////////////////////////////////////////////////////////////////////////////////////
// Demonstrate use of Vector Scan: one or multiple positioners set according to a position vector.
///////////////////////////////////////////////////////////////////////////////////////////////////


//1D vector scan, plot to 1D Vector tab
vector = [ 1, 3, 5, 10, 25, 40, 45, 47, 49]
r1 = vscan(ao1, [ai1,ai2], vector, false, 0.5, false, undefined, undefined, undefined, undefined, title = "1D Vector")




//2D vector scan, plot to 2D Vector tab
vector = [  [1,1] , [1,2] , [1,3] , [1,4]   ,
                     [1.5,2.5]              , 
            [2,1] , [2,2] , [2,3] , [2,4]   ,
                     [2.5,2.5]              ,
            [3,1] , [3,2] , [3,3] , [3,4]     ]

r2 = vscan([m1,m2], [ai1,ai2], vector, false, 0.1, false, undefined, undefined, undefined, undefined, title = "2D Vector")