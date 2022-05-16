package ch.psi.pshell.xscan.core;

import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class JythonFunction implements Function {
	
	private static final Logger logger = Logger.getLogger(JythonFunction.class.getName());

	public static final String ENTRY_FUNCTION_NAME = "calculate";
	private static final String ENTRY_FUNCTION_PATTERN = "def "+ENTRY_FUNCTION_NAME+"\\((.*)\\):";
	
	private ScriptEngine engine;
	private String additionalParameter = "";
	
	
	public JythonFunction(String script, Map<String, Object> map){
		
		// Workaround for Jython memory leak 
		// http://blog.hillbrecht.de/2009/07/11/jython-memory-leakout-of-memory-problem/
		System.setProperty("python.options.internalTablesImpl","weak");
		
		this.engine = new ScriptEngineManager().getEngineByName("python");
                if (this.engine == null) {
		    logger.severe("Error instantiating script engine");
                    throw new RuntimeException("Error instantiating script engine");
                }                
		
		// Determine script entry function and the function parameters
		String[] parameter;
		Pattern pattern = Pattern.compile(ENTRY_FUNCTION_PATTERN);
		Matcher matcher = pattern.matcher(script);
		if(matcher.find() && matcher.groupCount()==1){
			if(!matcher.group(1).trim().equals("")){
				logger.finest("Entry function '"+ENTRY_FUNCTION_PATTERN+"' found - Identified parameters: "+matcher.group(1));
				parameter = matcher.group(1).split(" *, *");
			}
			else{
				parameter = new String[0];
			}
		}
		else{
			throw new IllegalArgumentException("Cannot determine entry function: "+ENTRY_FUNCTION_PATTERN);
		}
		
		// Check whether all parameters are mapped
		StringBuilder b = new StringBuilder();
		for(int i=1;i<parameter.length; i++){ // Starting at 1 because first argument is implicit.
			if(!map.containsKey(parameter[i])){
				throw new IllegalArgumentException("Function parameter "+parameter[i]+" is not mapped");
			}
			b.append(",");
			b.append(parameter[i]);
		}
		additionalParameter = b.toString();
		
		// Set variables in Jython engine
		for(String k: map.keySet()){
			engine.put(k, map.get(k));
		}
		
		// Load manipulation script
		try {
			engine.eval(script);
		} catch (ScriptException e) {
			throw new RuntimeException("Unable to load manipulation script", e);
		}
	}
	
	@Override
	public double calculate(double parameter) {		
		try {
                    logger.fine("calculate( "+parameter+""+additionalParameter+" )");
                    return ((Double) engine.eval("calculate( "+parameter+""+additionalParameter+" )"));
		} catch (ScriptException e) {
			throw new RuntimeException("Calculating actuator step failed while executing the Jython script",e);
		}
		
	}

}
