package pt.lsts.imc.agents;

import info.zepinto.props.Property;
import info.zepinto.props.PropertyUtils;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.ini4j.Ini;

import pt.lsts.imc.agents.clock.Clock;
import pt.lsts.imc.agents.clock.SimulationClock;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.typesafe.config.ConfigFactory;

public class AgentContext {

	@Property
	int src_id = 0;
	
	@Property
	int local_port = 9000;
	
	@Property
	int remote_port = -1;
	
	@Property
	String remote_host = "localhost";
	
	private ActorRef bus;
	private ActorSystem system;
	
	// real time is the default clock
	//private Clock clock = new RTClock();
	private Clock clock = new SimulationClock(5.0);
	private Vector<ActorRef> actors = new Vector<>();

	public int entityOf(ActorRef actor) {
		if (!actors.contains(actor))
			actors.add(actor);
		
		return actors.indexOf(actor);
	}
	
	// Singleton
	private static AgentContext instance = null;
	private AgentContext(File config) {
		instance = this;
		try {
			parseConfig(config);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static AgentContext instance() {
		return instance;
	}
	
	public void parseConfig(File config) throws Exception {
		Ini ini = new Ini(config);
		Ini.Section sec = ini.get("AgentContext");
		Properties properties = new Properties();
		for (String option : sec.keySet())
			properties.put(option, sec.fetch(option));
		
		PropertyUtils.setProperties(this, properties, false);
		this.system = ActorSystem.create("IMCAgents", ConfigFactory.parseProperties(properties));
		this.bus = system.actorOf(Props.create(MessageBus.class));
		
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
		}
	}
	
	public ActorRef bootstrap(Class<?> c, Properties properties) {
		
		System.out.println("Creating agent of class "+c.getName());
		
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
		new AgentContext(new File("conf/twirl.props"));
	}

}
