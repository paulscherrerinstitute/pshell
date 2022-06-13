package ch.psi.pshell.xscan;

/**
 * Message that is send at the end of the action loop inside an ActionLoop implementation
 * of just to indicate that a particular stream has finished
 */
public class EndOfStreamMessage extends ControlMessage {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Intersect flag - flag to indicate that stream should be intersected
	 * after this message.
	 */
	private final boolean iflag;
	
	public EndOfStreamMessage(){
		this(false);
	}
	
	public EndOfStreamMessage(boolean iflag){
		this.iflag = iflag;
	}
	
	public boolean isIflag(){
		return(iflag);
	}

	@Override
	public String toString() {
		return "Message[ c message: end of stream ]";
	}
}
