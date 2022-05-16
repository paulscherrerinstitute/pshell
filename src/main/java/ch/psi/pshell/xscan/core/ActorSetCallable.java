package ch.psi.pshell.xscan.core;

import java.util.concurrent.Callable;

/**
 * Callable used for parallel execution of the set method of an actor
 */
public class ActorSetCallable implements Callable<Object> {

	private Actor actor;
	
	public ActorSetCallable(Actor actor){
		this.actor = actor;
	}
	
	@Override
	public Object call() throws Exception {
		actor.set();
		return null;
	}
}
