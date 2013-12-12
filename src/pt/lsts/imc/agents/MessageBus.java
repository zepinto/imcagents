package pt.lsts.imc.agents;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.ini4j.Ini;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MessageBus extends UntypedActor {

	private LinkedHashMap<ActorRef, Channel> channels = new LinkedHashMap<>();
	private boolean sendToSelf = false;

	public MessageBus(File config) throws Exception {
		Ini ini = new Ini(config);

		for (Ini.Section section : ini.values()) {

			String name = section.getName();
			Class<?> c;
			try {
				c = Class.forName(name);
			}
			catch (Exception e) {
				c = Class.forName("pt.lsts.imc.agents."+name);
			}
			Properties props = new Properties();

			for (String option : section.keySet())
				props.put(option, section.fetch(option));

			bootstrap(c, props);
		}			
	}

	public ActorRef bootstrap(Class<?> c, Properties properties) {
		ActorRef ref = getContext().actorOf(Props.create(c));
		ref.tell(properties, getSelf());
		Channel chan = new Channel(c);
		Map<String, Integer> periodicCalls = chan.periodicCalls();
		channels.put(ref, chan);
		for (Entry<String, Integer> entry : periodicCalls.entrySet()) {
			PeriodicCall call = new PeriodicCall(ref, entry.getKey(), entry.getValue());
			getContext().system().scheduler().schedule(
					Duration.create(entry.getValue(), TimeUnit.MILLISECONDS),
					Duration.create(entry.getValue(), TimeUnit.MILLISECONDS), call, getContext().system().dispatcher());
		}
		return ref;
	}


	@Override
	public void onReceive(Object arg0) throws Exception {

		// if the sender is not a children of this bus, ignore it
		if (!channels.containsKey(getSender()))
			throw new Exception("Sender not recognized: "+getSender());
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

	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create();
		system.shutdown();
		//		system.actorOf(Props.create(MessageBus.class, new File("/home/zp/Desktop/agents.props")));
		//		Thread.sleep(1000);
		//		//system.shutdown();
	}
}
