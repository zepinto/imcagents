package pt.lsts.imc.agents;

import info.zepinto.props.Property;
import info.zepinto.props.PropertyUtils;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.ini4j.Ini;

import pt.lsts.imc.agents.clock.Clock;
import pt.lsts.imc.agents.clock.SimulationClock;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class AgentContext {

	@Property
	int src_id;
	
	private ActorRef bus;
	private ActorSystem system;
	
	// real time is the default clock
	//private Clock clock = new RTClock();
	private Clock clock = new SimulationClock(10);

	// Singleton
	private static AgentContext instance = null;
	private AgentContext() {
		this.system = ActorSystem.create();
		this.bus = system.actorOf(Props.create(MessageBus.class));
	}

	public static AgentContext instance() {
		if (instance == null)
			instance = new AgentContext();
		return instance;
	}
	
	public void parseConfig(File config) throws Exception {
		Ini ini = new Ini(config);

		for (Ini.Section section : ini.values()) {
			String name = section.getName();
			Properties props = new Properties();
			for (String option : section.keySet())
				props.put(option, section.fetch(option));
			if (!name.equals("AgentContext")) {							
				Class<?> c;
				try {
					c = Class.forName(name);
				}
				catch (Exception e) {
					c = Class.forName("pt.lsts.imc.agents."+name);
				}
				bootstrap(c, props);
			}
			else {
				PropertyUtils.setProperties(this, props, false);
			}
		}
		
		System.out.println(src_id);
	}
	
	public ActorRef bootstrap(Class<?> c, Properties properties) {
		ActorRef ref = system.actorOf(Props.create(c));
		Channel chan = new Channel(c);
		bus.tell(chan, ref);
		ref.tell(properties, bus);
		Map<String, Integer> periodicCalls = chan.periodicCalls();
		for (Entry<String, Integer> entry : periodicCalls.entrySet()) {
			PeriodicCall call = new PeriodicCall(ref, entry.getKey(), entry.getValue());
			system.scheduler().schedule(
					Duration.create(clock.getDuration(entry.getValue()), TimeUnit.MILLISECONDS),
					Duration.create(clock.getDuration(entry.getValue()), TimeUnit.MILLISECONDS), call, system.dispatcher());
		}
		return ref;
	}


	public ActorRef getBus() {
		return bus;
	}

	public ActorSystem getSystem() {
		return system;
	}

	public long getTime() {
		return clock.getTimeMillis();
	}
	
	public long getDuration(long millis) {
		return clock.getDuration(millis);
	}

	public Date newDate() {
		return new Date(clock.getTimeMillis());
	}

	public Calendar calendarInstance() {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(getTime());
		return c;
	}

	public static void main(String[] args) throws Exception {
		AgentContext.instance().parseConfig(new File("/home/zp/Desktop/agents.props"));
	}

}
