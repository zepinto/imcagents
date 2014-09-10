package pt.lsts.imc.agents;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.Periodic;

/**
 * This class aggregates information about one agent in terms of its inputs and
 * outputs (interface). Inputs are messages listened and periodic calls, while
 * outputs are the messages that the agent declares to produce.
 * 
 * @author zp
 *
 */
public class AgentInterface {

	private HashSet<Class<?>> messagesToListen = new HashSet<>();
	private HashSet<Class<?>> messagesProduced = new HashSet<>();
	private LinkedHashMap<String, Integer> periodicCalls = new LinkedHashMap<>();
	private Class<?> agentClass;

	/**
	 * Constructs an AgentInterface that will use introspection to populate
	 * valid inputs and output for given Agent class.
	 * 
	 * @param agentClass
	 *            The class to be used to populate the interface
	 */
	public AgentInterface(Class<?> agentClass) {
		this.agentClass = agentClass;
		for (Class<?> c = agentClass; c != Object.class; c = c.getSuperclass()) {
			Agent a = c.getAnnotation(Agent.class);
			if (a != null) {
				for (Class<? extends IMCMessage> m : a.publishes()) {
					messagesProduced.add(m);
				}
			}

			for (Method m : getMethods(c)) {
				Consume cons = m.getAnnotation(Consume.class);
				Periodic p = m.getAnnotation(Periodic.class);
				if (cons != null) {
					Class<?>[] params = m.getParameterTypes();
					if (params.length != 1)
						continue;

					m.setAccessible(true);
					messagesToListen.add(m.getParameterTypes()[0]);
				}
				if (p != null) {
					if (m.getParameterTypes().length != 0)
						continue;
					periodicCalls.put(m.getName(), p.millisBetweenUpdates());
				}
			}
		}
	}

	/**
	 * @return An unmodifiable map with all the methods marked with
	 *         {@link Periodic} annotaion.
	 */
	public Map<String, Integer> periodicCalls() {
		return Collections.unmodifiableMap(periodicCalls);
	}

	/**
	 * Checks this interface to verify if it accepts the given message type
	 * 
	 * @param o
	 *            A message of any type.
	 * @return <code>true</code> if the agent accepts messages of given type or
	 *         <code>false</code> otherwise.
	 */
	public boolean accepts(Object o) {
		return messagesToListen.contains(o.getClass())
				|| messagesToListen.contains(IMCMessage.class);
	}

	/**
	 * Checks this interface to verify if this agent is allowed to sent messages
	 * of given type.
	 * 
	 * @param o
	 *            A message of any type.
	 * 
	 * @return <code>true</code> if the agent is allowed to send messages of
	 *         given type or <code>false</code> otherwise.
	 */
	public boolean allowedToSend(Object o) {
		return messagesProduced.contains(o.getClass());
	}

	private static Collection<Method> getMethods(Class<?> clazz) {
		HashSet<Method> methods = new HashSet<>();
		methods.addAll(Arrays.asList(clazz.getMethods()));
		methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
		return methods;
	}

	@Override
	public String toString() {
		String s = agentClass.getSimpleName() + " in[ ";
		for (Class<?> c : messagesToListen)
			s += c.getSimpleName() + " ";
		s += "]  out[ ";
		for (Class<?> c : messagesProduced)
			s += c.getSimpleName() + " ";

		return s + "]";
	}
}
