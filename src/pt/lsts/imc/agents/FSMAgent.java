package pt.lsts.imc.agents;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import pt.lsts.imc.Event;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.InitialState;
import pt.lsts.imc.annotations.Periodic;
import pt.lsts.imc.annotations.State;
import pt.lsts.imc.annotations.Transition;

public class FSMAgent extends ImcAgent {

	private String curState = null;
	private LinkedHashMap<String, Method> states = new LinkedHashMap<String, Method>();
	private LinkedHashMap<String, LinkedHashMap<String, Transition>> transitions = new LinkedHashMap<String, LinkedHashMap<String, Transition>>();

	private void loadStates(Class<?> c) {

		if (!FSMAgent.class.equals(c.getSuperclass())) {
			loadStates(c.getSuperclass());
		}

		String initial = null;

		for (Method m : c.getDeclaredMethods()) {
			State s = m.getAnnotation(State.class);
			if (s == null || m.getParameterTypes().length > 0)
				continue;
			if (!m.isAccessible())
				m.setAccessible(true);
			states.put(m.getName(), m);
			if (m.getAnnotation(InitialState.class) != null) {
				if (initial != null)
					System.err
							.println("More than one initial states have been defined!");
				initial = m.getName();
			}

			transitions.put(m.getName(),
					new LinkedHashMap<String, Transition>());
			for (Transition t : s.value())
				transitions.get(m.getName()).put(t.guard(), t);
		}

		if (initial == null)
			System.err
					.println("No initial state defined. Mark one state as initial by annotating it with @InitialState.");

		System.out.println(states);
	}

	public final void transition(String newState, String event) {
		curState = newState;
		if (event != null && !event.isEmpty())
			sendEvent(event);
	}

	@Consume
	public void on(Event event) {
		Transition t = transitions.get(curState).get(event.getTopic());
		if (t != null)
			transition(t.to(), t.event());
	}

	@Periodic(millisBetweenUpdates = 1000)
	public void update() {
		if (!states.containsKey(curState)) {
			System.err.println("State machine entered invalid state: "
					+ curState);
			return;
		}

		try {
			states.get(curState).invoke(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public final void init() {
		super.init();
		loadStates(getClass());
	}
}
