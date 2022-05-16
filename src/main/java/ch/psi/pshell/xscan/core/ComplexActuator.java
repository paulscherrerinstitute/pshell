package ch.psi.pshell.xscan.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Complex actuator that consists of a list of other actuators. The steps of this actor are composed out of the
 * steps of the actors that this actuator consists of.
 * First all the steps of the first actor are used, after that the steps of the next actor are done.
 * Before the first step of an actor pre actions are available and after the last step of an actor a post action is available.
 */
public class ComplexActuator implements Actor {

	private static Logger logger = Logger.getLogger(ComplexActuator.class.getName());
	
	/**
	 * List of actors this actor is made of
	 */
	private final List<Actor> actors;
	
	/**
	 * Actions that are executed directly before the first step of this actor
	 */
	private final List<Action> preActions;
	
	/**
	 * Actions that are executed directly after the last step of this actor
	 */
	private final List<Action> postActions;
	
	/**
	 * Flag that indicates whether there is a next set value for the Actor
	 */
	private boolean next;
	
	/**
	 * Index of the actor currently used
	 */
	private int actualActorCount;
	
	/**
	 * Actor currently used to perform steps
	 */
	private Actor actualActor;
	
	/**
	 * Flag to indicate the first set() execution of this actor.
	 * This flag is used to decide whether to execute the pre actions.
	 */
	private boolean firstrun;
	
	public ComplexActuator(){
		this.actors = new ArrayList<Actor>();
		this.preActions = new ArrayList<Action>();
		this.postActions = new ArrayList<Action>();
		
		this.next = false;
		this.actualActorCount = 0;
		
		this.firstrun = true;
	}
	
	@Override
	public void set() throws InterruptedException {
		if(!next){
			throw new IllegalStateException("The actuator does not have any next step.");
		}
		
		// PRE actions
		if(firstrun){
			this.firstrun = false;
			
			// Execute pre actions
			logger.finest("Execute pre actions");
			for(Action action: preActions){
				action.execute();
			}
		}
		
		actualActor.set(); // If the actor has no next step then something is wrong in the init/next step logic
		
		// If the last point of the actual actuator is set take the next actuator in the list if there is one available
		while(!actualActor.hasNext()&&actualActorCount<actors.size()){
			actualActorCount++;
			if(actualActorCount<actors.size()){
				actualActor = actors.get(actualActorCount);
			}
			else{
				break;
			}
		}
		
		if(actualActor.hasNext()){
			this.next = true;
		}
		else{
			this.next = false;
			
			// Execute post actions
			logger.finest("Execute post actions");
			for(Action action: postActions){
				action.execute();
			}
		}
		
	}

	@Override
	public boolean hasNext() {
		return next;
	}

	@Override
	public void init() {
		// Initialize all actors this actor consist of
		for(Actor a: actors){
			a.init();
		}
		
		// Set the actual actor to the first actor in the list
		actualActorCount=0;
		if(actualActorCount<actors.size()){
			actualActor = actors.get(actualActorCount);
			
			// Check whether the actor has a next step, if not increase to the next one
			// This is needed because the first actor might not have any step ... (very unlikely but bad things happens some time)
			while(!actualActor.hasNext()&&actualActorCount<actors.size()){
				actualActorCount++;
				if(actualActorCount<actors.size()){
					actualActor = actors.get(actualActorCount);
				}
				else{
					break;
				}
			}
			
			if(actualActor.hasNext()){
				this.next = true;
			}
			else{
				this.next = false;
			}
			
		}
		else{
			actualActor=null;
			this.next=false;
		}
		
		this.firstrun = true;
	}

	@Override
	public void reverse() {
		// Reverse all actors this actor consist of
		for(Actor a: actors){
			a.reverse();
		}
	}
	
	@Override
	public void reset() {
		// Reset all actors this actor consist of
		for(Actor a: actors){
			a.reset();
		}
	}
	
	public List<Action> getPreActions() {
		return preActions;
	}
	public List<Actor> getActors() {
		return actors;
	}
	public List<Action> getPostActions() {
		return postActions;
	}
}
