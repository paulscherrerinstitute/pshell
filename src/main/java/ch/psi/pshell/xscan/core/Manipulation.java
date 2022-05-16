package ch.psi.pshell.xscan.core;

import ch.psi.pshell.xscan.DataMessage;
import ch.psi.pshell.xscan.Metadata;
import java.util.List;

public interface Manipulation {

	/**
	 * Get the id of the manipulation
	 * @return	id of manipulation
	 */
	public String getId();
	
	/**
	 * Initialize the manipulation
	 * @param metadata	Metadata of the incomming data message
	 */
	public void initialize(List<Metadata> metadata);
	
	/**
	 * Execute the manipulation on the passed data message
	 * @param message Message to manipulate
	 * @return	Result of the manipulation
	 */
	public Object execute(DataMessage message);
}
