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
	private int requiredSamplers = 3;
	
	@Property
	private double cellWidth = 20;
	
	
	private LinkedHashMap<String, Map<String, ?>> samplers = new LinkedHashMap<String, Map<String,?>>();
	private enum FSM_STATE {DISCOVER_SAMPLERS, DISTRIBUTE_TARGETS, WAIT_FOR_SAMPLES}
	private FSM_STATE state = FSM_STATE.DISCOVER_SAMPLERS;
	private String myHost = null;
	private boolean firstSampling = true;
	
	@EventHandler("Sampler")
	public void newSampler(Event msg) {
		String name = ""+msg.getData().get("name");
		if (msg.getSrc() == getSrcId())
			myHost = name;
		
		if (!samplers.containsKey(name)) {
			samplers.put(name, msg.getData());
			System.out.println("Found new sampler: "+name+", "+(requiredSamplers-samplers.size())+" left.");			
		}
	}
	
	@Periodic(millisBetweenUpdates = 1000)
	public void update() {
		switch (state) {
		case DISCOVER_SAMPLERS:
			if (samplers.size() >= requiredSamplers) {
				System.out.println("Minimum number of samplers met. Will now send targets.");
				state = FSM_STATE.DISTRIBUTE_TARGETS;
			}
			break;
		case DISTRIBUTE_TARGETS:
			if (firstSampling) {
				
			}

		default:
			break;
		}
	}
}
