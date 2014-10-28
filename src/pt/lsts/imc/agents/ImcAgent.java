package pt.lsts.imc.agents;

import info.zepinto.props.Property;
import info.zepinto.props.PropertyUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import pt.lsts.imc.Event;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LogBookEntry;
import pt.lsts.imc.LogBookEntry.TYPE;
import pt.lsts.imc.agents.net.DeliveryResult;
import pt.lsts.imc.agents.net.ImcProtocol;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.Periodic;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

/**
 * This class is extended by all IMC agents (agents that exchange IMC messages
 * according to a specified interface)
 * 
 * @author zp
 *
 */
@Agent(name="IMCAgent", publishes=LogBookEntry.class)
public class ImcAgent extends UntypedActor {

	@Property 
	String name = getClass().getSimpleName();
	
	private ActorRef bus;
	private LinkedHashMap<Class<?>, List<Method>> messageHandlers = new LinkedHashMap<>();
	private LinkedHashMap<String, List<Method>> eventHandlers = new LinkedHashMap<>();

	private static int reliable_id = 1;
	// Initialization block that uses introspection to build message handlers
	// structure
	{
		HashSet<Method> methods = new HashSet<>();

		for (Class<?> cl = getClass(); cl != Object.class; cl = cl
				.getSuperclass()) {
			methods.addAll(Arrays.asList(cl.getMethods()));
			methods.addAll(Arrays.asList(cl.getDeclaredMethods()));
		}
		for (Method m : methods) {

			Consume c = m.getAnnotation(Consume.class);
			if (c == null)
				continue;
			Class<?>[] params = m.getParameterTypes();
			if (params.length != 1)
				continue;

			if (!messageHandlers.containsKey(params[0]))
				messageHandlers.put(params[0], new ArrayList<Method>());

			m.setAccessible(true);
			messageHandlers.get(params[0]).add(m);
		}

		for (Method m : methods) {
			EventHandler h = m.getAnnotation(EventHandler.class);
			if (h == null)
				continue;
			String event = h.value();

			Class<?>[] params = m.getParameterTypes();
			if (params.length > 1)
				continue;

			if (!eventHandlers.containsKey(event))
				eventHandlers.put(event, new ArrayList<Method>());

			m.setAccessible(true);
			eventHandlers.get(event).add(m);
		}
	}
	
	public String getAgentName() {
		return name;
	}
	
	public void setAgentName(String name) {
		this.name = name;
	}

	/**
	 * Send a message to a known destination
	 * 
	 * @param destination
	 *            The name of the imc destination (Example: "lauv-xplore-1")
	 * @param m
	 *            The message to be sent to the destination
	 */
	public void send(String destination, IMCMessage m) {
		if (destination != null)
			m.setDst(IMCDefinition.getInstance().getResolver()
					.resolve(destination));

		send(m);
	}

	/**
	 * Post a message to the internal message bus. The source, destination and
	 * src_ent fields will be left untouched. dispatching the message to other
	 * actors.
	 * 
	 * @param m
	 *            The message to be dispatched.
	 */
	public void postInternally(IMCMessage m) {
		if (bus != null)
			bus.tell(m, getSelf());
	}

	/**
	 * Send a message to the bus. Internally, the Bus actor will be in charge of
	 * dispatching the message to other actors.
	 * 
	 * @param m
	 *            The message to be dispatched.
	 */
	public void send(IMCMessage m) {
		if (AgentContext.instance().getUid() == -1) {
			postInternally(m);
			return;
		}
		m.setTimestampMillis(AgentContext.instance().getTime());
		m.setSrc(AgentContext.instance().getUid());
		m.setSrcEnt(AgentContext.instance().entityOf(getSelf()));
		if (bus != null)
			bus.tell(m, getSelf());
	}

	public void sendReliably(String destination, IMCMessage m, long timeoutMillis) throws Exception {
		if (AgentContext.instance().getUid() == -1) {
			throw new Exception("Not connected yet");
		}
		m.setTimestampMillis(AgentContext.instance().getTime());
		m.setSrc(AgentContext.instance().getUid());
		m.setSrcEnt(AgentContext.instance().entityOf(getSelf()));
		m.setValue("__reliable", reliable_id++);
		m.setValue("__dst", destination);
		m.setValue("__timeout", timeoutMillis);
		List<ActorRef> imcAgents = AgentContext.instance().actorsOfClass(ImcProtocol.class);
		if (imcAgents == null || imcAgents.isEmpty())
			throw new Exception("No agent is capable of delivering the message");

		ActorRef ref = imcAgents.get(0);
		Future<Object> result = Patterns.ask(ref, m, timeoutMillis);
		DeliveryResult r = (DeliveryResult) Await.result(result,
				Duration.create(timeoutMillis*2, TimeUnit.MILLISECONDS));
		if (!r.isSuccess())
			r.getException();
	}

	/**
	 * Creates and sends message of type {@link Event} with given topic and data
	 * 
	 * @param topic
	 *            The topic of the event to be send
	 * @param data
	 *            The data to be added to the event.
	 */
	public void sendEvent(String topic, Map<String, ?> data) {
		if (data != null) {
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
			copy.putAll(data);
			send(new Event().setTopic(topic).setData(copy));
		} else
			sendEvent(topic);
	}

	/**
	 * This method creates a message of type {@link Event} with given topic and
	 * parses any remaining (pair of) arguments to initialize the included data.
	 * Example: <br/>
	 * 
	 * <pre>
	 * &#064;Consume
	 * public void on(Announce ann) {
	 * 	sendEvent(&quot;NewLink&quot;, &quot;name&quot;, ann.getSysName(), &quot;type&quot;, ann.getSysType()
	 * 			.toString());
	 * }
	 * </pre>
	 * 
	 * @param topic
	 *            The topic of the event to be send
	 * @param data
	 *            The data to be added to the event. The data is passed as list
	 *            pairs of arguments where data[i] will be the key, data[i+1]
	 *            will be the value.
	 * 
	 */
	public void sendEvent(String topic, Object... data) {
		if (data != null) {
			LinkedHashMap<String, Object> map = new LinkedHashMap<>();
			for (int i = 0; i < data.length - 1; i += 2)
				map.put("" + data[i], data[i + 1]);

			send(new Event().setTopic(topic).setData(map));
		} else {
			sendEvent(topic);
		}
	}

	public void sendEventReliably(String topic, String destination, long timeoutMillis, Object... data) throws Exception {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		if (data != null) {
			for (int i = 0; i < data.length - 1; i += 2)
				map.put("" + data[i], data[i + 1]);
		}
		sendReliably(destination, new Event().setTopic(topic).setData(map), timeoutMillis);		
	}

	/**
	 * Creates and sends message of type {@link Event} with given topic and no
	 * data
	 * 
	 * @param event
	 *            The topic of the event
	 * 
	 * @see #sendEvent(String, Map)
	 */
	public void sendEvent(String event) {
		send(new Event().setTopic(event));
	}

	/**
	 * This method is called upon initialization. Override if needed.
	 */
	public void init() {
	}

	/**
	 * This method is called when the agent is being stopped. Override if
	 * needed.
	 */
	public void stop() {
	}

	/**
	 * This agent should not receive any more messages
	 */
	protected void terminate() {
		context().stop(getSelf());
	}
	
	public pt.lsts.imc.Agent serializeToImc() {
		pt.lsts.imc.Agent agent = new pt.lsts.imc.Agent();
		agent.setAgentClass(getClass().getName());
		agent.setAgentName(getAgentName());
		agent.setAgentState(PropertyUtils.getProperties(this, false));
		return agent;
	}
		
	/**
	 * This method serializes the Agent behavior (class definition) and state
	 * (fields marked with {@link Periodic}) to a byte array.
	 * 
	 * @return The current agent state, serialized to a byte array.
	 * @throws Exception
	 *             In case it is not possible to access the file system to load
	 *             the agent behavior.
	 */
	public byte[] serializeAgentFull() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zipOut = new ZipOutputStream(baos);
		zipOut.setMethod(ZipOutputStream.DEFLATED);
		zipOut.setLevel(9);

		// Create file with current state of the agent
		String propsOut = PropertyUtils.saveProperties(this);
		zipOut.putNextEntry(new ZipEntry("State"));
		zipOut.write(propsOut.getBytes());

		// Create file with class (behavior) definition
		zipOut.putNextEntry(new ZipEntry("Behavior"));

		// find .class where our code is stored
		String classFile = getClass().getProtectionDomain().getCodeSource()
				.getLocation().getFile();
		classFile += getClass().getCanonicalName().replaceAll("\\.", "/");
		classFile += ".class";

		// load class file into a byte array
		FileInputStream fis = new FileInputStream(classFile);
		byte buffer[] = new byte[1024];
		int read = 0;

		// write all bytes to the output stream
		while ((read = fis.read(buffer)) > 0) {
			zipOut.write(buffer, 0, read);
		}

		// close all streams
		fis.close();
		baos.close();
		zipOut.close();

		// return resulting data
		return baos.toByteArray();
	}

	public int getEntityId() {
		return AgentContext.instance().entityOf(getSelf());
	}

	public int getSrcId() {
		return AgentContext.instance().getUid();
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		// First message sent to each agent its their properties
		if (msg instanceof Properties) {
			PropertyUtils.setProperties(this, ((Properties) msg), false);
			bus = getSender();
			return;
		} else if (msg instanceof PeriodicCall) {
			PeriodicCall call = (PeriodicCall) msg;
			// drop in case of delays
			if (call.nextTime <= AgentContext.instance().getTime())
				return;
			try {
				getClass().getMethod(call.method).invoke(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		else if ("init".equals(msg)) {
			init();
			return;
		}
		// A "stop" message is sent to each agent right before terminating
		else if ("stop".equals(msg)) {
			stop();
			return;
		}
		else if ("state".equals(msg)) {
			getSender().tell(serializeToImc(), getSelf());
		}

		if (msg instanceof Event) {
			Event evt = (Event) msg;
			if (eventHandlers.containsKey(evt.getTopic())) {
				List<Method> handlers = eventHandlers.get(evt.getTopic());

				for (Method handler : handlers) {
					if (handler.getParameterTypes().length == 0) {
						handler.invoke(this);
					} else if (Map.class.isAssignableFrom(handler
							.getParameterTypes()[0])) {
						handler.invoke(this, (Map<String, String>) evt.getData());
					} else if (handler.getParameterTypes()[0].equals(Event.class)) {
						handler.invoke(this, evt);
					}
				}				
			}
		}

		// All other cases will correspond to message events
		if (msg instanceof IMCMessage) {
			Class<?> clazz = msg.getClass();
			while (clazz != IMCMessage.class) {
				if (messageHandlers.containsKey(clazz)) {					
					for (Method m : messageHandlers.get(clazz)) {
						m.invoke(this, msg);	
					}					

					return;
				}
				clazz = clazz.getSuperclass();
			}

			// Check if there is a "generic" message handler
			if (messageHandlers.containsKey(IMCMessage.class)) {
				for (Method m : messageHandlers.get(IMCMessage.class)) {
					m.invoke(this, msg);	
				}	
				return;
			}
		}
		// If there is no handler for this type of message, signal that back
		unhandled(msg);
	}

	private void log(TYPE type, String text) {
		send(new LogBookEntry()
		.setContext(getClass().getSimpleName())
		.setHtime(AgentContext.instance().getTime() / 1000.0)
		.setType(type)
		.setText(text));
	}

	public void inf(String text) {
		log(TYPE.INFO, text);
	}

	public void war(String text) {
		log(TYPE.WARNING, text);
	}

	public void debug(String text) {
		log(TYPE.DEBUG, text);
	}

	public void err(String text) {
		log(TYPE.ERROR, text);
	}

	public void critical(String text) {
		log(TYPE.CRITICAL, text);
	}
}
