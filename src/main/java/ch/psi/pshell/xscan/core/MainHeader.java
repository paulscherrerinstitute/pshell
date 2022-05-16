package ch.psi.pshell.xscan.core;

/**
 * Header of the ZMQ message
 */
public class MainHeader {
	private String htype;
	private int elements;
	
	public void setElements(int elements) {
		this.elements = elements;
	}
	public int getElements() {
		return elements;
	}
	
	public void setHtype(String htype) {
		this.htype = htype;
	}
	public String getHtype() {
		return htype;
	}
}
