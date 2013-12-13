package pt.lsts.imc.agents;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class MessageBus extends UntypedActor {

	private LinkedHashMap<ActorRef, Channel> channels = new LinkedHashMap<>();
	private boolean sendToSelf = false;
	
	@Override
	public void onReceive(Object arg0) throws Exception {

		// if the sender is unknown, it is either a new one or invalid
		if (!channels.containsKey(getSender())) {
			if (arg0.getClass().equals(Channel.class)) {
				channels.put(getSender(), (Channel)arg0);
				return;
			}
			else
				throw new Exception("Sender not recognized: "+getSender());
		}
			
		// if the sender does not advert this type of message, block sending
		if (!channels.get(getSender()).allowedToSend(arg0)) {
			throw new Exception("Sender not allowed to send "+arg0.getClass());
		}

		// Send this message to all children that are listening to this event type
		for (Entry<ActorRef, Channel> entry : channels.entrySet()) {
			if (entry.getValue().accepts(arg0)) {
				// if sendToSelf is not set, skip sender of the event
				if (entry.getKey().equals(getSender()) && !sendToSelf)
					continue;
				entry.getKey().tell(arg0, ActorRef.noSender());
			}
		}
	}
}
