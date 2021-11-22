################################################################################################### 
# Proxies to CPython functions implements almost transparent calls to CPython code.
# jeputils.import_py loads a proxy method to caller's globals handling:
# - Reloading of CPython module
# - Recursive convertion of Java arrays in arguments to NumPy arrays (minding dimensionality).
# - Recursive convertion of NumPy arrays in retuurn value to Java arrays (minding dimensionality).
################################################################################################### 

from jeputils import import_py

import_py("cpython", "calc")
import_py("cpython", "double_arr")


r1=  double_arr([1,2,3,4,5,6,7,8])
print r1

data = Convert.reshape(to_array([1,2,3,4,5,6,7,8,9,0],'d'), [2,5])
import_py("cpython", "calc")
r2 = calc(data)
print r2