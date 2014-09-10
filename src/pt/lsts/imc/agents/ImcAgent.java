package pt.lsts.imc.agents;

import info.zepinto.props.PropertyUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.Periodic;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

/**
 * This class is extended by all IMC agents (exchange IMC messages)
 * 
 * @author zp
 *
 */
public class ImcAgent extends UntypedActor {

	private ActorRef bus;
	private LinkedHashMap<Class<?>, Method> messageHandlers = new LinkedHashMap<>();

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
		}
	}

	/**
	 * Send a message to the bus. Internally, the Bus actor will be in charge of
	 * dispatching the message to other actors.
	 * 
	 * @param m
	 *            The message to be dispatched.
	 */
	public void send(IMCMessage m) {
		m.setTimestampMillis(AgentContext.instance().getTime());
		m.setSrc(AgentContext.instance().src_id);
		m.setSrcEnt(AgentContext.instance().entityOf(getSelf()));
		if (bus != null)
			bus.tell(m, getSelf());
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
