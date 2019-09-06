###################################################################################################
# Facade to JEP: Embedded Python
###################################################################################################

#Matplotlib won't work out of the box because it's default backend (Qt) uses signals, which only works in
#the main thread. Ideally should find a fix, in order to mark the running thread as the main.
#As  a workaround, one can use the Tk backend:
#
#import matplotlib
#matplotlib.use('TkAgg') 


#In principle just add JEP jar and library to the extensions folder.
#
#Alternatively on Linux:
#   Python 2:
#       - Add <python home>/lib/python3.X/site-packages/jep to LD_LIBRARY_PATH
#       - Add <python home>/lib/python3.X/site-packages/jep/jep-X.X.X.jar to the class path
#
#Python3:
#       - Add JEP library folder to LD_LIBRARY_PATH
#       - If using OpenJDK, add also python <python home>/lib folder to LD_LIBRARY_PATH			
#	- Set LD_PRELOAD=<python home>/lib/libpython3.5m.so


import sys
import os
import jep.Jep
import jep.NDArray
import java.lang.Thread
from startup import to_array, get_context

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
    j = jep.Jep(False)
    #Faster, but statements must be complete
    j.setInteractive(False) 
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

def __print_stdout():
    j=__get_jep()
    output = j.getValue("sys.stdout.str")
    err = j.getValue("sys.stderr.str")
    j.eval("sys.stdout.clear()")
    j.eval("sys.stderr.clear()")
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

def call_jep(module, function, args = [], reload=False):
    j=__get_jep()
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
            eval_jep("reload(" + module+")")
        eval_jep("from " + module + " import " + function + " as " + f)    
        ret = j.invoke(f, args)
    finally:
        __print_stdout()    
    return ret

#Converts pythonlist or Java array to numpy array
def to_npa(data, dimensions = None, type = None):
   
    data = to_array(data,'d' if type is None else type)
    return jep.NDArray(data, dimensions)    