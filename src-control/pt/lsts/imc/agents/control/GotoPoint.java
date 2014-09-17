package pt.lsts.imc.agents.control;

import java.util.Map;

import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.DesiredSpeed.SPEED_UNITS;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.DesiredZ.Z_UNITS;
import pt.lsts.imc.Event;
import pt.lsts.imc.Reference;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.annotations.EventHandler;
import pt.lsts.imc.annotations.Periodic;

@Agent(name="GotoPoint", publishes=Event.class)
public class GotoPoint extends WaypointController {

	@Override
	public Reference guide() {
		return new Reference()
		.setLat(Math.toRadians(41))
		.setLon(Math.toRadians(-8))
		.setSpeed(new DesiredSpeed(1, SPEED_UNITS.METERS_PS))
		.setZ(new DesiredZ(0, Z_UNITS.DEPTH));
	}
	
	@Periodic(millisBetweenUpdates=5000)
	public void startControl() {
		if (currentState != STATE.Controlling)
			sendEvent("setBehavior", "ctrl_id", ctrl_id);
	}
	
	@EventHandler("LinkCreated")
	public void newLink(Map<String, ?> data) {
		System.out.println("New Link: "+data);
	}
	
	@EventHandler("LinkDropped")
	public void dropLink(Map<String, ?> data) {
		System.out.println("Link Dropped: "+data);
	}
	
	@Override
	public void init() {
		super.init();
		
	}
}
