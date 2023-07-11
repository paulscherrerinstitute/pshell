###################################################################################################
#  Global definitions and built-in functions
###################################################################################################

from builtin_utils import *
from builtin_classes import *
from builtin_functions import *


###################################################################################################
#Default empty callbacks
###################################################################################################
def on_command_started(info): pass
def on_command_finished(info): pass
def on_session_started(id): pass
def on_session_finished(id): pass
def on_change_data_path(path): pass
def on_system_restart(): pass


###################################################################################################
#Help and access to function documentation
###################################################################################################
def _getBuiltinFunctions(filter = None):
    ret = []
    for name in globals().keys():
        val = globals()[name]
        if type(val) is PyFunction:
            if filter is None or filter in name:
                #Only "public" documented functions
                if not name.startswith('_') and (val.__doc__ is not None):
                    ret.append(val)
    return to_array(ret)


def _getBuiltinFunctionNames(filter = None):
    ret = []
    for function in _getBuiltinFunctions(filter):
        ret.append(function.func_name)
    return to_array(ret)

def _getFunctionDoc(function):
    if is_string(function):
        if function not in globals():
            return
        function = globals()[function]
    if type(function) is PyFunction and '__doc__' in dir(function):
        ac = function.func_code.co_argcount
        var = function.func_code.co_varnames
        args = list(var)[:ac]
        defs = function.func_defaults
        if defs is not None:
            for i in range (len(defs)):
                index = len(args) - len(defs) + i
                args[index] = args[index] + " = " + str(defs[i])
        flags = function.func_code.co_flags
        if flags & 4 > 0:
            args.append('*' + var[ac])
            ac=ac+1
        if flags & 8 > 0:
            args.append('**' + var[ac])
        d = function.func_doc
        return function.func_name+ "(" + ", ".join(args) + ")" + "\n\n" + (d if (d is not None) else "")

def help(object = None):
    """
    Print help message for function or object (if available).

    Args:
        object (any, optional): function or object to get help.
                    If null prints a list of the builtin functions.

    Returns:
        None
    """
    if object is None:
        print "Built-in functions:"
        for f in _getBuiltinFunctionNames():
            print "\t" + f
    else:
        if type(object) is PyFunction:
            print _getFunctionDoc(object)
        elif '__doc__' in dir(object):
            #The default doc is now shown
            import org.python.core.BuiltinDocs.object_doc
            if object.__doc__ != org.python.core.BuiltinDocs.object_doc:
                print object.__doc__

###################################################################################################
#Variable injection
###################################################################################################

def _get_caller():
    #Not doing inspect.currentframe().f_back because inspect is slow to load
    return sys._getframe(1).f_back if hasattr(sys, "_getframe") else None

def inject():
    """Restore initial globals: re-inject devices and startup variables to the interpreter.

    Args:
        None

    Returns:
        None
    """
    if __name__ == "__main__":
        get_context().injectVars()
    else:
        _get_caller().f_globals.update(get_context().scriptManager.injections)


###################################################################################################
#Script evaluation and return values
###################################################################################################

def run(script_name, args = None, locals = None):
    """Run script: can be absolute path, relative, or short name to be search in the path.
    Args:
        args(Dict or List): Sets sys.argv (if list) or gobal variables(if dict) to the script.
        locals(Dict): If not none sets the locals()for the runing script.
                      If locals is used then script definitions will not go to global namespace.

    Returns:
        The script return value (if set with set_return)
    """
    script = get_context().scriptManager.library.resolveFile(script_name)
    if script is not None and os.path.isfile(script):
        info = get_context().startScriptExecution(script_name, args)
        try:
            set_return(None)
            if args is not None:
                if isinstance(args,list) or isinstance(args,tuple):
                    sys.argv =  list(args)
                    globals()["args"] = sys.argv
                else:
                    for arg in args.keys():
                        globals()[arg] = args[arg]
            if (locals is None):
                execfile(script, globals())
            else:
                execfile(script, globals(), locals)
            ret = get_return()
            get_context().finishScriptExecution(info, ret)
            return ret
        except Exception, ex:
            get_context().finishScriptExecution(info, ex)
            raise ex
    raise IOError("Invalid script: " + str(script_name))

def abort():
    """Abort the execution of ongoing task. It can be called from the script to quit.

    Args:
        None

    Returns:
        None
    """
    fork(get_context().abort) #Cannot be on script execution thread
    while True: sleep(10.0)

def is_aborted():
    """Checks if ongoing task has been aborted.
       In Java>=20 threads cannot be forcibly stopped, so lengthy operations in scripts must check if task has been aborted.

    Args:
        None

    Returns:
        None
    """
    return java.lang.Thread.currentThread().isInterrupted() or get_exec_pars().commandInfo.aborted

def check_aborted():
    """Stops current execution if task has been aborted.
       In Java>=20 threads cannot be forcibly stopped, so lengthy operations in scripts must check if task has been aborted.
         
    Args:
        None

    Returns:
        None
    """
    if is_aborted():
        abort()

def set_return(value):
    """Sets the script return value. This value is returned by the "run" function.

    Args:
        value(Object): script return value.

    Returns:
        None
    """
    #In Jython, the output of last statement is not returned  when running a file
    if __name__ == "__main__":
        global __THREAD_EXEC_RESULT__
        if is_interpreter_thread():
            global _
            _=value
        __THREAD_EXEC_RESULT__[java.lang.Thread.currentThread()]=value         #Used when running file
    else:
        #if startup is imported, cannot set global
        caller = _get_caller()
        if is_interpreter_thread():
            caller.f_globals["_"]=value
        if not "__THREAD_EXEC_RESULT__" in caller.f_globals.keys():
            caller.f_globals["__THREAD_EXEC_RESULT__"] = {}
        caller.f_globals["__THREAD_EXEC_RESULT__"][java.lang.Thread.currentThread()]=value
    return value    #Used when parsing file

def get_return():
    if __name__ == "__main__":
        global __THREAD_EXEC_RESULT__
        return __THREAD_EXEC_RESULT__[java.lang.Thread.currentThread()]
    else:
        return _get_caller().f_globals["__THREAD_EXEC_RESULT__"][java.lang.Thread.currentThread()]


###################################################################################################
#Executed on startup
###################################################################################################

if __name__ == "__main__":
    ca_channel_path=os.path.join(get_context().setup.getStandardLibraryPath(), "epics")
    sys.path.append(ca_channel_path)
    #This is to destroy previous context of _ca (it is not shared with PShell)
    if run_count > 0:
        if sys.modules.has_key("_ca"):
            print
            import _ca
            _ca.initialize()
