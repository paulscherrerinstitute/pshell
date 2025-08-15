/////////////////////////////////////////////////////////////////////////////////////////////////// 
//  Facade to JEP: Embedded Python
/////////////////////////////////////////////////////////////////////////////////////////////////// 

//Matplotlib won't work out of the box because it's default backend (Qt) uses signals, which only works in
//the main thread. Ideally should find a fix, in order to mark the running thread as the main.
//As  a workaround, one can use the Tk backend:
//
//import matplotlib
//matplotlib.use('TkAgg') 


importClass(java.io.File)
importClass(java.lang.Thread)

Jep = Java.type('jep.Jep')
NDArray = Java.type('jep.NDArray')


__jep = {}

function __get_jep(){
    t = java.lang.Thread.currentThread()
    if (!(t in __jep)){
        init_jep()
    }
    return __jep[t]
}

function __close_jep(){
    t = java.lang.Thread.currentThread()
    if (t in __jep){
        __jep[t].close()
    }
}        

function init_jep(){ 
    //TODO: Should do it but generates errors
    //__close_jep()
    j = new Jep(false)
    //Faster, but statements must be complete
    j.setInteractive(false) 
    __jep[java.lang.Thread.currentThread()] = j
    j.eval("import sys")
    //sys.argv is not present in JEP and may be needed for certain modules (as Tkinter)
    j.eval("sys.argv = ['PShell']");
    //Add standard script path to python path
    j.eval("sys.path.append('" + get_context().setup.getScriptPath() + "')")
    
    //Redirect stdout
    j.eval("class JepStdout:\n" + 
           "    def write(self, str):\n" +
           "        self.str += str\n" + 
           "    def clear(self):\n" +
           "        self.str = ''\n" +
           "    def flush(self):\n" +
           "        pass\n")
    j.eval("sys.stdout=JepStdout()") 
    j.eval("sys.stderr=JepStdout()")    
    j.eval("sys.stdout.clear()")
    j.eval("sys.stderr.clear()")
}    

function __print_stdout(){
    j=__get_jep()
    output = j.getValue("sys.stdout.str")
    err = j.getValue("sys.stderr.str")
    j.eval("sys.stdout.clear()")
    j.eval("sys.stderr.clear()")
    if ((output != null) && (output.length>0)){
        print (output)
    }
    if ((err != null) && (err.length>0)){
        java.lang.System.err.println(err)
    }
}        
        
function run_jep(script_name, vars){ 
	if (!script_name.toLowerCase().endsWith(".py")){
		script_name += ".py"
	}
	if (!is_defined(vars)) {
		vars = {};
	}	
    script = get_context().scriptManager.library.resolveFile(script_name)
    if (script == null){    	
        script= new File(script_name).getAbsolutePath()
    }
    j=__get_jep()
            
    for (var v in vars){
        j.set(v, vars[v])
    }
    try{
        j.runScript(script)
    } finally {
        __print_stdout()
    }
}        

function eval_jep(line){ 
    j=__get_jep()
    try{
        j.eval(line)
    } finally {
        __print_stdout()
    }
}        

function set_jep(v, value){ 
    j=__get_jep()
    j.set(v, value)
}    

function get_jep(v){
    j=__get_jep()  
    return j.getValue(v)
}    

function call_jep(module, func, args){
	if (!is_defined(args)) {
		args = [];
	}	
	
    j=__get_jep()
    f = module+"_" + func +"_"+ j.hashCode()
    try{
        eval_jep("from " + module + " import " + func + " as " + f)    
        ret = j.invoke(f, to_array(args,'o'))
    } finally {
        __print_stdout()
    }
    return ret
}    

function to_npa(data, dimensions, type){
	if (!is_defined(dimensions)) {
		dimensions = null;
	}
    if (!is_defined(type)) {
    	type='d'
    }	
    data = to_array(data, type)
    return new NDArray(data, dimensions)    
}


    