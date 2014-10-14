package pt.lsts.imc.agents.control;

import info.zepinto.props.Property;

import java.util.LinkedHashMap;
import java.util.Map;

import pt.lsts.imc.Event;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.Periodic;
import pt.lsts.util.WGS84Utilities;

@Agent(name = "Multi-Vehicle Sampler", publishes = { Event.class })
public class MultiVehicleSampler extends ImcAgent {

	@Property
	private int requiredSamplers = 3;

	@Property
	private double cellWidth = 50;

	@Property
	private double surveyDepth = 3;

	private LinkedHashMap<String, Map<String, ?>> readysamplers = new LinkedHashMap<String, Map<String, ?>>();
	private LinkedHashMap<String, Map<String, ?>> busysamplers = new LinkedHashMap<String, Map<String, ?>>();

	private enum FSM_STATE {
		DISCOVER_SAMPLERS, DISTRIBUTE_TARGETS, WAIT_FOR_SAMPLES
	}

	private FSM_STATE state = FSM_STATE.DISCOVER_SAMPLERS;
	private String myHost = null;
	private boolean firstSampling = true;

	LinkedHashMap<String, double[]> targets = new LinkedHashMap<String, double[]>();

	@EventHandler("Ready")
	public void samplerReady(Event msg) {
		String name = "" + msg.getData().get("name");
		if (msg.getSrc() == getSrcId())
			myHost = name;
		readysamplers.put(name, msg.getData());
	}

	@EventHandler("Going")
	public void samplerGoing(Event msg) {
		String name = "" + msg.getData().get("name");
		busysamplers.put(name, msg.getData());
		readysamplers.remove(name);
	}

	@Periodic(millisBetweenUpdates = 1000)
	public void update() {
		switch (state) {
		case DISCOVER_SAMPLERS:
			if (readysamplers.size() >= requiredSamplers && readysamplers.containsKey(myHost)) {
				System.out
						.println("Minimum number of samplers met. Will now send targets.");
				
				state = FSM_STATE.DISTRIBUTE_TARGETS;
			}
			break;
		case DISTRIBUTE_TARGETS:
			if (firstSampling) {
				Map<String, ?> ann = readysamplers.get(myHost);
				double myLat = Double.parseDouble("" + ann.get("lat"));
				double myLon = Double.parseDouble("" + ann.get("lon"));
				String[] names = readysamplers.keySet().toArray(new String[0]);

				// myTarget
				sendEvent("Target", "vehicle", names[0], "lat", myLat, "lon",
						myLon, "depth", surveyDepth);
				double ang30 = Math.toRadians(30);

				double pos2[] = WGS84Utilities.WGS84displace(
						Math.toDegrees(myLat), Math.toDegrees(myLon), 0,
						Math.cos(ang30) * cellWidth, Math.sin(ang30)
								* cellWidth, 0);
				double pos3[] = WGS84Utilities.WGS84displace(
						Math.toDegrees(myLat), Math.toDegrees(myLon), 0,
						Math.cos(-ang30) * cellWidth, Math.sin(-ang30)
								* cellWidth, 0);

				sendEvent("Target", "vehicle", names[1], "lat",
						Math.toRadians(pos2[0]), "lon",
						Math.toRadians(pos2[1]), "depth", surveyDepth);
				sendEvent("Target", "vehicle", names[2], "lat",
						Math.toRadians(pos3[0]), "lon",
						Math.toRadians(pos3[1]), "depth", surveyDepth);
			}
		default:
			break;
		}
	}
}
