################################################################################################### 
# Demonstrate use of Vector Scan: one or multiple positioners set with a list or generator.
################################################################################################### 


#1D vector scan, positions in a list
vector = [ 1, 3, 5, 10, 25, 40, 45, 47, 49]
r1 = vscan(ao1,(ai1,ai2),vector,False, 0.5, title = "1D Vector")


#1D vector scan, positions given by a generator
def gen():    
    num = 0.0
    while num < 10:
        yield num
        num += 1
v2 = vscan(ao1,(ai1,ai2),gen(),False, 0.5, title = "1D Generator")


vector = [  [1,1] , [1,2] , [1,3] , [1,4]   ,
                     [1.5,2.5]              , 
            [2,1] , [2,2] , [2,3] , [2,4]   ,
                     [2.5,2.5]              ,
            [3,1] , [3,2] , [3,3] , [3,4]     ]

r3 = vscan((m1,m2),(ai1,ai2),vector,False, 0.1, title = "2D Vector")            


#2D vector scan, positions given by a generator
def gen():    
    a,b = 0.0, 1.0
    while abs(a) < 1.0:
        while b < 2.0:
            yield [a,b]
            b=b+0.1
        b=1.0
        a=a+0.25
r2 = vscan((m1,m2),(ai1,ai2),gen(),False, 0.1, title = "2D Generator", range = [-1.0,1.0, 1.0, 2.0])
