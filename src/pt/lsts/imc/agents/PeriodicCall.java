package pt.lsts.imc.agents;

import akka.actor.ActorRef;

public class PeriodicCall implements Runnable {
	
	public String method;
	public long thisTime, nextTime;
	private ActorRef ref; 
	private long period;
	
	public PeriodicCall(ActorRef ref, String method, long period) {
		this.ref = ref;
		this.method = method;
		this.period = period;	
	}
	
	
	public void run() {
		this.thisTime = AgentContext.instance().getTime();
		this.nextTime = thisTime + period;
		ref.tell(this, ActorRef.noSender());
	}
}
