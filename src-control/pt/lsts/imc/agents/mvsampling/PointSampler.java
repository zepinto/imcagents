package pt.lsts.imc.agents.mvsampling;

import info.zepinto.props.Property;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Event;
import pt.lsts.imc.Reference;
import pt.lsts.imc.agents.control.WaypointController;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.Consume;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.Periodic;
import pt.lsts.util.WGS84Utilities;

@Agent(name = "PointSampler", publishes = Event.class)
public class PointSampler extends WaypointController {

	@Property
	double speed = 1.2;

	private double targetLat = Double.NaN, targetLon = Double.NaN,
			targetDepth = Double.NaN;
	
	private String myMaster = null;
	private EstimatedState lastState = null;

	private enum FSM_STATE {
		READY, GOING, SEND_SAMPLE
	};

	private FSM_STATE state = FSM_STATE.READY;
	private double sample = -1, sampleLat, sampleLon, sampleDepth;

	@Periodic(millisBetweenUpdates = 3000)
	public void announce() {
		if (lastState != null) {
			double[] pos = WGS84Utilities.toLatLonDepth(lastState);
			switch (state) {
			case READY:
				sendEvent("Ready", "name", vehicle, "lat",
						Math.toRadians(pos[0]), "lon", Math.toRadians(pos[1]));	
				break;
			case GOING:
				sendEvent("Going", "name", vehicle, "lat",
						Math.toRadians(pos[0]), "lon", Math.toRadians(pos[1]));
				break;
			case SEND_SAMPLE:
				try {
					sendEventReliably("Sample", myMaster, 500, "name", vehicle, "value", sample, "lat", sampleLat, "lon",
							sampleLon, "depth", sampleDepth);
					state = FSM_STATE.READY;
				}
				catch (Exception e) {
					System.err.println("Error sending sample, will retry in 2 seconds.");
				}
				
			default:
				break;
			}
		}
	}

	@Periodic(millisBetweenUpdates = 2500)
	public void printState() {
		System.out.println(getClass().getSimpleName() + "." + vehicle + ":");
		System.out.println("\tState: " + state);
		System.out.println("\tTarget: " + targetLat + ", " + targetLon + ", "
				+ targetDepth);
		System.out.println("\tSample: " + sample);
		System.out.println("\tArrived: " + arrived());
	}

	@EventHandler("Target")
	public void receiveTarget(Event target) {
		
		myMaster = target.getSourceName();
		
		this.targetLat = Double.parseDouble("" + target.getData().get("lat"));
		this.targetLon = Double.parseDouble("" + target.getData().get("lon"));
		this.targetDepth = Double.parseDouble("" + target.getData().get("depth"));

		// 	sample is now indeterminate
		sample = sampleDepth = sampleLat = sampleLon = -1;

		System.out.println("Received target... now going there.");
		
		state = FSM_STATE.GOING;
		send(vehicle, guide());
	}

	@Consume
	public void onState(EstimatedState state) {
		if (state.getSourceName().equals(vehicle)) {
			this.lastState = state;			
		}
	}

	@Override
	public Reference guide() {
		switch (state) {
		case READY:
			if (Double.isNaN(targetLat) && lastState != null) {
				double[] pos = WGS84Utilities.toLatLonDepth(lastState);
				targetLat = Math.toRadians(pos[0]);
				targetLon = Math.toRadians(pos[1]);
				targetDepth = 0;	
			}						
			break;
		case GOING:
			if (arrivedZ() && horizontalDistanceTo(targetLat, targetLon) < 10) {
				double[] pos = WGS84Utilities.toLatLonDepth(lastState);
				sampleLat = pos[0];
				sampleLon = pos[1];
				sampleDepth = lastState.getDepth();
				sample = lastState.getAlt() + lastState.getDepth();
				targetDepth = 0;
				state = FSM_STATE.SEND_SAMPLE;
			}
			break;
		default:
			break;
		}
		if (!Double.isNaN(targetLat))
			return waypoint(targetLat, targetLon, targetDepth, speed);
		return null;
	}
}
