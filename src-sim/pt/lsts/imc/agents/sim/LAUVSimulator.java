package pt.lsts.imc.agents.sim;

import pt.lsts.imc.DesiredPath;
import pt.lsts.imc.PathControlState;
import pt.lsts.imc.agents.coords.Location;
import pt.lsts.imc.annotations.Agent;

@Agent(name="LAUV Simulator", publishes={})
public class LAUVSimulator extends VehicleSimulator {

	public PathControlState update(DesiredPath path, UnicycleModel model) {
		System.out.println("update model");
		PathControlState state = new PathControlState();
		double speed = 1.25;
		if (path != null) {
				Location loc = new Location(Math.toDegrees(path.getEndLat()), Math.toDegrees(path.getEndLon()));
				switch (path.getEndZUnits()) {
				case DEPTH:
					loc.setDepth(Math.max(0, path.getEndZ()));
					break;
				case ALTITUDE:
					loc.setDepth(Math.min(0, -path.getEndZ()));
					break;			
				default:
					loc.setDepth(0);
					break;
				}
				
				switch (path.getSpeedUnits()) {
				case METERS_PS:
					speed = path.getSpeed();
					break;
				case PERCENTAGE:
					speed = percentageToMps(path.getSpeed());
					break;
				case RPM:
					speed = rpmToMps(path.getSpeed());
				default:
					break;
				}
				
			model.guide(loc, speed);
			state.setEndLon(path.getEndLon());
			state.setEndZ(path.getEndZ());
			state.setEndZUnits((short)path.getEndZUnits().value());
			state.setLradius(0);
		}
		
		
		
	
		//TODO
		return state;
		
	}
	
	public double rpmToMps(double rpm) {
		return rpm / 1000;
	}

	public double percentageToMps(double percentage) {
		return (percentage / 100.0) * 1.6;
	}

	public double maxSpeedMps() {
		return 1.6;
	}

}
