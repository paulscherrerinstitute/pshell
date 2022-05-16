package ch.psi.pshell.xscan.core;


/**
 * Mapping of a script parameter to a component via the Id.
 */
public class JythonParameterMappingGlobalVariable extends JythonParameterMapping {
	
	private JythonGlobalVariable globalVariable;
		
	/**
	 * Constuctor
	 * @param variable
	 * @param globalVariable
	 */
	public JythonParameterMappingGlobalVariable(String variable, JythonGlobalVariable globalVariable){
		super(variable);
		this.globalVariable = globalVariable;
	}

	/**
	 * @return the globalVariable
	 */
	public JythonGlobalVariable getGlobalVariable() {
		return globalVariable;
	}

	/**
	 * @param globalVariable the globalVariable to set
	 */
	public void setGlobalVariable(JythonGlobalVariable globalVariable) {
		this.globalVariable = globalVariable;
	}
	
}
