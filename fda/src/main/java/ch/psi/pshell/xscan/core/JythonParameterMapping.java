package ch.psi.pshell.xscan.core;

/**
 * Mapping of a parameter to something
 */
public abstract class JythonParameterMapping {

    /**
     * Variable name inside the script
     */
    private String variable;

    /**
     * Constructor
     *
     * @param variable
     */
    public JythonParameterMapping(String variable) {
        this.variable = variable;
    }

    /**
     * @return the variable
     */
    public String getVariable() {
        return variable;
    }

    /**
     * @param variable the variable to set
     */
    public void setVariable(String variable) {
        this.variable = variable;
    }

}
