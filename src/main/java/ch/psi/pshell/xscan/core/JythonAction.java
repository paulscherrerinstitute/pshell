package ch.psi.pshell.xscan.core;


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Executes a python script inside a Jython interpreter
 */
public class JythonAction implements Action {

	private static Logger logger = Logger.getLogger(JythonAction.class.getName());
	
	/**
	 * Pattern of the entry function of the jython script
	 */
	private static final String entryFunction = "process";
	private static final String entryFunctionPattern = "def "+entryFunction+"\\((.*)\\):";
	
	private ScriptEngine engine;
	
	private String jythonCall; // entry call to script - including all parameters, etc.
	
	private final Map<String,Object> globalObjects;
	
	public JythonAction(String script, Map<String, ?> mapping){
		this(script, mapping, new HashMap<String,Object>());
	}
	
	public JythonAction(String script, Map<String, ?> mapping, Map<String,Object> globalObjects){
		
		this.globalObjects = globalObjects;
		
		// Workaround for Jython memory leak 
		// http://blog.hillbrecht.de/2009/07/11/jython-memory-leakout-of-memory-problem/
		System.setProperty("python.options.internalTablesImpl","weak");
		
		// Create new script engine
		this.engine = new ScriptEngineManager().getEngineByName("python");
                if (this.engine == null) {
		    logger.severe("Error instantiating script engine");
                    throw new RuntimeException("Error instantiating script engine");
                }
                
		
		// Determine script entry function and the function parameters
		Pattern pattern = Pattern.compile(entryFunctionPattern);
		Matcher matcher = pattern.matcher(script);
		String[] functionParameters = null;
		if(matcher.find() && matcher.groupCount()==1){
			logger.finest("Entry function '"+entryFunctionPattern+"' found - Identified parameters: "+matcher.group(1));
			jythonCall = entryFunction+"("+matcher.group(1)+")";
			if(matcher.group(1).matches(" *")){
				functionParameters = new String[0];
			}
			else{
				functionParameters = matcher.group(1).split(" *, *");
			}
		}
		else{
			throw new IllegalArgumentException("Cannot determine entry function: "+entryFunctionPattern);
		}
		
		// Check whether all function parameters have a mapping
		for(int i=0;i<functionParameters.length; i++){
			String p = functionParameters[i];
			p = p.trim();
			boolean found = false;
			for(String pm: mapping.keySet()){
				if(pm.equals(p)){
					found=true;
					break;
				}
			}
			if(!found){
				throw new IllegalArgumentException("No mapping compontent found for parameter "+p);
			}
		}
		
		// Check whether more mappings are specified than function parameters
		if(functionParameters.length<mapping.size()){
			throw new IllegalArgumentException("More mappings than function parameters are specified");
		}
		
		// Load manipulation script
		try {
			engine.eval(script);
		} catch (ScriptException e) {
			throw new RuntimeException("Unable to load manipulation script", e);
		}
		
		for(String b: mapping.keySet()){
			// Assign channel bean to variable
			engine.put(b, mapping.get(b));
		}
	}
	
	@Override
	public void execute() throws InterruptedException {
		
		// Set global objects
		// This block is not in initialization as we want to assure that all invocations
		// of this manipulation will get the same value (i.e. to prevent inconsistent behavior
		// if variable was changed during an execution of the manipulation)
		for(String k: globalObjects.keySet()){
			engine.put(k, globalObjects.get(k));
		}
		
		try {
			engine.eval(jythonCall+"\n");
		} catch (ScriptException e) {
                    if ((e.getMessage() !=null) && (e.getMessage().startsWith("KeyboardInterrupt"))){
                        throw new InterruptedException();
                    }
                    throw new RuntimeException("Action failed while executing the Jython script",e);
		}		
	}

}
