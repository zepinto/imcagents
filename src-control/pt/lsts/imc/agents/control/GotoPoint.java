package pt.lsts.imc.agents.control;

import pt.lsts.imc.DesiredSpeed.SPEED_UNITS;
import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.DesiredZ.Z_UNITS;
import pt.lsts.imc.annotations.Agent;
import pt.lsts.imc.Reference;

@Agent(name="GotoPoint", publishes=Reference.class)
public class GotoPoint extends WaypointController {

	@Override
	public Reference guide() {
		return new Reference()
		.setLat(Math.toRadians(41))
		.setLon(Math.toRadians(-8))
		.setSpeed(new DesiredSpeed(1, SPEED_UNITS.METERS_PS))
		.setZ(new DesiredZ(0, Z_UNITS.DEPTH));
	}
}
