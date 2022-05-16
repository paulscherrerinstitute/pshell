package ch.psi.pshell.xscan.core;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
/**
 * Complex sensor that complements an other sensor with pre and post actions.
 * Before reading out the complemented sensor the pre actions are executed. After the
 * readout the post actions.
 */
public class ComplexSensor implements Sensor {

	private static Logger logger = Logger.getLogger(ComplexSensor.class.getName());
	
	private String id;
	private final Sensor sensor;
	private final List<Action> preActions;
	private final List<Action> postActions;

	public ComplexSensor(String id, Sensor sensor){
		this.id = id;
		this.sensor = sensor;
		this.preActions = new ArrayList<Action>();
		this.postActions = new ArrayList<Action>();
	}

	@Override
	public Object read() throws InterruptedException {
		logger.finest("Execute pre actions");
		for(Action action: preActions){
			action.execute();
		}
		
		Object value = sensor.read();
		
		logger.finest("Execute post actions");
		for(Action action: postActions){
			action.execute();
		}
		
		return value;
	}

	@Override
	public String getId() {
		return id;
	}

	public List<Action> getPreActions() {
		return preActions;
	}
	public List<Action> getPostActions() {
		return postActions;
	}
}