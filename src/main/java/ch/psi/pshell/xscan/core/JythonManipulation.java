package ch.psi.pshell.xscan.core;

import ch.psi.pshell.xscan.DataMessage;
import ch.psi.pshell.xscan.Metadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JythonManipulation implements Manipulation {

    private static Logger logger = Logger.getLogger(JythonManipulation.class.getName());

    /**
     * Pattern of the entry function of the jython script
     */
    private static final String entryFunction = "process";
    private static final String entryFunctionPattern = "def " + entryFunction + "\\((.*)\\):";

    /**
     * Code of the manipulation. The script need to implement a function of the type:      <code>
	 * def process():
     *     # ... your code
     *     return value
     * </code>
     *
     * The number of parameters of the script are not limited. So <code>process(a)</code> is valid as well as
     * <code>process(a,b,c,d,e,f)</code>. However for each parameter there need to be a mapping inside the mapping list!
     * If there is no mapping inside the Manipulator evaluating this Manipulation will throw an Exception.
     */
    private final String script;

    /**
     * Id of the resulting component
     */
    private final String id;

    /**
     * Mapping of components to manipulation code variables
     */
    private final List<JythonParameterMapping> mapping;

    private final boolean returnArray;

    /**
     * Script engine of the manipulator
     */
    private ScriptEngine engine;
    /**
     * Component index of the script parameter. The sequence of the indexes in this array correspond to the script
     * parameter position, i.e. the first index corresponds to the first parameter.
     */
//	private Integer[] parameterIndex;
    private List<String> parameterIds = new ArrayList<>();
    /**
     * Parameter array of the entry function
     */
    private String[] parameter;

    /**
     * Jython entry call
     */
    private String jythonCall;

    private Map<String, Object> gvariables = new HashMap<String, Object>();

    public JythonManipulation(String id, String script, List<JythonParameterMapping> mapping) {
        this(id, script, mapping, false);
    }

    /**
     * Constructor
     *
     * @param id
     * @param script
     * @param mapping
     */
    public JythonManipulation(String id, String script, List<JythonParameterMapping> mapping, boolean returnArray) {
        this.id = id;
        this.script = script;
        this.mapping = mapping;
        this.returnArray = returnArray;
    }

    /**
     * @return the script
     */
    public String getScript() {
        return script;
    }

    /**
     * @return the mapping
     */
    public List<JythonParameterMapping> getMapping() {
        return mapping;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void initialize(List<Metadata> metadata) {

        // Workaround for Jython memory leak 
        // http://blog.hillbrecht.de/2009/07/11/jython-memory-leakout-of-memory-problem/
        System.setProperty("python.options.internalTablesImpl", "weak");

        // Create new script engine
        this.engine = new ScriptEngineManager().getEngineByName("python");
        if (this.engine == null) {
            logger.severe("Error instantiating script engine");
            throw new RuntimeException("Error instantiating script engine");
        }

        // Determine script entry function and the function parameters
        Pattern pattern = Pattern.compile(entryFunctionPattern);
        Matcher matcher = pattern.matcher(this.script);
        if (matcher.find() && matcher.groupCount() == 1) {
            if (!matcher.group(1).trim().equals("")) {
                logger.finest("Entry function '" + entryFunctionPattern + "' found - Identified parameters: " + matcher.group(1));
                parameter = matcher.group(1).split(" *, *");
            } else {
                parameter = new String[0];
            }
        } else {
            throw new IllegalArgumentException("Cannot determine entry function: " + entryFunctionPattern);
        }

        // Load manipulation script
        try {
            engine.eval(this.script);
        } catch (ScriptException e) {
            throw new RuntimeException("Unable to load manipulation script", e);
        }

        // Determine component index of the needed parameters
        // If the component index of the parameter cannot be determined an IllegalArgumentException is thrown
//		parameterIndex = new Integer[parameter.length];
        parameterIds = new ArrayList<>();
        for (int i = 0; i < parameter.length; i++) {
            String p = parameter[i];
            p = p.trim();
            boolean found = false;

            for (JythonParameterMapping jpm : this.mapping) {
                if (jpm.getVariable().equals(p)) {
                    if (jpm instanceof JythonParameterMappingID) {
                        JythonParameterMappingID pm = (JythonParameterMappingID) jpm;
                        // Mapping for parameter found, determine index of the corresponding component
//						parameterIndex[i] = metadata.getIndex(pm.getRefid());
                        parameterIds.add(pm.getRefid());
                    } else if (jpm instanceof JythonParameterMappingChannel) {
                        JythonParameterMappingChannel<?> pm = (JythonParameterMappingChannel<?>) jpm;
//						parameterIndex[i] = null;
                        parameterIds.add(null);
                        engine.put(pm.getVariable(), pm.getChannel());
                    } else if (jpm instanceof JythonParameterMappingGlobalVariable) {
                        JythonParameterMappingGlobalVariable pm = (JythonParameterMappingGlobalVariable) jpm;
                        parameterIds.add(null);
//						parameterIndex[i] = null;

                        engine.put(pm.getVariable(), pm.getGlobalVariable());
                    }
                    found = true;
                    break;
                }
            }
            // If there is no mapping nothing can be found
            // If there are mappings everything need to be found
            if (!found) {
                throw new IllegalArgumentException("No mapping compontent found for parameter " + p);
            }
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(entryFunction);
        buffer.append("(");

        for (String p : parameter) {
            buffer.append(p);
            buffer.append(",");
        }
        if (parameter.length > 0) {
            buffer.setCharAt(buffer.length() - 1, ')');
        } else {
            buffer.append(")");
        }

        jythonCall = buffer.toString();
    }

    @Override
    public Object execute(DataMessage message) {

        // Set global variables - WORKAROUND gvariables
        // This block is not in initialization as we want to assure that all invocations
        // of this manipulation will get the same value (i.e. to prevent inconsistent behaviour
        // if variable was changed during an execution of the manipulation)
        for (String k : gvariables.keySet()) {
            engine.put(k, gvariables.get(k));
        }

        // Manipulate data
        for (int i = 0; i < parameterIds.size(); i++) {
            if (parameterIds.get(i) != null) {
                engine.put(parameter[i], message.getData(parameterIds.get(i)));
            }
        }

        try {
            if (returnArray) {
                return ((double[]) engine.eval(jythonCall));
            } else {
                // Due to the typeless nature of Python an Integer of Float/Double might be returned from the 
                // Python code (e.g. depending on the factors in a calculation)
                // Always return the return value to a double. If this cannot be done return NaN
                Object r = engine.eval(jythonCall);
                if (r instanceof Double) {
                    return ((Double) r);
                } else if (r instanceof Integer) {
                    return (((Integer) r).doubleValue());
                } else {
//					return Double.NaN;
                    return r;
                }
            }
        } catch (ScriptException e) {
//			throw new RuntimeException("Data manipulaton [id: "+id+"] failed while executing the manipulation script",e);
            logger.log(Level.WARNING, "Data manipulaton [id: " + id + "] failed: " + e.getMessage());
            return Double.NaN;
        }

    }

    /**
     * Workaround to put variables into the jython engine.
     *
     * @param name
     * @param value
     */
    public void setVariable(String name, Object value) {
        gvariables.put(name, value);
    }
}
