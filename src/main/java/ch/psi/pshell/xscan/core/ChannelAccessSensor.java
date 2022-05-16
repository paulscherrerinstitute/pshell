package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import ch.psi.jcae.ChannelException;
import ch.psi.utils.Str;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Channel Access sensor capable of reading a channel access channel
 */
public class ChannelAccessSensor<T> implements Sensor {

	private static Logger logger = Logger.getLogger(ChannelAccessSensor.class.getName());
	
	private Channel<T> channel;
	private final String id;
	private boolean failOnSensorError = true;
	
	public ChannelAccessSensor(String id, Channel<T> channel){
		this.id = id;
		this.channel = channel;
	}
	
	public ChannelAccessSensor(String id, Channel<T> channel, boolean failOnSensorError){
		this.id = id;
		this.channel = channel;
		this.failOnSensorError = failOnSensorError;
	}
	
	@Override
	public Object read() throws InterruptedException {				
		T v;
		try {
			v = channel.getValue();
                        if (logger.isLoggable(Level.FINEST)){
                            logger.finest("Read channel ["+channel.getName()+"]: " + Str.toString(v, 10));
                        }                                                
		} catch (TimeoutException | ChannelException | ExecutionException e) {
                        String errMsg = "Unable to get value from channel ["+channel.getName()+"]";
                        logger.fine(errMsg);
			if(failOnSensorError){
				throw new RuntimeException(errMsg,e);
			}
			v = null;
		} 
		return(v);
	}

	@Override
	public String getId() {
		return id;
	}
}
