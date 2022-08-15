package ch.psi.pshell.xscan.core;

import ch.psi.pshell.xscan.ProcessorXScan;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes a python script inside a Jython interpreter
 */
public class JythonAction implements Action {

    private static Logger logger = Logger.getLogger(JythonAction.class.getName());

    /**
     * Pattern of the entry function of the jython script
     */
    private static final String entryFunction = "process";
    private static final String entryFunctionPattern = "def " + entryFunction + "\\((.*)\\):";

    private String jythonCall; // entry call to script - including all parameters, etc.
    private String script;

    private final Map<String, Object> globalObjects;

    public JythonAction(String script, int functionId, Map<String, ?> mapping) {
        this(script, functionId, mapping, new HashMap<String, Object>());
    }

    public JythonAction(String script, int functionId, Map<String, ?> mapping, Map<String, Object> globalObjects) {

        this.globalObjects = globalObjects;
        this.script=script;

        // Determine script entry function and the function parameters
        Pattern pattern = Pattern.compile(entryFunctionPattern);
        Matcher matcher = pattern.matcher(script);
        String[] functionParameters = null;
        String uniqueEntryFunction = entryFunction + "_" + functionId; 
        uniqueEntryFunction = uniqueEntryFunction.replaceAll("[^a-zA-Z0-9_]", "_");
            
        if (matcher.find() && matcher.groupCount() == 1) {
            logger.finest("Entry function '" + entryFunctionPattern + "' found - Identified parameters: " + matcher.group(1));         
            jythonCall = uniqueEntryFunction + "(" + matcher.group(1) + ")";
            if (matcher.group(1).matches(" *")) {
                functionParameters = new String[0];
            } else {
                functionParameters = matcher.group(1).split(" *, *");
            }
            
        } else {
            if (mapping.size()>0){
                throw new IllegalArgumentException("Cannot determine entry function: " + entryFunctionPattern);
            }
            logger.finest("No entry function ");
            return;
        }

        // Check whether all function parameters have a mapping
        for (int i = 0; i < functionParameters.length; i++) {
            String p = functionParameters[i];
            p = p.trim();
            boolean found = false;
            for (String pm : mapping.keySet()) {
                if (pm.equals(p)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("No mapping compontent found for parameter " + p);
            }
        }

        // Check whether more mappings are specified than function parameters
        if (functionParameters.length < mapping.size()) {
            throw new IllegalArgumentException("More mappings than function parameters are specified");
        }

         String uniqueScript = this.script.replaceFirst(entryFunction, uniqueEntryFunction);  
        // Load manipulation script
        try {
            ProcessorXScan.eval(uniqueScript);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load action script", e);
        }

        for (String b : mapping.keySet()) {
            // Assign channel bean to variable
            ProcessorXScan.setInterpreterVariable(b, mapping.get(b));
        }
    }

    @Override
    public void execute() throws InterruptedException {

        // Set global objects
        // This block is not in initialization as we want to assure that all invocations
        // of this manipulation will get the same value (i.e. to prevent inconsistent behavior
        // if variable was changed during an execution of the manipulation)
        for (String k : globalObjects.keySet()) {
            ProcessorXScan.setInterpreterVariable(k, globalObjects.get(k));
        }

        try {
            if (jythonCall!=null){
                ProcessorXScan.eval(jythonCall + "\n");
            } else {
                ProcessorXScan.eval(script);
            }
        } catch (Exception e) {
            if ((e.getMessage() != null) && (e.getMessage().startsWith("KeyboardInterrupt"))) {
                throw new InterruptedException();
            }
            throw new RuntimeException("Action failed while executing the script: " + e.getMessage(), e);
        }
    }

}
