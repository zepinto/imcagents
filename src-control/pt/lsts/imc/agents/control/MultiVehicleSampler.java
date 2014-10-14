package pt.lsts.imc.agents.control;

import info.zepinto.props.Property;

import java.util.LinkedHashMap;
import java.util.Map;

import pt.lsts.imc.Event;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.Periodic;

@Agent(name = "Multi-Vehicle Sampler", publishes = {Event.class})
public class MultiVehicleSampler extends ImcAgent {

	@Property
	int requiredSamplers = 3;
	
	private LinkedHashMap<String, Map<String, ?>> samplers = new LinkedHashMap<String, Map<String,?>>();
	private enum FSM_STATE {DISCOVER_SAMPLERS, DISTRIBUTE_TARGETS, WAIT_FOR_SAMPLES}
	private FSM_STATE state = FSM_STATE.DISCOVER_SAMPLERS;
	
	@EventHandler("Sampler")
	public void newSampler(Map<String, ?> data) {
		String name = ""+data.get("name");
		if (!samplers.containsKey(name)) {
			samplers.put(""+data.get("name"), data);
			System.out.println("Found new sampler: "+name);
		}
	}
	
	@Periodic(millisBetweenUpdates = 1000)
	public void update() {
		switch (state) {
		case DISCOVER_SAMPLERS:
			if (samplers.size() >= requiredSamplers) {
				System.out.println("Minimum number of samplers met. Now sending targets.");
				state = FSM_STATE.DISTRIBUTE_TARGETS;
			}
			break;
		case DISTRIBUTE_TARGETS:
			

		default:
			break;
		}
	}
}
