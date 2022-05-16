package ch.psi.pshell.xscan.core;


/**
 * Mapping of a script parameter to a component via the Id.
 */
public class JythonParameterMappingID extends JythonParameterMapping {
	
	/**
	 * Id of the component to map to this variable
	 */
	private String refid;
		
	/**
	 * Constructor accepting varible/id pair
	 * @param variable
	 * @param refid
	 */
	public JythonParameterMappingID(String variable, String refid){
		super(variable);
		this.refid = refid;
	}
	
	/**
	 * @return the refid
	 */
	public String getRefid() {
		return refid;
	}
	/**
	 * @param refid the refid to set
	 */
	public void setRefid(String refid) {
		this.refid = refid;
	}
	
}
