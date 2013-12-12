package pt.lsts.imc.agents;

import info.zepinto.props.PropertyUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.annotations.Consume;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

/**
 * This class is extended by all IMC agents that exchange IMC messages
 * @author zp
 *
 */
public class ImcAgent extends UntypedActor {

	private ActorRef bus;
	private LinkedHashMap<Class<?>, Method> messageHandlers = new LinkedHashMap<>();	

	// Initialization block that uses introspection to build message handlers structure
	{
		HashSet<Method> methods = new HashSet<>();
		methods.addAll(Arrays.asList(getClass().getMethods()));
		methods.addAll(Arrays.asList(getClass().getDeclaredMethods()));

		for (Method m : methods) {
			Consume c = m.getAnnotation(Consume.class);
			if (c == null)
				continue;
			Class<?>[] params = m.getParameterTypes();
			if (params.length != 1)
				continue;

			m.setAccessible(true);
			messageHandlers.put(params[0], m);
		}
	}

	public void send(IMCMessage m) {
		if (bus != null)
			bus.tell(m, getSelf());		
	}

	public void init() {
		System.out.println("init() for "+getClass().getSimpleName()+" called");
	}

	public void stop() {
		System.out.println("stop() for "+getClass().getSimpleName()+" called");
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		// First message sent to each agent its their properties
		if (msg instanceof Properties) {
			PropertyUtils.setProperties(this, ((Properties)msg), false);
			bus = getSender();
			init();
		}
		else if (msg instanceof PeriodicCall) {
			PeriodicCall call = (PeriodicCall) msg;
			// drop in case of delays
			if (call.nextTime <= System.currentTimeMillis())
				return;
			try {
				getClass().getMethod(call.method).invoke(this);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		// A "stop" message is sent to each agent right before terminating
		else if ("stop".equals(msg))
			stop();
		// All other cases will correspond to message events
		else if (messageHandlers.containsKey(msg.getClass()))
			messageHandlers.get(msg.getClass()).invoke(this, msg);		
		else
		// If there is no handler for this type of message, signal that back
			unhandled(msg);
	}
}
