################################################################################################### 
# Executing CPython in external process.
# Requires cpython.py to be put in the scripts folder, or else in the python path.
################################################################################################### 

#Calling a method: return values must be JSON serializable
ret = exec_cpython("cpython", [], "test_pandas")
print ret

#Executing a module
ret = exec_cpython("cpython", [])
print ret
