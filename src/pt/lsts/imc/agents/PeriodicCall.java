package pt.lsts.imc.agents;

import pt.lsts.imc.annotations.Periodic;
import akka.actor.ActorRef;

/**
 * Objects of this class are generated for every method marked with the
 * {@link Periodic} annotation. It is used to call the given method in a timely
 * manner.
 * 
 * @author zp
 *
 */
public class PeriodicCall implements Runnable {

	public String method;
	public long thisTime, nextTime;
	private ActorRef ref;
	private long period;

	/**
	 * Create an instance by passing the actor, the method and interval between
	 * calls
	 * 
	 * @param ref
	 *            The actor reference (akka)
	 * @param method
	 *            The name of the method to be called on the actor
	 * @param period
	 *            The interval, in milliseconds between periodic calls
	 */
	public PeriodicCall(ActorRef ref, String method, long period) {
		this.ref = ref;
		this.method = method;
		this.period = period;
	}

	/**
	 * Whenever the periodic call is fired, a message with this object is sent
	 * to the actor. 
	 */
	public void run() {
		this.thisTime = AgentContext.instance().getTime();
		this.nextTime = thisTime + period;
		ref.tell(this, ActorRef.noSender());
	}
}
