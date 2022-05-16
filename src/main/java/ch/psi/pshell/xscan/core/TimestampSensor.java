package ch.psi.pshell.xscan.core;

/**
 * Sensor to read the current time in milliseconds
 */
public class TimestampSensor implements Sensor {

	private final String id; // Global id of the sensor
	
	public TimestampSensor(String id){
		this.id = id;
	}
	
	@Override
	public Object read() {
		// Return current time in milliseconds
		return new Double(System.currentTimeMillis());
	}

	@Override
	public String getId() {
		return id;
	}

}
