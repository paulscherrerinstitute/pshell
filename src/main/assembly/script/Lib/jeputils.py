###################################################################################################
# Facade to JEP: Embedded CPython
###################################################################################################

#Matplotlib won't work out of the box because it's default backend (Qt) uses signals, which only works in
#the main thread. Ideally should find a fix, in order to mark the running thread as the main.
#As  a workaround, one can use the Tk backend:
#
#import matplotlib
#matplotlib.use('TkAgg') 

#In general the only pre-condition is the environment variable "PYTHONHOME" to be be set with a Python 
#environment having JEP installed.
#
#Optionally one may use the config option PythonHome or command line argument -pyhm. In this case the
#software will try to set "PYTHONHOME".
#
#In some platforms it may be needed:
#       - Add JEP library folder to LD_LIBRARY_PATH
#       - Add <python home>/lib folder to LD_LIBRARY_PATH			
#	- Set LD_PRELOAD=<python home>/lib/libpython*.so


import sys
import os
import jep.Jep
import jep.SharedInterpreter
import jep.NDArray
import java.lang.Thread
import org.python.core.PyArray as PyArray
import java.lang.String as String
import java.util.List
import java.util.Map 
import java.util.HashMap 
import java.util.ArrayList 
import java.util.Collections
import ch.psi.pshell.scripting.ScriptUtils as ScriptUtils


from startup import to_array, get_context, _get_caller, Convert, Arr

__jep = {}

def __get_jep(): 
    t = java.lang.Thread.currentThread()
    if not t in __jep:
        init_jep()
    return __jep[t]

def __close_jep(): 
    t = java.lang.Thread.currentThread()
    if t in __jep:    
        __jep[t].close()

def init_jep():    
    #TODO: Should do it but generates errors
    #__close_jep()
    j = jep.SharedInterpreter()
    try:
        #Faster, but statements must be complete
        j.setInteractive(False) 
    except:
        pass    # Removed in 4.2
    __jep[java.lang.Thread.currentThread()] = j
    j.eval("import sys")
    #sys.argv is not present in JEP and may be needed for certain modules (as Tkinter)
    j.eval("sys.argv = ['PShell']");
    #Add standard script path to python path
    j.eval("sys.path.append('" + get_context().setup.getScriptPath() + "')")
    
    #Redirect stdout
    j.eval("class JepStdout:\n" + 
           "    def write(self, str):\n" +
           "        self.str += str\n" + 
           "    def clear(self):\n" +
           "        self.str = ''\n" +
           "    def flush(self):\n" +
           "        pass\n")
    j.eval("sys.stdout=JepStdout()");    
    j.eval("sys.stderr=JepStdout()");    
    j.eval("sys.stdout.clear()")
    j.eval("sys.stderr.clear()")

    #Import reload on Python 3
    j.eval("try:\n" + 
           "    reload  # Python 2.7\n" +
           "except NameError:\n" + 
           "    try:\n" +
           "        from importlib import reload  # Python 3.4+\n" +
           "    except ImportError:\n" +
           "        from imp import reload  # Python 3.0 - 3.3\n")

def __print_stdout():
    j=__get_jep()
    output = None
    err = None
    try:
        output = j.getValue("sys.stdout.str")
        err = j.getValue("sys.stderr.str")
        j.eval("sys.stdout.clear()")
        j.eval("sys.stderr.clear()")
    except:
        pass
    if (output is not None) and len(output)>0:
        print output
    if (err is not None) and len(err)>0:
        print >> sys.stderr, err
        
def run_jep(script_name, vars = {}):    
    global __jep
    script = get_context().scriptManager.library.resolveFile(script_name)
    if script is None : 
        script= os.path.abspath(script_name)
    j=__get_jep()
            
    for v in vars:
        j.set(v, vars[v])
    try:
        j.runScript(script)
    finally:
        __print_stdout()

def eval_jep(line):    
    j=__get_jep()
    try:
        j.eval(line)
    finally:
        __print_stdout()    

def set_jep(var, value):    
    j=__get_jep()
    j.set(var, value)

def get_jep(var):  
    j=__get_jep()  
    return j.getValue(var)

def call_jep(module, function, args = [], kwargs = {}, reload=False):
    j=__get_jep()
    if isinstance(args, java.util.List):
        args=list(args)
        
    if "/" in module: 
        script = get_context().scriptManager.library.resolveFile(module)       
        if "\\" in script: 
            #Windows paths
            module_path = script[0:script.rfind("\\")]
            module = script[script.rfind("\\")+1:]
        else:
            #Linux paths
            module_path = script[0:script.rfind("/")]
            module = script[script.rfind("/")+1:]
        eval_jep("import sys")    
        eval_jep("sys.path.append('" + module_path + "')")        
    if module.endswith(".py"): 
        module = module[0:-3]
    
    f = module+"_" + function+"_"+str(j.hashCode())
    try:
        if reload:
            eval_jep("import " + module)
            eval_jep("_=reload(" + module+")")
        eval_jep("from " + module + " import " + function + " as " + f)    
        if (kwargs is not None) and (len(kwargs)>0):
            hm=java.util.HashMap()
            hm.update(kwargs)            
            #The only way to get the overloaded method...
            m = j.getClass().getMethod("invoke", [String, ScriptUtils.getType("[o"), java.util.Map])
            ret = m.invoke(j, [f, to_array(args,'o'), hm])
        else:
            ret = j.invoke(f, args)
    finally:
        __print_stdout()    
    return ret

#Converts pythonlist or Java array to numpy array
def to_npa(data, dimensions = None, type = None):   
    if (not isinstance(data, PyArray)) or (type is not None):
        data = to_array(data,'d' if type is None else type)
    if dimensions is None:
        return jep.NDArray(data)    
    return jep.NDArray(data, dimensions)    

#recursivelly converts all NumPy arrays to Java arrys
def rec_from_npa(obj):
    ret = obj
    if isinstance(obj, jep.NDArray):
        ret = obj.data
        if len(obj.dimensions)>1:
            ret=Convert.reshape(ret, obj.dimensions)
    elif isinstance(obj, java.util.List) or isinstance(obj,tuple) or isinstance(obj,list):
        ret=[]
        for i in range(len(obj)):            
            ret.append(rec_from_npa(obj[i]))
        #if isinstance(obj,tuple) or obj.getClass().getSimpleName().startswith("Unmodifiable"): #TODO: Is it a good check for immutability?    
        #    ret=tuple(ret)
    elif isinstance(obj, java.util.Map) or isinstance(obj,dict):
        ret = {} 
        for k in obj.keys():            
            ret[k] = rec_from_npa(obj[k])     
    return ret

#recursivelly converts all Java arrays to NumPy arrays
def rec_to_npa(obj):
    ret = obj
    if isinstance(obj, PyArray):
        dimensions = Arr.getShape(obj)
        if len(dimensions)>1:
            obj = Convert.flatten(obj)
        ret = to_npa(obj, dimensions = dimensions)
    elif isinstance(obj, java.util.List) or isinstance(obj,tuple) or isinstance(obj,list):
        ret = java.util.ArrayList()
        for i in range(len(obj)):            
            ret.add(rec_to_npa(obj[i]))              
        #if isinstance(obj,tuple):
        #    ret = java.util.Collections.unmodifiableList(ret);
    elif isinstance(obj, java.util.Map) or isinstance(obj,dict):        
        ret = java.util.HashMap()
        for k in obj.keys():            
            ret[k] = rec_to_npa(obj[k])  
    return ret

def call_py(module, function, reload_function, *args, **kwargs):      
    """
    Calls a CPython function recursively converting Java arrays in arguments to NumPy,
    and  NumPy arrays in return values to Java arrays.
    """
    ret =  call_jep(module, function, rec_to_npa(args), rec_to_npa(kwargs), reload=reload_function)
    return rec_from_npa(ret)
    
def import_py(module, function):  
    """
    Adds a CPython function  to globals, creating a wrapper call to JEP, with 
    recurvive convertion of Java arrays in arguments to NumPy arrays,
    and  NumPy arrays in return values to Java arrays.
    """
    def jep_wrapper(*args, **kwargs):
        reload_function =  jep_wrapper.reload
        jep_wrapper.reload = False
        return call_py(module, function, reload_function, *args, **kwargs)
    jep_wrapper.reload=True
    _get_caller().f_globals[function] = jep_wrapper   
    return jep_wrapper   