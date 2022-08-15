package ch.psi.pshell.xscan.core;

import ch.psi.pshell.xscan.ProcessorXScan;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JythonFunction implements Function {

    private static final Logger logger = Logger.getLogger(JythonFunction.class.getName());

    public static final String ENTRY_FUNCTION_NAME = "calculate";
    private static final String ENTRY_FUNCTION_PATTERN = "def " + ENTRY_FUNCTION_NAME + "\\((.*)\\):";

    private String additionalParameter = "";
    private String uniqueEntryFunction;

    
    public JythonFunction(String script, int functionId, Map<String, Object> map) {   
        
        // Determine script entry function and the function parameters
        String[] parameter;
        Pattern pattern = Pattern.compile(ENTRY_FUNCTION_PATTERN);
        Matcher matcher = pattern.matcher(script);
        if (matcher.find() && matcher.groupCount() == 1) {
            if (!matcher.group(1).trim().equals("")) {
                logger.finest("Entry function '" + ENTRY_FUNCTION_PATTERN + "' found - Identified parameters: " + matcher.group(1));
                parameter = matcher.group(1).split(" *, *");
            } else {
                parameter = new String[0];
            }
        } else {
            throw new IllegalArgumentException("Cannot determine entry function: " + ENTRY_FUNCTION_PATTERN);
        }

        // Check whether all parameters are mapped
        StringBuilder b = new StringBuilder();
        for (int i = 1; i < parameter.length; i++) { // Starting at 1 because first argument is implicit.
            if (!map.containsKey(parameter[i])) {
                throw new IllegalArgumentException("Function parameter " + parameter[i] + " is not mapped");
            }
            b.append(",");
            b.append(parameter[i]);
        }
        additionalParameter = b.toString();

        // Set variables in Jython engine
        for (String k : map.keySet()) {
            ProcessorXScan.setInterpreterVariable(k, map.get(k));
        }
        
        uniqueEntryFunction = ENTRY_FUNCTION_NAME + "_" + functionId; 
        uniqueEntryFunction = uniqueEntryFunction.replaceAll("[^a-zA-Z0-9_]", "_");
        String uniqueScript  = script.replaceFirst(ENTRY_FUNCTION_NAME, uniqueEntryFunction);             
        // Load manipulation script
        try {
            ProcessorXScan.eval(uniqueScript);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load function script", e); 
        }
    }
 

    @Override
    public double calculate(double parameter) {
        try {
            logger.fine("calculate( " + parameter + "" + additionalParameter + " )");
            return ((Double) ProcessorXScan.eval(uniqueEntryFunction + "( " + parameter + "" + additionalParameter + " )"));
        } catch (Exception e) {
            throw new RuntimeException("Calculating actuator step failed while executing the script: " + e.getMessage(), e);
        }

    }

}
