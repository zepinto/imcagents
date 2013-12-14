package pt.lsts.imc.agents.sim;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.VehicleMedium;
import pt.lsts.imc.VehicleState;
import pt.lsts.imc.agents.ImcAgent;
import pt.lsts.imc.annotations.Agent;

@Agent(name="Abstract Vehicle Simulator", publishes={Heartbeat.class, EstimatedState.class, GpsFix.class, VehicleMedium.class, VehicleState.class})
public class VehicleSimulator extends ImcAgent {

	
}
