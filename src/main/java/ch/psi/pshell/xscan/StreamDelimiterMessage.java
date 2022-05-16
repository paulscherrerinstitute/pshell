package ch.psi.pshell.xscan;

/**
 * Message that is send at the end of the action loop inside an ActionLoop implementation
 */
public class StreamDelimiterMessage extends ControlMessage{
	private static final long serialVersionUID = 1L;
	/**
	 * Number of the dimension this delimiter belongs to.
	 */
	private final int number;
	
	/**
	 * Intersect flag - flag to indicate that stream should be intersected
	 * after this message.
	 */
	private final boolean iflag;
	
	/**
	 * @param number	Number of the dimension this delimiter belongs to
	 */
	public StreamDelimiterMessage(int number){
		this(number, false);
	}
	
	/**
	 * @param number
	 * @param iflag		Flag to indicate that data is grouped
	 */
	public StreamDelimiterMessage(int number, boolean iflag){
		this.number = number;
		this.iflag = iflag;
	}

	public int getNumber() {
		return number;
	}
	
	public boolean isIflag(){
		return iflag;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Message [ c message: delimiter dimension "+number+" ]";
	}
}
