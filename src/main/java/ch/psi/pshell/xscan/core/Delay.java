package ch.psi.pshell.xscan.core;

/**
 * Wait a specific time until executing the next action ...
 */
public class Delay implements Action {

	private final long time;
	
	/**
	 * @param time		Time to wait in milliseconds
	 */
	public Delay(long time){
		
		// Check if delay time is positive and >0
		if(time<=0){
			throw new IllegalArgumentException("Wait time must be >0");
		}
		
		this.time = time;
	}
	
	@Override
	public void execute() throws InterruptedException {
		Thread.sleep(time);
	}

}
