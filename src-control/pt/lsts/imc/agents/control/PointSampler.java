package pt.lsts.imc.agents.control;

import info.zepinto.props.Property;

import java.util.Map;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Event;
import pt.lsts.imc.Reference;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.Periodic;

@Agent(name="PointSampler", publishes=Event.class)
public class PointSampler extends WaypointController {

	@Property
	double speed = 1.2;
	
	private double targetLat = Double.NaN, targetLon = Double.NaN, targetDepth = Double.NaN;
	private EstimatedState lastState = null;
	private enum FSM_STATE {WAIT, GOTO_POINT, SURFACING};
	private FSM_STATE state = FSM_STATE.WAIT;
	private double sample = -1, sampleLat, sampleLon, sampleDepth;
	
	@Periodic(millisBetweenUpdates=5000)
	public void announce() {
		sendEvent("Sampler", "name", vehicle);
	}
	
	@Periodic(millisBetweenUpdates=2500)
	public void printState() {
		System.out.println(getClass().getSimpleName()+":");
		System.out.println("\tState: "+state);
		System.out.println("\tTarget: "+targetLat+", "+targetLon+", "+targetDepth);
		System.out.println("\tSample: "+sample);		
	}
	
	@EventHandler("Target")
	public void receiveTarget(Map<String, ?> data) {
		this.targetLat = Double.parseDouble(""+data.get("lat"));
		this.targetLon = Double.parseDouble(""+data.get("lon"));
		this.targetDepth = Double.parseDouble(""+data.get("depth"));
		
		// sample is now indeterminate
		sample = sampleDepth = sampleLat = sampleLon = -1;
		
		state = FSM_STATE.GOTO_POINT;
	}
	
	@Consume
	public void onState(EstimatedState state) {
		this.lastState = state;
	}
	
	@Override
	public Reference guide() {
		
		if (arrived()) {
			switch (state) {
			case GOTO_POINT:
				if (sample == -1 && lastState != null) {
					sample = lastState.getAlt();
					sampleLat = lastState.getLat();
					sampleLon = lastState.getLon();
					sampleDepth = lastState.getDepth();
				}
				break;
			case SURFACING:
				sendEvent("Sample", "value", sample);
			default:
				break;
			}
		}
		
		if (Double.isNaN(targetLat) && lastState != null) {
			targetLat = lastState.getLat();
			targetLon = lastState.getLon();
			targetDepth = 0;
		}
		if (!Double.isNaN(targetLat))
			return waypoint(targetLat, targetLon, targetDepth, speed);
		
		return null;
	}
}
