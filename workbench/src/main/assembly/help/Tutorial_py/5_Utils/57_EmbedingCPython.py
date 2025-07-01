################################################################################################### 
# Embedding CPython with JEP: USe of numpy, pandas and matpplotlib in the same process.
# Requires cpython.py to be put in the scripts folder, or else in the python path.
################################################################################################### 

#Requirements to the use of JEP:
# 1- PYTHONHOME is set to the python interpreter home folder.	
# 2- jep*.so is in LD_LIBRARY_PATH (e.g. in extensions folder )
# 3- jep*.jar is in the CLASS_PATH (e.g. in extensions folder )
#JEP works with python 2 and 3.

from jeputils import *

#In order to use matplotlib we must set Tk backend before importing plt
eval_jep("import matplotlib")
eval_jep("matplotlib.use('TkAgg')")
    

#Evaluating statements
eval_jep("import sys")
eval_jep("import numpy as np")
eval_jep("a = np.array((100, 100), )")
eval_jep("print (a)")
eval_jep("""
def stderr(str):     
    if sys.version_info < (3,0) :
        exec("print >> sys.stderr, '" + str + "'")
    else:
        exec("print ('" + str + "', file=sys.stderr)")
""")

eval_jep("stderr('Testing stderr')")

#Accessing a numpy array
a=get_jep("a")
print a.getData()
print a.getDimensions()


#Setting numpy array with scan data
steps = (3,4)
dims = (steps[0]+1,steps[1]+1)
r = ascan((m1,m2), (ai1), (0.0,0.0), (0.2,0.2), steps)
data = r[ai1]
a = to_npa(data, dims,'d')
print a.getDimensions()
print a.getData()
plot( Convert.reshape(a.getData(),a.getDimensions()),title="Scan Data")

#Calling a module function
b = call_jep("numpy", "transpose", [a,])
print b.getDimensions()
print b.getData()
plot( Convert.reshape(b.getData(),b.getDimensions()),title="Transposed")


#More calculations calling numpy
a = call_jep("numpy", "ones", [[400,200],'d'])
for i in range (100):
    b = call_jep("numpy", "ones", [[400,200],'d'])
    a = call_jep("numpy", "add", [a,b])
    s = call_jep("numpy", "sum", [a,])
    print a.getData()[0], s
    sleep(0.001)


#Calling a local function
data, dims = [1,2,3,4,5,6,7,8,9,0], [2,5] 
array = to_npa(data, dims,'d') #Auxiliary function to create numpy arrays from lists or java arrays.
ret = call_jep("cpython", "calc", [array,])
print ret.getDimensions(),ret.getData()


#Testing pandas
ret = call_jep("cpython", "test_pandas")


#Testing tkinter
ret = call_jep("cpython", "test_tkinter")


#Testing matplotlib
ret = call_jep("cpython", "test_matplotlib", [0.1, 4, 0.4])


#Running a modole
run_jep("cpython")

