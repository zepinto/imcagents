package pt.lsts.imc.agents;

import info.zepinto.props.PropertyUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import pt.lsts.imc.Event;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.Periodic;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

/**
 * This class is extended by all IMC agents (agents that exchange IMC messages
 * according to a specified interface)
 * 
 * @author zp
 *
 */
public class ImcAgent extends UntypedActor {

	private ActorRef bus;
	private LinkedHashMap<Class<?>, Method> messageHandlers = new LinkedHashMap<>();
	private LinkedHashMap<String, Method> eventHandlers = new LinkedHashMap<>();

	// Initialization block that uses introspection to build message handlers
	// structure
	{
		HashSet<Method> methods = new HashSet<>();

		for (Class<?> cl = getClass(); cl != Object.class; cl = cl
				.getSuperclass()) {
			methods.addAll(Arrays.asList(cl.getMethods()));
			methods.addAll(Arrays.asList(cl.getDeclaredMethods()));

			for (Method m : methods) {
				Consume c = m.getAnnotation(Consume.class);
				if (c == null)
					continue;
				Class<?>[] params = m.getParameterTypes();
				if (params.length != 1)
					continue;

				// If there is subclass already handling these events, do not
				// override it
				if (messageHandlers.containsKey(params[0]))
					continue;

				m.setAccessible(true);
				messageHandlers.put(params[0], m);
			}

			for (Method m : methods) {
				EventHandler h = m.getAnnotation(EventHandler.class);
				if (h == null)
					continue;
				String event = h.value();

				Class<?>[] params = m.getParameterTypes();
				if (params.length > 1)
					continue;

				// If there is subclass already handling these events, do not
				// override it
				if (eventHandlers.containsKey(event))
					continue;

				m.setAccessible(true);
				eventHandlers.put(event, m);
			}
		}
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
	 * Send a message to the bus. Internally, the Bus actor will be in charge of
	 * dispatching the message to other actors.
	 * 
	 * @param m
	 *            The message to be dispatched.
	 */
	public void send(IMCMessage m) {
		if (AgentContext.instance().getUid() == -1) {
			System.err.println("Not yet connected.");
			return;
		}
		m.setTimestampMillis(AgentContext.instance().getTime());
		m.setSrc(AgentContext.instance().getUid());
		m.setSrcEnt(AgentContext.instance().entityOf(getSelf()));
		if (bus != null)
			bus.tell(m, getSelf());
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
		} 
		else {
			sendEvent(topic);
		}
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

	/**
	 * This method serializes the Agent behavior (class definition) and state
	 * (fields marked with {@link Periodic}) to a byte array.
	 * 
	 * @return The current agent state, serialized to a byte array.
	 * @throws Exception
	 *             In case it is not possible to access the file system to load
	 *             the agent behavior.
	 */
	public byte[] serializeAgent() throws Exception {
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
			init();
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
		// A "stop" message is sent to each agent right before terminating
		else if ("stop".equals(msg)) {
			stop();
			return;
		}

		if (msg instanceof Event) {
			Event evt = (Event) msg;
			if (eventHandlers.containsKey(evt.getTopic())) {
				Method handler = eventHandlers.get(evt.getTopic());

				if (handler.getParameterTypes().length == 0) {
					handler.invoke(this);
				} else if (Map.class.isAssignableFrom(handler
						.getParameterTypes()[0])) {
					handler.invoke(this, (Map<String, String>) evt.getData());
				}
			}
		}

		// All other cases will correspond to message events
		if (msg instanceof IMCMessage) {
			Class<?> clazz = msg.getClass();
			while (clazz != IMCMessage.class) {
				if (messageHandlers.containsKey(clazz)) {
					messageHandlers.get(clazz).invoke(this, msg);
					return;
				}
				clazz = clazz.getSuperclass();
			}

			// Check if there is a "generic" message handler
			if (messageHandlers.containsKey(IMCMessage.class)) {
				messageHandlers.get(IMCMessage.class).invoke(this, msg);
				return;
			}
		}
		// If there is no handler for this type of message, signal that back
		unhandled(msg);
	}
}
