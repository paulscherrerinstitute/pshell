package ch.psi.pshell.xscan;

import java.io.Serializable;

/**
 * Metadata of a component of a message. Each component has a global id.
 * Optionally the component can also belong to a dimension. However, depending on the 
 * view the number of the dimension might vary. Therefore the dimension number
 * might change during the lifetime of a message (component).
 */
public class Metadata implements Serializable{

	private static final long serialVersionUID = 1L;

	private final String id;
	private int dimension;
	
	public Metadata(String id){
		this.id = id;
		this.dimension = 0;
	}
	
	public Metadata(String id, int dimension){
		this.id = id;
		this.dimension = dimension;
	}

	public void setDimension(int dimension){
		this.dimension = dimension;
	}
	
	public int getDimension() {
		return dimension;
	}

	public String getId() {
		return id;
	}
}
