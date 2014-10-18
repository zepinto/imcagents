package pt.lsts.imc.agents;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.ini4j.Ini;

import pt.lsts.imc.agents.clock.Clock;
import pt.lsts.imc.agents.clock.RTClock;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * This class defines a context where several agents can run and communicate
 * locally. A context provides a message bus for agents to communicate and a
 * clock implementation that can be used by agents running inside for keeping
 * time. A context can created from a configuration file specifying a number of
 * agents and their initial states. Only a single AgentContext is allowed to run
 * per JVM instance (Singleton).
 * 
 * @author zp
 *
 */
public class AgentContext {

	private int uid = -1;

	private LinkedHashMap<Class<?>, List<ActorRef>> actorsByClass = new LinkedHashMap<Class<?>, List<ActorRef>>();

	private ActorRef bus;
	private ActorSystem system;

	// real time is the default clock
	private Clock clock = new RTClock();
	private Vector<ActorRef> actors = new Vector<>();

	/**
	 * This method is used to retrieve the locally unique entity id of a given
	 * actor
	 * 
	 * @param actor
	 *            An actor reference
	 * @return The entity id (0-255) of this actor
	 */
	public int entityOf(ActorRef actor) {
		if (!actors.contains(actor))
			actors.add(actor);

		return actors.indexOf(actor) + 1;
	}

	// Singleton
	private static AgentContext instance = null;

	private AgentContext(File config) {
		instance = this;
		if (config == null)
			return;

		try {
			parseConfig(config);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Singleton accessor
	 * 
	 * @return The singleton instance
	 */
	public static AgentContext instance() {
		return instance;
	}

	/**
	 * Given an INI file with a configuration, parses the configuration and
	 * creates required agents.
	 * 
	 * @param config
	 *            A configuration (ini) file
	 * @throws Exception
	 *             In case there is a problem with the file.
	 */
	public void parseConfig(File config) throws Exception {
		Ini ini = new Ini(config);
		this.system = ActorSystem.create("IMCAgents");
		this.bus = system.actorOf(Props.create(MessageBus.class));

		Vector<ActorRef> actors = new Vector<ActorRef>();

		for (Ini.Section section : ini.values()) {
			String name = section.getName();
			Properties props = new Properties();
			for (String option : section.keySet())
				props.put(option, section.fetch(option));
			if (!name.equals("AgentContext")) {
				Class<?> c;
				try {
					c = Class.forName(name);
				} catch (Exception e) {
					c = Class.forName("pt.lsts.imc.agents." + name);
				}

				actors.add(bootstrap(c, props));
			}
		}

		for (ActorRef actor : actors)
			actor.tell("init", bus);
	}

	/**
	 * Instantiates and initializes an actor by using Java reflection
	 * 
	 * @param c
	 *            The class of the agent to be created
	 * @param properties
	 *            The initial state of the agent
	 * @return An actor reference to the newly created agent
	 */
	public ActorRef bootstrap(Class<?> c, Properties properties) {
		// Create actor with default initial state
		ActorRef ref = system.actorOf(Props.create(c));

		if (!actorsByClass.containsKey(c))
			actorsByClass.put(c, new ArrayList<ActorRef>());

		actorsByClass.get(c).add(ref);

		// Create an interface for the actor (using the annotations)
		AgentInterface chan = new AgentInterface(c);

		bus.tell(chan, ref);
		ref.tell(properties, bus);

		Map<String, Integer> periodicCalls = chan.periodicCalls();
		for (Entry<String, Integer> entry : periodicCalls.entrySet()) {
			PeriodicCall call = new PeriodicCall(ref, entry.getKey(),
					entry.getValue());
			system.scheduler().schedule(
					Duration.create(clock.getDuration(entry.getValue()),
							TimeUnit.MILLISECONDS),
					Duration.create(clock.getDuration(entry.getValue()),
							TimeUnit.MILLISECONDS), call, system.dispatcher());
		}
		return ref;
	}

	public List<ActorRef> actorsOfClass(Class<?> c) {
		if (actorsByClass.containsKey(c))
			return Collections.unmodifiableList(actorsByClass.get(c));
		return new ArrayList<ActorRef>();
	}

	/**
	 * @return reference to Bus actor that can be used to communicate with other
	 *         (local) actors.
	 */
	public ActorRef getBus() {
		return bus;
	}

	/**
	 * @return akka ActorSystem where the actors are kept running
	 */
	public ActorSystem getSystem() {
		return system;
	}

	/**
	 * @return current time according to the configured clock
	 */
	public long getTime() {
		return clock.getTimeMillis();
	}

	/**
	 * Uses the currently configured clock to calculate how much real-time a
	 * logic time duration should take
	 * 
	 * @param millis
	 *            A duration, in milliseconds
	 * @return A logical duration, in milliseconds, of how much real-time the
	 *         logic time duration should take
	 */
	public long getDuration(long millis) {
		return clock.getDuration(millis);
	}

	/**
	 * @return the Unique Identifier of this agent context
	 */
	public int getUid() {
		return uid;
	}

	/**
	 * @param uid
	 *            the uid to set
	 */
	public void setUid(int uid) {
		this.uid = uid;
	}

	/**
	 * @return the actors
	 */
	public Collection<ActorRef> getActors() {
		return Collections.unmodifiableCollection(actors);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: java -jar agents.jar <config>");
			System.exit(1);
		}

		new AgentContext(new File(args[0]));
	}
}
